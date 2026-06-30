
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

alter table erp_md_md_organization drop constraint PK_erp_md_md_organization;
alter table erp_md_md_organization add constraint PK_erp_md_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_md_auth_user drop constraint PK_erp_md_auth_user;
alter table erp_md_auth_user add constraint PK_erp_md_auth_user primary key (NOP_TENANT_ID, id);

alter table erp_hr_department drop constraint PK_erp_hr_department;
alter table erp_hr_department add constraint PK_erp_hr_department primary key (NOP_TENANT_ID, id);

alter table erp_hr_position drop constraint PK_erp_hr_position;
alter table erp_hr_position add constraint PK_erp_hr_position primary key (NOP_TENANT_ID, id);

alter table erp_hr_employee drop constraint PK_erp_hr_employee;
alter table erp_hr_employee add constraint PK_erp_hr_employee primary key (NOP_TENANT_ID, id);

alter table erp_hr_employment_contract drop constraint PK_erp_hr_employment_contract;
alter table erp_hr_employment_contract add constraint PK_erp_hr_employment_contract primary key (NOP_TENANT_ID, id);

alter table erp_hr_leave_request drop constraint PK_erp_hr_leave_request;
alter table erp_hr_leave_request add constraint PK_erp_hr_leave_request primary key (NOP_TENANT_ID, id);

alter table erp_hr_timesheet drop constraint PK_erp_hr_timesheet;
alter table erp_hr_timesheet add constraint PK_erp_hr_timesheet primary key (NOP_TENANT_ID, id);

alter table erp_hr_salary drop constraint PK_erp_hr_salary;
alter table erp_hr_salary add constraint PK_erp_hr_salary primary key (NOP_TENANT_ID, id);

alter table erp_hr_recruitment drop constraint PK_erp_hr_recruitment;
alter table erp_hr_recruitment add constraint PK_erp_hr_recruitment primary key (NOP_TENANT_ID, id);

alter table erp_hr_attendance drop constraint PK_erp_hr_attendance;
alter table erp_hr_attendance add constraint PK_erp_hr_attendance primary key (NOP_TENANT_ID, id);

alter table erp_hr_timesheet_line drop constraint PK_erp_hr_timesheet_line;
alter table erp_hr_timesheet_line add constraint PK_erp_hr_timesheet_line primary key (NOP_TENANT_ID, id);


