
CREATE TABLE erp_md_organization(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_TYPE INTEGER  ,
  PARENT_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_organization primary key (ID)
);

CREATE TABLE erp_md_location(
  ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARENT_ID NUMBER(20)  ,
  constraint PK_erp_md_location primary key (ID)
);

CREATE TABLE erp_md_employee(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_employee primary key (ID)
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

CREATE TABLE erp_md_subject(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARENT_ID NUMBER(20)  ,
  SUBJECT_CLASS INTEGER  ,
  DIRECTION INTEGER  ,
  constraint PK_erp_md_subject primary key (ID)
);

CREATE TABLE erp_md_material_category(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARENT_ID NUMBER(20)  ,
  constraint PK_erp_md_material_category primary key (ID)
);

CREATE TABLE erp_ast_asset_category(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  DEPRECIATION_METHOD VARCHAR2(20)  ,
  USEFUL_LIFE_MONTHS INTEGER  ,
  SUBJECT_ID NUMBER(20)  ,
  DEPRECIATION_SUBJECT_ID NUMBER(20)  ,
  EXPENSE_SUBJECT_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DISPOSAL_GAIN_LOSS_SUBJECT_ID NUMBER(20)  ,
  CIP_SUBJECT_ID NUMBER(20)  ,
  constraint PK_erp_ast_asset_category primary key (ID)
);

CREATE TABLE erp_ast_asset(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CATEGORY_ID NUMBER(20)  ,
  ACQUISITION_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  ORIGINAL_VALUE NUMBER(20,4) NOT NULL ,
  CURRENT_VALUE NUMBER(20,4)  ,
  RESIDUAL_VALUE NUMBER(20,4) default 0   ,
  DEPRECIATION_METHOD VARCHAR2(20)  ,
  DEPRECIATION_RATE NUMBER(10,6)  ,
  USEFUL_LIFE_MONTHS INTEGER  ,
  DEPARTMENT_ID NUMBER(20)  ,
  LOCATION_ID NUMBER(20)  ,
  EMPLOYEE_ID NUMBER(20)  ,
  STAFF_ID NUMBER(20)  ,
  BRAND_MODEL VARCHAR2(200)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  ACCUMULATED_DEPRECIATION NUMBER(20,4) default 0   ,
  NET_BOOK_VALUE NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_asset primary key (ID)
);

CREATE TABLE erp_ast_asset_capitalization(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ASSET_CODE VARCHAR2(50)  ,
  ASSET_NAME VARCHAR2(200)  ,
  CATEGORY_ID NUMBER(20)  ,
  CURRENCY_ID NUMBER(20)  ,
  CAPITALIZATION_DATE DATE NOT NULL ,
  ORIGINAL_VALUE NUMBER(20,4) NOT NULL ,
  SOURCE_TYPE VARCHAR2(20) NOT NULL ,
  SOURCE_CODE VARCHAR2(50)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT TIMESTAMP  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  BUSINESS_DATE DATE  ,
  EXCHANGE_RATE NUMBER(20,8) NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_asset_capitalization primary key (ID)
);

CREATE TABLE erp_ast_inventory(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200)  ,
  ORG_ID NUMBER(20)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  RANGE_DEPARTMENT_ID NUMBER(20)  ,
  RANGE_CATEGORY_ID NUMBER(20)  ,
  RANGE_LOCATION_ID NUMBER(20)  ,
  RESPONSIBLE_BY_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  SURPLUS_COUNT INTEGER default 0   ,
  SHORTAGE_COUNT INTEGER default 0   ,
  MATCHED_COUNT INTEGER default 0   ,
  SURPLUS_AMOUNT NUMBER(20,4) default 0   ,
  SHORTAGE_AMOUNT NUMBER(20,4) default 0   ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT TIMESTAMP  ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_ast_inventory primary key (ID)
);

