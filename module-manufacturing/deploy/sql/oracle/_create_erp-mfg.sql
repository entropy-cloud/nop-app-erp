
CREATE TABLE erp_md_material(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  MATERIAL_TYPE INTEGER  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_material primary key (ID)
);

CREATE TABLE erp_md_material_sku(
  ID NUMBER(20)  ,
  MATERIAL_ID NUMBER(20)  ,
  SKU_CODE VARCHAR2(50)  ,
  BARCODE VARCHAR2(50)  ,
  constraint PK_erp_md_material_sku primary key (ID)
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

CREATE TABLE erp_mfg_workcenter(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  CAPACITY NUMBER(16,4)  ,
  CAPACITY_UNIT VARCHAR2(20)  ,
  HOURLY_RATE NUMBER(16,4)  ,
  WORK_HOURS_PER_DAY NUMBER(8,2)  ,
  IS_EXTERNAL CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_workcenter primary key (ID)
);

CREATE TABLE erp_mfg_routing(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200)  ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_routing primary key (ID)
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

CREATE TABLE erp_md_currency(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  SYMBOL VARCHAR2(50)  ,
  DECIMAL_PLACES INTEGER  ,
  IS_FUNCTIONAL CHAR(1)  ,
  constraint PK_erp_md_currency primary key (ID)
);

CREATE TABLE erp_md_location(
  ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARENT_ID NUMBER(20)  ,
  constraint PK_erp_md_location primary key (ID)
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

CREATE TABLE erp_md_employee(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_employee primary key (ID)
);

CREATE TABLE erp_inv_batch(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  constraint PK_erp_inv_batch primary key (ID)
);

CREATE TABLE erp_mfg_bom(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  PRODUCT_ID NUMBER(20) NOT NULL ,
  BOM_TYPE VARCHAR2(20) NOT NULL ,
  CONSUMPTION VARCHAR2(20)  ,
  IS_ACTIVE CHAR(1) default 1   ,
  IS_DEFAULT CHAR(1) default 0   ,
  USE_MULTI_LEVEL_BOM CHAR(1) default 0   ,
  INSPECTION_REQUIRED CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  VERSION_LABEL VARCHAR2(50)  ,
  QTY NUMBER(20,4) default 1   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_bom primary key (ID)
);

CREATE TABLE erp_mfg_routing_operation(
  ID NUMBER(20) NOT NULL ,
  ROUTING_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  OPERATION_CODE VARCHAR2(50)  ,
  OPERATION_NAME VARCHAR2(200)  ,
  WORKCENTER_ID NUMBER(20)  ,
  STANDARD_TIME NUMBER(12,2)  ,
  TIME_UNIT VARCHAR2(20)  ,
  SETUP_TIME NUMBER(12,2)  ,
  RUN_TIME NUMBER(12,2)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_routing_operation primary key (ID)
);

CREATE TABLE erp_mfg_workcenter_calendar(
  ID NUMBER(20) NOT NULL ,
  WORKCENTER_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CALENDAR_NAME VARCHAR2(200) NOT NULL ,
  SHIFT_TYPE VARCHAR2(20) NOT NULL ,
  WORK_DATE_PATTERN VARCHAR2(20)  ,
  START_TIME VARCHAR2(8)  ,
  END_TIME VARCHAR2(8)  ,
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
  constraint PK_erp_mfg_workcenter_calendar primary key (ID)
);

CREATE TABLE erp_mfg_workcenter_capacity(
  ID NUMBER(20) NOT NULL ,
  WORKCENTER_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  CAPACITY_PER_HOUR NUMBER(16,4) NOT NULL ,
  SETUP_TIME NUMBER(12,2) default 0   ,
  CLEANUP_TIME NUMBER(12,2) default 0   ,
  EFFICIENCY_FACTOR NUMBER(10,4) default 1   ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_workcenter_capacity primary key (ID)
);

CREATE TABLE erp_mfg_mrp_plan(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  PLANNING_HORIZON_DAYS INTEGER  ,
  STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_mrp_plan primary key (ID)
);

CREATE TABLE erp_mfg_forecast(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PLAN_NAME VARCHAR2(200) NOT NULL ,
  PERIOD_FROM DATE NOT NULL ,
  PERIOD_TO DATE NOT NULL ,
  STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_forecast primary key (ID)
);

CREATE TABLE erp_mfg_cost_rollup(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  COSTING_VERSION VARCHAR2(50)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_cost_rollup primary key (ID)
);

CREATE TABLE erp_mfg_bom_byproduct(
  ID NUMBER(20) NOT NULL ,
  BOM_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  BYPRODUCT_TYPE VARCHAR2(20)  ,
  YIELD_RATE NUMBER(10,4) default 100   ,
  COST_ALLOCATION_PERCENT NUMBER(10,4) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_bom_byproduct primary key (ID)
);

CREATE TABLE erp_mfg_production_version(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  PRODUCT_ID NUMBER(20) NOT NULL ,
  BOM_ID NUMBER(20) NOT NULL ,
  ROUTING_ID NUMBER(20) NOT NULL ,
  VALID_FROM DATE  ,
  VALID_TO DATE  ,
  LOT_SIZE_FROM NUMBER(20,4)  ,
  LOT_SIZE_TO NUMBER(20,4)  ,
  IS_DEFAULT CHAR(1) default 0   ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_production_version primary key (ID)
);

CREATE TABLE erp_mfg_bom_operation(
  ID NUMBER(20) NOT NULL ,
  BOM_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  OPERATION_ID NUMBER(20) NOT NULL ,
  WORKCENTER_ID NUMBER(20)  ,
  STANDARD_TIME NUMBER(12,2)  ,
  TIME_UNIT VARCHAR2(20)  ,
  RATE NUMBER(10,4) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_bom_operation primary key (ID)
);

CREATE TABLE erp_mfg_mrp_plan_line(
  ID NUMBER(20) NOT NULL ,
  MRP_PLAN_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  ORDER_TYPE VARCHAR2(20) NOT NULL ,
  GROSS_REQUIREMENT NUMBER(20,4) NOT NULL ,
  SCHEDULED_RECEIPT NUMBER(20,4) default 0   ,
  ON_HAND NUMBER(20,4) default 0   ,
  NET_REQUIREMENT NUMBER(20,4) NOT NULL ,
  PLANNED_QUANTITY NUMBER(20,4) NOT NULL ,
  PLANNED_DATE DATE NOT NULL ,
  PARENT_LINE_ID NUMBER(20)  ,
  IS_FIRMED CHAR(1) default 0   ,
  CONVERTED_BILL_CODE VARCHAR2(50)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_mrp_plan_line primary key (ID)
);

CREATE TABLE erp_mfg_mrp_demand(
  ID NUMBER(20) NOT NULL ,
  MRP_PLAN_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  DEMAND_SOURCE VARCHAR2(20) NOT NULL ,
  SOURCE_BILL_TYPE VARCHAR2(50)  ,
  SOURCE_BILL_CODE VARCHAR2(50)  ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  REQUIREMENT_DATE DATE NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_mrp_demand primary key (ID)
);

CREATE TABLE erp_mfg_forecast_line(
  ID NUMBER(20) NOT NULL ,
  FORECAST_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  PERIOD_START DATE NOT NULL ,
  PERIOD_END DATE NOT NULL ,
  FORECAST_QTY NUMBER(20,4) NOT NULL ,
  SOURCED_FLAG VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_forecast_line primary key (ID)
);

CREATE TABLE erp_mfg_cost_rollup_line(
  ID NUMBER(20) NOT NULL ,
  COST_ROLLUP_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  MATERIAL_COST NUMBER(20,4) default 0   ,
  LABOR_COST NUMBER(20,4) default 0   ,
  OVERHEAD_COST NUMBER(20,4) default 0   ,
  SUBCONTRACT_COST NUMBER(20,4) default 0   ,
  TOTAL_COST NUMBER(20,4) default 0   ,
  UNIT_COST NUMBER(20,4) default 0   ,
  CURRENCY_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_cost_rollup_line primary key (ID)
);

CREATE TABLE erp_mfg_work_order(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BOM_ID NUMBER(20)  ,
  ROUTING_ID NUMBER(20)  ,
  PRODUCTION_VERSION_ID NUMBER(20)  ,
  SOURCE_MRP_PLAN_ID NUMBER(20)  ,
  SOURCE_ORDER_TYPE VARCHAR2(50)  ,
  SOURCE_ORDER_CODE VARCHAR2(50)  ,
  PRODUCT_ID NUMBER(20) NOT NULL ,
  PLANNED_QUANTITY NUMBER(20,4) NOT NULL ,
  COMPLETED_QUANTITY NUMBER(20,4) default 0   ,
  SCRAPPED_QUANTITY NUMBER(20,4) default 0   ,
  BUSINESS_DATE DATE NOT NULL ,
  PLANNED_START_DATE DATE  ,
  PLANNED_END_DATE DATE  ,
  ACTUAL_START_DATE DATE  ,
  ACTUAL_END_DATE DATE  ,
  CURRENCY_ID NUMBER(20)  ,
  MATERIAL_COST NUMBER(20,4) default 0   ,
  LABOR_COST NUMBER(20,4) default 0   ,
  OVERHEAD_COST NUMBER(20,4) default 0   ,
  SUBCONTRACT_COST NUMBER(20,4) default 0   ,
  TOTAL_COST NUMBER(20,4) default 0   ,
  UNIT_COST NUMBER(20,4) default 0   ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20)  ,
  PRIORITY VARCHAR2(20)  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1  NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_mfg_work_order primary key (ID)
);

CREATE TABLE erp_mfg_bom_line(
  ID NUMBER(20) NOT NULL ,
  BOM_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  OPERATION_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  SCRAP_RATE NUMBER(10,4) default 0   ,
  WAREHOUSE_ID NUMBER(20)  ,
  ALTERNATIVE_MATERIAL_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_bom_line primary key (ID)
);

CREATE TABLE erp_mfg_crp_load(
  ID NUMBER(20) NOT NULL ,
  WORKCENTER_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  WORK_ORDER_ID NUMBER(20)  ,
  LOAD_DATE DATE NOT NULL ,
  LOAD_HOURS NUMBER(12,2) default 0   ,
  SETUP_HOURS NUMBER(12,2) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_crp_load primary key (ID)
);

CREATE TABLE erp_mfg_work_order_line(
  ID NUMBER(20) NOT NULL ,
  WORK_ORDER_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  LINE_TYPE VARCHAR2(20) NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  PLANNED_QUANTITY NUMBER(20,4) NOT NULL ,
  ACTUAL_QUANTITY NUMBER(20,4) default 0   ,
  SCRAPPED_QUANTITY NUMBER(20,4) default 0   ,
  SOURCE_WAREHOUSE_ID NUMBER(20)  ,
  DEST_WAREHOUSE_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_work_order_line primary key (ID)
);

CREATE TABLE erp_mfg_subcontract_order(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  WORK_ORDER_ID NUMBER(20)  ,
  SUPPLIER_ID NUMBER(20) NOT NULL ,
  WORKCENTER_ID NUMBER(20)  ,
  ROUTING_ID NUMBER(20)  ,
  PRODUCTION_VERSION_ID NUMBER(20)  ,
  PRODUCT_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  CURRENCY_ID NUMBER(20) NOT NULL ,
  EXCHANGE_RATE NUMBER(20,8) default 1  NOT NULL ,
  PROCESSING_FEE NUMBER(20,4) default 0   ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_STATUS VARCHAR2(20) NOT NULL ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  REMARK VARCHAR2(1000)  ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_subcontract_order primary key (ID)
);

CREATE TABLE erp_mfg_job_card(
  ID NUMBER(20) NOT NULL ,
  WORK_ORDER_ID NUMBER(20) NOT NULL ,
  OPERATION_ID NUMBER(20)  ,
  LINE_NO INTEGER NOT NULL ,
  PLANNED_QUANTITY NUMBER(20,4) NOT NULL ,
  COMPLETED_QUANTITY NUMBER(20,4) default 0   ,
  SCRAPPED_QUANTITY NUMBER(20,4) default 0   ,
  STATUS VARCHAR2(30) NOT NULL ,
  WORKCENTER_ID NUMBER(20)  ,
  ACTUAL_START_TIME DATE  ,
  ACTUAL_END_TIME DATE  ,
  REMARK VARCHAR2(1000)  ,
  CODE VARCHAR2(50)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_job_card primary key (ID)
);

CREATE TABLE erp_mfg_cost_variance(
  ID NUMBER(20) NOT NULL ,
  WORK_ORDER_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  VARIANCE_TYPE VARCHAR2(50) NOT NULL ,
  COST_ELEMENT VARCHAR2(50) NOT NULL ,
  MATERIAL_ID NUMBER(20)  ,
  OPERATION_ID NUMBER(20)  ,
  STANDARD_AMOUNT NUMBER(20,4) default 0   ,
  ACTUAL_AMOUNT NUMBER(20,4) default 0   ,
  VARIANCE_AMOUNT NUMBER(20,4) default 0   ,
  VARIANCE_PERCENT NUMBER(10,4) default 0   ,
  STANDARD_QTY NUMBER(20,4) default 0   ,
  ACTUAL_QTY NUMBER(20,4) default 0   ,
  STANDARD_PRICE NUMBER(20,4) default 0   ,
  ACTUAL_PRICE NUMBER(20,4) default 0   ,
  WORKCENTER_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  POSTED CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_cost_variance primary key (ID)
);

CREATE TABLE erp_mfg_subcontract_order_line(
  ID NUMBER(20) NOT NULL ,
  SUBCONTRACT_ORDER_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_PROCESSING_FEE NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_subcontract_order_line primary key (ID)
);

CREATE TABLE erp_mfg_material_issue(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  WORK_ORDER_ID NUMBER(20) NOT NULL ,
  JOB_CARD_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,8) default 1  NOT NULL ,
  AMOUNT_SOURCE NUMBER(20,4) default 0   ,
  AMOUNT_FUNCTIONAL NUMBER(20,4) default 0   ,
  constraint PK_erp_mfg_material_issue primary key (ID)
);

