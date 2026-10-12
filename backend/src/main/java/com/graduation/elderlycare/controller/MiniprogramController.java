package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.Resident;
import com.graduation.elderlycare.entity.RiskRecord;
import com.graduation.elderlycare.entity.AlertMsg;
import com.graduation.elderlycare.entity.User;
import com.graduation.elderlycare.mapper.ResidentMapper;
import com.graduation.elderlycare.mapper.RiskRecordMapper;
import com.graduation.elderlycare.mapper.AlertMsgMapper;
import com.graduation.elderlycare.mapper.UserMapper;
import com.graduation.elderlycare.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.graduation.elderlycare.util.AuthUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/miniprogram")
@CrossOrigin
public class MiniprogramController {

    @Autowired
    private ResidentMapper residentMapper;
    
    @Autowired
    private RiskRecordMapper riskRecordMapper;
    
    @Autowired
    private AlertMsgMapper alertMsgMapper;
    
    @Autowired
    private UserMapper userMapper;

    // 小程序登录
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String userType = request.get("userType"); // CAREGIVER, CARE_MANAGER 或 RESIDENT
        
        // 验证输入
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        
        // 根据用户类型进行不同的登录验证
        if ("RESIDENT".equals(userType)) {
            return loginAsResident(username, password);
        } else {
            return loginAsStaff(username, password);
        }
    }
    
    /**
     * 照护人员登录（照护主管和护理员）
     */
    private Result<?> loginAsStaff(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        
        User loginUser = userMapper.selectOne(wrapper);
        
        if (loginUser != null) {
            // 使用MD5验证密码
            if (MD5Util.verify(password, loginUser.getPassword())) {
                // 构建返回数据
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", loginUser.getId());
                userInfo.put("username", loginUser.getUsername());
                userInfo.put("realName", loginUser.getRealName());
                userInfo.put("role", loginUser.getRole());
                userInfo.put("careZone", loginUser.getCareZone());
                userInfo.put("phone", loginUser.getPhone());
                
                Map<String, Object> data = new HashMap<>();
                data.put("token", "token-" + loginUser.getRole() + "-" + loginUser.getId() + "-" + System.currentTimeMillis());
                data.put("userInfo", userInfo);
                
                return Result.success(data);
            }
        }
        
        return Result.error("用户名或密码错误");
    }
    
    /**
     * 老人登录
     */
    private Result<?> loginAsResident(String username, String password) {
        Resident resident = null;
        
        try {
            // 老人可以使用用户名、手机号或入住编号登录
            LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> {
                w.eq(Resident::getUsername, username)
                 .or()
                 .eq(Resident::getResidentId, username);
            });
            
            resident = residentMapper.selectOne(wrapper);
        } catch (Exception e) {
            System.err.println("老人查询失败: " + e.getMessage());
        }
        
        if (resident != null) {
            // 验证密码
            String expectedPassword = resident.getPassword();
            
            // 兼容旧数据：如果没有设置密码，使用入住编号后6位或默认密码
            if (expectedPassword == null || expectedPassword.isEmpty()) {
                expectedPassword = resident.getResidentId() != null && resident.getResidentId().length() >= 6
                    ? resident.getResidentId().substring(resident.getResidentId().length() - 6)
                    : "123456";
            }
            
            // 使用MD5验证密码
            if (MD5Util.verify(password, expectedPassword)) {
                // 构建返回数据
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("residentId", resident.getId());
                userInfo.put("id", resident.getId());
                userInfo.put("name", resident.getName());
                userInfo.put("username", resident.getUsername());
                userInfo.put("residentNo", resident.getResidentId());
                userInfo.put("bedNo", resident.getRoomBedNo());
                userInfo.put("careZone", resident.getCareZone());
                userInfo.put("role", "RESIDENT");
                
                Map<String, Object> data = new HashMap<>();
                data.put("token", "token-RESIDENT-" + resident.getId() + "-" + System.currentTimeMillis());
                data.put("userInfo", userInfo);
                
                return Result.success(data);
            }
        }
        
        return Result.error("用户名或密码错误");
    }

    // 获取在住老人列表
    @GetMapping("/residents")
    public Result<?> getResidents(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(required = false) String keyword) {
        
        Page<Resident> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();
        if ("CAREGIVER".equals(AuthUtil.getCurrentRole())) {
            Long caregiverId = AuthUtil.getCurrentUserId();
            if (caregiverId == null) wrapper.apply("1 = 0");
            else wrapper.eq(Resident::getCaregiverId, caregiverId);
        }
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Resident::getName, keyword)
                           .or().like(Resident::getResidentId, keyword)
                           .or().like(Resident::getRoomBedNo, keyword));
        }
        
        wrapper.eq(Resident::getStatus, "RESIDENT")
               .orderByDesc(Resident::getAdmitTime);
        
        Page<Resident> result = residentMapper.selectPage(page, wrapper);
        
        // 转换为小程序需要的格式
        List<Map<String, Object>> residents = result.getRecords().stream().map(resident -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", resident.getId());
            item.put("name", resident.getName());
            item.put("gender", resident.getGender());
            item.put("age", resident.getAge());
            item.put("roomBedNo", resident.getRoomBedNo());
            item.put("residentId", resident.getResidentId());
            item.put("careZone", resident.getCareZone());
            item.put("healthStatus", resident.getHealthStatus());
            item.put("admitTime", resident.getAdmitTime());
            
            // 获取最新风险状态
            LambdaQueryWrapper<RiskRecord> riskWrapper = new LambdaQueryWrapper<>();
            riskWrapper.eq(RiskRecord::getResidentId, resident.getId())
                      .orderByDesc(RiskRecord::getEvaluateTime)
                      .last("LIMIT 1");
            RiskRecord latestRisk = riskRecordMapper.selectOne(riskWrapper);
            
            if (latestRisk != null) {
                item.put("riskLevel", latestRisk.getRiskLevel());
                item.put("riskType", latestRisk.getScaleType());
                item.put("lastAssessTime", latestRisk.getEvaluateTime());
            }
            
            return item;
        }).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("list", residents);
        response.put("total", result.getTotal());
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);
        
        return Result.success(response);
    }

    // 获取老人详情
    @GetMapping("/residents/{id}")
    public Result<?> getResidentDetail(@PathVariable Long id) {
        Resident resident = residentMapper.selectById(id);
        if (resident == null) {
            return Result.error("老人不存在");
        }
        if ("CAREGIVER".equals(AuthUtil.getCurrentRole())
                && !Objects.equals(resident.getCaregiverId(), AuthUtil.getCurrentUserId())) {
            return Result.error("无权查看其他护理员负责的老人");
        }
        
        // 获取当前风险状态
        LambdaQueryWrapper<RiskRecord> riskWrapper = new LambdaQueryWrapper<>();
        riskWrapper.eq(RiskRecord::getResidentId, id)
                   .orderByDesc(RiskRecord::getEvaluateTime);
        List<RiskRecord> recentRisks = riskRecordMapper.selectList(riskWrapper);
        
        // 构建当前风险状态
        List<Map<String, Object>> currentRisks = new ArrayList<>();
        Set<String> processedTypes = new HashSet<>();
        
        for (RiskRecord risk : recentRisks) {
            if (!processedTypes.contains(risk.getScaleType())) {
                Map<String, Object> riskInfo = new HashMap<>();
                riskInfo.put("type", risk.getScaleType());
                riskInfo.put("level", risk.getRiskLevel());
                riskInfo.put("typeName", getScaleTypeName(risk.getScaleType()));
                riskInfo.put("levelName", getRiskLevelName(risk.getRiskLevel()));
                currentRisks.add(riskInfo);
                processedTypes.add(risk.getScaleType());
            }
        }
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", resident.getId());
        result.put("name", resident.getName());
        result.put("gender", resident.getGender());
        result.put("age", resident.getAge());
        result.put("roomBedNo", resident.getRoomBedNo());
        result.put("residentId", resident.getResidentId());
        result.put("careZone", resident.getCareZone());
        result.put("careManager", resident.getCareManager());
        result.put("healthStatus", resident.getHealthStatus());
        result.put("admitTime", resident.getAdmitTime());
        result.put("currentRisks", currentRisks);
        result.put("emergencyContact", resident.getPhone()); // 假设使用老人手机号作为紧急联系人
        
        return Result.success(result);
    }

    // 获取老人照护风险评估历史
    @GetMapping("/residents/{id}/risk-history")
    public Result<?> getResidentRiskHistory(@PathVariable Long id) {
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getResidentId, id)
               .orderByDesc(RiskRecord::getEvaluateTime);
        
        List<RiskRecord> records = riskRecordMapper.selectList(wrapper);
        
        // 转换为小程序需要的格式
        List<Map<String, Object>> result = records.stream().map(record -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("evaluateTime", record.getEvaluateTime());
            item.put("scaleName", getScaleTypeName(record.getScaleType()));
            item.put("scaleType", record.getScaleType());
            item.put("totalScore", record.getTotalScore());
            item.put("riskLevel", record.getRiskLevel());
            item.put("riskLevelText", getRiskLevelName(record.getRiskLevel()));
            item.put("evaluatorName", record.getEvaluatorName());
            return item;
        }).collect(Collectors.toList());
        
        return Result.success(result);
    }

    // 获取消息列表 - 支持小程序消息中心
    @GetMapping("/messages")
    public Result<?> getMessages(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(required = false) Long receiverId) {
        
        Page<AlertMsg> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AlertMsg> wrapper = new LambdaQueryWrapper<>();
        
        if (receiverId != null) {
            wrapper.eq(AlertMsg::getReceiverId, receiverId);
        }
        
        wrapper.orderByDesc(AlertMsg::getCreateTime);
        
        Page<AlertMsg> result = alertMsgMapper.selectPage(page, wrapper);
        
        // 转换为小程序需要的格式
        List<Map<String, Object>> messages = result.getRecords().stream().map(msg -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", msg.getId());
            item.put("residentName", msg.getResidentName());
            item.put("riskType", msg.getRiskType());
            item.put("message", msg.getMessage());
            item.put("isRead", msg.getIsRead());
            item.put("createTime", msg.getCreateTime());
            item.put("riskTypeName", getScaleTypeName(msg.getRiskType()));
            return item;
        }).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("list", messages);
        response.put("total", result.getTotal());
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);
        
        return Result.success(response);
    }

    // 标记消息已读
    @PutMapping("/messages/{id}/read")
    public Result<?> markMessageRead(@PathVariable Long id) {
        try {
            AlertMsg msg = alertMsgMapper.selectById(id);
            if (msg == null) {
                return Result.error("消息不存在");
            }
            
            msg.setIsRead(1);
            alertMsgMapper.updateById(msg);
            
            return Result.success("消息已标记为已读");
            
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    // 全部标记已读
    @PutMapping("/messages/read-all")
    public Result<?> markAllMessagesRead(@RequestParam Long receiverId) {
        try {
            LambdaQueryWrapper<AlertMsg> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AlertMsg::getReceiverId, receiverId)
                   .eq(AlertMsg::getIsRead, 0);
            
            List<AlertMsg> unreadMessages = alertMsgMapper.selectList(wrapper);
            
            for (AlertMsg msg : unreadMessages) {
                msg.setIsRead(1);
                alertMsgMapper.updateById(msg);
            }
            
            return Result.success("所有消息已标记为已读");
            
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    // 删除消息
    @DeleteMapping("/messages/{id}")
    public Result<?> deleteMessage(@PathVariable Long id) {
        try {
            AlertMsg msg = alertMsgMapper.selectById(id);
            if (msg == null) {
                return Result.error("消息不存在");
            }
            
            alertMsgMapper.deleteById(id);
            return Result.success("消息删除成功");
            
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // 获取首页统计数据 - 支持小程序首页
    @GetMapping("/dashboard")
    public Result<?> getDashboardData() {
        System.out.println("========== 小程序Dashboard统计 ==========");
        
        // 1. 在住老人总数
        LambdaQueryWrapper<Resident> residentWrapper = new LambdaQueryWrapper<>();
        residentWrapper.eq(Resident::getStatus, "RESIDENT");
        Long totalResidents = residentMapper.selectCount(residentWrapper);
        System.out.println("在住老人总数: " + totalResidents);
        
        // 2. 今日评估数量
        LambdaQueryWrapper<RiskRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(RiskRecord::getEvaluateTime, LocalDateTime.now().toLocalDate().atStartOfDay())
                   .lt(RiskRecord::getEvaluateTime, LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay());
        Long todayAssessments = riskRecordMapper.selectCount(todayWrapper);
        System.out.println("今日评估数量: " + todayAssessments);
        
        // 3. 高危老人数量（最近7天内有高危评估的老人）
        LambdaQueryWrapper<RiskRecord> highRiskWrapper = new LambdaQueryWrapper<>();
        highRiskWrapper.eq(RiskRecord::getRiskLevel, "HIGH")
                      .ge(RiskRecord::getEvaluateTime, LocalDateTime.now().minusDays(7));
        Long highRiskResidents = riskRecordMapper.selectCount(highRiskWrapper);
        System.out.println("高危老人数量: " + highRiskResidents);
        
        // 4. 未读消息数量
        LambdaQueryWrapper<AlertMsg> unreadWrapper = new LambdaQueryWrapper<>();
        unreadWrapper.eq(AlertMsg::getIsRead, 0);
        Long unreadMessages = alertMsgMapper.selectCount(unreadWrapper);
        System.out.println("未读消息数量: " + unreadMessages);
        
        System.out.println("========================================");
        
        Map<String, Object> result = new HashMap<>();
        result.put("todayAssessments", todayAssessments.intValue());
        result.put("highRiskResidents", highRiskResidents.intValue());
        result.put("unreadMessages", unreadMessages.intValue());
        result.put("totalResidents", totalResidents.intValue());
        
        return Result.success(result);
    }

    // 扫码功能 - 支持小程序扫码评估
    @PostMapping("/scan")
    public Result<?> scanQRCode(@RequestBody Map<String, String> request) {
        String qrCode = request.get("qrCode");
        
        // 解析二维码，获取老人信息
        // 这里假设二维码格式为: RESIDENT:{residentId}
        if (qrCode != null && qrCode.startsWith("RESIDENT:")) {
            String residentIdStr = qrCode.replace("RESIDENT:", "");
            try {
                Long residentId = Long.parseLong(residentIdStr);
                Resident resident = residentMapper.selectById(residentId);
                
                if (resident != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("residentId", resident.getId());
                    result.put("residentName", resident.getName());
                    result.put("bedNo", resident.getRoomBedNo());
                    result.put("careZone", resident.getCareZone());
                    return Result.success(result);
                } else {
                    return Result.error("老人不存在");
                }
            } catch (NumberFormatException e) {
                return Result.error("无效的二维码格式");
            }
        }
        
        return Result.error("无法识别的二维码");
    }

    // 获取老人健康统计数据 - 支持小程序老人首页
    // 获取老人健康统计数据 - 支持小程序老人首页
    @GetMapping("/resident/stats")
    public Result<?> getResidentStats(@RequestParam("residentId") Long residentId) {
        System.out.println("========== 老人健康统计 ==========");
        System.out.println("接收到的老人ID: " + residentId);

        try {
            // 先验证老人是否存在
            Resident resident = residentMapper.selectById(residentId);
            if (resident == null) {
                System.out.println("错误：老人不存在，ID=" + residentId);
                return Result.error("老人不存在");
            }
            System.out.println("老人姓名: " + resident.getName());
            System.out.println("入住编号: " + resident.getResidentId());

            // 1. 总评估次数
            LambdaQueryWrapper<RiskRecord> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.eq(RiskRecord::getResidentId, residentId);
            Long totalAssessments = riskRecordMapper.selectCount(totalWrapper);
            System.out.println("总评估次数: " + totalAssessments);

            // 查看所有评估记录
            List<RiskRecord> allRecords = riskRecordMapper.selectList(totalWrapper);
            System.out.println("评估记录详情:");
            for (RiskRecord record : allRecords) {
                System.out.println("  - ID=" + record.getId() + ", 类型=" + record.getScaleType() + 
                                 ", 风险=" + record.getRiskLevel() + ", 时间=" + record.getEvaluateTime());
            }

            // 2. 高危次数（HIGH风险的评估记录数）
            LambdaQueryWrapper<RiskRecord> highRiskWrapper = new LambdaQueryWrapper<>();
            highRiskWrapper.eq(RiskRecord::getResidentId, residentId)
                          .eq(RiskRecord::getRiskLevel, "HIGH");
            Long highRiskCount = riskRecordMapper.selectCount(highRiskWrapper);
            System.out.println("高危次数: " + highRiskCount);

            // 3. 干预次数（中危+高危的评估记录数，因为这些都需要护理干预）
            LambdaQueryWrapper<RiskRecord> interventionWrapper = new LambdaQueryWrapper<>();
            interventionWrapper.eq(RiskRecord::getResidentId, residentId)
                              .in(RiskRecord::getRiskLevel, "MEDIUM", "HIGH");
            Long interventionCount = riskRecordMapper.selectCount(interventionWrapper);
            System.out.println("干预次数: " + interventionCount);

            System.out.println("====================================");

            Map<String, Object> result = new HashMap<>();
            result.put("totalAssessments", totalAssessments.intValue());
            result.put("highRiskCount", highRiskCount.intValue());
            result.put("interventionCount", interventionCount.intValue());

            return Result.success(result);
        } catch (Exception e) {
            System.out.println("查询失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    // 获取老人最新风险评估 - 支持小程序老人首页
    @GetMapping("/resident/risk/latest")
    public Result<?> getResidentLatestRisk(@RequestParam("residentId") Long residentId) {
        System.out.println("========== 老人最新风险评估 ==========");
        System.out.println("老人ID: " + residentId);

        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getResidentId, residentId)
               .orderByDesc(RiskRecord::getEvaluateTime)
               .last("LIMIT 1");

        RiskRecord latestRisk = riskRecordMapper.selectOne(wrapper);

        if (latestRisk == null) {
            System.out.println("未找到评估记录");
            System.out.println("====================================");
            return Result.error("暂无评估记录");
        }

        System.out.println("风险等级: " + latestRisk.getRiskLevel());
        System.out.println("评估时间: " + latestRisk.getEvaluateTime());
        System.out.println("====================================");

        Map<String, Object> result = new HashMap<>();
        result.put("riskLevel", latestRisk.getRiskLevel());
        result.put("riskType", latestRisk.getScaleType());
        result.put("totalScore", latestRisk.getTotalScore());
        result.put("evaluateTime", latestRisk.getEvaluateTime());
        result.put("evaluatorName", latestRisk.getEvaluatorName());
        result.put("riskTypeName", getScaleTypeName(latestRisk.getScaleType()));
        result.put("riskLevelName", getRiskLevelName(latestRisk.getRiskLevel()));

        return Result.success(result);
    }


    // 工具方法
    private String getScaleTypeName(String scaleType) {
        switch (scaleType) {
            case "FALL": return "跌倒";
            case "PRESSURE": return "压疮";
            case "COGNITIVE": return "认知障碍";
            case "MALNUTRITION": return "营养不良";
            case "DELIRIUM": return "谵妄";
            case "ABUSE": return "虐待与忽视";
            case "SELF_HARM": return "自伤";
            case "DEPRESSION": return "抑郁";
            default: return scaleType;
        }
    }
    
    private String getRiskLevelName(String riskLevel) {
        switch (riskLevel) {
            case "HIGH": return "高危";
            case "MEDIUM": return "中危";
            case "LOW": return "低危";
            default: return riskLevel;
        }
    }
}


