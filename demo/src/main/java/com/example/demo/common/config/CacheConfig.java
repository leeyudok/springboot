package com.example.demo.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 캐시 설정 (Caffeine, 로컬 인메모리).
 *
 * <p>캐시마다 유효기간이 달라야 하므로 <b>용도별 매니저를 나눠 둔다</b>.
 * 하나의 매니저에 TTL 을 하나만 두면 "10분이면 충분한 것"과 "1분도 길다"가 같은 값을 쓰게 된다.
 * 사용할 때 {@code @Cacheable(cacheManager = "...", cacheNames = "...")} 로 지정한다.
 *
 * <pre>
 * &#64;Cacheable(cacheManager = CacheConfig.SHORT_TERM, cacheNames = "memberSummary", key = "#memberId")
 * public MemberSummary getSummary(Long memberId) { ... }
 * </pre>
 *
 * <p><b>주의</b> — 인스턴스별 로컬 캐시다. 여러 대로 스케일아웃하면 인스턴스마다 값이 다를 수 있고
 * 무효화도 전파되지 않는다. 정합성이 중요한 데이터는 캐시하지 말거나 Redis 같은 공유 캐시로 옮긴다.
 *
 * <p>Caffeine 은 2.9.x 로 고정한다 — 3.x 는 Java 11 이상을 요구한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 짧은 주기로 바뀌는 조회 결과 (1분) */
    public static final String SHORT_TERM = "shortTermCacheManager";
    /** 준정적 데이터 — 코드값, 설정 등 (1시간) */
    public static final String LONG_TERM = "longTermCacheManager";

    @Bean
    @Primary
    public CacheManager shortTermCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .recordStats());
        return manager;
    }

    @Bean
    public CacheManager longTermCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(5_000)
                .recordStats());
        return manager;
    }
}
