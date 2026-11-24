
CREATE DATABASE IF NOT EXISTS lab_scheduler;
USE lab_scheduler;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'STAFF') NOT NULL DEFAULT 'STAFF',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS priorities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    rank_order INT NOT NULL,
    target_minutes INT NOT NULL,
    CONSTRAINT uq_priorities_name UNIQUE (name),
    CONSTRAINT uq_priorities_rank UNIQUE (rank_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS test_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT uq_test_types_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS machines (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL DEFAULT 1,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS machine_test_types (
    machine_id VARCHAR(50) NOT NULL,
    test_type_id INT NOT NULL,
    PRIMARY KEY (machine_id, test_type_id),
    CONSTRAINT fk_mtt_machine FOREIGN KEY (machine_id)
        REFERENCES machines (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_mtt_test_type FOREIGN KEY (test_type_id)
        REFERENCES test_types (id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_mtt_test_type (test_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS machine_downtime (
    id INT AUTO_INCREMENT PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    down_start DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    down_end DATETIME NULL,
    reason VARCHAR(255) NOT NULL,
    reported_by VARCHAR(50) NULL DEFAULT 'SYSTEM',
    CONSTRAINT fk_downtime_machine FOREIGN KEY (machine_id)
        REFERENCES machines (id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_downtime_machine (machine_id),
    INDEX idx_downtime_active (machine_id, down_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS samples (
    id VARCHAR(50) PRIMARY KEY,
    test_type_id INT NOT NULL,
    priority_id INT NOT NULL,
    sample_type VARCHAR(50) NOT NULL DEFAULT 'Blood',
    test_category VARCHAR(50) NOT NULL DEFAULT 'General',
    processing_type VARCHAR(50) NOT NULL DEFAULT 'Routine',
    received_time DATETIME NOT NULL,
    target_minutes INT NOT NULL,
    status ENUM('RECEIVED', 'QUEUED', 'PROCESSING', 'COMPLETED', 'ON_HOLD') NOT NULL DEFAULT 'RECEIVED',
    machine_id VARCHAR(50) NULL,
    start_time DATETIME NULL,
    completion_time DATETIME NULL,
    CONSTRAINT fk_samples_test_type FOREIGN KEY (test_type_id)
        REFERENCES test_types (id) ON UPDATE CASCADE,
    CONSTRAINT fk_samples_priority FOREIGN KEY (priority_id)
        REFERENCES priorities (id) ON UPDATE CASCADE,
    CONSTRAINT fk_samples_machine FOREIGN KEY (machine_id)
        REFERENCES machines (id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_samples_status (status),
    INDEX idx_samples_received_time (received_time),
    INDEX idx_samples_test_type (test_type_id),
    INDEX idx_samples_priority (priority_id),
    INDEX idx_samples_machine (machine_id),
    INDEX idx_samples_status_received (status, received_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sample_assignments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sample_id VARCHAR(50) NOT NULL,
    machine_id VARCHAR(50) NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    status ENUM('ASSIGNED', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'REASSIGNED') NOT NULL DEFAULT 'ASSIGNED',
    notes VARCHAR(255) NULL,
    CONSTRAINT fk_assign_sample FOREIGN KEY (sample_id)
        REFERENCES samples (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_assign_machine FOREIGN KEY (machine_id)
        REFERENCES machines (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_assign_sample (sample_id),
    INDEX idx_assign_machine (machine_id),
    INDEX idx_assign_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sample_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sample_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_history_sample_id (sample_id),
    INDEX idx_history_created_at (created_at),
    INDEX idx_history_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO users (username, password_hash, role) VALUES
('admin', '$2a$10$ZGFURnjTM/LBWqfDoJ0gXub8OjFHpJe6XZdmw08IVUrq/I4mJn9k2', 'ADMIN'),
('staff', '$2a$10$htZl8.2E/95zqDiqovctSObkKQtv4dEhXBo7M6ZxE7xr336scw7IS', 'STAFF')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role = VALUES(role);

INSERT INTO priorities (id, name, rank_order, target_minutes) VALUES
(1, 'EMERGENCY', 1, 15),
(2, 'HIGH',      2, 30),
(3, 'NORMAL',    3, 120),
(4, 'LOW',       4, 240)
ON DUPLICATE KEY UPDATE name = VALUES(name), rank_order = VALUES(rank_order), target_minutes = VALUES(target_minutes);

INSERT INTO test_types (id, name, description) VALUES
(1, 'Blood', 'Complete Blood Count (CBC), coagulation profile, and cellular analysis'),
(2, 'Urine', 'Urinalysis, 24-hour protein assay, and sediment microscopy'),
(3, 'Thyroid', 'Endocrinology panels including TSH, Free T3, and Free T4'),
(4, 'Biochemistry', 'Liver function (LFT), renal profile (BUN/Cr), and lipid panels'),
(5, 'Electrolytes', 'Critical care electrolytes (Sodium, Potassium, Chloride, Bicarbonate)'),
(6, 'Molecular PCR', 'Viral RT-PCR and nucleic acid amplification assays'),
(7, 'Microbiology', 'Bacterial culture, colony isolation, and antibiotic susceptibility testing (AST)'),
(8, 'Immunology', 'Serology, autoimmune markers, and infectious viral antibodies'),
(9, 'Histopathology', 'Surgical biopsy tissue processing, sectioning, and microscopic examination')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO machines (id, name, capacity, is_available) VALUES
('M1', 'Automated Hematology & Coagulation Analyzer', 3, TRUE),
('M2', 'Clinical Chemistry & Electrolyte Analyzer', 3, TRUE),
('M3', 'Chemiluminescent Immunoassay System', 2, TRUE),
('M4', 'Automated Molecular RT-PCR Platform', 2, TRUE),
('M5', 'Histopathology & Specialized Testing Station', 2, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), capacity = VALUES(capacity), is_available = VALUES(is_available);

INSERT IGNORE INTO machine_test_types (machine_id, test_type_id) VALUES
('M1', 1),
('M2', 2),
('M2', 4),
('M2', 5),
('M3', 3),
('M3', 8),
('M4', 6),
('M4', 7),
('M5', 2),
('M5', 9);

INSERT INTO samples (id, test_type_id, priority_id, sample_type, test_category, processing_type, received_time, target_minutes, status, machine_id, start_time, completion_time) VALUES
('S101', 1, 2, 'Blood',  'Hematology',              'Routine', '2026-11-23 15:45:00', 30,  'PROCESSING', 'M1',  '2026-11-23 15:50:00', NULL),
('S102', 2, 3, 'Urine',  'Urinalysis',             'Routine', '2026-11-23 15:30:00', 120, 'PROCESSING', 'M2',  '2026-11-23 15:35:00', NULL),
('S103', 4, 3, 'Serum',  'Liver Function (LFT)',    'Routine', '2026-11-23 15:15:00', 120, 'PROCESSING', 'M2',  '2026-11-23 15:20:00', NULL),
('S104', 3, 2, 'Serum',  'Endocrinology (TSH/FT4)', 'Routine', '2026-11-23 16:00:00', 30,  'PROCESSING', 'M3',  '2026-11-23 16:05:00', NULL),
('S105', 6, 1, 'Swab',   'Viral RT-PCR',            'STAT',    '2026-11-23 16:15:00', 15,  'PROCESSING', 'M4',  '2026-11-23 16:18:00', NULL),
('S106', 5, 1, 'Serum',  'Critical Care Electrolytes','STAT',   '2026-11-23 16:20:00', 15,  'QUEUED',     NULL,  NULL,                  NULL),
('S107', 7, 4, 'Sputum', 'Culture & AST',           'Batch',   '2026-11-23 14:00:00', 240, 'QUEUED',     NULL,  NULL,                  NULL),
('S108', 4, 3, 'Serum',  'Renal Profile (BUN/Cr)',  'Routine', '2026-11-23 16:05:00', 120, 'QUEUED',     NULL,  NULL,                  NULL),
('S109', 9, 4, 'Tissue', 'Biopsy Histology',        'Routine', '2026-11-23 13:30:00', 240, 'QUEUED',     NULL,  NULL,                  NULL),
('S110', 1, 2, 'Blood',  'Coagulation (PT/INR)',    'STAT',    '2026-11-23 16:10:00', 30,  'QUEUED',     NULL,  NULL,                  NULL),
('S111', 8, 3, 'Serum',  'Viral Serology (HBsAg)',  'Routine', '2026-11-23 15:50:00', 120, 'QUEUED',     NULL,  NULL,                  NULL),
('S112', 2, 3, 'Urine',  '24-Hr Protein Assay',     'Batch',   '2026-11-23 14:30:00', 120, 'ON_HOLD',    NULL,  NULL,                  NULL),
('S113', 6, 2, 'Saliva', 'Respiratory Multiplex',  'STAT',    '2026-11-23 15:00:00', 30,  'ON_HOLD',    NULL,  NULL,                  NULL),
('S114', 1, 3, 'Blood',  'Complete Blood Count (CBC)','Routine','2026-11-23 13:00:00', 120, 'COMPLETED',  'M1',  '2026-11-23 13:05:00', '2026-11-23 13:42:00'),
('S115', 4, 3, 'Serum',  'Lipid Panel Profile',     'Routine', '2026-11-23 13:10:00', 120, 'COMPLETED',  'M2',  '2026-11-23 13:15:00', '2026-11-23 14:05:00'),
('S116', 3, 2, 'Serum',  'Free T3 & Free T4 Assay', 'Routine', '2026-11-23 13:30:00', 30,  'COMPLETED',  'M3',  '2026-11-23 13:35:00', '2026-11-23 13:58:00'),
('S117', 6, 1, 'Swab',   'SARS-CoV-2 RT-PCR',       'STAT',    '2026-11-23 14:00:00', 15,  'COMPLETED',  'M4',  '2026-11-23 14:02:00', '2026-11-23 14:14:00'),
('S118', 2, 3, 'Urine',  'Microalbuminuria Screen', 'Routine', '2026-11-23 12:30:00', 120, 'COMPLETED',  'M2',  '2026-11-23 12:35:00', '2026-11-23 13:45:00'),
('S119', 9, 4, 'Tissue', 'Excisional Biopsy',       'Routine', '2026-11-23 11:00:00', 240, 'COMPLETED',  'M5',  '2026-11-23 11:15:00', '2026-11-23 14:20:00'),
('S120', 1, 1, 'Blood',  'D-Dimer Coagulation',     'STAT',    '2026-11-23 14:30:00', 15,  'COMPLETED',  'M1',  '2026-11-23 14:32:00', '2026-11-23 14:43:00')
ON DUPLICATE KEY UPDATE
    test_type_id = VALUES(test_type_id),
    priority_id = VALUES(priority_id),
    sample_type = VALUES(sample_type),
    test_category = VALUES(test_category),
    processing_type = VALUES(processing_type),
    target_minutes = VALUES(target_minutes),
    status = VALUES(status),
    machine_id = VALUES(machine_id),
    start_time = VALUES(start_time),
    completion_time = VALUES(completion_time);

INSERT INTO sample_history (sample_id, action, details, created_at) VALUES
('S101', 'RECEIVED', 'Specimen Blood received for Routine Hematology', '2026-11-23 15:45:00'),
('S101', 'PROCESSING', 'Assigned to Hematology Analyzer M1 (Slot: 1/3)', '2026-11-23 15:50:00'),
('S102', 'RECEIVED', 'Specimen Urine received for Routine Urinalysis', '2026-11-23 15:30:00'),
('S102', 'PROCESSING', 'Dispatched to Chemistry & Electrolyte Analyzer M2', '2026-11-23 15:35:00'),
('S103', 'RECEIVED', 'Specimen Serum received for Liver Function (LFT)', '2026-11-23 15:15:00'),
('S103', 'PROCESSING', 'Allocated to Chemistry Analyzer M2', '2026-11-23 15:20:00'),
('S104', 'RECEIVED', 'Specimen Serum received for Endocrinology TSH/FT4', '2026-11-23 16:00:00'),
('S104', 'PROCESSING', 'Transferred to Immunoassay System M3', '2026-11-23 16:05:00'),
('S105', 'RECEIVED', 'Specimen Swab received for STAT Viral RT-PCR', '2026-11-23 16:15:00'),
('S105', 'PROCESSING', 'Priority dispatch to Molecular RT-PCR Platform M4', '2026-11-23 16:18:00'),
('S106', 'RECEIVED', 'Specimen Serum received for STAT Electrolytes', '2026-11-23 16:20:00'),
('S106', 'QUEUED', 'Sample prioritized in Live Queue for next free slot', '2026-11-23 16:20:00'),
('S107', 'RECEIVED', 'Specimen Sputum received for Bacterial Culture & AST', '2026-11-23 14:00:00'),
('S107', 'QUEUED', 'Incubation batch queued awaiting incubator capacity', '2026-11-23 14:00:00'),
('S108', 'RECEIVED', 'Specimen Serum received for Renal Profile (BUN/Cr)', '2026-11-23 16:05:00'),
('S108', 'QUEUED', 'Queued in Routine Biochemistry line', '2026-11-23 16:05:00'),
('S109', 'RECEIVED', 'Specimen Tissue received for Biopsy Histopathology', '2026-11-23 13:30:00'),
('S109', 'QUEUED', 'Histology tissue processing scheduled', '2026-11-23 13:30:00'),
('S110', 'RECEIVED', 'Specimen Blood received for STAT Coagulation PT/INR', '2026-11-23 16:10:00'),
('S110', 'QUEUED', 'Prioritized in Queue with HIGH priority', '2026-11-23 16:10:00'),
('S112', 'RECEIVED', 'Specimen Urine received for 24-Hr Protein', '2026-11-23 14:30:00'),
('S112', 'ON_HOLD', 'Placed ON_HOLD: Container volume verification required', '2026-11-23 14:35:00'),
('S114', 'RECEIVED', 'Specimen Blood received for Complete Blood Count', '2026-11-23 13:00:00'),
('S114', 'COMPLETED', 'Analysis verified and reported within SLA', '2026-11-23 13:42:00'),
('S117', 'RECEIVED', 'Specimen Swab received for SARS-CoV-2 RT-PCR', '2026-11-23 14:00:00'),
('S117', 'COMPLETED', 'RT-PCR amplification completed (14 min turnaround)', '2026-11-23 14:14:00');
