CREATE TABLE IF NOT EXISTS t_script_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户账号',
    work_title VARCHAR(256) NOT NULL DEFAULT '' COMMENT '作品标题',
    chapter_number INT NOT NULL COMMENT '章节编号',
    chapter_content TEXT NOT NULL COMMENT '原章节内容',
    chapter_content_hash VARCHAR(64) DEFAULT NULL COMMENT '原章节内容哈希',
    script_content TEXT NOT NULL COMMENT '生成剧本内容',
    model_name VARCHAR(128) DEFAULT NULL COMMENT '模型名称',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_t_script_record_user_work_chapter (user_id, work_title, chapter_number),
    KEY idx_t_script_record_user_id (user_id),
    KEY idx_t_script_record_work_title (work_title),
    KEY idx_t_script_record_update_time (update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '剧本生成记录';
