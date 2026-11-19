package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.Essay;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface EssayMapper extends BaseMapper<Essay> {

    @Select("SELECT COUNT(*) FROM `essay` WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);
}
