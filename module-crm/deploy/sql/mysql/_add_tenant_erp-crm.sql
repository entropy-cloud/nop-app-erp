
    alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_source add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_status add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lost_reason add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_event_category add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_quote_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_team add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_campaign add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_stage add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_event add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_activity add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_conv_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

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

alter table erp_crm_quote_template drop primary key;
alter table erp_crm_quote_template add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_team drop primary key;
alter table erp_crm_team add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_campaign drop primary key;
alter table erp_crm_campaign add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_stage drop primary key;
alter table erp_crm_stage add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead drop primary key;
alter table erp_crm_lead add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_event drop primary key;
alter table erp_crm_event add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_activity drop primary key;
alter table erp_crm_activity add primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_conv_log drop primary key;
alter table erp_crm_lead_conv_log add primary key (NOP_TENANT_ID, ID);


