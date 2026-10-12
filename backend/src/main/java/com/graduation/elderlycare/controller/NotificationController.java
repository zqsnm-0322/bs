package com.graduation.elderlycare.controller;

import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.CareNotification;
import com.graduation.elderlycare.service.NotificationService;
import com.graduation.elderlycare.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取照护主管的未读通知
     */
    @GetMapping("/unread")
    public Result<?> getUnreadNotifications(@RequestParam(required = false) String careManagerId) {
        try {
            String recipientId = resolveRecipientId(careManagerId);
            List<CareNotification> notifications = notificationService.getUnreadNotifications(recipientId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("notifications", notifications);
            result.put("unreadCount", notifications.size());
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取通知失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有通知
     */
    @GetMapping("/all")
    public Result<?> getAllNotifications(@RequestParam(required = false) String careManagerId,
                                       @RequestParam(defaultValue = "20") Integer limit) {
        try {
            String recipientId = resolveRecipientId(careManagerId);
            List<CareNotification> notifications = notificationService.getAllNotifications(recipientId, limit);
            return Result.success(notifications);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取通知失败: " + e.getMessage());
        }
    }

    /**
     * 标记通知为已读
     */
    @PostMapping("/read/{notificationId}")
    public Result<?> markAsRead(@PathVariable Long notificationId) {
        try {
            notificationService.markAsRead(notificationId);
            return Result.success("标记成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("标记失败: " + e.getMessage());
        }
    }

    /**
     * 批量标记为已读
     */
    @PostMapping("/read-all")
    public Result<?> markAllAsRead(@RequestParam(required = false) String careManagerId) {
        try {
            String recipientId = resolveRecipientId(careManagerId);
            List<CareNotification> unreadNotifications = notificationService.getUnreadNotifications(recipientId);
            for (CareNotification notification : unreadNotifications) {
                notificationService.markAsRead(notification.getId());
            }
            return Result.success("全部标记成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("标记失败: " + e.getMessage());
        }
    }

    private String resolveRecipientId(String recipientId) {
        if (recipientId != null && !recipientId.trim().isEmpty()) return recipientId;
        Long currentUserId = AuthUtil.getCurrentUserId();
        return currentUserId == null ? null : String.valueOf(currentUserId);
    }
}
