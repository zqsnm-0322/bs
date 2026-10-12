package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.RiskRecord;
import com.graduation.elderlycare.mapper.RiskRecordMapper;
import com.graduation.elderlycare.service.ScaleEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.graduation.elderlycare.entity.AlertMsg;
import com.graduation.elderlycare.mapper.AlertMsgMapper;
import com.graduation.elderlycare.mapper.ResidentMapper;
import com.graduation.elderlycare.entity.Resident;
import com.graduation.elderlycare.service.ExportService;
import com.graduation.elderlycare.annotation.OperationLogAnnotation;
import com.graduation.elderlycare.util.AuthUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/risk")
@CrossOrigin
public class RiskRecordController {
    private static final Set<String> ELDERLY_CARE_SCALES = Set.of(
            "FALL", "PRESSURE", "MALNUTRITION", "COGNITIVE",
            "WANDERING", "DEPRESSION", "SELF_HARM", "NEGLECT");

    @Autowired
    private RiskRecordMapper riskRecordMapper;
    @Autowired
    private AlertMsgMapper alertMsgMapper;
    @Autowired
    private ResidentMapper residentMapper;
    @Autowired
    private ScaleEvaluationService scaleEvaluationService;
    @Autowired
    private ExportService exportService;

    // 获取评估记录列表
    @GetMapping("/list")
    public Result<?> list() {
        // 实际应分页，这里简化查全部
        List<RiskRecord> list = riskRecordMapper.selectListWithResident();
        list = list.stream().filter(record -> record.getScaleType() != null
                && ELDERLY_CARE_SCALES.contains(record.getScaleType().toUpperCase())).collect(Collectors.toList());
        if ("CAREGIVER".equals(AuthUtil.getCurrentRole())) {
            Long caregiverId = AuthUtil.getCurrentUserId();
            list = list.stream().filter(record -> {
                Resident resident = residentMapper.selectById(record.getResidentId());
                return resident != null && Objects.equals(resident.getCaregiverId(), caregiverId);
            }).collect(Collectors.toList());
        }
        return Result.success(list);
    }
    
    /**
     * 获取待评估老人列表
     * 返回所有 assessment_status = 'PENDING' 且 status = 'RESIDENT' 的在住老人
     */
    @GetMapping("/pending-assessment")
    public Result<?> getPendingAssessmentList(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();
            
            // 只查询待评估且在住的老人
            wrapper.eq(Resident::getStatus, "RESIDENT"); // 在住
            wrapper.eq(Resident::getAssessmentStatus, "PENDING"); // 待评估
            if ("CAREGIVER".equals(AuthUtil.getCurrentRole())) {
                wrapper.eq(Resident::getCaregiverId, AuthUtil.getCurrentUserId());
            }
            
            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.and(w -> w.like(Resident::getName, keyword)
                                 .or()
                                 .like(Resident::getRoomBedNo, keyword));
            }
            
            // 按申请时间倒序
            wrapper.orderByDesc(Resident::getAssessmentRequestTime);
            
            // 查询老人列表
            List<Resident> residents = residentMapper.selectList(wrapper);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> records = residents.stream().map(resident -> {
                Map<String, Object> record = new HashMap<>();
                record.put("residentId", resident.getId());
                record.put("residentName", resident.getName());
                record.put("bedNumber", resident.getRoomBedNo());
                record.put("gender", resident.getGender());
                record.put("age", resident.getAge());
                record.put("healthStatus", resident.getHealthStatus());
                record.put("careZone", resident.getCareZone());
                record.put("admissionDate", resident.getAdmitTime());
                record.put("requestSource", resident.getRequestSource() != null ? resident.getRequestSource() : "PC");
                record.put("requestTime", resident.getAssessmentRequestTime() != null ? 
                          resident.getAssessmentRequestTime() : resident.getCreateTime());
                record.put("status", resident.getAssessmentStatus());
                return record;
            }).collect(Collectors.toList());
            
            // 简单分页（实际应使用MyBatis-Plus的Page）
            int total = records.size();
            int start = (pageNum - 1) * pageSize;
            int end = Math.min(start + pageSize, total);
            
