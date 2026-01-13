-- =============================================================================
-- EShop Database Schema - Common Tables
-- =============================================================================
-- Run this script on each service database that uses Outbox or Saga patterns
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Outbox Pattern Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outbox_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    topic VARCHAR(200) NOT NULL,
    message_key VARCHAR(200),
    event_type VARCHAR(500) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status ENUM('PENDING', 'IN_PROGRESS', 'PUBLISHED', 'FAILED', 'DEAD') NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP(3) NULL,
    locked_by VARCHAR(100) NULL,
    locked_at TIMESTAMP(3) NULL,
    last_error VARCHAR(2000) NULL,
    headers VARCHAR(1000) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    published_at TIMESTAMP(3) NULL,
    
    INDEX idx_outbox_status_next (status, next_retry_at),
    INDEX idx_outbox_created (created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Saga State Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saga_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    saga_id VARCHAR(36) NOT NULL UNIQUE,
    saga_type VARCHAR(50) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    current_step VARCHAR(50) NOT NULL,
    status ENUM('STARTED', 'IN_PROGRESS', 'COMPLETED', 'COMPENSATING', 'COMPENSATED', 'COMPENSATION_FAILED', 'TIMED_OUT') NOT NULL DEFAULT 'STARTED',
    payload TEXT,
    completed_steps VARCHAR(1000),
    failed_step VARCHAR(50) NULL,
    failure_reason VARCHAR(2000) NULL,
    compensation_attempts INT NOT NULL DEFAULT 0,
    timeout_at TIMESTAMP(3) NULL,
    step_started_at TIMESTAMP(3) NULL,
    locked_by VARCHAR(100) NULL,
    locked_at TIMESTAMP(3) NULL,
    user_id INT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    completed_at TIMESTAMP(3) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    
    INDEX idx_saga_status (status),
    INDEX idx_saga_type_correlation (saga_type, correlation_id),
    INDEX idx_saga_timeout (status, timeout_at),
    INDEX idx_saga_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- ShedLock Table (for distributed scheduling)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Idempotency Table (for consumer deduplication)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS processed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    event_type VARCHAR(200) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    INDEX idx_processed_events_aggregate (aggregate_type, aggregate_id),
    INDEX idx_processed_events_processed (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Cleanup Job: Delete old processed events (run daily)
-- -----------------------------------------------------------------------------
-- Run this as a scheduled event or cron job
-- DELETE FROM processed_events WHERE processed_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
-- DELETE FROM outbox_messages WHERE status = 'PUBLISHED' AND published_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
-- DELETE FROM saga_states WHERE status IN ('COMPLETED', 'COMPENSATED') AND completed_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- -----------------------------------------------------------------------------
-- Create MySQL Event for automatic cleanup (optional)
-- -----------------------------------------------------------------------------
DELIMITER //

CREATE EVENT IF NOT EXISTS cleanup_old_outbox_messages
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    DELETE FROM outbox_messages 
    WHERE status = 'PUBLISHED' 
    AND published_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
    
    DELETE FROM processed_events 
    WHERE processed_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
    
    DELETE FROM saga_states 
    WHERE status IN ('COMPLETED', 'COMPENSATED') 
    AND completed_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
END //

DELIMITER ;

-- Enable event scheduler (run once on MySQL server)
-- SET GLOBAL event_scheduler = ON;
