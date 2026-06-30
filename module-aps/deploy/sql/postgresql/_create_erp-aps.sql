
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
  assigned_to_id INT8  ,
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
                    
