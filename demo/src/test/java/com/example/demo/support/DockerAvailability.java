package com.example.demo.support;

import org.testcontainers.DockerClientFactory;

/**
 * 컨테이너 런타임 가용 여부 판정.
 *
 * <p>Docker/Podman 이 없는 환경(일부 CI, 로컬 머신)에서 통합테스트가 <b>실패</b>가 아니라
 * <b>건너뜀</b>이 되도록 {@code @EnabledIf} 조건으로 쓴다.
 */
public final class DockerAvailability {

    private DockerAvailability() {
    }

    public static boolean isAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable e) {
            return false;
        }
    }
}
