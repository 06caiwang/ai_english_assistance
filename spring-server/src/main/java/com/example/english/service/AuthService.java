package com.example.english.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.english.common.BizException;
import com.example.english.dto.Dtos;
import com.example.english.entity.CharacterEntity;
import com.example.english.entity.Pet;
import com.example.english.entity.User;
import com.example.english.entity.UserCharacter;
import com.example.english.mapper.CharacterMapper;
import com.example.english.mapper.PetMapper;
import com.example.english.mapper.UserCharacterMapper;
import com.example.english.mapper.UserMapper;
import com.example.english.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final CharacterMapper characterMapper;
    private final UserCharacterMapper userCharacterMapper;
    private final JwtUtil jwtUtil;
    private final FoodService foodService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${app.init.welcome-coin:50}")
    private int welcomeCoin;

    /**
     * 注册：user + pet + 默认形象解锁记录 三条写入，必须在一个事务里。
     * 少写一条会出现「有账号没宠物」这种半残状态，后面所有接口都会空指针。
     */
    @Transactional
    public Dtos.AuthResp register(Dtos.RegisterReq req) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.username()));
        if (exists != null && exists > 0) {
            throw new BizException("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPassword(encoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank()
                ? req.username() : req.nickname());
        user.setCoin(welcomeCoin);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);

        CharacterEntity defaultChar = characterMapper.selectDefault();
        if (defaultChar == null) {
            throw new BizException(5000, "系统缺少默认形象，请先导入 character 种子数据");
        }

        Pet pet = new Pet();
        pet.setUserId(user.getId());
        pet.setName("Luna");
        pet.setHunger(100);
        pet.setLevel(1);
        pet.setExp(0);
        pet.setCharacterId(defaultChar.getId());
        pet.setLastFedAt(LocalDateTime.now());
        petMapper.insert(pet);

        userCharacterMapper.insert(new UserCharacter(user.getId(), defaultChar.getId()));

        // 送 2 个入门食物：新用户一进来就是满饿值，但送点吃的能立刻体验喂食
        foodService.giftForNewUser(user.getId());

        return buildAuth(user);
    }

    public Dtos.AuthResp login(Dtos.LoginReq req) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.username()));
        if (user == null || !encoder.matches(req.password(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        return buildAuth(user);
    }

    /** 设置页改昵称 */
    public Dtos.UserInfo updateNickname(Long userId, String nickname) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        user.setNickname(nickname);
        userMapper.updateById(user);
        return toInfo(user);
    }

    public Dtos.UserInfo me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return toInfo(user);
    }

    private Dtos.AuthResp buildAuth(User user) {
        return new Dtos.AuthResp(jwtUtil.issue(user.getId()), toInfo(user));
    }

    private Dtos.UserInfo toInfo(User user) {
        return new Dtos.UserInfo(user.getId(), user.getUsername(), user.getNickname(), user.getCoin());
    }
}