CREATE TABLE erp_mfg_job_card_time_log(
  ID NUMBER(20) NOT NULL ,
  JOB_CARD_ID NUMBER(20) NOT NULL ,
  WORK_ORDER_ID NUMBER(20) NOT NULL ,
  OPERATOR_ID VARCHAR2(36) NOT NULL ,
  WORK_DATE DATE NOT NULL ,
  START_TIME DATE  ,
  END_TIME DATE  ,
  DURATION_MINS NUMBER(12,2) NOT NULL ,
  SETUP_MINS NUMBER(12,2) default 0   ,
  RUN_MINS NUMBER(12,2) default 0   ,
  COMPLETED_QUANTITY NUMBER(20,4) default 0   ,
  SCRAPPED_QUANTITY NUMBER(20,4) default 0   ,
  HOURLY_RATE NUMBER(16,4)  ,
  LABOR_COST NUMBER(20,4) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_job_card_time_log primary key (ID)
);

CREATE TABLE erp_mfg_batch_genealogy(
  ID NUMBER(20) NOT NULL ,
  WORK_ORDER_ID NUMBER(20) NOT NULL ,
  JOB_CARD_ID NUMBER(20)  ,
  OPERATION_ID NUMBER(20)  ,
  INPUT_LOT_ID NUMBER(20) NOT NULL ,
  INPUT_MATERIAL_ID NUMBER(20) NOT NULL ,
  INPUT_QTY NUMBER(20,4) NOT NULL ,
  INPUT_UO_M_ID NUMBER(20) NOT NULL ,
  OUTPUT_LOT_ID NUMBER(20) NOT NULL ,
  OUTPUT_MATERIAL_ID NUMBER(20) NOT NULL ,
  OUTPUT_QTY NUMBER(20,4) NOT NULL ,
  OUTPUT_UO_M_ID NUMBER(20) NOT NULL ,
  PRODUCTION_DATE DATE NOT NULL ,
  PRODUCTION_TIME DATE  ,
  LINE_NO INTEGER NOT NULL ,
  LOT_STATUS VARCHAR2(50)  ,
  IS_INPUT_CONSUMED CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_batch_genealogy primary key (ID)
);

