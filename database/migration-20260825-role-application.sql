-- 身份申请：公开注册保持 USER，管理员审核后才能授予业务身份。

USE clas;

CREATE TABLE IF NOT EXISTS role_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(20) NOT NULL,
    target_role VARCHAR(20) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_remarks VARCHAR(255),
    operator_id VARCHAR(20),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_role_application_user (user_id, created_at DESC),
    INDEX idx_role_application_status (target_role, status, created_at DESC)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 当前最小用例只开放 RIDER 申请；MERCHANT 继续使用已有的入驻资料与审核流程。
