package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.usuario.request.UsuarioRequest;
import com.rh.recrutamento.backend.dto.usuario.response.UsuarioResponse;
import com.rh.recrutamento.backend.dto.usuario.request.UsuarioUpdateRequest;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.exception.EmailJaCadastradoException;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.mapper.UsuarioMapperImpl;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService usuarioService;

    @BeforeEach
    void montarService() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder, new UsuarioMapperImpl());
    }

    @Test
    void criarDeveCriptografarSenhaENormalizarEmail() {
        UsuarioRequest request = new UsuarioRequest(
            "  Marina Souza  ", "  Marina.Souza@Email.com ", "senha123", Usuario.Perfil.candidato, null);
        when(usuarioRepository.existsByEmail("marina.souza@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resposta = usuarioService.criar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Marina Souza");
        assertThat(salvo.getEmail()).isEqualTo("marina.souza@email.com");
        assertThat(salvo.getSenhaHash()).isEqualTo("hash-bcrypt");
        assertThat(salvo.getStatus()).isEqualTo(Usuario.Status.ativo);
        assertThat(resposta.email()).isEqualTo("marina.souza@email.com");
        assertThat(resposta.perfil()).isEqualTo("candidato");
    }

    @Test
    void criarDeveRespeitarStatusInformado() {
        UsuarioRequest request = new UsuarioRequest(
            "Camila", "camila@email.com", "senha123", Usuario.Perfil.rh, Usuario.Status.inativo);
        when(usuarioRepository.existsByEmail("camila@email.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(usuarioService.criar(request).status()).isEqualTo("inativo");
    }

    @Test
    void criarDeveRecusarEmailJaCadastrado() {
        UsuarioRequest request = new UsuarioRequest(
            "Marina", "marina@email.com", "senha123", Usuario.Perfil.candidato, null);
        when(usuarioRepository.existsByEmail("marina@email.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(request))
            .isInstanceOf(EmailJaCadastradoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void listarDeveConverterTodosOsUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of(
            usuarioExistente(1L, "marina@email.com"),
            usuarioExistente(2L, "camila@email.com")));

        assertThat(usuarioService.listar())
            .extracting(UsuarioResponse::email)
            .containsExactly("marina@email.com", "camila@email.com");
    }

    @Test
    void buscarPorIdDeveRetornarUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente(1L, "marina@email.com")));

        assertThat(usuarioService.buscarPorId(1L).id()).isEqualTo(1L);
    }

    @Test
    void buscarPorIdDeveFalharQuandoNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("99");
    }

    @Test
    void atualizarSemSenhaDeveManterHashAtual() {
        Usuario usuario = usuarioExistente(1L, "marina@email.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdNot("nova@email.com", 1L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resposta = usuarioService.atualizar(1L, new UsuarioUpdateRequest(
            "Marina Andrade", "nova@email.com", Usuario.Perfil.rh, Usuario.Status.bloqueado, null));

        assertThat(resposta.nome()).isEqualTo("Marina Andrade");
        assertThat(resposta.perfil()).isEqualTo("rh");
        assertThat(resposta.status()).isEqualTo("bloqueado");
        assertThat(usuario.getSenhaHash()).isEqualTo("hash-antigo");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void atualizarComSenhaDeveCriptografarNovaSenha() {
        Usuario usuario = usuarioExistente(1L, "marina@email.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdNot("marina@email.com", 1L)).thenReturn(false);
        when(passwordEncoder.encode("novaSenha")).thenReturn("hash-novo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.atualizar(1L, new UsuarioUpdateRequest(
            "Marina", "marina@email.com", Usuario.Perfil.candidato, Usuario.Status.ativo, "novaSenha"));

        assertThat(usuario.getSenhaHash()).isEqualTo("hash-novo");
    }

    @Test
    void atualizarDeveRecusarEmailDeOutroUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente(1L, "marina@email.com")));
        when(usuarioRepository.existsByEmailAndIdNot("camila@email.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.atualizar(1L, new UsuarioUpdateRequest(
            "Marina", "camila@email.com", Usuario.Perfil.candidato, Usuario.Status.ativo, null)))
            .isInstanceOf(EmailJaCadastradoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void excluirDeveRemoverUsuarioExistente() {
        Usuario usuario = usuarioExistente(1L, "marina@email.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        usuarioService.excluir(1L);

        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void excluirDeveFalharQuandoNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.excluir(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(usuarioRepository, never()).delete(any());
    }

    private Usuario usuarioExistente(Long id, String email) {
        Usuario usuario = new Usuario(
            "Marina", email, "hash-antigo", Usuario.Perfil.candidato, Usuario.Status.ativo);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