CREATE TABLE erp_ast_depreciation_schedule(
  ID NUMBER(20) NOT NULL ,
  ASSET_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PERIOD VARCHAR2(20) NOT NULL ,
  PLANNED_AMOUNT NUMBER(20,4) NOT NULL ,
  ACTUAL_AMOUNT NUMBER(20,4)  ,
  ACCUMULATED_DEPRECIATION NUMBER(20,4) default 0   ,
  NET_BOOK_VALUE NUMBER(20,4)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  EXECUTED_AT TIMESTAMP  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(36)  ,
  VOUCHER_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  BUSINESS_DATE DATE  ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,8) default 1  NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_depreciation_schedule primary key (ID)
);

CREATE TABLE erp_ast_movement(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ASSET_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  FROM_DATE DATE NOT NULL ,
  THRU_DATE DATE  ,
  FROM_DEPARTMENT_ID NUMBER(20)  ,
  TO_DEPARTMENT_ID NUMBER(20)  ,
  FROM_STAFF_ID NUMBER(20)  ,
  TO_STAFF_ID NUMBER(20)  ,
  FROM_LOCATION_ID NUMBER(20)  ,
  TO_LOCATION_ID NUMBER(20)  ,
  HANDLER_ID NUMBER(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(36)  ,
  DOC_VERSION VARCHAR2(20) default '1'  NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT TIMESTAMP  ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,8) NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_movement primary key (ID)
);

CREATE TABLE erp_ast_value_adjustment(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ASSET_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  ADJUSTMENT_TYPE VARCHAR2(20) NOT NULL ,
  ADJUSTMENT_AMOUNT NUMBER(20,4) NOT NULL ,
  REASON VARCHAR2(500)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT TIMESTAMP  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_value_adjustment primary key (ID)
);

CREATE TABLE erp_ast_disposal(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ASSET_ID NUMBER(20) NOT NULL ,
  DISPOSAL_TYPE VARCHAR2(20) NOT NULL ,
  DISPOSAL_AMOUNT NUMBER(20,4)  ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  BUSINESS_DATE DATE NOT NULL ,
  GAIN_LOSS NUMBER(20,4)  ,
  REASON VARCHAR2(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT TIMESTAMP  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  NOP_FLOW_ID VARCHAR2(32)  ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_disposal primary key (ID)
);

CREATE TABLE erp_ast_cip(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CATEGORY_ID NUMBER(20)  ,
  CURRENCY_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  ESTIMATED_COMPLETION_DATE DATE  ,
  ACCUMULATED_COST NUMBER(20,4) default 0   ,
  IS_COMPLETED CHAR(1) default 0   ,
  COMPLETED_ASSET_ID NUMBER(20)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  PROJECT_ID NUMBER(20)  ,
  CIP_ASSET_CATEGORY_SNAPSHOT VARCHAR2(500)  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(50)  ,
  EXCHANGE_RATE NUMBER(20,8) NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_cip primary key (ID)
);

CREATE TABLE erp_ast_split(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SOURCE_ASSET_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  SPLIT_REASON VARCHAR2(500)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT TIMESTAMP  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  EXCHANGE_RATE NUMBER(20,8) NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_split primary key (ID)
);

CREATE TABLE erp_ast_merge(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TARGET_ASSET_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  MERGE_REASON VARCHAR2(500)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT TIMESTAMP  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  EXCHANGE_RATE NUMBER(20,8) NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_merge primary key (ID)
);

CREATE TABLE erp_ast_maintenance(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200)  ,
  ORG_ID NUMBER(20)  ,
  ASSET_ID NUMBER(20) NOT NULL ,
  MAINTENANCE_VISIT_ID NUMBER(20)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  TREATMENT VARCHAR2(20)  ,
  CAPITALIZED_AMOUNT NUMBER(20,4) default 0   ,
  TOTAL_COST_AMOUNT NUMBER(20,4) default 0   ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  REASON VARCHAR2(500)  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT TIMESTAMP  ,
  REVERSED CHAR(1) default 0   ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_ast_maintenance primary key (ID)
);

