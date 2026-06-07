CREATE TABLE IF NOT EXISTS t_script_record (
    id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    work_title VARCHAR(256) NOT NULL DEFAULT '',
    chapter_number INT NOT NULL,
    chapter_content CLOB NOT NULL,
    chapter_content_hash VARCHAR(64) DEFAULT NULL,
    script_content CLOB NOT NULL,
    model_name VARCHAR(128) DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_t_script_record_user_work_chapter UNIQUE (user_id, work_title, chapter_number)
);
