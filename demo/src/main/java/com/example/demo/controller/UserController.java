package com.example.demo.controller;

import com.example.demo.dto.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {

    @GetMapping("/users")
    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        users.add(new User(1L, "User One", "user.one@example.com", "010-1111-1111", "010-1111-1111"));
        users.add(new User(2L, "User Two", "user.two@example.com", "010-2222-2222", "010-2222-2222"));
        users.add(new User(3L, "User Three", "user.three@example.com", "010-3333-3333", "010-3333-3333"));
        return users;
    }
}

