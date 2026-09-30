package com.rh.recrutamento.backend.usuario.controller;

import tools.jackson.databind.ObjectMapper;
import com.rh.recrutamento.backend.auth.config.SegurancaConfig;
import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.comum.config.CorsConfig;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.usuario.dto.request.UsuarioRequest;
import com.rh.recrutamento.backend.usuario.dto.request.UsuarioUpdateRequest;
import com.rh.recrutamento.backend.usuario.dto.response.UsuarioResponse;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.exception.EmailJaCadastradoException;
import com.rh.recrutamento.backend.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.rh.recrutamento.backend.Autenticacao.comoAdministrador;
import static com.rh.recrutamento.backend.Autenticacao.comoCandidato;
import static com.rh.recrutamento.backend.Autenticacao.comoRh;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@Import({SegurancaConfig.class, CorsConfig.class})
class UsuarioControllerTest {

    private static final UsuarioLogado ADMINISTRADOR = new UsuarioLogado(9L, Usuario.Perfil.administrador);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    private final UsuarioResponse marina =
        new UsuarioResponse(1L, "Marina Souza", "marina@email.com", "candidato", "ativo");

    @Test
    void postSemTokenDeveCriarUsuarioERetornar201() throws Exception {
        // cadastro publico: sem token o service recebe solicitante nulo (so aceita candidato)
        when(usuarioService.criar(any(UsuarioRequest.class), isNull())).thenReturn(marina);

        mockMvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UsuarioRequest(
                    "Marina Souza", "marina@email.com", "senha123", Usuario.Perfil.candidato, null))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/usuarios/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("marina@email.com"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void postComCamposInvalidosDeveRetornar400ComDetalhePorCampo() throws Exception {
        String corpo = """
            {"nome":"","email":"nao-e-email","senha":"123","perfil":"candidato"}
            """;

        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.campos.nome").exists())
            .andExpect(jsonPath("$.campos.email").exists())
            .andExpect(jsonPath("$.campos.senha").exists());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void postComPerfilForaDoDominioDeveRetornar400() throws Exception {
        String corpo = """
            {"nome":"Marina","email":"marina@email.com","senha":"senha123","perfil":"diretor"}
            """;

        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void postComoAdministradorDeveRepassarQuemEstaLogado() throws Exception {
        UsuarioResponse rh = new UsuarioResponse(2L, "Rita", "rita@email.com", "rh", "ativo");
        when(usuarioService.criar(any(UsuarioRequest.class), eq(ADMINISTRADOR))).thenReturn(rh);

        mockMvc.perform(post("/usuarios").with(comoAdministrador(9))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UsuarioRequest(
                    "Rita", "rita@email.com", "senha123", Usuario.Perfil.rh, null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.perfil").value("rh"));
    }

    @Test
    void postComEmailDuplicadoDeveRetornar409() throws Exception {
        when(usuarioService.criar(any(UsuarioRequest.class), any()))
            .thenThrow(new EmailJaCadastradoException("marina@email.com"));

        mockMvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UsuarioRequest(
                    "Marina", "marina@email.com", "senha123", Usuario.Perfil.candidato, null))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Já existe um usuário cadastrado com o e-mail marina@email.com."));
    }

    @Test
    void getDeveListarUsuarios() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(marina));

        mockMvc.perform(get("/usuarios").with(comoAdministrador(9)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].perfil").value("candidato"));
    }

    @Test
    void getListaSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/usuarios"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").exists());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void configuracoesDevemSerNegadasAoRhQueNaoEAdministrador() throws Exception {
        mockMvc.perform(get("/usuarios").with(comoRh(2)))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/usuarios/3").with(comoRh(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UsuarioUpdateRequest(
                    "Rita", "rita@email.com", Usuario.Perfil.administrador, Usuario.Status.ativo, null))))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/usuarios/3").with(comoCandidato(1)))
            .andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void getPorIdDeveRetornarUsuario() throws Exception {
        when(usuarioService.buscarPorId(1L, new UsuarioLogado(1L, Usuario.Perfil.candidato))).thenReturn(marina);

        mockMvc.perform(get("/usuarios/1").with(comoCandidato(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Marina Souza"));
    }

    @Test
    void getPorIdInexistenteDeveRetornar404() throws Exception {
        when(usuarioService.buscarPorId(99L, ADMINISTRADOR))
            .thenThrow(new RecursoNaoEncontradoException("Usuário 99 não encontrado."));

        mockMvc.perform(get("/usuarios/99").with(comoAdministrador(9)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensagem").value("Usuário 99 não encontrado."));
    }

    @Test
    void putDeveAtualizarUsuario() throws Exception {
        UsuarioResponse atualizada = new UsuarioResponse(1L, "Marina Andrade", "marina@email.com", "rh", "ativo");
        when(usuarioService.atualizar(eq(1L), any(UsuarioUpdateRequest.class), eq(ADMINISTRADOR))).thenReturn(atualizada);

        mockMvc.perform(put("/usuarios/1").with(comoAdministrador(9))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UsuarioUpdateRequest(
                    "Marina Andrade", "marina@email.com", Usuario.Perfil.rh, Usuario.Status.ativo, null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Marina Andrade"))
            .andExpect(jsonPath("$.perfil").value("rh"));
    }

    @Test
    void deleteDeveRetornar204() throws Exception {
        mockMvc.perform(delete("/usuarios/1").with(comoAdministrador(9)))
            .andExpect(status().isNoContent());
        verify(usuarioService).excluir(1L, ADMINISTRADOR);
    }

    @Test
    void deleteInexistenteDeveRetornar404() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Usuário 99 não encontrado."))
            .when(usuarioService).excluir(99L, ADMINISTRADOR);

        mockMvc.perform(delete("/usuarios/99").with(comoAdministrador(9)))
            .andExpect(status().isNotFound());
    }
}
