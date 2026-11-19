package com.example.english.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** 所有出入参集中在这里，避免十几个只有三个字段的小文件。 */
public final class Dtos {

    private Dtos() {
    }

    // ---------- 鉴权 ----------
    public record RegisterReq(
            @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 20, message = "用户名 3-20 位") String username,
            @NotBlank(message = "密码不能为空") @Size(min = 6, max = 32, message = "密码 6-32 位") String password,
            String nickname) {
    }

    public record LoginReq(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record UserInfo(Long id, String username, String nickname, Integer coin) {
    }

    public record AuthResp(String token, UserInfo user) {
    }

    // ---------- 单词 ----------
    /** fillHint：缺空提示，如 "ab_n__n"；modes：今天这个词要走的关卡顺序 */
    public record WordItem(Long wordId, String spelling, String phonetic, String meaning,
                           String example, int stage, boolean isNew,
                           String fillHint, List<String> modes) {
    }

    /** know 用 correct；fill / spell 用 answer（整词），服务端统一判题 */
    public record AnswerReq(@NotNull(message = "wordId 不能为空") Long wordId,
                            String mode,
                            String answer,
                            Boolean correct) {
    }

    public record AnswerResp(boolean correct, String correctAnswer, int coinGained, int expGained,
                             int coin, int exp, int level, int hunger,
                             int stage, String nextReviewAt, String message) {
    }

    // ---------- 宠物 ----------
    public record PetResp(String name, int hunger, int level, int exp,
                          Long characterId, String characterImage, String characterName,
                          String lastFedAt) {
    }

    // 注：FeedResp 在「食物 / 背包」段统一定义（喂食已改为按食物恢复、扣库存）

    // ---------- 形象 ----------
    public record CharacterItem(Long id, String name, String image, int price,
                                boolean owned, boolean current) {
    }

    public record CharacterReq(@NotNull(message = "characterId 不能为空") Long characterId) {
    }

    public record UnlockResp(int coin, Long characterId, String name) {
    }

    public record SwitchResp(Long characterId, String characterImage) {
    }

    // ---------- AI ----------
    public record TranslateReq(@NotBlank(message = "text 不能为空") @Size(max = 500) String text) {
    }

    public record TranslateResp(String meaning, String phonetic, String example,
                                String pos, String source) {
    }

    public record EssayReq(@NotBlank(message = "content 不能为空") @Size(max = 5000) String content) {
    }

    public record Deduction(String type, String sentence, String fix, int points) {
    }

    public record EssayResp(int score, Map<String, Integer> dimensions,
                            List<Deduction> deductions, int wordCount, String lengthNote,
                            String level, List<String> suggestions, List<String> trace,
                            String source) {
    }

    public record GenerateReq(@NotBlank(message = "topic 不能为空") @Size(max = 100) String topic,
                              String genre, Integer targetWords, String level) {
    }

    public record KeyWord(String word, String meaning) {
    }

    public record GenerateResp(String title, String essay, List<String> outline, int wordCount,
                               List<KeyWord> keyWords, List<String> goodSentences,
                               List<String> trace, String source) {
    }

    // ---------- 食物 / 背包 ----------
    public record FoodItem(Long id, String name, String image, int restore, int price,
                           String description, int count) {
    }

    public record BuyFoodReq(@NotNull(message = "foodId 不能为空") Long foodId,
                             @NotNull(message = "数量不能为空")
                             @jakarta.validation.constraints.Min(value = 1, message = "至少买 1 个")
                             @jakarta.validation.constraints.Max(value = 99, message = "一次最多 99 个")
                             Integer count) {
    }

    public record BuyFoodResp(int coin, Long foodId, int foodCount, int spent) {
    }

    public record BagFood(Long foodId, String name, String image, int restore, int count) {
    }

    public record BagCharacter(Long id, String name, String image, boolean current) {
    }

    public record BagResp(List<BagFood> foods, List<BagCharacter> characters) {
    }

    public record FeedReq(@NotNull(message = "foodId 不能为空") Long foodId) {
    }

    public record FeedResp(int hunger, int restored, int foodCount, String petName) {
    }

    public record NicknameReq(@NotBlank(message = "昵称不能为空") @Size(max = 20, message = "昵称最多 20 字") String nickname) {
    }

    // ---------- 统计 ----------
    public record StatsResp(int learnedCount, double accuracy, int streakDays, int essayCount) {
    }
}
