
CREATE TABLE erp_md_md_organization(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_organization primary key (id)
);

CREATE TABLE erp_md_auth_user(
  id INT8 NOT NULL ,
  constraint PK_erp_md_auth_user primary key (id)
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
  attachment_id INT8  ,
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
  resume_attachment_id INT8  ,
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


      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_auth_user IS 'NopAuthUser';
                
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
                    
      COMMENT ON COLUMN erp_hr_employment_contract.attachment_id IS '合同文件';
                    
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
                    
      COMMENT ON COLUMN erp_hr_recruitment.resume_attachment_id IS '简历附件';
                    
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
                    
