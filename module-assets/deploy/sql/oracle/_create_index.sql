-- app-erp-assets indexes generated from model/app-erp-assets.orm.xml
-- Dialect: oracle.
-- This file is maintained separately from _create_erp-assets.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-assets.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (53 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_AST_ASSET_ORG_STATUS ON erp_ast_asset (org_id, status);
CREATE INDEX IDX_AST_ASSET_CATEGORY_ID ON erp_ast_asset (category_id);
CREATE INDEX IDX_AST_ASSET_CURRENCY_ID ON erp_ast_asset (currency_id);
CREATE INDEX IDX_AST_ASSET_DEPARTMENT_ID ON erp_ast_asset (department_id);
CREATE INDEX IDX_AST_ASSET_LOCATION_ID ON erp_ast_asset (location_id);
CREATE INDEX IDX_AST_ASSET_EMPLOYEE_ID ON erp_ast_asset (employee_id);
CREATE INDEX IDX_AST_ASSET_STAFF_ID ON erp_ast_asset (staff_id);
CREATE INDEX IDX_AST_ASSET_CATEGORY_SUBJECT_ID ON erp_ast_asset_category (subject_id);
CREATE INDEX IDX_AST_ASSET_CATEGORY_DEPRECIATION_SUBJECT_ID ON erp_ast_asset_category (depreciation_subject_id);
CREATE INDEX IDX_AST_ASSET_CATEGORY_EXPENSE_SUBJECT_ID ON erp_ast_asset_category (expense_subject_id);
CREATE INDEX IDX_AST_ASSET_CATEGORY_DISPOSAL_GAIN_LOSS_SUBJECT_ID ON erp_ast_asset_category (disposal_gain_loss_subject_id);
CREATE INDEX IDX_AST_ASSET_CATEGORY_CIP_SUBJECT_ID ON erp_ast_asset_category (cip_subject_id);
CREATE INDEX IDX_AST_DEPRECIATION_SCHEDULE_ORG_STATUS ON erp_ast_depreciation_schedule (org_id, status);
CREATE INDEX IDX_AST_DEPRECIATION_SCHEDULE_ORG_BUSINESS_DATE ON erp_ast_depreciation_schedule (org_id, business_date);
CREATE INDEX IDX_AST_DEPRECIATION_SCHEDULE_ASSET_ID ON erp_ast_depreciation_schedule (asset_id);
CREATE INDEX IDX_AST_DEPRECIATION_SCHEDULE_VOUCHER_ID ON erp_ast_depreciation_schedule (voucher_id);
CREATE INDEX IDX_AST_DEPRECIATION_SCHEDULE_CURRENCY_ID ON erp_ast_depreciation_schedule (currency_id);
CREATE INDEX IDX_AST_MOVEMENT_ORG_DOC_STATUS ON erp_ast_movement (org_id, doc_status);
CREATE INDEX IDX_AST_MOVEMENT_ORG_BUSINESS_DATE ON erp_ast_movement (org_id, business_date);
CREATE INDEX IDX_AST_MOVEMENT_ASSET_ID ON erp_ast_movement (asset_id);
CREATE INDEX IDX_AST_MOVEMENT_FROM_DEPARTMENT_ID ON erp_ast_movement (from_department_id);
CREATE INDEX IDX_AST_MOVEMENT_TO_DEPARTMENT_ID ON erp_ast_movement (to_department_id);
CREATE INDEX IDX_AST_MOVEMENT_FROM_STAFF_ID ON erp_ast_movement (from_staff_id);
CREATE INDEX IDX_AST_MOVEMENT_TO_STAFF_ID ON erp_ast_movement (to_staff_id);
CREATE INDEX IDX_AST_MOVEMENT_FROM_LOCATION_ID ON erp_ast_movement (from_location_id);
CREATE INDEX IDX_AST_MOVEMENT_TO_LOCATION_ID ON erp_ast_movement (to_location_id);
CREATE INDEX IDX_AST_MOVEMENT_HANDLER_ID ON erp_ast_movement (handler_id);
CREATE INDEX IDX_AST_MOVEMENT_CURRENCY_ID ON erp_ast_movement (currency_id);
CREATE INDEX IDX_AST_VALUE_ADJUSTMENT_ORG_DOC_STATUS ON erp_ast_value_adjustment (org_id, doc_status);
CREATE INDEX IDX_AST_VALUE_ADJUSTMENT_ORG_BUSINESS_DATE ON erp_ast_value_adjustment (org_id, business_date);
CREATE INDEX IDX_AST_VALUE_ADJUSTMENT_ASSET_ID ON erp_ast_value_adjustment (asset_id);
CREATE INDEX IDX_AST_VALUE_ADJUSTMENT_CURRENCY_ID ON erp_ast_value_adjustment (currency_id);
CREATE INDEX IDX_AST_DISPOSAL_ORG_DOC_STATUS ON erp_ast_disposal (org_id, doc_status);
CREATE INDEX IDX_AST_DISPOSAL_ORG_BUSINESS_DATE ON erp_ast_disposal (org_id, business_date);
CREATE INDEX IDX_AST_DISPOSAL_ASSET_ID ON erp_ast_disposal (asset_id);
CREATE INDEX IDX_AST_DISPOSAL_CURRENCY_ID ON erp_ast_disposal (currency_id);
CREATE INDEX IDX_AST_ASSET_CAPITALIZATION_ORG_DOC_STATUS ON erp_ast_asset_capitalization (org_id, doc_status);
CREATE INDEX IDX_AST_ASSET_CAPITALIZATION_ORG_BUSINESS_DATE ON erp_ast_asset_capitalization (org_id, business_date);
CREATE INDEX IDX_AST_ASSET_CAPITALIZATION_CATEGORY_ID ON erp_ast_asset_capitalization (category_id);
CREATE INDEX IDX_AST_ASSET_CAPITALIZATION_CURRENCY_ID ON erp_ast_asset_capitalization (currency_id);
CREATE INDEX IDX_AST_CIP_ORG_STATUS ON erp_ast_cip (org_id, status);
CREATE INDEX IDX_AST_CIP_ORG_BUSINESS_DATE ON erp_ast_cip (org_id, business_date);
CREATE INDEX IDX_AST_CIP_CATEGORY_ID ON erp_ast_cip (category_id);
CREATE INDEX IDX_AST_CIP_CURRENCY_ID ON erp_ast_cip (currency_id);
CREATE INDEX IDX_AST_CIP_COMPLETED_ASSET_ID ON erp_ast_cip (completed_asset_id);
CREATE INDEX IDX_AST_SPLIT_ORG_DOC_STATUS ON erp_ast_split (org_id, doc_status);
CREATE INDEX IDX_AST_SPLIT_ORG_BUSINESS_DATE ON erp_ast_split (org_id, business_date);
CREATE INDEX IDX_AST_SPLIT_SOURCE_ASSET_ID ON erp_ast_split (source_asset_id);
CREATE INDEX IDX_AST_SPLIT_CURRENCY_ID ON erp_ast_split (currency_id);
CREATE INDEX IDX_AST_MERGE_ORG_DOC_STATUS ON erp_ast_merge (org_id, doc_status);
CREATE INDEX IDX_AST_MERGE_ORG_BUSINESS_DATE ON erp_ast_merge (org_id, business_date);
CREATE INDEX IDX_AST_MERGE_TARGET_ASSET_ID ON erp_ast_merge (target_asset_id);
CREATE INDEX IDX_AST_MERGE_CURRENCY_ID ON erp_ast_merge (currency_id);
