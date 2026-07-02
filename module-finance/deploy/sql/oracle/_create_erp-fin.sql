
CREATE TABLE erp_md_acct_schema(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  CURRENCY_ID NUMBER(20)  ,
  NATURE INTEGER  ,
  constraint PK_erp_md_acct_schema primary key (ID)
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

CREATE TABLE erp_md_subject(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARENT_ID NUMBER(20)  ,
  SUBJECT_CLASS INTEGER  ,
  DIRECTION INTEGER  ,
  constraint PK_erp_md_subject primary key (ID)
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

CREATE TABLE erp_md_warehouse(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  WAREHOUSE_TYPE INTEGER  ,
  ORG_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_warehouse primary key (ID)
);

CREATE TABLE erp_md_material(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  MATERIAL_TYPE INTEGER  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_material primary key (ID)
);

CREATE TABLE erp_prj_project(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  CUSTOMER_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_prj_project primary key (ID)
);

CREATE TABLE erp_md_cost_center(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_ID NUMBER(20)  ,
  constraint PK_erp_md_cost_center primary key (ID)
);

CREATE TABLE erp_md_employee(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_employee primary key (ID)
);

CREATE TABLE erp_ast_asset(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ASSET_TYPE INTEGER  ,
  STATUS INTEGER  ,
  constraint PK_erp_ast_asset primary key (ID)
);

CREATE TABLE erp_fin_voucher_template(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ACCT_SCHEMA_ID NUMBER(20)  ,
  BUSINESS_TYPE INTEGER NOT NULL ,
  VOUCHER_TYPE INTEGER  ,
  TEMPLATE_TYPE VARCHAR2(50)  ,
  IS_ACTIVE CHAR(1) default 1   ,
  VALID_FROM DATE  ,
  VALID_TO DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher_template primary key (ID)
);

CREATE TABLE erp_fin_accounting_period(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  YEAR INTEGER NOT NULL ,
  MONTH INTEGER NOT NULL ,
  START_DATE DATE NOT NULL ,
  END_DATE DATE NOT NULL ,
  QUARTER INTEGER  ,
  IS_ADJUSTMENT CHAR(1) default 0   ,
  STATUS INTEGER NOT NULL ,
  CLOSED_BY VARCHAR2(36)  ,
  CLOSED_AT DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_accounting_period primary key (ID)
);

CREATE TABLE erp_fin_fund_account(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ACCOUNT_TYPE INTEGER NOT NULL ,
  SUBJECT_ID NUMBER(20)  ,
  BANK_NAME VARCHAR2(200)  ,
  BANK_ACCOUNT VARCHAR2(50)  ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  OPENING_BALANCE NUMBER(20,4) default 0   ,
  CURRENT_BALANCE NUMBER(20,4) default 0   ,
  STATUS INTEGER NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_fund_account primary key (ID)
);

CREATE TABLE erp_fin_reconciliation(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20) NOT NULL ,
  ACCT_SCHEMA_ID NUMBER(20) NOT NULL ,
  DIRECTION INTEGER NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  TOTAL_AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  FX_GAIN_LOSS NUMBER(20,4) default 0   ,
  DOC_STATUS INTEGER NOT NULL ,
  POSTED_BY VARCHAR2(36)  ,
  POSTED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_reconciliation primary key (ID)
);

CREATE TABLE erp_fin_employee_advance(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  ADVANCE_TYPE INTEGER NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  SETTLED_AMOUNT NUMBER(20,4) default 0   ,
  OUTSTANDING_AMOUNT NUMBER(20,4) default 0   ,
  PROJECT_ID NUMBER(20)  ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  POSTED CHAR(1) default 0   ,
  POSTED_BY VARCHAR2(36)  ,
  POSTED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_employee_advance primary key (ID)
);

CREATE TABLE erp_fin_voucher_template_line(
  ID NUMBER(20) NOT NULL ,
  TEMPLATE_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  SUBJECT_CODE VARCHAR2(50) NOT NULL ,
  DC_DIRECTION INTEGER NOT NULL ,
  AMOUNT_EXPRESSION VARCHAR2(200)  ,
  ACCOUNT_KEY VARCHAR2(50)  ,
  AMOUNT_KEY VARCHAR2(50)  ,
  MEMO_TEMPLATE VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher_template_line primary key (ID)
);

CREATE TABLE erp_fin_voucher(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  VOUCHER_TYPE INTEGER NOT NULL ,
  POSTING_TYPE INTEGER  ,
  VOUCHER_DATE DATE NOT NULL ,
  VOUCHER_NO VARCHAR2(50)  ,
  ORG_ID NUMBER(20) NOT NULL ,
  ACCT_SCHEMA_ID NUMBER(20) NOT NULL ,
  PERIOD_ID NUMBER(20) NOT NULL ,
  TOTAL_DEBIT NUMBER(20,4) default 0   ,
  TOTAL_CREDIT NUMBER(20,4) default 0   ,
  IS_REVERSED CHAR(1) default 0   ,
  REVERSAL_OF_VOUCHER_ID NUMBER(20)  ,
  DOC_STATUS INTEGER NOT NULL ,
  POSTED_BY VARCHAR2(36)  ,
  POSTED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher primary key (ID)
);

CREATE TABLE erp_fin_accounting_period_status(
  ID NUMBER(20) NOT NULL ,
  PERIOD_ID NUMBER(20) NOT NULL ,
  ACCT_SCHEMA_ID NUMBER(20) NOT NULL ,
  TOTAL_VOUCHERS INTEGER default 0   ,
  POSTED_VOUCHERS INTEGER default 0   ,
  UNPOSTED_VOUCHERS INTEGER default 0   ,
  AR_STATUS INTEGER NOT NULL ,
  AP_STATUS INTEGER NOT NULL ,
  INV_STATUS INTEGER NOT NULL ,
  GL_STATUS INTEGER NOT NULL ,
  ASSET_STATUS INTEGER NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_accounting_period_status primary key (ID)
);

CREATE TABLE erp_fin_ar_ap_item(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20) NOT NULL ,
  ACCT_SCHEMA_ID NUMBER(20) NOT NULL ,
  DIRECTION INTEGER NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  SOURCE_BILL_TYPE VARCHAR2(50) NOT NULL ,
  SOURCE_BILL_CODE VARCHAR2(50) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  DUE_DATE DATE  ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4) NOT NULL ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) NOT NULL ,
  SETTLED_AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  SETTLED_AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  OPEN_AMOUNT_SOURCE NUMBER(20,4) NOT NULL ,
  OPEN_AMOUNT_FUNCTIONAL NUMBER(20,4) NOT NULL ,
  STATUS INTEGER NOT NULL ,
  PERIOD_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_ar_ap_item primary key (ID)
);

