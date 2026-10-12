package com.graduation.elderlycare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.graduation.elderlycare.mapper")
public class ElderlyCareApplication {
    public static void main(String[] args) {
        SpringApplication.run(ElderlyCareApplication.class, args);
    }
}


