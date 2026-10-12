package com.graduation.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Data
@TableName("symptom_reports")
public class SymptomReport {
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
    
    private String symptoms; // 症状描述（多个症状用逗号分隔）
    private String symptomType; // 症状类型
    private String symptomName; // 症状名称
    private String severity; // 严重程度：mild(轻微), moderate(中等), severe(严重)
    private String severityName; // 严重程度名称
    private String description; // 详细描述
    private String location; // 症状位置
    private String startTime; // 开始时间
    private String triggers; // 诱发因素
    private String additionalInfo; // 附加信息
    private String images; // 图片路径（多张图片用逗号分隔）
    
    private String status; // 状态：PENDING(待处理), HANDLING(处理中), HANDLED(已处理)
    private Long handlerId; // 处理人ID
    private String handlerName; // 处理人姓名
    private String handleNote; // 处理备注
    private String handleResult; // 处理结果
    private LocalDateTime handleTime; // 处理时间
    
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}

