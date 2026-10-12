package com.graduation.elderlycare.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.graduation.elderlycare.entity.ScaleDef;
import com.graduation.elderlycare.entity.ScaleQuestion;
import com.graduation.elderlycare.mapper.ScaleDefMapper;
import com.graduation.elderlycare.mapper.ScaleQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * 量表评估服务类
 * 负责动态评分计算和风险等级判定
 */
@Service
public class ScaleEvaluationService {
    
    @Autowired
    private ScaleDefMapper scaleDefMapper;
    
    @Autowired
    private ScaleQuestionMapper scaleQuestionMapper;
    
    /**
     * 根据答题结果计算评分和风险等级
     * @param scaleType 量表类型
     * @param answers 答题结果 Map<questionId, selectedValue>
     * @return 评估结果
     */
    public EvaluationResult evaluate(String scaleType, Map<String, Object> answers) {
        // 1. 获取量表定义
        ScaleDef scaleDef = scaleDefMapper.selectByScaleType(scaleType);
        if (scaleDef == null) {
            throw new RuntimeException("未找到量表定义: " + scaleType);
        }
        
        // 2. 获取量表题目
        List<ScaleQuestion> questions = scaleQuestionMapper.selectByScaleId(scaleDef.getId());
        
        // 3. 计算总分
        int totalScore = calculateTotalScore(questions, answers);
        
        // 4. 判定风险等级
        String riskLevel = determineRiskLevel(scaleDef, totalScore);
        
        // 5. 获取干预建议
        List<String> interventions = getInterventions(scaleDef, riskLevel);
        
        return new EvaluationResult(totalScore, riskLevel, interventions);
    }
    
    /**
     * 计算总分
     */
    private int calculateTotalScore(List<ScaleQuestion> questions, Map<String, Object> answers) {
        int totalScore = 0;
        
        for (ScaleQuestion question : questions) {
            String questionId = question.getId().toString();
            Object answer = answers.get(questionId);
            
            if (answer != null) {
                // 解析选项配置
                JSONArray options = JSON.parseArray(question.getOptionsJson());
                for (int i = 0; i < options.size(); i++) {
                    JSONObject option = options.getJSONObject(i);
                    if (option.get("value").equals(answer)) {
                        int score = option.getIntValue("score");
                        // 考虑题目权重
                        if (question.getWeight() != null) {
                            score = (int) (score * question.getWeight());
                        }
                        totalScore += score;
                        break;
                    }
                }
            }
        }
        
        return totalScore;
    }
    
    /**
     * 判定风险等级
     */
    private String determineRiskLevel(ScaleDef scaleDef, int totalScore) {
        JSONObject scoringRules = JSON.parseObject(scaleDef.getScoringRulesJson());
        JSONArray levels = scoringRules.getJSONArray("levels");
        
        for (int i = 0; i < levels.size(); i++) {
            JSONObject level = levels.getJSONObject(i);
            int min = level.getIntValue("min");
            int max = level.getIntValue("max");
            
            if (totalScore >= min && totalScore <= max) {
                return level.getString("level");
            }
        }
        
        return "UNKNOWN";
    }
    
    /**
     * 获取干预建议
     */
    private List<String> getInterventions(ScaleDef scaleDef, String riskLevel) {
        JSONArray riskLevels = JSON.parseArray(scaleDef.getRiskLevelsJson());
        
        for (int i = 0; i < riskLevels.size(); i++) {
            JSONObject level = riskLevels.getJSONObject(i);
            if (level.getString("level").equals(riskLevel)) {
                return level.getJSONArray("interventions").toJavaList(String.class);
            }
        }
        
        return List.of();
    }
    
    /**
     * 评估结果内部类
     */
    public static class EvaluationResult {
        private int totalScore;
        private String riskLevel;
        private List<String> interventions;
        
        public EvaluationResult(int totalScore, String riskLevel, List<String> interventions) {
            this.totalScore = totalScore;
            this.riskLevel = riskLevel;
            this.interventions = interventions;
        }
        
        // Getters
        public int getTotalScore() { return totalScore; }
        public String getRiskLevel() { return riskLevel; }
        public List<String> getInterventions() { return interventions; }
    }
}

