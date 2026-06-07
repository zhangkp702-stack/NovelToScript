-- 开发环境专用：删除剧本与 LLM Trace 表后按 Phase 1 最新结构重建。
-- 会清空：t_script_work、t_script_record、t_llm_trace_run、t_llm_trace_node

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS t_script_record;
DROP TABLE IF EXISTS t_character;
DROP TABLE IF EXISTS t_script_work;
DROP TABLE IF EXISTS t_llm_trace_node;
DROP TABLE IF EXISTS t_llm_trace_run;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS t_llm_trace_run (
    id BIGINT NOT NULL COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL COMMENT '全局链路ID',
    trace_name VARCHAR(128) NOT NULL COMMENT '链路名称',
    entry_method VARCHAR(256) NOT NULL COMMENT '入口方法',
    conversation_id VARCHAR(64) DEFAULT NULL COMMENT '会话ID',
    task_id VARCHAR(64) DEFAULT NULL COMMENT '任务ID',
    user_id VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
    status VARCHAR(16) NOT NULL COMMENT 'RUNNING/SUCCESS/ERROR',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    start_time DATETIME(3) NOT NULL COMMENT '开始时间',
    end_time DATETIME(3) DEFAULT NULL COMMENT '结束时间',
    duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    extra_data TEXT DEFAULT NULL COMMENT '扩展信息JSON',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_t_llm_trace_run_trace_id (trace_id),
    KEY idx_t_llm_trace_run_task_id (task_id),
    KEY idx_t_llm_trace_run_user_id (user_id),
    KEY idx_t_llm_trace_run_start_time (start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'LLM链路运行日志';

CREATE TABLE IF NOT EXISTS t_llm_trace_node (
    id BIGINT NOT NULL COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL COMMENT '全局链路ID',
    node_id VARCHAR(64) NOT NULL COMMENT '节点ID',
    parent_node_id VARCHAR(64) DEFAULT NULL COMMENT '父节点ID',
    depth INT NOT NULL DEFAULT 0 COMMENT '节点深度',
    node_type VARCHAR(32) NOT NULL COMMENT '节点类型',
    node_name VARCHAR(128) NOT NULL COMMENT '节点名称',
    class_name VARCHAR(256) NOT NULL COMMENT '类名',
    method_name VARCHAR(128) NOT NULL COMMENT '方法名',
    status VARCHAR(16) NOT NULL COMMENT 'RUNNING/SUCCESS/ERROR',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    start_time DATETIME(3) NOT NULL COMMENT '开始时间',
    end_time DATETIME(3) DEFAULT NULL COMMENT '结束时间',
    duration_ms BIGINT DEFAULT NULL COMMENT '耗时毫秒',
    extra_data TEXT DEFAULT NULL COMMENT '扩展信息JSON',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_t_llm_trace_node_trace_node (trace_id, node_id),
    KEY idx_t_llm_trace_node_parent (trace_id, parent_node_id),
    KEY idx_t_llm_trace_node_start_time (start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'LLM链路节点日志';

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

CREATE TABLE IF NOT EXISTS t_character (
    id VARCHAR(64) NOT NULL COMMENT '人物ID',
    work_id VARCHAR(64) NOT NULL COMMENT '作品ID',
    name VARCHAR(128) NOT NULL COMMENT '剧本中使用的名称',
    display_name VARCHAR(128) DEFAULT NULL COMMENT '显示名称或别名',
    description VARCHAR(1000) DEFAULT NULL COMMENT '身份或背景描述',
    personality VARCHAR(1000) DEFAULT NULL COMMENT '性格特征',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    KEY idx_t_character_work_id (work_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作品人物设定';

CREATE TABLE IF NOT EXISTS t_script_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户账号',
    work_id VARCHAR(64) DEFAULT NULL COMMENT '作品ID',
    work_title VARCHAR(256) NOT NULL DEFAULT '' COMMENT '作品标题',
    chapter_number INT NOT NULL COMMENT '章节编号',
    chapter_content TEXT NOT NULL COMMENT '原章节内容',
    chapter_content_hash VARCHAR(64) DEFAULT NULL COMMENT '原章节内容哈希',
    script_content TEXT NOT NULL COMMENT '生成剧本内容',
    model_name VARCHAR(128) DEFAULT NULL COMMENT '模型名称',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT 'LLM链路ID',
    generation_id VARCHAR(64) DEFAULT NULL COMMENT '单次生成任务ID',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_t_script_record_user_work_chapter (user_id, work_title, chapter_number),
    KEY idx_t_script_record_user_id (user_id),
    KEY idx_t_script_record_work_id (work_id),
    KEY idx_t_script_record_work_title (work_title),
    KEY idx_t_script_record_update_time (update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '剧本生成记录';

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
