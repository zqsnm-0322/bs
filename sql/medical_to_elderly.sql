-- =====================================================
-- 医疗系统 -> 颐康园管理系统 数据库改造脚本
-- 执行前请备份数据库！
-- =====================================================

-- ==================== 1. 改造 biz_patient 表 ====================
ALTER TABLE biz_patient
  CHANGE COLUMN hospital_no resident_id VARCHAR(50) COMMENT '入住编号',
  CHANGE COLUMN bed_no room_bed_no VARCHAR(20) COMMENT '房间床位号',
  CHANGE COLUMN diagnosis health_status VARCHAR(200) COMMENT '健康状况描述',
  CHANGE COLUMN doctor_name care_manager VARCHAR(50) COMMENT '照护主管',
  CHANGE COLUMN department care_zone VARCHAR(50) COMMENT '护理区(自理区/半护区/全护区/失智区)',
  CHANGE COLUMN admission_time admit_time DATETIME COMMENT '入住时间',
  CHANGE COLUMN discharge_time leave_time DATETIME COMMENT '离院时间',
  CHANGE COLUMN discharge_reason leave_reason VARCHAR(200) COMMENT '离院原因',
  CHANGE COLUMN emergency_contact emergency_contact_phone VARCHAR(20) COMMENT '紧急联系人电话',
  ADD COLUMN id_type VARCHAR(20) DEFAULT 'IDCARD' COMMENT '证件类型',
  ADD COLUMN id_number VARCHAR(50) COMMENT '证件号码',
  ADD COLUMN origin_address VARCHAR(200) COMMENT '籍贯/住址',
  ADD COLUMN admission_type VARCHAR(20) DEFAULT '长期' COMMENT '入住类型(长期/短期/托养)',
  ADD COLUMN health_level VARCHAR(20) DEFAULT '自理' COMMENT '自理能力等级(自理/半护/全护/失智)',
  ADD COLUMN allergy_history TEXT COMMENT '过敏史',
  ADD COLUMN chronic_diseases TEXT COMMENT '慢性疾病',
  ADD COLUMN diet_restriction VARCHAR(200) COMMENT '饮食禁忌',
  ADD COLUMN emergency_contact_name VARCHAR(50) COMMENT '紧急联系人姓名',
  ADD COLUMN emergency_contact_relation VARCHAR(50) COMMENT '紧急联系人关系',
  ADD COLUMN insurance_type VARCHAR(50) COMMENT '保险类型',
  ADD COLUMN fee_standard DECIMAL(10,2) COMMENT '收费标准(元/月)',
  ADD COLUMN contract_start_date DATE COMMENT '合同开始日期',
  ADD COLUMN contract_end_date DATE COMMENT '合同结束日期',
  MODIFY COLUMN status VARCHAR(20) DEFAULT 'RESIDENT' COMMENT '状态(RESIDENT-在住/LEAVE-离院/BLOCKED-停用)',
  COMMENT='入住老人信息表';

-- 存量数据迁移：status 数字转字符串
UPDATE biz_patient SET status = 'RESIDENT' WHERE status = '1';
UPDATE biz_patient SET status = 'LEAVE' WHERE status = '0';

-- 预警表保留了旧字段名，但展示内容必须使用老人当前护理区和房间床位。
UPDATE biz_alert_msg alert_msg
JOIN biz_patient resident ON resident.id = alert_msg.patient_id
SET alert_msg.department = resident.care_zone,
    alert_msg.bed_no = resident.room_bed_no,
    alert_msg.patient_name = resident.name;

-- ==================== 2. 改造 biz_bed 表 ====================
ALTER TABLE biz_bed
  CHANGE COLUMN bed_no room_bed_no VARCHAR(20) COMMENT '房间床位号',
  CHANGE COLUMN department care_zone VARCHAR(50) COMMENT '所属护理区',
  CHANGE COLUMN bed_type room_type VARCHAR(20) DEFAULT '单人房' COMMENT '房间类型(单人房/双人房/多人房)',
  MODIFY COLUMN status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态(AVAILABLE-空闲/OCCUPIED-已入住/MAINTENANCE-维护中)',
  ADD COLUMN floor INT COMMENT '楼层',
  ADD COLUMN room_no VARCHAR(20) COMMENT '房间号',
  COMMENT='居住床位表';

