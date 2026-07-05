-- app-erp-quality indexes generated from model/app-erp-quality.orm.xml
-- Dialect: postgresql.
-- This file is maintained separately from _create_erp-quality.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-quality.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (33 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_QA_INSPECTION_ORG_DOC_STATUS ON erp_qa_inspection (org_id, doc_status);
CREATE INDEX IDX_QA_INSPECTION_ORG_BUSINESS_DATE ON erp_qa_inspection (org_id, business_date);
CREATE INDEX IDX_QA_INSPECTION_MATERIAL_ID ON erp_qa_inspection (material_id);
CREATE INDEX IDX_QA_INSPECTION_TEMPLATE_ID ON erp_qa_inspection (template_id);
CREATE INDEX IDX_QA_INSPECTION_SUPPLIER_ID ON erp_qa_inspection (supplier_id);
CREATE INDEX IDX_QA_INSPECTION_WAREHOUSE_ID ON erp_qa_inspection (warehouse_id);
CREATE INDEX IDX_QA_INSPECTION_INSPECTOR_ID ON erp_qa_inspection (inspector_id);
CREATE INDEX IDX_QA_INSPECTION_LINE_INSPECTION_ID ON erp_qa_inspection_line (inspection_id);
CREATE INDEX IDX_QA_INSPECTION_LINE_PARAMETER_ID ON erp_qa_inspection_line (parameter_id);
CREATE INDEX IDX_QA_INSPECTION_TEMPLATE_MATERIAL_ID ON erp_qa_inspection_template (material_id);
CREATE INDEX IDX_QA_INSPECTION_TEMPLATE_LINE_TEMPLATE_ID ON erp_qa_inspection_template_line (template_id);
CREATE INDEX IDX_QA_NON_CONFORMANCE_STATUS ON erp_qa_non_conformance (status);
CREATE INDEX IDX_QA_NON_CONFORMANCE_MATERIAL_ID ON erp_qa_non_conformance (material_id);
CREATE INDEX IDX_QA_NON_CONFORMANCE_INSPECTION_ID ON erp_qa_non_conformance (inspection_id);
CREATE INDEX IDX_QA_NON_CONFORMANCE_SUPPLIER_ID ON erp_qa_non_conformance (supplier_id);
CREATE INDEX IDX_QA_ACTION_STATUS ON erp_qa_action (status);
CREATE INDEX IDX_QA_ACTION_NCR_ID ON erp_qa_action (ncr_id);
CREATE INDEX IDX_QA_RISK_REGISTER_STATUS ON erp_qa_risk_register (status);
CREATE INDEX IDX_QA_RISK_REGISTER_OWNER_ID ON erp_qa_risk_register (owner_id);
CREATE INDEX IDX_QA_QUALITY_GOAL_STATUS ON erp_qa_quality_goal (status);
CREATE INDEX IDX_QA_QUALITY_GOAL_RESPONSIBLE_PERSON_ID ON erp_qa_quality_goal (responsible_person_id);
CREATE INDEX IDX_QA_REVIEW_ORG_DOC_STATUS ON erp_qa_review (org_id, doc_status);
CREATE INDEX IDX_QA_CALIBRATION_ORG_DOC_STATUS ON erp_qa_calibration (org_id, doc_status);
CREATE INDEX IDX_QA_CALIBRATION_ORG_BUSINESS_DATE ON erp_qa_calibration (org_id, business_date);
CREATE INDEX IDX_QA_RECALL_APPROVE_STATUS ON erp_qa_recall (approve_status);
CREATE INDEX IDX_QA_RECALL_BUSINESS_DATE ON erp_qa_recall (business_date);
CREATE INDEX IDX_QA_RECALL_SOURCE_NCR_ID ON erp_qa_recall (source_ncr_id);
CREATE INDEX IDX_QA_RECALL_MATERIAL_ID ON erp_qa_recall (material_id);
CREATE INDEX IDX_QA_RECALL_BATCH_ID ON erp_qa_recall (batch_id);
CREATE INDEX IDX_QA_RECALL_TARGET_RECALL_ID ON erp_qa_recall_target (recall_id);
CREATE INDEX IDX_QA_RECALL_TARGET_PARTNER_ID ON erp_qa_recall_target (partner_id);
CREATE INDEX IDX_QA_RECALL_TARGET_SALES_DELIVERY_ID ON erp_qa_recall_target (sales_delivery_id);
CREATE INDEX IDX_QA_RECALL_TARGET_GENERATED_RETURN_ID ON erp_qa_recall_target (generated_return_id);
