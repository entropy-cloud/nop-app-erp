-- app-erp-crm indexes generated from model/app-erp-crm.orm.xml
-- Dialect: postgresql.
-- This file is maintained separately from _create_erp-crm.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-crm.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (65 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_CRM_LEAD_ORG_DOC_STATUS ON erp_crm_lead (org_id, doc_status);
CREATE INDEX IDX_CRM_LEAD_PARTNER_ID ON erp_crm_lead (partner_id);
CREATE INDEX IDX_CRM_LEAD_SOURCE_ID ON erp_crm_lead (source_id);
CREATE INDEX IDX_CRM_LEAD_LEAD_STATUS_ID ON erp_crm_lead (lead_status_id);
CREATE INDEX IDX_CRM_LEAD_STAGE_ID ON erp_crm_lead (stage_id);
CREATE INDEX IDX_CRM_LEAD_CAMPAIGN_ID ON erp_crm_lead (campaign_id);
CREATE INDEX IDX_CRM_LEAD_OWNER_ID ON erp_crm_lead (owner_id);
CREATE INDEX IDX_CRM_LEAD_TEAM_ID ON erp_crm_lead (team_id);
CREATE INDEX IDX_CRM_LEAD_LOST_REASON_ID ON erp_crm_lead (lost_reason_id);
CREATE INDEX IDX_CRM_STAGE_TEAM_ID ON erp_crm_stage (team_id);
CREATE INDEX IDX_CRM_TEAM_TEAM_LEADER_ID ON erp_crm_team (team_leader_id);
CREATE INDEX IDX_CRM_EVENT_ORG_STATUS ON erp_crm_event (org_id, status);
CREATE INDEX IDX_CRM_EVENT_EVENT_CATEGORY_ID ON erp_crm_event (event_category_id);
CREATE INDEX IDX_CRM_EVENT_RELATED_LEAD_ID ON erp_crm_event (related_lead_id);
CREATE INDEX IDX_CRM_EVENT_PARTNER_ID ON erp_crm_event (partner_id);
CREATE INDEX IDX_CRM_EVENT_CONTACT_ID ON erp_crm_event (contact_id);
CREATE INDEX IDX_CRM_EVENT_OWNER_ID ON erp_crm_event (owner_id);
CREATE INDEX IDX_CRM_EVENT_PARENT_EVENT_ID ON erp_crm_event (parent_event_id);
CREATE INDEX IDX_CRM_ACTIVITY_LEAD_ID ON erp_crm_activity (lead_id);
CREATE INDEX IDX_CRM_ACTIVITY_OWNER_ID ON erp_crm_activity (owner_id);
CREATE INDEX IDX_CRM_LEAD_CONV_LOG_LEAD_ID ON erp_crm_lead_conv_log (lead_id);
CREATE INDEX IDX_CRM_LEAD_CONV_LOG_FROM_STAGE_ID ON erp_crm_lead_conv_log (from_stage_id);
CREATE INDEX IDX_CRM_LEAD_CONV_LOG_TO_STAGE_ID ON erp_crm_lead_conv_log (to_stage_id);
CREATE INDEX IDX_CRM_LEAD_SCORE_CONFIG_LINE_CONFIG_ID ON erp_crm_lead_score_config_line (config_id);
CREATE INDEX IDX_CRM_LEAD_SCORE_LEAD_ID ON erp_crm_lead_score (lead_id);
CREATE INDEX IDX_CRM_LEAD_SCORE_CONFIG_ID ON erp_crm_lead_score (config_id);
CREATE INDEX IDX_CRM_LEAD_SCORE_LINE_SCORE_ID ON erp_crm_lead_score_line (score_id);
CREATE INDEX IDX_CRM_LEAD_SCORE_LINE_CONFIG_LINE_ID ON erp_crm_lead_score_line (config_line_id);
CREATE INDEX IDX_CRM_FORECAST_PERIOD_ORG_STATUS ON erp_crm_forecast_period (org_id, status);
CREATE INDEX IDX_CRM_FORECAST_PERIOD_ID ON erp_crm_forecast (period_id);
CREATE INDEX IDX_CRM_FORECAST_TERRITORY_ID ON erp_crm_forecast (territory_id);
CREATE INDEX IDX_CRM_FORECAST_TEAM_ID ON erp_crm_forecast (team_id);
CREATE INDEX IDX_CRM_FORECAST_OWNER_ID ON erp_crm_forecast (owner_id);
CREATE INDEX IDX_CRM_FORECAST_CURRENCY_ID ON erp_crm_forecast (currency_id);
CREATE INDEX IDX_CRM_FORECAST_LINE_FORECAST_ID ON erp_crm_forecast_line (forecast_id);
CREATE INDEX IDX_CRM_FORECAST_LINE_LEAD_ID ON erp_crm_forecast_line (lead_id);
CREATE INDEX IDX_CRM_FORECAST_ACCURACY_FORECAST_ID ON erp_crm_forecast_accuracy (forecast_id);
CREATE INDEX IDX_CRM_FORECAST_ACCURACY_PERIOD_ID ON erp_crm_forecast_accuracy (period_id);
CREATE INDEX IDX_CRM_FORECAST_ACCURACY_OWNER_ID ON erp_crm_forecast_accuracy (owner_id);
CREATE INDEX IDX_CRM_FORECAST_ACCURACY_TEAM_ID ON erp_crm_forecast_accuracy (team_id);
CREATE INDEX IDX_CRM_FORECAST_ACCURACY_TERRITORY_ID ON erp_crm_forecast_accuracy (territory_id);
CREATE INDEX IDX_CRM_TERRITORY_PARENT_ID ON erp_crm_territory (parent_id);
CREATE INDEX IDX_CRM_TERRITORY_MANAGER_ID ON erp_crm_territory (manager_id);
CREATE INDEX IDX_CRM_TERRITORY_ASSIGNMENT_RULE_TERRITORY_ID ON erp_crm_territory_assignment_rule (territory_id);
CREATE INDEX IDX_CRM_TERRITORY_ASSIGNMENT_RULE_GROUP_ID ON erp_crm_territory_assignment_rule (group_id);
CREATE INDEX IDX_CRM_QUOTA_TERRITORY_ID ON erp_crm_quota (territory_id);
CREATE INDEX IDX_CRM_QUOTA_TEAM_ID ON erp_crm_quota (team_id);
CREATE INDEX IDX_CRM_QUOTA_OWNER_ID ON erp_crm_quota (owner_id);
CREATE INDEX IDX_CRM_QUOTA_CURRENCY_ID ON erp_crm_quota (currency_id);
CREATE INDEX IDX_CRM_CONFIG_RULE_CONFIGURATOR_ID ON erp_crm_config_rule (configurator_id);
CREATE INDEX IDX_CRM_BUNDLE_PRICING_LINE_BUNDLE_ID ON erp_crm_bundle_pricing_line (bundle_id);
CREATE INDEX IDX_CRM_BUNDLE_PRICING_LINE_PRODUCT_ID ON erp_crm_bundle_pricing_line (product_id);
CREATE INDEX IDX_CRM_PRICE_RULE_PRODUCT_ID ON erp_crm_price_rule (product_id);
CREATE INDEX IDX_CRM_PRICE_RULE_CUSTOMER_ID ON erp_crm_price_rule (customer_id);
CREATE INDEX IDX_CRM_PRICE_RULE_CURRENCY_ID ON erp_crm_price_rule (currency_id);
CREATE INDEX IDX_CRM_SEQUENCE_STEP_SEQUENCE_ID ON erp_crm_sequence_step (sequence_id);
CREATE INDEX IDX_CRM_SEQUENCE_ASSIGNMENT_SEQUENCE_ID ON erp_crm_sequence_assignment (sequence_id);
CREATE INDEX IDX_CRM_LEAD_SEQ_PROGRESS_ORG_STATUS ON erp_crm_lead_seq_progress (org_id, status);
CREATE INDEX IDX_CRM_LEAD_SEQ_PROGRESS_LEAD_ID ON erp_crm_lead_seq_progress (lead_id);
CREATE INDEX IDX_CRM_LEAD_SEQ_PROGRESS_SEQUENCE_ID ON erp_crm_lead_seq_progress (sequence_id);
CREATE INDEX IDX_CRM_LEAD_FUNNEL_TERRITORY_ID ON erp_crm_lead_funnel (territory_id);
CREATE INDEX IDX_CRM_LEAD_FUNNEL_TEAM_ID ON erp_crm_lead_funnel (team_id);
CREATE INDEX IDX_CRM_LEAD_FUNNEL_SOURCE_ID ON erp_crm_lead_funnel (source_id);
CREATE INDEX IDX_CRM_FUNNEL_STAGE_METRICS_FUNNEL_ID ON erp_crm_funnel_stage_metrics (funnel_id);
CREATE INDEX IDX_CRM_FUNNEL_STAGE_METRICS_STAGE_ID ON erp_crm_funnel_stage_metrics (stage_id);
