
CREATE TABLE erp_md_employee(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_employee primary key (ID)
);

CREATE TABLE erp_md_currency(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  SYMBOL VARCHAR2(50)  ,
  DECIMAL_PLACES INTEGER  ,
  IS_FUNCTIONAL CHAR(1)  ,
  constraint PK_erp_md_currency primary key (ID)
);

CREATE TABLE erp_md_partner(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARTNER_TYPE INTEGER  ,
  STATUS INTEGER  ,
  CREDIT_LIMIT VARCHAR2(50)  ,
  constraint PK_erp_md_partner primary key (ID)
);

CREATE TABLE erp_md_subject(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARENT_ID NUMBER(20)  ,
  SUBJECT_CLASS INTEGER  ,
  DIRECTION INTEGER  ,
  constraint PK_erp_md_subject primary key (ID)
);

CREATE TABLE erp_md_organization(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_TYPE INTEGER  ,
  PARENT_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_organization primary key (ID)
);

CREATE TABLE erp_prj_project_type(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  DEFAULT_SUBJECT_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_project_type primary key (ID)
);

CREATE TABLE erp_prj_activity_type(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  COST_RATE NUMBER(20,4)  ,
  SUBJECT_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_activity_type primary key (ID)
);

CREATE TABLE erp_prj_project(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PROJECT_TYPE_ID NUMBER(20)  ,
  CUSTOMER_ID NUMBER(20)  ,
  CURRENCY_ID NUMBER(20)  ,
  START_DATE DATE  ,
  END_DATE DATE  ,
  BUDGET NUMBER(20,4) default 0   ,
  COMMITTED_COST NUMBER(20,4) default 0   ,
  ACTUAL_COST NUMBER(20,4) default 0   ,
  BILLED_AMOUNT NUMBER(20,4) default 0   ,
  STATUS INTEGER NOT NULL ,
  MANAGER_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_project primary key (ID)
);

CREATE TABLE erp_prj_task(
  ID NUMBER(20) NOT NULL ,
  PROJECT_ID NUMBER(20) NOT NULL ,
  PARENT_TASK_ID NUMBER(20)  ,
  TITLE VARCHAR2(200) NOT NULL ,
  ASSIGNEE_ID NUMBER(20)  ,
  PLANNED_START_DATE DATE  ,
  PLANNED_END_DATE DATE  ,
  ACTUAL_START_DATE DATE  ,
  ACTUAL_END_DATE DATE  ,
  ESTIMATED_HOURS NUMBER(10,2)  ,
  ACTUAL_HOURS NUMBER(10,2)  ,
  DEPENDS_ON_ID NUMBER(20)  ,
  STATUS INTEGER NOT NULL ,
  PRIORITY INTEGER  ,
  BLOCK_REASON VARCHAR2(500)  ,
  SORT_NUM INTEGER  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_task primary key (ID)
);

CREATE TABLE erp_prj_project_user(
  ID NUMBER(20) NOT NULL ,
  PROJECT_ID NUMBER(20) NOT NULL ,
  USER_ID NUMBER(20) NOT NULL ,
  ROLE VARCHAR2(50)  ,
  START_DATE DATE  ,
  END_DATE DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_project_user primary key (ID)
);

CREATE TABLE erp_prj_budget(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  PROJECT_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_budget primary key (ID)
);

CREATE TABLE erp_prj_cost_collection(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  PROJECT_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1  NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_prj_cost_collection primary key (ID)
);

CREATE TABLE erp_prj_milestone(
  ID NUMBER(20) NOT NULL ,
  PROJECT_ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  PLANNED_DATE DATE NOT NULL ,
  ACTUAL_DATE DATE  ,
  BILLING_AMOUNT NUMBER(20,4) default 0   ,
  IS_BILLING_TRIGGER CHAR(1) default 0   ,
  STATUS INTEGER NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_milestone primary key (ID)
);

CREATE TABLE erp_prj_timesheet(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PROJECT_ID NUMBER(20) NOT NULL ,
  TASK_ID NUMBER(20)  ,
  USER_ID NUMBER(20) NOT NULL ,
  WORK_DATE DATE NOT NULL ,
  HOURS NUMBER(10,2) NOT NULL ,
  ACTIVITY_TYPE_ID NUMBER(20)  ,
  CURRENCY_ID NUMBER(20)  ,
  COST_RATE NUMBER(20,4)  ,
  COST_AMOUNT NUMBER(20,4)  ,
  STATUS INTEGER NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY NUMBER(20)  ,
  APPROVED_BY NUMBER(20)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_timesheet primary key (ID)
);

