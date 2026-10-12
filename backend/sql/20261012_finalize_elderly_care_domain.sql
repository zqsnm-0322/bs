-- 养老系统最终领域收尾：合并用户护理区字段并移除医疗系统兼容列。
-- 本脚本仅在新的 elderly_care 数据库执行；原 medical_risk 数据库保留作回退备份。

SET @schema_name = DATABASE();

SET @add_care_zone = IF(
  EXISTS(
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'care_zone'
  ),
  'SELECT 1',
  'ALTER TABLE sys_user ADD COLUMN care_zone VARCHAR(50) NULL COMMENT ''负责护理区'' AFTER role'
);
PREPARE stmt FROM @add_care_zone;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @copy_zone = CONCAT(
  'UPDATE sys_user SET care_zone = COALESCE(NULLIF(care_zone, ''''), ',
  IF(EXISTS(
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'work_zone'
  ), 'NULLIF(work_zone, ''''), ', ''),
  IF(EXISTS(
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'department'
  ), 'NULLIF(department, '''')', 'NULL'),
  ')'
);
PREPARE stmt FROM @copy_zone;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_work_zone = IF(
  EXISTS(
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'work_zone'
  ),
  'ALTER TABLE sys_user DROP COLUMN work_zone',
  'SELECT 1'
);
PREPARE stmt FROM @drop_work_zone;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_department = IF(
  EXISTS(
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'department'
  ),
  'ALTER TABLE sys_user DROP COLUMN department',
  'SELECT 1'
);
PREPARE stmt FROM @drop_department;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE sys_user
  MODIFY COLUMN role VARCHAR(20) NOT NULL COMMENT '角色(ADMIN/DIRECTOR/CARE_MANAGER/CAREGIVER/RESIDENT)',
  COMMENT = '养老服务平台用户表';
