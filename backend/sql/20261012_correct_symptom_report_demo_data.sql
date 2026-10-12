-- 清理医疗系统遗留的症状测试记录。
-- 可重复执行，按测试姓名、旧医疗科室及无意义描述识别。

DELETE FROM symptom_reports
WHERE resident_name IN ('测试患者', '测试病人', '123', '1234')
   OR care_zone IN ('骨科', '内科', '外科')
   OR description IN ('11', '111', '1111', '222111');
