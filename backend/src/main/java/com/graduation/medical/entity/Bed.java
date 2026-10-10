package com.graduation.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_bed")
public class Bed {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roomBedNo;   // 原 bedNo
    private String careZone;    // 原 department
    private String ward;
    private String roomType;    // 原 bedType：单人房/双人房/多人房
    private Integer floor;      // 新增：楼层
    private String roomNo;      // 新增：房间号
    private String status;      // AVAILABLE/OCCUPIED/MAINTENANCE
    private Long residentId;    // 原 residentId
    private String residentName;// 原 residentName
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}