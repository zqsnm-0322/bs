package com.graduation.medical.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;       // ADMIN / DIRECTOR / CARE_MANAGER / CAREGIVER / RESIDENT
    @JsonProperty("careZone")
    private String department; // 数据库兼容列，对外统一为 careZone
    private String phone;
    private String email;
    private String employeeNo; // 新增：员工工号
    private LocalDate hireDate; // 新增：入职日期
    private String workZone;    // 新增：负责护理区
    @TableField("avatar_url")
    private String avatar;      // 用户头像访问地址
    private LocalDateTime createTime;
}
