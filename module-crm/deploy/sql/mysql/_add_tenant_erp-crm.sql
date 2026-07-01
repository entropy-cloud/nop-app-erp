
    alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_source add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_status add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lost_reason add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_event_category add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner_contact add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_quote_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_team add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_campaign add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_score_config add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_forecast_period add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_territory add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_product_configurator add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_bundle_pricing add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_sequence add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_price_rule add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_stage add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_score_config_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_forecast add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_territory_assignment_rule add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_quota add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_funnel add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_config_rule add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_bundle_pricing_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_sequence_step add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_sequence_assignment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_forecast_accuracy add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_funnel_stage_metrics add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_event add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_activity add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_conv_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_score add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_forecast_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_seq_progress add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_score_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_source drop primary key;
alter table erp_crm_source add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_status drop primary key;
alter table erp_crm_lead_status add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lost_reason drop primary key;
alter table erp_crm_lost_reason add primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_event_category drop primary key;
alter table erp_crm_event_category add primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner_contact drop primary key;
alter table erp_md_partner_contact add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_quote_template drop primary key;
alter table erp_crm_quote_template add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop primary key;
alter table erp_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_team drop primary key;
alter table erp_crm_team add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_campaign drop primary key;
alter table erp_crm_campaign add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_score_config drop primary key;
alter table erp_crm_lead_score_config add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_forecast_period drop primary key;
alter table erp_crm_forecast_period add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_territory drop primary key;
alter table erp_crm_territory add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_product_configurator drop primary key;
alter table erp_crm_product_configurator add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_bundle_pricing drop primary key;
alter table erp_crm_bundle_pricing add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_sequence drop primary key;
alter table erp_crm_sequence add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_price_rule drop primary key;
alter table erp_crm_price_rule add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_stage drop primary key;
alter table erp_crm_stage add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_score_config_line drop primary key;
alter table erp_crm_lead_score_config_line add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_forecast drop primary key;
alter table erp_crm_forecast add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_territory_assignment_rule drop primary key;
alter table erp_crm_territory_assignment_rule add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_quota drop primary key;
alter table erp_crm_quota add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_funnel drop primary key;
alter table erp_crm_lead_funnel add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_config_rule drop primary key;
alter table erp_crm_config_rule add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_bundle_pricing_line drop primary key;
alter table erp_crm_bundle_pricing_line add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_sequence_step drop primary key;
alter table erp_crm_sequence_step add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_sequence_assignment drop primary key;
alter table erp_crm_sequence_assignment add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead drop primary key;
alter table erp_crm_lead add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_forecast_accuracy drop primary key;
alter table erp_crm_forecast_accuracy add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_funnel_stage_metrics drop primary key;
alter table erp_crm_funnel_stage_metrics add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_event drop primary key;
alter table erp_crm_event add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_activity drop primary key;
alter table erp_crm_activity add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_conv_log drop primary key;
alter table erp_crm_lead_conv_log add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_score drop primary key;
alter table erp_crm_lead_score add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_forecast_line drop primary key;
alter table erp_crm_forecast_line add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_seq_progress drop primary key;
alter table erp_crm_lead_seq_progress add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_score_line drop primary key;
alter table erp_crm_lead_score_line add primary key (NOP_TENANT_ID, ID);


