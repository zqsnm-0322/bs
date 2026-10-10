package com.graduation.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

@Data
@TableName("biz_care_plan")
public class CarePlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("resident_id")
    private Long residentId;
    @TableField("resident_name")
    private String residentName;
    @TableField("room_bed_no")
    private String roomBedNo;
    @TableField("care_zone")
    private String careZone;
    private String planTitle;
    private String planType;
    private String planTypeLabel;
    private String priority;
    private String priorityLabel;
    private Date startDate;
    private Date endDate;
    @TableField("assigned_caregiver_id")
    private Long assignedCaregiverId;
    @TableField("assigned_caregiver")
    private String assignedCaregiver;
    private String notes;
    private String status;
    private Integer progress;
    private Long creatorId;
    private String creatorName;
    private String creatorRole;
    private Date createTime;
    private Date updateTime;
}
