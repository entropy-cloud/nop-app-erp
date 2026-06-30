
CREATE TABLE erp_md_md_organization(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_organization primary key (ID)
);

CREATE TABLE erp_md_auth_user(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_auth_user primary key (ID)
);

CREATE TABLE erp_hr_department(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  PARENT_ID NUMBER(20)  ,
  MANAGER_ID NUMBER(20)  ,
  COST_CENTER_ID NUMBER(20)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_department primary key (ID)
);

CREATE TABLE erp_hr_position(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  DEPARTMENT_ID NUMBER(20)  ,
  JOB_GRADE VARCHAR2(50)  ,
  JOB_CATEGORY VARCHAR2(100)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_position primary key (ID)
);

CREATE TABLE erp_hr_employee(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  FIRST_NAME VARCHAR2(100) NOT NULL ,
  LAST_NAME VARCHAR2(100) NOT NULL ,
  FULL_NAME VARCHAR2(200)  ,
  GENDER INTEGER NOT NULL ,
  BIRTH_DATE DATE  ,
  ID_CARD_TYPE VARCHAR2(50)  ,
  ID_CARD_NO VARCHAR2(50)  ,
  EMAIL VARCHAR2(200)  ,
  MOBILE_PHONE VARCHAR2(20)  ,
  MARITAL_STATUS INTEGER  ,
  NATIONALITY VARCHAR2(50)  ,
  EMERGENCY_CONTACT VARCHAR2(100)  ,
  EMERGENCY_PHONE VARCHAR2(20)  ,
  DEPARTMENT_ID NUMBER(20)  ,
  POSITION_ID NUMBER(20)  ,
  JOB_TITLE VARCHAR2(200)  ,
  SUPERIOR_ID NUMBER(20)  ,
  COST_CENTER_ID NUMBER(20)  ,
  HIRE_DATE DATE NOT NULL ,
  PROBATION_END_DATE DATE  ,
  REGULAR_DATE DATE  ,
  RESIGNATION_DATE DATE  ,
  RESIGNATION_REASON VARCHAR2(500)  ,
  EMPLOYMENT_STATUS INTEGER NOT NULL ,
  EMPLOYEE_TYPE INTEGER NOT NULL ,
  BANK_ACCOUNT_ID NUMBER(20)  ,
  SOCIAL_SECURITY_NO VARCHAR2(50)  ,
  TAX_FILE_NO VARCHAR2(50)  ,
  USER_ACCOUNT_ID NUMBER(20)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_employee primary key (ID)
);

CREATE TABLE erp_hr_employment_contract(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  CONTRACT_TYPE INTEGER NOT NULL ,
  SIGN_DATE DATE NOT NULL ,
  START_DATE DATE NOT NULL ,
  END_DATE DATE  ,
  PROBATION_MONTHS INTEGER  ,
  WORKING_HOURS_PER_WEEK NUMBER(5,1)  ,
  ANNUAL_SALARY NUMBER(20,4)  ,
  MONTHLY_SALARY NUMBER(20,4)  ,
  SALARY_CURRENCY_ID NUMBER(20)  ,
  SALARY_PAY_METHOD VARCHAR2(50)  ,
  SOCIAL_INSURANCE_BASE NUMBER(20,4)  ,
  HOUSING_FUND_BASE NUMBER(20,4)  ,
  STATUS INTEGER NOT NULL ,
  ATTACHMENT_ID NUMBER(20)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_employment_contract primary key (ID)
);

CREATE TABLE erp_hr_leave_request(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  LEAVE_TYPE INTEGER NOT NULL ,
  START_DATE DATE NOT NULL ,
  END_DATE DATE NOT NULL ,
  DURATION_DAYS NUMBER(10,2)  ,
  REASON VARCHAR2(500)  ,
  STATUS INTEGER NOT NULL ,
  APPROVER_ID NUMBER(20)  ,
  APPROVED_AT DATE  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_leave_request primary key (ID)
);

