package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.CurriculoUpdateRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.ExperienciaRequest;
import com.rh.recrutamento.backend.dto.curriculo.request.FormacaoRequest;
import com.rh.recrutamento.backend.dto.curriculo.response.CurriculoResponse;
import com.rh.recrutamento.backend.entity.Curriculo;
import com.rh.recrutamento.backend.entity.CurriculoArquivo;
import com.rh.recrutamento.backend.entity.CurriculoExperiencia;
import com.rh.recrutamento.backend.entity.CurriculoFormacao;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.exception.CandidatoInvalidoException;
import com.rh.recrutamento.backend.exception.CurriculoJaExisteException;
import com.rh.recrutamento.backend.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.mapper.CurriculoMapper;
import com.rh.recrutamento.backend.repository.CurriculoRepository;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Regras de negocio do cadastro de curriculo (RF04/RF05). */
@Service
@Transactional(readOnly = true)
public class CurriculoService {

    private final CurriculoRepository curriculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurriculoMapper curriculoMapper;
    private final ArquivoCurriculoStorage arquivoStorage;

    public CurriculoService(CurriculoRepository curriculoRepository, UsuarioRepository usuarioRepository,
                             CurriculoMapper curriculoMapper, ArquivoCurriculoStorage arquivoStorage) {
        this.curriculoRepository = curriculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.curriculoMapper = curriculoMapper;
        this.arquivoStorage = arquivoStorage;
    }

    @Transactional
    public CurriculoResponse criar(CurriculoRequest request) {
        Usuario candidato = obterCandidato(request.usuarioId());

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

    public CurriculoResponse buscarPorId(Long id) {
        return curriculoMapper.toResponse(obterCurriculo(id));
    }

    public CurriculoResponse buscarPorUsuario(Long usuarioId) {
        Curriculo curriculo = curriculoRepository.findByUsuario_Id(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Curriculo do usuario " + usuarioId + " nao encontrado."));
        return curriculoMapper.toResponse(curriculo);
    }

    @Transactional
    public CurriculoResponse atualizar(Long id, CurriculoUpdateRequest request) {
        Curriculo curriculo = obterCurriculo(id);

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
    public CurriculoResponse anexarArquivo(Long id, MultipartFile arquivo) {
        Curriculo curriculo = obterCurriculo(id);
        CurriculoArquivo anterior = curriculo.getArquivo();

        String nomeArmazenado = arquivoStorage.salvar(arquivo);
        curriculo.definirArquivo(new CurriculoArquivo(
            arquivo.getOriginalFilename(), nomeArmazenado, arquivo.getContentType(), arquivo.getSize()));

        Curriculo salvo = curriculoRepository.save(curriculo);
        if (anterior != null) {
            arquivoStorage.remover(anterior.getNomeArmazenado());
        }
        return curriculoMapper.toResponse(salvo);
    }

    public ArquivoBaixado baixarArquivo(Long id) {
        CurriculoArquivo arquivo = obterCurriculo(id).getArquivo();
        if (arquivo == null) {
            throw new RecursoNaoEncontradoException("Curriculo " + id + " nao possui arquivo anexado.");
        }
        return new ArquivoBaixado(arquivo.getNomeOriginal(), arquivo.getContentType(),
            arquivoStorage.ler(arquivo.getNomeArmazenado()));
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
