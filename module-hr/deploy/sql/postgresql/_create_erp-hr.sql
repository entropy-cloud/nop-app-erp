
CREATE TABLE erp_md_cost_center(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_cost_center primary key (id)
);

CREATE TABLE erp_md_bank_account(
  id INT8  ,
  code VARCHAR(50)  ,
  bankname VARCHAR(50)  ,
  constraint PK_erp_md_bank_account primary key (id)
);

CREATE TABLE erp_md_md_organization(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_organization primary key (id)
);

CREATE TABLE erp_md_currency(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_currency primary key (id)
);

CREATE TABLE erp_prj_project(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_prj_project primary key (id)
);

CREATE TABLE erp_prj_task(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_prj_task primary key (id)
);

CREATE TABLE erp_hr_department(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  parent_id INT8  ,
  manager_id INT8  ,
  cost_center_id INT8  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_department primary key (id)
);

CREATE TABLE erp_hr_shift(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  name VARCHAR(200) NOT NULL ,
  shift_type INT4 NOT NULL ,
  start_time VARCHAR(8) NOT NULL ,
  end_time VARCHAR(8) NOT NULL ,
  grace_late_minutes INT4 default 15   ,
  grace_early_leave_minutes INT4 default 15   ,
  require_clock_in BOOLEAN default true   ,
  require_clock_out BOOLEAN default true   ,
  rest_start_time VARCHAR(8)  ,
  rest_end_time VARCHAR(8)  ,
  total_work_minutes INT4  ,
  allow_overtime BOOLEAN default true   ,
  color_hex VARCHAR(10)  ,
  description VARCHAR(500)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_shift primary key (id)
);

CREATE TABLE erp_hr_shift_rotation_pattern(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  pattern_type VARCHAR(50)  ,
  pattern_data VARCHAR(2000)  ,
  rotate_interval INT4  ,
  start_date DATE  ,
  group_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_shift_rotation_pattern primary key (id)
);

CREATE TABLE erp_hr_competency(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  description VARCHAR(1000)  ,
  category INT4 NOT NULL ,
  competency_group VARCHAR(100)  ,
  is_technical BOOLEAN default false   ,
  parent_id INT8  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_competency primary key (id)
);

CREATE TABLE erp_hr_position(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  department_id INT8  ,
  job_grade VARCHAR(50)  ,
  job_category VARCHAR(100)  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_position primary key (id)
);

CREATE TABLE erp_hr_survey(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  title VARCHAR(200) NOT NULL ,
  description VARCHAR(2000)  ,
  survey_type INT4 NOT NULL ,
  is_anonymous BOOLEAN default true   ,
  status INT4 NOT NULL ,
  start_date DATE  ,
  end_date DATE  ,
  target_department_id INT8  ,
  include_enps BOOLEAN default false   ,
  enps_question VARCHAR(500)  ,
  reminder_days INT4 default 3   ,
  total_questions INT4 default 0   ,
  total_responses INT4 default 0   ,
  completion_rate NUMERIC(5,2)  ,
  avg_score NUMERIC(5,2)  ,
  enps_score INT4  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey primary key (id)
);

CREATE TABLE erp_hr_competency_level(
  id INT8 NOT NULL ,
  competency_id INT8 NOT NULL ,
  level_number INT4 NOT NULL ,
  level_name VARCHAR(100)  ,
  behavioral_anchor VARCHAR(1000)  ,
  sort_order INT4  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_competency_level primary key (id)
);

CREATE TABLE erp_hr_employee(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  first_name VARCHAR(100) NOT NULL ,
  last_name VARCHAR(100) NOT NULL ,
  full_name VARCHAR(200)  ,
  gender INT4 NOT NULL ,
  birth_date DATE  ,
  id_card_type VARCHAR(50)  ,
  id_card_no VARCHAR(50)  ,
  email VARCHAR(200)  ,
  mobile_phone VARCHAR(20)  ,
  marital_status INT4  ,
  nationality VARCHAR(50)  ,
  emergency_contact VARCHAR(100)  ,
  emergency_phone VARCHAR(20)  ,
  department_id INT8  ,
  position_id INT8  ,
  job_title VARCHAR(200)  ,
  superior_id INT8  ,
  cost_center_id INT8  ,
  hire_date DATE NOT NULL ,
  probation_end_date DATE  ,
  regular_date DATE  ,
  resignation_date DATE  ,
  resignation_reason VARCHAR(500)  ,
  employment_status INT4 NOT NULL ,
  employee_type INT4 NOT NULL ,
  bank_account_id INT8  ,
  social_security_no VARCHAR(50)  ,
  tax_file_no VARCHAR(50)  ,
  user_account_id INT8  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_employee primary key (id)
);

CREATE TABLE erp_hr_role_competency(
  id INT8 NOT NULL ,
  position_id INT8 NOT NULL ,
  competency_id INT8 NOT NULL ,
  required_level INT4 NOT NULL ,
  weight NUMERIC(5,2) default 1.0   ,
  is_critical BOOLEAN default false   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_role_competency primary key (id)
);

