
CREATE TABLE erp_md_md_organization(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_organization primary key (id)
);

CREATE TABLE erp_md_material(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_material primary key (id)
);

CREATE TABLE erp_md_warehouse(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_warehouse primary key (id)
);

CREATE TABLE erp_md_partner(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_partner primary key (id)
);

CREATE TABLE erp_md_location(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_location primary key (id)
);

CREATE TABLE erp_inv_stock_move(
  id INT8  ,
  code VARCHAR(50)  ,
  constraint PK_erp_inv_stock_move primary key (id)
);

CREATE TABLE erp_drp_plan(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  plan_name VARCHAR(200) NOT NULL ,
  period_from DATE NOT NULL ,
  period_to DATE NOT NULL ,
  status INT4 NOT NULL ,
  total_replenishment_qty NUMERIC(20,4)  ,
  run_at TIMESTAMP  ,
  run_by VARCHAR(50)  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_drp_plan primary key (id)
);

CREATE TABLE erp_inv_drp_safety_stock_calc(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  material_id INT8 NOT NULL ,
  warehouse_id INT8  ,
  method INT4 NOT NULL ,
  service_level INT4 NOT NULL ,
  history_months INT4 default 6   ,
  lead_time_days INT4 NOT NULL ,
  calculated_safety_stock NUMERIC(20,4)  ,
  calculated_rop NUMERIC(20,4)  ,
  override_safety_stock NUMERIC(20,4)  ,
  last_calculated_at TIMESTAMP  ,
  overwritten_by VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_drp_safety_stock_calc primary key (id)
);

CREATE TABLE erp_drp_parameter(
  id INT8 NOT NULL ,
  warehouse_id INT8 NOT NULL ,
  material_id INT8 NOT NULL ,
  safety_stock NUMERIC(20,4)  ,
  replenishment_lead_time INT4  ,
  order_multiple NUMERIC(20,4)  ,
  preferred_source_warehouse_id INT8  ,
  preferred_supplier_id INT8  ,
  replenishment_method INT4 NOT NULL ,
  min_stock_level NUMERIC(20,4)  ,
  max_stock_level NUMERIC(20,4)  ,
  review_period_days INT4  ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_drp_parameter primary key (id)
);

CREATE TABLE erp_inv_drp_lead_time_record(
  id INT8 NOT NULL ,
  org_id INT8  ,
  supplier_id INT8 NOT NULL ,
  material_id INT8 NOT NULL ,
  order_date DATE NOT NULL ,
  receipt_date DATE NOT NULL ,
  actual_lead_time INT4 NOT NULL ,
  expected_lead_time INT4  ,
  variance_days INT4  ,
  purchase_order_code VARCHAR(50)  ,
  is_on_time BOOLEAN default true   ,
  early_late_flag VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_drp_lead_time_record primary key (id)
);

CREATE TABLE erp_drp_line(
  id INT8 NOT NULL ,
  plan_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  warehouse_id INT8 NOT NULL ,
  source_warehouse_id INT8  ,
  replenishment_type INT4 NOT NULL ,
  current_stock NUMERIC(20,4)  ,
  allocated_qty NUMERIC(20,4)  ,
  on_order_qty NUMERIC(20,4)  ,
  forecast_demand NUMERIC(20,4)  ,
  safety_stock NUMERIC(20,4)  ,
  net_requirement NUMERIC(20,4)  ,
  suggested_qty NUMERIC(20,4)  ,
  approved_qty NUMERIC(20,4)  ,
  order_bill_type VARCHAR(50)  ,
  order_bill_code VARCHAR(50)  ,
  status INT4 NOT NULL ,
  org_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_drp_line primary key (id)
);

CREATE TABLE erp_inv_drp_cross_dock(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  drp_line_id INT8  ,
  inbound_move_id INT8  ,
  outbound_move_id INT8  ,
  source_bill_type VARCHAR(50)  ,
  source_bill_code VARCHAR(50)  ,
  target_bill_type VARCHAR(50)  ,
  target_bill_code VARCHAR(50)  ,
  material_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  staging_location_id INT8  ,
  dock_slot_time TIMESTAMP  ,
  status INT4 NOT NULL ,
  matched_at TIMESTAMP  ,
  loaded_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_drp_cross_dock primary key (id)
);

