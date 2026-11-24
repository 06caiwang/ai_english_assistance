package com.example.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`essay`")
public class Essay {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String content;
    private Integer score;
    private String comment;
    /** 四维分数 JSON，历史详情用 */
    private String dimsJson;
    /** 扣分明细 JSON */
    private String deductionsJson;
    private LocalDateTime createdAt;
}
