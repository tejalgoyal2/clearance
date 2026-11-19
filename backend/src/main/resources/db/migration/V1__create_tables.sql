-- One uploaded dependency file.
-- The counts are stored, not computed: a component's policy status never changes after
-- the scan (reviewer decisions are recorded separately), so the counts never go stale.
CREATE TABLE scan (
    id             BIGSERIAL    PRIMARY KEY,
    project_name   VARCHAR(200) NOT NULL,
    ecosystem      VARCHAR(10)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    approved_count INT          NOT NULL,
    review_count   INT          NOT NULL,
    unknown_count  INT          NOT NULL,
    denied_count   INT          NOT NULL
);

-- One dependency found in a scan, with its licence verdict and the reviewer's decision.
CREATE TABLE component (
    id               BIGSERIAL    PRIMARY KEY,
    scan_id          BIGINT       NOT NULL REFERENCES scan (id),
    name             VARCHAR(300) NOT NULL,
    version          VARCHAR(100),
    raw_license      TEXT,
    license_source   VARCHAR(20),
    spdx_expression  TEXT,
    policy_status    VARCHAR(10)  NOT NULL,
    policy_note      TEXT,
    decision         VARCHAR(10)  NOT NULL,
    decided_by       VARCHAR(100),
    decided_at       TIMESTAMPTZ,
    decision_comment TEXT,
    -- Optimistic locking: stops two reviewers deciding the same component at once.
    -- Named row_version because "version" is the package version.
    row_version      BIGINT       NOT NULL
);

-- The scan page filters components by status ("show me the DENIED ones").
CREATE INDEX component_scan_status_idx ON component (scan_id, policy_status);

-- Who did what, and when. Append-only.
CREATE TABLE audit_event (
    id           BIGSERIAL    PRIMARY KEY,
    scan_id      BIGINT       NOT NULL REFERENCES scan (id),
    component_id BIGINT       REFERENCES component (id),
    actor        VARCHAR(100) NOT NULL,
    action       VARCHAR(20)  NOT NULL,
    detail       TEXT,
    occurred_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX audit_event_scan_idx ON audit_event (scan_id, occurred_at);

-- The application never updates or deletes audit events. This trigger makes the database
-- enforce it too, so a bug, a script or a person with SQL access can't rewrite history.
CREATE FUNCTION reject_audit_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_event is append-only: % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_event_append_only
    BEFORE UPDATE OR DELETE ON audit_event
    FOR EACH ROW EXECUTE FUNCTION reject_audit_change();
