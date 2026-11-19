package com.example.english.controller;

import com.example.english.common.Result;
import com.example.english.dto.Dtos;
import com.example.english.security.UserContext;
import com.example.english.service.FoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping("/foods")
    public Result<List<Dtos.FoodItem>> list() {
        return Result.ok(foodService.list(UserContext.get()));
    }

    @PostMapping("/foods/buy")
    public Result<Dtos.BuyFoodResp> buy(@Valid @RequestBody Dtos.BuyFoodReq req) {
        return Result.ok(foodService.buy(UserContext.get(), req.foodId(), req.count()));
    }

    @GetMapping("/bag")
    public Result<Dtos.BagResp> bag() {
        return Result.ok(foodService.bag(UserContext.get()));
    }
}