CREATE TABLE erp_hr_survey_question(
  id INT8 NOT NULL ,
  survey_id INT8 NOT NULL ,
  sort_order INT4  ,
  question_text VARCHAR(2000) NOT NULL ,
  question_type INT4 NOT NULL ,
  rating_scale_min INT4 default 1   ,
  rating_scale_max INT4 default 5   ,
  rating_label_min VARCHAR(100)  ,
  rating_label_max VARCHAR(100)  ,
  options VARCHAR(2000)  ,
  driver_category INT4  ,
  is_required BOOLEAN default true   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey_question primary key (id)
);

CREATE TABLE erp_hr_survey_result(
  id INT8 NOT NULL ,
  survey_id INT8 NOT NULL ,
  department_id INT8  ,
  total_responses INT4 default 0   ,
  avg_score NUMERIC(5,2)  ,
  enps_score INT4  ,
  driver_scores VARCHAR(2000)  ,
  question_breakdown VARCHAR(4000)  ,
  trend_data VARCHAR(4000)  ,
  last_calculated_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey_result primary key (id)
);

CREATE TABLE erp_hr_employment_contract(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  employee_id INT8 NOT NULL ,
  contract_type INT4 NOT NULL ,
  sign_date DATE NOT NULL ,
  start_date DATE NOT NULL ,
  end_date DATE  ,
  probation_months INT4  ,
  working_hours_per_week NUMERIC(5,1)  ,
  annual_salary NUMERIC(20,4)  ,
  monthly_salary NUMERIC(20,4)  ,
  salary_currency_id INT8  ,
  salary_pay_method VARCHAR(50)  ,
  social_insurance_base NUMERIC(20,4)  ,
  housing_fund_base NUMERIC(20,4)  ,
  status INT4 NOT NULL ,
  attachment_file_id VARCHAR(200)  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_employment_contract primary key (id)
);

CREATE TABLE erp_hr_leave_request(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  employee_id INT8 NOT NULL ,
  leave_type INT4 NOT NULL ,
  start_date DATE NOT NULL ,
  end_date DATE NOT NULL ,
  duration_days NUMERIC(10,2)  ,
  reason VARCHAR(500)  ,
  status INT4 NOT NULL ,
  approver_id INT8  ,
  approved_at TIMESTAMP  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_leave_request primary key (id)
);

CREATE TABLE erp_hr_timesheet(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  employee_id INT8 NOT NULL ,
  period_from DATE NOT NULL ,
  period_to DATE NOT NULL ,
  total_hours NUMERIC(10,2)  ,
  status INT4 NOT NULL ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_timesheet primary key (id)
);

CREATE TABLE erp_hr_salary(
  id INT8 NOT NULL ,
  employee_id INT8 NOT NULL ,
  year INT4 NOT NULL ,
  month INT4 NOT NULL ,
  basic_salary NUMERIC(20,4)  ,
  position_allowance NUMERIC(20,4)  ,
  performance_bonus NUMERIC(20,4)  ,
  overtime_pay NUMERIC(20,4)  ,
  meal_allowance NUMERIC(20,4)  ,
  transport_allowance NUMERIC(20,4)  ,
  other_allowance NUMERIC(20,4)  ,
  gross_salary NUMERIC(20,4)  ,
  social_insurance NUMERIC(20,4)  ,
  housing_fund NUMERIC(20,4)  ,
  tax_amount NUMERIC(20,4)  ,
  other_deductions NUMERIC(20,4)  ,
  net_salary NUMERIC(20,4)  ,
  payment_status INT4 NOT NULL ,
  payment_date DATE  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_salary primary key (id)
);

CREATE TABLE erp_hr_recruitment(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  position_id INT8  ,
  department_id INT8  ,
  headcount INT4 default 1   ,
  candidate_name VARCHAR(200) NOT NULL ,
  candidate_phone VARCHAR(20)  ,
  candidate_email VARCHAR(200)  ,
  source INT4  ,
  resume_attachment_file_id VARCHAR(200)  ,
  status INT4 NOT NULL ,
  interviewer_id INT8  ,
  interview_date DATE  ,
  offer_salary NUMERIC(20,4)  ,
  hired_date DATE  ,
  employee_id INT8  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_recruitment primary key (id)
);

CREATE TABLE erp_hr_survey_response(
  id INT8 NOT NULL ,
  survey_id INT8 NOT NULL ,
  employee_id INT8  ,
  respondent_hash VARCHAR(100)  ,
  submitted_at TIMESTAMP  ,
  time_spent_seconds INT4  ,
  is_complete BOOLEAN default false   ,
  org_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey_response primary key (id)
);

CREATE TABLE erp_hr_employee_assessment(
  id INT8 NOT NULL ,
  employee_id INT8 NOT NULL ,
  assessment_type INT4 NOT NULL ,
  assessor_id INT8  ,
  assessment_date DATE  ,
  status INT4 NOT NULL ,
  overall_score NUMERIC(5,2)  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_employee_assessment primary key (id)
);

