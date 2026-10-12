package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.Resident;
import com.graduation.elderlycare.entity.RiskRecord;
import com.graduation.elderlycare.mapper.ResidentMapper;
import com.graduation.elderlycare.mapper.RiskRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin
public class StatisticsController {

    private static final List<String> ELDERLY_RISK_TYPES = List.of(
        "FALL", "PRESSURE", "COGNITIVE", "MALNUTRITION",
        "DELIRIUM", "ABUSE", "SELF_HARM", "DEPRESSION"
    );

    @Autowired
    private ResidentMapper residentMapper;
    
    @Autowired
    private RiskRecordMapper riskRecordMapper;

    // 获取统计报表数据 - 支持前端统计报表页面
    @GetMapping("/report")
    public Result<?> getStatisticsReport(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String careZone,
                                        @RequestParam(required = false) String scaleType) {
        
        LocalDateTime start = startDate != null ? 
            LocalDate.parse(startDate).atStartOfDay() : 
            LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = endDate != null ? 
            LocalDate.parse(endDate).atTime(23, 59, 59) : 
            LocalDateTime.now();
        
        // 1. 总评估次数 - 从数据库统计
        LambdaQueryWrapper<RiskRecord> assessmentWrapper = new LambdaQueryWrapper<>();
        assessmentWrapper.between(RiskRecord::getEvaluateTime, start, end);
        if (scaleType != null && !scaleType.trim().isEmpty()) {
            assessmentWrapper.eq(RiskRecord::getScaleType, scaleType);
        }
        int totalAssessments = Math.toIntExact(riskRecordMapper.selectCount(assessmentWrapper));
        
        // 2. 高危老人数量 - 从数据库统计（去重老人ID）
        LambdaQueryWrapper<RiskRecord> highRiskWrapper = new LambdaQueryWrapper<>();
        highRiskWrapper.between(RiskRecord::getEvaluateTime, start, end)
                      .eq(RiskRecord::getRiskLevel, "HIGH");
        if (scaleType != null && !scaleType.trim().isEmpty()) {
            highRiskWrapper.eq(RiskRecord::getScaleType, scaleType);
        }
        List<RiskRecord> highRiskRecords = riskRecordMapper.selectList(highRiskWrapper);
        // 去重统计高危老人数量
        long highRiskResidents = highRiskRecords.stream()
            .map(RiskRecord::getResidentId)
            .distinct()
            .count();
        
        // 3. 护理干预次数 - 从数据库统计（高危和中危都需要干预）
        LambdaQueryWrapper<RiskRecord> interventionWrapper = new LambdaQueryWrapper<>();
        interventionWrapper.between(RiskRecord::getEvaluateTime, start, end)
                          .in(RiskRecord::getRiskLevel, "HIGH", "MEDIUM");
        if (scaleType != null && !scaleType.trim().isEmpty()) {
            interventionWrapper.eq(RiskRecord::getScaleType, scaleType);
        }
        int interventionCount = Math.toIntExact(riskRecordMapper.selectCount(interventionWrapper));
        
        // 4. 评估及时率 - 从数据库计算（假设24小时内完成评估为及时）
        // 获取所有在住老人数量
        LambdaQueryWrapper<Resident> residentWrapper = new LambdaQueryWrapper<>();
        residentWrapper.eq(Resident::getStatus, "RESIDENT"); // 在住状态
        long totalResidents = residentMapper.selectCount(residentWrapper);
        
        // 计算本月应评估次数（每个老人至少评估一次）
        long expectedAssessments = totalResidents > 0 ? totalResidents : 1;
        
        // 计算及时率（实际评估次数 / 应评估次数 * 100）
        double timelyRate = totalResidents > 0 
            ? Math.min((totalAssessments * 100.0 / expectedAssessments), 100.0)
            : 0.0;
        
        // 5. 计算趋势（与上月对比）
        LocalDateTime lastMonthStart = start.minusMonths(1);
        LocalDateTime lastMonthEnd = end.minusMonths(1);
        
        // 上月评估次数
        LambdaQueryWrapper<RiskRecord> lastMonthWrapper = new LambdaQueryWrapper<>();
        lastMonthWrapper.between(RiskRecord::getEvaluateTime, lastMonthStart, lastMonthEnd);
        int lastMonthAssessments = Math.toIntExact(riskRecordMapper.selectCount(lastMonthWrapper));
        
        // 上月高危老人数
        LambdaQueryWrapper<RiskRecord> lastMonthHighRiskWrapper = new LambdaQueryWrapper<>();
        lastMonthHighRiskWrapper.between(RiskRecord::getEvaluateTime, lastMonthStart, lastMonthEnd)
                               .eq(RiskRecord::getRiskLevel, "HIGH");
        List<RiskRecord> lastMonthHighRiskRecords = riskRecordMapper.selectList(lastMonthHighRiskWrapper);
        long lastMonthHighRiskResidents = lastMonthHighRiskRecords.stream()
            .map(RiskRecord::getResidentId)
            .distinct()
            .count();
        
        // 上月干预次数
        LambdaQueryWrapper<RiskRecord> lastMonthInterventionWrapper = new LambdaQueryWrapper<>();
        lastMonthInterventionWrapper.between(RiskRecord::getEvaluateTime, lastMonthStart, lastMonthEnd)
                                   .in(RiskRecord::getRiskLevel, "HIGH", "MEDIUM");
        int lastMonthInterventions = Math.toIntExact(riskRecordMapper.selectCount(lastMonthInterventionWrapper));
        
        // 计算趋势百分比
        double assessmentTrend = lastMonthAssessments > 0 
            ? ((totalAssessments - lastMonthAssessments) * 100.0 / lastMonthAssessments)
            : 0.0;
        double highRiskTrend = lastMonthHighRiskResidents > 0 
            ? ((highRiskResidents - lastMonthHighRiskResidents) * 100.0 / lastMonthHighRiskResidents)
            : 0.0;
        double interventionTrend = lastMonthInterventions > 0 
            ? ((interventionCount - lastMonthInterventions) * 100.0 / lastMonthInterventions)
            : 0.0;
        double timelyTrend = 0.0; // 及时率趋势暂时设为0
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalAssessments", totalAssessments);
        result.put("highRiskResidents", highRiskResidents);
        result.put("interventionCount", interventionCount);
        result.put("timelyRate", Math.round(timelyRate * 100.0) / 100.0);
        result.put("assessmentTrend", Math.round(assessmentTrend * 100.0) / 100.0);
        result.put("highRiskTrend", Math.round(highRiskTrend * 100.0) / 100.0);
        result.put("interventionTrend", Math.round(interventionTrend * 100.0) / 100.0);
        result.put("timelyTrend", Math.round(timelyTrend * 100.0) / 100.0);
        
        return Result.success(result);
    }

