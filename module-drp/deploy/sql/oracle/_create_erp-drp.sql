
CREATE TABLE erp_md_organization(
  ID NUMBER(20) NOT NULL ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_organization primary key (ID)
);

CREATE TABLE erp_md_material(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_material primary key (ID)
);

CREATE TABLE erp_md_warehouse(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_warehouse primary key (ID)
);

CREATE TABLE erp_md_partner(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_partner primary key (ID)
);

CREATE TABLE erp_md_location(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_location primary key (ID)
);

CREATE TABLE erp_inv_stock_move(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_inv_stock_move primary key (ID)
);

CREATE TABLE erp_drp_plan(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  PLAN_NAME VARCHAR2(200) NOT NULL ,
  PERIOD_FROM DATE NOT NULL ,
  PERIOD_TO DATE NOT NULL ,
  STATUS VARCHAR2(20) NOT NULL ,
  TOTAL_REPLENISHMENT_QTY NUMBER(20,4)  ,
  RUN_AT TIMESTAMP  ,
  RUN_BY VARCHAR2(50)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  constraint PK_erp_drp_plan primary key (ID)
);

CREATE TABLE erp_inv_drp_safety_stock_calc(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20)  ,
  METHOD VARCHAR2(20) NOT NULL ,
  SERVICE_LEVEL VARCHAR2(20) NOT NULL ,
  HISTORY_MONTHS INTEGER default 6   ,
  LEAD_TIME_DAYS INTEGER NOT NULL ,
  CALCULATED_SAFETY_STOCK NUMBER(20,4)  ,
  CALCULATED_ROP NUMBER(20,4)  ,
  OVERRIDE_SAFETY_STOCK NUMBER(20,4)  ,
  LAST_CALCULATED_AT TIMESTAMP  ,
  OVERWRITTEN_BY VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_drp_safety_stock_calc primary key (ID)
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
  REPLENISHMENT_METHOD VARCHAR2(20) NOT NULL ,
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

CREATE TABLE erp_inv_drp_lead_time_record(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SUPPLIER_ID NUMBER(20) NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  ORDER_DATE DATE NOT NULL ,
  RECEIPT_DATE DATE NOT NULL ,
  ACTUAL_LEAD_TIME INTEGER NOT NULL ,
  EXPECTED_LEAD_TIME INTEGER  ,
  VARIANCE_DAYS INTEGER  ,
  PURCHASE_ORDER_CODE VARCHAR2(50)  ,
  IS_ON_TIME CHAR(1) default 1   ,
  EARLY_LATE_FLAG VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_drp_lead_time_record primary key (ID)
);

CREATE TABLE erp_drp_line(
  ID NUMBER(20) NOT NULL ,
  PLAN_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  SOURCE_WAREHOUSE_ID NUMBER(20)  ,
  REPLENISHMENT_TYPE VARCHAR2(20) NOT NULL ,
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
  STATUS VARCHAR2(20) NOT NULL ,
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

CREATE TABLE erp_inv_drp_cross_dock(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  DRP_LINE_ID NUMBER(20)  ,
  INBOUND_MOVE_ID NUMBER(20)  ,
  OUTBOUND_MOVE_ID NUMBER(20)  ,
  SOURCE_BILL_TYPE VARCHAR2(50)  ,
  SOURCE_BILL_CODE VARCHAR2(50)  ,
  TARGET_BILL_TYPE VARCHAR2(50)  ,
  TARGET_BILL_CODE VARCHAR2(50)  ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  STAGING_LOCATION_ID NUMBER(20)  ,
  DOCK_SLOT_TIME TIMESTAMP  ,
  STATUS VARCHAR2(20) NOT NULL ,
  MATCHED_AT TIMESTAMP  ,
  LOADED_AT TIMESTAMP  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_drp_cross_dock primary key (ID)
);

CREATE TABLE erp_inv_drp_dock_appointment(
  ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  DOCK_ID NUMBER(20) NOT NULL ,
  APPOINTMENT_DATE DATE NOT NULL ,
  SLOT_START TIMESTAMP NOT NULL ,
  SLOT_END TIMESTAMP NOT NULL ,
  CROSS_DOCK_ID NUMBER(20)  ,
  CARRIER_INFO VARCHAR2(500)  ,
  STATUS VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  ORG_ID NUMBER(20)  ,
  constraint PK_erp_inv_drp_dock_appointment primary key (ID)
);


      COMMENT ON TABLE erp_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON TABLE erp_inv_stock_move IS '库存移动';
                
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
                    
      COMMENT ON COLUMN erp_drp_plan.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON TABLE erp_inv_drp_safety_stock_calc IS '安全库存计算';
                
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.METHOD IS '计算方法';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.SERVICE_LEVEL IS '服务水平';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.HISTORY_MONTHS IS '分析月数';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.LEAD_TIME_DAYS IS '提前期(天)';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.CALCULATED_SAFETY_STOCK IS '建议安全库存';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.CALCULATED_ROP IS '建议再订货点';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.OVERRIDE_SAFETY_STOCK IS '覆盖安全库存';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.LAST_CALCULATED_AT IS '计算时间';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.OVERWRITTEN_BY IS '覆盖人';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_inv_drp_lead_time_record IS '提前期记录';
                
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.ORDER_DATE IS '订单日期';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.RECEIPT_DATE IS '入库日期';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.ACTUAL_LEAD_TIME IS '实际提前期(天)';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.EXPECTED_LEAD_TIME IS '预期提前期(天)';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.VARIANCE_DAYS IS '偏差天数';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.PURCHASE_ORDER_CODE IS '采购单号';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.IS_ON_TIME IS '是否准时';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.EARLY_LATE_FLAG IS '提前/延迟标记';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_inv_drp_cross_dock IS '越库执行记录';
                
      COMMENT ON COLUMN erp_inv_drp_cross_dock.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.DRP_LINE_ID IS 'DRP行';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.INBOUND_MOVE_ID IS '入站移动单';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.OUTBOUND_MOVE_ID IS '出站移动单';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.SOURCE_BILL_TYPE IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.SOURCE_BILL_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.TARGET_BILL_TYPE IS '目标单据类型';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.TARGET_BILL_CODE IS '目标单据号';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.QUANTITY IS '越库数量';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.STAGING_LOCATION_ID IS '暂存库位';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.DOCK_SLOT_TIME IS '月台时间窗口';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.MATCHED_AT IS '匹配时间';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.LOADED_AT IS '装车完成时间';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_drp_dock_appointment IS '月台预约';
                
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.DOCK_ID IS '月台';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.APPOINTMENT_DATE IS '预约日期';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.SLOT_START IS '时间窗口开始';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.SLOT_END IS '时间窗口结束';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.CROSS_DOCK_ID IS '关联越库';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.CARRIER_INFO IS '承运商信息';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.ORG_ID IS '业务组织';
                    
