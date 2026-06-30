
CREATE TABLE erp_md_md_organization(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_organization primary key (ID)
);

CREATE TABLE erp_drp_plan(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  PLAN_NAME VARCHAR2(200) NOT NULL ,
  PERIOD_FROM DATE NOT NULL ,
  PERIOD_TO DATE NOT NULL ,
  STATUS INTEGER NOT NULL ,
  TOTAL_REPLENISHMENT_QTY NUMBER(20,4)  ,
  RUN_AT DATE  ,
  RUN_BY VARCHAR2(50)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_drp_plan primary key (ID)
);

CREATE TABLE erp_drp_parameter(
  ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SAFETY_STOCK NUMBER(20,4)  ,
  REPLENISHMENT_LEAD_TIME INTEGER  ,
  ORDER_MULTIPLE NUMBER(20,4)  ,
  PREFERRED_SOURCE_WAREHOUSE_ID NUMBER(20)  ,
  PREFERRED_SUPPLIER_ID NUMBER(20)  ,
  REPLENISHMENT_METHOD INTEGER NOT NULL ,
  MIN_STOCK_LEVEL NUMBER(20,4)  ,
  MAX_STOCK_LEVEL NUMBER(20,4)  ,
  REVIEW_PERIOD_DAYS INTEGER  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_drp_parameter primary key (ID)
);

CREATE TABLE erp_drp_line(
  ID NUMBER(20) NOT NULL ,
  PLAN_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  SOURCE_WAREHOUSE_ID NUMBER(20)  ,
  REPLENISHMENT_TYPE INTEGER NOT NULL ,
  CURRENT_STOCK NUMBER(20,4)  ,
  ALLOCATED_QTY NUMBER(20,4)  ,
  ON_ORDER_QTY NUMBER(20,4)  ,
  FORECAST_DEMAND NUMBER(20,4)  ,
  SAFETY_STOCK NUMBER(20,4)  ,
  NET_REQUIREMENT NUMBER(20,4)  ,
  SUGGESTED_QTY NUMBER(20,4)  ,
  APPROVED_QTY NUMBER(20,4)  ,
  ORDER_BILL_TYPE VARCHAR2(50)  ,
  ORDER_BILL_CODE VARCHAR2(50)  ,
  STATUS INTEGER NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_drp_line primary key (ID)
);


      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_drp_plan IS 'DRP计划';
                
      COMMENT ON COLUMN erp_drp_plan.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_drp_plan.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_drp_plan.PLAN_NAME IS '计划名称';
                    
      COMMENT ON COLUMN erp_drp_plan.PERIOD_FROM IS '期间开始';
                    
      COMMENT ON COLUMN erp_drp_plan.PERIOD_TO IS '期间结束';
                    
      COMMENT ON COLUMN erp_drp_plan.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_drp_plan.TOTAL_REPLENISHMENT_QTY IS '总补货数量';
                    
      COMMENT ON COLUMN erp_drp_plan.RUN_AT IS '运行时间';
                    
      COMMENT ON COLUMN erp_drp_plan.RUN_BY IS '运行人';
                    
      COMMENT ON COLUMN erp_drp_plan.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_drp_plan.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_drp_plan.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_drp_plan.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_drp_plan.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_drp_plan.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_drp_plan.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_drp_plan.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_drp_parameter IS '仓库补货参数';
                
      COMMENT ON COLUMN erp_drp_parameter.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_drp_parameter.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_drp_parameter.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_drp_parameter.SAFETY_STOCK IS '安全库存';
                    
      COMMENT ON COLUMN erp_drp_parameter.REPLENISHMENT_LEAD_TIME IS '补货提前期(天)';
                    
      COMMENT ON COLUMN erp_drp_parameter.ORDER_MULTIPLE IS '订货倍数';
                    
      COMMENT ON COLUMN erp_drp_parameter.PREFERRED_SOURCE_WAREHOUSE_ID IS '首选调出仓库';
                    
      COMMENT ON COLUMN erp_drp_parameter.PREFERRED_SUPPLIER_ID IS '首选供应商';
                    
      COMMENT ON COLUMN erp_drp_parameter.REPLENISHMENT_METHOD IS '补货方法';
                    
      COMMENT ON COLUMN erp_drp_parameter.MIN_STOCK_LEVEL IS '最低库存';
                    
      COMMENT ON COLUMN erp_drp_parameter.MAX_STOCK_LEVEL IS '最高库存';
                    
      COMMENT ON COLUMN erp_drp_parameter.REVIEW_PERIOD_DAYS IS '审视周期(天)';
                    
      COMMENT ON COLUMN erp_drp_parameter.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_drp_parameter.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_drp_parameter.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_drp_parameter.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_drp_parameter.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_drp_parameter.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_drp_parameter.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_drp_parameter.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_drp_line IS 'DRP明细';
                
      COMMENT ON COLUMN erp_drp_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_drp_line.PLAN_ID IS '计划ID';
                    
      COMMENT ON COLUMN erp_drp_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_drp_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_drp_line.WAREHOUSE_ID IS '目标仓库';
                    
      COMMENT ON COLUMN erp_drp_line.SOURCE_WAREHOUSE_ID IS '来源仓库';
                    
      COMMENT ON COLUMN erp_drp_line.REPLENISHMENT_TYPE IS '补货类型';
                    
      COMMENT ON COLUMN erp_drp_line.CURRENT_STOCK IS '当前库存';
                    
      COMMENT ON COLUMN erp_drp_line.ALLOCATED_QTY IS '已分配量';
                    
      COMMENT ON COLUMN erp_drp_line.ON_ORDER_QTY IS '在单量';
                    
      COMMENT ON COLUMN erp_drp_line.FORECAST_DEMAND IS '预测需求量';
                    
      COMMENT ON COLUMN erp_drp_line.SAFETY_STOCK IS '安全库存';
                    
      COMMENT ON COLUMN erp_drp_line.NET_REQUIREMENT IS '净需求';
                    
      COMMENT ON COLUMN erp_drp_line.SUGGESTED_QTY IS '建议补货量';
                    
      COMMENT ON COLUMN erp_drp_line.APPROVED_QTY IS '批准补货量';
                    
      COMMENT ON COLUMN erp_drp_line.ORDER_BILL_TYPE IS '生成单据类型';
                    
      COMMENT ON COLUMN erp_drp_line.ORDER_BILL_CODE IS '生成单据号';
                    
      COMMENT ON COLUMN erp_drp_line.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_drp_line.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_drp_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_drp_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_drp_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_drp_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_drp_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_drp_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_drp_line.UPDATE_TIME IS '修改时间';
                    
