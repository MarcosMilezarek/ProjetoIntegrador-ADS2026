package com.rh.recrutamento.backend.comum.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentacao OpenAPI e o esquema Bearer JWT, que habilita o botao Authorize do Swagger UI.
 * O token e o mesmo devolvido por POST /auth/login.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerJwt";

    @Bean
    public OpenAPI documentacaoApi() {
        return new OpenAPI()
            .info(new Info()
                .title("API de Seleção e Recrutamento")
                .description("API REST do portal do candidato e do painel do RH: vagas, currículos, candidaturas, "
                    + "documentos de contratação e triagem de currículos por IA. Para testar as rotas protegidas, "
                    + "faça login em POST /auth/login e informe o token no botão Authorize.")
                .version("0.0.1"))
            .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
