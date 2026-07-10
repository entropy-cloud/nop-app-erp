
CREATE TABLE erp_md_warehouse(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  WAREHOUSE_TYPE INTEGER  ,
  ORG_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_warehouse primary key (ID)
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

CREATE TABLE erp_md_location(
  ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARENT_ID NUMBER(20)  ,
  constraint PK_erp_md_location primary key (ID)
);

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

CREATE TABLE erp_md_currency(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  SYMBOL VARCHAR2(50)  ,
  DECIMAL_PLACES INTEGER  ,
  IS_FUNCTIONAL CHAR(1)  ,
  constraint PK_erp_md_currency primary key (ID)
);

CREATE TABLE erp_md_acct_schema(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  CURRENCY_ID NUMBER(20)  ,
  NATURE INTEGER  ,
  constraint PK_erp_md_acct_schema primary key (ID)
);

CREATE TABLE erp_md_partner(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARTNER_TYPE INTEGER  ,
  constraint PK_erp_md_partner primary key (ID)
);

CREATE TABLE erp_md_employee(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_employee primary key (ID)
);

CREATE TABLE erp_inv_transfer_order(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  FROM_WAREHOUSE_ID NUMBER(20) NOT NULL ,
  TO_WAREHOUSE_ID NUMBER(20) NOT NULL ,
  IN_TRANSIT_WAREHOUSE_ID NUMBER(20)  ,
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
  constraint PK_erp_inv_transfer_order primary key (ID)
);

CREATE TABLE erp_inv_stock_take(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  TAKE_TYPE VARCHAR2(20)  ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
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
  constraint PK_erp_inv_stock_take primary key (ID)
);

CREATE TABLE erp_inv_stock_move(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  MOVE_TYPE VARCHAR2(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  SOURCE_WAREHOUSE_ID NUMBER(20)  ,
  SOURCE_LOCATION_ID NUMBER(20)  ,
  DEST_WAREHOUSE_ID NUMBER(20)  ,
  DEST_LOCATION_ID NUMBER(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  ORIGIN_MOVE_ID NUMBER(20)  ,
  ORIGIN_RETURNED_MOVE_ID NUMBER(20)  ,
  constraint PK_erp_inv_stock_move primary key (ID)
);

CREATE TABLE erp_inv_batch(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50) NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  TOTAL_QUANTITY NUMBER(20,4) NOT NULL ,
  AVAILABLE_QUANTITY NUMBER(20,4) NOT NULL ,
  PRODUCTION_DATE DATE  ,
  EXPIRY_DATE DATE  ,
  SHELF_LIFE_DAYS INTEGER  ,
  STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_batch primary key (ID)
);

CREATE TABLE erp_inv_serial_number(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SERIAL_NO VARCHAR2(50) NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  LOCATION_ID NUMBER(20)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  IN_BILL_TYPE VARCHAR2(50)  ,
  IN_BILL_CODE VARCHAR2(50)  ,
  OUT_BILL_TYPE VARCHAR2(50)  ,
  OUT_BILL_CODE VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_serial_number primary key (ID)
);

CREATE TABLE erp_inv_cost_adjust(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  ADJUST_TYPE VARCHAR2(30) NOT NULL ,
  REASON VARCHAR2(500)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  CURRENCY_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_cost_adjust primary key (ID)
);

CREATE TABLE erp_inv_stock_balance(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  LOCATION_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50)  ,
  TOTAL_QUANTITY NUMBER(20,4) NOT NULL ,
  RESERVED_QUANTITY NUMBER(20,4) default 0   ,
  LOCKED_QUANTITY NUMBER(20,4) default 0   ,
  AVAILABLE_QUANTITY NUMBER(20,4) NOT NULL ,
  COST_METHOD VARCHAR2(20)  ,
  AVG_COST NUMBER(20,4)  ,
  TOTAL_COST NUMBER(20,4)  ,
  CURRENCY_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  OWNER_ID NUMBER(20)  ,
  OWNERSHIP_TYPE VARCHAR2(20) default 'OWNED'   ,
  constraint PK_erp_inv_stock_balance primary key (ID)
);

CREATE TABLE erp_inv_reservation(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  SOURCE_BILL_TYPE VARCHAR2(50) NOT NULL ,
  SOURCE_BILL_CODE VARCHAR2(50) NOT NULL ,
  RESERVED_FOR_PARTNER_ID NUMBER(20)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  VALID_UNTIL DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_reservation primary key (ID)
);

CREATE TABLE erp_inv_ownership_transfer(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  TRANSFER_TYPE VARCHAR2(30) NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  SOURCE_LOC_ID NUMBER(20) NOT NULL ,
  DEST_LOC_ID NUMBER(20) NOT NULL ,
  FROM_OWNERSHIP_TYPE VARCHAR2(20) NOT NULL ,
  TO_OWNERSHIP_TYPE VARCHAR2(20) NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
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
  constraint PK_erp_inv_ownership_transfer primary key (ID)
);

CREATE TABLE erp_inv_landed_cost(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  RECEIVE_ID NUMBER(20) NOT NULL ,
  SUPPLIER_ID NUMBER(20)  ,
  CURRENCY_ID NUMBER(20)  ,
  EXCHANGE_RATE NUMBER(20,6)  ,
  TOTAL_COST_AMOUNT NUMBER(20,4)  ,
  ALLOCATION_METHOD VARCHAR2(20) NOT NULL ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  BUSINESS_DATE DATE NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_landed_cost primary key (ID)
);

CREATE TABLE erp_inv_picking_order(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  PICKER_ID NUMBER(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_picking_order primary key (ID)
);

CREATE TABLE erp_inv_transfer_order_line(
  ID NUMBER(20) NOT NULL ,
  TRANSFER_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  BATCH_NO VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_transfer_order_line primary key (ID)
);

CREATE TABLE erp_inv_stock_take_line(
  ID NUMBER(20) NOT NULL ,
  TAKE_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  LOCATION_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50)  ,
  BOOK_QUANTITY NUMBER(20,4) NOT NULL ,
  ACTUAL_QUANTITY NUMBER(20,4) NOT NULL ,
  DIFFERENCE_QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_COST NUMBER(20,4)  ,
  DIFFERENCE_AMOUNT NUMBER(20,4)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_stock_take_line primary key (ID)
);

CREATE TABLE erp_inv_stock_move_line(
  ID NUMBER(20) NOT NULL ,
  MOVE_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_COST NUMBER(20,4)  ,
  TOTAL_COST NUMBER(20,4)  ,
  CURRENCY_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50)  ,
  SERIAL_NO VARCHAR2(50)  ,
  SOURCE_LOCATION_ID NUMBER(20)  ,
  DEST_LOCATION_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_stock_move_line primary key (ID)
);

