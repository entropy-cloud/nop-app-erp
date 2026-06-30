
    alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_parameter add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_safety_stock_calc add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_cross_dock add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_dock_appointment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_lead_time_record add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization drop constraint PK_erp_md_md_organization;
alter table erp_md_md_organization add constraint PK_erp_md_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_drp_plan drop constraint PK_erp_drp_plan;
alter table erp_drp_plan add constraint PK_erp_drp_plan primary key (NOP_TENANT_ID, id);

alter table erp_drp_parameter drop constraint PK_erp_drp_parameter;
alter table erp_drp_parameter add constraint PK_erp_drp_parameter primary key (NOP_TENANT_ID, id);

alter table erp_inv_drp_safety_stock_calc drop constraint PK_erp_inv_drp_safety_stock_calc;
alter table erp_inv_drp_safety_stock_calc add constraint PK_erp_inv_drp_safety_stock_calc primary key (NOP_TENANT_ID, id);

alter table erp_inv_drp_cross_dock drop constraint PK_erp_inv_drp_cross_dock;
alter table erp_inv_drp_cross_dock add constraint PK_erp_inv_drp_cross_dock primary key (NOP_TENANT_ID, id);

alter table erp_inv_drp_dock_appointment drop constraint PK_erp_inv_drp_dock_appointment;
alter table erp_inv_drp_dock_appointment add constraint PK_erp_inv_drp_dock_appointment primary key (NOP_TENANT_ID, id);

alter table erp_inv_drp_lead_time_record drop constraint PK_erp_inv_drp_lead_time_record;
alter table erp_inv_drp_lead_time_record add constraint PK_erp_inv_drp_lead_time_record primary key (NOP_TENANT_ID, id);

alter table erp_drp_line drop constraint PK_erp_drp_line;
alter table erp_drp_line add constraint PK_erp_drp_line primary key (NOP_TENANT_ID, id);


