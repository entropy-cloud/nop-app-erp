-- Advance NOP_SYS_SEQUENCE default row's NEXT_VALUE above seed-id range.
-- Runs in DataInitInitializer.executeSqlFiles() AFTER CSV load but BEFORE
-- SysSequenceGenerator.lazyInit() (ioc:delay-method, runs after all beans start),
-- so the table is empty here: MERGE inserts the default row. addDefaultSequence()'s
-- if(!exists) guard then sees the row exists and skips, preserving NEXT_VALUE=100000.
-- zz- prefix sorts after all CSV-named resources. Plan 2026-07-09-0814-1.
MERGE INTO NOP_SYS_SEQUENCE (
    SEQ_NAME, SEQ_TYPE, IS_UUID, NEXT_VALUE, STEP_SIZE, CACHE_SIZE,
    DEL_FLAG, VERSION, CREATED_BY, CREATE_TIME, UPDATED_BY, UPDATE_TIME
) KEY(SEQ_NAME)
VALUES (
    'default', 'seq', 0, 100000, 1, 100,
    0, 0, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP
)
