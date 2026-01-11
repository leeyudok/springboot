package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import java.util.Map;

@RestController
public class HelloController {

    @Operation(summary = "Hello World", description = "Returns a simple 'Hello, World!' message.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the message.")
    @GetMapping("/")
    public Map<String, String> hello() {
        return Collections.singletonMap("message", "Hello, World!");
    }

}
