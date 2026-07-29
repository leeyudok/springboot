package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 애플리케이션 진입점.
 *
 * <p>계층 구조
 * <ul>
 *   <li>{@code common} — 표준 응답, 에러코드/전역 예외처리, 거래추적, 감사로그, 마스킹, 공통 설정</li>
 *   <li>{@code security} — JWT 발급·검증, 인증/인가 필터, 로그인 API</li>
 *   <li>{@code domain} — 업무 도메인 (도메인별로 controller/service/mapper/dto 를 한 패키지에)</li>
 * </ul>
 *
 * <p>{@link UserDetailsServiceAutoConfiguration} 은 제외한다. 기동할 때마다 임의 비밀번호를 가진
 * 인메모리 사용자({@code user})가 만들어지는데, 인증은 {@code member} 테이블 기반이므로 불필요하고
 * 운영 로그에 자격증명이 찍히는 것도 바람직하지 않다.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
