
CREATE TABLE erp_md_md_partner(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_partner primary key (id)
);

CREATE TABLE erp_cs_ticket_type(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  default_priority INT4  ,
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

CREATE TABLE erp_md_auth_user(
  id INT8 NOT NULL ,
  constraint PK_erp_md_auth_user primary key (id)
);

CREATE TABLE erp_md_md_organization(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_organization primary key (id)
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

CREATE TABLE erp_cs_team(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  team_leader_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_team primary key (id)
);

CREATE TABLE erp_cs_sla_policy(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  ticket_type_id INT8  ,
  min_priority INT4  ,
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

CREATE TABLE erp_cs_ticket(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  subject VARCHAR(200) NOT NULL ,
  description VARCHAR(2000)  ,
  customer_id INT8 NOT NULL ,
  contact_id INT8  ,
  ticket_type_id INT8 NOT NULL ,
  priority INT4 NOT NULL ,
  source INT4  ,
  assigned_to_id INT8  ,
  sla_policy_id INT8  ,
  deadline_date_time TIMESTAMP  ,
  is_sla_completed BOOLEAN default false   ,
  start_date_time TIMESTAMP  ,
  end_date_time TIMESTAMP  ,
  duration INT4  ,
  progress INT4 default 0   ,
  status INT4 NOT NULL ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_ticket primary key (id)
);

CREATE TABLE erp_cs_ticket_action(
  id INT8 NOT NULL ,
  ticket_id INT8 NOT NULL ,
  action_type INT4 NOT NULL ,
  from_status INT4  ,
  to_status INT4  ,
  operator_id INT8  ,
  content VARCHAR(4000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_ticket_action primary key (id)
);


      COMMENT ON TABLE erp_md_md_partner IS 'ErpMdPartner';
                
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
                    
      COMMENT ON TABLE erp_md_auth_user IS 'NopAuthUser';
                
      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
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
                    
      COMMENT ON COLUMN erp_cs_ticket.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_ticket.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_ticket.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_ticket.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_ticket.update_time IS '修改时间';
                    
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
                    
