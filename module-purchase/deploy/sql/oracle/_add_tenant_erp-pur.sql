
    alter table erp_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_settlement_method add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_sku add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_tax_rate add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_bank_account add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_requisition add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_scorecard add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_price_list add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_invoice add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_payment add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_requisition_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_rfq add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_scorecard_criteria add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_payment_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_rfq_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_quotation add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_scorecard_variable add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_quotation_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_order add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_order_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_receive add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_receive_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_return add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_invoice_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_return_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_md_employee drop constraint PK_erp_md_employee;
alter table erp_md_employee add constraint PK_erp_md_employee primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop constraint PK_erp_md_material;
alter table erp_md_material add constraint PK_erp_md_material primary key (NOP_TENANT_ID, ID);

alter table erp_md_uom drop constraint PK_erp_md_uom;
alter table erp_md_uom add constraint PK_erp_md_uom primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project drop constraint PK_erp_prj_project;
alter table erp_prj_project add constraint PK_erp_prj_project primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop constraint PK_erp_md_currency;
alter table erp_md_currency add constraint PK_erp_md_currency primary key (NOP_TENANT_ID, ID);

alter table erp_md_warehouse drop constraint PK_erp_md_warehouse;
alter table erp_md_warehouse add constraint PK_erp_md_warehouse primary key (NOP_TENANT_ID, ID);

alter table erp_md_settlement_method drop constraint PK_erp_md_settlement_method;
alter table erp_md_settlement_method add constraint PK_erp_md_settlement_method primary key (NOP_TENANT_ID, ID);

alter table erp_md_material_sku drop constraint PK_erp_md_material_sku;
alter table erp_md_material_sku add constraint PK_erp_md_material_sku primary key (NOP_TENANT_ID, ID);

alter table erp_md_tax_rate drop constraint PK_erp_md_tax_rate;
alter table erp_md_tax_rate add constraint PK_erp_md_tax_rate primary key (NOP_TENANT_ID, ID);

alter table erp_md_bank_account drop constraint PK_erp_md_bank_account;
alter table erp_md_bank_account add constraint PK_erp_md_bank_account primary key (NOP_TENANT_ID, ID);

alter table erp_pur_requisition drop constraint PK_erp_pur_requisition;
alter table erp_pur_requisition add constraint PK_erp_pur_requisition primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_scorecard drop constraint PK_erp_pur_supplier_scorecard;
alter table erp_pur_supplier_scorecard add constraint PK_erp_pur_supplier_scorecard primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_price_list drop constraint PK_erp_pur_supplier_price_list;
alter table erp_pur_supplier_price_list add constraint PK_erp_pur_supplier_price_list primary key (NOP_TENANT_ID, ID);

alter table erp_pur_invoice drop constraint PK_erp_pur_invoice;
alter table erp_pur_invoice add constraint PK_erp_pur_invoice primary key (NOP_TENANT_ID, ID);

alter table erp_pur_payment drop constraint PK_erp_pur_payment;
alter table erp_pur_payment add constraint PK_erp_pur_payment primary key (NOP_TENANT_ID, ID);

alter table erp_pur_requisition_line drop constraint PK_erp_pur_requisition_line;
alter table erp_pur_requisition_line add constraint PK_erp_pur_requisition_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_rfq drop constraint PK_erp_pur_rfq;
alter table erp_pur_rfq add constraint PK_erp_pur_rfq primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_scorecard_criteria drop constraint PK_erp_pur_supplier_scorecard_criteria;
alter table erp_pur_supplier_scorecard_criteria add constraint PK_erp_pur_supplier_scorecard_criteria primary key (NOP_TENANT_ID, ID);

alter table erp_pur_payment_line drop constraint PK_erp_pur_payment_line;
alter table erp_pur_payment_line add constraint PK_erp_pur_payment_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_rfq_line drop constraint PK_erp_pur_rfq_line;
alter table erp_pur_rfq_line add constraint PK_erp_pur_rfq_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_quotation drop constraint PK_erp_pur_quotation;
alter table erp_pur_quotation add constraint PK_erp_pur_quotation primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_scorecard_variable drop constraint PK_erp_pur_supplier_scorecard_variable;
alter table erp_pur_supplier_scorecard_variable add constraint PK_erp_pur_supplier_scorecard_variable primary key (NOP_TENANT_ID, ID);

alter table erp_pur_quotation_line drop constraint PK_erp_pur_quotation_line;
alter table erp_pur_quotation_line add constraint PK_erp_pur_quotation_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_order drop constraint PK_erp_pur_order;
alter table erp_pur_order add constraint PK_erp_pur_order primary key (NOP_TENANT_ID, ID);

alter table erp_pur_order_line drop constraint PK_erp_pur_order_line;
alter table erp_pur_order_line add constraint PK_erp_pur_order_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_receive drop constraint PK_erp_pur_receive;
alter table erp_pur_receive add constraint PK_erp_pur_receive primary key (NOP_TENANT_ID, ID);

alter table erp_pur_receive_line drop constraint PK_erp_pur_receive_line;
alter table erp_pur_receive_line add constraint PK_erp_pur_receive_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_return drop constraint PK_erp_pur_return;
alter table erp_pur_return add constraint PK_erp_pur_return primary key (NOP_TENANT_ID, ID);

alter table erp_pur_invoice_line drop constraint PK_erp_pur_invoice_line;
alter table erp_pur_invoice_line add constraint PK_erp_pur_invoice_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_return_line drop constraint PK_erp_pur_return_line;
alter table erp_pur_return_line add constraint PK_erp_pur_return_line primary key (NOP_TENANT_ID, ID);