CREATE TABLE erp_ast_inventory_line(
  ID NUMBER(20) NOT NULL ,
  INVENTORY_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  LINE_NO INTEGER  ,
  ASSET_ID NUMBER(20)  ,
  ASSET_CODE_SNAPSHOT VARCHAR2(50)  ,
  ASSET_NAME_SNAPSHOT VARCHAR2(200)  ,
  CATEGORY_ID NUMBER(20)  ,
  BOOK_QUANTITY INTEGER default 0   ,
  ACTUAL_QUANTITY INTEGER  ,
  VARIANCE_QUANTITY INTEGER default 0   ,
  VARIANCE_TYPE VARCHAR2(20)  ,
  BOOK_VALUE NUMBER(20,4) default 0   ,
  ASSESSED_VALUE NUMBER(20,4) default 0   ,
  VARIANCE_AMOUNT NUMBER(20,4) default 0   ,
  DISPOSITION VARCHAR2(20)  ,
  NEW_ASSET_ID NUMBER(20)  ,
  CAPITALIZATION_ID NUMBER(20)  ,
  DISPOSAL_ID NUMBER(20)  ,
  INVESTIGATED_REMARK VARCHAR2(500)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_ast_inventory_line primary key (ID)
);

CREATE TABLE erp_ast_cip_cost_item(
  ID NUMBER(20) NOT NULL ,
  CIP_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  LINE_NO INTEGER  ,
  COST_TYPE VARCHAR2(30) NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  EXCHANGE_RATE NUMBER(20,8) default 1  NOT NULL ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0  NOT NULL ,
  SOURCE_BILL_TYPE VARCHAR2(30)  ,
  SOURCE_BILL_CODE VARCHAR2(50)  ,
  POSTED_TRANSFER_FLAG CHAR(1) default 0   ,
  CAPITALIZATION_ID NUMBER(20)  ,
  BUSINESS_DATE DATE  ,
  CURRENCY_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_ast_cip_cost_item primary key (ID)
);

CREATE TABLE erp_ast_cip_progress_billing(
  ID NUMBER(20) NOT NULL ,
  CIP_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  LINE_NO INTEGER  ,
  BILLING_DATE DATE NOT NULL ,
  BILLING_MILESTONE VARCHAR2(200)  ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  EXCHANGE_RATE NUMBER(20,8) default 1  NOT NULL ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0  NOT NULL ,
  PAYMENT_VOUCHER_CODE VARCHAR2(50)  ,
  PAID_FLAG CHAR(1) default 0   ,
  CURRENCY_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_ast_cip_progress_billing primary key (ID)
);

CREATE TABLE erp_ast_split_line(
  ID NUMBER(20) NOT NULL ,
  SPLIT_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  LINE_NO INTEGER  ,
  TARGET_ASSET_CODE VARCHAR2(50) NOT NULL ,
  TARGET_ASSET_NAME VARCHAR2(200)  ,
  CATEGORY_ID NUMBER(20)  ,
  ALLOCATION_METHOD VARCHAR2(20)  ,
  PROPORTION NUMBER(8,6) default 0   ,
  ORIGINAL_COST_AMOUNT NUMBER(20,4) default 0  NOT NULL ,
  ACCUMULATED_DEPRECIATION_AMOUNT NUMBER(20,4) default 0   ,
  NET_BOOK_VALUE NUMBER(20,4) default 0   ,
  TARGET_ASSET_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_ast_split_line primary key (ID)
);

CREATE TABLE erp_ast_merge_line(
  ID NUMBER(20) NOT NULL ,
  MERGE_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  LINE_NO INTEGER  ,
  SOURCE_ASSET_ID NUMBER(20) NOT NULL ,
  CONTRIBUTION_PROPORTION NUMBER(8,6) default 0   ,
  ORIGINAL_COST_AMOUNT NUMBER(20,4) default 0  NOT NULL ,
  ACCUMULATED_DEPRECIATION_AMOUNT NUMBER(20,4) default 0   ,
  NET_BOOK_VALUE NUMBER(20,4) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_ast_merge_line primary key (ID)
);

