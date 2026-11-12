package com.rh.recrutamento.backend.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Swagger UI e OpenAPI abrem sem token e descrevem o esquema Bearer JWT usado pelo botao Authorize. */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentacaoApiIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiEPublicaEDescreveOEsquemaJwt() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("API de Seleção e Recrutamento"))
            .andExpect(jsonPath("$.components.securitySchemes.bearerJwt.scheme").value("bearer"))
            .andExpect(jsonPath("$.components.securitySchemes.bearerJwt.bearerFormat").value("JWT"))
            .andExpect(jsonPath("$.paths['/auth/login']").exists());
    }

    @Test
    void swaggerUiEPublico() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }
}
