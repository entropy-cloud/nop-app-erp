
    alter table erp_sys_notification_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sys_notification add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sys_notification_read add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_sys_notification_template drop constraint PK_erp_sys_notification_template;
alter table erp_sys_notification_template add constraint PK_erp_sys_notification_template primary key (NOP_TENANT_ID, id);

alter table erp_sys_notification drop constraint PK_erp_sys_notification;
alter table erp_sys_notification add constraint PK_erp_sys_notification primary key (NOP_TENANT_ID, id);

alter table erp_sys_notification_read drop constraint PK_erp_sys_notification_read;
alter table erp_sys_notification_read add constraint PK_erp_sys_notification_read primary key (NOP_TENANT_ID, id);


