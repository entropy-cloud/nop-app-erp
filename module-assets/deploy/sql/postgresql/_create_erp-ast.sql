
CREATE TABLE erp_md_organization(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  org_type INT4  ,
  parent_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_organization primary key (id)
);

CREATE TABLE erp_md_location(
  id INT8  ,
  warehouse_id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  parent_id INT8  ,
  constraint PK_erp_md_location primary key (id)
);

CREATE TABLE erp_md_employee(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  org_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_employee primary key (id)
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

CREATE TABLE erp_md_subject(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  parent_id INT8  ,
  subject_class INT4  ,
  direction INT4  ,
  constraint PK_erp_md_subject primary key (id)
);

CREATE TABLE erp_md_material_category(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  parent_id INT8  ,
  constraint PK_erp_md_material_category primary key (id)
);

CREATE TABLE erp_ast_asset_category(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  depreciation_method VARCHAR(20)  ,
  useful_life_months INT4  ,
  subject_id INT8  ,
  depreciation_subject_id INT8  ,
  expense_subject_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  disposal_gain_loss_subject_id INT8  ,
  cip_subject_id INT8  ,
  constraint PK_erp_ast_asset_category primary key (id)
);

CREATE TABLE erp_ast_asset(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  category_id INT8  ,
  acquisition_date DATE NOT NULL ,
  currency_id INT8  ,
  original_value NUMERIC(20,4) NOT NULL ,
  current_value NUMERIC(20,4)  ,
  residual_value NUMERIC(20,4) default 0   ,
  depreciation_method VARCHAR(20)  ,
  depreciation_rate NUMERIC(10,6)  ,
  useful_life_months INT4  ,
  department_id INT8  ,
  location_id INT8  ,
  employee_id INT8  ,
  staff_id INT8  ,
  brand_model VARCHAR(200)  ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  accumulated_depreciation NUMERIC(20,4) default 0   ,
  net_book_value NUMERIC(20,4) default 0   ,
  constraint PK_erp_ast_asset primary key (id)
);

CREATE TABLE erp_ast_asset_capitalization(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  asset_code VARCHAR(50)  ,
  asset_name VARCHAR(200)  ,
  category_id INT8  ,
  currency_id INT8  ,
  capitalization_date DATE NOT NULL ,
  original_value NUMERIC(20,4) NOT NULL ,
  source_type VARCHAR(20) NOT NULL ,
  source_code VARCHAR(50)  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  business_date DATE  ,
  exchange_rate NUMERIC(20,8) NOT NULL ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_ast_asset_capitalization primary key (id)
);

CREATE TABLE erp_ast_inventory(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200)  ,
  org_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  range_department_id INT8  ,
  range_category_id INT8  ,
  range_location_id INT8  ,
  responsible_by_id INT8  ,
  business_date DATE NOT NULL ,
  currency_id INT8  ,
  exchange_rate NUMERIC(20,8) default 1   ,
  surplus_count INT4 default 0   ,
  shortage_count INT4 default 0   ,
  matched_count INT4 default 0   ,
  surplus_amount NUMERIC(20,4) default 0   ,
  shortage_amount NUMERIC(20,4) default 0   ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_ast_inventory primary key (id)
);

CREATE TABLE erp_ast_depreciation_schedule(
  id INT8 NOT NULL ,
  asset_id INT8 NOT NULL ,
  org_id INT8  ,
  period VARCHAR(20) NOT NULL ,
  planned_amount NUMERIC(20,4) NOT NULL ,
  actual_amount NUMERIC(20,4)  ,
  accumulated_depreciation NUMERIC(20,4) default 0   ,
  net_book_value NUMERIC(20,4)  ,
  status VARCHAR(20) NOT NULL ,
  executed_at TIMESTAMP  ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  voucher_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  business_date DATE  ,
  currency_id INT8  ,
  exchange_rate NUMERIC(20,8) default 1  NOT NULL ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_ast_depreciation_schedule primary key (id)
);

