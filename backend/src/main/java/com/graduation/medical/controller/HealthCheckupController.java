package com.graduation.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.medical.common.Result;
import com.graduation.medical.entity.HealthCheckup;
import com.graduation.medical.mapper.HealthCheckupMapper;
import com.graduation.medical.mapper.ResidentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/health-checkup")
@CrossOrigin
public class HealthCheckupController {

    @Autowired
    private HealthCheckupMapper healthCheckupMapper;
    @Autowired
    private ResidentMapper residentMapper;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer current,
                          @RequestParam(defaultValue = "20") Integer size,
                          @RequestParam(required = false) Long residentId,
                          @RequestParam(required = false) String residentName) {
        Page<HealthCheckup> page = new Page<>(current, size);
        LambdaQueryWrapper<HealthCheckup> wrapper = new LambdaQueryWrapper<>();
        if (residentId != null) wrapper.eq(HealthCheckup::getResidentId, residentId);
        if (residentName != null) wrapper.like(HealthCheckup::getResidentName, residentName);
        wrapper.orderByDesc(HealthCheckup::getCheckDate);
        return Result.success(healthCheckupMapper.selectPage(page, wrapper));
    }

    @PostMapping
    public Result<?> add(@RequestBody HealthCheckup checkup) {
        if (checkup.getResidentId() == null) return Result.error("老人ID不能为空");
        if (checkup.getCheckDate() == null) return Result.error("检查日期不能为空");
        com.graduation.medical.entity.Resident resident = residentMapper.selectById(checkup.getResidentId());
        if (resident == null) return Result.error("老人不存在");
        checkup.setResidentName(resident.getName());
        calculateBmi(checkup);
        healthCheckupMapper.insert(checkup);
        return Result.success("健康检查记录保存成功");
    }

    private void calculateBmi(HealthCheckup checkup) {
        if (checkup.getHeight() != null && checkup.getWeight() != null
                && checkup.getHeight().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heightInMeters = checkup.getHeight()
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal bmi = checkup.getWeight().divide(
                    heightInMeters.multiply(heightInMeters), 2, RoundingMode.HALF_UP);
            checkup.setBmi(bmi);
        }
    }

    @PutMapping
    public Result<?> update(@RequestBody HealthCheckup checkup) {
        if (checkup.getId() == null || healthCheckupMapper.selectById(checkup.getId()) == null) {
            return Result.error("健康检查记录不存在");
        }
        calculateBmi(checkup);
        healthCheckupMapper.updateById(checkup);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (healthCheckupMapper.deleteById(id) == 0) return Result.error("健康检查记录不存在");
        return Result.success("删除成功");
    }
}
