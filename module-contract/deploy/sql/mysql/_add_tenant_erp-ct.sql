
    alter table erp_md_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_approval_matrix add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_contract_version add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_approval_record add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_rebate_agreement add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_document add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_invoice_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_consumption_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_volume_discount add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_signature_request add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_rebate_tier add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_rebate_accrual add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ct_rebate_settlement add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_partner drop primary key;
alter table erp_md_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_template drop primary key;
alter table erp_ct_template add primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop primary key;
alter table erp_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_approval_matrix drop primary key;
alter table erp_ct_approval_matrix add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_contract drop primary key;
alter table erp_ct_contract add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_contract_line drop primary key;
alter table erp_ct_contract_line add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_contract_version drop primary key;
alter table erp_ct_contract_version add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_approval_record drop primary key;
alter table erp_ct_approval_record add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_rebate_agreement drop primary key;
alter table erp_ct_rebate_agreement add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_document drop primary key;
alter table erp_ct_document add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_invoice_plan drop primary key;
alter table erp_ct_invoice_plan add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_consumption_line drop primary key;
alter table erp_ct_consumption_line add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_volume_discount drop primary key;
alter table erp_ct_volume_discount add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_signature_request drop primary key;
alter table erp_ct_signature_request add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_rebate_tier drop primary key;
alter table erp_ct_rebate_tier add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_rebate_accrual drop primary key;
alter table erp_ct_rebate_accrual add primary key (NOP_TENANT_ID, ID);

alter table erp_ct_rebate_settlement drop primary key;
alter table erp_ct_rebate_settlement add primary key (NOP_TENANT_ID, ID);


