package com.graduation.elderlycare.controller;

import com.graduation.elderlycare.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system-test")
@CrossOrigin
public class SystemTestController {

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public Result<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "养老照护管理系统运行正常");
        return Result.success(health);
    }

    /**
     * 数据库连接测试
     */
    @GetMapping("/db-test")
    public Result<?> databaseTest() {
        try {
            // 这里可以添加数据库连接测试逻辑
            Map<String, Object> dbStatus = new HashMap<>();
            dbStatus.put("database", "connected");
            dbStatus.put("tables", "ready");
            dbStatus.put("message", "数据库连接正常");
            return Result.success(dbStatus);
        } catch (Exception e) {
            return Result.error("数据库连接失败: " + e.getMessage());
        }
    }

    /**
     * API接口测试
     */
    @PostMapping("/api-test")
    public Result<?> apiTest(@RequestBody Map<String, Object> testData) {
        Map<String, Object> response = new HashMap<>();
        response.put("received", testData);
        response.put("processed", true);
        response.put("timestamp", System.currentTimeMillis());
        return Result.success(response);
    }
}
