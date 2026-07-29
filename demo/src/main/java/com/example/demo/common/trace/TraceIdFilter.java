package com.example.demo.common.trace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 거래추적 ID 필터.
 *
 * <p>요청 헤더({@code X-Trace-Id})에 추적 ID 가 있으면 이어받고, 없으면 새로 발급한다.
 * 채널계·EAI 를 거쳐 들어오는 요청의 추적 ID 를 유지해 시스템 간 거래를 한 줄로 꿰기 위함이다.
 * 발급/계승한 값은 MDC 와 응답 헤더 양쪽에 실린다.
 *
 * <p>가장 앞단에 두어야 이후 모든 필터·컨트롤러 로그에 추적 ID 가 찍힌다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** 외부에서 넘어온 추적 ID 의 최대 허용 길이 — 초과 시 신규 발급 */
    private static final int MAX_TRACE_ID_LENGTH = 40;

    @Value("${app.trace.header:X-Trace-Id}")
    private String traceHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            TraceContext.setTraceId(resolveTraceId(request));
            TraceContext.setClientIp(resolveClientIp(request));
            response.setHeader(traceHeader, TraceContext.getTraceId());
            chain.doFilter(request, response);
        } finally {
            // 톰캣 스레드는 재사용되므로 반드시 비운다 — 누락 시 다음 요청 로그가 오염된다.
            TraceContext.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String inbound = request.getHeader(traceHeader);
        if (inbound != null) {
            String trimmed = inbound.trim();
            // 로그 인젝션 방지 — 영숫자/하이픈만 허용
            if (!trimmed.isEmpty() && trimmed.length() <= MAX_TRACE_ID_LENGTH && trimmed.matches("[A-Za-z0-9-]+")) {
                return trimmed;
            }
        }
        return TraceContext.newTraceId();
    }

    /**
     * 클라이언트 IP 를 판별한다.
     *
     * <p>L4/프록시 뒤에 놓이는 것이 일반적이므로 프록시 헤더를 우선 확인한다.
     * {@code X-Forwarded-For} 는 쉼표로 이어지므로 최초 항목이 원 클라이언트다.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "X-Real-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        return request.getRemoteAddr();
    }
}
