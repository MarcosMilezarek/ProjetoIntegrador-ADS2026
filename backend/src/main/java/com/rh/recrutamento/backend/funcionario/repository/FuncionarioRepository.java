package com.rh.recrutamento.backend.funcionario.repository;

import com.rh.recrutamento.backend.funcionario.entity.Funcionario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    @EntityGraph(attributePaths = {"candidatura.candidato", "candidatura.vaga"})
    List<Funcionario> findAllByOrderByDataContratacaoDesc();

    /** Funcionarios contratados nas vagas sob responsabilidade do RH (RN07). */
    @EntityGraph(attributePaths = {"candidatura.candidato", "candidatura.vaga"})
    List<Funcionario> findByCandidatura_Vaga_Rh_IdOrderByDataContratacaoDesc(Long rhId);
}
