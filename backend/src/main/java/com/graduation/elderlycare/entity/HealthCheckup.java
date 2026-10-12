package com.graduation.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("biz_health_checkup")
public class HealthCheckup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long residentId;
    private String residentName;
    private LocalDate checkDate;
    private Integer bloodPressureSystolic;
    private Integer bloodPressureDiastolic;
    private Integer heartRate;
    private BigDecimal temperature;
    private BigDecimal bloodSugarFasting;
    private BigDecimal bloodSugarPostprandial;
    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal bmi;
    private BigDecimal bloodOxygen;
    private String checkItems;
    private String conclusion;
    private String suggestion;
    private String checkedBy;
    private Date createTime;
}
