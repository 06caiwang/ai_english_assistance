package com.example.english.service;

import com.example.english.common.BizException;
import com.example.english.common.GameRules;
import com.example.english.dto.Dtos;
import com.example.english.entity.Pet;
import com.example.english.entity.User;
import com.example.english.entity.UserWord;
import com.example.english.entity.Word;
import com.example.english.mapper.UserMapper;
import com.example.english.mapper.UserWordMapper;
import com.example.english.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WordService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DAILY_SIZE = 10;

    private final WordMapper wordMapper;
    private final UserWordMapper userWordMapper;
    private final UserMapper userMapper;
    private final PetService petService;

    /** 今日单词 = 到期待复习的词 + 补足的新词，每个词带上今天要走的题型 */
    public List<Dtos.WordItem> today(Long userId) {
        List<Dtos.WordItem> result = new ArrayList<>();

        List<Word> due = wordMapper.selectDueWords(userId, DAILY_SIZE);
        for (Word w : due) {
            UserWord uw = userWordMapper.selectOne(userId, w.getId());
            int stage = uw == null || uw.getStage() == null ? 0 : uw.getStage();
            result.add(toItem(w, stage, false));
        }

        if (result.size() < DAILY_SIZE) {
            List<Word> fresh = wordMapper.selectNewWords(userId, DAILY_SIZE - result.size());
            for (Word w : fresh) {
                result.add(toItem(w, 0, true));
            }
        }
        return result;
    }

    private Dtos.WordItem toItem(Word w, int stage, boolean isNew) {
        String hint = GameRules.fillHint(w.getSpelling(), w.getId());
        List<String> modes = Arrays.asList(GameRules.modesForStage(stage));
        return new Dtos.WordItem(w.getId(), w.getSpelling(), w.getPhonetic(),
                w.getMeaning(), w.getExample(), stage, isNew, hint, modes);
    }

    /**
     * 答题：三种题型统一入口。
     * 关键规则：
     * 1. 每个关卡消耗 2 点饥饿值（这就是"背单词会掉饥饿值"）；
     * 2. 饥饿值为 0 时仍可答题、仍给金币，但不给经验（学不进去，但不会死锁）；
     * 3. 只有走完该词今天的最后一关才推进复习阶段，前面几关不推进。
     */
    @Transactional
    public Dtos.AnswerResp answer(Long userId, Dtos.AnswerReq req) {
        Word word = wordMapper.selectById(req.wordId());
        if (word == null) {
            throw new BizException("单词不存在");
        }

        String mode = req.mode() == null ? GameRules.MODE_KNOW : req.mode();
        boolean correct = GameRules.MODE_KNOW.equals(mode)
                ? Boolean.TRUE.equals(req.correct())
                : GameRules.matchSpelling(word.getSpelling(), req.answer());

        LocalDateTime now = LocalDateTime.now();
        UserWord uw = userWordMapper.selectOne(userId, req.wordId());
        boolean isNewRecord = uw == null;
        if (isNewRecord) {
            uw = new UserWord();
            uw.setUserId(userId);
            uw.setWordId(req.wordId());
            uw.setStage(0);
            uw.setWrongCount(0);
        }

        int stage = uw.getStage() == null ? 0 : uw.getStage();
        int newStage;
        LocalDateTime nextReview;

        if (correct) {
            if (GameRules.isLastMode(stage, mode)) {
                newStage = GameRules.nextStageOnCorrect(stage);
                nextReview = now.plusDays(GameRules.reviewIntervalDays(newStage));
            } else {
                // 中间的关卡过了，阶段不动，今天没过完，明天接着来
                newStage = stage;
                nextReview = now.plusDays(1);
            }
        } else {
            newStage = 0;
            nextReview = now.plusDays(1);
            uw.setWrongCount((uw.getWrongCount() == null ? 0 : uw.getWrongCount()) + 1);
        }
        uw.setStage(newStage);
        uw.setNextReviewAt(nextReview);
        uw.setUpdatedAt(now);
        if (isNewRecord) {
            userWordMapper.insert(uw);
        } else {
            userWordMapper.updateById(uw);
        }

        // 宠物：先算能不能学到东西（饥饿值为 0 时不给经验）
        Pet pet = petService.requirePet(userId);
        int hungerBefore = GameRules.clampHunger(pet.getHunger() == null ? 100 : pet.getHunger());
        boolean canLearn = hungerBefore > 0;

        int coinGain = correct ? GameRules.coinFor(mode) : 0;
        int expGain = (correct && canLearn) ? GameRules.expFor(mode) : 0;

        int hungerAfter = GameRules.clampHunger(hungerBefore - GameRules.HUNGER_COST_PER_LEVEL);
        int expAfter = (pet.getExp() == null ? 0 : pet.getExp()) + expGain;
        pet.setHunger(hungerAfter);
        pet.setExp(expAfter);
        pet.setLevel(GameRules.levelOf(expAfter));
        petService.update(pet);

        userMapper.addCoin(userId, coinGain);
        User user = userMapper.selectById(userId);

        String message = !correct ? "答错了，正确答案是 " + word.getSpelling()
                : (!canLearn ? "答对了，但它饿得没力气，这次没有经验" : "答对了");

        return new Dtos.AnswerResp(correct, word.getSpelling(), coinGain, expGain,
                user.getCoin(), expAfter, pet.getLevel(), hungerAfter,
                newStage, nextReview.format(FMT), message);
    }
}
