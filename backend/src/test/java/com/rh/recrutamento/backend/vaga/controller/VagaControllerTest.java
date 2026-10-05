package com.rh.recrutamento.backend.vaga.controller;

import tools.jackson.databind.ObjectMapper;
import com.rh.recrutamento.backend.auth.config.SegurancaConfig;
import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.comum.config.CorsConfig;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.vaga.dto.request.VagaRequest;
import com.rh.recrutamento.backend.vaga.dto.request.VagaUpdateRequest;
import com.rh.recrutamento.backend.vaga.dto.response.VagaResponse;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import com.rh.recrutamento.backend.vaga.exception.RhInvalidoException;
import com.rh.recrutamento.backend.vaga.service.VagaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.rh.recrutamento.backend.Autenticacao.comoCandidato;
import static com.rh.recrutamento.backend.Autenticacao.comoRh;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VagaController.class)
@Import({SegurancaConfig.class, CorsConfig.class})
class VagaControllerTest {

    private static final UsuarioLogado RH = new UsuarioLogado(1L, Usuario.Perfil.rh);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VagaService vagaService;

    private final VagaResponse vagaDesenvolvedor = new VagaResponse(
        1L, 1L, "Desenvolvedor Backend", "Descrição", "Java", null, null, "Remoto",
        "remoto", "clt", "rascunho", null, LocalDateTime.now(), LocalDateTime.now());

    private String novaVaga() {
        return objectMapper.writeValueAsString(new VagaRequest(
            "Desenvolvedor Backend", "Descrição", "Java", null, null, "Remoto",
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null));
    }

    @Test
    void postDeveCriarVagaComORhDoTokenERetornar201() throws Exception {
        when(vagaService.criar(any(VagaRequest.class), eq(RH))).thenReturn(vagaDesenvolvedor);

        mockMvc.perform(post("/vagas").with(comoRh(1)).contentType(MediaType.APPLICATION_JSON).content(novaVaga()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/vagas/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.titulo").value("Desenvolvedor Backend"));
    }

    @Test
    void postSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(post("/vagas").contentType(MediaType.APPLICATION_JSON).content(novaVaga()))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(vagaService);
    }

    @Test
    void postComoCandidatoDeveRetornar403() throws Exception {
        mockMvc.perform(post("/vagas").with(comoCandidato(5)).contentType(MediaType.APPLICATION_JSON).content(novaVaga()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensagem").exists());
        verifyNoInteractions(vagaService);
    }

    @Test
    void postComCamposInvalidosDeveRetornar400() throws Exception {
        String corpo = """
            {"titulo":"","descricao":"","modalidade":"remoto","tipoContrato":"clt"}
            """;

        mockMvc.perform(post("/vagas").with(comoRh(1)).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.titulo").exists())
            .andExpect(jsonPath("$.campos.descricao").exists());
        verifyNoInteractions(vagaService);
    }

    @Test
    void postComTokenAntigoDeQuemDeixouDeSerRhDeveRetornar403() throws Exception {
        when(vagaService.criar(any(VagaRequest.class), eq(RH))).thenThrow(new RhInvalidoException(1L));

        mockMvc.perform(post("/vagas").with(comoRh(1)).contentType(MediaType.APPLICATION_JSON).content(novaVaga()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensagem").value("Usuário 1 não possui perfil de RH."));
    }

    @Test
    void getDeveListarVagas() throws Exception {
        when(vagaService.listar(new UsuarioLogado(5L, Usuario.Perfil.candidato))).thenReturn(List.of(vagaDesenvolvedor));

        mockMvc.perform(get("/vagas").with(comoCandidato(5)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].titulo").value("Desenvolvedor Backend"));
    }

    @Test
    void getPorIdDeveRetornarVaga() throws Exception {
        when(vagaService.buscarPorId(1L, RH)).thenReturn(vagaDesenvolvedor);

        mockMvc.perform(get("/vagas/1").with(comoRh(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("rascunho"));
    }

    @Test
    void getPorIdInexistenteDeveRetornar404() throws Exception {
        when(vagaService.buscarPorId(99L, RH))
            .thenThrow(new RecursoNaoEncontradoException("Vaga 99 não encontrada."));

        mockMvc.perform(get("/vagas/99").with(comoRh(1)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Vaga 99 não encontrada."));
    }

    @Test
    void putDeveAtualizarVaga() throws Exception {
        VagaResponse encerrada = new VagaResponse(
            1L, 1L, "Desenvolvedor Backend", "Descrição", "Java", null, null, "Remoto",
            "remoto", "clt", "encerrada", null, LocalDateTime.now(), LocalDateTime.now());
        when(vagaService.atualizar(eq(1L), any(VagaUpdateRequest.class), eq(RH))).thenReturn(encerrada);

        mockMvc.perform(put("/vagas/1").with(comoRh(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VagaUpdateRequest(
                    "Desenvolvedor Backend", "Descrição", "Java", null, null, "Remoto",
                    Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.encerrada, null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("encerrada"));
    }

    @Test
    void putEmVagaDeOutroRhDeveRetornar403() throws Exception {
        when(vagaService.atualizar(eq(7L), any(VagaUpdateRequest.class), eq(RH)))
            .thenThrow(new AcessoNegadoException("Esta vaga está sob responsabilidade de outro RH."));

        mockMvc.perform(put("/vagas/7").with(comoRh(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VagaUpdateRequest(
                    "Vaga", "Descrição", null, null, null, null,
                    Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensagem").value("Esta vaga está sob responsabilidade de outro RH."));
    }

    @Test
    void putEmVagaInexistenteDeveRetornar404() throws Exception {
        when(vagaService.atualizar(eq(99L), any(VagaUpdateRequest.class), eq(RH)))
            .thenThrow(new RecursoNaoEncontradoException("Vaga 99 não encontrada."));

        mockMvc.perform(put("/vagas/99").with(comoRh(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VagaUpdateRequest(
                    "Vaga", "Descrição", null, null, null, null,
                    Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null))))
            .andExpect(status().isNotFound());
    }
}