CREATE TABLE erp_hr_timesheet(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  PERIOD_FROM DATE NOT NULL ,
  PERIOD_TO DATE NOT NULL ,
  TOTAL_HOURS NUMBER(10,2)  ,
  STATUS INTEGER NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_timesheet primary key (ID)
);

CREATE TABLE erp_hr_salary(
  ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  YEAR INTEGER NOT NULL ,
  MONTH INTEGER NOT NULL ,
  BASIC_SALARY NUMBER(20,4)  ,
  POSITION_ALLOWANCE NUMBER(20,4)  ,
  PERFORMANCE_BONUS NUMBER(20,4)  ,
  OVERTIME_PAY NUMBER(20,4)  ,
  MEAL_ALLOWANCE NUMBER(20,4)  ,
  TRANSPORT_ALLOWANCE NUMBER(20,4)  ,
  OTHER_ALLOWANCE NUMBER(20,4)  ,
  GROSS_SALARY NUMBER(20,4)  ,
  SOCIAL_INSURANCE NUMBER(20,4)  ,
  HOUSING_FUND NUMBER(20,4)  ,
  TAX_AMOUNT NUMBER(20,4)  ,
  OTHER_DEDUCTIONS NUMBER(20,4)  ,
  NET_SALARY NUMBER(20,4)  ,
  PAYMENT_STATUS INTEGER NOT NULL ,
  PAYMENT_DATE DATE  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_salary primary key (ID)
);

CREATE TABLE erp_hr_recruitment(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  POSITION_ID NUMBER(20)  ,
  DEPARTMENT_ID NUMBER(20)  ,
  HEADCOUNT INTEGER default 1   ,
  CANDIDATE_NAME VARCHAR2(200) NOT NULL ,
  CANDIDATE_PHONE VARCHAR2(20)  ,
  CANDIDATE_EMAIL VARCHAR2(200)  ,
  SOURCE INTEGER  ,
  RESUME_ATTACHMENT_ID NUMBER(20)  ,
  STATUS INTEGER NOT NULL ,
  INTERVIEWER_ID NUMBER(20)  ,
  INTERVIEW_DATE DATE  ,
  OFFER_SALARY NUMBER(20,4)  ,
  HIRED_DATE DATE  ,
  EMPLOYEE_ID NUMBER(20)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_recruitment primary key (ID)
);

CREATE TABLE erp_hr_attendance(
  ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  "DATE" DATE NOT NULL ,
  CLOCK_IN DATE  ,
  CLOCK_OUT DATE  ,
  WORK_HOURS NUMBER(10,2)  ,
  LATE_MINUTES INTEGER default 0   ,
  EARLY_LEAVE_MINUTES INTEGER default 0   ,
  IS_ABSENT CHAR(1) default 0   ,
  SOURCE INTEGER  ,
  LEAVE_REQUEST_ID NUMBER(20)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_attendance primary key (ID)
);

