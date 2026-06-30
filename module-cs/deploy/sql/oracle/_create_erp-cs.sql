
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

CREATE TABLE erp_cs_canned_category(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PARENT_ID NUMBER(20)  ,
  ICON VARCHAR2(100)  ,
  SEQUENCE INTEGER default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_canned_category primary key (ID)
);

CREATE TABLE erp_cs_catalog_category(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PARENT_ID NUMBER(20)  ,
  ICON VARCHAR2(100)  ,
  SEQUENCE INTEGER default 0   ,
  IS_ACTIVE CHAR(1) default 1   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_catalog_category primary key (ID)
);

CREATE TABLE erp_pro_project(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_pro_project primary key (ID)
);

CREATE TABLE erp_pro_task(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_pro_task primary key (ID)
);

CREATE TABLE erp_cs_contract(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20)  ,
  CONTRACT_TYPE INTEGER  ,
  START_DATE DATE  ,
  END_DATE DATE  ,
  TOTAL_AMOUNT NUMBER(20,4)  ,
  BILLING_CYCLE INTEGER  ,
  STATUS INTEGER  ,
  ATTACHMENT_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_contract primary key (ID)
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

CREATE TABLE erp_cs_agent_rate(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  AGENT_ID NUMBER(20) NOT NULL ,
  SERVICE_TYPE INTEGER  ,
  RATE NUMBER(20,4)  ,
  EFFECTIVE_DATE DATE  ,
  IS_ACTIVE CHAR(1) default 1   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_agent_rate primary key (ID)
);

