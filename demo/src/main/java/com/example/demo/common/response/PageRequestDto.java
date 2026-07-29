package com.example.demo.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * 목록 조회 공통 요청 파라미터.
 *
 * <p>MyBatis 쿼리에는 {@link #getOffset()} / {@link #getSize()} 를 그대로 넘긴다.
 * {@code size} 상한을 두어 무제한 조회로 인한 DB 부하를 차단한다.
 */
@Getter
@Setter
@Schema(description = "페이징 요청")
public class PageRequestDto {

    /** 페이지 당 최대 건수 — 초과 요청은 검증에서 거부 */
    public static final int MAX_SIZE = 200;

    @Schema(description = "페이지 번호 (1부터)", example = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Schema(description = "페이지 당 건수 (최대 200)", example = "20")
    @Min(value = 1, message = "페이지 당 건수는 1 이상이어야 합니다.")
    @Max(value = MAX_SIZE, message = "페이지 당 건수는 " + MAX_SIZE + " 이하여야 합니다.")
    private int size = 20;

    /**
     * 정렬 키.
     *
     * <p>컬럼명이 아니라 <b>미리 약속한 키</b>를 받는다. 매퍼 XML 이 {@code <choose>} 로
     * 키를 검사해 정해진 {@code ORDER BY} 만 만들어 낸다.
     *
     * <p>컬럼명·정렬방향을 그대로 받아 SQL 에 이어 붙이면 안 된다. MyBatis 의 {@code #{}} 는
     * 값 바인딩이라 컬럼명 자리에 쓸 수 없고, 그렇다고 {@code ${}} 로 문자열 치환하면
     * 그 순간 SQL 인젝션 통로가 된다. 허용 목록 분기만이 안전한 방법이다.
     */
    @Schema(description = "정렬 키 (도메인별 허용 목록)", example = "recent")
    @Size(max = 30, message = "정렬 키가 올바르지 않습니다.")
    private String sort;

    /** SQL {@code OFFSET} 절에 사용할 값. */
    public int getOffset() {
        return (page - 1) * size;
    }
}
