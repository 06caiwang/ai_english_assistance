package com.example.english;

import com.example.english.common.GameRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 纯函数单测：不需要 Spring 容器、不需要数据库。 */
class GameRulesTest {

    @Test
    void modesDependOnStage() {
        assertArrayEquals(new String[]{"know", "fill", "spell"}, GameRules.modesForStage(0));
        assertArrayEquals(new String[]{"fill", "spell"}, GameRules.modesForStage(1));
        assertArrayEquals(new String[]{"fill", "spell"}, GameRules.modesForStage(2));
        assertArrayEquals(new String[]{"spell"}, GameRules.modesForStage(3));
        assertArrayEquals(new String[]{"know"}, GameRules.modesForStage(4));
    }

    @Test
    void onlyLastModeAdvancesStage() {
        assertFalse(GameRules.isLastMode(0, "know"));
        assertFalse(GameRules.isLastMode(0, "fill"));
        assertTrue(GameRules.isLastMode(0, "spell"));
        assertTrue(GameRules.isLastMode(3, "spell"));
        assertTrue(GameRules.isLastMode(4, "know"));
    }

    @Test
    void rewardsGrowWithDifficulty() {
        assertEquals(1, GameRules.coinFor("know"));
        assertEquals(1, GameRules.coinFor("fill"));
        assertEquals(2, GameRules.coinFor("spell"));
        assertEquals(2, GameRules.expFor("know"));
        assertEquals(3, GameRules.expFor("fill"));
        assertEquals(5, GameRules.expFor("spell"));
        // 10 词三关全对：金币 40、经验 100
        assertEquals(40, 10 * (GameRules.coinFor("know") + GameRules.coinFor("fill") + GameRules.coinFor("spell")));
        assertEquals(100, 10 * (GameRules.expFor("know") + GameRules.expFor("fill") + GameRules.expFor("spell")));
    }

    @Test
    void hungerClampsBetweenZeroAndHundred() {
        assertEquals(0, GameRules.clampHunger(-8));
        assertEquals(100, GameRules.clampHunger(120));
        assertEquals(56, GameRules.clampHunger(56));
    }

    @Test
    void fillHintMasksDeterministically() {
        String a = GameRules.fillHint("abandon", 7L);
        String b = GameRules.fillHint("abandon", 7L);
        assertEquals(a, b, "同一 wordId 每次必须一致");
        assertEquals(7, a.length());
        assertEquals('a', a.charAt(0), "首字母必须保留");
        assertEquals('n', a.charAt(6), "末字母必须保留");

        long underscores = a.chars().filter(c -> c == '_').count();
        assertEquals(3, underscores, "7 个字母、中间 5 个，遮一半向上取整 = 3");

        // 太短的词退化成全遮
        assertEquals("___", GameRules.fillHint("cat", 1L));
    }

    @Test
    void spellingMatchIgnoresCaseAndSpace() {
        assertTrue(GameRules.matchSpelling("abandon", " abandon "));
        assertTrue(GameRules.matchSpelling("abandon", "Abandon"));
        assertFalse(GameRules.matchSpelling("abandon", "abandons"));
        assertFalse(GameRules.matchSpelling("abandon", null));
    }

    @Test
    void levelGrowsEveryFiftyExp() {
        assertEquals(1, GameRules.levelOf(0));
        assertEquals(1, GameRules.levelOf(49));
        assertEquals(2, GameRules.levelOf(50));
        assertEquals(3, GameRules.levelOf(100));
    }

    @Test
    void reviewStageAdvancesAndCaps() {
        assertEquals(1, GameRules.nextStageOnCorrect(0));
        assertEquals(4, GameRules.nextStageOnCorrect(4));
        assertEquals(0, GameRules.reviewIntervalDays(0));
        assertEquals(15, GameRules.reviewIntervalDays(4));
    }
}
