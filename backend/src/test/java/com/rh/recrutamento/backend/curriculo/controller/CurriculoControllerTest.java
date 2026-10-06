package com.rh.recrutamento.backend.curriculo.controller;

import tools.jackson.databind.ObjectMapper;
import com.rh.recrutamento.backend.auth.config.SegurancaConfig;
import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.comum.config.CorsConfig;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.ArquivoInvalidoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.curriculo.dto.request.CurriculoRequest;
import com.rh.recrutamento.backend.curriculo.dto.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.curriculo.dto.request.ExperienciaRequest;
import com.rh.recrutamento.backend.curriculo.dto.request.FormacaoRequest;
import com.rh.recrutamento.backend.curriculo.dto.response.ArquivoResponse;
import com.rh.recrutamento.backend.curriculo.dto.response.CurriculoResponse;
import com.rh.recrutamento.backend.curriculo.dto.response.ExperienciaResponse;
import com.rh.recrutamento.backend.curriculo.dto.response.FormacaoResponse;
import com.rh.recrutamento.backend.curriculo.entity.Curriculo;
import com.rh.recrutamento.backend.curriculo.exception.CandidatoInvalidoException;
import com.rh.recrutamento.backend.curriculo.exception.CurriculoJaExisteException;
import com.rh.recrutamento.backend.curriculo.service.CurriculoService;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static com.rh.recrutamento.backend.Autenticacao.comoCandidato;
import static com.rh.recrutamento.backend.Autenticacao.comoRh;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CurriculoController.class)
@Import({SegurancaConfig.class, CorsConfig.class})
class CurriculoControllerTest {

    private static final UsuarioLogado ANA = new UsuarioLogado(1L, Usuario.Perfil.candidato);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CurriculoService curriculoService;

    private final CurriculoResponse curriculoAna = new CurriculoResponse(
        5L, 1L, LocalDate.of(1998, 4, 12), 27, Curriculo.Sexo.feminino, "Campinas", "SP",
        "19999990000", "https://linkedin.com/in/ana", "Java, SQL", "AWS Cloud Practitioner", "Resumo",
        List.of(new FormacaoResponse(1L, "ADS", "Fatec", LocalDate.of(2020, 2, 1), LocalDate.of(2023, 12, 15))),
        List.of(new ExperienciaResponse(1L, "Dev Junior", "Acme", LocalDate.of(2023, 1, 10), null, true, "APIs")),
        new ArquivoResponse(1L, "curriculo-ana.pdf", "application/pdf", 2048L, LocalDateTime.now()),
        LocalDateTime.now());

    @Test
    void postDeveCriarCurriculoERetornar201() throws Exception {
        when(curriculoService.criar(any(CurriculoRequest.class), eq(ANA))).thenReturn(curriculoAna);

        mockMvc.perform(post("/curriculos").with(comoCandidato(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestCompleto())))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/curriculos/5"))
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.usuarioId").value(1))
            .andExpect(jsonPath("$.idade").value(27))
            .andExpect(jsonPath("$.formacoes[0].curso").value("ADS"))
            .andExpect(jsonPath("$.experiencias[0].trabalhoAtual").value(true))
            .andExpect(jsonPath("$.experiencias[0].dataDemissao").doesNotExist())
            .andExpect(jsonPath("$.arquivo.nomeOriginal").value("curriculo-ana.pdf"));
    }

    @Test
    void postSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(post("/curriculos").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(curriculoService);
    }

