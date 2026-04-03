package com.noa99kee.board.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.noa99kee.board.support.AbstractIntegrationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

	@Test
	@DisplayName("POST /api/auth/register 성공 시 accessToken 과 Set-Cookie(refresh) 를 반환한다")
	void registerSuccess() throws Exception {
		mockMvc.perform(
						post("/api/auth/register")
								.contentType(APPLICATION_JSON)
								.content(
										"""
										{"email":"user1@example.com","password":"password12","name":"User One"}
										"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.user.email").value("user1@example.com"))
				.andExpect(jsonPath("$.user.name").value("User One"))
				.andExpect(cookie().exists("refresh_token"));
	}

	@Test
	@DisplayName("동일 이메일로 재가입 시 409")
	void registerDuplicateEmail() throws Exception {
		String body =
				"""
				{"email":"dup@example.com","password":"password12","name":"A"}
				""";
		mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("이메일")));
	}

	@Test
	@DisplayName("POST /api/auth/login 성공 시 토큰을 반환한다")
	void loginSuccess() throws Exception {
		mockMvc.perform(
						post("/api/auth/register")
								.contentType(APPLICATION_JSON)
								.content(
										"""
										{"email":"login@example.com","password":"secret1234","name":"Login User"}
										"""));
		mockMvc.perform(
						post("/api/auth/login")
								.contentType(APPLICATION_JSON)
								.content(
										"""
										{"email":"login@example.com","password":"secret1234"}
										"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.user.email").value("login@example.com"));
	}

	@Test
	@DisplayName("잘못된 비밀번호로 로그인 시 401 (이메일 노출 최소화 메시지)")
	void loginBadPassword() throws Exception {
		mockMvc.perform(
						post("/api/auth/register")
								.contentType(APPLICATION_JSON)
								.content(
										"""
										{"email":"badpw@example.com","password":"correctpass1","name":"X"}
										"""));
		mockMvc.perform(
						post("/api/auth/login")
								.contentType(APPLICATION_JSON)
								.content(
										"""
										{"email":"badpw@example.com","password":"wrongpassword"}
										"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	@DisplayName("Bean Validation 실패 시 400")
	void registerValidationError() throws Exception {
		mockMvc.perform(
						post("/api/auth/register")
								.contentType(APPLICATION_JSON)
								.content(
										"""
										{"email":"not-an-email","password":"short","name":""}
										"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.statusCode").value(400))
				.andExpect(jsonPath("$.message").exists());
	}
}
