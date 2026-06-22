
    alter table erp_pur_requisition add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_price_list add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_order add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_invoice add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_payment add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_requisition_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_rfq add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_order_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_receive add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_invoice_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_payment_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_rfq_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_quotation add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_receive_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_return add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_quotation_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_return_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_pur_requisition drop constraint PK_erp_pur_requisition;
alter table erp_pur_requisition add constraint PK_erp_pur_requisition primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_price_list drop constraint PK_erp_pur_supplier_price_list;
alter table erp_pur_supplier_price_list add constraint PK_erp_pur_supplier_price_list primary key (NOP_TENANT_ID, ID);

alter table erp_pur_order drop constraint PK_erp_pur_order;
alter table erp_pur_order add constraint PK_erp_pur_order primary key (NOP_TENANT_ID, ID);

alter table erp_pur_invoice drop constraint PK_erp_pur_invoice;
alter table erp_pur_invoice add constraint PK_erp_pur_invoice primary key (NOP_TENANT_ID, ID);

alter table erp_pur_payment drop constraint PK_erp_pur_payment;
alter table erp_pur_payment add constraint PK_erp_pur_payment primary key (NOP_TENANT_ID, ID);

alter table erp_pur_requisition_line drop constraint PK_erp_pur_requisition_line;
alter table erp_pur_requisition_line add constraint PK_erp_pur_requisition_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_rfq drop constraint PK_erp_pur_rfq;
alter table erp_pur_rfq add constraint PK_erp_pur_rfq primary key (NOP_TENANT_ID, ID);

alter table erp_pur_order_line drop constraint PK_erp_pur_order_line;
alter table erp_pur_order_line add constraint PK_erp_pur_order_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_receive drop constraint PK_erp_pur_receive;
alter table erp_pur_receive add constraint PK_erp_pur_receive primary key (NOP_TENANT_ID, ID);

alter table erp_pur_invoice_line drop constraint PK_erp_pur_invoice_line;
alter table erp_pur_invoice_line add constraint PK_erp_pur_invoice_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_payment_line drop constraint PK_erp_pur_payment_line;
alter table erp_pur_payment_line add constraint PK_erp_pur_payment_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_rfq_line drop constraint PK_erp_pur_rfq_line;
alter table erp_pur_rfq_line add constraint PK_erp_pur_rfq_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_quotation drop constraint PK_erp_pur_quotation;
alter table erp_pur_quotation add constraint PK_erp_pur_quotation primary key (NOP_TENANT_ID, ID);

alter table erp_pur_receive_line drop constraint PK_erp_pur_receive_line;
alter table erp_pur_receive_line add constraint PK_erp_pur_receive_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_return drop constraint PK_erp_pur_return;
alter table erp_pur_return add constraint PK_erp_pur_return primary key (NOP_TENANT_ID, ID);

alter table erp_pur_quotation_line drop constraint PK_erp_pur_quotation_line;
alter table erp_pur_quotation_line add constraint PK_erp_pur_quotation_line primary key (NOP_TENANT_ID, ID);

alter table erp_pur_return_line drop constraint PK_erp_pur_return_line;
alter table erp_pur_return_line add constraint PK_erp_pur_return_line primary key (NOP_TENANT_ID, ID);


