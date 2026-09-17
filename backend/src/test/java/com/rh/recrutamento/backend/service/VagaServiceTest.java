package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.vaga.request.VagaRequest;
import com.rh.recrutamento.backend.dto.vaga.request.VagaUpdateRequest;
import com.rh.recrutamento.backend.dto.vaga.response.VagaResponse;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.entity.Vaga;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.exception.RhInvalidoException;
import com.rh.recrutamento.backend.mapper.VagaMapperImpl;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import com.rh.recrutamento.backend.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VagaServiceTest {

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private VagaService vagaService;

    @BeforeEach
    void montarService() {
        vagaService = new VagaService(vagaRepository, usuarioRepository, new VagaMapperImpl());
    }

    @Test
    void criarDeveUsarRascunhoQuandoStatusNaoInformado() {
        VagaRequest request = new VagaRequest(
            1L, "Desenvolvedor Backend", "Descrição da vaga", "Java, Spring", "Remoto",
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(rh(1L)));
        when(vagaRepository.save(any(Vaga.class))).thenAnswer(inv -> inv.getArgument(0));

        VagaResponse resposta = vagaService.criar(request);

        ArgumentCaptor<Vaga> captor = ArgumentCaptor.forClass(Vaga.class);
        verify(vagaRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Vaga.Status.rascunho);
        assertThat(resposta.rhId()).isEqualTo(1L);
        assertThat(resposta.titulo()).isEqualTo("Desenvolvedor Backend");
        assertThat(resposta.status()).isEqualTo("rascunho");
    }

    @Test
    void criarDeveRespeitarStatusInformado() {
        VagaRequest request = new VagaRequest(
            1L, "Desenvolvedor Backend", "Descrição", null, null,
            Vaga.Modalidade.hibrido, Vaga.TipoContrato.pj, Vaga.Status.aberta, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(rh(1L)));
        when(vagaRepository.save(any(Vaga.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(vagaService.criar(request).status()).isEqualTo("aberta");
    }

    @Test
    void criarDeveRecusarQuandoUsuarioNaoEhRh() {
        Usuario candidato = new Usuario("Marina", "marina@email.com", "hash", Usuario.Perfil.candidato, Usuario.Status.ativo);
        ReflectionTestUtils.setField(candidato, "id", 2L);
        VagaRequest request = new VagaRequest(
            2L, "Vaga", "Descrição", null, null, Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(candidato));

        assertThatThrownBy(() -> vagaService.criar(request))
            .isInstanceOf(RhInvalidoException.class);
        verify(vagaRepository, never()).save(any());
    }

    @Test
    void criarDeveRecusarQuandoRhNaoExiste() {
        VagaRequest request = new VagaRequest(
            99L, "Vaga", "Descrição", null, null, Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vagaService.criar(request))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(vagaRepository, never()).save(any());
    }

    @Test
    void listarDeveConverterTodasAsVagas() {
        when(vagaRepository.findAll()).thenReturn(List.of(
            vagaExistente(1L, "Vaga A"), vagaExistente(2L, "Vaga B")));

        assertThat(vagaService.listar())
            .extracting(VagaResponse::titulo)
            .containsExactly("Vaga A", "Vaga B");
    }

    @Test
    void buscarPorIdDeveRetornarVaga() {
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vagaExistente(1L, "Vaga A")));

        assertThat(vagaService.buscarPorId(1L).id()).isEqualTo(1L);
    }

    @Test
    void buscarPorIdDeveFalharQuandoNaoExiste() {
        when(vagaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vagaService.buscarPorId(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("99");
    }

    @Test
    void atualizarDeveAlterarDadosDaVaga() {
        Vaga vaga = vagaExistente(1L, "Vaga A");
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
        when(vagaRepository.save(any(Vaga.class))).thenAnswer(inv -> inv.getArgument(0));

        VagaResponse resposta = vagaService.atualizar(1L, new VagaUpdateRequest(
            "Vaga Atualizada", "Nova descrição", "Novo requisito", "São Paulo",
            Vaga.Modalidade.presencial, Vaga.TipoContrato.estagio, Vaga.Status.aberta, null));

        assertThat(resposta.titulo()).isEqualTo("Vaga Atualizada");
        assertThat(resposta.modalidade()).isEqualTo("presencial");
        assertThat(resposta.status()).isEqualTo("aberta");
    }

    @Test
    void atualizarDevePermitirEncerrarVaga() {
        Vaga vaga = vagaExistente(1L, "Vaga A");
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
        when(vagaRepository.save(any(Vaga.class))).thenAnswer(inv -> inv.getArgument(0));

        VagaResponse resposta = vagaService.atualizar(1L, new VagaUpdateRequest(
            "Vaga A", "Descrição", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.encerrada, null));

        assertThat(resposta.status()).isEqualTo("encerrada");
    }

    @Test
    void atualizarDeveFalharQuandoNaoExiste() {
        when(vagaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vagaService.atualizar(99L, new VagaUpdateRequest(
            "Vaga", "Descrição", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null)))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(vagaRepository, never()).save(any());
    }

    private Usuario rh(Long id) {
        Usuario usuario = new Usuario("Carla RH", "carla@email.com", "hash", Usuario.Perfil.rh, Usuario.Status.ativo);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private Vaga vagaExistente(Long id, String titulo) {
        Vaga vaga = new Vaga(rh(1L), titulo, "Descrição", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.rascunho, null);
        ReflectionTestUtils.setField(vaga, "id", id);
        return vaga;
    }
}
