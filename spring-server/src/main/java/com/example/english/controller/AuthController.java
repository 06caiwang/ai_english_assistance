package com.example.english.controller;

import com.example.english.common.Result;
import com.example.english.dto.Dtos;
import com.example.english.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Dtos.AuthResp> register(@Valid @RequestBody Dtos.RegisterReq req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/login")
    public Result<Dtos.AuthResp> login(@Valid @RequestBody Dtos.LoginReq req) {
        return Result.ok(authService.login(req));
    }
}
