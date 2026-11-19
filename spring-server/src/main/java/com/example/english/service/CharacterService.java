package com.example.english.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.english.common.BizException;
import com.example.english.dto.Dtos;
import com.example.english.entity.CharacterEntity;
import com.example.english.entity.Pet;
import com.example.english.entity.User;
import com.example.english.entity.UserCharacter;
import com.example.english.mapper.CharacterMapper;
import com.example.english.mapper.UserCharacterMapper;
import com.example.english.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterMapper characterMapper;
    private final UserCharacterMapper userCharacterMapper;
    private final UserMapper userMapper;
    private final PetService petService;

    /** 形象列表：带上"是否已解锁"和"是否当前在用"，客户端不用自己算。 */
    public List<Dtos.CharacterItem> list(Long userId) {
        List<CharacterEntity> all = characterMapper.selectList(
                Wrappers.<CharacterEntity>lambdaQuery().orderByAsc(CharacterEntity::getId));
        Set<Long> owned = new HashSet<>(userCharacterMapper.selectOwnedIds(userId));
        Pet pet = petService.requirePet(userId);

        List<Dtos.CharacterItem> result = new ArrayList<>();
        for (CharacterEntity c : all) {
            int price = c.getPrice() == null ? 0 : c.getPrice();
            result.add(new Dtos.CharacterItem(
                    c.getId(), c.getName(), c.getImage(), price,
                    owned.contains(c.getId()),
                    c.getId().equals(pet.getCharacterId())));
        }
        return result;
    }

    /**
     * 金币解锁。先条件扣钱再插记录，顺序反了会出现"记录插了钱没扣"的脏数据。
     */
    @Transactional
    public Dtos.UnlockResp unlock(Long userId, Long characterId) {
        CharacterEntity c = petService.characterOrThrow(characterId);
        if (userCharacterMapper.countOwned(userId, characterId) > 0) {
            throw new BizException("这个形象已经解锁过了");
        }
        int price = c.getPrice() == null ? 0 : c.getPrice();
        if (price <= 0) {
            // price=0 的形象注册时就送了，走到这里说明数据不一致
            throw new BizException("该形象为默认形象，无需解锁");
        }

        int rows = userMapper.deductCoin(userId, price);
        if (rows == 0) {
            throw new BizException("金币不足，还差 " + (price - coinOf(userId)) + " 个");
        }
        try {
            userCharacterMapper.insert(new UserCharacter(userId, characterId));
        } catch (DuplicateKeyException e) {
            throw new BizException("这个形象已经解锁过了");
        }

        return new Dtos.UnlockResp(coinOf(userId), characterId, c.getName());
    }

    /** 切换形象：只能切已解锁的，只改 pet.character_id。 */
    public Dtos.SwitchResp switchTo(Long userId, Long characterId) {
        CharacterEntity c = petService.characterOrThrow(characterId);
        if (userCharacterMapper.countOwned(userId, characterId) == 0) {
            throw new BizException("还没解锁这个形象");
        }
        Pet pet = petService.requirePet(userId);
        pet.setCharacterId(characterId);
        petService.update(pet);
        return new Dtos.SwitchResp(characterId, c.getImage());
    }

    private int coinOf(Long userId) {
        User u = userMapper.selectById(userId);
        return u == null || u.getCoin() == null ? 0 : u.getCoin();
    }
}
