package com.example.english.common;

import java.util.Locale;

/**
 * 游戏规则集中在这里，全部是纯函数，方便单测。
 *
 * 饥饿值口径（整改后）：**只有学习会消耗**，不再随时间自然掉落。
 * 理由：这是个学习 App，宠物饿不饿应该由"你学了多少"决定；
 * 按时间掉会导致"三天没打开，回来宠物快饿死"，既不合理也不好演示。
 */
public final class GameRules {

    /** 复习间隔天数，索引即 stage */
    public static final int[] REVIEW_DAYS = {0, 1, 3, 7, 15};

    public static final int HUNGER_MAX = 100;
    /** 每通过一个关卡消耗的饥饿值：10 词 × 3 关 = 60 点/天 */
    public static final int HUNGER_COST_PER_LEVEL = 2;

    public static final int EXP_PER_LEVEL = 50;

    /** 金币：认识 1 / 缺空 1 / 完整 2 → 10 词全过 = 40 币/天 */
    public static final int COIN_KNOW = 1;
    public static final int COIN_FILL = 1;
    public static final int COIN_SPELL = 2;

    /** 经验：认识 2 / 缺空 3 / 完整 5 */
    public static final int EXP_KNOW = 2;
    public static final int EXP_FILL = 3;
    public static final int EXP_SPELL = 5;

    public static final String MODE_KNOW = "know";
    public static final String MODE_FILL = "fill";
    public static final String MODE_SPELL = "spell";

    private GameRules() {
    }

    /** 按复习阶段决定今天要走哪几关：新词三关，熟悉后只验拼写 */
    public static String[] modesForStage(int stage) {
        return switch (stage) {
            case 0 -> new String[]{MODE_KNOW, MODE_FILL, MODE_SPELL};
            case 1, 2 -> new String[]{MODE_FILL, MODE_SPELL};
            case 3 -> new String[]{MODE_SPELL};
            default -> new String[]{MODE_KNOW};
        };
    }

    /** 只有走完最后一关才算真正掌握，才推进复习阶段 */
    public static boolean isLastMode(int stage, String mode) {
        String[] modes = modesForStage(stage);
        return modes[modes.length - 1].equals(mode);
    }

    public static int coinFor(String mode) {
        return switch (mode) {
            case MODE_SPELL -> COIN_SPELL;
            case MODE_FILL -> COIN_FILL;
            default -> COIN_KNOW;
        };
    }

    public static int expFor(String mode) {
        return switch (mode) {
            case MODE_SPELL -> EXP_SPELL;
            case MODE_FILL -> EXP_FILL;
            default -> EXP_KNOW;
        };
    }

    public static int clampHunger(int hunger) {
        return Math.max(0, Math.min(HUNGER_MAX, hunger));
    }

    public static int levelOf(int exp) {
        return exp / EXP_PER_LEVEL + 1;
    }

    public static int nextStageOnCorrect(int stage) {
        return Math.min(stage + 1, REVIEW_DAYS.length - 1);
    }

    public static int reviewIntervalDays(int stage) {
        int s = Math.max(0, Math.min(stage, REVIEW_DAYS.length - 1));
        return REVIEW_DAYS[s];
    }

    /**
     * 缺空提示：保留首字母和末字母，中间遮住 ceil(n/2) 个。
     * 用 wordId 做种子，保证同一个词每次遮的位置一样 —— 否则用户会觉得随机、记不住。
     */
    public static String fillHint(String spelling, long seed) {
        if (spelling == null || spelling.isEmpty()) {
            return "";
        }
        int len = spelling.length();
        if (len <= 3) {
            return spelling.replaceAll(".", "_");
        }

        int inner = len - 2;
        int blank = (inner + 1) / 2;

        boolean[] masked = new boolean[len];
        int picked = 0;
        long state = seed * 6364136223846793005L + 1442695040888963407L;
        while (picked < blank) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int pos = 1 + (int) Math.floorMod(state >>> 17, inner);
            if (!masked[pos]) {
                masked[pos] = true;
                picked++;
            }
        }

        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(masked[i] ? '_' : spelling.charAt(i));
        }
        return sb.toString();
    }

    /** 判题：忽略大小写与空格 */
    public static boolean matchSpelling(String spelling, String answer) {
        if (spelling == null || answer == null) {
            return false;
        }
        String a = answer.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        return spelling.toLowerCase(Locale.ROOT).equals(a);
    }
}
