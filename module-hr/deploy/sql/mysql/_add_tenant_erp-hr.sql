
    alter table erp_md_cost_center add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_bank_account add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_task add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_department add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_salary_item add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_social_insurance_config add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_tax_config add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_payroll_bank_file add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_shift add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_shift_rotation_pattern add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_competency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_position add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_competency_level add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_role_competency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey_question add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey_result add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_employment_contract add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_leave_request add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_timesheet add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_salary add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_recruitment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_social_insurance_base add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_tax_special_deduction add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey_response add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_employee_assessment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_gap_analysis add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_development_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_attendance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_shift_assignment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_timesheet_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_salary_simulation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_survey_answer add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_assessment_detail add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_development_plan_item add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_shift_swap_request add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_hr_salary_simulation_item_adj add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_cost_center drop primary key;
alter table erp_md_cost_center add primary key (NOP_TENANT_ID, ID);

alter table erp_md_bank_account drop primary key;
alter table erp_md_bank_account add primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_organization drop primary key;
alter table erp_md_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_project drop primary key;
alter table erp_prj_project add primary key (NOP_TENANT_ID, ID);

alter table erp_prj_task drop primary key;
alter table erp_prj_task add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_department drop primary key;
alter table erp_hr_department add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_salary_item drop primary key;
alter table erp_hr_salary_item add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_social_insurance_config drop primary key;
alter table erp_hr_social_insurance_config add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_tax_config drop primary key;
alter table erp_hr_tax_config add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_payroll_bank_file drop primary key;
alter table erp_hr_payroll_bank_file add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_shift drop primary key;
alter table erp_hr_shift add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_shift_rotation_pattern drop primary key;
alter table erp_hr_shift_rotation_pattern add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_competency drop primary key;
alter table erp_hr_competency add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_position drop primary key;
alter table erp_hr_position add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey drop primary key;
alter table erp_hr_survey add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_competency_level drop primary key;
alter table erp_hr_competency_level add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_employee drop primary key;
alter table erp_hr_employee add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_role_competency drop primary key;
alter table erp_hr_role_competency add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey_question drop primary key;
alter table erp_hr_survey_question add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey_result drop primary key;
alter table erp_hr_survey_result add primary key (NOP_TENANT_ID, ID);

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

alter table erp_hr_social_insurance_base drop primary key;
alter table erp_hr_social_insurance_base add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_tax_special_deduction drop primary key;
alter table erp_hr_tax_special_deduction add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey_response drop primary key;
alter table erp_hr_survey_response add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_employee_assessment drop primary key;
alter table erp_hr_employee_assessment add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_gap_analysis drop primary key;
alter table erp_hr_gap_analysis add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_development_plan drop primary key;
alter table erp_hr_development_plan add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_attendance drop primary key;
alter table erp_hr_attendance add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_shift_assignment drop primary key;
alter table erp_hr_shift_assignment add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_timesheet_line drop primary key;
alter table erp_hr_timesheet_line add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_salary_simulation drop primary key;
alter table erp_hr_salary_simulation add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_survey_answer drop primary key;
alter table erp_hr_survey_answer add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_assessment_detail drop primary key;
alter table erp_hr_assessment_detail add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_development_plan_item drop primary key;
alter table erp_hr_development_plan_item add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_shift_swap_request drop primary key;
alter table erp_hr_shift_swap_request add primary key (NOP_TENANT_ID, ID);

alter table erp_hr_salary_simulation_item_adj drop primary key;
alter table erp_hr_salary_simulation_item_adj add primary key (NOP_TENANT_ID, ID);


