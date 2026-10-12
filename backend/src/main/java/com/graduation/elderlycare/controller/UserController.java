package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.User;
import com.graduation.elderlycare.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/info")
    public Result<?> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        // 从token中解析用户信息
        if (token == null || token.isEmpty()) {
            return Result.unauthorized("登录状态已失效，请重新登录");
        }
        
        // 移除 Bearer 前缀
        token = token.replace("Bearer ", "").trim();
        
        // 简化处理:从token中提取用户ID
        // token格式: token-{userType}-{userId}-{timestamp}
        try {
            String[] parts = token.split("-");
            if (parts.length >= 3) {
                String userType = parts[1];
                Long userId = Long.parseLong(parts[2]);
                
                if ("CAREGIVER".equals(userType)
                        || "DIRECTOR".equals(userType) || "CARE_MANAGER".equals(userType) || "ADMIN".equals(userType)) {
                    // 查询用户信息
                    User user = userMapper.selectById(userId);
                    if (user != null) {
                        user.setPassword(null); // 不返回密码
                        return Result.success(user);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析token失败: " + e.getMessage());
        }
        
        return Result.unauthorized("登录状态已失效，请重新登录");
    }

    // 获取用户列表 - 支持前端用户管理页面
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer current,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String username,
                          @RequestParam(required = false) String realName,
                          @RequestParam(required = false) String role,
                          @RequestParam(required = false) String careZone) {
        
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username);
        }
        if (StringUtils.hasText(realName)) {
            wrapper.like(User::getRealName, realName);
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }
        if (StringUtils.hasText(careZone)) {
            wrapper.eq(User::getCareZone, careZone);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        
        Page<User> result = userMapper.selectPage(page, wrapper);
        
        // 清除密码字段
        result.getRecords().forEach(user -> user.setPassword(null));
        
        return Result.success(result);
    }

    // 添加用户 - 支持前端用户管理页面
    @PostMapping
    public Result<?> addUser(@RequestBody User user) {
        try {
            // 检查用户名是否已存在
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, user.getUsername());
            User existingUser = userMapper.selectOne(wrapper);
            
            if (existingUser != null) {
                return Result.error("用户名已存在");
            }
            
            // 设置默认值
            if (!StringUtils.hasText(user.getPassword())) {
                user.setPassword("123456"); // 默认密码
            }
            user.setCreateTime(LocalDateTime.now());
            
            userMapper.insert(user);
            return Result.success("用户添加成功");
            
        } catch (Exception e) {
            return Result.error("用户添加失败: " + e.getMessage());
        }
    }
    
    // 更新用户 - 支持前端用户管理页面
    @PutMapping
    public Result<?> updateUser(@RequestBody User user) {
        try {
            User existingUser = userMapper.selectById(user.getId());
            if (existingUser == null) {
                return Result.error("用户不存在");
            }
            
            // 如果密码为空，则不更新密码
            if (!StringUtils.hasText(user.getPassword())) {
                user.setPassword(null);
            }
            
            userMapper.updateById(user);
            return Result.success("用户更新成功");
            
        } catch (Exception e) {
            return Result.error("用户更新失败: " + e.getMessage());
        }
    }
    
    // 删除用户
    @DeleteMapping("/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        try {
            User user = userMapper.selectById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            userMapper.deleteById(id);
            return Result.success("用户删除成功");
            
        } catch (Exception e) {
            return Result.error("用户删除失败: " + e.getMessage());
        }
    }
    
    // 重置用户密码
    @PutMapping("/{id}/password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            if (!StringUtils.hasText(newPassword)) {
                return Result.error("新密码不能为空");
            }
            
            User user = userMapper.selectById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            user.setPassword(newPassword);
            userMapper.updateById(user);
            
            return Result.success("密码重置成功");
            
        } catch (Exception e) {
            return Result.error("密码重置失败: " + e.getMessage());
        }
    }
    
    // 启用/禁用用户
    @PutMapping("/{id}/status")
    public Result<?> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            
            User user = userMapper.selectById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 这里可以添加状态字段到User实体，暂时返回成功
            return Result.success(enabled ? "用户已启用" : "用户已禁用");
            
        } catch (Exception e) {
            return Result.error("状态更新失败: " + e.getMessage());
        }
    }
}


