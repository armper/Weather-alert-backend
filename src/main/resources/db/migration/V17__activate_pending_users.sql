UPDATE users
SET approval_status = 'ACTIVE',
    approved_at = COALESCE(approved_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE approval_status = 'PENDING_APPROVAL';
