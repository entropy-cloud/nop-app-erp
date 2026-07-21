
    alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_location add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_move add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_safety_stock_calc add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_parameter add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_lead_time_record add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_scenario add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_cross_dock add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_scenario_version add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_drp_scenario_param add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_drp_dock_appointment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop primary key;
alter table erp_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_md_warehouse drop primary key;
alter table erp_md_warehouse add primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_md_location drop primary key;
alter table erp_md_location add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_move drop primary key;
alter table erp_inv_stock_move add primary key (NOP_TENANT_ID, ID);

alter table erp_drp_plan drop primary key;
alter table erp_drp_plan add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_drp_safety_stock_calc drop primary key;
alter table erp_inv_drp_safety_stock_calc add primary key (NOP_TENANT_ID, ID);

alter table erp_drp_parameter drop primary key;
alter table erp_drp_parameter add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_drp_lead_time_record drop primary key;
alter table erp_inv_drp_lead_time_record add primary key (NOP_TENANT_ID, ID);

alter table erp_drp_line drop primary key;
alter table erp_drp_line add primary key (NOP_TENANT_ID, ID);

alter table erp_drp_scenario drop primary key;
alter table erp_drp_scenario add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_drp_cross_dock drop primary key;
alter table erp_inv_drp_cross_dock add primary key (NOP_TENANT_ID, ID);

alter table erp_drp_scenario_version drop primary key;
alter table erp_drp_scenario_version add primary key (NOP_TENANT_ID, ID);

alter table erp_drp_scenario_param drop primary key;
alter table erp_drp_scenario_param add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_drp_dock_appointment drop primary key;
alter table erp_inv_drp_dock_appointment add primary key (NOP_TENANT_ID, ID);


