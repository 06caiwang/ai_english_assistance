package com.example.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 单词查询历史：同一用户同一个词只保留一条（再查则更新时间与结果）。 */
@Data
@TableName("`translate_record`")
public class TranslateRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String text;
    private String meaning;
    private String phonetic;
    private String pos;
    private String example;
    private String source;
    private LocalDateTime createdAt;
}
