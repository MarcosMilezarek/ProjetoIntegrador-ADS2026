package com.rh.recrutamento.backend.candidatura.repository;

import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
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

    /** Agenda do administrador: entrevistas marcadas de todas as vagas, as mais proximas primeiro. */
    @EntityGraph(attributePaths = {"candidato", "vaga"})
    List<Candidatura> findByStatusAndEntrevistaEmIsNotNullOrderByEntrevistaEmAsc(Candidatura.Status status);

    /** Agenda do RH: entrevistas marcadas das vagas sob a sua responsabilidade (RN07). */
    @EntityGraph(attributePaths = {"candidato", "vaga"})
    List<Candidatura> findByStatusAndEntrevistaEmIsNotNullAndVaga_Rh_IdOrderByEntrevistaEmAsc(Candidatura.Status status, Long rhId);
}
