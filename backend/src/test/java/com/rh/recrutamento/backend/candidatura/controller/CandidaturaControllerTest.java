package com.rh.recrutamento.backend.candidatura.controller;

import com.rh.recrutamento.backend.auth.config.SegurancaConfig;
import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.candidatura.dto.request.CandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.request.StatusCandidaturaRequest;
import com.rh.recrutamento.backend.candidatura.dto.response.CandidaturaResponse;
import com.rh.recrutamento.backend.candidatura.exception.CandidaturaNaoPermitidaException;
import com.rh.recrutamento.backend.candidatura.service.CandidaturaService;
import com.rh.recrutamento.backend.comum.config.CorsConfig;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.rh.recrutamento.backend.Autenticacao.comoAdministrador;
import static com.rh.recrutamento.backend.Autenticacao.comoCandidato;
import static com.rh.recrutamento.backend.Autenticacao.comoRh;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandidaturaController.class)
@Import({SegurancaConfig.class, CorsConfig.class})
class CandidaturaControllerTest {

    private static final UsuarioLogado ANA = new UsuarioLogado(1L, Usuario.Perfil.candidato);
    private static final UsuarioLogado RITA = new UsuarioLogado(7L, Usuario.Perfil.rh);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidaturaService candidaturaService;

    private final CandidaturaResponse inscricao = new CandidaturaResponse(
        20L, 10L, "Backend Java", "aberta", 1L, "Ana", "ana@teste.com", "inscrito", LocalDateTime.now());

    @Test
    void candidatoSeInscreveEmVaga() throws Exception {
        when(candidaturaService.candidatar(new CandidaturaRequest(10L), ANA)).thenReturn(inscricao);

        mockMvc.perform(post("/candidaturas").with(comoCandidato(1))
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":10}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("inscrito"))
            .andExpect(jsonPath("$.vagaTitulo").value("Backend Java"));
    }

    @Test
    void candidaturaRepetidaDeveRetornar409() throws Exception {
        when(candidaturaService.candidatar(new CandidaturaRequest(10L), ANA))
            .thenThrow(new CandidaturaNaoPermitidaException("Você já se candidatou a esta vaga."));

        mockMvc.perform(post("/candidaturas").with(comoCandidato(1))
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":10}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Você já se candidatou a esta vaga."));
    }

    @Test
    void rhNaoSeCandidata() throws Exception {
        mockMvc.perform(post("/candidaturas").with(comoRh(7))
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":10}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(candidaturaService);
    }

    @Test
    void candidatoListaAsProprias() throws Exception {
        when(candidaturaService.listarMinhas(ANA)).thenReturn(List.of(inscricao));

        mockMvc.perform(get("/candidaturas/minhas").with(comoCandidato(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(20));
    }

    @Test
    void rhListaInscritosDaVaga() throws Exception {
        when(candidaturaService.listarPorVaga(10L, RITA)).thenReturn(List.of(inscricao));

        mockMvc.perform(get("/vagas/10/candidaturas").with(comoRh(7)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].candidatoNome").value("Ana"))
            .andExpect(jsonPath("$[0].candidatoEmail").value("ana@teste.com"));
    }

    @Test
    void candidatoNaoVeInscritosDeVaga() throws Exception {
        mockMvc.perform(get("/vagas/10/candidaturas").with(comoCandidato(1)))
            .andExpect(status().isForbidden());
        verifyNoInteractions(candidaturaService);
    }

    @Test
    void rhDeOutraVagaRecebe403() throws Exception {
        when(candidaturaService.listarPorVaga(eq(10L), any()))
            .thenThrow(new AcessoNegadoException("Esta vaga está sob responsabilidade de outro RH."));

        mockMvc.perform(get("/vagas/10/candidaturas").with(comoRh(8)))
            .andExpect(status().isForbidden());
    }

    @Test
    void rhMudaEtapaDoCandidato() throws Exception {
        CandidaturaResponse emEntrevista = new CandidaturaResponse(
            20L, 10L, "Backend Java", "aberta", 1L, "Ana", "ana@teste.com", "entrevista", LocalDateTime.now());
        when(candidaturaService.alterarStatus(eq(20L), any(StatusCandidaturaRequest.class), eq(RITA))).thenReturn(emEntrevista);

        mockMvc.perform(put("/candidaturas/20/status").with(comoRh(7))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"entrevista\",\"observacao\":\"Marcada\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("entrevista"));
    }

    @Test
    void mudarEtapaSemStatusOuComStatusInvalidoRetorna400() throws Exception {
        mockMvc.perform(put("/candidaturas/20/status").with(comoAdministrador(9))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.status").exists());
        mockMvc.perform(put("/candidaturas/20/status").with(comoAdministrador(9))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"promovido\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(candidaturaService);
    }

    @Test
    void candidatoNaoMudaEtapa() throws Exception {
        mockMvc.perform(put("/candidaturas/20/status").with(comoCandidato(1))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"aprovado\"}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(candidaturaService);
    }
}
