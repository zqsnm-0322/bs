package com.graduation.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.medical.common.Result;
import com.graduation.medical.entity.SymptomReport;
import com.graduation.medical.entity.Resident;
import com.graduation.medical.mapper.SymptomReportMapper;
import com.graduation.medical.mapper.ResidentMapper;
import com.graduation.medical.annotation.OperationLogAnnotation;
import com.graduation.medical.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 症状上报控制器
 * 老人提交症状上报，护理员仅可查看自己负责老人的记录
 */
@RestController
@RequestMapping("/api/symptom")
@CrossOrigin
public class SymptomReportController {

    @Autowired
    private SymptomReportMapper symptomReportMapper;

    @Autowired
    private ResidentMapper residentMapper;

    /**
     * 老人提交症状上报
     */
    @PostMapping("/report")
    @OperationLogAnnotation(module = "症状上报", operationType = "CREATE", description = "老人症状上报")
    public Result<?> submitReport(@RequestBody Map<String, Object> data) {
        try {
            Long residentId = Long.valueOf(data.get("residentId").toString());
            String symptomType = (String) data.get("symptomType");
            String severity = (String) data.get("severity");
            String description = (String) data.get("description");

            if (symptomType == null || symptomType.trim().isEmpty()) {
                return Result.error("请选择症状类型");
            }
            if (severity == null || severity.trim().isEmpty()) {
                return Result.error("请选择严重程度");
            }
            if (description == null || description.trim().isEmpty()) {
                return Result.error("请描述症状详情");
            }

            // 获取老人信息
            Resident resident = residentMapper.selectById(residentId);
            if (resident == null) {
                return Result.error("老人不存在");
            }

            SymptomReport report = new SymptomReport();
            report.setResidentId(residentId);
            report.setResidentName(resident.getName());
            report.setRoomBedNo(resident.getRoomBedNo());
            report.setCareZone(resident.getCareZone());
            report.setSymptomType(symptomType);
            report.setSymptomName(getSymptomName(symptomType));
            report.setSeverity(severity);
            report.setSeverityName(getSeverityName(severity));
            report.setDescription(description);
            report.setLocation((String) data.get("location"));
            report.setStartTime((String) data.get("startTime"));

            // 处理诱发因素
            Object triggersObj = data.get("triggers");
            if (triggersObj instanceof List) {
                report.setTriggers(String.join(",", (List<String>) triggersObj));
            } else if (triggersObj instanceof String) {
                report.setTriggers((String) triggersObj);
            }

            report.setAdditionalInfo((String) data.get("additionalInfo"));
            report.setStatus("PENDING");
            report.setCreateTime(LocalDateTime.now());

            symptomReportMapper.insert(report);

            return Result.success("症状上报成功，护理员将尽快处理");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上报失败: " + e.getMessage());
        }
    }

