
CREATE TABLE erp_md_md_organization(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_organization primary key (ID)
);

CREATE TABLE erp_aps_operation_order(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  WORK_ORDER_ID NUMBER(20) NOT NULL ,
  OPERATION_NAME VARCHAR2(200) NOT NULL ,
  SEQUENCE INTEGER NOT NULL ,
  MACHINE_ID NUMBER(20) NOT NULL ,
  PRIORITY INTEGER default 50   ,
  PLANNED_START_DATE_T DATE  ,
  PLANNED_END_DATE_T DATE  ,
  REAL_START_DATE_T DATE  ,
  REAL_END_DATE_T DATE  ,
  SETUP_TIME NUMBER(10,2)  ,
  RUNTIME_PER_UNIT NUMBER(10,2)  ,
  QTY NUMBER(20,4) NOT NULL ,
  TOTAL_DURATION NUMBER(10,2)  ,
  ASSIGNED_TO_ID NUMBER(20)  ,
  IS_OUTSOURCED CHAR(1) default 0   ,
  STATUS INTEGER NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_operation_order primary key (ID)
);

CREATE TABLE erp_aps_schedule(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  SCHEDULE_DATE DATE NOT NULL ,
  SCHEDULING_MODE INTEGER NOT NULL ,
  HORIZON_START DATE NOT NULL ,
  HORIZON_END DATE NOT NULL ,
  STATUS INTEGER NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_schedule primary key (ID)
);

CREATE TABLE erp_aps_constraint(
  ID NUMBER(20) NOT NULL ,
  MACHINE_ID NUMBER(20) NOT NULL ,
  CONSTRAINT_TYPE INTEGER NOT NULL ,
  START_TIME DATE NOT NULL ,
  END_TIME DATE NOT NULL ,
  DESCRIPTION VARCHAR2(500)  ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_constraint primary key (ID)
);


      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_aps_operation_order IS '工序工单';
                
      COMMENT ON COLUMN erp_aps_operation_order.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_operation_order.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_aps_operation_order.WORK_ORDER_ID IS '主工单';
                    
      COMMENT ON COLUMN erp_aps_operation_order.OPERATION_NAME IS '工序名称';
                    
      COMMENT ON COLUMN erp_aps_operation_order.SEQUENCE IS '工序顺序';
                    
      COMMENT ON COLUMN erp_aps_operation_order.MACHINE_ID IS '工作中心/设备';
                    
      COMMENT ON COLUMN erp_aps_operation_order.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_aps_operation_order.PLANNED_START_DATE_T IS '计划开工时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.PLANNED_END_DATE_T IS '计划完工时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.REAL_START_DATE_T IS '实际开工时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.REAL_END_DATE_T IS '实际完工时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.SETUP_TIME IS '准备时间(分钟)';
                    
      COMMENT ON COLUMN erp_aps_operation_order.RUNTIME_PER_UNIT IS '每件加工时间(分钟)';
                    
      COMMENT ON COLUMN erp_aps_operation_order.QTY IS '加工数量';
                    
      COMMENT ON COLUMN erp_aps_operation_order.TOTAL_DURATION IS '总耗时(分钟)';
                    
      COMMENT ON COLUMN erp_aps_operation_order.ASSIGNED_TO_ID IS '操作工';
                    
      COMMENT ON COLUMN erp_aps_operation_order.IS_OUTSOURCED IS '是否外协';
                    
      COMMENT ON COLUMN erp_aps_operation_order.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_aps_operation_order.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_operation_order.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_aps_operation_order.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_operation_order.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_operation_order.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_operation_order.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_operation_order.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_aps_schedule IS '排产方案';
                
      COMMENT ON COLUMN erp_aps_schedule.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_schedule.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_aps_schedule.NAME IS '方案名称';
                    
      COMMENT ON COLUMN erp_aps_schedule.SCHEDULE_DATE IS '排产日期';
                    
      COMMENT ON COLUMN erp_aps_schedule.SCHEDULING_MODE IS '排产模式';
                    
      COMMENT ON COLUMN erp_aps_schedule.HORIZON_START IS '展望期开始';
                    
      COMMENT ON COLUMN erp_aps_schedule.HORIZON_END IS '展望期结束';
                    
      COMMENT ON COLUMN erp_aps_schedule.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_aps_schedule.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_schedule.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_aps_schedule.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_schedule.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_schedule.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_schedule.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_schedule.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_schedule.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_aps_constraint IS '排产约束';
                
      COMMENT ON COLUMN erp_aps_constraint.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_constraint.MACHINE_ID IS '工作中心/设备';
                    
      COMMENT ON COLUMN erp_aps_constraint.CONSTRAINT_TYPE IS '约束类型';
                    
      COMMENT ON COLUMN erp_aps_constraint.START_TIME IS '开始时间';
                    
      COMMENT ON COLUMN erp_aps_constraint.END_TIME IS '结束时间';
                    
      COMMENT ON COLUMN erp_aps_constraint.DESCRIPTION IS '约束描述';
                    
      COMMENT ON COLUMN erp_aps_constraint.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_constraint.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_aps_constraint.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_constraint.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_constraint.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_constraint.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_constraint.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_constraint.UPDATE_TIME IS '修改时间';
                    
