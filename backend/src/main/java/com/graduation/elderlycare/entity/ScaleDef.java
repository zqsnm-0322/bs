package com.graduation.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 量表定义实体类
 * 支持动态配置量表题目、分值和计算规则
 */
@Data
@TableName("biz_scale_def")
public class ScaleDef {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 量表类型：包含跌倒/压疮等基础量表，以及认知、营养、谵妄、虐待、自伤和抑郁等养老场景量表
     */
    private String scaleType;
    
    /**
     * 量表名称
     */
    private String scaleName;
    
    /**
     * 量表描述
     */
    private String description;
    
    /**
     * 题目配置JSON
     * 格式: [{"id":1,"question":"年龄","options":[{"value":0,"label":"<65岁"},{"value":1,"label":"65-85岁"},{"value":2,"label":">85岁"}],"type":"radio"}]
     */
    private String questionsJson;
    
    /**
     * 评分规则JSON
     * 格式: {"rules":[{"condition":"age>=2 && mobility>=1","score":5}],"levels":[{"min":0,"max":24,"level":"LOW"},{"min":25,"max":44,"level":"MEDIUM"},{"min":45,"max":100,"level":"HIGH"}]}
     */
    private String scoringRulesJson;
    
    /**
     * 风险等级配置JSON
     * 格式: [{"level":"LOW","label":"低危","color":"#52c41a","interventions":["定期观察"]},{"level":"MEDIUM","label":"中危","color":"#faad14","interventions":["加强巡视","防护措施"]},{"level":"HIGH","label":"高危","color":"#f5222d","interventions":["专人看护","立即干预"]}]
     */
    private String riskLevelsJson;
    
    /**
     * 是否启用：1-启用，0-禁用
     */
    private Integer status;
    
    /**
     * 版本号，用于量表更新管理
     */
    private String version;

    /** 在养老标准量表模块内部所占权重（百分比） */
    private BigDecimal assessmentWeight;
    
    /**
     * 创建人ID
     */
    private Long creatorId;
    
    /**
     * 创建人姓名
     */
    private String creatorName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