-- 兼容旧字段名
ALTER TABLE biz_bed CHANGE COLUMN patient_id resident_id BIGINT NULL COMMENT '当前入住老人ID';
ALTER TABLE biz_bed CHANGE COLUMN patient_name resident_name VARCHAR(50) NULL COMMENT '当前入住老人姓名';

-- 补齐历史床位的楼层、房间号和中文房型。
-- 兼容 C101、G301、NF501 等旧床位编号。
UPDATE biz_bed
SET room_no = REGEXP_REPLACE(room_bed_no, '^[A-Za-z]+', ''),
    floor = CAST(SUBSTRING(REGEXP_REPLACE(room_bed_no, '^[A-Za-z]+', ''), 1, 1) AS UNSIGNED)
WHERE (floor IS NULL OR room_no IS NULL OR room_no = '')
  AND room_bed_no REGEXP '^[A-Za-z]+[0-9]{3,}$';

UPDATE biz_bed
SET room_type = CASE room_type
    WHEN 'NORMAL' THEN '双人房'
    WHEN 'VIP' THEN '单人房'
    WHEN 'ICU' THEN '多人房'
    ELSE room_type
END;

-- 为仅填写楼层和房间号的历史记录生成唯一床位号。
CREATE TEMPORARY TABLE bed_number_fix AS
SELECT id,
       ROW_NUMBER() OVER (PARTITION BY care_zone, floor, room_no ORDER BY id) AS seq
FROM biz_bed
WHERE room_bed_no IS NULL OR room_bed_no = '';

UPDATE biz_bed b
JOIN bed_number_fix f ON f.id = b.id
SET b.room_bed_no = CONCAT(
    CASE b.care_zone
        WHEN '自理区' THEN 'Z'
        WHEN '半护区' THEN 'B'
        WHEN '全护区' THEN 'Q'
        WHEN '失智区' THEN 'S'
        ELSE 'Y'
    END,
    b.room_no,
    '-',
    f.seq
);

DROP TEMPORARY TABLE bed_number_fix;


