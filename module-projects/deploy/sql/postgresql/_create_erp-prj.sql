
CREATE TABLE erp_prj_project_type(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  default_subject_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_project_type primary key (id)
);

CREATE TABLE erp_prj_activity_type(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  cost_rate NUMERIC(20,4)  ,
  subject_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_activity_type primary key (id)
);

CREATE TABLE erp_prj_project(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  project_type_id INT8  ,
  customer_id INT8  ,
  currency_id INT8  ,
  start_date DATE  ,
  end_date DATE  ,
  budget NUMERIC(20,4) default 0   ,
  committed_cost NUMERIC(20,4) default 0   ,
  actual_cost NUMERIC(20,4) default 0   ,
  billed_amount NUMERIC(20,4) default 0   ,
  status INT4 NOT NULL ,
  manager_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_project primary key (id)
);

CREATE TABLE erp_prj_task(
  id INT8 NOT NULL ,
  project_id INT8 NOT NULL ,
  parent_task_id INT8  ,
  title VARCHAR(200) NOT NULL ,
  assignee_id INT8  ,
  planned_start_date DATE  ,
  planned_end_date DATE  ,
  actual_start_date DATE  ,
  actual_end_date DATE  ,
  estimated_hours NUMERIC(10,2)  ,
  actual_hours NUMERIC(10,2)  ,
  depends_on_id INT8  ,
  status INT4 NOT NULL ,
  priority INT4  ,
  sort_num INT4  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_task primary key (id)
);

CREATE TABLE erp_prj_project_user(
  id INT8 NOT NULL ,
  project_id INT8 NOT NULL ,
  user_id INT8 NOT NULL ,
  role VARCHAR(50)  ,
  start_date DATE  ,
  end_date DATE  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_project_user primary key (id)
);

CREATE TABLE erp_prj_budget(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  project_id INT8 NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  currency_id INT8  ,
  total_amount NUMERIC(20,4) default 0   ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_budget primary key (id)
);

CREATE TABLE erp_prj_cost_collection(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  project_id INT8 NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  currency_id INT8  ,
  total_amount NUMERIC(20,4) default 0   ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_cost_collection primary key (id)
);

CREATE TABLE erp_prj_milestone(
  id INT8 NOT NULL ,
  project_id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  planned_date DATE NOT NULL ,
  actual_date DATE  ,
  billing_amount NUMERIC(20,4) default 0   ,
  is_billing_trigger BOOLEAN default false   ,
  status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_milestone primary key (id)
);

CREATE TABLE erp_prj_timesheet(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  project_id INT8 NOT NULL ,
  task_id INT8  ,
  user_id INT8 NOT NULL ,
  work_date DATE NOT NULL ,
  hours NUMERIC(10,2) NOT NULL ,
  activity_type_id INT8  ,
  currency_id INT8  ,
  cost_rate NUMERIC(20,4)  ,
  cost_amount NUMERIC(20,4)  ,
  status INT4 NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_timesheet primary key (id)
);

CREATE TABLE erp_prj_budget_line(
  id INT8 NOT NULL ,
  budget_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  cost_category VARCHAR(50) NOT NULL ,
  subject_id INT8  ,
  task_id INT8  ,
  planned_amount NUMERIC(20,4) NOT NULL ,
  committed_amount NUMERIC(20,4) default 0   ,
  actual_amount NUMERIC(20,4) default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_budget_line primary key (id)
);

CREATE TABLE erp_prj_cost_collection_line(
  id INT8 NOT NULL ,
  cost_collection_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  cost_category VARCHAR(50) NOT NULL ,
  source_bill_type VARCHAR(50)  ,
  source_bill_code VARCHAR(50)  ,
  subject_id INT8  ,
  task_id INT8  ,
  amount NUMERIC(20,4) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_cost_collection_line primary key (id)
);

CREATE TABLE erp_prj_billing(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  project_id INT8 NOT NULL ,
  org_id INT8  ,
  customer_id INT8 NOT NULL ,
  milestone_id INT8  ,
  business_date DATE NOT NULL ,
  currency_id INT8  ,
  exchange_rate NUMERIC(20,8) default 1   ,
  total_amount NUMERIC(20,4) default 0   ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_billing primary key (id)
);

