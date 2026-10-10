package com.graduation.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.medical.entity.HealthCheckup;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthCheckupMapper extends BaseMapper<HealthCheckup> {
}