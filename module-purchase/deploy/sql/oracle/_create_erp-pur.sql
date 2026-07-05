
CREATE TABLE erp_md_organization(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_TYPE INTEGER  ,
  PARENT_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_organization primary key (ID)
);

CREATE TABLE erp_md_employee(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_employee primary key (ID)
);

CREATE TABLE erp_md_material(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  MATERIAL_TYPE INTEGER  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_material primary key (ID)
);

CREATE TABLE erp_md_uom(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  UOM_GROUP VARCHAR2(50)  ,
  IS_BASE CHAR(1)  ,
  constraint PK_erp_md_uom primary key (ID)
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

CREATE TABLE erp_prj_project(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  STATUS INTEGER  ,
  constraint PK_erp_prj_project primary key (ID)
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

CREATE TABLE erp_md_warehouse(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  WAREHOUSE_TYPE INTEGER  ,
  ORG_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_warehouse primary key (ID)
);

CREATE TABLE erp_md_settlement_method(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  SETTLEMENT_TYPE INTEGER  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_settlement_method primary key (ID)
);

CREATE TABLE erp_md_material_sku(
  ID NUMBER(20)  ,
  MATERIAL_ID NUMBER(20)  ,
  SKU_CODE VARCHAR2(50)  ,
  BARCODE VARCHAR2(50)  ,
  constraint PK_erp_md_material_sku primary key (ID)
);

CREATE TABLE erp_md_tax_rate(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  TAX_TYPE INTEGER  ,
  RATE VARCHAR2(50)  ,
  constraint PK_erp_md_tax_rate primary key (ID)
);

CREATE TABLE erp_md_bank_account(
  ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20)  ,
  BANK_NAME VARCHAR2(50)  ,
  BANK_ACCOUNT VARCHAR2(50)  ,
  ACCOUNT_TYPE INTEGER  ,
  constraint PK_erp_md_bank_account primary key (ID)
);

CREATE TABLE erp_pur_requisition(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REQUESTER_ID NUMBER(20) NOT NULL ,
  DEPARTMENT_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  REQUIRED_DATE DATE  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_requisition primary key (ID)
);

CREATE TABLE erp_pur_supplier_scorecard(
  ID NUMBER(20) NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PERIOD_FROM DATE NOT NULL ,
  PERIOD_TO DATE NOT NULL ,
  TOTAL_SCORE NUMBER(18,2)  ,
  STANDING VARCHAR2(20)  ,
  WARN_THRESHOLD NUMBER(18,2)  ,
  HOLD_THRESHOLD NUMBER(18,2)  ,
  PREVENT_THRESHOLD NUMBER(18,2)  ,
  STATUS VARCHAR2(20) default '10'  NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_supplier_scorecard primary key (ID)
);

CREATE TABLE erp_pur_supplier_price_list(
  ID NUMBER(20) NOT NULL ,
  SUPPLIER_ID NUMBER(20) NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  UNIT_PRICE NUMBER(20,4) NOT NULL ,
  TAX_RATE NUMBER(10,4)  ,
  MIN_ORDER_QUANTITY NUMBER(20,4)  ,
  LEAD_TIME_DAYS INTEGER  ,
  VALID_FROM DATE  ,
  VALID_TO DATE  ,
  PRIORITY INTEGER default 100   ,
  IS_ACTIVE CHAR(1) default 1   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_supplier_price_list primary key (ID)
);

CREATE TABLE erp_pur_invoice(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SUPPLIER_ID NUMBER(20) NOT NULL ,
  INVOICE_NO VARCHAR2(50)  ,
  INVOICE_TYPE VARCHAR2(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_TAX_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT_WITH_TAX NUMBER(20,4) default 0   ,
  PAID_AMOUNT NUMBER(20,4) default 0   ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  PAID_STATUS VARCHAR2(20)  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_invoice primary key (ID)
);

CREATE TABLE erp_pur_payment(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SUPPLIER_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4) NOT NULL ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) NOT NULL ,
  TOTAL_AMOUNT NUMBER(20,4) NOT NULL ,
  SETTLEMENT_METHOD_ID NUMBER(20)  ,
  PAYMENT_METHOD VARCHAR2(50)  ,
  BANK_ACCOUNT_ID NUMBER(20)  ,
  PARTNER_BANK_ACCOUNT_ID NUMBER(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  WRITTEN_OFF_STATUS VARCHAR2(20)  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_payment primary key (ID)
);

CREATE TABLE erp_pur_requisition_line(
  ID NUMBER(20) NOT NULL ,
  REQUISITION_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  REQUIRED_DATE DATE  ,
  SUGGESTED_SUPPLIER_ID NUMBER(20)  ,
  PROJECT_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_requisition_line primary key (ID)
);

CREATE TABLE erp_pur_rfq(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REQUISITION_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  VALID_UNTIL DATE  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  constraint PK_erp_pur_rfq primary key (ID)
);

CREATE TABLE erp_pur_supplier_scorecard_criteria(
  ID NUMBER(20) NOT NULL ,
  SCORECARD_ID NUMBER(20) NOT NULL ,
  CRITERIA_NAME VARCHAR2(100) NOT NULL ,
  WEIGHT NUMBER(5,2) NOT NULL ,
  FORMULA VARCHAR2(1000) NOT NULL ,
  SCORE NUMBER(18,2)  ,
  WEIGHTED_SCORE NUMBER(18,2)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_supplier_scorecard_criteria primary key (ID)
);

CREATE TABLE erp_pur_payment_line(
  ID NUMBER(20) NOT NULL ,
  PAYMENT_ID NUMBER(20) NOT NULL ,
  INVOICE_ID NUMBER(20) NOT NULL ,
  AMOUNT NUMBER(20,4) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_payment_line primary key (ID)
);

CREATE TABLE erp_pur_rfq_line(
  ID NUMBER(20) NOT NULL ,
  RFQ_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_rfq_line primary key (ID)
);

CREATE TABLE erp_pur_quotation(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  RFQ_ID NUMBER(20)  ,
  SUPPLIER_ID NUMBER(20) NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  BUSINESS_DATE DATE NOT NULL ,
  VALID_FROM DATE  ,
  VALID_TO DATE  ,
  IS_ACCEPTED CHAR(1) default 0   ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  constraint PK_erp_pur_quotation primary key (ID)
);

CREATE TABLE erp_pur_supplier_scorecard_variable(
  ID NUMBER(20) NOT NULL ,
  CRITERIA_ID NUMBER(20) NOT NULL ,
  VARIABLE_NAME VARCHAR2(100) NOT NULL ,
  PATH VARCHAR2(200) NOT NULL ,
  VALUE NUMBER(20,6)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_supplier_scorecard_variable primary key (ID)
);

CREATE TABLE erp_pur_quotation_line(
  ID NUMBER(20) NOT NULL ,
  QUOTATION_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4)  ,
  UNIT_PRICE NUMBER(20,4) NOT NULL ,
  TAX_RATE NUMBER(10,4)  ,
  LEAD_TIME_DAYS INTEGER  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_quotation_line primary key (ID)
);

