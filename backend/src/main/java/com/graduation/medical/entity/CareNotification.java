package com.graduation.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
@TableName("care_notifications")
public class CareNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("care_manager_id")
    private String careManagerId;
    @TableField("resident_id")
    private Long residentId;
    @TableField("resident_name")
    private String residentName;
    @TableField("care_zone")
    private String careZone;
    private String notificationType; // 通知类型：HEALTH_PREDICTION, RISK_ALERT, SYMPTOM_REPORT等
    private String title; // 通知标题
    private String content; // 通知内容
    private String extraData; // 额外数据JSON
    private Boolean isRead; // 是否已读
    private Date createTime; // 创建时间
    private Date readTime; // 阅读时间
}
