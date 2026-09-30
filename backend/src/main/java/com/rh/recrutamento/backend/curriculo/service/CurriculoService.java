package com.rh.recrutamento.backend.curriculo.service;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.candidatura.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.ArquivoInvalidoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.comum.service.ArquivoStorage;
import com.rh.recrutamento.backend.curriculo.dto.request.CurriculoRequest;
import com.rh.recrutamento.backend.curriculo.dto.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.curriculo.dto.request.ExperienciaRequest;
import com.rh.recrutamento.backend.curriculo.dto.request.FormacaoRequest;
import com.rh.recrutamento.backend.curriculo.dto.response.CurriculoResponse;
import com.rh.recrutamento.backend.curriculo.entity.Curriculo;
import com.rh.recrutamento.backend.curriculo.entity.CurriculoArquivo;
import com.rh.recrutamento.backend.curriculo.entity.CurriculoExperiencia;
import com.rh.recrutamento.backend.curriculo.entity.CurriculoFormacao;
import com.rh.recrutamento.backend.curriculo.exception.CandidatoInvalidoException;
import com.rh.recrutamento.backend.curriculo.exception.CurriculoJaExisteException;
import com.rh.recrutamento.backend.curriculo.mapper.CurriculoMapper;
import com.rh.recrutamento.backend.curriculo.repository.CurriculoRepository;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Regras de negocio do cadastro de curriculo (RF04/RF05). */
@Service
@Transactional(readOnly = true)
public class CurriculoService {

    private static final String PASTA_ARQUIVOS = "curriculo";

    private final CurriculoRepository curriculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurriculoMapper curriculoMapper;
    private final ArquivoStorage arquivoStorage;
    private final CandidaturaRepository candidaturaRepository;

    public CurriculoService(CurriculoRepository curriculoRepository, UsuarioRepository usuarioRepository,
                             CurriculoMapper curriculoMapper, ArquivoStorage arquivoStorage,
                             CandidaturaRepository candidaturaRepository) {
        this.curriculoRepository = curriculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.curriculoMapper = curriculoMapper;
        this.arquivoStorage = arquivoStorage;
        this.candidaturaRepository = candidaturaRepository;
    }

    @Transactional
    public CurriculoResponse criar(CurriculoRequest request, UsuarioLogado logado) {
        Usuario candidato = obterCandidato(logado.id());

        if (curriculoRepository.existsByUsuario_Id(candidato.getId())) {
            throw new CurriculoJaExisteException(candidato.getId());
        }

        Curriculo curriculo = new Curriculo(candidato);
        curriculo.atualizarDados(
            request.dataNascimento(),
            request.sexo(),
            request.cidade(),
            request.uf(),
            request.numeroContato(),
            request.perfilLinkedin(),
            request.competencias(),
            request.certificacoes(),
            request.resumo()
        );
        curriculo.substituirFormacoes(paraFormacoes(request.formacoes()));
        curriculo.substituirExperiencias(paraExperiencias(request.experiencias()));

        return curriculoMapper.toResponse(curriculoRepository.save(curriculo));
    }

    public CurriculoResponse buscarPorId(Long id, UsuarioLogado logado) {
        Curriculo curriculo = obterCurriculo(id);
        verificarLeitura(curriculo.getUsuario().getId(), logado);
        return curriculoMapper.toResponse(curriculo);
    }

