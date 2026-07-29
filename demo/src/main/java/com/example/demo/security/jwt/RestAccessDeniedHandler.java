package com.example.demo.security.jwt;

import com.example.demo.common.error.ErrorCode;
import com.example.demo.common.response.ApiResponse;
import com.example.demo.common.trace.TraceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 권한 부족 요청 처리기 (403).
 *
 * <p>인가 위반은 내부 통제상 추적 대상이므로 WARN 으로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("[AUTH] access denied. actor={} uri={}", TraceContext.getUserId(), request.getRequestURI());

        response.setStatus(ErrorCode.ACCESS_DENIED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(ErrorCode.ACCESS_DENIED));
    }
}
