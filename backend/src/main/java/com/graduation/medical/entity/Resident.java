package com.graduation.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("biz_resident")
public class Resident {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("resident_id")
    private String residentId;
    private String name;
    private String gender;
    private Integer age;
    
    @TableField("room_bed_no")
    private String roomBedNo;
    
    @TableField("health_status")
    private String healthStatus;
    
    @TableField("admit_time")
    private Date admitTime;
    
    @TableField("leave_time")
    private Date leaveTime;
    
    @TableField("leave_reason")
    private String leaveReason;
    
    @TableField("care_manager")
    private String careManager;
    
    @TableField("care_zone")
    private String careZone;
    
    private String status;
    
    private String idType;
    private String idNumber;
    
    @TableField("origin_address")
    private String originAddress;
    
    @TableField("admission_type")
    private String admissionType;
    
    @TableField("health_level")
    private String healthLevel;
    
    @TableField("allergy_history")
    private String allergyHistory;
    
    @TableField("chronic_diseases")
    private String chronicDiseases;
    
    @TableField("diet_restriction")
    private String dietRestriction;
    
    @TableField("emergency_contact_name")
    private String emergencyContactName;
    
    @TableField("emergency_contact_phone")
    private String emergencyContactPhone;
    
    @TableField("emergency_contact_relation")
    private String emergencyContactRelation;
    
    private String insuranceType;
    private BigDecimal feeStandard;
    
    @TableField("contract_start_date")
    private LocalDate contractStartDate;
    
    @TableField("contract_end_date")
    private LocalDate contractEndDate;
    
    @TableField("caregiver_name")
    private String caregiverName;
    @TableField("caregiver_id")
    private Long caregiverId;
    
    private String username;
    private String idCard;
    private String phone;
    private String password;
    private String currentRisks;
    private String interventions;
    private Date createTime;
    private String assessmentStatus;
    private Date assessmentRequestTime;
    private String requestSource;
}
