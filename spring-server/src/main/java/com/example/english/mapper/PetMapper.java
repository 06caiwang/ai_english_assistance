package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.Pet;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PetMapper extends BaseMapper<Pet> {

    @Select("SELECT * FROM `pet` WHERE user_id = #{userId}")
    Pet selectByUserId(@Param("userId") Long userId);
}