    // 获取风险分布统计
    @GetMapping("/risk-distribution")
    public Result<?> getRiskDistribution(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String careZone) {
        
        LocalDateTime start = startDate != null ? 
            LocalDate.parse(startDate).atStartOfDay() : 
            LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = endDate != null ? 
            LocalDate.parse(endDate).atTime(23, 59, 59) : 
            LocalDateTime.now();
        
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(RiskRecord::getEvaluateTime, start, end)
               .in(RiskRecord::getScaleType, ELDERLY_RISK_TYPES);
        
        List<RiskRecord> records = riskRecordMapper.selectList(wrapper);
        
        // 按风险等级分组统计
        Map<String, Long> riskDistribution = records.stream()
            .collect(Collectors.groupingBy(RiskRecord::getRiskLevel, Collectors.counting()));
        
        Map<String, Object> result = new HashMap<>();
        result.put("HIGH", riskDistribution.getOrDefault("HIGH", 0L));
        result.put("MEDIUM", riskDistribution.getOrDefault("MEDIUM", 0L));
        result.put("LOW", riskDistribution.getOrDefault("LOW", 0L));
        
        return Result.success(result);
    }

    // 获取护理区风险排行
    @GetMapping("/care-zone-ranking")
    public Result<?> getCareZoneRanking(@RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate,
                                         @RequestParam(defaultValue = "10") Integer limit) {
        
        LocalDateTime start = startDate != null ? 
            LocalDate.parse(startDate).atStartOfDay() : 
            LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = endDate != null ? 
            LocalDate.parse(endDate).atTime(23, 59, 59) : 
            LocalDateTime.now();
        
        // 获取所有老人
        List<Resident> residents = residentMapper.selectList(new LambdaQueryWrapper<Resident>()
            .eq(Resident::getStatus, "RESIDENT"));
        
        // 获取时间范围内的高危评估记录
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(RiskRecord::getEvaluateTime, start, end)
               .eq(RiskRecord::getRiskLevel, "HIGH");
        List<RiskRecord> highRiskRecords = riskRecordMapper.selectList(wrapper);
        
        // 按护理区统计高危数量
        Map<String, Long> zoneHighRiskCount = new HashMap<>();
        Map<String, Long> careZoneResidentCount = new HashMap<>();
        
        // 统计各护理区老人数量
        for (Resident resident : residents) {
            String zone = resident.getCareZone();
            if (zone != null) {
                careZoneResidentCount.merge(zone, 1L, Long::sum);
            }
        }
        
        // 统计各护理区高危数量
        for (RiskRecord record : highRiskRecords) {
            Resident resident = residentMapper.selectById(record.getResidentId());
            if (resident != null && resident.getCareZone() != null) {
                String zone = resident.getCareZone();
                zoneHighRiskCount.merge(zone, 1L, Long::sum);
            }
        }
        
        // 构建排行榜
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (String zone : careZoneResidentCount.keySet()) {
            long residentCount = careZoneResidentCount.get(zone);
            long highRiskCount = zoneHighRiskCount.getOrDefault(zone, 0L);
            double highRiskRate = residentCount > 0 ? (highRiskCount * 100.0 / residentCount) : 0.0;
            
            Map<String, Object> item = new HashMap<>();
            item.put("careZone", zone);
            item.put("residentCount", residentCount);
            item.put("highRiskCount", highRiskCount);
            item.put("highRiskRate", Math.round(highRiskRate * 100.0) / 100.0);
            ranking.add(item);
        }
        
        // 按高危数量排序
        ranking.sort((a, b) -> Long.compare((Long)b.get("highRiskCount"), (Long)a.get("highRiskCount")));
        
        // 限制返回数量
        if (ranking.size() > limit) {
            ranking = ranking.subList(0, limit);
        }
        
        return Result.success(ranking);
    }

