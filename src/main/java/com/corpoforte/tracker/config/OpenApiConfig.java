package com.corpoforte.tracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Especificacao OpenAPI da API (/v3/api-docs, perfil dev). E' dela que os
 * clientes geram o codigo de acesso a API, entao ela e' o contrato: a v1
 * so' muda de forma aditiva (campo ou endpoint novo, nunca remocao ou
 * renomeacao), porque cliente ja instalado no aparelho de alguem nao
 * atualiza no mesmo dia que o servidor. Mudanca incompativel vira /api/v2.
 *
 * Todo endpoint exige o access token por padrao; os de autenticacao se
 * declaram publicos com @SecurityRequirements vazio.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_ACCESS_TOKEN = "accessToken";

    @Bean
    OpenAPI especificacaoDaApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Corpo Forte API")
                        .version("v1")
                        .description("API REST do Corpo Forte. Erros no formato ProblemDetail (RFC 7807)."))
                .components(new Components().addSecuritySchemes(ESQUEMA_ACCESS_TOKEN, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_ACCESS_TOKEN));
    }
}
