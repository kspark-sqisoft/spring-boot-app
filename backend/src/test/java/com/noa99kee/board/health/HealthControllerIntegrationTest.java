package com.noa99kee.board.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.noa99kee.board.support.AbstractIntegrationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HealthControllerIntegrationTest extends AbstractIntegrationTest {

	@Test
	@DisplayName("GET /api/health 는 인증 없이 200과 status=ok 를 반환한다")
	void healthOk() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"))
				.andExpect(jsonPath("$.timestamp").exists());
	}
}
