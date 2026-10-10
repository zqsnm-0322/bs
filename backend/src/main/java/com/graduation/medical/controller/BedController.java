package com.graduation.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.medical.common.Result;
import com.graduation.medical.entity.Bed;
import com.graduation.medical.entity.Resident;
import com.graduation.medical.mapper.BedMapper;
import com.graduation.medical.mapper.ResidentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bed")
@CrossOrigin
public class BedController {

    @Autowired
    private BedMapper bedMapper;

    @Autowired
    private ResidentMapper residentMapper;

    @PostMapping
    public Result<?> add(@RequestBody Bed bed) {
        String validation = validateBed(bed);
        if (validation != null) return Result.error(validation);
        if (findByNumber(bed.getRoomBedNo(), null) != null) return Result.error("房间床位号已存在");
        bed.setStatus("AVAILABLE");
        bed.setResidentId(null);
        bed.setResidentName(null);
        bedMapper.insert(bed);
        return Result.success("床位新增成功");
    }

    @PutMapping
    public Result<?> update(@RequestBody Bed bed) {
        if (bed.getId() == null) return Result.error("床位ID不能为空");
        Bed existing = bedMapper.selectById(bed.getId());
        if (existing == null) return Result.error("床位不存在");
        String validation = validateBed(bed);
        if (validation != null) return Result.error(validation);
        Bed duplicate = findByNumber(bed.getRoomBedNo(), bed.getId());
        if (duplicate != null) return Result.error("房间床位号已存在");
        // 入住关系和状态只能由分配、离院、维护接口改变。
        bed.setStatus(null);
        bed.setResidentId(null);
        bed.setResidentName(null);
        bedMapper.updateById(bed);
        return Result.success("床位更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Bed bed = bedMapper.selectById(id);
        if (bed == null) return Result.error("床位不存在");
        if (bed.getResidentId() != null || "OCCUPIED".equals(bed.getStatus())) {
            return Result.error("已入住床位不能删除，请先办理换床或离院");
        }
        bedMapper.deleteById(id);
        return Result.success("床位删除成功");
    }

    /**
     * 调换床位 - 护理员功能
     * 支持两位老人互换床位，或将老人调换到空床位
     */
    @PostMapping("/swap")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> swapBeds(@RequestBody Map<String, Object> request) {
        try {
            Long residentId1 = Long.valueOf(requiredValue(request, "residentId1").toString());
            String roomBedNo1 = requiredValue(request, "roomBedNo1").toString();
            String roomBedNo2 = requiredValue(request, "roomBedNo2").toString();
            String careZone = requiredValue(request, "careZone").toString();

            // 获取老人1信息
            Resident resident1 = residentMapper.selectById(residentId1);
            if (resident1 == null) {
                return Result.error("老人不存在");
            }

            // 查询目标床位
            LambdaQueryWrapper<Bed> bedWrapper2 = new LambdaQueryWrapper<>();
            bedWrapper2.eq(Bed::getRoomBedNo, roomBedNo2)
                      .eq(Bed::getCareZone, careZone);
            Bed targetBed = bedMapper.selectOne(bedWrapper2);

            if (targetBed == null) {
                return Result.error("目标床位不存在");
            }

            if ("MAINTENANCE".equals(targetBed.getStatus())) {
                return Result.error("目标床位正在维护中，无法调换");
            }

            // 查询原床位
            LambdaQueryWrapper<Bed> bedWrapper1 = new LambdaQueryWrapper<>();
            bedWrapper1.eq(Bed::getRoomBedNo, roomBedNo1)
                      .eq(Bed::getCareZone, careZone);
            Bed sourceBed = bedMapper.selectOne(bedWrapper1);

            // 如果目标床位已被占用，进行互换
            if ("OCCUPIED".equals(targetBed.getStatus()) && targetBed.getResidentId() != null) {
                if (sourceBed == null) {
                    return Result.error("原床位不存在，无法与已占用床位互换");
                }
                Resident resident2 = residentMapper.selectById(targetBed.getResidentId());
                if (resident2 == null) {
                    return Result.error("目标床位的老人信息不存在");
                }

                // 互换床位
                // 更新老人1到床位2
                resident1.setRoomBedNo(roomBedNo2);
                resident1.setCareZone(targetBed.getCareZone());
                residentMapper.updateById(resident1);

                // 更新老人2到床位1
                resident2.setRoomBedNo(roomBedNo1);
                resident2.setCareZone(sourceBed == null ? careZone : sourceBed.getCareZone());
                residentMapper.updateById(resident2);

                // 更新床位1信息
                if (sourceBed != null) {
                    sourceBed.setResidentId(resident2.getId());
                    sourceBed.setResidentName(resident2.getName());
                    sourceBed.setStatus("OCCUPIED");
                    bedMapper.updateById(sourceBed);
                }

                // 更新床位2信息
                targetBed.setResidentId(resident1.getId());
                targetBed.setResidentName(resident1.getName());
                targetBed.setStatus("OCCUPIED");
                bedMapper.updateById(targetBed);

                return Result.success("床位互换成功");
            } else {
                // 目标床位空闲，直接调换
                // 更新老人到新床位
                resident1.setRoomBedNo(roomBedNo2);
                resident1.setCareZone(targetBed.getCareZone());
                residentMapper.updateById(resident1);

                // 释放原床位
                if (sourceBed != null) {
                    sourceBed.setResidentId(null);
                    sourceBed.setResidentName(null);
                    sourceBed.setStatus("AVAILABLE");
                    bedMapper.updateById(sourceBed);
                }

                // 占用新床位
                targetBed.setResidentId(resident1.getId());
                targetBed.setResidentName(resident1.getName());
                targetBed.setStatus("OCCUPIED");
                bedMapper.updateById(targetBed);

                return Result.success("床位调换成功");
            }

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return Result.error("床位调换失败: " + e.getMessage());
        }
    }

    /**
     * 查询可用床位列表
     */
    @GetMapping("/available")
    public Result<?> getAvailableBeds(@RequestParam(required = false) String careZone,
                                      @RequestParam(required = false) String bedType) {
        LambdaQueryWrapper<Bed> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(careZone)) {
            wrapper.eq(Bed::getCareZone, careZone);
        }
        if (StringUtils.hasText(bedType)) {
            wrapper.eq(Bed::getRoomType, bedType);
        }
        
        wrapper.eq(Bed::getStatus, "AVAILABLE");
        wrapper.orderByAsc(Bed::getRoomBedNo);

        List<Bed> beds = bedMapper.selectList(wrapper);
        return Result.success(beds);
    }

