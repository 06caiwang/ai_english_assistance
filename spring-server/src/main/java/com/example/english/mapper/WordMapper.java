package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.Word;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface WordMapper extends BaseMapper<Word> {

    /** 到期待复习的词，按到期时间升序 */
    @Select("""
            SELECT w.* FROM `word` w
            JOIN `user_word` uw ON uw.word_id = w.id
            WHERE uw.user_id = #{userId} AND uw.next_review_at IS NOT NULL
              AND uw.next_review_at <= NOW()
            ORDER BY uw.next_review_at ASC
            LIMIT #{limit}
            """)
    List<Word> selectDueWords(@Param("userId") Long userId, @Param("limit") int limit);

    /** 还没学过的新词 */
    @Select("""
            SELECT w.* FROM `word` w
            WHERE w.id NOT IN (SELECT word_id FROM `user_word` WHERE user_id = #{userId})
            ORDER BY w.id ASC
            LIMIT #{limit}
            """)
    List<Word> selectNewWords(@Param("userId") Long userId, @Param("limit") int limit);
}
