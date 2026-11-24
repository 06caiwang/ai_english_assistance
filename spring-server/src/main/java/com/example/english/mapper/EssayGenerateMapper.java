package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.EssayGenerate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EssayGenerateMapper extends BaseMapper<EssayGenerate> {

    @Select("""
            SELECT * FROM `essay_generate`
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<EssayGenerate> selectPage(@Param("userId") Long userId,
                                   @Param("size") int size,
                                   @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM `essay_generate` WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);
}
