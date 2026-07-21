
CREATE TABLE erp_md_material(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  material_type INT4  ,
  status INT4  ,
  constraint PK_erp_md_material primary key (id)
);

CREATE TABLE erp_md_material_sku(
  id INT8  ,
  material_id INT8  ,
  sku_code VARCHAR(50)  ,
  barcode VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_material_sku primary key (id)
);

CREATE TABLE erp_md_uom(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  uom_group VARCHAR(50)  ,
  is_base BOOLEAN  ,
  constraint PK_erp_md_uom primary key (id)
);

CREATE TABLE erp_md_warehouse(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  warehouse_type INT4  ,
  org_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_warehouse primary key (id)
);

CREATE TABLE erp_mfg_workcenter(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  capacity NUMERIC(16,4)  ,
  capacity_unit VARCHAR(20)  ,
  hourly_rate NUMERIC(16,4)  ,
  work_hours_per_day NUMERIC(8,2)  ,
  is_external BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_workcenter primary key (id)
);

CREATE TABLE erp_mfg_routing(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200)  ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_routing primary key (id)
);

CREATE TABLE erp_md_organization(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  org_type INT4  ,
  parent_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_organization primary key (id)
);

CREATE TABLE erp_md_currency(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  symbol VARCHAR(50)  ,
  decimal_places INT4  ,
  is_functional BOOLEAN  ,
  constraint PK_erp_md_currency primary key (id)
);

CREATE TABLE erp_md_location(
  id INT8  ,
  warehouse_id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  parent_id INT8  ,
  constraint PK_erp_md_location primary key (id)
);

CREATE TABLE erp_md_partner(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  partner_type INT4  ,
  status INT4  ,
  credit_limit VARCHAR(50)  ,
  constraint PK_erp_md_partner primary key (id)
);

CREATE TABLE erp_md_employee(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_employee primary key (id)
);

CREATE TABLE erp_inv_batch(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_inv_batch primary key (id)
);

CREATE TABLE erp_mfg_bom(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  product_id INT8 NOT NULL ,
  bom_type VARCHAR(20) NOT NULL ,
  consumption VARCHAR(20)  ,
  is_active BOOLEAN default true   ,
  is_default BOOLEAN default false   ,
  use_multi_level_bom BOOLEAN default false   ,
  inspection_required BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  version_label VARCHAR(50)  ,
  qty NUMERIC(20,4) default 1   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_bom primary key (id)
);

CREATE TABLE erp_mfg_routing_operation(
  id INT8 NOT NULL ,
  routing_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  operation_code VARCHAR(50)  ,
  operation_name VARCHAR(200)  ,
  workcenter_id INT8  ,
  standard_time NUMERIC(12,2)  ,
  time_unit VARCHAR(20)  ,
  setup_time NUMERIC(12,2)  ,
  run_time NUMERIC(12,2)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_routing_operation primary key (id)
);

CREATE TABLE erp_mfg_workcenter_calendar(
  id INT8 NOT NULL ,
  workcenter_id INT8 NOT NULL ,
  org_id INT8  ,
  calendar_name VARCHAR(200) NOT NULL ,
  shift_type VARCHAR(20) NOT NULL ,
  work_date_pattern VARCHAR(20)  ,
  start_time VARCHAR(8)  ,
  end_time VARCHAR(8)  ,
  effective_from DATE  ,
  effective_to DATE  ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_workcenter_calendar primary key (id)
);

CREATE TABLE erp_mfg_workcenter_capacity(
  id INT8 NOT NULL ,
  workcenter_id INT8 NOT NULL ,
  org_id INT8  ,
  material_id INT8 NOT NULL ,
  capacity_per_hour NUMERIC(16,4) NOT NULL ,
  setup_time NUMERIC(12,2) default 0   ,
  cleanup_time NUMERIC(12,2) default 0   ,
  efficiency_factor NUMERIC(10,4) default 1   ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_workcenter_capacity primary key (id)
);

