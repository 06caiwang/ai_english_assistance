package com.example.english.controller;

import com.example.english.common.Result;
import com.example.english.dto.Dtos;
import com.example.english.security.UserContext;
import com.example.english.service.WordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @GetMapping("/today")
    public Result<List<Dtos.WordItem>> today() {
        return Result.ok(wordService.today(UserContext.get()));
    }

    @PostMapping("/answer")
    public Result<Dtos.AnswerResp> answer(@Valid @RequestBody Dtos.AnswerReq req) {
        return Result.ok(wordService.answer(UserContext.get(), req));
    }
}
