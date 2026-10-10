package com.graduation.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.medical.common.Result;
import com.graduation.medical.entity.ScaleDef;
import com.graduation.medical.entity.ScaleQuestion;
import com.graduation.medical.mapper.ScaleDefMapper;
import com.graduation.medical.mapper.ScaleQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/scale")
@CrossOrigin
public class ScaleController {

    private static final Set<String> ELDERLY_CARE_SCALES = new HashSet<>(Arrays.asList(
            "FALL", "PRESSURE", "MALNUTRITION", "COGNITIVE",
            "WANDERING", "DEPRESSION", "SELF_HARM", "NEGLECT"));

    @Autowired
    private ScaleDefMapper scaleDefMapper;
    
    @Autowired
    private ScaleQuestionMapper scaleQuestionMapper;

    // 获取启用的量表列表 - 支持前端风险评估页面
    @GetMapping("/enabled")
    public Result<?> getEnabledScales() {
        LambdaQueryWrapper<ScaleDef> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScaleDef::getStatus, 1)
               .in(ScaleDef::getScaleType, ELDERLY_CARE_SCALES)
               .orderByDesc(ScaleDef::getCreateTime);
        
        List<ScaleDef> scales = scaleDefMapper.selectList(wrapper);
        
        // 转换为前端需要的格式
        List<Map<String, Object>> result = scales.stream().map(scale -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", scale.getId());
            item.put("scaleType", scale.getScaleType());
            item.put("scaleName", scale.getScaleName());
            item.put("description", scale.getDescription());
            item.put("version", scale.getVersion());
            return item;
        }).collect(java.util.stream.Collectors.toList());
        
        return Result.success(result);
    }

    // 根据量表类型获取量表详情 - 支持前端风险评估页面
    @GetMapping("/type/{scaleType}")
    public Result<?> getScaleByType(@PathVariable String scaleType) {
        if (!isElderlyCareScale(scaleType)) {
            return Result.error("仅支持养老照护风险量表");
        }
        LambdaQueryWrapper<ScaleDef> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ScaleDef::getScaleType, ELDERLY_CARE_SCALES);
        wrapper.eq(ScaleDef::getScaleType, scaleType)
               .eq(ScaleDef::getStatus, 1)
               .orderByDesc(ScaleDef::getCreateTime)
               .last("LIMIT 1");
        
        ScaleDef scaleDef = scaleDefMapper.selectOne(wrapper);
        if (scaleDef == null) {
            return Result.error("量表不存在或已禁用");
        }
        
        // 获取量表题目
        LambdaQueryWrapper<ScaleQuestion> questionWrapper = new LambdaQueryWrapper<>();
        questionWrapper.eq(ScaleQuestion::getScaleId, scaleDef.getId())
                      .orderByAsc(ScaleQuestion::getQuestionOrder);
        
        List<ScaleQuestion> questions = scaleQuestionMapper.selectList(questionWrapper);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", scaleDef.getId());
        result.put("scaleType", scaleDef.getScaleType());
        result.put("scaleName", scaleDef.getScaleName());
        result.put("description", scaleDef.getDescription());
        result.put("version", scaleDef.getVersion());
        result.put("scoringRulesJson", scaleDef.getScoringRulesJson());
        result.put("riskLevelsJson", scaleDef.getRiskLevelsJson());
        result.put("questions", questions);
        
        return Result.success(result);
    }

    // 获取量表详情 - 支持前端量表设计器
    @GetMapping("/{id}")
    public Result<?> getScaleDetail(@PathVariable Long id) {
        try {
            ScaleDef scaleDef = scaleDefMapper.selectById(id);
            if (scaleDef == null) {
                return Result.error("量表不存在");
            }
            
            // 获取量表题目
            LambdaQueryWrapper<ScaleQuestion> questionWrapper = new LambdaQueryWrapper<>();
            questionWrapper.eq(ScaleQuestion::getScaleId, id)
                          .orderByAsc(ScaleQuestion::getQuestionOrder);
            
            List<ScaleQuestion> questions = scaleQuestionMapper.selectList(questionWrapper);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("scaleDef", scaleDef);
            result.put("questions", questions);
            
            return Result.success(result);
            
        } catch (Exception e) {
            return Result.error("获取量表详情失败: " + e.getMessage());
        }
    }

    // 获取量表列表 - 支持前端量表管理页面
    @GetMapping("/list")
    public Result<?> getScaleList(@RequestParam(defaultValue = "1") Integer current,
                                 @RequestParam(defaultValue = "10") Integer size,
                                 @RequestParam(required = false) String scaleName,
                                 @RequestParam(required = false) String scaleType) {
        Page<ScaleDef> page = new Page<>(current, size);
        LambdaQueryWrapper<ScaleDef> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ScaleDef::getScaleType, ELDERLY_CARE_SCALES);
        
        if (scaleName != null && !scaleName.trim().isEmpty()) {
            wrapper.like(ScaleDef::getScaleName, scaleName);
        }
        if (scaleType != null && !scaleType.trim().isEmpty()) {
            wrapper.eq(ScaleDef::getScaleType, scaleType);
        }
        
        wrapper.orderByDesc(ScaleDef::getCreateTime);
        
        Page<ScaleDef> result = scaleDefMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    // 创建量表 - 支持前端量表设计器
    @PostMapping
    @Transactional
    public Result<?> createScale(@RequestBody CreateScaleRequest request) {
        try {
            // 1. 保存量表定义
            ScaleDef scaleDef = request.getScaleDef();
            if (scaleDef == null || !isElderlyCareScale(scaleDef.getScaleType())) {
                return Result.error("不能创建已停用的急性医疗风险量表");
            }
            scaleDef.setCreateTime(LocalDateTime.now());
            scaleDef.setUpdateTime(LocalDateTime.now());
            scaleDefMapper.insert(scaleDef);
            
            // 2. 保存量表题目
            List<ScaleQuestion> questions = request.getQuestions();
            if (questions != null && !questions.isEmpty()) {
                for (ScaleQuestion question : questions) {
                    question.setScaleId(scaleDef.getId());
                    scaleQuestionMapper.insert(question);
                }
            }
            
            // 3. 返回创建的量表ID
            Map<String, Object> result = new HashMap<>();
            result.put("id", scaleDef.getId());
            result.put("message", "量表创建成功");
            
            return Result.success(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("量表创建失败: " + e.getMessage());
        }
    }

    // 更新量表 - 支持前端量表设计器
    @PutMapping
    @Transactional
    public Result<?> updateScale(@RequestBody UpdateScaleRequest request) {
        try {
            // 1. 更新量表定义
            ScaleDef scaleDef = request.getScaleDef();
            if (scaleDef == null || !isElderlyCareScale(scaleDef.getScaleType())) {
                return Result.error("不能启用已停用的急性医疗风险量表");
            }
            
            // 确保ID存在
            if (scaleDef.getId() == null) {
                return Result.error("量表ID不能为空");
            }
            
            scaleDef.setUpdateTime(LocalDateTime.now());
            scaleDefMapper.updateById(scaleDef);
            
            List<ScaleQuestion> questions = request.getQuestions();
            // questions 为 null 表示仅编辑基础信息；设计器提交数组时才整体替换题目。
            if (questions != null) {
                LambdaQueryWrapper<ScaleQuestion> deleteWrapper = new LambdaQueryWrapper<>();
                deleteWrapper.eq(ScaleQuestion::getScaleId, scaleDef.getId());
                scaleQuestionMapper.delete(deleteWrapper);
                for (ScaleQuestion question : questions) {
                    question.setScaleId(scaleDef.getId());
                    question.setId(null); // 确保是新插入
                    scaleQuestionMapper.insert(question);
                }
            }
            
            return Result.success("量表更新成功");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("量表更新失败: " + e.getMessage());
        }
    }

    // 删除量表
    @DeleteMapping("/{id}")
    @Transactional
    public Result<?> deleteScale(@PathVariable Long id) {
        try {
            // 1. 删除量表题目
            LambdaQueryWrapper<ScaleQuestion> questionWrapper = new LambdaQueryWrapper<>();
            questionWrapper.eq(ScaleQuestion::getScaleId, id);
            scaleQuestionMapper.delete(questionWrapper);
            
            // 2. 删除量表定义
            scaleDefMapper.deleteById(id);
            
            return Result.success("量表删除成功");
            
        } catch (Exception e) {
            return Result.error("量表删除失败: " + e.getMessage());
        }
    }

    // 启用/禁用量表
    @PutMapping("/{id}/status")
    public Result<?> toggleScaleStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Boolean enabled = (Boolean) request.get("enabled");
            
            ScaleDef scaleDef = new ScaleDef();
            scaleDef.setId(id);
            scaleDef.setStatus(enabled ? 1 : 0);
            scaleDef.setUpdateTime(LocalDateTime.now());
            
            scaleDefMapper.updateById(scaleDef);
            
            return Result.success(enabled ? "量表已启用" : "量表已禁用");
            
        } catch (Exception e) {
            return Result.error("状态更新失败: " + e.getMessage());
        }
    }

    // 预览量表
    @GetMapping("/{id}/preview")
    public Result<?> previewScale(@PathVariable Long id) {
        ScaleDef scaleDef = scaleDefMapper.selectById(id);
        if (scaleDef == null) {
            return Result.error("量表不存在");
        }
        
        // 获取量表题目
        LambdaQueryWrapper<ScaleQuestion> questionWrapper = new LambdaQueryWrapper<>();
        questionWrapper.eq(ScaleQuestion::getScaleId, id)
                      .orderByAsc(ScaleQuestion::getQuestionOrder);
        
        List<ScaleQuestion> questions = scaleQuestionMapper.selectList(questionWrapper);
        
        // 构建预览数据
        Map<String, Object> result = new HashMap<>();
        result.put("scaleDef", scaleDef);
        result.put("questions", questions);
        
        return Result.success(result);
    }

    // 请求参数类
    public static class CreateScaleRequest {
        private ScaleDef scaleDef;
        private List<ScaleQuestion> questions;
        
        public ScaleDef getScaleDef() { return scaleDef; }
        public void setScaleDef(ScaleDef scaleDef) { this.scaleDef = scaleDef; }
        public List<ScaleQuestion> getQuestions() { return questions; }
        public void setQuestions(List<ScaleQuestion> questions) { this.questions = questions; }
    }

    private boolean isElderlyCareScale(String scaleType) {
        return scaleType != null && ELDERLY_CARE_SCALES.contains(scaleType.toUpperCase());
    }

    public static class UpdateScaleRequest {
        private ScaleDef scaleDef;
        private List<ScaleQuestion> questions;
        
        public ScaleDef getScaleDef() { return scaleDef; }
        public void setScaleDef(ScaleDef scaleDef) { this.scaleDef = scaleDef; }
        public List<ScaleQuestion> getQuestions() { return questions; }
        public void setQuestions(List<ScaleQuestion> questions) { this.questions = questions; }
    }
}
