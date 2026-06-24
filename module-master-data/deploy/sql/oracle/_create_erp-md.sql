
CREATE TABLE erp_md_material_category(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  PARENT_ID NUMBER(20)  ,
  SORT_NUM INTEGER  ,
  PRICE_VALIDATION_LEVEL INTEGER default 20   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_material_category primary key (ID)
);

CREATE TABLE erp_md_uom(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(20) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  UOM_GROUP VARCHAR2(50)  ,
  IS_BASE CHAR(1) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_uom primary key (ID)
);

CREATE TABLE erp_md_tax_rate(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  TAX_TYPE INTEGER NOT NULL ,
  RATE NUMBER(10,6) NOT NULL ,
  IS_ZERO_RATED CHAR(1) default 0   ,
  IS_EXEMPT CHAR(1) default 0   ,
  VALID_FROM DATE  ,
  VALID_TO DATE  ,
  STATUS INTEGER NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_tax_rate primary key (ID)
);

CREATE TABLE erp_md_partner(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  PARTNER_TYPE INTEGER NOT NULL ,
  STATUS INTEGER NOT NULL ,
  CONTACT_PERSON VARCHAR2(100)  ,
  PHONE VARCHAR2(50)  ,
  EMAIL VARCHAR2(200)  ,
  ADDRESS VARCHAR2(500)  ,
  TAX_NO VARCHAR2(50)  ,
  CREDIT_LIMIT NUMBER(20,4)  ,
  CREDIT_PERIOD_DAYS INTEGER  ,
  RECEIVABLE_BALANCE NUMBER(20,4) default 0   ,
  PAYABLE_BALANCE NUMBER(20,4) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_md_partner primary key (ID)
);

CREATE TABLE erp_md_currency(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(10) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  SYMBOL VARCHAR2(10)  ,
  DECIMAL_PLACES INTEGER default 2   ,
  IS_FUNCTIONAL CHAR(1) default 0   ,
  IS_ACTIVE CHAR(1) default 1   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_currency primary key (ID)
);

CREATE TABLE erp_md_settlement_method(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  SETTLEMENT_TYPE INTEGER NOT NULL ,
  DEFAULT_FUND_ACCOUNT_ID NUMBER(20)  ,
  DEFAULT_DAYS INTEGER  ,
  STATUS INTEGER NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_settlement_method primary key (ID)
);

CREATE TABLE erp_md_partner_address(
  ID NUMBER(20) NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  ADDRESS_TYPE INTEGER NOT NULL ,
  CONTACT_PERSON VARCHAR2(100)  ,
  PHONE VARCHAR2(50)  ,
  ADDRESS VARCHAR2(500) NOT NULL ,
  IS_DEFAULT CHAR(1) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_partner_address primary key (ID)
);

CREATE TABLE erp_md_partner_contact(
  ID NUMBER(20) NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  CONTACT_PERSON VARCHAR2(100) NOT NULL ,
  POSITION VARCHAR2(100)  ,
  PHONE VARCHAR2(50)  ,
  EMAIL VARCHAR2(200)  ,
  IS_DEFAULT CHAR(1) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_md_partner_contact primary key (ID)
);

CREATE TABLE erp_md_bank_account(
  ID NUMBER(20) NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  BANK_NAME VARCHAR2(200) NOT NULL ,
  BANK_BRANCH VARCHAR2(200)  ,
  BANK_ACCOUNT VARCHAR2(50) NOT NULL ,
  ACCOUNT_TYPE INTEGER  ,
  ACCOUNT_HOLDER VARCHAR2(200)  ,
  IS_DEFAULT CHAR(1) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_bank_account primary key (ID)
);

CREATE TABLE erp_md_exchange_rate(
  ID NUMBER(20) NOT NULL ,
  FROM_CURRENCY_ID NUMBER(20) NOT NULL ,
  TO_CURRENCY_ID NUMBER(20) NOT NULL ,
  RATE_TYPE VARCHAR2(30) default 'SPOT'   ,
  RATE NUMBER(20,8) NOT NULL ,
  VALID_FROM DATE NOT NULL ,
  VALID_TO DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_exchange_rate primary key (ID)
);