    /**
     * 查询床位列表（分页）
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer current,
                          @RequestParam(defaultValue = "20") Integer size,
                          @RequestParam(required = false) String careZone,
                          @RequestParam(required = false) String bedNo,
                          @RequestParam(required = false) String status) {
        Page<Bed> page = new Page<>(current, size);
        LambdaQueryWrapper<Bed> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(careZone)) {
            wrapper.eq(Bed::getCareZone, careZone);
        }
        if (StringUtils.hasText(bedNo)) {
            wrapper.like(Bed::getRoomBedNo, bedNo);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Bed::getStatus, status);
        }
        
        wrapper.orderByAsc(Bed::getCareZone, Bed::getRoomBedNo);
        
        Page<Bed> result = bedMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取床位详情
     */
    @GetMapping("/{id}")
    public Result<?> getBedDetail(@PathVariable Long id) {
        Bed bed = bedMapper.selectById(id);
        if (bed == null) {
            return Result.error("床位不存在");
        }
        return Result.success(bed);
    }

    /**
     * 获取床位统计信息
     */
    @GetMapping("/statistics")
    public Result<?> getStatistics(@RequestParam(required = false) String careZone) {
        LambdaQueryWrapper<Bed> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(careZone)) {
            wrapper.eq(Bed::getCareZone, careZone);
        }
        
        // 查询所有床位
        List<Bed> allBeds = bedMapper.selectList(wrapper);
        
