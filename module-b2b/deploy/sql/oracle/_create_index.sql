-- app-erp-b2b indexes generated from model/app-erp-b2b.orm.xml
-- Dialect: oracle.
-- This file is maintained separately from _create_erp-b2b.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-b2b.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (22 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_B2B_EDI_DOC_FORMAT_ID ON erp_b2b_edi_doc (format_id);
CREATE INDEX IDX_B2B_EDI_DOC_ATTACHMENT_FILE_ID ON erp_b2b_edi_doc (attachment_file_id);
CREATE INDEX IDX_B2B_ASN_ORG_STATUS ON erp_b2b_asn (org_id, status);
CREATE INDEX IDX_B2B_ASN_SOURCE_EDI_DOC_ID ON erp_b2b_asn (source_edi_doc_id);
CREATE INDEX IDX_B2B_ASN_PARTNER_ID ON erp_b2b_asn (partner_id);
CREATE INDEX IDX_B2B_ASN_LINE_ASN_ID ON erp_b2b_asn_line (asn_id);
CREATE INDEX IDX_B2B_ASN_LINE_MATERIAL_ID ON erp_b2b_asn_line (material_id);
CREATE INDEX IDX_B2B_CODE_MAPPING_PARTNER_ID ON erp_b2b_code_mapping (partner_id);
CREATE INDEX IDX_B2B_EDI_LOG_EDI_DOC_ID ON erp_b2b_edi_log (edi_doc_id);
CREATE INDEX IDX_B2B_PARTNER_PROFILE_ORG_STATUS ON erp_b2b_partner_profile (org_id, status);
CREATE INDEX IDX_B2B_PARTNER_PROFILE_PARTNER_ID ON erp_b2b_partner_profile (partner_id);
CREATE INDEX IDX_B2B_PARTNER_CREDENTIAL_PARTNER_PROFILE_ID ON erp_b2b_partner_credential (partner_profile_id);
CREATE INDEX IDX_B2B_TEST_EXCHANGE_PARTNER_PROFILE_ID ON erp_b2b_test_exchange (partner_profile_id);
CREATE INDEX IDX_B2B_CERTIFICATION_CHECKLIST_PARTNER_PROFILE_ID ON erp_b2b_certification_checklist (partner_profile_id);
CREATE INDEX IDX_B2B_MFT_CONFIG_PARTNER_ID ON erp_b2b_mft_config (partner_id);
CREATE INDEX IDX_B2B_MFT_CONFIG_LOCAL_AS2_ID ON erp_b2b_mft_config (local_as2_id);
CREATE INDEX IDX_B2B_MFT_CONFIG_REMOTE_AS2_ID ON erp_b2b_mft_config (remote_as2_id);
CREATE INDEX IDX_B2B_MFT_CONFIG_CERT_ID ON erp_b2b_mft_config (cert_id);
CREATE INDEX IDX_B2B_MFT_CERTIFICATE_PARTNER_ID ON erp_b2b_mft_certificate (partner_id);
CREATE INDEX IDX_B2B_MFT_LOG_ORG_STATUS ON erp_b2b_mft_log (org_id, status);
CREATE INDEX IDX_B2B_MFT_LOG_CONFIG_ID ON erp_b2b_mft_log (config_id);
CREATE INDEX IDX_B2B_MFT_LOG_MESSAGE_ID ON erp_b2b_mft_log (message_id);
