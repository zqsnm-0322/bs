package com.graduation.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graduation.medical.entity.RiskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RiskRecordMapper extends BaseMapper<RiskRecord> {
    // 简单的关联查询，实际项目中建议用XML或Service层组装
    @Select("SELECT r.*, p.name AS resident_name FROM biz_risk_record r LEFT JOIN biz_resident p ON r.resident_id = p.id ORDER BY r.evaluate_time DESC")
    List<RiskRecord> selectListWithResident();
}
