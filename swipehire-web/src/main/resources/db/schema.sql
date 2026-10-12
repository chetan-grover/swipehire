-- =====================================================================
-- SwipeHire schema (MySQL 8.0.16+, InnoDB, utf8mb4). Hand-written, in 3NF.
-- Hibernate only VALIDATES against this file (ddl-auto=validate).
--
-- Conventions
--  * Enum-like columns are VARCHAR + CHECK (not MySQL ENUM) so values match the
--    Java enums in swipehire-common and Hibernate validation stays simple.
--  * Many-to-many data (skills, cities, colleges) always lives in a junction
--    table; there are no array/JSON columns.
--  * Script is idempotent (IF NOT EXISTS) because Spring runs it on every boot.
--    After changing a table during development: DROP DATABASE swipehire; and restart.
-- =====================================================================

-- ---------- Identity & verification ----------

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    CONSTRAINT ck_users_role CHECK (role IN ('APPLICANT', 'HIRER', 'ADMIN'))
) ENGINE = InnoDB;

-- A college is identified by its student email domain (e.g. vitstudent.ac.in).
-- Applicants can request a new college; it stays PENDING until an admin approves it.
CREATE TABLE IF NOT EXISTS college (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(150) NOT NULL,
    email_domain VARCHAR(100) NOT NULL,
    city         VARCHAR(80)  NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    requested_by BIGINT       NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_college_domain (email_domain),
    CONSTRAINT fk_college_requested_by FOREIGN KEY (requested_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_college_status CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
) ENGINE = InnoDB;

-- One row per OTP sent. Only the BCrypt hash of the OTP is stored, never the code itself.
CREATE TABLE IF NOT EXISTS email_verification (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    email       VARCHAR(190) NOT NULL,
    otp_hash    VARCHAR(100) NOT NULL,
    attempts    INT      NOT NULL DEFAULT 0,
    expires_at  DATETIME     NOT NULL,
    verified_at DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ev_user (user_id, created_at),
    CONSTRAINT fk_ev_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_ev_attempts CHECK (attempts BETWEEN 0 AND 5)
) ENGINE = InnoDB;

-- ---------- Applicant side ----------

-- 1:1 with users (shared primary key). college_id is set only after OTP verification.
CREATE TABLE IF NOT EXISTS applicant_profile (
    user_id             BIGINT       NOT NULL,
    headline            VARCHAR(160) NULL,
    about               TEXT         NULL,
    phone               VARCHAR(20)  NULL,
    city                VARCHAR(80)  NULL,
    degree              VARCHAR(100) NULL,
    grad_year           INT     NULL,
    college_id          BIGINT       NULL,
    college_email       VARCHAR(190) NULL,
    college_verified_at DATETIME     NULL,
    pref_work_mode      VARCHAR(16)  NOT NULL DEFAULT 'ANY',
    pref_job_type       VARCHAR(16)  NOT NULL DEFAULT 'ANY',
    min_stipend         INT          NOT NULL DEFAULT 0,
    discoverable        BOOLEAN      NOT NULL DEFAULT TRUE,
    photo_path          VARCHAR(255) NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_ap_college_email (college_email),
    CONSTRAINT fk_ap_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ap_college FOREIGN KEY (college_id) REFERENCES college (id),
    CONSTRAINT ck_ap_mode CHECK (pref_work_mode IN ('REMOTE', 'HYBRID', 'ONSITE', 'ANY')),
    CONSTRAINT ck_ap_type CHECK (pref_job_type IN ('INTERNSHIP', 'FULL_TIME', 'ANY')),
    CONSTRAINT ck_ap_stipend CHECK (min_stipend >= 0)
) ENGINE = InnoDB;

-- Multi-valued preference -> its own table (1NF).
CREATE TABLE IF NOT EXISTS applicant_pref_city (
    applicant_id BIGINT      NOT NULL,
    city         VARCHAR(80) NOT NULL,
    PRIMARY KEY (applicant_id, city),
    CONSTRAINT fk_apc_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------- Company & hirer ----------

CREATE TABLE IF NOT EXISTS company (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL,
    website     VARCHAR(200) NULL,
    description TEXT         NULL,
    city        VARCHAR(80)  NULL,
    logo_path   VARCHAR(255) NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    verified_by BIGINT       NULL,
    verified_at DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_name (name),
    CONSTRAINT fk_company_verified_by FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_company_status CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS hirer (
    user_id     BIGINT       NOT NULL,
    company_id  BIGINT       NOT NULL,
    designation VARCHAR(100) NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_hirer_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_hirer_company FOREIGN KEY (company_id) REFERENCES company (id)
) ENGINE = InnoDB;

-- ---------- Skills dictionary ----------

CREATE TABLE IF NOT EXISTS skill (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    name     VARCHAR(80) NOT NULL,
    category VARCHAR(40) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_skill_name (name)
) ENGINE = InnoDB;

-- Alternative spellings used by the PDF import ("JS" -> JavaScript).
CREATE TABLE IF NOT EXISTS skill_synonym (
    alias    VARCHAR(80) NOT NULL,
    skill_id BIGINT      NOT NULL,
    PRIMARY KEY (alias),
    CONSTRAINT fk_syn_skill FOREIGN KEY (skill_id) REFERENCES skill (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS applicant_skill (
    applicant_id BIGINT  NOT NULL,
    skill_id     BIGINT  NOT NULL,
    proficiency  INT NOT NULL,
    PRIMARY KEY (applicant_id, skill_id),
    CONSTRAINT fk_as_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_as_skill FOREIGN KEY (skill_id) REFERENCES skill (id),
    CONSTRAINT ck_as_prof CHECK (proficiency BETWEEN 1 AND 5)
) ENGINE = InnoDB;

-- Learned from swipes: positive = applicant tends to like jobs needing this skill.
CREATE TABLE IF NOT EXISTS applicant_skill_pref (
    applicant_id BIGINT   NOT NULL,
    skill_id     BIGINT   NOT NULL,
    affinity     DOUBLE   NOT NULL DEFAULT 0,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (applicant_id, skill_id),
    CONSTRAINT fk_asp_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_asp_skill FOREIGN KEY (skill_id) REFERENCES skill (id) ON DELETE CASCADE,
    CONSTRAINT ck_asp_range CHECK (affinity BETWEEN -1 AND 1)
) ENGINE = InnoDB;

-- ---------- Structured resume ----------

CREATE TABLE IF NOT EXISTS resume_item (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    applicant_id  BIGINT       NOT NULL,
    item_type     VARCHAR(16)  NOT NULL,
    title         VARCHAR(160) NOT NULL,
    organization  VARCHAR(160) NULL,
    start_date    DATE         NULL,
    end_date      DATE         NULL,
    description   TEXT         NULL,
    display_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ri_applicant (applicant_id, item_type),
    CONSTRAINT fk_ri_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE,
    CONSTRAINT ck_ri_type CHECK (item_type IN ('EDUCATION', 'EXPERIENCE', 'PROJECT', 'ACHIEVEMENT', 'CERTIFICATION')),
    CONSTRAINT ck_ri_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS resume_item_skill (
    resume_item_id BIGINT NOT NULL,
    skill_id       BIGINT NOT NULL,
    PRIMARY KEY (resume_item_id, skill_id),
    CONSTRAINT fk_ris_item FOREIGN KEY (resume_item_id) REFERENCES resume_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_ris_skill FOREIGN KEY (skill_id) REFERENCES skill (id)
) ENGINE = InnoDB;

-- Uploaded PDF resumes; extracted_text (via PDFBox) feeds TF-IDF.
CREATE TABLE IF NOT EXISTS resume_upload (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    applicant_id   BIGINT       NOT NULL,
    file_name      VARCHAR(200) NOT NULL,
    file_path      VARCHAR(255) NOT NULL,
    extracted_text MEDIUMTEXT   NULL,
    uploaded_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ru_applicant (applicant_id, uploaded_at),
    CONSTRAINT fk_ru_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------- Jobs ----------

-- company_id is stored (not derived via posted_by -> hirer) on purpose: the job belongs to the
-- company even if the posting recruiter later moves to another company. So it is not a transitive dependency.
CREATE TABLE IF NOT EXISTS job (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    company_id     BIGINT       NOT NULL,
    posted_by      BIGINT       NOT NULL,
    title          VARCHAR(160) NOT NULL,
    description    TEXT         NOT NULL,
    job_type       VARCHAR(16)  NOT NULL,
    work_mode      VARCHAR(16)  NOT NULL,
    city           VARCHAR(80)  NULL,
    stipend_min    INT          NOT NULL DEFAULT 0,
    stipend_max    INT          NOT NULL DEFAULT 0,
    openings       INT          NOT NULL DEFAULT 1,
    deadline       DATE         NOT NULL,
    min_grad_year  INT     NULL,
    max_grad_year  INT     NULL,
    verified_only  BOOLEAN      NOT NULL DEFAULT FALSE,
    status         VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_job_deck (status, deadline),
    KEY idx_job_company (company_id),
    CONSTRAINT fk_job_company FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_job_posted_by FOREIGN KEY (posted_by) REFERENCES hirer (user_id),
    CONSTRAINT ck_job_type CHECK (job_type IN ('INTERNSHIP', 'FULL_TIME')),
    CONSTRAINT ck_job_mode CHECK (work_mode IN ('REMOTE', 'HYBRID', 'ONSITE')),
    CONSTRAINT ck_job_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'CLOSED')),
    CONSTRAINT ck_job_stipend CHECK (stipend_min >= 0 AND stipend_max >= stipend_min),
    CONSTRAINT ck_job_openings CHECK (openings >= 1),
    CONSTRAINT ck_job_grad CHECK (min_grad_year IS NULL OR max_grad_year IS NULL OR max_grad_year >= min_grad_year),
    CONSTRAINT ck_job_city CHECK (work_mode = 'REMOTE' OR city IS NOT NULL)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS job_skill (
    job_id      BIGINT  NOT NULL,
    skill_id    BIGINT  NOT NULL,
    weight      INT NOT NULL DEFAULT 3,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (job_id, skill_id),
    CONSTRAINT fk_js_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE,
    CONSTRAINT fk_js_skill FOREIGN KEY (skill_id) REFERENCES skill (id),
    CONSTRAINT ck_js_weight CHECK (weight BETWEEN 1 AND 5)
) ENGINE = InnoDB;

-- Optional restriction: if a job has rows here, only these colleges' verified students may see it.
CREATE TABLE IF NOT EXISTS job_allowed_college (
    job_id     BIGINT NOT NULL,
    college_id BIGINT NOT NULL,
    PRIMARY KEY (job_id, college_id),
    CONSTRAINT fk_jac_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE,
    CONSTRAINT fk_jac_college FOREIGN KEY (college_id) REFERENCES college (id)
) ENGINE = InnoDB;

-- ---------- Swipes & quota ----------

-- One swipe per (applicant, job), enforced by the unique key, not by application code.
CREATE TABLE IF NOT EXISTS applicant_swipe (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    applicant_id BIGINT       NOT NULL,
    job_id       BIGINT       NOT NULL,
    direction    VARCHAR(8)   NOT NULL,
    pitch        VARCHAR(140) NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_aswipe (applicant_id, job_id),
    KEY idx_aswipe_job (job_id, direction),
    CONSTRAINT fk_aswipe_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_aswipe_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE,
    CONSTRAINT ck_aswipe_dir CHECK (direction IN ('LEFT', 'RIGHT', 'SUPER')),
    CONSTRAINT ck_aswipe_pitch CHECK (direction = 'SUPER' OR pitch IS NULL)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS hirer_swipe (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    hirer_id     BIGINT      NOT NULL,
    applicant_id BIGINT      NOT NULL,
    job_id       BIGINT      NOT NULL,
    direction    VARCHAR(8)  NOT NULL,
    source       VARCHAR(10) NOT NULL,
    created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_hswipe (applicant_id, job_id),
    KEY idx_hswipe_job (job_id),
    CONSTRAINT fk_hswipe_hirer FOREIGN KEY (hirer_id) REFERENCES hirer (user_id),
    CONSTRAINT fk_hswipe_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_hswipe_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE,
    CONSTRAINT ck_hswipe_dir CHECK (direction IN ('LEFT', 'RIGHT')),
    CONSTRAINT ck_hswipe_src CHECK (source IN ('QUEUE', 'DISCOVER'))
) ENGINE = InnoDB;

-- Per-day counters. The CHECKs are the last line of defence; the service uses a
-- conditional UPDATE (... WHERE right_used < 25) so concurrent requests cannot exceed the limit.
CREATE TABLE IF NOT EXISTS daily_quota (
    applicant_id BIGINT  NOT NULL,
    quota_date   DATE    NOT NULL,
    right_used   INT NOT NULL DEFAULT 0,
    super_used   INT NOT NULL DEFAULT 0,
    PRIMARY KEY (applicant_id, quota_date),
    CONSTRAINT fk_dq_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE,
    CONSTRAINT ck_dq_right CHECK (right_used BETWEEN 0 AND 25),
    CONSTRAINT ck_dq_super CHECK (super_used BETWEEN 0 AND 3)
) ENGINE = InnoDB;

-- ---------- Match & application pipeline ----------

-- Named job_match because MATCH is a reserved word in MySQL.
CREATE TABLE IF NOT EXISTS job_match (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    applicant_id BIGINT   NOT NULL,
    job_id       BIGINT   NOT NULL,
    matched_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_match (applicant_id, job_id),
    KEY idx_match_job (job_id),
    CONSTRAINT fk_match_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_match_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- 1:1 with job_match. status is the current state; history keeps every transition.
CREATE TABLE IF NOT EXISTS application (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    match_id          BIGINT       NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'MATCHED',
    tailored_pdf_path VARCHAR(255) NULL,
    applied_at        DATETIME     NULL,
    status_changed_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_match (match_id),
    KEY idx_app_status (status, status_changed_at),
    CONSTRAINT fk_app_match FOREIGN KEY (match_id) REFERENCES job_match (id) ON DELETE CASCADE,
    CONSTRAINT ck_app_status CHECK (status IN ('MATCHED', 'APPLIED', 'SHORTLISTED', 'INTERVIEW', 'OFFER',
                                               'REJECTED', 'WITHDRAWN', 'EXPIRED'))
) ENGINE = InnoDB;

-- changed_by NULL means the system did it (e.g. the expiry scheduler).
CREATE TABLE IF NOT EXISTS application_status_history (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    application_id BIGINT       NOT NULL,
    from_status    VARCHAR(16)  NULL,
    to_status      VARCHAR(16)  NOT NULL,
    changed_by     BIGINT       NULL,
    note           VARCHAR(500) NULL,
    changed_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ash_app (application_id, changed_at),
    CONSTRAINT fk_ash_app FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE CASCADE,
    CONSTRAINT fk_ash_user FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- Which resume items the tailoring step chose, in which order: the PDF is reproducible and auditable.
CREATE TABLE IF NOT EXISTS tailored_resume_item (
    application_id BIGINT NOT NULL,
    resume_item_id BIGINT NOT NULL,
    rank_order     INT    NOT NULL,
    relevance      DOUBLE NOT NULL,
    PRIMARY KEY (application_id, resume_item_id),
    CONSTRAINT fk_tri_app FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE CASCADE,
    CONSTRAINT fk_tri_item FOREIGN KEY (resume_item_id) REFERENCES resume_item (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS interview_slot (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    application_id BIGINT       NOT NULL,
    slot_start     DATETIME     NOT NULL,
    duration_min   INT     NOT NULL DEFAULT 30,
    location       VARCHAR(255) NULL,
    chosen         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_slot_app (application_id),
    CONSTRAINT fk_slot_app FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE CASCADE,
    CONSTRAINT ck_slot_duration CHECK (duration_min BETWEEN 10 AND 240)
) ENGINE = InnoDB;

-- ---------- Communication ----------

CREATE TABLE IF NOT EXISTS message (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    match_id  BIGINT      NOT NULL,
    sender_id BIGINT      NOT NULL,
    body      TEXT        NOT NULL,
    sent_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    read_at   DATETIME    NULL,
    PRIMARY KEY (id),
    KEY idx_msg_match (match_id, sent_at),
    CONSTRAINT fk_msg_match FOREIGN KEY (match_id) REFERENCES job_match (id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS notification (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    title      VARCHAR(160) NOT NULL,
    body       VARCHAR(500) NULL,
    link       VARCHAR(255) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at    DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_notif_user (user_id, read_at),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_notif_type CHECK (type IN ('MATCH', 'STATUS_CHANGE', 'MESSAGE', 'INTERVIEW', 'EXPIRY_WARNING', 'VERIFICATION'))
) ENGINE = InnoDB;

-- ---------- Explainable scoring ----------

-- Every component is stored so any card's score can be explained later. Matched/missing skill
-- lists are NOT stored; they are derived from applicant_skill and job_skill (avoids redundancy).
CREATE TABLE IF NOT EXISTS match_score_cache (
    applicant_id     BIGINT   NOT NULL,
    job_id           BIGINT   NOT NULL,
    total            DOUBLE   NOT NULL,
    skill_coverage   DOUBLE   NOT NULL,
    tfidf_cosine     DOUBLE   NOT NULL,
    preference_boost DOUBLE   NOT NULL,
    location_fit     DOUBLE   NOT NULL,
    stipend_fit      DOUBLE   NOT NULL,
    computed_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (applicant_id, job_id),
    KEY idx_msc_job (job_id, total),
    CONSTRAINT fk_msc_applicant FOREIGN KEY (applicant_id) REFERENCES applicant_profile (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_msc_job FOREIGN KEY (job_id) REFERENCES job (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------- Views ----------

-- Hirer responsiveness: of the applications that were APPLIED and are "decided" (the hirer
-- already acted, or 7 days have passed), what share did the hirer act on within 7 days of the
-- applicant applying? The clock starts at applied_at, so applicants who never apply don't hurt the hirer.
CREATE OR REPLACE VIEW v_hirer_responsiveness AS
SELECT j.posted_by AS hirer_id,
       COUNT(*)    AS decided_applications,
       SUM(CASE
               WHEN EXISTS (SELECT 1
                            FROM application_status_history h
                            WHERE h.application_id = a.id
                              AND h.changed_by = j.posted_by
                              AND h.changed_at <= a.applied_at + INTERVAL 7 DAY) THEN 1
               ELSE 0 END) AS acted_in_time
FROM application a
         JOIN job_match m ON m.id = a.match_id
         JOIN job j ON j.id = m.job_id
WHERE a.applied_at IS NOT NULL
  AND (a.applied_at <= NOW() - INTERVAL 7 DAY
       OR EXISTS (SELECT 1 FROM application_status_history h2
                  WHERE h2.application_id = a.id AND h2.changed_by = j.posted_by))
GROUP BY j.posted_by;
