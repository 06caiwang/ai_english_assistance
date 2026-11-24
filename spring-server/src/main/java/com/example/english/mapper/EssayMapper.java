package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.Essay;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EssayMapper extends BaseMapper<Essay> {

    @Select("SELECT COUNT(*) FROM `essay` WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);

    @Select("""
            SELECT * FROM `essay`
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<Essay> selectPage(@Param("userId") Long userId,
                           @Param("size") int size,
                           @Param("offset") int offset);
}
