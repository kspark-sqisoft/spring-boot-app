package com.noa99kee.board.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI(OpenAPI 3) 설정. UI에서 JWT Bearer 입력 후 보호 API를 호출할 수 있습니다.
 */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI boardOpenApi() {
		final String bearerName = "bearerAuth";
		return new OpenAPI()
				.info(
						new Info()
								.title("Board API")
								.description("게시판·인증 REST API (액세스 토큰: Authorization Bearer)")
								.version("0.0.1"))
				.components(
						new Components()
								.addSecuritySchemes(
										bearerName,
										new SecurityScheme()
												.type(SecurityScheme.Type.HTTP)
												.scheme("bearer")
												.bearerFormat("JWT")
												.description("로그인 응답의 accessToken 값")));
	}
}
