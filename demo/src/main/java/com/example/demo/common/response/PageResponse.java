package com.example.demo.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

/**
 * 목록 조회 공통 페이징 응답.
 *
 * <p>MyBatis 는 JPA 의 {@code Page} 같은 표준 타입이 없으므로 전 도메인이 이 타입을 공유한다.
 * 총건수는 별도 count 쿼리 결과를 넘겨받는다.
 */
@Getter
@Schema(description = "페이징 응답")
public class PageResponse<T> {

    @Schema(description = "현재 페이지 번호 (1부터)", example = "1")
    private final int page;

    @Schema(description = "페이지 당 건수", example = "20")
    private final int size;

    @Schema(description = "전체 건수", example = "137")
    private final long totalElements;

    @Schema(description = "전체 페이지 수", example = "7")
    private final int totalPages;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private final boolean hasNext;

    @Schema(description = "조회 결과 목록")
    private final List<T> content;

    private PageResponse(int page, int size, long totalElements, List<T> content) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        this.hasNext = page < this.totalPages;
        this.content = content;
    }

    public static <T> PageResponse<T> of(int page, int size, long totalElements, List<T> content) {
        return new PageResponse<T>(page, size, totalElements, content);
    }
}
