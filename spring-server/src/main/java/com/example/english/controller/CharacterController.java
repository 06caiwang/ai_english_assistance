package com.example.english.controller;

import com.example.english.common.Result;
import com.example.english.dto.Dtos;
import com.example.english.security.UserContext;
import com.example.english.service.CharacterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public Result<List<Dtos.CharacterItem>> list() {
        return Result.ok(characterService.list(UserContext.get()));
    }

    @PostMapping("/unlock")
    public Result<Dtos.UnlockResp> unlock(@Valid @RequestBody Dtos.CharacterReq req) {
        return Result.ok(characterService.unlock(UserContext.get(), req.characterId()));
    }
}