CREATE TABLE erp_mfg_mrp_plan(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  planning_horizon_days INT4  ,
  status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_mrp_plan primary key (id)
);

CREATE TABLE erp_mfg_forecast(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  plan_name VARCHAR(200) NOT NULL ,
  period_from DATE NOT NULL ,
  period_to DATE NOT NULL ,
  status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_forecast primary key (id)
);

CREATE TABLE erp_mfg_cost_rollup(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  costing_version VARCHAR(50)  ,
  status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_cost_rollup primary key (id)
);

CREATE TABLE erp_mfg_bom_byproduct(
  id INT8 NOT NULL ,
  bom_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  remark VARCHAR(1000)  ,
  byproduct_type VARCHAR(20)  ,
  yield_rate NUMERIC(10,4) default 100   ,
  cost_allocation_percent NUMERIC(10,4) default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_bom_byproduct primary key (id)
);

CREATE TABLE erp_mfg_production_version(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  product_id INT8 NOT NULL ,
  bom_id INT8 NOT NULL ,
  routing_id INT8 NOT NULL ,
  valid_from DATE  ,
  valid_to DATE  ,
  lot_size_from NUMERIC(20,4)  ,
  lot_size_to NUMERIC(20,4)  ,
  is_default BOOLEAN default false   ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_production_version primary key (id)
);

CREATE TABLE erp_mfg_bom_operation(
  id INT8 NOT NULL ,
  bom_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  operation_id INT8 NOT NULL ,
  workcenter_id INT8  ,
  standard_time NUMERIC(12,2)  ,
  time_unit VARCHAR(20)  ,
  rate NUMERIC(10,4) default 1   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_bom_operation primary key (id)
);

CREATE TABLE erp_mfg_mrp_plan_line(
  id INT8 NOT NULL ,
  mrp_plan_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  order_type VARCHAR(20) NOT NULL ,
  gross_requirement NUMERIC(20,4) NOT NULL ,
  scheduled_receipt NUMERIC(20,4) default 0   ,
  on_hand NUMERIC(20,4) default 0   ,
  net_requirement NUMERIC(20,4) NOT NULL ,
  planned_quantity NUMERIC(20,4) NOT NULL ,
  planned_date DATE NOT NULL ,
  parent_line_id INT8  ,
  is_firmed BOOLEAN default false   ,
  converted_bill_code VARCHAR(50)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_mrp_plan_line primary key (id)
);

CREATE TABLE erp_mfg_mrp_demand(
  id INT8 NOT NULL ,
  mrp_plan_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  demand_source VARCHAR(20) NOT NULL ,
  source_bill_type VARCHAR(50)  ,
  source_bill_code VARCHAR(50)  ,
  quantity NUMERIC(20,4) NOT NULL ,
  requirement_date DATE NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_mrp_demand primary key (id)
);

CREATE TABLE erp_mfg_mrp_scenario(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  base_mrp_plan_id INT8  ,
  description VARCHAR(1000)  ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_mrp_scenario primary key (id)
);

CREATE TABLE erp_mfg_forecast_line(
  id INT8 NOT NULL ,
  forecast_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  warehouse_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  period_start DATE NOT NULL ,
  period_end DATE NOT NULL ,
  forecast_qty NUMERIC(20,4) NOT NULL ,
  sourced_flag VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_forecast_line primary key (id)
);

CREATE TABLE erp_mfg_cost_rollup_line(
  id INT8 NOT NULL ,
  cost_rollup_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  material_cost NUMERIC(20,4) default 0   ,
  labor_cost NUMERIC(20,4) default 0   ,
  overhead_cost NUMERIC(20,4) default 0   ,
  subcontract_cost NUMERIC(20,4) default 0   ,
  total_cost NUMERIC(20,4) default 0   ,
  unit_cost NUMERIC(20,4) default 0   ,
  currency_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_cost_rollup_line primary key (id)
);

