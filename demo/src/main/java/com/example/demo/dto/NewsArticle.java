package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 뉴스 기사 정보 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticle {
    /** 기사 제목 */
    private String title;
    /** 기사 링크 URL */
    private String link;
    /** 기사 요약 */
    private String description;
    /** 발행 일시 (yyyy-MM-dd HH:mm:ss) */
    private String pubDate;
}
