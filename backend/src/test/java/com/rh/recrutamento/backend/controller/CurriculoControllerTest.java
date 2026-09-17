package com.rh.recrutamento.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.dto.curriculo.response.CurriculoResponse;
import com.rh.recrutamento.backend.exception.CandidatoInvalidoException;
import com.rh.recrutamento.backend.exception.CurriculoJaExisteException;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.service.CurriculoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CurriculoController.class)
class CurriculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CurriculoService curriculoService;

    private final CurriculoResponse curriculoMarina = new CurriculoResponse(
        1L, 1L, "Formacao", "Experiencias", "Competencias", "Resumo", LocalDateTime.now());

    @Test
    void postDeveCriarCurriculoERetornar201() throws Exception {
        when(curriculoService.criar(any(CurriculoRequest.class))).thenReturn(curriculoMarina);

        mockMvc.perform(post("/curriculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CurriculoRequest(
                    1L, "Formacao", "Experiencias", "Competencias", "Resumo"))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/curriculos/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.usuarioId").value(1));
    }

    @Test
    void postComUsuarioIdAusenteDeveRetornar400() throws Exception {
        String corpo = """
            {"formacao":"Formacao"}
            """;

        mockMvc.perform(post("/curriculos").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.usuarioId").exists());
        verifyNoInteractions(curriculoService);
    }

    @Test
    void postComCandidatoInvalidoDeveRetornar403() throws Exception {
        when(curriculoService.criar(any(CurriculoRequest.class)))
            .thenThrow(new CandidatoInvalidoException(2L));

        mockMvc.perform(post("/curriculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CurriculoRequest(2L, null, null, null, null))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensagem").value("Usuario 2 nao possui perfil de candidato."));
    }

    @Test
    void postComCurriculoDuplicadoDeveRetornar409() throws Exception {
        when(curriculoService.criar(any(CurriculoRequest.class)))
            .thenThrow(new CurriculoJaExisteException(1L));

        mockMvc.perform(post("/curriculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CurriculoRequest(1L, null, null, null, null))))
            .andExpect(status().isConflict());
    }

    @Test
    void getPorIdDeveRetornarCurriculo() throws Exception {
        when(curriculoService.buscarPorId(1L)).thenReturn(curriculoMarina);

        mockMvc.perform(get("/curriculos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumo").value("Resumo"));
    }

    @Test
    void getPorIdInexistenteDeveRetornar404() throws Exception {
        when(curriculoService.buscarPorId(99L))
            .thenThrow(new RecursoNaoEncontradoException("Curriculo 99 nao encontrado."));

        mockMvc.perform(get("/curriculos/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Curriculo 99 nao encontrado."));
    }

    @Test
    void getPorUsuarioDeveRetornarCurriculo() throws Exception {
        when(curriculoService.buscarPorUsuario(1L)).thenReturn(curriculoMarina);

        mockMvc.perform(get("/curriculos/usuario/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuarioId").value(1));
    }

    @Test
    void getPorUsuarioInexistenteDeveRetornar404() throws Exception {
        when(curriculoService.buscarPorUsuario(99L))
            .thenThrow(new RecursoNaoEncontradoException("Curriculo do usuario 99 nao encontrado."));

        mockMvc.perform(get("/curriculos/usuario/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void putDeveAtualizarCurriculo() throws Exception {
        CurriculoResponse atualizado = new CurriculoResponse(
            1L, 1L, "Nova formacao", "Experiencias", "Competencias", "Resumo", LocalDateTime.now());
        when(curriculoService.atualizar(eq(1L), any(CurriculoUpdateRequest.class))).thenReturn(atualizado);

        mockMvc.perform(put("/curriculos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CurriculoUpdateRequest(
                    "Nova formacao", "Experiencias", "Competencias", "Resumo"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formacao").value("Nova formacao"));
    }

    @Test
    void putEmCurriculoInexistenteDeveRetornar404() throws Exception {
        when(curriculoService.atualizar(eq(99L), any(CurriculoUpdateRequest.class)))
            .thenThrow(new RecursoNaoEncontradoException("Curriculo 99 nao encontrado."));

        mockMvc.perform(put("/curriculos/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CurriculoUpdateRequest(
                    "Formacao", null, null, null))))
            .andExpect(status().isNotFound());
    }
}
