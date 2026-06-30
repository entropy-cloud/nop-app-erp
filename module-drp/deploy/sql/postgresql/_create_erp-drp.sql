
CREATE TABLE erp_md_md_organization(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_organization primary key (id)
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


      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
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
                    
