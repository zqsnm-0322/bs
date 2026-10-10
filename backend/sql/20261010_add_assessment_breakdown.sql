ALTER TABLE biz_risk_assessment
    ADD COLUMN assessment_breakdown LONGTEXT NULL COMMENT '六模块综合评估得分、权重和数据依据JSON'
    AFTER health_predictions;
