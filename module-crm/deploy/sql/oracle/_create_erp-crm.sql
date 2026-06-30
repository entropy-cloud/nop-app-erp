
CREATE TABLE erp_md_partner(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARTNER_TYPE INTEGER  ,
  STATUS INTEGER  ,
  CREDIT_LIMIT VARCHAR2(50)  ,
  constraint PK_erp_md_partner primary key (ID)
);

CREATE TABLE erp_crm_source(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  SEQUENCE INTEGER default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_source primary key (ID)
);

CREATE TABLE erp_crm_lead_status(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  SEQUENCE INTEGER default 0   ,
  IS_DEFAULT CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_status primary key (ID)
);

CREATE TABLE erp_crm_lost_reason(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  SEQUENCE INTEGER default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lost_reason primary key (ID)
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

CREATE TABLE erp_crm_event_category(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  COLOR VARCHAR2(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_event_category primary key (ID)
);

CREATE TABLE erp_crm_quote_template(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  TEMPLATE_CONTENT VARCHAR2(4000)  ,
  IS_DEFAULT CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_quote_template primary key (ID)
);

CREATE TABLE erp_crm_team(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TEAM_LEADER_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_team primary key (ID)
);

CREATE TABLE erp_crm_campaign(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CAMPAIGN_NAME VARCHAR2(200) NOT NULL ,
  MEDIUM VARCHAR2(100)  ,
  SOURCE VARCHAR2(100)  ,
  START_DATE DATE  ,
  END_DATE DATE  ,
  BUDGET_AMOUNT NUMBER(20,4)  ,
  ACTUAL_COST NUMBER(20,4)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_campaign primary key (ID)
);

CREATE TABLE erp_crm_stage(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  STAGE_NAME VARCHAR2(200) NOT NULL ,
  SEQUENCE INTEGER default 0  NOT NULL ,
  TEAM_ID NUMBER(20)  ,
  DEFAULT_PROBABILITY INTEGER  ,
  IS_WON_STAGE CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_stage primary key (ID)
);

CREATE TABLE erp_crm_lead(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  LEAD_TYPE VARCHAR2(20) NOT NULL ,
  PARTNER_ID NUMBER(20)  ,
  CONTACT_NAME VARCHAR2(100)  ,
  CONTACT_PHONE VARCHAR2(50)  ,
  CONTACT_EMAIL VARCHAR2(200)  ,
  COMPANY_NAME VARCHAR2(200)  ,
  JOB_TITLE VARCHAR2(100)  ,
  DEPARTMENT VARCHAR2(100)  ,
  SOURCE_ID NUMBER(20)  ,
  LEAD_STATUS_ID NUMBER(20)  ,
  STAGE_ID NUMBER(20)  ,
  EXPECTED_REVENUE NUMBER(20,4)  ,
  BEST_CASE_AMOUNT NUMBER(20,4)  ,
  WORST_CASE_AMOUNT NUMBER(20,4)  ,
  RECURRING_REVENUE NUMBER(20,4)  ,
  RECURRING_PLAN VARCHAR2(50)  ,
  EXPECTED_CLOSE_DATE DATE  ,
  PROBABILITY INTEGER  ,
  CAMPAIGN_ID NUMBER(20)  ,
  UTM_MEDIUM VARCHAR2(100)  ,
  UTM_SOURCE VARCHAR2(100)  ,
  OWNER_ID NUMBER(20)  ,
  TEAM_ID NUMBER(20)  ,
  LOST_REASON_ID NUMBER(20)  ,
  LOST_REASON_DESC VARCHAR2(500)  ,
  LAST_CONTACT_DATE DATE  ,
  NEXT_ACTIVITY_DATE DATE  ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead primary key (ID)
);

CREATE TABLE erp_crm_event(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  EVENT_TYPE VARCHAR2(20) NOT NULL ,
  EVENT_CATEGORY_ID NUMBER(20)  ,
  SUBJECT VARCHAR2(200) NOT NULL ,
  DESCRIPTION VARCHAR2(2000)  ,
  START_DATE_TIME DATE NOT NULL ,
  END_DATE_TIME DATE  ,
  DURATION INTEGER  ,
  RELATED_LEAD_ID NUMBER(20)  ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  PARTNER_ID NUMBER(20)  ,
  CONTACT_ID NUMBER(20)  ,
  OWNER_ID NUMBER(20)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  PRIORITY VARCHAR2(20) NOT NULL ,
  IS_RECURRENT CHAR(1) default 0   ,
  PARENT_EVENT_ID NUMBER(20)  ,
  REMINDER_MINUTES_BEFORE INTEGER  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_event primary key (ID)
);

CREATE TABLE erp_crm_activity(
  ID NUMBER(20) NOT NULL ,
  LEAD_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ACTIVITY_TYPE VARCHAR2(20) NOT NULL ,
  ACTIVITY_DATE DATE NOT NULL ,
  SUMMARY VARCHAR2(500)  ,
  OWNER_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_activity primary key (ID)
);

CREATE TABLE erp_crm_lead_conv_log(
  ID NUMBER(20) NOT NULL ,
  LEAD_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  FROM_STAGE_ID NUMBER(20)  ,
  TO_STAGE_ID NUMBER(20)  ,
  CHANGED_AT DATE NOT NULL ,
  CHANGED_BY NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_conv_log primary key (ID)
);


      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_crm_source IS '线索来源';
                
      COMMENT ON COLUMN erp_crm_source.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_source.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_source.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_source.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_crm_source.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_source.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_source.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_source.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_source.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_source.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_source.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_status IS '线索状态';
                
      COMMENT ON COLUMN erp_crm_lead_status.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_status.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lead_status.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_lead_status.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lead_status.IS_DEFAULT IS '是否默认';
                    
      COMMENT ON COLUMN erp_crm_lead_status.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_status.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_status.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_status.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_status.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_status.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_status.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lost_reason IS '丢单原因';
                
      COMMENT ON COLUMN erp_crm_lost_reason.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_crm_event_category IS '活动类别';
                
      COMMENT ON COLUMN erp_crm_event_category.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_event_category.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_event_category.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_event_category.COLOR IS '日历颜色';
                    
      COMMENT ON COLUMN erp_crm_event_category.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_event_category.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_event_category.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_event_category.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_event_category.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_event_category.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_event_category.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_quote_template IS '报价模板';
                
      COMMENT ON COLUMN erp_crm_quote_template.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_quote_template.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_quote_template.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_quote_template.TEMPLATE_CONTENT IS '模板内容';
                    
      COMMENT ON COLUMN erp_crm_quote_template.IS_DEFAULT IS '是否默认';
                    
      COMMENT ON COLUMN erp_crm_quote_template.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_quote_template.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_quote_template.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_quote_template.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_quote_template.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_quote_template.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_quote_template.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_team IS '销售团队';
                
      COMMENT ON COLUMN erp_crm_team.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_team.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_team.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_team.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_team.TEAM_LEADER_ID IS '团队负责人';
                    
      COMMENT ON COLUMN erp_crm_team.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_team.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_team.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_team.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_team.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_team.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_team.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_campaign IS '营销活动';
                
      COMMENT ON COLUMN erp_crm_campaign.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_campaign.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_campaign.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_campaign.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_campaign.CAMPAIGN_NAME IS '活动名称';
                    
      COMMENT ON COLUMN erp_crm_campaign.MEDIUM IS 'UTM Medium';
                    
      COMMENT ON COLUMN erp_crm_campaign.SOURCE IS 'UTM Source';
                    
      COMMENT ON COLUMN erp_crm_campaign.START_DATE IS '开始日期';
                    
      COMMENT ON COLUMN erp_crm_campaign.END_DATE IS '结束日期';
                    
      COMMENT ON COLUMN erp_crm_campaign.BUDGET_AMOUNT IS '预算金额';
                    
      COMMENT ON COLUMN erp_crm_campaign.ACTUAL_COST IS '实际成本';
                    
      COMMENT ON COLUMN erp_crm_campaign.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_campaign.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_campaign.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_campaign.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_campaign.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_campaign.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_campaign.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_stage IS '漏斗阶段';
                
      COMMENT ON COLUMN erp_crm_stage.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_stage.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_stage.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_stage.STAGE_NAME IS '阶段名称';
                    
      COMMENT ON COLUMN erp_crm_stage.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_crm_stage.TEAM_ID IS '团队作用域';
                    
      COMMENT ON COLUMN erp_crm_stage.DEFAULT_PROBABILITY IS '默认成交概率%';
                    
      COMMENT ON COLUMN erp_crm_stage.IS_WON_STAGE IS '是否赢单阶段';
                    
      COMMENT ON COLUMN erp_crm_stage.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_stage.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_stage.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_stage.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_stage.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_stage.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_stage.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead IS '线索/商机';
                
      COMMENT ON COLUMN erp_crm_lead.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lead.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead.LEAD_TYPE IS '线索/商机类型';
                    
      COMMENT ON COLUMN erp_crm_lead.PARTNER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_crm_lead.CONTACT_NAME IS '联系人姓名';
                    
      COMMENT ON COLUMN erp_crm_lead.CONTACT_PHONE IS '联系电话';
                    
      COMMENT ON COLUMN erp_crm_lead.CONTACT_EMAIL IS '联系邮箱';
                    
      COMMENT ON COLUMN erp_crm_lead.COMPANY_NAME IS '公司名称';
                    
      COMMENT ON COLUMN erp_crm_lead.JOB_TITLE IS '职位';
                    
      COMMENT ON COLUMN erp_crm_lead.DEPARTMENT IS '部门';
                    
      COMMENT ON COLUMN erp_crm_lead.SOURCE_ID IS '线索来源';
                    
      COMMENT ON COLUMN erp_crm_lead.LEAD_STATUS_ID IS '线索状态';
                    
      COMMENT ON COLUMN erp_crm_lead.STAGE_ID IS '漏斗阶段';
                    
      COMMENT ON COLUMN erp_crm_lead.EXPECTED_REVENUE IS '预期收入';
                    
      COMMENT ON COLUMN erp_crm_lead.BEST_CASE_AMOUNT IS '乐观预期';
                    
      COMMENT ON COLUMN erp_crm_lead.WORST_CASE_AMOUNT IS '悲观预期';
                    
      COMMENT ON COLUMN erp_crm_lead.RECURRING_REVENUE IS '周期性收入';
                    
      COMMENT ON COLUMN erp_crm_lead.RECURRING_PLAN IS '周期性计划';
                    
      COMMENT ON COLUMN erp_crm_lead.EXPECTED_CLOSE_DATE IS '预期签单日';
                    
      COMMENT ON COLUMN erp_crm_lead.PROBABILITY IS '成交概率';
                    
      COMMENT ON COLUMN erp_crm_lead.CAMPAIGN_ID IS '营销活动';
                    
      COMMENT ON COLUMN erp_crm_lead.UTM_MEDIUM IS 'UTM Medium';
                    
      COMMENT ON COLUMN erp_crm_lead.UTM_SOURCE IS 'UTM Source';
                    
      COMMENT ON COLUMN erp_crm_lead.OWNER_ID IS '负责人';
                    
      COMMENT ON COLUMN erp_crm_lead.TEAM_ID IS '销售团队';
                    
      COMMENT ON COLUMN erp_crm_lead.LOST_REASON_ID IS '丢单原因';
                    
      COMMENT ON COLUMN erp_crm_lead.LOST_REASON_DESC IS '丢单描述';
                    
      COMMENT ON COLUMN erp_crm_lead.LAST_CONTACT_DATE IS '最后联系日期';
                    
      COMMENT ON COLUMN erp_crm_lead.NEXT_ACTIVITY_DATE IS '下次活动日期';
                    
      COMMENT ON COLUMN erp_crm_lead.RELATED_BILL_TYPE IS '转化结果单据类型';
                    
      COMMENT ON COLUMN erp_crm_lead.RELATED_BILL_CODE IS '转化结果单据号';
                    
      COMMENT ON COLUMN erp_crm_lead.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_crm_lead.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_event IS '活动/事件';
                
      COMMENT ON COLUMN erp_crm_event.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_event.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_event.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_event.EVENT_TYPE IS '事件类型';
                    
      COMMENT ON COLUMN erp_crm_event.EVENT_CATEGORY_ID IS '活动类别';
                    
      COMMENT ON COLUMN erp_crm_event.SUBJECT IS '主题';
                    
      COMMENT ON COLUMN erp_crm_event.DESCRIPTION IS '描述';
                    
      COMMENT ON COLUMN erp_crm_event.START_DATE_TIME IS '开始时间';
                    
      COMMENT ON COLUMN erp_crm_event.END_DATE_TIME IS '结束时间';
                    
      COMMENT ON COLUMN erp_crm_event.DURATION IS '时长(分钟)';
                    
      COMMENT ON COLUMN erp_crm_event.RELATED_LEAD_ID IS '关联线索/商机';
                    
      COMMENT ON COLUMN erp_crm_event.RELATED_BILL_TYPE IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_crm_event.RELATED_BILL_CODE IS '关联单据号';
                    
      COMMENT ON COLUMN erp_crm_event.PARTNER_ID IS '关联客户';
                    
      COMMENT ON COLUMN erp_crm_event.CONTACT_ID IS '联系人';
                    
      COMMENT ON COLUMN erp_crm_event.OWNER_ID IS '负责人';
                    
      COMMENT ON COLUMN erp_crm_event.STATUS IS '活动状态';
                    
      COMMENT ON COLUMN erp_crm_event.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_crm_event.IS_RECURRENT IS '是否重复事件';
                    
      COMMENT ON COLUMN erp_crm_event.PARENT_EVENT_ID IS '父事件';
                    
      COMMENT ON COLUMN erp_crm_event.REMINDER_MINUTES_BEFORE IS '提醒提前分钟数';
                    
      COMMENT ON COLUMN erp_crm_event.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_event.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_event.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_event.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_event.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_event.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_event.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_activity IS '活动记录';
                
      COMMENT ON COLUMN erp_crm_activity.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_activity.LEAD_ID IS '关联线索/商机';
                    
      COMMENT ON COLUMN erp_crm_activity.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_activity.ACTIVITY_TYPE IS '活动类型';
                    
      COMMENT ON COLUMN erp_crm_activity.ACTIVITY_DATE IS '活动日期';
                    
      COMMENT ON COLUMN erp_crm_activity.SUMMARY IS '内容摘要';
                    
      COMMENT ON COLUMN erp_crm_activity.OWNER_ID IS '负责人';
                    
      COMMENT ON COLUMN erp_crm_activity.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_activity.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_activity.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_activity.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_activity.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_activity.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_activity.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_conv_log IS '阶段流转日志';
                
      COMMENT ON COLUMN erp_crm_lead_conv_log.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.LEAD_ID IS '线索/商机';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.FROM_STAGE_ID IS '前阶段';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.TO_STAGE_ID IS '后阶段';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.CHANGED_AT IS '变更时间';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.CHANGED_BY IS '变更人';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.UPDATE_TIME IS '修改时间';
                    
