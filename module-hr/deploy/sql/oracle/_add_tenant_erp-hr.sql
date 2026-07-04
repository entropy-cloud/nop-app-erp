
    alter table erp_md_cost_center add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_bank_account add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_prj_task add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_department add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_salary_item add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_social_insurance_config add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_tax_config add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_payroll_bank_file add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_shift add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_shift_rotation_pattern add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_competency add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_position add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_competency_level add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_employee add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_role_competency add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey_question add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey_result add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_employment_contract add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_leave_request add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_timesheet add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_salary add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_recruitment add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_social_insurance_base add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_tax_special_deduction add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey_response add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_employee_assessment add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_gap_analysis add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_development_plan add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_attendance add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_shift_assignment add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_timesheet_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_salary_simulation add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey_answer add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_assessment_detail add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_development_plan_item add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_shift_swap_request add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_hr_salary_simulation_item_adj add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_md_cost_center drop constraint PK_erp_md_cost_center;
alter table erp_md_cost_center add constraint PK_erp_md_cost_center primary key (NOP_TENANT_ID, ID);

alter table erp_md_bank_account drop constraint PK_erp_md_bank_account;
alter table erp_md_bank_account add constraint PK_erp_md_bank_account primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_organization drop constraint PK_erp_md_md_organization;
alter table erp_md_md_organization add constraint PK_erp_md_md_organization primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop constraint PK_erp_md_currency;
alter table erp_md_currency add constraint PK_erp_md_currency primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project drop constraint PK_erp_prj_project;
alter table erp_prj_project add constraint PK_erp_prj_project primary key (NOP_TENANT_ID, ID);

alter table erp_prj_task drop constraint PK_erp_prj_task;
alter table erp_prj_task add constraint PK_erp_prj_task primary key (NOP_TENANT_ID, ID);

alter table erp_hr_department drop constraint PK_erp_hr_department;
alter table erp_hr_department add constraint PK_erp_hr_department primary key (NOP_TENANT_ID, ID);

alter table erp_hr_salary_item drop constraint PK_erp_hr_salary_item;
alter table erp_hr_salary_item add constraint PK_erp_hr_salary_item primary key (NOP_TENANT_ID, ID);

alter table erp_hr_social_insurance_config drop constraint PK_erp_hr_social_insurance_config;
alter table erp_hr_social_insurance_config add constraint PK_erp_hr_social_insurance_config primary key (NOP_TENANT_ID, ID);

alter table erp_hr_tax_config drop constraint PK_erp_hr_tax_config;
alter table erp_hr_tax_config add constraint PK_erp_hr_tax_config primary key (NOP_TENANT_ID, ID);

alter table erp_hr_payroll_bank_file drop constraint PK_erp_hr_payroll_bank_file;
alter table erp_hr_payroll_bank_file add constraint PK_erp_hr_payroll_bank_file primary key (NOP_TENANT_ID, ID);

alter table erp_hr_shift drop constraint PK_erp_hr_shift;
alter table erp_hr_shift add constraint PK_erp_hr_shift primary key (NOP_TENANT_ID, ID);

alter table erp_hr_shift_rotation_pattern drop constraint PK_erp_hr_shift_rotation_pattern;
alter table erp_hr_shift_rotation_pattern add constraint PK_erp_hr_shift_rotation_pattern primary key (NOP_TENANT_ID, ID);

alter table erp_hr_competency drop constraint PK_erp_hr_competency;
alter table erp_hr_competency add constraint PK_erp_hr_competency primary key (NOP_TENANT_ID, ID);

alter table erp_hr_position drop constraint PK_erp_hr_position;
alter table erp_hr_position add constraint PK_erp_hr_position primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey drop constraint PK_erp_hr_survey;
alter table erp_hr_survey add constraint PK_erp_hr_survey primary key (NOP_TENANT_ID, ID);

alter table erp_hr_competency_level drop constraint PK_erp_hr_competency_level;
alter table erp_hr_competency_level add constraint PK_erp_hr_competency_level primary key (NOP_TENANT_ID, ID);

alter table erp_hr_employee drop constraint PK_erp_hr_employee;
alter table erp_hr_employee add constraint PK_erp_hr_employee primary key (NOP_TENANT_ID, ID);

alter table erp_hr_role_competency drop constraint PK_erp_hr_role_competency;
alter table erp_hr_role_competency add constraint PK_erp_hr_role_competency primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey_question drop constraint PK_erp_hr_survey_question;
alter table erp_hr_survey_question add constraint PK_erp_hr_survey_question primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey_result drop constraint PK_erp_hr_survey_result;
alter table erp_hr_survey_result add constraint PK_erp_hr_survey_result primary key (NOP_TENANT_ID, ID);

