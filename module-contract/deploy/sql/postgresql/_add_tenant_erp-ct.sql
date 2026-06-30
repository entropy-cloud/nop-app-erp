
    alter table erp_md_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract_version add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_invoice_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_consumption_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_partner drop constraint PK_erp_md_md_partner;
alter table erp_md_md_partner add constraint PK_erp_md_md_partner primary key (NOP_TENANT_ID, id);

alter table erp_ct_template drop constraint PK_erp_ct_template;
alter table erp_ct_template add constraint PK_erp_ct_template primary key (NOP_TENANT_ID, id);

alter table erp_md_md_organization drop constraint PK_erp_md_md_organization;
alter table erp_md_md_organization add constraint PK_erp_md_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_ct_contract drop constraint PK_erp_ct_contract;
alter table erp_ct_contract add constraint PK_erp_ct_contract primary key (NOP_TENANT_ID, id);

alter table erp_ct_contract_line drop constraint PK_erp_ct_contract_line;
alter table erp_ct_contract_line add constraint PK_erp_ct_contract_line primary key (NOP_TENANT_ID, id);

alter table erp_ct_contract_version drop constraint PK_erp_ct_contract_version;
alter table erp_ct_contract_version add constraint PK_erp_ct_contract_version primary key (NOP_TENANT_ID, id);

alter table erp_ct_invoice_plan drop constraint PK_erp_ct_invoice_plan;
alter table erp_ct_invoice_plan add constraint PK_erp_ct_invoice_plan primary key (NOP_TENANT_ID, id);

alter table erp_ct_consumption_line drop constraint PK_erp_ct_consumption_line;
alter table erp_ct_consumption_line add constraint PK_erp_ct_consumption_line primary key (NOP_TENANT_ID, id);


