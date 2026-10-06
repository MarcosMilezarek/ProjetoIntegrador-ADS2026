package com.rh.recrutamento.backend.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.rh.recrutamento.backend.Autenticacao.comoRh;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Erros de protocolo HTTP saem no formato unico de erro e com o status certo, nunca como 500. */
@SpringBootTest
@AutoConfigureMockMvc
class ErrosPadraoIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void verboNaoSuportadoRetorna405() throws Exception {
        mockMvc.perform(patch("/vagas/1").with(comoRh(1)))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.mensagem").isString());
    }

    @Test
    void corpoEmFormatoNaoSuportadoRetorna415() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.TEXT_PLAIN).content("texto"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.status").value(415))
            .andExpect(jsonPath("$.mensagem").isString());
    }
}
