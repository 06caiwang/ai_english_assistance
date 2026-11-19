package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 条件扣币：让数据库保证不会扣成负数。
     * 返回 0 表示余额不足，调用方要抛业务异常。
     */
    @Update("UPDATE `user` SET coin = coin - #{price} WHERE id = #{userId} AND coin >= #{price}")
    int deductCoin(@Param("userId") Long userId, @Param("price") int price);

    @Update("UPDATE `user` SET coin = coin + #{delta} WHERE id = #{userId}")
    int addCoin(@Param("userId") Long userId, @Param("delta") int delta);
}
