package com.example.demo.common.trace;

import com.example.demo.common.mask.SensitiveMasker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 요청/응답 로깅 필터.
 *
 * <p>거래 1건마다 진입·종료 로그를 한 쌍으로 남긴다. 본문은 마스킹 후 일정 길이까지만 기록하고,
 * 파일 업로드·바이너리 응답은 본문을 남기지 않는다.
 *
 * <p>{@link TraceIdFilter} 바로 뒤에 두어 모든 라인이 추적 ID 를 갖도록 한다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    /** 로그에 남길 본문 최대 길이 */
    private static final int MAX_BODY_LENGTH = 2000;

    /** 본문 로깅 제외 경로 — 헬스체크/문서/정적 리소스 */
    private static final String[] EXCLUDED_PREFIXES = {
            "/actuator", "/swagger-ui", "/v3/api-docs", "/favicon.ico", "/css", "/js", "/images"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : EXCLUDED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        long startedAt = System.currentTimeMillis();

        log.info("[REQ ] {} {}{} ip={}",
                request.getMethod(),
                request.getRequestURI(),
                queryString(request),
                TraceContext.getClientIp());

        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long elapsed = System.currentTimeMillis() - startedAt;

            if (log.isDebugEnabled()) {
                String requestBody = readBody(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding());
                if (StringUtils.hasText(requestBody)) {
                    log.debug("[REQ ] body={}", requestBody);
                }
            }

            log.info("[RES ] {} {} status={} elapsed={}ms",
                    request.getMethod(), request.getRequestURI(), responseWrapper.getStatus(), elapsed);

            if (log.isDebugEnabled() && isTextResponse(responseWrapper)) {
                String responseBody = readBody(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding());
                if (StringUtils.hasText(responseBody)) {
                    log.debug("[RES ] body={}", responseBody);
                }
            }

            // 캐싱된 응답 본문을 실제 스트림으로 흘려보낸다 — 호출 누락 시 응답이 비어버린다.
            responseWrapper.copyBodyToResponse();
        }
    }

    private String queryString(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? "" : "?" + SensitiveMasker.maskAll(query);
    }

    private boolean isTextResponse(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        return contentType != null
                && (contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)
                || contentType.startsWith(MediaType.TEXT_PLAIN_VALUE));
    }

    private String readBody(byte[] content, String encoding) {
        if (content == null || content.length == 0) {
            return null;
        }
        Charset charset = encoding == null ? Charset.forName("UTF-8") : Charset.forName(encoding);
        int length = Math.min(content.length, MAX_BODY_LENGTH);
        String body = new String(content, 0, length, charset).replaceAll("\\s+", " ");
        if (content.length > MAX_BODY_LENGTH) {
            body = body + "...(truncated, total=" + content.length + " bytes)";
        }
        return SensitiveMasker.maskAll(body);
    }
}
