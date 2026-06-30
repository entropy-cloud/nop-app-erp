
CREATE TABLE erp_md_partner(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  partner_type INT4  ,
  status INT4  ,
  credit_limit VARCHAR(50)  ,
  constraint PK_erp_md_partner primary key (id)
);

CREATE TABLE erp_crm_source(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  sequence INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_source primary key (id)
);

CREATE TABLE erp_crm_lead_status(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  sequence INT4 default 0   ,
  is_default BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_status primary key (id)
);

CREATE TABLE erp_crm_lost_reason(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  sequence INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lost_reason primary key (id)
);

CREATE TABLE erp_md_organization(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  org_type INT4  ,
  parent_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_organization primary key (id)
);

CREATE TABLE erp_crm_event_category(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  color VARCHAR(20)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_event_category primary key (id)
);

CREATE TABLE erp_crm_quote_template(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  template_content VARCHAR(4000)  ,
  is_default BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_quote_template primary key (id)
);

CREATE TABLE erp_crm_team(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  team_leader_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_team primary key (id)
);

CREATE TABLE erp_crm_campaign(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  campaign_name VARCHAR(200) NOT NULL ,
  medium VARCHAR(100)  ,
  source VARCHAR(100)  ,
  start_date DATE  ,
  end_date DATE  ,
  budget_amount NUMERIC(20,4)  ,
  actual_cost NUMERIC(20,4)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_campaign primary key (id)
);

CREATE TABLE erp_crm_stage(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  stage_name VARCHAR(200) NOT NULL ,
  sequence INT4 default 0  NOT NULL ,
  team_id INT8  ,
  default_probability INT4  ,
  is_won_stage BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_stage primary key (id)
);

CREATE TABLE erp_crm_lead(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  lead_type VARCHAR(20) NOT NULL ,
  partner_id INT8  ,
  contact_name VARCHAR(100)  ,
  contact_phone VARCHAR(50)  ,
  contact_email VARCHAR(200)  ,
  company_name VARCHAR(200)  ,
  job_title VARCHAR(100)  ,
  department VARCHAR(100)  ,
  source_id INT8  ,
  lead_status_id INT8  ,
  stage_id INT8  ,
  expected_revenue NUMERIC(20,4)  ,
  best_case_amount NUMERIC(20,4)  ,
  worst_case_amount NUMERIC(20,4)  ,
  recurring_revenue NUMERIC(20,4)  ,
  recurring_plan VARCHAR(50)  ,
  expected_close_date DATE  ,
  probability INT4  ,
  campaign_id INT8  ,
  utm_medium VARCHAR(100)  ,
  utm_source VARCHAR(100)  ,
  owner_id INT8  ,
  team_id INT8  ,
  lost_reason_id INT8  ,
  lost_reason_desc VARCHAR(500)  ,
  last_contact_date TIMESTAMP  ,
  next_activity_date TIMESTAMP  ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  doc_status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead primary key (id)
);

CREATE TABLE erp_crm_event(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  event_type VARCHAR(20) NOT NULL ,
  event_category_id INT8  ,
  subject VARCHAR(200) NOT NULL ,
  description VARCHAR(2000)  ,
  start_date_time TIMESTAMP NOT NULL ,
  end_date_time TIMESTAMP  ,
  duration INT4  ,
  related_lead_id INT8  ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  partner_id INT8  ,
  contact_id INT8  ,
  owner_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  priority VARCHAR(20) NOT NULL ,
  is_recurrent BOOLEAN default false   ,
  parent_event_id INT8  ,
  reminder_minutes_before INT4  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_event primary key (id)
);

CREATE TABLE erp_crm_activity(
  id INT8 NOT NULL ,
  lead_id INT8 NOT NULL ,
  org_id INT8  ,
  activity_type VARCHAR(20) NOT NULL ,
  activity_date TIMESTAMP NOT NULL ,
  summary VARCHAR(500)  ,
  owner_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_activity primary key (id)
);