CREATE TABLE erp_fin_gl_balance(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20) NOT NULL ,
  ACCT_SCHEMA_ID NUMBER(20) NOT NULL ,
  PERIOD_ID NUMBER(20) NOT NULL ,
  SUBJECT_ID NUMBER(20) NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  OPENING_DEBIT NUMBER(20,4) default 0   ,
  OPENING_CREDIT NUMBER(20,4) default 0   ,
  PERIOD_DEBIT NUMBER(20,4) default 0   ,
  PERIOD_CREDIT NUMBER(20,4) default 0   ,
  CLOSING_DEBIT NUMBER(20,4) default 0   ,
  CLOSING_CREDIT NUMBER(20,4) default 0   ,
  YEAR_OPENING_DEBIT NUMBER(20,4) default 0   ,
  YEAR_OPENING_CREDIT NUMBER(20,4) default 0   ,
  PARTNER_ID NUMBER(20)  ,
  DEPARTMENT_ID NUMBER(20)  ,
  PROJECT_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_gl_balance primary key (ID)
);

CREATE TABLE erp_fin_trial_balance(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20) NOT NULL ,
  ACCT_SCHEMA_ID NUMBER(20) NOT NULL ,
  PERIOD_ID NUMBER(20) NOT NULL ,
  SUBJECT_ID NUMBER(20) NOT NULL ,
  SUBJECT_CODE VARCHAR2(50) NOT NULL ,
  SUBJECT_NAME VARCHAR2(200)  ,
  OPENING_DEBIT NUMBER(20,4) default 0   ,
  OPENING_CREDIT NUMBER(20,4) default 0   ,
  PERIOD_DEBIT NUMBER(20,4) default 0   ,
  PERIOD_CREDIT NUMBER(20,4) default 0   ,
  CLOSING_DEBIT NUMBER(20,4) default 0   ,
  CLOSING_CREDIT NUMBER(20,4) default 0   ,
  GENERATED_AT DATE NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_trial_balance primary key (ID)
);

