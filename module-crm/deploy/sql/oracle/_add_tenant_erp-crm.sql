
    alter table erp_md_partner add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_source add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_status add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lost_reason add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_event_category add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_quote_template add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_team add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_campaign add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_stage add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_event add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_activity add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_crm_lead_conv_log add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

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

alter table erp_crm_team drop constraint PK_erp_crm_team;
alter table erp_crm_team add constraint PK_erp_crm_team primary key (NOP_TENANT_ID, ID);

alter table erp_crm_campaign drop constraint PK_erp_crm_campaign;
alter table erp_crm_campaign add constraint PK_erp_crm_campaign primary key (NOP_TENANT_ID, ID);

alter table erp_crm_stage drop constraint PK_erp_crm_stage;
alter table erp_crm_stage add constraint PK_erp_crm_stage primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead drop constraint PK_erp_crm_lead;
alter table erp_crm_lead add constraint PK_erp_crm_lead primary key (NOP_TENANT_ID, ID);

alter table erp_crm_event drop constraint PK_erp_crm_event;
alter table erp_crm_event add constraint PK_erp_crm_event primary key (NOP_TENANT_ID, ID);

alter table erp_crm_activity drop constraint PK_erp_crm_activity;
alter table erp_crm_activity add constraint PK_erp_crm_activity primary key (NOP_TENANT_ID, ID);

alter table erp_crm_lead_conv_log drop constraint PK_erp_crm_lead_conv_log;
alter table erp_crm_lead_conv_log add constraint PK_erp_crm_lead_conv_log primary key (NOP_TENANT_ID, ID);


