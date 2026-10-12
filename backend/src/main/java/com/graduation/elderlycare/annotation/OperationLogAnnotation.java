package com.graduation.elderlycare.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogAnnotation {
    
    /**
     * 操作模块
     */
    String module() default "";
    
    /**
     * 操作类型
     */
    String operationType() default "";
    
    /**
     * 操作描述
     */
    String description() default "";
    
    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;
    
    /**
     * 是否记录返回结果
     */
    boolean recordResponse() default false;
}

