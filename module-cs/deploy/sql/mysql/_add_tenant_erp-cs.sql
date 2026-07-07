
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

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_team drop primary key;
alter table erp_cs_team add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_canned_category drop primary key;
alter table erp_cs_canned_category add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_agent_rate drop primary key;
alter table erp_cs_agent_rate add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_contract drop primary key;
alter table erp_cs_contract add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_catalog_category drop primary key;
alter table erp_cs_catalog_category add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_sla_policy drop primary key;
alter table erp_cs_sla_policy add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_knowledge_base drop primary key;
alter table erp_cs_knowledge_base add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_ticket_type drop primary key;
alter table erp_cs_ticket_type add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_entitlement drop primary key;
alter table erp_cs_entitlement add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_canned_response drop primary key;
alter table erp_cs_canned_response add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_service_catalog_item drop primary key;
alter table erp_cs_service_catalog_item add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_ticket drop primary key;
alter table erp_cs_ticket add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_catalog_fulfillment drop primary key;
alter table erp_cs_catalog_fulfillment add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_ticket_action drop primary key;
alter table erp_cs_ticket_action add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_survey drop primary key;
alter table erp_cs_survey add primary key (NOP_TENANT_ID, ID);

alter table erp_cs_time_entry drop primary key;
alter table erp_cs_time_entry add primary key (NOP_TENANT_ID, ID);


