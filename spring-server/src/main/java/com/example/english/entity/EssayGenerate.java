package com.example.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 作文生成历史：每次生成都是独立记录。 */
@Data
@TableName("`essay_generate`")
public class EssayGenerate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String topic;
    private String genre;
    private Integer targetWords;
    private String level;
    private String essay;
    private Integer wordCount;
    private String keywordsJson;
    private LocalDateTime createdAt;
}
