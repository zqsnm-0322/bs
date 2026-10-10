package com.graduation.medical.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.graduation.medical.entity.Resident;
import com.graduation.medical.mapper.ResidentMapper;

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
    void bedCanBeCreatedInsideRollbackTransaction() throws Exception {
        String number = "TEST-" + System.currentTimeMillis();
        mockMvc.perform(post("/api/bed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomBedNo\":\"" + number + "\",\"careZone\":\"自理区\",\"roomType\":\"单人房\",\"floor\":1,\"roomNo\":\"TEST\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void legacyMedicalEndpointsAreDisabledByDefault() throws Exception {
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
}
