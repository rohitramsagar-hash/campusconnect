-- CampusConnect - MySQL 8 schema (reference)
-- You do NOT need to run this: Spring Boot creates/updates these tables automatically
-- (spring.jpa.hibernate.ddl-auto=update) and the database itself (createDatabaseIfNotExist=true).
-- It's here so you can read the design, draw the ER diagram, or create the DB by hand.

CREATE DATABASE IF NOT EXISTS campusconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campusconnect;

CREATE TABLE users (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,            -- BCrypt hash, never the plain password
    role          VARCHAR(20)   NOT NULL,            -- STUDENT | STAFF | ADMIN
    department    VARCHAR(100),
    active        BIT           NOT NULL DEFAULT 1,
    created_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(60)  NOT NULL,
    description VARCHAR(255),
    active      BIT          NOT NULL DEFAULT 1,     -- hidden categories stay on old complaints
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE complaints (
    id             BIGINT        NOT NULL AUTO_INCREMENT,  -- ticket no. CC-<year>-<id padded to 5>
    title          VARCHAR(150)  NOT NULL,
    description    VARCHAR(2000) NOT NULL,
    location       VARCHAR(150),
    category_id    BIGINT        NOT NULL,
    priority       VARCHAR(20)   NOT NULL,                  -- LOW | MEDIUM | HIGH | URGENT
    priority_rank  INT           NOT NULL,                  -- 1..4, used for sorting
    status         VARCHAR(20)   NOT NULL,                  -- OPEN | IN_PROGRESS | RESOLVED | CLOSED | REJECTED
    anonymous      BIT           NOT NULL,
    is_public      BIT           NOT NULL,
    created_by_id  BIGINT        NOT NULL,
    assigned_to_id BIGINT,
    upvote_count   INT           NOT NULL DEFAULT 0,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    due_at         DATETIME(6)   NOT NULL,                  -- created_at + priority's resolution target
    resolved_at    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_complaints_category    FOREIGN KEY (category_id)    REFERENCES categories (id),
    CONSTRAINT fk_complaints_created_by  FOREIGN KEY (created_by_id)  REFERENCES users (id),
    CONSTRAINT fk_complaints_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id),
    INDEX idx_complaints_status (status),
    INDEX idx_complaints_created_by (created_by_id),
    INDEX idx_complaints_assigned_to (assigned_to_id),
    INDEX idx_complaints_created_at (created_at)
);

CREATE TABLE comments (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT        NOT NULL,
    author_id    BIGINT        NOT NULL,
    message      VARCHAR(1000) NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id),
    CONSTRAINT fk_comments_author    FOREIGN KEY (author_id)    REFERENCES users (id),
    INDEX idx_comments_complaint (complaint_id)
);

-- Append-only audit trail of every status change and assignment.
CREATE TABLE status_history (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    complaint_id  BIGINT       NOT NULL,
    from_status   VARCHAR(20),                 -- NULL for the first "submitted" entry
    to_status     VARCHAR(20)  NOT NULL,
    changed_by_id BIGINT       NOT NULL,
    note          VARCHAR(500),
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_history_complaint  FOREIGN KEY (complaint_id)  REFERENCES complaints (id),
    CONSTRAINT fk_history_changed_by FOREIGN KEY (changed_by_id) REFERENCES users (id),
    INDEX idx_history_complaint (complaint_id)
);

-- "Me too" votes: one per user per complaint.
CREATE TABLE upvotes (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_upvotes_complaint_user UNIQUE (complaint_id, user_id),
    CONSTRAINT fk_upvotes_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id),
    CONSTRAINT fk_upvotes_user      FOREIGN KEY (user_id)      REFERENCES users (id)
);