        // 统计各状态数量
        Map<String, Object> statistics = new HashMap<>();
        long total = allBeds.size();
        long available = allBeds.stream().filter(b -> "AVAILABLE".equals(b.getStatus())).count();
        long occupied = allBeds.stream().filter(b -> "OCCUPIED".equals(b.getStatus())).count();
        long maintenance = allBeds.stream().filter(b -> "MAINTENANCE".equals(b.getStatus())).count();
        
        statistics.put("total", total);
        statistics.put("available", available);
        statistics.put("occupied", occupied);
        statistics.put("maintenance", maintenance);
        
        return Result.success(statistics);
    }

    /**
     * 分配床位给老人
     */
    @PostMapping("/assign")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> assignBed(@RequestBody Map<String, Object> request) {
        try {
            Long bedId = Long.valueOf(request.get("bedId").toString());
            Object residentNameValue = request.get("residentName");
            Object residentIdValue = request.get("residentId");
            String residentName = residentNameValue == null ? null : residentNameValue.toString();
            Long residentId = residentIdValue == null ? null : Long.valueOf(residentIdValue.toString());
            
            // 查询床位
            Bed bed = bedMapper.selectById(bedId);
            if (bed == null) {
                return Result.error("床位不存在");
            }
            
            if (!"AVAILABLE".equals(bed.getStatus())) {
                return Result.error("床位不可用");
            }
            
            // 更新老人信息
            if (residentId == null) return Result.error("老人ID不能为空");
            Resident resident = residentMapper.selectById(residentId);
            if (resident == null) return Result.error("老人不存在");
            if (!"RESIDENT".equals(resident.getStatus())) return Result.error("只能为在住老人分配床位");
            if (residentName == null || residentName.trim().isEmpty()) residentName = resident.getName();

            LambdaQueryWrapper<Bed> oldBedWrapper = new LambdaQueryWrapper<>();
            oldBedWrapper.eq(Bed::getResidentId, residentId);
            Bed oldBed = bedMapper.selectOne(oldBedWrapper);
            if (oldBed != null && !oldBed.getId().equals(bedId)) {
                oldBed.setResidentId(null);
                oldBed.setResidentName(null);
                oldBed.setStatus("AVAILABLE");
                bedMapper.updateById(oldBed);
            }

            resident.setRoomBedNo(bed.getRoomBedNo());
            resident.setCareZone(bed.getCareZone());
            residentMapper.updateById(resident);
            
            // 更新床位状态
            bed.setResidentId(residentId);
            bed.setResidentName(residentName);
            bed.setStatus("OCCUPIED");
            bedMapper.updateById(bed);
            
            return Result.success("床位分配成功");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return Result.error("床位分配失败: " + e.getMessage());
        }
    }

    /**
     * 老人离院，释放床位
     */
    @PostMapping("/{id}/discharge")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> dischargeBed(@PathVariable Long id) {
        try {
            // 查询床位
            Bed bed = bedMapper.selectById(id);
            if (bed == null) {
                return Result.error("床位不存在");
            }
            
            if (!"OCCUPIED".equals(bed.getStatus())) {
                return Result.error("床位未被占用");
            }
            
            // 如果有关联老人，更新老人信息
            if (bed.getResidentId() != null) {
                Resident resident = residentMapper.selectById(bed.getResidentId());
                if (resident != null) {
                    // 清空床位信息
                    resident.setRoomBedNo(null);
                    // 设置老人状态为离院
                    resident.setStatus("LEAVE");
                    // 需求12: 从待评估列表移除
                    resident.setAssessmentStatus("CANCELLED");
                    resident.setLeaveTime(new java.util.Date());
                    residentMapper.updateById(resident);
                }
            }
            
            // 释放床位
            bed.setResidentId(null);
            bed.setResidentName(null);
            bed.setStatus("AVAILABLE");
            bedMapper.updateById(bed);
            
            return Result.success("离院办理成功，床位已释放，老人已从待评估列表移除");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return Result.error("离院办理失败: " + e.getMessage());
        }
    }

    /**
     * 设置床位维护状态
     */
    @PostMapping("/{id}/maintenance")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> setBedMaintenance(@PathVariable Long id, 
                                       @RequestBody Map<String, Object> request) {
        try {
            Boolean maintenance = Boolean.valueOf(request.get("maintenance").toString());
            
            // 查询床位
            Bed bed = bedMapper.selectById(id);
            if (bed == null) {
                return Result.error("床位不存在");
            }
            
            if (maintenance) {
                // 设为维护状态
                if ("OCCUPIED".equals(bed.getStatus())) {
                    return Result.error("床位已被占用，无法设为维护状态");
                }
                bed.setStatus("MAINTENANCE");
                bedMapper.updateById(bed);
                return Result.success("床位已设为维护状态");
            } else {
                // 结束维护
                if (!"MAINTENANCE".equals(bed.getStatus())) {
                    return Result.error("床位不在维护状态");
                }
                bed.setStatus("AVAILABLE");
                bedMapper.updateById(bed);
                return Result.success("床位维护已结束，恢复可用状态");
            }
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return Result.error("维护状态切换失败: " + e.getMessage());
        }
    }

    /**
     * 同步床位和老人数据
     * 修复数据不一致问题
     */
    @PostMapping("/sync")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> syncBedData() {
        try {
            // 获取所有床位
            List<Bed> allBeds = bedMapper.selectList(null);
            int syncCount = 0;
            
            for (Bed bed : allBeds) {
                boolean needUpdate = false;
                
                // 如果床位标记为已占用但没有老人ID，设为空闲
                if ("OCCUPIED".equals(bed.getStatus()) && bed.getResidentId() == null) {
                    bed.setStatus("AVAILABLE");
                    bed.setResidentName(null);
                    needUpdate = true;
                }
                
                // 如果床位有老人ID，检查老人是否存在
                if (bed.getResidentId() != null) {
                    Resident resident = residentMapper.selectById(bed.getResidentId());
                    if (resident == null) {
                        // 老人不存在，释放床位
                        bed.setResidentId(null);
                        bed.setResidentName(null);
                        bed.setStatus("AVAILABLE");
                        needUpdate = true;
                    } else {
                        // 老人存在，同步床位信息
                        if (!bed.getRoomBedNo().equals(resident.getRoomBedNo())) {
                            resident.setRoomBedNo(bed.getRoomBedNo());
                            resident.setCareZone(bed.getCareZone());
                            residentMapper.updateById(resident);
                        }
                        // 确保床位状态为已占用
                        if (!"OCCUPIED".equals(bed.getStatus())) {
                            bed.setStatus("OCCUPIED");
                            needUpdate = true;
                        }
                        // 同步老人姓名
                        if (!resident.getName().equals(bed.getResidentName())) {
                            bed.setResidentName(resident.getName());
                            needUpdate = true;
                        }
                    }
                }
                
                if (needUpdate) {
                    bedMapper.updateById(bed);
                    syncCount++;
                }
            }
            
            return Result.success("数据同步完成，共修复 " + syncCount + " 条记录");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return Result.error("数据同步失败: " + e.getMessage());
        }
    }

    private Object requiredValue(Map<String, Object> request, String primaryKey) {
        Object value = request.get(primaryKey);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("缺少参数: " + primaryKey);
        }
        return value;
    }

    private String validateBed(Bed bed) {
        if (!StringUtils.hasText(bed.getRoomBedNo())) return "房间床位号不能为空";
        if (!StringUtils.hasText(bed.getCareZone())) return "护理区不能为空";
        if (bed.getFloor() == null || bed.getFloor() < 1) return "楼层不能为空";
        if (!StringUtils.hasText(bed.getRoomNo())) return "房间号不能为空";
        if (!StringUtils.hasText(bed.getRoomType())) return "房型不能为空";
        return null;
    }

    private Bed findByNumber(String roomBedNo, Long excludeId) {
        LambdaQueryWrapper<Bed> wrapper = new LambdaQueryWrapper<Bed>()
            .eq(Bed::getRoomBedNo, roomBedNo)
            .ne(excludeId != null, Bed::getId, excludeId);
        return bedMapper.selectOne(wrapper);
    }
}
