package com.graduation.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("biz_care_task")
public class CareTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String title;
    private String time;
    private Boolean completed;
    private Date completeTime;
    private Long completeUserId;
    private String completeUserName;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
}