    /**
     * 护理员查看症状上报列表（只能查看自己负责老人的）
     */
    @GetMapping("/list")
    public Result<?> getReportList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department) {

        Page<SymptomReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SymptomReport> wrapper = new LambdaQueryWrapper<>();

        List<Long> assignedResidentIds = getAssignedResidentIds();
        if (assignedResidentIds != null) {
            if (assignedResidentIds.isEmpty()) wrapper.apply("1 = 0");
            else wrapper.in(SymptomReport::getResidentId, assignedResidentIds);
        }

        // 如果传入了护理区参数，只查询该护理区的上报
        if (department != null && !department.trim().isEmpty()) {
            wrapper.eq(SymptomReport::getCareZone, department);
        }

        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(SymptomReport::getStatus, status);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(SymptomReport::getResidentName, keyword)
                             .or().like(SymptomReport::getDescription, keyword)
                             .or().like(SymptomReport::getRoomBedNo, keyword));
        }

        wrapper.orderByDesc(SymptomReport::getCreateTime);

        Page<SymptomReport> result = symptomReportMapper.selectPage(page, wrapper);

        List<Map<String, Object>> list = result.getRecords().stream()
                .map(this::convertReport).collect(Collectors.toList());

        // 统计待处理数量（按护理区）
        LambdaQueryWrapper<SymptomReport> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(SymptomReport::getStatus, "PENDING");
        if (assignedResidentIds != null) {
            if (assignedResidentIds.isEmpty()) pendingWrapper.apply("1 = 0");
            else pendingWrapper.in(SymptomReport::getResidentId, assignedResidentIds);
        }
        if (department != null && !department.trim().isEmpty()) {
            pendingWrapper.eq(SymptomReport::getCareZone, department);
        }
        long pendingCount = symptomReportMapper.selectCount(pendingWrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("list", list);
        response.put("total", result.getTotal());
        response.put("pendingCount", pendingCount);
        return Result.success(response);
    }

    /**
     * 查看上报详情
     */
    @GetMapping("/detail/{id}")
    public Result<?> getReportDetail(@PathVariable Long id) {
        SymptomReport report = symptomReportMapper.selectById(id);
        if (report == null) {
            return Result.error("上报记录不存在");
        }
        if (isCaregiverDenied(report.getResidentId())) return Result.error("无权查看其他护理员负责的老人上报");
        return Result.success(convertReport(report));
    }

    /**
     * 护理员处理上报
     */
    @PutMapping("/{id}/handle")
    @OperationLogAnnotation(module = "症状上报", operationType = "UPDATE", description = "护理员处理症状上报")
    public Result<?> handleReport(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            SymptomReport report = symptomReportMapper.selectById(id);
            if (report == null) {
                return Result.error("上报记录不存在");
            }
            if (isCaregiverDenied(report.getResidentId())) return Result.error("无权处理其他护理员负责的老人上报");

            Long handlerId = data.get("handlerId") != null ? Long.valueOf(data.get("handlerId").toString()) : null;
            String handlerName = (String) data.get("handlerName");
            String handleResult = (String) data.get("handleResult");

            if (handleResult == null || handleResult.trim().isEmpty()) {
                return Result.error("请填写处理结果");
            }

            report.setHandlerId(handlerId);
            report.setHandlerName(handlerName);
            report.setHandleResult(handleResult);
            report.setStatus("HANDLED");
            report.setHandleTime(LocalDateTime.now());

            symptomReportMapper.updateById(report);

            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 老人修改症状上报（仅限未处理状态）
     */
    @PutMapping("/{id}/update")
    @OperationLogAnnotation(module = "症状上报", operationType = "UPDATE", description = "老人修改症状上报")
    public Result<?> updateReport(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            // 查询上报记录
            SymptomReport report = symptomReportMapper.selectById(id);
            if (report == null) {
                return Result.error("上报记录不存在");
            }

            // 只有待处理状态才能修改
            if (!"PENDING".equals(report.getStatus())) {
                return Result.error("只有未处理的上报才能修改");
            }

            // 验证必填字段
            String symptomType = (String) data.get("symptomType");
            String severity = (String) data.get("severity");
            String description = (String) data.get("description");

            if (symptomType == null || symptomType.trim().isEmpty()) {
                return Result.error("请选择症状类型");
            }
            if (severity == null || severity.trim().isEmpty()) {
                return Result.error("请选择严重程度");
            }
            if (description == null || description.trim().isEmpty()) {
                return Result.error("请描述症状详情");
            }

            // 更新症状信息
            report.setSymptomType(symptomType);
            report.setSymptomName(getSymptomName(symptomType));
            report.setSeverity(severity);
            report.setSeverityName(getSeverityName(severity));
            report.setDescription(description);
            report.setLocation((String) data.get("location"));
            report.setStartTime((String) data.get("startTime"));

            // 处理诱发因素
            Object triggersObj = data.get("triggers");
            if (triggersObj instanceof List) {
                report.setTriggers(String.join(",", (List<String>) triggersObj));
            } else if (triggersObj instanceof String) {
                report.setTriggers((String) triggersObj);
            }

            report.setAdditionalInfo((String) data.get("additionalInfo"));
            report.setUpdateTime(LocalDateTime.now());

            symptomReportMapper.updateById(report);

            return Result.success("修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改失败: " + e.getMessage());
        }
    }

    /**
     * 老人查看自己的上报历史
     */
    @GetMapping("/my")
    public Result<?> getMyReports(
            @RequestParam("residentId") Long residentId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<SymptomReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SymptomReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SymptomReport::getResidentId, residentId)
               .orderByDesc(SymptomReport::getCreateTime);

        Page<SymptomReport> result = symptomReportMapper.selectPage(page, wrapper);

        List<Map<String, Object>> list = result.getRecords().stream()
                .map(this::convertReport).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("list", list);
        response.put("total", result.getTotal());
        return Result.success(response);
    }

    // ========== 辅助方法 ==========

    private List<Long> getAssignedResidentIds() {
        if (!"CAREGIVER".equals(AuthUtil.getCurrentRole())) return null;
        return residentMapper.selectList(new LambdaQueryWrapper<Resident>()
                        .eq(Resident::getCaregiverId, AuthUtil.getCurrentUserId()))
                .stream().map(Resident::getId).collect(Collectors.toList());
    }

    private boolean isCaregiverDenied(Long residentId) {
        if (!"CAREGIVER".equals(AuthUtil.getCurrentRole())) return false;
        Resident resident = residentMapper.selectById(residentId);
        return resident == null || !Objects.equals(resident.getCaregiverId(), AuthUtil.getCurrentUserId());
    }

    private Map<String, Object> convertReport(SymptomReport r) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", r.getId());
        item.put("residentId", r.getResidentId());
        item.put("residentName", r.getResidentName());
        item.put("roomBedNo", r.getRoomBedNo());
        item.put("careZone", r.getCareZone());
        item.put("symptomType", r.getSymptomType());
        item.put("symptomName", r.getSymptomName());
        item.put("severity", r.getSeverity());
        item.put("severityName", r.getSeverityName());
        item.put("description", r.getDescription());
        item.put("location", r.getLocation());
        item.put("startTime", r.getStartTime());
        item.put("triggers", r.getTriggers());
        item.put("additionalInfo", r.getAdditionalInfo());
        item.put("status", r.getStatus());
        item.put("statusText", getStatusText(r.getStatus()));
        item.put("handlerId", r.getHandlerId());
        item.put("handlerName", r.getHandlerName());
        item.put("handleResult", r.getHandleResult());
        item.put("handleTime", r.getHandleTime());
        item.put("createTime", r.getCreateTime());
        return item;
    }

    private String getSymptomName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "pain" -> "疼痛";
            case "nausea" -> "恶心呕吐";
            case "fever" -> "发热";
            case "dizzy" -> "头晕";
            case "breath" -> "呼吸困难";
            case "sleep" -> "睡眠问题";
            case "appetite" -> "食欲不振";
            case "other" -> "其他";
            default -> type;
        };
    }

    private String getSeverityName(String severity) {
        if (severity == null) return "";
        return switch (severity) {
            case "mild" -> "轻微";
            case "moderate" -> "中等";
            case "severe" -> "严重";
            default -> severity;
        };
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "待处理";
            case "HANDLING" -> "处理中";
            case "HANDLED" -> "已处理";
            default -> status;
        };
    }
}