CREATE TABLE erp_prj_billing_line(
  id INT8 NOT NULL ,
  billing_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  cost_category VARCHAR(50)  ,
  task_id INT8  ,
  subject_id INT8  ,
  amount NUMERIC(20,4) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_prj_billing_line primary key (id)
);


      COMMENT ON TABLE erp_prj_project_type IS '项目类型';
                
      COMMENT ON COLUMN erp_prj_project_type.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_project_type.code IS '类型编码';
                    
      COMMENT ON COLUMN erp_prj_project_type.name IS '类型名称';
                    
      COMMENT ON COLUMN erp_prj_project_type.default_subject_id IS '默认科目';
                    
      COMMENT ON COLUMN erp_prj_project_type.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_project_type.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_project_type.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_project_type.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_project_type.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_project_type.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_project_type.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_activity_type IS '活动类型';
                
      COMMENT ON COLUMN erp_prj_activity_type.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_activity_type.code IS '类型编码';
                    
      COMMENT ON COLUMN erp_prj_activity_type.name IS '类型名称';
                    
      COMMENT ON COLUMN erp_prj_activity_type.cost_rate IS '成本费率';
                    
      COMMENT ON COLUMN erp_prj_activity_type.subject_id IS '科目';
                    
      COMMENT ON COLUMN erp_prj_activity_type.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_activity_type.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_activity_type.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_activity_type.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_activity_type.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_activity_type.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_activity_type.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_project IS '项目';
                
      COMMENT ON COLUMN erp_prj_project.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_project.code IS '项目编码';
                    
      COMMENT ON COLUMN erp_prj_project.name IS '项目名称';
                    
      COMMENT ON COLUMN erp_prj_project.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_project.project_type_id IS '项目类型';
                    
      COMMENT ON COLUMN erp_prj_project.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_prj_project.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_prj_project.start_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_prj_project.end_date IS '结束日期';
                    
      COMMENT ON COLUMN erp_prj_project.budget IS '预算总额';
                    
      COMMENT ON COLUMN erp_prj_project.committed_cost IS '已承诺成本';
                    
      COMMENT ON COLUMN erp_prj_project.actual_cost IS '实际成本';
                    
      COMMENT ON COLUMN erp_prj_project.billed_amount IS '已开票金额';
                    
      COMMENT ON COLUMN erp_prj_project.status IS '状态';
                    
      COMMENT ON COLUMN erp_prj_project.manager_id IS '项目经理(职员)';
                    
      COMMENT ON COLUMN erp_prj_project.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_project.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_project.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_project.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_project.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_project.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_project.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_task IS '任务';
                
      COMMENT ON COLUMN erp_prj_task.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_task.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_prj_task.parent_task_id IS '父任务';
                    
      COMMENT ON COLUMN erp_prj_task.title IS '任务标题';
                    
      COMMENT ON COLUMN erp_prj_task.assignee_id IS '负责人';
                    
      COMMENT ON COLUMN erp_prj_task.planned_start_date IS '计划开始日期';
                    
      COMMENT ON COLUMN erp_prj_task.planned_end_date IS '计划结束日期';
                    
      COMMENT ON COLUMN erp_prj_task.actual_start_date IS '实际开始日期';
                    
      COMMENT ON COLUMN erp_prj_task.actual_end_date IS '实际结束日期';
                    
      COMMENT ON COLUMN erp_prj_task.estimated_hours IS '预估工时';
                    
      COMMENT ON COLUMN erp_prj_task.actual_hours IS '实际工时';
                    
      COMMENT ON COLUMN erp_prj_task.depends_on_id IS '依赖任务';
                    
      COMMENT ON COLUMN erp_prj_task.status IS '状态';
                    
      COMMENT ON COLUMN erp_prj_task.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_prj_task.sort_num IS '排序号';
                    
      COMMENT ON COLUMN erp_prj_task.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_task.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_task.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_task.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_task.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_task.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_task.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_project_user IS '项目成员';
                
      COMMENT ON COLUMN erp_prj_project_user.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_project_user.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_prj_project_user.user_id IS '用户';
                    
      COMMENT ON COLUMN erp_prj_project_user.role IS '角色';
                    
      COMMENT ON COLUMN erp_prj_project_user.start_date IS '加入日期';
                    
      COMMENT ON COLUMN erp_prj_project_user.end_date IS '退出日期';
                    
      COMMENT ON COLUMN erp_prj_project_user.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_project_user.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_project_user.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_project_user.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_project_user.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_project_user.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_budget IS '项目预算';
                
      COMMENT ON COLUMN erp_prj_budget.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_budget.code IS '单号';
                    
      COMMENT ON COLUMN erp_prj_budget.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_prj_budget.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_budget.business_date IS '预算日期';
                    
      COMMENT ON COLUMN erp_prj_budget.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_prj_budget.total_amount IS '预算总额';
                    
      COMMENT ON COLUMN erp_prj_budget.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_prj_budget.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_prj_budget.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_budget.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_budget.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_budget.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_budget.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_budget.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_budget.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_cost_collection IS '项目成本归集';
                
      COMMENT ON COLUMN erp_prj_cost_collection.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.code IS '单号';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.business_date IS '归集日期';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.total_amount IS '归集金额合计';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.posted IS '已过账(已转成本凭证)';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_milestone IS '项目里程碑';
                
      COMMENT ON COLUMN erp_prj_milestone.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_milestone.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_prj_milestone.code IS '编码';
                    
      COMMENT ON COLUMN erp_prj_milestone.name IS '里程碑名称';
                    
      COMMENT ON COLUMN erp_prj_milestone.planned_date IS '计划完成日期';
                    
      COMMENT ON COLUMN erp_prj_milestone.actual_date IS '实际完成日期';
                    
      COMMENT ON COLUMN erp_prj_milestone.billing_amount IS '应结算金额';
                    
      COMMENT ON COLUMN erp_prj_milestone.is_billing_trigger IS '是否触发开票节点';
                    
      COMMENT ON COLUMN erp_prj_milestone.status IS '状态';
                    
      COMMENT ON COLUMN erp_prj_milestone.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_milestone.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_milestone.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_milestone.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_milestone.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_milestone.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_milestone.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_timesheet IS '工时记录';
                
      COMMENT ON COLUMN erp_prj_timesheet.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_timesheet.code IS '单号';
                    
      COMMENT ON COLUMN erp_prj_timesheet.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_timesheet.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_prj_timesheet.task_id IS '任务';
                    
      COMMENT ON COLUMN erp_prj_timesheet.user_id IS '用户(职员)';
                    
      COMMENT ON COLUMN erp_prj_timesheet.work_date IS '工作日期';
                    
      COMMENT ON COLUMN erp_prj_timesheet.hours IS '工时';
                    
      COMMENT ON COLUMN erp_prj_timesheet.activity_type_id IS '活动类型';
                    
      COMMENT ON COLUMN erp_prj_timesheet.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_prj_timesheet.cost_rate IS '成本费率';
                    
      COMMENT ON COLUMN erp_prj_timesheet.cost_amount IS '成本金额';
                    
      COMMENT ON COLUMN erp_prj_timesheet.status IS '状态';
                    
      COMMENT ON COLUMN erp_prj_timesheet.posted IS '已过账(已转成本凭证)';
                    
      COMMENT ON COLUMN erp_prj_timesheet.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_prj_timesheet.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_prj_timesheet.approved_by IS '审批人';
                    
      COMMENT ON COLUMN erp_prj_timesheet.approved_at IS '审批时间';
                    
      COMMENT ON COLUMN erp_prj_timesheet.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_timesheet.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_timesheet.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_timesheet.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_timesheet.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_timesheet.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_timesheet.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_budget_line IS '项目预算行';
                
      COMMENT ON COLUMN erp_prj_budget_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_budget_line.budget_id IS '预算ID';
                    
      COMMENT ON COLUMN erp_prj_budget_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_prj_budget_line.cost_category IS '成本类别(人工/材料/费用/分包)';
                    
      COMMENT ON COLUMN erp_prj_budget_line.subject_id IS '科目';
                    
      COMMENT ON COLUMN erp_prj_budget_line.task_id IS '任务';
                    
      COMMENT ON COLUMN erp_prj_budget_line.planned_amount IS '预算金额';
                    
      COMMENT ON COLUMN erp_prj_budget_line.committed_amount IS '已承诺金额';
                    
      COMMENT ON COLUMN erp_prj_budget_line.actual_amount IS '实际金额';
                    
      COMMENT ON COLUMN erp_prj_budget_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_budget_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_budget_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_budget_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_budget_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_budget_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_budget_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_cost_collection_line IS '项目成本归集行';
                
      COMMENT ON COLUMN erp_prj_cost_collection_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.cost_collection_id IS '归集单ID';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.cost_category IS '成本类别(人工/材料/费用/分包)';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.source_bill_type IS '来源单据类型(TIMESHEET/PURCHASE/EXPENSE)';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.source_bill_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.subject_id IS '科目';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.task_id IS '任务';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.amount IS '金额';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_cost_collection_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_billing IS '项目开票';
                
      COMMENT ON COLUMN erp_prj_billing.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_billing.code IS '单号';
                    
      COMMENT ON COLUMN erp_prj_billing.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_prj_billing.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_prj_billing.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_prj_billing.milestone_id IS '关联里程碑';
                    
      COMMENT ON COLUMN erp_prj_billing.business_date IS '开票日期';
                    
      COMMENT ON COLUMN erp_prj_billing.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_prj_billing.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_prj_billing.total_amount IS '开票金额';
                    
      COMMENT ON COLUMN erp_prj_billing.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_prj_billing.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_prj_billing.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_prj_billing.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_prj_billing.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_prj_billing.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_billing.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_billing.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_billing.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_billing.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_billing.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_billing.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_prj_billing_line IS '项目开票行';
                
      COMMENT ON COLUMN erp_prj_billing_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_prj_billing_line.billing_id IS '开票单ID';
                    
      COMMENT ON COLUMN erp_prj_billing_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_prj_billing_line.cost_category IS '成本类别';
                    
      COMMENT ON COLUMN erp_prj_billing_line.task_id IS '任务';
                    
      COMMENT ON COLUMN erp_prj_billing_line.subject_id IS '科目';
                    
      COMMENT ON COLUMN erp_prj_billing_line.amount IS '金额';
                    
      COMMENT ON COLUMN erp_prj_billing_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_prj_billing_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_prj_billing_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_prj_billing_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_prj_billing_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_prj_billing_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_prj_billing_line.update_time IS '修改时间';
                    
