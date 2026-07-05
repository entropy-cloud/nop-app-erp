-- app-erp-aps indexes generated from model/app-erp-aps.orm.xml
-- Dialect: mysql.
-- This file is maintained separately from _create_erp-aps.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-aps.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (11 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_APS_OPERATION_ORDER_ORG_STATUS ON erp_aps_operation_order (org_id, status);
CREATE INDEX IDX_APS_OPERATION_ORDER_WORK_ORDER_ID ON erp_aps_operation_order (work_order_id);
CREATE INDEX IDX_APS_OPERATION_ORDER_MACHINE_ID ON erp_aps_operation_order (machine_id);
CREATE INDEX IDX_APS_OPERATION_ORDER_ASSIGNED_TO_ID ON erp_aps_operation_order (assigned_to_id);
CREATE INDEX IDX_APS_SCHEDULE_ORG_STATUS ON erp_aps_schedule (org_id, status);
CREATE INDEX IDX_APS_CONSTRAINT_MACHINE_ID ON erp_aps_constraint (machine_id);
CREATE INDEX IDX_APS_OP_ROUTING_OPERATION_ID ON erp_aps_op_routing (operation_id);
CREATE INDEX IDX_APS_OP_ROUTING_MACHINE_ID ON erp_aps_op_routing (machine_id);
CREATE INDEX IDX_APS_DISPATCH_RULE_WORKCENTER_ID ON erp_aps_dispatch_rule (workcenter_id);
CREATE INDEX IDX_APS_DISPATCH_LOG_OPERATION_ORDER_ID ON erp_aps_dispatch_log (operation_order_id);
CREATE INDEX IDX_APS_DISPATCH_LOG_WORKCENTER_ID ON erp_aps_dispatch_log (workcenter_id);
