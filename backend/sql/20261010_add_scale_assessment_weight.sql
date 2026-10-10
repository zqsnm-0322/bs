ALTER TABLE biz_scale_def
    ADD COLUMN assessment_weight DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '养老量表模块内部权重百分比'
    AFTER version;

UPDATE biz_scale_def SET assessment_weight = CASE scale_type
    WHEN 'FALL' THEN 25
    WHEN 'PRESSURE' THEN 20
    WHEN 'COGNITIVE' THEN 20
    WHEN 'MALNUTRITION' THEN 15
    WHEN 'DEPRESSION' THEN 10
    WHEN 'SELF_HARM' THEN 10
    ELSE 0
END;
