package com.example.demo.controller;

import com.example.demo.dto.User;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 사용자 조회/파일 생성 API.
 */
@Slf4j
@RestController
public class UserController {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UserController(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    /** 사용자 전체 목록을 반환한다. */
    @Operation(summary = "사용자 전체목록 조회", description = "ㅋ Returns a list of all users.")
    @ApiResponse(responseCode = "200", description = "성공")
    @GetMapping("/users")
    public List<User> getUsers() {
        List<User> users = userService.getUsers();
        log.info("Returning {} users", users.size());
        return users;
    }

    /** 사용자 목록을 실행 디렉터리의 {@code users.json} 파일로 저장한다. */
    @Operation(summary = "Generate users.json file", description = "Generates a JSON file containing all users.")
    @GetMapping("/gen-users")
    public String generateUsersFile() {
        List<User> users = userService.getUsers();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("users.json"), users);
            return "users.json file created successfully.";
        } catch (IOException e) {
            log.error("Error creating users.json file", e);
            return "Error creating users.json file: " + e.getMessage();
        }
    }
}