CREATE TABLE erp_inv_drp_dock_appointment(
  id INT8 NOT NULL ,
  warehouse_id INT8 NOT NULL ,
  dock_id INT8 NOT NULL ,
  appointment_date DATE NOT NULL ,
  slot_start TIMESTAMP NOT NULL ,
  slot_end TIMESTAMP NOT NULL ,
  cross_dock_id INT8  ,
  carrier_info VARCHAR(500)  ,
  status VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  org_id INT8  ,
  constraint PK_erp_inv_drp_dock_appointment primary key (id)
);


      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON TABLE erp_inv_stock_move IS '库存移动';
                
      COMMENT ON TABLE erp_drp_plan IS 'DRP计划';
                
      COMMENT ON COLUMN erp_drp_plan.id IS 'ID';
                    
      COMMENT ON COLUMN erp_drp_plan.code IS '编号';
                    
      COMMENT ON COLUMN erp_drp_plan.plan_name IS '计划名称';
                    
      COMMENT ON COLUMN erp_drp_plan.period_from IS '期间开始';
                    
      COMMENT ON COLUMN erp_drp_plan.period_to IS '期间结束';
                    
      COMMENT ON COLUMN erp_drp_plan.status IS '状态';
                    
      COMMENT ON COLUMN erp_drp_plan.total_replenishment_qty IS '总补货数量';
                    
      COMMENT ON COLUMN erp_drp_plan.run_at IS '运行时间';
                    
      COMMENT ON COLUMN erp_drp_plan.run_by IS '运行人';
                    
      COMMENT ON COLUMN erp_drp_plan.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_drp_plan.remark IS '备注';
                    
      COMMENT ON COLUMN erp_drp_plan.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_drp_plan.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_drp_plan.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_drp_plan.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_drp_plan.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_drp_plan.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_drp_safety_stock_calc IS '安全库存计算';
                
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.code IS '编号';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.method IS '计算方法';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.service_level IS '服务水平';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.history_months IS '分析月数';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.lead_time_days IS '提前期(天)';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.calculated_safety_stock IS '建议安全库存';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.calculated_rop IS '建议再订货点';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.override_safety_stock IS '覆盖安全库存';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.last_calculated_at IS '计算时间';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.overwritten_by IS '覆盖人';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_drp_safety_stock_calc.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_drp_parameter IS '仓库补货参数';
                
      COMMENT ON COLUMN erp_drp_parameter.id IS 'ID';
                    
      COMMENT ON COLUMN erp_drp_parameter.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_drp_parameter.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_drp_parameter.safety_stock IS '安全库存';
                    
      COMMENT ON COLUMN erp_drp_parameter.replenishment_lead_time IS '补货提前期(天)';
                    
      COMMENT ON COLUMN erp_drp_parameter.order_multiple IS '订货倍数';
                    
      COMMENT ON COLUMN erp_drp_parameter.preferred_source_warehouse_id IS '首选调出仓库';
                    
      COMMENT ON COLUMN erp_drp_parameter.preferred_supplier_id IS '首选供应商';
                    
      COMMENT ON COLUMN erp_drp_parameter.replenishment_method IS '补货方法';
                    
      COMMENT ON COLUMN erp_drp_parameter.min_stock_level IS '最低库存';
                    
      COMMENT ON COLUMN erp_drp_parameter.max_stock_level IS '最高库存';
                    
      COMMENT ON COLUMN erp_drp_parameter.review_period_days IS '审视周期(天)';
                    
      COMMENT ON COLUMN erp_drp_parameter.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_drp_parameter.remark IS '备注';
                    
      COMMENT ON COLUMN erp_drp_parameter.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_drp_parameter.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_drp_parameter.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_drp_parameter.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_drp_parameter.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_drp_parameter.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_drp_lead_time_record IS '提前期记录';
                
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.order_date IS '订单日期';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.receipt_date IS '入库日期';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.actual_lead_time IS '实际提前期(天)';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.expected_lead_time IS '预期提前期(天)';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.variance_days IS '偏差天数';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.purchase_order_code IS '采购单号';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.is_on_time IS '是否准时';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.early_late_flag IS '提前/延迟标记';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_drp_lead_time_record.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_drp_line IS 'DRP明细';
                
      COMMENT ON COLUMN erp_drp_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_drp_line.plan_id IS '计划ID';
                    
      COMMENT ON COLUMN erp_drp_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_drp_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_drp_line.warehouse_id IS '目标仓库';
                    
      COMMENT ON COLUMN erp_drp_line.source_warehouse_id IS '来源仓库';
                    
      COMMENT ON COLUMN erp_drp_line.replenishment_type IS '补货类型';
                    
      COMMENT ON COLUMN erp_drp_line.current_stock IS '当前库存';
                    
      COMMENT ON COLUMN erp_drp_line.allocated_qty IS '已分配量';
                    
      COMMENT ON COLUMN erp_drp_line.on_order_qty IS '在单量';
                    
      COMMENT ON COLUMN erp_drp_line.forecast_demand IS '预测需求量';
                    
      COMMENT ON COLUMN erp_drp_line.safety_stock IS '安全库存';
                    
      COMMENT ON COLUMN erp_drp_line.net_requirement IS '净需求';
                    
      COMMENT ON COLUMN erp_drp_line.suggested_qty IS '建议补货量';
                    
      COMMENT ON COLUMN erp_drp_line.approved_qty IS '批准补货量';
                    
      COMMENT ON COLUMN erp_drp_line.order_bill_type IS '生成单据类型';
                    
      COMMENT ON COLUMN erp_drp_line.order_bill_code IS '生成单据号';
                    
      COMMENT ON COLUMN erp_drp_line.status IS '状态';
                    
      COMMENT ON COLUMN erp_drp_line.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_drp_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_drp_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_drp_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_drp_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_drp_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_drp_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_drp_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_drp_cross_dock IS '越库执行记录';
                
      COMMENT ON COLUMN erp_inv_drp_cross_dock.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.code IS '编号';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.drp_line_id IS 'DRP行';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.inbound_move_id IS '入站移动单';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.outbound_move_id IS '出站移动单';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.source_bill_type IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.source_bill_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.target_bill_type IS '目标单据类型';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.target_bill_code IS '目标单据号';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.quantity IS '越库数量';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.staging_location_id IS '暂存库位';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.dock_slot_time IS '月台时间窗口';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.status IS '状态';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.matched_at IS '匹配时间';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.loaded_at IS '装车完成时间';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_drp_cross_dock.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_drp_dock_appointment IS '月台预约';
                
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.dock_id IS '月台';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.appointment_date IS '预约日期';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.slot_start IS '时间窗口开始';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.slot_end IS '时间窗口结束';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.cross_dock_id IS '关联越库';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.carrier_info IS '承运商信息';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.status IS '状态';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_inv_drp_dock_appointment.org_id IS '业务组织';
                    
