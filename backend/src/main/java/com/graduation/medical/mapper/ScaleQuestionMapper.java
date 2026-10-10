package com.graduation.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.medical.entity.ScaleQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 量表题目Mapper接口
 */
@Mapper
public interface ScaleQuestionMapper extends BaseMapper<ScaleQuestion> {
    
    /**
     * 根据量表ID查询所有题目
     */
    @Select("SELECT * FROM biz_scale_question WHERE scale_id = #{scaleId} ORDER BY question_order")
    List<ScaleQuestion> selectByScaleId(Long scaleId);
}
