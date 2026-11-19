package com.example.english.controller;

import com.example.english.common.Result;
import com.example.english.dto.Dtos;
import com.example.english.security.UserContext;
import com.example.english.service.CharacterService;
import com.example.english.service.FoodService;
import com.example.english.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final CharacterService characterService;
    private final FoodService foodService;

    @GetMapping
    public Result<Dtos.PetResp> detail() {
        return Result.ok(petService.detail(UserContext.get()));
    }

    /** 喂食：必须指定食物，按食物恢复值回血、扣背包库存 */
    @PostMapping("/feed")
    public Result<Dtos.FeedResp> feed(@Valid @RequestBody Dtos.FeedReq req) {
        return Result.ok(foodService.feed(UserContext.get(), req.foodId()));
    }

    @PostMapping("/character")
    public Result<Dtos.SwitchResp> switchCharacter(@Valid @RequestBody Dtos.CharacterReq req) {
        return Result.ok(characterService.switchTo(UserContext.get(), req.characterId()));
    }
}
