package com.example.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user_word`")
public class UserWord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordId;
    /** 复习阶段 0-4，对应间隔 0/1/3/7/15 天 */
    private Integer stage;
    private LocalDateTime nextReviewAt;
    private Integer wrongCount;
    private LocalDateTime updatedAt;
}
