
    alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_parameter add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization drop primary key;
alter table erp_md_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_drp_plan drop primary key;
alter table erp_drp_plan add primary key (NOP_TENANT_ID, ID);

alter table erp_drp_parameter drop primary key;
alter table erp_drp_parameter add primary key (NOP_TENANT_ID, ID);

alter table erp_drp_line drop primary key;
alter table erp_drp_line add primary key (NOP_TENANT_ID, ID);


