CREATE TABLE IF NOT EXISTS entry_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    programme_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    requirement_text TEXT NOT NULL,
    subject_requirements TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_entry_requirements_programme_id ON entry_requirements(programme_id);

INSERT INTO entry_requirements (programme_id, requirement_text, subject_requirements)
SELECT c.id,
       'Minimum Grade 12 pass with English plus programme-specific requirements.',
       CASE
           WHEN lower(c.name) LIKE '%engineering%' OR lower(c.name) LIKE '%computer%' OR lower(c.name) LIKE '%data%'
               THEN 'Mathematics and Physical Sciences are commonly required.'
           ELSE 'Check the university faculty prospectus for required school subjects.'
       END
FROM courses c
WHERE NOT EXISTS (
    SELECT 1 FROM entry_requirements er WHERE er.programme_id = c.id
);
