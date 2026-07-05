
    alter table erp_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_aps_operation_order add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_aps_schedule add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_aps_constraint add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_aps_op_routing add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_aps_dispatch_rule add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_aps_dispatch_log add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_aps_operation_order drop constraint PK_erp_aps_operation_order;
alter table erp_aps_operation_order add constraint PK_erp_aps_operation_order primary key (NOP_TENANT_ID, ID);

alter table erp_aps_schedule drop constraint PK_erp_aps_schedule;
alter table erp_aps_schedule add constraint PK_erp_aps_schedule primary key (NOP_TENANT_ID, ID);

alter table erp_aps_constraint drop constraint PK_erp_aps_constraint;
alter table erp_aps_constraint add constraint PK_erp_aps_constraint primary key (NOP_TENANT_ID, ID);

alter table erp_aps_op_routing drop constraint PK_erp_aps_op_routing;
alter table erp_aps_op_routing add constraint PK_erp_aps_op_routing primary key (NOP_TENANT_ID, ID);

alter table erp_aps_dispatch_rule drop constraint PK_erp_aps_dispatch_rule;
alter table erp_aps_dispatch_rule add constraint PK_erp_aps_dispatch_rule primary key (NOP_TENANT_ID, ID);

alter table erp_aps_dispatch_log drop constraint PK_erp_aps_dispatch_log;
alter table erp_aps_dispatch_log add constraint PK_erp_aps_dispatch_log primary key (NOP_TENANT_ID, ID);


