-- app-erp-contract indexes generated from model/app-erp-contract.orm.xml
-- Dialect: oracle.
-- This file is maintained separately from _create_erp-contract.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-contract.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (30 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_CT_CONTRACT_ORG_STATUS ON erp_ct_contract (org_id, status);
CREATE INDEX IDX_CT_CONTRACT_PARTNER_ID ON erp_ct_contract (partner_id);
CREATE INDEX IDX_CT_CONTRACT_CURRENCY_ID ON erp_ct_contract (currency_id);
CREATE INDEX IDX_CT_CONTRACT_TEMPLATE_ID ON erp_ct_contract (template_id);
CREATE INDEX IDX_CT_CONTRACT_PARENT_CONTRACT_ID ON erp_ct_contract (parent_contract_id);
CREATE INDEX IDX_CT_CONTRACT_ATTACHMENT_FILE_ID ON erp_ct_contract (attachment_file_id);
CREATE INDEX IDX_CT_CONTRACT_LINE_CONTRACT_ID ON erp_ct_contract_line (contract_id);
CREATE INDEX IDX_CT_CONTRACT_LINE_MATERIAL_ID ON erp_ct_contract_line (material_id);
CREATE INDEX IDX_CT_CONTRACT_VERSION_STATUS ON erp_ct_contract_version (status);
CREATE INDEX IDX_CT_CONTRACT_VERSION_CONTRACT_ID ON erp_ct_contract_version (contract_id);
CREATE INDEX IDX_CT_CONTRACT_VERSION_ATTACHMENT_FILE_ID ON erp_ct_contract_version (attachment_file_id);
CREATE INDEX IDX_CT_INVOICE_PLAN_CONTRACT_LINE_ID ON erp_ct_invoice_plan (contract_line_id);
CREATE INDEX IDX_CT_CONSUMPTION_LINE_CONTRACT_LINE_ID ON erp_ct_consumption_line (contract_line_id);
CREATE INDEX IDX_CT_APPROVAL_RECORD_CONTRACT_ID ON erp_ct_approval_record (contract_id);
CREATE INDEX IDX_CT_APPROVAL_RECORD_APPROVAL_MATRIX_ID ON erp_ct_approval_record (approval_matrix_id);
CREATE INDEX IDX_CT_APPROVAL_RECORD_APPROVER_ID ON erp_ct_approval_record (approver_id);
CREATE INDEX IDX_CT_VOLUME_DISCOUNT_CONTRACT_LINE_ID ON erp_ct_volume_discount (contract_line_id);
CREATE INDEX IDX_CT_REBATE_AGREEMENT_ORG_STATUS ON erp_ct_rebate_agreement (org_id, status);
CREATE INDEX IDX_CT_REBATE_AGREEMENT_CONTRACT_ID ON erp_ct_rebate_agreement (contract_id);
CREATE INDEX IDX_CT_REBATE_AGREEMENT_PARTNER_ID ON erp_ct_rebate_agreement (partner_id);
CREATE INDEX IDX_CT_REBATE_TIER_REBATE_AGREEMENT_ID ON erp_ct_rebate_tier (rebate_agreement_id);
CREATE INDEX IDX_CT_REBATE_ACCRUAL_REBATE_AGREEMENT_ID ON erp_ct_rebate_accrual (rebate_agreement_id);
CREATE INDEX IDX_CT_REBATE_SETTLEMENT_ORG_STATUS ON erp_ct_rebate_settlement (org_id, status);
CREATE INDEX IDX_CT_REBATE_SETTLEMENT_REBATE_AGREEMENT_ID ON erp_ct_rebate_settlement (rebate_agreement_id);
CREATE INDEX IDX_CT_SIGNATURE_REQUEST_ORG_STATUS ON erp_ct_signature_request (org_id, status);
CREATE INDEX IDX_CT_SIGNATURE_REQUEST_CONTRACT_VERSION_ID ON erp_ct_signature_request (contract_version_id);
CREATE INDEX IDX_CT_SIGNATURE_REQUEST_PROVIDER_REQUEST_ID ON erp_ct_signature_request (provider_request_id);
CREATE INDEX IDX_CT_SIGNATURE_REQUEST_ATTACHMENT_FILE_ID ON erp_ct_signature_request (attachment_file_id);
CREATE INDEX IDX_CT_DOCUMENT_CONTRACT_ID ON erp_ct_document (contract_id);
CREATE INDEX IDX_CT_DOCUMENT_ATTACHMENT_FILE_ID ON erp_ct_document (attachment_file_id);
