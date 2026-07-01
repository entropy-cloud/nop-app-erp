
    alter table erp_md_md_partner add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_employee add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_material add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_log_carrier add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_log_delivery_window add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_log_carrier_config add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_log_shipment add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_log_shipment_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_log_shipment_parcel add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_log_shipment_log add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_partner drop constraint PK_erp_md_md_partner;
alter table erp_md_md_partner add constraint PK_erp_md_md_partner primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_organization drop constraint PK_erp_md_md_organization;
alter table erp_md_md_organization add constraint PK_erp_md_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_employee drop constraint PK_erp_md_md_employee;
alter table erp_md_md_employee add constraint PK_erp_md_md_employee primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop constraint PK_erp_md_currency;
alter table erp_md_currency add constraint PK_erp_md_currency primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_material drop constraint PK_erp_md_md_material;
alter table erp_md_md_material add constraint PK_erp_md_md_material primary key (NOP_TENANT_ID, ID);

alter table erp_log_carrier drop constraint PK_erp_log_carrier;
alter table erp_log_carrier add constraint PK_erp_log_carrier primary key (NOP_TENANT_ID, ID);

alter table erp_log_delivery_window drop constraint PK_erp_log_delivery_window;
alter table erp_log_delivery_window add constraint PK_erp_log_delivery_window primary key (NOP_TENANT_ID, ID);

alter table erp_log_carrier_config drop constraint PK_erp_log_carrier_config;
alter table erp_log_carrier_config add constraint PK_erp_log_carrier_config primary key (NOP_TENANT_ID, ID);

alter table erp_log_shipment drop constraint PK_erp_log_shipment;
alter table erp_log_shipment add constraint PK_erp_log_shipment primary key (NOP_TENANT_ID, ID);

alter table erp_log_shipment_line drop constraint PK_erp_log_shipment_line;
alter table erp_log_shipment_line add constraint PK_erp_log_shipment_line primary key (NOP_TENANT_ID, ID);

alter table erp_log_shipment_parcel drop constraint PK_erp_log_shipment_parcel;
alter table erp_log_shipment_parcel add constraint PK_erp_log_shipment_parcel primary key (NOP_TENANT_ID, ID);

alter table erp_log_shipment_log drop constraint PK_erp_log_shipment_log;
alter table erp_log_shipment_log add constraint PK_erp_log_shipment_log primary key (NOP_TENANT_ID, ID);


