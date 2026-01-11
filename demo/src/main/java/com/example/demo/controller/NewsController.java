package com.example.demo.controller;

import com.example.demo.dto.NewsArticle;
import com.example.demo.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @Operation(summary = "Get real-time news", description = "Returns a list of real-time news articles.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of news articles.")
    @GetMapping("/news")
    public List<NewsArticle> getRealtimeNews() {
        return newsService.getRealtimeNews();
    }
}
