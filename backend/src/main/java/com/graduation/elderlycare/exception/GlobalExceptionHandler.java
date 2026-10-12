package com.graduation.elderlycare.exception;

import com.graduation.elderlycare.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        System.err.println("全局异常处理: " + e.getClass().getName());
        System.err.println("异常信息: " + e.getMessage());
        
        // 打印完整堆栈
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
            System.err.println("  at " + stackTrace[i]);
        }
        
        return Result.error("服务器内部错误: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        e.printStackTrace();
        return Result.error("参数错误: " + e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleNullPointerException(NullPointerException e) {
        e.printStackTrace();
        System.err.println("空指针异常: " + e.getMessage());
        return Result.error("数据处理错误，请检查必填字段");
    }
}