CREATE TABLE erp_mfg_work_order(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  bom_id INT8  ,
  routing_id INT8  ,
  production_version_id INT8  ,
  source_mrp_plan_id INT8  ,
  source_order_type VARCHAR(50)  ,
  source_order_code VARCHAR(50)  ,
  product_id INT8 NOT NULL ,
  planned_quantity NUMERIC(20,4) NOT NULL ,
  completed_quantity NUMERIC(20,4) default 0   ,
  scrapped_quantity NUMERIC(20,4) default 0   ,
  business_date DATE NOT NULL ,
  planned_start_date DATE  ,
  planned_end_date DATE  ,
  actual_start_date DATE  ,
  actual_end_date DATE  ,
  currency_id INT8  ,
  material_cost NUMERIC(20,4) default 0   ,
  labor_cost NUMERIC(20,4) default 0   ,
  overhead_cost NUMERIC(20,4) default 0   ,
  subcontract_cost NUMERIC(20,4) default 0   ,
  total_cost NUMERIC(20,4) default 0   ,
  unit_cost NUMERIC(20,4) default 0   ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20)  ,
  priority VARCHAR(20)  ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  source_schedule_id INT8  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  exchange_rate NUMERIC(20,8) default 1  NOT NULL ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_mfg_work_order primary key (id)
);

CREATE TABLE erp_mfg_bom_line(
  id INT8 NOT NULL ,
  bom_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  operation_id INT8  ,
  remark VARCHAR(1000)  ,
  scrap_rate NUMERIC(10,4) default 0   ,
  warehouse_id INT8  ,
  alternative_material_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_bom_line primary key (id)
);

CREATE TABLE erp_mfg_mrp_scenario_version(
  id INT8 NOT NULL ,
  scenario_id INT8 NOT NULL ,
  version_no INT4 NOT NULL ,
  computed_mrp_plan_id INT8  ,
  snapshot_summary VARCHAR(1000)  ,
  status VARCHAR(20) NOT NULL ,
  promoted_plan_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_mrp_scenario_version primary key (id)
);

CREATE TABLE erp_mfg_mrp_scenario_param(
  id INT8 NOT NULL ,
  scenario_id INT8 NOT NULL ,
  material_id INT8  ,
  param_type VARCHAR(20) NOT NULL ,
  param_value NUMERIC(20,4) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_mrp_scenario_param primary key (id)
);

CREATE TABLE erp_mfg_crp_load(
  id INT8 NOT NULL ,
  workcenter_id INT8 NOT NULL ,
  org_id INT8  ,
  work_order_id INT8  ,
  load_date DATE NOT NULL ,
  load_hours NUMERIC(12,2) default 0   ,
  setup_hours NUMERIC(12,2) default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_crp_load primary key (id)
);

CREATE TABLE erp_mfg_work_order_line(
  id INT8 NOT NULL ,
  work_order_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  line_type VARCHAR(20) NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  planned_quantity NUMERIC(20,4) NOT NULL ,
  actual_quantity NUMERIC(20,4) default 0   ,
  scrapped_quantity NUMERIC(20,4) default 0   ,
  source_warehouse_id INT8  ,
  dest_warehouse_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_work_order_line primary key (id)
);

CREATE TABLE erp_mfg_subcontract_order(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  work_order_id INT8  ,
  supplier_id INT8 NOT NULL ,
  workcenter_id INT8  ,
  routing_id INT8  ,
  production_version_id INT8  ,
  product_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1  NOT NULL ,
  processing_fee NUMERIC(20,4) default 0   ,
  total_amount NUMERIC(20,4) default 0   ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_status VARCHAR(20) NOT NULL ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  remark VARCHAR(1000)  ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  constraint PK_erp_mfg_subcontract_order primary key (id)
);

CREATE TABLE erp_mfg_job_card(
  id INT8 NOT NULL ,
  work_order_id INT8 NOT NULL ,
  operation_id INT8  ,
  line_no INT4 NOT NULL ,
  planned_quantity NUMERIC(20,4) NOT NULL ,
  completed_quantity NUMERIC(20,4) default 0   ,
  scrapped_quantity NUMERIC(20,4) default 0   ,
  status VARCHAR(30) NOT NULL ,
  workcenter_id INT8  ,
  actual_start_time TIMESTAMP  ,
  actual_end_time TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  code VARCHAR(50)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  source_schedule_id INT8  ,
  constraint PK_erp_mfg_job_card primary key (id)
);

