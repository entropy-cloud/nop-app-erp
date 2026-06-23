
    alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_subject add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project_type add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_activity_type add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_task add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project_user add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_budget add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_cost_collection add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_milestone add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_timesheet add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_budget_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_cost_collection_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_billing add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_billing_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee drop constraint PK_erp_md_employee;
alter table erp_md_employee add constraint PK_erp_md_employee primary key (NOP_TENANT_ID, id);

alter table erp_md_currency drop constraint PK_erp_md_currency;
alter table erp_md_currency add constraint PK_erp_md_currency primary key (NOP_TENANT_ID, id);

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, id);

alter table erp_md_subject drop constraint PK_erp_md_subject;
alter table erp_md_subject add constraint PK_erp_md_subject primary key (NOP_TENANT_ID, id);

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_prj_project_type drop constraint PK_erp_prj_project_type;
alter table erp_prj_project_type add constraint PK_erp_prj_project_type primary key (NOP_TENANT_ID, id);

alter table erp_prj_activity_type drop constraint PK_erp_prj_activity_type;
alter table erp_prj_activity_type add constraint PK_erp_prj_activity_type primary key (NOP_TENANT_ID, id);

alter table erp_prj_project drop constraint PK_erp_prj_project;
alter table erp_prj_project add constraint PK_erp_prj_project primary key (NOP_TENANT_ID, id);

alter table erp_prj_task drop constraint PK_erp_prj_task;
alter table erp_prj_task add constraint PK_erp_prj_task primary key (NOP_TENANT_ID, id);

alter table erp_prj_project_user drop constraint PK_erp_prj_project_user;
alter table erp_prj_project_user add constraint PK_erp_prj_project_user primary key (NOP_TENANT_ID, id);

alter table erp_prj_budget drop constraint PK_erp_prj_budget;
alter table erp_prj_budget add constraint PK_erp_prj_budget primary key (NOP_TENANT_ID, id);

alter table erp_prj_cost_collection drop constraint PK_erp_prj_cost_collection;
alter table erp_prj_cost_collection add constraint PK_erp_prj_cost_collection primary key (NOP_TENANT_ID, id);

alter table erp_prj_milestone drop constraint PK_erp_prj_milestone;
alter table erp_prj_milestone add constraint PK_erp_prj_milestone primary key (NOP_TENANT_ID, id);

alter table erp_prj_timesheet drop constraint PK_erp_prj_timesheet;
alter table erp_prj_timesheet add constraint PK_erp_prj_timesheet primary key (NOP_TENANT_ID, id);

alter table erp_prj_budget_line drop constraint PK_erp_prj_budget_line;
alter table erp_prj_budget_line add constraint PK_erp_prj_budget_line primary key (NOP_TENANT_ID, id);

alter table erp_prj_cost_collection_line drop constraint PK_erp_prj_cost_collection_line;
alter table erp_prj_cost_collection_line add constraint PK_erp_prj_cost_collection_line primary key (NOP_TENANT_ID, id);

alter table erp_prj_billing drop constraint PK_erp_prj_billing;
alter table erp_prj_billing add constraint PK_erp_prj_billing primary key (NOP_TENANT_ID, id);

alter table erp_prj_billing_line drop constraint PK_erp_prj_billing_line;
alter table erp_prj_billing_line add constraint PK_erp_prj_billing_line primary key (NOP_TENANT_ID, id);


