
    alter table erp_sys_notification_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sys_notification add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sys_notification_read add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sys_notification_template drop primary key;
alter table erp_sys_notification_template add primary key (NOP_TENANT_ID, ID);

alter table erp_sys_notification drop primary key;
alter table erp_sys_notification add primary key (NOP_TENANT_ID, ID);

alter table erp_sys_notification_read drop primary key;
alter table erp_sys_notification_read add primary key (NOP_TENANT_ID, ID);