CREATE TABLE erp_hr_gap_analysis(
  id INT8 NOT NULL ,
  employee_id INT8 NOT NULL ,
  competency_id INT8 NOT NULL ,
  required_level INT4  ,
  actual_level INT4  ,
  gap_value INT4  ,
  gap_severity INT4  ,
  assessment_date DATE  ,
  analysis_date TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_gap_analysis primary key (id)
);

CREATE TABLE erp_hr_development_plan(
  id INT8 NOT NULL ,
  employee_id INT8 NOT NULL ,
  plan_name VARCHAR(200)  ,
  target_date DATE  ,
  status INT4 NOT NULL ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_development_plan primary key (id)
);

CREATE TABLE erp_hr_attendance(
  id INT8 NOT NULL ,
  employee_id INT8 NOT NULL ,
  date DATE NOT NULL ,
  clock_in TIMESTAMP  ,
  clock_out TIMESTAMP  ,
  work_hours NUMERIC(10,2)  ,
  late_minutes INT4 default 0   ,
  early_leave_minutes INT4 default 0   ,
  is_absent BOOLEAN default false   ,
  source INT4  ,
  leave_request_id INT8  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_attendance primary key (id)
);

CREATE TABLE erp_hr_shift_assignment(
  id INT8 NOT NULL ,
  org_id INT8  ,
  employee_id INT8 NOT NULL ,
  shift_id INT8 NOT NULL ,
  assignment_date DATE NOT NULL ,
  actual_start_time TIMESTAMP  ,
  actual_end_time TIMESTAMP  ,
  is_absent BOOLEAN default false   ,
  absence_reason VARCHAR(50)  ,
  leave_request_id INT8  ,
  swap_request_id INT8  ,
  replaced_by_assignment_id INT8  ,
  status VARCHAR(50)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_shift_assignment primary key (id)
);

CREATE TABLE erp_hr_timesheet_line(
  id INT8 NOT NULL ,
  timesheet_id INT8 NOT NULL ,
  employee_id INT8 NOT NULL ,
  work_date DATE NOT NULL ,
  project_id INT8  ,
  task_id INT8  ,
  activity_type VARCHAR(100)  ,
  hours NUMERIC(10,2) NOT NULL ,
  description VARCHAR(500)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_timesheet_line primary key (id)
);

CREATE TABLE erp_hr_salary_simulation(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  source_salary_id INT8  ,
  simulation_period_year INT4 NOT NULL ,
  simulation_period_month INT4 NOT NULL ,
  simulation_name VARCHAR(200)  ,
  status INT4 NOT NULL ,
  reviewer_id INT8  ,
  reviewed_at TIMESTAMP  ,
  converted_at TIMESTAMP  ,
  converted_salary_id INT8  ,
  notes VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_salary_simulation primary key (id)
);

