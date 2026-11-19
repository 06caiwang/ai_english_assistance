package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.CharacterEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CharacterMapper extends BaseMapper<CharacterEntity> {

    /** 注册时取一个免费形象作为默认形象 */
    @Select("SELECT * FROM `character` WHERE price = 0 ORDER BY id ASC LIMIT 1")
    CharacterEntity selectDefault();

    @Select("SELECT * FROM `character` WHERE id = #{id}")
    CharacterEntity selectByIdManual(@Param("id") Long id);
}
