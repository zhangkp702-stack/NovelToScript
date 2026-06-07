CREATE TABLE IF NOT EXISTS t_script_work (
    id VARCHAR(64) NOT NULL COMMENT '作品ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户账号',
    title VARCHAR(256) NOT NULL DEFAULT '' COMMENT '作品标题',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    KEY idx_t_script_work_user_id (user_id),
    KEY idx_t_script_work_update_time (update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '剧本作品';

ALTER TABLE t_script_record
    ADD COLUMN work_id VARCHAR(64) DEFAULT NULL COMMENT '作品ID' AFTER user_id,
    ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL COMMENT 'LLM链路ID' AFTER model_name,
    ADD COLUMN generation_id VARCHAR(64) DEFAULT NULL COMMENT '单次生成任务ID' AFTER trace_id;

ALTER TABLE t_script_record
    ADD KEY idx_t_script_record_work_id (work_id);
