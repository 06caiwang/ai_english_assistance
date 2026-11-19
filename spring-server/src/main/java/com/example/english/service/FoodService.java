package com.example.english.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.english.common.BizException;
import com.example.english.common.GameRules;
import com.example.english.dto.BagFoodRow;
import com.example.english.dto.Dtos;
import com.example.english.entity.Food;
import com.example.english.entity.Pet;
import com.example.english.entity.User;
import com.example.english.entity.UserFood;
import com.example.english.mapper.FoodMapper;
import com.example.english.mapper.UserFoodMapper;
import com.example.english.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 食物与背包：金币买食物 → 食物进背包 → 喂食消耗库存。
 * 三点必须守住：
 * 1. 扣金币、扣库存都用"条件更新"，让数据库保证不会扣成负数；
 * 2. 喂食消耗库存而不是金币（金币在购买时已经花掉了）；
 * 3. 饥饿值满时拒绝喂食，避免浪费，也让"restored"永远等于真实恢复量。
 */
@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodMapper foodMapper;
    private final UserFoodMapper userFoodMapper;
    private final UserMapper userMapper;
    private final PetService petService;
    private final CharacterService characterService;

    /** 食物列表：带上背包里已有的数量，商城和背包共用 */
    public List<Dtos.FoodItem> list(Long userId) {
        List<Food> all = foodMapper.selectList(
                Wrappers.<Food>lambdaQuery().orderByAsc(Food::getId));
        List<Dtos.FoodItem> result = new ArrayList<>();
        for (Food f : all) {
            Integer c = userFoodMapper.selectCount(userId, f.getId());
            result.add(new Dtos.FoodItem(f.getId(), f.getName(), f.getImage(),
                    f.getRestore(), f.getPrice(), f.getDescription(), c == null ? 0 : c));
        }
        return result;
    }

    /** 购买：先条件扣币，再加库存（库存记录不存在则插入） */
    @Transactional
    public Dtos.BuyFoodResp buy(Long userId, Long foodId, int count) {
        Food food = foodMapper.selectById(foodId);
        if (food == null) {
            throw new BizException("食物不存在");
        }
        int total = food.getPrice() * count;
        int rows = userMapper.deductCoin(userId, total);
        if (rows == 0) {
            User u = userMapper.selectById(userId);
            int lack = total - (u == null || u.getCoin() == null ? 0 : u.getCoin());
            throw new BizException("金币不足，还差 " + lack + " 个");
        }

        int updated = userFoodMapper.addCount(userId, foodId, count);
        if (updated == 0) {
            userFoodMapper.insertNew(userId, foodId, count);
        }

        User after = userMapper.selectById(userId);
        Integer now = userFoodMapper.selectCount(userId, foodId);
        return new Dtos.BuyFoodResp(after.getCoin(), foodId, now == null ? 0 : now, total);
    }

    /** 背包：食物（有库存的）+ 已解锁形象 */
    public Dtos.BagResp bag(Long userId) {
        List<BagFoodRow> rows = userFoodMapper.selectBag(userId);
        List<Dtos.BagFood> foods = new ArrayList<>();
        for (BagFoodRow r : rows) {
            foods.add(new Dtos.BagFood(r.getFoodId(), r.getName(), r.getImage(),
                    r.getRestore(), r.getCount()));
        }

        List<Dtos.BagCharacter> chars = new ArrayList<>();
        for (Dtos.CharacterItem c : characterService.list(userId)) {
            if (c.owned()) {
                chars.add(new Dtos.BagCharacter(c.id(), c.name(), c.image(), c.current()));
            }
        }
        return new Dtos.BagResp(foods, chars);
    }

    /** 喂食：扣 1 个库存，按食物恢复值回血，上限 100 */
    @Transactional
    public Dtos.FeedResp feed(Long userId, Long foodId) {
        Food food = foodMapper.selectById(foodId);
        if (food == null) {
            throw new BizException("食物不存在");
        }

        Pet pet = petService.requirePet(userId);
        LocalDateTime now = LocalDateTime.now();
        int current = GameRules.clampHunger(pet.getHunger() == null ? 100 : pet.getHunger());
        if (current >= 100) {
            throw new BizException("它已经吃饱了，别浪费食物");
        }

        // 先扣库存再算恢复：库存不足直接失败，不会出现"喂了但没扣"的账
        int deducted = userFoodMapper.deductOne(userId, foodId);
        if (deducted == 0) {
            throw new BizException("这个食物没有了，去商城买点吧");
        }

        int restored = Math.min(food.getRestore(), 100 - current);
        int target = current + restored;
        pet.setHunger(target);
        pet.setLastFedAt(now);   // 只用于展示"上次喂食"，不再参与计算
        petService.update(pet);

        Integer left = userFoodMapper.selectCount(userId, foodId);
        return new Dtos.FeedResp(target, restored, left == null ? 0 : left, pet.getName());
    }

    /** 注册赠送：给 2 个入门食物，避免新用户一进来就饿着 */
    @Transactional
    public void giftForNewUser(Long userId) {
        Food food = foodMapper.selectCheapestRestore();
        if (food == null) {
            return;
        }
        userFoodMapper.insert(new UserFood(userId, food.getId(), 2));
    }
}
