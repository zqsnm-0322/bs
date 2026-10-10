package com.graduation.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.medical.entity.CareNotification;
import com.graduation.medical.mapper.CareNotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private CareNotificationMapper notificationMapper;

    /**
     * 发送养老风险通知给照护主管
     */
    public void sendHealthPredictionNotification(Long residentId, String residentName, 
                                               String predictions, String careManagerId) {
        CareNotification notification = new CareNotification();
        notification.setCareManagerId(careManagerId);
        notification.setResidentId(residentId);
        notification.setResidentName(residentName);
        notification.setNotificationType("HEALTH_PREDICTION");
        notification.setTitle("老人照护风险评估报告");
        notification.setContent(String.format("老人 %s 的照护风险评估已完成，请及时查看并落实护理措施", residentName));
        notification.setExtraData(predictions);
        notification.setIsRead(false);
        notification.setCreateTime(new Date());
        
        notificationMapper.insert(notification);
        
        System.out.println("=== 健康预测通知已发送 ===");
        System.out.println("老人: " + residentName + " (ID: " + residentId + ")");
        System.out.println("通知内容: " + notification.getContent());
        System.out.println("========================");
    }

    /**
     * 获取照护主管或护理员的未读通知
     */
    public List<CareNotification> getUnreadNotifications(String careManagerId) {
        LambdaQueryWrapper<CareNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(CareNotification::getCareManagerId, careManagerId)
                         .or()
                         .isNull(CareNotification::getCareManagerId));
        wrapper.eq(CareNotification::getIsRead, false);
        wrapper.orderByDesc(CareNotification::getCreateTime);
        
        return notificationMapper.selectList(wrapper);
    }

    /**
     * 标记通知为已读
     */
    public void markAsRead(Long notificationId) {
        CareNotification notification = notificationMapper.selectById(notificationId);
        if (notification != null) {
            notification.setIsRead(true);
            notification.setReadTime(new Date());
            notificationMapper.updateById(notification);
        }
    }

    /**
     * 创建通知
     */
    public void createNotification(CareNotification notification) {
        notificationMapper.insert(notification);
    }

    /**
     * 获取所有通知（分页）
     * 获取照护通知列表
     */
    public List<CareNotification> getAllNotifications(String careManagerId, int limit) {
        LambdaQueryWrapper<CareNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(CareNotification::getCareManagerId, careManagerId)
                         .or()
                         .isNull(CareNotification::getCareManagerId));
        wrapper.orderByDesc(CareNotification::getCreateTime);
        wrapper.last("LIMIT " + limit);
        
        return notificationMapper.selectList(wrapper);
    }
}