CREATE TABLE erp_ast_movement(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  asset_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  from_date DATE NOT NULL ,
  thru_date DATE  ,
  from_department_id INT8  ,
  to_department_id INT8  ,
  from_staff_id INT8  ,
  to_staff_id INT8  ,
  from_location_id INT8  ,
  to_location_id INT8  ,
  handler_id INT8  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  doc_version VARCHAR(20) default '1'  NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  currency_id INT8  ,
  exchange_rate NUMERIC(20,8) NOT NULL ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_ast_movement primary key (id)
);

CREATE TABLE erp_ast_value_adjustment(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  asset_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  currency_id INT8  ,
  exchange_rate NUMERIC(20,8) default 1   ,
  adjustment_type VARCHAR(20) NOT NULL ,
  adjustment_amount NUMERIC(20,4) NOT NULL ,
  reason VARCHAR(500)  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_ast_value_adjustment primary key (id)
);

CREATE TABLE erp_ast_disposal(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  asset_id INT8 NOT NULL ,
  disposal_type VARCHAR(20) NOT NULL ,
  disposal_amount NUMERIC(20,4)  ,
  currency_id INT8  ,
  exchange_rate NUMERIC(20,8) default 1   ,
  business_date DATE NOT NULL ,
  gain_loss NUMERIC(20,4)  ,
  reason VARCHAR(20)  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  nop_flow_id VARCHAR(32)  ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_ast_disposal primary key (id)
);

CREATE TABLE erp_ast_cip(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  category_id INT8  ,
  currency_id INT8  ,
  business_date DATE NOT NULL ,
  estimated_completion_date DATE  ,
  accumulated_cost NUMERIC(20,4) default 0   ,
  is_completed BOOLEAN default false   ,
  completed_asset_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  project_id INT8  ,
  cip_asset_category_snapshot VARCHAR(500)  ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(50)  ,
  exchange_rate NUMERIC(20,8) NOT NULL ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_ast_cip primary key (id)
);

CREATE TABLE erp_ast_split(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  source_asset_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  currency_id INT8  ,
  split_reason VARCHAR(500)  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  exchange_rate NUMERIC(20,8) NOT NULL ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_ast_split primary key (id)
);

CREATE TABLE erp_ast_merge(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  target_asset_id INT8  ,
  business_date DATE NOT NULL ,
  currency_id INT8  ,
  merge_reason VARCHAR(500)  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  exchange_rate NUMERIC(20,8) NOT NULL ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  constraint PK_erp_ast_merge primary key (id)
);

CREATE TABLE erp_ast_maintenance(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200)  ,
  org_id INT8  ,
  asset_id INT8 NOT NULL ,
  maintenance_visit_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  treatment VARCHAR(20)  ,
  capitalized_amount NUMERIC(20,4) default 0   ,
  total_cost_amount NUMERIC(20,4) default 0   ,
  business_date DATE NOT NULL ,
  currency_id INT8  ,
  exchange_rate NUMERIC(20,8) default 1   ,
  reason VARCHAR(500)  ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  reversed BOOLEAN default false   ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_ast_maintenance primary key (id)
);

CREATE TABLE erp_ast_inventory_line(
  id INT8 NOT NULL ,
  inventory_id INT8 NOT NULL ,
  org_id INT8  ,
  line_no INT4  ,
  asset_id INT8  ,
  asset_code_snapshot VARCHAR(50)  ,
  asset_name_snapshot VARCHAR(200)  ,
  category_id INT8  ,
  book_quantity INT4 default 0   ,
  actual_quantity INT4  ,
  variance_quantity INT4 default 0   ,
  variance_type VARCHAR(20)  ,
  book_value NUMERIC(20,4) default 0   ,
  assessed_value NUMERIC(20,4) default 0   ,
  variance_amount NUMERIC(20,4) default 0   ,
  disposition VARCHAR(20)  ,
  new_asset_id INT8  ,
  capitalization_id INT8  ,
  disposal_id INT8  ,
  investigated_remark VARCHAR(500)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_ast_inventory_line primary key (id)
);

CREATE TABLE erp_ast_cip_cost_item(
  id INT8 NOT NULL ,
  cip_id INT8 NOT NULL ,
  org_id INT8  ,
  line_no INT4  ,
  cost_type VARCHAR(30) NOT NULL ,
  amount_source NUMERIC(20,4) default 0   ,
  exchange_rate NUMERIC(20,8) default 1  NOT NULL ,
  amount_functional NUMERIC(20,4) default 0  NOT NULL ,
  source_bill_type VARCHAR(30)  ,
  source_bill_code VARCHAR(50)  ,
  posted_transfer_flag BOOLEAN default false   ,
  capitalization_id INT8  ,
  business_date DATE  ,
  currency_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_ast_cip_cost_item primary key (id)
);