    public CurriculoResponse buscarPorUsuario(Long usuarioId, UsuarioLogado logado) {
        // checa antes de buscar, para nao revelar a terceiros se o curriculo existe
        verificarLeitura(usuarioId, logado);
        Curriculo curriculo = curriculoRepository.findByUsuario_Id(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Curriculo do usuario " + usuarioId + " nao encontrado."));
        return curriculoMapper.toResponse(curriculo);
    }

    @Transactional
    public CurriculoResponse atualizar(Long id, CurriculoUpdateRequest request, UsuarioLogado logado) {
        Curriculo curriculo = obterCurriculo(id);
        verificarDono(curriculo, logado);

        curriculo.atualizarDados(
            request.dataNascimento(),
            request.sexo(),
            request.cidade(),
            request.uf(),
            request.numeroContato(),
            request.perfilLinkedin(),
            request.competencias(),
            request.certificacoes(),
            request.resumo()
        );
        curriculo.substituirFormacoes(paraFormacoes(request.formacoes()));
        curriculo.substituirExperiencias(paraExperiencias(request.experiencias()));

        return curriculoMapper.toResponse(curriculoRepository.save(curriculo));
    }

    /** Anexa (ou substitui) o PDF do curriculo. */
    @Transactional
    public CurriculoResponse anexarArquivo(Long id, MultipartFile arquivo, UsuarioLogado logado) {
        Curriculo curriculo = obterCurriculo(id);
        verificarDono(curriculo, logado);
        CurriculoArquivo anterior = curriculo.getArquivo();
        // definirArquivo atualiza a entidade existente no lugar, entao o nome antigo precisa ser lido antes.
        String nomeAnterior = anterior != null ? anterior.getNomeArmazenado() : null;

        if (!"application/pdf".equalsIgnoreCase(arquivo.getContentType())) {
            throw new ArquivoInvalidoException("Somente arquivos PDF sao aceitos.");
        }
        String nomeArmazenado = arquivoStorage.salvar(PASTA_ARQUIVOS, arquivo, "pdf");
        curriculo.definirArquivo(new CurriculoArquivo(
            arquivo.getOriginalFilename(), nomeArmazenado, arquivo.getContentType(), arquivo.getSize()));

        Curriculo salvo = curriculoRepository.save(curriculo);
        if (nomeAnterior != null) {
            arquivoStorage.remover(PASTA_ARQUIVOS, nomeAnterior);
        }
        return curriculoMapper.toResponse(salvo);
    }

    public ArquivoBaixado baixarArquivo(Long id, UsuarioLogado logado) {
        Curriculo curriculo = obterCurriculo(id);
        verificarLeitura(curriculo.getUsuario().getId(), logado);
        CurriculoArquivo arquivo = curriculo.getArquivo();
        if (arquivo == null) {
            throw new RecursoNaoEncontradoException("Curriculo " + id + " nao possui arquivo anexado.");
        }
        return new ArquivoBaixado(arquivo.getNomeOriginal(), arquivo.getContentType(),
            arquivoStorage.ler(PASTA_ARQUIVOS, arquivo.getNomeArmazenado()));
    }

    /** So o proprio candidato altera o curriculo. */
    private void verificarDono(Curriculo curriculo, UsuarioLogado logado) {
        if (!curriculo.getUsuario().getId().equals(logado.id())) {
            throw new AcessoNegadoException("Você só pode alterar o seu próprio currículo.");
        }
    }

    /** Leem o curriculo: o proprio candidato, o administrador e o RH de uma vaga em que o candidato se inscreveu (RF12/RN07). */
    private void verificarLeitura(Long donoId, UsuarioLogado logado) {
        boolean permitido = switch (logado.perfil()) {
            case candidato -> donoId.equals(logado.id());
            case rh -> candidaturaRepository.existsByCandidato_IdAndVaga_Rh_Id(donoId, logado.id());
            case administrador -> true;
        };
        if (!permitido) {
            throw new AcessoNegadoException("Você não tem acesso a este currículo.");
        }
    }

    private Curriculo obterCurriculo(Long id) {
        return curriculoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Curriculo " + id + " nao encontrado."));
    }

    private Usuario obterCandidato(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario " + usuarioId + " nao encontrado."));
        if (usuario.getPerfil() != Usuario.Perfil.candidato) {
            throw new CandidatoInvalidoException(usuarioId);
        }
        return usuario;
    }

    private List<CurriculoFormacao> paraFormacoes(List<FormacaoRequest> itens) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream()
            .map(item -> new CurriculoFormacao(
                item.curso().trim(), item.instituicao().trim(), item.dataInicio(), item.dataTermino()))
            .toList();
    }

    private List<CurriculoExperiencia> paraExperiencias(List<ExperienciaRequest> itens) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream()
            .map(item -> new CurriculoExperiencia(
                item.cargo().trim(), item.empresa().trim(), item.dataContratacao(),
                item.dataDemissao(), item.trabalhoAtual(), item.descricaoAtividades()))
            .toList();
    }

    /** PDF pronto para resposta HTTP. */
    public record ArquivoBaixado(String nomeOriginal, String contentType, byte[] conteudo) {}
}
