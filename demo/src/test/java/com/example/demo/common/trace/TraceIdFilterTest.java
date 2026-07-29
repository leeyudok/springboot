package com.example.demo.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 거래추적 필터 단위테스트 — 특히 클라이언트 IP 판별의 위조 내성을 검증한다.
 */
class TraceIdFilterTest {

    private static final String REMOTE_ADDR = "10.0.0.9";

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    private TraceIdFilter filterWith(String clientIpHeader) {
        TraceProperties properties = new TraceProperties();
        properties.setClientIpHeader(clientIpHeader);
        return new TraceIdFilter(properties);
    }

    /** 필터를 태우고, 체인 내부에서 MDC 에 담긴 값을 꺼내 돌려준다. */
    private String[] runFilter(TraceIdFilter filter, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] captured = new String[3];
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
                captured[0] = TraceContext.getTraceId();
                captured[1] = TraceContext.getClientIp();
            }
        };
        filter.doFilter(request, response, chain);
        captured[2] = response.getHeader("X-Trace-Id");
        return captured;
    }

    @Test
    @DisplayName("X-Forwarded-For 는 신뢰하지 않는다 — 위조해도 접속 주소가 쓰인다")
    void ignoresForwardedForHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setRemoteAddr(REMOTE_ADDR);
        request.addHeader("X-Forwarded-For", "1.2.3.4");   // 클라이언트가 임의로 붙인 값

        String[] result = runFilter(filterWith("X-Real-IP"), request);

        assertThat(result[1]).isEqualTo(REMOTE_ADDR);
    }

    @Test
    @DisplayName("신뢰 헤더로 지정한 값만 클라이언트 IP 로 인정한다")
    void usesConfiguredTrustedHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setRemoteAddr(REMOTE_ADDR);
        request.addHeader("X-Real-IP", "203.0.113.7");

        String[] result = runFilter(filterWith("X-Real-IP"), request);

        assertThat(result[1]).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("신뢰 헤더에 값이 여러 개면 접속 주소로 되돌린다")
    void fallsBackWhenTrustedHeaderHasMultipleValues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setRemoteAddr(REMOTE_ADDR);
        request.addHeader("X-Real-IP", "1.2.3.4, 203.0.113.7");

        String[] result = runFilter(filterWith("X-Real-IP"), request);

        assertThat(result[1]).isEqualTo(REMOTE_ADDR);
    }

    @Test
    @DisplayName("신뢰 헤더에 IP 가 아닌 값이 오면 접속 주소로 되돌린다")
    void fallsBackWhenTrustedHeaderIsNotAnIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setRemoteAddr(REMOTE_ADDR);
        request.addHeader("X-Real-IP", "'; DROP TABLE audit_log; --");

        String[] result = runFilter(filterWith("X-Real-IP"), request);

        assertThat(result[1]).isEqualTo(REMOTE_ADDR);
    }

    @Test
    @DisplayName("신뢰 헤더를 비우면 어떤 헤더도 보지 않는다")
    void ignoresAllHeadersWhenTrustedHeaderIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setRemoteAddr(REMOTE_ADDR);
        request.addHeader("X-Real-IP", "203.0.113.7");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        String[] result = runFilter(filterWith(""), request);

        assertThat(result[1]).isEqualTo(REMOTE_ADDR);
    }

    @Test
    @DisplayName("추적 ID 는 계승하고 응답 헤더에도 실린다")
    void inheritsInboundTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setRemoteAddr(REMOTE_ADDR);
        request.addHeader("X-Trace-Id", "abc-123");

        String[] result = runFilter(filterWith("X-Real-IP"), request);

        assertThat(result[0]).isEqualTo("abc-123");
        assertThat(result[2]).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("추적 ID 에 제어문자가 섞여 오면 새로 발급한다 (로그 인젝션 방지)")
    void reissuesTraceIdWhenInboundIsUnsafe() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setRemoteAddr(REMOTE_ADDR);
        request.addHeader("X-Trace-Id", "abc\n[FAKE] injected log line");

        String[] result = runFilter(filterWith("X-Real-IP"), request);

        assertThat(result[0]).doesNotContain("injected").hasSize(32);
    }

    @Test
    @DisplayName("요청이 끝나면 MDC 를 비운다 — 스레드 재사용 시 오염 방지")
    void clearsContextAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        request.setRemoteAddr(REMOTE_ADDR);

        runFilter(filterWith("X-Real-IP"), request);

        assertThat(TraceContext.getTraceId()).isNull();
        assertThat(TraceContext.getClientIp()).isNull();
    }
}
