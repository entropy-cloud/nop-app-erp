
    alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_team add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_canned_category add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_agent_rate add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_contract add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_catalog_category add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_sla_policy add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_knowledge_base add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_ticket_type add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_entitlement add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_canned_response add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_service_catalog_item add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_ticket add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_catalog_fulfillment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_ticket_action add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_survey add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_time_entry add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, id);

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_cs_team drop constraint PK_erp_cs_team;
alter table erp_cs_team add constraint PK_erp_cs_team primary key (NOP_TENANT_ID, id);

alter table erp_cs_canned_category drop constraint PK_erp_cs_canned_category;
alter table erp_cs_canned_category add constraint PK_erp_cs_canned_category primary key (NOP_TENANT_ID, id);

alter table erp_cs_agent_rate drop constraint PK_erp_cs_agent_rate;
alter table erp_cs_agent_rate add constraint PK_erp_cs_agent_rate primary key (NOP_TENANT_ID, id);

alter table erp_cs_contract drop constraint PK_erp_cs_contract;
alter table erp_cs_contract add constraint PK_erp_cs_contract primary key (NOP_TENANT_ID, id);

alter table erp_cs_catalog_category drop constraint PK_erp_cs_catalog_category;
alter table erp_cs_catalog_category add constraint PK_erp_cs_catalog_category primary key (NOP_TENANT_ID, id);

alter table erp_cs_sla_policy drop constraint PK_erp_cs_sla_policy;
alter table erp_cs_sla_policy add constraint PK_erp_cs_sla_policy primary key (NOP_TENANT_ID, id);

alter table erp_cs_knowledge_base drop constraint PK_erp_cs_knowledge_base;
alter table erp_cs_knowledge_base add constraint PK_erp_cs_knowledge_base primary key (NOP_TENANT_ID, id);

alter table erp_cs_ticket_type drop constraint PK_erp_cs_ticket_type;
alter table erp_cs_ticket_type add constraint PK_erp_cs_ticket_type primary key (NOP_TENANT_ID, id);

alter table erp_cs_entitlement drop constraint PK_erp_cs_entitlement;
alter table erp_cs_entitlement add constraint PK_erp_cs_entitlement primary key (NOP_TENANT_ID, id);

alter table erp_cs_canned_response drop constraint PK_erp_cs_canned_response;
alter table erp_cs_canned_response add constraint PK_erp_cs_canned_response primary key (NOP_TENANT_ID, id);

alter table erp_cs_service_catalog_item drop constraint PK_erp_cs_service_catalog_item;
alter table erp_cs_service_catalog_item add constraint PK_erp_cs_service_catalog_item primary key (NOP_TENANT_ID, id);

alter table erp_cs_ticket drop constraint PK_erp_cs_ticket;
alter table erp_cs_ticket add constraint PK_erp_cs_ticket primary key (NOP_TENANT_ID, id);

alter table erp_cs_catalog_fulfillment drop constraint PK_erp_cs_catalog_fulfillment;
alter table erp_cs_catalog_fulfillment add constraint PK_erp_cs_catalog_fulfillment primary key (NOP_TENANT_ID, id);

alter table erp_cs_ticket_action drop constraint PK_erp_cs_ticket_action;
alter table erp_cs_ticket_action add constraint PK_erp_cs_ticket_action primary key (NOP_TENANT_ID, id);

alter table erp_cs_survey drop constraint PK_erp_cs_survey;
alter table erp_cs_survey add constraint PK_erp_cs_survey primary key (NOP_TENANT_ID, id);

alter table erp_cs_time_entry drop constraint PK_erp_cs_time_entry;
alter table erp_cs_time_entry add constraint PK_erp_cs_time_entry primary key (NOP_TENANT_ID, id);