CREATE TABLE erp_hr_timesheet_line(
  ID NUMBER(20) NOT NULL ,
  TIMESHEET_ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  WORK_DATE DATE NOT NULL ,
  PROJECT_ID NUMBER(20)  ,
  TASK_ID NUMBER(20)  ,
  ACTIVITY_TYPE VARCHAR2(100)  ,
  HOURS NUMBER(10,2) NOT NULL ,
  DESCRIPTION VARCHAR2(500)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_timesheet_line primary key (ID)
);


      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_auth_user IS 'NopAuthUser';
                
      COMMENT ON TABLE erp_hr_department IS '部门';
                
      COMMENT ON COLUMN erp_hr_department.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_department.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_hr_department.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_hr_department.PARENT_ID IS '上级部门';
                    
      COMMENT ON COLUMN erp_hr_department.MANAGER_ID IS '部门负责人';
                    
      COMMENT ON COLUMN erp_hr_department.COST_CENTER_ID IS '成本中心';
                    
      COMMENT ON COLUMN erp_hr_department.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_department.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_department.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_department.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_department.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_department.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_department.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_department.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_position IS '职位';
                
      COMMENT ON COLUMN erp_hr_position.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_position.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_hr_position.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_hr_position.DEPARTMENT_ID IS '所属部门';
                    
      COMMENT ON COLUMN erp_hr_position.JOB_GRADE IS '职级';
                    
      COMMENT ON COLUMN erp_hr_position.JOB_CATEGORY IS '职位类别';
                    
      COMMENT ON COLUMN erp_hr_position.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_position.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_position.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_position.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_position.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_position.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_position.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_position.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_employee IS '员工';
                
      COMMENT ON COLUMN erp_hr_employee.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_employee.CODE IS '工号';
                    
      COMMENT ON COLUMN erp_hr_employee.FIRST_NAME IS '姓';
                    
      COMMENT ON COLUMN erp_hr_employee.LAST_NAME IS '名';
                    
      COMMENT ON COLUMN erp_hr_employee.FULL_NAME IS '全名';
                    
      COMMENT ON COLUMN erp_hr_employee.GENDER IS '性别';
                    
      COMMENT ON COLUMN erp_hr_employee.BIRTH_DATE IS '出生日期';
                    
      COMMENT ON COLUMN erp_hr_employee.ID_CARD_TYPE IS '证件类型';
                    
      COMMENT ON COLUMN erp_hr_employee.ID_CARD_NO IS '证件号码';
                    
      COMMENT ON COLUMN erp_hr_employee.EMAIL IS '电子邮箱';
                    
      COMMENT ON COLUMN erp_hr_employee.MOBILE_PHONE IS '手机号';
                    
      COMMENT ON COLUMN erp_hr_employee.MARITAL_STATUS IS '婚姻状况';
                    
      COMMENT ON COLUMN erp_hr_employee.NATIONALITY IS '国籍';
                    
      COMMENT ON COLUMN erp_hr_employee.EMERGENCY_CONTACT IS '紧急联系人';
                    
      COMMENT ON COLUMN erp_hr_employee.EMERGENCY_PHONE IS '紧急联系电话';
                    
      COMMENT ON COLUMN erp_hr_employee.DEPARTMENT_ID IS '部门';
                    
      COMMENT ON COLUMN erp_hr_employee.POSITION_ID IS '职位';
                    
      COMMENT ON COLUMN erp_hr_employee.JOB_TITLE IS '岗位名称';
                    
      COMMENT ON COLUMN erp_hr_employee.SUPERIOR_ID IS '直接上级';
                    
      COMMENT ON COLUMN erp_hr_employee.COST_CENTER_ID IS '默认成本中心';
                    
      COMMENT ON COLUMN erp_hr_employee.HIRE_DATE IS '入职日期';
                    
      COMMENT ON COLUMN erp_hr_employee.PROBATION_END_DATE IS '试用期截止';
                    
      COMMENT ON COLUMN erp_hr_employee.REGULAR_DATE IS '转正日期';
                    
      COMMENT ON COLUMN erp_hr_employee.RESIGNATION_DATE IS '离职日期';
                    
      COMMENT ON COLUMN erp_hr_employee.RESIGNATION_REASON IS '离职原因';
                    
      COMMENT ON COLUMN erp_hr_employee.EMPLOYMENT_STATUS IS '雇佣状态';
                    
      COMMENT ON COLUMN erp_hr_employee.EMPLOYEE_TYPE IS '员工类型';
                    
      COMMENT ON COLUMN erp_hr_employee.BANK_ACCOUNT_ID IS '工资卡账户';
                    
      COMMENT ON COLUMN erp_hr_employee.SOCIAL_SECURITY_NO IS '社保号';
                    
      COMMENT ON COLUMN erp_hr_employee.TAX_FILE_NO IS '个税档案号';
                    
      COMMENT ON COLUMN erp_hr_employee.USER_ACCOUNT_ID IS '系统用户ID';
                    
      COMMENT ON COLUMN erp_hr_employee.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_employee.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_employee.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_employee.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_employee.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_employee.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_employee.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_employee.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_employment_contract IS '劳动合同';
                
      COMMENT ON COLUMN erp_hr_employment_contract.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.CODE IS '合同编号';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.CONTRACT_TYPE IS '合同类型';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.SIGN_DATE IS '签订日期';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.START_DATE IS '生效日期';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.END_DATE IS '到期日期';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.PROBATION_MONTHS IS '试用期月数';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.WORKING_HOURS_PER_WEEK IS '每周工时';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.ANNUAL_SALARY IS '年薪(税前)';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.MONTHLY_SALARY IS '月薪(税前)';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.SALARY_CURRENCY_ID IS '薪资币种';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.SALARY_PAY_METHOD IS '发薪方式';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.SOCIAL_INSURANCE_BASE IS '社保基数';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.HOUSING_FUND_BASE IS '公积金基数';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.STATUS IS '合同状态';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.ATTACHMENT_ID IS '合同文件';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_employment_contract.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_leave_request IS '休假申请';
                
      COMMENT ON COLUMN erp_hr_leave_request.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_leave_request.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_hr_leave_request.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_leave_request.LEAVE_TYPE IS '休假类型';
                    
      COMMENT ON COLUMN erp_hr_leave_request.START_DATE IS '开始日期';
                    
      COMMENT ON COLUMN erp_hr_leave_request.END_DATE IS '结束日期';
                    
      COMMENT ON COLUMN erp_hr_leave_request.DURATION_DAYS IS '天数';
                    
      COMMENT ON COLUMN erp_hr_leave_request.REASON IS '请假原因';
                    
      COMMENT ON COLUMN erp_hr_leave_request.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_leave_request.APPROVER_ID IS '审批人';
                    
      COMMENT ON COLUMN erp_hr_leave_request.APPROVED_AT IS '审批时间';
                    
      COMMENT ON COLUMN erp_hr_leave_request.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_leave_request.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_leave_request.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_leave_request.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_leave_request.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_leave_request.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_leave_request.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_leave_request.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_timesheet IS '工时表';
                
      COMMENT ON COLUMN erp_hr_timesheet.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_timesheet.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_hr_timesheet.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_timesheet.PERIOD_FROM IS '周期开始';
                    
      COMMENT ON COLUMN erp_hr_timesheet.PERIOD_TO IS '周期结束';
                    
      COMMENT ON COLUMN erp_hr_timesheet.TOTAL_HOURS IS '总工时';
                    
      COMMENT ON COLUMN erp_hr_timesheet.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_timesheet.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_timesheet.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_timesheet.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_timesheet.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_timesheet.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_timesheet.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_timesheet.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_timesheet.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_salary IS '薪酬记录';
                
      COMMENT ON COLUMN erp_hr_salary.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_salary.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_salary.YEAR IS '年份';
                    
      COMMENT ON COLUMN erp_hr_salary.MONTH IS '月份';
                    
      COMMENT ON COLUMN erp_hr_salary.BASIC_SALARY IS '基本工资';
                    
      COMMENT ON COLUMN erp_hr_salary.POSITION_ALLOWANCE IS '岗位津贴';
                    
      COMMENT ON COLUMN erp_hr_salary.PERFORMANCE_BONUS IS '绩效奖金';
                    
      COMMENT ON COLUMN erp_hr_salary.OVERTIME_PAY IS '加班费';
                    
      COMMENT ON COLUMN erp_hr_salary.MEAL_ALLOWANCE IS '餐补';
                    
      COMMENT ON COLUMN erp_hr_salary.TRANSPORT_ALLOWANCE IS '交通补贴';
                    
      COMMENT ON COLUMN erp_hr_salary.OTHER_ALLOWANCE IS '其他补贴';
                    
      COMMENT ON COLUMN erp_hr_salary.GROSS_SALARY IS '应发合计';
                    
      COMMENT ON COLUMN erp_hr_salary.SOCIAL_INSURANCE IS '社保个人部分';
                    
      COMMENT ON COLUMN erp_hr_salary.HOUSING_FUND IS '公积金个人部分';
                    
      COMMENT ON COLUMN erp_hr_salary.TAX_AMOUNT IS '个税';
                    
      COMMENT ON COLUMN erp_hr_salary.OTHER_DEDUCTIONS IS '其他扣款';
                    
      COMMENT ON COLUMN erp_hr_salary.NET_SALARY IS '实发合计';
                    
      COMMENT ON COLUMN erp_hr_salary.PAYMENT_STATUS IS '支付状态';
                    
      COMMENT ON COLUMN erp_hr_salary.PAYMENT_DATE IS '实发日期';
                    
      COMMENT ON COLUMN erp_hr_salary.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_salary.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_salary.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_salary.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_salary.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_salary.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_salary.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_salary.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_recruitment IS '招聘记录';
                
      COMMENT ON COLUMN erp_hr_recruitment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_recruitment.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_hr_recruitment.POSITION_ID IS '招聘职位';
                    
      COMMENT ON COLUMN erp_hr_recruitment.DEPARTMENT_ID IS '招聘部门';
                    
      COMMENT ON COLUMN erp_hr_recruitment.HEADCOUNT IS '招聘人数';
                    
      COMMENT ON COLUMN erp_hr_recruitment.CANDIDATE_NAME IS '应聘者姓名';
                    
      COMMENT ON COLUMN erp_hr_recruitment.CANDIDATE_PHONE IS '联系电话';
                    
      COMMENT ON COLUMN erp_hr_recruitment.CANDIDATE_EMAIL IS '电子邮箱';
                    
      COMMENT ON COLUMN erp_hr_recruitment.SOURCE IS '来源';
                    
      COMMENT ON COLUMN erp_hr_recruitment.RESUME_ATTACHMENT_ID IS '简历附件';
                    
      COMMENT ON COLUMN erp_hr_recruitment.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_recruitment.INTERVIEWER_ID IS '面试官';
                    
      COMMENT ON COLUMN erp_hr_recruitment.INTERVIEW_DATE IS '面试日期';
                    
      COMMENT ON COLUMN erp_hr_recruitment.OFFER_SALARY IS 'Offer薪资';
                    
      COMMENT ON COLUMN erp_hr_recruitment.HIRED_DATE IS '入职日期';
                    
      COMMENT ON COLUMN erp_hr_recruitment.EMPLOYEE_ID IS '关联员工';
                    
      COMMENT ON COLUMN erp_hr_recruitment.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_recruitment.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_recruitment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_recruitment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_recruitment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_recruitment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_recruitment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_recruitment.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_attendance IS '考勤记录';
                
      COMMENT ON COLUMN erp_hr_attendance.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_attendance.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_attendance."DATE" IS '考勤日期';
                    
      COMMENT ON COLUMN erp_hr_attendance.CLOCK_IN IS '签到时间';
                    
      COMMENT ON COLUMN erp_hr_attendance.CLOCK_OUT IS '签退时间';
                    
      COMMENT ON COLUMN erp_hr_attendance.WORK_HOURS IS '出勤时长';
                    
      COMMENT ON COLUMN erp_hr_attendance.LATE_MINUTES IS '迟到分钟数';
                    
      COMMENT ON COLUMN erp_hr_attendance.EARLY_LEAVE_MINUTES IS '早退分钟数';
                    
      COMMENT ON COLUMN erp_hr_attendance.IS_ABSENT IS '是否旷工';
                    
      COMMENT ON COLUMN erp_hr_attendance.SOURCE IS '来源';
                    
      COMMENT ON COLUMN erp_hr_attendance.LEAVE_REQUEST_ID IS '关联休假';
                    
      COMMENT ON COLUMN erp_hr_attendance.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_attendance.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_attendance.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_attendance.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_attendance.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_attendance.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_attendance.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_attendance.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_timesheet_line IS '工时表明细';
                
      COMMENT ON COLUMN erp_hr_timesheet_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.TIMESHEET_ID IS '工时表ID';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.WORK_DATE IS '工作日期';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.TASK_ID IS '任务';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.ACTIVITY_TYPE IS '活动类型';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.HOURS IS '小时数';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.DESCRIPTION IS '工作内容';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_timesheet_line.UPDATE_TIME IS '修改时间';
                    
