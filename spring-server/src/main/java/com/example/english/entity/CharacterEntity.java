package com.example.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`character`")
public class CharacterEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** drawable 资源名，如 char_panda，不存 URL */
    private String image;
    /** 0 = 注册即送 */
    private Integer price;
}