            List<Map<String, Object>> pageRecords = start < total ? 
                records.subList(start, end) : new java.util.ArrayList<>();
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("records", pageRecords);
            result.put("list", pageRecords); // 兼容不同的前端命名
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取待评估老人列表失败: " + e.getMessage());
        }
    }
    
    // 获取单个评估记录详情
    @GetMapping("/detail/{id}")
    public Result<?> getDetail(@PathVariable Long id) {
        RiskRecord record = riskRecordMapper.selectById(id);
        if (record == null) {
            return Result.error("评估记录不存在");
        }
        if (record.getScaleType() == null || !ELDERLY_CARE_SCALES.contains(record.getScaleType().toUpperCase())) {
            return Result.error("该记录不属于养老风险评估");
        }
        
        // 获取老人信息
        Resident resident = residentMapper.selectById(record.getResidentId());
        if (isCaregiverDenied(resident)) return Result.error("无权查看其他护理员负责的评估记录");
        
        // 解析评估详情JSON
        Map<String, Object> detailJson = null;
        if (record.getDetailJson() != null && !record.getDetailJson().isEmpty()) {
            detailJson = com.alibaba.fastjson.JSON.parseObject(
                record.getDetailJson(), 
                new com.alibaba.fastjson.TypeReference<Map<String, Object>>() {}
            );
        }
        
        // 根据评估类型重新计算干预措施
        ScaleEvaluationService.EvaluationResult evalResult = 
            scaleEvaluationService.evaluate(record.getScaleType(), detailJson);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("residentId", record.getResidentId());
        result.put("residentName", resident != null ? resident.getName() : "未知");
        result.put("bedNo", resident != null ? resident.getRoomBedNo() : "-");
        result.put("careZone", resident != null ? resident.getCareZone() : "-");
        result.put("scaleType", record.getScaleType());
        result.put("scaleName", getScaleTypeName(record.getScaleType()));
        result.put("totalScore", record.getTotalScore());
        result.put("riskLevel", record.getRiskLevel());
        result.put("riskLevelText", getRiskLevelName(record.getRiskLevel()));
        result.put("evaluatorName", record.getEvaluatorName());
        result.put("evaluateTime", record.getEvaluateTime());
        result.put("answers", detailJson);
        result.put("interventions", evalResult.getInterventions());
        
        return Result.success(result);
    }
    
    // 获取老人风险评估历史 - 支持前端老人详情页面
    @GetMapping("/history/{residentId}")
    public Result<?> getResidentRiskHistory(@PathVariable("residentId") Long residentId) {
        Resident resident = residentMapper.selectById(residentId);
        if (isCaregiverDenied(resident)) return Result.error("无权查看其他护理员负责的评估记录");
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRecord::getResidentId, residentId)
               .orderByDesc(RiskRecord::getEvaluateTime);
        
        List<RiskRecord> records = riskRecordMapper.selectList(wrapper);
        records = records.stream().filter(record -> record.getScaleType() != null
                && ELDERLY_CARE_SCALES.contains(record.getScaleType().toUpperCase())).collect(Collectors.toList());
        
        // 转换为前端需要的格式
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
            item.put("sentToResident", record.getSentToResident() != null && record.getSentToResident() == 1);
            item.put("sentTime", record.getSentTime());
            item.put("sentBy", record.getSentBy());
            return item;
        }).collect(Collectors.toList());
        
        return Result.success(result);
    }

    private boolean isCaregiverDenied(Resident resident) {
        return "CAREGIVER".equals(AuthUtil.getCurrentRole())
                && (resident == null || !Objects.equals(resident.getCaregiverId(), AuthUtil.getCurrentUserId()));
    }


    // 提交评估
    @PostMapping("/submit")
    @OperationLogAnnotation(module = "风险评估", operationType = "CREATE", description = "提交风险评估", recordParams = true)
    public Result<?> submit(@RequestBody SubmitRequest request) {
        try {
            Resident assignedResident = residentMapper.selectById(request.getResidentId());
            if (isCaregiverDenied(assignedResident)) return Result.error("无权评估其他护理员负责的老人");
            String scaleType = request.getScaleType() == null ? "" : request.getScaleType().toUpperCase();
            if (!java.util.Arrays.asList("FALL", "PRESSURE", "MALNUTRITION", "COGNITIVE",
                    "WANDERING", "DEPRESSION", "SELF_HARM", "NEGLECT").contains(scaleType)) {
                return Result.error("仅支持养老照护风险量表");
            }
            // 1. 使用动态评分服务计算评分和风险等级
            ScaleEvaluationService.EvaluationResult result = 
                scaleEvaluationService.evaluate(request.getScaleType(), request.getAnswers());
            
            // 2. 创建评估记录
            RiskRecord record = new RiskRecord();
            record.setResidentId(request.getResidentId());
            record.setEvaluatorId(request.getEvaluatorId());
            record.setEvaluatorName(request.getEvaluatorName());
            record.setScaleType(request.getScaleType());
            record.setTotalScore(result.getTotalScore());
            record.setRiskLevel(result.getRiskLevel());
            record.setDetailJson(com.alibaba.fastjson.JSON.toJSONString(request.getAnswers()));
            record.setEvaluateTime(LocalDateTime.now());
            
            // 需求11: 设置评估来源
            if (request.getRequestSource() != null) {
                record.setRequestSource(request.getRequestSource());
            }
            
            riskRecordMapper.insert(record);
            
            // 需求12: 更新老人评估状态为已完成，从待评估列表移除
            Resident resident = residentMapper.selectById(request.getResidentId());
            if (resident != null) {
                resident.setAssessmentStatus("COMPLETED");
                if (request.getRequestSource() != null) {
                    resident.setRequestSource(request.getRequestSource());
                }
                residentMapper.updateById(resident);
            }
            
            // 3. 如果是高危或中危，生成预警消息
            if ("HIGH".equals(result.getRiskLevel()) || "MEDIUM".equals(result.getRiskLevel())) {
                createAlertMessage(record, result.getInterventions());
            }
            
            // 4. 返回评估结果
            SubmitResponse response = new SubmitResponse();
            response.setRecordId(record.getId());
            response.setTotalScore(result.getTotalScore());
            response.setRiskLevel(result.getRiskLevel());
            response.setInterventions(result.getInterventions());
            
            return Result.success(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("评估失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建预警消息
     */
    private void createAlertMessage(RiskRecord record, List<String> interventions) {
        AlertMsg msg = new AlertMsg();
        
        // 获取老人信息
        Resident resident = residentMapper.selectById(record.getResidentId());
        String residentName = resident != null ? resident.getName() : "未知老人";
        String bedNo = resident != null ? resident.getRoomBedNo() : "-";
        String careZone = resident != null ? resident.getCareZone() : "-";
        
        msg.setResidentId(record.getResidentId());
        msg.setResidentName(residentName);
        msg.setRoomBedNo(bedNo);
        msg.setCareZone(careZone);
        msg.setRiskType(record.getScaleType());
        msg.setRiskLevel(record.getRiskLevel());
        msg.setRiskScore(record.getTotalScore());
        msg.setStatus("PENDING");
        
        // 构建预警消息
        String riskLevelText = "HIGH".equals(record.getRiskLevel()) ? "高危" : 
                              "MEDIUM".equals(record.getRiskLevel()) ? "中危" : "低危";
        StringBuilder message = new StringBuilder();
        message.append("老人 ").append(residentName)
               .append(" 被评估为 ").append(getScaleTypeName(record.getScaleType()))
               .append(" ").append(riskLevelText)
               .append("风险，请及时关注！");
        
        msg.setMessage(message.toString());
        
        // 保存干预措施
        if (!interventions.isEmpty()) {
            msg.setInterventions(String.join(",", interventions));
        }
        
        msg.setReceiverId(record.getEvaluatorId());
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        
        alertMsgMapper.insert(msg);
    }
    
    /**
     * 提交评估请求参数
     */
    public static class SubmitRequest {
        @JsonProperty("residentId")
        private Long residentId;
        private Long evaluatorId;
        private String evaluatorName;
        private String scaleType;
        private Map<String, Object> answers;
        private String requestSource; // 需求11: 评估来源 PC/MINIPROGRAM
        
        // Getters and Setters
        public Long getResidentId() { return residentId; }
        public void setResidentId(Long residentId) { this.residentId = residentId; }
        public Long getEvaluatorId() { return evaluatorId; }
        public void setEvaluatorId(Long evaluatorId) { this.evaluatorId = evaluatorId; }
        public String getEvaluatorName() { return evaluatorName; }
        public void setEvaluatorName(String evaluatorName) { this.evaluatorName = evaluatorName; }
        public String getScaleType() { return scaleType; }
        public void setScaleType(String scaleType) { this.scaleType = scaleType; }
        public Map<String, Object> getAnswers() { return answers; }
        public void setAnswers(Map<String, Object> answers) { this.answers = answers; }
        public String getRequestSource() { return requestSource; }
        public void setRequestSource(String requestSource) { this.requestSource = requestSource; }
    }
    
    /**
     * 提交评估响应结果
     */
    public static class SubmitResponse {
        private Long recordId;
        private Integer totalScore;
        private String riskLevel;
        private List<String> interventions;
        
        // Getters and Setters
        public Long getRecordId() { return recordId; }
        public void setRecordId(Long recordId) { this.recordId = recordId; }
        public Integer getTotalScore() { return totalScore; }
        public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public List<String> getInterventions() { return interventions; }
        public void setInterventions(List<String> interventions) { this.interventions = interventions; }
    }
    
    /**
     * 导出风险评估记录到Excel
     */
    @GetMapping("/export/excel")
    @OperationLogAnnotation(module = "风险评估", operationType = "EXPORT", description = "导出评估记录到Excel")
    public void exportToExcel(HttpServletResponse response,
                             @RequestParam(required = false) String scaleType,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate) {
        try {
            exportService.exportRiskRecordsToExcel(response, scaleType, startDate, endDate);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败: " + e.getMessage());
        }
    }

    /**
     * 需求7: 发送评估报告给老人
     */
    @PostMapping("/send-to-resident")
    @OperationLogAnnotation(module = "风险评估", operationType = "UPDATE", description = "发送评估报告给老人及家属")
    public Result<?> sendAssessmentToResident(@RequestParam Long assessmentId, 
                                            @RequestParam("residentId") Long residentId,
                                            @RequestParam(required = false) String sentBy) {
        try {
            // 查询评估记录
            RiskRecord record = riskRecordMapper.selectById(assessmentId);
            
            if (record == null || !record.getResidentId().equals(residentId)) {
                return Result.error("评估记录不存在");
            }
            if (isCaregiverDenied(residentMapper.selectById(residentId))) return Result.error("无权操作其他护理员负责的老人评估");
            
            // 更新发送状态
            record.setSentToResident(1);
            record.setSentTime(LocalDateTime.now());
            record.setSentBy(sentBy != null ? sentBy : "系统");
            
            riskRecordMapper.updateById(record);
            
            return Result.success("评估报告已发送给老人及家属");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送失败: " + e.getMessage());
        }
    }

    /**
     * 需求7: 获取老人的评估报告列表（小程序端使用）
     * 返回已发送给老人的所有评估报告
     */
    @GetMapping("/resident-reports/{residentId}")
    public Result<?> getResidentAssessmentReports(@PathVariable("residentId") Long residentId) {
        try {
            if (isCaregiverDenied(residentMapper.selectById(residentId))) return Result.error("无权查看其他护理员负责的老人评估");
            // 查询已发送给老人的评估记录
            LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskRecord::getResidentId, residentId);
            wrapper.eq(RiskRecord::getSentToResident, 1); // 只查询已发送的
            wrapper.orderByDesc(RiskRecord::getEvaluateTime);
            
            List<RiskRecord> records = riskRecordMapper.selectList(wrapper);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> reports = records.stream().map(record -> {
                Map<String, Object> report = new HashMap<>();
                report.put("id", record.getId());
                report.put("residentId", record.getResidentId());
                report.put("scaleType", record.getScaleType());
                report.put("scaleName", getScaleName(record.getScaleType()));
                report.put("totalScore", record.getTotalScore());
                report.put("riskLevel", record.getRiskLevel());
                report.put("riskLevelText", getRiskLevelName(record.getRiskLevel()));
                report.put("suggestion", record.getSuggestion());
                report.put("evaluatorName", record.getEvaluatorName());
                report.put("evaluateTime", record.getEvaluateTime());
                report.put("sentTime", record.getSentTime());
                report.put("sentBy", record.getSentBy());
                
                return report;
            }).collect(Collectors.toList());
            
            return Result.success(reports);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取评估报告失败: " + e.getMessage());
        }
    }

    /**
     * 获取量表名称（完整版）
     */
    private String getScaleName(String scaleType) {
        if (scaleType == null) return "未知量表";
        
        switch (scaleType.toUpperCase()) {
            case "FALL":
            case "MORSE":
                return "跌倒风险评估";
            case "PRESSURE":
            case "BRADEN":
                return "压疮风险评估";
            case "COGNITIVE": return "认知障碍风险评估";
            case "MALNUTRITION": return "营养不良风险评估";
            case "DELIRIUM": return "谵妄风险评估";
            case "ABUSE": return "虐待与忽视风险评估";
            case "SELF_HARM": return "自伤风险评估";
            case "DEPRESSION": return "老年抑郁风险评估";
            default:
                return scaleType;
        }
    }
    
    // 工具方法
    private String getScaleTypeName(String scaleType) {
        switch (scaleType) {
            case "FALL": return "跌倒风险评分";
            case "PRESSURE": return "压疮风险评分";
            case "COGNITIVE": return "认知障碍评分";
            case "MALNUTRITION": return "营养不良评分";
            case "DELIRIUM": return "谵妄评分";
            case "ABUSE": return "虐待与忽视评分";
            case "SELF_HARM": return "自伤风险评分";
            case "DEPRESSION": return "老年抑郁评分";
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


