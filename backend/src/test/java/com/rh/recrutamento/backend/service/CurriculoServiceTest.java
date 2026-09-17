package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.dto.curriculo.response.CurriculoResponse;
import com.rh.recrutamento.backend.entity.Candidato;
import com.rh.recrutamento.backend.entity.Curriculo;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.exception.CandidatoInvalidoException;
import com.rh.recrutamento.backend.exception.CurriculoJaExisteException;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.mapper.CurriculoMapperImpl;
import com.rh.recrutamento.backend.repository.CandidatoRepository;
import com.rh.recrutamento.backend.repository.CurriculoRepository;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculoServiceTest {

    @Mock
    private CurriculoRepository curriculoRepository;

    @Mock
    private CandidatoRepository candidatoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private CurriculoService curriculoService;

    @BeforeEach
    void montarService() {
        curriculoService = new CurriculoService(
            curriculoRepository, candidatoRepository, usuarioRepository, new CurriculoMapperImpl());
    }

    @Test
    void criarDeveAutoprovisionarCandidatoQuandoNaoExiste() {
        CurriculoRequest request = new CurriculoRequest(1L, "Formacao", "Experiencias", "Competencias", "Resumo");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(candidatoUsuario(1L)));
        when(candidatoRepository.findById(1L)).thenReturn(Optional.empty());
        when(candidatoRepository.save(any(Candidato.class))).thenAnswer(inv -> inv.getArgument(0));
        when(curriculoRepository.existsByCandidato_UsuarioId(1L)).thenReturn(false);
        when(curriculoRepository.save(any(Curriculo.class))).thenAnswer(inv -> inv.getArgument(0));

        CurriculoResponse resposta = curriculoService.criar(request);

        ArgumentCaptor<Curriculo> captor = ArgumentCaptor.forClass(Curriculo.class);
        verify(curriculoRepository).save(captor.capture());
        assertThat(captor.getValue().getFormacao()).isEqualTo("Formacao");
        assertThat(resposta.usuarioId()).isEqualTo(1L);
        assertThat(resposta.resumo()).isEqualTo("Resumo");
    }

    @Test
    void criarDeveReutilizarCandidatoExistente() {
        CurriculoRequest request = new CurriculoRequest(1L, null, null, null, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(candidatoUsuario(1L)));
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidatoExistente(1L)));
        when(curriculoRepository.existsByCandidato_UsuarioId(1L)).thenReturn(false);
        when(curriculoRepository.save(any(Curriculo.class))).thenAnswer(inv -> inv.getArgument(0));

        curriculoService.criar(request);

        verify(candidatoRepository, never()).save(any());
    }

    @Test
    void criarDeveRecusarQuandoUsuarioNaoEhCandidato() {
        Usuario rh = new Usuario("Carla RH", "carla@email.com", "hash", Usuario.Perfil.rh, Usuario.Status.ativo);
        ReflectionTestUtils.setField(rh, "id", 2L);
        CurriculoRequest request = new CurriculoRequest(2L, null, null, null, null);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(rh));

        assertThatThrownBy(() -> curriculoService.criar(request))
            .isInstanceOf(CandidatoInvalidoException.class);
        verify(curriculoRepository, never()).save(any());
    }

    @Test
    void criarDeveRecusarQuandoUsuarioNaoExiste() {
        CurriculoRequest request = new CurriculoRequest(99L, null, null, null, null);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculoService.criar(request))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(curriculoRepository, never()).save(any());
    }

    @Test
    void criarDeveRecusarQuandoCandidatoJaPossuiCurriculo() {
        CurriculoRequest request = new CurriculoRequest(1L, null, null, null, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(candidatoUsuario(1L)));
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidatoExistente(1L)));
        when(curriculoRepository.existsByCandidato_UsuarioId(1L)).thenReturn(true);

        assertThatThrownBy(() -> curriculoService.criar(request))
            .isInstanceOf(CurriculoJaExisteException.class);
        verify(curriculoRepository, never()).save(any());
    }

    @Test
    void buscarPorIdDeveRetornarCurriculo() {
        when(curriculoRepository.findById(1L)).thenReturn(Optional.of(curriculoExistente(1L, 1L)));

        assertThat(curriculoService.buscarPorId(1L).id()).isEqualTo(1L);
    }

    @Test
    void buscarPorIdDeveFalharQuandoNaoExiste() {
        when(curriculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculoService.buscarPorId(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("99");
    }

    @Test
    void buscarPorUsuarioDeveRetornarCurriculo() {
        when(curriculoRepository.findByCandidato_UsuarioId(1L)).thenReturn(Optional.of(curriculoExistente(1L, 1L)));

        assertThat(curriculoService.buscarPorUsuario(1L).usuarioId()).isEqualTo(1L);
    }

    @Test
    void buscarPorUsuarioDeveFalharQuandoNaoExiste() {
        when(curriculoRepository.findByCandidato_UsuarioId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculoService.buscarPorUsuario(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizarDeveAlterarDadosDoCurriculo() {
        Curriculo curriculo = curriculoExistente(1L, 1L);
        when(curriculoRepository.findById(1L)).thenReturn(Optional.of(curriculo));
        when(curriculoRepository.save(any(Curriculo.class))).thenAnswer(inv -> inv.getArgument(0));

        CurriculoResponse resposta = curriculoService.atualizar(1L, new CurriculoUpdateRequest(
            "Nova formacao", "Novas experiencias", "Novas competencias", "Novo resumo"));

        assertThat(resposta.formacao()).isEqualTo("Nova formacao");
        assertThat(resposta.resumo()).isEqualTo("Novo resumo");
    }

    @Test
    void atualizarDeveFalharQuandoNaoExiste() {
        when(curriculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curriculoService.atualizar(99L, new CurriculoUpdateRequest(
            "Formacao", null, null, null)))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(curriculoRepository, never()).save(any());
    }

    private Usuario candidatoUsuario(Long id) {
        Usuario usuario = new Usuario("Marina", "marina@email.com", "hash", Usuario.Perfil.candidato, Usuario.Status.ativo);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private Candidato candidatoExistente(Long usuarioId) {
        return new Candidato(candidatoUsuario(usuarioId));
    }

    private Curriculo curriculoExistente(Long id, Long usuarioId) {
        Curriculo curriculo = new Curriculo(candidatoExistente(usuarioId), "Formacao", "Experiencias", "Competencias", "Resumo");
        ReflectionTestUtils.setField(curriculo, "id", id);
        return curriculo;
    }
}
