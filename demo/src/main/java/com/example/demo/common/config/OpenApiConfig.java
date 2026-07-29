package com.example.demo.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 설정.
 *
 * <p>운영 프로파일에서는 {@code springdoc.api-docs.enabled=false} 로 문서 자체를 내린다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("로그인 API(/api/v1/auth/login)로 발급받은 accessToken 을 입력한다.");

        return new OpenAPI()
                .info(new Info()
                        .title("Demo Financial API")
                        .description("금융권 공통 API 베이스 — 표준 응답 / 거래추적 / 감사로그 / JWT 인증")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
