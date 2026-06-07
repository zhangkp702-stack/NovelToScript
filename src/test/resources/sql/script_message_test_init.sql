CREATE TABLE IF NOT EXISTS t_script_message (
    id VARCHAR(64) NOT NULL,
    work_id VARCHAR(64) NOT NULL,
    chapter_number INT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content CLOB NOT NULL,
    trace_id VARCHAR(64) DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_t_script_message_work_chapter (work_id, chapter_number)
);
