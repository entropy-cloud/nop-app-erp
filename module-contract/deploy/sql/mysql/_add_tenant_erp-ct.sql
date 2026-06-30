
    alter table erp_md_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract_version add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_invoice_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_consumption_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_partner drop primary key;
alter table erp_md_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_template drop primary key;
alter table erp_ct_template add primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_organization drop primary key;
alter table erp_md_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_contract drop primary key;
alter table erp_ct_contract add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_contract_line drop primary key;
alter table erp_ct_contract_line add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_contract_version drop primary key;
alter table erp_ct_contract_version add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_invoice_plan drop primary key;
alter table erp_ct_invoice_plan add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_consumption_line drop primary key;
alter table erp_ct_consumption_line add primary key (NOP_TENANT_ID, ID);


