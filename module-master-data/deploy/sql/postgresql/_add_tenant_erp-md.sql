
    alter table erp_md_material_category add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_tax_rate add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_settlement_method add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner_address add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner_contact add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_bank_account add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_exchange_rate add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_subject add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_acct_schema add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_supplier_approval add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sys_config add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_cost_center add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_acct_schema_coa add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_subject_mapping add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_location add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_customs add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_sku add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom_conversion add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_category drop constraint PK_erp_md_material_category;
alter table erp_md_material_category add constraint PK_erp_md_material_category primary key (NOP_TENANT_ID, id);

alter table erp_md_uom drop constraint PK_erp_md_uom;
alter table erp_md_uom add constraint PK_erp_md_uom primary key (NOP_TENANT_ID, id);

alter table erp_md_tax_rate drop constraint PK_erp_md_tax_rate;
alter table erp_md_tax_rate add constraint PK_erp_md_tax_rate primary key (NOP_TENANT_ID, id);

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, id);

alter table erp_md_currency drop constraint PK_erp_md_currency;
alter table erp_md_currency add constraint PK_erp_md_currency primary key (NOP_TENANT_ID, id);

alter table erp_md_settlement_method drop constraint PK_erp_md_settlement_method;
alter table erp_md_settlement_method add constraint PK_erp_md_settlement_method primary key (NOP_TENANT_ID, id);

alter table erp_md_partner_address drop constraint PK_erp_md_partner_address;
alter table erp_md_partner_address add constraint PK_erp_md_partner_address primary key (NOP_TENANT_ID, id);

alter table erp_md_partner_contact drop constraint PK_erp_md_partner_contact;
alter table erp_md_partner_contact add constraint PK_erp_md_partner_contact primary key (NOP_TENANT_ID, id);

alter table erp_md_bank_account drop constraint PK_erp_md_bank_account;
alter table erp_md_bank_account add constraint PK_erp_md_bank_account primary key (NOP_TENANT_ID, id);

alter table erp_md_exchange_rate drop constraint PK_erp_md_exchange_rate;
alter table erp_md_exchange_rate add constraint PK_erp_md_exchange_rate primary key (NOP_TENANT_ID, id);

alter table erp_md_subject drop constraint PK_erp_md_subject;
alter table erp_md_subject add constraint PK_erp_md_subject primary key (NOP_TENANT_ID, id);

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_md_employee drop constraint PK_erp_md_employee;
alter table erp_md_employee add constraint PK_erp_md_employee primary key (NOP_TENANT_ID, id);

alter table erp_md_acct_schema drop constraint PK_erp_md_acct_schema;
alter table erp_md_acct_schema add constraint PK_erp_md_acct_schema primary key (NOP_TENANT_ID, id);

alter table erp_md_supplier_approval drop constraint PK_erp_md_supplier_approval;
alter table erp_md_supplier_approval add constraint PK_erp_md_supplier_approval primary key (NOP_TENANT_ID, id);

alter table erp_sys_config drop constraint PK_erp_sys_config;
alter table erp_sys_config add constraint PK_erp_sys_config primary key (NOP_TENANT_ID, id);

alter table erp_md_warehouse drop constraint PK_erp_md_warehouse;
alter table erp_md_warehouse add constraint PK_erp_md_warehouse primary key (NOP_TENANT_ID, id);

alter table erp_md_cost_center drop constraint PK_erp_md_cost_center;
alter table erp_md_cost_center add constraint PK_erp_md_cost_center primary key (NOP_TENANT_ID, id);

alter table erp_md_acct_schema_coa drop constraint PK_erp_md_acct_schema_coa;
alter table erp_md_acct_schema_coa add constraint PK_erp_md_acct_schema_coa primary key (NOP_TENANT_ID, id);

alter table erp_md_subject_mapping drop constraint PK_erp_md_subject_mapping;
alter table erp_md_subject_mapping add constraint PK_erp_md_subject_mapping primary key (NOP_TENANT_ID, id);

alter table erp_md_material drop constraint PK_erp_md_material;
alter table erp_md_material add constraint PK_erp_md_material primary key (NOP_TENANT_ID, id);

alter table erp_md_location drop constraint PK_erp_md_location;
alter table erp_md_location add constraint PK_erp_md_location primary key (NOP_TENANT_ID, id);

alter table erp_md_material_customs drop constraint PK_erp_md_material_customs;
alter table erp_md_material_customs add constraint PK_erp_md_material_customs primary key (NOP_TENANT_ID, id);

alter table erp_md_material_sku drop constraint PK_erp_md_material_sku;
alter table erp_md_material_sku add constraint PK_erp_md_material_sku primary key (NOP_TENANT_ID, id);

alter table erp_md_uom_conversion drop constraint PK_erp_md_uom_conversion;
alter table erp_md_uom_conversion add constraint PK_erp_md_uom_conversion primary key (NOP_TENANT_ID, id);


