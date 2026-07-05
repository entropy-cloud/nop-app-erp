-- app-erp-maintenance indexes generated from model/app-erp-maintenance.orm.xml
-- Dialect: postgresql.
-- This file is maintained separately from _create_erp-maintenance.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-maintenance.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (32 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_MNT_EQUIPMENT_ORG_STATUS ON erp_mnt_equipment (org_id, status);
CREATE INDEX IDX_MNT_EQUIPMENT_ASSET_ID ON erp_mnt_equipment (asset_id);
CREATE INDEX IDX_MNT_EQUIPMENT_WORKCENTER_ID ON erp_mnt_equipment (workcenter_id);
CREATE INDEX IDX_MNT_EQUIPMENT_LOCATION_ID ON erp_mnt_equipment (location_id);
CREATE INDEX IDX_MNT_EQUIPMENT_CATEGORY_ID ON erp_mnt_equipment (category_id);
CREATE INDEX IDX_MNT_EQUIPMENT_CATEGORY_PARENT_ID ON erp_mnt_equipment_category (parent_id);
CREATE INDEX IDX_MNT_SCHEDULE_EQUIPMENT_ID ON erp_mnt_schedule (equipment_id);
CREATE INDEX IDX_MNT_VISIT_ORG_STATUS ON erp_mnt_visit (org_id, status);
CREATE INDEX IDX_MNT_VISIT_ORG_BUSINESS_DATE ON erp_mnt_visit (org_id, business_date);
CREATE INDEX IDX_MNT_VISIT_SCHEDULE_ID ON erp_mnt_visit (schedule_id);
CREATE INDEX IDX_MNT_VISIT_EQUIPMENT_ID ON erp_mnt_visit (equipment_id);
CREATE INDEX IDX_MNT_VISIT_TASK_STATUS ON erp_mnt_visit_task (status);
CREATE INDEX IDX_MNT_VISIT_TASK_VISIT_ID ON erp_mnt_visit_task (visit_id);
CREATE INDEX IDX_MNT_REQUEST_STATUS ON erp_mnt_request (status);
CREATE INDEX IDX_MNT_REQUEST_EQUIPMENT_ID ON erp_mnt_request (equipment_id);
CREATE INDEX IDX_MNT_DOWNTIME_ENTRY_EQUIPMENT_ID ON erp_mnt_downtime_entry (equipment_id);
CREATE INDEX IDX_MNT_DOWNTIME_ENTRY_RELATED_JOB_ORDER_ID ON erp_mnt_downtime_entry (related_job_order_id);
CREATE INDEX IDX_MNT_MAINTENANCE_TEAM_LEADER_ID ON erp_mnt_maintenance_team (leader_id);
CREATE INDEX IDX_MNT_MAINTENANCE_TEAM_MEMBER_TEAM_ID ON erp_mnt_maintenance_team_member (team_id);
CREATE INDEX IDX_MNT_MAINTENANCE_TEAM_MEMBER_EMPLOYEE_ID ON erp_mnt_maintenance_team_member (employee_id);
CREATE INDEX IDX_MNT_SPARE_PART_USAGE_ORG_DOC_STATUS ON erp_mnt_spare_part_usage (org_id, doc_status);
CREATE INDEX IDX_MNT_SPARE_PART_USAGE_ORG_BUSINESS_DATE ON erp_mnt_spare_part_usage (org_id, business_date);
CREATE INDEX IDX_MNT_SPARE_PART_USAGE_VISIT_ID ON erp_mnt_spare_part_usage (visit_id);
CREATE INDEX IDX_MNT_SPARE_PART_USAGE_REQUEST_ID ON erp_mnt_spare_part_usage (request_id);
CREATE INDEX IDX_MNT_SPARE_PART_USAGE_EQUIPMENT_ID ON erp_mnt_spare_part_usage (equipment_id);
CREATE INDEX IDX_MNT_SPARE_PART_USAGE_WAREHOUSE_ID ON erp_mnt_spare_part_usage (warehouse_id);
CREATE INDEX IDX_MNT_SPARE_PART_USAGE_LINE_SPARE_PART_USAGE_ID ON erp_mnt_spare_part_usage_line (spare_part_usage_id);
CREATE INDEX IDX_MNT_SPARE_PART_USAGE_LINE_MATERIAL_ID ON erp_mnt_spare_part_usage_line (material_id);
CREATE INDEX IDX_MNT_SPARE_PART_USAGE_LINE_UO_M_ID ON erp_mnt_spare_part_usage_line (uo_m_id);
CREATE INDEX IDX_MNT_CALIBRATION_ORG_DOC_STATUS ON erp_mnt_calibration (org_id, doc_status);
CREATE INDEX IDX_MNT_CALIBRATION_ORG_BUSINESS_DATE ON erp_mnt_calibration (org_id, business_date);
CREATE INDEX IDX_MNT_CALIBRATION_EQUIPMENT_ID ON erp_mnt_calibration (equipment_id);
