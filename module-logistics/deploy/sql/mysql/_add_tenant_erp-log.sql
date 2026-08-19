
    alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_log_carrier add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_log_delivery_window add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_log_carrier_config add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_log_shipment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_log_shipment_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_log_shipment_parcel add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_log_shipment_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_log_delivery_booking add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_employee drop primary key;
alter table erp_md_employee add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop primary key;
alter table erp_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_log_carrier drop primary key;
alter table erp_log_carrier add primary key (NOP_TENANT_ID, ID);

alter table erp_log_delivery_window drop primary key;
alter table erp_log_delivery_window add primary key (NOP_TENANT_ID, ID);

alter table erp_log_carrier_config drop primary key;
alter table erp_log_carrier_config add primary key (NOP_TENANT_ID, ID);

alter table erp_log_shipment drop primary key;
alter table erp_log_shipment add primary key (NOP_TENANT_ID, ID);

alter table erp_log_shipment_line drop primary key;
alter table erp_log_shipment_line add primary key (NOP_TENANT_ID, ID);

alter table erp_log_shipment_parcel drop primary key;
alter table erp_log_shipment_parcel add primary key (NOP_TENANT_ID, ID);

alter table erp_log_shipment_log drop primary key;
alter table erp_log_shipment_log add primary key (NOP_TENANT_ID, ID);

alter table erp_log_delivery_booking drop primary key;
alter table erp_log_delivery_booking add primary key (NOP_TENANT_ID, ID);

alter table erp_log_shipment drop constraint UK_LOG_SHIPMENT_TRACKING_CARRIER;
alter table erp_log_shipment add constraint UK_LOG_SHIPMENT_TRACKING_CARRIER unique (NOP_TENANT_ID,TRACKING_NO,CARRIER_ID,DEL_VERSION);

                alter table erp_log_delivery_booking drop constraint UK_LOG_DELIVERY_BOOKING_SHIPMENT;
alter table erp_log_delivery_booking add constraint UK_LOG_DELIVERY_BOOKING_SHIPMENT unique (NOP_TENANT_ID,SHIPMENT_ID,DEL_VERSION);

                
