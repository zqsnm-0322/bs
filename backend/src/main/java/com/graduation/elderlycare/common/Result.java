package com.graduation.elderlycare.common;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class Result<T> {
    private Integer code; // 200成功, 401未授权, 500失败
    
    @JsonProperty("msg")  // JSON序列化时使用 msg
    private String message;
    
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(msg);
        return result;
    }

    public static <T> Result<T> unauthorized(String msg) {
        Result<T> result = new Result<>();
        result.setCode(401);
        result.setMessage(msg);
        return result;
    }
}

