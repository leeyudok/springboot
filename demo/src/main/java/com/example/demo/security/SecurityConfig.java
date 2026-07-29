package com.example.demo.security;

import com.example.demo.security.jwt.JwtAuthenticationFilter;
import com.example.demo.security.jwt.JwtTokenProvider;
import com.example.demo.security.jwt.RestAccessDeniedHandler;
import com.example.demo.security.jwt.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 시큐리티 설정.
 *
 * <p>정책
 * <ul>
 *   <li><b>무상태(STATELESS)</b> — 서버 세션을 만들지 않는다. 스케일아웃 시 세션 클러스터링이 불필요하다.</li>
 *   <li><b>CSRF 비활성</b> — 쿠키가 아닌 Authorization 헤더로 토큰을 받으므로 CSRF 표면이 없다.
 *       쿠키 기반으로 바꾸면 반드시 다시 켜야 한다.</li>
 *   <li><b>기본 거부</b> — 명시적으로 permitAll 한 경로 외에는 전부 인증을 요구한다.</li>
 * </ul>
 *
 * <p><b>CORS 와 401 의 상호작용</b> — 브라우저 클라이언트가 붙는 순간 반복해서 터지는 조합이라 근거를 남긴다.
 * <ol>
 *   <li>CORS 는 {@code WebMvcConfigurer#addCorsMappings} 가 아니라 <b>필터</b>로 건다.
 *       여기서는 {@code http.cors()} 가 시큐리티 필터체인 앞쪽에 {@code CorsFilter} 를 심으므로
 *       인증 실패로 응답이 조기 종료돼도 CORS 헤더가 이미 붙어 있다.
 *       MVC 매핑 방식은 요청이 컨트롤러까지 도달해야 적용되므로, 인증 단계에서 끊기면
 *       브라우저에는 CORS 오류로 보인다.</li>
 *   <li>401 응답은 {@link RestAuthenticationEntryPoint} 가 <b>직접 본문을 쓴다</b>.
 *       {@code sendError} 를 쓰면 컨테이너가 {@code /error} 로 forward 하는데,
 *       그 경로는 CORS 매핑 밖이라 응답이 브라우저 단에서 깨진다.</li>
 *   <li>프리플라이트({@code OPTIONS})는 인증 자격을 싣지 않으므로 반드시 열어 둔다 (아래 permitAll).</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)   // 서비스 계층 @PreAuthorize 사용
public class SecurityConfig {

    /** 인증 없이 접근 가능한 경로 */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/favicon.ico",
            "/index.htm"
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().configurationSource(corsConfigurationSource())
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .httpBasic().disable()
                .formLogin().disable()
                .logout().disable()
                .headers()
                    .frameOptions().deny()                       // 클릭재킹 방지
                    .contentTypeOptions().and()                  // MIME 스니핑 방지
                    .cacheControl().and()                        // 인증 응답 캐시 금지
                    .httpStrictTransportSecurity()
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000)
                .and().and()
                .authorizeRequests()
                    .antMatchers(PUBLIC_PATHS).permitAll()
                    .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .antMatchers("/actuator/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                .and()
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 인코더 — BCrypt strength 10.
     *
     * <p>강도를 올리면(12~13) 무차별 대입 저항은 커지지만 로그인 지연도 함께 커진다.
     * 변경 시 기존 해시는 그대로 검증되므로 점진 전환이 가능하다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * CORS 설정.
     *
     * <p>운영에서는 {@code allowedOriginPatterns} 를 실제 채널 도메인으로 좁힐 것.
     * 와일드카드 + credentials 조합은 브라우저가 거부한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("X-Trace-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
