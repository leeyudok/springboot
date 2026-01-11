package com.example.demo.controller;

import com.example.demo.dto.NewsArticle;
import com.example.demo.service.NewsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/news")
    public List<NewsArticle> getRealtimeNews() {
        return newsService.getRealtimeNews();
    }
}