CREATE TABLE erp_hr_survey_answer(
  id INT8 NOT NULL ,
  response_id INT8 NOT NULL ,
  question_id INT8 NOT NULL ,
  rating_value INT4  ,
  selected_option VARCHAR(500)  ,
  open_text VARCHAR(4000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey_answer primary key (id)
);

CREATE TABLE erp_hr_assessment_detail(
  id INT8 NOT NULL ,
  assessment_id INT8 NOT NULL ,
  competency_id INT8 NOT NULL ,
  actual_level INT4  ,
  comment VARCHAR(1000)  ,
  source_type INT4  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_assessment_detail primary key (id)
);

CREATE TABLE erp_hr_development_plan_item(
  id INT8 NOT NULL ,
  plan_id INT8 NOT NULL ,
  competency_id INT8 NOT NULL ,
  gap_id INT8  ,
  target_level INT4 NOT NULL ,
  development_action VARCHAR(500)  ,
  mentor_id INT8  ,
  start_date DATE  ,
  end_date DATE  ,
  status INT4 NOT NULL ,
  progress_note VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_development_plan_item primary key (id)
);

CREATE TABLE erp_hr_shift_swap_request(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  requester_id INT8 NOT NULL ,
  target_employee_id INT8  ,
  source_assignment_id INT8 NOT NULL ,
  target_assignment_id INT8  ,
  swap_date DATE NOT NULL ,
  reason VARCHAR(500)  ,
  status INT4 NOT NULL ,
  approved_by_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_shift_swap_request primary key (id)
);


      COMMENT ON TABLE erp_md_cost_center IS '成本中心';
                
      COMMENT ON TABLE erp_md_bank_account IS '银行账户';
                
      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_prj_project IS '项目';
                
      COMMENT ON TABLE erp_prj_task IS '任务';
                
      COMMENT ON TABLE erp_hr_department IS '部门';
                
      COMMENT ON COLUMN erp_hr_department.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_department.code IS '编码';
                    
      COMMENT ON COLUMN erp_hr_department.name IS '名称';
                    
      COMMENT ON COLUMN erp_hr_department.parent_id IS '上级部门';
                    
      COMMENT ON COLUMN erp_hr_department.manager_id IS '部门负责人';
                    
      COMMENT ON COLUMN erp_hr_department.cost_center_id IS '成本中心';
                    
      COMMENT ON COLUMN erp_hr_department.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_department.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_department.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_department.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_department.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_department.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_department.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_department.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_shift IS '班次模板';
                
      COMMENT ON COLUMN erp_hr_shift.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_shift.code IS '编码';
                    
      COMMENT ON COLUMN erp_hr_shift.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_shift.name IS '班次名称';
                    
      COMMENT ON COLUMN erp_hr_shift.shift_type IS '班次类型';
                    
      COMMENT ON COLUMN erp_hr_shift.start_time IS '上班时间';
                    
      COMMENT ON COLUMN erp_hr_shift.end_time IS '下班时间';
                    
      COMMENT ON COLUMN erp_hr_shift.grace_late_minutes IS '迟到宽容分钟数';
                    
      COMMENT ON COLUMN erp_hr_shift.grace_early_leave_minutes IS '早退宽容分钟数';
                    
      COMMENT ON COLUMN erp_hr_shift.require_clock_in IS '需签到';
                    
      COMMENT ON COLUMN erp_hr_shift.require_clock_out IS '需签退';
                    
      COMMENT ON COLUMN erp_hr_shift.rest_start_time IS '休息开始';
                    
      COMMENT ON COLUMN erp_hr_shift.rest_end_time IS '休息结束';
                    
      COMMENT ON COLUMN erp_hr_shift.total_work_minutes IS '标准工时(分钟)';
                    
      COMMENT ON COLUMN erp_hr_shift.allow_overtime IS '允许加班';
                    
      COMMENT ON COLUMN erp_hr_shift.color_hex IS '显示颜色';
                    
      COMMENT ON COLUMN erp_hr_shift.description IS '说明';
                    
      COMMENT ON COLUMN erp_hr_shift.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_shift.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_shift.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_shift.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_shift.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_shift.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_shift_rotation_pattern IS '轮换排班模板';
                
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.code IS '编码';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.name IS '名称';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.pattern_type IS '轮换类型';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.pattern_data IS '轮换序列';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.rotate_interval IS '轮换间隔(天)';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.start_date IS '起始日期';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.group_id IS '轮换组';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_competency IS '胜任力字典';
                
      COMMENT ON COLUMN erp_hr_competency.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_competency.code IS '编码';
                    
      COMMENT ON COLUMN erp_hr_competency.name IS '名称';
                    
      COMMENT ON COLUMN erp_hr_competency.description IS '描述';
                    
      COMMENT ON COLUMN erp_hr_competency.category IS '分类';
                    
      COMMENT ON COLUMN erp_hr_competency.competency_group IS '能力组';
                    
      COMMENT ON COLUMN erp_hr_competency.is_technical IS '是否技术能力';
                    
      COMMENT ON COLUMN erp_hr_competency.parent_id IS '上级胜任力';
                    
      COMMENT ON COLUMN erp_hr_competency.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_competency.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_competency.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_competency.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_competency.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_competency.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_competency.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_competency.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_position IS '职位';
                
      COMMENT ON COLUMN erp_hr_position.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_position.code IS '编码';
                    
      COMMENT ON COLUMN erp_hr_position.name IS '名称';
                    
      COMMENT ON COLUMN erp_hr_position.department_id IS '所属部门';
                    
      COMMENT ON COLUMN erp_hr_position.job_grade IS '职级';
                    
      COMMENT ON COLUMN erp_hr_position.job_category IS '职位类别';
                    
      COMMENT ON COLUMN erp_hr_position.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_position.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_position.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_position.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_position.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_position.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_position.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_position.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_survey IS '问卷模板';
                
      COMMENT ON COLUMN erp_hr_survey.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey.code IS '编号';
                    
      COMMENT ON COLUMN erp_hr_survey.title IS '问卷标题';
                    
      COMMENT ON COLUMN erp_hr_survey.description IS '问卷说明';
                    
      COMMENT ON COLUMN erp_hr_survey.survey_type IS '调研类型';
                    
      COMMENT ON COLUMN erp_hr_survey.is_anonymous IS '是否匿名';
                    
      COMMENT ON COLUMN erp_hr_survey.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_survey.start_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_hr_survey.end_date IS '截止日期';
                    
      COMMENT ON COLUMN erp_hr_survey.target_department_id IS '目标部门';
                    
      COMMENT ON COLUMN erp_hr_survey.include_enps IS '包含eNPS';
                    
      COMMENT ON COLUMN erp_hr_survey.enps_question IS 'eNPS题面';
                    
      COMMENT ON COLUMN erp_hr_survey.reminder_days IS '催填间隔天数';
                    
      COMMENT ON COLUMN erp_hr_survey.total_questions IS '总题数';
                    
      COMMENT ON COLUMN erp_hr_survey.total_responses IS '总答卷数';
                    
      COMMENT ON COLUMN erp_hr_survey.completion_rate IS '完成率';
                    
      COMMENT ON COLUMN erp_hr_survey.avg_score IS '平均分';
                    
      COMMENT ON COLUMN erp_hr_survey.enps_score IS 'eNPS得分';
                    
      COMMENT ON COLUMN erp_hr_survey.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_survey.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_survey.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_competency_level IS '胜任力等级';
                
      COMMENT ON COLUMN erp_hr_competency_level.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_competency_level.competency_id IS '胜任力';
                    
      COMMENT ON COLUMN erp_hr_competency_level.level_number IS '等级编号';
                    
      COMMENT ON COLUMN erp_hr_competency_level.level_name IS '等级名称';
                    
      COMMENT ON COLUMN erp_hr_competency_level.behavioral_anchor IS '行为锚定';
                    
      COMMENT ON COLUMN erp_hr_competency_level.sort_order IS '排序号';
                    
      COMMENT ON COLUMN erp_hr_competency_level.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_competency_level.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_competency_level.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_competency_level.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_competency_level.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_competency_level.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_employee IS '员工';
                
      COMMENT ON COLUMN erp_hr_employee.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_employee.code IS '工号';
                    
      COMMENT ON COLUMN erp_hr_employee.first_name IS '姓';
                    
      COMMENT ON COLUMN erp_hr_employee.last_name IS '名';
                    
      COMMENT ON COLUMN erp_hr_employee.full_name IS '全名';
                    
      COMMENT ON COLUMN erp_hr_employee.gender IS '性别';
                    
      COMMENT ON COLUMN erp_hr_employee.birth_date IS '出生日期';
                    
      COMMENT ON COLUMN erp_hr_employee.id_card_type IS '证件类型';
                    
      COMMENT ON COLUMN erp_hr_employee.id_card_no IS '证件号码';
                    
      COMMENT ON COLUMN erp_hr_employee.email IS '电子邮箱';
                    
      COMMENT ON COLUMN erp_hr_employee.mobile_phone IS '手机号';
                    
      COMMENT ON COLUMN erp_hr_employee.marital_status IS '婚姻状况';
                    
      COMMENT ON COLUMN erp_hr_employee.nationality IS '国籍';
                    
      COMMENT ON COLUMN erp_hr_employee.emergency_contact IS '紧急联系人';
                    
      COMMENT ON COLUMN erp_hr_employee.emergency_phone IS '紧急联系电话';
                    
      COMMENT ON COLUMN erp_hr_employee.department_id IS '部门';
                    
      COMMENT ON COLUMN erp_hr_employee.position_id IS '职位';
                    
      COMMENT ON COLUMN erp_hr_employee.job_title IS '岗位名称';
                    
      COMMENT ON COLUMN erp_hr_employee.superior_id IS '直接上级';
                    
      COMMENT ON COLUMN erp_hr_employee.cost_center_id IS '默认成本中心';
                    
      COMMENT ON COLUMN erp_hr_employee.hire_date IS '入职日期';
                    
      COMMENT ON COLUMN erp_hr_employee.probation_end_date IS '试用期截止';
                    
      COMMENT ON COLUMN erp_hr_employee.regular_date IS '转正日期';
                    
      COMMENT ON COLUMN erp_hr_employee.resignation_date IS '离职日期';
                    
      COMMENT ON COLUMN erp_hr_employee.resignation_reason IS '离职原因';
                    
      COMMENT ON COLUMN erp_hr_employee.employment_status IS '雇佣状态';
                    
      COMMENT ON COLUMN erp_hr_employee.employee_type IS '员工类型';
                    
      COMMENT ON COLUMN erp_hr_employee.bank_account_id IS '工资卡账户';
                    
      COMMENT ON COLUMN erp_hr_employee.social_security_no IS '社保号';
                    
      COMMENT ON COLUMN erp_hr_employee.tax_file_no IS '个税档案号';
                    
      COMMENT ON COLUMN erp_hr_employee.user_account_id IS '系统用户ID';
                    
      COMMENT ON COLUMN erp_hr_employee.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_employee.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_employee.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_employee.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_employee.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_employee.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_employee.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_employee.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_role_competency IS '岗位胜任力要求';
                
      COMMENT ON COLUMN erp_hr_role_competency.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_role_competency.position_id IS '岗位';
                    
      COMMENT ON COLUMN erp_hr_role_competency.competency_id IS '胜任力';
                    
      COMMENT ON COLUMN erp_hr_role_competency.required_level IS '要求等级';
                    
      COMMENT ON COLUMN erp_hr_role_competency.weight IS '权重';
                    
      COMMENT ON COLUMN erp_hr_role_competency.is_critical IS '是否关键';
                    
      COMMENT ON COLUMN erp_hr_role_competency.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_role_competency.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_role_competency.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_role_competency.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_role_competency.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_role_competency.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_survey_question IS '问卷题目';
                
      COMMENT ON COLUMN erp_hr_survey_question.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey_question.survey_id IS '所属问卷';
                    
      COMMENT ON COLUMN erp_hr_survey_question.sort_order IS '排序号';
                    
      COMMENT ON COLUMN erp_hr_survey_question.question_text IS '题目内容';
                    
      COMMENT ON COLUMN erp_hr_survey_question.question_type IS '题型';
                    
      COMMENT ON COLUMN erp_hr_survey_question.rating_scale_min IS '评分最低分';
                    
      COMMENT ON COLUMN erp_hr_survey_question.rating_scale_max IS '评分最高分';
                    
      COMMENT ON COLUMN erp_hr_survey_question.rating_label_min IS '最低分标签';
                    
      COMMENT ON COLUMN erp_hr_survey_question.rating_label_max IS '最高分标签';
                    
      COMMENT ON COLUMN erp_hr_survey_question.options IS '选项列表';
                    
      COMMENT ON COLUMN erp_hr_survey_question.driver_category IS '驱动因子分类';
                    
      COMMENT ON COLUMN erp_hr_survey_question.is_required IS '是否必填';
                    
      COMMENT ON COLUMN erp_hr_survey_question.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey_question.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey_question.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey_question.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey_question.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey_question.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_survey_result IS '调研结果';
                
      COMMENT ON COLUMN erp_hr_survey_result.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey_result.survey_id IS '所属问卷';
                    
      COMMENT ON COLUMN erp_hr_survey_result.department_id IS '部门';
                    
      COMMENT ON COLUMN erp_hr_survey_result.total_responses IS '答卷数';
                    
      COMMENT ON COLUMN erp_hr_survey_result.avg_score IS '平均分';
                    
      COMMENT ON COLUMN erp_hr_survey_result.enps_score IS 'eNPS得分';
                    
      COMMENT ON COLUMN erp_hr_survey_result.driver_scores IS '驱动因子得分';
                    
      COMMENT ON COLUMN erp_hr_survey_result.question_breakdown IS '每题得分';
                    
      COMMENT ON COLUMN erp_hr_survey_result.trend_data IS '历史趋势';
                    
      COMMENT ON COLUMN erp_hr_survey_result.last_calculated_at IS '最后计算时间';
                    
      COMMENT ON COLUMN erp_hr_survey_result.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey_result.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey_result.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey_result.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey_result.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey_result.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_employment_contract IS '劳动合同';
                
      COMMENT ON COLUMN erp_hr_employment_contract.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.code IS '合同编号';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.employee_id IS '员工';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.contract_type IS '合同类型';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.sign_date IS '签订日期';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.start_date IS '生效日期';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.end_date IS '到期日期';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.probation_months IS '试用期月数';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.working_hours_per_week IS '每周工时';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.annual_salary IS '年薪(税前)';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.monthly_salary IS '月薪(税前)';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.salary_currency_id IS '薪资币种';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.salary_pay_method IS '发薪方式';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.social_insurance_base IS '社保基数';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.housing_fund_base IS '公积金基数';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.status IS '合同状态';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.attachment_file_id IS '合同文件';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_leave_request IS '休假申请';
                
      COMMENT ON COLUMN erp_hr_leave_request.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_leave_request.code IS '单号';
                    
      COMMENT ON COLUMN erp_hr_leave_request.employee_id IS '员工';
                    
      COMMENT ON COLUMN erp_hr_leave_request.leave_type IS '休假类型';
                    
      COMMENT ON COLUMN erp_hr_leave_request.start_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_hr_leave_request.end_date IS '结束日期';
                    
      COMMENT ON COLUMN erp_hr_leave_request.duration_days IS '天数';
                    
      COMMENT ON COLUMN erp_hr_leave_request.reason IS '请假原因';
                    
      COMMENT ON COLUMN erp_hr_leave_request.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_leave_request.approver_id IS '审批人';
                    
      COMMENT ON COLUMN erp_hr_leave_request.approved_at IS '审批时间';
                    
      COMMENT ON COLUMN erp_hr_leave_request.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_leave_request.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_leave_request.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_leave_request.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_leave_request.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_leave_request.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_leave_request.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_leave_request.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_timesheet IS '工时表';
                
      COMMENT ON COLUMN erp_hr_timesheet.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_timesheet.code IS '单号';
                    
      COMMENT ON COLUMN erp_hr_timesheet.employee_id IS '员工';
                    
      COMMENT ON COLUMN erp_hr_timesheet.period_from IS '周期开始';
                    
      COMMENT ON COLUMN erp_hr_timesheet.period_to IS '周期结束';
                    
      COMMENT ON COLUMN erp_hr_timesheet.total_hours IS '总工时';
                    
      COMMENT ON COLUMN erp_hr_timesheet.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_timesheet.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_timesheet.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_timesheet.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_timesheet.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_timesheet.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_timesheet.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_timesheet.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_timesheet.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_salary IS '薪酬记录';
                
      COMMENT ON COLUMN erp_hr_salary.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_salary.employee_id IS '员工';
                    
      COMMENT ON COLUMN erp_hr_salary.year IS '年份';
                    
      COMMENT ON COLUMN erp_hr_salary.month IS '月份';
                    
      COMMENT ON COLUMN erp_hr_salary.basic_salary IS '基本工资';
                    
      COMMENT ON COLUMN erp_hr_salary.position_allowance IS '岗位津贴';
                    
      COMMENT ON COLUMN erp_hr_salary.performance_bonus IS '绩效奖金';
                    
      COMMENT ON COLUMN erp_hr_salary.overtime_pay IS '加班费';
                    
      COMMENT ON COLUMN erp_hr_salary.meal_allowance IS '餐补';
                    
      COMMENT ON COLUMN erp_hr_salary.transport_allowance IS '交通补贴';
                    
      COMMENT ON COLUMN erp_hr_salary.other_allowance IS '其他补贴';
                    
      COMMENT ON COLUMN erp_hr_salary.gross_salary IS '应发合计';
                    
      COMMENT ON COLUMN erp_hr_salary.social_insurance IS '社保个人部分';
                    
      COMMENT ON COLUMN erp_hr_salary.housing_fund IS '公积金个人部分';
                    
      COMMENT ON COLUMN erp_hr_salary.tax_amount IS '个税';
                    
      COMMENT ON COLUMN erp_hr_salary.other_deductions IS '其他扣款';
                    
      COMMENT ON COLUMN erp_hr_salary.net_salary IS '实发合计';
                    
      COMMENT ON COLUMN erp_hr_salary.payment_status IS '支付状态';
                    
      COMMENT ON COLUMN erp_hr_salary.payment_date IS '实发日期';
                    
      COMMENT ON COLUMN erp_hr_salary.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_salary.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_salary.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_salary.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_salary.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_salary.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_salary.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_salary.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_recruitment IS '招聘记录';
                
      COMMENT ON COLUMN erp_hr_recruitment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_recruitment.code IS '编号';
                    
      COMMENT ON COLUMN erp_hr_recruitment.position_id IS '招聘职位';
                    
      COMMENT ON COLUMN erp_hr_recruitment.department_id IS '招聘部门';
                    
      COMMENT ON COLUMN erp_hr_recruitment.headcount IS '招聘人数';
                    
      COMMENT ON COLUMN erp_hr_recruitment.candidate_name IS '应聘者姓名';
                    
      COMMENT ON COLUMN erp_hr_recruitment.candidate_phone IS '联系电话';
                    
      COMMENT ON COLUMN erp_hr_recruitment.candidate_email IS '电子邮箱';
                    
      COMMENT ON COLUMN erp_hr_recruitment.source IS '来源';
                    
      COMMENT ON COLUMN erp_hr_recruitment.resume_attachment_file_id IS '简历附件';
                    
      COMMENT ON COLUMN erp_hr_recruitment.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_recruitment.interviewer_id IS '面试官';
                    
      COMMENT ON COLUMN erp_hr_recruitment.interview_date IS '面试日期';
                    
      COMMENT ON COLUMN erp_hr_recruitment.offer_salary IS 'Offer薪资';
                    
      COMMENT ON COLUMN erp_hr_recruitment.hired_date IS '入职日期';
                    
      COMMENT ON COLUMN erp_hr_recruitment.employee_id IS '关联员工';
                    
      COMMENT ON COLUMN erp_hr_recruitment.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_recruitment.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_recruitment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_recruitment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_recruitment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_recruitment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_recruitment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_recruitment.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_survey_response IS '答卷';
                
      COMMENT ON COLUMN erp_hr_survey_response.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey_response.survey_id IS '所属问卷';
                    
      COMMENT ON COLUMN erp_hr_survey_response.employee_id IS '填写员工';
                    
      COMMENT ON COLUMN erp_hr_survey_response.respondent_hash IS '匿名哈希';
                    
      COMMENT ON COLUMN erp_hr_survey_response.submitted_at IS '提交时间';
                    
      COMMENT ON COLUMN erp_hr_survey_response.time_spent_seconds IS '填写耗时';
                    
      COMMENT ON COLUMN erp_hr_survey_response.is_complete IS '是否完整';
                    
      COMMENT ON COLUMN erp_hr_survey_response.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_survey_response.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey_response.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey_response.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey_response.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey_response.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey_response.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_employee_assessment IS '员工评估';
                
      COMMENT ON COLUMN erp_hr_employee_assessment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.employee_id IS '被评估员工';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.assessment_type IS '评估类型';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.assessor_id IS '评估人';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.assessment_date IS '评估日期';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.overall_score IS '综合评分';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_gap_analysis IS '差距分析';
                
      COMMENT ON COLUMN erp_hr_gap_analysis.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.employee_id IS '员工';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.competency_id IS '胜任力';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.required_level IS '要求等级';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.actual_level IS '实际等级';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.gap_value IS '差距值';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.gap_severity IS '差距严重程度';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.assessment_date IS '评估日期';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.analysis_date IS '分析日期';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_development_plan IS '发展计划';
                
      COMMENT ON COLUMN erp_hr_development_plan.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_development_plan.employee_id IS '员工';
                    
      COMMENT ON COLUMN erp_hr_development_plan.plan_name IS '计划名称';
                    
      COMMENT ON COLUMN erp_hr_development_plan.target_date IS '目标完成日期';
                    
      COMMENT ON COLUMN erp_hr_development_plan.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_development_plan.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_development_plan.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_development_plan.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_development_plan.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_development_plan.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_development_plan.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_development_plan.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_development_plan.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_attendance IS '考勤记录';
                
      COMMENT ON COLUMN erp_hr_attendance.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_attendance.employee_id IS '员工';
                    
      COMMENT ON COLUMN erp_hr_attendance.date IS '考勤日期';
                    
      COMMENT ON COLUMN erp_hr_attendance.clock_in IS '签到时间';
                    
      COMMENT ON COLUMN erp_hr_attendance.clock_out IS '签退时间';
                    
      COMMENT ON COLUMN erp_hr_attendance.work_hours IS '出勤时长';
                    
      COMMENT ON COLUMN erp_hr_attendance.late_minutes IS '迟到分钟数';
                    
      COMMENT ON COLUMN erp_hr_attendance.early_leave_minutes IS '早退分钟数';
                    
      COMMENT ON COLUMN erp_hr_attendance.is_absent IS '是否旷工';
                    
      COMMENT ON COLUMN erp_hr_attendance.source IS '来源';
                    
      COMMENT ON COLUMN erp_hr_attendance.leave_request_id IS '关联休假';
                    
      COMMENT ON COLUMN erp_hr_attendance.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_attendance.remark IS '备注';
                    
      COMMENT ON COLUMN erp_hr_attendance.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_attendance.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_attendance.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_attendance.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_attendance.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_attendance.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_shift_assignment IS '排班分配';
                
      COMMENT ON COLUMN erp_hr_shift_assignment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.employee_id IS '员工';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.shift_id IS '班次';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.assignment_date IS '排班日期';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.actual_start_time IS '实际签到时间';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.actual_end_time IS '实际签退时间';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.is_absent IS '是否缺勤';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.absence_reason IS '缺勤原因';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.leave_request_id IS '关联休假';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.swap_request_id IS '关联调换';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.replaced_by_assignment_id IS '被替换排班';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_timesheet_line IS '工时表明细';
                
      COMMENT ON COLUMN erp_hr_timesheet_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.timesheet_id IS '工时表ID';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.employee_id IS '员工';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.work_date IS '工作日期';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.task_id IS '任务';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.activity_type IS '活动类型';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.hours IS '小时数';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.description IS '工作内容';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_salary_simulation IS '薪酬模拟';
                
      COMMENT ON COLUMN erp_hr_salary_simulation.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.code IS '编号';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.source_salary_id IS '源薪酬记录';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.simulation_period_year IS '模拟年份';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.simulation_period_month IS '模拟月份';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.simulation_name IS '模拟名称';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.reviewer_id IS '审批人';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.reviewed_at IS '审批时间';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.converted_at IS '转正式时间';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.converted_salary_id IS '转正式薪酬ID';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.notes IS '备注';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_survey_answer IS '回答明细';
                
      COMMENT ON COLUMN erp_hr_survey_answer.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.response_id IS '答卷';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.question_id IS '题目';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.rating_value IS '评分值';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.selected_option IS '选项';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.open_text IS '文本回答';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_assessment_detail IS '评估明细';
                
      COMMENT ON COLUMN erp_hr_assessment_detail.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.assessment_id IS '评估';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.competency_id IS '胜任力';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.actual_level IS '实际等级';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.comment IS '评语';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.source_type IS '来源类型';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_development_plan_item IS '发展计划项';
                
      COMMENT ON COLUMN erp_hr_development_plan_item.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.plan_id IS '发展计划';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.competency_id IS '目标胜任力';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.gap_id IS '关联差距分析';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.target_level IS '目标等级';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.development_action IS '发展行动';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.mentor_id IS '导师';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.start_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.end_date IS '预计完成日期';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.progress_note IS '进度说明';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_shift_swap_request IS '排班调换申请';
                
      COMMENT ON COLUMN erp_hr_shift_swap_request.id IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.code IS '编号';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.requester_id IS '申请人';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.target_employee_id IS '目标员工';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.source_assignment_id IS '原排班';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.target_assignment_id IS '目标排班';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.swap_date IS '调换日期';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.reason IS '调换原因';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.status IS '状态';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.approved_by_id IS '审批人';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.update_time IS '修改时间';
                    
