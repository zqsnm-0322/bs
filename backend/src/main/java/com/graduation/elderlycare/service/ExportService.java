package com.graduation.elderlycare.service;

import com.graduation.elderlycare.entity.RiskRecord;
import com.graduation.elderlycare.mapper.RiskRecordMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据导出服务
 * 支持Excel和PDF格式导出
 */
@Service
public class ExportService {
    
    @Autowired
    private RiskRecordMapper riskRecordMapper;
    
    /**
     * 导出风险评估记录到Excel
     */
    public void exportRiskRecordsToExcel(HttpServletResponse response, 
                                       String scaleType, 
                                       String startDate, 
                                       String endDate) throws IOException {
        
        // 1. 查询数据
        List<RiskRecord> records = riskRecordMapper.selectListWithResident();
        
        // 2. 创建工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("风险评估记录");
        
        // 3. 创建标题样式
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        // 4. 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"序号", "老人姓名", "入住编号", "房间床位", "量表类型", "总分", "风险等级", "评估人", "评估时间"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 5. 填充数据
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int rowNum = 1;
        
        for (RiskRecord record : records) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(record.getResidentName() != null ? record.getResidentName() : "");
            row.createCell(2).setCellValue("");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue(getScaleTypeName(record.getScaleType()));
            row.createCell(5).setCellValue(record.getTotalScore() != null ? record.getTotalScore() : 0);
            row.createCell(6).setCellValue(getRiskLevelName(record.getRiskLevel()));
            row.createCell(7).setCellValue(record.getEvaluatorName() != null ? record.getEvaluatorName() : "");
            row.createCell(8).setCellValue(record.getEvaluateTime() != null ? 
                record.getEvaluateTime().format(formatter) : "");
        }
        
        // 6. 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // 7. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=risk_records.xlsx");
        
        // 8. 写入响应流
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    /**
     * 导出老人列表到Excel
     */
    public void exportResidentsToExcel(HttpServletResponse response) throws IOException {
        // 老人基础信息导出模板
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("老人列表");
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"序号", "姓名", "入住编号", "性别", "年龄", "房间床位", "健康状况", "入住时间", "护理区", "状态"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=residents.xlsx");
        
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    /**
     * 获取量表类型中文名称
     */
    private String getScaleTypeName(String scaleType) {
        if (scaleType == null) return "";
        
        switch (scaleType.toUpperCase()) {
            case "FALL": return "跌倒风险";
            case "PRESSURE": return "压疮风险";
            case "MORSE": return "莫尔斯跌倒";
            case "BRADEN": return "布雷登压疮";
            case "COGNITIVE": return "认知障碍风险";
            case "WANDERING": return "走失风险";
            case "MALNUTRITION": return "营养不良风险";
            case "DELIRIUM": return "谵妄风险";
            case "ABUSE": return "虐待与忽视风险";
            case "NEGLECT": return "照护忽视风险";
            case "SELF_HARM": return "自伤风险";
            case "DEPRESSION": return "抑郁风险";
            default: return scaleType;
        }
    }
    
    /**
     * 获取风险等级中文名称
     */
    private String getRiskLevelName(String riskLevel) {
        if (riskLevel == null) return "";
        
        switch (riskLevel.toUpperCase()) {
            case "LOW": return "低危";
            case "MEDIUM": return "中危";
            case "HIGH": return "高危";
            default: return riskLevel;
        }
    }
}

