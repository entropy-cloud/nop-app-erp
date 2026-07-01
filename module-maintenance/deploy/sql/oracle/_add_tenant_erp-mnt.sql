
    alter table erp_md_location add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_equipment_category add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_ast_asset add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_category add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_equipment add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_maintenance_team add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_schedule add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_request add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_downtime_entry add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_calibration add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_maintenance_team_member add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_visit add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_visit_task add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_spare_part_usage add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_spare_part_usage_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_location drop constraint PK_erp_md_location;
alter table erp_md_location add constraint PK_erp_md_location primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_equipment_category drop constraint PK_erp_mnt_equipment_category;
alter table erp_mnt_equipment_category add constraint PK_erp_mnt_equipment_category primary key (NOP_TENANT_ID, ID);

alter table erp_ast_asset drop constraint PK_erp_ast_asset;
alter table erp_ast_asset add constraint PK_erp_ast_asset primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_md_employee drop constraint PK_erp_md_employee;
alter table erp_md_employee add constraint PK_erp_md_employee primary key (NOP_TENANT_ID, ID);

alter table erp_md_warehouse drop constraint PK_erp_md_warehouse;
alter table erp_md_warehouse add constraint PK_erp_md_warehouse primary key (NOP_TENANT_ID, ID);

alter table erp_md_material drop constraint PK_erp_md_material;
alter table erp_md_material add constraint PK_erp_md_material primary key (NOP_TENANT_ID, ID);

alter table erp_md_uom drop constraint PK_erp_md_uom;
alter table erp_md_uom add constraint PK_erp_md_uom primary key (NOP_TENANT_ID, ID);

alter table erp_md_material_category drop constraint PK_erp_md_material_category;
alter table erp_md_material_category add constraint PK_erp_md_material_category primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_equipment drop constraint PK_erp_mnt_equipment;
alter table erp_mnt_equipment add constraint PK_erp_mnt_equipment primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_maintenance_team drop constraint PK_erp_mnt_maintenance_team;
alter table erp_mnt_maintenance_team add constraint PK_erp_mnt_maintenance_team primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_schedule drop constraint PK_erp_mnt_schedule;
alter table erp_mnt_schedule add constraint PK_erp_mnt_schedule primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_request drop constraint PK_erp_mnt_request;
alter table erp_mnt_request add constraint PK_erp_mnt_request primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_downtime_entry drop constraint PK_erp_mnt_downtime_entry;
alter table erp_mnt_downtime_entry add constraint PK_erp_mnt_downtime_entry primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_calibration drop constraint PK_erp_mnt_calibration;
alter table erp_mnt_calibration add constraint PK_erp_mnt_calibration primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_maintenance_team_member drop constraint PK_erp_mnt_maintenance_team_member;
alter table erp_mnt_maintenance_team_member add constraint PK_erp_mnt_maintenance_team_member primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_visit drop constraint PK_erp_mnt_visit;
alter table erp_mnt_visit add constraint PK_erp_mnt_visit primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_visit_task drop constraint PK_erp_mnt_visit_task;
alter table erp_mnt_visit_task add constraint PK_erp_mnt_visit_task primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_spare_part_usage drop constraint PK_erp_mnt_spare_part_usage;
alter table erp_mnt_spare_part_usage add constraint PK_erp_mnt_spare_part_usage primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_spare_part_usage_line drop constraint PK_erp_mnt_spare_part_usage_line;
alter table erp_mnt_spare_part_usage_line add constraint PK_erp_mnt_spare_part_usage_line primary key (NOP_TENANT_ID, ID);


