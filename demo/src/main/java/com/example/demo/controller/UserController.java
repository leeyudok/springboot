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

@Slf4j
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "사용자 전체목록 조회", description = "ㅋ Returns a list of all users.")
    @ApiResponse(responseCode = "200", description = "성공")
    @GetMapping("/users")
    public List<User> getUsers() {
        List<User> users = userService.getUsers();
        users.forEach(user -> log.info("User: {}", user.getName()));
        return users;
    }

    @Operation(summary = "Generate users.json file", description = "Generates a JSON file containing all users.")
    @GetMapping("/gen-users")
    public String generateUsersFile() {
        List<User> users = userService.getUsers();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Use writerWithDefaultPrettyPrinter() for a nicely formatted JSON file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("users.json"), users);
            return "users.json file created successfully.";
        } catch (IOException e) {
            log.error("Error creating users.json file", e);
            return "Error creating users.json file: " + e.getMessage();
        }
    }
}
