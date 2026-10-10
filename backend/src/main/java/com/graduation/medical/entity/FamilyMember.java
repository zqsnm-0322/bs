package com.graduation.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("biz_family_member")
public class FamilyMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long residentId;
    private String name;
    private String relation;
    private String phone;
    private String email;
    private String address;
    private Integer isPrimary;
    private String visitFrequency;
    private Date createTime;
}