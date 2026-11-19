package com.example.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`user_food`")
public class UserFood {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long foodId;
    private Integer count;

    public UserFood() {
    }

    public UserFood(Long userId, Long foodId, Integer count) {
        this.userId = userId;
        this.foodId = foodId;
        this.count = count;
    }
}
