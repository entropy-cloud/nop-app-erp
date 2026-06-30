
    alter table erp_md_md_partner add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_cs_ticket_type add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_auth_user add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_cs_knowledge_base add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_cs_team add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_cs_sla_policy add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_cs_ticket add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_cs_ticket_action add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_partner drop constraint PK_erp_md_md_partner;
alter table erp_md_md_partner add constraint PK_erp_md_md_partner primary key (NOP_TENANT_ID, ID);

alter table erp_cs_ticket_type drop constraint PK_erp_cs_ticket_type;
alter table erp_cs_ticket_type add constraint PK_erp_cs_ticket_type primary key (NOP_TENANT_ID, ID);

alter table erp_md_auth_user drop constraint PK_erp_md_auth_user;
alter table erp_md_auth_user add constraint PK_erp_md_auth_user primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_organization drop constraint PK_erp_md_md_organization;
alter table erp_md_md_organization add constraint PK_erp_md_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_cs_knowledge_base drop constraint PK_erp_cs_knowledge_base;
alter table erp_cs_knowledge_base add constraint PK_erp_cs_knowledge_base primary key (NOP_TENANT_ID, ID);

alter table erp_cs_team drop constraint PK_erp_cs_team;
alter table erp_cs_team add constraint PK_erp_cs_team primary key (NOP_TENANT_ID, ID);

alter table erp_cs_sla_policy drop constraint PK_erp_cs_sla_policy;
alter table erp_cs_sla_policy add constraint PK_erp_cs_sla_policy primary key (NOP_TENANT_ID, ID);

alter table erp_cs_ticket drop constraint PK_erp_cs_ticket;
alter table erp_cs_ticket add constraint PK_erp_cs_ticket primary key (NOP_TENANT_ID, ID);

alter table erp_cs_ticket_action drop constraint PK_erp_cs_ticket_action;
alter table erp_cs_ticket_action add constraint PK_erp_cs_ticket_action primary key (NOP_TENANT_ID, ID);


