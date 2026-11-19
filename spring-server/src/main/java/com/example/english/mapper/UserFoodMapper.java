package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.dto.BagFoodRow;
import com.example.english.entity.UserFood;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserFoodMapper extends BaseMapper<UserFood> {

    @Select("""
            SELECT f.id AS foodId, f.name AS name, f.image AS image,
                   f.restore AS restore, uf.count AS count
            FROM `user_food` uf JOIN `food` f ON f.id = uf.food_id
            WHERE uf.user_id = #{userId} AND uf.count > 0
            ORDER BY f.id ASC
            """)
    List<BagFoodRow> selectBag(@Param("userId") Long userId);

    @Select("SELECT `count` FROM `user_food` WHERE user_id = #{userId} AND food_id = #{foodId}")
    Integer selectCount(@Param("userId") Long userId, @Param("foodId") Long foodId);

    /** 条件加库存：记录不存在时返回 0，由调用方插入 */
    @Update("UPDATE `user_food` SET `count` = `count` + #{n} WHERE user_id = #{userId} AND food_id = #{foodId}")
    int addCount(@Param("userId") Long userId, @Param("foodId") Long foodId, @Param("n") int n);

    /** 条件扣库存：库存不足返回 0，这是防超扣的关键 */
    @Update("UPDATE `user_food` SET `count` = `count` - 1 WHERE user_id = #{userId} AND food_id = #{foodId} AND `count` > 0")
    int deductOne(@Param("userId") Long userId, @Param("foodId") Long foodId);

    @Insert("INSERT INTO `user_food` (user_id, food_id, `count`) VALUES (#{userId}, #{foodId}, #{n})")
    int insertNew(@Param("userId") Long userId, @Param("foodId") Long foodId, @Param("n") int n);
}
