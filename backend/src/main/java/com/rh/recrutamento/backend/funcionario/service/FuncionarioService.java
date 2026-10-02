package com.rh.recrutamento.backend.funcionario.service;

import com.rh.recrutamento.backend.auth.dto.UsuarioLogado;
import com.rh.recrutamento.backend.comum.exception.AcessoNegadoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import com.rh.recrutamento.backend.documento.dto.response.DocumentoResponse;
import com.rh.recrutamento.backend.documento.mapper.DocumentoMapper;
import com.rh.recrutamento.backend.documento.repository.DocumentoRepository;
import com.rh.recrutamento.backend.funcionario.dto.response.FuncionarioPerfilResponse;
import com.rh.recrutamento.backend.funcionario.dto.response.FuncionarioResponse;
import com.rh.recrutamento.backend.funcionario.entity.Funcionario;
import com.rh.recrutamento.backend.funcionario.mapper.FuncionarioMapper;
import com.rh.recrutamento.backend.funcionario.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Funcionarios (candidaturas contratadas). O RH ve os das suas vagas (RN07); o administrador ve todos.
 * A contratacao em si e feita por CandidaturaService.contratar, junto com a mudanca de etapa.
 */
@Service
@Transactional(readOnly = true)
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final FuncionarioMapper funcionarioMapper;
    private final DocumentoRepository documentoRepository;
    private final DocumentoMapper documentoMapper;

    public FuncionarioService(FuncionarioRepository funcionarioRepository, FuncionarioMapper funcionarioMapper,
                              DocumentoRepository documentoRepository, DocumentoMapper documentoMapper) {
        this.funcionarioRepository = funcionarioRepository;
        this.funcionarioMapper = funcionarioMapper;
        this.documentoRepository = documentoRepository;
        this.documentoMapper = documentoMapper;
    }

    public List<FuncionarioResponse> listar(UsuarioLogado logado) {
        List<Funcionario> funcionarios = logado.ehAdministrador()
            ? funcionarioRepository.findAllByOrderByDataContratacaoDesc()
            : funcionarioRepository.findByCandidatura_Vaga_Rh_IdOrderByDataContratacaoDesc(logado.id());
        return funcionarios.stream().map(funcionarioMapper::toResponse).toList();
    }

    public FuncionarioPerfilResponse perfil(Long id, UsuarioLogado logado) {
        Funcionario funcionario = obterComAcesso(id, logado);
        List<DocumentoResponse> documentos = documentoRepository
            .findByCandidatura_IdOrderByDataEnvioDesc(funcionario.getCandidatura().getId()).stream()
            .map(documentoMapper::toResponse)
            .toList();
        return new FuncionarioPerfilResponse(funcionarioMapper.toResponse(funcionario), documentos);
    }

    /** Inativacao suave: so muda o status. Inativar quem ja esta inativo nao muda nada. */
    @Transactional
    public FuncionarioResponse inativar(Long id, UsuarioLogado logado) {
        Funcionario funcionario = obterComAcesso(id, logado);
        funcionario.inativar();
        return funcionarioMapper.toResponse(funcionario);
    }

    private Funcionario obterComAcesso(Long id, UsuarioLogado logado) {
        Funcionario funcionario = funcionarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário " + id + " não encontrado."));
        if (!logado.gerencia(funcionario.getCandidatura().getVaga())) {
            throw new AcessoNegadoException("Este funcionário foi contratado em uma vaga sob responsabilidade de outro RH.");
        }
        return funcionario;
    }
}