CREATE TABLE erp_inv_cost_layer(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  BATCH_NO VARCHAR2(50)  ,
  COST_METHOD VARCHAR2(20) NOT NULL ,
  INCOMING_QUANTITY NUMBER(20,4) NOT NULL ,
  REMAINING_QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_COST NUMBER(20,4) NOT NULL ,
  TOTAL_COST NUMBER(20,4) NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  INCOMING_DATE DATE  ,
  INCOMING_MOVE_ID NUMBER(20)  ,
  ACCT_SCHEMA_ID NUMBER(20)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_cost_layer primary key (ID)
);

CREATE TABLE erp_inv_cost_adjust_line(
  ID NUMBER(20) NOT NULL ,
  ADJUST_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  BATCH_NO VARCHAR2(50)  ,
  OLD_UNIT_COST NUMBER(20,4)  ,
  NEW_UNIT_COST NUMBER(20,4) NOT NULL ,
  ADJUST_QTY NUMBER(20,4)  ,
  ADJUST_AMOUNT NUMBER(20,4)  ,
  ADJUST_REASON VARCHAR2(200)  ,
  CURRENCY_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_cost_adjust_line primary key (ID)
);

CREATE TABLE erp_inv_reservation_line(
  ID NUMBER(20) NOT NULL ,
  RESERVATION_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  LOCATION_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50)  ,
  RESERVED_QUANTITY NUMBER(20,4) NOT NULL ,
  CONSUMED_QUANTITY NUMBER(20,4) default 0   ,
  UOM_ID NUMBER(20) NOT NULL ,
  SOURCE_LINE_CODE VARCHAR2(50)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_reservation_line primary key (ID)
);

CREATE TABLE erp_inv_ownership_transfer_line(
  ID NUMBER(20) NOT NULL ,
  TRANSFER_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50)  ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_COST NUMBER(20,4)  ,
  TOTAL_COST NUMBER(20,4)  ,
  SOURCE_BILL_TYPE VARCHAR2(50)  ,
  SOURCE_BILL_CODE VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_ownership_transfer_line primary key (ID)
);

