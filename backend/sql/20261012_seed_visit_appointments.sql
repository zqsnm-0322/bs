-- 探访预约管理演示数据。按“老人 + 探访人 + 日期 + 开始时间”去重，可重复执行。
INSERT INTO biz_visit_appointment (
  resident_id, resident_name, visitor_name, visitor_phone, visitor_relation,
  visit_date, visit_start_time, visit_end_time, visitor_count, special_notes,
  status, approved_by, approve_time, reject_reason, create_time
)
SELECT
  seed.resident_id, resident.name, seed.visitor_name, seed.visitor_phone, seed.visitor_relation,
  seed.visit_date, seed.visit_start_time, seed.visit_end_time, seed.visitor_count, seed.special_notes,
  seed.status, seed.approved_by, seed.approve_time, seed.reject_reason, seed.create_time
FROM (
  SELECT 2 resident_id, '张晓梅' visitor_name, '13800138021' visitor_phone, '女儿' visitor_relation, '2026-10-05' visit_date, '09:00:00' visit_start_time, '10:00:00' visit_end_time, 2 visitor_count, '携带换季衣物' special_notes, 'COMPLETED' status, '李院长' approved_by, '2026-10-03 10:20:00' approve_time, NULL reject_reason, '2026-10-02 09:15:00' create_time
  UNION ALL SELECT 3, '李建国', '13900139032', '儿子', '2026-10-06', '14:00:00', '15:30:00', 3, '老人行动不便，请协助引导', 'COMPLETED', '张主管', '2026-10-04 11:05:00', NULL, '2026-10-03 16:30:00'
  UNION ALL SELECT 4, '赵欣怡', '13600136043', '女儿', '2026-10-07', '10:00:00', '11:00:00', 1, NULL, 'COMPLETED', '张主管', '2026-10-05 09:40:00', NULL, '2026-10-04 18:10:00'
  UNION ALL SELECT 5, '钱志强', '13700137054', '儿子', '2026-10-08', '15:00:00', '16:00:00', 2, '携带老人喜爱的书籍', 'COMPLETED', '李院长', '2026-10-06 14:25:00', NULL, '2026-10-05 20:05:00'
  UNION ALL SELECT 6, '孙丽', '13500135065', '姐姐', '2026-10-09', '09:30:00', '10:30:00', 1, NULL, 'COMPLETED', '张主管', '2026-10-07 08:50:00', NULL, '2026-10-06 13:45:00'
  UNION ALL SELECT 7, '周明', '13300133076', '儿子', '2026-10-10', '14:30:00', '15:30:00', 2, '希望在一楼会客区探访', 'COMPLETED', '李院长', '2026-10-08 10:15:00', NULL, '2026-10-07 17:20:00'
  UNION ALL SELECT 8, '吴佳佳', '13200132087', '女儿', '2026-10-11', '10:00:00', '11:30:00', 3, NULL, 'COMPLETED', '张主管', '2026-10-09 09:10:00', NULL, '2026-10-08 12:35:00'
  UNION ALL SELECT 9, '郑海峰', '13800138098', '儿子', '2026-10-13', '09:00:00', '10:00:00', 2, '带水果，已确认无饮食禁忌', 'APPROVED', '李院长', '2026-10-11 10:30:00', NULL, '2026-10-10 08:45:00'
  UNION ALL SELECT 10, '冯静', '13900139109', '女儿', '2026-10-13', '15:00:00', '16:30:00', 1, NULL, 'APPROVED', '张主管', '2026-10-11 14:00:00', NULL, '2026-10-10 11:20:00'
  UNION ALL SELECT 11, '陈伟', '13600136110', '儿子', '2026-10-14', '10:30:00', '11:30:00', 2, '需要轮椅通道', 'APPROVED', '张主管', '2026-10-12 08:30:00', NULL, '2026-10-11 19:05:00'
  UNION ALL SELECT 12, '褚芳', '13700137121', '女儿', '2026-10-15', '14:00:00', '15:00:00', 2, NULL, 'PENDING', NULL, NULL, NULL, '2026-10-12 08:10:00'
  UNION ALL SELECT 13, '卫国强', '13500135132', '儿子', '2026-10-16', '09:30:00', '10:30:00', 3, '其中一名儿童同行', 'PENDING', NULL, NULL, NULL, '2026-10-12 08:35:00'
  UNION ALL SELECT 14, '蒋雯', '13300133143', '女儿', '2026-10-17', '15:00:00', '16:00:00', 1, '希望与护理员沟通近期情况', 'PENDING', NULL, NULL, NULL, '2026-10-12 09:00:00'
  UNION ALL SELECT 15, '沈建军', '13200132154', '儿子', '2026-10-18', '10:00:00', '11:00:00', 2, NULL, 'PENDING', NULL, NULL, NULL, '2026-10-12 09:25:00'
  UNION ALL SELECT 3, '李晓彤', '13800138235', '孙女', '2026-10-12', '09:00:00', '10:00:00', 2, NULL, 'REJECTED', '李院长', '2026-10-11 16:20:00', '该时段与健康检查安排冲突，请重新预约', '2026-10-11 14:10:00'
  UNION ALL SELECT 7, '周海', '13900139767', '侄子', '2026-10-14', '13:00:00', '14:00:00', 4, NULL, 'REJECTED', '张主管', '2026-10-12 08:20:00', '同时到访人数超过当前接待限制', '2026-10-11 21:30:00'
  UNION ALL SELECT 5, '钱小雨', '13600136525', '孙女', '2026-10-12', '16:00:00', '17:00:00', 1, '临时行程变化', 'CANCELLED', NULL, NULL, NULL, '2026-10-10 18:40:00'
  UNION ALL SELECT 10, '冯凯', '13700137010', '儿子', '2026-10-19', '14:30:00', '15:30:00', 2, '家属主动取消', 'CANCELLED', '张主管', '2026-10-11 09:00:00', NULL, '2026-10-10 20:15:00'
) seed
JOIN biz_resident resident
  ON resident.id = seed.resident_id AND resident.status = 'RESIDENT'
WHERE NOT EXISTS (
  SELECT 1
  FROM biz_visit_appointment existing
  WHERE existing.resident_id = seed.resident_id
    AND existing.visitor_name = seed.visitor_name
    AND existing.visit_date = seed.visit_date
    AND existing.visit_start_time = seed.visit_start_time
);
