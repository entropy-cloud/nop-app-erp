
CREATE TABLE erp_md_organization(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_organization primary key (ID)
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
  ASSIGNED_TO_ID VARCHAR2(36)  ,
  IS_OUTSOURCED CHAR(1) default 0   ,
  STATUS VARCHAR2(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  EARLIEST_START_DATE_T DATE  ,
  LATEST_END_DATE_T DATE  ,
  constraint PK_erp_aps_operation_order primary key (ID)
);

CREATE TABLE erp_aps_schedule(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  SCHEDULE_DATE DATE NOT NULL ,
  SCHEDULING_MODE VARCHAR2(20) NOT NULL ,
  HORIZON_START DATE NOT NULL ,
  HORIZON_END DATE NOT NULL ,
  STATUS VARCHAR2(20) NOT NULL ,
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
  CONSTRAINT_TYPE VARCHAR2(20) NOT NULL ,
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

CREATE TABLE erp_aps_op_routing(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  OPERATION_ID NUMBER(20) NOT NULL ,
  MACHINE_ID NUMBER(20) NOT NULL ,
  PRIORITY INTEGER NOT NULL ,
  SETUP_TIME_DELTA NUMBER(10,2)  ,
  RUNTIME_PER_UNIT_DELTA NUMBER(10,2)  ,
  IS_DEFAULT CHAR(1) default 0   ,
  IS_ENABLED CHAR(1) default 1   ,
  EFFECTIVE_FROM DATE  ,
  EFFECTIVE_TO DATE  ,
  MIN_BATCH_QTY NUMBER(20,4)  ,
  MAX_BATCH_QTY NUMBER(20,4)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_op_routing primary key (ID)
);

CREATE TABLE erp_aps_dispatch_rule(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  WORKCENTER_ID NUMBER(20) NOT NULL ,
  RULE_NAME VARCHAR2(200) NOT NULL ,
  ENABLE_AUTO CHAR(1) default 1   ,
  REQUIRE_MATERIAL CHAR(1) default 1   ,
  REQUIRE_OPERATOR CHAR(1) default 1   ,
  REQUIRE_TOOLING CHAR(1) default 0   ,
  MAX_LOOKAHEAD_MINUTES INTEGER default 120   ,
  DISPATCH_AHEAD_MINUTES INTEGER default 15   ,
  AUTO_CONFIRM_MATERIAL CHAR(1) default 1   ,
  MAX_CONCURRENT_OPS INTEGER  ,
  PRIORITY_THRESHOLD INTEGER  ,
  ENABLED_HOURS VARCHAR2(500)  ,
  HOLD_UNTIL DATE  ,
  HOLD_REASON VARCHAR2(500)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_dispatch_rule primary key (ID)
);

CREATE TABLE erp_aps_dispatch_log(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  OPERATION_ORDER_ID NUMBER(20) NOT NULL ,
  WORKCENTER_ID NUMBER(20)  ,
  DISPATCH_TYPE VARCHAR2(20) NOT NULL ,
  PREVIOUS_STATUS VARCHAR2(50)  ,
  NEW_STATUS VARCHAR2(50)  ,
  CONDITION_CHECK_RESULT VARCHAR2(2000)  ,
  DISPATCHED_BY VARCHAR2(50)  ,
  DISPATCHED_AT DATE  ,
  MATERIAL_AVAILABLE CHAR(1)  ,
  OPERATOR_AVAILABLE CHAR(1)  ,
  TOOLING_AVAILABLE CHAR(1)  ,
  NOTE VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_dispatch_log primary key (ID)
);


      COMMENT ON TABLE erp_md_organization IS 'ErpMdOrganization';
                
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
                    
      COMMENT ON COLUMN erp_aps_operation_order.EARLIEST_START_DATE_T IS '最早开工时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.LATEST_END_DATE_T IS '最晚完工时间';
                    
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
                    
      COMMENT ON TABLE erp_aps_op_routing IS '替代工艺路线';
                
      COMMENT ON COLUMN erp_aps_op_routing.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_op_routing.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_op_routing.OPERATION_ID IS '工序';
                    
      COMMENT ON COLUMN erp_aps_op_routing.MACHINE_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_aps_op_routing.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_aps_op_routing.SETUP_TIME_DELTA IS '换模时间差(分钟)';
                    
      COMMENT ON COLUMN erp_aps_op_routing.RUNTIME_PER_UNIT_DELTA IS '单件加工时间差(分钟)';
                    
      COMMENT ON COLUMN erp_aps_op_routing.IS_DEFAULT IS '默认路由';
                    
      COMMENT ON COLUMN erp_aps_op_routing.IS_ENABLED IS '启用';
                    
      COMMENT ON COLUMN erp_aps_op_routing.EFFECTIVE_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_aps_op_routing.EFFECTIVE_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_aps_op_routing.MIN_BATCH_QTY IS '最小批量';
                    
      COMMENT ON COLUMN erp_aps_op_routing.MAX_BATCH_QTY IS '最大批量';
                    
      COMMENT ON COLUMN erp_aps_op_routing.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_aps_op_routing.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_op_routing.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_op_routing.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_op_routing.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_op_routing.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_op_routing.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_aps_dispatch_rule IS '自动派工规则';
                
      COMMENT ON COLUMN erp_aps_dispatch_rule.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.WORKCENTER_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.RULE_NAME IS '规则名称';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.ENABLE_AUTO IS '自动派工启用';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.REQUIRE_MATERIAL IS '物料齐套检查';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.REQUIRE_OPERATOR IS '操作工检查';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.REQUIRE_TOOLING IS '工装检查';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.MAX_LOOKAHEAD_MINUTES IS '前瞻窗口(分钟)';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.DISPATCH_AHEAD_MINUTES IS '提前派工(分钟)';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.AUTO_CONFIRM_MATERIAL IS '自动确认物料';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.MAX_CONCURRENT_OPS IS '最大并行工序数';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.PRIORITY_THRESHOLD IS '优先级阈值';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.ENABLED_HOURS IS '允许时段(JSON)';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.HOLD_UNTIL IS '暂停到';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.HOLD_REASON IS '暂停原因';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_aps_dispatch_log IS '派工日志';
                
      COMMENT ON COLUMN erp_aps_dispatch_log.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.OPERATION_ORDER_ID IS '工序工单';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.WORKCENTER_ID IS '工作中心';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.DISPATCH_TYPE IS '派工类型';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.PREVIOUS_STATUS IS '派工前状态';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.NEW_STATUS IS '派工后状态';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.CONDITION_CHECK_RESULT IS '条件检查结果(JSON)';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.DISPATCHED_BY IS '派工人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.DISPATCHED_AT IS '派工时间';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.MATERIAL_AVAILABLE IS '物料齐套';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.OPERATOR_AVAILABLE IS '操作工可用';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.TOOLING_AVAILABLE IS '工装可用';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.NOTE IS '备注';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.UPDATE_TIME IS '修改时间';
                    
