package com.example.english.service;

import com.example.english.common.BizException;
import com.example.english.dto.Dtos;
import com.example.english.entity.Essay;
import com.example.english.mapper.EssayMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 转发层。这一层只有两件事：加超时、失败降级。
 * 任何异常都转成 code=5001 的友好提示，绝不让 500 穿透到客户端。
 */
@Slf4j
@Service
public class AiService {

    private final RestTemplate restTemplate;
    private final EssayMapper essayMapper;
    private final String baseUrl;

    public AiService(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                     EssayMapper essayMapper,
                     @Value("${app.ai.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.essayMapper = essayMapper;
        this.baseUrl = baseUrl;
    }

    public Dtos.TranslateResp translate(String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        try {
            Dtos.TranslateResp resp = restTemplate.postForObject(
                    baseUrl + "/ai/translate", body, Dtos.TranslateResp.class);
            if (resp == null || resp.meaning() == null || resp.meaning().isBlank()) {
                throw new BizException(5001, "AI 返回内容为空，请重试");
            }
            return resp;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("translate failed: {}", e.toString());
            throw new BizException(5001, "AI 服务暂时不可用，请稍后再试");
        }
    }

    public Dtos.GenerateResp generate(String topic, String genre, Integer targetWords, String level) {
        Map<String, Object> body = new HashMap<>();
        body.put("topic", topic);
        body.put("genre", genre == null ? "narrative" : genre);
        body.put("target_words", targetWords == null ? 120 : targetWords);
        body.put("level", level == null ? "senior" : level);
        try {
            Dtos.GenerateResp resp = restTemplate.postForObject(
                    baseUrl + "/ai/essay/generate", body, Dtos.GenerateResp.class);
            if (resp == null || resp.essay() == null || resp.essay().isBlank()) {
                throw new BizException(5001, "AI 没有返回作文，请重试");
            }
            return resp;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("generate failed: {}", e.toString());
            throw new BizException(5001, "AI 服务暂时不可用，请稍后再试");
        }
    }

    public Dtos.EssayResp essay(Long userId, String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        Dtos.EssayResp resp;
        try {
            resp = restTemplate.postForObject(baseUrl + "/ai/essay", body, Dtos.EssayResp.class);
        } catch (Exception e) {
            log.warn("essay failed: {}", e.toString());
            throw new BizException(5001, "AI 服务暂时不可用，请稍后再试");
        }
        if (resp == null) {
            throw new BizException(5001, "AI 返回内容为空，请重试");
        }

        // 批改结果落库，失败不影响返回
        try {
            Essay essay = new Essay();
            essay.setUserId(userId);
            essay.setContent(content);
            essay.setScore(resp.score());
            List<String> sugg = resp.suggestions();
            essay.setComment(sugg == null ? "" : String.join("；", sugg));
            essay.setCreatedAt(LocalDateTime.now());
            essayMapper.insert(essay);
        } catch (Exception e) {
            log.warn("save essay failed: {}", e.toString());
        }
        return resp;
    }
}
