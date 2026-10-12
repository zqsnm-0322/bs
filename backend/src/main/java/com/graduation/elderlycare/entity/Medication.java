package com.graduation.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用药记录实体类
 */
@Data
@TableName("biz_medication")
public class Medication {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("resident_id")
    private Long residentId;
    @TableField("resident_name")
    private String residentName;
    private String medicationName; // 药品名称
    private String medicationCode; // 药品编码
    private String dosage; // 剂量（如：500mg）
    private String unit; // 单位
    private String frequency; // 频次（如：每日3次）
    private String route; // 给药途径（口服/静脉/肌注等）
    private LocalDate startDate; // 开始日期
    private LocalDate endDate; // 结束日期
    private Integer duration; // 疗程天数
    private String purpose; // 用药目的
    private String notes; // 备注
    private String prescribedBy;
    private Long prescribedById;
    private String status; // 状态: ACTIVE-进行中, COMPLETED-已完成, STOPPED-已停药
    private String stopReason; // 停药原因
    private LocalDateTime stopTime; // 停药时间
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}

