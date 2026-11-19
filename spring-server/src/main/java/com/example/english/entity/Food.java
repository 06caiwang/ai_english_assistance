package com.example.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`food`")
public class Food {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** drawable 资源名，如 food_bread */
    private String image;
    /** 恢复的饥饿值点数 */
    private Integer restore;
    private Integer price;
    private String description;
}
