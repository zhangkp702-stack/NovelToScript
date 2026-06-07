CREATE TABLE IF NOT EXISTS t_script_message (
    id VARCHAR(64) NOT NULL COMMENT '消息ID',
    work_id VARCHAR(64) NOT NULL COMMENT '作品ID',
    chapter_number INT NOT NULL COMMENT '章节编号',
    role VARCHAR(16) NOT NULL COMMENT '消息角色：system/user/assistant',
    content TEXT NOT NULL COMMENT '消息内容',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'LLM链路ID',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    KEY idx_t_script_message_work_chapter (work_id, chapter_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '剧本改编对话消息';
