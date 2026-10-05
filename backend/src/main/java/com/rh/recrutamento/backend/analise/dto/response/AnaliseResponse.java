package com.rh.recrutamento.backend.analise.dto.response;

import java.util.List;

/** Triagem por IA, so para o RH. Fora de CONCLUIDA a aderencia e nula e as listas vem vazias. */
public record AnaliseResponse(
    /** PENDENTE, CONCLUIDA ou FALHA. */
    String status,
    /** Aderencia do candidato a vaga, inteiro de 0 a 100 (%). */
    Integer aderencia,
    List<String> pontosPositivos,
    List<String> pontosNegativos
) {}