    // 获取风险趋势数据
    @GetMapping("/risk-trend")
    public Result<?> getRiskTrend(@RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(required = false) String scaleType,
                                 @RequestParam(defaultValue = "day") String granularity) {
        
        LocalDateTime start = startDate != null ? 
            LocalDate.parse(startDate).atStartOfDay() : 
            LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? 
            LocalDate.parse(endDate).atTime(23, 59, 59) : 
            LocalDateTime.now();
        
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(RiskRecord::getEvaluateTime, start, end);
        if (scaleType != null && !scaleType.trim().isEmpty()) {
            wrapper.eq(RiskRecord::getScaleType, scaleType);
        }
        wrapper.orderByAsc(RiskRecord::getEvaluateTime);
        
        List<RiskRecord> records = riskRecordMapper.selectList(wrapper);
        
        // 按日期分组统计
        Map<String, Map<String, Long>> trendData = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (RiskRecord record : records) {
            String dateKey = record.getEvaluateTime().format(formatter);
            trendData.computeIfAbsent(dateKey, k -> new HashMap<>());
            
            Map<String, Long> dayData = trendData.get(dateKey);
            dayData.merge("total", 1L, Long::sum);
            dayData.merge(record.getRiskLevel(), 1L, Long::sum);
        }
        
        // 转换为前端需要的格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> entry : trendData.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", entry.getKey());
            item.put("total", entry.getValue().getOrDefault("total", 0L));
            item.put("highRisk", entry.getValue().getOrDefault("HIGH", 0L));
            item.put("mediumRisk", entry.getValue().getOrDefault("MEDIUM", 0L));
            item.put("lowRisk", entry.getValue().getOrDefault("LOW", 0L));
            result.add(item);
        }
        
