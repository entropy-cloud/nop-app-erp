
CREATE TABLE erp_md_warehouse(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  warehouse_type INT4  ,
  org_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_warehouse primary key (id)
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

CREATE TABLE erp_md_location(
  id INT8  ,
  warehouse_id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  parent_id INT8  ,
  constraint PK_erp_md_location primary key (id)
);

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

CREATE TABLE erp_md_currency(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  symbol VARCHAR(50)  ,
  decimal_places INT4  ,
  is_functional BOOLEAN  ,
  constraint PK_erp_md_currency primary key (id)
);

CREATE TABLE erp_md_acct_schema(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  currency_id INT8  ,
  nature INT4  ,
  constraint PK_erp_md_acct_schema primary key (id)
);

CREATE TABLE erp_md_partner(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  partner_type INT4  ,
  constraint PK_erp_md_partner primary key (id)
);

CREATE TABLE erp_md_employee(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_employee primary key (id)
);

CREATE TABLE erp_inv_transfer_order(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  from_warehouse_id INT8 NOT NULL ,
  to_warehouse_id INT8 NOT NULL ,
  in_transit_warehouse_id INT8  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_transfer_order primary key (id)
);

CREATE TABLE erp_inv_stock_take(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  take_type VARCHAR(20)  ,
  warehouse_id INT8 NOT NULL ,
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
  constraint PK_erp_inv_stock_take primary key (id)
);

CREATE TABLE erp_inv_stock_move(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  move_type VARCHAR(20) NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  source_warehouse_id INT8  ,
  source_location_id INT8  ,
  dest_warehouse_id INT8  ,
  dest_location_id INT8  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  origin_move_id INT8  ,
  origin_returned_move_id INT8  ,
  constraint PK_erp_inv_stock_move primary key (id)
);

CREATE TABLE erp_inv_batch(
  id INT8 NOT NULL ,
  org_id INT8  ,
  batch_no VARCHAR(50) NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  warehouse_id INT8 NOT NULL ,
  total_quantity NUMERIC(20,4) NOT NULL ,
  available_quantity NUMERIC(20,4) NOT NULL ,
  production_date DATE  ,
  expiry_date DATE  ,
  shelf_life_days INT4  ,
  status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_batch primary key (id)
);

CREATE TABLE erp_inv_serial_number(
  id INT8 NOT NULL ,
  org_id INT8  ,
  serial_no VARCHAR(50) NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  warehouse_id INT8  ,
  location_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  in_bill_type VARCHAR(50)  ,
  in_bill_code VARCHAR(50)  ,
  out_bill_type VARCHAR(50)  ,
  out_bill_code VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_serial_number primary key (id)
);

CREATE TABLE erp_inv_cost_adjust(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  adjust_type VARCHAR(30) NOT NULL ,
  reason VARCHAR(500)  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  currency_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_cost_adjust primary key (id)
);

CREATE TABLE erp_inv_stock_balance(
  id INT8 NOT NULL ,
  org_id INT8  ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  warehouse_id INT8 NOT NULL ,
  location_id INT8  ,
  batch_no VARCHAR(50)  ,
  total_quantity NUMERIC(20,4) NOT NULL ,
  reserved_quantity NUMERIC(20,4) default 0   ,
  locked_quantity NUMERIC(20,4) default 0   ,
  available_quantity NUMERIC(20,4) NOT NULL ,
  cost_method VARCHAR(20)  ,
  avg_cost NUMERIC(20,4)  ,
  total_cost NUMERIC(20,4)  ,
  currency_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  owner_id INT8  ,
  ownership_type VARCHAR(20) default 'OWNED'   ,
  constraint PK_erp_inv_stock_balance primary key (id)
);

CREATE TABLE erp_inv_reservation(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  source_bill_type VARCHAR(50) NOT NULL ,
  source_bill_code VARCHAR(50) NOT NULL ,
  reserved_for_partner_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  valid_until TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_reservation primary key (id)
);

CREATE TABLE erp_inv_ownership_transfer(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  transfer_type VARCHAR(30) NOT NULL ,
  partner_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  warehouse_id INT8 NOT NULL ,
  source_loc_id INT8 NOT NULL ,
  dest_loc_id INT8 NOT NULL ,
  from_ownership_type VARCHAR(20) NOT NULL ,
  to_ownership_type VARCHAR(20) NOT NULL ,
  currency_id INT8  ,
  doc_status VARCHAR(20) NOT NULL ,
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
  constraint PK_erp_inv_ownership_transfer primary key (id)
);

CREATE TABLE erp_inv_landed_cost(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  receive_id INT8 NOT NULL ,
  supplier_id INT8  ,
  currency_id INT8  ,
  exchange_rate NUMERIC(20,6)  ,
  total_cost_amount NUMERIC(20,4)  ,
  allocation_method VARCHAR(20) NOT NULL ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  business_date DATE NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_landed_cost primary key (id)
);

CREATE TABLE erp_inv_picking_order(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  business_date DATE NOT NULL ,
  warehouse_id INT8 NOT NULL ,
  picker_id INT8  ,
  doc_status VARCHAR(20) NOT NULL ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_picking_order primary key (id)
);

CREATE TABLE erp_inv_transfer_order_line(
  id INT8 NOT NULL ,
  transfer_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  batch_no VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_transfer_order_line primary key (id)
);

CREATE TABLE erp_inv_stock_take_line(
  id INT8 NOT NULL ,
  take_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  location_id INT8  ,
  batch_no VARCHAR(50)  ,
  book_quantity NUMERIC(20,4) NOT NULL ,
  actual_quantity NUMERIC(20,4) NOT NULL ,
  difference_quantity NUMERIC(20,4) NOT NULL ,
  unit_cost NUMERIC(20,4)  ,
  difference_amount NUMERIC(20,4)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_stock_take_line primary key (id)
);

CREATE TABLE erp_inv_stock_move_line(
  id INT8 NOT NULL ,
  move_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_cost NUMERIC(20,4)  ,
  total_cost NUMERIC(20,4)  ,
  currency_id INT8  ,
  batch_no VARCHAR(50)  ,
  serial_no VARCHAR(50)  ,
  source_location_id INT8  ,
  dest_location_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_stock_move_line primary key (id)
);

CREATE TABLE erp_inv_cost_layer(
  id INT8 NOT NULL ,
  org_id INT8  ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  warehouse_id INT8 NOT NULL ,
  batch_no VARCHAR(50)  ,
  cost_method VARCHAR(20) NOT NULL ,
  incoming_quantity NUMERIC(20,4) NOT NULL ,
  remaining_quantity NUMERIC(20,4) NOT NULL ,
  unit_cost NUMERIC(20,4) NOT NULL ,
  total_cost NUMERIC(20,4) NOT NULL ,
  currency_id INT8  ,
  incoming_date DATE  ,
  incoming_move_id INT8  ,
  acct_schema_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_cost_layer primary key (id)
);

CREATE TABLE erp_inv_cost_adjust_line(
  id INT8 NOT NULL ,
  adjust_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  warehouse_id INT8 NOT NULL ,
  batch_no VARCHAR(50)  ,
  old_unit_cost NUMERIC(20,4)  ,
  new_unit_cost NUMERIC(20,4) NOT NULL ,
  adjust_qty NUMERIC(20,4)  ,
  adjust_amount NUMERIC(20,4)  ,
  adjust_reason VARCHAR(200)  ,
  currency_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_cost_adjust_line primary key (id)
);

CREATE TABLE erp_inv_reservation_line(
  id INT8 NOT NULL ,
  reservation_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  warehouse_id INT8 NOT NULL ,
  location_id INT8  ,
  batch_no VARCHAR(50)  ,
  reserved_quantity NUMERIC(20,4) NOT NULL ,
  consumed_quantity NUMERIC(20,4) default 0   ,
  uom_id INT8 NOT NULL ,
  source_line_code VARCHAR(50)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_reservation_line primary key (id)
);

CREATE TABLE erp_inv_ownership_transfer_line(
  id INT8 NOT NULL ,
  transfer_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  batch_no VARCHAR(50)  ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_cost NUMERIC(20,4)  ,
  total_cost NUMERIC(20,4)  ,
  source_bill_type VARCHAR(50)  ,
  source_bill_code VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_ownership_transfer_line primary key (id)
);

CREATE TABLE erp_inv_landed_cost_line(
  id INT8 NOT NULL ,
  landed_cost_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  cost_element VARCHAR(30) NOT NULL ,
  amount NUMERIC(20,4) NOT NULL ,
  ap_partner_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_landed_cost_line primary key (id)
);

CREATE TABLE erp_inv_picking_order_line(
  id INT8 NOT NULL ,
  picking_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  source_location_id INT8  ,
  quantity NUMERIC(20,4) NOT NULL ,
  picked_quantity NUMERIC(20,4) default 0   ,
  batch_no VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_inv_picking_order_line primary key (id)
);

CREATE TABLE erp_inv_stock_ledger(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  move_id INT8 NOT NULL ,
  move_line_id INT8 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  warehouse_id INT8 NOT NULL ,
  location_id INT8  ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_cost NUMERIC(20,4)  ,
  total_cost NUMERIC(20,4)  ,
  balance_quantity NUMERIC(20,4) NOT NULL ,
  balance_total_cost NUMERIC(20,4) NOT NULL ,
  cost_method VARCHAR(20)  ,
  acct_schema_id INT8  ,
  currency_id INT8  ,
  business_date DATE  ,
  batch_no VARCHAR(50)  ,
  serial_no VARCHAR(50)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  owner_id INT8  ,
  ownership_type VARCHAR(20) default 'OWNED'   ,
  constraint PK_erp_inv_stock_ledger primary key (id)
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
                
      COMMENT ON COLUMN erp_inv_transfer_order.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.code IS '单号';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.from_warehouse_id IS '调出仓库';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.to_warehouse_id IS '调入仓库';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.in_transit_warehouse_id IS '在途仓库';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_take IS '盘点单';
                
      COMMENT ON COLUMN erp_inv_stock_take.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_take.code IS '单号';
                    
      COMMENT ON COLUMN erp_inv_stock_take.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_stock_take.business_date IS '盘点日期';
                    
      COMMENT ON COLUMN erp_inv_stock_take.take_type IS '盘点类型';
                    
      COMMENT ON COLUMN erp_inv_stock_take.warehouse_id IS '盘点仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_take.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_stock_take.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_stock_take.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_stock_take.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_stock_take.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_stock_take.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_stock_take.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_take.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_take.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_take.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_take.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_take.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_move IS '库存移动单';
                
      COMMENT ON COLUMN erp_inv_stock_move.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_move.code IS '单号';
                    
      COMMENT ON COLUMN erp_inv_stock_move.move_type IS '作业类型';
                    
      COMMENT ON COLUMN erp_inv_stock_move.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_stock_move.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_stock_move.source_warehouse_id IS '源仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_move.source_location_id IS '源库位';
                    
      COMMENT ON COLUMN erp_inv_stock_move.dest_warehouse_id IS '目标仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_move.dest_location_id IS '目标库位';
                    
      COMMENT ON COLUMN erp_inv_stock_move.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_stock_move.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_stock_move.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_stock_move.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_stock_move.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_stock_move.related_bill_type IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_inv_stock_move.related_bill_code IS '关联单据号';
                    
      COMMENT ON COLUMN erp_inv_stock_move.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_stock_move.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_move.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_move.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_move.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_move.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_move.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_inv_stock_move.origin_move_id IS '上游移动单';
                    
      COMMENT ON COLUMN erp_inv_stock_move.origin_returned_move_id IS '原退货移动单';
                    
      COMMENT ON TABLE erp_inv_batch IS '批次台账';
                
      COMMENT ON COLUMN erp_inv_batch.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_batch.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_batch.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_batch.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_batch.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_batch.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_batch.total_quantity IS '总数量';
                    
      COMMENT ON COLUMN erp_inv_batch.available_quantity IS '可用数量';
                    
      COMMENT ON COLUMN erp_inv_batch.production_date IS '生产日期';
                    
      COMMENT ON COLUMN erp_inv_batch.expiry_date IS '有效期至';
                    
      COMMENT ON COLUMN erp_inv_batch.shelf_life_days IS '保质期(天)';
                    
      COMMENT ON COLUMN erp_inv_batch.status IS '状态';
                    
      COMMENT ON COLUMN erp_inv_batch.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_batch.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_batch.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_batch.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_batch.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_batch.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_batch.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_serial_number IS '序列号台账';
                
      COMMENT ON COLUMN erp_inv_serial_number.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_serial_number.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_serial_number.serial_no IS '序列号';
                    
      COMMENT ON COLUMN erp_inv_serial_number.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_serial_number.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_serial_number.warehouse_id IS '当前仓库';
                    
      COMMENT ON COLUMN erp_inv_serial_number.location_id IS '当前库位';
                    
      COMMENT ON COLUMN erp_inv_serial_number.status IS '状态';
                    
      COMMENT ON COLUMN erp_inv_serial_number.in_bill_type IS '入库单类型';
                    
      COMMENT ON COLUMN erp_inv_serial_number.in_bill_code IS '入库单号';
                    
      COMMENT ON COLUMN erp_inv_serial_number.out_bill_type IS '出库单类型';
                    
      COMMENT ON COLUMN erp_inv_serial_number.out_bill_code IS '出库单号';
                    
      COMMENT ON COLUMN erp_inv_serial_number.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_serial_number.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_serial_number.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_serial_number.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_serial_number.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_serial_number.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_serial_number.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_cost_adjust IS '成本调整单';
                
      COMMENT ON COLUMN erp_inv_cost_adjust.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.code IS '单号';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.adjust_type IS '调整类型';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.reason IS '调整原因';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_balance IS '库存余额';
                
      COMMENT ON COLUMN erp_inv_stock_balance.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.location_id IS '库位';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.total_quantity IS '总数量';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.reserved_quantity IS '预留数量';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.locked_quantity IS '冻结数量';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.available_quantity IS '可用数量';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.cost_method IS '计价方法';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.avg_cost IS '平均成本';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.total_cost IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.owner_id IS '所有权往来单位';
                    
      COMMENT ON COLUMN erp_inv_stock_balance.ownership_type IS '所有权类型';
                    
      COMMENT ON TABLE erp_inv_reservation IS '库存预留单';
                
      COMMENT ON COLUMN erp_inv_reservation.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_reservation.code IS '单号';
                    
      COMMENT ON COLUMN erp_inv_reservation.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_reservation.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_reservation.source_bill_type IS '来源单据类型(如 SALES_ORDER/WORK_ORDER)';
                    
      COMMENT ON COLUMN erp_inv_reservation.source_bill_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_inv_reservation.reserved_for_partner_id IS '为谁预留(往来单位)';
                    
      COMMENT ON COLUMN erp_inv_reservation.status IS '状态';
                    
      COMMENT ON COLUMN erp_inv_reservation.valid_until IS '预留有效期至';
                    
      COMMENT ON COLUMN erp_inv_reservation.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_reservation.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_reservation.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_reservation.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_reservation.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_reservation.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_reservation.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_ownership_transfer IS '所有权转移单';
                
      COMMENT ON COLUMN erp_inv_ownership_transfer.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.code IS '单号';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.transfer_type IS '转移类型';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.partner_id IS '所有权对方(往来单位)';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.source_loc_id IS '源库位(=目的库位)';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.dest_loc_id IS '目的库位(=源库位)';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.from_ownership_type IS '转前所有权类型';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.to_ownership_type IS '转后所有权类型';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_landed_cost IS '到岸成本单';
                
      COMMENT ON COLUMN erp_inv_landed_cost.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.code IS '单号';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.receive_id IS '采购入库单';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.supplier_id IS '采购供应商';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.total_cost_amount IS '到岸成本合计';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.allocation_method IS '分摊方法';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_picking_order IS '拣货单';
                
      COMMENT ON COLUMN erp_inv_picking_order.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_picking_order.code IS '单号';
                    
      COMMENT ON COLUMN erp_inv_picking_order.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_picking_order.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_picking_order.warehouse_id IS '拣货仓库';
                    
      COMMENT ON COLUMN erp_inv_picking_order.picker_id IS '拣货人(职员)';
                    
      COMMENT ON COLUMN erp_inv_picking_order.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_inv_picking_order.related_bill_type IS '关联单据类型(SALES_DELIVERY/WORK_ORDER_ISSUE)';
                    
      COMMENT ON COLUMN erp_inv_picking_order.related_bill_code IS '关联单据号';
                    
      COMMENT ON COLUMN erp_inv_picking_order.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_picking_order.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_picking_order.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_picking_order.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_picking_order.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_picking_order.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_picking_order.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_transfer_order_line IS '调拨单行';
                
      COMMENT ON COLUMN erp_inv_transfer_order_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.transfer_id IS '调拨单ID';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_transfer_order_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_take_line IS '盘点单行';
                
      COMMENT ON COLUMN erp_inv_stock_take_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.take_id IS '盘点单ID';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.location_id IS '库位';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.book_quantity IS '账面数量';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.actual_quantity IS '实盘数量';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.difference_quantity IS '差异数量';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.unit_cost IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.difference_amount IS '差异金额';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_take_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_move_line IS '库存移动单行';
                
      COMMENT ON COLUMN erp_inv_stock_move_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.move_id IS '移动单ID';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.unit_cost IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.total_cost IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.serial_no IS '序列号';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.source_location_id IS '源库位';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.dest_location_id IS '目标库位';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_move_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_cost_layer IS '成本层';
                
      COMMENT ON COLUMN erp_inv_cost_layer.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.cost_method IS '计价方法';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.incoming_quantity IS '入库数量';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.remaining_quantity IS '剩余数量';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.unit_cost IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.total_cost IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.incoming_date IS '入库日期';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.incoming_move_id IS '入库移动单';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_cost_layer.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_cost_adjust_line IS '成本调整单行';
                
      COMMENT ON COLUMN erp_inv_cost_adjust_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.adjust_id IS '成本调整单ID';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.old_unit_cost IS '原单位成本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.new_unit_cost IS '新单位成本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.adjust_qty IS '调整数量';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.adjust_amount IS '调整金额';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.adjust_reason IS '行调整原因';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_cost_adjust_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_reservation_line IS '库存预留单行';
                
      COMMENT ON COLUMN erp_inv_reservation_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.reservation_id IS '预留单ID';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.location_id IS '库位';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.reserved_quantity IS '预留数量';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.consumed_quantity IS '已消耗数量';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.uom_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.source_line_code IS '来源行号';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_reservation_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_ownership_transfer_line IS '所有权转移单行';
                
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.transfer_id IS '转移单ID';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.unit_cost IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.total_cost IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.source_bill_type IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.source_bill_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_ownership_transfer_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_landed_cost_line IS '到岸成本行';
                
      COMMENT ON COLUMN erp_inv_landed_cost_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.landed_cost_id IS '到岸成本单ID';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.cost_element IS '费用要素';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.amount IS '金额';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.ap_partner_id IS '应付对象';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_landed_cost_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_picking_order_line IS '拣货单行';
                
      COMMENT ON COLUMN erp_inv_picking_order_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.picking_id IS '拣货单ID';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.source_location_id IS '拣货库位';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.quantity IS '应拣数量';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.picked_quantity IS '已拣数量';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_picking_order_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_inv_stock_ledger IS '库存流水';
                
      COMMENT ON COLUMN erp_inv_stock_ledger.id IS 'ID';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.code IS '流水号';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.move_id IS '移动单ID';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.move_line_id IS '移动单行ID';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.location_id IS '库位';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.unit_cost IS '单位成本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.total_cost IS '总成本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.balance_quantity IS '结存数量';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.balance_total_cost IS '结存总成本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.cost_method IS '计价方法';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.serial_no IS '序列号';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.owner_id IS '所有权往来单位';
                    
      COMMENT ON COLUMN erp_inv_stock_ledger.ownership_type IS '所有权类型';
                    
