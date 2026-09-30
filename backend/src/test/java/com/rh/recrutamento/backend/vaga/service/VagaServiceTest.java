package com.rh.recrutamento.backend.vaga.service;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.repository.UsuarioRepository;
import com.rh.recrutamento.backend.vaga.dto.request.VagaRequest;
import com.rh.recrutamento.backend.vaga.dto.request.VagaUpdateRequest;
import com.rh.recrutamento.backend.vaga.dto.response.VagaResponse;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import com.rh.recrutamento.backend.vaga.exception.RhInvalidoException;
import com.rh.recrutamento.backend.vaga.repository.VagaRepository;
import com.rh.recrutamento.backend.vaga.mapper.VagaMapperImpl;
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

    private static final UsuarioLogado RH_1 = new UsuarioLogado(1L, Usuario.Perfil.rh);
    private static final UsuarioLogado RH_2 = new UsuarioLogado(2L, Usuario.Perfil.rh);
    private static final UsuarioLogado ADMINISTRADOR = new UsuarioLogado(9L, Usuario.Perfil.administrador);
    private static final UsuarioLogado CANDIDATO = new UsuarioLogado(5L, Usuario.Perfil.candidato);

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
            "Desenvolvedor Backend", "Descrição da vaga", "Java, Spring", "Remoto",
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(rh(1L)));
        when(vagaRepository.save(any(Vaga.class))).thenAnswer(inv -> inv.getArgument(0));

        VagaResponse resposta = vagaService.criar(request, RH_1);

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
            "Desenvolvedor Backend", "Descrição", null, null,
            Vaga.Modalidade.hibrido, Vaga.TipoContrato.pj, Vaga.Status.aberta, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(rh(1L)));
        when(vagaRepository.save(any(Vaga.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(vagaService.criar(request, RH_1).status()).isEqualTo("aberta");
    }

    @Test
    void criarDeveRecusarQuandoUsuarioNaoEhRh() {
        Usuario candidato = new Usuario("Marina", "marina@email.com", "hash", Usuario.Perfil.candidato, Usuario.Status.ativo);
        ReflectionTestUtils.setField(candidato, "id", 2L);
        VagaRequest request = new VagaRequest(
            "Vaga", "Descrição", null, null, Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(candidato));

        // token emitido quando ainda era RH: o banco e quem decide
        assertThatThrownBy(() -> vagaService.criar(request, RH_2))
            .isInstanceOf(RhInvalidoException.class);
        verify(vagaRepository, never()).save(any());
    }

    @Test
    void criarDeveRecusarQuandoRhNaoExiste() {
        VagaRequest request = new VagaRequest(
            "Vaga", "Descrição", null, null, Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vagaService.criar(request, new UsuarioLogado(99L, Usuario.Perfil.rh)))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(vagaRepository, never()).save(any());
    }

    @Test
    void listarParaAdministradorDeveTrazerTodasAsVagas() {
        when(vagaRepository.findAll()).thenReturn(List.of(
            vagaExistente(1L, "Vaga A"), vagaExistente(2L, "Vaga B")));

        assertThat(vagaService.listar(ADMINISTRADOR))
            .extracting(VagaResponse::titulo)
            .containsExactly("Vaga A", "Vaga B");
    }

    @Test
    void buscarPorIdDeveRetornarVaga() {
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vagaExistente(1L, "Vaga A")));

        assertThat(vagaService.buscarPorId(1L, RH_1).id()).isEqualTo(1L);
    }

    @Test
    void buscarPorIdDeveFalharQuandoNaoExiste() {
        when(vagaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vagaService.buscarPorId(99L, RH_1))
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
            Vaga.Modalidade.presencial, Vaga.TipoContrato.estagio, Vaga.Status.aberta, null), RH_1);

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
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.encerrada, null), RH_1);

        assertThat(resposta.status()).isEqualTo("encerrada");
    }

    @Test
    void atualizarDeveFalharQuandoNaoExiste() {
        when(vagaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vagaService.atualizar(99L, new VagaUpdateRequest(
            "Vaga", "Descrição", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null), RH_1))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(vagaRepository, never()).save(any());
    }

    @Test
    void criarPeloAdministradorDeveDeixaloComoResponsavel() {
        Usuario admin = new Usuario("Admin", "admin@email.com", "hash", Usuario.Perfil.administrador, Usuario.Status.ativo);
        ReflectionTestUtils.setField(admin, "id", 9L);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(vagaRepository.save(any(Vaga.class))).thenAnswer(inv -> inv.getArgument(0));

        VagaRequest request = new VagaRequest(
            "Vaga", "Descrição", null, null, Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, null, null);

        assertThat(vagaService.criar(request, ADMINISTRADOR).rhId()).isEqualTo(9L);
    }

    @Test
    void listarParaRhDeveTrazerSoAsVagasDele() {
        when(vagaRepository.findByRh_Id(1L)).thenReturn(List.of(vagaExistente(1L, "Vaga A")));

        assertThat(vagaService.listar(RH_1)).extracting(VagaResponse::titulo).containsExactly("Vaga A");
        verify(vagaRepository, never()).findAll();
    }

    @Test
    void listarParaCandidatoNaoDeveTrazerRascunhos() {
        when(vagaRepository.findByStatusNot(Vaga.Status.rascunho)).thenReturn(List.of(vagaExistente(1L, "Vaga A")));

        assertThat(vagaService.listar(CANDIDATO)).hasSize(1);
        verify(vagaRepository, never()).findAll();
    }

    @Test
    void buscarRascunhoComoCandidatoDeveResponderComoInexistente() {
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vagaExistente(1L, "Vaga A")));

        assertThatThrownBy(() -> vagaService.buscarPorId(1L, CANDIDATO))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarVagaDeOutroRhDeveSerNegado() {
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vagaExistente(1L, "Vaga A")));

        assertThatThrownBy(() -> vagaService.buscarPorId(1L, RH_2))
            .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void atualizarVagaDeOutroRhDeveSerNegado() {
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vagaExistente(1L, "Vaga A")));

        assertThatThrownBy(() -> vagaService.atualizar(1L, new VagaUpdateRequest(
            "Vaga", "Descrição", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null), RH_2))
            .isInstanceOf(AcessoNegadoException.class);
        verify(vagaRepository, never()).save(any());
    }

    @Test
    void administradorPodeAtualizarVagaDeQualquerRh() {
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vagaExistente(1L, "Vaga A")));
        when(vagaRepository.save(any(Vaga.class))).thenAnswer(inv -> inv.getArgument(0));

        VagaResponse resposta = vagaService.atualizar(1L, new VagaUpdateRequest(
            "Vaga B", "Descrição", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null), ADMINISTRADOR);

        assertThat(resposta.titulo()).isEqualTo("Vaga B");
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
