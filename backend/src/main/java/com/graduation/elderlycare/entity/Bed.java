package com.graduation.elderlycare.entity;

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
    private String roomBedNo;
    private String careZone;
    private String ward;
    private String roomType;    // 单人房/双人房/多人房
    private Integer floor;      // 新增：楼层
    private String roomNo;      // 新增：房间号
    private String status;      // AVAILABLE/OCCUPIED/MAINTENANCE
    private Long residentId;
    private String residentName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
