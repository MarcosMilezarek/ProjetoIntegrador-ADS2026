package com.rh.recrutamento.backend.documento.service;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import com.rh.recrutamento.backend.candidatura.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.ArquivoInvalidoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.comum.service.ArquivoStorage;
import com.rh.recrutamento.backend.documento.dto.response.DocumentoResponse;
import com.rh.recrutamento.backend.documento.entity.Documento;
import com.rh.recrutamento.backend.documento.exception.DocumentoNaoPermitidoException;
import com.rh.recrutamento.backend.documento.mapper.DocumentoMapper;
import com.rh.recrutamento.backend.documento.repository.DocumentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Documentos de contratacao (RF09), separados por candidato: o candidato ve so os seus (RN06);
 * o RH ve os dos candidatos das suas vagas (RN07); o administrador ve todos.
 */
@Service
@Transactional(readOnly = true)
public class DocumentoService {

    private static final String PASTA_ARQUIVOS = "documento";

    private final DocumentoRepository documentoRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final DocumentoMapper documentoMapper;
    private final ArquivoStorage arquivoStorage;

    public DocumentoService(DocumentoRepository documentoRepository, CandidaturaRepository candidaturaRepository,
                            DocumentoMapper documentoMapper, ArquivoStorage arquivoStorage) {
        this.documentoRepository = documentoRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.documentoMapper = documentoMapper;
        this.arquivoStorage = arquivoStorage;
    }

    @Transactional
    public DocumentoResponse enviar(Long candidaturaId, String tipo, MultipartFile arquivo, UsuarioLogado logado) {
        Candidatura candidatura = candidaturaRepository.findById(candidaturaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Candidatura " + candidaturaId + " não encontrada."));
        if (!candidatura.getCandidato().getId().equals(logado.id())) {
            throw new AcessoNegadoException("Você só pode enviar documentos das suas próprias candidaturas.");
        }
        if (candidatura.getStatus() != Candidatura.Status.aprovado
                && candidatura.getStatus() != Candidatura.Status.contratado) {
            throw new DocumentoNaoPermitidoException();
        }
        if (tipo == null || tipo.isBlank() || tipo.trim().length() > 100) {
            throw new ArquivoInvalidoException("Informe o tipo do documento (até 100 caracteres).");
        }
        Documento.Formato formato = Arrays.stream(Documento.Formato.values())
            .filter(f -> f.getContentType().equalsIgnoreCase(arquivo.getContentType()))
            .findFirst()
            .orElseThrow(() -> new ArquivoInvalidoException("Somente arquivos PDF ou DOCX são aceitos."));

        String nomeArmazenado = arquivoStorage.salvar(PASTA_ARQUIVOS, arquivo, formato.name());
        Documento documento = documentoRepository.save(
            new Documento(candidatura, tipo.trim(), formato, nomeArmazenado, arquivo.getSize()));
        return documentoMapper.toResponse(documento);
    }

    /** Lista conforme o papel; candidatoId (opcional) filtra os documentos de um candidato. */
    public List<DocumentoResponse> listar(Long candidatoId, UsuarioLogado logado) {
        if (logado.ehCandidato() && candidatoId != null && !candidatoId.equals(logado.id())) {
            throw new AcessoNegadoException("Você só pode ver os seus próprios documentos.");
        }
        List<Documento> documentos = switch (logado.perfil()) {
            case candidato -> documentoRepository.findByCandidatura_Candidato_IdOrderByDataEnvioDesc(logado.id());
            case rh -> documentoRepository.findByCandidatura_Vaga_Rh_IdOrderByDataEnvioDesc(logado.id());
            case administrador -> documentoRepository.findAllByOrderByDataEnvioDesc();
        };
        return documentos.stream()
            .filter(d -> candidatoId == null || d.getCandidatura().getCandidato().getId().equals(candidatoId))
            .map(documentoMapper::toResponse)
            .toList();
    }

    public DocumentoBaixado baixar(Long id, UsuarioLogado logado) {
        Documento documento = documentoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Documento " + id + " não encontrado."));
        Candidatura candidatura = documento.getCandidatura();
        boolean permitido = candidatura.getCandidato().getId().equals(logado.id()) || logado.gerencia(candidatura.getVaga());
        if (!permitido) {
            throw new AcessoNegadoException("Você não tem acesso a este documento.");
        }
        return new DocumentoBaixado(
            documento.getTipo() + "." + documento.getFormato().name(),
            documento.getFormato().getContentType(),
            arquivoStorage.ler(PASTA_ARQUIVOS, documento.getArquivoUrl()));
    }

    /** Arquivo pronto para resposta HTTP. */
    public record DocumentoBaixado(String nome, String contentType, byte[] conteudo) {}
}
