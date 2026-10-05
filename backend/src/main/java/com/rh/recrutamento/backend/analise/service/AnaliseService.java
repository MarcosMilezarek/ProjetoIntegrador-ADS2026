package com.rh.recrutamento.backend.analise.service;

import com.rh.recrutamento.backend.analise.client.OpenRouterClient;
import com.rh.recrutamento.backend.analise.dto.response.AnaliseResponse;
import com.rh.recrutamento.backend.analise.entity.AnaliseCandidatura;
import com.rh.recrutamento.backend.analise.repository.AnaliseCandidaturaRepository;
import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import com.rh.recrutamento.backend.candidatura.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.curriculo.entity.Curriculo;
import com.rh.recrutamento.backend.curriculo.repository.CurriculoRepository;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Triagem por IA da candidatura (RN04: a IA so recomenda, quem decide e o RH). A analise nasce PENDENTE na
 * mesma transacao da candidatura e e processada depois do commit, em outra thread: a candidatura nunca espera
 * nem falha por causa da IA. Qualquer problema vira FALHA; saida invalida nunca e gravada como analise.
 */
@Service
public class AnaliseService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseService.class);

    /** PONTO UNICO DE SUBSTITUICAO do prompt de avaliacao. [PENDENTE: texto final do prompt] */
    static final String PROMPT_AVALIACAO = "[PENDENTE: prompt de avaliacao]";

    private final AnaliseCandidaturaRepository analiseRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final CurriculoRepository curriculoRepository;
    private final OpenRouterClient openRouter;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher publisher;
    private final TransactionTemplate transacao;

    public AnaliseService(AnaliseCandidaturaRepository analiseRepository, CandidaturaRepository candidaturaRepository,
                          CurriculoRepository curriculoRepository, OpenRouterClient openRouter,
                          ObjectMapper objectMapper, ApplicationEventPublisher publisher,
                          PlatformTransactionManager gerenteTransacao) {
        this.analiseRepository = analiseRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.curriculoRepository = curriculoRepository;
        this.openRouter = openRouter;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
        this.transacao = new TransactionTemplate(gerenteTransacao);
    }

    /** Pedido de analise, publicado dentro da transacao da candidatura e tratado so depois do commit. */
    public record Solicitacao(Long candidaturaId) {}

    /** Chamado na transacao da candidatura: registra a analise PENDENTE e agenda o processamento. */
    @Transactional
    public void solicitar(Candidatura candidatura) {
        analiseRepository.save(new AnaliseCandidatura(candidatura));
        publisher.publishEvent(new Solicitacao(candidatura.getId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoSolicitar(Solicitacao solicitacao) {
        processar(solicitacao.candidaturaId());
    }

    /** Analises das candidaturas dadas, por id da candidatura. Quem nao tem analise (anteriores a V7) fica de fora. */
    @Transactional(readOnly = true)
    public Map<Long, AnaliseResponse> porCandidatura(Collection<Long> candidaturaIds) {
        if (candidaturaIds.isEmpty()) {
            return Map.of();
        }
        return analiseRepository.findByCandidatura_IdIn(candidaturaIds).stream()
            .collect(Collectors.toMap(a -> a.getCandidatura().getId(), this::paraResponse));
    }

    private void processar(Long candidaturaId) {
        long inicio = System.currentTimeMillis();
        String modelo = openRouter.modelo();
        AnaliseCandidatura.Status resultado = null;
        try {
            // nada de transacao aberta durante a chamada HTTP: a entrada e montada antes, o resultado gravado depois
            String entrada = transacao.execute(s -> montarEntradaSePendente(candidaturaId));
            if (entrada == null) {
                return; // ja processada: retentativa nao duplica
            }
            if (!openRouter.configurado()) {
                throw new IllegalStateException("OPENROUTER_API_KEY ou OPENROUTER_MODEL nao configurados.");
            }
            OpenRouterClient.Resposta resposta = openRouter.avaliar(PROMPT_AVALIACAO, entrada);
            modelo = resposta.modelo();
            Avaliacao avaliacao = validar(resposta.conteudo());
            String positivos = objectMapper.writeValueAsString(avaliacao.positivos());
            String negativos = objectMapper.writeValueAsString(avaliacao.negativos());
            atualizar(candidaturaId, a -> a.concluir(avaliacao.aderencia(), positivos, negativos, resposta.modelo()));
            resultado = AnaliseCandidatura.Status.CONCLUIDA;
        } catch (Exception e) {
            resultado = AnaliseCandidatura.Status.FALHA;
            // so o tipo da falha (e o status HTTP): a mensagem pode trazer trecho da resposta ou dos dados
            log.warn("Analise da candidatura {} falhou: {}{}", candidaturaId, e.getClass().getSimpleName(),
                e instanceof RestClientResponseException http ? " (HTTP " + http.getStatusCode().value() + ")" : "");
            final String modeloFalha = modelo;
            try {
                atualizar(candidaturaId, a -> a.falhar(modeloFalha));
            } catch (Exception ignorada) {
                log.error("Nao foi possivel registrar a falha da analise da candidatura {}.", candidaturaId);
            }
        } finally {
            if (resultado != null) {
                log.info("Analise da candidatura {}: {} em {} ms (modelo {})", candidaturaId, resultado,
                    System.currentTimeMillis() - inicio, modelo);
            }
        }
    }

    /** So a analise ainda PENDENTE segue adiante. */
    private String montarEntradaSePendente(Long candidaturaId) {
        AnaliseCandidatura analise = analiseRepository.findByCandidatura_Id(candidaturaId).orElse(null);
        if (analise == null || analise.getStatus() != AnaliseCandidatura.Status.PENDENTE) {
            return null;
        }
        Candidatura candidatura = candidaturaRepository.findById(candidaturaId).orElseThrow();
        Curriculo curriculo = curriculoRepository.findByUsuario_Id(candidatura.getCandidato().getId()).orElseThrow();
        return montarEntrada(candidatura.getVaga(), curriculo);
    }

    /**
     * So o que serve a avaliacao: nada de nome, e-mail, telefone, LinkedIn, nascimento, sexo, cidade ou UF.
     * O conteudo e dado do candidato, nao instrucao: vai como JSON dentro de uma tag fixa, e o "<" dos textos
     * e escapado para que nenhum texto consiga fechar a tag.
     */
    private String montarEntrada(Vaga vaga, Curriculo curriculo) {
        Map<String, Object> dadosVaga = new LinkedHashMap<>();
        dadosVaga.put("titulo", vaga.getTitulo());
        dadosVaga.put("descricao", vaga.getDescricao());
        dadosVaga.put("requisitos", vaga.getRequisitos());
        dadosVaga.put("modalidade", vaga.getModalidade().name());
        dadosVaga.put("tipoContrato", vaga.getTipoContrato().name());

        Map<String, Object> dadosCandidato = new LinkedHashMap<>();
        dadosCandidato.put("resumo", curriculo.getResumo());
        dadosCandidato.put("competencias", curriculo.getCompetencias());
        dadosCandidato.put("certificacoes", curriculo.getCertificacoes());
        dadosCandidato.put("formacoes", curriculo.getFormacoes().stream().map(f -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("curso", f.getCurso());
            item.put("instituicao", f.getInstituicao());
            item.put("inicio", String.valueOf(f.getDataInicio()));
            item.put("termino", f.getDataTermino() == null ? null : f.getDataTermino().toString());
            return item;
        }).toList());
        dadosCandidato.put("experiencias", curriculo.getExperiencias().stream().map(e -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cargo", e.getCargo());
            item.put("empresa", e.getEmpresa());
            item.put("inicio", String.valueOf(e.getDataContratacao()));
            item.put("termino", e.getDataDemissao() == null ? null : e.getDataDemissao().toString());
            item.put("trabalhoAtual", e.isTrabalhoAtual());
            item.put("atividades", e.getDescricaoAtividades());
            return item;
        }).toList());

        String json = objectMapper.writeValueAsString(Map.of("vaga", dadosVaga, "candidato", dadosCandidato));
        return "<dados>" + json.replace("<", "\\u003c") + "</dados>";
    }

    private record Avaliacao(int aderencia, List<String> positivos, List<String> negativos) {}

    /**
     * A saida precisa ser um objeto com a aderencia (numero de 0 a 100, arredondado para inteiro) e duas listas
     * de textos nao vazios, com ao menos um ponto.
     */
    private Avaliacao validar(String conteudo) {
        JsonNode raiz = objectMapper.readTree(conteudo);
        JsonNode nota = raiz.path("aderencia");
        if (!nota.isNumber() || nota.asDouble() < 0 || nota.asDouble() > 100) {
            throw new IllegalStateException("Campo aderencia ausente ou fora de 0 a 100.");
        }
        List<String> positivos = lista(raiz, "pontosPositivos");
        List<String> negativos = lista(raiz, "pontosNegativos");
        if (positivos.isEmpty() && negativos.isEmpty()) {
            throw new IllegalStateException("Analise sem nenhum ponto.");
        }
        return new Avaliacao((int) Math.round(nota.asDouble()), positivos, negativos);
    }

    private List<String> lista(JsonNode raiz, String campo) {
        JsonNode no = raiz.path(campo);
        if (!no.isArray()) {
            throw new IllegalStateException("Campo " + campo + " ausente ou nao e lista.");
        }
        List<String> itens = new ArrayList<>();
        for (JsonNode item : no) {
            if (!item.isString() || item.asString().isBlank()) {
                throw new IllegalStateException("Campo " + campo + " com item invalido.");
            }
            itens.add(item.asString().trim());
        }
        return itens;
    }

    private void atualizar(Long candidaturaId, Consumer<AnaliseCandidatura> mudanca) {
        transacao.executeWithoutResult(s ->
            analiseRepository.findByCandidatura_Id(candidaturaId).ifPresent(mudanca));
    }

    private AnaliseResponse paraResponse(AnaliseCandidatura analise) {
        return new AnaliseResponse(analise.getStatus().name(), analise.getAderencia(), lerLista(analise.getPontosPositivos()),
            lerLista(analise.getPontosNegativos()));
    }

    private List<String> lerLista(String json) {
        return json == null ? List.of() : objectMapper.readValue(json, new TypeReference<List<String>>() { });
    }
}
