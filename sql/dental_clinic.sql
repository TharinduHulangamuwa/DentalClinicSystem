-- =====================================================================
--  SUNRISE DENTAL CLINIC
--  Appointment and Patient Management System
--
--  CIS6003 Advanced Programming - WRIT1
--  Cardiff Metropolitan University / ICBT Campus
--
--  Student : [Your Name]
--  ID      : [stXXXXXXXX]
--
--  ONE SCRIPT. Run this whole file once in phpMyAdmin.
--  It drops and recreates everything, so it is safe to re-run.
-- =====================================================================

DROP DATABASE IF EXISTS dental_clinic;
CREATE DATABASE dental_clinic
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE dental_clinic;

-- =====================================================================
-- 1. TABLES
-- =====================================================================

-- ---------------------------------------------------------------------
-- users - staff accounts (Functionality 1: authentication)
-- Passwords are SHA-256 digests, never plain text.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id    INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(64)  NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'STAFF',
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- treatments - the price list (Functionality 4: billing)
-- Prices live in the database so the clinic can change them without
-- recompiling the application.
-- ---------------------------------------------------------------------
CREATE TABLE treatments (
    treatment_id   INT AUTO_INCREMENT PRIMARY KEY,
    treatment_type VARCHAR(50)   NOT NULL UNIQUE,
    cost           DECIMAL(10,2) NOT NULL CHECK (cost >= 0),
    duration_mins  INT           NOT NULL DEFAULT 30
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- appointments - the core record (Functionalities 2 and 3)
--
-- BUSINESS RULE ENFORCED IN THE DATABASE:
-- uq_slot makes it physically impossible to book one dentist twice at
-- the same date and time. This is the "double bookings" problem named
-- in the scenario. Enforcing it here rather than only in Java means the
-- rule still holds if a second client application is ever written.
-- ---------------------------------------------------------------------
CREATE TABLE appointments (
    appointment_no   VARCHAR(15)  PRIMARY KEY,
    patient_name     VARCHAR(100) NOT NULL,
    address          VARCHAR(200),
    contact_no       VARCHAR(15)  NOT NULL,
    dentist_name     VARCHAR(100) NOT NULL,
    treatment_type   VARCHAR(50)  NOT NULL,
    appointment_date DATE         NOT NULL,
    appointment_time TIME         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'BOOKED',
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_treatment FOREIGN KEY (treatment_type)
        REFERENCES treatments(treatment_type) ON UPDATE CASCADE,
    CONSTRAINT uq_slot UNIQUE (dentist_name, appointment_date, appointment_time)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- sessions - the desktop equivalent of a server session store
--
-- A Swing application has no browser, so there is no JSESSIONID cookie.
-- This table provides the same capability explicitly: each sign-in
-- creates a row identified by a random token, and that token proves the
-- user is still authenticated.
--
-- The token is stored HASHED, exactly as passwords are, so reading this
-- table gives an attacker nothing they can replay.
-- ---------------------------------------------------------------------
CREATE TABLE sessions (
    session_id    INT AUTO_INCREMENT PRIMARY KEY,
    token_hash    VARCHAR(64) NOT NULL UNIQUE,
    user_id       INT         NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    TIMESTAMP   NOT NULL,
    remember_me   TINYINT(1)  NOT NULL DEFAULT 0,
    machine_name  VARCHAR(100),
    active        TINYINT(1)  NOT NULL DEFAULT 1,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_session_lookup ON sessions (token_hash, active);

-- ---------------------------------------------------------------------
-- audit_log - written automatically by triggers
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    log_id     INT AUTO_INCREMENT PRIMARY KEY,
    action     VARCHAR(50) NOT NULL,
    record_ref VARCHAR(50),
    details    VARCHAR(200),
    logged_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =====================================================================
-- 2. TRIGGERS
-- =====================================================================

DELIMITER $$

CREATE TRIGGER trg_appointment_created
AFTER INSERT ON appointments
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (action, record_ref, details)
    VALUES ('APPOINTMENT_CREATED', NEW.appointment_no,
            CONCAT(NEW.patient_name, ' with ', NEW.dentist_name,
                   ' on ', NEW.appointment_date, ' ', NEW.appointment_time));
END$$

CREATE TRIGGER trg_appointment_cancelled
AFTER DELETE ON appointments
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (action, record_ref, details)
    VALUES ('APPOINTMENT_DELETED', OLD.appointment_no,
            CONCAT(OLD.patient_name, ' on ', OLD.appointment_date));
END$$

CREATE TRIGGER trg_user_created
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (action, record_ref, details)
    VALUES ('USER_CREATED', NEW.username,
            CONCAT(NEW.full_name, ' created with role ', NEW.role));
END$$

CREATE TRIGGER trg_user_deleted
AFTER DELETE ON users
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (action, record_ref, details)
    VALUES ('USER_DELETED', OLD.username,
            CONCAT(OLD.full_name, ' (', OLD.role, ') removed'));
END$$

DELIMITER ;

-- =====================================================================
-- 3. STORED FUNCTION
-- =====================================================================

DELIMITER $$

CREATE FUNCTION fn_bill_total(p_treatment VARCHAR(50), p_fee DECIMAL(10,2))
RETURNS DECIMAL(10,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_cost DECIMAL(10,2) DEFAULT 0.00;
    SELECT cost INTO v_cost FROM treatments WHERE treatment_type = p_treatment;
    RETURN IFNULL(v_cost, 0.00) + IFNULL(p_fee, 0.00);
END$$

DELIMITER ;

-- =====================================================================
-- 4. VIEWS - reports that support decision making
-- =====================================================================

CREATE VIEW vw_daily_schedule AS
SELECT a.appointment_date,
       a.dentist_name,
       COUNT(*)    AS total_appointments,
       SUM(t.cost) AS expected_treatment_revenue
FROM appointments a
JOIN treatments t ON a.treatment_type = t.treatment_type
GROUP BY a.appointment_date, a.dentist_name
ORDER BY a.appointment_date DESC;

CREATE VIEW vw_active_sessions AS
SELECT s.session_id, u.username, u.full_name, s.machine_name,
       s.created_at, s.last_activity, s.expires_at,
       TIMESTAMPDIFF(MINUTE, s.last_activity, NOW()) AS idle_minutes
FROM sessions s
JOIN users u ON s.user_id = u.user_id
WHERE s.active = 1 AND s.expires_at > NOW()
ORDER BY s.last_activity DESC;

-- =====================================================================
-- 5. SEED DATA
-- =====================================================================

INSERT INTO treatments (treatment_type, cost, duration_mins) VALUES
 ('Consultation Only',       0.00, 15),
 ('Scaling & Polishing',  4500.00, 30),
 ('Filling',              6000.00, 45),
 ('Extraction',           8000.00, 30),
 ('Teeth Whitening',     18000.00, 60),
 ('Root Canal',          25000.00, 90),
 ('Braces Fitting',      45000.00, 120);

-- admin / admin123     nimali / nimali123
INSERT INTO users (username, password, full_name, role) VALUES
 ('admin',  SHA2('admin123',  256), 'Clinic Administrator', 'ADMIN'),
 ('nimali', SHA2('nimali123', 256), 'Nimali Perera',        'STAFF');

INSERT INTO appointments
 (appointment_no, patient_name, address, contact_no,
  dentist_name, treatment_type, appointment_date, appointment_time) VALUES
 ('APT1001','Kamal Silva','12 Galle Road, Colombo 03','0771234567',
  'Dr. Fernando','Filling',    CURDATE(),                        '10:30:00'),
 ('APT1002','Sanduni Jayasuriya','5 Kandy Road, Kadawatha','0712223334',
  'Dr. Silva','Root Canal',    CURDATE(),                        '14:00:00'),
 ('APT1003','Ruwan Bandara','88 Marine Drive, Dehiwala','0763334445',
  'Dr. Fernando','Extraction', DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00'),
 ('APT1004','Priya Nathan','21 Temple Lane, Nugegoda','0754445556',
  'Dr. Perera','Scaling & Polishing',
                              DATE_ADD(CURDATE(), INTERVAL 1 DAY),'11:15:00');

-- =====================================================================
-- 6. VERIFICATION - each of these should return rows
-- =====================================================================
-- SELECT * FROM users;
-- SELECT * FROM treatments;
-- SELECT * FROM appointments;
-- SELECT * FROM audit_log;                 -- 4 rows written by the trigger
-- SELECT * FROM vw_daily_schedule;
-- SELECT fn_bill_total('Root Canal', 1500.00);   -- expect 26500.00
--
-- Double booking must be REJECTED with error 1062:
-- INSERT INTO appointments (appointment_no, patient_name, contact_no,
--   dentist_name, treatment_type, appointment_date, appointment_time)
-- VALUES ('APT9999','Test','0770000000','Dr. Fernando','Filling',
--         CURDATE(),'10:30:00');
