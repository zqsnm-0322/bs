package com.graduation.medical.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 权限工具类
 */
public class AuthUtil {
    
    /**
     * 检查当前用户是否为老人
     * @return true表示是老人，false表示是工作人员
     */
    public static boolean isResident() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return false;
            }
            
            // 从请求头中获取用户角色信息
            String userRole = request.getHeader("User-Role");
            if (userRole != null) {
                return "RESIDENT".equals(userRole);
            }
            
            // 从token中解析用户类型（简化处理）
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("token-")) {
                return token.contains("-RESIDENT-");
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查当前用户是否为养老机构工作人员
     * @return true表示是工作人员，false表示是老人
     */
    public static boolean isCareStaff() {
        return !isResident();
    }
    
    /**
     * 获取当前请求对象
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return null;
            }
            
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("token-")) {
                String[] parts = token.split("-");
                if (parts.length >= 4) {
                    return Long.parseLong(parts[2]);
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取当前令牌中的角色。 */
    public static String getCurrentRole() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) return null;
            String token = request.getHeader("Authorization");
            if (token == null) return request.getHeader("User-Role");
            token = token.replace("Bearer ", "").trim();
            String[] parts = token.split("-");
            return parts.length >= 3 ? parts[1] : request.getHeader("User-Role");
        } catch (Exception e) {
            return null;
        }
    }
}
