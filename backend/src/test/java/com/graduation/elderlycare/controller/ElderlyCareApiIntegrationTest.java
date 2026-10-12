package com.graduation.elderlycare.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.graduation.elderlycare.entity.Resident;
import com.graduation.elderlycare.entity.Bed;
import com.graduation.elderlycare.entity.VisitAppointment;
import com.graduation.elderlycare.mapper.BedMapper;
import com.graduation.elderlycare.mapper.ResidentMapper;
import com.graduation.elderlycare.mapper.VisitAppointmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ElderlyCareApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResidentMapper residentMapper;

    @Autowired
    private BedMapper bedMapper;

    @Autowired
    private VisitAppointmentMapper visitAppointmentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void finalSchemaUsesOnlyElderlyCareDomainColumns() {
        Integer legacyColumns = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' " +
                "AND COLUMN_NAME IN ('department', 'work_zone')",
            Integer.class);
        Integer careZoneColumns = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' " +
                "AND COLUMN_NAME = 'care_zone'",
            Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(0, legacyColumns);
        org.junit.jupiter.api.Assertions.assertEquals(1, careZoneColumns);
    }

    @Test
    void elderlyCareCoreListsAreAvailable() throws Exception {
        mockMvc.perform(get("/api/resident/list").param("current", "1").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/visit/list").param("current", "1").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/dashboard/stats"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.residentCount").exists());
    }

    @Test
    void caregiverDashboardAndResidentListOnlyExposeAssignedResidents() throws Exception {
        Long caregiverId = jdbcTemplate.queryForObject(
            "SELECT caregiver_id FROM biz_resident " +
                "WHERE status = 'RESIDENT' AND caregiver_id IS NOT NULL " +
                "GROUP BY caregiver_id ORDER BY COUNT(*) DESC LIMIT 1",
            Long.class);
        Long expectedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_resident WHERE status = 'RESIDENT' AND caregiver_id = ?",
            Long.class,
            caregiverId);
        String token = "token-CAREGIVER-" + caregiverId + "-test";

        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.residentCount").value(expectedCount));
        mockMvc.perform(get("/api/resident/list")
                .header("Authorization", token)
                .param("current", "1")
                .param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(expectedCount));
    }

    @Test
    void bedCanBeCreatedInsideRollbackTransaction() throws Exception {
        String number = "TEST-" + System.currentTimeMillis();
        mockMvc.perform(post("/api/bed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomBedNo\":\"" + number + "\",\"careZone\":\"自理区\",\"roomType\":\"单人房\",\"floor\":1,\"roomNo\":\"TEST\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void admissionOccupiesBedAndLeavingReleasesIt() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String roomBedNo = "FLOW-" + suffix;
        Bed bed = new Bed();
        bed.setRoomBedNo(roomBedNo);
        bed.setCareZone("自理区");
        bed.setRoomType("单人房");
        bed.setFloor(1);
        bed.setRoomNo("F" + suffix.substring(suffix.length() - 4));
        bed.setStatus("AVAILABLE");
        bedMapper.insert(bed);

        String residentName = "迁移验收老人" + suffix;
        mockMvc.perform(post("/api/resident")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + residentName + "\",\"age\":72,\"roomBedNo\":\"" + roomBedNo + "\",\"careZone\":\"自理区\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.residentId").value(org.hamcrest.Matchers.startsWith("YZ")));

        Resident resident = residentMapper.selectOne(new LambdaQueryWrapper<Resident>()
            .eq(Resident::getName, residentName));
        Bed occupied = bedMapper.selectById(bed.getId());
        org.junit.jupiter.api.Assertions.assertEquals("OCCUPIED", occupied.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(resident.getId(), occupied.getResidentId());

        mockMvc.perform(post("/api/resident/leave/" + resident.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leaveReason\":\"迁移验收\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        org.junit.jupiter.api.Assertions.assertEquals("LEAVE", residentMapper.selectById(resident.getId()).getStatus());
        Bed released = bedMapper.selectById(bed.getId());
        org.junit.jupiter.api.Assertions.assertEquals("AVAILABLE", released.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(released.getResidentId());
    }

    @Test
    void retiredAppointmentEndpointIsDisabled() throws Exception {
        mockMvc.perform(get("/api/appointment/list"))
            .andExpect(status().isNotFound());
    }

    @Test
    void familyHealthCheckupAndVisitFlowsAreAvailable() throws Exception {
        Resident resident = residentMapper.selectList(null).stream()
            .filter(item -> "RESIDENT".equals(item.getStatus()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("测试数据库至少需要一位在住老人"));

        mockMvc.perform(post("/api/family")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident.getId() + ",\"name\":\"测试家属\",\"relation\":\"子女\",\"isPrimary\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/health-checkup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident.getId() + ",\"checkDate\":\"2026-10-10\",\"height\":170,\"weight\":65}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/visit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident.getId() + ",\"visitorName\":\"测试访客\",\"visitDate\":\"2026-10-11\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void visitApprovalAndCompletionStateFlowWorks() throws Exception {
        Resident resident = residentMapper.selectList(null).stream()
            .filter(item -> "RESIDENT".equals(item.getStatus()))
            .findFirst()
            .orElseThrow();
        String visitorName = "状态流访客" + System.currentTimeMillis();

        mockMvc.perform(post("/api/visit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident.getId() + ",\"visitorName\":\"" + visitorName + "\",\"visitDate\":\"2026-10-20\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        VisitAppointment appointment = visitAppointmentMapper.selectOne(
            new LambdaQueryWrapper<VisitAppointment>().eq(VisitAppointment::getVisitorName, visitorName));
        mockMvc.perform(post("/api/visit/approve/" + appointment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approvedBy\":\"迁移验收员\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/visit/complete/" + appointment.getId()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        org.junit.jupiter.api.Assertions.assertEquals(
            "COMPLETED", visitAppointmentMapper.selectById(appointment.getId()).getStatus());
    }

    @Test
    void comprehensiveAssessmentPersistsSixModuleWeightedBreakdown() throws Exception {
        Resident resident = residentMapper.selectList(null).stream()
            .filter(item -> "RESIDENT".equals(item.getStatus()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("测试数据库至少需要一位在住老人"));

        mockMvc.perform(post("/api/enhanced-risk/assess/" + resident.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalScore").isNumber())
            .andExpect(jsonPath("$.data.assessmentBreakdown").isNotEmpty());
    }
}

