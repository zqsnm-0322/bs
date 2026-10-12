package com.graduation.elderlycare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.elderlycare.entity.Medication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用药记录Mapper接口
 */
@Mapper
public interface MedicationMapper extends BaseMapper<Medication> {
}

