
CREATE TABLE erp_md_md_organization(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_organization primary key (id)
);

CREATE TABLE erp_aps_operation_order(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  work_order_id INT8 NOT NULL ,
  operation_name VARCHAR(200) NOT NULL ,
  sequence INT4 NOT NULL ,
  machine_id INT8 NOT NULL ,
  priority INT4 default 50   ,
  planned_start_date_t TIMESTAMP  ,
  planned_end_date_t TIMESTAMP  ,
  real_start_date_t TIMESTAMP  ,
  real_end_date_t TIMESTAMP  ,
  setup_time NUMERIC(10,2)  ,
  runtime_per_unit NUMERIC(10,2)  ,
  qty NUMERIC(20,4) NOT NULL ,
  total_duration NUMERIC(10,2)  ,
  assigned_to_id VARCHAR(36)  ,
  is_outsourced BOOLEAN default false   ,
  status INT4 NOT NULL ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_operation_order primary key (id)
);

CREATE TABLE erp_aps_schedule(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  schedule_date DATE NOT NULL ,
  scheduling_mode INT4 NOT NULL ,
  horizon_start TIMESTAMP NOT NULL ,
  horizon_end TIMESTAMP NOT NULL ,
  status INT4 NOT NULL ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_schedule primary key (id)
);

CREATE TABLE erp_aps_constraint(
  id INT8 NOT NULL ,
  machine_id INT8 NOT NULL ,
  constraint_type INT4 NOT NULL ,
  start_time TIMESTAMP NOT NULL ,
  end_time TIMESTAMP NOT NULL ,
  description VARCHAR(500)  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_constraint primary key (id)
);

CREATE TABLE erp_aps_op_routing(
  id INT8 NOT NULL ,
  org_id INT8  ,
  operation_id INT8 NOT NULL ,
  machine_id INT8 NOT NULL ,
  priority INT4 NOT NULL ,
  setup_time_delta NUMERIC(10,2)  ,
  runtime_per_unit_delta NUMERIC(10,2)  ,
  is_default BOOLEAN default false   ,
  is_enabled BOOLEAN default true   ,
  effective_from DATE  ,
  effective_to DATE  ,
  min_batch_qty NUMERIC(20,4)  ,
  max_batch_qty NUMERIC(20,4)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_op_routing primary key (id)
);

CREATE TABLE erp_aps_dispatch_rule(
  id INT8 NOT NULL ,
  org_id INT8  ,
  workcenter_id INT8 NOT NULL ,
  rule_name VARCHAR(200) NOT NULL ,
  enable_auto BOOLEAN default true   ,
  require_material BOOLEAN default true   ,
  require_operator BOOLEAN default true   ,
  require_tooling BOOLEAN default false   ,
  max_lookahead_minutes INT4 default 120   ,
  dispatch_ahead_minutes INT4 default 15   ,
  auto_confirm_material BOOLEAN default true   ,
  max_concurrent_ops INT4  ,
  priority_threshold INT4  ,
  enabled_hours VARCHAR(500)  ,
  hold_until TIMESTAMP  ,
  hold_reason VARCHAR(500)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_dispatch_rule primary key (id)
);

