-- Backfill work_id for legacy t_script_record rows (run after upgrade_v0.0.3_to_v0.0.4.sql).
-- Creates one t_script_work per distinct (user_id, work_title) among orphan records,
-- then links orphan records to the matching work row.

INSERT INTO t_script_work (id, user_id, title, create_time, update_time, deleted)
SELECT
    UUID(),
    grouped.user_id,
    grouped.work_title,
    grouped.first_create_time,
    grouped.last_update_time,
    0
FROM (
    SELECT
        user_id,
        work_title,
        MIN(create_time) AS first_create_time,
        MAX(update_time) AS last_update_time
    FROM t_script_record
    WHERE work_id IS NULL OR work_id = ''
    GROUP BY user_id, work_title
) AS grouped
WHERE NOT EXISTS (
    SELECT 1
    FROM t_script_work existing
    WHERE existing.user_id = grouped.user_id
      AND existing.title = grouped.work_title
      AND existing.deleted = 0
);

UPDATE t_script_record record
INNER JOIN t_script_work work
    ON work.user_id = record.user_id
   AND work.title = record.work_title
   AND work.deleted = 0
SET record.work_id = work.id
WHERE record.work_id IS NULL OR record.work_id = '';
