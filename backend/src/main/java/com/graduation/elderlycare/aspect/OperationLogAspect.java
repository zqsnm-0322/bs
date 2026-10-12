package com.graduation.elderlycare.aspect;

import com.alibaba.fastjson.JSON;
import com.graduation.elderlycare.annotation.OperationLogAnnotation;
import com.graduation.elderlycare.entity.OperationLog;
import com.graduation.elderlycare.mapper.OperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志AOP切面
 * 自动记录标记了@OperationLogAnnotation注解的方法调用
 */
@Aspect
@Component
public class OperationLogAspect {
    
    @Autowired
    private OperationLogMapper operationLogMapper;
    
    /**
     * 定义切点：所有标记了@OperationLogAnnotation注解的方法
     */
    @Pointcut("@annotation(com.graduation.elderlycare.annotation.OperationLogAnnotation)")
    public void operationLogPointcut() {}
    
    /**
     * 环绕通知：记录操作日志
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLogAnnotation annotation = method.getAnnotation(OperationLogAnnotation.class);
        
        // 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        
        // 创建日志对象
        OperationLog log = new OperationLog();
        log.setModule(annotation.module());
        log.setOperationType(annotation.operationType());
        log.setDescription(annotation.description());
        log.setMethod(request != null ? request.getMethod() : "");
        log.setRequestUrl(request != null ? request.getRequestURL().toString() : "");
        
        // 记录请求参数
        if (annotation.recordParams() && request != null) {
            Object[] args = joinPoint.getArgs();
            // 过滤掉HttpServletRequest和HttpServletResponse参数
            Object[] filteredArgs = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest) && 
                              !(arg instanceof javax.servlet.http.HttpServletResponse))
                .toArray();
            
            if (filteredArgs.length > 0) {
                try {
                    log.setRequestParams(JSON.toJSONString(filteredArgs));
                } catch (Exception e) {
                    log.setRequestParams("参数序列化失败: " + e.getMessage());
                }
            }
        }
        
        // 记录IP地址和用户代理
        if (request != null) {
            log.setIpAddress(getClientIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));
        }
        
        // 设置操作人信息（这里简化处理，实际应从Session或Token中获取）
        log.setUserId(1L); // 默认用户ID
        log.setUserName("系统管理员"); // 默认用户名
        
        Object result = null;
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 记录成功状态
            log.setStatus("SUCCESS");
            
            // 记录返回结果
            if (annotation.recordResponse() && result != null) {
                try {
                    log.setResponse(JSON.toJSONString(result));
                } catch (Exception e) {
                    log.setResponse("结果序列化失败: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            // 记录失败状态
            log.setStatus("FAILURE");
            log.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            log.setExecutionTime(endTime - startTime);
            log.setCreateTime(LocalDateTime.now());
            
            // 异步保存日志（避免影响主业务）
            saveLogAsync(log);
        }
        
        return result;
    }
    
    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * 异步保存日志
     */
    private void saveLogAsync(OperationLog log) {
        try {
            // 这里可以使用线程池异步处理，避免影响主业务性能
            new Thread(() -> {
                try {
                    operationLogMapper.insert(log);
                } catch (Exception e) {
                    // 日志保存失败，可以记录到文件或其他地方
                    System.err.println("保存操作日志失败: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            System.err.println("启动日志保存线程失败: " + e.getMessage());
        }
    }
}

