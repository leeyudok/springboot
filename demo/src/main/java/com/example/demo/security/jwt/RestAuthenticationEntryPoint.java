package com.example.demo.security.jwt;

import com.example.demo.common.error.ErrorCode;
import com.example.demo.common.error.ErrorCodeSpec;
import com.example.demo.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 미인증 요청 처리기 (401).
 *
 * <p>시큐리티 필터 단계에서 발생하므로 {@code @RestControllerAdvice} 가 잡지 못한다.
 * 여기서 직접 공통 응답({@link ApiResponse}) 형태로 내려 클라이언트가 보는 포맷을 통일한다.
 * 토큰 만료/위조 구분은 {@link JwtAuthenticationFilter} 가 남긴 요청 속성에서 가져온다.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.ATTR_AUTH_ERROR);
        ErrorCodeSpec errorCode = (attribute instanceof ErrorCodeSpec) ? (ErrorCodeSpec) attribute : ErrorCode.UNAUTHENTICATED;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
