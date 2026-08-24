INSERT INTO permissions (name, description) VALUES
('INCIDENT_CREATE', 'Create incidents'),
('INCIDENT_READ', 'View incidents'),
('INCIDENT_UPDATE', 'Update incidents'),
('INCIDENT_DELETE', 'Delete incidents'),
('INCIDENT_ASSIGN', 'Assign incidents'),
('INCIDENT_STATUS_UPDATE', 'Update incident status'),
('INCIDENT_SEVERITY_UPDATE', 'Update incident severity'),
('AUDIT_READ', 'View audit logs'),
('USER_READ', 'View users'),
('USER_MANAGE', 'Manage users');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ROLE_USER'
AND p.name IN (
    'INCIDENT_CREATE',
    'INCIDENT_READ',
    'INCIDENT_UPDATE',
    'INCIDENT_STATUS_UPDATE',
    'INCIDENT_SEVERITY_UPDATE'
)
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
    AND rp.permission_id = p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN'
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
    AND rp.permission_id = p.id
);
