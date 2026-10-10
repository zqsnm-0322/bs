package com.graduation.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Data
@TableName("biz_vital_signs")
public class VitalSigns {
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
    private LocalDate recordDate;
    private LocalTime recordTime;
    
    // 新增字段 - 与数据库表对应
    private Date measurementTime;
    private String measurementType; // ROUTINE, CONTINUOUS, EMERGENCY
    
    // 基础生命体征 - 兼容原有字段名
    private Integer systolicPressure; // 收缩压 (原字段名)
    private Integer diastolicPressure; // 舒张压 (原字段名)
    private BigDecimal systolicBp; // 收缩压 (新字段名)
    private BigDecimal diastolicBp; // 舒张压 (新字段名)
    
    private BigDecimal heartRate; // 心率
    private BigDecimal respiratoryRate; // 呼吸频率
    private BigDecimal temperature; // 体温
    private BigDecimal oxygenSaturation; // 血氧饱和度
    private BigDecimal bloodOxygen; // 血氧 (原字段名)
    
    // 扩展指标
    private BigDecimal meanArterialPressure; // 平均动脉压
    private BigDecimal pulsePressure; // 脉压差
    private String respiratoryDepth; // 呼吸深度
    private String respiratoryRhythm; // 呼吸节律
    
    // 其他指标
    private BigDecimal weight; // 体重
    private BigDecimal bloodGlucose; // 血糖
    
    // 计算指标
    private BigDecimal bpVariability; // 血压变异性
    private BigDecimal hrVariability; // 心率变异性
    
    // 记录信息
    private String measuredBy; // 测量人员
    private String notes; // 备注
    private String remark; // 备注 (原字段名)
    private Long recorderId; // 记录人ID
    private String recorderName; // 记录人姓名
    private String recorderType; // 记录人类型
    
    private Date createTime;
    
    // 兼容性方法 - 将新字段值同步到旧字段
    public void syncFields() {
        if (this.systolicBp != null && this.systolicPressure == null) {
            this.systolicPressure = this.systolicBp.intValue();
        }
        if (this.diastolicBp != null && this.diastolicPressure == null) {
            this.diastolicPressure = this.diastolicBp.intValue();
        }
        if (this.oxygenSaturation != null && this.bloodOxygen == null) {
            this.bloodOxygen = this.oxygenSaturation;
        }
        if (this.notes != null && this.remark == null) {
            this.remark = this.notes;
        }
        if (this.measuredBy != null && this.recorderName == null) {
            this.recorderName = this.measuredBy;
        }
    }
    
    // 反向同步 - 将旧字段值同步到新字段
    public void syncFromOldFields() {
        if (this.systolicPressure != null && this.systolicBp == null) {
            this.systolicBp = BigDecimal.valueOf(this.systolicPressure);
        }
        if (this.diastolicPressure != null && this.diastolicBp == null) {
            this.diastolicBp = BigDecimal.valueOf(this.diastolicPressure);
        }
        if (this.bloodOxygen != null && this.oxygenSaturation == null) {
            this.oxygenSaturation = this.bloodOxygen;
        }
        if (this.remark != null && this.notes == null) {
            this.notes = this.remark;
        }
        if (this.recorderName != null && this.measuredBy == null) {
            this.measuredBy = this.recorderName;
        }
    }
}