CREATE TABLE erp_crm_lead_conv_log(
  id INT8 NOT NULL ,
  lead_id INT8 NOT NULL ,
  org_id INT8  ,
  from_stage_id INT8  ,
  to_stage_id INT8  ,
  changed_at TIMESTAMP NOT NULL ,
  changed_by INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_conv_log primary key (id)
);


      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_crm_source IS '线索来源';
                
      COMMENT ON COLUMN erp_crm_source.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_source.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_source.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_source.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_source.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_source.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_source.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_source.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_source.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_source.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_source.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_status IS '线索状态';
                
      COMMENT ON COLUMN erp_crm_lead_status.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_status.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lead_status.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_lead_status.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lead_status.is_default IS '是否默认';
                    
      COMMENT ON COLUMN erp_crm_lead_status.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_status.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_status.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_status.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_status.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_status.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_status.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lost_reason IS '丢单原因';
                
      COMMENT ON COLUMN erp_crm_lost_reason.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_crm_event_category IS '活动类别';
                
      COMMENT ON COLUMN erp_crm_event_category.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_event_category.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_event_category.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_event_category.color IS '日历颜色';
                    
      COMMENT ON COLUMN erp_crm_event_category.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_event_category.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_event_category.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_event_category.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_event_category.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_event_category.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_event_category.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_quote_template IS '报价模板';
                
      COMMENT ON COLUMN erp_crm_quote_template.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_quote_template.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_quote_template.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_quote_template.template_content IS '模板内容';
                    
      COMMENT ON COLUMN erp_crm_quote_template.is_default IS '是否默认';
                    
      COMMENT ON COLUMN erp_crm_quote_template.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_quote_template.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_quote_template.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_quote_template.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_quote_template.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_quote_template.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_quote_template.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_team IS '销售团队';
                
      COMMENT ON COLUMN erp_crm_team.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_team.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_team.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_team.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_team.team_leader_id IS '团队负责人';
                    
      COMMENT ON COLUMN erp_crm_team.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_team.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_team.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_team.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_team.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_team.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_team.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_campaign IS '营销活动';
                
      COMMENT ON COLUMN erp_crm_campaign.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_campaign.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_campaign.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_campaign.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_campaign.campaign_name IS '活动名称';
                    
      COMMENT ON COLUMN erp_crm_campaign.medium IS 'UTM Medium';
                    
      COMMENT ON COLUMN erp_crm_campaign.source IS 'UTM Source';
                    
      COMMENT ON COLUMN erp_crm_campaign.start_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_crm_campaign.end_date IS '结束日期';
                    
      COMMENT ON COLUMN erp_crm_campaign.budget_amount IS '预算金额';
                    
      COMMENT ON COLUMN erp_crm_campaign.actual_cost IS '实际成本';
                    
      COMMENT ON COLUMN erp_crm_campaign.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_campaign.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_campaign.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_campaign.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_campaign.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_campaign.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_campaign.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_stage IS '漏斗阶段';
                
      COMMENT ON COLUMN erp_crm_stage.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_stage.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_stage.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_stage.stage_name IS '阶段名称';
                    
      COMMENT ON COLUMN erp_crm_stage.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_stage.team_id IS '团队作用域';
                    
      COMMENT ON COLUMN erp_crm_stage.default_probability IS '默认成交概率%';
                    
      COMMENT ON COLUMN erp_crm_stage.is_won_stage IS '是否赢单阶段';
                    
      COMMENT ON COLUMN erp_crm_stage.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_stage.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_stage.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_stage.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_stage.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_stage.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_stage.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead IS '线索/商机';
                
      COMMENT ON COLUMN erp_crm_lead.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lead.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead.lead_type IS '线索/商机类型';
                    
      COMMENT ON COLUMN erp_crm_lead.partner_id IS '客户';
                    
      COMMENT ON COLUMN erp_crm_lead.contact_name IS '联系人姓名';
                    
      COMMENT ON COLUMN erp_crm_lead.contact_phone IS '联系电话';
                    
      COMMENT ON COLUMN erp_crm_lead.contact_email IS '联系邮箱';
                    
      COMMENT ON COLUMN erp_crm_lead.company_name IS '公司名称';
                    
      COMMENT ON COLUMN erp_crm_lead.job_title IS '职位';
                    
      COMMENT ON COLUMN erp_crm_lead.department IS '部门';
                    
      COMMENT ON COLUMN erp_crm_lead.source_id IS '线索来源';
                    
      COMMENT ON COLUMN erp_crm_lead.lead_status_id IS '线索状态';
                    
      COMMENT ON COLUMN erp_crm_lead.stage_id IS '漏斗阶段';
                    
      COMMENT ON COLUMN erp_crm_lead.expected_revenue IS '预期收入';
                    
      COMMENT ON COLUMN erp_crm_lead.best_case_amount IS '乐观预期';
                    
      COMMENT ON COLUMN erp_crm_lead.worst_case_amount IS '悲观预期';
                    
      COMMENT ON COLUMN erp_crm_lead.recurring_revenue IS '周期性收入';
                    
      COMMENT ON COLUMN erp_crm_lead.recurring_plan IS '周期性计划';
                    
      COMMENT ON COLUMN erp_crm_lead.expected_close_date IS '预期签单日';
                    
      COMMENT ON COLUMN erp_crm_lead.probability IS '成交概率';
                    
      COMMENT ON COLUMN erp_crm_lead.campaign_id IS '营销活动';
                    
      COMMENT ON COLUMN erp_crm_lead.utm_medium IS 'UTM Medium';
                    
      COMMENT ON COLUMN erp_crm_lead.utm_source IS 'UTM Source';
                    
      COMMENT ON COLUMN erp_crm_lead.owner_id IS '负责人';
                    
      COMMENT ON COLUMN erp_crm_lead.team_id IS '销售团队';
                    
      COMMENT ON COLUMN erp_crm_lead.lost_reason_id IS '丢单原因';
                    
      COMMENT ON COLUMN erp_crm_lead.lost_reason_desc IS '丢单描述';
                    
      COMMENT ON COLUMN erp_crm_lead.last_contact_date IS '最后联系日期';
                    
      COMMENT ON COLUMN erp_crm_lead.next_activity_date IS '下次活动日期';
                    
      COMMENT ON COLUMN erp_crm_lead.related_bill_type IS '转化结果单据类型';
                    
      COMMENT ON COLUMN erp_crm_lead.related_bill_code IS '转化结果单据号';
                    
      COMMENT ON COLUMN erp_crm_lead.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_crm_lead.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_event IS '活动/事件';
                
      COMMENT ON COLUMN erp_crm_event.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_event.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_event.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_event.event_type IS '事件类型';
                    
      COMMENT ON COLUMN erp_crm_event.event_category_id IS '活动类别';
                    
      COMMENT ON COLUMN erp_crm_event.subject IS '主题';
                    
      COMMENT ON COLUMN erp_crm_event.description IS '描述';
                    
      COMMENT ON COLUMN erp_crm_event.start_date_time IS '开始时间';
                    
      COMMENT ON COLUMN erp_crm_event.end_date_time IS '结束时间';
                    
      COMMENT ON COLUMN erp_crm_event.duration IS '时长(分钟)';
                    
      COMMENT ON COLUMN erp_crm_event.related_lead_id IS '关联线索/商机';
                    
      COMMENT ON COLUMN erp_crm_event.related_bill_type IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_crm_event.related_bill_code IS '关联单据号';
                    
      COMMENT ON COLUMN erp_crm_event.partner_id IS '关联客户';
                    
      COMMENT ON COLUMN erp_crm_event.contact_id IS '联系人';
                    
      COMMENT ON COLUMN erp_crm_event.owner_id IS '负责人';
                    
      COMMENT ON COLUMN erp_crm_event.status IS '活动状态';
                    
      COMMENT ON COLUMN erp_crm_event.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_crm_event.is_recurrent IS '是否重复事件';
                    
      COMMENT ON COLUMN erp_crm_event.parent_event_id IS '父事件';
                    
      COMMENT ON COLUMN erp_crm_event.reminder_minutes_before IS '提醒提前分钟数';
                    
      COMMENT ON COLUMN erp_crm_event.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_event.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_event.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_event.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_event.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_event.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_event.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_activity IS '活动记录';
                
      COMMENT ON COLUMN erp_crm_activity.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_activity.lead_id IS '关联线索/商机';
                    
      COMMENT ON COLUMN erp_crm_activity.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_activity.activity_type IS '活动类型';
                    
      COMMENT ON COLUMN erp_crm_activity.activity_date IS '活动日期';
                    
      COMMENT ON COLUMN erp_crm_activity.summary IS '内容摘要';
                    
      COMMENT ON COLUMN erp_crm_activity.owner_id IS '负责人';
                    
      COMMENT ON COLUMN erp_crm_activity.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_activity.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_activity.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_activity.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_activity.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_activity.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_activity.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_conv_log IS '阶段流转日志';
                
      COMMENT ON COLUMN erp_crm_lead_conv_log.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.lead_id IS '线索/商机';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.from_stage_id IS '前阶段';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.to_stage_id IS '后阶段';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.changed_at IS '变更时间';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.changed_by IS '变更人';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.update_time IS '修改时间';
                    
