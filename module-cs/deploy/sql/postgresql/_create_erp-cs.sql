
CREATE TABLE erp_md_partner(
  id INT8 NOT NULL ,
  constraint PK_erp_md_partner primary key (id)
);

CREATE TABLE erp_md_organization(
  id INT8 NOT NULL ,
  constraint PK_erp_md_organization primary key (id)
);

CREATE TABLE erp_cs_team(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  team_leader_id VARCHAR(36)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_team primary key (id)
);

CREATE TABLE erp_cs_canned_category(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  parent_id INT8  ,
  icon VARCHAR(100)  ,
  sequence INT4 default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_canned_category primary key (id)
);

CREATE TABLE erp_cs_agent_rate(
  id INT8 NOT NULL ,
  org_id INT8  ,
  agent_id INT8 NOT NULL ,
  service_type VARCHAR(20)  ,
  rate NUMERIC(18,2)  ,
  effective_date DATE  ,
  is_active BOOLEAN default true   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_agent_rate primary key (id)
);

CREATE TABLE erp_cs_contract(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  partner_id INT8  ,
  contract_type VARCHAR(20)  ,
  start_date DATE  ,
  end_date DATE  ,
  total_amount NUMERIC(18,2)  ,
  billing_cycle VARCHAR(20)  ,
  status VARCHAR(20)  ,
  attachment_file_id VARCHAR(200)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  business_date DATE NOT NULL ,
  constraint PK_erp_cs_contract primary key (id)
);

CREATE TABLE erp_cs_catalog_category(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  parent_id INT8  ,
  icon VARCHAR(100)  ,
  sequence INT4 default 0   ,
  is_active BOOLEAN default true   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_catalog_category primary key (id)
);

CREATE TABLE erp_cs_sla_policy(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  ticket_type_id INT8  ,
  min_priority VARCHAR(20)  ,
  team_id INT8  ,
  resolve_hours INT4  ,
  resolve_days INT4  ,
  is_working_days BOOLEAN default true   ,
  escalation_user_id INT8  ,
  description VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_sla_policy primary key (id)
);

CREATE TABLE erp_cs_knowledge_base(
  id INT8 NOT NULL ,
  code VARCHAR(50)  ,
  title VARCHAR(200) NOT NULL ,
  content VARCHAR(4000)  ,
  category_id INT8  ,
  is_published BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_knowledge_base primary key (id)
);

CREATE TABLE erp_cs_ticket_type(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  default_priority VARCHAR(20)  ,
  default_sla_policy_id INT8  ,
  sequence INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_ticket_type primary key (id)
);

