package com.example.demo.common.mask;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 로그 출력 직전에 민감정보를 마스킹하는 Logback 레이아웃.
 *
 * <p>애플리케이션 코드가 실수로 주민번호·계좌번호를 로그에 찍더라도 파일/콘솔에는 마스킹된 값만 남는다.
 * 예외 스택트레이스까지 포함한 <b>최종 렌더링 문자열</b>에 적용되므로 누락 지점이 없다.
 *
 * <p>{@code logback-spring.xml} 에서 {@code LayoutWrappingEncoder} 의 layout 으로 지정한다.
 */
public class MaskingPatternLayout extends PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        return SensitiveMasker.maskAll(super.doLayout(event));
    }
}
