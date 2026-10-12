package com.graduation.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Data
@TableName("biz_visit_appointment")
public class VisitAppointment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long residentId;
    private String residentName;
    private String visitorName;
    private String visitorPhone;
    private String visitorRelation;
    private LocalDate visitDate;
    private LocalTime visitStartTime;
    private LocalTime visitEndTime;
    private Integer visitorCount;
    private String specialNotes;
    private String status;
    private String approvedBy;
    private Date approveTime;
    private String rejectReason;
    private Date createTime;
}
