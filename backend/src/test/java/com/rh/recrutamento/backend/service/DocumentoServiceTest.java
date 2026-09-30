package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.auth.UsuarioLogado;
import com.rh.recrutamento.backend.dto.documento.response.DocumentoResponse;
import com.rh.recrutamento.backend.entity.Candidatura;
import com.rh.recrutamento.backend.entity.Documento;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.entity.Vaga;
import com.rh.recrutamento.backend.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.exception.ArquivoInvalidoException;
import com.rh.recrutamento.backend.exception.DocumentoNaoPermitidoException;
import com.rh.recrutamento.backend.mapper.DocumentoMapperImpl;
import com.rh.recrutamento.backend.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.repository.DocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    private static final UsuarioLogado ANA = new UsuarioLogado(1L, Usuario.Perfil.candidato);
    private static final UsuarioLogado BRUNO = new UsuarioLogado(2L, Usuario.Perfil.candidato);
    private static final UsuarioLogado RITA = new UsuarioLogado(7L, Usuario.Perfil.rh);
    private static final UsuarioLogado PAULO = new UsuarioLogado(8L, Usuario.Perfil.rh);

    private final MockMultipartFile pdf = new MockMultipartFile("arquivo", "rg.pdf", "application/pdf", "%PDF".getBytes());

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private CandidaturaRepository candidaturaRepository;

    @Mock
    private ArquivoStorage arquivoStorage;

    private DocumentoService documentoService;

    @BeforeEach
    void montarService() {
        documentoService = new DocumentoService(documentoRepository, candidaturaRepository,
            new DocumentoMapperImpl(), arquivoStorage);
    }

    @Test
    void candidatoAprovadoEnviaDocumento() {
        when(candidaturaRepository.findById(20L)).thenReturn(Optional.of(candidaturaDaAna(Candidatura.Status.aprovado)));
        when(arquivoStorage.salvar(eq("documento"), any(), eq("pdf"))).thenReturn("gerado.pdf");
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoResponse resposta = documentoService.enviar(20L, " RG ", pdf, ANA);

        assertThat(resposta.tipo()).isEqualTo("RG");
        assertThat(resposta.formato()).isEqualTo("pdf");
        assertThat(resposta.candidatoId()).isEqualTo(1L);
        assertThat(resposta.vagaTitulo()).isEqualTo("Backend Java");
    }

    @Test
    void envioAntesDaAprovacaoDeveSerRecusado() {
        when(candidaturaRepository.findById(20L)).thenReturn(Optional.of(candidaturaDaAna(Candidatura.Status.entrevista)));

        assertThatThrownBy(() -> documentoService.enviar(20L, "RG", pdf, ANA))
            .isInstanceOf(DocumentoNaoPermitidoException.class);
        verifyNoInteractions(arquivoStorage);
    }

    @Test
    void envioNaCandidaturaDeOutroCandidatoDeveSerNegado() {
        when(candidaturaRepository.findById(20L)).thenReturn(Optional.of(candidaturaDaAna(Candidatura.Status.aprovado)));

        assertThatThrownBy(() -> documentoService.enviar(20L, "RG", pdf, BRUNO))
            .isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(arquivoStorage);
    }

    @Test
    void formatoForaDePdfOuDocxDeveSerRecusado() {
        when(candidaturaRepository.findById(20L)).thenReturn(Optional.of(candidaturaDaAna(Candidatura.Status.aprovado)));

        assertThatThrownBy(() -> documentoService.enviar(20L, "RG",
            new MockMultipartFile("arquivo", "rg.png", "image/png", "png".getBytes()), ANA))
            .isInstanceOf(ArquivoInvalidoException.class)
            .hasMessageContaining("PDF ou DOCX");
        verifyNoInteractions(arquivoStorage);
    }

    @Test
    void candidatoListaSoOsProprios() {
        when(documentoRepository.findByCandidatura_Candidato_IdOrderByDataEnvioDesc(1L))
            .thenReturn(List.of(documentoDaAna()));

        assertThat(documentoService.listar(null, ANA)).extracting(DocumentoResponse::candidatoId).containsOnly(1L);
        verify(documentoRepository, never()).findAllByOrderByDataEnvioDesc();
    }

    @Test
    void candidatoNaoPedeDocumentosDeOutroCandidato() {
        assertThatThrownBy(() -> documentoService.listar(2L, ANA))
            .isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(documentoRepository);
    }

    @Test
    void rhListaDocumentosDasSuasVagasFiltrandoPorCandidato() {
        when(documentoRepository.findByCandidatura_Vaga_Rh_IdOrderByDataEnvioDesc(7L))
            .thenReturn(List.of(documentoDaAna()));

        assertThat(documentoService.listar(1L, RITA)).hasSize(1);
        assertThat(documentoService.listar(2L, RITA)).isEmpty();
    }

    @Test
    void downloadSoParaODonoORhDaVagaEOAdministrador() {
        Documento documento = documentoDaAna();
        when(documentoRepository.findById(30L)).thenReturn(Optional.of(documento));
        when(arquivoStorage.ler("documento", "gerado.pdf")).thenReturn("%PDF".getBytes());

        assertThat(documentoService.baixar(30L, ANA).nome()).isEqualTo("RG.pdf");
        assertThat(documentoService.baixar(30L, RITA).contentType()).isEqualTo("application/pdf");
        assertThat(documentoService.baixar(30L, new UsuarioLogado(9L, Usuario.Perfil.administrador)).conteudo())
            .isEqualTo("%PDF".getBytes());
        assertThatThrownBy(() -> documentoService.baixar(30L, BRUNO)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> documentoService.baixar(30L, PAULO)).isInstanceOf(AcessoNegadoException.class);
    }

    /** Candidatura da Ana (id 1) na vaga da Rita (id 7). */
    private Candidatura candidaturaDaAna(Candidatura.Status status) {
        Vaga vaga = new Vaga(usuario(7L, Usuario.Perfil.rh), "Backend Java", "Descricao", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null);
        ReflectionTestUtils.setField(vaga, "id", 10L);
        Candidatura candidatura = new Candidatura(usuario(1L, Usuario.Perfil.candidato), vaga);
        candidatura.alterarStatus(status);
        ReflectionTestUtils.setField(candidatura, "id", 20L);
        return candidatura;
    }

    private Documento documentoDaAna() {
        Documento documento = new Documento(candidaturaDaAna(Candidatura.Status.aprovado), "RG",
            Documento.Formato.pdf, "gerado.pdf", 4L);
        ReflectionTestUtils.setField(documento, "id", 30L);
        return documento;
    }

    private Usuario usuario(Long id, Usuario.Perfil perfil) {
        Usuario usuario = new Usuario("Usuario " + id, "u" + id + "@teste.com", "hash", perfil, Usuario.Status.ativo);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
