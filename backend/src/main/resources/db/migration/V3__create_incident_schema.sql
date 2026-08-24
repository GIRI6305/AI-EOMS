CREATE TABLE incidents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by BIGINT NOT NULL,
    assigned_to BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_incident_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),

    CONSTRAINT fk_incident_assigned_to
        FOREIGN KEY (assigned_to) REFERENCES users(id),

    CONSTRAINT chk_incident_severity
        CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),

    CONSTRAINT chk_incident_status
        CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),

    INDEX idx_incident_status (status),
    INDEX idx_incident_severity (severity),
    INDEX idx_incident_created_by (created_by),
    INDEX idx_incident_assigned_to (assigned_to),
    INDEX idx_incident_created_at (created_at)
);
