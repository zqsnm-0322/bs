package com.graduation.elderlycare.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.graduation.elderlycare.common.Result;
import com.graduation.elderlycare.entity.Bed;
import com.graduation.elderlycare.entity.Resident;
import com.graduation.elderlycare.entity.User;
import com.graduation.elderlycare.mapper.BedMapper;
import com.graduation.elderlycare.mapper.ResidentMapper;
import com.graduation.elderlycare.mapper.UserMapper;
import com.graduation.elderlycare.util.MD5Util;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/resident")
@CrossOrigin
public class ResidentController {

    @Autowired
    private ResidentMapper residentMapper;
    @Autowired
    private BedMapper bedMapper;
    @Autowired
    private UserMapper userMapper;

    private Map<String, Object> getCurrentUserInfo(String token) {
        if (token == null || token.isEmpty()) return null;
        token = token.replace("Bearer ", "").trim();
        try {
            String[] parts = token.split("-");
            if (parts.length >= 3) {
                String userType = parts[1];
                Long userId = Long.parseLong(parts[2]);
                if ("CAREGIVER".equals(userType)
                    || "CARE_MANAGER".equals(userType) || "ADMIN".equals(userType) || "DIRECTOR".equals(userType)) {
                    User user = userMapper.selectById(userId);
                    if (user != null) {
                        Map<String, Object> info = new HashMap<>();
                        info.put("id", user.getId());
                        info.put("username", user.getUsername());
                        info.put("realName", user.getRealName());
                        info.put("role", user.getRole());
                        info.put("careZone", user.getCareZone());
                        return info;
                    }
                }
                if ("RESIDENT".equals(userType)) {
                    Resident resident = residentMapper.selectById(userId);
                    if (resident != null) {
                        Map<String, Object> info = new HashMap<>();
                        info.put("id", resident.getId());
                        info.put("username", resident.getUsername());
                        info.put("realName", resident.getName());
                        info.put("role", "RESIDENT");
                        return info;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析token失败: " + e.getMessage());
        }
        return null;
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String roomBedNo,
            @RequestParam(required = false) String residentId,
            @RequestParam(required = false) String careZone,
            @RequestParam(required = false) String healthLevel,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Page<Resident> page = new Page<>(current, size);
        LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(name)) wrapper.like(Resident::getName, name);
        if (StringUtils.hasText(roomBedNo)) wrapper.like(Resident::getRoomBedNo, roomBedNo);
        if (StringUtils.hasText(residentId)) wrapper.like(Resident::getResidentId, residentId);
        if (StringUtils.hasText(careZone)) wrapper.eq(Resident::getCareZone, careZone);
        if (StringUtils.hasText(healthLevel)) wrapper.eq(Resident::getHealthLevel, healthLevel);

        Map<String, Object> userInfo = getCurrentUserInfo(token);
        if (userInfo != null) {
            String role = (String) userInfo.get("role");
            String realName = (String) userInfo.get("realName");
            if ("CAREGIVER".equals(role)) {
                Long caregiverId = (Long) userInfo.get("id");
                if (caregiverId != null) {
                    wrapper.eq(Resident::getCaregiverId, caregiverId);
                } else {
                    wrapper.apply("1 = 0");
                }
            }
            if ("CARE_MANAGER".equals(role)) {
                wrapper.eq(Resident::getCareManager, realName);
            }
        }

        wrapper.eq(Resident::getStatus, "RESIDENT");
        wrapper.orderByDesc(Resident::getAdmitTime);

        Page<Resident> result = residentMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<?> add(@RequestBody Resident resident) {
        try {
            if (!StringUtils.hasText(resident.getName())) {
                return Result.error("老人姓名不能为空");
            }
            if (resident.getAge() == null || resident.getAge() < 60 || resident.getAge() > 120) {
                return Result.error("入住老人年龄应为60至120岁");
            }

            Bed selectedBed = null;
            if (StringUtils.hasText(resident.getRoomBedNo())) {
                LambdaQueryWrapper<Bed> bedWrapper = new LambdaQueryWrapper<>();
                bedWrapper.eq(Bed::getRoomBedNo, resident.getRoomBedNo());
                if (StringUtils.hasText(resident.getCareZone())) {
                    bedWrapper.eq(Bed::getCareZone, resident.getCareZone());
                }
                selectedBed = bedMapper.selectOne(bedWrapper);
                if (selectedBed == null) {
                    return Result.error("所选房间床位不存在");
                }
                if (!"AVAILABLE".equals(selectedBed.getStatus()) || selectedBed.getResidentId() != null) {
                    return Result.error("所选房间床位不可用");
                }
                resident.setCareZone(selectedBed.getCareZone());
            }

            String residentId = generateResidentId();
            resident.setResidentId(residentId);

            if (resident.getPassword() != null && !resident.getPassword().isEmpty()) {
                resident.setPassword(MD5Util.encrypt(resident.getPassword()));
            }
            if (resident.getAdmitTime() == null) {
                resident.setAdmitTime(new Date());
            }
            if (resident.getStatus() == null) {
                resident.setStatus("RESIDENT");
            }
            if (resident.getHealthLevel() == null) {
                resident.setHealthLevel("自理");
            }

            residentMapper.insert(resident);

            if (selectedBed != null) {
                selectedBed.setResidentId(resident.getId());
                selectedBed.setResidentName(resident.getName());
                selectedBed.setStatus("OCCUPIED");
                bedMapper.updateById(selectedBed);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("residentId", residentId);
            result.put("message", "入住登记成功");
            return Result.success(result);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("入住登记失败: " + e.getMessage());
        }
    }

    @PostMapping("/leave/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> leave(@PathVariable Long id,
                           @RequestBody Map<String, Object> request) {
        try {
            String reason = request == null ? null : (String) request.get("leaveReason");
            Resident resident = residentMapper.selectById(id);
            if (resident == null) return Result.error("老人不存在");

            resident.setLeaveTime(new Date());
            resident.setLeaveReason(reason);
            resident.setStatus("LEAVE");
            residentMapper.updateById(resident);
            residentMapper.update(null, new LambdaUpdateWrapper<Resident>()
                .eq(Resident::getId, id)
                .set(Resident::getRoomBedNo, null));

            LambdaQueryWrapper<Bed> bedWrapper = new LambdaQueryWrapper<>();
            bedWrapper.eq(Bed::getResidentId, id)
                      .or(StringUtils.hasText(resident.getRoomBedNo()),
                          wrapper -> wrapper.eq(Bed::getRoomBedNo, resident.getRoomBedNo()));
            Bed bed = bedMapper.selectOne(bedWrapper);
            if (bed != null) {
                bedMapper.update(null, new LambdaUpdateWrapper<Bed>()
                    .eq(Bed::getId, bed.getId())
                    .set(Bed::getResidentId, null)
                    .set(Bed::getResidentName, null)
                    .set(Bed::getStatus, "AVAILABLE"));
            }

            return Result.success("离院办理成功，床位已释放");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("离院办理失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id,
                            @RequestHeader(value = "Authorization", required = false) String token) {
        Resident resident = residentMapper.selectById(id);
        if (resident == null) return Result.error("老人不存在");
        Map<String, Object> userInfo = getCurrentUserInfo(token);
        if (userInfo != null && "CAREGIVER".equals(userInfo.get("role"))
                && !Objects.equals(resident.getCaregiverId(), userInfo.get("id"))) {
            return Result.error("无权查看其他护理员负责的老人");
        }
        if (userInfo != null && "RESIDENT".equals(userInfo.get("role"))
                && !Objects.equals(resident.getId(), userInfo.get("id"))) {
            return Result.error("入住老人只能查看自己的养老档案");
        }
        return Result.success(resident);
    }

    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<?> update(@RequestBody Resident resident,
                            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Map<String, Object> userInfo = getCurrentUserInfo(token);
            if (userInfo != null && "RESIDENT".equals(userInfo.get("role"))) {
                return Result.error("入住老人无权修改养老档案");
            }
            if (resident.getId() == null) return Result.error("老人ID不能为空");
            Resident existing = residentMapper.selectById(resident.getId());
            if (existing == null) return Result.error("老人不存在");
            if (resident.getAge() != null && (resident.getAge() < 60 || resident.getAge() > 120)) {
                return Result.error("老人年龄应为60至120岁");
            }

            if (resident.getRoomBedNo() != null
                    && !Objects.equals(resident.getRoomBedNo(), existing.getRoomBedNo())) {
                Bed targetBed = null;
                if (StringUtils.hasText(resident.getRoomBedNo())) {
                    LambdaQueryWrapper<Bed> targetWrapper = new LambdaQueryWrapper<>();
                    targetWrapper.eq(Bed::getRoomBedNo, resident.getRoomBedNo());
                    targetBed = bedMapper.selectOne(targetWrapper);
                    if (targetBed == null) return Result.error("目标房间床位不存在");
                    if (!"AVAILABLE".equals(targetBed.getStatus()) || targetBed.getResidentId() != null) {
                        return Result.error("目标房间床位不可用");
                    }
                    resident.setCareZone(targetBed.getCareZone());
                }

                LambdaQueryWrapper<Bed> currentWrapper = new LambdaQueryWrapper<>();
                currentWrapper.eq(Bed::getResidentId, resident.getId());
                Bed currentBed = bedMapper.selectOne(currentWrapper);
                if (currentBed != null) {
                    bedMapper.update(null, new LambdaUpdateWrapper<Bed>()
                        .eq(Bed::getId, currentBed.getId())
                        .set(Bed::getResidentId, null)
                        .set(Bed::getResidentName, null)
                        .set(Bed::getStatus, "AVAILABLE"));
                }
                if (targetBed != null) {
                    targetBed.setResidentId(existing.getId());
                    targetBed.setResidentName(existing.getName());
                    targetBed.setStatus("OCCUPIED");
                    bedMapper.updateById(targetBed);
                }
            }
            residentMapper.updateById(resident);
            return Result.success("更新成功");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    private String generateResidentId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        String year = dateStr.substring(0, 4);
        String prefix = "YZ" + year;

        LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Resident::getResidentId, prefix)
               .orderByDesc(Resident::getResidentId)
               .last("LIMIT 1");
        Resident last = residentMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getResidentId() != null) {
            String lastSequence = last.getResidentId().substring(prefix.length());
            seq = Integer.parseInt(lastSequence) + 1;
        }
        return String.format("YZ%s%04d", year, seq);
    }
}