-- ==================== 3. 新增 家属信息表 ====================
CREATE TABLE IF NOT EXISTS biz_family_member (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resident_id BIGINT NOT NULL COMMENT '老人ID',
  name VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  relation VARCHAR(50) COMMENT '与老人关系(子女/配偶/其他)',
  phone VARCHAR(20) COMMENT '联系电话',
  email VARCHAR(100) COMMENT '邮箱',
  address VARCHAR(200) COMMENT '住址',
  is_primary TINYINT DEFAULT 0 COMMENT '是否主要联系人',
  visit_frequency VARCHAR(50) COMMENT '探访频率偏好',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_resident_id (resident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家属信息表';

-- ==================== 4. 新增 健康检查记录表 ====================
CREATE TABLE IF NOT EXISTS biz_health_checkup (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resident_id BIGINT NOT NULL COMMENT '老人ID',
  resident_name VARCHAR(50) COMMENT '老人姓名',
  check_date DATE NOT NULL COMMENT '检查日期',
  blood_pressure_systolic INT COMMENT '收缩压',
  blood_pressure_diastolic INT COMMENT '舒张压',
  heart_rate INT COMMENT '心率',
  temperature DECIMAL(4,1) COMMENT '体温',
  blood_sugar_fasting DECIMAL(5,2) COMMENT '空腹血糖',
  blood_sugar_postprandial DECIMAL(5,2) COMMENT '餐后血糖',
  weight DECIMAL(5,2) COMMENT '体重(kg)',
  height DECIMAL(5,2) COMMENT '身高(cm)',
  bmi DECIMAL(5,2) COMMENT 'BMI指数',
  blood_oxygen DECIMAL(5,2) COMMENT '血氧饱和度',
  check_items TEXT COMMENT '检查项目详情JSON',
  conclusion TEXT COMMENT '检查结论',
  suggestion TEXT COMMENT '护理建议',
  checked_by VARCHAR(50) COMMENT '检查人员',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_resident_id (resident_id),
  INDEX idx_check_date (check_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康检查记录表';

-- ==================== 5. 新增 探访预约表 ====================
CREATE TABLE IF NOT EXISTS biz_visit_appointment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resident_id BIGINT NOT NULL COMMENT '老人ID',
  resident_name VARCHAR(50) COMMENT '老人姓名',
  visitor_name VARCHAR(50) NOT NULL COMMENT '探访人姓名',
  visitor_phone VARCHAR(20) COMMENT '探访人电话',
  visitor_relation VARCHAR(50) COMMENT '与老人关系',
  visit_date DATE NOT NULL COMMENT '探访日期',
  visit_start_time TIME COMMENT '探访开始时间',
  visit_end_time TIME COMMENT '探访结束时间',
  visitor_count INT DEFAULT 1 COMMENT '探访人数',
  special_notes VARCHAR(255) COMMENT '特殊备注',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING-待确认/APPROVED-已批准/REJECTED-已拒绝/CANCELLED-已取消/COMPLETED-已完成)',
  approved_by VARCHAR(50) COMMENT '审批人',
  approve_time DATETIME COMMENT '审批时间',
  reject_reason VARCHAR(255) COMMENT '拒绝原因',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_resident_id (resident_id),
  INDEX idx_visit_date (visit_date),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家属探访预约表';

-- ==================== 6. 改造 sys_user 表 ====================
ALTER TABLE sys_user
  MODIFY COLUMN role VARCHAR(20) COMMENT '角色(ADMIN-管理员/DIRECTOR-院长/CARE_MANAGER-照护主管/CAREGIVER-护理员/RESIDENT-老人)',
  ADD COLUMN employee_no VARCHAR(50) COMMENT '员工工号',
  ADD COLUMN hire_date DATE COMMENT '入职日期',
  ADD COLUMN work_zone VARCHAR(50) COMMENT '负责护理区',
  ADD COLUMN avatar_url VARCHAR(255) COMMENT '用户头像URL',
  COMMENT='系统用户表';

-- 统一存量账号角色和演示账号，停止对外暴露旧医疗角色。
UPDATE sys_user SET role = 'CARE_MANAGER' WHERE role = 'DOCTOR';
UPDATE sys_user SET role = 'CAREGIVER' WHERE role = 'NURSE';
-- 早期迁移曾把旧医生批量写成 DIRECTOR，通过旧用户名识别并纠正，真正的院长账号不受影响。
UPDATE sys_user SET role = 'CARE_MANAGER'
WHERE role = 'DIRECTOR' AND LOWER(username) LIKE '%doctor%';
UPDATE sys_user
SET real_name = REPLACE(REPLACE(real_name, '医生', '照护主管'), '护士', '护理员')
WHERE real_name LIKE '%医生%' OR real_name LIKE '%护士%';
UPDATE sys_user
SET work_zone = CASE
  WHEN work_zone = '失智区' THEN '失智照护区'
  WHEN work_zone IN ('自理区', '半护区', '全护区', '失智照护区') THEN work_zone
  ELSE '自理区'
END;
UPDATE sys_user SET department = work_zone WHERE role IN ('CARE_MANAGER', 'CAREGIVER');
UPDATE sys_user
SET username = CASE WHEN id = 2 THEN 'care_manager01' ELSE CONCAT('care_manager', LPAD(id, 3, '0')) END
WHERE role = 'CARE_MANAGER';
UPDATE sys_user
SET username = CASE WHEN id = 3 THEN 'caregiver001' ELSE CONCAT('caregiver', LPAD(id, 3, '0')) END
WHERE role = 'CAREGIVER';
UPDATE sys_user SET username = 'resident001' WHERE username = 'patient001';

-- 将老人和护理计划绑定到迁移后的护理员账号，确保护理员只能查看本人负责数据。
UPDATE biz_patient
SET nurse_name = REPLACE(nurse_name, '护士', '护理员')
WHERE nurse_name IS NOT NULL;
UPDATE biz_patient resident
SET nurse_id = (
  SELECT MIN(caregiver.id) FROM sys_user caregiver
  WHERE caregiver.role = 'CAREGIVER' AND caregiver.real_name = resident.nurse_name
)
WHERE resident.nurse_name IS NOT NULL;
UPDATE biz_care_plan
SET assigned_nurse = REPLACE(assigned_nurse, '护士', '护理员')
WHERE assigned_nurse IS NOT NULL;
-- 旧系统的科室与改造后的护理区字段一一对应，先回填以保证存量账号仍能看到负责区域的数据。
UPDATE sys_user SET work_zone = department
WHERE (work_zone IS NULL OR work_zone = '') AND department IS NOT NULL;

-- ==================== 7. 新增养老场景风险量表 ====================
-- 保留历史评估数据，但停止在新评估中使用急性医疗场景量表。
UPDATE biz_scale_def SET status = 0
WHERE scale_type IN ('VTE', 'STROKE', 'GCS', 'PAIN', 'CAPRINI');

INSERT INTO biz_scale_def
  (scale_type, scale_name, description, scoring_rules_json, risk_levels_json, status, version, creator_name)
SELECT 'COGNITIVE', '简易认知障碍筛查', '基于定向力、记忆力和计算能力的养老机构认知筛查',
       '{"levels":[{"level":"LOW","min":0,"max":2},{"level":"MEDIUM","min":3,"max":5},{"level":"HIGH","min":6,"max":9}]}',
       '[{"level":"LOW","label":"低风险","interventions":["保持日常认知活动","每半年复评"]},{"level":"MEDIUM","label":"中风险","interventions":["开展认知训练","增加巡视","建议专业评估"]},{"level":"HIGH","label":"高风险","interventions":["启动防走失措施","安排专人照护","尽快转诊评估"]}]',
       1, '1.0', '系统'
WHERE NOT EXISTS (SELECT 1 FROM biz_scale_def WHERE scale_type = 'COGNITIVE');

INSERT INTO biz_scale_def
  (scale_type, scale_name, description, scoring_rules_json, risk_levels_json, status, version, creator_name)
SELECT 'MALNUTRITION', 'MNA-SF营养不良风险筛查', '用于养老机构老人的营养不良风险快速筛查',
       '{"levels":[{"level":"LOW","min":0,"max":2},{"level":"MEDIUM","min":3,"max":5},{"level":"HIGH","min":6,"max":9}]}',
       '[{"level":"LOW","label":"营养正常","interventions":["维持均衡饮食","定期监测体重"]},{"level":"MEDIUM","label":"存在风险","interventions":["制定营养计划","每周监测体重","营养师评估"]},{"level":"HIGH","label":"营养不良","interventions":["立即营养干预","评估吞咽功能","必要时就医"]}]',
       1, '1.0', '系统'
WHERE NOT EXISTS (SELECT 1 FROM biz_scale_def WHERE scale_type = 'MALNUTRITION');

INSERT INTO biz_scale_def
  (scale_type, scale_name, description, scoring_rules_json, risk_levels_json, status, version, creator_name)
SELECT 'DELIRIUM', '3D-CAM谵妄风险筛查', '用于识别急性意识和注意力改变',
       '{"levels":[{"level":"LOW","min":0,"max":1},{"level":"MEDIUM","min":2,"max":3},{"level":"HIGH","min":4,"max":6}]}',
       '[{"level":"LOW","label":"低风险","interventions":["维持规律作息","继续观察"]},{"level":"MEDIUM","label":"疑似风险","interventions":["加强生命体征观察","排查诱因","通知照护主管"]},{"level":"HIGH","label":"高风险","interventions":["立即医学评估","预防跌倒和走失","安排持续陪护"]}]',
       1, '1.0', '系统'
WHERE NOT EXISTS (SELECT 1 FROM biz_scale_def WHERE scale_type = 'DELIRIUM');

INSERT INTO biz_scale_def
  (scale_type, scale_name, description, scoring_rules_json, risk_levels_json, status, version, creator_name)
SELECT 'ABUSE', '老年人虐待与忽视风险筛查', '筛查身体、心理、经济虐待及照护忽视风险',
       '{"levels":[{"level":"LOW","min":0,"max":0},{"level":"MEDIUM","min":1,"max":2},{"level":"HIGH","min":3,"max":6}]}',
       '[{"level":"LOW","label":"未见风险","interventions":["常规关注"]},{"level":"MEDIUM","label":"存在疑点","interventions":["单独访谈老人","记录异常迹象","上报管理人员"]},{"level":"HIGH","label":"高风险","interventions":["立即启动保护流程","保留记录和证据","联系主管部门"]}]',
       1, '1.0', '系统'
WHERE NOT EXISTS (SELECT 1 FROM biz_scale_def WHERE scale_type = 'ABUSE');

INSERT INTO biz_scale_def
  (scale_type, scale_name, description, scoring_rules_json, risk_levels_json, status, version, creator_name)
SELECT 'SELF_HARM', '自伤风险筛查', '用于识别老人近期自伤想法、计划和行为风险',
       '{"levels":[{"level":"LOW","min":0,"max":0},{"level":"MEDIUM","min":1,"max":2},{"level":"HIGH","min":3,"max":6}]}',
       '[{"level":"LOW","label":"低风险","interventions":["常规心理关怀"]},{"level":"MEDIUM","label":"中风险","interventions":["增加陪伴和巡视频率","通知家属和照护主管","安排心理评估"]},{"level":"HIGH","label":"高风险","interventions":["不得独处","移除危险物品","立即联系专业医疗机构"]}]',
       1, '1.0', '系统'
WHERE NOT EXISTS (SELECT 1 FROM biz_scale_def WHERE scale_type = 'SELF_HARM');

INSERT INTO biz_scale_def
  (scale_type, scale_name, description, scoring_rules_json, risk_levels_json, status, version, creator_name)
SELECT 'DEPRESSION', 'GDS老年抑郁风险筛查', '用于识别老人抑郁情绪和社会退缩风险',
       '{"levels":[{"level":"LOW","min":0,"max":2},{"level":"MEDIUM","min":3,"max":4},{"level":"HIGH","min":5,"max":6}]}',
       '[{"level":"LOW","label":"情绪正常","interventions":["鼓励社交活动","定期复评"]},{"level":"MEDIUM","label":"疑似抑郁","interventions":["增加心理支持","联系家属","建议专业评估"]},{"level":"HIGH","label":"高风险","interventions":["立即进行心理或精神科评估","评估自伤风险","制定重点照护计划"]}]',
       1, '1.0', '系统'
WHERE NOT EXISTS (SELECT 1 FROM biz_scale_def WHERE scale_type = 'DEPRESSION');

-- 每个新增量表配置三个可动态计分的问题；重复执行不会重复插入。
INSERT INTO biz_scale_question (scale_id, question_order, question_text, question_type, options_json, required, weight, group_name)
SELECT d.id, q.question_order, q.question_text, 'radio', q.options_json, 1, 1.00, q.group_name
FROM biz_scale_def d
JOIN (
  SELECT 'COGNITIVE' scale_type, 1 question_order, '当前时间和地点定向是否准确' question_text, '[{"value":"NORMAL","label":"准确","score":0},{"value":"PARTIAL","label":"部分错误","score":1},{"value":"IMPAIRED","label":"明显错误","score":3}]' options_json, '认知' group_name
  UNION ALL SELECT 'COGNITIVE', 2, '能否复述并延迟回忆三个词语', '[{"value":"NORMAL","label":"全部正确","score":0},{"value":"PARTIAL","label":"部分正确","score":1},{"value":"IMPAIRED","label":"无法完成","score":3}]', '认知'
  UNION ALL SELECT 'COGNITIVE', 3, '能否完成简单计算或执行指令', '[{"value":"NORMAL","label":"可以","score":0},{"value":"PARTIAL","label":"需要提示","score":1},{"value":"IMPAIRED","label":"无法完成","score":3}]', '认知'
  UNION ALL SELECT 'MALNUTRITION', 1, '近三个月进食量是否下降', '[{"value":"NO","label":"无下降","score":0},{"value":"MILD","label":"轻度下降","score":1},{"value":"SEVERE","label":"明显下降","score":3}]', '营养'
  UNION ALL SELECT 'MALNUTRITION', 2, '近三个月体重是否下降', '[{"value":"NO","label":"无下降","score":0},{"value":"MILD","label":"下降1-3kg","score":1},{"value":"SEVERE","label":"下降超过3kg","score":3}]', '营养'
  UNION ALL SELECT 'MALNUTRITION', 3, '活动能力是否受限', '[{"value":"NORMAL","label":"可独立外出","score":0},{"value":"LIMITED","label":"仅室内活动","score":1},{"value":"BEDFAST","label":"卧床或轮椅","score":3}]', '营养'
  UNION ALL SELECT 'DELIRIUM', 1, '近期精神状态是否出现急性改变', '[{"value":"NO","label":"否","score":0},{"value":"YES","label":"是","score":2}]', '意识'
  UNION ALL SELECT 'DELIRIUM', 2, '注意力是否明显不集中', '[{"value":"NO","label":"否","score":0},{"value":"YES","label":"是","score":2}]', '意识'
  UNION ALL SELECT 'DELIRIUM', 3, '思维是否紊乱或意识水平波动', '[{"value":"NO","label":"否","score":0},{"value":"YES","label":"是","score":2}]', '意识'
  UNION ALL SELECT 'ABUSE', 1, '是否发现不明原因伤痕或反复受伤', '[{"value":"NO","label":"否","score":0},{"value":"SUSPECTED","label":"疑似","score":1},{"value":"YES","label":"是","score":2}]', '安全'
  UNION ALL SELECT 'ABUSE', 2, '老人是否表达害怕特定照护者或家属', '[{"value":"NO","label":"否","score":0},{"value":"SUSPECTED","label":"疑似","score":1},{"value":"YES","label":"是","score":2}]', '安全'
  UNION ALL SELECT 'ABUSE', 3, '是否存在基本照护、用药或财务需求被忽视', '[{"value":"NO","label":"否","score":0},{"value":"SUSPECTED","label":"疑似","score":1},{"value":"YES","label":"是","score":2}]', '安全'
  UNION ALL SELECT 'SELF_HARM', 1, '近期是否表达不想活或伤害自己的想法', '[{"value":"NO","label":"否","score":0},{"value":"PASSIVE","label":"偶尔表达","score":1},{"value":"YES","label":"明确表达","score":2}]', '心理'
  UNION ALL SELECT 'SELF_HARM', 2, '是否形成具体自伤计划', '[{"value":"NO","label":"否","score":0},{"value":"UNCLEAR","label":"不明确","score":1},{"value":"YES","label":"是","score":2}]', '心理'
  UNION ALL SELECT 'SELF_HARM', 3, '近期是否有自伤行为或准备行为', '[{"value":"NO","label":"否","score":0},{"value":"SUSPECTED","label":"疑似","score":1},{"value":"YES","label":"是","score":2}]', '心理'
  UNION ALL SELECT 'DEPRESSION', 1, '近期是否经常感到情绪低落或生活空虚', '[{"value":"NO","label":"否","score":0},{"value":"SOMETIMES","label":"有时","score":1},{"value":"OFTEN","label":"经常","score":2}]', '情绪'
  UNION ALL SELECT 'DEPRESSION', 2, '是否对原本喜欢的活动失去兴趣', '[{"value":"NO","label":"否","score":0},{"value":"SOMETIMES","label":"部分","score":1},{"value":"YES","label":"是","score":2}]', '情绪'
  UNION ALL SELECT 'DEPRESSION', 3, '是否感到无助、无望或主动回避社交', '[{"value":"NO","label":"否","score":0},{"value":"SOMETIMES","label":"有时","score":1},{"value":"OFTEN","label":"经常","score":2}]', '情绪'
) q ON q.scale_type = d.scale_type
WHERE NOT EXISTS (
  SELECT 1 FROM biz_scale_question existing
  WHERE existing.scale_id = d.id AND existing.question_order = q.question_order
);

-- ==================== 8. 物理表与字段养老化 ====================
-- 本节完成后，后端不再依赖 patient / doctor / nurse 等旧医疗表名和列名。
-- 旧风险触发器仍引用 biz_patient/patient_id，且会与后端预警逻辑重复执行。
DROP TRIGGER IF EXISTS tr_risk_record_insert;

RENAME TABLE biz_patient TO biz_resident;
RENAME TABLE biz_patient_medical_history TO biz_resident_health_history;
RENAME TABLE doctor_notifications TO care_notifications;

ALTER TABLE biz_resident
  CHANGE COLUMN nurse_name caregiver_name VARCHAR(100) NULL COMMENT '责任护理员姓名',
  CHANGE COLUMN nurse_id caregiver_id BIGINT NULL COMMENT '责任护理员ID';

ALTER TABLE biz_resident_health_history
  CHANGE COLUMN patient_id resident_id BIGINT NOT NULL COMMENT '老人ID',
  CHANGE COLUMN disease_name condition_name VARCHAR(255) NULL COMMENT '健康状况名称',
  CHANGE COLUMN disease_code condition_code VARCHAR(20) NULL COMMENT '健康状况编码',
  CHANGE COLUMN diagnosis_date record_date DATETIME NULL COMMENT '记录日期';

ALTER TABLE care_notifications
  CHANGE COLUMN doctor_id care_manager_id VARCHAR(64) NULL COMMENT '照护主管ID',
  CHANGE COLUMN patient_id resident_id BIGINT NULL COMMENT '老人ID',
  CHANGE COLUMN patient_name resident_name VARCHAR(100) NULL COMMENT '老人姓名',
  CHANGE COLUMN department care_zone VARCHAR(100) NULL COMMENT '护理区';

ALTER TABLE biz_care_plan
  CHANGE COLUMN patient_id resident_id BIGINT NOT NULL COMMENT '老人ID',
  CHANGE COLUMN patient_name resident_name VARCHAR(100) NULL COMMENT '老人姓名',
  CHANGE COLUMN bed_no room_bed_no VARCHAR(50) NULL COMMENT '房间床位号',
  CHANGE COLUMN department care_zone VARCHAR(100) NULL COMMENT '护理区',
  CHANGE COLUMN assigned_nurse_id assigned_caregiver_id BIGINT NULL COMMENT '护理员ID',
  CHANGE COLUMN assigned_nurse assigned_caregiver VARCHAR(100) NULL COMMENT '护理员姓名';

-- 护理计划使用老人当前的床位和护理区，清除历史医疗科室快照。
UPDATE biz_care_plan cp
JOIN biz_resident r ON r.id = cp.resident_id
SET cp.room_bed_no = r.room_bed_no,
    cp.care_zone = r.care_zone,
    cp.resident_name = r.name;

UPDATE biz_care_plan
SET plan_title = CASE
        WHEN plan_title = '术后康复护理计划' THEN '肢体康复护理计划'
        WHEN plan_title = '血栓预防护理计划' THEN '压疮预防护理计划'
        ELSE plan_title
    END,
    plan_type = CASE
        WHEN plan_type IN ('FALL_PREVENTION', 'VTE_PREVENTION') THEN 'SAFETY'
        ELSE plan_type
    END,
    plan_type_label = CASE
        WHEN plan_type IN ('FALL_PREVENTION', 'VTE_PREVENTION') THEN '安全防护'
        WHEN plan_type = 'REHABILITATION' THEN '康复训练'
        ELSE plan_type_label
    END;

-- 清理旧医院演示数据中的未成年、青年年龄，使养老演示数据保持在合理范围。
UPDATE biz_resident
SET age = 60 + MOD(id * 7, 31)
WHERE age IS NULL OR age < 60 OR age > 120;

ALTER TABLE biz_medication
  CHANGE COLUMN patient_id resident_id BIGINT NOT NULL COMMENT '老人ID',
  CHANGE COLUMN patient_name resident_name VARCHAR(100) NULL COMMENT '老人姓名';

ALTER TABLE biz_risk_record
  CHANGE COLUMN patient_id resident_id BIGINT NOT NULL COMMENT '老人ID',
  CHANGE COLUMN sent_to_patient sent_to_resident TINYINT DEFAULT 0 COMMENT '是否发送给老人';

ALTER TABLE biz_risk_assessment
  CHANGE COLUMN patient_id resident_id BIGINT NOT NULL COMMENT '老人ID',
  CHANGE COLUMN medical_history_score health_history_score DECIMAL(10,2) NULL COMMENT '健康史评分',
  CHANGE COLUMN lab_results_score health_check_score DECIMAL(10,2) NULL COMMENT '健康检查评分',
  CHANGE COLUMN blood_routine_risk fall_risk DECIMAL(10,2) NULL COMMENT '跌倒风险',
  CHANGE COLUMN biochemistry_risk pressure_sore_risk DECIMAL(10,2) NULL COMMENT '压疮风险',
  CHANGE COLUMN inflammation_risk cognitive_risk DECIMAL(10,2) NULL COMMENT '认知风险',
  CHANGE COLUMN cardiac_risk wandering_risk DECIMAL(10,2) NULL COMMENT '走失风险',
  CHANGE COLUMN bp_variability_risk self_harm_risk DECIMAL(10,2) NULL COMMENT '自伤风险',
  CHANGE COLUMN hr_variability_risk neglect_risk DECIMAL(10,2) NULL COMMENT '照护忽视风险',
  CHANGE COLUMN sent_to_patient sent_to_resident TINYINT DEFAULT 0 COMMENT '是否发送给老人';
ALTER TABLE biz_risk_assessment
  CHANGE COLUMN nutrition_risk depression_risk DECIMAL(10,2) NULL COMMENT '抑郁风险';
ALTER TABLE biz_risk_assessment
  CHANGE COLUMN coagulation_risk nutrition_risk DECIMAL(10,2) NULL COMMENT '营养风险';

ALTER TABLE symptom_reports
  CHANGE COLUMN patient_id resident_id BIGINT NOT NULL COMMENT '老人ID',
  CHANGE COLUMN patient_name resident_name VARCHAR(100) NULL COMMENT '老人姓名',
  CHANGE COLUMN bed_no room_bed_no VARCHAR(50) NULL COMMENT '房间床位号',
  CHANGE COLUMN department care_zone VARCHAR(100) NULL COMMENT '护理区';

ALTER TABLE biz_vital_signs
  CHANGE COLUMN patient_id resident_id BIGINT NOT NULL COMMENT '老人ID',
  CHANGE COLUMN patient_name resident_name VARCHAR(100) NULL COMMENT '老人姓名',
  CHANGE COLUMN bed_no room_bed_no VARCHAR(50) NULL COMMENT '房间床位号',
  CHANGE COLUMN department care_zone VARCHAR(100) NULL COMMENT '护理区';

ALTER TABLE biz_alert_msg
  CHANGE COLUMN patient_id resident_id BIGINT NOT NULL COMMENT '老人ID',
  CHANGE COLUMN patient_name resident_name VARCHAR(100) NULL COMMENT '老人姓名',
  CHANGE COLUMN bed_no room_bed_no VARCHAR(50) NULL COMMENT '房间床位号',
  CHANGE COLUMN department care_zone VARCHAR(100) NULL COMMENT '护理区';

-- ==================== 9. 归档未被养老系统引用的旧医疗表 ====================
-- 保留数据但退出运行时业务，便于审计和需要时人工恢复。
RENAME TABLE biz_appointment TO legacy_appointment;
RENAME TABLE biz_edu_push TO legacy_education_push;
RENAME TABLE biz_intervention TO legacy_intervention;
RENAME TABLE biz_lab_results TO legacy_health_check_results;
RENAME TABLE biz_medication_backup TO legacy_medication_backup;
RENAME TABLE biz_medication_reminder TO legacy_medication_reminder;
RENAME TABLE biz_patient_assessment_status TO legacy_resident_assessment_status;
RENAME TABLE patient_assessment_status TO legacy_resident_assessment_status_v1;
RENAME TABLE biz_symptom_report TO legacy_health_report;
RENAME TABLE biz_task TO legacy_care_task;
