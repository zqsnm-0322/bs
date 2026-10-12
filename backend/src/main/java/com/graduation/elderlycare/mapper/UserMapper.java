package com.graduation.elderlycare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.elderlycare.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

