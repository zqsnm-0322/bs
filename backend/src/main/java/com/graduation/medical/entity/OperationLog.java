package com.graduation.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 记录系统关键操作，确保数据安全可追溯
 */
@Data
@TableName("sys_operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 操作人ID
     */
    private Long userId;
    
    /**
     * 操作人姓名
     */
    private String userName;
    
    /**
     * 操作模块
     */
    private String module;
    
    /**
     * 操作类型：CREATE, UPDATE, DELETE, QUERY, EXPORT
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 请求方法
     */
    private String method;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * 请求参数
     */
    private String requestParams;
    
    /**
     * 返回结果
     */
    private String response;
    
    /**
     * 操作IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 操作状态：SUCCESS, FAILURE
     */
    private String status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
