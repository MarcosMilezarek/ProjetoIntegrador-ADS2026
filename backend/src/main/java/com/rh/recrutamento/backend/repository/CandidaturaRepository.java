package com.rh.recrutamento.backend.repository;

import com.rh.recrutamento.backend.entity.Candidatura;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidaturaRepository extends JpaRepository<Candidatura, Long> {

    boolean existsByCandidato_IdAndVaga_Id(Long candidatoId, Long vagaId);

    /** RN07: o RH so acessa dados de quem se inscreveu em alguma vaga dele. */
    boolean existsByCandidato_IdAndVaga_Rh_Id(Long candidatoId, Long rhId);

    @EntityGraph(attributePaths = {"candidato", "vaga"})
    List<Candidatura> findByCandidato_IdOrderByDataCandidaturaDesc(Long candidatoId);

    @EntityGraph(attributePaths = {"candidato", "vaga"})
    List<Candidatura> findByVaga_IdOrderByDataCandidaturaAsc(Long vagaId);
}
