package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.AlertMsg;
import com.graduation.elderlycare.entity.Resident;
import com.graduation.elderlycare.mapper.AlertMsgMapper;
import com.graduation.elderlycare.mapper.ResidentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alert")
@CrossOrigin
public class AlertController {

    @Autowired
    private AlertMsgMapper alertMsgMapper;

    @Autowired
    private ResidentMapper residentMapper;

    private static final Set<String> ELDERLY_RISK_TYPES = Set.of(
            "FALL", "PRESSURE", "COGNITIVE", "MALNUTRITION",
            "DELIRIUM", "ABUSE", "SELF_HARM", "DEPRESSION"
    );

    // 获取我的预警消息
    @GetMapping("/my")
    public Result<?> myAlerts(@RequestParam(required = false) Long userId) {
        // 实际应从Token获取userId
        LambdaQueryWrapper<AlertMsg> wrapper = new LambdaQueryWrapper<>();
        // wrapper.eq(AlertMsg::getReceiverId, userId); // 简化期间先不筛选用户
        wrapper.orderByDesc(AlertMsg::getCreateTime);
        List<AlertMsg> list = alertMsgMapper.selectList(wrapper).stream()
                .filter(alert -> ELDERLY_RISK_TYPES.contains(alert.getRiskType()))
                .peek(alert -> {
                    if (alert.getResidentId() == null) return;
                    Resident resident = residentMapper.selectById(alert.getResidentId());
                    if (resident != null) {
                        alert.setResidentName(resident.getName());
                        alert.setRoomBedNo(resident.getRoomBedNo());
                        alert.setCareZone(resident.getCareZone());
                    }
                })
                .collect(Collectors.toList());
        return Result.success(list);
    }
    
    // 标记已读
    @PutMapping("/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        AlertMsg msg = new AlertMsg();
        msg.setId(id);
        msg.setIsRead(1);
        alertMsgMapper.updateById(msg);
        return Result.success(null);
    }
}

