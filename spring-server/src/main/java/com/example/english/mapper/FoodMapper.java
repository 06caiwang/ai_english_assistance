package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.Food;
import org.apache.ibatis.annotations.Select;

public interface FoodMapper extends BaseMapper<Food> {

    /** 注册时赠送的入门食物：恢复值最低的那一种 */
    @Select("SELECT * FROM `food` ORDER BY `restore` ASC LIMIT 1")
    Food selectCheapestRestore();
}
