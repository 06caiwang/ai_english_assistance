package com.example.english.service;

import com.example.english.dto.Dtos;
import com.example.english.entity.Essay;
import com.example.english.entity.EssayGenerate;
import com.example.english.entity.TranslateRecord;
import com.example.english.mapper.EssayGenerateMapper;
import com.example.english.mapper.EssayMapper;
import com.example.english.mapper.TranslateRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史记录：三个模块的写入与查询都在这里。
 *
 * 两条原则：
 * 1. 写入失败不能影响主流程 —— 用户查词成功，不该因为"记历史失败"而报错；
 * 2. 只记成功结果 —— AI 报错时不写，避免历史里出现一堆空记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_SIZE = 50;

    private final TranslateRecordMapper translateMapper;
    private final EssayMapper essayMapper;
    private final EssayGenerateMapper generateMapper;
    private final ObjectMapper objectMapper;

    // ---------------- 写入 ----------------

    public void saveTranslate(Long userId, String text, Dtos.TranslateResp resp) {
        try {
            TranslateRecord r = new TranslateRecord();
            r.setUserId(userId);
            r.setText(cut(text, 255));
            r.setMeaning(cut(resp.meaning(), 500));
            r.setPhonetic(cut(resp.phonetic(), 100));
            r.setPos(cut(resp.pos(), 50));
            r.setExample(cut(resp.example(), 500));
            r.setSource(resp.source());
            translateMapper.upsert(r);
        } catch (Exception e) {
            log.warn("save translate record failed: {}", e.toString());
        }
    }

    public void saveGenerate(Long userId, String topic, Dtos.GenerateReq req, Dtos.GenerateResp resp) {
        try {
            EssayGenerate g = new EssayGenerate();
            g.setUserId(userId);
            g.setTopic(cut(topic, 100));
            g.setGenre(req.genre());
            g.setTargetWords(req.targetWords());
            g.setLevel(req.level());
            g.setEssay(resp.essay());
            g.setWordCount(resp.wordCount());
            g.setKeywordsJson(toJson(resp.keyWords()));
            g.setCreatedAt(LocalDateTime.now());
            generateMapper.insert(g);
        } catch (Exception e) {
            log.warn("save generate record failed: {}", e.toString());
        }
    }

    // ---------------- 查询 ----------------

    public Dtos.HistorySummary summary(Long userId) {
        return new Dtos.HistorySummary(
                translateMapper.countByUser(userId),
                essayMapper.countByUser(userId),
                generateMapper.countByUser(userId));
    }

    public Dtos.HistoryPage<Dtos.TranslateRecordItem> translate(Long userId, int page, int size) {
        int p = Math.max(1, page);
        int s = clampSize(size);
        int total = translateMapper.countByUser(userId);
        List<Dtos.TranslateRecordItem> items = new ArrayList<>();
        for (TranslateRecord r : translateMapper.selectPage(userId, s, (p - 1) * s)) {
            items.add(new Dtos.TranslateRecordItem(r.getId(), r.getText(), r.getMeaning(),
                    r.getPhonetic(), r.getPos(), r.getExample(), r.getSource(), fmt(r.getCreatedAt())));
        }
        return new Dtos.HistoryPage<>(total, p, s, items);
    }

    public Dtos.HistoryPage<Dtos.EssayRecordItem> essay(Long userId, int page, int size) {
        int p = Math.max(1, page);
        int s = clampSize(size);
        int total = essayMapper.countByUser(userId);
        List<Dtos.EssayRecordItem> items = new ArrayList<>();
        for (Essay e : essayMapper.selectPage(userId, s, (p - 1) * s)) {
            items.add(new Dtos.EssayRecordItem(e.getId(), e.getContent(),
                    e.getScore() == null ? 0 : e.getScore(), e.getComment(),
                    e.getDimsJson(), e.getDeductionsJson(), fmt(e.getCreatedAt())));
        }
        return new Dtos.HistoryPage<>(total, p, s, items);
    }

    public Dtos.HistoryPage<Dtos.GenerateRecordItem> generate(Long userId, int page, int size) {
        int p = Math.max(1, page);
        int s = clampSize(size);
        int total = generateMapper.countByUser(userId);
        List<Dtos.GenerateRecordItem> items = new ArrayList<>();
        for (EssayGenerate g : generateMapper.selectPage(userId, s, (p - 1) * s)) {
            items.add(new Dtos.GenerateRecordItem(g.getId(), g.getTopic(), g.getEssay(),
                    g.getWordCount() == null ? 0 : g.getWordCount(), g.getGenre(), g.getLevel(),
                    levelText(g.getLevel()), g.getKeywordsJson(), fmt(g.getCreatedAt())));
        }
        return new Dtos.HistoryPage<>(total, p, s, items);
    }

    // ---------------- 工具 ----------------

    private int clampSize(int size) {
        return size <= 0 ? 20 : Math.min(size, MAX_SIZE);
    }

    private String fmt(LocalDateTime t) {
        return t == null ? "" : t.format(FMT);
    }

    private String cut(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    private String levelText(String level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case "junior" -> "初中";
            case "cet4" -> "四级";
            default -> "高中";
        };
    }
}