CREATE TABLE erp_ast_cip_progress_billing(
  id INT8 NOT NULL ,
  cip_id INT8 NOT NULL ,
  org_id INT8  ,
  line_no INT4  ,
  billing_date DATE NOT NULL ,
  billing_milestone VARCHAR(200)  ,
  amount_source NUMERIC(20,4) default 0   ,
  exchange_rate NUMERIC(20,8) default 1  NOT NULL ,
  amount_functional NUMERIC(20,4) default 0  NOT NULL ,
  payment_voucher_code VARCHAR(50)  ,
  paid_flag BOOLEAN default false   ,
  currency_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_ast_cip_progress_billing primary key (id)
);

CREATE TABLE erp_ast_split_line(
  id INT8 NOT NULL ,
  split_id INT8 NOT NULL ,
  org_id INT8  ,
  line_no INT4  ,
  target_asset_code VARCHAR(50) NOT NULL ,
  target_asset_name VARCHAR(200)  ,
  category_id INT8  ,
  allocation_method VARCHAR(20)  ,
  proportion NUMERIC(8,6) default 0   ,
  original_cost_amount NUMERIC(20,4) default 0  NOT NULL ,
  accumulated_depreciation_amount NUMERIC(20,4) default 0   ,
  net_book_value NUMERIC(20,4) default 0   ,
  target_asset_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_ast_split_line primary key (id)
);

CREATE TABLE erp_ast_merge_line(
  id INT8 NOT NULL ,
  merge_id INT8 NOT NULL ,
  org_id INT8  ,
  line_no INT4  ,
  source_asset_id INT8 NOT NULL ,
  contribution_proportion NUMERIC(8,6) default 0   ,
  original_cost_amount NUMERIC(20,4) default 0  NOT NULL ,
  accumulated_depreciation_amount NUMERIC(20,4) default 0   ,
  net_book_value NUMERIC(20,4) default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_ast_merge_line primary key (id)
);

