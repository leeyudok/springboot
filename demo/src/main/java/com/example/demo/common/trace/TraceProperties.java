package com.example.demo.common.trace;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 거래추적 설정 ({@code app.trace.*}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.trace")
public class TraceProperties {

    /** 추적 ID 를 주고받는 헤더명 */
    private String header = "X-Trace-Id";

    /**
     * 클라이언트 IP 로 신뢰할 헤더명. <b>비우면 헤더를 일절 신뢰하지 않고 TCP 접속 주소만 사용한다.</b>
     *
     * <p>기본값이 {@code X-Real-IP} 인 이유 — 리버스프록시가 <b>덮어쓰는</b> 헤더만 신뢰할 수 있기 때문이다.
     * {@code X-Forwarded-For} 는 표준상 기존 값 뒤에 이어 붙이는(append) 헤더라,
     * 클라이언트가 미리 넣어 보낸 값이 맨 앞에 남는다. 이걸 그대로 클라이언트 IP 로 쓰면
     * <ul>
     *   <li>감사로그의 접속 IP 를 임의로 위조할 수 있고,</li>
     *   <li>IP 기준 차단·유량제어를 우회하거나, 반대로 타인의 IP 를 지목해 잠그는 서비스 거부가 가능하다.</li>
     * </ul>
     *
     * <p>따라서 <b>프록시가 이 헤더를 무조건 덮어쓰도록 설정되어 있을 때만</b> 값을 지정한다.
     * nginx 예시: {@code proxy_set_header X-Real-IP $remote_addr;}
     * (클라이언트가 보낸 값을 무시하고 실제 접속 주소로 덮어쓴다)
     *
     * <p>프록시 없이 직접 노출되는 환경이라면 반드시 빈 값으로 두어 위조를 원천 차단한다.
     */
    private String clientIpHeader = "X-Real-IP";
}