        return Result.success(result);
    }

    // 获取评估统计数据 - 支持前端统计报表页面
    @GetMapping("/assessment-stats")
    public Result<?> getAssessmentStats(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate,
                                       @RequestParam(required = false) Long evaluatorId) {
        
        LocalDateTime start = startDate != null ? 
            LocalDate.parse(startDate).atStartOfDay() : 
            LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = endDate != null ? 
            LocalDate.parse(endDate).atTime(23, 59, 59) : 
            LocalDateTime.now();
        
        // 获取评估记录
        LambdaQueryWrapper<RiskRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(RiskRecord::getEvaluateTime, start, end);
        if (evaluatorId != null) {
            wrapper.eq(RiskRecord::getEvaluatorId, evaluatorId);
        }
        
        List<RiskRecord> records = riskRecordMapper.selectList(wrapper);
        
        // 按评估人分组统计
        Map<String, List<RiskRecord>> evaluatorRecords = records.stream()
            .filter(r -> r.getEvaluatorName() != null)
            .collect(Collectors.groupingBy(RiskRecord::getEvaluatorName));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<RiskRecord>> entry : evaluatorRecords.entrySet()) {
            String evaluatorName = entry.getKey();
            List<RiskRecord> evaluatorList = entry.getValue();
            
            long totalAssessments = evaluatorList.size();
            long highRiskCount = evaluatorList.stream()
                .mapToLong(r -> "HIGH".equals(r.getRiskLevel()) ? 1 : 0)
                .sum();
            // 干预次数 = 高危 + 中危评估次数
            long interventionCount = evaluatorList.stream()
                .mapToLong(r -> ("HIGH".equals(r.getRiskLevel()) || "MEDIUM".equals(r.getRiskLevel())) ? 1 : 0)
                .sum();
            
            // 计算及时率：假设每个老人每月至少需要评估一次
            // 获取该评估人负责的老人数量
            long uniqueResidents = evaluatorList.stream()
                .map(RiskRecord::getResidentId)
                .distinct()
                .count();
            
            // 及时率 = (实际评估次数 / 应评估次数) * 100，最高100%
            double timelyRate = uniqueResidents > 0 
                ? Math.min((totalAssessments * 100.0 / uniqueResidents), 100.0)
                : 100.0;
            
            // 确保不超过100%
            timelyRate = Math.min(timelyRate, 100.0);
            
            // 获取护理区信息（从第一个记录的老人信息获取）
            String careZone = "未知护理区";
            if (!evaluatorList.isEmpty()) {
                Resident resident = residentMapper.selectById(evaluatorList.get(0).getResidentId());
                if (resident != null && resident.getCareZone() != null) {
                    careZone = resident.getCareZone();
                }
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("evaluatorName", evaluatorName);
            item.put("careZone", careZone);
            item.put("assessmentCount", totalAssessments);
            item.put("highRiskCount", highRiskCount);
            item.put("interventionCount", interventionCount);
            item.put("timelyRate", Math.round(timelyRate * 100.0) / 100.0 + "%");
            
            result.add(item);
        }
        
        // 按评估次数排序
        result.sort((a, b) -> Long.compare((Long)b.get("assessmentCount"), (Long)a.get("assessmentCount")));
        
        // 模拟分页
        int total = result.size();
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, total);
        
        List<Map<String, Object>> pageResult = startIndex < total ? 
            result.subList(startIndex, endIndex) : new ArrayList<>();
        
        Map<String, Object> response = new HashMap<>();
        response.put("records", pageResult);
        response.put("total", total);
        response.put("current", pageNum);
        response.put("size", pageSize);
        
        return Result.success(response);
    }

    // 导出统计报表
    @GetMapping("/export")
    public Result<?> exportStatisticsReport(@RequestParam(required = false) String startDate,
                                           @RequestParam(required = false) String endDate,
                                           @RequestParam(required = false) String careZone,
                                           @RequestParam(defaultValue = "excel") String format) {
        
        // 这里应该实现真正的导出逻辑
        // 暂时返回成功消息
        Map<String, Object> result = new HashMap<>();
        result.put("message", "统计报表导出成功");
        result.put("fileName", "统计报表_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "." + format);
        
        return Result.success(result);
    }
}


