CREATE DATABASE IF NOT EXISTS NTS_user
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE NTS_user;

CREATE TABLE IF NOT EXISTS NTC_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    user_name VARCHAR(64) NOT NULL COMMENT 'Display name',
    account VARCHAR(64) NOT NULL COMMENT 'Login account',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Password hash ({bcrypt}... or {noop}...)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0=active,1=deleted',
    failed_login_count INT NOT NULL DEFAULT 0 COMMENT 'Continuous failed login count',
    locked_until DATETIME NULL COMMENT 'Lock end time',
    last_login_at DATETIME NULL COMMENT 'Last successful login time',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    created_by VARCHAR(64) NULL COMMENT 'Creator',
    updated_by VARCHAR(64) NULL COMMENT 'Updater',
    UNIQUE KEY uk_ntc_user_account (account),
    KEY idx_ntc_user_status_deleted (status, is_deleted)
) COMMENT='System user table';

INSERT INTO NTC_user (
    user_name,
    account,
    password_hash,
    status,
    is_deleted,
    created_by,
    updated_by
)
VALUES (
    'System Admin',
    'admin',
    '{noop}1233321',
    1,
    0,
    'system',
    'system'
)
ON DUPLICATE KEY UPDATE
    user_name = VALUES(user_name),
    status = VALUES(status),
    is_deleted = VALUES(is_deleted),
    updated_by = 'system';
