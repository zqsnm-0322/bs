package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.VitalSigns;
import com.graduation.elderlycare.entity.Resident;
import com.graduation.elderlycare.mapper.VitalSignsMapper;
import com.graduation.elderlycare.mapper.ResidentMapper;
import com.graduation.elderlycare.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/vital-signs")
@CrossOrigin
public class VitalSignsController {

    @Autowired
    private VitalSignsMapper vitalSignsMapper;

    @Autowired
    private ResidentMapper residentMapper;

    // 获取老人生命体征列表
    @GetMapping("/resident/{residentId}")
    public Result<?> getResidentVitalSigns(@PathVariable("residentId") Long residentId,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                         @RequestParam(required = false, defaultValue = "30") Integer limit) {
        if (isCaregiverDenied(residentId)) return Result.error("无权查看其他护理员负责的老人");
        LambdaQueryWrapper<VitalSigns> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VitalSigns::getResidentId, residentId);
        
        if (startDate != null) {
            wrapper.ge(VitalSigns::getRecordDate, startDate);
        }
        
        if (endDate != null) {
            wrapper.le(VitalSigns::getRecordDate, endDate);
        }
        
        wrapper.orderByDesc(VitalSigns::getRecordDate, VitalSigns::getRecordTime);
        wrapper.last("LIMIT " + limit);
        
        List<VitalSigns> vitalSignsList = vitalSignsMapper.selectList(wrapper);
        
        // 转换为前端需要的格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (VitalSigns vs : vitalSignsList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", vs.getId());
            item.put("date", vs.getRecordDate() != null ? vs.getRecordDate().toString() : "");
            item.put("time", vs.getRecordTime() != null ? vs.getRecordTime().toString() : null);
            
            // 体温
            if (vs.getTemperature() != null) {
                item.put("temperature", vs.getTemperature() + "°C");
            }
            
            // 血压 - 优先使用Integer字段，如果没有则使用BigDecimal字段
            Integer systolicPressure = vs.getSystolicPressure();
            Integer diastolicPressure = vs.getDiastolicPressure();
            
            if (systolicPressure == null && vs.getSystolicBp() != null) {
                systolicPressure = vs.getSystolicBp().intValue();
            }
            if (diastolicPressure == null && vs.getDiastolicBp() != null) {
                diastolicPressure = vs.getDiastolicBp().intValue();
            }
            
            if (systolicPressure != null && diastolicPressure != null) {
                item.put("bloodPressure", systolicPressure + "/" + diastolicPressure);
            }
            
            // 心率
            if (vs.getHeartRate() != null) {
                item.put("heartRate", vs.getHeartRate() + "次/分");
            }
            
            // 呼吸频率
            if (vs.getRespiratoryRate() != null) {
                item.put("respiratoryRate", vs.getRespiratoryRate() + "次/分");
            }
            
            // 血氧 - 优先使用bloodOxygen字段
            BigDecimal bloodOxygen = vs.getBloodOxygen();
            if (bloodOxygen == null) {
                bloodOxygen = vs.getOxygenSaturation();
            }
            if (bloodOxygen != null) {
                item.put("bloodOxygen", bloodOxygen + "%");
            }
            
            // 体重
            if (vs.getWeight() != null) {
                item.put("weight", vs.getWeight() + "kg");
            }
            
            // 血糖
            if (vs.getBloodGlucose() != null) {
                item.put("bloodGlucose", vs.getBloodGlucose() + "mmol/L");
            }
            
            item.put("remark", vs.getRemark());
            item.put("recorderName", vs.getRecorderName());
            item.put("recorderType", vs.getRecorderType());
            