CREATE TABLE erp_cs_entitlement(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  partner_id INT8 NOT NULL ,
  contract_id INT8  ,
  sla_policy_id INT8  ,
  service_type VARCHAR(20) NOT NULL ,
  start_date DATE  ,
  end_date DATE  ,
  max_tickets INT4  ,
  used_tickets INT4 default 0   ,
  max_response_time INT4  ,
  max_resolution_time INT4  ,
  is_active BOOLEAN default true   ,
  notes VARCHAR(4000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_entitlement primary key (id)
);

CREATE TABLE erp_cs_canned_response(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  title VARCHAR(200) NOT NULL ,
  content VARCHAR(4000) NOT NULL ,
  category_id INT8  ,
  variable_defs VARCHAR(4000)  ,
  macro_ticket_type_id INT8  ,
  macro_priority VARCHAR(20)  ,
  sequence INT4 default 0   ,
  is_active BOOLEAN default true   ,
  usage_count INT4 default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_canned_response primary key (id)
);

CREATE TABLE erp_cs_service_catalog_item(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  category_id INT8  ,
  parent_id INT8  ,
  short_description VARCHAR(500)  ,
  full_description VARCHAR(4000)  ,
  ticket_type_id INT8  ,
  sla_policy_id INT8  ,
  fulfillment_process_id VARCHAR(100)  ,
  request_form_config VARCHAR(4000)  ,
  is_active BOOLEAN default true   ,
  is_public BOOLEAN default false   ,
  sequence INT4 default 0   ,
  estimated_resolution VARCHAR(200)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_service_catalog_item primary key (id)
);

CREATE TABLE erp_cs_ticket(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  subject VARCHAR(200) NOT NULL ,
  description VARCHAR(2000)  ,
  customer_id INT8 NOT NULL ,
  contact_id INT8  ,
  ticket_type_id INT8 NOT NULL ,
  priority VARCHAR(20) NOT NULL ,
  source VARCHAR(20)  ,
  assigned_to_id VARCHAR(36)  ,
  sla_policy_id INT8  ,
  deadline_date_time TIMESTAMP  ,
  is_sla_completed BOOLEAN default false   ,
  start_date_time TIMESTAMP  ,
  end_date_time TIMESTAMP  ,
  duration INT4  ,
  progress INT4 default 0   ,
  status VARCHAR(20) NOT NULL ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  catalog_item_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  business_date DATE NOT NULL ,
  constraint PK_erp_cs_ticket primary key (id)
);

CREATE TABLE erp_cs_catalog_fulfillment(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  catalog_item_id INT8 NOT NULL ,
  sequence INT4  ,
  action_type VARCHAR(20) NOT NULL ,
  action_config VARCHAR(4000)  ,
  assign_to_role VARCHAR(100)  ,
  estimated_duration INT4  ,
  is_mandatory BOOLEAN default true   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_catalog_fulfillment primary key (id)
);

CREATE TABLE erp_cs_ticket_action(
  id INT8 NOT NULL ,
  ticket_id INT8 NOT NULL ,
  action_type VARCHAR(20) NOT NULL ,
  from_status VARCHAR(20)  ,
  to_status VARCHAR(20)  ,
  operator_id VARCHAR(36)  ,
  content VARCHAR(4000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_ticket_action primary key (id)
);

CREATE TABLE erp_cs_survey(
  id INT8 NOT NULL ,
  org_id INT8  ,
  ticket_id INT8 NOT NULL ,
  survey_token VARCHAR(50)  ,
  csat_score INT4  ,
  nps_score INT4  ,
  ces_score INT4  ,
  comment VARCHAR(4000)  ,
  responded_at TIMESTAMP  ,
  survey_sent_at TIMESTAMP  ,
  survey_channel VARCHAR(20)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_survey primary key (id)
);

CREATE TABLE erp_cs_time_entry(
  id INT8 NOT NULL ,
  org_id INT8  ,
  ticket_id INT8 NOT NULL ,
  agent_id INT8 NOT NULL ,
  start_time TIMESTAMP  ,
  end_time TIMESTAMP  ,
  duration INT4  ,
  is_billable BOOLEAN default true   ,
  billing_rate NUMERIC(18,2)  ,
  billable_amount NUMERIC(18,2)  ,
  description VARCHAR(1000)  ,
  approval_status VARCHAR(20)  ,
  approved_by_id VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  project_id INT8  ,
  task_id INT8  ,
  source VARCHAR(20)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_time_entry primary key (id)
);


      COMMENT ON TABLE erp_md_partner IS 'ErpMdPartner';
                
      COMMENT ON TABLE erp_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_cs_team IS '客服团队';
                
      COMMENT ON COLUMN erp_cs_team.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_team.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_team.name IS '名称';
                    
      COMMENT ON COLUMN erp_cs_team.team_leader_id IS '团队负责人';
                    
      COMMENT ON COLUMN erp_cs_team.remark IS '备注';
                    
      COMMENT ON COLUMN erp_cs_team.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_team.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_team.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_team.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_team.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_team.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_canned_category IS '预设应答分类';
                
      COMMENT ON COLUMN erp_cs_canned_category.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_canned_category.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_canned_category.name IS '名称';
                    
      COMMENT ON COLUMN erp_cs_canned_category.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_canned_category.parent_id IS '父分类';
                    
      COMMENT ON COLUMN erp_cs_canned_category.icon IS '图标';
                    
      COMMENT ON COLUMN erp_cs_canned_category.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_cs_canned_category.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_canned_category.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_canned_category.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_canned_category.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_canned_category.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_canned_category.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_agent_rate IS '客服费率';
                
      COMMENT ON COLUMN erp_cs_agent_rate.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.agent_id IS '客服';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.service_type IS '服务类型';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.rate IS '费率(元/小时)';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.effective_date IS '生效日期';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_contract IS '支持合同';
                
      COMMENT ON COLUMN erp_cs_contract.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_contract.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_contract.name IS '名称';
                    
      COMMENT ON COLUMN erp_cs_contract.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_contract.partner_id IS '签约客户';
                    
      COMMENT ON COLUMN erp_cs_contract.contract_type IS '合同类型';
                    
      COMMENT ON COLUMN erp_cs_contract.start_date IS '生效日期';
                    
      COMMENT ON COLUMN erp_cs_contract.end_date IS '到期日期';
                    
      COMMENT ON COLUMN erp_cs_contract.total_amount IS '合同总金额';
                    
      COMMENT ON COLUMN erp_cs_contract.billing_cycle IS '计费周期';
                    
      COMMENT ON COLUMN erp_cs_contract.status IS '合同状态';
                    
      COMMENT ON COLUMN erp_cs_contract.attachment_file_id IS '合同附件';
                    
      COMMENT ON COLUMN erp_cs_contract.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_contract.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_contract.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_contract.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_contract.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_contract.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_cs_contract.business_date IS '业务日期';
                    
      COMMENT ON TABLE erp_cs_catalog_category IS '目录分类';
                
      COMMENT ON COLUMN erp_cs_catalog_category.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.name IS '名称';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.parent_id IS '父分类';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.icon IS '图标';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_sla_policy IS 'SLA 策略';
                
      COMMENT ON COLUMN erp_cs_sla_policy.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.name IS '名称';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.ticket_type_id IS '适用工单类型';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.min_priority IS '最低触发优先级';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.team_id IS '适用团队';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.resolve_hours IS '解决时限(小时)';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.resolve_days IS '解决时限(天)';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.is_working_days IS '仅计算工作日';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.escalation_user_id IS '升级通知人';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.description IS '说明';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_knowledge_base IS '知识库';
                
      COMMENT ON COLUMN erp_cs_knowledge_base.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.title IS '标题';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.content IS '正文';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.category_id IS '分类';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.is_published IS '是否发布';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.remark IS '备注';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_ticket_type IS '工单类型';
                
      COMMENT ON COLUMN erp_cs_ticket_type.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.name IS '名称';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.default_priority IS '默认优先级';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.default_sla_policy_id IS '默认 SLA 策略';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.remark IS '备注';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_entitlement IS '服务权益';
                
      COMMENT ON COLUMN erp_cs_entitlement.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_entitlement.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_entitlement.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_entitlement.partner_id IS '客户';
                    
      COMMENT ON COLUMN erp_cs_entitlement.contract_id IS '支持合同';
                    
      COMMENT ON COLUMN erp_cs_entitlement.sla_policy_id IS 'SLA策略';
                    
      COMMENT ON COLUMN erp_cs_entitlement.service_type IS '服务类型';
                    
      COMMENT ON COLUMN erp_cs_entitlement.start_date IS '生效日期';
                    
      COMMENT ON COLUMN erp_cs_entitlement.end_date IS '失效日期';
                    
      COMMENT ON COLUMN erp_cs_entitlement.max_tickets IS '最大工单数';
                    
      COMMENT ON COLUMN erp_cs_entitlement.used_tickets IS '已用工单数';
                    
      COMMENT ON COLUMN erp_cs_entitlement.max_response_time IS '承诺响应时限(分钟)';
                    
      COMMENT ON COLUMN erp_cs_entitlement.max_resolution_time IS '承诺解决时限(分钟)';
                    
      COMMENT ON COLUMN erp_cs_entitlement.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_cs_entitlement.notes IS '备注';
                    
      COMMENT ON COLUMN erp_cs_entitlement.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_entitlement.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_entitlement.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_entitlement.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_entitlement.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_entitlement.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_canned_response IS '预设应答模板';
                
      COMMENT ON COLUMN erp_cs_canned_response.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_canned_response.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_canned_response.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_canned_response.title IS '模板标题';
                    
      COMMENT ON COLUMN erp_cs_canned_response.content IS '模板正文';
                    
      COMMENT ON COLUMN erp_cs_canned_response.category_id IS '应答分类';
                    
      COMMENT ON COLUMN erp_cs_canned_response.variable_defs IS '变量定义';
                    
      COMMENT ON COLUMN erp_cs_canned_response.macro_ticket_type_id IS '自动匹配工单类型';
                    
      COMMENT ON COLUMN erp_cs_canned_response.macro_priority IS '自动匹配优先级';
                    
      COMMENT ON COLUMN erp_cs_canned_response.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_cs_canned_response.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_cs_canned_response.usage_count IS '使用次数';
                    
      COMMENT ON COLUMN erp_cs_canned_response.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_canned_response.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_canned_response.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_canned_response.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_canned_response.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_canned_response.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_service_catalog_item IS '服务目录项';
                
      COMMENT ON COLUMN erp_cs_service_catalog_item.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.name IS '名称';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.category_id IS '目录分类';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.parent_id IS '父项';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.short_description IS '简短描述';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.full_description IS '详细说明';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.ticket_type_id IS '关联工单类型';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.sla_policy_id IS '默认SLA策略';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.fulfillment_process_id IS '履行流程标识';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.request_form_config IS '请求表单配置';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.is_active IS '是否上架';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.is_public IS '是否客户可见';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.estimated_resolution IS '预计解决时间';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_ticket IS '客服工单';
                
      COMMENT ON COLUMN erp_cs_ticket.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_ticket.code IS '单号';
                    
      COMMENT ON COLUMN erp_cs_ticket.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_ticket.subject IS '工单主题';
                    
      COMMENT ON COLUMN erp_cs_ticket.description IS '问题描述';
                    
      COMMENT ON COLUMN erp_cs_ticket.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_cs_ticket.contact_id IS '联系人';
                    
      COMMENT ON COLUMN erp_cs_ticket.ticket_type_id IS '工单类型';
                    
      COMMENT ON COLUMN erp_cs_ticket.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_cs_ticket.source IS '来源';
                    
      COMMENT ON COLUMN erp_cs_ticket.assigned_to_id IS '分配处理人';
                    
      COMMENT ON COLUMN erp_cs_ticket.sla_policy_id IS 'SLA 策略';
                    
      COMMENT ON COLUMN erp_cs_ticket.deadline_date_time IS 'SLA 截止时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.is_sla_completed IS 'SLA 是否完成';
                    
      COMMENT ON COLUMN erp_cs_ticket.start_date_time IS '开始处理时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.end_date_time IS '关闭时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.duration IS '处理时长(分钟)';
                    
      COMMENT ON COLUMN erp_cs_ticket.progress IS '进度(%)';
                    
      COMMENT ON COLUMN erp_cs_ticket.status IS '工单状态';
                    
      COMMENT ON COLUMN erp_cs_ticket.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_cs_ticket.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_cs_ticket.remark IS '备注';
                    
      COMMENT ON COLUMN erp_cs_ticket.catalog_item_id IS '服务目录项';
                    
      COMMENT ON COLUMN erp_cs_ticket.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_ticket.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_ticket.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_ticket.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_ticket.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_cs_ticket.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.business_date IS '业务日期';
                    
      COMMENT ON TABLE erp_cs_catalog_fulfillment IS '目录项履行映射';
                
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.code IS '编码';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.catalog_item_id IS '目录项';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.sequence IS '执行顺序';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.action_type IS '动作类型';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.action_config IS '动作配置';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.assign_to_role IS '执行角色';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.estimated_duration IS '预估时长(分钟)';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.is_mandatory IS '是否必须执行';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_ticket_action IS '工单操作日志';
                
      COMMENT ON COLUMN erp_cs_ticket_action.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.ticket_id IS '工单ID';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.action_type IS '操作类型';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.from_status IS '起始状态';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.to_status IS '目标状态';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.operator_id IS '操作人';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.content IS '操作内容';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_survey IS '满意度调查';
                
      COMMENT ON COLUMN erp_cs_survey.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_survey.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_survey.ticket_id IS '关联工单';
                    
      COMMENT ON COLUMN erp_cs_survey.survey_token IS '调查令牌';
                    
      COMMENT ON COLUMN erp_cs_survey.csat_score IS 'CSAT评分';
                    
      COMMENT ON COLUMN erp_cs_survey.nps_score IS 'NPS评分';
                    
      COMMENT ON COLUMN erp_cs_survey.ces_score IS 'CES评分';
                    
      COMMENT ON COLUMN erp_cs_survey.comment IS '文字反馈';
                    
      COMMENT ON COLUMN erp_cs_survey.responded_at IS '响应时间';
                    
      COMMENT ON COLUMN erp_cs_survey.survey_sent_at IS '调查发送时间';
                    
      COMMENT ON COLUMN erp_cs_survey.survey_channel IS '发送渠道';
                    
      COMMENT ON COLUMN erp_cs_survey.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_survey.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_survey.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_survey.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_survey.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_survey.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_time_entry IS '工单计时条目';
                
      COMMENT ON COLUMN erp_cs_time_entry.id IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_time_entry.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_time_entry.ticket_id IS '关联工单';
                    
      COMMENT ON COLUMN erp_cs_time_entry.agent_id IS '处理人';
                    
      COMMENT ON COLUMN erp_cs_time_entry.start_time IS '开始时间';
                    
      COMMENT ON COLUMN erp_cs_time_entry.end_time IS '结束时间';
                    
      COMMENT ON COLUMN erp_cs_time_entry.duration IS '时长(分钟)';
                    
      COMMENT ON COLUMN erp_cs_time_entry.is_billable IS '是否可计费';
                    
      COMMENT ON COLUMN erp_cs_time_entry.billing_rate IS '计费费率';
                    
      COMMENT ON COLUMN erp_cs_time_entry.billable_amount IS '计费金额';
                    
      COMMENT ON COLUMN erp_cs_time_entry.description IS '工作内容描述';
                    
      COMMENT ON COLUMN erp_cs_time_entry.approval_status IS '审批状态';
                    
      COMMENT ON COLUMN erp_cs_time_entry.approved_by_id IS '审批人';
                    
      COMMENT ON COLUMN erp_cs_time_entry.approved_at IS '审批时间';
                    
      COMMENT ON COLUMN erp_cs_time_entry.project_id IS '关联项目';
                    
      COMMENT ON COLUMN erp_cs_time_entry.task_id IS '关联任务';
                    
      COMMENT ON COLUMN erp_cs_time_entry.source IS '来源';
                    
      COMMENT ON COLUMN erp_cs_time_entry.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_time_entry.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_time_entry.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_time_entry.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_time_entry.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_time_entry.update_time IS '修改时间';
                    
