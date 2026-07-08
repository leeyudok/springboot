package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import java.util.Map;

/**
 * 루트 경로 헬스체크 겸 인사말 API.
 */
@RestController
public class HelloController {

    /** Hello, World 메시지를 반환한다. */
    @Operation(summary = "Hello World", description = "Returns a simple 'Hello, World!' message.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the message.")
    @GetMapping("/")
    public Map<String, String> hello() {
        return Collections.singletonMap("message", "Hello, World!");
    }

}
