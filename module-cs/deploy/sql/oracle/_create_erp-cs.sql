
CREATE TABLE erp_md_md_partner(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_partner primary key (ID)
);

CREATE TABLE erp_cs_ticket_type(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  DEFAULT_PRIORITY INTEGER  ,
  DEFAULT_SLA_POLICY_ID NUMBER(20)  ,
  SEQUENCE INTEGER default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_ticket_type primary key (ID)
);

CREATE TABLE erp_md_auth_user(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_auth_user primary key (ID)
);

CREATE TABLE erp_md_md_organization(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_organization primary key (ID)
);

CREATE TABLE erp_cs_knowledge_base(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50)  ,
  TITLE VARCHAR2(200) NOT NULL ,
  CONTENT VARCHAR2(4000)  ,
  CATEGORY_ID NUMBER(20)  ,
  IS_PUBLISHED CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_knowledge_base primary key (ID)
);

CREATE TABLE erp_cs_team(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  TEAM_LEADER_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_team primary key (ID)
);

CREATE TABLE erp_cs_sla_policy(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  TICKET_TYPE_ID NUMBER(20)  ,
  MIN_PRIORITY INTEGER  ,
  TEAM_ID NUMBER(20)  ,
  RESOLVE_HOURS INTEGER  ,
  RESOLVE_DAYS INTEGER  ,
  IS_WORKING_DAYS CHAR(1) default 1   ,
  ESCALATION_USER_ID NUMBER(20)  ,
  DESCRIPTION VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_sla_policy primary key (ID)
);

CREATE TABLE erp_cs_ticket(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SUBJECT VARCHAR2(200) NOT NULL ,
  DESCRIPTION VARCHAR2(2000)  ,
  CUSTOMER_ID NUMBER(20) NOT NULL ,
  CONTACT_ID NUMBER(20)  ,
  TICKET_TYPE_ID NUMBER(20) NOT NULL ,
  PRIORITY INTEGER NOT NULL ,
  SOURCE INTEGER  ,
  ASSIGNED_TO_ID NUMBER(20)  ,
  SLA_POLICY_ID NUMBER(20)  ,
  DEADLINE_DATE_TIME DATE  ,
  IS_SLA_COMPLETED CHAR(1) default 0   ,
  START_DATE_TIME DATE  ,
  END_DATE_TIME DATE  ,
  DURATION INTEGER  ,
  PROGRESS INTEGER default 0   ,
  STATUS INTEGER NOT NULL ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_ticket primary key (ID)
);

CREATE TABLE erp_cs_ticket_action(
  ID NUMBER(20) NOT NULL ,
  TICKET_ID NUMBER(20) NOT NULL ,
  ACTION_TYPE INTEGER NOT NULL ,
  FROM_STATUS INTEGER  ,
  TO_STATUS INTEGER  ,
  OPERATOR_ID NUMBER(20)  ,
  CONTENT VARCHAR2(4000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_ticket_action primary key (ID)
);


      COMMENT ON TABLE erp_md_md_partner IS 'ErpMdPartner';
                
      COMMENT ON TABLE erp_cs_ticket_type IS '工单类型';
                
      COMMENT ON COLUMN erp_cs_ticket_type.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.DEFAULT_PRIORITY IS '默认优先级';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.DEFAULT_SLA_POLICY_ID IS '默认 SLA 策略';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_ticket_type.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_auth_user IS 'NopAuthUser';
                
      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_cs_knowledge_base IS '知识库';
                
      COMMENT ON COLUMN erp_cs_knowledge_base.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.TITLE IS '标题';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.CONTENT IS '正文';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.CATEGORY_ID IS '分类';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.IS_PUBLISHED IS '是否发布';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_knowledge_base.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_team IS '客服团队';
                
      COMMENT ON COLUMN erp_cs_team.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_team.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_team.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_cs_team.TEAM_LEADER_ID IS '团队负责人';
                    
      COMMENT ON COLUMN erp_cs_team.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_cs_team.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_team.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_team.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_team.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_team.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_team.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_sla_policy IS 'SLA 策略';
                
      COMMENT ON COLUMN erp_cs_sla_policy.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.TICKET_TYPE_ID IS '适用工单类型';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.MIN_PRIORITY IS '最低触发优先级';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.TEAM_ID IS '适用团队';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.RESOLVE_HOURS IS '解决时限(小时)';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.RESOLVE_DAYS IS '解决时限(天)';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.IS_WORKING_DAYS IS '仅计算工作日';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.ESCALATION_USER_ID IS '升级通知人';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.DESCRIPTION IS '说明';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_sla_policy.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_ticket IS '客服工单';
                
      COMMENT ON COLUMN erp_cs_ticket.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_ticket.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_cs_ticket.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_ticket.SUBJECT IS '工单主题';
                    
      COMMENT ON COLUMN erp_cs_ticket.DESCRIPTION IS '问题描述';
                    
      COMMENT ON COLUMN erp_cs_ticket.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_cs_ticket.CONTACT_ID IS '联系人';
                    
      COMMENT ON COLUMN erp_cs_ticket.TICKET_TYPE_ID IS '工单类型';
                    
      COMMENT ON COLUMN erp_cs_ticket.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_cs_ticket.SOURCE IS '来源';
                    
      COMMENT ON COLUMN erp_cs_ticket.ASSIGNED_TO_ID IS '分配处理人';
                    
      COMMENT ON COLUMN erp_cs_ticket.SLA_POLICY_ID IS 'SLA 策略';
                    
      COMMENT ON COLUMN erp_cs_ticket.DEADLINE_DATE_TIME IS 'SLA 截止时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.IS_SLA_COMPLETED IS 'SLA 是否完成';
                    
      COMMENT ON COLUMN erp_cs_ticket.START_DATE_TIME IS '开始处理时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.END_DATE_TIME IS '关闭时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.DURATION IS '处理时长(分钟)';
                    
      COMMENT ON COLUMN erp_cs_ticket.PROGRESS IS '进度(%)';
                    
      COMMENT ON COLUMN erp_cs_ticket.STATUS IS '工单状态';
                    
      COMMENT ON COLUMN erp_cs_ticket.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_cs_ticket.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_cs_ticket.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_cs_ticket.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_ticket.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_ticket.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_ticket.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_ticket.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_ticket_action IS '工单操作日志';
                
      COMMENT ON COLUMN erp_cs_ticket_action.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.TICKET_ID IS '工单ID';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.ACTION_TYPE IS '操作类型';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.FROM_STATUS IS '起始状态';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.TO_STATUS IS '目标状态';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.OPERATOR_ID IS '操作人';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.CONTENT IS '操作内容';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_ticket_action.UPDATE_TIME IS '修改时间';
                    