CREATE TABLE erp_mfg_cost_variance(
  id INT8 NOT NULL ,
  work_order_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  variance_type VARCHAR(50) NOT NULL ,
  cost_element VARCHAR(50) NOT NULL ,
  material_id INT8  ,
  operation_id INT8  ,
  standard_amount NUMERIC(20,4) default 0   ,
  actual_amount NUMERIC(20,4) default 0   ,
  variance_amount NUMERIC(20,4) default 0   ,
  variance_percent NUMERIC(10,4) default 0   ,
  standard_qty NUMERIC(20,4) default 0   ,
  actual_qty NUMERIC(20,4) default 0   ,
  standard_price NUMERIC(20,4) default 0   ,
  actual_price NUMERIC(20,4) default 0   ,
  workcenter_id INT8  ,
  business_date DATE NOT NULL ,
  posted BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_cost_variance primary key (id)
);

CREATE TABLE erp_mfg_subcontract_order_line(
  id INT8 NOT NULL ,
  subcontract_order_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_processing_fee NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_subcontract_order_line primary key (id)
);

CREATE TABLE erp_mfg_material_issue(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  work_order_id INT8 NOT NULL ,
  job_card_id INT8  ,
  warehouse_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  currency_id INT8  ,
  exchange_rate NUMERIC(20,8) default 1  NOT NULL ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_mfg_material_issue primary key (id)
);

CREATE TABLE erp_mfg_job_card_time_log(
  id INT8 NOT NULL ,
  job_card_id INT8 NOT NULL ,
  work_order_id INT8 NOT NULL ,
  operator_id VARCHAR(36) NOT NULL ,
  work_date DATE NOT NULL ,
  start_time TIMESTAMP  ,
  end_time TIMESTAMP  ,
  duration_mins NUMERIC(12,2) NOT NULL ,
  setup_mins NUMERIC(12,2) default 0   ,
  run_mins NUMERIC(12,2) default 0   ,
  completed_quantity NUMERIC(20,4) default 0   ,
  scrapped_quantity NUMERIC(20,4) default 0   ,
  hourly_rate NUMERIC(16,4)  ,
  labor_cost NUMERIC(20,4) default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_job_card_time_log primary key (id)
);

CREATE TABLE erp_mfg_batch_genealogy(
  id INT8 NOT NULL ,
  work_order_id INT8 NOT NULL ,
  job_card_id INT8  ,
  operation_id INT8  ,
  input_lot_id INT8 NOT NULL ,
  input_material_id INT8 NOT NULL ,
  input_qty NUMERIC(20,4) NOT NULL ,
  input_uo_m_id INT8 NOT NULL ,
  output_lot_id INT8 NOT NULL ,
  output_material_id INT8 NOT NULL ,
  output_qty NUMERIC(20,4) NOT NULL ,
  output_uo_m_id INT8 NOT NULL ,
  production_date DATE NOT NULL ,
  production_time TIMESTAMP  ,
  line_no INT4 NOT NULL ,
  lot_status VARCHAR(50)  ,
  is_input_consumed BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_batch_genealogy primary key (id)
);

