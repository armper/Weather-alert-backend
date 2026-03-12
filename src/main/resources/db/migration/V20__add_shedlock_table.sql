-- ShedLock table for distributed scheduler coordination.
-- Only one application instance acquires the lock at a time,
-- preventing duplicate scheduled-job executions across replicas.
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
