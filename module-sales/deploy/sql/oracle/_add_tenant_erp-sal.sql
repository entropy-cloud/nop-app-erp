
    alter table erp_sal_quotation add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_contract add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_order add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_invoice add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_receipt add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_quotation_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_order_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_delivery add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_invoice_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_receipt_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_delivery_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_return add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_return_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_sal_quotation drop constraint PK_erp_sal_quotation;
alter table erp_sal_quotation add constraint PK_erp_sal_quotation primary key (NOP_TENANT_ID, ID);

alter table erp_sal_contract drop constraint PK_erp_sal_contract;
alter table erp_sal_contract add constraint PK_erp_sal_contract primary key (NOP_TENANT_ID, ID);

alter table erp_sal_order drop constraint PK_erp_sal_order;
alter table erp_sal_order add constraint PK_erp_sal_order primary key (NOP_TENANT_ID, ID);

alter table erp_sal_invoice drop constraint PK_erp_sal_invoice;
alter table erp_sal_invoice add constraint PK_erp_sal_invoice primary key (NOP_TENANT_ID, ID);

alter table erp_sal_receipt drop constraint PK_erp_sal_receipt;
alter table erp_sal_receipt add constraint PK_erp_sal_receipt primary key (NOP_TENANT_ID, ID);

alter table erp_sal_quotation_line drop constraint PK_erp_sal_quotation_line;
alter table erp_sal_quotation_line add constraint PK_erp_sal_quotation_line primary key (NOP_TENANT_ID, ID);

alter table erp_sal_order_line drop constraint PK_erp_sal_order_line;
alter table erp_sal_order_line add constraint PK_erp_sal_order_line primary key (NOP_TENANT_ID, ID);

alter table erp_sal_delivery drop constraint PK_erp_sal_delivery;
alter table erp_sal_delivery add constraint PK_erp_sal_delivery primary key (NOP_TENANT_ID, ID);

alter table erp_sal_invoice_line drop constraint PK_erp_sal_invoice_line;
alter table erp_sal_invoice_line add constraint PK_erp_sal_invoice_line primary key (NOP_TENANT_ID, ID);

alter table erp_sal_receipt_line drop constraint PK_erp_sal_receipt_line;
alter table erp_sal_receipt_line add constraint PK_erp_sal_receipt_line primary key (NOP_TENANT_ID, ID);

alter table erp_sal_delivery_line drop constraint PK_erp_sal_delivery_line;
alter table erp_sal_delivery_line add constraint PK_erp_sal_delivery_line primary key (NOP_TENANT_ID, ID);

alter table erp_sal_return drop constraint PK_erp_sal_return;
alter table erp_sal_return add constraint PK_erp_sal_return primary key (NOP_TENANT_ID, ID);

alter table erp_sal_return_line drop constraint PK_erp_sal_return_line;
alter table erp_sal_return_line add constraint PK_erp_sal_return_line primary key (NOP_TENANT_ID, ID);


