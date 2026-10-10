package com.graduation.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.medical.entity.Resident;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResidentMapper extends BaseMapper<Resident> {
}