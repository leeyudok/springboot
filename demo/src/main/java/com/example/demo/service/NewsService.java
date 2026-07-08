package com.example.demo.service;

import com.example.demo.dto.NewsArticle;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 뉴스 데이터 서비스 — 네이버 뉴스 형태의 시뮬레이션 기사를 생성한다.
 */
@Service
public class NewsService {

    private static final DateTimeFormatter PUB_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int ARTICLE_COUNT = 10;

    /** 시뮬레이션 뉴스 기사 목록을 반환한다 (발행시각은 1분 간격 과거로 분산). */
    public List<NewsArticle> getRealtimeNews() {
        List<NewsArticle> news = new ArrayList<>();
        for (int i = 1; i <= ARTICLE_COUNT; i++) {
            String title = "Simulated Naver News Article " + i;
            String link = "https://news.naver.com/main/read.naver?mode=LSD&mid=shm&sid1=100&oid=000&aid=000000000" + i;
            String description = "This is a simulated description for Naver News Article " + i + ". It covers various topics.";
            String pubDate = LocalDateTime.now().minusMinutes(ARTICLE_COUNT - i).format(PUB_DATE_FORMAT);

            news.add(new NewsArticle(title, link, description, pubDate));
        }
        return news;
    }
}
