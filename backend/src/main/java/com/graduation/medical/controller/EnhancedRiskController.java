package com.graduation.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.medical.common.Result;
import com.graduation.medical.entity.*;
import com.graduation.medical.mapper.*;
import com.graduation.medical.service.EnhancedRiskPredictionService;
import com.graduation.medical.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enhanced-risk")
@CrossOrigin
public class EnhancedRiskController {

    @Autowired
    private EnhancedRiskPredictionService riskPredictionService;
    
    @Autowired
    private ResidentHealthHistoryMapper healthHistoryMapper;
    
    @Autowired
    private VitalSignsMapper vitalSignsMapper;
    
    @Autowired
    private RiskAssessmentMapper riskAssessmentMapper;
    
    @Autowired
    private ResidentMapper residentMapper;
    
    @Autowired
    private NotificationService notificationService;

    /**
     * 执行综合风险评估
     */
    @PostMapping("/assess/{residentId}")
    public Result<?> performRiskAssessment(@PathVariable("residentId") Long residentId) {
        try {
            RiskAssessment assessment = riskPredictionService.performComprehensiveRiskAssessment(residentId);
            
            // 更新老人的当前风险状态
            updateResidentRiskStatus(residentId, assessment);
            
            return Result.success(assessment);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("风险评估失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新老人的当前风险状态
     */
    private void updateResidentRiskStatus(Long residentId, RiskAssessment assessment) {
        try {
            Resident resident = residentMapper.selectById(residentId);
            if (resident != null) {
                // 构建风险状态JSON
                Map<String, Object> riskStatus = new HashMap<>();
                riskStatus.put("riskLevel", assessment.getRiskLevel());
                riskStatus.put("totalScore", assessment.getTotalScore());
                riskStatus.put("confidenceLevel", assessment.getConfidenceLevel());
                riskStatus.put("assessmentTime", assessment.getAssessmentTime());
                riskStatus.put("assessmentType", assessment.getAssessmentType());
                
                // 转换为JSON字符串
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String riskJson = mapper.writeValueAsString(riskStatus);
                
                resident.setCurrentRisks(riskJson);
                residentMapper.updateById(resident);
                
                System.out.println("已更新老人 " + residentId + " 的风险状态: " + assessment.getRiskLevel());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("更新老人风险状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取老人风险评估历史
     */
    @GetMapping("/history/{residentId}")
    public Result<?> getRiskAssessmentHistory(@PathVariable("residentId") Long residentId,
                                            @RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size) {
        Page<RiskAssessment> page = new Page<>(current, size);
        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAssessment::getResidentId, residentId);
        wrapper.orderByDesc(RiskAssessment::getAssessmentTime);
        
        Page<RiskAssessment> result = riskAssessmentMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    /**
     * 添加老人健康史
     */
    @PostMapping("/health-history")
    public Result<?> addHealthHistory(@RequestBody ResidentHealthHistory healthHistory) {
        System.out.println("========== 开始添加病史 ==========");
        System.out.println("接收到的病史数据: " + healthHistory);
        System.out.println("老人ID: " + healthHistory.getResidentId());
        System.out.println("健康状况名称: " + healthHistory.getConditionName());
        System.out.println("健康状况编码: " + healthHistory.getConditionCode());
        System.out.println("健康记录日期: " + healthHistory.getRecordDate());
        System.out.println("严重程度: " + healthHistory.getSeverity());
        System.out.println("健康状况状态: " + healthHistory.getStatus());
        System.out.println("备注: " + healthHistory.getNotes());
        
        // 设置创建时间
        if (healthHistory.getCreateTime() == null) {
            healthHistory.setCreateTime(new Date());
        }
        if (healthHistory.getUpdateTime() == null) {
            healthHistory.setUpdateTime(new Date());
        }
        
        System.out.println("准备插入数据库...");
        int result = healthHistoryMapper.insert(healthHistory);
        System.out.println("插入结果: " + result);
        System.out.println("生成的ID: " + healthHistory.getId());
        System.out.println("========== 添加病史完成 ==========");
        
        return Result.success(healthHistory.getId());
    }

    /**
     * 更新老人健康史
     */
    @PutMapping("/health-history/{id}")
    public Result<?> updateHealthHistory(@PathVariable Long id, @RequestBody ResidentHealthHistory healthHistory) {
        try {
            healthHistory.setId(id);
            healthHistory.setUpdateTime(new Date());
            healthHistoryMapper.updateById(healthHistory);
            return Result.success("更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新病史失败: " + e.getMessage());
        }
    }

    /**
     * 删除老人健康史
     */
    @DeleteMapping("/health-history/{id}")
    public Result<?> deleteHealthHistory(@PathVariable Long id) {
        try {
            healthHistoryMapper.deleteById(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除病史失败: " + e.getMessage());
        }
    }

    /**
     * 获取老人健康史
     */
    @GetMapping("/health-history/{residentId}")
    public Result<?> getHealthHistory(@PathVariable("residentId") Long residentId) {
        LambdaQueryWrapper<ResidentHealthHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResidentHealthHistory::getResidentId, residentId);
        wrapper.orderByDesc(ResidentHealthHistory::getRecordDate);
        
        List<ResidentHealthHistory> histories = healthHistoryMapper.selectList(wrapper);
        return Result.success(histories);
    }

    /**
     * 添加生命体征数据
     */
    @PostMapping("/vital-signs")
    public Result<?> addVitalSigns(@RequestBody VitalSigns vitalSigns) {
        try {
            if (vitalSigns.getCreateTime() == null) {
                vitalSigns.setCreateTime(new Date());
            }
            if (vitalSigns.getMeasurementTime() == null) {
                vitalSigns.setMeasurementTime(new Date());
            }
            // 同步新旧字段
            vitalSigns.syncFields();
            vitalSignsMapper.insert(vitalSigns);
            return Result.success(vitalSigns.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("添加生命体征失败: " + e.getMessage());
        }
    }

    /**
     * 更新生命体征数据
     */
    @PutMapping("/vital-signs/{id}")
    public Result<?> updateVitalSigns(@PathVariable Long id, @RequestBody VitalSigns vitalSigns) {
        try {
            vitalSigns.setId(id);
            vitalSigns.syncFields();
            vitalSignsMapper.updateById(vitalSigns);
            return Result.success("更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新生命体征失败: " + e.getMessage());
        }
    }

    /**
     * 删除生命体征数据
     */
    @DeleteMapping("/vital-signs/{id}")
    public Result<?> deleteVitalSigns(@PathVariable Long id) {
        try {
            vitalSignsMapper.deleteById(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除生命体征失败: " + e.getMessage());
        }
    }

    /**
     * 获取老人生命体征趋势
     */
    @GetMapping("/vital-signs/{residentId}")
    public Result<?> getVitalSignsTrend(@PathVariable("residentId") Long residentId,
                                       @RequestParam(defaultValue = "24") Integer hours) {
        LambdaQueryWrapper<VitalSigns> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VitalSigns::getResidentId, residentId);
        wrapper.ge(VitalSigns::getMeasurementTime, 
                  new java.util.Date(System.currentTimeMillis() - hours * 60 * 60 * 1000L));
        wrapper.orderByAsc(VitalSigns::getMeasurementTime);
        
        List<VitalSigns> vitalSigns = vitalSignsMapper.selectList(wrapper);
        return Result.success(vitalSigns);
    }

    /**
     * 获取风险评估详情
     */
    @GetMapping("/assessment/{assessmentId}")
    public Result<?> getAssessmentDetail(@PathVariable Long assessmentId) {
        RiskAssessment assessment = riskAssessmentMapper.selectById(assessmentId);
        if (assessment == null) {
            return Result.error("风险评估记录不存在");
        }
        return Result.success(assessment);
    }

    /**
     * 获取老人综合风险概览
     */
    @GetMapping("/overview/{residentId}")
    public Result<?> getRiskOverview(@PathVariable("residentId") Long residentId) {
        Map<String, Object> overview = new HashMap<>();
        
        // 最新风险评估
        LambdaQueryWrapper<RiskAssessment> assessmentWrapper = new LambdaQueryWrapper<>();
        assessmentWrapper.eq(RiskAssessment::getResidentId, residentId);
        assessmentWrapper.orderByDesc(RiskAssessment::getAssessmentTime);
        assessmentWrapper.last("LIMIT 1");
        RiskAssessment latestAssessment = riskAssessmentMapper.selectOne(assessmentWrapper);
        
        // 健康史统计
        LambdaQueryWrapper<ResidentHealthHistory> historyWrapper = new LambdaQueryWrapper<>();
        historyWrapper.eq(ResidentHealthHistory::getResidentId, residentId);
        Long historyCount = healthHistoryMapper.selectCount(historyWrapper);
        
        // 最新生命体征
        LambdaQueryWrapper<VitalSigns> vitalWrapper = new LambdaQueryWrapper<>();
        vitalWrapper.eq(VitalSigns::getResidentId, residentId);
        vitalWrapper.orderByDesc(VitalSigns::getMeasurementTime);
        vitalWrapper.last("LIMIT 1");
        VitalSigns latestVital = vitalSignsMapper.selectOne(vitalWrapper);
        
        overview.put("latestAssessment", latestAssessment);
        overview.put("healthHistoryCount", historyCount);
        overview.put("latestVitalSigns", latestVital);
        
        return Result.success(overview);
    }

    /**
     * 获取老人风险评估统计
     */
    @GetMapping("/statistics/{residentId}")
    public Result<?> getRiskStatistics(@PathVariable("residentId") Long residentId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总评估数
        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAssessment::getResidentId, residentId);
        Long total = riskAssessmentMapper.selectCount(wrapper);
        
        // 高风险评估数
        LambdaQueryWrapper<RiskAssessment> highRiskWrapper = new LambdaQueryWrapper<>();
        highRiskWrapper.eq(RiskAssessment::getResidentId, residentId);
        highRiskWrapper.in(RiskAssessment::getRiskLevel, "HIGH", "CRITICAL");
        Long highRisk = riskAssessmentMapper.selectCount(highRiskWrapper);
        
        // 中风险评估数
        LambdaQueryWrapper<RiskAssessment> mediumRiskWrapper = new LambdaQueryWrapper<>();
        mediumRiskWrapper.eq(RiskAssessment::getResidentId, residentId);
        mediumRiskWrapper.eq(RiskAssessment::getRiskLevel, "MEDIUM");
        Long mediumRisk = riskAssessmentMapper.selectCount(mediumRiskWrapper);
        
        // 低风险评估数
        LambdaQueryWrapper<RiskAssessment> lowRiskWrapper = new LambdaQueryWrapper<>();
        lowRiskWrapper.eq(RiskAssessment::getResidentId, residentId);
        lowRiskWrapper.eq(RiskAssessment::getRiskLevel, "LOW");
        Long lowRisk = riskAssessmentMapper.selectCount(lowRiskWrapper);
        
        statistics.put("total", total);
        statistics.put("highRisk", highRisk);
        statistics.put("mediumRisk", mediumRisk);
        statistics.put("lowRisk", lowRisk);
        
        return Result.success(statistics);
    }

    /**
     * 获取老人健康状况预测
     */
    @GetMapping("/predictions/{residentId}")
    public Result<?> getHealthPredictions(@PathVariable("residentId") Long residentId) {
        try {
            // 获取最新的风险评估记录
            LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskAssessment::getResidentId, residentId);
            wrapper.orderByDesc(RiskAssessment::getAssessmentTime);
            wrapper.last("LIMIT 1");
            RiskAssessment latestAssessment = riskAssessmentMapper.selectOne(wrapper);
            
            if (latestAssessment == null) {
                return Result.error("该老人暂无风险评估记录，请先进行风险评估");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("assessmentId", latestAssessment.getId());
            result.put("assessmentTime", latestAssessment.getAssessmentTime());
            result.put("riskLevel", latestAssessment.getRiskLevel());
            result.put("totalScore", latestAssessment.getTotalScore());
            result.put("confidenceLevel", latestAssessment.getConfidenceLevel());
            
            // 解析健康预测JSON
            if (latestAssessment.getHealthPredictions() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                try {
                    List<Map<String, Object>> predictions = mapper.readValue(
                        latestAssessment.getHealthPredictions(), 
                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                    );
                    result.put("predictions", predictions);
                } catch (Exception e) {
                    result.put("predictions", new java.util.ArrayList<>());
                }
            } else {
                result.put("predictions", new java.util.ArrayList<>());
            }
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取健康预测失败: " + e.getMessage());
        }
    }

    /**
     * 发送预测结果给照护主管
     */
    @PostMapping("/predictions/notify/{residentId}")
    public Result<?> notifyCareManagerPredictions(@PathVariable("residentId") Long residentId, 
                                           @RequestParam(required = false) String careManagerId,
                                           @RequestParam(required = false) String message) {
        try {
            // 获取最新的健康预测
            LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskAssessment::getResidentId, residentId);
            wrapper.orderByDesc(RiskAssessment::getAssessmentTime);
            wrapper.last("LIMIT 1");
            RiskAssessment latestAssessment = riskAssessmentMapper.selectOne(wrapper);
            
            if (latestAssessment == null) {
                return Result.error("该老人暂无风险评估记录");
            }
            
            // 获取老人信息
            Resident resident = residentMapper.selectById(residentId);
            if (resident == null) {
                return Result.error("老人信息不存在");
            }
            
            // 构建通知内容
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "HEALTH_PREDICTION");
            notification.put("residentId", residentId);
            notification.put("residentName", resident.getName());
            notification.put("riskLevel", latestAssessment.getRiskLevel());
            notification.put("totalScore", latestAssessment.getTotalScore());
            notification.put("assessmentTime", latestAssessment.getAssessmentTime());
            notification.put("predictions", latestAssessment.getHealthPredictions());
            notification.put("message", message != null ? message : "系统基于老人当前状况生成了照护风险提醒，请及时关注");
            notification.put("createTime", new Date());
            
            // 发送通知给照护主管
            notificationService.sendHealthPredictionNotification(
                residentId, 
                resident.getName(), 
                latestAssessment.getHealthPredictions(),
                careManagerId
            );
            
            return Result.success(notification);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送预测结果失败: " + e.getMessage());
        }
    }

    /**
     * 生成可分享的风险评估报告（简化版）
     * 老人可以查看此报告
     */
    @PostMapping("/share-report/{residentId}")
    public Result<?> shareRiskReport(@PathVariable("residentId") Long residentId, 
                                     @RequestParam(required = false) Long assessmentId) {
        try {
            RiskAssessment assessment;
            
            if (assessmentId != null) {
                // 获取指定的评估记录
                assessment = riskAssessmentMapper.selectById(assessmentId);
                if (assessment == null || !assessment.getResidentId().equals(residentId)) {
                    return Result.error("评估记录不存在或不属于该老人");
                }
            } else {
                // 获取最新的评估记录
                LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(RiskAssessment::getResidentId, residentId);
                wrapper.orderByDesc(RiskAssessment::getAssessmentTime);
                wrapper.last("LIMIT 1");
                assessment = riskAssessmentMapper.selectOne(wrapper);
                
                if (assessment == null) {
                    return Result.error("该老人暂无风险评估记录");
                }
            }
            
            // 获取老人信息
            Resident resident = residentMapper.selectById(residentId);
            if (resident == null) {
                return Result.error("老人信息不存在");
            }
            
            // 构建简化版报告（隐藏敏感医学细节）
            Map<String, Object> report = new HashMap<>();
            report.put("reportId", "RISK_" + assessment.getId());
            report.put("residentName", resident.getName());
            report.put("assessmentTime", assessment.getAssessmentTime());
            report.put("riskLevel", assessment.getRiskLevel());
            report.put("riskLevelText", getRiskLevelText(assessment.getRiskLevel()));
            report.put("totalScore", assessment.getTotalScore());
            report.put("confidenceLevel", assessment.getConfidenceLevel());
            
            // 简化的风险因素（不包含详细医学数据）
            Map<String, Object> simplifiedFactors = new HashMap<>();
            simplifiedFactors.put("hasHighRisk", "HIGH".equals(assessment.getRiskLevel()) || "CRITICAL".equals(assessment.getRiskLevel()));
            simplifiedFactors.put("needAttention", assessment.getTotalScore() != null && assessment.getTotalScore().compareTo(new java.math.BigDecimal("50")) >= 0);
            
            // 通用健康建议（不包含具体治疗方案）
            List<String> generalAdvice = new java.util.ArrayList<>();
            if ("HIGH".equals(assessment.getRiskLevel()) || "CRITICAL".equals(assessment.getRiskLevel())) {
                generalAdvice.add("建议密切关注身体状况");
                generalAdvice.add("按时服药，遵医嘱");
                generalAdvice.add("如有不适请及时就医");
                generalAdvice.add("保持良好的作息习惯");
            } else if ("MEDIUM".equals(assessment.getRiskLevel())) {
                generalAdvice.add("注意日常健康管理");
                generalAdvice.add("定期复查");
                generalAdvice.add("保持健康的生活方式");
            } else {
                generalAdvice.add("继续保持良好的健康状态");
                generalAdvice.add("定期体检");
            }
            
            report.put("riskFactors", simplifiedFactors);
            report.put("healthAdvice", generalAdvice);
            report.put("shareTime", new Date());
            report.put("validUntil", new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)); // 7天有效期
            
            // 记录分享操作（可选：保存到数据库）
            System.out.println("风险评估报告已分享给老人: " + residentId + ", 评估ID: " + assessment.getId());
            
            return Result.success(report);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成分享报告失败: " + e.getMessage());
        }
    }

    /**
     * 老人查看自己的风险评估报告（简化版）
     */
    @GetMapping("/resident-report/{residentId}")
    public Result<?> getResidentRiskReport(@PathVariable("residentId") Long residentId) {
        try {
            // 获取最新的评估记录
            LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskAssessment::getResidentId, residentId);
            wrapper.orderByDesc(RiskAssessment::getAssessmentTime);
            wrapper.last("LIMIT 1");
            RiskAssessment assessment = riskAssessmentMapper.selectOne(wrapper);
            
            if (assessment == null) {
                return Result.error("暂无风险评估记录");
            }
            
            // 获取老人信息
            Resident resident = residentMapper.selectById(residentId);
            
            // 构建简化版报告
            Map<String, Object> report = new HashMap<>();
            report.put("assessmentTime", assessment.getAssessmentTime());
            report.put("riskLevel", assessment.getRiskLevel());
            report.put("riskLevelText", getRiskLevelText(assessment.getRiskLevel()));
            report.put("riskLevelColor", getRiskLevelColor(assessment.getRiskLevel()));
            
            // 健康状态描述
            String healthStatus = getHealthStatusDescription(assessment.getRiskLevel());
            report.put("healthStatus", healthStatus);
            
            // 通用健康建议
            List<String> advice = getGeneralHealthAdvice(assessment.getRiskLevel());
            report.put("healthAdvice", advice);
            
            return Result.success(report);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取报告失败: " + e.getMessage());
        }
    }

    /**
     * 需求7: 获取老人的评估报告列表（小程序端使用）
     * 返回已发送给老人的所有评估报告
     */
    @GetMapping("/resident-reports/{residentId}")
    public Result<?> getResidentAssessmentReports(@PathVariable("residentId") Long residentId) {
        try {
            // 查询已发送给老人的评估记录
            LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskAssessment::getResidentId, residentId);
            wrapper.eq(RiskAssessment::getSentToResident, 1); // 只查询已发送的
            wrapper.orderByDesc(RiskAssessment::getAssessmentTime);
            
            List<RiskAssessment> assessments = riskAssessmentMapper.selectList(wrapper);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> reports = new java.util.ArrayList<>();
            for (RiskAssessment assessment : assessments) {
                Map<String, Object> report = new HashMap<>();
                report.put("id", assessment.getId());
                report.put("residentId", assessment.getResidentId());
                report.put("scaleType", assessment.getScaleType());
                report.put("scaleName", getScaleName(assessment.getScaleType()));
                report.put("totalScore", assessment.getTotalScore());
                report.put("riskLevel", assessment.getRiskLevel());
                report.put("riskLevelText", getRiskLevelText(assessment.getRiskLevel()));
                report.put("suggestion", assessment.getSuggestion());
                report.put("evaluatorName", assessment.getEvaluatorName());
                report.put("evaluateTime", assessment.getAssessmentTime());
                report.put("sentTime", assessment.getSentTime());
                report.put("sentBy", assessment.getSentBy());
                
                reports.add(report);
            }
            
            return Result.success(reports);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取评估报告失败: " + e.getMessage());
        }
    }

    /**
     * 需求7: 发送评估报告给老人
     */
    @PostMapping("/send-to-resident")
    public Result<?> sendAssessmentToResident(@RequestParam Long assessmentId, 
                                            @RequestParam("residentId") Long residentId,
                                            @RequestParam(required = false) String sentBy) {
        try {
            // 查询评估记录
            RiskAssessment assessment = riskAssessmentMapper.selectById(assessmentId);
            
            if (assessment == null || !assessment.getResidentId().equals(residentId)) {
                return Result.error("评估记录不存在");
            }
            
            // 更新发送状态
            assessment.setSentToResident(1);
            assessment.setSentTime(new Date());
            assessment.setSentBy(sentBy != null ? sentBy : "系统");
            
            riskAssessmentMapper.updateById(assessment);
            
            return Result.success("评估报告已发送给老人");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送失败: " + e.getMessage());
        }
    }

    /**
     * 获取量表名称
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

    /**
     * 获取风险等级文本
     */
    private String getRiskLevelText(String riskLevel) {
        switch (riskLevel) {
            case "LOW": return "低风险";
            case "MEDIUM": return "中风险";
            case "HIGH": return "高风险";
            case "CRITICAL": return "极高风险";
            default: return "未知";
        }
    }

    /**
     * 获取风险等级颜色
     */
    private String getRiskLevelColor(String riskLevel) {
        switch (riskLevel) {
            case "LOW": return "#52c41a";
            case "MEDIUM": return "#faad14";
            case "HIGH": return "#fa8c16";
            case "CRITICAL": return "#f5222d";
            default: return "#999999";
        }
    }

    /**
     * 获取健康状态描述
     */
    private String getHealthStatusDescription(String riskLevel) {
        switch (riskLevel) {
            case "LOW": 
                return "您的健康状况良好，请继续保持。";
            case "MEDIUM": 
                return "您的健康状况需要关注，建议加强日常健康管理。";
            case "HIGH": 
                return "您的健康状况需要重点关注，请遵医嘱并密切监测。";
            case "CRITICAL": 
                return "您的健康状况需要高度重视，请严格遵医嘱并及时就医。";
            default: 
                return "健康状况评估中。";
        }
    }

    /**
     * 获取通用健康建议
     */
    private List<String> getGeneralHealthAdvice(String riskLevel) {
        List<String> advice = new java.util.ArrayList<>();
        
        switch (riskLevel) {
            case "LOW":
                advice.add("保持规律作息，充足睡眠");
                advice.add("均衡饮食，适量运动");
                advice.add("定期体检，预防为主");
                break;
            case "MEDIUM":
                advice.add("注意日常健康监测");
                advice.add("按时服药，遵医嘱");
                advice.add("避免过度劳累");
                advice.add("定期复查");
                break;
            case "HIGH":
            case "CRITICAL":
                advice.add("严格遵医嘱用药");
                advice.add("密切关注身体变化");
                advice.add("如有不适立即就医");
                advice.add("保持良好心态");
                advice.add("家属应加强陪护");
                break;
        }
        
        return advice;
    }

    /**
     * 分享健康预测结果给老人
     * 老人可以在小程序中查看预测结果
     */
    @PostMapping("/share-predictions/{residentId}")
    public Result<?> sharePredictionsToResident(@PathVariable("residentId") Long residentId,
                                               @RequestBody Map<String, Object> requestData) {
        try {
            // 获取老人信息
            Resident resident = residentMapper.selectById(residentId);
            if (resident == null) {
                return Result.error("老人信息不存在");
            }
            
            // 获取预测数据
            List<Map<String, Object>> predictions = (List<Map<String, Object>>) requestData.get("predictions");
            String assessmentTime = (String) requestData.get("assessmentTime");
            String riskLevel = (String) requestData.get("riskLevel");
            Object totalScore = requestData.get("totalScore");
            Object confidenceLevel = requestData.get("confidenceLevel");
            
            if (predictions == null || predictions.isEmpty()) {
                return Result.error("预测数据为空");
            }
            
            // 获取最新的风险评估记录
            LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RiskAssessment::getResidentId, residentId);
            wrapper.orderByDesc(RiskAssessment::getAssessmentTime);
            wrapper.last("LIMIT 1");
            RiskAssessment latestAssessment = riskAssessmentMapper.selectOne(wrapper);
            
            if (latestAssessment != null) {
                // 更新评估记录，标记为已分享
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String predictionsJson = mapper.writeValueAsString(predictions);
                    latestAssessment.setHealthPredictions(predictionsJson);
                    // 可以添加一个字段标记已分享，这里暂时通过更新时间来标识
                    riskAssessmentMapper.updateById(latestAssessment);
                } catch (Exception e) {
                    System.err.println("保存预测数据失败: " + e.getMessage());
                }
            }
            
            // 构建老人可查看的简化版预测报告
            Map<String, Object> residentReport = new HashMap<>();
            residentReport.put("reportId", "PRED_" + residentId + "_" + System.currentTimeMillis());
            residentReport.put("residentName", resident.getName());
            residentReport.put("assessmentTime", assessmentTime);
            residentReport.put("riskLevel", riskLevel);
            residentReport.put("riskLevelText", getRiskLevelText(riskLevel));
            residentReport.put("totalScore", totalScore);
            residentReport.put("confidenceLevel", confidenceLevel);
            
            // 简化预测信息（移除过于专业的医学术语）
            List<Map<String, Object>> simplifiedPredictions = new java.util.ArrayList<>();
            for (Map<String, Object> prediction : predictions) {
                Map<String, Object> simplified = new HashMap<>();
                simplified.put("condition", prediction.get("condition"));
                simplified.put("severity", prediction.get("severity"));
                simplified.put("probability", prediction.get("probability"));
                simplified.put("timeframe", prediction.get("timeframe"));
                simplified.put("symptoms", prediction.get("symptoms"));
                simplified.put("interventions", prediction.get("interventions"));
                // 不包含statisticalBasis等专业信息
                simplifiedPredictions.add(simplified);
            }
            
            residentReport.put("predictions", simplifiedPredictions);
            residentReport.put("healthAdvice", getGeneralHealthAdvice(riskLevel));
            residentReport.put("healthStatus", getHealthStatusDescription(riskLevel));
            residentReport.put("shareTime", new Date());
            residentReport.put("validUntil", new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)); // 30天有效期
            
            // 发送通知给老人（可选功能，如果需要可以实现）
            // 注：当前NotificationService主要用于照护主管通知，老人通知可以通过小程序推送实现
            
            // 记录分享操作
            System.out.println("健康预测结果已分享给老人: " + residentId + ", 预测数量: " + predictions.size());
            
            return Result.success(residentReport);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("分享预测结果失败: " + e.getMessage());
        }
    }
}
