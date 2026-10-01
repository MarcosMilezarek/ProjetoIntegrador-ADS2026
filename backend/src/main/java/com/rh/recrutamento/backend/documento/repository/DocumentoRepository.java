package com.rh.recrutamento.backend.documento.repository;

import com.rh.recrutamento.backend.documento.entity.Documento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    @EntityGraph(attributePaths = {"candidatura.candidato", "candidatura.vaga"})
    List<Documento> findByCandidatura_Candidato_IdOrderByDataEnvioDesc(Long candidatoId);

    /** Documentos das candidaturas das vagas sob responsabilidade do RH (RN07). */
    @EntityGraph(attributePaths = {"candidatura.candidato", "candidatura.vaga"})
    List<Documento> findByCandidatura_Vaga_Rh_IdOrderByDataEnvioDesc(Long rhId);

    @EntityGraph(attributePaths = {"candidatura.candidato", "candidatura.vaga"})
    List<Documento> findAllByOrderByDataEnvioDesc();

    @EntityGraph(attributePaths = {"candidatura.candidato", "candidatura.vaga"})
    List<Documento> findByCandidatura_IdOrderByDataEnvioDesc(Long candidaturaId);

    Optional<Documento> findByCandidatura_IdAndTipo(Long candidaturaId, Documento.Tipo tipo);
}
