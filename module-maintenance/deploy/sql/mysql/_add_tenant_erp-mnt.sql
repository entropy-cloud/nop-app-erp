
    alter table erp_mnt_equipment_category add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_maintenance_team add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_equipment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_maintenance_team_member add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_schedule add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_request add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_downtime_entry add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_calibration add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_visit add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_visit_task add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_spare_part_usage add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_spare_part_usage_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mnt_equipment_category drop primary key;
alter table erp_mnt_equipment_category add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_maintenance_team drop primary key;
alter table erp_mnt_maintenance_team add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_equipment drop primary key;
alter table erp_mnt_equipment add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_maintenance_team_member drop primary key;
alter table erp_mnt_maintenance_team_member add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_schedule drop primary key;
alter table erp_mnt_schedule add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_request drop primary key;
alter table erp_mnt_request add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_downtime_entry drop primary key;
alter table erp_mnt_downtime_entry add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_calibration drop primary key;
alter table erp_mnt_calibration add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_visit drop primary key;
alter table erp_mnt_visit add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_visit_task drop primary key;
alter table erp_mnt_visit_task add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_spare_part_usage drop primary key;
alter table erp_mnt_spare_part_usage add primary key (NOP_TENANT_ID, ID);

alter table erp_mnt_spare_part_usage_line drop primary key;
alter table erp_mnt_spare_part_usage_line add primary key (NOP_TENANT_ID, ID);


