package com.example.english.dto;

import lombok.Data;

/** 背包查询的行对象（MyBatis 映射用，转成 record 再返回给客户端）。 */
@Data
public class BagFoodRow {
    private Long foodId;
    private String name;
    private String image;
    private Integer restore;
    private Integer count;
}