CREATE TABLE erp_aps_dispatch_log(
  id INT8 NOT NULL ,
  org_id INT8  ,
  operation_order_id INT8 NOT NULL ,
  workcenter_id INT8  ,
  dispatch_type INT4 NOT NULL ,
  previous_status VARCHAR(50)  ,
  new_status VARCHAR(50)  ,
  condition_check_result VARCHAR(2000)  ,
  dispatched_by VARCHAR(50)  ,
  dispatched_at TIMESTAMP  ,
  material_available BOOLEAN  ,
  operator_available BOOLEAN  ,
  tooling_available BOOLEAN  ,
  note VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_aps_dispatch_log primary key (id)
);


      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_aps_operation_order IS '工序工单';
                
      COMMENT ON COLUMN erp_aps_operation_order.id IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_operation_order.code IS '编号';
                    
      COMMENT ON COLUMN erp_aps_operation_order.work_order_id IS '主工单';
                    
      COMMENT ON COLUMN erp_aps_operation_order.operation_name IS '工序名称';
                    
      COMMENT ON COLUMN erp_aps_operation_order.sequence IS '工序顺序';
                    
      COMMENT ON COLUMN erp_aps_operation_order.machine_id IS '工作中心/设备';
                    
      COMMENT ON COLUMN erp_aps_operation_order.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_aps_operation_order.planned_start_date_t IS '计划开工时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.planned_end_date_t IS '计划完工时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.real_start_date_t IS '实际开工时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.real_end_date_t IS '实际完工时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.setup_time IS '准备时间(分钟)';
                    
      COMMENT ON COLUMN erp_aps_operation_order.runtime_per_unit IS '每件加工时间(分钟)';
                    
      COMMENT ON COLUMN erp_aps_operation_order.qty IS '加工数量';
                    
      COMMENT ON COLUMN erp_aps_operation_order.total_duration IS '总耗时(分钟)';
                    
      COMMENT ON COLUMN erp_aps_operation_order.assigned_to_id IS '操作工';
                    
      COMMENT ON COLUMN erp_aps_operation_order.is_outsourced IS '是否外协';
                    
      COMMENT ON COLUMN erp_aps_operation_order.status IS '状态';
                    
      COMMENT ON COLUMN erp_aps_operation_order.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_operation_order.remark IS '备注';
                    
      COMMENT ON COLUMN erp_aps_operation_order.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_operation_order.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_operation_order.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_operation_order.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_operation_order.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_operation_order.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_aps_schedule IS '排产方案';
                
      COMMENT ON COLUMN erp_aps_schedule.id IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_schedule.code IS '编号';
                    
      COMMENT ON COLUMN erp_aps_schedule.name IS '方案名称';
                    
      COMMENT ON COLUMN erp_aps_schedule.schedule_date IS '排产日期';
                    
      COMMENT ON COLUMN erp_aps_schedule.scheduling_mode IS '排产模式';
                    
      COMMENT ON COLUMN erp_aps_schedule.horizon_start IS '展望期开始';
                    
      COMMENT ON COLUMN erp_aps_schedule.horizon_end IS '展望期结束';
                    
      COMMENT ON COLUMN erp_aps_schedule.status IS '状态';
                    
      COMMENT ON COLUMN erp_aps_schedule.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_schedule.remark IS '备注';
                    
      COMMENT ON COLUMN erp_aps_schedule.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_schedule.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_schedule.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_schedule.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_schedule.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_schedule.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_aps_constraint IS '排产约束';
                
      COMMENT ON COLUMN erp_aps_constraint.id IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_constraint.machine_id IS '工作中心/设备';
                    
      COMMENT ON COLUMN erp_aps_constraint.constraint_type IS '约束类型';
                    
      COMMENT ON COLUMN erp_aps_constraint.start_time IS '开始时间';
                    
      COMMENT ON COLUMN erp_aps_constraint.end_time IS '结束时间';
                    
      COMMENT ON COLUMN erp_aps_constraint.description IS '约束描述';
                    
      COMMENT ON COLUMN erp_aps_constraint.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_constraint.remark IS '备注';
                    
      COMMENT ON COLUMN erp_aps_constraint.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_constraint.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_constraint.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_constraint.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_constraint.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_constraint.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_aps_op_routing IS '替代工艺路线';
                
      COMMENT ON COLUMN erp_aps_op_routing.id IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_op_routing.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_op_routing.operation_id IS '工序';
                    
      COMMENT ON COLUMN erp_aps_op_routing.machine_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_aps_op_routing.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_aps_op_routing.setup_time_delta IS '换模时间差(分钟)';
                    
      COMMENT ON COLUMN erp_aps_op_routing.runtime_per_unit_delta IS '单件加工时间差(分钟)';
                    
      COMMENT ON COLUMN erp_aps_op_routing.is_default IS '默认路由';
                    
      COMMENT ON COLUMN erp_aps_op_routing.is_enabled IS '启用';
                    
      COMMENT ON COLUMN erp_aps_op_routing.effective_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_aps_op_routing.effective_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_aps_op_routing.min_batch_qty IS '最小批量';
                    
      COMMENT ON COLUMN erp_aps_op_routing.max_batch_qty IS '最大批量';
                    
      COMMENT ON COLUMN erp_aps_op_routing.remark IS '备注';
                    
      COMMENT ON COLUMN erp_aps_op_routing.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_op_routing.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_op_routing.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_op_routing.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_op_routing.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_op_routing.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_aps_dispatch_rule IS '自动派工规则';
                
      COMMENT ON COLUMN erp_aps_dispatch_rule.id IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.workcenter_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.rule_name IS '规则名称';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.enable_auto IS '自动派工启用';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.require_material IS '物料齐套检查';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.require_operator IS '操作工检查';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.require_tooling IS '工装检查';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.max_lookahead_minutes IS '前瞻窗口(分钟)';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.dispatch_ahead_minutes IS '提前派工(分钟)';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.auto_confirm_material IS '自动确认物料';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.max_concurrent_ops IS '最大并行工序数';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.priority_threshold IS '优先级阈值';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.enabled_hours IS '允许时段(JSON)';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.hold_until IS '暂停到';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.hold_reason IS '暂停原因';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.remark IS '备注';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_rule.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_aps_dispatch_log IS '派工日志';
                
      COMMENT ON COLUMN erp_aps_dispatch_log.id IS 'ID';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.operation_order_id IS '工序工单';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.workcenter_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.dispatch_type IS '派工类型';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.previous_status IS '派工前状态';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.new_status IS '派工后状态';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.condition_check_result IS '条件检查结果(JSON)';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.dispatched_by IS '派工人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.dispatched_at IS '派工时间';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.material_available IS '物料齐套';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.operator_available IS '操作工可用';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.tooling_available IS '工装可用';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.note IS '备注';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_aps_dispatch_log.update_time IS '修改时间';
                    