CREATE TABLE erp_fin_bank_statement(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20) NOT NULL ,
  FUND_ACCOUNT_ID NUMBER(20) NOT NULL ,
  STATEMENT_DATE DATE NOT NULL ,
  BEGINNING_BALANCE NUMBER(20,4) default 0   ,
  ENDING_BALANCE NUMBER(20,4) default 0   ,
  TOTAL_DEBIT NUMBER(20,4) default 0   ,
  TOTAL_CREDIT NUMBER(20,4) default 0   ,
  IMPORT_TIME DATE  ,
  DOC_STATUS INTEGER NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_bank_statement primary key (ID)
);

CREATE TABLE erp_fin_expense_claim(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20) NOT NULL ,
  CLAIMANT_ID NUMBER(20) NOT NULL ,
  DEPARTMENT_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  PAYMENT_MODE INTEGER NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  AMOUNT_WITHOUT_TAX NUMBER(20,4) default 0   ,
  TAX_AMOUNT NUMBER(20,4) default 0   ,
  AMOUNT_WITH_TAX NUMBER(20,4) default 0   ,
  SETTLE_ADVANCE_ID NUMBER(20)  ,
  REASON VARCHAR2(1000)  ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  POSTED CHAR(1) default 0   ,
  POSTED_BY VARCHAR2(36)  ,
  POSTED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_expense_claim primary key (ID)
);

CREATE TABLE erp_fin_voucher_line(
  ID NUMBER(20) NOT NULL ,
  VOUCHER_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  SUBJECT_ID NUMBER(20) NOT NULL ,
  SUBJECT_CODE VARCHAR2(50) NOT NULL ,
  SUBJECT_NAME VARCHAR2(200)  ,
  DC_DIRECTION INTEGER NOT NULL ,
  DEBIT_AMOUNT NUMBER(20,4) default 0   ,
  CREDIT_AMOUNT NUMBER(20,4) default 0   ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4)  ,
  AMOUNT_FUNCTIONAL NUMBER(20,4)  ,
  ACCT_SCHEMA_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  MEMO VARCHAR2(1000)  ,
  PARTNER_ID NUMBER(20)  ,
  DEPARTMENT_ID NUMBER(20)  ,
  PROJECT_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  MATERIAL_ID NUMBER(20)  ,
  BUSINESS_TYPE INTEGER  ,
  COST_CENTER_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher_line primary key (ID)
);

CREATE TABLE erp_fin_voucher_bill_r(
  ID NUMBER(20) NOT NULL ,
  VOUCHER_ID NUMBER(20) NOT NULL ,
  BILL_TYPE VARCHAR2(50) NOT NULL ,
  BILL_CODE VARCHAR2(50) NOT NULL ,
  BILL_LINE_CODE VARCHAR2(50)  ,
  BUSINESS_TYPE INTEGER  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher_bill_r primary key (ID)
);

CREATE TABLE erp_fin_reconciliation_line(
  ID NUMBER(20) NOT NULL ,
  RECONCILIATION_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  PAYMENT_ITEM_ID NUMBER(20) NOT NULL ,
  INVOICE_ITEM_ID NUMBER(20) NOT NULL ,
  SETTLED_AMOUNT_SOURCE NUMBER(20,4) NOT NULL ,
  SETTLED_AMOUNT_FUNCTIONAL NUMBER(20,4) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_reconciliation_line primary key (ID)
);

