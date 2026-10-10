package com.graduation.medical.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.medical.entity.Resident;
import com.graduation.medical.entity.RiskAssessment;
import com.graduation.medical.mapper.ResidentMapper;
import com.graduation.medical.mapper.RiskAssessmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnhancedRiskPredictionService {
    @Autowired private ResidentMapper residentMapper;
    @Autowired private RiskAssessmentMapper riskAssessmentMapper;
    @Autowired private ObjectMapper objectMapper;

    /** 仅评估养老照护风险，不生成急性医疗疾病预测。 */
    public RiskAssessment performComprehensiveRiskAssessment(Long residentId) {
        Resident resident = residentMapper.selectById(residentId);
        if (resident == null) throw new IllegalArgumentException("老人不存在");
        BigDecimal base = baseScore(resident);
        Map<String, BigDecimal> scores = buildScores(resident, base.intValue());
        BigDecimal total = scores.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

        RiskAssessment result = new RiskAssessment();
        result.setResidentId(residentId);
        result.setAssessmentTime(new Date());
        result.setAssessmentType("ELDERLY_CARE");
        result.setHealthHistoryScore(base);
        result.setHealthCheckScore(BigDecimal.ZERO);
        result.setVitalSignsScore(BigDecimal.ZERO);
        result.setQuestionnaireScore(BigDecimal.ZERO);
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
        result.setConfidenceLevel(new BigDecimal("0.85"));
        result.setRiskFactors(json(factors(resident, scores)));
        result.setProtectiveFactors(json(List.of("持续护理观察", "家属与机构协同")));
        result.setRecommendations(json(recommendations(scores)));
        result.setHealthPredictions(json(predictions(scores)));
        result.setAssessedBy("养老照护风险评估系统");
        result.setSentToResident(0);
        result.setCreateTime(new Date());
        riskAssessmentMapper.insert(result);
        return result;
    }

    private BigDecimal baseScore(Resident resident) {
        int score = resident.getAge() == null ? 10 : Math.max(5, Math.min(35, resident.getAge() - 55));
        String level = resident.getHealthLevel() == null ? "" : resident.getHealthLevel();
        if (level.contains("全护") || level.contains("失智")) score += 25;
        else if (level.contains("半护")) score += 15;
        if (resident.getChronicDiseases() != null && !resident.getChronicDiseases().isBlank()) score += 10;
        return score(score);
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
        scores.forEach((type, score) -> result.add(Map.of("riskType", type, "riskName", label(type),
            "score", score, "riskLevel", level(score), "recommendation", advice(type))));
        return result;
    }

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
