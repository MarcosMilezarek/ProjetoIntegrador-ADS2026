package com.rh.recrutamento.backend.analise.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Unico ponto de contato com o OpenRouter (POST /chat/completions). Chave e modelo vem de
 * OPENROUTER_API_KEY e OPENROUTER_MODEL; a chave so vai no cabecalho Authorization e nunca e logada.
 * Sem retentativa: quem chama decide o que fazer com a falha.
 */
@Component
public class OpenRouterClient {

    private static final Duration TEMPO_CONEXAO = Duration.ofSeconds(5);
    private static final Duration TEMPO_RESPOSTA = Duration.ofSeconds(120); // modelo com raciocinio demora mais

    /** Raciocinio ligado: o modelo configurado (qwen/qwen3.8-27b:free) raciocina antes de responder. A resposta fica em content. */
    private static final Map<String, Object> REASONING = Map.of("enabled", true);

    /** Saida pedida ao modelo. Nem todo modelo a respeita: quem chama valida de novo. */
    private static final Map<String, Object> RESPONSE_FORMAT = Map.of(
        "type", "json_schema",
        "json_schema", Map.of(
            "name", "analise_candidatura",
            "strict", true,
            "schema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "aderencia", Map.of("type", "integer", "description", "Porcentagem de 0 a 100"),
                    "pontos_positivos", Map.of("type", "array", "items", Map.of("type", "string")),
                    "pontos_negativos", Map.of("type", "array", "items", Map.of("type", "string"))),
                "required", List.of("aderencia", "pontos_positivos", "pontos_negativos"),
                "additionalProperties", false)));

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final String url;
    private final String chave;
    private final String modelo;

    public OpenRouterClient(ObjectMapper objectMapper,
                            @Value("${app.openrouter.url:https://openrouter.ai/api/v1/chat/completions}") String url,
                            @Value("${app.openrouter.api-key:}") String chave,
                            @Value("${app.openrouter.model:}") String modelo) {
        JdkClientHttpRequestFactory fabrica =
            new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(TEMPO_CONEXAO).build());
        fabrica.setReadTimeout(TEMPO_RESPOSTA);
        this.http = RestClient.builder().requestFactory(fabrica).build();
        this.objectMapper = objectMapper;
        this.url = url;
        this.chave = chave.trim();
        this.modelo = modelo.trim();
    }

    /** Sem chave ou sem modelo nao ha o que chamar. */
    public boolean configurado() {
        return !chave.isEmpty() && !modelo.isEmpty();
    }

    public String modelo() {
        return modelo;
    }

    /** O modelo que respondeu (o OpenRouter pode rotear) e o texto da resposta. */
    public record Resposta(String modelo, String conteudo) {}

    /**
     * Envia o prompt de sistema e a entrada do usuario. Lanca excecao em erro de rede, status diferente de 2xx
     * ou resposta sem texto. A mensagem da excecao pode trazer trecho da resposta: nao a registre.
     */
    public Resposta avaliar(String prompt, String entrada) {
        String corpo = objectMapper.writeValueAsString(Map.of(
            "model", modelo,
            "messages", List.of(
                Map.of("role", "system", "content", prompt),
                Map.of("role", "user", "content", entrada)),
            "response_format", RESPONSE_FORMAT,
            "reasoning", REASONING));

        String resposta = http.post()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + chave)
            .contentType(MediaType.APPLICATION_JSON)
            .body(corpo)
            .retrieve()
            .body(String.class);

        JsonNode raiz = objectMapper.readTree(resposta);
        JsonNode conteudo = raiz.path("choices").path(0).path("message").path("content");
        if (!conteudo.isString() || conteudo.asString().isBlank()) {
            throw new IllegalStateException("Resposta do OpenRouter sem texto.");
        }
        JsonNode modeloUsado = raiz.path("model");
        return new Resposta(modeloUsado.isString() ? modeloUsado.asString() : modelo, conteudo.asString());
    }
}
