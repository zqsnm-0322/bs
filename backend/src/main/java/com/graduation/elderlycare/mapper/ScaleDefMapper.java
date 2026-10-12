package com.graduation.elderlycare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.elderlycare.entity.ScaleDef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 量表定义Mapper接口
 */
@Mapper
public interface ScaleDefMapper extends BaseMapper<ScaleDef> {
    
    /**
     * 根据量表类型查询启用的量表定义
     */
    @Select("SELECT * FROM biz_scale_def WHERE scale_type = #{scaleType} AND status = 1 ORDER BY version DESC LIMIT 1")
    ScaleDef selectByScaleType(String scaleType);
    
    /**
     * 查询所有启用的量表定义
     */
    @Select("SELECT * FROM biz_scale_def WHERE status = 1 ORDER BY scale_type, version DESC")
    List<ScaleDef> selectAllEnabled();
}

