package com.example.demo.common.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * JSON 직렬화 설정.
 *
 * <p>금융 데이터 특성상 두 가지가 중요하다.
 * <ul>
 *   <li><b>날짜/시간</b> — ISO 타임스탬프(숫자) 대신 {@code yyyy-MM-dd HH:mm:ss} 문자열로 내려
 *       채널·로그·전문 간 표기를 통일한다.</li>
 *   <li><b>금액</b> — {@link BigDecimal} 을 문자열로 직렬화한다. JavaScript 클라이언트가
 *       숫자로 받으면 double 로 변환되며 정밀도가 깨진다.</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.modules(new JavaTimeModule());
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.featuresToEnable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
            // 금액 정밀도 보존 — BigDecimal 은 JSON 문자열로 내보낸다.
            builder.serializerByType(BigDecimal.class, new com.fasterxml.jackson.databind.ser.std.ToStringSerializer());
        };
    }
}
