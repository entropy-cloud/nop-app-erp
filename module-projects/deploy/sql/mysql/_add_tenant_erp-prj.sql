
    alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_subject add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project_type add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_activity_type add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_task add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project_user add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_budget add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_cost_collection add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_milestone add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project_pnl add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_timesheet add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_budget_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_cost_collection_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_billing add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project_settlement add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_billing_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project_settlement_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee drop primary key;
alter table erp_md_employee add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_subject drop primary key;
alter table erp_md_subject add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project_type drop primary key;
alter table erp_prj_project_type add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_activity_type drop primary key;
alter table erp_prj_activity_type add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project drop primary key;
alter table erp_prj_project add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_task drop primary key;
alter table erp_prj_task add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project_user drop primary key;
alter table erp_prj_project_user add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_budget drop primary key;
alter table erp_prj_budget add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_cost_collection drop primary key;
alter table erp_prj_cost_collection add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_milestone drop primary key;
alter table erp_prj_milestone add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project_pnl drop primary key;
alter table erp_prj_project_pnl add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_timesheet drop primary key;
alter table erp_prj_timesheet add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_budget_line drop primary key;
alter table erp_prj_budget_line add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_cost_collection_line drop primary key;
alter table erp_prj_cost_collection_line add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_billing drop primary key;
alter table erp_prj_billing add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project_settlement drop primary key;
alter table erp_prj_project_settlement add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_billing_line drop primary key;
alter table erp_prj_billing_line add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project_settlement_line drop primary key;
alter table erp_prj_project_settlement_line add primary key (NOP_TENANT_ID, ID);


