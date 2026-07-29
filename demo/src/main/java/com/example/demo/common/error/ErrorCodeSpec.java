package com.example.demo.common.error;

import org.springframework.http.HttpStatus;

/**
 * 에러코드 계약.
 *
 * <p>공통 대역은 {@link ErrorCode} 가 고정으로 갖고, 업무 대역은 도메인마다 이 인터페이스를 구현한
 * 별도 enum 을 둔다. 새 프로젝트는 도메인 enum 만 갈아끼우면 되고 공통 계층은 손대지 않는다.
 *
 * <p>코드 대역 규약
 * <ul>
 *   <li>{@code 00xx} / {@code 10xx} / {@code 20xx} / {@code 90xx} — 공통. {@link ErrorCode} 전용이므로 도메인에서 쓰지 않는다</li>
 *   <li>{@code 30xx} 이상 — 업무 도메인. 도메인마다 대역을 하나씩 배정한다
 *       (예: 회원 {@code 30xx}, 원장 {@code 40xx})</li>
 * </ul>
 *
 * <p>{@link #getMessage()} 는 <b>대외 노출용</b>이다. 내부 원인(스택트레이스, SQL, 키 값)은 절대 담지 말고
 * {@link BusinessException} 의 detail 로 넘겨 로그에만 남긴다.
 */
public interface ErrorCodeSpec {

    /** 업무 코드 (4자리) */
    String getCode();

    /** 매핑할 HTTP 상태 */
    HttpStatus getHttpStatus();

    /** 대외 노출 메시지 */
    String getMessage();
}