CREATE TABLE erp_mfg_material_issue_line(
  id INT8 NOT NULL ,
  issue_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  work_order_line_id INT8  ,
  required_quantity NUMERIC(20,4) NOT NULL ,
  issued_quantity NUMERIC(20,4) default 0   ,
  unit_cost NUMERIC(20,4)  ,
  total_cost NUMERIC(20,4)  ,
  batch_no VARCHAR(50)  ,
  location_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mfg_material_issue_line primary key (id)
);


      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_material_sku IS '物料SKU';
                
      COMMENT ON TABLE erp_md_uom IS '计量单位';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_mfg_workcenter IS '工作中心';
                
      COMMENT ON COLUMN erp_mfg_workcenter.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.code IS '工作中心编码';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.name IS '工作中心名称';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.capacity IS '产能';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.capacity_unit IS '产能单位';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.hourly_rate IS '小时费率';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.work_hours_per_day IS '日工时';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.is_external IS '是否外协';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_routing IS '工艺路线';
                
      COMMENT ON COLUMN erp_mfg_routing.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_routing.code IS '工艺路线编码';
                    
      COMMENT ON COLUMN erp_mfg_routing.name IS '工艺路线名称';
                    
      COMMENT ON COLUMN erp_mfg_routing.is_active IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_routing.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_routing.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_routing.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_routing.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_routing.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_routing.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_routing.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_employee IS '员工';
                
      COMMENT ON TABLE erp_inv_batch IS '批次';
                
      COMMENT ON TABLE erp_mfg_bom IS 'BOM';
                
      COMMENT ON COLUMN erp_mfg_bom.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_bom.code IS 'BOM编码';
                    
      COMMENT ON COLUMN erp_mfg_bom.product_id IS '产品';
                    
      COMMENT ON COLUMN erp_mfg_bom.bom_type IS 'BOM类型';
                    
      COMMENT ON COLUMN erp_mfg_bom.consumption IS '消耗控制';
                    
      COMMENT ON COLUMN erp_mfg_bom.is_active IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_bom.is_default IS '是否默认';
                    
      COMMENT ON COLUMN erp_mfg_bom.use_multi_level_bom IS '展开多层BOM';
                    
      COMMENT ON COLUMN erp_mfg_bom.inspection_required IS '需要质检';
                    
      COMMENT ON COLUMN erp_mfg_bom.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_bom.version_label IS '版本号';
                    
      COMMENT ON COLUMN erp_mfg_bom.qty IS 'BOM数量';
                    
      COMMENT ON COLUMN erp_mfg_bom.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_bom.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_bom.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_bom.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_bom.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_bom.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_routing_operation IS '工艺路线工序';
                
      COMMENT ON COLUMN erp_mfg_routing_operation.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.routing_id IS '工艺路线ID';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.operation_code IS '工序编码';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.operation_name IS '工序名称';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.workcenter_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.standard_time IS '标准工时';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.time_unit IS '时间单位';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.setup_time IS '准备时间';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.run_time IS '加工时间';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_routing_operation.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_workcenter_calendar IS '工作中心日历';
                
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.workcenter_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.calendar_name IS '日历名称';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.shift_type IS '班次类型';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.work_date_pattern IS '工作日模式';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.start_time IS '班次开始时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.end_time IS '班次结束时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.effective_from IS '生效起始日期';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.effective_to IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.is_active IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_calendar.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_workcenter_capacity IS '工作中心产能';
                
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.workcenter_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.material_id IS '关联物料';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.capacity_per_hour IS '每小时产能';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.setup_time IS '换模时间(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.cleanup_time IS '清理时间(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.efficiency_factor IS '效率系数';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.is_active IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_workcenter_capacity.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_mrp_plan IS 'MRP计划';
                
      COMMENT ON COLUMN erp_mfg_mrp_plan.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.code IS '计划号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.business_date IS '计划日期';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.planning_horizon_days IS '计划区间(天)';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.status IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_forecast IS '需求预测';
                
      COMMENT ON COLUMN erp_mfg_forecast.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_forecast.code IS '单号';
                    
      COMMENT ON COLUMN erp_mfg_forecast.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_forecast.plan_name IS '预测名称';
                    
      COMMENT ON COLUMN erp_mfg_forecast.period_from IS '预测区间起';
                    
      COMMENT ON COLUMN erp_mfg_forecast.period_to IS '预测区间止';
                    
      COMMENT ON COLUMN erp_mfg_forecast.status IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_forecast.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_forecast.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_forecast.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_forecast.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_forecast.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_forecast.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_forecast.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_cost_rollup IS '标准成本滚算';
                
      COMMENT ON COLUMN erp_mfg_cost_rollup.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.code IS '单号';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.business_date IS '计算日期';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.costing_version IS '成本核算版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.status IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_bom_byproduct IS 'BOM联副产品';
                
      COMMENT ON COLUMN erp_mfg_bom_byproduct.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.bom_id IS 'BOM ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.byproduct_type IS '联副产品类型';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.yield_rate IS '产出率(%)';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.cost_allocation_percent IS '成本分摊比例(%)';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_bom_byproduct.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_production_version IS '生产版本';
                
      COMMENT ON COLUMN erp_mfg_production_version.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_production_version.code IS '版本编码';
                    
      COMMENT ON COLUMN erp_mfg_production_version.product_id IS '产品';
                    
      COMMENT ON COLUMN erp_mfg_production_version.bom_id IS 'BOM';
                    
      COMMENT ON COLUMN erp_mfg_production_version.routing_id IS '工艺路线';
                    
      COMMENT ON COLUMN erp_mfg_production_version.valid_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_mfg_production_version.valid_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_mfg_production_version.lot_size_from IS '批量下限';
                    
      COMMENT ON COLUMN erp_mfg_production_version.lot_size_to IS '批量上限';
                    
      COMMENT ON COLUMN erp_mfg_production_version.is_default IS '是否默认版本';
                    
      COMMENT ON COLUMN erp_mfg_production_version.is_active IS '是否有效';
                    
      COMMENT ON COLUMN erp_mfg_production_version.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_production_version.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_production_version.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_production_version.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_production_version.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_production_version.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_production_version.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_bom_operation IS 'BOM工艺';
                
      COMMENT ON COLUMN erp_mfg_bom_operation.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.bom_id IS 'BOM ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.operation_id IS '工序ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.workcenter_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.standard_time IS '标准工时';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.time_unit IS '时间单位';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.rate IS '效率系数';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_bom_operation.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_mrp_plan_line IS 'MRP计划行';
                
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.mrp_plan_id IS 'MRP计划ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.order_type IS '建议类型';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.gross_requirement IS '毛需求';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.scheduled_receipt IS '预计入库';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.on_hand IS '现有库存';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.net_requirement IS '净需求';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.planned_quantity IS '计划数量';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.planned_date IS '计划到货/开工日期';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.parent_line_id IS '父需求行(多级 BOM 展开)';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.is_firmed IS '是否已转正式单据';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.converted_bill_code IS '转换后单据号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_plan_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_mrp_demand IS 'MRP独立需求';
                
      COMMENT ON COLUMN erp_mfg_mrp_demand.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.mrp_plan_id IS 'MRP计划ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.demand_source IS '需求来源';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.source_bill_type IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.source_bill_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.quantity IS '需求数量';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.requirement_date IS '需求日期';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_demand.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_mrp_scenario IS 'MRP仿真场景';
                
      COMMENT ON COLUMN erp_mfg_mrp_scenario.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.code IS '场景号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.base_mrp_plan_id IS '基线MRP计划';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.description IS '场景描述';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.status IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_forecast_line IS '需求预测行';
                
      COMMENT ON COLUMN erp_mfg_forecast_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.forecast_id IS '需求预测ID';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.warehouse_id IS '仓库（可选，空=产品级 MRP 消费，填=仓级 DRP 消费）';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.period_start IS '桶起始日期';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.period_end IS '桶结束日期';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.forecast_qty IS '预测数量';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.sourced_flag IS '来源标记';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_forecast_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_cost_rollup_line IS '标准成本滚算行';
                
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.cost_rollup_id IS '滚算单ID';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.material_id IS '产品';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.material_cost IS '材料成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.labor_cost IS '人工成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.overhead_cost IS '制造费用';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.subcontract_cost IS '委外成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.total_cost IS '总成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.unit_cost IS '单位标准成本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_cost_rollup_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_work_order IS '工单';
                
      COMMENT ON COLUMN erp_mfg_work_order.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_work_order.code IS '工单号';
                    
      COMMENT ON COLUMN erp_mfg_work_order.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_work_order.bom_id IS 'BOM';
                    
      COMMENT ON COLUMN erp_mfg_work_order.routing_id IS '工艺路线';
                    
      COMMENT ON COLUMN erp_mfg_work_order.production_version_id IS '生产版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.source_mrp_plan_id IS '来源 MRP 计划';
                    
      COMMENT ON COLUMN erp_mfg_work_order.source_order_type IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_mfg_work_order.source_order_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_mfg_work_order.product_id IS '产品(主产出)';
                    
      COMMENT ON COLUMN erp_mfg_work_order.planned_quantity IS '计划数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order.completed_quantity IS '完工数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order.scrapped_quantity IS '报废数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order.business_date IS '工单日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.planned_start_date IS '计划开工日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.planned_end_date IS '计划完工日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.actual_start_date IS '实际开工日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.actual_end_date IS '实际完工日期';
                    
      COMMENT ON COLUMN erp_mfg_work_order.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_mfg_work_order.material_cost IS '材料成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.labor_cost IS '人工成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.overhead_cost IS '制造费用';
                    
      COMMENT ON COLUMN erp_mfg_work_order.subcontract_cost IS '委外成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.total_cost IS '总成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.unit_cost IS '单位成本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_mfg_work_order.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_mfg_work_order.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_mfg_work_order.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_mfg_work_order.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_mfg_work_order.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_mfg_work_order.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_work_order.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_work_order.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_work_order.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_work_order.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_mfg_work_order.source_schedule_id IS 'APS排程来源(弱参照)';
                    
      COMMENT ON COLUMN erp_mfg_work_order.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_mfg_work_order.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_mfg_work_order.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_mfg_work_order.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_mfg_work_order.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_mfg_bom_line IS 'BOM行';
                
      COMMENT ON COLUMN erp_mfg_bom_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.bom_id IS 'BOM ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.operation_id IS '工序ID';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.scrap_rate IS '损耗率(%)';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.warehouse_id IS '发货仓库';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.alternative_material_id IS '替代物料';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_bom_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_mrp_scenario_version IS 'MRP仿真版本';
                
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.scenario_id IS '仿真场景';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.version_no IS '版本号';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.computed_mrp_plan_id IS '计算结果MRP计划';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.snapshot_summary IS '快照摘要';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.status IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.promoted_plan_id IS '转正式计划ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_version.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_mrp_scenario_param IS 'MRP仿真参数';
                
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.scenario_id IS '仿真场景';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.material_id IS '物料(空=全局覆盖)';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.param_type IS '参数类型';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.param_value IS '参数值';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_mrp_scenario_param.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_crp_load IS 'CRP负荷';
                
      COMMENT ON COLUMN erp_mfg_crp_load.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.workcenter_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.work_order_id IS '工单(弱指针)';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.load_date IS '负荷日期';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.load_hours IS '占用工时';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.setup_hours IS '换模工时';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_crp_load.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_work_order_line IS '工单行';
                
      COMMENT ON COLUMN erp_mfg_work_order_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.work_order_id IS '工单ID';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.line_type IS '行类型(OUTPUT 产出/INPUT 投入/BYPRODUCT 联副)';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.planned_quantity IS '计划数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.actual_quantity IS '实际数量(完工/领用)';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.scrapped_quantity IS '报废数量';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.source_warehouse_id IS '领料仓库(投入用)';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.dest_warehouse_id IS '入库仓库(产出用)';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_work_order_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_subcontract_order IS '委外加工单';
                
      COMMENT ON COLUMN erp_mfg_subcontract_order.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.code IS '单号';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.work_order_id IS '来源工单';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.supplier_id IS '委外供应商';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.workcenter_id IS '外协工作中心';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.routing_id IS '工艺路线';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.production_version_id IS '生产版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.product_id IS '产品';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.business_date IS '委外日期';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.processing_fee IS '加工费';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.total_amount IS '合计金额';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.posted_status IS '过账状态';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.amount_functional IS '本位币金额';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order.approved_at IS '审核时间';
                    
      COMMENT ON TABLE erp_mfg_job_card IS '作业卡';
                
      COMMENT ON COLUMN erp_mfg_job_card.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card.work_order_id IS '工单ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card.operation_id IS '工序ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_job_card.planned_quantity IS '计划数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card.completed_quantity IS '完工数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card.scrapped_quantity IS '报废数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card.status IS '状态';
                    
      COMMENT ON COLUMN erp_mfg_job_card.workcenter_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_job_card.actual_start_time IS '实际开始时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card.actual_end_time IS '实际结束时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_job_card.code IS '作业卡编号';
                    
      COMMENT ON COLUMN erp_mfg_job_card.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_job_card.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_job_card.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_job_card.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_job_card.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card.source_schedule_id IS 'APS排程来源(弱参照)';
                    
      COMMENT ON TABLE erp_mfg_cost_variance IS '成本差异记录';
                
      COMMENT ON COLUMN erp_mfg_cost_variance.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.work_order_id IS '工单';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.variance_type IS '差异类型';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.cost_element IS '成本要素';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.operation_id IS '工序';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.standard_amount IS '标准金额';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.actual_amount IS '实际金额';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.variance_amount IS '差异金额';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.variance_percent IS '差异百分比';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.standard_qty IS '标准数量';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.actual_qty IS '实际数量';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.standard_price IS '标准单价';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.actual_price IS '实际单价';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.workcenter_id IS '工作中心';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_cost_variance.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_subcontract_order_line IS '委外加工单行';
                
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.subcontract_order_id IS '委外单ID';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.quantity IS '委外数量';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.unit_processing_fee IS '单位加工费';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.amount IS '金额';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_subcontract_order_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_material_issue IS '领料单';
                
      COMMENT ON COLUMN erp_mfg_material_issue.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.code IS '单号';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.work_order_id IS '工单';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.job_card_id IS '作业卡';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.warehouse_id IS '发料仓库';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.business_date IS '领料日期';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_mfg_material_issue.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_mfg_job_card_time_log IS '作业工时记录';
                
      COMMENT ON COLUMN erp_mfg_job_card_time_log.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.job_card_id IS '作业卡ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.work_order_id IS '工单ID';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.operator_id IS '操作员(职员)';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.work_date IS '作业日期';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.start_time IS '开始时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.end_time IS '结束时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.duration_mins IS '工时(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.setup_mins IS '准备时间(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.run_mins IS '加工时间(分钟)';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.completed_quantity IS '本班完工数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.scrapped_quantity IS '本班报废数量';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.hourly_rate IS '小时费率';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.labor_cost IS '人工成本';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_job_card_time_log.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_batch_genealogy IS '批次追溯';
                
      COMMENT ON COLUMN erp_mfg_batch_genealogy.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.work_order_id IS '工单';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.job_card_id IS '作业卡';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.operation_id IS '工序';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.input_lot_id IS '输入批次';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.input_material_id IS '输入物料';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.input_qty IS '投入数量';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.input_uo_m_id IS '投入计量单位';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.output_lot_id IS '产出批次';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.output_material_id IS '产出物料';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.output_qty IS '产出数量';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.output_uo_m_id IS '产出计量单位';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.production_date IS '生产日期';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.production_time IS '生产时间';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.lot_status IS '批次状态';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.is_input_consumed IS '输入是否已消耗';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_batch_genealogy.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mfg_material_issue_line IS '领料单行';
                
      COMMENT ON COLUMN erp_mfg_material_issue_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.issue_id IS '领料单ID';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.work_order_line_id IS '对应工单投入行';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.required_quantity IS '应领数量';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.issued_quantity IS '实领数量';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.unit_cost IS '单位成本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.total_cost IS '总成本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.location_id IS '库位';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mfg_material_issue_line.update_time IS '修改时间';
                    
