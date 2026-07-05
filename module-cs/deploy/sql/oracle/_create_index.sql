-- app-erp-cs indexes generated from model/app-erp-cs.orm.xml
-- Dialect: oracle.
-- This file is maintained separately from _create_erp-cs.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-cs.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (38 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_CS_TICKET_ORG_DOC_STATUS ON erp_cs_ticket (org_id, doc_status);
CREATE INDEX IDX_CS_TICKET_CUSTOMER_ID ON erp_cs_ticket (customer_id);
CREATE INDEX IDX_CS_TICKET_CONTACT_ID ON erp_cs_ticket (contact_id);
CREATE INDEX IDX_CS_TICKET_TICKET_TYPE_ID ON erp_cs_ticket (ticket_type_id);
CREATE INDEX IDX_CS_TICKET_ASSIGNED_TO_ID ON erp_cs_ticket (assigned_to_id);
CREATE INDEX IDX_CS_TICKET_SLA_POLICY_ID ON erp_cs_ticket (sla_policy_id);
CREATE INDEX IDX_CS_TICKET_CATALOG_ITEM_ID ON erp_cs_ticket (catalog_item_id);
CREATE INDEX IDX_CS_TICKET_TYPE_DEFAULT_SLA_POLICY_ID ON erp_cs_ticket_type (default_sla_policy_id);
CREATE INDEX IDX_CS_SLA_POLICY_TICKET_TYPE_ID ON erp_cs_sla_policy (ticket_type_id);
CREATE INDEX IDX_CS_SLA_POLICY_TEAM_ID ON erp_cs_sla_policy (team_id);
CREATE INDEX IDX_CS_SLA_POLICY_ESCALATION_USER_ID ON erp_cs_sla_policy (escalation_user_id);
CREATE INDEX IDX_CS_TEAM_TEAM_LEADER_ID ON erp_cs_team (team_leader_id);
CREATE INDEX IDX_CS_TICKET_ACTION_TICKET_ID ON erp_cs_ticket_action (ticket_id);
CREATE INDEX IDX_CS_TICKET_ACTION_OPERATOR_ID ON erp_cs_ticket_action (operator_id);
CREATE INDEX IDX_CS_KNOWLEDGE_BASE_CATEGORY_ID ON erp_cs_knowledge_base (category_id);
CREATE INDEX IDX_CS_CANNED_CATEGORY_PARENT_ID ON erp_cs_canned_category (parent_id);
CREATE INDEX IDX_CS_CANNED_RESPONSE_CATEGORY_ID ON erp_cs_canned_response (category_id);
CREATE INDEX IDX_CS_CANNED_RESPONSE_MACRO_TICKET_TYPE_ID ON erp_cs_canned_response (macro_ticket_type_id);
CREATE INDEX IDX_CS_SURVEY_TICKET_ID ON erp_cs_survey (ticket_id);
CREATE INDEX IDX_CS_AGENT_RATE_AGENT_ID ON erp_cs_agent_rate (agent_id);
CREATE INDEX IDX_CS_CONTRACT_ORG_STATUS ON erp_cs_contract (org_id, status);
CREATE INDEX IDX_CS_CONTRACT_PARTNER_ID ON erp_cs_contract (partner_id);
CREATE INDEX IDX_CS_CONTRACT_ATTACHMENT_FILE_ID ON erp_cs_contract (attachment_file_id);
CREATE INDEX IDX_CS_ENTITLEMENT_PARTNER_ID ON erp_cs_entitlement (partner_id);
CREATE INDEX IDX_CS_ENTITLEMENT_CONTRACT_ID ON erp_cs_entitlement (contract_id);
CREATE INDEX IDX_CS_ENTITLEMENT_SLA_POLICY_ID ON erp_cs_entitlement (sla_policy_id);
CREATE INDEX IDX_CS_CATALOG_CATEGORY_PARENT_ID ON erp_cs_catalog_category (parent_id);
CREATE INDEX IDX_CS_SERVICE_CATALOG_ITEM_CATEGORY_ID ON erp_cs_service_catalog_item (category_id);
CREATE INDEX IDX_CS_SERVICE_CATALOG_ITEM_PARENT_ID ON erp_cs_service_catalog_item (parent_id);
CREATE INDEX IDX_CS_SERVICE_CATALOG_ITEM_TICKET_TYPE_ID ON erp_cs_service_catalog_item (ticket_type_id);
CREATE INDEX IDX_CS_SERVICE_CATALOG_ITEM_SLA_POLICY_ID ON erp_cs_service_catalog_item (sla_policy_id);
CREATE INDEX IDX_CS_SERVICE_CATALOG_ITEM_FULFILLMENT_PROCESS_ID ON erp_cs_service_catalog_item (fulfillment_process_id);
CREATE INDEX IDX_CS_CATALOG_FULFILLMENT_CATALOG_ITEM_ID ON erp_cs_catalog_fulfillment (catalog_item_id);
CREATE INDEX IDX_CS_TIME_ENTRY_TICKET_ID ON erp_cs_time_entry (ticket_id);
CREATE INDEX IDX_CS_TIME_ENTRY_AGENT_ID ON erp_cs_time_entry (agent_id);
CREATE INDEX IDX_CS_TIME_ENTRY_APPROVED_BY_ID ON erp_cs_time_entry (approved_by_id);
CREATE INDEX IDX_CS_TIME_ENTRY_PROJECT_ID ON erp_cs_time_entry (project_id);
CREATE INDEX IDX_CS_TIME_ENTRY_TASK_ID ON erp_cs_time_entry (task_id);