CREATE TABLE erp_inv_landed_cost_line(
  ID NUMBER(20) NOT NULL ,
  LANDED_COST_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  COST_ELEMENT VARCHAR2(30) NOT NULL ,
  AMOUNT NUMBER(20,4) NOT NULL ,
  AP_PARTNER_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_landed_cost_line primary key (ID)
);

CREATE TABLE erp_inv_picking_order_line(
  ID NUMBER(20) NOT NULL ,
  PICKING_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  UO_M_ID NUMBER(20) NOT NULL ,
  SOURCE_LOCATION_ID NUMBER(20)  ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  PICKED_QUANTITY NUMBER(20,4) default 0   ,
  BATCH_NO VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_picking_order_line primary key (ID)
);

CREATE TABLE erp_inv_stock_ledger(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  MOVE_ID NUMBER(20) NOT NULL ,
  MOVE_LINE_ID NUMBER(20) NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  SKU_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20) NOT NULL ,
  LOCATION_ID NUMBER(20)  ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_COST NUMBER(20,4)  ,
  TOTAL_COST NUMBER(20,4)  ,
  BALANCE_QUANTITY NUMBER(20,4) NOT NULL ,
  BALANCE_TOTAL_COST NUMBER(20,4) NOT NULL ,
  COST_METHOD VARCHAR2(20)  ,
  ACCT_SCHEMA_ID NUMBER(20)  ,
  CURRENCY_ID NUMBER(20)  ,
  BUSINESS_DATE DATE  ,
  BATCH_NO VARCHAR2(50)  ,
  SERIAL_NO VARCHAR2(50)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  OWNER_ID NUMBER(20)  ,
  OWNERSHIP_TYPE VARCHAR2(20) default 'OWNED'   ,
  constraint PK_erp_inv_stock_ledger primary key (ID)
);


      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_material_sku IS '物料SKU';
                
      COMMENT ON TABLE erp_md_uom IS '计量单位';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_acct_schema IS '会计核算表(账套)';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_employee IS '员工';
                
      COMMENT ON TABLE erp_inv_transfer_order IS '调拨单';
                
      COMMENT ON COLUMN erp_inv_transfer_order.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.FROM_WAREHOUSE_ID IS '调出仓库';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.TO_WAREHOUSE_ID IS '调入仓库';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.IN_TRANSIT_WAREHOUSE_ID IS '在途仓库';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_take IS '盘点单';
                
      COMMENT ON COLUMN erp_inv_stock_take.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_take.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_inv_stock_take.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_stock_take.BUSINESS_DATE IS '盘点日期';
                    
      COMMENT ON COLUMN erp_inv_stock_take.TAKE_TYPE IS '盘点类型';
                    
      COMMENT ON COLUMN erp_inv_stock_take.WAREHOUSE_ID IS '盘点仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_take.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_stock_take.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_stock_take.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_stock_take.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_stock_take.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_stock_take.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_stock_take.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_take.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_take.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_take.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_take.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_take.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_move IS '库存移动单';
                
      COMMENT ON COLUMN erp_inv_stock_move.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_move.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_inv_stock_move.MOVE_TYPE IS '作业类型';
                    
      COMMENT ON COLUMN erp_inv_stock_move.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_stock_move.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_stock_move.SOURCE_WAREHOUSE_ID IS '源仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_move.SOURCE_LOCATION_ID IS '源库位';
                    
      COMMENT ON COLUMN erp_inv_stock_move.DEST_WAREHOUSE_ID IS '目标仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_move.DEST_LOCATION_ID IS '目标库位';
                    
      COMMENT ON COLUMN erp_inv_stock_move.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_stock_move.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_stock_move.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_stock_move.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_stock_move.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_stock_move.RELATED_BILL_TYPE IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_inv_stock_move.RELATED_BILL_CODE IS '关联单据号';
                    
      COMMENT ON COLUMN erp_inv_stock_move.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_stock_move.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_move.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_move.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_move.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_move.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_move.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_inv_stock_move.ORIGIN_MOVE_ID IS '上游移动单';
                    
      COMMENT ON COLUMN erp_inv_stock_move.ORIGIN_RETURNED_MOVE_ID IS '原退货移动单';
                    
      COMMENT ON TABLE erp_inv_batch IS '批次台账';
                
      COMMENT ON COLUMN erp_inv_batch.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_batch.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_batch.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_batch.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_batch.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_batch.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_batch.TOTAL_QUANTITY IS '总数量';
                    
      COMMENT ON COLUMN erp_inv_batch.AVAILABLE_QUANTITY IS '可用数量';
                    
      COMMENT ON COLUMN erp_inv_batch.PRODUCTION_DATE IS '生产日期';
                    
      COMMENT ON COLUMN erp_inv_batch.EXPIRY_DATE IS '有效期至';
                    
      COMMENT ON COLUMN erp_inv_batch.SHELF_LIFE_DAYS IS '保质期(天)';
                    
      COMMENT ON COLUMN erp_inv_batch.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_inv_batch.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_batch.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_batch.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_batch.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_batch.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_batch.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_batch.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_serial_number IS '序列号台账';
                
      COMMENT ON COLUMN erp_inv_serial_number.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_serial_number.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_serial_number.SERIAL_NO IS '序列号';
                    
      COMMENT ON COLUMN erp_inv_serial_number.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_serial_number.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_serial_number.WAREHOUSE_ID IS '当前仓库';
                    
      COMMENT ON COLUMN erp_inv_serial_number.LOCATION_ID IS '当前库位';
                    
      COMMENT ON COLUMN erp_inv_serial_number.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_inv_serial_number.IN_BILL_TYPE IS '入库单类型';
                    
      COMMENT ON COLUMN erp_inv_serial_number.IN_BILL_CODE IS '入库单号';
                    
      COMMENT ON COLUMN erp_inv_serial_number.OUT_BILL_TYPE IS '出库单类型';
                    
      COMMENT ON COLUMN erp_inv_serial_number.OUT_BILL_CODE IS '出库单号';
                    
      COMMENT ON COLUMN erp_inv_serial_number.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_serial_number.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_serial_number.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_serial_number.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_serial_number.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_serial_number.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_serial_number.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_cost_adjust IS '成本调整单';
                
      COMMENT ON COLUMN erp_inv_cost_adjust.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.ADJUST_TYPE IS '调整类型';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.REASON IS '调整原因';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_balance IS '库存余额';
                
      COMMENT ON COLUMN erp_inv_stock_balance.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.LOCATION_ID IS '库位';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.TOTAL_QUANTITY IS '总数量';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.RESERVED_QUANTITY IS '预留数量';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.LOCKED_QUANTITY IS '冻结数量';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.AVAILABLE_QUANTITY IS '可用数量';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.COST_METHOD IS '计价方法';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.AVG_COST IS '平均成本';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.TOTAL_COST IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.OWNER_ID IS '所有权往来单位';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.OWNERSHIP_TYPE IS '所有权类型';
                    
      COMMENT ON TABLE erp_inv_reservation IS '库存预留单';
                
      COMMENT ON COLUMN erp_inv_reservation.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_reservation.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_inv_reservation.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_reservation.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_reservation.SOURCE_BILL_TYPE IS '来源单据类型(如 SALES_ORDER/WORK_ORDER)';
                    
      COMMENT ON COLUMN erp_inv_reservation.SOURCE_BILL_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_inv_reservation.RESERVED_FOR_PARTNER_ID IS '为谁预留(往来单位)';
                    
      COMMENT ON COLUMN erp_inv_reservation.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_inv_reservation.VALID_UNTIL IS '预留有效期至';
                    
      COMMENT ON COLUMN erp_inv_reservation.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_reservation.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_reservation.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_reservation.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_reservation.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_reservation.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_reservation.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_ownership_transfer IS '所有权转移单';
                
      COMMENT ON COLUMN erp_inv_ownership_transfer.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.TRANSFER_TYPE IS '转移类型';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.PARTNER_ID IS '所有权对方(往来单位)';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.SOURCE_LOC_ID IS '源库位(=目的库位)';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.DEST_LOC_ID IS '目的库位(=源库位)';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.FROM_OWNERSHIP_TYPE IS '转前所有权类型';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.TO_OWNERSHIP_TYPE IS '转后所有权类型';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_landed_cost IS '到岸成本单';
                
      COMMENT ON COLUMN erp_inv_landed_cost.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.RECEIVE_ID IS '采购入库单';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.SUPPLIER_ID IS '采购供应商';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.EXCHANGE_RATE IS '汇率';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.TOTAL_COST_AMOUNT IS '到岸成本合计';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.ALLOCATION_METHOD IS '分摊方法';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.POSTED IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_picking_order IS '拣货单';
                
      COMMENT ON COLUMN erp_inv_picking_order.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_picking_order.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_inv_picking_order.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_picking_order.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_picking_order.WAREHOUSE_ID IS '拣货仓库';
                    
      COMMENT ON COLUMN erp_inv_picking_order.PICKER_ID IS '拣货人(职员)';
                    
      COMMENT ON COLUMN erp_inv_picking_order.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_picking_order.RELATED_BILL_TYPE IS '关联单据类型(SALES_DELIVERY/WORK_ORDER_ISSUE)';
                    
      COMMENT ON COLUMN erp_inv_picking_order.RELATED_BILL_CODE IS '关联单据号';
                    
      COMMENT ON COLUMN erp_inv_picking_order.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_picking_order.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_picking_order.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_picking_order.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_picking_order.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_picking_order.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_picking_order.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_transfer_order_line IS '调拨单行';
                
      COMMENT ON COLUMN erp_inv_transfer_order_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.TRANSFER_ID IS '调拨单ID';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_take_line IS '盘点单行';
                
      COMMENT ON COLUMN erp_inv_stock_take_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.TAKE_ID IS '盘点单ID';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.LOCATION_ID IS '库位';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.BOOK_QUANTITY IS '账面数量';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.ACTUAL_QUANTITY IS '实盘数量';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.DIFFERENCE_QUANTITY IS '差异数量';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.UNIT_COST IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.DIFFERENCE_AMOUNT IS '差异金额';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_move_line IS '库存移动单行';
                
      COMMENT ON COLUMN erp_inv_stock_move_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.MOVE_ID IS '移动单ID';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.UNIT_COST IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.TOTAL_COST IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.SERIAL_NO IS '序列号';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.SOURCE_LOCATION_ID IS '源库位';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.DEST_LOCATION_ID IS '目标库位';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_cost_layer IS '成本层';
                
      COMMENT ON COLUMN erp_inv_cost_layer.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.COST_METHOD IS '计价方法';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.INCOMING_QUANTITY IS '入库数量';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.REMAINING_QUANTITY IS '剩余数量';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.UNIT_COST IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.TOTAL_COST IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.INCOMING_DATE IS '入库日期';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.INCOMING_MOVE_ID IS '入库移动单';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_cost_adjust_line IS '成本调整单行';
                
      COMMENT ON COLUMN erp_inv_cost_adjust_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.ADJUST_ID IS '成本调整单ID';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.OLD_UNIT_COST IS '原单位成本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.NEW_UNIT_COST IS '新单位成本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.ADJUST_QTY IS '调整数量';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.ADJUST_AMOUNT IS '调整金额';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.ADJUST_REASON IS '行调整原因';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_reservation_line IS '库存预留单行';
                
      COMMENT ON COLUMN erp_inv_reservation_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.RESERVATION_ID IS '预留单ID';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.LOCATION_ID IS '库位';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.RESERVED_QUANTITY IS '预留数量';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.CONSUMED_QUANTITY IS '已消耗数量';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.UOM_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.SOURCE_LINE_CODE IS '来源行号';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_ownership_transfer_line IS '所有权转移单行';
                
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.TRANSFER_ID IS '转移单ID';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.UNIT_COST IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.TOTAL_COST IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.SOURCE_BILL_TYPE IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.SOURCE_BILL_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_landed_cost_line IS '到岸成本行';
                
      COMMENT ON COLUMN erp_inv_landed_cost_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.LANDED_COST_ID IS '到岸成本单ID';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.COST_ELEMENT IS '费用要素';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.AP_PARTNER_ID IS '应付对象';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_picking_order_line IS '拣货单行';
                
      COMMENT ON COLUMN erp_inv_picking_order_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.PICKING_ID IS '拣货单ID';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.SOURCE_LOCATION_ID IS '拣货库位';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.QUANTITY IS '应拣数量';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.PICKED_QUANTITY IS '已拣数量';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_ledger IS '库存流水';
                
      COMMENT ON COLUMN erp_inv_stock_ledger.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.CODE IS '流水号';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.MOVE_ID IS '移动单ID';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.MOVE_LINE_ID IS '移动单行ID';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.SKU_ID IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.LOCATION_ID IS '库位';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.UNIT_COST IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.TOTAL_COST IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.BALANCE_QUANTITY IS '结存数量';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.BALANCE_TOTAL_COST IS '结存总成本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.COST_METHOD IS '计价方法';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.ACCT_SCHEMA_ID IS '账套';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.SERIAL_NO IS '序列号';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.OWNER_ID IS '所有权往来单位';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.OWNERSHIP_TYPE IS '所有权类型';
                    
