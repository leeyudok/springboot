package com.example.demo.common.trace;

import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 거래추적 ID 필터.
 *
 * <p>요청 헤더({@code X-Trace-Id})에 추적 ID 가 있으면 이어받고, 없으면 새로 발급한다.
 * 상위 시스템·게이트웨이를 거쳐 들어오는 요청의 추적 ID 를 유지해 시스템 간 요청을 한 줄로 꿰기 위함이다.
 * 발급/계승한 값은 MDC 와 응답 헤더 양쪽에 실린다.
 *
 * <p>가장 앞단에 두어야 이후 모든 필터·컨트롤러 로그에 추적 ID 가 찍힌다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TraceIdFilter extends OncePerRequestFilter {

    /** 외부에서 넘어온 추적 ID 의 최대 허용 길이 — 초과 시 신규 발급 */
    private static final int MAX_TRACE_ID_LENGTH = 40;

    /** 추적 ID 허용 문자 — 로그 인젝션(개행·제어문자 주입) 방지 */
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9-]+");

    /** IP 로 인정할 문자 (IPv4/IPv6) — 헤더 값 위생 처리용 */
    private static final Pattern SAFE_IP = Pattern.compile("[0-9a-fA-F.:]{3,45}");

    private final TraceProperties traceProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            TraceContext.setTraceId(resolveTraceId(request));
            TraceContext.setClientIp(resolveClientIp(request));
            response.setHeader(traceProperties.getHeader(), TraceContext.getTraceId());
            chain.doFilter(request, response);
        } finally {
            // 톰캣 스레드는 재사용되므로 반드시 비운다 — 누락 시 다음 요청 로그가 오염된다.
            TraceContext.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String inbound = request.getHeader(traceProperties.getHeader());
        if (inbound != null) {
            String trimmed = inbound.trim();
            if (!trimmed.isEmpty() && trimmed.length() <= MAX_TRACE_ID_LENGTH && SAFE_TRACE_ID.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return TraceContext.newTraceId();
    }

    /**
     * 클라이언트 IP 를 판별한다.
     *
     * <p><b>설정으로 지정한 헤더 하나만</b> 신뢰하고, 없거나 형식이 이상하면 TCP 접속 주소로 되돌린다.
     * 헤더 목록을 순회하며 아무거나 잡는 방식(특히 {@code X-Forwarded-For} 우선)은 쓰지 않는다 —
     * 클라이언트가 임의로 넣어 보낼 수 있어 감사로그 IP 위조와 IP 기준 차단 우회의 통로가 된다.
     * 자세한 근거는 {@link TraceProperties#getClientIpHeader()} 참고.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String headerName = traceProperties.getClientIpHeader();
        if (!StringUtils.hasText(headerName)) {
            return request.getRemoteAddr();
        }
        String value = request.getHeader(headerName);
        if (value == null) {
            return request.getRemoteAddr();
        }
        // 프록시가 덮어쓰는 헤더라도 값이 여러 개면 신뢰 구간을 알 수 없으므로 접속 주소로 되돌린다.
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.indexOf(',') >= 0 || !SAFE_IP.matcher(trimmed).matches()) {
            return request.getRemoteAddr();
        }
        return trimmed;
    }
}
