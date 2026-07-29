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
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Auditable} 메서드의 실행을 감사 로그로 남기는 어드바이스.
 *
 * <p>성공·실패 모두 기록한다. 실패 시에는 에러코드와 사유를 함께 남겨
 * "누가 어떤 거래를 시도했다가 왜 거절당했는지"까지 추적 가능하게 한다.
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
                    "9000", e.getClass().getSimpleName() + ": " + e.getMessage(), elapsed(startedAt));
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
                .targetId(extractTargetId(joinPoint, auditable.targetExpression()))
                .params(auditable.logParams() ? serializeArgs(joinPoint) : null)
                .result(result)
                .errorCode(errorCode)
                .errorMessage(truncate(SensitiveMasker.maskAll(errorMessage), MAX_ERROR_MESSAGE_LENGTH))
                .elapsedMs(elapsedMs)
                .build();
        auditLogService.record(auditLog);
    }

    /**
     * 감사 대상 식별자를 추출한다.
     *
     * <p>표현식이 숫자면 해당 인덱스 파라미터를, 아니면 각 파라미터에서 동명의 프로퍼티를 찾는다.
     */
    private String extractTargetId(ProceedingJoinPoint joinPoint, String expression) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        Object[] args = joinPoint.getArgs();
        try {
            if (expression.matches("\\d+")) {
                int index = Integer.parseInt(expression);
                return index < args.length && args[index] != null ? String.valueOf(args[index]) : null;
            }
            // 파라미터명 우선 매칭 (-parameters 컴파일 옵션 전제)
            String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    if (expression.equals(parameterNames[i]) && args[i] != null) {
                        return String.valueOf(args[i]);
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
                        return String.valueOf(value);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AUDIT] failed to extract targetId by expression '{}'", expression, e);
        }
        return null;
    }

    /** 파라미터를 JSON 으로 직렬화하고 마스킹한다. */
    private String serializeArgs(ProceedingJoinPoint joinPoint) {
        try {
            String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            Object[] args = joinPoint.getArgs();
            Map<String, Object> params = new HashMap<String, Object>();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (isUnserializable(arg)) {
                    continue;
                }
                String name = (parameterNames != null && i < parameterNames.length) ? parameterNames[i] : ("arg" + i);
                params.put(name, arg);
            }
            return truncate(SensitiveMasker.maskAll(objectMapper.writeValueAsString(params)), MAX_PARAM_LENGTH);
        } catch (Exception e) {
            log.debug("[AUDIT] failed to serialize params", e);
            return null;
        }
    }

    /** 서블릿 객체·스트림 등 직렬화하면 안 되는 인자 판별. */
    private boolean isUnserializable(Object arg) {
        return arg == null
                || arg instanceof javax.servlet.ServletRequest
                || arg instanceof javax.servlet.ServletResponse
                || arg instanceof java.io.InputStream
                || arg instanceof java.io.OutputStream
                || arg instanceof org.springframework.web.multipart.MultipartFile;
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
