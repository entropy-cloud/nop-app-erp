
    alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_settlement_method add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_sku add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_tax_rate add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_bank_account add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_quotation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_contract add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_invoice add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_receipt add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_quotation_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_invoice_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_receipt_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_delivery add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_delivery_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_return add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_return_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop primary key;
alter table erp_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_md_uom drop primary key;
alter table erp_md_uom add primary key (NOP_TENANT_ID, ID);

alter table erp_md_warehouse drop primary key;
alter table erp_md_warehouse add primary key (NOP_TENANT_ID, ID);

alter table erp_md_settlement_method drop primary key;
alter table erp_md_settlement_method add primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material_sku drop primary key;
alter table erp_md_material_sku add primary key (NOP_TENANT_ID, ID);

alter table erp_md_tax_rate drop primary key;
alter table erp_md_tax_rate add primary key (NOP_TENANT_ID, ID);

alter table erp_md_bank_account drop primary key;
alter table erp_md_bank_account add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_quotation drop primary key;
alter table erp_sal_quotation add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_contract drop primary key;
alter table erp_sal_contract add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_invoice drop primary key;
alter table erp_sal_invoice add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_receipt drop primary key;
alter table erp_sal_receipt add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_order drop primary key;
alter table erp_sal_order add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_quotation_line drop primary key;
alter table erp_sal_quotation_line add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_invoice_line drop primary key;
alter table erp_sal_invoice_line add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_receipt_line drop primary key;
alter table erp_sal_receipt_line add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_order_line drop primary key;
alter table erp_sal_order_line add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_delivery drop primary key;
alter table erp_sal_delivery add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_delivery_line drop primary key;
alter table erp_sal_delivery_line add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_return drop primary key;
alter table erp_sal_return add primary key (NOP_TENANT_ID, ID);

alter table erp_sal_return_line drop primary key;
alter table erp_sal_return_line add primary key (NOP_TENANT_ID, ID);


