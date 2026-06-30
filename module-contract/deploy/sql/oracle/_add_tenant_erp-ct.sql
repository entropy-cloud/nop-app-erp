
    alter table erp_md_md_partner add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_template add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_approval_matrix add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract_version add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_approval_record add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_rebate_agreement add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_document add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_invoice_plan add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_consumption_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_volume_discount add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_signature_request add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_rebate_tier add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_rebate_accrual add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ct_rebate_settlement add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_partner drop constraint PK_erp_md_md_partner;
alter table erp_md_md_partner add constraint PK_erp_md_md_partner primary key (NOP_TENANT_ID, ID);

alter table erp_ct_template drop constraint PK_erp_ct_template;
alter table erp_ct_template add constraint PK_erp_ct_template primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_organization drop constraint PK_erp_md_md_organization;
alter table erp_md_md_organization add constraint PK_erp_md_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_ct_contract drop constraint PK_erp_ct_contract;
alter table erp_ct_contract add constraint PK_erp_ct_contract primary key (NOP_TENANT_ID, ID);

alter table erp_ct_approval_matrix drop constraint PK_erp_ct_approval_matrix;
alter table erp_ct_approval_matrix add constraint PK_erp_ct_approval_matrix primary key (NOP_TENANT_ID, ID);

alter table erp_ct_contract_line drop constraint PK_erp_ct_contract_line;
alter table erp_ct_contract_line add constraint PK_erp_ct_contract_line primary key (NOP_TENANT_ID, ID);

alter table erp_ct_contract_version drop constraint PK_erp_ct_contract_version;
alter table erp_ct_contract_version add constraint PK_erp_ct_contract_version primary key (NOP_TENANT_ID, ID);

alter table erp_ct_approval_record drop constraint PK_erp_ct_approval_record;
alter table erp_ct_approval_record add constraint PK_erp_ct_approval_record primary key (NOP_TENANT_ID, ID);

alter table erp_ct_rebate_agreement drop constraint PK_erp_ct_rebate_agreement;
alter table erp_ct_rebate_agreement add constraint PK_erp_ct_rebate_agreement primary key (NOP_TENANT_ID, ID);

alter table erp_ct_document drop constraint PK_erp_ct_document;
alter table erp_ct_document add constraint PK_erp_ct_document primary key (NOP_TENANT_ID, ID);

alter table erp_ct_invoice_plan drop constraint PK_erp_ct_invoice_plan;
alter table erp_ct_invoice_plan add constraint PK_erp_ct_invoice_plan primary key (NOP_TENANT_ID, ID);

alter table erp_ct_consumption_line drop constraint PK_erp_ct_consumption_line;
alter table erp_ct_consumption_line add constraint PK_erp_ct_consumption_line primary key (NOP_TENANT_ID, ID);

alter table erp_ct_volume_discount drop constraint PK_erp_ct_volume_discount;
alter table erp_ct_volume_discount add constraint PK_erp_ct_volume_discount primary key (NOP_TENANT_ID, ID);

alter table erp_ct_signature_request drop constraint PK_erp_ct_signature_request;
alter table erp_ct_signature_request add constraint PK_erp_ct_signature_request primary key (NOP_TENANT_ID, ID);

alter table erp_ct_rebate_tier drop constraint PK_erp_ct_rebate_tier;
alter table erp_ct_rebate_tier add constraint PK_erp_ct_rebate_tier primary key (NOP_TENANT_ID, ID);

alter table erp_ct_rebate_accrual drop constraint PK_erp_ct_rebate_accrual;
alter table erp_ct_rebate_accrual add constraint PK_erp_ct_rebate_accrual primary key (NOP_TENANT_ID, ID);

alter table erp_ct_rebate_settlement drop constraint PK_erp_ct_rebate_settlement;
alter table erp_ct_rebate_settlement add constraint PK_erp_ct_rebate_settlement primary key (NOP_TENANT_ID, ID);