alter table erp_hr_employment_contract drop constraint PK_erp_hr_employment_contract;
alter table erp_hr_employment_contract add constraint PK_erp_hr_employment_contract primary key (NOP_TENANT_ID, ID);

alter table erp_hr_leave_request drop constraint PK_erp_hr_leave_request;
alter table erp_hr_leave_request add constraint PK_erp_hr_leave_request primary key (NOP_TENANT_ID, ID);

alter table erp_hr_timesheet drop constraint PK_erp_hr_timesheet;
alter table erp_hr_timesheet add constraint PK_erp_hr_timesheet primary key (NOP_TENANT_ID, ID);

alter table erp_hr_salary drop constraint PK_erp_hr_salary;
alter table erp_hr_salary add constraint PK_erp_hr_salary primary key (NOP_TENANT_ID, ID);

alter table erp_hr_recruitment drop constraint PK_erp_hr_recruitment;
alter table erp_hr_recruitment add constraint PK_erp_hr_recruitment primary key (NOP_TENANT_ID, ID);

alter table erp_hr_social_insurance_base drop constraint PK_erp_hr_social_insurance_base;
alter table erp_hr_social_insurance_base add constraint PK_erp_hr_social_insurance_base primary key (NOP_TENANT_ID, ID);

alter table erp_hr_tax_special_deduction drop constraint PK_erp_hr_tax_special_deduction;
alter table erp_hr_tax_special_deduction add constraint PK_erp_hr_tax_special_deduction primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey_response drop constraint PK_erp_hr_survey_response;
alter table erp_hr_survey_response add constraint PK_erp_hr_survey_response primary key (NOP_TENANT_ID, ID);

alter table erp_hr_employee_assessment drop constraint PK_erp_hr_employee_assessment;
alter table erp_hr_employee_assessment add constraint PK_erp_hr_employee_assessment primary key (NOP_TENANT_ID, ID);

alter table erp_hr_gap_analysis drop constraint PK_erp_hr_gap_analysis;
alter table erp_hr_gap_analysis add constraint PK_erp_hr_gap_analysis primary key (NOP_TENANT_ID, ID);

alter table erp_hr_development_plan drop constraint PK_erp_hr_development_plan;
alter table erp_hr_development_plan add constraint PK_erp_hr_development_plan primary key (NOP_TENANT_ID, ID);

alter table erp_hr_attendance drop constraint PK_erp_hr_attendance;
alter table erp_hr_attendance add constraint PK_erp_hr_attendance primary key (NOP_TENANT_ID, ID);

alter table erp_hr_shift_assignment drop constraint PK_erp_hr_shift_assignment;
alter table erp_hr_shift_assignment add constraint PK_erp_hr_shift_assignment primary key (NOP_TENANT_ID, ID);

alter table erp_hr_timesheet_line drop constraint PK_erp_hr_timesheet_line;
alter table erp_hr_timesheet_line add constraint PK_erp_hr_timesheet_line primary key (NOP_TENANT_ID, ID);

alter table erp_hr_salary_simulation drop constraint PK_erp_hr_salary_simulation;
alter table erp_hr_salary_simulation add constraint PK_erp_hr_salary_simulation primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey_answer drop constraint PK_erp_hr_survey_answer;
alter table erp_hr_survey_answer add constraint PK_erp_hr_survey_answer primary key (NOP_TENANT_ID, ID);

alter table erp_hr_assessment_detail drop constraint PK_erp_hr_assessment_detail;
alter table erp_hr_assessment_detail add constraint PK_erp_hr_assessment_detail primary key (NOP_TENANT_ID, ID);

alter table erp_hr_development_plan_item drop constraint PK_erp_hr_development_plan_item;
alter table erp_hr_development_plan_item add constraint PK_erp_hr_development_plan_item primary key (NOP_TENANT_ID, ID);

alter table erp_hr_shift_swap_request drop constraint PK_erp_hr_shift_swap_request;
alter table erp_hr_shift_swap_request add constraint PK_erp_hr_shift_swap_request primary key (NOP_TENANT_ID, ID);

alter table erp_hr_salary_simulation_item_adj drop constraint PK_erp_hr_salary_simulation_item_adj;
alter table erp_hr_salary_simulation_item_adj add constraint PK_erp_hr_salary_simulation_item_adj primary key (NOP_TENANT_ID, ID);