CREATE TABLE erp_md_subject(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  PARENT_ID NUMBER(20)  ,
  SUBJECT_CLASS INTEGER NOT NULL ,
  DIRECTION INTEGER NOT NULL ,
  BALANCE_TYPE INTEGER  ,
  CURRENCY_ID NUMBER(20)  ,
  IS_AUXILIARY_PARTNER CHAR(1) default 0   ,
  IS_AUXILIARY_DEPARTMENT CHAR(1) default 0   ,
  IS_AUXILIARY_PROJECT CHAR(1) default 0   ,
  IS_AUXILIARY_WAREHOUSE CHAR(1) default 0   ,
  IS_AUXILIARY_PRODUCT CHAR(1) default 0   ,
  IS_LEAF CHAR(1) default 1   ,
  STATUS INTEGER NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_md_subject primary key (ID)
);

CREATE TABLE erp_md_organization(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  PARENT_ID NUMBER(20)  ,
  ORG_TYPE INTEGER NOT NULL ,
  FUNCTIONAL_CURRENCY_ID NUMBER(20)  ,
  STATUS INTEGER NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_organization primary key (ID)
);

CREATE TABLE erp_md_employee(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  POSITION VARCHAR2(100)  ,
  PHONE VARCHAR2(50)  ,
  EMAIL VARCHAR2(200)  ,
  PARTNER_ID NUMBER(20)  ,
  STATUS INTEGER NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_employee primary key (ID)
);

CREATE TABLE erp_md_acct_schema(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20) NOT NULL ,
  NATURE INTEGER NOT NULL ,
  FUNCTIONAL_CURRENCY_ID NUMBER(20) NOT NULL ,
  COSTING_METHOD INTEGER  ,
  IS_ADJUST_CURRENCY CHAR(1) default 1   ,
  IS_PROPAGATE CHAR(1) default 0   ,
  STATUS INTEGER NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_acct_schema primary key (ID)
);

CREATE TABLE erp_md_warehouse(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  WAREHOUSE_TYPE INTEGER  ,
  ORG_ID NUMBER(20)  ,
  ADDRESS VARCHAR2(500)  ,
  MANAGER_ID NUMBER(20)  ,
  STATUS INTEGER NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_md_warehouse primary key (ID)
);

CREATE TABLE erp_md_acct_schema_coa(
  ID NUMBER(20) NOT NULL ,
  ACCT_SCHEMA_ID NUMBER(20) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  IS_PRIMARY CHAR(1) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_acct_schema_coa primary key (ID)
);

CREATE TABLE erp_md_material(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  MATERIAL_TYPE INTEGER NOT NULL ,
  CATEGORY_ID NUMBER(20)  ,
  UOM_ID NUMBER(20) NOT NULL ,
  STATUS INTEGER NOT NULL ,
  COST_METHOD INTEGER  ,
  IS_BATCH_MANAGED CHAR(1) default 0   ,
  IS_SERIAL_MANAGED CHAR(1) default 0   ,
  DEFAULT_WAREHOUSE_ID NUMBER(20)  ,
  MIN_STOCK NUMBER(20,4)  ,
  MAX_STOCK NUMBER(20,4)  ,
  SAFETY_STOCK NUMBER(20,4)  ,
  LEAD_TIME_DAYS INTEGER  ,
  WEIGHT NUMBER(12,4)  ,
  VOLUME NUMBER(12,6)  ,
  DEFAULT_TAX_RATE_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_md_material primary key (ID)
);

CREATE TABLE erp_md_location(
  ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  PARENT_ID NUMBER(20)  ,
  IS_ACTIVE CHAR(1) default 1   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_location primary key (ID)
);

CREATE TABLE erp_md_material_sku(
  ID NUMBER(20) NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_CODE VARCHAR2(50) NOT NULL ,
  BARCODE VARCHAR2(50)  ,
  UOM_ID NUMBER(20) NOT NULL ,
  CONVERSION_RATE NUMBER(12,4) default 1   ,
  PURCHASE_PRICE NUMBER(20,4)  ,
  SALE_PRICE NUMBER(20,4)  ,
  WHOLESALE_PRICE NUMBER(20,4)  ,
  RETAIL_PRICE NUMBER(20,4)  ,
  TAX_RATE_ID NUMBER(20)  ,
  IS_DEFAULT CHAR(1) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_material_sku primary key (ID)
);

