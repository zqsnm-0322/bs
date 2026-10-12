package com.graduation.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 量表题目实体类
 * 用于存储量表的具体题目和选项
 */
@Data
@TableName("biz_scale_question")
public class ScaleQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 所属量表ID
     */
    private Long scaleId;
    
    /**
     * 题目序号
     */
    private Integer questionOrder;
    
    /**
     * 题目内容
     */
    private String questionText;
    
    /**
     * 题目类型：radio(单选), checkbox(多选), input(输入), select(下拉)
     */
    private String questionType;
    
    /**
     * 选项配置JSON
     * 格式: [{"value":0,"label":"无","score":0},{"value":1,"label":"轻微","score":1},{"value":2,"label":"严重","score":2}]
     */
    private String optionsJson;
    
    /**
     * 是否必填：1-必填，0-选填
     */
    private Integer required;
    
    /**
     * 题目权重（用于复杂评分算法）
     */
    private Double weight;
    
    /**
     * 题目分组（用于分类显示）
     */
    private String groupName;
}

