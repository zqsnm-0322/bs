-- 修正养老演示数据中姓名称谓与性别明显不一致的记录。
UPDATE biz_resident
SET gender = '女'
WHERE resident_id = 'YZ20260506'
  AND name = '孙大妈'
  AND gender <> '女';
