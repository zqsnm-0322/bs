package com.graduation.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
@TableName("biz_resident_health_history")
public class ResidentHealthHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("resident_id")
    private Long residentId;
    @TableField("condition_name")
    private String conditionName;
    private String conditionCode;
    @TableField("record_date")
    private Date recordDate;
    private String severity; // MILD, MODERATE, SEVERE
    private String status; // ACTIVE, RESOLVED, CHRONIC
    private String notes;
    private Date createTime;
    private Date updateTime;
}

