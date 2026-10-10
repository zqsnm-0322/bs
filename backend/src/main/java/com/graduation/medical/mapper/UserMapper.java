package com.graduation.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.medical.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
