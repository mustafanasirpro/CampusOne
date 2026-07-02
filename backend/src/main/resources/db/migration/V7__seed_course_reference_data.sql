-- Stable Computer Science course data used by Notes and local API testing.
INSERT INTO courses (
    id,
    department_id,
    course_code,
    title,
    recommended_semester,
    active
)
VALUES
    (
        'a1100000-0000-4000-8000-000000000001',
        '7c8e4a3b-6d2f-4b5a-8c9e-0f1a2b3c4d50',
        'CSC101',
        'Programming Fundamentals',
        1,
        TRUE
    ),
    (
        'a1100000-0000-4000-8000-000000000002',
        '7c8e4a3b-6d2f-4b5a-8c9e-0f1a2b3c4d50',
        'CSC102',
        'Object Oriented Programming',
        2,
        TRUE
    ),
    (
        'a1100000-0000-4000-8000-000000000003',
        '7c8e4a3b-6d2f-4b5a-8c9e-0f1a2b3c4d50',
        'CSC201',
        'Data Structures and Algorithms',
        3,
        TRUE
    ),
    (
        'a1100000-0000-4000-8000-000000000004',
        '7c8e4a3b-6d2f-4b5a-8c9e-0f1a2b3c4d50',
        'CSC301',
        'Database Systems',
        4,
        TRUE
    ),
    (
        'a1100000-0000-4000-8000-000000000005',
        '7c8e4a3b-6d2f-4b5a-8c9e-0f1a2b3c4d50',
        'CSC302',
        'Operating Systems',
        4,
        TRUE
    )
ON CONFLICT DO NOTHING;
