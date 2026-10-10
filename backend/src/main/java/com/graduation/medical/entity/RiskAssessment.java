package com.graduation.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("biz_risk_assessment")
public class RiskAssessment {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("resident_id")
    private Long residentId;
    private Date assessmentTime;
    private String assessmentType;
    
    // 各维度风险评分
    @TableField("health_history_score")
    private BigDecimal healthHistoryScore;
    @TableField("health_check_score")
    private BigDecimal healthCheckScore;
    private BigDecimal vitalSignsScore; // 生命体征风险评分
    private BigDecimal questionnaireScore; // 问卷调查风险评分
    
    // 子项评分
    @TableField("fall_risk")
    private BigDecimal fallRisk;
    @TableField("pressure_sore_risk")
    private BigDecimal pressureSoreRisk;
    @TableField("nutrition_risk")
    private BigDecimal nutritionRisk;
    @TableField("cognitive_risk")
    private BigDecimal cognitiveRisk;
    @TableField("wandering_risk")
    private BigDecimal wanderingRisk;
    @TableField("depression_risk")
    private BigDecimal depressionRisk;
    @TableField("self_harm_risk")
    private BigDecimal selfHarmRisk;
    @TableField("neglect_risk")
    private BigDecimal neglectRisk;
    
    // 综合评估结果
    private BigDecimal totalScore; // 总风险评分
    private String riskLevel; // 风险等级
    private BigDecimal confidenceLevel; // 预测置信度
    
    // 风险因素详情
    private String riskFactors; // 主要风险因素JSON
    private String protectiveFactors; // 保护性因素JSON
    private String recommendations; // 护理建议JSON
    private String healthPredictions; // 健康状况预测JSON
    
    private String assessedBy; // 评估人员
    private Date createTime;
    
    @TableField("sent_to_resident")
    private Integer sentToResident;
    private Date sentTime; // 发送时间
    private String sentBy; // 发送人
    
    // 量表评估相关字段（用于兼容量表评估功能）
    private String scaleType;
    private String suggestion; // 评估建议
    private String evaluatorName; // 评估人姓名
}
