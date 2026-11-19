package com.example.english.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * 调 AI 服务专用。读超时必须 ≥10 秒，否则模型还没算完这边就超时了；
     * 连接超时 3 秒是为了在 8081 没启动时快速失败，而不是让用户干等。
     */
    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        // 作文批改要跑 5 次模型调用（四步图），真实模型实测 20-25 秒；
        // 原来给 10 秒，翻译（1 次调用）能过、作文必超时，是最容易误判成"AI 挂了"的坑
        factory.setReadTimeout(120_000);
        return new RestTemplate(factory);
    }
}