CREATE TABLE erp_fin_bank_reconciliation(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20) NOT NULL ,
  FUND_ACCOUNT_ID NUMBER(20) NOT NULL ,
  STATEMENT_ID NUMBER(20)  ,
  RECONCILIATION_DATE DATE NOT NULL ,
  BOOK_BALANCE NUMBER(20,4) NOT NULL ,
  STATEMENT_BALANCE NUMBER(20,4) NOT NULL ,
  UNRECONCILED_DIFF NUMBER(20,4) default 0   ,
  IS_BALANCED CHAR(1) default 0   ,
  RECONCILED_AT DATE  ,
  RECONCILED_BY NUMBER(20)  ,
  DOC_STATUS INTEGER NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_bank_reconciliation primary key (ID)
);

CREATE TABLE erp_fin_expense_claim_line(
  ID NUMBER(20) NOT NULL ,
  CLAIM_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  EXPENSE_TYPE INTEGER NOT NULL ,
  PROJECT_ID NUMBER(20)  ,
  COST_CENTER_ID NUMBER(20)  ,
  SUBJECT_ID NUMBER(20)  ,
  SUBJECT_CODE VARCHAR2(50)  ,
  AMOUNT_WITHOUT_TAX NUMBER(20,4) default 0   ,
  TAX_RATE NUMBER(20,8)  ,
  TAX_AMOUNT NUMBER(20,4) default 0   ,
  AMOUNT_WITH_TAX NUMBER(20,4) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_expense_claim_line primary key (ID)
);

CREATE TABLE erp_fin_bank_statement_line(
  ID NUMBER(20) NOT NULL ,
  STATEMENT_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  TRANSACTION_DATE DATE NOT NULL ,
  DESCRIPTION VARCHAR2(1000)  ,
  REF_NO VARCHAR2(50)  ,
  DC_DIRECTION INTEGER NOT NULL ,
  AMOUNT NUMBER(20,4) NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  BALANCE_AFTER NUMBER(20,4)  ,
  MATCH_STATUS INTEGER NOT NULL ,
  MATCHED_LINE_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_bank_statement_line primary key (ID)
);