            result.add(item);
        }
        
        return Result.success(result);
    }

    // 获取最新的生命体征记录
    @GetMapping("/resident/{residentId}/latest")
    public Result<?> getLatestVitalSigns(@PathVariable("residentId") Long residentId) {
        if (isCaregiverDenied(residentId)) return Result.error("无权查看其他护理员负责的老人");
        LambdaQueryWrapper<VitalSigns> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VitalSigns::getResidentId, residentId);
        wrapper.orderByDesc(VitalSigns::getRecordDate, VitalSigns::getRecordTime);
        wrapper.last("LIMIT 1");
        
        VitalSigns vs = vitalSignsMapper.selectOne(wrapper);
        
        if (vs == null) {
            return Result.success(null);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", vs.getId());
        result.put("date", vs.getRecordDate() != null ? vs.getRecordDate().toString() : "");
        result.put("time", vs.getRecordTime() != null ? vs.getRecordTime().toString() : null);
        result.put("temperature", vs.getTemperature());
        
        // 血压处理
        result.put("systolicPressure", vs.getSystolicPressure() != null ? vs.getSystolicPressure() : 
                  (vs.getSystolicBp() != null ? vs.getSystolicBp().intValue() : null));
        result.put("diastolicPressure", vs.getDiastolicPressure() != null ? vs.getDiastolicPressure() : 
                  (vs.getDiastolicBp() != null ? vs.getDiastolicBp().intValue() : null));
        
        result.put("heartRate", vs.getHeartRate());
        result.put("respiratoryRate", vs.getRespiratoryRate());
        
        // 血氧处理
        result.put("bloodOxygen", vs.getBloodOxygen() != null ? vs.getBloodOxygen() : vs.getOxygenSaturation());
        
        result.put("weight", vs.getWeight());
        result.put("bloodGlucose", vs.getBloodGlucose());
        result.put("remark", vs.getRemark());
        
        return Result.success(result);
    }

    // 添加生命体征记录
    @PostMapping
    public Result<?> addVitalSigns(@RequestBody Map<String, Object> request) {
        try {
            Long residentId = Long.valueOf(request.get("residentId").toString());
            if (isCaregiverDenied(residentId)) return Result.error("无权为其他护理员负责的老人添加记录");
            VitalSigns vitalSigns = new VitalSigns();
            vitalSigns.setResidentId(residentId);
            
            if (request.containsKey("residentName")) {
                vitalSigns.setResidentName(request.get("residentName").toString());
            }
            if (request.containsKey("bedNo")) {
                vitalSigns.setRoomBedNo(request.get("bedNo").toString());
            }
            if (request.containsKey("careZone")) {
                vitalSigns.setCareZone(request.get("careZone").toString());
            }
            
            // 记录日期和时间
            if (request.containsKey("recordDate")) {
                vitalSigns.setRecordDate(LocalDate.parse(request.get("recordDate").toString()));
            } else {
                vitalSigns.setRecordDate(LocalDate.now());
            }
            
            if (request.containsKey("recordTime")) {
                vitalSigns.setRecordTime(LocalTime.parse(request.get("recordTime").toString()));
            } else {
                vitalSigns.setRecordTime(LocalTime.now());
            }
            
            // 设置measurementTime（新字段）
            if (request.containsKey("measurementTime")) {
                // 如果提供了measurementTime，直接使用
                vitalSigns.setMeasurementTime(new Date());
            } else {
                // 否则根据recordDate和recordTime构造
                vitalSigns.setMeasurementTime(new Date());
            }
            
            // 生命体征数据
            if (request.containsKey("temperature") && request.get("temperature") != null) {
                vitalSigns.setTemperature(new BigDecimal(request.get("temperature").toString()));
            }
            
            // 血压处理 - 同时设置新旧字段
            if (request.containsKey("systolicPressure") && request.get("systolicPressure") != null) {
                Integer systolicPressure = Integer.valueOf(request.get("systolicPressure").toString());
                vitalSigns.setSystolicPressure(systolicPressure);
                vitalSigns.setSystolicBp(BigDecimal.valueOf(systolicPressure));
            }
            if (request.containsKey("diastolicPressure") && request.get("diastolicPressure") != null) {
                Integer diastolicPressure = Integer.valueOf(request.get("diastolicPressure").toString());
                vitalSigns.setDiastolicPressure(diastolicPressure);
                vitalSigns.setDiastolicBp(BigDecimal.valueOf(diastolicPressure));
            }
            
            if (request.containsKey("heartRate") && request.get("heartRate") != null) {
                vitalSigns.setHeartRate(new BigDecimal(request.get("heartRate").toString()));
            }
            if (request.containsKey("respiratoryRate") && request.get("respiratoryRate") != null) {
                vitalSigns.setRespiratoryRate(new BigDecimal(request.get("respiratoryRate").toString()));
            }
            
            // 血氧处理 - 同时设置新旧字段
            if (request.containsKey("bloodOxygen") && request.get("bloodOxygen") != null) {
                BigDecimal bloodOxygen = new BigDecimal(request.get("bloodOxygen").toString());
                vitalSigns.setBloodOxygen(bloodOxygen);
                vitalSigns.setOxygenSaturation(bloodOxygen);
            }
            
            if (request.containsKey("weight") && request.get("weight") != null) {
                vitalSigns.setWeight(new BigDecimal(request.get("weight").toString()));
            }
            if (request.containsKey("bloodGlucose") && request.get("bloodGlucose") != null) {
                vitalSigns.setBloodGlucose(new BigDecimal(request.get("bloodGlucose").toString()));
            }
            
            // 备注处理 - 同时设置新旧字段
            if (request.containsKey("remark")) {
                String remark = request.get("remark").toString();
                vitalSigns.setRemark(remark);
                vitalSigns.setNotes(remark);
            }
            
            if (request.containsKey("recorderId")) {
                vitalSigns.setRecorderId(Long.valueOf(request.get("recorderId").toString()));
            }
            if (request.containsKey("recorderName")) {
                String recorderName = request.get("recorderName").toString();
                vitalSigns.setRecorderName(recorderName);
                vitalSigns.setMeasuredBy(recorderName);
            }
            if (request.containsKey("recorderType")) {
                vitalSigns.setRecorderType(request.get("recorderType").toString());
            } else {
                vitalSigns.setRecorderType("CAREGIVER");
            }
            
            // 设置测量类型
            if (request.containsKey("measurementType")) {
                vitalSigns.setMeasurementType(request.get("measurementType").toString());
            } else {
                vitalSigns.setMeasurementType("ROUTINE");
            }
            
            vitalSignsMapper.insert(vitalSigns);
            
            return Result.success(vitalSigns.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("添加生命体征记录失败: " + e.getMessage());
        }
    }

    // 更新生命体征记录
    @PutMapping("/{id}")
    public Result<?> updateVitalSigns(@PathVariable Long id,
                                     @RequestBody Map<String, Object> request) {
        VitalSigns vitalSigns = vitalSignsMapper.selectById(id);
        if (vitalSigns == null) {
            return Result.error("记录不存在");
        }
        if (isCaregiverDenied(vitalSigns.getResidentId())) return Result.error("无权修改其他护理员负责的老人记录");
        
        try {
            if (request.containsKey("temperature") && request.get("temperature") != null) {
                vitalSigns.setTemperature(new BigDecimal(request.get("temperature").toString()));
            }
            
            // 血压处理
            if (request.containsKey("systolicPressure") && request.get("systolicPressure") != null) {
                Integer systolicPressure = Integer.valueOf(request.get("systolicPressure").toString());
                vitalSigns.setSystolicPressure(systolicPressure);
                vitalSigns.setSystolicBp(BigDecimal.valueOf(systolicPressure));
            }
            if (request.containsKey("diastolicPressure") && request.get("diastolicPressure") != null) {
                Integer diastolicPressure = Integer.valueOf(request.get("diastolicPressure").toString());
                vitalSigns.setDiastolicPressure(diastolicPressure);
                vitalSigns.setDiastolicBp(BigDecimal.valueOf(diastolicPressure));
            }
            
            if (request.containsKey("heartRate") && request.get("heartRate") != null) {
                vitalSigns.setHeartRate(new BigDecimal(request.get("heartRate").toString()));
            }
            if (request.containsKey("respiratoryRate") && request.get("respiratoryRate") != null) {
                vitalSigns.setRespiratoryRate(new BigDecimal(request.get("respiratoryRate").toString()));
            }
            
            // 血氧处理
            if (request.containsKey("bloodOxygen") && request.get("bloodOxygen") != null) {
                BigDecimal bloodOxygen = new BigDecimal(request.get("bloodOxygen").toString());
                vitalSigns.setBloodOxygen(bloodOxygen);
                vitalSigns.setOxygenSaturation(bloodOxygen);
            }
            
            if (request.containsKey("weight") && request.get("weight") != null) {
                vitalSigns.setWeight(new BigDecimal(request.get("weight").toString()));
            }
            if (request.containsKey("bloodGlucose") && request.get("bloodGlucose") != null) {
                vitalSigns.setBloodGlucose(new BigDecimal(request.get("bloodGlucose").toString()));
            }
            
            if (request.containsKey("remark")) {
                String remark = request.get("remark").toString();
                vitalSigns.setRemark(remark);
                vitalSigns.setNotes(remark);
            }
            
            vitalSignsMapper.updateById(vitalSigns);
            
            return Result.success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新生命体征记录失败: " + e.getMessage());
        }
    }

    // 删除生命体征记录
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        VitalSigns vitalSigns = vitalSignsMapper.selectById(id);
        if (vitalSigns == null) return Result.error("记录不存在");
        if (isCaregiverDenied(vitalSigns.getResidentId())) return Result.error("无权删除其他护理员负责的老人记录");
        vitalSignsMapper.deleteById(id);
        return Result.success(null);
    }

    // 获取生命体征趋势数据（用于图表展示）
    @GetMapping("/resident/{residentId}/trend")
    public Result<?> getVitalSignsTrend(@PathVariable("residentId") Long residentId,
                                       @RequestParam(required = false, defaultValue = "7") Integer days) {
        if (isCaregiverDenied(residentId)) return Result.error("无权查看其他护理员负责的老人");
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        LambdaQueryWrapper<VitalSigns> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VitalSigns::getResidentId, residentId);
        wrapper.ge(VitalSigns::getRecordDate, startDate);
        wrapper.le(VitalSigns::getRecordDate, endDate);
        wrapper.orderByAsc(VitalSigns::getRecordDate, VitalSigns::getRecordTime);
        
        List<VitalSigns> vitalSignsList = vitalSignsMapper.selectList(wrapper);
        
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> temperatures = new ArrayList<>();
        List<Integer> systolicPressures = new ArrayList<>();
        List<Integer> diastolicPressures = new ArrayList<>();
        List<BigDecimal> heartRates = new ArrayList<>();
        List<BigDecimal> bloodOxygens = new ArrayList<>();
        
        for (VitalSigns vs : vitalSignsList) {
            dates.add(vs.getRecordDate() != null ? vs.getRecordDate().toString() : "");
            temperatures.add(vs.getTemperature());
            
            // 血压处理
            systolicPressures.add(vs.getSystolicPressure() != null ? vs.getSystolicPressure() : 
                                 (vs.getSystolicBp() != null ? vs.getSystolicBp().intValue() : null));
            diastolicPressures.add(vs.getDiastolicPressure() != null ? vs.getDiastolicPressure() : 
                                  (vs.getDiastolicBp() != null ? vs.getDiastolicBp().intValue() : null));
            
            heartRates.add(vs.getHeartRate());
            
            // 血氧处理
            bloodOxygens.add(vs.getBloodOxygen() != null ? vs.getBloodOxygen() : vs.getOxygenSaturation());
        }
        
        result.put("dates", dates);
        result.put("temperature", temperatures);
        result.put("systolicPressure", systolicPressures);
        result.put("diastolicPressure", diastolicPressures);
        result.put("heartRate", heartRates);
        result.put("bloodOxygen", bloodOxygens);
        
        return Result.success(result);
    }

    private boolean isCaregiverDenied(Long residentId) {
        if (!"CAREGIVER".equals(AuthUtil.getCurrentRole())) return false;
        Resident resident = residentMapper.selectById(residentId);
        return resident == null || !Objects.equals(resident.getCaregiverId(), AuthUtil.getCurrentUserId());
    }
}

