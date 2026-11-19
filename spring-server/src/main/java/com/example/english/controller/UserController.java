package com.example.english.controller;

import com.example.english.common.Result;
import com.example.english.dto.Dtos;
import com.example.english.security.UserContext;
import com.example.english.service.AuthService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    public Result<Dtos.UserInfo> me() {
        return Result.ok(authService.me(UserContext.get()));
    }

    @PutMapping("/nickname")
    public Result<Dtos.UserInfo> nickname(@Valid @RequestBody Dtos.NicknameReq req) {
        return Result.ok(authService.updateNickname(UserContext.get(), req.nickname()));
    }
}
