
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

CREATE TABLE erp_md_partner_contact(
  ID NUMBER(20)  ,
  CONTACT_NAME VARCHAR2(100)  ,
  PHONE VARCHAR2(50)  ,
  EMAIL VARCHAR2(200)  ,
  CONTACT_PERSON VARCHAR2(200)  ,
  constraint PK_erp_md_partner_contact primary key (ID)
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

CREATE TABLE erp_md_currency(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_currency primary key (ID)
);

CREATE TABLE erp_md_material(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_material primary key (ID)
);

CREATE TABLE erp_crm_team(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TEAM_LEADER_ID VARCHAR2(36)  ,
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

CREATE TABLE erp_crm_lead_score_config(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONFIG_NAME VARCHAR2(200) NOT NULL ,
  IS_ACTIVE CHAR(1) default 0   ,
  EFFECTIVE_FROM DATE  ,
  EFFECTIVE_TO DATE  ,
  AUTO_QUALIFY_THRESHOLD INTEGER  ,
  MIN_SCORE_FOR_FOLLOW_UP INTEGER default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_score_config primary key (ID)
);

CREATE TABLE erp_crm_forecast_period(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PERIOD_TYPE VARCHAR2(20) NOT NULL ,
  PERIOD_START DATE NOT NULL ,
  PERIOD_END DATE NOT NULL ,
  LABEL VARCHAR2(100)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  IS_CURRENT CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_forecast_period primary key (ID)
);

CREATE TABLE erp_crm_territory(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PARENT_ID NUMBER(20)  ,
  TERRITORY_TYPE VARCHAR2(20) NOT NULL ,
  MANAGER_ID NUMBER(20)  ,
  DESCRIPTION VARCHAR2(500)  ,
  FULL_PATH VARCHAR2(500)  ,
  "LEVEL" INTEGER default 0   ,
  IS_ACTIVE CHAR(1) default 1   ,
  IS_LEAF CHAR(1) default 0   ,
  SORT_ORDER INTEGER default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_territory primary key (ID)
);

CREATE TABLE erp_crm_product_configurator(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PRODUCT_TYPE VARCHAR2(100)  ,
  CONFIG_NAME VARCHAR2(200)  ,
  WIZARD_LAYOUT VARCHAR2(4000)  ,
  IS_ACTIVE CHAR(1) default 1   ,
  EFFECTIVE_FROM DATE  ,
  EFFECTIVE_TO DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_product_configurator primary key (ID)
);

CREATE TABLE erp_crm_bundle_pricing(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUNDLE_NAME VARCHAR2(200) NOT NULL ,
  DESCRIPTION VARCHAR2(1000)  ,
  DISCOUNT_TYPE VARCHAR2(20)  ,
  DISCOUNT_VALUE NUMBER(20,4)  ,
  BUNDLE_AMOUNT NUMBER(20,4)  ,
  EFFECTIVE_FROM DATE  ,
  EFFECTIVE_TO DATE  ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_bundle_pricing primary key (ID)
);

CREATE TABLE erp_crm_sequence(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TEMPLATE_TYPE VARCHAR2(30) NOT NULL ,
  DESCRIPTION VARCHAR2(500)  ,
  EXPECTED_DURATION INTEGER  ,
  IS_ACTIVE CHAR(1) default 1   ,
  IS_DEFAULT CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_sequence primary key (ID)
);

CREATE TABLE erp_crm_price_rule(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  RULE_TYPE VARCHAR2(30) NOT NULL ,
  PRIORITY INTEGER default 0   ,
  PRODUCT_ID NUMBER(20)  ,
  PRODUCT_CATEGORY VARCHAR2(100)  ,
  CUSTOMER_ID NUMBER(20)  ,
  CUSTOMER_CATEGORY VARCHAR2(100)  ,
  MIN_QUANTITY NUMBER(20,4)  ,
  MAX_QUANTITY NUMBER(20,4)  ,
  PRICE_OVERRIDE NUMBER(20,4)  ,
  DISCOUNT_PERCENT NUMBER(10,2)  ,
  DISCOUNT_AMOUNT NUMBER(20,4)  ,
  CURRENCY_ID NUMBER(20)  ,
  EFFECTIVE_FROM DATE  ,
  EFFECTIVE_TO DATE  ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_price_rule primary key (ID)
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

CREATE TABLE erp_crm_lead_score_config_line(
  ID NUMBER(20) NOT NULL ,
  CONFIG_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CRITERION_CODE VARCHAR2(50) NOT NULL ,
  CRITERION_NAME VARCHAR2(200)  ,
  WEIGHT INTEGER default 0   ,
  SCORING_METHOD VARCHAR2(20) NOT NULL ,
  LOOKUP_TABLE VARCHAR2(4000)  ,
  FORMULA VARCHAR2(500)  ,
  MAX_SCORE INTEGER default 0   ,
  SEQUENCE INTEGER default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_score_config_line primary key (ID)
);

CREATE TABLE erp_crm_forecast(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PERIOD_ID NUMBER(20) NOT NULL ,
  TERRITORY_ID NUMBER(20)  ,
  TEAM_ID NUMBER(20)  ,
  OWNER_ID VARCHAR2(36)  ,
  CURRENCY_ID NUMBER(20)  ,
  COMMIT_AMOUNT NUMBER(20,4)  ,
  UPSIDE_AMOUNT NUMBER(20,4)  ,
  WEIGHTED_AMOUNT NUMBER(20,4)  ,
  BEST_CASE_AMOUNT NUMBER(20,4)  ,
  OPPORTUNITY_COUNT INTEGER default 0   ,
  COMMIT_OPPORTUNITY_COUNT INTEGER default 0   ,
  EXPECTED_CLOSED_REVENUE NUMBER(20,4)  ,
  LAST_CALCULATED_AT TIMESTAMP  ,
  NOTES VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_forecast primary key (ID)
);

CREATE TABLE erp_crm_territory_assignment_rule(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  RULE_NAME VARCHAR2(200) NOT NULL ,
  PRIORITY INTEGER default 0   ,
  TERRITORY_ID NUMBER(20) NOT NULL ,
  CONDITION_TYPE VARCHAR2(30) NOT NULL ,
  CONDITION_VALUE VARCHAR2(4000)  ,
  ASSIGNMENT_METHOD VARCHAR2(20) NOT NULL ,
  GROUP_ID NUMBER(20)  ,
  IS_DEFAULT CHAR(1) default 0   ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_territory_assignment_rule primary key (ID)
);

CREATE TABLE erp_crm_quota(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TERRITORY_ID NUMBER(20)  ,
  TEAM_ID NUMBER(20)  ,
  OWNER_ID VARCHAR2(36)  ,
  PERIOD_TYPE VARCHAR2(20) NOT NULL ,
  FISCAL_YEAR INTEGER  ,
  PERIOD_LABEL VARCHAR2(100)  ,
  QUOTA_AMOUNT NUMBER(20,4)  ,
  CURRENCY_ID NUMBER(20)  ,
  IS_FINALIZED CHAR(1) default 0   ,
  NOTES VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_quota primary key (ID)
);

CREATE TABLE erp_crm_lead_funnel(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  FUNNEL_NAME VARCHAR2(200) NOT NULL ,
  PERIOD_START DATE NOT NULL ,
  PERIOD_END DATE NOT NULL ,
  TERRITORY_ID NUMBER(20)  ,
  TEAM_ID NUMBER(20)  ,
  SOURCE_ID NUMBER(20)  ,
  TOTAL_LEADS_AT_TOP INTEGER default 0   ,
  TOTAL_OPPORTUNITIES INTEGER default 0   ,
  TOTAL_WON INTEGER default 0   ,
  TOTAL_LOST INTEGER default 0   ,
  TOTAL_REVENUE NUMBER(20,4)  ,
  LOST_REVENUE NUMBER(20,4)  ,
  WEIGHTED_REVENUE NUMBER(20,4)  ,
  AVG_DEAL_SIZE NUMBER(20,4)  ,
  AVG_SALES_CYCLE_DAYS NUMBER(10,2)  ,
  CALCULATED_AT TIMESTAMP  ,
  CALCULATED_BY VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_funnel primary key (ID)
);

CREATE TABLE erp_crm_config_rule(
  ID NUMBER(20) NOT NULL ,
  CONFIGURATOR_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  RULE_TYPE VARCHAR2(20) NOT NULL ,
  SOURCE_FEATURE_CODE VARCHAR2(100)  ,
  SOURCE_FEATURE_VALUE VARCHAR2(200)  ,
  TARGET_FEATURE_CODE VARCHAR2(100)  ,
  TARGET_FEATURE_VALUE VARCHAR2(200)  ,
  CONDITION_EXPRESSION VARCHAR2(500)  ,
  SEQUENCE INTEGER default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_config_rule primary key (ID)
);

CREATE TABLE erp_crm_bundle_pricing_line(
  ID NUMBER(20) NOT NULL ,
  BUNDLE_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PRODUCT_ID NUMBER(20)  ,
  QUANTITY NUMBER(20,4)  ,
  UNIT_PRICE NUMBER(20,4)  ,
  SEQUENCE INTEGER default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_bundle_pricing_line primary key (ID)
);

CREATE TABLE erp_crm_sequence_step(
  ID NUMBER(20) NOT NULL ,
  SEQUENCE_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  STEP_NAME VARCHAR2(200) NOT NULL ,
  STEP_ORDER INTEGER default 0  NOT NULL ,
  DUE_DAYS INTEGER default 0   ,
  ACTIVITY_TYPE VARCHAR2(20) NOT NULL ,
  STEP_DESCRIPTION VARCHAR2(1000)  ,
  COMPLETION_CONDITION VARCHAR2(30) NOT NULL ,
  IS_MANDATORY CHAR(1) default 1   ,
  AUTO_CREATE_EVENT CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_sequence_step primary key (ID)
);

CREATE TABLE erp_crm_sequence_assignment(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SEQUENCE_ID NUMBER(20) NOT NULL ,
  PRIORITY INTEGER default 0   ,
  CONDITION_TYPE VARCHAR2(30) NOT NULL ,
  CONDITION_VALUE VARCHAR2(4000)  ,
  IS_ACTIVE CHAR(1) default 1   ,
  IS_DEFAULT CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_sequence_assignment primary key (ID)
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
  OWNER_ID VARCHAR2(36)  ,
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
  TERRITORY_ID NUMBER(20)  ,
  constraint PK_erp_crm_lead primary key (ID)
);

CREATE TABLE erp_crm_forecast_accuracy(
  ID NUMBER(20) NOT NULL ,
  FORECAST_ID NUMBER(20)  ,
  ORG_ID NUMBER(20)  ,
  PERIOD_ID NUMBER(20) NOT NULL ,
  OWNER_ID VARCHAR2(36)  ,
  TEAM_ID NUMBER(20)  ,
  TERRITORY_ID NUMBER(20)  ,
  COMMIT_AMOUNT NUMBER(20,4)  ,
  UPSIDE_AMOUNT NUMBER(20,4)  ,
  ACTUAL_CLOSED_REVENUE NUMBER(20,4)  ,
  COMMIT_ACCURACY NUMBER(10,4)  ,
  UPSIDE_ACCURACY NUMBER(10,4)  ,
  DEVIATION_AMOUNT NUMBER(20,4)  ,
  CALCULATED_BY VARCHAR2(50)  ,
  CALCULATED_AT TIMESTAMP  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_forecast_accuracy primary key (ID)
);

CREATE TABLE erp_crm_funnel_stage_metrics(
  ID NUMBER(20) NOT NULL ,
  FUNNEL_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  STAGE_ID NUMBER(20) NOT NULL ,
  STAGE_ORDER INTEGER default 0   ,
  STAGE_NAME VARCHAR2(200)  ,
  LEAD_COUNT_IN INTEGER default 0   ,
  LEAD_COUNT_OUT INTEGER default 0   ,
  LEAD_COUNT_REMAINING INTEGER default 0   ,
  CONVERSION_RATE NUMBER(10,4)  ,
  DROP_OFF_RATE NUMBER(10,4)  ,
  AVG_DAYS_IN_STAGE NUMBER(10,2)  ,
  LOST_COUNT INTEGER default 0   ,
  LOST_AMOUNT NUMBER(20,4)  ,
  LOST_REASON_TOP VARCHAR2(4000)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_funnel_stage_metrics primary key (ID)
);

CREATE TABLE erp_crm_event(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  EVENT_TYPE VARCHAR2(20) NOT NULL ,
  EVENT_CATEGORY_ID NUMBER(20)  ,
  SUBJECT VARCHAR2(200) NOT NULL ,
  DESCRIPTION VARCHAR2(2000)  ,
  START_DATE_TIME TIMESTAMP NOT NULL ,
  END_DATE_TIME TIMESTAMP  ,
  DURATION INTEGER  ,
  RELATED_LEAD_ID NUMBER(20)  ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  PARTNER_ID NUMBER(20)  ,
  CONTACT_ID NUMBER(20)  ,
  OWNER_ID VARCHAR2(36)  ,
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
  OWNER_ID VARCHAR2(36)  ,
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
  CHANGED_AT TIMESTAMP NOT NULL ,
  CHANGED_BY VARCHAR2(36)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_conv_log primary key (ID)
);

CREATE TABLE erp_crm_lead_score(
  ID NUMBER(20) NOT NULL ,
  LEAD_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONFIG_ID NUMBER(20)  ,
  TOTAL_SCORE INTEGER default 0   ,
  SCORE_BREAKDOWN VARCHAR2(4000)  ,
  AUTO_QUALIFIED CHAR(1) default 0   ,
  TRIGGERED_ACTION VARCHAR2(20)  ,
  CALCULATED_AT TIMESTAMP  ,
  TRIGGER_EVENT VARCHAR2(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_score primary key (ID)
);

CREATE TABLE erp_crm_forecast_line(
  ID NUMBER(20) NOT NULL ,
  FORECAST_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  LEAD_ID NUMBER(20) NOT NULL ,
  PROBABILITY INTEGER  ,
  EXPECTED_REVENUE NUMBER(20,4)  ,
  WEIGHTED_REVENUE NUMBER(20,4)  ,
  FORECAST_CATEGORY VARCHAR2(20) NOT NULL ,
  INCLUDED_IN_COMMIT CHAR(1) default 0   ,
  STAGE_NAME VARCHAR2(200)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_forecast_line primary key (ID)
);

CREATE TABLE erp_crm_lead_seq_progress(
  ID NUMBER(20) NOT NULL ,
  LEAD_ID NUMBER(20) NOT NULL ,
  SEQUENCE_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CURRENT_STEP_INDEX INTEGER default 0   ,
  STATUS VARCHAR2(20) NOT NULL ,
  STARTED_AT TIMESTAMP  ,
  COMPLETED_AT TIMESTAMP  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_seq_progress primary key (ID)
);

CREATE TABLE erp_crm_lead_score_line(
  ID NUMBER(20) NOT NULL ,
  SCORE_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONFIG_LINE_ID NUMBER(20)  ,
  CRITERION_CODE VARCHAR2(50)  ,
  CRITERION_NAME VARCHAR2(200)  ,
  RAW_VALUE VARCHAR2(500)  ,
  LOOKUP_VALUE VARCHAR2(200)  ,
  RAW_SCORE INTEGER default 0   ,
  WEIGHTED_SCORE INTEGER default 0   ,
  SEQUENCE INTEGER default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_score_line primary key (ID)
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
                    
      COMMENT ON TABLE erp_md_partner_contact IS '往来单位联系人';
                
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
                    
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
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
                    
      COMMENT ON TABLE erp_crm_lead_score_config IS '评分规则配置';
                
      COMMENT ON COLUMN erp_crm_lead_score_config.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.CONFIG_NAME IS '规则集名称';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.IS_ACTIVE IS '是否生效';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.EFFECTIVE_FROM IS '生效开始日期';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.EFFECTIVE_TO IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.AUTO_QUALIFY_THRESHOLD IS '自动转商机阈值';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.MIN_SCORE_FOR_FOLLOW_UP IS '最低跟进分数';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_forecast_period IS '预测期间';
                
      COMMENT ON COLUMN erp_crm_forecast_period.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.PERIOD_TYPE IS '期间类型';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.PERIOD_START IS '期间开始日期';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.PERIOD_END IS '期间结束日期';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.LABEL IS '期间标签';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.STATUS IS '期间状态';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.IS_CURRENT IS '是否当前期间';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_territory IS '销售区域';
                
      COMMENT ON COLUMN erp_crm_territory.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_territory.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_territory.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_territory.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_territory.PARENT_ID IS '父区域';
                    
      COMMENT ON COLUMN erp_crm_territory.TERRITORY_TYPE IS '区域类型';
                    
      COMMENT ON COLUMN erp_crm_territory.MANAGER_ID IS '区域负责人';
                    
      COMMENT ON COLUMN erp_crm_territory.DESCRIPTION IS '区域描述';
                    
      COMMENT ON COLUMN erp_crm_territory.FULL_PATH IS '路径冗余';
                    
      COMMENT ON COLUMN erp_crm_territory."LEVEL" IS '层级深度';
                    
      COMMENT ON COLUMN erp_crm_territory.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_territory.IS_LEAF IS '是否叶子节点';
                    
      COMMENT ON COLUMN erp_crm_territory.SORT_ORDER IS '排序';
                    
      COMMENT ON COLUMN erp_crm_territory.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_territory.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_territory.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_territory.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_territory.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_territory.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_territory.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_product_configurator IS '产品配置器';
                
      COMMENT ON COLUMN erp_crm_product_configurator.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.PRODUCT_TYPE IS '适用产品品类';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.CONFIG_NAME IS '配置器名称';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.WIZARD_LAYOUT IS '向导步骤布局(JSON)';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.EFFECTIVE_FROM IS '生效开始日期';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.EFFECTIVE_TO IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_bundle_pricing IS '捆绑定价';
                
      COMMENT ON COLUMN erp_crm_bundle_pricing.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.BUNDLE_NAME IS '捆绑包名称';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.DESCRIPTION IS '描述';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.DISCOUNT_TYPE IS '折扣类型';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.DISCOUNT_VALUE IS '折扣值';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.BUNDLE_AMOUNT IS '捆绑包总价';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.EFFECTIVE_FROM IS '生效开始日期';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.EFFECTIVE_TO IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_sequence IS '销售序列';
                
      COMMENT ON COLUMN erp_crm_sequence.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_sequence.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_sequence.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_sequence.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_sequence.TEMPLATE_TYPE IS '序列模板类型';
                    
      COMMENT ON COLUMN erp_crm_sequence.DESCRIPTION IS '描述';
                    
      COMMENT ON COLUMN erp_crm_sequence.EXPECTED_DURATION IS '预期完成天数';
                    
      COMMENT ON COLUMN erp_crm_sequence.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_sequence.IS_DEFAULT IS '是否默认序列';
                    
      COMMENT ON COLUMN erp_crm_sequence.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_sequence.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_sequence.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_sequence.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_sequence.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_sequence.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_sequence.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_price_rule IS '价格规则';
                
      COMMENT ON COLUMN erp_crm_price_rule.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_price_rule.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_crm_price_rule.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_crm_price_rule.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_price_rule.RULE_TYPE IS '规则类型';
                    
      COMMENT ON COLUMN erp_crm_price_rule.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_crm_price_rule.PRODUCT_ID IS '适用产品';
                    
      COMMENT ON COLUMN erp_crm_price_rule.PRODUCT_CATEGORY IS '适用产品品类';
                    
      COMMENT ON COLUMN erp_crm_price_rule.CUSTOMER_ID IS '适用客户';
                    
      COMMENT ON COLUMN erp_crm_price_rule.CUSTOMER_CATEGORY IS '适用客户类别';
                    
      COMMENT ON COLUMN erp_crm_price_rule.MIN_QUANTITY IS '最小数量';
                    
      COMMENT ON COLUMN erp_crm_price_rule.MAX_QUANTITY IS '最大数量';
                    
      COMMENT ON COLUMN erp_crm_price_rule.PRICE_OVERRIDE IS '覆盖单价';
                    
      COMMENT ON COLUMN erp_crm_price_rule.DISCOUNT_PERCENT IS '折扣百分比';
                    
      COMMENT ON COLUMN erp_crm_price_rule.DISCOUNT_AMOUNT IS '折扣固定金额';
                    
      COMMENT ON COLUMN erp_crm_price_rule.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_crm_price_rule.EFFECTIVE_FROM IS '生效开始日期';
                    
      COMMENT ON COLUMN erp_crm_price_rule.EFFECTIVE_TO IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_crm_price_rule.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_price_rule.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_price_rule.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_price_rule.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_price_rule.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_price_rule.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_price_rule.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_price_rule.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_crm_lead_score_config_line IS '评分准则明细';
                
      COMMENT ON COLUMN erp_crm_lead_score_config_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.CONFIG_ID IS '评分规则配置';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.CRITERION_CODE IS '准则编码';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.CRITERION_NAME IS '准则名称';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.WEIGHT IS '权重系数';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.SCORING_METHOD IS '评分方法';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.LOOKUP_TABLE IS '值表(JSON)';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.FORMULA IS '公式表达式';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.MAX_SCORE IS '最高分';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_forecast IS '预测数据';
                
      COMMENT ON COLUMN erp_crm_forecast.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_forecast.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_forecast.PERIOD_ID IS '预测期间';
                    
      COMMENT ON COLUMN erp_crm_forecast.TERRITORY_ID IS '销售区域';
                    
      COMMENT ON COLUMN erp_crm_forecast.TEAM_ID IS '销售团队';
                    
      COMMENT ON COLUMN erp_crm_forecast.OWNER_ID IS '销售员';
                    
      COMMENT ON COLUMN erp_crm_forecast.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_crm_forecast.COMMIT_AMOUNT IS '承诺金额';
                    
      COMMENT ON COLUMN erp_crm_forecast.UPSIDE_AMOUNT IS '乐观金额';
                    
      COMMENT ON COLUMN erp_crm_forecast.WEIGHTED_AMOUNT IS '加权金额';
                    
      COMMENT ON COLUMN erp_crm_forecast.BEST_CASE_AMOUNT IS '最佳金额';
                    
      COMMENT ON COLUMN erp_crm_forecast.OPPORTUNITY_COUNT IS '商机总数';
                    
      COMMENT ON COLUMN erp_crm_forecast.COMMIT_OPPORTUNITY_COUNT IS '承诺商机数';
                    
      COMMENT ON COLUMN erp_crm_forecast.EXPECTED_CLOSED_REVENUE IS '实际已关闭收入';
                    
      COMMENT ON COLUMN erp_crm_forecast.LAST_CALCULATED_AT IS '最近计算时间';
                    
      COMMENT ON COLUMN erp_crm_forecast.NOTES IS '备注';
                    
      COMMENT ON COLUMN erp_crm_forecast.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_forecast.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_forecast.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_forecast.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_forecast.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_forecast.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_territory_assignment_rule IS '区域分配规则';
                
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.RULE_NAME IS '规则名称';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.TERRITORY_ID IS '目标区域';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.CONDITION_TYPE IS '条件类型';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.CONDITION_VALUE IS '条件值(JSON)';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.ASSIGNMENT_METHOD IS '分配方法';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.GROUP_ID IS '目标团队';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.IS_DEFAULT IS '是否默认规则';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_quota IS '销售配额';
                
      COMMENT ON COLUMN erp_crm_quota.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_quota.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_quota.TERRITORY_ID IS '区域';
                    
      COMMENT ON COLUMN erp_crm_quota.TEAM_ID IS '团队';
                    
      COMMENT ON COLUMN erp_crm_quota.OWNER_ID IS '销售员';
                    
      COMMENT ON COLUMN erp_crm_quota.PERIOD_TYPE IS '配额期间类型';
                    
      COMMENT ON COLUMN erp_crm_quota.FISCAL_YEAR IS '财年';
                    
      COMMENT ON COLUMN erp_crm_quota.PERIOD_LABEL IS '期间标签';
                    
      COMMENT ON COLUMN erp_crm_quota.QUOTA_AMOUNT IS '配额金额';
                    
      COMMENT ON COLUMN erp_crm_quota.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_crm_quota.IS_FINALIZED IS '是否已定稿';
                    
      COMMENT ON COLUMN erp_crm_quota.NOTES IS '备注';
                    
      COMMENT ON COLUMN erp_crm_quota.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_quota.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_quota.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_quota.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_quota.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_quota.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_funnel IS '线索漏斗';
                
      COMMENT ON COLUMN erp_crm_lead_funnel.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.FUNNEL_NAME IS '漏斗名称';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.PERIOD_START IS '分析期间开始';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.PERIOD_END IS '分析期间结束';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.TERRITORY_ID IS '区域维度';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.TEAM_ID IS '团队维度';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.SOURCE_ID IS '来源维度';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.TOTAL_LEADS_AT_TOP IS '漏斗顶部线索量';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.TOTAL_OPPORTUNITIES IS '商机总量';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.TOTAL_WON IS '赢单总量';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.TOTAL_LOST IS '丢单总量';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.TOTAL_REVENUE IS '赢单总收入';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.LOST_REVENUE IS '丢失金额合计';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.WEIGHTED_REVENUE IS '加权收入合计';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.AVG_DEAL_SIZE IS '平均赢单金额';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.AVG_SALES_CYCLE_DAYS IS '平均销售周期天数';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.CALCULATED_AT IS '聚合计算时间';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.CALCULATED_BY IS '聚合计算人';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_config_rule IS '配置规则';
                
      COMMENT ON COLUMN erp_crm_config_rule.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_config_rule.CONFIGURATOR_ID IS '所属配置器';
                    
      COMMENT ON COLUMN erp_crm_config_rule.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_config_rule.RULE_TYPE IS '规则类型';
                    
      COMMENT ON COLUMN erp_crm_config_rule.SOURCE_FEATURE_CODE IS '条件特征编码';
                    
      COMMENT ON COLUMN erp_crm_config_rule.SOURCE_FEATURE_VALUE IS '条件特征值';
                    
      COMMENT ON COLUMN erp_crm_config_rule.TARGET_FEATURE_CODE IS '目标特征编码';
                    
      COMMENT ON COLUMN erp_crm_config_rule.TARGET_FEATURE_VALUE IS '目标特征值';
                    
      COMMENT ON COLUMN erp_crm_config_rule.CONDITION_EXPRESSION IS '复杂条件表达式';
                    
      COMMENT ON COLUMN erp_crm_config_rule.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_crm_config_rule.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_config_rule.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_config_rule.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_config_rule.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_config_rule.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_config_rule.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_config_rule.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_bundle_pricing_line IS '捆绑包明细';
                
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.BUNDLE_ID IS '所属捆绑包';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.PRODUCT_ID IS '产品';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.UNIT_PRICE IS '单价';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_sequence_step IS '序列步骤';
                
      COMMENT ON COLUMN erp_crm_sequence_step.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.SEQUENCE_ID IS '所属序列';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.STEP_NAME IS '步骤名称';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.STEP_ORDER IS '步骤序号';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.DUE_DAYS IS '距上一步间隔天数';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.ACTIVITY_TYPE IS '步骤活动类型';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.STEP_DESCRIPTION IS '步骤说明';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.COMPLETION_CONDITION IS '完成条件';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.IS_MANDATORY IS '是否必须完成';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.AUTO_CREATE_EVENT IS '是否自动创建事件';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_sequence_assignment IS '序列分配规则';
                
      COMMENT ON COLUMN erp_crm_sequence_assignment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.SEQUENCE_ID IS '目标序列';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.CONDITION_TYPE IS '条件类型';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.CONDITION_VALUE IS '条件值(JSON)';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.IS_DEFAULT IS '是否默认规则';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON COLUMN erp_crm_lead.TERRITORY_ID IS '销售区域';
                    
      COMMENT ON TABLE erp_crm_forecast_accuracy IS '预测准确率';
                
      COMMENT ON COLUMN erp_crm_forecast_accuracy.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.FORECAST_ID IS '预测数据';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.PERIOD_ID IS '预测期间';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.OWNER_ID IS '销售员';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.TEAM_ID IS '销售团队';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.TERRITORY_ID IS '销售区域';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.COMMIT_AMOUNT IS '承诺金额';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.UPSIDE_AMOUNT IS '乐观金额';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.ACTUAL_CLOSED_REVENUE IS '实际关闭收入';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.COMMIT_ACCURACY IS '承诺准确率';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.UPSIDE_ACCURACY IS '乐观准确率';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.DEVIATION_AMOUNT IS '偏差绝对值';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.CALCULATED_BY IS '计算人';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.CALCULATED_AT IS '计算时间';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_funnel_stage_metrics IS '漏斗阶段度量';
                
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.FUNNEL_ID IS '所属漏斗';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.STAGE_ID IS '阶段';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.STAGE_ORDER IS '阶段排序';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.STAGE_NAME IS '阶段名称(快照)';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.LEAD_COUNT_IN IS '进入本阶段线索数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.LEAD_COUNT_OUT IS '流出本阶段线索数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.LEAD_COUNT_REMAINING IS '期末仍在本阶段数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.CONVERSION_RATE IS '转化率';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.DROP_OFF_RATE IS '流失率';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.AVG_DAYS_IN_STAGE IS '平均停留天数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.LOST_COUNT IS '本阶段丢失数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.LOST_AMOUNT IS '本阶段丢失金额';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.LOST_REASON_TOP IS 'TOP丢失原因(JSON)';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_crm_lead_score IS '线索评分记录';
                
      COMMENT ON COLUMN erp_crm_lead_score.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_score.LEAD_ID IS '线索/商机';
                    
      COMMENT ON COLUMN erp_crm_lead_score.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_score.CONFIG_ID IS '评分规则版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score.TOTAL_SCORE IS '总分';
                    
      COMMENT ON COLUMN erp_crm_lead_score.SCORE_BREAKDOWN IS '评分快照(JSON)';
                    
      COMMENT ON COLUMN erp_crm_lead_score.AUTO_QUALIFIED IS '是否自动转商机';
                    
      COMMENT ON COLUMN erp_crm_lead_score.TRIGGERED_ACTION IS '触发动作';
                    
      COMMENT ON COLUMN erp_crm_lead_score.CALCULATED_AT IS '计算时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score.TRIGGER_EVENT IS '触发计算事件';
                    
      COMMENT ON COLUMN erp_crm_lead_score.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_score.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_score.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_score.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_forecast_line IS '预测明细';
                
      COMMENT ON COLUMN erp_crm_forecast_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.FORECAST_ID IS '预测数据';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.LEAD_ID IS '商机';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.PROBABILITY IS '概率快照';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.EXPECTED_REVENUE IS '预期收入快照';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.WEIGHTED_REVENUE IS '加权收入';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.FORECAST_CATEGORY IS '预测分类';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.INCLUDED_IN_COMMIT IS '是否计入承诺';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.STAGE_NAME IS '阶段名快照';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_seq_progress IS '线索序列进度';
                
      COMMENT ON COLUMN erp_crm_lead_seq_progress.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.LEAD_ID IS '线索/商机';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.SEQUENCE_ID IS '应用序列';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.CURRENT_STEP_INDEX IS '当前步骤序号';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.STARTED_AT IS '开始时间';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.COMPLETED_AT IS '完成时间';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_score_line IS '评分记录明细';
                
      COMMENT ON COLUMN erp_crm_lead_score_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.SCORE_ID IS '评分记录';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.CONFIG_LINE_ID IS '对应准则行';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.CRITERION_CODE IS '准则编码(快照)';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.CRITERION_NAME IS '准则名称(快照)';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.RAW_VALUE IS '原始值';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.LOOKUP_VALUE IS '匹配档次';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.RAW_SCORE IS '原始得分';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.WEIGHTED_SCORE IS '加权得分';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.SEQUENCE IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.UPDATE_TIME IS '修改时间';
                    
