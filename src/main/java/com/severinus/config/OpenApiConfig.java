package com.severinus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configuração da documentação OpenAPI (Swagger) da API.
 *
 * <p>Define os metadados exibidos na interface e registra o esquema de segurança
 * {@code bearerAuth}, usado pelos endpoints protegidos por JWT. Controllers
 * declaram o uso do esquema com
 * {@code @SecurityRequirement(name = "bearerAuth")}.
 *
 * <p>A documentação dos endpoints é obrigatória neste projeto: todo controller
 * precisa de {@code @Tag} e todo método exposto precisa de {@code @Operation} e
 * dos {@code @ApiResponse} correspondentes aos códigos que realmente retorna.
 * Ver {@code AGENTS.md}.
 *
 * <p>Interface disponível em {@code /swagger-ui.html}; especificação em
 * {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    /** Nome do esquema de segurança JWT referenciado pelos controllers. */
    public static final String ESQUEMA_SEGURANCA_BEARER = "bearerAuth";

    /**
     * Monta a especificação OpenAPI da API, com metadados e o esquema de
     * autenticação JWT.
     *
     * @return especificação OpenAPI usada pelo springdoc para gerar a documentação
     */
    @Bean
    public OpenAPI severinusOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Severinus API")
                .description("""
                    API do Severinus, plataforma de contratação de serviços informais ("bicos").

                    Trabalhadores publicam serviços, clientes enviam propostas, o aceite gera \
                    um contrato e as partes conversam por chat em tempo real.
                    """)
                .version("v1")
                .contact(new Contact().name("Equipe Severinus"))
                .license(new License().name("Uso interno")))
            .components(new Components()
                .addSecuritySchemes(ESQUEMA_SEGURANCA_BEARER, new SecurityScheme()
                    .name(ESQUEMA_SEGURANCA_BEARER)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT obtido em POST /auth/login.")));
    }
}
