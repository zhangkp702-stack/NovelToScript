CREATE TABLE IF NOT EXISTS NTC_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(64) NOT NULL,
    account VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    CONSTRAINT uk_ntc_user_account UNIQUE (account)
);

MERGE INTO NTC_user (id, user_name, account, password_hash, status, is_deleted, created_by, updated_by)
KEY (id)
VALUES (1, 'System Admin', 'admin', '{noop}1233321', 1, 0, 'test', 'test');
