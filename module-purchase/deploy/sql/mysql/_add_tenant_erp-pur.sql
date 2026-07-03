
    alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_settlement_method add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_sku add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_tax_rate add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_bank_account add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_requisition add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_scorecard add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_price_list add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_invoice add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_payment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_requisition_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_rfq add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_scorecard_criteria add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_payment_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_rfq_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_quotation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_scorecard_variable add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_quotation_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_receive add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_receive_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_return add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_invoice_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_return_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_employee drop primary key;
alter table erp_md_employee add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop primary key;
alter table erp_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_md_uom drop primary key;
alter table erp_md_uom add primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project drop primary key;
alter table erp_prj_project add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_md_warehouse drop primary key;
alter table erp_md_warehouse add primary key (NOP_TENANT_ID, ID);

alter table erp_md_settlement_method drop primary key;
alter table erp_md_settlement_method add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material_sku drop primary key;
alter table erp_md_material_sku add primary key (NOP_TENANT_ID, ID);

alter table erp_md_tax_rate drop primary key;
alter table erp_md_tax_rate add primary key (NOP_TENANT_ID, ID);

alter table erp_md_bank_account drop primary key;
alter table erp_md_bank_account add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_requisition drop primary key;
alter table erp_pur_requisition add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_scorecard drop primary key;
alter table erp_pur_supplier_scorecard add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_price_list drop primary key;
alter table erp_pur_supplier_price_list add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_invoice drop primary key;
alter table erp_pur_invoice add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_payment drop primary key;
alter table erp_pur_payment add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_requisition_line drop primary key;
alter table erp_pur_requisition_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_rfq drop primary key;
alter table erp_pur_rfq add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_scorecard_criteria drop primary key;
alter table erp_pur_supplier_scorecard_criteria add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_payment_line drop primary key;
alter table erp_pur_payment_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_rfq_line drop primary key;
alter table erp_pur_rfq_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_quotation drop primary key;
alter table erp_pur_quotation add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_scorecard_variable drop primary key;
alter table erp_pur_supplier_scorecard_variable add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_quotation_line drop primary key;
alter table erp_pur_quotation_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_order drop primary key;
alter table erp_pur_order add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_order_line drop primary key;
alter table erp_pur_order_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_receive drop primary key;
alter table erp_pur_receive add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_receive_line drop primary key;
alter table erp_pur_receive_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_return drop primary key;
alter table erp_pur_return add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_invoice_line drop primary key;
alter table erp_pur_invoice_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_return_line drop primary key;
alter table erp_pur_return_line add primary key (NOP_TENANT_ID, ID);


