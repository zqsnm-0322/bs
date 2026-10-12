package com.graduation.elderlycare.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密工具类
 */
public class MD5Util {
    
    /**
     * 对字符串进行MD5加密
     * @param str 待加密的字符串
     * @return 加密后的32位小写字符串
     */
    public static String encrypt(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(str.getBytes());
            
            // 将字节数组转换为16进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }
    
    /**
     * 对字符串进行MD5加密（别名方法）
     * @param str 待加密的字符串
     * @return 加密后的32位小写字符串
     */
    public static String getMD5(String str) {
        return encrypt(str);
    }
    
    /**
     * 验证密码是否匹配
     * @param plainPassword 明文密码
     * @param encryptedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean verify(String plainPassword, String encryptedPassword) {
        if (plainPassword == null || encryptedPassword == null) {
            return false;
        }
        
        String encrypted = encrypt(plainPassword);
        return encrypted != null && encrypted.equals(encryptedPassword);
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        String password = "123456";
        String encrypted = encrypt(password);
        System.out.println("原始密码: " + password);
        System.out.println("MD5加密: " + encrypted);
        System.out.println("验证结果: " + verify(password, encrypted));
    }
}

