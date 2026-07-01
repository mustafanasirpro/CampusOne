-- Stable reference data used by registration and local API testing.
INSERT INTO universities (
    id,
    name,
    short_name,
    city,
    website,
    active
)
VALUES (
    '8d9f5b4a-8f3c-4e6b-9a7d-1c2e3f4a5b60',
    'COMSATS University Islamabad',
    'COMSATS',
    'Islamabad',
    'https://www.comsats.edu.pk',
    TRUE
)
ON CONFLICT DO NOTHING;

INSERT INTO departments (
    id,
    university_id,
    name,
    code,
    active
)
VALUES (
    '7c8e4a3b-6d2f-4b5a-8c9e-0f1a2b3c4d50',
    '8d9f5b4a-8f3c-4e6b-9a7d-1c2e3f4a5b60',
    'Computer Science',
    'CS',
    TRUE
)
ON CONFLICT DO NOTHING;
