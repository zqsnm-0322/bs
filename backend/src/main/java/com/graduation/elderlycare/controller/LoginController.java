package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.User;
import com.graduation.elderlycare.entity.Resident;
import com.graduation.elderlycare.mapper.UserMapper;
import com.graduation.elderlycare.mapper.ResidentMapper;
import com.graduation.elderlycare.annotation.OperationLogAnnotation;
import com.graduation.elderlycare.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录控制器
 * 支持照护人员和老人两种角色登录
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class LoginController {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ResidentMapper residentMapper;

    /**
     * 统一登录接口
     * @param loginRequest 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    @OperationLogAnnotation(module = "系统", operationType = "LOGIN", description = "用户登录")
    public Result<?> login(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String userType = loginRequest.getUserType(); // CAREGIVER 或 RESIDENT
        
        // 验证输入
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        
        // 根据用户类型进行不同的登录验证
        if ("RESIDENT".equals(userType)) {
            return loginAsResident(username, password);
        } else {
            return loginAsCaregiver(username, password);
        }
    }
    
    /**
     * 照护人员登录
     */
    private Result<?> loginAsCaregiver(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        
        User loginUser = userMapper.selectOne(wrapper);
        
        if (loginUser != null) {
            // 使用MD5验证密码
            if (MD5Util.verify(password, loginUser.getPassword())) {
                // 构建返回数据
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", loginUser.getId());
                userInfo.put("username", loginUser.getUsername());
                userInfo.put("realName", loginUser.getRealName());
                userInfo.put("role", loginUser.getRole());
                userInfo.put("careZone", loginUser.getCareZone());
                userInfo.put("employeeNo", loginUser.getEmployeeNo());
                userInfo.put("phone", loginUser.getPhone());
                userInfo.put("email", loginUser.getEmail());
                userInfo.put("avatar", loginUser.getAvatar());
                
                Map<String, Object> data = new HashMap<>();
                data.put("token", generateToken(loginUser.getId(), loginUser.getRole()));
                data.put("userInfo", userInfo);
                
                return Result.success(data);
            }
        }
        
        return Result.error("用户名或密码错误");
    }
    
    /**
     * 老人登录
     */
    private Result<?> loginAsResident(String username, String password) {
        Resident resident = null;
        
        try {
            // 老人可以使用用户名、手机号、身份证号或入住编号登录
            LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> {
                w.eq(Resident::getUsername, username)
                 .or()
                 .eq(Resident::getPhone, username)
                 .or()
                 .eq(Resident::getIdCard, username)
                 .or()
                 .eq(Resident::getResidentId, username);
            });
            
            resident = residentMapper.selectOne(wrapper);
        } catch (Exception e) {
            // 如果查询失败（可能是字段不存在），尝试简化查询
            System.err.println("老人查询失败，尝试简化查询: " + e.getMessage());
            e.printStackTrace();
            
            try {
                // 先尝试用用户名查询
                LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Resident::getUsername, username);
                resident = residentMapper.selectOne(wrapper);
                
                if (resident == null) {
                    // 再尝试用入住编号查询
                    wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(Resident::getResidentId, username);
                    resident = residentMapper.selectOne(wrapper);
                }
                
                if (resident == null) {
                    // 最后尝试用姓名查询
                    wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(Resident::getName, username);
                    resident = residentMapper.selectOne(wrapper);
                }
            } catch (Exception ex) {
                System.err.println("简化查询也失败: " + ex.getMessage());
                return Result.error("登录失败，请联系管理员");
            }
        }
        
        if (resident != null) {
            // 验证密码
            String expectedPassword = resident.getPassword();
            
            // 兼容旧数据：如果没有设置密码，使用入住编号后6位或默认密码
            if (expectedPassword == null || expectedPassword.isEmpty()) {
                expectedPassword = resident.getResidentId() != null && resident.getResidentId().length() >= 6
                    ? resident.getResidentId().substring(resident.getResidentId().length() - 6)
                    : "123456";
            }
            
            // 使用MD5验证密码
            if (MD5Util.verify(password, expectedPassword)) {
                // 构建返回数据
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("residentId", resident.getId());
                userInfo.put("id", resident.getId());
                userInfo.put("name", resident.getName());
                userInfo.put("username", resident.getUsername());
                userInfo.put("residentNo", resident.getResidentId());
                userInfo.put("bedNo", resident.getRoomBedNo());
                userInfo.put("residentId", resident.getResidentId());
                userInfo.put("roomBedNo", resident.getRoomBedNo());
                userInfo.put("careZone", resident.getCareZone());
                userInfo.put("role", "RESIDENT");
                
                Map<String, Object> data = new HashMap<>();
                data.put("token", generateToken(resident.getId(), "RESIDENT"));
                data.put("userInfo", userInfo);
                
                return Result.success(data);
            }
        }
        
        return Result.error("用户名或密码错误");
    }
    
    /**
     * 生成Token（简化处理，实际应使用JWT）
     */
    private String generateToken(Long userId, String userType) {
        return "token-" + userType + "-" + userId + "-" + System.currentTimeMillis();
    }
    
    /**
     * 登出接口
     */
    @PostMapping("/logout")
    @OperationLogAnnotation(module = "系统", operationType = "LOGOUT", description = "用户登出")
    public Result<?> logout() {
        // 实际应该清除token、session等
        return Result.success("登出成功");
    }
    
    /**
     * 登录请求参数类
     */
    public static class LoginRequest {
        private String username;
        private String password;
        private String userType; // CAREGIVER 或 RESIDENT
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
    }
}


