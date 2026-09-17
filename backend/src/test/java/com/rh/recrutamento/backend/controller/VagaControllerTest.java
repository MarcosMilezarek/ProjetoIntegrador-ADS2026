package com.rh.recrutamento.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.rh.recrutamento.backend.dto.vaga.request.VagaRequest;
import com.rh.recrutamento.backend.dto.vaga.request.VagaUpdateRequest;
import com.rh.recrutamento.backend.dto.vaga.response.VagaResponse;
import com.rh.recrutamento.backend.entity.Vaga;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.exception.RhInvalidoException;
import com.rh.recrutamento.backend.service.VagaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VagaController.class)
class VagaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VagaService vagaService;

    private final VagaResponse vagaDesenvolvedor = new VagaResponse(
        1L, 1L, "Desenvolvedor Backend", "Descrição", "Java", "Remoto",
        "remoto", "clt", "rascunho", null, LocalDateTime.now(), LocalDateTime.now());

    @Test
    void postDeveCriarVagaERetornar201() throws Exception {
        when(vagaService.criar(any(VagaRequest.class))).thenReturn(vagaDesenvolvedor);

        mockMvc.perform(post("/vagas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VagaRequest(
                    1L, "Desenvolvedor Backend", "Descrição", "Java", "Remoto",
                    Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/vagas/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.titulo").value("Desenvolvedor Backend"));
    }

    @Test
    void postComCamposInvalidosDeveRetornar400() throws Exception {
        String corpo = """
            {"titulo":"","descricao":"","modalidade":"remoto","tipoContrato":"clt"}
            """;

        mockMvc.perform(post("/vagas").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.rhId").exists())
            .andExpect(jsonPath("$.campos.titulo").exists())
            .andExpect(jsonPath("$.campos.descricao").exists());
        verifyNoInteractions(vagaService);
    }

    @Test
    void postComRhInvalidoDeveRetornar403() throws Exception {
        when(vagaService.criar(any(VagaRequest.class)))
            .thenThrow(new RhInvalidoException(2L));

        mockMvc.perform(post("/vagas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VagaRequest(
                    2L, "Vaga", "Descrição", null, null,
                    Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensagem").value("Usuário 2 não possui perfil de RH."));
    }

    @Test
    void getDeveListarVagas() throws Exception {
        when(vagaService.listar()).thenReturn(List.of(vagaDesenvolvedor));

        mockMvc.perform(get("/vagas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].titulo").value("Desenvolvedor Backend"));
    }

    @Test
    void getPorIdDeveRetornarVaga() throws Exception {
        when(vagaService.buscarPorId(1L)).thenReturn(vagaDesenvolvedor);

        mockMvc.perform(get("/vagas/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("rascunho"));
    }

    @Test
    void getPorIdInexistenteDeveRetornar404() throws Exception {
        when(vagaService.buscarPorId(99L))
            .thenThrow(new RecursoNaoEncontradoException("Vaga 99 não encontrada."));

        mockMvc.perform(get("/vagas/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Vaga 99 não encontrada."));
    }

    @Test
    void putDeveAtualizarVaga() throws Exception {
        VagaResponse encerrada = new VagaResponse(
            1L, 1L, "Desenvolvedor Backend", "Descrição", "Java", "Remoto",
            "remoto", "clt", "encerrada", null, LocalDateTime.now(), LocalDateTime.now());
        when(vagaService.atualizar(eq(1L), any(VagaUpdateRequest.class))).thenReturn(encerrada);

        mockMvc.perform(put("/vagas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VagaUpdateRequest(
                    "Desenvolvedor Backend", "Descrição", "Java", "Remoto",
                    Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.encerrada, null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("encerrada"));
    }

    @Test
    void putEmVagaInexistenteDeveRetornar404() throws Exception {
        when(vagaService.atualizar(eq(99L), any(VagaUpdateRequest.class)))
            .thenThrow(new RecursoNaoEncontradoException("Vaga 99 não encontrada."));

        mockMvc.perform(put("/vagas/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VagaUpdateRequest(
                    "Vaga", "Descrição", null, null,
                    Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null))))
            .andExpect(status().isNotFound());
    }
}
