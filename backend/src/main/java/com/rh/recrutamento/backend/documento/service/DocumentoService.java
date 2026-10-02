package com.rh.recrutamento.backend.documento.service;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import com.rh.recrutamento.backend.candidatura.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.ArquivoInvalidoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.comum.service.ArquivoStorage;
import com.rh.recrutamento.backend.documento.dto.response.DocumentoResponse;
import com.rh.recrutamento.backend.documento.dto.response.DocumentosDaCandidaturaResponse;
import com.rh.recrutamento.backend.documento.dto.response.TipoDocumentoResponse;
import com.rh.recrutamento.backend.documento.entity.Documento;
import com.rh.recrutamento.backend.documento.exception.DocumentoNaoPermitidoException;
import com.rh.recrutamento.backend.documento.mapper.DocumentoMapper;
import com.rh.recrutamento.backend.documento.repository.DocumentoRepository;
import com.rh.recrutamento.backend.notificacao.entity.Notificacao;
import com.rh.recrutamento.backend.notificacao.service.NotificacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final NotificacaoService notificacaoService;

    public DocumentoService(DocumentoRepository documentoRepository, CandidaturaRepository candidaturaRepository,
                            DocumentoMapper documentoMapper, ArquivoStorage arquivoStorage,
                            NotificacaoService notificacaoService) {
        this.documentoRepository = documentoRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.documentoMapper = documentoMapper;
        this.arquivoStorage = arquivoStorage;
        this.notificacaoService = notificacaoService;
    }

    /** A lista fechada de tipos, na ordem de exibicao. */
    public List<TipoDocumentoResponse> listarTipos() {
        return Arrays.stream(Documento.Tipo.values()).map(TipoDocumentoResponse::de).toList();
    }

    /**
     * Envia o documento de um tipo da lista. Se a candidatura ja tem um documento desse tipo,
     * o arquivo novo substitui o anterior (um por tipo).
     */
    @Transactional
    public DocumentoResponse enviar(Long candidaturaId, String tipo, MultipartFile arquivo, UsuarioLogado logado) {
        Candidatura candidatura = obterCandidatura(candidaturaId);
        if (!candidatura.getCandidato().getId().equals(logado.id())) {
            throw new AcessoNegadoException("Você só pode enviar documentos das suas próprias candidaturas.");
        }
        if (candidatura.getStatus() != Candidatura.Status.aprovado
                && candidatura.getStatus() != Candidatura.Status.contratado) {
            throw new DocumentoNaoPermitidoException();
        }
        Documento.Tipo tipoDocumento = Arrays.stream(Documento.Tipo.values())
            .filter(t -> t.name().equalsIgnoreCase(tipo == null ? "" : tipo.trim()))
            .findFirst()
            .orElseThrow(() -> new ArquivoInvalidoException(
                "Tipo de documento não reconhecido. Escolha um dos tipos da lista para continuar."));
        Documento.Formato formato = Arrays.stream(Documento.Formato.values())
            .filter(f -> f.getContentType().equalsIgnoreCase(arquivo.getContentType()))
            .findFirst()
            .orElseThrow(() -> new ArquivoInvalidoException(
                "Aceitamos arquivos em PDF ou DOCX. Salve o seu documento em um desses formatos e envie novamente."));

        String nomeArmazenado = arquivoStorage.salvar(PASTA_ARQUIVOS, arquivo, formato.name());
        Optional<Documento> anterior = documentoRepository.findByCandidatura_IdAndTipo(candidaturaId, tipoDocumento);
        Documento documento;
        if (anterior.isPresent()) {
            documento = anterior.get();
            String arquivoAnterior = documento.getArquivoUrl();
            documento.substituirArquivo(formato, nomeArmazenado, arquivo.getSize());
            arquivoStorage.remover(PASTA_ARQUIVOS, arquivoAnterior);
        } else {
            documento = documentoRepository.save(
                new Documento(candidatura, tipoDocumento, formato, nomeArmazenado, arquivo.getSize()));
        }
        notificacaoService.notificar(candidatura.getVaga().getRh(), Notificacao.Tipo.candidatura, candidatura.getId(),
            "Documento recebido",
            candidatura.getCandidato().getNome() + " enviou " + tipoDocumento.getNome()
                + " para a vaga " + candidatura.getVaga().getTitulo() + ".");
        return documentoMapper.toResponse(documento);
    }

    /**
     * O RH aprova ou recusa um documento enviado (so o responsavel pela vaga, ou o administrador).
     * O candidato e avisado uma unica vez, quando a situacao realmente muda.
     */
    @Transactional
    public DocumentoResponse avaliar(Long id, Documento.Status resultado, UsuarioLogado logado) {
        Documento documento = documentoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Documento " + id + " não encontrado."));
        Candidatura candidatura = documento.getCandidatura();
        if (!logado.gerencia(candidatura.getVaga())) {
            throw new AcessoNegadoException("Esta vaga está sob responsabilidade de outro RH.");
        }
        if (documento.getStatus() != resultado) {
            documento.alterarStatus(resultado);
            String descricao = documento.nomeDoTipo() + " da vaga " + candidatura.getVaga().getTitulo();
            if (resultado == Documento.Status.aprovado) {
                notificacaoService.notificar(candidatura.getCandidato(), Notificacao.Tipo.candidatura, candidatura.getId(),
                    "Documento aprovado", "Seu documento " + descricao + " foi aprovado. Obrigado pelo envio!");
            } else {
                notificacaoService.notificar(candidatura.getCandidato(), Notificacao.Tipo.candidatura, candidatura.getId(),
                    "Precisamos de um novo envio", "Não conseguimos aprovar o documento " + descricao
                        + " desta vez. Sem problema: envie uma nova versão na tela de Documentos e o RH analisa novamente.");
            }
        }
        return documentoMapper.toResponse(documento);
    }

    /** Nomes dos documentos obrigatorios que ainda nao foram aprovados pelo RH (vazio = pode contratar). */
    public List<String> obrigatoriosNaoAprovados(Long candidaturaId) {
        Set<Documento.Tipo> aprovados = documentoRepository.findByCandidatura_IdOrderByDataEnvioDesc(candidaturaId).stream()
            .filter(d -> d.getStatus() == Documento.Status.aprovado)
            .map(Documento::getTipo)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return Arrays.stream(Documento.Tipo.values())
            .filter(Documento.Tipo::isObrigatorio)
            .filter(t -> !aprovados.contains(t))
            .map(Documento.Tipo::getNome)
            .toList();
    }

    /** Enviados e pendentes de uma candidatura. Ve o dono, o RH da vaga e o administrador. */
    public DocumentosDaCandidaturaResponse situacao(Long candidaturaId, UsuarioLogado logado) {
        Candidatura candidatura = obterCandidatura(candidaturaId);
        verificarAcesso(candidatura, logado);

        List<Documento> enviados = documentoRepository.findByCandidatura_IdOrderByDataEnvioDesc(candidaturaId);
        Set<Documento.Tipo> tiposEnviados = enviados.stream()
            .map(Documento::getTipo)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<TipoDocumentoResponse> pendentes = Arrays.stream(Documento.Tipo.values())
            .filter(t -> !tiposEnviados.contains(t))
            .map(TipoDocumentoResponse::de)
            .toList();
        long totalExigidos = Arrays.stream(Documento.Tipo.values()).filter(Documento.Tipo::isObrigatorio).count();
        long enviadosExigidos = tiposEnviados.stream().filter(Documento.Tipo::isObrigatorio).count();

        return new DocumentosDaCandidaturaResponse(candidatura.getId(), candidatura.getCandidato().getId(),
            candidatura.getCandidato().getNome(), candidatura.getVaga().getId(), candidatura.getVaga().getTitulo(),
            enviadosExigidos, totalExigidos, enviados.stream().map(documentoMapper::toResponse).toList(), pendentes);
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
        verificarAcesso(documento.getCandidatura(), logado);
        return new DocumentoBaixado(
            documento.nomeDoTipo() + "." + documento.getFormato().name(),
            documento.getFormato().getContentType(),
            arquivoStorage.ler(PASTA_ARQUIVOS, documento.getArquivoUrl()));
    }

    /** O dono da candidatura, o RH responsavel pela vaga ou o administrador. */
    private void verificarAcesso(Candidatura candidatura, UsuarioLogado logado) {
        if (!candidatura.getCandidato().getId().equals(logado.id()) && !logado.gerencia(candidatura.getVaga())) {
            throw new AcessoNegadoException("Você não tem acesso aos documentos desta candidatura.");
        }
    }

    private Candidatura obterCandidatura(Long id) {
        return candidaturaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Candidatura " + id + " não encontrada."));
    }

    /** Arquivo pronto para resposta HTTP. */
    public record DocumentoBaixado(String nome, String contentType, byte[] conteudo) {}
}
