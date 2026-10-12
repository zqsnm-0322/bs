package com.graduation.elderlycare.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Configuration
public class DateFormatConfig {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // 自定义Date反序列化器，支持多种日期格式
        SimpleModule dateModule = new SimpleModule();
        dateModule.addDeserializer(Date.class, new com.fasterxml.jackson.databind.JsonDeserializer<Date>() {
            @Override
            public Date deserialize(com.fasterxml.jackson.core.JsonParser p, 
                                  com.fasterxml.jackson.databind.DeserializationContext ctxt) 
                    throws java.io.IOException {
                String dateStr = p.getText();
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    return null;
                }
                
                // 尝试多种日期格式
                String[] formats = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy/MM/dd"
                };
                
                for (String format : formats) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(format);
                        sdf.setLenient(false);
                        return sdf.parse(dateStr);
                    } catch (ParseException e) {
                        // 继续尝试下一个格式
                    }
                }
                
                throw new java.io.IOException("无法解析日期: " + dateStr);
            }
        });
        
        objectMapper.registerModule(dateModule);
        
        // 配置Java 8时间类型
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, 
            new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT)));
        javaTimeModule.addSerializer(LocalDate.class, 
            new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class, 
            new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class, 
            new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        
        objectMapper.registerModule(javaTimeModule);
        
        // 忽略未知属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 禁用将日期序列化为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 配置日期序列化格式
        objectMapper.setDateFormat(new SimpleDateFormat(DATETIME_FORMAT));
        
        return objectMapper;
    }
}

