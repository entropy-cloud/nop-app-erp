
    alter table erp_md_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_ticket_type add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_auth_user add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_knowledge_base add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_team add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_sla_policy add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_ticket add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_cs_ticket_action add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_partner drop primary key;
alter table erp_md_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_ticket_type drop primary key;
alter table erp_cs_ticket_type add primary key (NOP_TENANT_ID, ID);

alter table erp_md_auth_user drop primary key;
alter table erp_md_auth_user add primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_organization drop primary key;
alter table erp_md_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_knowledge_base drop primary key;
alter table erp_cs_knowledge_base add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_team drop primary key;
alter table erp_cs_team add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_sla_policy drop primary key;
alter table erp_cs_sla_policy add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_ticket drop primary key;
alter table erp_cs_ticket add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_ticket_action drop primary key;
alter table erp_cs_ticket_action add primary key (NOP_TENANT_ID, ID);


