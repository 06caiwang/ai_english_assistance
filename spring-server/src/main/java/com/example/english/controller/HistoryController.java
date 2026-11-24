package com.example.english.controller;

import com.example.english.common.Result;
import com.example.english.dto.Dtos;
import com.example.english.security.UserContext;
import com.example.english.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/summary")
    public Result<Dtos.HistorySummary> summary() {
        return Result.ok(historyService.summary(UserContext.get()));
    }

    @GetMapping("/translate")
    public Result<Dtos.HistoryPage<Dtos.TranslateRecordItem>> translate(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(historyService.translate(UserContext.get(), page, size));
    }

    @GetMapping("/essay")
    public Result<Dtos.HistoryPage<Dtos.EssayRecordItem>> essay(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(historyService.essay(UserContext.get(), page, size));
    }

    @GetMapping("/generate")
    public Result<Dtos.HistoryPage<Dtos.GenerateRecordItem>> generate(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(historyService.generate(UserContext.get(), page, size));
    }
}