CREATE TABLE erp_md_uom_conversion(
  ID NUMBER(20) NOT NULL ,
  MATERIAL_ID NUMBER(20)  ,
  FROM_UOM_ID NUMBER(20) NOT NULL ,
  TO_UOM_ID NUMBER(20) NOT NULL ,
  CONVERSION_RATE NUMBER(20,8) NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_md_uom_conversion primary key (ID)
);


      COMMENT ON TABLE erp_md_material_category IS '物料分类';
                
      COMMENT ON COLUMN erp_md_material_category.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_material_category.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_md_material_category.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_md_material_category.PARENT_ID IS '父级ID';
                    
      COMMENT ON COLUMN erp_md_material_category.SORT_NUM IS '排序';
                    
      COMMENT ON COLUMN erp_md_material_category.PRICE_VALIDATION_LEVEL IS '价格校验级别';
                    
      COMMENT ON COLUMN erp_md_material_category.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_material_category.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_material_category.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_material_category.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_material_category.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_material_category.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_uom IS '计量单位';
                
      COMMENT ON COLUMN erp_md_uom.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_uom.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_md_uom.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_md_uom.UOM_GROUP IS '单位组';
                    
      COMMENT ON COLUMN erp_md_uom.IS_BASE IS '是否基本单位';
                    
      COMMENT ON COLUMN erp_md_uom.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_uom.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_uom.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_uom.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_uom.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_uom.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_tax_rate IS '税率';
                
      COMMENT ON COLUMN erp_md_tax_rate.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_tax_rate.CODE IS '税率编码';
                    
      COMMENT ON COLUMN erp_md_tax_rate.NAME IS '税率名称';
                    
      COMMENT ON COLUMN erp_md_tax_rate.TAX_TYPE IS '税种';
                    
      COMMENT ON COLUMN erp_md_tax_rate.RATE IS '税率(%)';
                    
      COMMENT ON COLUMN erp_md_tax_rate.IS_ZERO_RATED IS '是否零税率';
                    
      COMMENT ON COLUMN erp_md_tax_rate.IS_EXEMPT IS '是否免税';
                    
      COMMENT ON COLUMN erp_md_tax_rate.VALID_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_md_tax_rate.VALID_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_md_tax_rate.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_md_tax_rate.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_tax_rate.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_tax_rate.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_tax_rate.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_tax_rate.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_tax_rate.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON COLUMN erp_md_partner.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_partner.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_md_partner.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_md_partner.PARTNER_TYPE IS '类型';
                    
      COMMENT ON COLUMN erp_md_partner.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_md_partner.CONTACT_PERSON IS '联系人';
                    
      COMMENT ON COLUMN erp_md_partner.PHONE IS '电话';
                    
      COMMENT ON COLUMN erp_md_partner.EMAIL IS '邮箱';
                    
      COMMENT ON COLUMN erp_md_partner.ADDRESS IS '地址';
                    
      COMMENT ON COLUMN erp_md_partner.TAX_NO IS '税号';
                    
      COMMENT ON COLUMN erp_md_partner.CREDIT_LIMIT IS '信用额度';
                    
      COMMENT ON COLUMN erp_md_partner.CREDIT_PERIOD_DAYS IS '信用期(天)';
                    
      COMMENT ON COLUMN erp_md_partner.RECEIVABLE_BALANCE IS '应收余额';
                    
      COMMENT ON COLUMN erp_md_partner.PAYABLE_BALANCE IS '应付余额';
                    
      COMMENT ON COLUMN erp_md_partner.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_partner.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_partner.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_partner.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_partner.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_partner.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_partner.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON COLUMN erp_md_currency.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_currency.CODE IS '币种代码';
                    
      COMMENT ON COLUMN erp_md_currency.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_md_currency.SYMBOL IS '符号';
                    
      COMMENT ON COLUMN erp_md_currency.DECIMAL_PLACES IS '小数位数';
                    
      COMMENT ON COLUMN erp_md_currency.IS_FUNCTIONAL IS '是否本位币';
                    
      COMMENT ON COLUMN erp_md_currency.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_md_currency.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_currency.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_currency.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_currency.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_currency.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_currency.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_settlement_method IS '结算方式';
                
      COMMENT ON COLUMN erp_md_settlement_method.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_settlement_method.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_md_settlement_method.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_md_settlement_method.SETTLEMENT_TYPE IS '结算类型';
                    
      COMMENT ON COLUMN erp_md_settlement_method.DEFAULT_FUND_ACCOUNT_ID IS '默认资金账户';
                    
      COMMENT ON COLUMN erp_md_settlement_method.DEFAULT_DAYS IS '默认账期(天)';
                    
      COMMENT ON COLUMN erp_md_settlement_method.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_md_settlement_method.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_settlement_method.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_settlement_method.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_settlement_method.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_settlement_method.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_settlement_method.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_partner_address IS '往来单位地址';
                
      COMMENT ON COLUMN erp_md_partner_address.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_partner_address.PARTNER_ID IS '往来单位';
                    
      COMMENT ON COLUMN erp_md_partner_address.ADDRESS_TYPE IS '地址类型';
                    
      COMMENT ON COLUMN erp_md_partner_address.CONTACT_PERSON IS '联系人';
                    
      COMMENT ON COLUMN erp_md_partner_address.PHONE IS '电话';
                    
      COMMENT ON COLUMN erp_md_partner_address.ADDRESS IS '详细地址';
                    
      COMMENT ON COLUMN erp_md_partner_address.IS_DEFAULT IS '是否默认';
                    
      COMMENT ON COLUMN erp_md_partner_address.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_partner_address.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_partner_address.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_partner_address.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_partner_address.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_partner_address.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_partner_contact IS '往来单位联系人';
                
      COMMENT ON COLUMN erp_md_partner_contact.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_partner_contact.PARTNER_ID IS '往来单位';
                    
      COMMENT ON COLUMN erp_md_partner_contact.CONTACT_PERSON IS '联系人姓名';
                    
      COMMENT ON COLUMN erp_md_partner_contact.POSITION IS '职务';
                    
      COMMENT ON COLUMN erp_md_partner_contact.PHONE IS '电话';
                    
      COMMENT ON COLUMN erp_md_partner_contact.EMAIL IS '邮箱';
                    
      COMMENT ON COLUMN erp_md_partner_contact.IS_DEFAULT IS '是否默认联系人';
                    
      COMMENT ON COLUMN erp_md_partner_contact.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_partner_contact.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_partner_contact.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_partner_contact.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_partner_contact.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_partner_contact.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_partner_contact.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_md_bank_account IS '银行账户';
                
      COMMENT ON COLUMN erp_md_bank_account.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_bank_account.PARTNER_ID IS '往来单位';
                    
      COMMENT ON COLUMN erp_md_bank_account.BANK_NAME IS '开户银行';
                    
      COMMENT ON COLUMN erp_md_bank_account.BANK_BRANCH IS '支行';
                    
      COMMENT ON COLUMN erp_md_bank_account.BANK_ACCOUNT IS '银行账号';
                    
      COMMENT ON COLUMN erp_md_bank_account.ACCOUNT_TYPE IS '账户类型';
                    
      COMMENT ON COLUMN erp_md_bank_account.ACCOUNT_HOLDER IS '户名';
                    
      COMMENT ON COLUMN erp_md_bank_account.IS_DEFAULT IS '是否默认';
                    
      COMMENT ON COLUMN erp_md_bank_account.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_bank_account.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_bank_account.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_bank_account.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_bank_account.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_bank_account.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_exchange_rate IS '汇率';
                
      COMMENT ON COLUMN erp_md_exchange_rate.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.FROM_CURRENCY_ID IS '源币种';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.TO_CURRENCY_ID IS '目标币种';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.RATE_TYPE IS '汇率类型';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.VALID_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.VALID_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_subject IS '会计科目';
                
      COMMENT ON COLUMN erp_md_subject.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_subject.CODE IS '科目编码';
                    
      COMMENT ON COLUMN erp_md_subject.NAME IS '科目名称';
                    
      COMMENT ON COLUMN erp_md_subject.PARENT_ID IS '父级科目';
                    
      COMMENT ON COLUMN erp_md_subject.SUBJECT_CLASS IS '科目类别';
                    
      COMMENT ON COLUMN erp_md_subject.DIRECTION IS '余额方向';
                    
      COMMENT ON COLUMN erp_md_subject.BALANCE_TYPE IS '余额类型';
                    
      COMMENT ON COLUMN erp_md_subject.CURRENCY_ID IS '核算币种';
                    
      COMMENT ON COLUMN erp_md_subject.IS_AUXILIARY_PARTNER IS '辅助-往来单位';
                    
      COMMENT ON COLUMN erp_md_subject.IS_AUXILIARY_DEPARTMENT IS '辅助-部门';
                    
      COMMENT ON COLUMN erp_md_subject.IS_AUXILIARY_PROJECT IS '辅助-项目';
                    
      COMMENT ON COLUMN erp_md_subject.IS_AUXILIARY_WAREHOUSE IS '辅助-仓库';
                    
      COMMENT ON COLUMN erp_md_subject.IS_AUXILIARY_PRODUCT IS '辅助-物料';
                    
      COMMENT ON COLUMN erp_md_subject.IS_LEAF IS '是否明细科目';
                    
      COMMENT ON COLUMN erp_md_subject.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_md_subject.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_subject.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_subject.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_subject.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_subject.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_subject.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_subject.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON COLUMN erp_md_organization.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_organization.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_md_organization.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_md_organization.PARENT_ID IS '父级组织';
                    
      COMMENT ON COLUMN erp_md_organization.ORG_TYPE IS '组织类型';
                    
      COMMENT ON COLUMN erp_md_organization.FUNCTIONAL_CURRENCY_ID IS '本位币';
                    
      COMMENT ON COLUMN erp_md_organization.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_md_organization.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_organization.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_organization.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_organization.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_organization.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_organization.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON COLUMN erp_md_employee.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_employee.CODE IS '工号';
                    
      COMMENT ON COLUMN erp_md_employee.NAME IS '姓名';
                    
      COMMENT ON COLUMN erp_md_employee.ORG_ID IS '所属组织/部门';
                    
      COMMENT ON COLUMN erp_md_employee.POSITION IS '职务';
                    
      COMMENT ON COLUMN erp_md_employee.PHONE IS '电话';
                    
      COMMENT ON COLUMN erp_md_employee.EMAIL IS '邮箱';
                    
      COMMENT ON COLUMN erp_md_employee.PARTNER_ID IS '对应内部往来单位';
                    
      COMMENT ON COLUMN erp_md_employee.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_md_employee.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_employee.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_employee.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_employee.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_employee.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_employee.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_acct_schema IS '会计核算表(账套)';
                
      COMMENT ON COLUMN erp_md_acct_schema.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_acct_schema.CODE IS '账套编码';
                    
      COMMENT ON COLUMN erp_md_acct_schema.NAME IS '账套名称';
                    
      COMMENT ON COLUMN erp_md_acct_schema.ORG_ID IS '核算组织';
                    
      COMMENT ON COLUMN erp_md_acct_schema.NATURE IS '账套性质';
                    
      COMMENT ON COLUMN erp_md_acct_schema.FUNCTIONAL_CURRENCY_ID IS '本位币';
                    
      COMMENT ON COLUMN erp_md_acct_schema.COSTING_METHOD IS '默认成本方法';
                    
      COMMENT ON COLUMN erp_md_acct_schema.IS_ADJUST_CURRENCY IS '是否调整汇率差异';
                    
      COMMENT ON COLUMN erp_md_acct_schema.IS_PROPAGATE IS '是否自动复制分录到其它账套';
                    
      COMMENT ON COLUMN erp_md_acct_schema.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_md_acct_schema.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_acct_schema.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_acct_schema.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_acct_schema.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_acct_schema.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_acct_schema.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON COLUMN erp_md_warehouse.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_warehouse.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_md_warehouse.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_md_warehouse.WAREHOUSE_TYPE IS '仓库类型';
                    
      COMMENT ON COLUMN erp_md_warehouse.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_md_warehouse.ADDRESS IS '地址';
                    
      COMMENT ON COLUMN erp_md_warehouse.MANAGER_ID IS '负责人(职员)';
                    
      COMMENT ON COLUMN erp_md_warehouse.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_md_warehouse.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_warehouse.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_warehouse.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_warehouse.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_warehouse.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_warehouse.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_warehouse.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_md_acct_schema_coa IS '账套科目表';
                
      COMMENT ON COLUMN erp_md_acct_schema_coa.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.NAME IS '科目表名称';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.IS_PRIMARY IS '是否主科目表';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON COLUMN erp_md_material.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_material.CODE IS '物料编码';
                    
      COMMENT ON COLUMN erp_md_material.NAME IS '物料名称';
                    
      COMMENT ON COLUMN erp_md_material.MATERIAL_TYPE IS '物料类型';
                    
      COMMENT ON COLUMN erp_md_material.CATEGORY_ID IS '分类ID';
                    
      COMMENT ON COLUMN erp_md_material.UOM_ID IS '主计量单位';
                    
      COMMENT ON COLUMN erp_md_material.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_md_material.COST_METHOD IS '存货计价方法';
                    
      COMMENT ON COLUMN erp_md_material.IS_BATCH_MANAGED IS '是否批次管理';
                    
      COMMENT ON COLUMN erp_md_material.IS_SERIAL_MANAGED IS '是否序列号管理';
                    
      COMMENT ON COLUMN erp_md_material.DEFAULT_WAREHOUSE_ID IS '默认仓库';
                    
      COMMENT ON COLUMN erp_md_material.MIN_STOCK IS '最低库存';
                    
      COMMENT ON COLUMN erp_md_material.MAX_STOCK IS '最高库存';
                    
      COMMENT ON COLUMN erp_md_material.SAFETY_STOCK IS '安全库存';
                    
      COMMENT ON COLUMN erp_md_material.LEAD_TIME_DAYS IS '采购提前期(天)';
                    
      COMMENT ON COLUMN erp_md_material.WEIGHT IS '重量(kg)';
                    
      COMMENT ON COLUMN erp_md_material.VOLUME IS '体积(m3)';
                    
      COMMENT ON COLUMN erp_md_material.DEFAULT_TAX_RATE_ID IS '默认税率';
                    
      COMMENT ON COLUMN erp_md_material.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_material.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_material.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_material.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_material.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_material.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_material.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON COLUMN erp_md_location.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_location.WAREHOUSE_ID IS '仓库ID';
                    
      COMMENT ON COLUMN erp_md_location.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_md_location.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_md_location.PARENT_ID IS '父级库位';
                    
      COMMENT ON COLUMN erp_md_location.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_md_location.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_location.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_location.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_location.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_location.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_location.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_material_sku IS '物料SKU';
                
      COMMENT ON COLUMN erp_md_material_sku.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_material_sku.MATERIAL_ID IS '物料ID';
                    
      COMMENT ON COLUMN erp_md_material_sku.SKU_CODE IS 'SKU编码';
                    
      COMMENT ON COLUMN erp_md_material_sku.BARCODE IS '条码';
                    
      COMMENT ON COLUMN erp_md_material_sku.UOM_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_md_material_sku.CONVERSION_RATE IS '换算系数';
                    
      COMMENT ON COLUMN erp_md_material_sku.PURCHASE_PRICE IS '采购价(不含税)';
                    
      COMMENT ON COLUMN erp_md_material_sku.SALE_PRICE IS '销售价(不含税)';
                    
      COMMENT ON COLUMN erp_md_material_sku.WHOLESALE_PRICE IS '批发价(不含税)';
                    
      COMMENT ON COLUMN erp_md_material_sku.RETAIL_PRICE IS '零售价(不含税)';
                    
      COMMENT ON COLUMN erp_md_material_sku.TAX_RATE_ID IS '默认税率';
                    
      COMMENT ON COLUMN erp_md_material_sku.IS_DEFAULT IS '是否默认SKU';
                    
      COMMENT ON COLUMN erp_md_material_sku.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_material_sku.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_material_sku.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_material_sku.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_material_sku.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_material_sku.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_uom_conversion IS '单位换算';
                
      COMMENT ON COLUMN erp_md_uom_conversion.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.MATERIAL_ID IS '物料(空表示通用)';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.FROM_UOM_ID IS '源单位';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.TO_UOM_ID IS '目标单位';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.CONVERSION_RATE IS '换算系数';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.UPDATE_TIME IS '修改时间';
                    