CREATE TABLE erp_ast_maintenance_cost(
  ID NUMBER(20) NOT NULL ,
  MAINTENANCE_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  LINE_NO INTEGER  ,
  COST_TYPE VARCHAR2(20) NOT NULL ,
  AMOUNT NUMBER(20,4) default 0   ,
  BUSINESS_DATE DATE  ,
  CURRENCY_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  constraint PK_erp_ast_maintenance_cost primary key (ID)
);


      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_subject IS '会计科目';
                
      COMMENT ON TABLE erp_md_material_category IS '物料分类';
                
      COMMENT ON TABLE erp_ast_asset_category IS '资产类别';
                
      COMMENT ON COLUMN erp_ast_asset_category.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_asset_category.CODE IS '类别编码';
                    
      COMMENT ON COLUMN erp_ast_asset_category.NAME IS '类别名称';
                    
      COMMENT ON COLUMN erp_ast_asset_category.DEPRECIATION_METHOD IS '默认折旧方法';
                    
      COMMENT ON COLUMN erp_ast_asset_category.USEFUL_LIFE_MONTHS IS '默认使用年限(月)';
                    
      COMMENT ON COLUMN erp_ast_asset_category.SUBJECT_ID IS '资产科目';
                    
      COMMENT ON COLUMN erp_ast_asset_category.DEPRECIATION_SUBJECT_ID IS '累计折旧科目';
                    
      COMMENT ON COLUMN erp_ast_asset_category.EXPENSE_SUBJECT_ID IS '折旧费用科目';
                    
      COMMENT ON COLUMN erp_ast_asset_category.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_asset_category.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_asset_category.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_asset_category.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_asset_category.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_asset_category.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_asset_category.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ast_asset_category.DISPOSAL_GAIN_LOSS_SUBJECT_ID IS '清理损益科目';
                    
      COMMENT ON COLUMN erp_ast_asset_category.CIP_SUBJECT_ID IS '在建工程科目';
                    
      COMMENT ON TABLE erp_ast_asset IS '固定资产';
                
      COMMENT ON COLUMN erp_ast_asset.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_asset.CODE IS '资产编码';
                    
      COMMENT ON COLUMN erp_ast_asset.NAME IS '资产名称';
                    
      COMMENT ON COLUMN erp_ast_asset.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_asset.CATEGORY_ID IS '资产类别';
                    
      COMMENT ON COLUMN erp_ast_asset.ACQUISITION_DATE IS '取得日期';
                    
      COMMENT ON COLUMN erp_ast_asset.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_asset.ORIGINAL_VALUE IS '原值';
                    
      COMMENT ON COLUMN erp_ast_asset.CURRENT_VALUE IS '当前价值';
                    
      COMMENT ON COLUMN erp_ast_asset.RESIDUAL_VALUE IS '残值';
                    
      COMMENT ON COLUMN erp_ast_asset.DEPRECIATION_METHOD IS '折旧方法';
                    
      COMMENT ON COLUMN erp_ast_asset.DEPRECIATION_RATE IS '折旧率';
                    
      COMMENT ON COLUMN erp_ast_asset.USEFUL_LIFE_MONTHS IS '使用年限(月)';
                    
      COMMENT ON COLUMN erp_ast_asset.DEPARTMENT_ID IS '使用部门';
                    
      COMMENT ON COLUMN erp_ast_asset.LOCATION_ID IS '使用地点';
                    
      COMMENT ON COLUMN erp_ast_asset.EMPLOYEE_ID IS '使用人(职员)';
                    
      COMMENT ON COLUMN erp_ast_asset.STAFF_ID IS '使用人(往来单位,旧字段保留)';
                    
      COMMENT ON COLUMN erp_ast_asset.BRAND_MODEL IS '品牌型号';
                    
      COMMENT ON COLUMN erp_ast_asset.STATUS IS '资产状态';
                    
      COMMENT ON COLUMN erp_ast_asset.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_asset.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_asset.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_asset.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_asset.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_asset.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_asset.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ast_asset.ACCUMULATED_DEPRECIATION IS '累计折旧';
                    
      COMMENT ON COLUMN erp_ast_asset.NET_BOOK_VALUE IS '净值';
                    
      COMMENT ON TABLE erp_ast_asset_capitalization IS '资产资本化';
                
      COMMENT ON COLUMN erp_ast_asset_capitalization.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.ASSET_CODE IS '资产编码';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.ASSET_NAME IS '资产名称';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.CATEGORY_ID IS '资产类别';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.CAPITALIZATION_DATE IS '资本化日期';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.ORIGINAL_VALUE IS '原值';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.SOURCE_TYPE IS '来源类型';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.SOURCE_CODE IS '来源单号';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_inventory IS '资产盘点单';
                
      COMMENT ON COLUMN erp_ast_inventory.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_inventory.CODE IS '盘点单号';
                    
      COMMENT ON COLUMN erp_ast_inventory.NAME IS '盘点名称';
                    
      COMMENT ON COLUMN erp_ast_inventory.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_inventory.STATUS IS '盘点状态';
                    
      COMMENT ON COLUMN erp_ast_inventory.RANGE_DEPARTMENT_ID IS '范围-部门';
                    
      COMMENT ON COLUMN erp_ast_inventory.RANGE_CATEGORY_ID IS '范围-资产类别';
                    
      COMMENT ON COLUMN erp_ast_inventory.RANGE_LOCATION_ID IS '范围-地点';
                    
      COMMENT ON COLUMN erp_ast_inventory.RESPONSIBLE_BY_ID IS '盘点负责人';
                    
      COMMENT ON COLUMN erp_ast_inventory.BUSINESS_DATE IS '盘点基准日';
                    
      COMMENT ON COLUMN erp_ast_inventory.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_inventory.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_inventory.SURPLUS_COUNT IS '盘盈行数';
                    
      COMMENT ON COLUMN erp_ast_inventory.SHORTAGE_COUNT IS '盘亏行数';
                    
      COMMENT ON COLUMN erp_ast_inventory.MATCHED_COUNT IS '一致行数';
                    
      COMMENT ON COLUMN erp_ast_inventory.SURPLUS_AMOUNT IS '盘盈金额';
                    
      COMMENT ON COLUMN erp_ast_inventory.SHORTAGE_AMOUNT IS '盘亏金额';
                    
      COMMENT ON COLUMN erp_ast_inventory.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_inventory.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_inventory.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_inventory.APPROVED_BY IS '复核人';
                    
      COMMENT ON COLUMN erp_ast_inventory.APPROVED_AT IS '复核时间';
                    
      COMMENT ON COLUMN erp_ast_inventory.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_inventory.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON COLUMN erp_ast_inventory.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_inventory.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_inventory.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_inventory.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_inventory.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_inventory.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_inventory.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_ast_depreciation_schedule IS '折旧计划';
                
      COMMENT ON COLUMN erp_ast_depreciation_schedule.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.ASSET_ID IS '资产ID';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.PERIOD IS '折旧期间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.PLANNED_AMOUNT IS '计划折旧额';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.ACTUAL_AMOUNT IS '实际折旧额';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.ACCUMULATED_DEPRECIATION IS '累计折旧';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.NET_BOOK_VALUE IS '净值';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.STATUS IS '执行状态';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.EXECUTED_AT IS '执行时间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.VOUCHER_ID IS '凭证ID';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_movement IS '资产移动';
                
      COMMENT ON COLUMN erp_ast_movement.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_movement.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_ast_movement.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_movement.ASSET_ID IS '资产ID';
                    
      COMMENT ON COLUMN erp_ast_movement.BUSINESS_DATE IS '移动日期';
                    
      COMMENT ON COLUMN erp_ast_movement.FROM_DATE IS '开始日期';
                    
      COMMENT ON COLUMN erp_ast_movement.THRU_DATE IS '截止日期';
                    
      COMMENT ON COLUMN erp_ast_movement.FROM_DEPARTMENT_ID IS '原使用部门';
                    
      COMMENT ON COLUMN erp_ast_movement.TO_DEPARTMENT_ID IS '新使用部门';
                    
      COMMENT ON COLUMN erp_ast_movement.FROM_STAFF_ID IS '原使用人';
                    
      COMMENT ON COLUMN erp_ast_movement.TO_STAFF_ID IS '新使用人';
                    
      COMMENT ON COLUMN erp_ast_movement.FROM_LOCATION_ID IS '原使用地点';
                    
      COMMENT ON COLUMN erp_ast_movement.TO_LOCATION_ID IS '新使用地点';
                    
      COMMENT ON COLUMN erp_ast_movement.HANDLER_ID IS '经办人';
                    
      COMMENT ON COLUMN erp_ast_movement.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_movement.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_movement.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_movement.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_movement.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_movement.DOC_VERSION IS '单据版本';
                    
      COMMENT ON COLUMN erp_ast_movement.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_movement.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_movement.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_movement.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_movement.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_movement.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_movement.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ast_movement.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_movement.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_movement.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_movement.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_movement.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_movement.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_value_adjustment IS '资产价值调整';
                
      COMMENT ON COLUMN erp_ast_value_adjustment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.ASSET_ID IS '资产ID';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.BUSINESS_DATE IS '调整日期';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.ADJUSTMENT_TYPE IS '调整类型';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.ADJUSTMENT_AMOUNT IS '调整金额';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.REASON IS '调整原因';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_disposal IS '资产处置';
                
      COMMENT ON COLUMN erp_ast_disposal.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_disposal.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_ast_disposal.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_disposal.ASSET_ID IS '资产ID';
                    
      COMMENT ON COLUMN erp_ast_disposal.DISPOSAL_TYPE IS '处置类型';
                    
      COMMENT ON COLUMN erp_ast_disposal.DISPOSAL_AMOUNT IS '处置金额';
                    
      COMMENT ON COLUMN erp_ast_disposal.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_disposal.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_disposal.BUSINESS_DATE IS '处置日期';
                    
      COMMENT ON COLUMN erp_ast_disposal.GAIN_LOSS IS '损益';
                    
      COMMENT ON COLUMN erp_ast_disposal.REASON IS '处置原因';
                    
      COMMENT ON COLUMN erp_ast_disposal.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_disposal.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_disposal.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_disposal.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_disposal.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_disposal.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_disposal.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_disposal.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_disposal.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_disposal.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_disposal.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_disposal.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_disposal.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_disposal.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ast_disposal.NOP_FLOW_ID IS '工作流实例';
                    
      COMMENT ON COLUMN erp_ast_disposal.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_disposal.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_cip IS '在建工程';
                
      COMMENT ON COLUMN erp_ast_cip.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_cip.CODE IS '工程编码';
                    
      COMMENT ON COLUMN erp_ast_cip.NAME IS '工程名称';
                    
      COMMENT ON COLUMN erp_ast_cip.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_cip.CATEGORY_ID IS '预定资产类别(完工后转入)';
                    
      COMMENT ON COLUMN erp_ast_cip.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_cip.BUSINESS_DATE IS '开工日期';
                    
      COMMENT ON COLUMN erp_ast_cip.ESTIMATED_COMPLETION_DATE IS '预计完工日期';
                    
      COMMENT ON COLUMN erp_ast_cip.ACCUMULATED_COST IS '累计归集成本';
                    
      COMMENT ON COLUMN erp_ast_cip.IS_COMPLETED IS '是否已转固';
                    
      COMMENT ON COLUMN erp_ast_cip.COMPLETED_ASSET_ID IS '完工转出资产';
                    
      COMMENT ON COLUMN erp_ast_cip.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_ast_cip.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_cip.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_cip.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_cip.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_cip.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_cip.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_cip.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ast_cip.PROJECT_ID IS '关联项目(可空)';
                    
      COMMENT ON COLUMN erp_ast_cip.CIP_ASSET_CATEGORY_SNAPSHOT IS '转固时类别快照';
                    
      COMMENT ON COLUMN erp_ast_cip.POSTED IS '是否已过账';
                    
      COMMENT ON COLUMN erp_ast_cip.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_cip.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_cip.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_cip.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_cip.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_split IS '资产拆分单';
                
      COMMENT ON COLUMN erp_ast_split.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_split.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_ast_split.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_split.SOURCE_ASSET_ID IS '被拆分资产';
                    
      COMMENT ON COLUMN erp_ast_split.BUSINESS_DATE IS '拆分日期';
                    
      COMMENT ON COLUMN erp_ast_split.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_split.SPLIT_REASON IS '拆分原因';
                    
      COMMENT ON COLUMN erp_ast_split.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_split.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_split.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_split.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_split.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_split.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_split.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_split.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_split.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_split.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_split.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_split.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_split.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_split.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ast_split.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_split.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_split.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_merge IS '资产合并单';
                
      COMMENT ON COLUMN erp_ast_merge.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_merge.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_ast_merge.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_merge.TARGET_ASSET_ID IS '合并目标资产';
                    
      COMMENT ON COLUMN erp_ast_merge.BUSINESS_DATE IS '合并日期';
                    
      COMMENT ON COLUMN erp_ast_merge.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_merge.MERGE_REASON IS '合并原因';
                    
      COMMENT ON COLUMN erp_ast_merge.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_merge.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_merge.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_merge.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_merge.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_merge.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_merge.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_merge.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_merge.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_merge.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_merge.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_merge.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_merge.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_merge.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ast_merge.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_merge.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_merge.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_maintenance IS '资产维修工单';
                
      COMMENT ON COLUMN erp_ast_maintenance.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_maintenance.CODE IS '维修单号';
                    
      COMMENT ON COLUMN erp_ast_maintenance.NAME IS '维修名称';
                    
      COMMENT ON COLUMN erp_ast_maintenance.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_maintenance.ASSET_ID IS '资产卡片';
                    
      COMMENT ON COLUMN erp_ast_maintenance.MAINTENANCE_VISIT_ID IS '维护工单(弱关联)';
                    
      COMMENT ON COLUMN erp_ast_maintenance.STATUS IS '维修状态';
                    
      COMMENT ON COLUMN erp_ast_maintenance.TREATMENT IS '处置裁决';
                    
      COMMENT ON COLUMN erp_ast_maintenance.CAPITALIZED_AMOUNT IS '资本化金额';
                    
      COMMENT ON COLUMN erp_ast_maintenance.TOTAL_COST_AMOUNT IS '费用合计';
                    
      COMMENT ON COLUMN erp_ast_maintenance.BUSINESS_DATE IS '维修日期';
                    
      COMMENT ON COLUMN erp_ast_maintenance.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_maintenance.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_maintenance.REASON IS '维修原因';
                    
      COMMENT ON COLUMN erp_ast_maintenance.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_maintenance.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_maintenance.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_maintenance.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance.REVERSED IS '已红冲';
                    
      COMMENT ON COLUMN erp_ast_maintenance.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_maintenance.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON COLUMN erp_ast_maintenance.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_maintenance.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_maintenance.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_maintenance.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_maintenance.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_ast_inventory_line IS '资产盘点行';
                
      COMMENT ON COLUMN erp_ast_inventory_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.INVENTORY_ID IS '盘点单';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.ASSET_ID IS '账面资产';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.ASSET_CODE_SNAPSHOT IS '资产编码快照';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.ASSET_NAME_SNAPSHOT IS '资产名称快照';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.CATEGORY_ID IS '资产类别';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.BOOK_QUANTITY IS '账面数量';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.ACTUAL_QUANTITY IS '实盘数量';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.VARIANCE_QUANTITY IS '差异数量';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.VARIANCE_TYPE IS '差异类型';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.BOOK_VALUE IS '账面价值';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.ASSESSED_VALUE IS '评估价值';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.VARIANCE_AMOUNT IS '差异金额';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.DISPOSITION IS '差异处置';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.NEW_ASSET_ID IS '盘盈新建资产';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.CAPITALIZATION_ID IS '盘盈资本化单';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.DISPOSAL_ID IS '盘亏处置单';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.INVESTIGATED_REMARK IS '调查备注';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_ast_cip_cost_item IS 'CIP成本归集行';
                
      COMMENT ON COLUMN erp_ast_cip_cost_item.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.CIP_ID IS '在建工程';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.COST_TYPE IS '成本类型';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.SOURCE_BILL_TYPE IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.SOURCE_BILL_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.POSTED_TRANSFER_FLAG IS '已转固标记';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.CAPITALIZATION_ID IS '转固资本化单';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_ast_cip_progress_billing IS 'CIP进度付款';
                
      COMMENT ON COLUMN erp_ast_cip_progress_billing.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.CIP_ID IS '在建工程';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.BILLING_DATE IS '付款日期';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.BILLING_MILESTONE IS '里程碑';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.PAYMENT_VOUCHER_CODE IS '付款凭证号';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.PAID_FLAG IS '已付标记';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_ast_split_line IS '资产拆分行';
                
      COMMENT ON COLUMN erp_ast_split_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_split_line.SPLIT_ID IS '拆分单';
                    
      COMMENT ON COLUMN erp_ast_split_line.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_split_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_ast_split_line.TARGET_ASSET_CODE IS '目标资产编码';
                    
      COMMENT ON COLUMN erp_ast_split_line.TARGET_ASSET_NAME IS '目标资产名称';
                    
      COMMENT ON COLUMN erp_ast_split_line.CATEGORY_ID IS '目标资产类别';
                    
      COMMENT ON COLUMN erp_ast_split_line.ALLOCATION_METHOD IS '分摊方式';
                    
      COMMENT ON COLUMN erp_ast_split_line.PROPORTION IS '比例';
                    
      COMMENT ON COLUMN erp_ast_split_line.ORIGINAL_COST_AMOUNT IS '原值金额';
                    
      COMMENT ON COLUMN erp_ast_split_line.ACCUMULATED_DEPRECIATION_AMOUNT IS '累计折旧金额';
                    
      COMMENT ON COLUMN erp_ast_split_line.NET_BOOK_VALUE IS '账面净值';
                    
      COMMENT ON COLUMN erp_ast_split_line.TARGET_ASSET_ID IS '目标资产';
                    
      COMMENT ON COLUMN erp_ast_split_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_split_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_split_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_split_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_split_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_split_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_split_line.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_ast_merge_line IS '资产合并行';
                
      COMMENT ON COLUMN erp_ast_merge_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_merge_line.MERGE_ID IS '合并单';
                    
      COMMENT ON COLUMN erp_ast_merge_line.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_merge_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_ast_merge_line.SOURCE_ASSET_ID IS '源资产';
                    
      COMMENT ON COLUMN erp_ast_merge_line.CONTRIBUTION_PROPORTION IS '贡献比例';
                    
      COMMENT ON COLUMN erp_ast_merge_line.ORIGINAL_COST_AMOUNT IS '原值金额';
                    
      COMMENT ON COLUMN erp_ast_merge_line.ACCUMULATED_DEPRECIATION_AMOUNT IS '累计折旧金额';
                    
      COMMENT ON COLUMN erp_ast_merge_line.NET_BOOK_VALUE IS '账面净值';
                    
      COMMENT ON COLUMN erp_ast_merge_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_merge_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_merge_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_merge_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_merge_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_merge_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_merge_line.REMARK IS '备注';
                    
      COMMENT ON TABLE erp_ast_maintenance_cost IS '维修费用行';
                
      COMMENT ON COLUMN erp_ast_maintenance_cost.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.MAINTENANCE_ID IS '维修工单';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.COST_TYPE IS '费用类型';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.REMARK IS '备注';
                    
