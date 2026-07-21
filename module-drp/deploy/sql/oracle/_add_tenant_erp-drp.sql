
    alter table erp_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_location add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_move add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_drp_plan add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_safety_stock_calc add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_drp_parameter add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_lead_time_record add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_drp_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_drp_scenario add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_cross_dock add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_drp_scenario_version add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_drp_scenario_param add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_dock_appointment add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop constraint PK_erp_md_material;
alter table erp_md_material add constraint PK_erp_md_material primary key (NOP_TENANT_ID, ID);

alter table erp_md_warehouse drop constraint PK_erp_md_warehouse;
alter table erp_md_warehouse add constraint PK_erp_md_warehouse primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, ID);

alter table erp_md_location drop constraint PK_erp_md_location;
alter table erp_md_location add constraint PK_erp_md_location primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_move drop constraint PK_erp_inv_stock_move;
alter table erp_inv_stock_move add constraint PK_erp_inv_stock_move primary key (NOP_TENANT_ID, ID);

alter table erp_drp_plan drop constraint PK_erp_drp_plan;
alter table erp_drp_plan add constraint PK_erp_drp_plan primary key (NOP_TENANT_ID, ID);

alter table erp_inv_drp_safety_stock_calc drop constraint PK_erp_inv_drp_safety_stock_calc;
alter table erp_inv_drp_safety_stock_calc add constraint PK_erp_inv_drp_safety_stock_calc primary key (NOP_TENANT_ID, ID);

alter table erp_drp_parameter drop constraint PK_erp_drp_parameter;
alter table erp_drp_parameter add constraint PK_erp_drp_parameter primary key (NOP_TENANT_ID, ID);

alter table erp_inv_drp_lead_time_record drop constraint PK_erp_inv_drp_lead_time_record;
alter table erp_inv_drp_lead_time_record add constraint PK_erp_inv_drp_lead_time_record primary key (NOP_TENANT_ID, ID);

alter table erp_drp_line drop constraint PK_erp_drp_line;
alter table erp_drp_line add constraint PK_erp_drp_line primary key (NOP_TENANT_ID, ID);

alter table erp_drp_scenario drop constraint PK_erp_drp_scenario;
alter table erp_drp_scenario add constraint PK_erp_drp_scenario primary key (NOP_TENANT_ID, ID);

alter table erp_inv_drp_cross_dock drop constraint PK_erp_inv_drp_cross_dock;
alter table erp_inv_drp_cross_dock add constraint PK_erp_inv_drp_cross_dock primary key (NOP_TENANT_ID, ID);

alter table erp_drp_scenario_version drop constraint PK_erp_drp_scenario_version;
alter table erp_drp_scenario_version add constraint PK_erp_drp_scenario_version primary key (NOP_TENANT_ID, ID);

alter table erp_drp_scenario_param drop constraint PK_erp_drp_scenario_param;
alter table erp_drp_scenario_param add constraint PK_erp_drp_scenario_param primary key (NOP_TENANT_ID, ID);

alter table erp_inv_drp_dock_appointment drop constraint PK_erp_inv_drp_dock_appointment;
alter table erp_inv_drp_dock_appointment add constraint PK_erp_inv_drp_dock_appointment primary key (NOP_TENANT_ID, ID);


