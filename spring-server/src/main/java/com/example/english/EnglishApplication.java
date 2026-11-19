package com.example.english;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.english.mapper")
public class EnglishApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnglishApplication.class, args);
    }
}
