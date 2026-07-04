
CREATE TABLE erp_md_cost_center(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_cost_center primary key (ID)
);

CREATE TABLE erp_md_bank_account(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  BANKNAME VARCHAR2(50)  ,
  constraint PK_erp_md_bank_account primary key (ID)
);

CREATE TABLE erp_md_md_organization(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_organization primary key (ID)
);

CREATE TABLE erp_md_currency(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_currency primary key (ID)
);

CREATE TABLE erp_prj_project(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_prj_project primary key (ID)
);

CREATE TABLE erp_prj_task(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_prj_task primary key (ID)
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

CREATE TABLE erp_hr_salary_item(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ITEM_CATEGORY VARCHAR2(20) NOT NULL ,
  ITEM_GROUP VARCHAR2(20) NOT NULL ,
  CALC_METHOD VARCHAR2(20) NOT NULL ,
  FORMULA VARCHAR2(1000)  ,
  IS_TAXABLE CHAR(1) default 0   ,
  IS_SOCIAL_INSURANCE_BASE CHAR(1) default 0   ,
  IS_MANDATORY CHAR(1) default 0   ,
  SORT_ORDER INTEGER default 0   ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_salary_item primary key (ID)
);

CREATE TABLE erp_hr_social_insurance_config(
  ID NUMBER(20) NOT NULL ,
  CITY_CODE VARCHAR2(20) NOT NULL ,
  INSURANCE_TYPE VARCHAR2(20) NOT NULL ,
  COMPANY_RATE NUMBER(8,6) default 0   ,
  EMPLOYEE_RATE NUMBER(8,6) default 0   ,
  BASE_LOWER_LIMIT NUMBER(18,2)  ,
  BASE_UPPER_LIMIT NUMBER(18,2)  ,
  EFFECTIVE_FROM DATE  ,
  EFFECTIVE_TO DATE  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_social_insurance_config primary key (ID)
);

CREATE TABLE erp_hr_tax_config(
  ID NUMBER(20) NOT NULL ,
  YEAR INTEGER NOT NULL ,
  TAX_THRESHOLD NUMBER(18,2) NOT NULL ,
  TAX_BRACKETS VARCHAR2(4000)  ,
  SPECIAL_DEDUCTION_ITEMS VARCHAR2(2000)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_tax_config primary key (ID)
);

CREATE TABLE erp_hr_payroll_bank_file(
  ID NUMBER(20) NOT NULL ,
  BATCH_NO VARCHAR2(50) NOT NULL ,
  PAYMENT_DATE DATE NOT NULL ,
  TOTAL_AMOUNT NUMBER(18,2) NOT NULL ,
  RECORD_COUNT INTEGER default 0  NOT NULL ,
  FILE_FORMAT VARCHAR2(20) NOT NULL ,
  FILE_CONTENT CLOB  ,
  STATUS VARCHAR2(20) default 'GENERATED'  NOT NULL ,
  BANK_ID NUMBER(20)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_payroll_bank_file primary key (ID)
);

CREATE TABLE erp_hr_shift(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  NAME VARCHAR2(200) NOT NULL ,
  SHIFT_TYPE VARCHAR2(20) NOT NULL ,
  START_TIME VARCHAR2(8) NOT NULL ,
  END_TIME VARCHAR2(8) NOT NULL ,
  GRACE_LATE_MINUTES INTEGER default 15   ,
  GRACE_EARLY_LEAVE_MINUTES INTEGER default 15   ,
  REQUIRE_CLOCK_IN CHAR(1) default 1   ,
  REQUIRE_CLOCK_OUT CHAR(1) default 1   ,
  REST_START_TIME VARCHAR2(8)  ,
  REST_END_TIME VARCHAR2(8)  ,
  TOTAL_WORK_MINUTES INTEGER  ,
  ALLOW_OVERTIME CHAR(1) default 1   ,
  COLOR_HEX VARCHAR2(10)  ,
  DESCRIPTION VARCHAR2(500)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_shift primary key (ID)
);

CREATE TABLE erp_hr_shift_rotation_pattern(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PATTERN_TYPE VARCHAR2(50)  ,
  PATTERN_DATA VARCHAR2(2000)  ,
  ROTATE_INTERVAL INTEGER  ,
  START_DATE DATE  ,
  GROUP_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_shift_rotation_pattern primary key (ID)
);

CREATE TABLE erp_hr_competency(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  DESCRIPTION VARCHAR2(1000)  ,
  CATEGORY VARCHAR2(20) NOT NULL ,
  COMPETENCY_GROUP VARCHAR2(100)  ,
  IS_TECHNICAL CHAR(1) default 0   ,
  PARENT_ID NUMBER(20)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_competency primary key (ID)
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

CREATE TABLE erp_hr_survey(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  TITLE VARCHAR2(200) NOT NULL ,
  DESCRIPTION VARCHAR2(2000)  ,
  SURVEY_TYPE VARCHAR2(20) NOT NULL ,
  IS_ANONYMOUS CHAR(1) default 1   ,
  STATUS VARCHAR2(20) NOT NULL ,
  START_DATE DATE  ,
  END_DATE DATE  ,
  TARGET_DEPARTMENT_ID NUMBER(20)  ,
  INCLUDE_ENPS CHAR(1) default 0   ,
  ENPS_QUESTION VARCHAR2(500)  ,
  REMINDER_DAYS INTEGER default 3   ,
  TOTAL_QUESTIONS INTEGER default 0   ,
  TOTAL_RESPONSES INTEGER default 0   ,
  COMPLETION_RATE NUMBER(5,2)  ,
  AVG_SCORE NUMBER(5,2)  ,
  ENPS_SCORE INTEGER  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey primary key (ID)
);

CREATE TABLE erp_hr_competency_level(
  ID NUMBER(20) NOT NULL ,
  COMPETENCY_ID NUMBER(20) NOT NULL ,
  LEVEL_NUMBER INTEGER NOT NULL ,
  LEVEL_NAME VARCHAR2(100)  ,
  BEHAVIORAL_ANCHOR VARCHAR2(1000)  ,
  SORT_ORDER INTEGER  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_competency_level primary key (ID)
);

CREATE TABLE erp_hr_employee(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  FIRST_NAME VARCHAR2(100) NOT NULL ,
  LAST_NAME VARCHAR2(100) NOT NULL ,
  FULL_NAME VARCHAR2(200)  ,
  GENDER VARCHAR2(20) NOT NULL ,
  BIRTH_DATE DATE  ,
  ID_CARD_TYPE VARCHAR2(50)  ,
  ID_CARD_NO VARCHAR2(50)  ,
  EMAIL VARCHAR2(200)  ,
  MOBILE_PHONE VARCHAR2(20)  ,
  MARITAL_STATUS VARCHAR2(20)  ,
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
  EMPLOYMENT_STATUS VARCHAR2(20) NOT NULL ,
  EMPLOYEE_TYPE VARCHAR2(20) NOT NULL ,
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

CREATE TABLE erp_hr_role_competency(
  ID NUMBER(20) NOT NULL ,
  POSITION_ID NUMBER(20) NOT NULL ,
  COMPETENCY_ID NUMBER(20) NOT NULL ,
  REQUIRED_LEVEL INTEGER NOT NULL ,
  WEIGHT NUMBER(5,2) default 1.0   ,
  IS_CRITICAL CHAR(1) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_role_competency primary key (ID)
);

CREATE TABLE erp_hr_survey_question(
  ID NUMBER(20) NOT NULL ,
  SURVEY_ID NUMBER(20) NOT NULL ,
  SORT_ORDER INTEGER  ,
  QUESTION_TEXT VARCHAR2(2000) NOT NULL ,
  QUESTION_TYPE VARCHAR2(20) NOT NULL ,
  RATING_SCALE_MIN INTEGER default 1   ,
  RATING_SCALE_MAX INTEGER default 5   ,
  RATING_LABEL_MIN VARCHAR2(100)  ,
  RATING_LABEL_MAX VARCHAR2(100)  ,
  OPTIONS VARCHAR2(2000)  ,
  DRIVER_CATEGORY VARCHAR2(20)  ,
  IS_REQUIRED CHAR(1) default 1   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey_question primary key (ID)
);

CREATE TABLE erp_hr_survey_result(
  ID NUMBER(20) NOT NULL ,
  SURVEY_ID NUMBER(20) NOT NULL ,
  DEPARTMENT_ID NUMBER(20)  ,
  TOTAL_RESPONSES INTEGER default 0   ,
  AVG_SCORE NUMBER(5,2)  ,
  ENPS_SCORE INTEGER  ,
  DRIVER_SCORES VARCHAR2(2000)  ,
  QUESTION_BREAKDOWN VARCHAR2(4000)  ,
  TREND_DATA VARCHAR2(4000)  ,
  LAST_CALCULATED_AT DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey_result primary key (ID)
);

CREATE TABLE erp_hr_employment_contract(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  CONTRACT_TYPE VARCHAR2(20) NOT NULL ,
  SIGN_DATE DATE NOT NULL ,
  START_DATE DATE NOT NULL ,
  END_DATE DATE  ,
  PROBATION_MONTHS INTEGER  ,
  WORKING_HOURS_PER_WEEK NUMBER(5,1)  ,
  ANNUAL_SALARY NUMBER(18,2)  ,
  MONTHLY_SALARY NUMBER(18,2)  ,
  SALARY_CURRENCY_ID NUMBER(20)  ,
  SALARY_PAY_METHOD VARCHAR2(50)  ,
  SOCIAL_INSURANCE_BASE NUMBER(18,2)  ,
  HOUSING_FUND_BASE NUMBER(18,2)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  ATTACHMENT_FILE_ID VARCHAR2(200)  ,
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
  LEAVE_TYPE VARCHAR2(20) NOT NULL ,
  START_DATE DATE NOT NULL ,
  END_DATE DATE NOT NULL ,
  DURATION_DAYS NUMBER(10,2)  ,
  REASON VARCHAR2(500)  ,
  STATUS VARCHAR2(20) NOT NULL ,
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
  STATUS VARCHAR2(20) NOT NULL ,
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
  BASIC_SALARY NUMBER(18,2)  ,
  POSITION_ALLOWANCE NUMBER(18,2)  ,
  PERFORMANCE_BONUS NUMBER(18,2)  ,
  OVERTIME_PAY NUMBER(18,2)  ,
  MEAL_ALLOWANCE NUMBER(18,2)  ,
  TRANSPORT_ALLOWANCE NUMBER(18,2)  ,
  OTHER_ALLOWANCE NUMBER(18,2)  ,
  GROSS_SALARY NUMBER(18,2)  ,
  SOCIAL_INSURANCE NUMBER(18,2)  ,
  HOUSING_FUND NUMBER(18,2)  ,
  TAX_AMOUNT NUMBER(18,2)  ,
  OTHER_DEDUCTIONS NUMBER(18,2)  ,
  NET_SALARY NUMBER(18,2)  ,
  PAYMENT_STATUS VARCHAR2(20) NOT NULL ,
  PAYMENT_DATE DATE  ,
  APPROVAL_STATUS VARCHAR2(20) default 'PENDING'  NOT NULL ,
  PERFORMANCE_FACTOR NUMBER(5,2)  ,
  ACTUAL_WORK_DAYS NUMBER(10,2)  ,
  REQUIRED_WORK_DAYS NUMBER(10,2)  ,
  TOTAL_OVERTIME_HOURS NUMBER(10,2)  ,
  UNPAID_LEAVE_DAYS NUMBER(10,2)  ,
  CUMULATIVE_DATA VARCHAR2(2000)  ,
  REVIEW_NOTE VARCHAR2(1000)  ,
  PAYMENT_BATCH_NO VARCHAR2(100)  ,
  BANK_FILE_ID NUMBER(20)  ,
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
  SOURCE VARCHAR2(20)  ,
  RESUME_ATTACHMENT_FILE_ID VARCHAR2(200)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  INTERVIEWER_ID NUMBER(20)  ,
  INTERVIEW_DATE DATE  ,
  OFFER_SALARY NUMBER(18,2)  ,
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

CREATE TABLE erp_hr_social_insurance_base(
  ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  CITY_CODE VARCHAR2(20) NOT NULL ,
  SOCIAL_INSURANCE_BASE NUMBER(18,2) NOT NULL ,
  HOUSING_FUND_BASE NUMBER(18,2)  ,
  EFFECTIVE_FROM DATE NOT NULL ,
  EFFECTIVE_TO DATE  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_social_insurance_base primary key (ID)
);

CREATE TABLE erp_hr_tax_special_deduction(
  ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  YEAR INTEGER NOT NULL ,
  MONTH INTEGER NOT NULL ,
  DEDUCTION_TYPE VARCHAR2(30) NOT NULL ,
  MONTHLY_AMOUNT NUMBER(18,2) NOT NULL ,
  VERIFIED CHAR(1) default 0   ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_tax_special_deduction primary key (ID)
);

CREATE TABLE erp_hr_survey_response(
  ID NUMBER(20) NOT NULL ,
  SURVEY_ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20)  ,
  RESPONDENT_HASH VARCHAR2(100)  ,
  SUBMITTED_AT DATE  ,
  TIME_SPENT_SECONDS INTEGER  ,
  IS_COMPLETE CHAR(1) default 0   ,
  ORG_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey_response primary key (ID)
);

CREATE TABLE erp_hr_employee_assessment(
  ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  ASSESSMENT_TYPE VARCHAR2(20) NOT NULL ,
  ASSESSOR_ID NUMBER(20)  ,
  ASSESSMENT_DATE DATE  ,
  STATUS VARCHAR2(20) NOT NULL ,
  OVERALL_SCORE NUMBER(5,2)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_employee_assessment primary key (ID)
);

CREATE TABLE erp_hr_gap_analysis(
  ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  COMPETENCY_ID NUMBER(20) NOT NULL ,
  REQUIRED_LEVEL INTEGER  ,
  ACTUAL_LEVEL INTEGER  ,
  GAP_VALUE INTEGER  ,
  GAP_SEVERITY VARCHAR2(20)  ,
  ASSESSMENT_DATE DATE  ,
  ANALYSIS_DATE DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_gap_analysis primary key (ID)
);

CREATE TABLE erp_hr_development_plan(
  ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  PLAN_NAME VARCHAR2(200)  ,
  TARGET_DATE DATE  ,
  STATUS VARCHAR2(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_development_plan primary key (ID)
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
  SOURCE VARCHAR2(20)  ,
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

CREATE TABLE erp_hr_shift_assignment(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  SHIFT_ID NUMBER(20) NOT NULL ,
  ASSIGNMENT_DATE DATE NOT NULL ,
  ACTUAL_START_TIME DATE  ,
  ACTUAL_END_TIME DATE  ,
  IS_ABSENT CHAR(1) default 0   ,
  ABSENCE_REASON VARCHAR2(50)  ,
  LEAVE_REQUEST_ID NUMBER(20)  ,
  SWAP_REQUEST_ID NUMBER(20)  ,
  REPLACED_BY_ASSIGNMENT_ID NUMBER(20)  ,
  STATUS VARCHAR2(50)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_shift_assignment primary key (ID)
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

CREATE TABLE erp_hr_salary_simulation(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SOURCE_SALARY_ID NUMBER(20)  ,
  SIMULATION_PERIOD_YEAR INTEGER NOT NULL ,
  SIMULATION_PERIOD_MONTH INTEGER NOT NULL ,
  SIMULATION_NAME VARCHAR2(200)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  REVIEWER_ID NUMBER(20)  ,
  REVIEWED_AT DATE  ,
  CONVERTED_AT DATE  ,
  CONVERTED_SALARY_ID NUMBER(20)  ,
  NOTES VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_salary_simulation primary key (ID)
);

CREATE TABLE erp_hr_survey_answer(
  ID NUMBER(20) NOT NULL ,
  RESPONSE_ID NUMBER(20) NOT NULL ,
  QUESTION_ID NUMBER(20) NOT NULL ,
  RATING_VALUE INTEGER  ,
  SELECTED_OPTION VARCHAR2(500)  ,
  OPEN_TEXT VARCHAR2(4000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_survey_answer primary key (ID)
);

CREATE TABLE erp_hr_assessment_detail(
  ID NUMBER(20) NOT NULL ,
  ASSESSMENT_ID NUMBER(20) NOT NULL ,
  COMPETENCY_ID NUMBER(20) NOT NULL ,
  ACTUAL_LEVEL INTEGER  ,
  "COMMENT" VARCHAR2(1000)  ,
  SOURCE_TYPE VARCHAR2(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_assessment_detail primary key (ID)
);

CREATE TABLE erp_hr_development_plan_item(
  ID NUMBER(20) NOT NULL ,
  PLAN_ID NUMBER(20) NOT NULL ,
  COMPETENCY_ID NUMBER(20) NOT NULL ,
  GAP_ID NUMBER(20)  ,
  TARGET_LEVEL INTEGER NOT NULL ,
  DEVELOPMENT_ACTION VARCHAR2(500)  ,
  MENTOR_ID NUMBER(20)  ,
  START_DATE DATE  ,
  END_DATE DATE  ,
  STATUS VARCHAR2(20) NOT NULL ,
  PROGRESS_NOTE VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_development_plan_item primary key (ID)
);

CREATE TABLE erp_hr_shift_swap_request(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REQUESTER_ID NUMBER(20) NOT NULL ,
  TARGET_EMPLOYEE_ID NUMBER(20)  ,
  SOURCE_ASSIGNMENT_ID NUMBER(20) NOT NULL ,
  TARGET_ASSIGNMENT_ID NUMBER(20)  ,
  SWAP_DATE DATE NOT NULL ,
  REASON VARCHAR2(500)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  APPROVED_BY_ID VARCHAR2(36)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_shift_swap_request primary key (ID)
);

CREATE TABLE erp_hr_salary_simulation_item_adj(
  ID NUMBER(20) NOT NULL ,
  SIMULATION_ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  SALARY_ITEM_CODE VARCHAR2(50) NOT NULL ,
  ORIGINAL_AMOUNT NUMBER(18,2) NOT NULL ,
  ADJUSTED_AMOUNT NUMBER(18,2) NOT NULL ,
  ADJUSTMENT_REASON VARCHAR2(20)  ,
  ADJUSTED_BY VARCHAR2(50)  ,
  ADJUSTED_AT DATE  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_hr_salary_simulation_item_adj primary key (ID)
);


      COMMENT ON TABLE erp_md_cost_center IS '成本中心';
                
      COMMENT ON TABLE erp_md_bank_account IS '银行账户';
                
      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_prj_project IS '项目';
                
      COMMENT ON TABLE erp_prj_task IS '任务';
                
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
                    
      COMMENT ON TABLE erp_hr_salary_item IS '薪酬项目';
                
      COMMENT ON COLUMN erp_hr_salary_item.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_salary_item.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_hr_salary_item.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_hr_salary_item.ITEM_CATEGORY IS '类别';
                    
      COMMENT ON COLUMN erp_hr_salary_item.ITEM_GROUP IS '分组';
                    
      COMMENT ON COLUMN erp_hr_salary_item.CALC_METHOD IS '计算方式';
                    
      COMMENT ON COLUMN erp_hr_salary_item.FORMULA IS '计算公式';
                    
      COMMENT ON COLUMN erp_hr_salary_item.IS_TAXABLE IS '是否应税';
                    
      COMMENT ON COLUMN erp_hr_salary_item.IS_SOCIAL_INSURANCE_BASE IS '计入社保基数';
                    
      COMMENT ON COLUMN erp_hr_salary_item.IS_MANDATORY IS '是否必含';
                    
      COMMENT ON COLUMN erp_hr_salary_item.SORT_ORDER IS '排序号';
                    
      COMMENT ON COLUMN erp_hr_salary_item.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_salary_item.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_salary_item.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_salary_item.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_salary_item.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_salary_item.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_salary_item.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_salary_item.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_social_insurance_config IS '社保配置';
                
      COMMENT ON COLUMN erp_hr_social_insurance_config.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.CITY_CODE IS '城市';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.INSURANCE_TYPE IS '险种';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.COMPANY_RATE IS '公司比例';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.EMPLOYEE_RATE IS '个人比例';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.BASE_LOWER_LIMIT IS '基数下限';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.BASE_UPPER_LIMIT IS '基数上限';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.EFFECTIVE_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.EFFECTIVE_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_config.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_tax_config IS '个税配置';
                
      COMMENT ON COLUMN erp_hr_tax_config.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_tax_config.YEAR IS '年份';
                    
      COMMENT ON COLUMN erp_hr_tax_config.TAX_THRESHOLD IS '起征点(月)';
                    
      COMMENT ON COLUMN erp_hr_tax_config.TAX_BRACKETS IS '税率表';
                    
      COMMENT ON COLUMN erp_hr_tax_config.SPECIAL_DEDUCTION_ITEMS IS '专项附加扣除项配置';
                    
      COMMENT ON COLUMN erp_hr_tax_config.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_tax_config.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_tax_config.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_tax_config.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_tax_config.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_tax_config.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_tax_config.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_tax_config.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_payroll_bank_file IS '银行代发文件';
                
      COMMENT ON COLUMN erp_hr_payroll_bank_file.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.BATCH_NO IS '批次号';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.PAYMENT_DATE IS '发放日期';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.TOTAL_AMOUNT IS '总金额';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.RECORD_COUNT IS '记录数';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.FILE_FORMAT IS '文件格式';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.FILE_CONTENT IS '文件内容';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.BANK_ID IS '开户银行';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_payroll_bank_file.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_shift IS '班次模板';
                
      COMMENT ON COLUMN erp_hr_shift.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_shift.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_hr_shift.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_shift.NAME IS '班次名称';
                    
      COMMENT ON COLUMN erp_hr_shift.SHIFT_TYPE IS '班次类型';
                    
      COMMENT ON COLUMN erp_hr_shift.START_TIME IS '上班时间';
                    
      COMMENT ON COLUMN erp_hr_shift.END_TIME IS '下班时间';
                    
      COMMENT ON COLUMN erp_hr_shift.GRACE_LATE_MINUTES IS '迟到宽容分钟数';
                    
      COMMENT ON COLUMN erp_hr_shift.GRACE_EARLY_LEAVE_MINUTES IS '早退宽容分钟数';
                    
      COMMENT ON COLUMN erp_hr_shift.REQUIRE_CLOCK_IN IS '需签到';
                    
      COMMENT ON COLUMN erp_hr_shift.REQUIRE_CLOCK_OUT IS '需签退';
                    
      COMMENT ON COLUMN erp_hr_shift.REST_START_TIME IS '休息开始';
                    
      COMMENT ON COLUMN erp_hr_shift.REST_END_TIME IS '休息结束';
                    
      COMMENT ON COLUMN erp_hr_shift.TOTAL_WORK_MINUTES IS '标准工时(分钟)';
                    
      COMMENT ON COLUMN erp_hr_shift.ALLOW_OVERTIME IS '允许加班';
                    
      COMMENT ON COLUMN erp_hr_shift.COLOR_HEX IS '显示颜色';
                    
      COMMENT ON COLUMN erp_hr_shift.DESCRIPTION IS '说明';
                    
      COMMENT ON COLUMN erp_hr_shift.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_shift.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_shift.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_shift.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_shift.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_shift.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_shift_rotation_pattern IS '轮换排班模板';
                
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.PATTERN_TYPE IS '轮换类型';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.PATTERN_DATA IS '轮换序列';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.ROTATE_INTERVAL IS '轮换间隔(天)';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.START_DATE IS '起始日期';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.GROUP_ID IS '轮换组';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_shift_rotation_pattern.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_competency IS '胜任力字典';
                
      COMMENT ON COLUMN erp_hr_competency.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_competency.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_hr_competency.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_hr_competency.DESCRIPTION IS '描述';
                    
      COMMENT ON COLUMN erp_hr_competency.CATEGORY IS '分类';
                    
      COMMENT ON COLUMN erp_hr_competency.COMPETENCY_GROUP IS '能力组';
                    
      COMMENT ON COLUMN erp_hr_competency.IS_TECHNICAL IS '是否技术能力';
                    
      COMMENT ON COLUMN erp_hr_competency.PARENT_ID IS '上级胜任力';
                    
      COMMENT ON COLUMN erp_hr_competency.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_competency.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_competency.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_competency.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_competency.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_competency.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_competency.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_competency.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_hr_survey IS '问卷模板';
                
      COMMENT ON COLUMN erp_hr_survey.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_hr_survey.TITLE IS '问卷标题';
                    
      COMMENT ON COLUMN erp_hr_survey.DESCRIPTION IS '问卷说明';
                    
      COMMENT ON COLUMN erp_hr_survey.SURVEY_TYPE IS '调研类型';
                    
      COMMENT ON COLUMN erp_hr_survey.IS_ANONYMOUS IS '是否匿名';
                    
      COMMENT ON COLUMN erp_hr_survey.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_survey.START_DATE IS '开始日期';
                    
      COMMENT ON COLUMN erp_hr_survey.END_DATE IS '截止日期';
                    
      COMMENT ON COLUMN erp_hr_survey.TARGET_DEPARTMENT_ID IS '目标部门';
                    
      COMMENT ON COLUMN erp_hr_survey.INCLUDE_ENPS IS '包含eNPS';
                    
      COMMENT ON COLUMN erp_hr_survey.ENPS_QUESTION IS 'eNPS题面';
                    
      COMMENT ON COLUMN erp_hr_survey.REMINDER_DAYS IS '催填间隔天数';
                    
      COMMENT ON COLUMN erp_hr_survey.TOTAL_QUESTIONS IS '总题数';
                    
      COMMENT ON COLUMN erp_hr_survey.TOTAL_RESPONSES IS '总答卷数';
                    
      COMMENT ON COLUMN erp_hr_survey.COMPLETION_RATE IS '完成率';
                    
      COMMENT ON COLUMN erp_hr_survey.AVG_SCORE IS '平均分';
                    
      COMMENT ON COLUMN erp_hr_survey.ENPS_SCORE IS 'eNPS得分';
                    
      COMMENT ON COLUMN erp_hr_survey.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_survey.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_survey.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_competency_level IS '胜任力等级';
                
      COMMENT ON COLUMN erp_hr_competency_level.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_competency_level.COMPETENCY_ID IS '胜任力';
                    
      COMMENT ON COLUMN erp_hr_competency_level.LEVEL_NUMBER IS '等级编号';
                    
      COMMENT ON COLUMN erp_hr_competency_level.LEVEL_NAME IS '等级名称';
                    
      COMMENT ON COLUMN erp_hr_competency_level.BEHAVIORAL_ANCHOR IS '行为锚定';
                    
      COMMENT ON COLUMN erp_hr_competency_level.SORT_ORDER IS '排序号';
                    
      COMMENT ON COLUMN erp_hr_competency_level.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_competency_level.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_competency_level.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_competency_level.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_competency_level.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_competency_level.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_hr_role_competency IS '岗位胜任力要求';
                
      COMMENT ON COLUMN erp_hr_role_competency.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_role_competency.POSITION_ID IS '岗位';
                    
      COMMENT ON COLUMN erp_hr_role_competency.COMPETENCY_ID IS '胜任力';
                    
      COMMENT ON COLUMN erp_hr_role_competency.REQUIRED_LEVEL IS '要求等级';
                    
      COMMENT ON COLUMN erp_hr_role_competency.WEIGHT IS '权重';
                    
      COMMENT ON COLUMN erp_hr_role_competency.IS_CRITICAL IS '是否关键';
                    
      COMMENT ON COLUMN erp_hr_role_competency.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_role_competency.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_role_competency.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_role_competency.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_role_competency.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_role_competency.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_survey_question IS '问卷题目';
                
      COMMENT ON COLUMN erp_hr_survey_question.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey_question.SURVEY_ID IS '所属问卷';
                    
      COMMENT ON COLUMN erp_hr_survey_question.SORT_ORDER IS '排序号';
                    
      COMMENT ON COLUMN erp_hr_survey_question.QUESTION_TEXT IS '题目内容';
                    
      COMMENT ON COLUMN erp_hr_survey_question.QUESTION_TYPE IS '题型';
                    
      COMMENT ON COLUMN erp_hr_survey_question.RATING_SCALE_MIN IS '评分最低分';
                    
      COMMENT ON COLUMN erp_hr_survey_question.RATING_SCALE_MAX IS '评分最高分';
                    
      COMMENT ON COLUMN erp_hr_survey_question.RATING_LABEL_MIN IS '最低分标签';
                    
      COMMENT ON COLUMN erp_hr_survey_question.RATING_LABEL_MAX IS '最高分标签';
                    
      COMMENT ON COLUMN erp_hr_survey_question.OPTIONS IS '选项列表';
                    
      COMMENT ON COLUMN erp_hr_survey_question.DRIVER_CATEGORY IS '驱动因子分类';
                    
      COMMENT ON COLUMN erp_hr_survey_question.IS_REQUIRED IS '是否必填';
                    
      COMMENT ON COLUMN erp_hr_survey_question.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey_question.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey_question.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey_question.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey_question.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey_question.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_survey_result IS '调研结果';
                
      COMMENT ON COLUMN erp_hr_survey_result.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey_result.SURVEY_ID IS '所属问卷';
                    
      COMMENT ON COLUMN erp_hr_survey_result.DEPARTMENT_ID IS '部门';
                    
      COMMENT ON COLUMN erp_hr_survey_result.TOTAL_RESPONSES IS '答卷数';
                    
      COMMENT ON COLUMN erp_hr_survey_result.AVG_SCORE IS '平均分';
                    
      COMMENT ON COLUMN erp_hr_survey_result.ENPS_SCORE IS 'eNPS得分';
                    
      COMMENT ON COLUMN erp_hr_survey_result.DRIVER_SCORES IS '驱动因子得分';
                    
      COMMENT ON COLUMN erp_hr_survey_result.QUESTION_BREAKDOWN IS '每题得分';
                    
      COMMENT ON COLUMN erp_hr_survey_result.TREND_DATA IS '历史趋势';
                    
      COMMENT ON COLUMN erp_hr_survey_result.LAST_CALCULATED_AT IS '最后计算时间';
                    
      COMMENT ON COLUMN erp_hr_survey_result.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey_result.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey_result.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey_result.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey_result.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey_result.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON COLUMN erp_hr_employment_contract.ATTACHMENT_FILE_ID IS '合同文件';
                    
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
                    
      COMMENT ON COLUMN erp_hr_salary.APPROVAL_STATUS IS '审批状态';
                    
      COMMENT ON COLUMN erp_hr_salary.PERFORMANCE_FACTOR IS '绩效系数';
                    
      COMMENT ON COLUMN erp_hr_salary.ACTUAL_WORK_DAYS IS '实际出勤日';
                    
      COMMENT ON COLUMN erp_hr_salary.REQUIRED_WORK_DAYS IS '应出勤日';
                    
      COMMENT ON COLUMN erp_hr_salary.TOTAL_OVERTIME_HOURS IS '月总加班小时';
                    
      COMMENT ON COLUMN erp_hr_salary.UNPAID_LEAVE_DAYS IS '无薪假天数';
                    
      COMMENT ON COLUMN erp_hr_salary.CUMULATIVE_DATA IS '累计个税数据';
                    
      COMMENT ON COLUMN erp_hr_salary.REVIEW_NOTE IS '审核备注';
                    
      COMMENT ON COLUMN erp_hr_salary.PAYMENT_BATCH_NO IS '发放批次号';
                    
      COMMENT ON COLUMN erp_hr_salary.BANK_FILE_ID IS '银行文件';
                    
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
                    
      COMMENT ON COLUMN erp_hr_recruitment.RESUME_ATTACHMENT_FILE_ID IS '简历附件';
                    
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
                    
      COMMENT ON TABLE erp_hr_social_insurance_base IS '员工社保基数';
                
      COMMENT ON COLUMN erp_hr_social_insurance_base.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.CITY_CODE IS '参保城市';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.SOCIAL_INSURANCE_BASE IS '社保基数';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.HOUSING_FUND_BASE IS '公积金基数';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.EFFECTIVE_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.EFFECTIVE_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_social_insurance_base.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_tax_special_deduction IS '员工专项附加扣除';
                
      COMMENT ON COLUMN erp_hr_tax_special_deduction.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.YEAR IS '年份';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.MONTH IS '月份';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.DEDUCTION_TYPE IS '扣除项目';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.MONTHLY_AMOUNT IS '每月扣除金额';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.VERIFIED IS '是否已核验';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_tax_special_deduction.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_survey_response IS '答卷';
                
      COMMENT ON COLUMN erp_hr_survey_response.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey_response.SURVEY_ID IS '所属问卷';
                    
      COMMENT ON COLUMN erp_hr_survey_response.EMPLOYEE_ID IS '填写员工';
                    
      COMMENT ON COLUMN erp_hr_survey_response.RESPONDENT_HASH IS '匿名哈希';
                    
      COMMENT ON COLUMN erp_hr_survey_response.SUBMITTED_AT IS '提交时间';
                    
      COMMENT ON COLUMN erp_hr_survey_response.TIME_SPENT_SECONDS IS '填写耗时';
                    
      COMMENT ON COLUMN erp_hr_survey_response.IS_COMPLETE IS '是否完整';
                    
      COMMENT ON COLUMN erp_hr_survey_response.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_survey_response.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey_response.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey_response.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey_response.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey_response.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey_response.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_employee_assessment IS '员工评估';
                
      COMMENT ON COLUMN erp_hr_employee_assessment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.EMPLOYEE_ID IS '被评估员工';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.ASSESSMENT_TYPE IS '评估类型';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.ASSESSOR_ID IS '评估人';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.ASSESSMENT_DATE IS '评估日期';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.OVERALL_SCORE IS '综合评分';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_employee_assessment.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_gap_analysis IS '差距分析';
                
      COMMENT ON COLUMN erp_hr_gap_analysis.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.COMPETENCY_ID IS '胜任力';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.REQUIRED_LEVEL IS '要求等级';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.ACTUAL_LEVEL IS '实际等级';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.GAP_VALUE IS '差距值';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.GAP_SEVERITY IS '差距严重程度';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.ASSESSMENT_DATE IS '评估日期';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.ANALYSIS_DATE IS '分析日期';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_gap_analysis.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_development_plan IS '发展计划';
                
      COMMENT ON COLUMN erp_hr_development_plan.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_development_plan.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_development_plan.PLAN_NAME IS '计划名称';
                    
      COMMENT ON COLUMN erp_hr_development_plan.TARGET_DATE IS '目标完成日期';
                    
      COMMENT ON COLUMN erp_hr_development_plan.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_development_plan.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_development_plan.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_development_plan.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_development_plan.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_development_plan.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_development_plan.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_development_plan.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_development_plan.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_hr_shift_assignment IS '排班分配';
                
      COMMENT ON COLUMN erp_hr_shift_assignment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.SHIFT_ID IS '班次';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.ASSIGNMENT_DATE IS '排班日期';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.ACTUAL_START_TIME IS '实际签到时间';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.ACTUAL_END_TIME IS '实际签退时间';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.IS_ABSENT IS '是否缺勤';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.ABSENCE_REASON IS '缺勤原因';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.LEAVE_REQUEST_ID IS '关联休假';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.SWAP_REQUEST_ID IS '关联调换';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.REPLACED_BY_ASSIGNMENT_ID IS '被替换排班';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_shift_assignment.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_hr_salary_simulation IS '薪酬模拟';
                
      COMMENT ON COLUMN erp_hr_salary_simulation.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.SOURCE_SALARY_ID IS '源薪酬记录';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.SIMULATION_PERIOD_YEAR IS '模拟年份';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.SIMULATION_PERIOD_MONTH IS '模拟月份';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.SIMULATION_NAME IS '模拟名称';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.REVIEWER_ID IS '审批人';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.REVIEWED_AT IS '审批时间';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.CONVERTED_AT IS '转正式时间';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.CONVERTED_SALARY_ID IS '转正式薪酬ID';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.NOTES IS '备注';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_survey_answer IS '回答明细';
                
      COMMENT ON COLUMN erp_hr_survey_answer.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.RESPONSE_ID IS '答卷';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.QUESTION_ID IS '题目';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.RATING_VALUE IS '评分值';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.SELECTED_OPTION IS '选项';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.OPEN_TEXT IS '文本回答';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_survey_answer.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_assessment_detail IS '评估明细';
                
      COMMENT ON COLUMN erp_hr_assessment_detail.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.ASSESSMENT_ID IS '评估';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.COMPETENCY_ID IS '胜任力';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.ACTUAL_LEVEL IS '实际等级';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail."COMMENT" IS '评语';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.SOURCE_TYPE IS '来源类型';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_assessment_detail.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_development_plan_item IS '发展计划项';
                
      COMMENT ON COLUMN erp_hr_development_plan_item.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.PLAN_ID IS '发展计划';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.COMPETENCY_ID IS '目标胜任力';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.GAP_ID IS '关联差距分析';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.TARGET_LEVEL IS '目标等级';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.DEVELOPMENT_ACTION IS '发展行动';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.MENTOR_ID IS '导师';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.START_DATE IS '开始日期';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.END_DATE IS '预计完成日期';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.PROGRESS_NOTE IS '进度说明';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_development_plan_item.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_shift_swap_request IS '排班调换申请';
                
      COMMENT ON COLUMN erp_hr_shift_swap_request.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.REQUESTER_ID IS '申请人';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.TARGET_EMPLOYEE_ID IS '目标员工';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.SOURCE_ASSIGNMENT_ID IS '原排班';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.TARGET_ASSIGNMENT_ID IS '目标排班';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.SWAP_DATE IS '调换日期';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.REASON IS '调换原因';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.APPROVED_BY_ID IS '审批人';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_shift_swap_request.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_hr_salary_simulation_item_adj IS '薪酬模拟调整项';
                
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.SIMULATION_ID IS '模拟';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.EMPLOYEE_ID IS '员工';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.SALARY_ITEM_CODE IS '薪酬项目编码';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.ORIGINAL_AMOUNT IS '源值';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.ADJUSTED_AMOUNT IS '调整后值';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.ADJUSTMENT_REASON IS '调整原因';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.ADJUSTED_BY IS '调整人';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.ADJUSTED_AT IS '调整时间';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_hr_salary_simulation_item_adj.UPDATE_TIME IS '修改时间';
                    