    @Test
    void escritaDeCurriculoPeloRhDeveRetornar403() throws Exception {
        mockMvc.perform(post("/curriculos").with(comoRh(2)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/curriculos/5/arquivo").with(comoRh(2))
                .file(new MockMultipartFile("arquivo", "c.pdf", "application/pdf", "pdf".getBytes())))
            .andExpect(status().isForbidden());
        verifyNoInteractions(curriculoService);
    }

    @Test
    void getCurriculoDeOutroCandidatoDeveRetornar403() throws Exception {
        when(curriculoService.buscarPorUsuario(3L, ANA))
            .thenThrow(new AcessoNegadoException("Você não tem acesso a este currículo."));

        mockMvc.perform(get("/curriculos/usuario/3").with(comoCandidato(1)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensagem").value("Você não tem acesso a este currículo."));
    }

    @Test
    void postComFormacaoIncompletaDeveRetornar400() throws Exception {
        String corpo = """
            {"formacoes":[{"curso":"","instituicao":"Fatec"}]}
            """;

        mockMvc.perform(post("/curriculos").with(comoCandidato(1)).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos['formacoes[0].curso']").exists())
            .andExpect(jsonPath("$.campos['formacoes[0].dataInicio']").exists());
        verifyNoInteractions(curriculoService);
    }

    @Test
    void postComTextoMaiorQueAColunaDeveRetornar400() throws Exception {
        // as colunas de curso, instituicao, cargo e empresa tem 150 caracteres: acima disso o banco recusaria
        String longo = "x".repeat(151);
        String corpo = """
            {"formacoes":[{"curso":"%s","instituicao":"%s","dataInicio":"2020-01-10"}],
             "experiencias":[{"cargo":"%s","empresa":"%s","dataContratacao":"2024-01-10","trabalhoAtual":true}]}
            """.formatted(longo, longo, longo, longo);

        mockMvc.perform(post("/curriculos").with(comoCandidato(1)).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos['formacoes[0].curso']").exists())
            .andExpect(jsonPath("$.campos['formacoes[0].instituicao']").exists())
            .andExpect(jsonPath("$.campos['experiencias[0].cargo']").exists())
            .andExpect(jsonPath("$.campos['experiencias[0].empresa']").exists());
        verifyNoInteractions(curriculoService);
    }

    @Test
    void postComUfInvalidaDeveRetornar400() throws Exception {
        String corpo = """
            {"uf":"SAO"}
            """;

        mockMvc.perform(post("/curriculos").with(comoCandidato(1)).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.uf").exists());
    }

    @Test
    void postComCandidatoInvalidoDeveRetornar403() throws Exception {
        when(curriculoService.criar(any(CurriculoRequest.class), eq(ANA))).thenThrow(new CandidatoInvalidoException(9L));

        mockMvc.perform(post("/curriculos").with(comoCandidato(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestCompleto())))
            .andExpect(status().isForbidden());
    }

    @Test
    void postComCurriculoDuplicadoDeveRetornar409() throws Exception {
        when(curriculoService.criar(any(CurriculoRequest.class), eq(ANA))).thenThrow(new CurriculoJaExisteException(1L));

        mockMvc.perform(post("/curriculos").with(comoCandidato(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestCompleto())))
            .andExpect(status().isConflict());
    }

    @Test
    void getPorIdDeveRetornarCurriculo() throws Exception {
        when(curriculoService.buscarPorId(5L, ANA)).thenReturn(curriculoAna);

        mockMvc.perform(get("/curriculos/5").with(comoCandidato(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cidade").value("Campinas"))
            .andExpect(jsonPath("$.certificacoes").value("AWS Cloud Practitioner"));
    }

    @Test
    void getPorIdInexistenteDeveRetornar404() throws Exception {
        when(curriculoService.buscarPorId(99L, ANA))
            .thenThrow(new RecursoNaoEncontradoException("Curriculo 99 nao encontrado."));

        mockMvc.perform(get("/curriculos/99").with(comoCandidato(1)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Curriculo 99 nao encontrado."));
    }

    @Test
    void getPorUsuarioDeveRetornarCurriculo() throws Exception {
        when(curriculoService.buscarPorUsuario(1L, ANA)).thenReturn(curriculoAna);

        mockMvc.perform(get("/curriculos/usuario/1").with(comoCandidato(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuarioId").value(1));
    }

    @Test
    void getPorUsuarioInexistenteDeveRetornar404() throws Exception {
        when(curriculoService.buscarPorUsuario(77L, ANA))
            .thenThrow(new RecursoNaoEncontradoException("Curriculo do usuario 77 nao encontrado."));

        mockMvc.perform(get("/curriculos/usuario/77").with(comoCandidato(1)))
            .andExpect(status().isNotFound());
    }

    @Test
    void putDeveAtualizarCurriculo() throws Exception {
        when(curriculoService.atualizar(eq(5L), any(CurriculoUpdateRequest.class), eq(ANA))).thenReturn(curriculoAna);

        CurriculoUpdateRequest request = new CurriculoUpdateRequest(
            LocalDate.of(1998, 4, 12), Curriculo.Sexo.feminino, "Campinas", "SP",
            "19999990000", "https://linkedin.com/in/ana", "Java, SQL", "AWS Cloud Practitioner", "Resumo",
            List.of(new FormacaoRequest("ADS", "Fatec", LocalDate.of(2020, 2, 1), null)),
            List.of());

        mockMvc.perform(put("/curriculos/5").with(comoCandidato(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void putEmCurriculoInexistenteDeveRetornar404() throws Exception {
        when(curriculoService.atualizar(eq(99L), any(CurriculoUpdateRequest.class), eq(ANA)))
            .thenThrow(new RecursoNaoEncontradoException("Curriculo 99 nao encontrado."));

        mockMvc.perform(put("/curriculos/99").with(comoCandidato(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CurriculoUpdateRequest(
                    null, null, null, null, null, null, null, null, null, List.of(), List.of()))))
            .andExpect(status().isNotFound());
    }

    @Test
    void postArquivoDeveAnexarPdf() throws Exception {
        when(curriculoService.anexarArquivo(eq(5L), any(), eq(ANA))).thenReturn(curriculoAna);

        mockMvc.perform(multipart("/curriculos/5/arquivo").with(comoCandidato(1))
                .file(new MockMultipartFile("arquivo", "curriculo-ana.pdf", "application/pdf", "pdf".getBytes())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.arquivo.contentType").value("application/pdf"))
            .andExpect(jsonPath("$.arquivo.tamanhoBytes").value(2048));
    }

    @Test
    void postArquivoQueNaoEhPdfDeveRetornar400() throws Exception {
        when(curriculoService.anexarArquivo(eq(5L), any(), eq(ANA)))
            .thenThrow(new ArquivoInvalidoException("Somente arquivos PDF sao aceitos."));

        mockMvc.perform(multipart("/curriculos/5/arquivo").with(comoCandidato(1))
                .file(new MockMultipartFile("arquivo", "foto.png", "image/png", "png".getBytes())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("Somente arquivos PDF sao aceitos."));
    }

    @Test
    void getArquivoDeveBaixarPdfComNomeOriginal() throws Exception {
        when(curriculoService.baixarArquivo(5L, ANA)).thenReturn(new CurriculoService.ArquivoBaixado(
            "curriculo-ana.pdf", "application/pdf", "pdf".getBytes()));

        mockMvc.perform(get("/curriculos/5/arquivo").with(comoCandidato(1)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", containsStringIgnoringCase("curriculo-ana.pdf")))
            .andExpect(content().bytes("pdf".getBytes()));
    }

    @Test
    void getArquivoInexistenteDeveRetornar404() throws Exception {
        when(curriculoService.baixarArquivo(5L, ANA))
            .thenThrow(new RecursoNaoEncontradoException("Curriculo 5 nao possui arquivo anexado."));

        mockMvc.perform(get("/curriculos/5/arquivo").with(comoCandidato(1)))
            .andExpect(status().isNotFound());
    }

    @Test
    void postComDemissaoAntesDaContratacaoDeveRetornar400() throws Exception {
        String corpo = """
            {"experiencias":[{"cargo":"Dev","empresa":"Acme",
             "dataContratacao":"2024-01-10","dataDemissao":"2023-05-30","trabalhoAtual":false}]}
            """;

        mockMvc.perform(post("/curriculos").with(comoCandidato(1)).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos['experiencias[0].periodoValido']").exists());
        verifyNoInteractions(curriculoService);
    }

    private CurriculoRequest requestCompleto() {
        return new CurriculoRequest(
            LocalDate.of(1998, 4, 12), Curriculo.Sexo.feminino, "Campinas", "SP",
            "19999990000", "https://linkedin.com/in/ana", "Java, SQL", "AWS Cloud Practitioner", "Resumo",
            List.of(new FormacaoRequest("ADS", "Fatec", LocalDate.of(2020, 2, 1), LocalDate.of(2023, 12, 15))),
            List.of(new ExperienciaRequest("Dev Junior", "Acme", LocalDate.of(2023, 1, 10), null, true, "APIs")));
    }
}
