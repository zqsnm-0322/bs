package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.User;
import com.graduation.elderlycare.mapper.UserMapper;
import com.graduation.elderlycare.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 个人信息管理Controller
 * 用于照护主管、护理员在APP端查看和编辑个人信息
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserProfileController {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private final Path avatarDirectory = Paths.get("uploads", "avatars").toAbsolutePath().normalize();

    @Autowired
    private UserMapper userMapper;

    /**
     * 从token中获取当前用户信息
     */
    private Map<String, Object> getCurrentUserInfo(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // 移除 Bearer 前缀
        token = token.replace("Bearer ", "").trim();
        
        try {
            // token格式: token-{userType}-{userId}-{timestamp}
            String[] parts = token.split("-");
            if (parts.length >= 3) {
                String userType = parts[1];
                Long userId = Long.parseLong(parts[2]);
                
                if ("CAREGIVER".equals(userType)
                        || "DIRECTOR".equals(userType) || "CARE_MANAGER".equals(userType)
                        || "ADMIN".equals(userType)) {
                    User user = userMapper.selectById(userId);
                    if (user != null) {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("id", user.getId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("realName", user.getRealName());
                        userInfo.put("role", user.getRole());
                        userInfo.put("careZone", user.getCareZone());
                        userInfo.put("employeeNo", user.getEmployeeNo());
                        userInfo.put("hireDate", user.getHireDate());
                        userInfo.put("phone", user.getPhone());
                        userInfo.put("email", user.getEmail());
                        userInfo.put("avatar", user.getAvatar());
                        return userInfo;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析token失败: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 获取当前登录用户的个人信息
     */
    @GetMapping({"/profile", "/current"})
    public Result<?> getProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo == null) {
                return Result.error("未登录或token无效");
            }
            
            Long userId = (Long) userInfo.get("id");
            User user = userMapper.selectById(userId);
            
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 构建返回数据（不包含密码）
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("realName", user.getRealName());
            profile.put("role", user.getRole());
            profile.put("careZone", user.getCareZone());
            profile.put("phone", user.getPhone());
            profile.put("email", user.getEmail());
            profile.put("avatar", user.getAvatar());
            profile.put("createTime", user.getCreateTime());
            
            return Result.success(profile);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取个人信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新个人信息
     * 只能更新：真实姓名、电话、邮箱
     * 不能更新：用户名、角色、护理区（这些由管理员管理）
     */
    @PutMapping({"/profile", "/current"})
    public Result<?> updateProfile(@RequestBody Map<String, Object> updateData,
                                   @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo == null) {
                return Result.error("未登录或token无效");
            }
            
            Long userId = (Long) userInfo.get("id");
            User user = userMapper.selectById(userId);
            
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 构建更新条件
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", userId);
            
            // 只允许更新特定字段
            if (updateData.containsKey("realName") && StringUtils.hasText((String) updateData.get("realName"))) {
                updateWrapper.set("real_name", updateData.get("realName"));
            }
            if (updateData.containsKey("phone")) {
                updateWrapper.set("phone", updateData.get("phone"));
            }
            if (updateData.containsKey("email")) {
                updateWrapper.set("email", updateData.get("email"));
            }
            
            userMapper.update(null, updateWrapper);
            
            // 返回更新后的信息
            User updatedUser = userMapper.selectById(userId);
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", updatedUser.getId());
            profile.put("username", updatedUser.getUsername());
            profile.put("realName", updatedUser.getRealName());
            profile.put("role", updatedUser.getRole());
            profile.put("careZone", updatedUser.getCareZone());
            profile.put("phone", updatedUser.getPhone());
            profile.put("email", updatedUser.getEmail());
            profile.put("avatar", updatedUser.getAvatar());
            
            return Result.success(profile);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新个人信息失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping({"/password", "/change-password"})
    public Result<?> changePassword(@RequestBody Map<String, String> passwordData,
                                    @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo == null) {
                return Result.error("未登录或token无效");
            }
            
            Long userId = (Long) userInfo.get("id");
            User user = userMapper.selectById(userId);
            
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            
            if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
                return Result.error("旧密码和新密码不能为空");
            }
            
            // 验证旧密码
            String oldPasswordMd5 = MD5Util.getMD5(oldPassword);
            if (!oldPasswordMd5.equals(user.getPassword())) {
                return Result.error("旧密码错误");
            }
            
            // 更新密码
            String newPasswordMd5 = MD5Util.getMD5(newPassword);
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", userId);
            updateWrapper.set("password", newPasswordMd5);
            
            userMapper.update(null, updateWrapper);
            
            return Result.success("密码修改成功");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改密码失败: " + e.getMessage());
        }
    }

    /**
     * 上传并更新当前登录用户头像。
     */
    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    public Result<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                  @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo == null) {
                return Result.error("未登录或token无效");
            }
            if (file == null || file.isEmpty()) {
                return Result.error("请选择头像图片");
            }
            if (file.getSize() > MAX_AVATAR_SIZE) {
                return Result.error("头像图片不能超过2MB");
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                return Result.error("仅支持 JPG、PNG、GIF、WEBP 图片");
            }

            String extension = switch (contentType.toLowerCase()) {
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            Files.createDirectories(avatarDirectory);
            Path target = avatarDirectory.resolve(filename).normalize();
            if (!target.getParent().equals(avatarDirectory)) {
                return Result.error("头像文件名无效");
            }
            file.transferTo(target);

            Long userId = (Long) userInfo.get("id");
            String avatarUrl = "/api/user/avatar/" + filename;
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", userId).set("avatar_url", avatarUrl);
            userMapper.update(null, updateWrapper);

            Map<String, Object> result = new HashMap<>();
            result.put("avatar", avatarUrl);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 读取已上传的头像文件。
     */
    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        try {
            if (!filename.matches("[a-fA-F0-9]{32}\\.(jpg|png|gif|webp)")) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = avatarDirectory.resolve(filename).normalize();
            if (!filePath.getParent().equals(avatarDirectory) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaTypeFactory.getMediaType(filename)
                            .orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取用户统计信息（可选功能）
     * 照护主管：负责的老人数、今日评估数等
     * 护理员：负责的老人数、今日护理记录数等
     */
    @GetMapping("/statistics")
    public Result<?> getStatistics(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo == null) {
                return Result.error("未登录或token无效");
            }
            
            String role = (String) userInfo.get("role");
            String realName = (String) userInfo.get("realName");
            String careZone = (String) userInfo.get("careZone");
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("role", role);
            statistics.put("realName", realName);
            statistics.put("careZone", careZone);
            
            // TODO: 根据角色查询相关统计数据
            // 这里可以添加更多统计信息，如：
            // - 照护主管负责的老人数
            // - 今日完成的评估数
            // - 护理员负责的老人数
            // - 今日护理记录数
            
            return Result.success(statistics);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }
}


