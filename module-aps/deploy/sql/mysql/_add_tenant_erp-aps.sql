
    alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_aps_operation_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_aps_schedule add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_aps_constraint add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_aps_op_routing add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_aps_dispatch_rule add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_aps_dispatch_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization drop primary key;
alter table erp_md_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_aps_operation_order drop primary key;
alter table erp_aps_operation_order add primary key (NOP_TENANT_ID, ID);

alter table erp_aps_schedule drop primary key;
alter table erp_aps_schedule add primary key (NOP_TENANT_ID, ID);

alter table erp_aps_constraint drop primary key;
alter table erp_aps_constraint add primary key (NOP_TENANT_ID, ID);

alter table erp_aps_op_routing drop primary key;
alter table erp_aps_op_routing add primary key (NOP_TENANT_ID, ID);

alter table erp_aps_dispatch_rule drop primary key;
alter table erp_aps_dispatch_rule add primary key (NOP_TENANT_ID, ID);

alter table erp_aps_dispatch_log drop primary key;
alter table erp_aps_dispatch_log add primary key (NOP_TENANT_ID, ID);


