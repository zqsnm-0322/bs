package com.graduation.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.graduation.medical.common.Result;
import com.graduation.medical.entity.Medication;
import com.graduation.medical.entity.Resident;
import com.graduation.medical.entity.User;
import com.graduation.medical.mapper.MedicationMapper;
import com.graduation.medical.mapper.ResidentMapper;
import com.graduation.medical.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 用药记录管理Controller
 */
@RestController
@RequestMapping("/api/medication")
@CrossOrigin
public class MedicationController {

    @Autowired
    private MedicationMapper medicationMapper;
    
    @Autowired
    private ResidentMapper residentMapper;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 从token中获取当前用户信息
     */
    private Map<String, Object> getCurrentUserInfo(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // 移除 Bearer 前缀
        token = token.replace("Bearer ", "").trim();
        
        try {
            // token格式: token-{userType}-{userId}-{timestamp}
            String[] parts = token.split("-");
            if (parts.length >= 3) {
                String userType = parts[1];
                Long userId = Long.parseLong(parts[2]);
                
                if ("CAREGIVER".equals(userType) || "CARE_MANAGER".equals(userType) || "ADMIN".equals(userType)) {
                    User user = userMapper.selectById(userId);
                    if (user != null) {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("id", user.getId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("realName", user.getRealName());
                        userInfo.put("role", user.getRole());
                        userInfo.put("department", user.getDepartment());
                        return userInfo;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析token失败: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 查询老人用药记录
     * @param residentId 老人ID
     * @param status 状态筛选（可选）：ACTIVE-进行中, COMPLETED-已完成, STOPPED-已停药
     */
    @GetMapping("/resident/{residentId}")
    public Result<?> getResidentMedications(@PathVariable("residentId") Long residentId,
                                           @RequestParam(required = false) String status,
                                           @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 权限检查：照护主管和护理员只能查看自己负责的老人，管理员可以查看所有
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo != null) {
                String role = (String) userInfo.get("role");
                String realName = (String) userInfo.get("realName");
                String department = (String) userInfo.get("department");
                
                // 查询老人信息
                Resident resident = residentMapper.selectById(residentId);
                if (resident == null) {
                    return Result.error("老人不存在");
                }
                
                // 权限验证
                if ("CARE_MANAGER".equals(role)) {
                    // 照护主管只能查看自己负责的老人
                    if (!realName.equals(resident.getCareManager())) {
                        return Result.error("无权查看该老人的用药记录");
                    }
                } else if ("CAREGIVER".equals(role)) {
                    // 护理员只能查看本人负责的老人
                    if (!Objects.equals(userInfo.get("id"), resident.getCaregiverId())) {
                        return Result.error("无权查看该老人的用药记录");
                    }
                }
                // 管理员可以查看所有老人
            }
            
            // 构建查询条件
            LambdaQueryWrapper<Medication> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Medication::getResidentId, residentId);
            
            if (StringUtils.hasText(status)) {
                wrapper.eq(Medication::getStatus, status);
            }
            
            wrapper.orderByDesc(Medication::getStartDate);
            
            List<Medication> medications = medicationMapper.selectList(wrapper);
            return Result.success(medications);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询用药记录失败: " + e.getMessage());
        }
    }

    /**
     * 添加用药记录
     * 权限：照护主管可以为自己负责的老人添加，护理员可以为自己负责的老人添加，管理员可以为任何老人添加
     */
    @PostMapping
    public Result<?> addMedication(@RequestBody Medication medication,
                                   @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 权限检查
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo == null) {
                return Result.error("未登录或token无效");
            }
            
            String role = (String) userInfo.get("role");
            String realName = (String) userInfo.get("realName");
            Long userId = (Long) userInfo.get("id");
            
            // 查询老人信息
            Resident resident = residentMapper.selectById(medication.getResidentId());
            if (resident == null) {
                return Result.error("老人不存在");
            }
            
            // 权限验证
            boolean hasPermission = false;
            
            if ("ADMIN".equals(role) || "R_SUPER".equals(role)) {
                // 管理员可以为任何老人添加
                hasPermission = true;
            } else if ("CARE_MANAGER".equals(role) || "R_CARE_MANAGER".equals(role)) {
                // 照护主管只能为自己负责的老人添加用药
                if (realName.equals(resident.getCareManager())) {
                    hasPermission = true;
                }
            } else if ("CAREGIVER".equals(role) || "R_CAREGIVER".equals(role)) {
                // 护理员可以为自己负责的老人添加用药记录
                if (Objects.equals(userInfo.get("id"), resident.getCaregiverId())) {
                    hasPermission = true;
                }
            }
            
            if (!hasPermission) {
                return Result.error("无权为该老人添加用药记录，只能为自己负责的老人添加");
            }
            
            // 设置老人姓名
            medication.setResidentName(resident.getName());
            
            // 设置开具照护主管信息
            medication.setPrescribedBy(realName);
            medication.setPrescribedById(userId);
            
            // 设置默认状态
            if (!StringUtils.hasText(medication.getStatus())) {
                medication.setStatus("ACTIVE");
            }
            
            // 设置创建时间
            medication.setCreateTime(LocalDateTime.now());
            medication.setUpdateTime(LocalDateTime.now());
            
            medicationMapper.insert(medication);
            return Result.success(medication);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("添加用药记录失败: " + e.getMessage());
        }
    }

    /**
     * 更新用药记录
     * 权限：照护主管可以更新自己开具的用药，护理员可以更新自己负责老人的用药，管理员可以更新任何用药
     */
    @PutMapping("/{id}")
    public Result<?> updateMedication(@PathVariable Long id,
                                      @RequestBody Medication medication,
                                      @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 权限检查
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo == null) {
                return Result.error("未登录或token无效");
            }
            
            String role = (String) userInfo.get("role");
            String realName = (String) userInfo.get("realName");
            
            // 查询原用药记录
            Medication existingMedication = medicationMapper.selectById(id);
            if (existingMedication == null) {
                return Result.error("用药记录不存在");
            }
            
            // 查询老人信息
            Resident resident = residentMapper.selectById(existingMedication.getResidentId());
            if (resident == null) {
                return Result.error("老人不存在");
            }
            
            // 权限验证
            boolean hasPermission = false;
            
            if ("ADMIN".equals(role) || "R_SUPER".equals(role)) {
                // 管理员可以更新任何用药
                hasPermission = true;
            } else if ("CARE_MANAGER".equals(role) || "R_CARE_MANAGER".equals(role)) {
                // 照护主管可以更新自己开具的用药，或自己负责老人的用药
                if (realName.equals(existingMedication.getPrescribedBy()) || 
                    realName.equals(resident.getCareManager())) {
                    hasPermission = true;
                }
            } else if ("CAREGIVER".equals(role) || "R_CAREGIVER".equals(role)) {
                // 护理员可以更新自己负责老人的用药记录
                if (Objects.equals(userInfo.get("id"), resident.getCaregiverId())) {
                    hasPermission = true;
                }
            }
            
            if (!hasPermission) {
                return Result.error("无权修改该用药记录");
            }
            
            // 更新字段
            medication.setId(id);
            medication.setUpdateTime(LocalDateTime.now());
            
            medicationMapper.updateById(medication);
            return Result.success(medication);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新用药记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除用药记录
     * 权限：照护主管可以删除自己开具的用药，护理员可以删除自己负责老人的用药，管理员可以删除任何用药
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteMedication(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 权限检查
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo == null) {
                return Result.error("未登录或token无效");
            }
            
            String role = (String) userInfo.get("role");
            String realName = (String) userInfo.get("realName");
            
            // 查询用药记录
            Medication medication = medicationMapper.selectById(id);
            if (medication == null) {
                return Result.error("用药记录不存在");
            }
            
            // 查询老人信息
            Resident resident = residentMapper.selectById(medication.getResidentId());
            if (resident == null) {
                return Result.error("老人不存在");
            }
            
            // 权限验证
            boolean hasPermission = false;
            
            if ("ADMIN".equals(role) || "R_SUPER".equals(role)) {
                // 管理员可以删除任何用药
                hasPermission = true;
            } else if ("CARE_MANAGER".equals(role) || "R_CARE_MANAGER".equals(role)) {
                // 照护主管可以删除自己开具的用药，或自己负责老人的用药
                if (realName.equals(medication.getPrescribedBy()) || 
                    realName.equals(resident.getCareManager())) {
                    hasPermission = true;
                }
            } else if ("CAREGIVER".equals(role) || "R_CAREGIVER".equals(role)) {
                // 护理员可以删除自己负责老人的用药记录
                if (Objects.equals(userInfo.get("id"), resident.getCaregiverId())) {
                    hasPermission = true;
                }
            }
            
            if (!hasPermission) {
                return Result.error("无权删除该用药记录");
            }
            
            medicationMapper.deleteById(id);
            return Result.success("删除成功");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除用药记录失败: " + e.getMessage());
        }
    }

    /**
     * 停药
     * 权限：照护主管可以停止自己开具的用药，护理员可以停止自己负责老人的用药，管理员可以停止任何用药
     */
    @PutMapping("/{id}/stop")
    public Result<?> stopMedication(@PathVariable Long id,
                                    @RequestParam(required = false) String stopReason,
                                    @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 权限检查
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo == null) {
                return Result.error("未登录或token无效");
            }
            
            String role = (String) userInfo.get("role");
            String realName = (String) userInfo.get("realName");
            
            // 查询用药记录
            Medication medication = medicationMapper.selectById(id);
            if (medication == null) {
                return Result.error("用药记录不存在");
            }
            
            // 查询老人信息
            Resident resident = residentMapper.selectById(medication.getResidentId());
            if (resident == null) {
                return Result.error("老人不存在");
            }
            
            // 权限验证
            boolean hasPermission = false;
            
            if ("ADMIN".equals(role) || "R_SUPER".equals(role)) {
                // 管理员可以停止任何用药
                hasPermission = true;
            } else if ("CARE_MANAGER".equals(role) || "R_CARE_MANAGER".equals(role)) {
                // 照护主管可以停止自己开具的用药，或自己负责老人的用药
                if (realName.equals(medication.getPrescribedBy()) || 
                    realName.equals(resident.getCareManager())) {
                    hasPermission = true;
                }
            } else if ("CAREGIVER".equals(role) || "R_CAREGIVER".equals(role)) {
                // 护理员可以停止自己负责老人的用药
                if (Objects.equals(userInfo.get("id"), resident.getCaregiverId())) {
                    hasPermission = true;
                }
            }
            
            if (!hasPermission) {
                return Result.error("无权停止该用药");
            }
            
            // 更新状态为已停药
            UpdateWrapper<Medication> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", id);
            updateWrapper.set("status", "STOPPED");
            updateWrapper.set("stop_time", LocalDateTime.now());
            if (StringUtils.hasText(stopReason)) {
                updateWrapper.set("stop_reason", stopReason);
            }
            updateWrapper.set("update_time", LocalDateTime.now());
            
            medicationMapper.update(null, updateWrapper);
            
            // 返回更新后的记录
            Medication updatedMedication = medicationMapper.selectById(id);
            return Result.success(updatedMedication);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("停药失败: " + e.getMessage());
        }
    }

    /**
     * 获取用药记录详情
     */
    @GetMapping("/{id}")
    public Result<?> getMedicationDetail(@PathVariable Long id,
                                         @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Medication medication = medicationMapper.selectById(id);
            if (medication == null) {
                return Result.error("用药记录不存在");
            }
            
            // 权限检查（可选）
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo != null) {
                String role = (String) userInfo.get("role");
                String realName = (String) userInfo.get("realName");
                String department = (String) userInfo.get("department");
                
                // 查询老人信息
                Resident resident = residentMapper.selectById(medication.getResidentId());
                if (resident != null) {
                    if ("CARE_MANAGER".equals(role) && !realName.equals(resident.getCareManager())) {
                        return Result.error("无权查看该用药记录");
                    } else if ("CAREGIVER".equals(role) && !Objects.equals(userInfo.get("id"), resident.getCaregiverId())) {
                        return Result.error("无权查看该用药记录");
                    }
                }
            }
            
            return Result.success(medication);
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询用药记录失败: " + e.getMessage());
        }
    }
}
