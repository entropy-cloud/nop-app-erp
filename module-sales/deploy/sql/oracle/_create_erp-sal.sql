
CREATE TABLE erp_md_partner(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARTNER_TYPE INTEGER  ,
  STATUS INTEGER  ,
  CREDIT_LIMIT VARCHAR2(50)  ,
  constraint PK_erp_md_partner primary key (ID)
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

CREATE TABLE erp_md_organization(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_TYPE INTEGER  ,
  PARENT_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_organization primary key (ID)
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

CREATE TABLE erp_prj_project(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  STATUS INTEGER  ,
  constraint PK_erp_prj_project primary key (ID)
);

CREATE TABLE erp_md_bank_account(
  ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20)  ,
  BANK_NAME VARCHAR2(50)  ,
  BANK_ACCOUNT VARCHAR2(50)  ,
  ACCOUNT_TYPE INTEGER  ,
  constraint PK_erp_md_bank_account primary key (ID)
);

CREATE TABLE erp_sal_quotation(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CUSTOMER_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  VALID_FROM DATE  ,
  VALID_TO DATE  ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_TAX_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT_WITH_TAX NUMBER(20,4) default 0   ,
  IS_ACCEPTED CHAR(1) default 0   ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  constraint PK_erp_sal_quotation primary key (ID)
);

CREATE TABLE erp_sal_contract(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CUSTOMER_ID NUMBER(20) NOT NULL ,
  CONTRACT_NAME VARCHAR2(200) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  VALID_FROM DATE  ,
  VALID_TO DATE  ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_TAX_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT_WITH_TAX NUMBER(20,4) default 0   ,
  SIGNED_BY NUMBER(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  constraint PK_erp_sal_contract primary key (ID)
);

CREATE TABLE erp_sal_invoice(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CUSTOMER_ID NUMBER(20) NOT NULL ,
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
  RECEIVED_AMOUNT NUMBER(20,4) default 0   ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  RECEIVED_STATUS VARCHAR2(20)  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_invoice primary key (ID)
);

CREATE TABLE erp_sal_receipt(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CUSTOMER_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  AMOUNT_SOURCE NUMBER(20,4) NOT NULL ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) NOT NULL ,
  TOTAL_AMOUNT NUMBER(20,4) NOT NULL ,
  SETTLEMENT_METHOD_ID NUMBER(20)  ,
  RECEIPT_METHOD VARCHAR2(50)  ,
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
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_receipt primary key (ID)
);

CREATE TABLE erp_sal_quotation_line(
  ID NUMBER(20) NOT NULL ,
  QUOTATION_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UOM_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4)  ,
  UNIT_PRICE NUMBER(20,4) NOT NULL ,
  TAX_RATE NUMBER(10,4)  ,
  TAX_AMOUNT NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  AMOUNT_WITH_TAX NUMBER(20,4)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_quotation_line primary key (ID)
);

