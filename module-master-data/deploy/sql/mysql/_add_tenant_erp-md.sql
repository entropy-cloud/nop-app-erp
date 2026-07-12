
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

alter table erp_md_material_sku add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom_conversion add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_category drop primary key;
alter table erp_md_material_category add primary key (NOP_TENANT_ID, ID);

alter table erp_md_uom drop primary key;
alter table erp_md_uom add primary key (NOP_TENANT_ID, ID);

alter table erp_md_tax_rate drop primary key;
alter table erp_md_tax_rate add primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_md_settlement_method drop primary key;
alter table erp_md_settlement_method add primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner_address drop primary key;
alter table erp_md_partner_address add primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner_contact drop primary key;
alter table erp_md_partner_contact add primary key (NOP_TENANT_ID, ID);

alter table erp_md_bank_account drop primary key;
alter table erp_md_bank_account add primary key (NOP_TENANT_ID, ID);

alter table erp_md_exchange_rate drop primary key;
alter table erp_md_exchange_rate add primary key (NOP_TENANT_ID, ID);

alter table erp_md_subject drop primary key;
alter table erp_md_subject add primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_employee drop primary key;
alter table erp_md_employee add primary key (NOP_TENANT_ID, ID);

alter table erp_md_acct_schema drop primary key;
alter table erp_md_acct_schema add primary key (NOP_TENANT_ID, ID);

alter table erp_md_supplier_approval drop primary key;
alter table erp_md_supplier_approval add primary key (NOP_TENANT_ID, ID);

alter table erp_sys_config drop primary key;
alter table erp_sys_config add primary key (NOP_TENANT_ID, ID);

alter table erp_md_warehouse drop primary key;
alter table erp_md_warehouse add primary key (NOP_TENANT_ID, ID);

alter table erp_md_cost_center drop primary key;
alter table erp_md_cost_center add primary key (NOP_TENANT_ID, ID);

alter table erp_md_acct_schema_coa drop primary key;
alter table erp_md_acct_schema_coa add primary key (NOP_TENANT_ID, ID);

alter table erp_md_subject_mapping drop primary key;
alter table erp_md_subject_mapping add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop primary key;
alter table erp_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_md_location drop primary key;
alter table erp_md_location add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material_sku drop primary key;
alter table erp_md_material_sku add primary key (NOP_TENANT_ID, ID);

alter table erp_md_uom_conversion drop primary key;
alter table erp_md_uom_conversion add primary key (NOP_TENANT_ID, ID);


