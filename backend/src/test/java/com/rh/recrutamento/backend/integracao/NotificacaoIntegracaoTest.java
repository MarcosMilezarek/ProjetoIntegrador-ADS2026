package com.rh.recrutamento.backend.integracao;

import com.jayway.jsonpath.JsonPath;
import com.rh.recrutamento.backend.auth.service.TokenService;
import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import com.rh.recrutamento.backend.candidatura.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.curriculo.entity.Curriculo;
import com.rh.recrutamento.backend.curriculo.repository.CurriculoRepository;
import com.rh.recrutamento.backend.notificacao.entity.Notificacao;
import com.rh.recrutamento.backend.notificacao.repository.NotificacaoRepository;
import com.rh.recrutamento.backend.notificacao.service.NotificacaoService;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.repository.UsuarioRepository;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import com.rh.recrutamento.backend.vaga.repository.VagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Notificacoes em tempo real sem mocks e com JWT real: uma acao de um usuario chega na conexao
 * SSE do outro, fica gravada para depois e nunca chega a um terceiro.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificacaoIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private CandidaturaRepository candidaturaRepository;

    @Autowired
    private CurriculoRepository curriculoRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void acaoDeUmChegaNaHoraNoOutroENuncaNumTerceiro() throws Exception {
        Usuario rita = usuario("rita@notificacao.test", Usuario.Perfil.rh);
        Usuario paulo = usuario("paulo@notificacao.test", Usuario.Perfil.rh);
        Usuario ana = usuario("ana@notificacao.test", Usuario.Perfil.candidato);
        Usuario bruno = usuario("bruno@notificacao.test", Usuario.Perfil.candidato);
        curriculoRepository.save(new Curriculo(ana));
        Vaga vaga = vagaRepository.save(new Vaga(rita, "Backend Java", "Descricao", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null));

        // conexoes abertas antes das acoes
        MvcResult streamRita = conectar(rita);
        MvcResult streamPaulo = conectar(paulo);
        MvcResult streamAna = conectar(ana);
        MvcResult streamBruno = conectar(bruno);

        // candidato -> RH: a candidatura avisa o RH da vaga
        String inscricao = mockMvc.perform(post("/candidaturas").header("Authorization", token(ana))
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga.getId() + "}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Integer candidaturaId = JsonPath.read(inscricao, "$.id");
        assertThat(conteudo(streamRita)).contains("event:notificacao").contains("ana se candidatou à vaga Backend Java.");

        // RH -> candidato: entrevista com data e hora; 17:30 UTC = 14:30 em Brasilia
        mockMvc.perform(put("/candidaturas/" + candidaturaId + "/entrevista").header("Authorization", token(rita))
                .contentType(MediaType.APPLICATION_JSON).content("{\"dataHora\":\"2030-10-15T14:30:00-03:00\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("entrevista"))
            .andExpect(jsonPath("$.entrevistaEm").value("2030-10-15T17:30:00Z"));
        assertThat(conteudo(streamAna)).contains("event:notificacao")
            .contains("sua entrevista foi marcada para 15/10/2030 às 14:30 (horário de Brasília)");

        // ninguem mais recebeu nada
        assertThat(conteudo(streamPaulo)).doesNotContain("event:notificacao");
        assertThat(conteudo(streamBruno)).doesNotContain("event:notificacao");

        // a entrevista e a notificacao persistem (recarregar a pagina)
        mockMvc.perform(get("/candidaturas/minhas").header("Authorization", token(ana)))
            .andExpect(jsonPath("$[0].entrevistaEm").value("2030-10-15T17:30:00Z"));
        String daAna = mockMvc.perform(get("/notificacoes").header("Authorization", token(ana)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].titulo").value("Entrevista agendada"))
            .andExpect(jsonPath("$[0].lida").value(false))
            .andReturn().getResponse().getContentAsString();
        Integer notificacaoDaAna = JsonPath.read(daAna, "$[0].id");
        mockMvc.perform(get("/notificacoes").header("Authorization", token(bruno)))
            .andExpect(jsonPath("$.length()").value(0));

        // so o destinatario marca como lida
        mockMvc.perform(put("/notificacoes/" + notificacaoDaAna + "/lida").header("Authorization", token(bruno)))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/notificacoes/" + notificacaoDaAna + "/lida").header("Authorization", token(ana)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lida").value(true));
        mockMvc.perform(put("/notificacoes/lidas").header("Authorization", token(rita)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/notificacoes").header("Authorization", token(rita)))
            .andExpect(jsonPath("$[0].lida").value(true));

        // sem token nao conecta
        mockMvc.perform(get("/notificacoes/stream")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/notificacoes")).andExpect(status().isUnauthorized());
    }

    /**
     * O backend local e o do site usam o mesmo MySQL, mas cada um guarda as conexoes SSE na propria memoria.
     * Uma notificacao gravada por outra instancia precisa chegar tambem a quem esta conectado aqui.
     */
    @Test
    void notificacaoGravadaPorOutraInstanciaTambemChegaNaConexao() throws Exception {
        Usuario ana = usuario("ana@outra-instancia.test", Usuario.Perfil.candidato);
        Usuario bruno = usuario("bruno@outra-instancia.test", Usuario.Perfil.candidato);
        MvcResult streamAna = conectar(ana);
        MvcResult streamBruno = conectar(bruno);

        // outra instancia grava direto no banco: nada passa pelo NotificacaoService desta
        notificacaoRepository.save(new Notificacao(ana, Notificacao.Tipo.candidatura, 77L,
            "Sua candidatura mudou de etapa", "Gravada por outra instancia."));

        aguardar(() -> conteudo(streamAna).contains("Gravada por outra instancia."));
        assertThat(conteudo(streamAna)).contains("event:notificacao");
        assertThat(conteudo(streamBruno)).doesNotContain("Gravada por outra instancia.");
    }

    /** O envio direto e o sincronizador convivem: o que esta instancia criou nao pode chegar duas vezes. */
    @Test
    void notificacaoCriadaAquiNaoChegaEmDobroDepoisDoSincronizador() throws Exception {
        Usuario rita = usuario("rita@dobro.test", Usuario.Perfil.rh);
        Usuario ana = usuario("ana@dobro.test", Usuario.Perfil.candidato);
        curriculoRepository.save(new Curriculo(ana));
        Vaga vaga = vagaRepository.save(new Vaga(rita, "Financeiro", "Descricao", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null));
        MvcResult streamRita = conectar(rita);

        mockMvc.perform(post("/candidaturas").header("Authorization", token(ana))
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga.getId() + "}"))
            .andExpect(status().isCreated());
        notificacaoService.sincronizar();
        notificacaoService.sincronizar();

        assertThat(conteudo(streamRita).split("event:notificacao", -1)).hasSize(2);
    }

    @Test
    void entrevistaSoPeloRhDaVagaENoFuturo() throws Exception {
        Usuario rita = usuario("rita@entrevista.test", Usuario.Perfil.rh);
        Usuario paulo = usuario("paulo@entrevista.test", Usuario.Perfil.rh);
        Usuario ana = usuario("ana@entrevista.test", Usuario.Perfil.candidato);
        Vaga vaga = vagaRepository.save(new Vaga(rita, "Dados", "Descricao", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null));
        Candidatura candidatura = candidaturaRepository.save(new Candidatura(ana, vaga));
        String rota = "/candidaturas/" + candidatura.getId() + "/entrevista";
        String futuro = "{\"dataHora\":\"2030-10-15T14:30:00-03:00\"}";

        mockMvc.perform(put(rota).header("Authorization", token(ana))
                .contentType(MediaType.APPLICATION_JSON).content(futuro))
            .andExpect(status().isForbidden());
        mockMvc.perform(put(rota).header("Authorization", token(paulo))
                .contentType(MediaType.APPLICATION_JSON).content(futuro))
            .andExpect(status().isForbidden());
        mockMvc.perform(put(rota).header("Authorization", token(rita))
                .contentType(MediaType.APPLICATION_JSON).content("{\"dataHora\":\"2020-10-15T14:30:00-03:00\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/notificacoes").header("Authorization", token(ana)))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sessaoERestauradaPeloTokenGuardado() throws Exception {
        Usuario ana = usuario("ana@sessao.test", Usuario.Perfil.candidato);
        Usuario bloqueado = usuarioRepository.save(new Usuario("bloqueado", "bloqueado@sessao.test", "hash",
            Usuario.Perfil.rh, Usuario.Status.bloqueado));

        mockMvc.perform(get("/auth/me").header("Authorization", token(ana)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ana.getId()))
            .andExpect(jsonPath("$.perfil").value("candidato"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist());
        mockMvc.perform(get("/auth/me").header("Authorization", token(bloqueado)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/auth/me").header("Authorization", token(ana) + "x"))
            .andExpect(status().isUnauthorized());
        // token vencido: o frontend limpa a sessao e volta ao login
        JwtClaimsSet vencido = JwtClaimsSet.builder().subject(ana.getId().toString()).claim("perfil", "candidato")
            .issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(60)).build();
        String tokenVencido = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), vencido))
            .getTokenValue();
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tokenVencido))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/notificacoes/stream").header("Authorization", "Bearer " + tokenVencido))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    private MvcResult conectar(Usuario usuario) throws Exception {
        return mockMvc.perform(get("/notificacoes/stream").header("Authorization", token(usuario)))
            .andExpect(request().asyncStarted())
            .andExpect(header().string("X-Accel-Buffering", "no"))
            .andReturn();
    }

    private static String conteudo(MvcResult stream) throws Exception {
        return stream.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Espera (ate 10s) a condicao ficar verdadeira; a entrega entre instancias e assincrona. */
    private static void aguardar(Condicao condicao) throws Exception {
        long limite = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (!condicao.verdadeira() && System.nanoTime() < limite) {
            Thread.sleep(100);
        }
    }

    @FunctionalInterface
    private interface Condicao {
        boolean verdadeira() throws Exception;
    }

    private Usuario usuario(String email, Usuario.Perfil perfil) {
        return usuarioRepository.save(new Usuario(email.substring(0, email.indexOf('@')), email, "hash", perfil,
            Usuario.Status.ativo));
    }

    private String token(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario);
    }
}
