package com.example.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user_character`")
public class UserCharacter {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long characterId;
    private LocalDateTime unlockedAt;

    public UserCharacter() {
    }

    public UserCharacter(Long userId, Long characterId) {
        this.userId = userId;
        this.characterId = characterId;
    }
}
