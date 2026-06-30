
    alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_drp_plan add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_drp_parameter add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_drp_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization drop constraint PK_erp_md_md_organization;
alter table erp_md_md_organization add constraint PK_erp_md_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_drp_plan drop constraint PK_erp_drp_plan;
alter table erp_drp_plan add constraint PK_erp_drp_plan primary key (NOP_TENANT_ID, ID);

alter table erp_drp_parameter drop constraint PK_erp_drp_parameter;
alter table erp_drp_parameter add constraint PK_erp_drp_parameter primary key (NOP_TENANT_ID, ID);

alter table erp_drp_line drop constraint PK_erp_drp_line;
alter table erp_drp_line add constraint PK_erp_drp_line primary key (NOP_TENANT_ID, ID);