CREATE TABLE erp_mfg_material_issue_line(
  ID NUMBER(20) NOT NULL ,
  ISSUE_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  WORK_ORDER_LINE_ID NUMBER(20)  ,
  REQUIRED_QUANTITY NUMBER(20,4) NOT NULL ,
  ISSUED_QUANTITY NUMBER(20,4) default 0   ,
  UNIT_COST NUMBER(20,4)  ,
  TOTAL_COST NUMBER(20,4)  ,
  BATCH_NO VARCHAR2(50)  ,
  LOCATION_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_material_issue_line primary key (ID)
);


      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_material_sku IS '物料SKU';
                
      COMMENT ON TABLE erp_md_uom IS '计量单位';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_mfg_workcenter IS '工作中心';
                
      COMMENT ON COLUMN erp_mfg_workcenter.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.CODE IS '工作中心编码';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.NAME IS '工作中心名称';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.CAPACITY IS '产能';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.CAPACITY_UNIT IS '产能单位';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.HOURLY_RATE IS '小时费率';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.WORK_HOURS_PER_DAY IS '日工时';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.IS_EXTERNAL IS '是否外协';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_routing IS '工艺路线';
                
      COMMENT ON COLUMN erp_mfg_routing.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_routing.CODE IS '工艺路线编码';
                    
      COMMENT ON COLUMN erp_mfg_routing.NAME IS '工艺路线名称';
                    
      COMMENT ON COLUMN erp_mfg_routing.IS_ACTIVE IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_routing.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_routing.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_routing.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_routing.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_routing.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_routing.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_routing.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_employee IS '员工';
                
      COMMENT ON TABLE erp_inv_batch IS '批次';
                
      COMMENT ON TABLE erp_mfg_bom IS 'BOM';
                
      COMMENT ON COLUMN erp_mfg_bom.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_bom.CODE IS 'BOM编码';
                    
      COMMENT ON COLUMN erp_mfg_bom.PRODUCT_ID IS '产品';
                    
      COMMENT ON COLUMN erp_mfg_bom.BOM_TYPE IS 'BOM类型';
                    
      COMMENT ON COLUMN erp_mfg_bom.CONSUMPTION IS '消耗控制';
                    
      COMMENT ON COLUMN erp_mfg_bom.IS_ACTIVE IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_bom.IS_DEFAULT IS '是否默认';
                    
      COMMENT ON COLUMN erp_mfg_bom.USE_MULTI_LEVEL_BOM IS '展开多层BOM';
                    
      COMMENT ON COLUMN erp_mfg_bom.INSPECTION_REQUIRED IS '需要质检';
                    
      COMMENT ON COLUMN erp_mfg_bom.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_bom.VERSION_LABEL IS '版本号';
                    
      COMMENT ON COLUMN erp_mfg_bom.QTY IS 'BOM数量';
                    
      COMMENT ON COLUMN erp_mfg_bom.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_bom.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_bom.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_bom.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_bom.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_bom.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_routing_operation IS '工艺路线工序';
                
      COMMENT ON COLUMN erp_mfg_routing_operation.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.ROUTING_ID IS '工艺路线ID';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.OPERATION_CODE IS '工序编码';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.OPERATION_NAME IS '工序名称';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.WORKCENTER_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.STANDARD_TIME IS '标准工时';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.TIME_UNIT IS '时间单位';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.SETUP_TIME IS '准备时间';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.RUN_TIME IS '加工时间';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_workcenter_calendar IS '工作中心日历';
                
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.WORKCENTER_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.CALENDAR_NAME IS '日历名称';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.SHIFT_TYPE IS '班次类型';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.WORK_DATE_PATTERN IS '工作日模式';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.START_TIME IS '班次开始时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.END_TIME IS '班次结束时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.EFFECTIVE_FROM IS '生效起始日期';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.EFFECTIVE_TO IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.IS_ACTIVE IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_workcenter_capacity IS '工作中心产能';
                
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.WORKCENTER_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.MATERIAL_ID IS '关联物料';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.CAPACITY_PER_HOUR IS '每小时产能';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.SETUP_TIME IS '换模时间(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.CLEANUP_TIME IS '清理时间(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.EFFICIENCY_FACTOR IS '效率系数';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.IS_ACTIVE IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_mrp_plan IS 'MRP计划';
                
      COMMENT ON COLUMN erp_mfg_mrp_plan.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.CODE IS '计划号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.BUSINESS_DATE IS '计划日期';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.PLANNING_HORIZON_DAYS IS '计划区间(天)';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_forecast IS '需求预测';
                
      COMMENT ON COLUMN erp_mfg_forecast.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_forecast.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_mfg_forecast.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_forecast.PLAN_NAME IS '预测名称';
                    
      COMMENT ON COLUMN erp_mfg_forecast.PERIOD_FROM IS '预测区间起';
                    
      COMMENT ON COLUMN erp_mfg_forecast.PERIOD_TO IS '预测区间止';
                    
      COMMENT ON COLUMN erp_mfg_forecast.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_forecast.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_forecast.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_forecast.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_forecast.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_forecast.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_forecast.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_forecast.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_cost_rollup IS '标准成本滚算';
                
      COMMENT ON COLUMN erp_mfg_cost_rollup.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.BUSINESS_DATE IS '计算日期';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.COSTING_VERSION IS '成本核算版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_bom_byproduct IS 'BOM联副产品';
                
      COMMENT ON COLUMN erp_mfg_bom_byproduct.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.BOM_ID IS 'BOM ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.BYPRODUCT_TYPE IS '联副产品类型';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.YIELD_RATE IS '产出率(%)';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.COST_ALLOCATION_PERCENT IS '成本分摊比例(%)';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_production_version IS '生产版本';
                
      COMMENT ON COLUMN erp_mfg_production_version.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_production_version.CODE IS '版本编码';
                    
      COMMENT ON COLUMN erp_mfg_production_version.PRODUCT_ID IS '产品';
                    
      COMMENT ON COLUMN erp_mfg_production_version.BOM_ID IS 'BOM';
                    
      COMMENT ON COLUMN erp_mfg_production_version.ROUTING_ID IS '工艺路线';
                    
      COMMENT ON COLUMN erp_mfg_production_version.VALID_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_mfg_production_version.VALID_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_mfg_production_version.LOT_SIZE_FROM IS '批量下限';
                    
      COMMENT ON COLUMN erp_mfg_production_version.LOT_SIZE_TO IS '批量上限';
                    
      COMMENT ON COLUMN erp_mfg_production_version.IS_DEFAULT IS '是否默认版本';
                    
      COMMENT ON COLUMN erp_mfg_production_version.IS_ACTIVE IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_production_version.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_production_version.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_production_version.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_production_version.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_production_version.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_production_version.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_production_version.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_bom_operation IS 'BOM工艺';
                
      COMMENT ON COLUMN erp_mfg_bom_operation.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.BOM_ID IS 'BOM ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.OPERATION_ID IS '工序ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.WORKCENTER_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.STANDARD_TIME IS '标准工时';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.TIME_UNIT IS '时间单位';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.RATE IS '效率系数';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_mrp_plan_line IS 'MRP计划行';
                
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.MRP_PLAN_ID IS 'MRP计划ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.ORDER_TYPE IS '建议类型';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.GROSS_REQUIREMENT IS '毛需求';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.SCHEDULED_RECEIPT IS '预计入库';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.ON_HAND IS '现有库存';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.NET_REQUIREMENT IS '净需求';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.PLANNED_QUANTITY IS '计划数量';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.PLANNED_DATE IS '计划到货/开工日期';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.PARENT_LINE_ID IS '父需求行(多级 BOM 展开)';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.IS_FIRMED IS '是否已转正式单据';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.CONVERTED_BILL_CODE IS '转换后单据号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_mrp_demand IS 'MRP独立需求';
                
      COMMENT ON COLUMN erp_mfg_mrp_demand.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.MRP_PLAN_ID IS 'MRP计划ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.DEMAND_SOURCE IS '需求来源';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.SOURCE_BILL_TYPE IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.SOURCE_BILL_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.QUANTITY IS '需求数量';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.REQUIREMENT_DATE IS '需求日期';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_forecast_line IS '需求预测行';
                
      COMMENT ON COLUMN erp_mfg_forecast_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.FORECAST_ID IS '需求预测ID';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.WAREHOUSE_ID IS '仓库（可选，空=产品级 MRP 消费，填=仓级 DRP 消费）';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.PERIOD_START IS '桶起始日期';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.PERIOD_END IS '桶结束日期';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.FORECAST_QTY IS '预测数量';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.SOURCED_FLAG IS '来源标记';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_cost_rollup_line IS '标准成本滚算行';
                
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.COST_ROLLUP_ID IS '滚算单ID';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.MATERIAL_ID IS '产品';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.MATERIAL_COST IS '材料成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.LABOR_COST IS '人工成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.OVERHEAD_COST IS '制造费用';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.SUBCONTRACT_COST IS '委外成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.TOTAL_COST IS '总成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.UNIT_COST IS '单位标准成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_work_order IS '工单';
                
      COMMENT ON COLUMN erp_mfg_work_order.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_work_order.CODE IS '工单号';
                    
      COMMENT ON COLUMN erp_mfg_work_order.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_work_order.BOM_ID IS 'BOM';
                    
      COMMENT ON COLUMN erp_mfg_work_order.ROUTING_ID IS '工艺路线';
                    
      COMMENT ON COLUMN erp_mfg_work_order.PRODUCTION_VERSION_ID IS '生产版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.SOURCE_MRP_PLAN_ID IS '来源 MRP 计划';
                    
      COMMENT ON COLUMN erp_mfg_work_order.SOURCE_ORDER_TYPE IS '来源单据类型(SALES_ORDER/FORECAST/MANUAL)';
                    
      COMMENT ON COLUMN erp_mfg_work_order.SOURCE_ORDER_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_mfg_work_order.PRODUCT_ID IS '产品(主产出)';
                    
      COMMENT ON COLUMN erp_mfg_work_order.PLANNED_QUANTITY IS '计划数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order.COMPLETED_QUANTITY IS '完工数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order.SCRAPPED_QUANTITY IS '报废数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order.BUSINESS_DATE IS '工单日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.PLANNED_START_DATE IS '计划开工日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.PLANNED_END_DATE IS '计划完工日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.ACTUAL_START_DATE IS '实际开工日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.ACTUAL_END_DATE IS '实际完工日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_mfg_work_order.MATERIAL_COST IS '材料成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.LABOR_COST IS '人工成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.OVERHEAD_COST IS '制造费用';
                    
      COMMENT ON COLUMN erp_mfg_work_order.SUBCONTRACT_COST IS '委外成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.TOTAL_COST IS '总成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.UNIT_COST IS '单位成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_mfg_work_order.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_mfg_work_order.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_mfg_work_order.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_mfg_work_order.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_mfg_work_order.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_mfg_work_order.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_work_order.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_work_order.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_work_order.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_work_order.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_mfg_work_order.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_mfg_work_order.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_mfg_work_order.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_mfg_bom_line IS 'BOM行';
                
      COMMENT ON COLUMN erp_mfg_bom_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.BOM_ID IS 'BOM ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.OPERATION_ID IS '工序ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.SCRAP_RATE IS '损耗率(%)';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.WAREHOUSE_ID IS '发货仓库';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.ALTERNATIVE_MATERIAL_ID IS '替代物料';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_crp_load IS 'CRP负荷';
                
      COMMENT ON COLUMN erp_mfg_crp_load.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.WORKCENTER_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.WORK_ORDER_ID IS '工单(弱指针)';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.LOAD_DATE IS '负荷日期';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.LOAD_HOURS IS '占用工时';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.SETUP_HOURS IS '换模工时';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_work_order_line IS '工单行';
                
      COMMENT ON COLUMN erp_mfg_work_order_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.WORK_ORDER_ID IS '工单ID';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.LINE_TYPE IS '行类型(OUTPUT 产出/INPUT 投入/BYPRODUCT 联副)';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.PLANNED_QUANTITY IS '计划数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.ACTUAL_QUANTITY IS '实际数量(完工/领用)';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.SCRAPPED_QUANTITY IS '报废数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.SOURCE_WAREHOUSE_ID IS '领料仓库(投入用)';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.DEST_WAREHOUSE_ID IS '入库仓库(产出用)';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_subcontract_order IS '委外加工单';
                
      COMMENT ON COLUMN erp_mfg_subcontract_order.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.WORK_ORDER_ID IS '来源工单';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.SUPPLIER_ID IS '委外供应商';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.WORKCENTER_ID IS '外协工作中心';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.ROUTING_ID IS '工艺路线';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.PRODUCTION_VERSION_ID IS '生产版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.PRODUCT_ID IS '产品';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.BUSINESS_DATE IS '委外日期';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.PROCESSING_FEE IS '加工费';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.TOTAL_AMOUNT IS '合计金额';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.POSTED_STATUS IS '过账状态';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_job_card IS '作业卡';
                
      COMMENT ON COLUMN erp_mfg_job_card.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card.WORK_ORDER_ID IS '工单ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card.OPERATION_ID IS '工序ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_job_card.PLANNED_QUANTITY IS '计划数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card.COMPLETED_QUANTITY IS '完工数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card.SCRAPPED_QUANTITY IS '报废数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_job_card.WORKCENTER_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_job_card.ACTUAL_START_TIME IS '实际开始时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card.ACTUAL_END_TIME IS '实际结束时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_job_card.CODE IS '作业卡编号';
                    
      COMMENT ON COLUMN erp_mfg_job_card.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_job_card.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_job_card.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_job_card.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_job_card.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_cost_variance IS '成本差异记录';
                
      COMMENT ON COLUMN erp_mfg_cost_variance.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.WORK_ORDER_ID IS '工单';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.VARIANCE_TYPE IS '差异类型';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.COST_ELEMENT IS '成本要素';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.OPERATION_ID IS '工序';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.STANDARD_AMOUNT IS '标准金额';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.ACTUAL_AMOUNT IS '实际金额';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.VARIANCE_AMOUNT IS '差异金额';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.VARIANCE_PERCENT IS '差异百分比';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.STANDARD_QTY IS '标准数量';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.ACTUAL_QTY IS '实际数量';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.STANDARD_PRICE IS '标准单价';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.ACTUAL_PRICE IS '实际单价';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.WORKCENTER_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_subcontract_order_line IS '委外加工单行';
                
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.SUBCONTRACT_ORDER_ID IS '委外单ID';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.QUANTITY IS '委外数量';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.UNIT_PROCESSING_FEE IS '单位加工费';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_material_issue IS '领料单';
                
      COMMENT ON COLUMN erp_mfg_material_issue.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.WORK_ORDER_ID IS '工单';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.JOB_CARD_ID IS '作业卡';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.WAREHOUSE_ID IS '发料仓库';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.BUSINESS_DATE IS '领料日期';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.AMOUNT_SOURCE IS '源币种金额';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.AMOUNT_FUNCTIONAL IS '本位币金额';
                    
      COMMENT ON TABLE erp_mfg_job_card_time_log IS '作业工时记录';
                
      COMMENT ON COLUMN erp_mfg_job_card_time_log.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.JOB_CARD_ID IS '作业卡ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.WORK_ORDER_ID IS '工单ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.OPERATOR_ID IS '操作员(职员)';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.WORK_DATE IS '作业日期';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.START_TIME IS '开始时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.END_TIME IS '结束时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.DURATION_MINS IS '工时(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.SETUP_MINS IS '准备时间(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.RUN_MINS IS '加工时间(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.COMPLETED_QUANTITY IS '本班完工数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.SCRAPPED_QUANTITY IS '本班报废数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.HOURLY_RATE IS '小时费率';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.LABOR_COST IS '人工成本';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_batch_genealogy IS '批次追溯';
                
      COMMENT ON COLUMN erp_mfg_batch_genealogy.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.WORK_ORDER_ID IS '工单';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.JOB_CARD_ID IS '作业卡';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.OPERATION_ID IS '工序';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.INPUT_LOT_ID IS '输入批次';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.INPUT_MATERIAL_ID IS '输入物料';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.INPUT_QTY IS '投入数量';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.INPUT_UO_M_ID IS '投入计量单位';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.OUTPUT_LOT_ID IS '产出批次';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.OUTPUT_MATERIAL_ID IS '产出物料';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.OUTPUT_QTY IS '产出数量';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.OUTPUT_UO_M_ID IS '产出计量单位';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.PRODUCTION_DATE IS '生产日期';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.PRODUCTION_TIME IS '生产时间';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.LOT_STATUS IS '批次状态';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.IS_INPUT_CONSUMED IS '输入是否已消耗';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_material_issue_line IS '领料单行';
                
      COMMENT ON COLUMN erp_mfg_material_issue_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.ISSUE_ID IS '领料单ID';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.WORK_ORDER_LINE_ID IS '对应工单投入行';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.REQUIRED_QUANTITY IS '应领数量';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.ISSUED_QUANTITY IS '实领数量';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.UNIT_COST IS '单位成本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.TOTAL_COST IS '总成本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.LOCATION_ID IS '库位';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.UPDATE_TIME IS '修改时间';
                    
