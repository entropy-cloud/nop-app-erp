
    alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_settlement_method add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_sku add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_tax_rate add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_bank_account add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_price_list add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_quotation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_contract add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_invoice add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_pricing_rule add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_receipt add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_price_list_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_quotation_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_receipt_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_delivery add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_delivery_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_return add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_invoice_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sal_return_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, id);

alter table erp_md_currency drop constraint PK_erp_md_currency;
alter table erp_md_currency add constraint PK_erp_md_currency primary key (NOP_TENANT_ID, id);

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_md_material drop constraint PK_erp_md_material;
alter table erp_md_material add constraint PK_erp_md_material primary key (NOP_TENANT_ID, id);

alter table erp_md_uom drop constraint PK_erp_md_uom;
alter table erp_md_uom add constraint PK_erp_md_uom primary key (NOP_TENANT_ID, id);

alter table erp_md_warehouse drop constraint PK_erp_md_warehouse;
alter table erp_md_warehouse add constraint PK_erp_md_warehouse primary key (NOP_TENANT_ID, id);

alter table erp_md_settlement_method drop constraint PK_erp_md_settlement_method;
alter table erp_md_settlement_method add constraint PK_erp_md_settlement_method primary key (NOP_TENANT_ID, id);

alter table erp_md_material_sku drop constraint PK_erp_md_material_sku;
alter table erp_md_material_sku add constraint PK_erp_md_material_sku primary key (NOP_TENANT_ID, id);

alter table erp_md_tax_rate drop constraint PK_erp_md_tax_rate;
alter table erp_md_tax_rate add constraint PK_erp_md_tax_rate primary key (NOP_TENANT_ID, id);

alter table erp_prj_project drop constraint PK_erp_prj_project;
alter table erp_prj_project add constraint PK_erp_prj_project primary key (NOP_TENANT_ID, id);

alter table erp_md_bank_account drop constraint PK_erp_md_bank_account;
alter table erp_md_bank_account add constraint PK_erp_md_bank_account primary key (NOP_TENANT_ID, id);

alter table erp_sal_price_list drop constraint PK_erp_sal_price_list;
alter table erp_sal_price_list add constraint PK_erp_sal_price_list primary key (NOP_TENANT_ID, id);

alter table erp_sal_quotation drop constraint PK_erp_sal_quotation;
alter table erp_sal_quotation add constraint PK_erp_sal_quotation primary key (NOP_TENANT_ID, id);

alter table erp_sal_contract drop constraint PK_erp_sal_contract;
alter table erp_sal_contract add constraint PK_erp_sal_contract primary key (NOP_TENANT_ID, id);

alter table erp_sal_invoice drop constraint PK_erp_sal_invoice;
alter table erp_sal_invoice add constraint PK_erp_sal_invoice primary key (NOP_TENANT_ID, id);

alter table erp_sal_pricing_rule drop constraint PK_erp_sal_pricing_rule;
alter table erp_sal_pricing_rule add constraint PK_erp_sal_pricing_rule primary key (NOP_TENANT_ID, id);

alter table erp_sal_receipt drop constraint PK_erp_sal_receipt;
alter table erp_sal_receipt add constraint PK_erp_sal_receipt primary key (NOP_TENANT_ID, id);

alter table erp_sal_price_list_line drop constraint PK_erp_sal_price_list_line;
alter table erp_sal_price_list_line add constraint PK_erp_sal_price_list_line primary key (NOP_TENANT_ID, id);

alter table erp_sal_quotation_line drop constraint PK_erp_sal_quotation_line;
alter table erp_sal_quotation_line add constraint PK_erp_sal_quotation_line primary key (NOP_TENANT_ID, id);

alter table erp_sal_order drop constraint PK_erp_sal_order;
alter table erp_sal_order add constraint PK_erp_sal_order primary key (NOP_TENANT_ID, id);

alter table erp_sal_receipt_line drop constraint PK_erp_sal_receipt_line;
alter table erp_sal_receipt_line add constraint PK_erp_sal_receipt_line primary key (NOP_TENANT_ID, id);

alter table erp_sal_order_line drop constraint PK_erp_sal_order_line;
alter table erp_sal_order_line add constraint PK_erp_sal_order_line primary key (NOP_TENANT_ID, id);

alter table erp_sal_delivery drop constraint PK_erp_sal_delivery;
alter table erp_sal_delivery add constraint PK_erp_sal_delivery primary key (NOP_TENANT_ID, id);

alter table erp_sal_delivery_line drop constraint PK_erp_sal_delivery_line;
alter table erp_sal_delivery_line add constraint PK_erp_sal_delivery_line primary key (NOP_TENANT_ID, id);

alter table erp_sal_return drop constraint PK_erp_sal_return;
alter table erp_sal_return add constraint PK_erp_sal_return primary key (NOP_TENANT_ID, id);

alter table erp_sal_invoice_line drop constraint PK_erp_sal_invoice_line;
alter table erp_sal_invoice_line add constraint PK_erp_sal_invoice_line primary key (NOP_TENANT_ID, id);

alter table erp_sal_return_line drop constraint PK_erp_sal_return_line;
alter table erp_sal_return_line add constraint PK_erp_sal_return_line primary key (NOP_TENANT_ID, id);