CREATE TABLE erp_ast_maintenance_cost(
  id INT8 NOT NULL ,
  maintenance_id INT8 NOT NULL ,
  org_id INT8  ,
  line_no INT4  ,
  cost_type VARCHAR(20) NOT NULL ,
  amount NUMERIC(20,4) default 0   ,
  business_date DATE  ,
  currency_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_ast_maintenance_cost primary key (id)
);


      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_subject IS '会计科目';
                
      COMMENT ON TABLE erp_md_material_category IS '物料分类';
                
      COMMENT ON TABLE erp_ast_asset_category IS '资产类别';
                
      COMMENT ON COLUMN erp_ast_asset_category.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_asset_category.code IS '类别编码';
                    
      COMMENT ON COLUMN erp_ast_asset_category.name IS '类别名称';
                    
      COMMENT ON COLUMN erp_ast_asset_category.depreciation_method IS '默认折旧方法';
                    
      COMMENT ON COLUMN erp_ast_asset_category.useful_life_months IS '默认使用年限(月)';
                    
      COMMENT ON COLUMN erp_ast_asset_category.subject_id IS '资产科目';
                    
      COMMENT ON COLUMN erp_ast_asset_category.depreciation_subject_id IS '累计折旧科目';
                    
      COMMENT ON COLUMN erp_ast_asset_category.expense_subject_id IS '折旧费用科目';
                    
      COMMENT ON COLUMN erp_ast_asset_category.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_asset_category.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_asset_category.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_asset_category.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_asset_category.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_asset_category.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_asset_category.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ast_asset_category.disposal_gain_loss_subject_id IS '清理损益科目';
                    
      COMMENT ON COLUMN erp_ast_asset_category.cip_subject_id IS '在建工程科目';
                    
      COMMENT ON TABLE erp_ast_asset IS '固定资产';
                
      COMMENT ON COLUMN erp_ast_asset.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_asset.code IS '资产编码';
                    
      COMMENT ON COLUMN erp_ast_asset.name IS '资产名称';
                    
      COMMENT ON COLUMN erp_ast_asset.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_asset.category_id IS '资产类别';
                    
      COMMENT ON COLUMN erp_ast_asset.acquisition_date IS '取得日期';
                    
      COMMENT ON COLUMN erp_ast_asset.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_asset.original_value IS '原值';
                    
      COMMENT ON COLUMN erp_ast_asset.current_value IS '当前价值';
                    
      COMMENT ON COLUMN erp_ast_asset.residual_value IS '残值';
                    
      COMMENT ON COLUMN erp_ast_asset.depreciation_method IS '折旧方法';
                    
      COMMENT ON COLUMN erp_ast_asset.depreciation_rate IS '折旧率';
                    
      COMMENT ON COLUMN erp_ast_asset.useful_life_months IS '使用年限(月)';
                    
      COMMENT ON COLUMN erp_ast_asset.department_id IS '使用部门';
                    
      COMMENT ON COLUMN erp_ast_asset.location_id IS '使用地点';
                    
      COMMENT ON COLUMN erp_ast_asset.employee_id IS '使用人(职员)';
                    
      COMMENT ON COLUMN erp_ast_asset.staff_id IS '使用人(往来单位,旧字段保留)';
                    
      COMMENT ON COLUMN erp_ast_asset.brand_model IS '品牌型号';
                    
      COMMENT ON COLUMN erp_ast_asset.status IS '资产状态';
                    
      COMMENT ON COLUMN erp_ast_asset.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_asset.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_asset.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_asset.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_asset.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_asset.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_asset.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ast_asset.accumulated_depreciation IS '累计折旧';
                    
      COMMENT ON COLUMN erp_ast_asset.net_book_value IS '净值';
                    
      COMMENT ON TABLE erp_ast_asset_capitalization IS '资产资本化';
                
      COMMENT ON COLUMN erp_ast_asset_capitalization.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.code IS '单号';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.asset_code IS '资产编码';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.asset_name IS '资产名称';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.category_id IS '资产类别';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.capitalization_date IS '资本化日期';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.original_value IS '原值';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.source_type IS '来源类型';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.source_code IS '来源单号';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_asset_capitalization.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_inventory IS '资产盘点单';
                
      COMMENT ON COLUMN erp_ast_inventory.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_inventory.code IS '盘点单号';
                    
      COMMENT ON COLUMN erp_ast_inventory.name IS '盘点名称';
                    
      COMMENT ON COLUMN erp_ast_inventory.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_inventory.status IS '盘点状态';
                    
      COMMENT ON COLUMN erp_ast_inventory.range_department_id IS '范围-部门';
                    
      COMMENT ON COLUMN erp_ast_inventory.range_category_id IS '范围-资产类别';
                    
      COMMENT ON COLUMN erp_ast_inventory.range_location_id IS '范围-地点';
                    
      COMMENT ON COLUMN erp_ast_inventory.responsible_by_id IS '盘点负责人';
                    
      COMMENT ON COLUMN erp_ast_inventory.business_date IS '盘点基准日';
                    
      COMMENT ON COLUMN erp_ast_inventory.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_inventory.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_inventory.surplus_count IS '盘盈行数';
                    
      COMMENT ON COLUMN erp_ast_inventory.shortage_count IS '盘亏行数';
                    
      COMMENT ON COLUMN erp_ast_inventory.matched_count IS '一致行数';
                    
      COMMENT ON COLUMN erp_ast_inventory.surplus_amount IS '盘盈金额';
                    
      COMMENT ON COLUMN erp_ast_inventory.shortage_amount IS '盘亏金额';
                    
      COMMENT ON COLUMN erp_ast_inventory.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_inventory.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_inventory.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_inventory.approved_by IS '复核人';
                    
      COMMENT ON COLUMN erp_ast_inventory.approved_at IS '复核时间';
                    
      COMMENT ON COLUMN erp_ast_inventory.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_inventory.amount_functional IS '本位币金额';
                    
      COMMENT ON COLUMN erp_ast_inventory.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_inventory.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_inventory.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_inventory.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_inventory.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_inventory.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_inventory.remark IS '备注';
                    
      COMMENT ON TABLE erp_ast_depreciation_schedule IS '折旧计划';
                
      COMMENT ON COLUMN erp_ast_depreciation_schedule.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.asset_id IS '资产ID';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.period IS '折旧期间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.planned_amount IS '计划折旧额';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.actual_amount IS '实际折旧额';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.accumulated_depreciation IS '累计折旧';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.net_book_value IS '净值';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.status IS '执行状态';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.executed_at IS '执行时间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.voucher_id IS '凭证ID';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_depreciation_schedule.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_movement IS '资产移动';
                
      COMMENT ON COLUMN erp_ast_movement.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_movement.code IS '单号';
                    
      COMMENT ON COLUMN erp_ast_movement.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_movement.asset_id IS '资产ID';
                    
      COMMENT ON COLUMN erp_ast_movement.business_date IS '移动日期';
                    
      COMMENT ON COLUMN erp_ast_movement.from_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_ast_movement.thru_date IS '截止日期';
                    
      COMMENT ON COLUMN erp_ast_movement.from_department_id IS '原使用部门';
                    
      COMMENT ON COLUMN erp_ast_movement.to_department_id IS '新使用部门';
                    
      COMMENT ON COLUMN erp_ast_movement.from_staff_id IS '原使用人';
                    
      COMMENT ON COLUMN erp_ast_movement.to_staff_id IS '新使用人';
                    
      COMMENT ON COLUMN erp_ast_movement.from_location_id IS '原使用地点';
                    
      COMMENT ON COLUMN erp_ast_movement.to_location_id IS '新使用地点';
                    
      COMMENT ON COLUMN erp_ast_movement.handler_id IS '经办人';
                    
      COMMENT ON COLUMN erp_ast_movement.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_movement.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_movement.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_movement.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_movement.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_movement.doc_version IS '单据版本';
                    
      COMMENT ON COLUMN erp_ast_movement.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_movement.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_movement.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_movement.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_movement.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_movement.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_movement.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ast_movement.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_movement.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_movement.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_movement.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_movement.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_movement.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_value_adjustment IS '资产价值调整';
                
      COMMENT ON COLUMN erp_ast_value_adjustment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.code IS '单号';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.asset_id IS '资产ID';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.business_date IS '调整日期';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.adjustment_type IS '调整类型';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.adjustment_amount IS '调整金额';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.reason IS '调整原因';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_value_adjustment.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_disposal IS '资产处置';
                
      COMMENT ON COLUMN erp_ast_disposal.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_disposal.code IS '单号';
                    
      COMMENT ON COLUMN erp_ast_disposal.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_disposal.asset_id IS '资产ID';
                    
      COMMENT ON COLUMN erp_ast_disposal.disposal_type IS '处置类型';
                    
      COMMENT ON COLUMN erp_ast_disposal.disposal_amount IS '处置金额';
                    
      COMMENT ON COLUMN erp_ast_disposal.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_disposal.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_disposal.business_date IS '处置日期';
                    
      COMMENT ON COLUMN erp_ast_disposal.gain_loss IS '损益';
                    
      COMMENT ON COLUMN erp_ast_disposal.reason IS '处置原因';
                    
      COMMENT ON COLUMN erp_ast_disposal.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_disposal.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_disposal.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_disposal.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_disposal.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_disposal.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_disposal.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_disposal.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_disposal.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_disposal.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_disposal.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_disposal.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_disposal.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_disposal.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ast_disposal.nop_flow_id IS '工作流实例';
                    
      COMMENT ON COLUMN erp_ast_disposal.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_disposal.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_cip IS '在建工程';
                
      COMMENT ON COLUMN erp_ast_cip.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_cip.code IS '工程编码';
                    
      COMMENT ON COLUMN erp_ast_cip.name IS '工程名称';
                    
      COMMENT ON COLUMN erp_ast_cip.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_cip.category_id IS '预定资产类别(完工后转入)';
                    
      COMMENT ON COLUMN erp_ast_cip.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_cip.business_date IS '开工日期';
                    
      COMMENT ON COLUMN erp_ast_cip.estimated_completion_date IS '预计完工日期';
                    
      COMMENT ON COLUMN erp_ast_cip.accumulated_cost IS '累计归集成本';
                    
      COMMENT ON COLUMN erp_ast_cip.is_completed IS '是否已转固';
                    
      COMMENT ON COLUMN erp_ast_cip.completed_asset_id IS '完工转出资产';
                    
      COMMENT ON COLUMN erp_ast_cip.status IS '状态';
                    
      COMMENT ON COLUMN erp_ast_cip.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_cip.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_cip.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_cip.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_cip.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_cip.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_cip.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ast_cip.project_id IS '关联项目(可空)';
                    
      COMMENT ON COLUMN erp_ast_cip.cip_asset_category_snapshot IS '转固时类别快照';
                    
      COMMENT ON COLUMN erp_ast_cip.posted IS '是否已过账';
                    
      COMMENT ON COLUMN erp_ast_cip.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_cip.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_cip.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_cip.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_cip.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_split IS '资产拆分单';
                
      COMMENT ON COLUMN erp_ast_split.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_split.code IS '单号';
                    
      COMMENT ON COLUMN erp_ast_split.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_split.source_asset_id IS '被拆分资产';
                    
      COMMENT ON COLUMN erp_ast_split.business_date IS '拆分日期';
                    
      COMMENT ON COLUMN erp_ast_split.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_split.split_reason IS '拆分原因';
                    
      COMMENT ON COLUMN erp_ast_split.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_split.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_split.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_split.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_split.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_split.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_split.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_split.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_split.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_split.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_split.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_split.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_split.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_split.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ast_split.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_split.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_split.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_merge IS '资产合并单';
                
      COMMENT ON COLUMN erp_ast_merge.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_merge.code IS '单号';
                    
      COMMENT ON COLUMN erp_ast_merge.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_merge.target_asset_id IS '合并目标资产';
                    
      COMMENT ON COLUMN erp_ast_merge.business_date IS '合并日期';
                    
      COMMENT ON COLUMN erp_ast_merge.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_merge.merge_reason IS '合并原因';
                    
      COMMENT ON COLUMN erp_ast_merge.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_ast_merge.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_ast_merge.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_merge.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_merge.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_merge.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_merge.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_merge.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_merge.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_merge.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_merge.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_merge.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_merge.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_merge.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ast_merge.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_merge.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_merge.amount_functional IS '本位币金额';
                    
      COMMENT ON TABLE erp_ast_maintenance IS '资产维修工单';
                
      COMMENT ON COLUMN erp_ast_maintenance.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_maintenance.code IS '维修单号';
                    
      COMMENT ON COLUMN erp_ast_maintenance.name IS '维修名称';
                    
      COMMENT ON COLUMN erp_ast_maintenance.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ast_maintenance.asset_id IS '资产卡片';
                    
      COMMENT ON COLUMN erp_ast_maintenance.maintenance_visit_id IS '维护工单(弱关联)';
                    
      COMMENT ON COLUMN erp_ast_maintenance.status IS '维修状态';
                    
      COMMENT ON COLUMN erp_ast_maintenance.treatment IS '处置裁决';
                    
      COMMENT ON COLUMN erp_ast_maintenance.capitalized_amount IS '资本化金额';
                    
      COMMENT ON COLUMN erp_ast_maintenance.total_cost_amount IS '费用合计';
                    
      COMMENT ON COLUMN erp_ast_maintenance.business_date IS '维修日期';
                    
      COMMENT ON COLUMN erp_ast_maintenance.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_maintenance.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_maintenance.reason IS '维修原因';
                    
      COMMENT ON COLUMN erp_ast_maintenance.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_ast_maintenance.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ast_maintenance.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_ast_maintenance.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance.reversed IS '已红冲';
                    
      COMMENT ON COLUMN erp_ast_maintenance.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_maintenance.amount_functional IS '本位币金额';
                    
      COMMENT ON COLUMN erp_ast_maintenance.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_maintenance.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_maintenance.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_maintenance.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_maintenance.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance.remark IS '备注';
                    
      COMMENT ON TABLE erp_ast_inventory_line IS '资产盘点行';
                
      COMMENT ON COLUMN erp_ast_inventory_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.inventory_id IS '盘点单';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.asset_id IS '账面资产';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.asset_code_snapshot IS '资产编码快照';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.asset_name_snapshot IS '资产名称快照';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.category_id IS '资产类别';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.book_quantity IS '账面数量';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.actual_quantity IS '实盘数量';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.variance_quantity IS '差异数量';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.variance_type IS '差异类型';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.book_value IS '账面价值';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.assessed_value IS '评估价值';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.variance_amount IS '差异金额';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.disposition IS '差异处置';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.new_asset_id IS '盘盈新建资产';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.capitalization_id IS '盘盈资本化单';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.disposal_id IS '盘亏处置单';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.investigated_remark IS '调查备注';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_inventory_line.remark IS '备注';
                    
      COMMENT ON TABLE erp_ast_cip_cost_item IS 'CIP成本归集行';
                
      COMMENT ON COLUMN erp_ast_cip_cost_item.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.cip_id IS '在建工程';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.cost_type IS '成本类型';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.amount_functional IS '本位币金额';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.source_bill_type IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.source_bill_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.posted_transfer_flag IS '已转固标记';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.capitalization_id IS '转固资本化单';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_cip_cost_item.remark IS '备注';
                    
      COMMENT ON TABLE erp_ast_cip_progress_billing IS 'CIP进度付款';
                
      COMMENT ON COLUMN erp_ast_cip_progress_billing.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.cip_id IS '在建工程';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.billing_date IS '付款日期';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.billing_milestone IS '里程碑';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.amount_functional IS '本位币金额';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.payment_voucher_code IS '付款凭证号';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.paid_flag IS '已付标记';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_cip_progress_billing.remark IS '备注';
                    
      COMMENT ON TABLE erp_ast_split_line IS '资产拆分行';
                
      COMMENT ON COLUMN erp_ast_split_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_split_line.split_id IS '拆分单';
                    
      COMMENT ON COLUMN erp_ast_split_line.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_split_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_ast_split_line.target_asset_code IS '目标资产编码';
                    
      COMMENT ON COLUMN erp_ast_split_line.target_asset_name IS '目标资产名称';
                    
      COMMENT ON COLUMN erp_ast_split_line.category_id IS '目标资产类别';
                    
      COMMENT ON COLUMN erp_ast_split_line.allocation_method IS '分摊方式';
                    
      COMMENT ON COLUMN erp_ast_split_line.proportion IS '比例';
                    
      COMMENT ON COLUMN erp_ast_split_line.original_cost_amount IS '原值金额';
                    
      COMMENT ON COLUMN erp_ast_split_line.accumulated_depreciation_amount IS '累计折旧金额';
                    
      COMMENT ON COLUMN erp_ast_split_line.net_book_value IS '账面净值';
                    
      COMMENT ON COLUMN erp_ast_split_line.target_asset_id IS '目标资产';
                    
      COMMENT ON COLUMN erp_ast_split_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_split_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_split_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_split_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_split_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_split_line.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_split_line.remark IS '备注';
                    
      COMMENT ON TABLE erp_ast_merge_line IS '资产合并行';
                
      COMMENT ON COLUMN erp_ast_merge_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_merge_line.merge_id IS '合并单';
                    
      COMMENT ON COLUMN erp_ast_merge_line.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_merge_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_ast_merge_line.source_asset_id IS '源资产';
                    
      COMMENT ON COLUMN erp_ast_merge_line.contribution_proportion IS '贡献比例';
                    
      COMMENT ON COLUMN erp_ast_merge_line.original_cost_amount IS '原值金额';
                    
      COMMENT ON COLUMN erp_ast_merge_line.accumulated_depreciation_amount IS '累计折旧金额';
                    
      COMMENT ON COLUMN erp_ast_merge_line.net_book_value IS '账面净值';
                    
      COMMENT ON COLUMN erp_ast_merge_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_merge_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_merge_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_merge_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_merge_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_merge_line.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_merge_line.remark IS '备注';
                    
      COMMENT ON TABLE erp_ast_maintenance_cost IS '维修费用行';
                
      COMMENT ON COLUMN erp_ast_maintenance_cost.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.maintenance_id IS '维修工单';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.cost_type IS '费用类型';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.amount IS '金额';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_ast_maintenance_cost.remark IS '备注';
                    
