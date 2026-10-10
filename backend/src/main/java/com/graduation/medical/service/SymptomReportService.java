package com.graduation.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graduation.medical.entity.CareNotification;
import com.graduation.medical.entity.SymptomReport;
import com.graduation.medical.entity.User;
import com.graduation.medical.mapper.SymptomReportMapper;
import com.graduation.medical.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SymptomReportService {

    @Autowired
    private SymptomReportMapper symptomReportMapper;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 创建症状上报
     */
    @Transactional
    public SymptomReport createReport(SymptomReport report) {
        report.setStatus("PENDING");
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        
        symptomReportMapper.insert(report);
        
        // 查询老人所在护理区的护理员
        String careZone = report.getCareZone();
        if (careZone != null && !careZone.isEmpty()) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getRole, "CAREGIVER")
                      .eq(User::getDepartment, careZone);
            List<User> caregivers = userMapper.selectList(userWrapper);
            
            for (User caregiver : caregivers) {
                CareNotification notification = new CareNotification();
                notification.setCareManagerId(String.valueOf(caregiver.getId()));
                notification.setResidentId(report.getResidentId());
                notification.setResidentName(report.getResidentName());
                notification.setCareZone(careZone);
                notification.setNotificationType("SYMPTOM_REPORT");
                notification.setTitle("老人健康状况上报");
                notification.setContent(String.format("老人 %s（房间床位：%s）上报身体不适：%s", 
                    report.getResidentName(), report.getRoomBedNo(), 
                    report.getSymptomName() != null ? report.getSymptomName() : report.getSymptoms()));
                notification.setExtraData(String.valueOf(report.getId()));
                notification.setIsRead(false);
                notification.setCreateTime(new java.util.Date());
                
                notificationService.createNotification(notification);
            }
        }
        
        return report;
    }

    /**
     * 获取症状上报列表
     */
    public Page<SymptomReport> getReportList(Integer pageNum, Integer pageSize, String status, String careZone) {
        Page<SymptomReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SymptomReport> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SymptomReport::getStatus, status);
        }
        
        if (careZone != null && !careZone.isEmpty()) {
            wrapper.eq(SymptomReport::getCareZone, careZone);
        }
        
        wrapper.orderByDesc(SymptomReport::getCreateTime);
        
        return symptomReportMapper.selectPage(page, wrapper);
    }

    /**
     * 获取老人的健康状况上报历史
     */
    public List<SymptomReport> getResidentReports(Long residentId) {
        LambdaQueryWrapper<SymptomReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SymptomReport::getResidentId, residentId)
               .orderByDesc(SymptomReport::getCreateTime);
        
        return symptomReportMapper.selectList(wrapper);
    }

    /**
     * 处理症状上报
     */
    @Transactional
    public void handleReport(Long reportId, String handlerName, String handleNote) {
        SymptomReport report = symptomReportMapper.selectById(reportId);
        if (report != null) {
            report.setStatus("RESOLVED");
            report.setHandlerName(handlerName);
            report.setHandleNote(handleNote);
            report.setHandleTime(LocalDateTime.now());
            report.setUpdateTime(LocalDateTime.now());
            
            symptomReportMapper.updateById(report);
        }
    }

    /**
     * 更新症状上报状态
     */
    public void updateStatus(Long reportId, String status) {
        SymptomReport report = symptomReportMapper.selectById(reportId);
        if (report != null) {
            report.setStatus(status);
            report.setUpdateTime(LocalDateTime.now());
            symptomReportMapper.updateById(report);
        }
    }

    /**
     * 获取待处理数量
     */
    public Long getPendingCount(String careZone) {
        LambdaQueryWrapper<SymptomReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SymptomReport::getStatus, "PENDING");
        
        if (careZone != null && !careZone.isEmpty()) {
            wrapper.eq(SymptomReport::getCareZone, careZone);
        }
        
        return symptomReportMapper.selectCount(wrapper);
    }
}