CREATE TABLE erp_cs_canned_response(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TITLE VARCHAR2(200) NOT NULL ,
  CONTENT VARCHAR2(4000) NOT NULL ,
  CATEGORY_ID NUMBER(20)  ,
  VARIABLE_DEFS VARCHAR2(4000)  ,
  MACRO_TICKET_TYPE_ID NUMBER(20)  ,
  MACRO_PRIORITY INTEGER  ,
  SEQUENCE INTEGER default 0   ,
  IS_ACTIVE CHAR(1) default 1   ,
  USAGE_COUNT INTEGER default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_canned_response primary key (ID)
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

CREATE TABLE erp_cs_entitlement(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  CONTRACT_ID NUMBER(20)  ,
  SLA_POLICY_ID NUMBER(20)  ,
  SERVICE_TYPE INTEGER NOT NULL ,
  START_DATE DATE  ,
  END_DATE DATE  ,
  MAX_TICKETS INTEGER  ,
  USED_TICKETS INTEGER default 0   ,
  MAX_RESPONSE_TIME INTEGER  ,
  MAX_RESOLUTION_TIME INTEGER  ,
  IS_ACTIVE CHAR(1) default 1   ,
  NOTES VARCHAR2(4000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_entitlement primary key (ID)
);

CREATE TABLE erp_cs_service_catalog_item(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CATEGORY_ID NUMBER(20)  ,
  PARENT_ID NUMBER(20)  ,
  SHORT_DESCRIPTION VARCHAR2(500)  ,
  FULL_DESCRIPTION VARCHAR2(4000)  ,
  TICKET_TYPE_ID NUMBER(20)  ,
  SLA_POLICY_ID NUMBER(20)  ,
  FULFILLMENT_PROCESS_ID VARCHAR2(100)  ,
  REQUEST_FORM_CONFIG VARCHAR2(4000)  ,
  IS_ACTIVE CHAR(1) default 1   ,
  IS_PUBLIC CHAR(1) default 0   ,
  SEQUENCE INTEGER default 0   ,
  ESTIMATED_RESOLUTION VARCHAR2(200)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_service_catalog_item primary key (ID)
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
  CATALOG_ITEM_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_ticket primary key (ID)
);

CREATE TABLE erp_cs_catalog_fulfillment(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CATALOG_ITEM_ID NUMBER(20) NOT NULL ,
  SEQUENCE INTEGER  ,
  ACTION_TYPE INTEGER NOT NULL ,
  ACTION_CONFIG VARCHAR2(4000)  ,
  ASSIGN_TO_ROLE VARCHAR2(100)  ,
  ESTIMATED_DURATION INTEGER  ,
  IS_MANDATORY CHAR(1) default 1   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_catalog_fulfillment primary key (ID)
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

CREATE TABLE erp_cs_survey(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TICKET_ID NUMBER(20) NOT NULL ,
  SURVEY_TOKEN VARCHAR2(50)  ,
  CSAT_SCORE INTEGER  ,
  NPS_SCORE INTEGER  ,
  CES_SCORE INTEGER  ,
  "COMMENT" VARCHAR2(4000)  ,
  RESPONDED_AT DATE  ,
  SURVEY_SENT_AT DATE  ,
  SURVEY_CHANNEL INTEGER  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_survey primary key (ID)
);

CREATE TABLE erp_cs_time_entry(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TICKET_ID NUMBER(20) NOT NULL ,
  AGENT_ID NUMBER(20) NOT NULL ,
  START_TIME DATE  ,
  END_TIME DATE  ,
  DURATION INTEGER  ,
  IS_BILLABLE CHAR(1) default 1   ,
  BILLING_RATE NUMBER(20,4)  ,
  BILLABLE_AMOUNT NUMBER(20,4)  ,
  DESCRIPTION VARCHAR2(1000)  ,
  APPROVAL_STATUS INTEGER  ,
  APPROVED_BY_ID NUMBER(20)  ,
  APPROVED_AT DATE  ,
  PROJECT_ID NUMBER(20)  ,
  TASK_ID NUMBER(20)  ,
  SOURCE INTEGER  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_cs_time_entry primary key (ID)
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
                    
      COMMENT ON TABLE erp_cs_canned_category IS '预设应答分类';
                
      COMMENT ON COLUMN erp_cs_canned_category.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_canned_category.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_canned_category.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_cs_canned_category.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_canned_category.PARENT_ID IS '父分类';
                    
      COMMENT ON COLUMN erp_cs_canned_category.ICON IS '图标';
                    
      COMMENT ON COLUMN erp_cs_canned_category.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_cs_canned_category.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_canned_category.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_canned_category.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_canned_category.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_canned_category.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_canned_category.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_catalog_category IS '目录分类';
                
      COMMENT ON COLUMN erp_cs_catalog_category.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.PARENT_ID IS '父分类';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.ICON IS '图标';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_catalog_category.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pro_project IS 'ErpProProject';
                
      COMMENT ON TABLE erp_pro_task IS 'ErpProTask';
                
      COMMENT ON TABLE erp_cs_contract IS '支持合同';
                
      COMMENT ON COLUMN erp_cs_contract.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_contract.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_contract.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_cs_contract.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_contract.PARTNER_ID IS '签约客户';
                    
      COMMENT ON COLUMN erp_cs_contract.CONTRACT_TYPE IS '合同类型';
                    
      COMMENT ON COLUMN erp_cs_contract.START_DATE IS '生效日期';
                    
      COMMENT ON COLUMN erp_cs_contract.END_DATE IS '到期日期';
                    
      COMMENT ON COLUMN erp_cs_contract.TOTAL_AMOUNT IS '合同总金额';
                    
      COMMENT ON COLUMN erp_cs_contract.BILLING_CYCLE IS '计费周期';
                    
      COMMENT ON COLUMN erp_cs_contract.STATUS IS '合同状态';
                    
      COMMENT ON COLUMN erp_cs_contract.ATTACHMENT_ID IS '合同附件';
                    
      COMMENT ON COLUMN erp_cs_contract.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_contract.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_contract.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_contract.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_contract.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_contract.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_cs_agent_rate IS '客服费率';
                
      COMMENT ON COLUMN erp_cs_agent_rate.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.AGENT_ID IS '客服';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.SERVICE_TYPE IS '服务类型';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.RATE IS '费率(元/小时)';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.EFFECTIVE_DATE IS '生效日期';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_agent_rate.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_canned_response IS '预设应答模板';
                
      COMMENT ON COLUMN erp_cs_canned_response.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_canned_response.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_canned_response.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_canned_response.TITLE IS '模板标题';
                    
      COMMENT ON COLUMN erp_cs_canned_response.CONTENT IS '模板正文';
                    
      COMMENT ON COLUMN erp_cs_canned_response.CATEGORY_ID IS '应答分类';
                    
      COMMENT ON COLUMN erp_cs_canned_response.VARIABLE_DEFS IS '变量定义';
                    
      COMMENT ON COLUMN erp_cs_canned_response.MACRO_TICKET_TYPE_ID IS '自动匹配工单类型';
                    
      COMMENT ON COLUMN erp_cs_canned_response.MACRO_PRIORITY IS '自动匹配优先级';
                    
      COMMENT ON COLUMN erp_cs_canned_response.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_cs_canned_response.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_cs_canned_response.USAGE_COUNT IS '使用次数';
                    
      COMMENT ON COLUMN erp_cs_canned_response.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_canned_response.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_canned_response.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_canned_response.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_canned_response.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_canned_response.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_cs_entitlement IS '服务权益';
                
      COMMENT ON COLUMN erp_cs_entitlement.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_entitlement.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_entitlement.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_entitlement.PARTNER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_cs_entitlement.CONTRACT_ID IS '支持合同';
                    
      COMMENT ON COLUMN erp_cs_entitlement.SLA_POLICY_ID IS 'SLA策略';
                    
      COMMENT ON COLUMN erp_cs_entitlement.SERVICE_TYPE IS '服务类型';
                    
      COMMENT ON COLUMN erp_cs_entitlement.START_DATE IS '生效日期';
                    
      COMMENT ON COLUMN erp_cs_entitlement.END_DATE IS '失效日期';
                    
      COMMENT ON COLUMN erp_cs_entitlement.MAX_TICKETS IS '最大工单数';
                    
      COMMENT ON COLUMN erp_cs_entitlement.USED_TICKETS IS '已用工单数';
                    
      COMMENT ON COLUMN erp_cs_entitlement.MAX_RESPONSE_TIME IS '承诺响应时限(分钟)';
                    
      COMMENT ON COLUMN erp_cs_entitlement.MAX_RESOLUTION_TIME IS '承诺解决时限(分钟)';
                    
      COMMENT ON COLUMN erp_cs_entitlement.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_cs_entitlement.NOTES IS '备注';
                    
      COMMENT ON COLUMN erp_cs_entitlement.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_entitlement.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_entitlement.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_entitlement.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_entitlement.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_entitlement.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_service_catalog_item IS '服务目录项';
                
      COMMENT ON COLUMN erp_cs_service_catalog_item.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.CATEGORY_ID IS '目录分类';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.PARENT_ID IS '父项';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.SHORT_DESCRIPTION IS '简短描述';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.FULL_DESCRIPTION IS '详细说明';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.TICKET_TYPE_ID IS '关联工单类型';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.SLA_POLICY_ID IS '默认SLA策略';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.FULFILLMENT_PROCESS_ID IS '履行流程标识';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.REQUEST_FORM_CONFIG IS '请求表单配置';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.IS_ACTIVE IS '是否上架';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.IS_PUBLIC IS '是否客户可见';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.ESTIMATED_RESOLUTION IS '预计解决时间';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_service_catalog_item.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON COLUMN erp_cs_ticket.CATALOG_ITEM_ID IS '服务目录项';
                    
      COMMENT ON COLUMN erp_cs_ticket.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_ticket.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_ticket.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_ticket.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_ticket.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_ticket.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_catalog_fulfillment IS '目录项履行映射';
                
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.CATALOG_ITEM_ID IS '目录项';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.SEQUENCE IS '执行顺序';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.ACTION_TYPE IS '动作类型';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.ACTION_CONFIG IS '动作配置';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.ASSIGN_TO_ROLE IS '执行角色';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.ESTIMATED_DURATION IS '预估时长(分钟)';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.IS_MANDATORY IS '是否必须执行';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_catalog_fulfillment.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_cs_survey IS '满意度调查';
                
      COMMENT ON COLUMN erp_cs_survey.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_survey.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_survey.TICKET_ID IS '关联工单';
                    
      COMMENT ON COLUMN erp_cs_survey.SURVEY_TOKEN IS '调查令牌';
                    
      COMMENT ON COLUMN erp_cs_survey.CSAT_SCORE IS 'CSAT评分';
                    
      COMMENT ON COLUMN erp_cs_survey.NPS_SCORE IS 'NPS评分';
                    
      COMMENT ON COLUMN erp_cs_survey.CES_SCORE IS 'CES评分';
                    
      COMMENT ON COLUMN erp_cs_survey."COMMENT" IS '文字反馈';
                    
      COMMENT ON COLUMN erp_cs_survey.RESPONDED_AT IS '响应时间';
                    
      COMMENT ON COLUMN erp_cs_survey.SURVEY_SENT_AT IS '调查发送时间';
                    
      COMMENT ON COLUMN erp_cs_survey.SURVEY_CHANNEL IS '发送渠道';
                    
      COMMENT ON COLUMN erp_cs_survey.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_survey.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_survey.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_survey.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_survey.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_survey.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_cs_time_entry IS '工单计时条目';
                
      COMMENT ON COLUMN erp_cs_time_entry.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_cs_time_entry.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_cs_time_entry.TICKET_ID IS '关联工单';
                    
      COMMENT ON COLUMN erp_cs_time_entry.AGENT_ID IS '处理人';
                    
      COMMENT ON COLUMN erp_cs_time_entry.START_TIME IS '开始时间';
                    
      COMMENT ON COLUMN erp_cs_time_entry.END_TIME IS '结束时间';
                    
      COMMENT ON COLUMN erp_cs_time_entry.DURATION IS '时长(分钟)';
                    
      COMMENT ON COLUMN erp_cs_time_entry.IS_BILLABLE IS '是否可计费';
                    
      COMMENT ON COLUMN erp_cs_time_entry.BILLING_RATE IS '计费费率';
                    
      COMMENT ON COLUMN erp_cs_time_entry.BILLABLE_AMOUNT IS '计费金额';
                    
      COMMENT ON COLUMN erp_cs_time_entry.DESCRIPTION IS '工作内容描述';
                    
      COMMENT ON COLUMN erp_cs_time_entry.APPROVAL_STATUS IS '审批状态';
                    
      COMMENT ON COLUMN erp_cs_time_entry.APPROVED_BY_ID IS '审批人';
                    
      COMMENT ON COLUMN erp_cs_time_entry.APPROVED_AT IS '审批时间';
                    
      COMMENT ON COLUMN erp_cs_time_entry.PROJECT_ID IS '关联项目';
                    
      COMMENT ON COLUMN erp_cs_time_entry.TASK_ID IS '关联任务';
                    
      COMMENT ON COLUMN erp_cs_time_entry.SOURCE IS '来源';
                    
      COMMENT ON COLUMN erp_cs_time_entry.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_cs_time_entry.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_cs_time_entry.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_cs_time_entry.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_cs_time_entry.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_cs_time_entry.UPDATE_TIME IS '修改时间';
                    