CREATE TABLE erp_fin_bank_reconciliation_line(
  ID NUMBER(20) NOT NULL ,
  RECONCILIATION_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  ADJUSTMENT_TYPE VARCHAR2(50) NOT NULL ,
  DESCRIPTION VARCHAR2(1000)  ,
  DC_DIRECTION INTEGER NOT NULL ,
  AMOUNT NUMBER(20,4) NOT NULL ,
  SIDE VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_bank_reconciliation_line primary key (ID)
);


      COMMENT ON TABLE erp_md_acct_schema IS '会计核算表(账套)';
                
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_subject IS '会计科目';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_prj_project IS '项目';
                
      COMMENT ON TABLE erp_md_cost_center IS '成本中心';
                
      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON TABLE erp_ast_asset IS '固定资产';
                
      COMMENT ON TABLE erp_fin_voucher_template IS '凭证模板';
                
      COMMENT ON COLUMN erp_fin_voucher_template.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.CODE IS '模板编码';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.NAME IS '模板名称';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.ACCT_SCHEMA_ID IS '账套(空表示所有账套通用)';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.BUSINESS_TYPE IS '业务类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.VOUCHER_TYPE IS '凭证字';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.TEMPLATE_TYPE IS '模板类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.VALID_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.VALID_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_accounting_period IS '会计期间';
                
      COMMENT ON COLUMN erp_fin_accounting_period.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.CODE IS '期间编码';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.NAME IS '期间名称';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.ORG_ID IS '组织';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.YEAR IS '年度';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.MONTH IS '月份';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.START_DATE IS '开始日期';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.END_DATE IS '结束日期';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.QUARTER IS '季度';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.IS_ADJUSTMENT IS '是否调整期';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.STATUS IS '期间状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.CLOSED_BY IS '结账人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.CLOSED_AT IS '结账时间';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_fund_account IS '资金账户';
                
      COMMENT ON COLUMN erp_fin_fund_account.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_fund_account.CODE IS '账户编码';
                    
      COMMENT ON COLUMN erp_fin_fund_account.NAME IS '账户名称';
                    
      COMMENT ON COLUMN erp_fin_fund_account.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_fin_fund_account.ACCOUNT_TYPE IS '账户类型';
                    
      COMMENT ON COLUMN erp_fin_fund_account.SUBJECT_ID IS '对应会计科目';
                    
      COMMENT ON COLUMN erp_fin_fund_account.BANK_NAME IS '开户银行';
                    
      COMMENT ON COLUMN erp_fin_fund_account.BANK_ACCOUNT IS '银行账号';
                    
      COMMENT ON COLUMN erp_fin_fund_account.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_fin_fund_account.OPENING_BALANCE IS '期初余额';
                    
      COMMENT ON COLUMN erp_fin_fund_account.CURRENT_BALANCE IS '当前余额';
                    
      COMMENT ON COLUMN erp_fin_fund_account.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_fin_fund_account.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_fund_account.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_fund_account.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_fund_account.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_fund_account.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_fund_account.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_fund_account.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_reconciliation IS '核销单';
                
      COMMENT ON COLUMN erp_fin_reconciliation.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.ORG_ID IS '组织';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.DIRECTION IS '应收应付';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.PARTNER_ID IS '往来单位';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.TOTAL_AMOUNT_SOURCE IS '核销总额(源币)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.TOTAL_AMOUNT_FUNCTIONAL IS '核销总额(本位币)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.FX_GAIN_LOSS IS '汇兑损益';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.DOC_STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_employee_advance IS '员工借款单';
                
      COMMENT ON COLUMN erp_fin_employee_advance.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.ORG_ID IS '核算组织';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.EMPLOYEE_ID IS '借款人';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.ADVANCE_TYPE IS '借款类型';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.BUSINESS_DATE IS '借款日期';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.SETTLED_AMOUNT IS '已清算金额';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.OUTSTANDING_AMOUNT IS '未还金额';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.PROJECT_ID IS '关联项目';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.POSTED IS '是否已过账';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_employee_advance.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_voucher_template_line IS '凭证模板行';
                
      COMMENT ON COLUMN erp_fin_voucher_template_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.TEMPLATE_ID IS '模板ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.SUBJECT_CODE IS '科目编码(可含占位符)';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.DC_DIRECTION IS '借贷方向';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.AMOUNT_EXPRESSION IS '金额表达式';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.ACCOUNT_KEY IS '科目映射键';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.AMOUNT_KEY IS '金额占位键';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.MEMO_TEMPLATE IS '摘要模板';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_voucher IS '会计凭证';
                
      COMMENT ON COLUMN erp_fin_voucher.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher.CODE IS '凭证号';
                    
      COMMENT ON COLUMN erp_fin_voucher.VOUCHER_TYPE IS '凭证字';
                    
      COMMENT ON COLUMN erp_fin_voucher.POSTING_TYPE IS '过账类型';
                    
      COMMENT ON COLUMN erp_fin_voucher.VOUCHER_DATE IS '凭证日期';
                    
      COMMENT ON COLUMN erp_fin_voucher.VOUCHER_NO IS '凭证编号';
                    
      COMMENT ON COLUMN erp_fin_voucher.ORG_ID IS '核算组织';
                    
      COMMENT ON COLUMN erp_fin_voucher.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_fin_voucher.PERIOD_ID IS '会计期间';
                    
      COMMENT ON COLUMN erp_fin_voucher.TOTAL_DEBIT IS '借方合计(本位币)';
                    
      COMMENT ON COLUMN erp_fin_voucher.TOTAL_CREDIT IS '贷方合计(本位币)';
                    
      COMMENT ON COLUMN erp_fin_voucher.IS_REVERSED IS '是否红字冲销凭证';
                    
      COMMENT ON COLUMN erp_fin_voucher.REVERSAL_OF_VOUCHER_ID IS '原冲销凭证ID';
                    
      COMMENT ON COLUMN erp_fin_voucher.DOC_STATUS IS '凭证状态';
                    
      COMMENT ON COLUMN erp_fin_voucher.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_fin_voucher.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_fin_voucher.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_voucher.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_accounting_period_status IS '期间结账状态';
                
      COMMENT ON COLUMN erp_fin_accounting_period_status.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.PERIOD_ID IS '期间ID';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.TOTAL_VOUCHERS IS '凭证总数';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.POSTED_VOUCHERS IS '已过账凭证数';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.UNPOSTED_VOUCHERS IS '未过账凭证数';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.AR_STATUS IS '应收模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.AP_STATUS IS '应付模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.INV_STATUS IS '存货模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.GL_STATUS IS '总账模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.ASSET_STATUS IS '资产模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_ar_ap_item IS '应收应付明细账';
                
      COMMENT ON COLUMN erp_fin_ar_ap_item.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.ORG_ID IS '组织';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.DIRECTION IS '应收应付';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.PARTNER_ID IS '往来单位';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.SOURCE_BILL_TYPE IS '来源单据类型(AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT)';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.SOURCE_BILL_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.DUE_DATE IS '到期日';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.SETTLED_AMOUNT_SOURCE IS '已核销源币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.SETTLED_AMOUNT_FUNCTIONAL IS '已核销本位币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.OPEN_AMOUNT_SOURCE IS '未核销源币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.OPEN_AMOUNT_FUNCTIONAL IS '未核销本位币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.PERIOD_ID IS '所属期间';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_gl_balance IS '总账余额';
                
      COMMENT ON COLUMN erp_fin_gl_balance.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.ORG_ID IS '组织';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.PERIOD_ID IS '期间';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.SUBJECT_ID IS '科目';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.OPENING_DEBIT IS '期初借方(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.OPENING_CREDIT IS '期初贷方(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.PERIOD_DEBIT IS '本期借方发生(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.PERIOD_CREDIT IS '本期贷方发生(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.CLOSING_DEBIT IS '期末借方(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.CLOSING_CREDIT IS '期末贷方(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.YEAR_OPENING_DEBIT IS '年初借方';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.YEAR_OPENING_CREDIT IS '年初贷方';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.PARTNER_ID IS '辅助-往来单位';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.DEPARTMENT_ID IS '辅助-部门';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.PROJECT_ID IS '辅助-项目';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.WAREHOUSE_ID IS '辅助-仓库';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_trial_balance IS '试算平衡表';
                
      COMMENT ON COLUMN erp_fin_trial_balance.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.ORG_ID IS '组织';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.PERIOD_ID IS '期间';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.SUBJECT_ID IS '科目';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.SUBJECT_CODE IS '科目编码';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.SUBJECT_NAME IS '科目名称';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.OPENING_DEBIT IS '期初借方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.OPENING_CREDIT IS '期初贷方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.PERIOD_DEBIT IS '本期借方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.PERIOD_CREDIT IS '本期贷方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.CLOSING_DEBIT IS '期末借方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.CLOSING_CREDIT IS '期末贷方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.GENERATED_AT IS '生成时间';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_bank_statement IS '银行对账单';
                
      COMMENT ON COLUMN erp_fin_bank_statement.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.ORG_ID IS '组织';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.FUND_ACCOUNT_ID IS '资金账户';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.STATEMENT_DATE IS '对账日期';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.BEGINNING_BALANCE IS '期初余额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.ENDING_BALANCE IS '期末余额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.TOTAL_DEBIT IS '借方发生额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.TOTAL_CREDIT IS '贷方发生额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.IMPORT_TIME IS '导入时间';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.DOC_STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_expense_claim IS '费用报销单';
                
      COMMENT ON COLUMN erp_fin_expense_claim.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.ORG_ID IS '核算组织';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.CLAIMANT_ID IS '报销人';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.DEPARTMENT_ID IS '部门';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.PAYMENT_MODE IS '付款方式';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.AMOUNT_WITHOUT_TAX IS '不含税金额';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.AMOUNT_WITH_TAX IS '价税合计';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.SETTLE_ADVANCE_ID IS '抵扣借款单';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.REASON IS '报销事由';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.POSTED IS '是否已过账';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_expense_claim.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_voucher_line IS '凭证分录行';
                
      COMMENT ON COLUMN erp_fin_voucher_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.VOUCHER_ID IS '凭证ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.SUBJECT_ID IS '科目ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.SUBJECT_CODE IS '科目编码';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.SUBJECT_NAME IS '科目名称';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.DC_DIRECTION IS '借贷方向';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.DEBIT_AMOUNT IS '借方金额(本位币)';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.CREDIT_AMOUNT IS '贷方金额(本位币)';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.ORG_ID IS '核算组织';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.MEMO IS '摘要';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.PARTNER_ID IS '辅助-往来单位';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.DEPARTMENT_ID IS '辅助-部门';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.PROJECT_ID IS '辅助-项目';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.WAREHOUSE_ID IS '辅助-仓库';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.MATERIAL_ID IS '辅助-物料';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.BUSINESS_TYPE IS '业务类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.COST_CENTER_ID IS '成本中心';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_voucher_bill_r IS '业财回链';
                
      COMMENT ON COLUMN erp_fin_voucher_bill_r.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.VOUCHER_ID IS '凭证ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.BILL_TYPE IS '单据类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.BILL_CODE IS '单据编号';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.BILL_LINE_CODE IS '单据行号';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.BUSINESS_TYPE IS '业务类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_reconciliation_line IS '核销单行';
                
      COMMENT ON COLUMN erp_fin_reconciliation_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.RECONCILIATION_ID IS '核销单ID';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.PAYMENT_ITEM_ID IS '付款/收款项(invoice or payment item)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.INVOICE_ITEM_ID IS '被核销发票项';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.SETTLED_AMOUNT_SOURCE IS '核销金额(源币)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.SETTLED_AMOUNT_FUNCTIONAL IS '核销金额(本位币)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_bank_reconciliation IS '银行对账(余额调节表)';
                
      COMMENT ON COLUMN erp_fin_bank_reconciliation.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.ORG_ID IS '组织';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.FUND_ACCOUNT_ID IS '资金账户';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.STATEMENT_ID IS '银行对账单';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.RECONCILIATION_DATE IS '对账日期';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.BOOK_BALANCE IS '账面余额';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.STATEMENT_BALANCE IS '对账单余额';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.UNRECONCILED_DIFF IS '未达差异';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.IS_BALANCED IS '是否平衡';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.RECONCILED_AT IS '对账完成时间';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.RECONCILED_BY IS '对账人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.DOC_STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_expense_claim_line IS '费用报销单行';
                
      COMMENT ON COLUMN erp_fin_expense_claim_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.CLAIM_ID IS '报销单ID';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.EXPENSE_TYPE IS '费用类型';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.COST_CENTER_ID IS '成本中心';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.SUBJECT_ID IS '费用科目';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.SUBJECT_CODE IS '科目编码';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.AMOUNT_WITHOUT_TAX IS '不含税金额';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.TAX_RATE IS '税率';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.AMOUNT_WITH_TAX IS '价税合计';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_expense_claim_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_bank_statement_line IS '银行对账单行';
                
      COMMENT ON COLUMN erp_fin_bank_statement_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.STATEMENT_ID IS '对账单ID';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.TRANSACTION_DATE IS '交易日期';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.DESCRIPTION IS '摘要(银行)';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.REF_NO IS '银行参考号';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.DC_DIRECTION IS '借贷方向';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.BALANCE_AFTER IS '交易后余额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.MATCH_STATUS IS '匹配状态';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.MATCHED_LINE_ID IS '匹配凭证行ID';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_bank_reconciliation_line IS '银行对账调整行';
                
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.RECONCILIATION_ID IS '对账记录ID';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.ADJUSTMENT_TYPE IS '调整类型';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.DESCRIPTION IS '说明';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.DC_DIRECTION IS '借贷方向';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.SIDE IS '调整方';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.UPDATE_TIME IS '修改时间';
                    
