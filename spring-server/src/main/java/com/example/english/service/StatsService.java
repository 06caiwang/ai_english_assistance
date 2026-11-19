package com.example.english.service;

import com.example.english.dto.Dtos;
import com.example.english.mapper.EssayMapper;
import com.example.english.mapper.UserWordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserWordMapper userWordMapper;
    private final EssayMapper essayMapper;

    /**
     * 简化口径：正确率 = 已学词数 / (已学词数 + 累计错误次数)。
     * 没有单独记答题流水，用 wrong_count 当分母的一部分，够演示且不会算出 >1 的值。
     */
    public Dtos.StatsResp stats(Long userId) {
        int learned = userWordMapper.countByUser(userId);
        int wrongs = userWordMapper.sumWrong(userId);
        int attempts = learned + wrongs;
        double accuracy = attempts == 0 ? 0 : Math.min(1.0, (double) learned / attempts);

        return new Dtos.StatsResp(learned, Math.round(accuracy * 100) / 100.0,
                streakDays(userId), essayMapper.countByUser(userId));
    }

    /** 连续学习天数：从今天（或昨天）往前数，断了就停。 */
    int streakDays(Long userId) {
        List<Date> dates = userWordMapper.selectStudyDates(userId);
        if (dates == null || dates.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate cursor;
        LocalDate first = dates.get(0).toLocalDate();
        if (first.equals(today)) {
            cursor = today;
        } else if (first.equals(today.minusDays(1))) {
            cursor = today.minusDays(1);
        } else {
            return 0;
        }

        int streak = 0;
        for (Date d : dates) {
            if (d.toLocalDate().equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}
