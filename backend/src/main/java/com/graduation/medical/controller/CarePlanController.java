package com.graduation.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.medical.common.Result;
import com.graduation.medical.entity.CarePlan;
import com.graduation.medical.entity.CareTask;
import com.graduation.medical.entity.Resident;
import com.graduation.medical.entity.User;
import com.graduation.medical.mapper.CarePlanMapper;
import com.graduation.medical.mapper.CareTaskMapper;
import com.graduation.medical.mapper.ResidentMapper;
import com.graduation.medical.mapper.UserMapper;
import com.graduation.medical.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/care-plan")
@CrossOrigin
public class CarePlanController {

    @Autowired
    private CarePlanMapper carePlanMapper;
    
    @Autowired
    private CareTaskMapper careTaskMapper;
    
    @Autowired
    private ResidentMapper residentMapper;
    
    @Autowired
    private UserMapper userMapper;

    // 获取在住老人列表（底层旧字段仅用于兼容既有数据库）
    @GetMapping("/residents")
    public Result<?> getResidents(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resident::getStatus, "RESIDENT"); // 只查在住老人
        if ("CAREGIVER".equals(AuthUtil.getCurrentRole())) {
            Long caregiverId = AuthUtil.getCurrentUserId();
            if (caregiverId == null) wrapper.apply("1 = 0");
            else wrapper.eq(Resident::getCaregiverId, caregiverId);
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Resident::getName, keyword)
                    .or().like(Resident::getRoomBedNo, keyword)
                    .or().like(Resident::getResidentId, keyword));
        }
        
        wrapper.orderByDesc(Resident::getAdmitTime);
        wrapper.last("LIMIT 50"); // 限制返回数量
        
        List<Resident> residents = residentMapper.selectList(wrapper);
        
        List<Map<String, Object>> result = residents.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("bedNo", p.getRoomBedNo());
            map.put("careZone", p.getCareZone());
            map.put("residentId", p.getResidentId());
            map.put("age", p.getAge());
            map.put("gender", p.getGender());
            return map;
        }).collect(Collectors.toList());
        
        return Result.success(result);
    }

    // 获取护理员和照护员列表（用于选择器）
    @GetMapping("/caregivers")
    public Result<?> getCaregivers(@RequestParam(required = false) String careZone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "CAREGIVER");
        
        if (StringUtils.hasText(careZone)) {
            wrapper.eq(User::getWorkZone, careZone);
        }
        
        wrapper.orderByAsc(User::getRealName);
        
        List<User> caregivers = userMapper.selectList(wrapper);
        
        List<Map<String, Object>> result = caregivers.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("name", n.getRealName());
            map.put("careZone", n.getWorkZone());
            return map;
        }).collect(Collectors.toList());
        
        return Result.success(result);
    }

    // 创建护理计划
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createPlan(@RequestBody Map<String, Object> request) {
        try {
            CarePlan plan = new CarePlan();
            Long residentId = Long.valueOf(request.get("residentId").toString());
            Resident resident = residentMapper.selectById(residentId);
            if (resident == null) return Result.error("老人不存在");
            if ("CAREGIVER".equals(AuthUtil.getCurrentRole())
                    && !Objects.equals(resident.getCaregiverId(), AuthUtil.getCurrentUserId())) {
                return Result.error("无权为其他护理员负责的老人创建护理计划");
            }
            plan.setResidentId(residentId);
            plan.setResidentName(resident.getName());
            plan.setRoomBedNo(resident.getRoomBedNo());
            plan.setCareZone(resident.getCareZone());
            plan.setPlanTitle(request.get("planTitle").toString());
            plan.setPlanType(request.get("planType").toString());
            plan.setPlanTypeLabel(request.get("planTypeLabel").toString());
            plan.setPriority(request.get("priority").toString());
            plan.setPriorityLabel(request.get("priorityLabel").toString());
            
            // 解析日期字符串
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            plan.setStartDate(sdf.parse(request.get("startDate").toString()));
            plan.setEndDate(sdf.parse(request.get("endDate").toString()));
            
            if (request.containsKey("assignedCaregiverId") && request.get("assignedCaregiverId") != null) {
                plan.setAssignedCaregiverId(Long.valueOf(request.get("assignedCaregiverId").toString()));
            }
            plan.setAssignedCaregiver(request.get("assignedCaregiver").toString());
            plan.setNotes(request.get("notes") != null ? request.get("notes").toString() : "");
            plan.setStatus("ACTIVE");
            plan.setProgress(0);
            plan.setCreatorId(Long.valueOf(request.get("creatorId").toString()));
            plan.setCreatorName(request.get("creatorName").toString());
            plan.setCreatorRole(request.get("creatorRole").toString());
            
            carePlanMapper.insert(plan);
            
            replaceTasks(plan.getId(), (List<Map<String, Object>>) request.get("tasks"));
            
            return Result.success(plan.getId());
        } catch (Exception e) {
            throw new RuntimeException("创建护理计划失败: " + e.getMessage(), e);
        }
    }

    // 更新护理计划及任务
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updatePlan(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            CarePlan plan = carePlanMapper.selectById(id);
            if (plan == null) return Result.error("护理计划不存在");

            if (request.get("planTitle") != null) plan.setPlanTitle(request.get("planTitle").toString());
            if (request.get("planType") != null) plan.setPlanType(request.get("planType").toString());
            if (request.get("planTypeLabel") != null) plan.setPlanTypeLabel(request.get("planTypeLabel").toString());
            if (request.get("priority") != null) plan.setPriority(request.get("priority").toString());
            if (request.get("priorityLabel") != null) plan.setPriorityLabel(request.get("priorityLabel").toString());
            if (request.get("assignedCaregiverId") != null) plan.setAssignedCaregiverId(Long.valueOf(request.get("assignedCaregiverId").toString()));
            if (request.get("assignedCaregiver") != null) plan.setAssignedCaregiver(request.get("assignedCaregiver").toString());
            if (request.containsKey("notes")) plan.setNotes(Objects.toString(request.get("notes"), ""));
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            if (request.get("startDate") != null) plan.setStartDate(sdf.parse(request.get("startDate").toString()));
            if (request.get("endDate") != null) plan.setEndDate(sdf.parse(request.get("endDate").toString()));
            plan.setUpdateTime(new Date());
            carePlanMapper.updateById(plan);

            if (request.containsKey("tasks")) {
                List<Map<String, Object>> tasks = (List<Map<String, Object>>) request.get("tasks");
                replaceTasks(id, tasks);
                if (tasks == null || tasks.stream().noneMatch(task -> StringUtils.hasText(Objects.toString(task.get("title"), "")))) {
                    plan.setProgress(0);
                    if ("COMPLETED".equals(plan.getStatus())) plan.setStatus("ACTIVE");
                    carePlanMapper.updateById(plan);
                } else {
                    updatePlanProgress(id);
                }
            }
            return Result.success("护理计划更新成功");
        } catch (Exception e) {
            throw new RuntimeException("护理计划更新失败: " + e.getMessage(), e);
        }
    }

    // 查询护理计划列表
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer current,
                         @RequestParam(defaultValue = "20") Integer size,
                         @RequestParam(required = false) Long userId,
                         @RequestParam(required = false) String role,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String keyword) {
        
        Page<CarePlan> page = new Page<>(current, size);
        LambdaQueryWrapper<CarePlan> wrapper = new LambdaQueryWrapper<>();
        
        // 护理员只能看到分配给自己的或自己创建的计划
        String currentRole = AuthUtil.getCurrentRole();
        Long currentUserId = AuthUtil.getCurrentUserId();
        if ("CAREGIVER".equals(currentRole)) {
            if (currentUserId == null) wrapper.apply("1 = 0");
            else wrapper.eq(CarePlan::getAssignedCaregiverId, currentUserId);
        }
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(CarePlan::getStatus, status);
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CarePlan::getResidentName, keyword)
                    .or().like(CarePlan::getPlanTitle, keyword)
                    .or().like(CarePlan::getRoomBedNo, keyword));
        }
        
        wrapper.orderByDesc(CarePlan::getCreateTime);
        
        Page<CarePlan> result = carePlanMapper.selectPage(page, wrapper);
        
        // 查询每个计划的任务统计
        for (CarePlan plan : result.getRecords()) {
            LambdaQueryWrapper<CareTask> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(CareTask::getPlanId, plan.getId());
            List<CareTask> tasks = careTaskMapper.selectList(taskWrapper);
            
            // 计算任务统计
            if (!tasks.isEmpty()) {
                long completedCount = tasks.stream().filter(CareTask::getCompleted).count();
                int progress = (int) (completedCount * 100 / tasks.size());
                plan.setProgress(progress);
                
                // 添加任务统计字段（通过Map返回）
            }
        }
        
        // 转换为Map格式，添加任务统计
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total", result.getTotal());
        resultMap.put("size", result.getSize());
        resultMap.put("current", result.getCurrent());
        resultMap.put("pages", result.getPages());
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (CarePlan plan : result.getRecords()) {
            Map<String, Object> planMap = new HashMap<>();
            Resident currentResident = residentMapper.selectById(plan.getResidentId());
            planMap.put("id", plan.getId());
            planMap.put("residentId", plan.getResidentId());
            planMap.put("residentName", currentResident != null ? currentResident.getName() : plan.getResidentName());
            planMap.put("roomBedNo", currentResident != null ? currentResident.getRoomBedNo() : plan.getRoomBedNo());
            planMap.put("careZone", currentResident != null ? currentResident.getCareZone() : plan.getCareZone());
            planMap.put("planTitle", plan.getPlanTitle());
            planMap.put("planType", plan.getPlanType());
            planMap.put("planTypeLabel", plan.getPlanTypeLabel());
            planMap.put("priority", plan.getPriority());
            planMap.put("priorityLabel", plan.getPriorityLabel());
            planMap.put("startDate", plan.getStartDate());
            planMap.put("endDate", plan.getEndDate());
            planMap.put("assignedCaregiver", plan.getAssignedCaregiver());
            planMap.put("assignedCaregiverId", plan.getAssignedCaregiverId());
            planMap.put("status", plan.getStatus());
            planMap.put("progress", plan.getProgress());
            planMap.put("creatorName", plan.getCreatorName());
            planMap.put("creatorRole", plan.getCreatorRole());
            planMap.put("createTime", plan.getCreateTime());
            
            // 查询任务统计
            LambdaQueryWrapper<CareTask> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(CareTask::getPlanId, plan.getId());
            List<CareTask> tasks = careTaskMapper.selectList(taskWrapper);
            long completedCount = tasks.stream().filter(CareTask::getCompleted).count();
            
            planMap.put("completedTaskCount", completedCount);
            planMap.put("totalTaskCount", tasks.size());
            
            records.add(planMap);
        }
        
        resultMap.put("records", records);
        
        return Result.success(resultMap);
    }

    // 获取计划详情
    @GetMapping("/{id}")
    public Result<?> getDetail(@PathVariable Long id) {
        CarePlan plan = carePlanMapper.selectById(id);
        if (plan == null) {
            return Result.error("护理计划不存在");
        }
        if ("CAREGIVER".equals(AuthUtil.getCurrentRole())
                && !Objects.equals(plan.getAssignedCaregiverId(), AuthUtil.getCurrentUserId())) {
            return Result.error("无权查看其他护理员的护理计划");
        }
        
        LambdaQueryWrapper<CareTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(CareTask::getPlanId, id);
        taskWrapper.orderByAsc(CareTask::getSortOrder);
        List<CareTask> tasks = careTaskMapper.selectList(taskWrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan);
        result.put("tasks", tasks);
        
        return Result.success(result);
    }

    /** 查询某位老人的全部护理任务，供老人档案页使用。 */
    @GetMapping("/resident/{residentId}/tasks")
    public Result<?> getResidentTasks(@PathVariable Long residentId) {
        LambdaQueryWrapper<CarePlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(CarePlan::getResidentId, residentId)
                   .ne(CarePlan::getStatus, "CANCELLED")
                   .orderByDesc(CarePlan::getCreateTime);
        List<CarePlan> plans = carePlanMapper.selectList(planWrapper);
        if (plans.isEmpty()) return Result.success(Collections.emptyList());

        List<Long> planIds = plans.stream().map(CarePlan::getId).collect(java.util.stream.Collectors.toList());
        LambdaQueryWrapper<CareTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(CareTask::getPlanId, planIds)
                   .orderByAsc(CareTask::getCompleted, CareTask::getTime, CareTask::getSortOrder);
        return Result.success(careTaskMapper.selectList(taskWrapper));
    }

    // 更新任务完成状态
    @PutMapping("/task/{taskId}/complete")
    public Result<?> completeTask(@PathVariable Long taskId,
                                 @RequestBody Map<String, Object> request) {
        CareTask task = careTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        CarePlan plan = carePlanMapper.selectById(task.getPlanId());
        if ("CAREGIVER".equals(AuthUtil.getCurrentRole())
                && (plan == null || !Objects.equals(plan.getAssignedCaregiverId(), AuthUtil.getCurrentUserId()))) {
            return Result.error("无权操作其他护理员的任务");
        }
        
        task.setCompleted(true);
        task.setCompleteTime(new Date());
        task.setCompleteUserId(Long.valueOf(request.get("userId").toString()));
        task.setCompleteUserName(request.get("userName").toString());
        
        careTaskMapper.updateById(task);
        
        // 更新计划进度
        updatePlanProgress(task.getPlanId());
        
        return Result.success(null);
    }

    // 更新计划状态
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id,
                                 @RequestParam String status) {
        CarePlan plan = carePlanMapper.selectById(id);
        if (plan == null) {
            return Result.error("护理计划不存在");
        }
        
        plan.setStatus(status);
        carePlanMapper.updateById(plan);
        
        return Result.success(null);
    }

    // 删除计划
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(@PathVariable Long id) {
        if (carePlanMapper.selectById(id) == null) return Result.error("护理计划不存在");
        
        // 删除关联任务
        LambdaQueryWrapper<CareTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CareTask::getPlanId, id);
        careTaskMapper.delete(wrapper);
        
        return Result.success(null);
    }

    private void replaceTasks(Long planId, List<Map<String, Object>> tasks) {
        LambdaQueryWrapper<CareTask> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(CareTask::getPlanId, planId);
        careTaskMapper.delete(deleteWrapper);
        if (tasks == null) return;
        for (int i = 0; i < tasks.size(); i++) {
            Map<String, Object> taskData = tasks.get(i);
            String title = Objects.toString(taskData.get("title"), "").trim();
            if (title.isEmpty()) continue;
            CareTask task = new CareTask();
            task.setPlanId(planId);
            task.setTitle(title);
            task.setTime(Objects.toString(taskData.get("time"), ""));
            task.setCompleted(Boolean.TRUE.equals(taskData.get("completed")));
            task.setSortOrder(i);
            careTaskMapper.insert(task);
        }
    }

    // 更新计划进度
    private void updatePlanProgress(Long planId) {
        LambdaQueryWrapper<CareTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CareTask::getPlanId, planId);
        List<CareTask> tasks = careTaskMapper.selectList(wrapper);
        
        if (!tasks.isEmpty()) {
            long completedCount = tasks.stream().filter(CareTask::getCompleted).count();
            int progress = (int) (completedCount * 100 / tasks.size());
            
            CarePlan plan = carePlanMapper.selectById(planId);
            plan.setProgress(progress);
            
            // 如果全部完成，自动更新状态
            if (progress == 100) {
                plan.setStatus("COMPLETED");
            }
            
            carePlanMapper.updateById(plan);
        }
    }
}
