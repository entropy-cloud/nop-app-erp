
    alter table erp_md_partner add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_source add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_status add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lost_reason add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_event_category add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_quote_template add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table nop_auth_user add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_team add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_campaign add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_score_config add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_forecast_period add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_product_configurator add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_bundle_pricing add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_price_rule add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_sequence add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_territory add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_stage add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_score_config_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_config_rule add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_bundle_pricing_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_sequence_step add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_sequence_assignment add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_forecast add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_territory_assignment_rule add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_quota add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_funnel add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_forecast_accuracy add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_funnel_stage_metrics add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_event add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_activity add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_conv_log add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_score add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_forecast_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_seq_progress add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_score_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, ID);

alter table erp_crm_source drop constraint PK_erp_crm_source;
alter table erp_crm_source add constraint PK_erp_crm_source primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_status drop constraint PK_erp_crm_lead_status;
alter table erp_crm_lead_status add constraint PK_erp_crm_lead_status primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lost_reason drop constraint PK_erp_crm_lost_reason;
alter table erp_crm_lost_reason add constraint PK_erp_crm_lost_reason primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_crm_event_category drop constraint PK_erp_crm_event_category;
alter table erp_crm_event_category add constraint PK_erp_crm_event_category primary key (NOP_TENANT_ID, ID);

alter table erp_crm_quote_template drop constraint PK_erp_crm_quote_template;
alter table erp_crm_quote_template add constraint PK_erp_crm_quote_template primary key (NOP_TENANT_ID, ID);

alter table nop_auth_user drop constraint PK_nop_auth_user;
alter table nop_auth_user add constraint PK_nop_auth_user primary key (NOP_TENANT_ID, ID);

alter table erp_crm_team drop constraint PK_erp_crm_team;
alter table erp_crm_team add constraint PK_erp_crm_team primary key (NOP_TENANT_ID, ID);

alter table erp_crm_campaign drop constraint PK_erp_crm_campaign;
alter table erp_crm_campaign add constraint PK_erp_crm_campaign primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_score_config drop constraint PK_erp_crm_lead_score_config;
alter table erp_crm_lead_score_config add constraint PK_erp_crm_lead_score_config primary key (NOP_TENANT_ID, ID);

alter table erp_crm_forecast_period drop constraint PK_erp_crm_forecast_period;
alter table erp_crm_forecast_period add constraint PK_erp_crm_forecast_period primary key (NOP_TENANT_ID, ID);

alter table erp_crm_product_configurator drop constraint PK_erp_crm_product_configurator;
alter table erp_crm_product_configurator add constraint PK_erp_crm_product_configurator primary key (NOP_TENANT_ID, ID);

alter table erp_crm_bundle_pricing drop constraint PK_erp_crm_bundle_pricing;
alter table erp_crm_bundle_pricing add constraint PK_erp_crm_bundle_pricing primary key (NOP_TENANT_ID, ID);

alter table erp_crm_price_rule drop constraint PK_erp_crm_price_rule;
alter table erp_crm_price_rule add constraint PK_erp_crm_price_rule primary key (NOP_TENANT_ID, ID);

alter table erp_crm_sequence drop constraint PK_erp_crm_sequence;
alter table erp_crm_sequence add constraint PK_erp_crm_sequence primary key (NOP_TENANT_ID, ID);

alter table erp_crm_territory drop constraint PK_erp_crm_territory;
alter table erp_crm_territory add constraint PK_erp_crm_territory primary key (NOP_TENANT_ID, ID);

alter table erp_crm_stage drop constraint PK_erp_crm_stage;
alter table erp_crm_stage add constraint PK_erp_crm_stage primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_score_config_line drop constraint PK_erp_crm_lead_score_config_line;
alter table erp_crm_lead_score_config_line add constraint PK_erp_crm_lead_score_config_line primary key (NOP_TENANT_ID, ID);

alter table erp_crm_config_rule drop constraint PK_erp_crm_config_rule;
alter table erp_crm_config_rule add constraint PK_erp_crm_config_rule primary key (NOP_TENANT_ID, ID);

alter table erp_crm_bundle_pricing_line drop constraint PK_erp_crm_bundle_pricing_line;
alter table erp_crm_bundle_pricing_line add constraint PK_erp_crm_bundle_pricing_line primary key (NOP_TENANT_ID, ID);

alter table erp_crm_sequence_step drop constraint PK_erp_crm_sequence_step;
alter table erp_crm_sequence_step add constraint PK_erp_crm_sequence_step primary key (NOP_TENANT_ID, ID);

alter table erp_crm_sequence_assignment drop constraint PK_erp_crm_sequence_assignment;
alter table erp_crm_sequence_assignment add constraint PK_erp_crm_sequence_assignment primary key (NOP_TENANT_ID, ID);

alter table erp_crm_forecast drop constraint PK_erp_crm_forecast;
alter table erp_crm_forecast add constraint PK_erp_crm_forecast primary key (NOP_TENANT_ID, ID);

alter table erp_crm_territory_assignment_rule drop constraint PK_erp_crm_territory_assignment_rule;
alter table erp_crm_territory_assignment_rule add constraint PK_erp_crm_territory_assignment_rule primary key (NOP_TENANT_ID, ID);

alter table erp_crm_quota drop constraint PK_erp_crm_quota;
alter table erp_crm_quota add constraint PK_erp_crm_quota primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_funnel drop constraint PK_erp_crm_lead_funnel;
alter table erp_crm_lead_funnel add constraint PK_erp_crm_lead_funnel primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead drop constraint PK_erp_crm_lead;
alter table erp_crm_lead add constraint PK_erp_crm_lead primary key (NOP_TENANT_ID, ID);

alter table erp_crm_forecast_accuracy drop constraint PK_erp_crm_forecast_accuracy;
alter table erp_crm_forecast_accuracy add constraint PK_erp_crm_forecast_accuracy primary key (NOP_TENANT_ID, ID);

alter table erp_crm_funnel_stage_metrics drop constraint PK_erp_crm_funnel_stage_metrics;
alter table erp_crm_funnel_stage_metrics add constraint PK_erp_crm_funnel_stage_metrics primary key (NOP_TENANT_ID, ID);

alter table erp_crm_event drop constraint PK_erp_crm_event;
alter table erp_crm_event add constraint PK_erp_crm_event primary key (NOP_TENANT_ID, ID);

alter table erp_crm_activity drop constraint PK_erp_crm_activity;
alter table erp_crm_activity add constraint PK_erp_crm_activity primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_conv_log drop constraint PK_erp_crm_lead_conv_log;
alter table erp_crm_lead_conv_log add constraint PK_erp_crm_lead_conv_log primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_score drop constraint PK_erp_crm_lead_score;
alter table erp_crm_lead_score add constraint PK_erp_crm_lead_score primary key (NOP_TENANT_ID, ID);

alter table erp_crm_forecast_line drop constraint PK_erp_crm_forecast_line;
alter table erp_crm_forecast_line add constraint PK_erp_crm_forecast_line primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_seq_progress drop constraint PK_erp_crm_lead_seq_progress;
alter table erp_crm_lead_seq_progress add constraint PK_erp_crm_lead_seq_progress primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_score_line drop constraint PK_erp_crm_lead_score_line;
alter table erp_crm_lead_score_line add constraint PK_erp_crm_lead_score_line primary key (NOP_TENANT_ID, ID);


