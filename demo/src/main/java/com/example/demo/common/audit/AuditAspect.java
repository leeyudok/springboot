package com.example.demo.common.audit;

import com.example.demo.common.error.BusinessException;
import com.example.demo.common.mask.SensitiveMasker;
import com.example.demo.common.trace.TraceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link Auditable} 메서드의 실행을 감사 로그로 남기는 어드바이스.
 *
 * <p>성공·실패 모두 기록한다. 실패 시에는 에러코드와 사유를 함께 남겨
 * "누가 어떤 처리를 시도했다가 왜 거절당했는지"까지 추적 가능하게 한다.
 *
 * <p>추적 ID·주체·클라이언트 IP·요청 정보는 <b>요청 스레드에서 미리 읽어</b> 값 객체에 담는다.
 * 실제 적재는 {@link AuditLogService} 가 별도 스레드에서 수행하므로, 그때는 MDC 도
 * {@code RequestContextHolder} 도 비어 있기 때문이다.
 *
 * <p>{@code app.audit.enabled=false} 로 끌 수 있다 (성능 시험 등 예외 상황용).
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditAspect {

    /** params 컬럼 최대 길이 (TEXT 지만 과다 적재 방지) */
    private static final int MAX_PARAM_LENGTH = 4000;
    /** error_message 컬럼 길이 */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    /** 시스템 오류 기본 코드 */
    private static final String SYSTEM_ERROR_CODE = "9000";

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startedAt = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            record(joinPoint, auditable, AuditLog.RESULT_SUCCESS, null, null, elapsed(startedAt));
            return result;
        } catch (BusinessException e) {
            record(joinPoint, auditable, AuditLog.RESULT_FAILURE,
                    e.getErrorCode().getCode(), e.getMessage(), elapsed(startedAt));
            throw e;
        } catch (Throwable e) {
            record(joinPoint, auditable, AuditLog.RESULT_FAILURE,
                    SYSTEM_ERROR_CODE, e.getClass().getSimpleName() + ": " + e.getMessage(), elapsed(startedAt));
            throw e;
        }
    }

    private void record(ProceedingJoinPoint joinPoint, Auditable auditable, String result,
                        String errorCode, String errorMessage, long elapsedMs) {
        HttpServletRequest request = currentRequest();
        AuditLog auditLog = AuditLog.builder()
                .traceId(TraceContext.getTraceId())
                .eventType(auditable.eventType())
                .actor(TraceContext.getUserId())
                .clientIp(TraceContext.getClientIp())
                .httpMethod(request == null ? null : request.getMethod())
                .requestUri(request == null ? null : request.getRequestURI())
                .targetId(resolveValueAsString(joinPoint, auditable.targetExpression()))
                .params(serializeWhitelistedParams(joinPoint, auditable.params()))
                .result(result)
                .errorCode(errorCode)
                .errorMessage(truncate(SensitiveMasker.maskAll(errorMessage), MAX_ERROR_MESSAGE_LENGTH))
                .elapsedMs(elapsedMs)
                .build();
        auditLogService.record(auditLog);
    }

    /**
     * 화이트리스트에 지정된 필드만 뽑아 JSON 으로 만든다.
     *
     * <p>지정되지 않은 값은 애초에 읽지 않으므로, 요청 본문에 실린 비밀값이 감사 테이블로 새지 않는다.
     */
    private String serializeWhitelistedParams(ProceedingJoinPoint joinPoint, String[] fields) {
        if (fields == null || fields.length == 0) {
            return null;
        }
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        for (String field : fields) {
            if (!StringUtils.hasText(field)) {
                continue;
            }
            Object value = resolveValue(joinPoint, field);
            if (value != null) {
                params.put(field, String.valueOf(value));
            }
        }
        if (params.isEmpty()) {
            return null;
        }
        try {
            // 화이트리스트를 통과한 값이라도 마스킹은 그대로 적용한다 (이중 방어).
            return truncate(SensitiveMasker.maskAll(objectMapper.writeValueAsString(params)), MAX_PARAM_LENGTH);
        } catch (Exception e) {
            log.debug("[AUDIT] failed to serialize params", e);
            return null;
        }
    }

    private String resolveValueAsString(ProceedingJoinPoint joinPoint, String expression) {
        Object value = resolveValue(joinPoint, expression);
        return value == null ? null : SensitiveMasker.maskAll(String.valueOf(value));
    }

    /**
     * 표현식으로 파라미터 값을 찾는다.
     *
     * <p>숫자면 해당 인덱스 파라미터, 아니면 파라미터명 → 파라미터 객체의 프로퍼티 순으로 탐색한다.
     */
    private Object resolveValue(ProceedingJoinPoint joinPoint, String expression) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        Object[] args = joinPoint.getArgs();
        try {
            if (expression.matches("\\d+")) {
                int index = Integer.parseInt(expression);
                return index < args.length ? args[index] : null;
            }
            // 파라미터명 우선 매칭 (-parameters 컴파일 옵션 전제)
            String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    if (expression.equals(parameterNames[i]) && args[i] != null) {
                        return args[i];
                    }
                }
            }
            // 파라미터 객체의 프로퍼티 매칭
            for (Object arg : args) {
                if (arg == null || isSimpleType(arg)) {
                    continue;
                }
                BeanWrapperImpl wrapper = new BeanWrapperImpl(arg);
                if (wrapper.isReadableProperty(expression)) {
                    Object value = wrapper.getPropertyValue(expression);
                    if (value != null) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AUDIT] failed to resolve expression '{}'", expression, e);
        }
        return null;
    }

    private boolean isSimpleType(Object value) {
        return value instanceof CharSequence || value instanceof Number
                || value instanceof Boolean || value instanceof Character;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
