package com.example.english.controller;

import com.example.english.common.Result;
import com.example.english.dto.Dtos;
import com.example.english.security.UserContext;
import com.example.english.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/translate")
    public Result<Dtos.TranslateResp> translate(@Valid @RequestBody Dtos.TranslateReq req) {
        return Result.ok(aiService.translate(req.text()));
    }

    @PostMapping("/essay/generate")
    public Result<Dtos.GenerateResp> generate(@Valid @RequestBody Dtos.GenerateReq req) {
        return Result.ok(aiService.generate(req.topic(), req.genre(), req.targetWords(), req.level()));
    }

    @PostMapping("/essay")
    public Result<Dtos.EssayResp> essay(@Valid @RequestBody Dtos.EssayReq req) {
        return Result.ok(aiService.essay(UserContext.get(), req.content()));
    }
}
