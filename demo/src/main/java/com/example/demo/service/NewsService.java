package com.example.demo.service;

import com.example.demo.dto.NewsArticle;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class NewsService {

    public List<NewsArticle> getRealtimeNews() {
        List<NewsArticle> news = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Simulate 10 news articles
        for (int i = 1; i <= 10; i++) {
            String title = "Simulated Naver News Article " + i;
            String link = "https://news.naver.com/main/read.naver?mode=LSD&mid=shm&sid1=100&oid=000&aid=000000000" + i;
            String description = "This is a simulated description for Naver News Article " + i + ". It covers various topics.";
            String pubDate = LocalDateTime.now().minusMinutes(10 - i).format(formatter); // Simulate different publication times

            news.add(new NewsArticle(title, link, description, pubDate));
        }
        return news;
    }
}
