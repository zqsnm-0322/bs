package com.graduation.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Data
@TableName("biz_alert_msg")
public class AlertMsg {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiverId;
    @TableField("resident_id")
    private Long residentId;
    @TableField("resident_name")
    private String residentName;
    @TableField("room_bed_no")
    private String roomBedNo;
    @TableField("care_zone")
    private String careZone;
    private String riskType;       // 风险类型
    private String riskLevel;      // 风险等级
    private Integer riskScore;     // 风险分数
    private String message;        // 预警消息
    private String interventions;  // 干预措施（逗号分隔）
    private String status;         // 状态：PENDING待处理, PROCESSING处理中, COMPLETED已完成
    private Integer isRead;        // 0:未读 1:已读
    private LocalDateTime createTime;
}

