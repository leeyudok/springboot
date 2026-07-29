package com.example.demo.security;

import com.example.demo.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증 API 통합테스트 — 로그인, 토큰 사용, 인증 실패 경로를 검증한다.
 */
class AuthApiIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("올바른 자격증명으로 로그인하면 토큰이 발급된다")
    void loginSuccess() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"user01\",\"password\":\"User1234!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andReturn();

        // 추적 ID 가 응답 헤더와 본문 양쪽에 실린다
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("traceId").asText()).isNotEmpty();
        assertThat(result.getResponse().getHeader("X-Trace-Id")).isEqualTo(body.get("traceId").asText());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 과 에러코드 2001 을 반환한다")
    void loginWithWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"user02\",\"password\":\"WrongPass1!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    @DisplayName("존재하지 않는 계정도 동일한 에러코드를 반환한다 (계정 존재 여부 비노출)")
    void loginWithUnknownAccount() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"nosuchuser\",\"password\":\"Whatever1!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    @DisplayName("검증 실패 시 필드별 사유가 함께 내려온다")
    void loginWithBlankId() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("토큰 없이 보호 자원에 접근하면 401")
    void accessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("2000"));
    }

    @Test
    @DisplayName("위조 토큰으로 접근하면 401 과 에러코드 2003")
    void accessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("2003"));
    }

    @Test
    @DisplayName("발급받은 토큰으로 내 정보를 조회하면 마스킹된 값이 내려온다")
    void getMyInfoWithToken() throws Exception {
        String token = loginAndGetAccessToken("user01", "User1234!");

        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value("user01"))
                .andExpect(jsonPath("$.data.memberName").value("홍*동"))
                .andExpect(jsonPath("$.data.cellNo").value("010-****-5678"));
    }

    @Test
    @DisplayName("일반 사용자는 관리자 전용 API 에 접근할 수 없다 (403)")
    void userCannotAccessAdminApi() throws Exception {
        String token = loginAndGetAccessToken("user01", "User1234!");

        mockMvc.perform(get("/api/v1/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("2004"));
    }

    @Test
    @DisplayName("관리자는 회원 목록을 조회할 수 있다")
    void adminCanListMembers() throws Exception {
        String token = loginAndGetAccessToken("admin", "Passw0rd!");

        mockMvc.perform(get("/api/v1/members")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(4));
    }
}