CREATE TABLE erp_prj_budget_line(
  ID NUMBER(20) NOT NULL ,
  BUDGET_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  COST_CATEGORY VARCHAR2(50) NOT NULL ,
  SUBJECT_ID NUMBER(20)  ,
  TASK_ID NUMBER(20)  ,
  PLANNED_AMOUNT NUMBER(20,4) NOT NULL ,
  COMMITTED_AMOUNT NUMBER(20,4) default 0   ,
  ACTUAL_AMOUNT NUMBER(20,4) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_budget_line primary key (ID)
);

CREATE TABLE erp_prj_cost_collection_line(
  ID NUMBER(20) NOT NULL ,
  COST_COLLECTION_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  COST_CATEGORY VARCHAR2(50) NOT NULL ,
  SOURCE_BILL_TYPE VARCHAR2(50)  ,
  SOURCE_BILL_CODE VARCHAR2(50)  ,
  SUBJECT_ID NUMBER(20)  ,
  TASK_ID NUMBER(20)  ,
  AMOUNT NUMBER(20,4) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_cost_collection_line primary key (ID)
);

CREATE TABLE erp_prj_billing(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  PROJECT_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CUSTOMER_ID NUMBER(20) NOT NULL ,
  MILESTONE_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_prj_billing primary key (ID)
);