CREATE TABLE erp_pur_order(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REQUISITION_ID NUMBER(20)  ,
  QUOTATION_ID NUMBER(20)  ,
  SUPPLIER_ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  DELIVERY_DATE DATE  ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_TAX_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT_WITH_TAX NUMBER(20,4) default 0   ,
  DISCOUNT_RATE NUMBER(10,4)  ,
  DISCOUNT_AMOUNT NUMBER(20,4) default 0   ,
  PAID_AMOUNT NUMBER(20,4) default 0   ,
  SETTLEMENT_METHOD_ID NUMBER(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  PAID_STATUS VARCHAR2(20)  ,
  RECEIVE_STATUS VARCHAR2(20)  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_order primary key (ID)
);

CREATE TABLE erp_pur_order_line(
  ID NUMBER(20) NOT NULL ,
  ORDER_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_PRICE NUMBER(20,4) NOT NULL ,
  TAX_RATE NUMBER(10,4)  ,
  TAX_RATE_ID NUMBER(20)  ,
  TAX_AMOUNT NUMBER(20,4) default 0   ,
  AMOUNT NUMBER(20,4) NOT NULL ,
  AMOUNT_WITH_TAX NUMBER(20,4)  ,
  RECEIVED_QUANTITY NUMBER(20,4) default 0   ,
  INVOICED_QUANTITY NUMBER(20,4) default 0   ,
  WAREHOUSE_ID NUMBER(20)  ,
  PROJECT_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_order_line primary key (ID)
);

CREATE TABLE erp_pur_receive(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ORDER_ID NUMBER(20)  ,
  SUPPLIER_ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_TAX_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT_WITH_TAX NUMBER(20,4) default 0   ,
  RECEIVE_STATUS VARCHAR2(20)  ,
  RECEIVE_TYPE VARCHAR2(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_receive primary key (ID)
);

CREATE TABLE erp_pur_receive_line(
  ID NUMBER(20) NOT NULL ,
  RECEIVE_ID NUMBER(20) NOT NULL ,
  ORDER_LINE_ID NUMBER(20)  ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  REJECTED_QUANTITY NUMBER(20,4) default 0   ,
  UNIT_PRICE NUMBER(20,4)  ,
  TAX_RATE NUMBER(10,4)  ,
  TAX_AMOUNT NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_receive_line primary key (ID)
);

CREATE TABLE erp_pur_return(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  RECEIVE_ID NUMBER(20)  ,
  SUPPLIER_ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_TAX_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT_WITH_TAX NUMBER(20,4) default 0   ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_return primary key (ID)
);

CREATE TABLE erp_pur_invoice_line(
  ID NUMBER(20) NOT NULL ,
  INVOICE_ID NUMBER(20) NOT NULL ,
  RECEIVE_LINE_ID NUMBER(20)  ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_PRICE NUMBER(20,4)  ,
  TAX_RATE NUMBER(10,4)  ,
  TAX_AMOUNT NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_invoice_line primary key (ID)
);

CREATE TABLE erp_pur_return_line(
  ID NUMBER(20) NOT NULL ,
  RETURN_ID NUMBER(20) NOT NULL ,
  RECEIVE_LINE_ID NUMBER(20)  ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_PRICE NUMBER(20,4)  ,
  TAX_RATE NUMBER(10,4)  ,
  TAX_AMOUNT NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  REASON VARCHAR2(500)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_return_line primary key (ID)
);


      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_employee IS '员工';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_uom IS '计量单位';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_prj_project IS '项目';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_settlement_method IS '结算方式';
                
      COMMENT ON TABLE erp_md_material_sku IS '物料SKU';
                
      COMMENT ON TABLE erp_md_tax_rate IS '税率';
                
      COMMENT ON TABLE erp_md_bank_account IS '银行账户';
                
      COMMENT ON TABLE erp_pur_requisition IS '采购请购单';
                
      COMMENT ON COLUMN erp_pur_requisition.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_requisition.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_pur_requisition.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_requisition.REQUESTER_ID IS '请购人(职员)';
                    
      COMMENT ON COLUMN erp_pur_requisition.DEPARTMENT_ID IS '请购部门';
                    
      COMMENT ON COLUMN erp_pur_requisition.BUSINESS_DATE IS '请购日期';
                    
      COMMENT ON COLUMN erp_pur_requisition.REQUIRED_DATE IS '需求日期';
                    
      COMMENT ON COLUMN erp_pur_requisition.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_requisition.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_requisition.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_requisition.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_requisition.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_requisition.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_requisition.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_requisition.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_requisition.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_requisition.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_requisition.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_supplier_scorecard IS '供应商评分卡';
                
      COMMENT ON COLUMN erp_pur_supplier_scorecard.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.PARTNER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.PERIOD_FROM IS '评分周期起';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.PERIOD_TO IS '评分周期止';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.TOTAL_SCORE IS '总分(派生)';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.STANDING IS '评级';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.WARN_THRESHOLD IS 'warn阈值';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.HOLD_THRESHOLD IS 'hold阈值';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.PREVENT_THRESHOLD IS 'prevent阈值';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.STATUS IS '周期状态';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_supplier_price_list IS '供应商价格清单';
                
      COMMENT ON COLUMN erp_pur_supplier_price_list.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.UNIT_PRICE IS '协议单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.MIN_ORDER_QUANTITY IS '最小起订量';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.LEAD_TIME_DAYS IS '交货周期(天)';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.VALID_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.VALID_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.PRIORITY IS '优先级(数字小优先)';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_invoice IS '采购发票';
                
      COMMENT ON COLUMN erp_pur_invoice.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_invoice.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_pur_invoice.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_invoice.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_invoice.INVOICE_NO IS '发票号码';
                    
      COMMENT ON COLUMN erp_pur_invoice.INVOICE_TYPE IS '发票类型';
                    
      COMMENT ON COLUMN erp_pur_invoice.BUSINESS_DATE IS '发票日期';
                    
      COMMENT ON COLUMN erp_pur_invoice.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_pur_invoice.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_invoice.AMOUNT_SOURCE IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_pur_invoice.AMOUNT_FUNCTIONAL IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_pur_invoice.TOTAL_AMOUNT IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_invoice.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_pur_invoice.TOTAL_AMOUNT_WITH_TAX IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_invoice.PAID_AMOUNT IS '已付金额';
                    
      COMMENT ON COLUMN erp_pur_invoice.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_invoice.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_invoice.PAID_STATUS IS '付款进度';
                    
      COMMENT ON COLUMN erp_pur_invoice.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_invoice.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_invoice.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_invoice.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_invoice.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_invoice.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_invoice.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_invoice.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_invoice.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_invoice.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_invoice.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_invoice.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_payment IS '付款单';
                
      COMMENT ON COLUMN erp_pur_payment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_payment.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_pur_payment.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_payment.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_payment.BUSINESS_DATE IS '付款日期';
                    
      COMMENT ON COLUMN erp_pur_payment.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_pur_payment.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_payment.AMOUNT_SOURCE IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_pur_payment.AMOUNT_FUNCTIONAL IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_pur_payment.TOTAL_AMOUNT IS '合计金额';
                    
      COMMENT ON COLUMN erp_pur_payment.SETTLEMENT_METHOD_ID IS '结算方式';
                    
      COMMENT ON COLUMN erp_pur_payment.PAYMENT_METHOD IS '付款方式';
                    
      COMMENT ON COLUMN erp_pur_payment.BANK_ACCOUNT_ID IS '付款账户(资金账户)';
                    
      COMMENT ON COLUMN erp_pur_payment.PARTNER_BANK_ACCOUNT_ID IS '供应商收款账户(银行账户主数据)';
                    
      COMMENT ON COLUMN erp_pur_payment.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_payment.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_payment.WRITTEN_OFF_STATUS IS '核销状态';
                    
      COMMENT ON COLUMN erp_pur_payment.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_payment.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_payment.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_payment.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_payment.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_payment.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_payment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_payment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_payment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_payment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_payment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_payment.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_requisition_line IS '采购请购单行';
                
      COMMENT ON COLUMN erp_pur_requisition_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.REQUISITION_ID IS '请购单ID';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.QUANTITY IS '请购数量';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.REQUIRED_DATE IS '需求日期';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.SUGGESTED_SUPPLIER_ID IS '建议供应商';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_rfq IS '采购询价单';
                
      COMMENT ON COLUMN erp_pur_rfq.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_rfq.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_pur_rfq.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_rfq.REQUISITION_ID IS '请购单';
                    
      COMMENT ON COLUMN erp_pur_rfq.BUSINESS_DATE IS '询价日期';
                    
      COMMENT ON COLUMN erp_pur_rfq.VALID_UNTIL IS '报价有效期至';
                    
      COMMENT ON COLUMN erp_pur_rfq.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_rfq.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_rfq.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_rfq.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_rfq.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_rfq.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_rfq.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_rfq.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_rfq.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_pur_rfq.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_rfq.APPROVED_AT IS '审核时间';
                    
      COMMENT ON TABLE erp_pur_supplier_scorecard_criteria IS '评分维度';
                
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.SCORECARD_ID IS '评分卡';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.CRITERIA_NAME IS '维度名';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.WEIGHT IS '权重(0-100)';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.FORMULA IS '公式(XLang表达式)';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.SCORE IS '维度得分';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.WEIGHTED_SCORE IS '加权得分';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_criteria.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_payment_line IS '付款核销行';
                
      COMMENT ON COLUMN erp_pur_payment_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_payment_line.PAYMENT_ID IS '付款单ID';
                    
      COMMENT ON COLUMN erp_pur_payment_line.INVOICE_ID IS '发票ID';
                    
      COMMENT ON COLUMN erp_pur_payment_line.AMOUNT IS '核销金额';
                    
      COMMENT ON COLUMN erp_pur_payment_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_payment_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_payment_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_payment_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_payment_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_payment_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_payment_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_rfq_line IS '采购询价单行';
                
      COMMENT ON COLUMN erp_pur_rfq_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.RFQ_ID IS '询价单ID';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.QUANTITY IS '询价数量';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_quotation IS '供应商报价单';
                
      COMMENT ON COLUMN erp_pur_quotation.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_quotation.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_pur_quotation.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_quotation.RFQ_ID IS '询价单';
                    
      COMMENT ON COLUMN erp_pur_quotation.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_quotation.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_pur_quotation.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_quotation.BUSINESS_DATE IS '报价日期';
                    
      COMMENT ON COLUMN erp_pur_quotation.VALID_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_pur_quotation.VALID_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_pur_quotation.IS_ACCEPTED IS '是否中标';
                    
      COMMENT ON COLUMN erp_pur_quotation.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_quotation.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_quotation.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_quotation.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_quotation.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_quotation.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_quotation.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_quotation.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_quotation.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_pur_quotation.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_quotation.APPROVED_AT IS '审核时间';
                    
      COMMENT ON TABLE erp_pur_supplier_scorecard_variable IS '评分变量';
                
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.CRITERIA_ID IS '评分维度';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.VARIABLE_NAME IS '变量名';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.PATH IS '业务取值路径';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.VALUE IS '取值';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_supplier_scorecard_variable.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_quotation_line IS '供应商报价单行';
                
      COMMENT ON COLUMN erp_pur_quotation_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.QUOTATION_ID IS '报价单ID';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.UNIT_PRICE IS '报价单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.LEAD_TIME_DAYS IS '交货周期(天)';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_order IS '采购订单';
                
      COMMENT ON COLUMN erp_pur_order.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_order.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_pur_order.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_order.REQUISITION_ID IS '请购单';
                    
      COMMENT ON COLUMN erp_pur_order.QUOTATION_ID IS '报价单';
                    
      COMMENT ON COLUMN erp_pur_order.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_order.WAREHOUSE_ID IS '收货仓库';
                    
      COMMENT ON COLUMN erp_pur_order.BUSINESS_DATE IS '订单日期';
                    
      COMMENT ON COLUMN erp_pur_order.DELIVERY_DATE IS '交货日期';
                    
      COMMENT ON COLUMN erp_pur_order.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_pur_order.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_order.AMOUNT_SOURCE IS '合计金额(源币不含税)';
                    
      COMMENT ON COLUMN erp_pur_order.AMOUNT_FUNCTIONAL IS '合计金额(本位币不含税)';
                    
      COMMENT ON COLUMN erp_pur_order.TOTAL_AMOUNT IS '合计金额(本位币不含税)';
                    
      COMMENT ON COLUMN erp_pur_order.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_pur_order.TOTAL_AMOUNT_WITH_TAX IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_order.DISCOUNT_RATE IS '整单折扣率(%)';
                    
      COMMENT ON COLUMN erp_pur_order.DISCOUNT_AMOUNT IS '折扣金额';
                    
      COMMENT ON COLUMN erp_pur_order.PAID_AMOUNT IS '已付金额';
                    
      COMMENT ON COLUMN erp_pur_order.SETTLEMENT_METHOD_ID IS '结算方式';
                    
      COMMENT ON COLUMN erp_pur_order.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_order.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_order.PAID_STATUS IS '付款进度';
                    
      COMMENT ON COLUMN erp_pur_order.RECEIVE_STATUS IS '收货状态';
                    
      COMMENT ON COLUMN erp_pur_order.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_order.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_order.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_order.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_order.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_order.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_order.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_order.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_order.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_order.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_order.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_order.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_order_line IS '采购订单行';
                
      COMMENT ON COLUMN erp_pur_order_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_order_line.ORDER_ID IS '订单ID';
                    
      COMMENT ON COLUMN erp_pur_order_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_pur_order_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_pur_order_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_pur_order_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_order_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_pur_order_line.UNIT_PRICE IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_order_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_order_line.TAX_RATE_ID IS '税率主数据';
                    
      COMMENT ON COLUMN erp_pur_order_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_pur_order_line.AMOUNT IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_order_line.AMOUNT_WITH_TAX IS '金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_order_line.RECEIVED_QUANTITY IS '已收货数量';
                    
      COMMENT ON COLUMN erp_pur_order_line.INVOICED_QUANTITY IS '已开票数量';
                    
      COMMENT ON COLUMN erp_pur_order_line.WAREHOUSE_ID IS '收货仓库';
                    
      COMMENT ON COLUMN erp_pur_order_line.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_pur_order_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_order_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_order_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_order_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_order_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_order_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_order_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_receive IS '采购入库单';
                
      COMMENT ON COLUMN erp_pur_receive.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_receive.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_pur_receive.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_receive.ORDER_ID IS '采购订单';
                    
      COMMENT ON COLUMN erp_pur_receive.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_receive.WAREHOUSE_ID IS '入库仓库';
                    
      COMMENT ON COLUMN erp_pur_receive.BUSINESS_DATE IS '入库日期';
                    
      COMMENT ON COLUMN erp_pur_receive.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_pur_receive.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_receive.AMOUNT_SOURCE IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_pur_receive.AMOUNT_FUNCTIONAL IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_pur_receive.TOTAL_AMOUNT IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_receive.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_pur_receive.TOTAL_AMOUNT_WITH_TAX IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_receive.RECEIVE_STATUS IS '收货状态';
                    
      COMMENT ON COLUMN erp_pur_receive.RECEIVE_TYPE IS '入库类型';
                    
      COMMENT ON COLUMN erp_pur_receive.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_receive.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_receive.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_receive.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_receive.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_receive.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_receive.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_receive.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_receive.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_receive.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_receive.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_receive.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_receive.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_receive.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_receive_line IS '采购入库单行';
                
      COMMENT ON COLUMN erp_pur_receive_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_receive_line.RECEIVE_ID IS '入库单ID';
                    
      COMMENT ON COLUMN erp_pur_receive_line.ORDER_LINE_ID IS '订单行ID';
                    
      COMMENT ON COLUMN erp_pur_receive_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_pur_receive_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_pur_receive_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_pur_receive_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_receive_line.QUANTITY IS '实收数量';
                    
      COMMENT ON COLUMN erp_pur_receive_line.REJECTED_QUANTITY IS '拒收数量';
                    
      COMMENT ON COLUMN erp_pur_receive_line.UNIT_PRICE IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_receive_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_receive_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_pur_receive_line.AMOUNT IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_receive_line.WAREHOUSE_ID IS '入库库位';
                    
      COMMENT ON COLUMN erp_pur_receive_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_pur_receive_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_receive_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_receive_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_receive_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_receive_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_receive_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_receive_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_return IS '采购退货单';
                
      COMMENT ON COLUMN erp_pur_return.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_return.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_pur_return.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_return.RECEIVE_ID IS '入库单';
                    
      COMMENT ON COLUMN erp_pur_return.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_return.WAREHOUSE_ID IS '出库仓库';
                    
      COMMENT ON COLUMN erp_pur_return.BUSINESS_DATE IS '退货日期';
                    
      COMMENT ON COLUMN erp_pur_return.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_pur_return.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_return.AMOUNT_SOURCE IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_pur_return.AMOUNT_FUNCTIONAL IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_pur_return.TOTAL_AMOUNT IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_return.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_pur_return.TOTAL_AMOUNT_WITH_TAX IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_return.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_return.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_return.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_return.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_return.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_return.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_return.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_return.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_return.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_return.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_return.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_return.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_return.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_return.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_invoice_line IS '采购发票行';
                
      COMMENT ON COLUMN erp_pur_invoice_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.INVOICE_ID IS '发票ID';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.RECEIVE_LINE_ID IS '入库行ID';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.UNIT_PRICE IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.AMOUNT IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_return_line IS '采购退货单行';
                
      COMMENT ON COLUMN erp_pur_return_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_return_line.RETURN_ID IS '退货单ID';
                    
      COMMENT ON COLUMN erp_pur_return_line.RECEIVE_LINE_ID IS '入库行ID';
                    
      COMMENT ON COLUMN erp_pur_return_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_pur_return_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_pur_return_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_pur_return_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_return_line.QUANTITY IS '退货数量';
                    
      COMMENT ON COLUMN erp_pur_return_line.UNIT_PRICE IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_return_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_return_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_pur_return_line.AMOUNT IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_return_line.REASON IS '退货原因';
                    
      COMMENT ON COLUMN erp_pur_return_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_pur_return_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_return_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_return_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_return_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_return_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_return_line.UPDATE_TIME IS '修改时间';
                    
