package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.Resident;
import com.graduation.elderlycare.entity.RiskRecord;
import com.graduation.elderlycare.mapper.ResidentMapper;
import com.graduation.elderlycare.mapper.RiskRecordMapper;
import com.graduation.elderlycare.mapper.BedMapper;
import com.graduation.elderlycare.util.AuthUtil;
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

        Long caregiverId = "CAREGIVER".equals(AuthUtil.getCurrentRole())
            ? AuthUtil.getCurrentUserId()
            : null;
        QueryWrapper<Resident> residentQuery = new QueryWrapper<Resident>()
            .eq("status", "RESIDENT");
        if (caregiverId != null) residentQuery.eq("caregiver_id", caregiverId);
        List<Resident> visibleResidents = residentMapper.selectList(residentQuery);
        List<Long> visibleResidentIds = new ArrayList<>();
        for (Resident resident : visibleResidents) visibleResidentIds.add(resident.getId());

        map.put("residentCount", (long) visibleResidents.size());

        QueryWrapper<RiskRecord> highRiskQuery = new QueryWrapper<RiskRecord>()
            .eq("risk_level", "HIGH")
            .in("scale_type", ELDERLY_RISK_TYPES);
        if (caregiverId != null) {
            if (visibleResidentIds.isEmpty()) highRiskQuery.apply("1 = 0");
            else highRiskQuery.in("resident_id", visibleResidentIds);
        }
        map.put("highRiskCount", riskRecordMapper.selectCount(highRiskQuery));
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        QueryWrapper<Resident> admissionQuery = new QueryWrapper<Resident>()
            .eq("status", "RESIDENT")
            .ge("admit_time", cal.getTime());
        if (caregiverId != null) admissionQuery.eq("caregiver_id", caregiverId);
        map.put("newAdmissions", residentMapper.selectCount(admissionQuery));
        
        List<Map<String, Object>> zoneDist = new ArrayList<>();
        addZoneCount(zoneDist, "自理区", caregiverId);
        addZoneCount(zoneDist, "半护区", caregiverId);
        addZoneCount(zoneDist, "全护区", caregiverId);
        addZoneCount(zoneDist, "失智区", caregiverId);
        map.put("zoneDistribution", zoneDist);
        
        List<Map<String, Object>> healthDist = new ArrayList<>();
        addHealthLevelCount(healthDist, "自理", caregiverId);
        addHealthLevelCount(healthDist, "半护", caregiverId);
        addHealthLevelCount(healthDist, "全护", caregiverId);
        addHealthLevelCount(healthDist, "失智", caregiverId);
        map.put("healthLevelDistribution", healthDist);
        
        cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        QueryWrapper<Resident> leavingQuery = new QueryWrapper<Resident>()
            .eq("status", "LEAVE")
            .ge("leave_time", cal.getTime());
        if (caregiverId != null) leavingQuery.eq("caregiver_id", caregiverId);
        map.put("leavingCount", residentMapper.selectCount(leavingQuery));
        
        Long totalBeds = bedMapper.selectCount(null);
        Long occupiedBeds = bedMapper.selectCount(
            new QueryWrapper<com.graduation.elderlycare.entity.Bed>().eq("status", "OCCUPIED"));
        map.put("bedOccupancyRate", totalBeds > 0 ? Math.round((double)occupiedBeds / totalBeds * 100) : 0);
        
        return Result.success(map);
    }

    private void addZoneCount(List<Map<String, Object>> list, String zone, Long caregiverId) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", zone);
        QueryWrapper<Resident> query = new QueryWrapper<Resident>()
            .eq("care_zone", zone).eq("status", "RESIDENT");
        if (caregiverId != null) query.eq("caregiver_id", caregiverId);
        item.put("value", residentMapper.selectCount(query));
        list.add(item);
    }

    private void addHealthLevelCount(List<Map<String, Object>> list, String level, Long caregiverId) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", level);
        QueryWrapper<Resident> query = new QueryWrapper<Resident>()
            .eq("health_level", level).eq("status", "RESIDENT");
        if (caregiverId != null) query.eq("caregiver_id", caregiverId);
        item.put("value", residentMapper.selectCount(query));
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