CREATE TABLE erp_prj_billing_line(
  ID NUMBER(20) NOT NULL ,
  BILLING_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  COST_CATEGORY VARCHAR2(50)  ,
  TASK_ID NUMBER(20)  ,
  SUBJECT_ID NUMBER(20)  ,
  AMOUNT NUMBER(20,4) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_billing_line primary key (ID)
);


      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_subject IS '会计科目';
                
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_prj_project_type IS '项目类型';
                
      COMMENT ON COLUMN erp_prj_project_type.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_project_type.CODE IS '类型编码';
                    
      COMMENT ON COLUMN erp_prj_project_type.NAME IS '类型名称';
                    
      COMMENT ON COLUMN erp_prj_project_type.DEFAULT_SUBJECT_ID IS '默认科目';
                    
      COMMENT ON COLUMN erp_prj_project_type.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_project_type.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_project_type.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_project_type.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_project_type.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_project_type.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_project_type.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_activity_type IS '活动类型';
                
      COMMENT ON COLUMN erp_prj_activity_type.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_activity_type.CODE IS '类型编码';
                    
      COMMENT ON COLUMN erp_prj_activity_type.NAME IS '类型名称';
                    
      COMMENT ON COLUMN erp_prj_activity_type.COST_RATE IS '成本费率';
                    
      COMMENT ON COLUMN erp_prj_activity_type.SUBJECT_ID IS '科目';
                    
      COMMENT ON COLUMN erp_prj_activity_type.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_activity_type.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_activity_type.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_activity_type.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_activity_type.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_activity_type.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_activity_type.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_project IS '项目';
                
      COMMENT ON COLUMN erp_prj_project.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_project.CODE IS '项目编码';
                    
      COMMENT ON COLUMN erp_prj_project.NAME IS '项目名称';
                    
      COMMENT ON COLUMN erp_prj_project.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_project.PROJECT_TYPE_ID IS '项目类型';
                    
      COMMENT ON COLUMN erp_prj_project.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_prj_project.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_prj_project.START_DATE IS '开始日期';
                    
      COMMENT ON COLUMN erp_prj_project.END_DATE IS '结束日期';
                    
      COMMENT ON COLUMN erp_prj_project.BUDGET IS '预算总额';
                    
      COMMENT ON COLUMN erp_prj_project.COMMITTED_COST IS '已承诺成本';
                    
      COMMENT ON COLUMN erp_prj_project.ACTUAL_COST IS '实际成本';
                    
      COMMENT ON COLUMN erp_prj_project.BILLED_AMOUNT IS '已开票金额';
                    
      COMMENT ON COLUMN erp_prj_project.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_prj_project.MANAGER_ID IS '项目经理(职员)';
                    
      COMMENT ON COLUMN erp_prj_project.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_project.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_project.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_project.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_project.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_project.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_project.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_task IS '任务';
                
      COMMENT ON COLUMN erp_prj_task.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_task.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_prj_task.PARENT_TASK_ID IS '父任务';
                    
      COMMENT ON COLUMN erp_prj_task.TITLE IS '任务标题';
                    
      COMMENT ON COLUMN erp_prj_task.ASSIGNEE_ID IS '负责人';
                    
      COMMENT ON COLUMN erp_prj_task.PLANNED_START_DATE IS '计划开始日期';
                    
      COMMENT ON COLUMN erp_prj_task.PLANNED_END_DATE IS '计划结束日期';
                    
      COMMENT ON COLUMN erp_prj_task.ACTUAL_START_DATE IS '实际开始日期';
                    
      COMMENT ON COLUMN erp_prj_task.ACTUAL_END_DATE IS '实际结束日期';
                    
      COMMENT ON COLUMN erp_prj_task.ESTIMATED_HOURS IS '预估工时';
                    
      COMMENT ON COLUMN erp_prj_task.ACTUAL_HOURS IS '实际工时';
                    
      COMMENT ON COLUMN erp_prj_task.DEPENDS_ON_ID IS '依赖任务';
                    
      COMMENT ON COLUMN erp_prj_task.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_prj_task.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_prj_task.BLOCK_REASON IS '阻塞原因';
                    
      COMMENT ON COLUMN erp_prj_task.SORT_NUM IS '排序号';
                    
      COMMENT ON COLUMN erp_prj_task.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_task.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_task.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_task.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_task.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_task.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_task.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_project_user IS '项目成员';
                
      COMMENT ON COLUMN erp_prj_project_user.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_project_user.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_prj_project_user.USER_ID IS '用户';
                    
      COMMENT ON COLUMN erp_prj_project_user.ROLE IS '角色';
                    
      COMMENT ON COLUMN erp_prj_project_user.START_DATE IS '加入日期';
                    
      COMMENT ON COLUMN erp_prj_project_user.END_DATE IS '退出日期';
                    
      COMMENT ON COLUMN erp_prj_project_user.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_project_user.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_project_user.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_project_user.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_project_user.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_project_user.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_budget IS '项目预算';
                
      COMMENT ON COLUMN erp_prj_budget.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_budget.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_prj_budget.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_prj_budget.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_budget.BUSINESS_DATE IS '预算日期';
                    
      COMMENT ON COLUMN erp_prj_budget.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_prj_budget.TOTAL_AMOUNT IS '预算总额';
                    
      COMMENT ON COLUMN erp_prj_budget.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_prj_budget.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_prj_budget.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_budget.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_budget.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_budget.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_budget.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_budget.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_budget.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_cost_collection IS '项目成本归集';
                
      COMMENT ON COLUMN erp_prj_cost_collection.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.BUSINESS_DATE IS '归集日期';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.TOTAL_AMOUNT IS '归集金额合计';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.POSTED IS '已过账(已转成本凭证)';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_prj_milestone IS '项目里程碑';
                
      COMMENT ON COLUMN erp_prj_milestone.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_milestone.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_prj_milestone.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_prj_milestone.NAME IS '里程碑名称';
                    
      COMMENT ON COLUMN erp_prj_milestone.PLANNED_DATE IS '计划完成日期';
                    
      COMMENT ON COLUMN erp_prj_milestone.ACTUAL_DATE IS '实际完成日期';
                    
      COMMENT ON COLUMN erp_prj_milestone.BILLING_AMOUNT IS '应结算金额';
                    
      COMMENT ON COLUMN erp_prj_milestone.IS_BILLING_TRIGGER IS '是否触发开票节点';
                    
      COMMENT ON COLUMN erp_prj_milestone.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_prj_milestone.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_milestone.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_milestone.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_milestone.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_milestone.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_milestone.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_milestone.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_timesheet IS '工时记录';
                
      COMMENT ON COLUMN erp_prj_timesheet.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_timesheet.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_prj_timesheet.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_timesheet.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_prj_timesheet.TASK_ID IS '任务';
                    
      COMMENT ON COLUMN erp_prj_timesheet.USER_ID IS '用户(职员)';
                    
      COMMENT ON COLUMN erp_prj_timesheet.WORK_DATE IS '工作日期';
                    
      COMMENT ON COLUMN erp_prj_timesheet.HOURS IS '工时';
                    
      COMMENT ON COLUMN erp_prj_timesheet.ACTIVITY_TYPE_ID IS '活动类型';
                    
      COMMENT ON COLUMN erp_prj_timesheet.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_prj_timesheet.COST_RATE IS '成本费率';
                    
      COMMENT ON COLUMN erp_prj_timesheet.COST_AMOUNT IS '成本金额';
                    
      COMMENT ON COLUMN erp_prj_timesheet.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_prj_timesheet.POSTED IS '已过账(已转成本凭证)';
                    
      COMMENT ON COLUMN erp_prj_timesheet.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_prj_timesheet.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_prj_timesheet.APPROVED_BY IS '审批人';
                    
      COMMENT ON COLUMN erp_prj_timesheet.APPROVED_AT IS '审批时间';
                    
      COMMENT ON COLUMN erp_prj_timesheet.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_timesheet.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_timesheet.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_timesheet.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_timesheet.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_timesheet.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_timesheet.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_budget_line IS '项目预算行';
                
      COMMENT ON COLUMN erp_prj_budget_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_budget_line.BUDGET_ID IS '预算ID';
                    
      COMMENT ON COLUMN erp_prj_budget_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_prj_budget_line.COST_CATEGORY IS '成本类别(人工/材料/费用/分包)';
                    
      COMMENT ON COLUMN erp_prj_budget_line.SUBJECT_ID IS '科目';
                    
      COMMENT ON COLUMN erp_prj_budget_line.TASK_ID IS '任务';
                    
      COMMENT ON COLUMN erp_prj_budget_line.PLANNED_AMOUNT IS '预算金额';
                    
      COMMENT ON COLUMN erp_prj_budget_line.COMMITTED_AMOUNT IS '已承诺金额';
                    
      COMMENT ON COLUMN erp_prj_budget_line.ACTUAL_AMOUNT IS '实际金额';
                    
      COMMENT ON COLUMN erp_prj_budget_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_budget_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_budget_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_budget_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_budget_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_budget_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_budget_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_cost_collection_line IS '项目成本归集行';
                
      COMMENT ON COLUMN erp_prj_cost_collection_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.COST_COLLECTION_ID IS '归集单ID';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.COST_CATEGORY IS '成本类别(人工/材料/费用/分包)';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.SOURCE_BILL_TYPE IS '来源单据类型(TIMESHEET/PURCHASE/EXPENSE)';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.SOURCE_BILL_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.SUBJECT_ID IS '科目';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.TASK_ID IS '任务';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_billing IS '项目开票';
                
      COMMENT ON COLUMN erp_prj_billing.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_billing.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_prj_billing.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_prj_billing.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_billing.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_prj_billing.MILESTONE_ID IS '关联里程碑';
                    
      COMMENT ON COLUMN erp_prj_billing.BUSINESS_DATE IS '开票日期';
                    
      COMMENT ON COLUMN erp_prj_billing.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_prj_billing.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_prj_billing.TOTAL_AMOUNT IS '开票金额';
                    
      COMMENT ON COLUMN erp_prj_billing.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_prj_billing.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_prj_billing.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_prj_billing.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_prj_billing.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_prj_billing.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_billing.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_billing.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_billing.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_billing.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_billing.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_billing.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_prj_billing.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_prj_billing.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_prj_billing_line IS '项目开票行';
                
      COMMENT ON COLUMN erp_prj_billing_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_billing_line.BILLING_ID IS '开票单ID';
                    
      COMMENT ON COLUMN erp_prj_billing_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_prj_billing_line.COST_CATEGORY IS '成本类别';
                    
      COMMENT ON COLUMN erp_prj_billing_line.TASK_ID IS '任务';
                    
      COMMENT ON COLUMN erp_prj_billing_line.SUBJECT_ID IS '科目';
                    
      COMMENT ON COLUMN erp_prj_billing_line.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_prj_billing_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_prj_billing_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_billing_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_billing_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_billing_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_billing_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_billing_line.UPDATE_TIME IS '修改时间';
                    
