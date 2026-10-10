package com.graduation.medical.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.medical.entity.*;
import com.graduation.medical.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnhancedRiskPredictionService {
    @Autowired private ResidentMapper residentMapper;
    @Autowired private RiskAssessmentMapper riskAssessmentMapper;
    @Autowired private RiskRecordMapper riskRecordMapper;
    @Autowired private ScaleDefMapper scaleDefMapper;
    @Autowired private VitalSignsMapper vitalSignsMapper;
    @Autowired private HealthCheckupMapper healthCheckupMapper;
    @Autowired private ResidentHealthHistoryMapper healthHistoryMapper;
    @Autowired private SymptomReportMapper symptomReportMapper;
    @Autowired private CarePlanMapper carePlanMapper;
    @Autowired private CareTaskMapper careTaskMapper;
    @Autowired private ObjectMapper objectMapper;

    private static final BigDecimal W_SCALE = new BigDecimal("0.35");
    private static final BigDecimal W_HEALTH = new BigDecimal("0.20");
    private static final BigDecimal W_HISTORY = new BigDecimal("0.15");
    private static final BigDecimal W_EVENT = new BigDecimal("0.15");
    private static final BigDecimal W_CARE = new BigDecimal("0.10");
    private static final BigDecimal W_BASE = new BigDecimal("0.05");

    /** 仅评估养老照护风险，不生成急性医疗疾病预测。 */
    public RiskAssessment performComprehensiveRiskAssessment(Long residentId) {
        Resident resident = residentMapper.selectById(residentId);
        if (resident == null) throw new IllegalArgumentException("老人不存在");
        ModuleScore scale = latestScaleScore(residentId);
        ModuleScore health = healthObservationScore(residentId);
        ModuleScore history = healthHistoryScore(resident);
        ModuleScore events = abnormalEventScore(residentId);
        ModuleScore care = dailyCareScore(residentId);
        ModuleScore base = admissionBaseScore(resident);

        LinkedHashMap<String, ModuleScore> modules = new LinkedHashMap<>();
        modules.put("最新标准量表评估", scale.withWeight(W_SCALE));
        modules.put("生命体征与健康检查", health.withWeight(W_HEALTH));
        modules.put("健康历史、慢性健康问题", history.withWeight(W_HISTORY));
        modules.put("入住期间异常事件历史", events.withWeight(W_EVENT));
        modules.put("日常护理任务及生活能力", care.withWeight(W_CARE));
        modules.put("入住基础信息", base.withWeight(W_BASE));

        BigDecimal total = modules.values().stream().map(ModuleScore::weightedScore)
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> scores = buildScores(resident, total.intValue());

        RiskAssessment result = new RiskAssessment();
        result.setResidentId(residentId);
        result.setAssessmentTime(new Date());
        result.setAssessmentType("ELDERLY_CARE");
        result.setHealthHistoryScore(history.score());
        result.setHealthCheckScore(health.score());
        result.setVitalSignsScore(health.score());
        result.setQuestionnaireScore(scale.score());
        result.setFallRisk(scores.get("FALL"));
        result.setPressureSoreRisk(scores.get("PRESSURE"));
        result.setNutritionRisk(scores.get("MALNUTRITION"));
        result.setCognitiveRisk(scores.get("COGNITIVE"));
        result.setWanderingRisk(scores.get("WANDERING"));
        result.setDepressionRisk(scores.get("DEPRESSION"));
        result.setSelfHarmRisk(scores.get("SELF_HARM"));
        result.setNeglectRisk(scores.get("NEGLECT"));
        result.setTotalScore(total);
        result.setRiskLevel(level(total));
        long evidenceModules = modules.values().stream().filter(ModuleScore::hasData).count();
        result.setConfidenceLevel(BigDecimal.valueOf(0.40 + evidenceModules * 0.10)
            .min(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP));
        result.setRiskFactors(json(factors(resident, scores)));
        result.setProtectiveFactors(json(List.of("持续护理观察", "家属与机构协同")));
        result.setRecommendations(json(recommendations(scores)));
        result.setHealthPredictions(json(predictions(scores)));
        result.setAssessmentBreakdown(json(breakdown(modules, total)));
        result.setAssessedBy("养老照护风险评估系统");
        result.setSentToResident(0);
        result.setCreateTime(new Date());
        riskAssessmentMapper.insert(result);
        return result;
    }

    private ModuleScore admissionBaseScore(Resident resident) {
        int value = 0;
        if (resident.getAge() != null) value += resident.getAge() >= 85 ? 45 : resident.getAge() >= 75 ? 30 : resident.getAge() >= 65 ? 15 : 5;
        String level = resident.getHealthLevel() == null ? "" : resident.getHealthLevel();
        if (level.contains("失智") || level.contains("全护")) value += 40;
        else if (level.contains("半护")) value += 25;
        else value += 10;
        if (resident.getAdmitTime() != null && daysSince(resident.getAdmitTime()) <= 30) value += 15;
        return new ModuleScore(score(value), true, "年龄、照护等级及入住适应期");
    }

    private ModuleScore latestScaleScore(Long residentId) {
        List<ScaleDef> enabledScales = scaleDefMapper.selectAllEnabled().stream()
            .filter(item -> item.getAssessmentWeight() != null && item.getAssessmentWeight().compareTo(BigDecimal.ZERO) > 0)
            .toList();
        List<String> enabledScaleTypes = enabledScales.stream().map(ScaleDef::getScaleType).toList();
        if (enabledScaleTypes.isEmpty()) return ModuleScore.empty("暂无已启用的养老风险量表");
        List<RiskRecord> records = riskRecordMapper.selectList(new LambdaQueryWrapper<RiskRecord>()
            .eq(RiskRecord::getResidentId, residentId).in(RiskRecord::getScaleType, enabledScaleTypes)
            .orderByDesc(RiskRecord::getEvaluateTime));
        Map<String, RiskRecord> latestByType = new LinkedHashMap<>();
        records.forEach(record -> latestByType.putIfAbsent(record.getScaleType(), record));
        if (latestByType.isEmpty()) return ModuleScore.empty("暂无标准量表评估");

        BigDecimal configuredWeight = enabledScales.stream()
            .filter(item -> latestByType.containsKey(item.getScaleType()))
            .map(ScaleDef::getAssessmentWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weightedScore = BigDecimal.ZERO;
        List<String> evidence = new ArrayList<>();
        for (ScaleDef scale : enabledScales) {
            RiskRecord record = latestByType.get(scale.getScaleType());
            if (record == null) continue;
            BigDecimal normalized = normalizeScaleRisk(record);
            BigDecimal share = scale.getAssessmentWeight().divide(configuredWeight, 6, RoundingMode.HALF_UP);
            weightedScore = weightedScore.add(normalized.multiply(share));
            evidence.add(scale.getScaleName() + " " + scale.getAssessmentWeight().stripTrailingZeros().toPlainString()
                + "%（" + record.getRiskLevel() + "，原始分" + record.getTotalScore() + "）");
        }
        return new ModuleScore(clamp(weightedScore.setScale(2, RoundingMode.HALF_UP)), true,
            "已汇总" + evidence.size() + "类最新量表：" + String.join("；", evidence));
    }

    private BigDecimal normalizeScaleRisk(RiskRecord record) {
        return switch (record.getRiskLevel() == null ? "" : record.getRiskLevel().toUpperCase()) {
            case "HIGH", "CRITICAL" -> BigDecimal.valueOf(90);
            case "MEDIUM" -> BigDecimal.valueOf(60);
            case "LOW" -> BigDecimal.valueOf(25);
            default -> clamp(BigDecimal.valueOf(record.getTotalScore()));
        };
    }

    private ModuleScore healthObservationScore(Long residentId) {
        VitalSigns vital = vitalSignsMapper.selectOne(new LambdaQueryWrapper<VitalSigns>()
            .eq(VitalSigns::getResidentId, residentId).orderByDesc(VitalSigns::getMeasurementTime).last("LIMIT 1"));
        HealthCheckup check = healthCheckupMapper.selectOne(new LambdaQueryWrapper<HealthCheckup>()
            .eq(HealthCheckup::getResidentId, residentId).orderByDesc(HealthCheckup::getCheckDate).last("LIMIT 1"));
        List<Integer> values = new ArrayList<>();
        if (vital != null) values.add(vitalRisk(vital));
        if (check != null) values.add(checkupRisk(check));
        if (values.isEmpty()) return ModuleScore.empty("暂无生命体征或健康检查");
        int average = values.stream().mapToInt(Integer::intValue).sum() / values.size();
        return new ModuleScore(score(average), true, "最近生命体征和健康检查，共" + values.size() + "类数据");
    }

    private int vitalRisk(VitalSigns v) {
        int risk = 0;
        Integer sbp = v.getSystolicPressure() != null ? v.getSystolicPressure() : value(v.getSystolicBp());
        Integer dbp = v.getDiastolicPressure() != null ? v.getDiastolicPressure() : value(v.getDiastolicBp());
        if (sbp != null && (sbp < 90 || sbp > 140)) risk += 20;
        if (dbp != null && (dbp < 60 || dbp > 90)) risk += 15;
        if (outside(v.getHeartRate(), 50, 100)) risk += 15;
        if (outside(v.getTemperature(), 36, 37.5)) risk += 15;
        BigDecimal oxygen = v.getOxygenSaturation() != null ? v.getOxygenSaturation() : v.getBloodOxygen();
        if (oxygen != null && oxygen.doubleValue() < 95) risk += 20;
        if (outside(v.getBloodGlucose(), 3.9, 11.1)) risk += 15;
        return Math.min(100, risk);
    }

    private int checkupRisk(HealthCheckup c) {
        int risk = 0;
        if (c.getBloodPressureSystolic() != null && (c.getBloodPressureSystolic() < 90 || c.getBloodPressureSystolic() > 140)) risk += 20;
        if (c.getBloodPressureDiastolic() != null && (c.getBloodPressureDiastolic() < 60 || c.getBloodPressureDiastolic() > 90)) risk += 15;
        if (c.getHeartRate() != null && (c.getHeartRate() < 50 || c.getHeartRate() > 100)) risk += 15;
        if (outside(c.getTemperature(), 36, 37.5)) risk += 10;
        if (outside(c.getBloodSugarFasting(), 3.9, 6.1)) risk += 10;
        if (c.getBmi() != null && (c.getBmi().doubleValue() < 18.5 || c.getBmi().doubleValue() > 28)) risk += 15;
        if (c.getBloodOxygen() != null && c.getBloodOxygen().doubleValue() < 95) risk += 15;
        return Math.min(100, risk);
    }

    private ModuleScore healthHistoryScore(Resident resident) {
        List<ResidentHealthHistory> histories = healthHistoryMapper.selectList(new LambdaQueryWrapper<ResidentHealthHistory>()
            .eq(ResidentHealthHistory::getResidentId, resident.getId()).orderByDesc(ResidentHealthHistory::getRecordDate));
        double weighted = 0, weights = 0;
        for (ResidentHealthHistory item : histories) {
            int severity = "SEVERE".equalsIgnoreCase(item.getSeverity()) ? 90 : "MODERATE".equalsIgnoreCase(item.getSeverity()) ? 60 : 30;
            if ("RESOLVED".equalsIgnoreCase(item.getStatus())) severity /= 2;
            double decay = decay(item.getRecordDate());
            weighted += severity * decay; weights += decay;
        }
        if (resident.getChronicDiseases() != null && !resident.getChronicDiseases().isBlank()) { weighted += 60; weights += 1; }
        if (weights == 0) return ModuleScore.empty("暂无健康历史或慢性健康问题");
        return new ModuleScore(score((int) Math.round(weighted / weights)), true, histories.size() + "条健康历史及慢性健康问题");
    }

    private ModuleScore abnormalEventScore(Long residentId) {
        List<SymptomReport> reports = symptomReportMapper.selectList(new LambdaQueryWrapper<SymptomReport>()
            .eq(SymptomReport::getResidentId, residentId).orderByDesc(SymptomReport::getCreateTime));
        double total = 0;
        for (SymptomReport report : reports) {
            int severity = "severe".equalsIgnoreCase(report.getSeverity()) ? 80 : "moderate".equalsIgnoreCase(report.getSeverity()) ? 50 : 25;
            if (!"HANDLED".equalsIgnoreCase(report.getStatus())) severity += 15;
            total += Math.min(100, severity) * decay(report.getCreateTime());
        }
        if (reports.isEmpty()) return ModuleScore.empty("入住期间暂无异常事件记录");
        return new ModuleScore(score((int) Math.min(100, Math.round(total))), true, reports.size() + "条异常事件，已按时间衰减");
    }

    private ModuleScore dailyCareScore(Long residentId) {
        List<CarePlan> plans = carePlanMapper.selectList(new LambdaQueryWrapper<CarePlan>().eq(CarePlan::getResidentId, residentId));
        if (plans.isEmpty()) return ModuleScore.empty("暂无护理计划和任务记录");
        double planRisk = plans.stream().mapToInt(p -> 100 - Math.max(0, Math.min(100, p.getProgress() == null ? 0 : p.getProgress()))).average().orElse(0);
        List<Long> planIds = plans.stream().map(CarePlan::getId).toList();
        List<CareTask> tasks = careTaskMapper.selectList(new LambdaQueryWrapper<CareTask>().in(CareTask::getPlanId, planIds));
        double taskRisk = tasks.isEmpty() ? planRisk : tasks.stream().filter(t -> !Boolean.TRUE.equals(t.getCompleted())).count() * 100.0 / tasks.size();
        return new ModuleScore(score((int) Math.round((planRisk + taskRisk) / 2)), true,
            plans.size() + "个护理计划、" + tasks.size() + "项护理任务");
    }

    private Map<String, BigDecimal> buildScores(Resident resident, int base) {
        boolean cognitive = contains(resident.getHealthLevel(), "失智") || contains(resident.getHealthStatus(), "认知");
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        scores.put("FALL", score(base + 10));
        scores.put("PRESSURE", score(base + (contains(resident.getHealthLevel(), "全护") ? 20 : 0)));
        scores.put("MALNUTRITION", score(base / 2 + 10));
        scores.put("COGNITIVE", score(cognitive ? base + 30 : base / 2));
        scores.put("WANDERING", score(cognitive ? base + 25 : base / 3));
        scores.put("DEPRESSION", score(base / 2));
        scores.put("SELF_HARM", score(base / 3));
        scores.put("NEGLECT", score(base / 2 + (resident.getCareZone() == null ? 20 : 0)));
        return scores;
    }

    private List<String> factors(Resident resident, Map<String, BigDecimal> scores) {
        List<String> result = new ArrayList<>();
        if (resident.getAge() != null && resident.getAge() >= 80) result.add("高龄");
        if (resident.getChronicDiseases() != null && !resident.getChronicDiseases().isBlank()) result.add("慢性健康问题");
        scores.forEach((type, score) -> { if (score.intValue() >= 60) result.add(label(type) + "风险较高"); });
        if (result.isEmpty()) result.add("当前未发现突出风险因素");
        return result;
    }

    private List<String> recommendations(Map<String, BigDecimal> scores) {
        List<String> result = new ArrayList<>();
        scores.forEach((type, score) -> { if (score.intValue() >= 40) result.add(label(type) + "：" + advice(type)); });
        if (result.isEmpty()) result.add("维持日常照护，每月复评一次");
        return result;
    }

    private List<Map<String, Object>> predictions(Map<String, BigDecimal> scores) {
        List<Map<String, Object>> result = new ArrayList<>();
        scores.forEach((type, score) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            String riskLevel = level(score);
            item.put("riskType", type);
            item.put("riskName", label(type));
            item.put("condition", label(type) + "照护风险");
            item.put("score", score);
            item.put("probability", score + "分"); // 兼容既有前端字段，语义为风险评分而非发病概率
            item.put("riskLevel", riskLevel);
            item.put("severity", riskLevel);
            item.put("timeframe", "持续观察");
            item.put("symptoms", observations(type));
            item.put("interventions", List.of(advice(type)));
            item.put("recommendation", advice(type));
            item.put("statisticalBasis", "依据老人年龄、照护等级、健康状况及当前档案综合评估，不作为医疗诊断");
            result.add(item);
        });
        return result;
    }

    private Map<String, Object> breakdown(LinkedHashMap<String, ModuleScore> modules, BigDecimal total) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        modules.forEach((name, module) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("module", name);
            item.put("score", module.score());
            item.put("weight", module.weight().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%");
            item.put("weightedScore", module.weightedScore());
            item.put("hasData", module.hasData());
            item.put("evidence", module.evidence());
            items.add(item);
        });
        result.put("modules", items);
        result.put("totalScore", total);
        result.put("formula", "量表35% + 生命体征与健康检查20% + 健康历史15% + 异常事件15% + 日常护理10% + 入住基础5%");
        result.put("historyDecay", Map.of("0-30天", "100%", "31-90天", "70%", "91-180天", "40%", "180天以上", "20%"));
        return result;
    }

    private Integer value(BigDecimal number) { return number == null ? null : number.intValue(); }
    private boolean outside(BigDecimal number, double min, double max) {
        return number != null && (number.doubleValue() < min || number.doubleValue() > max);
    }
    private BigDecimal clamp(BigDecimal value) { return value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)); }
    private long daysSince(Date date) {
        return Math.max(0, ChronoUnit.DAYS.between(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()));
    }
    private double decay(Date date) { return date == null ? 0.2 : decayDays(daysSince(date)); }
    private double decay(LocalDateTime date) {
        return date == null ? 0.2 : decayDays(Math.max(0, ChronoUnit.DAYS.between(date.toLocalDate(), LocalDate.now())));
    }
    private double decayDays(long days) { return days <= 30 ? 1.0 : days <= 90 ? 0.7 : days <= 180 ? 0.4 : 0.2; }

    private record ModuleScore(BigDecimal score, boolean hasData, String evidence, BigDecimal weight) {
        ModuleScore(BigDecimal score, boolean hasData, String evidence) { this(score, hasData, evidence, BigDecimal.ZERO); }
        static ModuleScore empty(String evidence) { return new ModuleScore(BigDecimal.ZERO, false, evidence); }
        ModuleScore withWeight(BigDecimal weight) { return new ModuleScore(score, hasData, evidence, weight); }
        BigDecimal weightedScore() { return score.multiply(weight).setScale(2, RoundingMode.HALF_UP); }
    }

    private List<String> observations(String type) { return switch (type) {
        case "FALL" -> List.of("步态和平衡变化", "起身或夜间活动安全");
        case "PRESSURE" -> List.of("受压部位皮肤状态", "翻身与活动频率");
        case "MALNUTRITION" -> List.of("进食量和体重变化", "吞咽及食欲情况");
        case "COGNITIVE" -> List.of("记忆与定向能力变化", "日常指令执行情况");
        case "WANDERING" -> List.of("异常外出倾向", "活动路线和定位情况");
        case "DEPRESSION" -> List.of("情绪和睡眠变化", "社交参与情况");
        case "SELF_HARM" -> List.of("异常言语和情绪波动", "独处时的安全状态");
        default -> List.of("照护计划执行情况", "基本生活需求是否得到满足");
    }; }

    private String label(String type) { return switch (type) {
        case "FALL" -> "跌倒"; case "PRESSURE" -> "压疮"; case "MALNUTRITION" -> "营养";
        case "COGNITIVE" -> "认知"; case "WANDERING" -> "走失"; case "DEPRESSION" -> "抑郁";
        case "SELF_HARM" -> "自伤"; default -> "照护忽视"; }; }
    private String advice(String type) { return switch (type) {
        case "FALL" -> "加强陪同与环境防滑"; case "PRESSURE" -> "定时翻身并检查皮肤";
        case "MALNUTRITION" -> "记录饮食并开展营养干预"; case "COGNITIVE" -> "加强认知训练和定向提示";
        case "WANDERING" -> "完善出入管理和定位关怀"; case "DEPRESSION" -> "增加陪伴和心理关怀";
        case "SELF_HARM" -> "加强观察并及时启动心理支持"; default -> "核查照护计划与执行记录"; }; }
    private BigDecimal score(int value) { return BigDecimal.valueOf(Math.max(0, Math.min(100, value))); }
    private boolean contains(String value, String text) { return value != null && value.contains(text); }
    private String level(BigDecimal score) { return score.intValue() >= 70 ? "HIGH" : score.intValue() >= 40 ? "MEDIUM" : "LOW"; }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("风险结果序列化失败", e); } }
}
