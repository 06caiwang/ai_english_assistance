package com.example.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.english.entity.TranslateRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TranslateRecordMapper extends BaseMapper<TranslateRecord> {

    /**
     * 幂等写入：同一个用户查同一个词只留一条，重复查询时刷新结果与时间。
     * 依赖表上的 uk_user_text 唯一索引，并发也不会插重。
     */
    @Insert("""
            INSERT INTO `translate_record`
                (user_id, `text`, meaning, phonetic, pos, example, source, created_at)
            VALUES
                (#{userId}, #{text}, #{meaning}, #{phonetic}, #{pos}, #{example}, #{source}, NOW())
            ON DUPLICATE KEY UPDATE
                meaning = VALUES(meaning),
                phonetic = VALUES(phonetic),
                pos = VALUES(pos),
                example = VALUES(example),
                source = VALUES(source),
                created_at = NOW()
            """)
    int upsert(TranslateRecord record);

    @Select("""
            SELECT * FROM `translate_record`
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<TranslateRecord> selectPage(@Param("userId") Long userId,
                                     @Param("size") int size,
                                     @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM `translate_record` WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);
}
