package com.graduation.elderlycare.entity;

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
    private String careZone;   // 负责护理区
    private String phone;
    private String email;
    private String employeeNo;
    private LocalDate hireDate;
    @TableField("avatar_url")
    private String avatar;      // 用户头像访问地址
    private LocalDateTime createTime;
}

