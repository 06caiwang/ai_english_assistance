package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.UserCharacter;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserCharacterMapper extends BaseMapper<UserCharacter> {

    @Select("SELECT character_id FROM `user_character` WHERE user_id = #{userId}")
    List<Long> selectOwnedIds(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM `user_character` WHERE user_id = #{userId} AND character_id = #{characterId}")
    int countOwned(@Param("userId") Long userId, @Param("characterId") Long characterId);
}
