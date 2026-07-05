-- app-erp-drp indexes generated from model/app-erp-drp.orm.xml
-- Dialect: postgresql.
-- This file is maintained separately from _create_erp-drp.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-drp.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (24 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_DRP_PLAN_ORG_STATUS ON erp_drp_plan (org_id, status);
CREATE INDEX IDX_DRP_LINE_ORG_STATUS ON erp_drp_line (org_id, status);
CREATE INDEX IDX_DRP_LINE_PLAN_ID ON erp_drp_line (plan_id);
CREATE INDEX IDX_DRP_LINE_MATERIAL_ID ON erp_drp_line (material_id);
CREATE INDEX IDX_DRP_LINE_WAREHOUSE_ID ON erp_drp_line (warehouse_id);
CREATE INDEX IDX_DRP_LINE_SOURCE_WAREHOUSE_ID ON erp_drp_line (source_warehouse_id);
CREATE INDEX IDX_DRP_PARAMETER_WAREHOUSE_ID ON erp_drp_parameter (warehouse_id);
CREATE INDEX IDX_DRP_PARAMETER_MATERIAL_ID ON erp_drp_parameter (material_id);
CREATE INDEX IDX_DRP_PARAMETER_PREFERRED_SOURCE_WAREHOUSE_ID ON erp_drp_parameter (preferred_source_warehouse_id);
CREATE INDEX IDX_DRP_PARAMETER_PREFERRED_SUPPLIER_ID ON erp_drp_parameter (preferred_supplier_id);
CREATE INDEX IDX_INV_DRP_SAFETY_STOCK_CALC_MATERIAL_ID ON erp_inv_drp_safety_stock_calc (material_id);
CREATE INDEX IDX_INV_DRP_SAFETY_STOCK_CALC_WAREHOUSE_ID ON erp_inv_drp_safety_stock_calc (warehouse_id);
CREATE INDEX IDX_INV_DRP_CROSS_DOCK_ORG_STATUS ON erp_inv_drp_cross_dock (org_id, status);
CREATE INDEX IDX_INV_DRP_CROSS_DOCK_DRP_LINE_ID ON erp_inv_drp_cross_dock (drp_line_id);
CREATE INDEX IDX_INV_DRP_CROSS_DOCK_INBOUND_MOVE_ID ON erp_inv_drp_cross_dock (inbound_move_id);
CREATE INDEX IDX_INV_DRP_CROSS_DOCK_OUTBOUND_MOVE_ID ON erp_inv_drp_cross_dock (outbound_move_id);
CREATE INDEX IDX_INV_DRP_CROSS_DOCK_MATERIAL_ID ON erp_inv_drp_cross_dock (material_id);
CREATE INDEX IDX_INV_DRP_CROSS_DOCK_STAGING_LOCATION_ID ON erp_inv_drp_cross_dock (staging_location_id);
CREATE INDEX IDX_INV_DRP_DOCK_APPOINTMENT_ORG_STATUS ON erp_inv_drp_dock_appointment (org_id, status);
CREATE INDEX IDX_INV_DRP_DOCK_APPOINTMENT_WAREHOUSE_ID ON erp_inv_drp_dock_appointment (warehouse_id);
CREATE INDEX IDX_INV_DRP_DOCK_APPOINTMENT_DOCK_ID ON erp_inv_drp_dock_appointment (dock_id);
CREATE INDEX IDX_INV_DRP_DOCK_APPOINTMENT_CROSS_DOCK_ID ON erp_inv_drp_dock_appointment (cross_dock_id);
CREATE INDEX IDX_INV_DRP_LEAD_TIME_RECORD_SUPPLIER_ID ON erp_inv_drp_lead_time_record (supplier_id);
CREATE INDEX IDX_INV_DRP_LEAD_TIME_RECORD_MATERIAL_ID ON erp_inv_drp_lead_time_record (material_id);
