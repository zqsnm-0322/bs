package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.VisitAppointment;
import com.graduation.elderlycare.mapper.VisitAppointmentMapper;
import com.graduation.elderlycare.mapper.ResidentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/visit")
@CrossOrigin
public class VisitController {

    @Autowired
    private VisitAppointmentMapper visitAppointmentMapper;
    @Autowired
    private ResidentMapper residentMapper;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer current,
                          @RequestParam(defaultValue = "20") Integer size,
                          @RequestParam(required = false) Long residentId,
                          @RequestParam(required = false) String status) {
        Page<VisitAppointment> page = new Page<>(current, size);
        LambdaQueryWrapper<VisitAppointment> wrapper = new LambdaQueryWrapper<>();
        if (residentId != null) wrapper.eq(VisitAppointment::getResidentId, residentId);
        if (status != null) wrapper.eq(VisitAppointment::getStatus, status);
        wrapper.orderByDesc(VisitAppointment::getCreateTime);
        return Result.success(visitAppointmentMapper.selectPage(page, wrapper));
    }

    @PostMapping
    public Result<?> add(@RequestBody VisitAppointment appointment) {
        if (appointment.getResidentId() == null) return Result.error("请选择探访老人");
        if (appointment.getVisitorName() == null || appointment.getVisitorName().trim().isEmpty()) {
            return Result.error("探访人姓名不能为空");
        }
        if (appointment.getVisitDate() == null) return Result.error("探访日期不能为空");
        com.graduation.elderlycare.entity.Resident resident = residentMapper.selectById(appointment.getResidentId());
        if (resident == null || !"RESIDENT".equals(resident.getStatus())) return Result.error("在住老人不存在");
        appointment.setResidentName(resident.getName());
        appointment.setStatus("PENDING");
        if (appointment.getVisitorCount() == null || appointment.getVisitorCount() < 1) {
            appointment.setVisitorCount(1);
        }
        visitAppointmentMapper.insert(appointment);
        return Result.success("探访预约提交成功，等待审批");
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        VisitAppointment appointment = visitAppointmentMapper.selectById(id);
        return appointment == null ? Result.error("预约不存在") : Result.success(appointment);
    }

    @PutMapping
    public Result<?> update(@RequestBody VisitAppointment appointment) {
        if (appointment.getId() == null) return Result.error("预约ID不能为空");
        VisitAppointment existing = visitAppointmentMapper.selectById(appointment.getId());
        if (existing == null) return Result.error("预约不存在");
        if (!"PENDING".equals(existing.getStatus())) return Result.error("只能编辑待确认的预约");
        if (appointment.getResidentId() != null) {
            com.graduation.elderlycare.entity.Resident resident = residentMapper.selectById(appointment.getResidentId());
            if (resident == null || !"RESIDENT".equals(resident.getStatus())) return Result.error("在住老人不存在");
            appointment.setResidentName(resident.getName());
        }
        // 审批状态只能通过专用审批接口改变。
        appointment.setStatus(null);
        appointment.setApprovedBy(null);
        appointment.setApproveTime(null);
        appointment.setRejectReason(null);
        visitAppointmentMapper.updateById(appointment);
        return Result.success("预约更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        VisitAppointment appointment = visitAppointmentMapper.selectById(id);
        if (appointment == null) return Result.error("预约不存在");
        if ("APPROVED".equals(appointment.getStatus()) || "COMPLETED".equals(appointment.getStatus())) {
            return Result.error("已批准或已完成的预约不能删除");
        }
        visitAppointmentMapper.deleteById(id);
        return Result.success("预约删除成功");
    }

    @PostMapping("/approve/{id}")
    public Result<?> approve(@PathVariable Long id,
                             @RequestBody(required = false) Map<String, String> body) {
        VisitAppointment appt = visitAppointmentMapper.selectById(id);
        if (appt == null) return Result.error("预约不存在");
        if (!"PENDING".equals(appt.getStatus())) return Result.error("只能审批待确认的预约");
        appt.setStatus("APPROVED");
        appt.setApprovedBy(body == null ? null : body.get("approvedBy"));
        appt.setApproveTime(new Date());
        visitAppointmentMapper.updateById(appt);
        return Result.success("审批通过");
    }

    @PostMapping("/reject/{id}")
    public Result<?> reject(@PathVariable Long id,
                           @RequestBody Map<String, String> body) {
        VisitAppointment appt = visitAppointmentMapper.selectById(id);
        if (appt == null) return Result.error("预约不存在");
        if (!"PENDING".equals(appt.getStatus())) return Result.error("只能拒绝待确认的预约");
        if (body == null || body.get("reason") == null || body.get("reason").trim().isEmpty()) {
            return Result.error("请填写拒绝原因");
        }
        appt.setStatus("REJECTED");
        appt.setApprovedBy(body.get("approvedBy"));
        appt.setApproveTime(new Date());
        appt.setRejectReason(body.get("reason"));
        visitAppointmentMapper.updateById(appt);
        return Result.success("已拒绝");
    }

    @PostMapping("/cancel/{id}")
    public Result<?> cancel(@PathVariable Long id) {
        VisitAppointment appt = visitAppointmentMapper.selectById(id);
        if (appt == null) return Result.error("预约不存在");
        if (!"PENDING".equals(appt.getStatus()) && !"APPROVED".equals(appt.getStatus())) {
            return Result.error("只能取消待确认或已批准的预约");
        }
        appt.setStatus("CANCELLED");
        visitAppointmentMapper.updateById(appt);
        return Result.success("预约已取消");
    }

    @PostMapping("/complete/{id}")
    public Result<?> complete(@PathVariable Long id) {
        VisitAppointment appt = visitAppointmentMapper.selectById(id);
        if (appt == null) return Result.error("预约不存在");
        if (!"APPROVED".equals(appt.getStatus())) return Result.error("只能完成已批准的预约");
        appt.setStatus("COMPLETED");
        visitAppointmentMapper.updateById(appt);
        return Result.success("探访已完成");
    }
}