CREATE TABLE erp_sal_order(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  QUOTATION_ID NUMBER(20)  ,
  CONTRACT_ID NUMBER(20)  ,
  CUSTOMER_ID NUMBER(20) NOT NULL ,
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
  RECEIVED_AMOUNT NUMBER(20,4) default 0   ,
  SETTLEMENT_METHOD_ID NUMBER(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  RECEIVED_STATUS VARCHAR2(20)  ,
  DELIVERY_STATUS VARCHAR2(20)  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_order primary key (ID)
);

CREATE TABLE erp_sal_receipt_line(
  ID NUMBER(20) NOT NULL ,
  RECEIPT_ID NUMBER(20) NOT NULL ,
  INVOICE_ID NUMBER(20) NOT NULL ,
  AMOUNT NUMBER(20,4) NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_receipt_line primary key (ID)
);

CREATE TABLE erp_sal_order_line(
  ID NUMBER(20) NOT NULL ,
  ORDER_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UOM_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_PRICE NUMBER(20,4) NOT NULL ,
  TAX_RATE NUMBER(10,4)  ,
  TAX_RATE_ID NUMBER(20)  ,
  TAX_AMOUNT NUMBER(20,4) default 0   ,
  AMOUNT NUMBER(20,4) NOT NULL ,
  AMOUNT_WITH_TAX NUMBER(20,4)  ,
  DELIVERED_QUANTITY NUMBER(20,4) default 0   ,
  INVOICED_QUANTITY NUMBER(20,4) default 0   ,
  WAREHOUSE_ID NUMBER(20)  ,
  PROJECT_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_order_line primary key (ID)
);

CREATE TABLE erp_sal_delivery(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ORDER_ID NUMBER(20)  ,
  CUSTOMER_ID NUMBER(20) NOT NULL ,
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
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_delivery primary key (ID)
);

CREATE TABLE erp_sal_delivery_line(
  ID NUMBER(20) NOT NULL ,
  DELIVERY_ID NUMBER(20) NOT NULL ,
  ORDER_LINE_ID NUMBER(20)  ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UOM_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_PRICE NUMBER(20,4)  ,
  TAX_RATE NUMBER(10,4)  ,
  TAX_AMOUNT NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_delivery_line primary key (ID)
);

CREATE TABLE erp_sal_return(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  DELIVERY_ID NUMBER(20)  ,
  CUSTOMER_ID NUMBER(20) NOT NULL ,
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
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_return primary key (ID)
);

CREATE TABLE erp_sal_invoice_line(
  ID NUMBER(20) NOT NULL ,
  INVOICE_ID NUMBER(20) NOT NULL ,
  DELIVERY_LINE_ID NUMBER(20)  ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UOM_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_PRICE NUMBER(20,4)  ,
  TAX_RATE NUMBER(10,4)  ,
  TAX_AMOUNT NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_invoice_line primary key (ID)
);

CREATE TABLE erp_sal_return_line(
  ID NUMBER(20) NOT NULL ,
  RETURN_ID NUMBER(20) NOT NULL ,
  DELIVERY_LINE_ID NUMBER(20)  ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UOM_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_PRICE NUMBER(20,4)  ,
  TAX_RATE NUMBER(10,4)  ,
  TAX_AMOUNT NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  REASON VARCHAR2(500)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_sal_return_line primary key (ID)
);


      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_uom IS '计量单位';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_settlement_method IS '结算方式';
                
      COMMENT ON TABLE erp_md_material_sku IS '物料SKU';
                
      COMMENT ON TABLE erp_md_tax_rate IS '税率';
                
      COMMENT ON TABLE erp_prj_project IS '项目';
                
      COMMENT ON TABLE erp_md_bank_account IS '银行账户';
                
      COMMENT ON TABLE erp_sal_quotation IS '销售报价单';
                
      COMMENT ON COLUMN erp_sal_quotation.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_quotation.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_sal_quotation.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_quotation.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_sal_quotation.BUSINESS_DATE IS '报价日期';
                    
      COMMENT ON COLUMN erp_sal_quotation.VALID_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_sal_quotation.VALID_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_sal_quotation.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_sal_quotation.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_quotation.TOTAL_AMOUNT IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_quotation.TOTAL_AMOUNT_WITH_TAX IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation.IS_ACCEPTED IS '是否已转订单';
                    
      COMMENT ON COLUMN erp_sal_quotation.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_quotation.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_quotation.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_quotation.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_quotation.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_quotation.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_quotation.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_quotation.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_quotation.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_sal_quotation.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_quotation.APPROVED_AT IS '审核时间';
                    
      COMMENT ON TABLE erp_sal_contract IS '销售合同';
                
      COMMENT ON COLUMN erp_sal_contract.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_contract.CODE IS '合同号';
                    
      COMMENT ON COLUMN erp_sal_contract.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_contract.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_sal_contract.CONTRACT_NAME IS '合同名称';
                    
      COMMENT ON COLUMN erp_sal_contract.BUSINESS_DATE IS '签订日期';
                    
      COMMENT ON COLUMN erp_sal_contract.VALID_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_sal_contract.VALID_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_sal_contract.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_sal_contract.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_contract.TOTAL_AMOUNT IS '合同金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_contract.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_contract.TOTAL_AMOUNT_WITH_TAX IS '合同金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_contract.SIGNED_BY IS '签约人(职员)';
                    
      COMMENT ON COLUMN erp_sal_contract.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_contract.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_contract.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_contract.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_contract.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_contract.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_contract.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_contract.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_contract.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_sal_contract.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_contract.APPROVED_AT IS '审核时间';
                    
      COMMENT ON TABLE erp_sal_invoice IS '销售发票';
                
      COMMENT ON COLUMN erp_sal_invoice.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_invoice.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_sal_invoice.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_invoice.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_sal_invoice.INVOICE_NO IS '发票号码';
                    
      COMMENT ON COLUMN erp_sal_invoice.INVOICE_TYPE IS '发票类型';
                    
      COMMENT ON COLUMN erp_sal_invoice.BUSINESS_DATE IS '发票日期';
                    
      COMMENT ON COLUMN erp_sal_invoice.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_sal_invoice.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_invoice.AMOUNT_SOURCE IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_sal_invoice.AMOUNT_FUNCTIONAL IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_sal_invoice.TOTAL_AMOUNT IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_invoice.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_invoice.TOTAL_AMOUNT_WITH_TAX IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_invoice.RECEIVED_AMOUNT IS '已收金额';
                    
      COMMENT ON COLUMN erp_sal_invoice.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_invoice.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_invoice.RECEIVED_STATUS IS '收款进度';
                    
      COMMENT ON COLUMN erp_sal_invoice.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_invoice.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_invoice.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_invoice.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_invoice.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_invoice.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_invoice.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_invoice.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_invoice.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_invoice.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_invoice.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_invoice.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_receipt IS '收款单';
                
      COMMENT ON COLUMN erp_sal_receipt.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_receipt.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_sal_receipt.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_receipt.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_sal_receipt.BUSINESS_DATE IS '收款日期';
                    
      COMMENT ON COLUMN erp_sal_receipt.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_sal_receipt.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_receipt.AMOUNT_SOURCE IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_sal_receipt.AMOUNT_FUNCTIONAL IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_sal_receipt.TOTAL_AMOUNT IS '合计金额';
                    
      COMMENT ON COLUMN erp_sal_receipt.SETTLEMENT_METHOD_ID IS '结算方式';
                    
      COMMENT ON COLUMN erp_sal_receipt.RECEIPT_METHOD IS '收款方式';
                    
      COMMENT ON COLUMN erp_sal_receipt.BANK_ACCOUNT_ID IS '收款账户(资金账户)';
                    
      COMMENT ON COLUMN erp_sal_receipt.PARTNER_BANK_ACCOUNT_ID IS '客户付款账户(银行账户主数据)';
                    
      COMMENT ON COLUMN erp_sal_receipt.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_receipt.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_receipt.WRITTEN_OFF_STATUS IS '核销状态';
                    
      COMMENT ON COLUMN erp_sal_receipt.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_receipt.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_receipt.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_receipt.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_receipt.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_receipt.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_receipt.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_receipt.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_receipt.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_receipt.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_receipt.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_receipt.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_quotation_line IS '销售报价单行';
                
      COMMENT ON COLUMN erp_sal_quotation_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.QUOTATION_ID IS '报价单ID';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.UOM_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.UNIT_PRICE IS '报价单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.AMOUNT IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.AMOUNT_WITH_TAX IS '金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_order IS '销售订单';
                
      COMMENT ON COLUMN erp_sal_order.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_order.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_sal_order.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_order.QUOTATION_ID IS '报价单';
                    
      COMMENT ON COLUMN erp_sal_order.CONTRACT_ID IS '销售合同';
                    
      COMMENT ON COLUMN erp_sal_order.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_sal_order.WAREHOUSE_ID IS '发货仓库';
                    
      COMMENT ON COLUMN erp_sal_order.BUSINESS_DATE IS '订单日期';
                    
      COMMENT ON COLUMN erp_sal_order.DELIVERY_DATE IS '交货日期';
                    
      COMMENT ON COLUMN erp_sal_order.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_sal_order.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_order.AMOUNT_SOURCE IS '合计金额(源币不含税)';
                    
      COMMENT ON COLUMN erp_sal_order.AMOUNT_FUNCTIONAL IS '合计金额(本位币不含税)';
                    
      COMMENT ON COLUMN erp_sal_order.TOTAL_AMOUNT IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_order.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_order.TOTAL_AMOUNT_WITH_TAX IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_order.DISCOUNT_RATE IS '整单折扣率(%)';
                    
      COMMENT ON COLUMN erp_sal_order.DISCOUNT_AMOUNT IS '折扣金额';
                    
      COMMENT ON COLUMN erp_sal_order.RECEIVED_AMOUNT IS '已收金额';
                    
      COMMENT ON COLUMN erp_sal_order.SETTLEMENT_METHOD_ID IS '结算方式';
                    
      COMMENT ON COLUMN erp_sal_order.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_order.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_order.RECEIVED_STATUS IS '收款进度';
                    
      COMMENT ON COLUMN erp_sal_order.DELIVERY_STATUS IS '发货进度';
                    
      COMMENT ON COLUMN erp_sal_order.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_order.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_order.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_order.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_order.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_order.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_order.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_order.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_order.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_order.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_order.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_order.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_receipt_line IS '收款核销行';
                
      COMMENT ON COLUMN erp_sal_receipt_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.RECEIPT_ID IS '收款单ID';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.INVOICE_ID IS '发票ID';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.AMOUNT IS '核销金额';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_order_line IS '销售订单行';
                
      COMMENT ON COLUMN erp_sal_order_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_order_line.ORDER_ID IS '订单ID';
                    
      COMMENT ON COLUMN erp_sal_order_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_sal_order_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_sal_order_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_sal_order_line.UOM_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_order_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_sal_order_line.UNIT_PRICE IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_order_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_order_line.TAX_RATE_ID IS '税率主数据';
                    
      COMMENT ON COLUMN erp_sal_order_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_sal_order_line.AMOUNT IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_order_line.AMOUNT_WITH_TAX IS '金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_order_line.DELIVERED_QUANTITY IS '已出库数量';
                    
      COMMENT ON COLUMN erp_sal_order_line.INVOICED_QUANTITY IS '已开票数量';
                    
      COMMENT ON COLUMN erp_sal_order_line.WAREHOUSE_ID IS '发货仓库';
                    
      COMMENT ON COLUMN erp_sal_order_line.PROJECT_ID IS '项目';
                    
      COMMENT ON COLUMN erp_sal_order_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_order_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_order_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_order_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_order_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_order_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_order_line.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_delivery IS '销售出库单';
                
      COMMENT ON COLUMN erp_sal_delivery.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_delivery.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_sal_delivery.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_delivery.ORDER_ID IS '销售订单';
                    
      COMMENT ON COLUMN erp_sal_delivery.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_sal_delivery.WAREHOUSE_ID IS '出库仓库';
                    
      COMMENT ON COLUMN erp_sal_delivery.BUSINESS_DATE IS '出库日期';
                    
      COMMENT ON COLUMN erp_sal_delivery.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_sal_delivery.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_delivery.AMOUNT_SOURCE IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_sal_delivery.AMOUNT_FUNCTIONAL IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_sal_delivery.TOTAL_AMOUNT IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_delivery.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_delivery.TOTAL_AMOUNT_WITH_TAX IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_delivery.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_delivery.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_delivery.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_delivery.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_delivery.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_delivery.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_delivery.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_delivery.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_delivery.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_delivery.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_delivery.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_delivery.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_delivery.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_delivery.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_delivery_line IS '销售出库单行';
                
      COMMENT ON COLUMN erp_sal_delivery_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.DELIVERY_ID IS '出库单ID';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.ORDER_LINE_ID IS '订单行ID';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.UOM_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.QUANTITY IS '实发数量';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.UNIT_PRICE IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.AMOUNT IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.WAREHOUSE_ID IS '出库库位';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_return IS '销售退货单';
                
      COMMENT ON COLUMN erp_sal_return.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_return.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_sal_return.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_return.DELIVERY_ID IS '出库单';
                    
      COMMENT ON COLUMN erp_sal_return.CUSTOMER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_sal_return.WAREHOUSE_ID IS '入库仓库';
                    
      COMMENT ON COLUMN erp_sal_return.BUSINESS_DATE IS '退货日期';
                    
      COMMENT ON COLUMN erp_sal_return.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_sal_return.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_return.AMOUNT_SOURCE IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_sal_return.AMOUNT_FUNCTIONAL IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_sal_return.TOTAL_AMOUNT IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_return.TOTAL_TAX_AMOUNT IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_return.TOTAL_AMOUNT_WITH_TAX IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_return.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_return.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_return.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_return.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_return.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_return.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_return.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_return.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_return.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_return.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_return.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_return.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_return.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_return.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_invoice_line IS '销售发票行';
                
      COMMENT ON COLUMN erp_sal_invoice_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.INVOICE_ID IS '发票ID';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.DELIVERY_LINE_ID IS '出库行ID';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.UOM_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.UNIT_PRICE IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.AMOUNT IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_sal_return_line IS '销售退货单行';
                
      COMMENT ON COLUMN erp_sal_return_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_return_line.RETURN_ID IS '退货单ID';
                    
      COMMENT ON COLUMN erp_sal_return_line.DELIVERY_LINE_ID IS '出库行ID';
                    
      COMMENT ON COLUMN erp_sal_return_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_sal_return_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_sal_return_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_sal_return_line.UOM_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_return_line.QUANTITY IS '退货数量';
                    
      COMMENT ON COLUMN erp_sal_return_line.UNIT_PRICE IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_return_line.TAX_RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_return_line.TAX_AMOUNT IS '税额';
                    
      COMMENT ON COLUMN erp_sal_return_line.AMOUNT IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_return_line.REASON IS '退货原因';
                    
      COMMENT ON COLUMN erp_sal_return_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_return_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_return_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_return_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_return_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_return_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_return_line.REMARK IS '备注';
                    
