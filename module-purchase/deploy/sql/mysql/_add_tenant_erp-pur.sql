
    alter table erp_pur_requisition add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_supplier_price_list add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_invoice add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_payment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_requisition_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_rfq add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_receive add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_invoice_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_payment_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_rfq_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_quotation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_receive_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_return add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_quotation_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_return_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_pur_requisition drop primary key;
alter table erp_pur_requisition add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_supplier_price_list drop primary key;
alter table erp_pur_supplier_price_list add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_order drop primary key;
alter table erp_pur_order add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_invoice drop primary key;
alter table erp_pur_invoice add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_payment drop primary key;
alter table erp_pur_payment add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_requisition_line drop primary key;
alter table erp_pur_requisition_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_rfq drop primary key;
alter table erp_pur_rfq add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_order_line drop primary key;
alter table erp_pur_order_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_receive drop primary key;
alter table erp_pur_receive add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_invoice_line drop primary key;
alter table erp_pur_invoice_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_payment_line drop primary key;
alter table erp_pur_payment_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_rfq_line drop primary key;
alter table erp_pur_rfq_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_quotation drop primary key;
alter table erp_pur_quotation add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_receive_line drop primary key;
alter table erp_pur_receive_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_return drop primary key;
alter table erp_pur_return add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_quotation_line drop primary key;
alter table erp_pur_quotation_line add primary key (NOP_TENANT_ID, ID);

alter table erp_pur_return_line drop primary key;
alter table erp_pur_return_line add primary key (NOP_TENANT_ID, ID);


