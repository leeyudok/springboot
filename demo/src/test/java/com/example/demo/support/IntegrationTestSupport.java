package com.example.demo.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 통합테스트 공통 베이스.
 *
 * <p>실제 PostgreSQL 컨테이너를 띄우고 Flyway 마이그레이션·시드까지 그대로 적용한다.
 * H2 같은 대체 DB 를 쓰지 않는 이유는 {@code FOR UPDATE}·{@code numeric} 정밀도·CHECK 제약 동작이
 * 운영 DB 와 달라 <b>테스트는 통과하는데 운영에서 깨지는</b> 상황을 만들기 때문이다.
 *
 * <p><b>싱글턴 컨테이너 패턴</b>을 쓴다. {@code @Testcontainers} + {@code @Container} 조합은
 * 테스트 <i>클래스마다</i> 컨테이너를 종료하는데, 스프링 컨텍스트(그리고 커넥션 풀)는 클래스 간에 재사용되므로
 * 두 번째 클래스부터는 이미 죽은 DB 를 붙잡고 커넥션 타임아웃이 난다.
 * 여기서는 JVM 당 한 번만 기동하고 종료 훅에서 정리한다.
 *
 * <p>Podman 사용 시 {@code DOCKER_HOST} 환경변수 설정이 필요하다 — README 참고.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIf(value = "com.example.demo.support.DockerAvailability#isAvailable",
        disabledReason = "Docker/Podman 런타임이 없어 통합테스트를 건너뜁니다.")
public abstract class IntegrationTestSupport {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("demodb")
                    .withUsername("demo")
                    .withPassword("demo1234");

    static {
        // 런타임이 없으면 기동 자체를 시도하지 않는다 (@EnabledIf 로 테스트는 건너뛴다).
        if (DockerAvailability.isAvailable()) {
            POSTGRES.start();
            Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop));
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** 로그인해서 액세스 토큰을 얻는다. 인증이 필요한 시나리오의 공통 준비 단계. */
    protected String loginAndGetAccessToken(String loginId, String password) throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }
}
