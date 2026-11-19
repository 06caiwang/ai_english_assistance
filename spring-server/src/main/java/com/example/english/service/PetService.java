package com.example.english.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.english.common.BizException;
import com.example.english.common.GameRules;
import com.example.english.dto.Dtos;
import com.example.english.entity.CharacterEntity;
import com.example.english.entity.Pet;
import com.example.english.mapper.CharacterMapper;
import com.example.english.mapper.PetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PetService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PetMapper petMapper;
    private final CharacterMapper characterMapper;

    public Pet requirePet(Long userId) {
        Pet pet = petMapper.selectByUserId(userId);
        if (pet == null) {
            throw new BizException(5000, "宠物数据缺失，请重新注册");
        }
        return pet;
    }

    /**
     * 饥饿值直接读库：整改后饥饿值只由"答题消耗"和"喂食恢复"两条路径改变，
     * 不再按时间现算，所以这里不需要任何换算，返回的就是真值。
     */
    public Dtos.PetResp detail(Long userId) {
        Pet pet = requirePet(userId);
        int hunger = GameRules.clampHunger(pet.getHunger() == null ? 100 : pet.getHunger());

        String image = "";
        String charName = "";
        CharacterEntity c = characterMapper.selectById(pet.getCharacterId());
        if (c != null) {
            image = c.getImage();
            charName = c.getName();
        }
        return new Dtos.PetResp(pet.getName(), hunger, pet.getLevel(), pet.getExp(),
                pet.getCharacterId(), image, charName,
                pet.getLastFedAt() == null ? null : pet.getLastFedAt().format(FMT));
    }

    public void update(Pet pet) {
        petMapper.updateById(pet);
    }

    CharacterEntity characterOrThrow(Long characterId) {
        CharacterEntity c = characterMapper.selectById(characterId);
        if (c == null) {
            throw new BizException("形象不存在");
        }
        return c;
    }

    String imageOf(Long characterId) {
        CharacterEntity c = characterMapper.selectById(characterId);
        return c == null ? "" : c.getImage();
    }

    Pet findByUser(Long userId) {
        return petMapper.selectOne(Wrappers.<Pet>lambdaQuery().eq(Pet::getUserId, userId));
    }

    /** 喂食/答题后统一写回，避免两处各写一套 */
    void saveHungerAndExp(Pet pet, int hunger, int exp) {
        pet.setHunger(GameRules.clampHunger(hunger));
        pet.setExp(exp);
        pet.setLevel(GameRules.levelOf(exp));
        pet.setLastFedAt(LocalDateTime.now());
        petMapper.updateById(pet);
    }
}
