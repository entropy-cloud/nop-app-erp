
    alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_auth_user add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_department add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_position add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_employment_contract add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_leave_request add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_timesheet add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_salary add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_recruitment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_attendance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_timesheet_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization drop primary key;
alter table erp_md_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_auth_user drop primary key;
alter table erp_md_auth_user add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_department drop primary key;
alter table erp_hr_department add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_position drop primary key;
alter table erp_hr_position add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_employee drop primary key;
alter table erp_hr_employee add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_employment_contract drop primary key;
alter table erp_hr_employment_contract add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_leave_request drop primary key;
alter table erp_hr_leave_request add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_timesheet drop primary key;
alter table erp_hr_timesheet add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_salary drop primary key;
alter table erp_hr_salary add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_recruitment drop primary key;
alter table erp_hr_recruitment add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_attendance drop primary key;
alter table erp_hr_attendance add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_timesheet_line drop primary key;
alter table erp_hr_timesheet_line add primary key (NOP_TENANT_ID, ID);


