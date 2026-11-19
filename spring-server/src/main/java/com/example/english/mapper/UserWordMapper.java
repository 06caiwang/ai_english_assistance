package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.UserWord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Date;
import java.util.List;

public interface UserWordMapper extends BaseMapper<UserWord> {

    @Select("SELECT * FROM `user_word` WHERE user_id = #{userId} AND word_id = #{wordId}")
    UserWord selectOne(@Param("userId") Long userId, @Param("wordId") Long wordId);

    @Select("SELECT COUNT(*) FROM `user_word` WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(wrong_count), 0) FROM `user_word` WHERE user_id = #{userId}")
    int sumWrong(@Param("userId") Long userId);

    /** 最近有学习行为的日期，倒序，用于算连续天数 */
    @Select("""
            SELECT DISTINCT DATE(updated_at) AS d FROM `user_word`
            WHERE user_id = #{userId} ORDER BY d DESC LIMIT 60
            """)
    List<Date> selectStudyDates(@Param("userId") Long userId);
}
