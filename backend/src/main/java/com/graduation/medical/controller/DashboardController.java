package com.graduation.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.graduation.medical.common.Result;
import com.graduation.medical.entity.Resident;
import com.graduation.medical.entity.RiskRecord;
import com.graduation.medical.mapper.ResidentMapper;
import com.graduation.medical.mapper.RiskRecordMapper;
import com.graduation.medical.mapper.BedMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
public class DashboardController {

    private static final List<String> ELDERLY_RISK_TYPES = List.of(
        "FALL", "PRESSURE", "COGNITIVE", "MALNUTRITION",
        "DELIRIUM", "ABUSE", "SELF_HARM", "DEPRESSION"
    );

    @Autowired
    private ResidentMapper residentMapper;
    @Autowired
    private RiskRecordMapper riskRecordMapper;
    @Autowired
    private BedMapper bedMapper;

    @GetMapping("/stats")
    public Result<?> getStats() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("residentCount", residentMapper.selectCount(
            new QueryWrapper<Resident>().eq("status", "RESIDENT")));
        
        map.put("highRiskCount", riskRecordMapper.selectCount(
            new QueryWrapper<com.graduation.medical.entity.RiskRecord>()
                .eq("risk_level", "HIGH").in("scale_type", ELDERLY_RISK_TYPES)));
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        map.put("newAdmissions", residentMapper.selectCount(
            new QueryWrapper<Resident>()
                .eq("status", "RESIDENT")
                .ge("admit_time", cal.getTime())));
        
        List<Map<String, Object>> zoneDist = new ArrayList<>();
        addZoneCount(zoneDist, "自理区");
        addZoneCount(zoneDist, "半护区");
        addZoneCount(zoneDist, "全护区");
        addZoneCount(zoneDist, "失智区");
        map.put("zoneDistribution", zoneDist);
        
        List<Map<String, Object>> healthDist = new ArrayList<>();
        addHealthLevelCount(healthDist, "自理");
        addHealthLevelCount(healthDist, "半护");
        addHealthLevelCount(healthDist, "全护");
        addHealthLevelCount(healthDist, "失智");
        map.put("healthLevelDistribution", healthDist);
        
        cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        map.put("leavingCount", residentMapper.selectCount(
            new QueryWrapper<Resident>()
                .eq("status", "LEAVE")
                .ge("leave_time", cal.getTime())));
        
        Long totalBeds = bedMapper.selectCount(null);
        Long occupiedBeds = bedMapper.selectCount(
            new QueryWrapper<com.graduation.medical.entity.Bed>().eq("status", "OCCUPIED"));
        map.put("bedOccupancyRate", totalBeds > 0 ? Math.round((double)occupiedBeds / totalBeds * 100) : 0);
        
        return Result.success(map);
    }

    private void addZoneCount(List<Map<String, Object>> list, String zone) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", zone);
        item.put("value", residentMapper.selectCount(
            new QueryWrapper<Resident>()
                .eq("care_zone", zone).eq("status", "RESIDENT")));
        list.add(item);
    }

    private void addHealthLevelCount(List<Map<String, Object>> list, String level) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", level);
        item.put("value", residentMapper.selectCount(
            new QueryWrapper<Resident>()
                .eq("health_level", level).eq("status", "RESIDENT")));
        list.add(item);
    }

    @GetMapping("/scan")
    public Result<?> scanRisk() {
        List<String> report = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusDays(3);
        List<RiskRecord> overdueRecords = riskRecordMapper.selectList(
            new QueryWrapper<RiskRecord>()
                .in("risk_level", "HIGH", "CRITICAL")
                .le("evaluate_time", deadline)
                .orderByDesc("evaluate_time"));

        Set<Long> overdueResidentIds = new LinkedHashSet<>();
        for (RiskRecord record : overdueRecords) {
            Resident resident = residentMapper.selectById(record.getResidentId());
            if (resident != null && "RESIDENT".equals(resident.getStatus())) {
                overdueResidentIds.add(resident.getId());
            }
        }

        report.add("扫描时间：" + now);
        report.add("发现 " + overdueResidentIds.size() + " 位高风险老人超过 3 天未复评。");
        report.add(overdueResidentIds.isEmpty()
            ? "当前没有需要跟进的超期复评。"
            : "请照护主管安排护理人员尽快复评并记录干预措施。");
        return Result.success(report);
    }
}
