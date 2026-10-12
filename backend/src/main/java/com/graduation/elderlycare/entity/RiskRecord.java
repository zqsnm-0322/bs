package com.graduation.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Data
@TableName("biz_risk_record")
public class RiskRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("resident_id")
    private Long residentId;
    
    @TableField(exist = false) // 数据库查询时关联查询，非表中字段
    private String residentName;
    
    private Long evaluatorId;
    private String evaluatorName;
    private String scaleType;
    private Integer totalScore;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String detailJson; // 存储答题详情
    private LocalDateTime evaluateTime;
    
    @TableField("sent_to_resident")
    private Integer sentToResident;
    private LocalDateTime sentTime; // 发送时间
    private String sentBy; // 发送人
    private String suggestion; // 评估建议
    
    // 需求11: 评估来源字段
    private String requestSource; // 评估来源: PC-电脑端, MINIPROGRAM-小程序
}

