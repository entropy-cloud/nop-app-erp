
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

CREATE TABLE erp_fin_voucher(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  constraint PK_erp_fin_voucher primary key (ID)
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
  DEPRECIATION_METHOD INTEGER  ,
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
  DEPRECIATION_METHOD INTEGER  ,
  DEPRECIATION_RATE NUMBER(10,6)  ,
  USEFUL_LIFE_MONTHS INTEGER  ,
  DEPARTMENT_ID NUMBER(20)  ,
  LOCATION_ID NUMBER(20)  ,
  EMPLOYEE_ID NUMBER(20)  ,
  STAFF_ID NUMBER(20)  ,
  BRAND_MODEL VARCHAR2(200)  ,
  STATUS INTEGER NOT NULL ,
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
  SOURCE_TYPE INTEGER NOT NULL ,
  SOURCE_CODE VARCHAR2(50)  ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
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
  BUSINESS_DATE DATE  ,
  EXCHANGE_RATE NUMBER(20,8) NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_asset_capitalization primary key (ID)
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
  STATUS INTEGER NOT NULL ,
  EXECUTED_AT DATE  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
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
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  DOC_VERSION VARCHAR2(20) default '1'  NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
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
  ADJUSTMENT_TYPE INTEGER NOT NULL ,
  ADJUSTMENT_AMOUNT NUMBER(20,4) NOT NULL ,
  REASON VARCHAR2(500)  ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
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
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_value_adjustment primary key (ID)
);

CREATE TABLE erp_ast_disposal(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ASSET_ID NUMBER(20) NOT NULL ,
  DISPOSAL_TYPE INTEGER NOT NULL ,
  DISPOSAL_AMOUNT NUMBER(20,4)  ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,8) default 1   ,
  BUSINESS_DATE DATE NOT NULL ,
  GAIN_LOSS NUMBER(20,4)  ,
  REASON INTEGER  ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
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
  STATUS INTEGER NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  REMARK VARCHAR2(1000)  ,
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
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
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
  EXCHANGE_RATE NUMBER(20,8) NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_split primary key (ID)
);

CREATE TABLE erp_ast_merge(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TARGET_ASSET_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  MERGE_REASON VARCHAR2(500)  ,
  DOC_STATUS INTEGER NOT NULL ,
  APPROVE_STATUS INTEGER NOT NULL ,
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
  EXCHANGE_RATE NUMBER(20,8) NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_ast_merge primary key (ID)
);


      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_subject IS '会计科目';
                
      COMMENT ON TABLE erp_fin_voucher IS '凭证';
                
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
                    
