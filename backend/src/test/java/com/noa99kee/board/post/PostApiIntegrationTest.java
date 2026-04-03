package com.noa99kee.board.post;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import com.noa99kee.board.support.AbstractIntegrationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class PostApiIntegrationTest extends AbstractIntegrationTest {

	@Test
	@DisplayName("GET /api/posts 는 익명으로 200")
	void listPostsAnonymous() throws Exception {
		mockMvc.perform(get("/api/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@DisplayName("인증 없이 글 작성 시 401")
	void createPostWithoutAuth() throws Exception {
		mockMvc.perform(
						post("/api/posts")
								.contentType(APPLICATION_JSON)
								.content(
										"""
										{"title":"T","content":"C"}
										"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("잘못된 Bearer 토큰은 401")
	void invalidBearerRejected() throws Exception {
		mockMvc.perform(
						get("/api/auth/me").header("Authorization", "Bearer not-a-valid-jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("회원가입 후 글 작성·목록·상세·삭제 흐름")
	void postCrudHappyPath() throws Exception {
		String access = registerAccessToken("author@example.com", "password12", "Author");

		MvcResult createResult =
				mockMvc.perform(
								post("/api/posts")
										.header("Authorization", "Bearer " + access)
										.contentType(APPLICATION_JSON)
										.content(
												"""
												{"title":"Hello","content":"Body text"}
												"""))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.title").value("Hello"))
						.andExpect(jsonPath("$.content").value("Body text"))
						.andExpect(jsonPath("$.id").exists())
						.andReturn();

		String postId =
				objectMapper
						.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
						.get("id")
						.asText();

		mockMvc.perform(get("/api/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(postId))
				.andExpect(jsonPath("$[0].title").value("Hello"));

		mockMvc.perform(get("/api/posts/" + postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(postId));

		mockMvc.perform(
						delete("/api/posts/" + postId).header("Authorization", "Bearer " + access))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/posts/" + postId))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("작성자가 아닌 사용자는 수정·삭제 불가 (403)")
	void forbiddenWhenNotAuthor() throws Exception {
		String authorToken = registerAccessToken("owner@example.com", "password12", "Owner");
		String otherToken = registerAccessToken("other@example.com", "password12", "Other");

		MvcResult createResult =
				mockMvc.perform(
								post("/api/posts")
										.header("Authorization", "Bearer " + authorToken)
										.contentType(APPLICATION_JSON)
										.content(
												"""
												{"title":"Mine","content":"Secret"}
												"""))
						.andExpect(status().isOk())
						.andReturn();
		String postId =
				objectMapper
						.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
						.get("id")
						.asText();

		mockMvc.perform(
						patch("/api/posts/" + postId)
								.header("Authorization", "Bearer " + otherToken)
								.contentType(APPLICATION_JSON)
								.content("{\"title\":\"Hacked\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(containsString("본인")));

		mockMvc.perform(
						delete("/api/posts/" + postId).header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isForbidden());
	}

	private String registerAccessToken(String email, String password, String name) throws Exception {
		String json =
				"{\"email\":\""
						+ email
						+ "\",\"password\":\""
						+ password
						+ "\",\"name\":\""
						+ name.replace("\"", "\\\"")
						+ "\"}";
		MvcResult r =
				mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(json))
						.andExpect(status().isOk())
						.andReturn();
		return objectMapper
				.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8))
				.get("accessToken")
				.asText();
	}
}
