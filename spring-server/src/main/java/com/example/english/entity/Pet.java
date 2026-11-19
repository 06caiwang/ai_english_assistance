package com.example.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`pet`")
public class Pet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    /** 库里存的是上次已知值，对外一律按 lastFedAt 现算 */
    private Integer hunger;
    private Integer level;
    private Integer exp;
    private Long characterId;
    private LocalDateTime lastFedAt;
}
