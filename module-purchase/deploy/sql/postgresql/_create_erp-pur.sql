
CREATE TABLE erp_pur_requisition(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  requester_id INT8 NOT NULL ,
  department_id INT8  ,
  business_date DATE NOT NULL ,
  required_date DATE  ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_requisition primary key (id)
);

CREATE TABLE erp_pur_supplier_price_list(
  id INT8 NOT NULL ,
  supplier_id INT8 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  currency_id INT8 NOT NULL ,
  unit_price NUMERIC(20,4) NOT NULL ,
  tax_rate NUMERIC(10,4)  ,
  min_order_quantity NUMERIC(20,4)  ,
  lead_time_days INT4  ,
  valid_from DATE  ,
  valid_to DATE  ,
  priority INT4 default 100   ,
  is_active BOOLEAN default true   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_supplier_price_list primary key (id)
);

CREATE TABLE erp_pur_order(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  requisition_id INT8  ,
  quotation_id INT8  ,
  supplier_id INT8 NOT NULL ,
  warehouse_id INT8  ,
  business_date DATE NOT NULL ,
  delivery_date DATE  ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  total_amount NUMERIC(20,4) default 0   ,
  total_tax_amount NUMERIC(20,4) default 0   ,
  total_amount_with_tax NUMERIC(20,4) default 0   ,
  discount_rate NUMERIC(10,4)  ,
  discount_amount NUMERIC(20,4) default 0   ,
  paid_amount NUMERIC(20,4) default 0   ,
  settlement_method_id INT8  ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  paid_status INT4  ,
  receive_status INT4  ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_order primary key (id)
);

CREATE TABLE erp_pur_invoice(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  supplier_id INT8 NOT NULL ,
  invoice_no VARCHAR(50)  ,
  business_date DATE NOT NULL ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  total_amount NUMERIC(20,4) default 0   ,
  total_tax_amount NUMERIC(20,4) default 0   ,
  total_amount_with_tax NUMERIC(20,4) default 0   ,
  paid_amount NUMERIC(20,4) default 0   ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  paid_status INT4  ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_invoice primary key (id)
);

CREATE TABLE erp_pur_payment(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  supplier_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  amount_source NUMERIC(20,4) NOT NULL ,
  amount_functional NUMERIC(20,4) NOT NULL ,
  total_amount NUMERIC(20,4) NOT NULL ,
  settlement_method_id INT8  ,
  payment_method VARCHAR(50)  ,
  bank_account_id INT8  ,
  partner_bank_account_id INT8  ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  written_off_status INT4  ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_payment primary key (id)
);

CREATE TABLE erp_pur_requisition_line(
  id INT8 NOT NULL ,
  requisition_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  required_date DATE  ,
  suggested_supplier_id INT8  ,
  project_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_requisition_line primary key (id)
);

CREATE TABLE erp_pur_rfq(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  requisition_id INT8  ,
  business_date DATE NOT NULL ,
  valid_until DATE  ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_rfq primary key (id)
);

CREATE TABLE erp_pur_order_line(
  id INT8 NOT NULL ,
  order_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_price NUMERIC(20,4) NOT NULL ,
  tax_rate NUMERIC(10,4)  ,
  tax_rate_id INT8  ,
  tax_amount NUMERIC(20,4) default 0   ,
  amount NUMERIC(20,4) NOT NULL ,
  amount_with_tax NUMERIC(20,4)  ,
  received_quantity NUMERIC(20,4) default 0   ,
  invoiced_quantity NUMERIC(20,4) default 0   ,
  warehouse_id INT8  ,
  project_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_order_line primary key (id)
);

CREATE TABLE erp_pur_receive(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  order_id INT8  ,
  supplier_id INT8 NOT NULL ,
  warehouse_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  total_amount NUMERIC(20,4) default 0   ,
  total_tax_amount NUMERIC(20,4) default 0   ,
  total_amount_with_tax NUMERIC(20,4) default 0   ,
  receive_status INT4  ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_receive primary key (id)
);

CREATE TABLE erp_pur_invoice_line(
  id INT8 NOT NULL ,
  invoice_id INT8 NOT NULL ,
  receive_line_id INT8  ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_price NUMERIC(20,4)  ,
  tax_rate NUMERIC(10,4)  ,
  tax_amount NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_invoice_line primary key (id)
);

CREATE TABLE erp_pur_payment_line(
  id INT8 NOT NULL ,
  payment_id INT8 NOT NULL ,
  invoice_id INT8 NOT NULL ,
  amount NUMERIC(20,4) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_payment_line primary key (id)
);

CREATE TABLE erp_pur_rfq_line(
  id INT8 NOT NULL ,
  rfq_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_rfq_line primary key (id)
);

CREATE TABLE erp_pur_quotation(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  rfq_id INT8  ,
  supplier_id INT8 NOT NULL ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  business_date DATE NOT NULL ,
  valid_from DATE  ,
  valid_to DATE  ,
  is_accepted BOOLEAN default false   ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_quotation primary key (id)
);

CREATE TABLE erp_pur_receive_line(
  id INT8 NOT NULL ,
  receive_id INT8 NOT NULL ,
  order_line_id INT8  ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  rejected_quantity NUMERIC(20,4) default 0   ,
  unit_price NUMERIC(20,4)  ,
  tax_rate NUMERIC(10,4)  ,
  tax_amount NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  warehouse_id INT8  ,
  batch_no VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_receive_line primary key (id)
);

CREATE TABLE erp_pur_return(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  receive_id INT8  ,
  supplier_id INT8 NOT NULL ,
  warehouse_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  total_amount NUMERIC(20,4) default 0   ,
  total_tax_amount NUMERIC(20,4) default 0   ,
  total_amount_with_tax NUMERIC(20,4) default 0   ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_return primary key (id)
);

CREATE TABLE erp_pur_quotation_line(
  id INT8 NOT NULL ,
  quotation_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4)  ,
  unit_price NUMERIC(20,4) NOT NULL ,
  tax_rate NUMERIC(10,4)  ,
  lead_time_days INT4  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_quotation_line primary key (id)
);

CREATE TABLE erp_pur_return_line(
  id INT8 NOT NULL ,
  return_id INT8 NOT NULL ,
  receive_line_id INT8  ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_price NUMERIC(20,4)  ,
  tax_rate NUMERIC(10,4)  ,
  tax_amount NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  reason VARCHAR(500)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_pur_return_line primary key (id)
);


      COMMENT ON TABLE erp_pur_requisition IS '采购请购单';
                
      COMMENT ON COLUMN erp_pur_requisition.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_requisition.code IS '单号';
                    
      COMMENT ON COLUMN erp_pur_requisition.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_requisition.requester_id IS '请购人(职员)';
                    
      COMMENT ON COLUMN erp_pur_requisition.department_id IS '请购部门';
                    
      COMMENT ON COLUMN erp_pur_requisition.business_date IS '请购日期';
                    
      COMMENT ON COLUMN erp_pur_requisition.required_date IS '需求日期';
                    
      COMMENT ON COLUMN erp_pur_requisition.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_requisition.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_requisition.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_requisition.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_requisition.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_requisition.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_requisition.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_requisition.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_requisition.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_requisition.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_requisition.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_supplier_price_list IS '供应商价格清单';
                
      COMMENT ON COLUMN erp_pur_supplier_price_list.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.unit_price IS '协议单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.min_order_quantity IS '最小起订量';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.lead_time_days IS '交货周期(天)';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.valid_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.valid_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.priority IS '优先级(数字小优先)';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_supplier_price_list.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_order IS '采购订单';
                
      COMMENT ON COLUMN erp_pur_order.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_order.code IS '单号';
                    
      COMMENT ON COLUMN erp_pur_order.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_order.requisition_id IS '请购单';
                    
      COMMENT ON COLUMN erp_pur_order.quotation_id IS '报价单';
                    
      COMMENT ON COLUMN erp_pur_order.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_order.warehouse_id IS '收货仓库';
                    
      COMMENT ON COLUMN erp_pur_order.business_date IS '订单日期';
                    
      COMMENT ON COLUMN erp_pur_order.delivery_date IS '交货日期';
                    
      COMMENT ON COLUMN erp_pur_order.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_pur_order.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_order.amount_source IS '合计金额(源币不含税)';
                    
      COMMENT ON COLUMN erp_pur_order.amount_functional IS '合计金额(本位币不含税)';
                    
      COMMENT ON COLUMN erp_pur_order.total_amount IS '合计金额(本位币不含税)';
                    
      COMMENT ON COLUMN erp_pur_order.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_pur_order.total_amount_with_tax IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_order.discount_rate IS '整单折扣率(%)';
                    
      COMMENT ON COLUMN erp_pur_order.discount_amount IS '折扣金额';
                    
      COMMENT ON COLUMN erp_pur_order.paid_amount IS '已付金额';
                    
      COMMENT ON COLUMN erp_pur_order.settlement_method_id IS '结算方式';
                    
      COMMENT ON COLUMN erp_pur_order.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_order.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_order.paid_status IS '付款进度';
                    
      COMMENT ON COLUMN erp_pur_order.receive_status IS '收货状态';
                    
      COMMENT ON COLUMN erp_pur_order.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_order.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_order.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_order.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_order.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_order.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_order.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_order.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_order.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_order.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_order.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_order.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_invoice IS '采购发票';
                
      COMMENT ON COLUMN erp_pur_invoice.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_invoice.code IS '单号';
                    
      COMMENT ON COLUMN erp_pur_invoice.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_invoice.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_invoice.invoice_no IS '发票号码';
                    
      COMMENT ON COLUMN erp_pur_invoice.business_date IS '发票日期';
                    
      COMMENT ON COLUMN erp_pur_invoice.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_pur_invoice.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_invoice.amount_source IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_pur_invoice.amount_functional IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_pur_invoice.total_amount IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_invoice.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_pur_invoice.total_amount_with_tax IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_invoice.paid_amount IS '已付金额';
                    
      COMMENT ON COLUMN erp_pur_invoice.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_invoice.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_invoice.paid_status IS '付款进度';
                    
      COMMENT ON COLUMN erp_pur_invoice.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_invoice.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_invoice.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_invoice.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_invoice.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_invoice.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_invoice.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_invoice.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_invoice.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_invoice.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_invoice.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_invoice.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_payment IS '付款单';
                
      COMMENT ON COLUMN erp_pur_payment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_payment.code IS '单号';
                    
      COMMENT ON COLUMN erp_pur_payment.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_payment.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_payment.business_date IS '付款日期';
                    
      COMMENT ON COLUMN erp_pur_payment.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_pur_payment.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_payment.amount_source IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_pur_payment.amount_functional IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_pur_payment.total_amount IS '合计金额';
                    
      COMMENT ON COLUMN erp_pur_payment.settlement_method_id IS '结算方式';
                    
      COMMENT ON COLUMN erp_pur_payment.payment_method IS '付款方式';
                    
      COMMENT ON COLUMN erp_pur_payment.bank_account_id IS '付款账户(资金账户)';
                    
      COMMENT ON COLUMN erp_pur_payment.partner_bank_account_id IS '供应商收款账户(银行账户主数据)';
                    
      COMMENT ON COLUMN erp_pur_payment.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_payment.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_payment.written_off_status IS '核销状态';
                    
      COMMENT ON COLUMN erp_pur_payment.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_payment.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_payment.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_payment.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_payment.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_payment.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_payment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_payment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_payment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_payment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_payment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_payment.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_requisition_line IS '采购请购单行';
                
      COMMENT ON COLUMN erp_pur_requisition_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.requisition_id IS '请购单ID';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.quantity IS '请购数量';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.required_date IS '需求日期';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.suggested_supplier_id IS '建议供应商';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_requisition_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_rfq IS '采购询价单';
                
      COMMENT ON COLUMN erp_pur_rfq.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_rfq.code IS '单号';
                    
      COMMENT ON COLUMN erp_pur_rfq.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_rfq.requisition_id IS '请购单';
                    
      COMMENT ON COLUMN erp_pur_rfq.business_date IS '询价日期';
                    
      COMMENT ON COLUMN erp_pur_rfq.valid_until IS '报价有效期至';
                    
      COMMENT ON COLUMN erp_pur_rfq.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_rfq.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_rfq.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_rfq.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_rfq.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_rfq.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_rfq.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_rfq.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_rfq.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_order_line IS '采购订单行';
                
      COMMENT ON COLUMN erp_pur_order_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_order_line.order_id IS '订单ID';
                    
      COMMENT ON COLUMN erp_pur_order_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_pur_order_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_pur_order_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_pur_order_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_order_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_pur_order_line.unit_price IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_order_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_order_line.tax_rate_id IS '税率主数据';
                    
      COMMENT ON COLUMN erp_pur_order_line.tax_amount IS '税额';
                    
      COMMENT ON COLUMN erp_pur_order_line.amount IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_order_line.amount_with_tax IS '金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_order_line.received_quantity IS '已收货数量';
                    
      COMMENT ON COLUMN erp_pur_order_line.invoiced_quantity IS '已开票数量';
                    
      COMMENT ON COLUMN erp_pur_order_line.warehouse_id IS '收货仓库';
                    
      COMMENT ON COLUMN erp_pur_order_line.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_pur_order_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_order_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_order_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_order_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_order_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_order_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_order_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_receive IS '采购入库单';
                
      COMMENT ON COLUMN erp_pur_receive.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_receive.code IS '单号';
                    
      COMMENT ON COLUMN erp_pur_receive.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_receive.order_id IS '采购订单';
                    
      COMMENT ON COLUMN erp_pur_receive.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_receive.warehouse_id IS '入库仓库';
                    
      COMMENT ON COLUMN erp_pur_receive.business_date IS '入库日期';
                    
      COMMENT ON COLUMN erp_pur_receive.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_pur_receive.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_receive.amount_source IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_pur_receive.amount_functional IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_pur_receive.total_amount IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_receive.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_pur_receive.total_amount_with_tax IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_receive.receive_status IS '收货状态';
                    
      COMMENT ON COLUMN erp_pur_receive.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_receive.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_receive.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_receive.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_receive.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_receive.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_receive.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_receive.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_receive.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_receive.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_receive.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_receive.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_receive.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_receive.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_invoice_line IS '采购发票行';
                
      COMMENT ON COLUMN erp_pur_invoice_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.invoice_id IS '发票ID';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.receive_line_id IS '入库行ID';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.unit_price IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.tax_amount IS '税额';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.amount IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_invoice_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_payment_line IS '付款核销行';
                
      COMMENT ON COLUMN erp_pur_payment_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_payment_line.payment_id IS '付款单ID';
                    
      COMMENT ON COLUMN erp_pur_payment_line.invoice_id IS '发票ID';
                    
      COMMENT ON COLUMN erp_pur_payment_line.amount IS '核销金额';
                    
      COMMENT ON COLUMN erp_pur_payment_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_payment_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_payment_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_payment_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_payment_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_payment_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_payment_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_rfq_line IS '采购询价单行';
                
      COMMENT ON COLUMN erp_pur_rfq_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.rfq_id IS '询价单ID';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.quantity IS '询价数量';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_rfq_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_quotation IS '供应商报价单';
                
      COMMENT ON COLUMN erp_pur_quotation.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_quotation.code IS '单号';
                    
      COMMENT ON COLUMN erp_pur_quotation.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_quotation.rfq_id IS '询价单';
                    
      COMMENT ON COLUMN erp_pur_quotation.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_quotation.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_pur_quotation.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_quotation.business_date IS '报价日期';
                    
      COMMENT ON COLUMN erp_pur_quotation.valid_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_pur_quotation.valid_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_pur_quotation.is_accepted IS '是否中标';
                    
      COMMENT ON COLUMN erp_pur_quotation.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_quotation.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_quotation.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_quotation.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_quotation.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_quotation.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_quotation.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_quotation.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_quotation.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_receive_line IS '采购入库单行';
                
      COMMENT ON COLUMN erp_pur_receive_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_receive_line.receive_id IS '入库单ID';
                    
      COMMENT ON COLUMN erp_pur_receive_line.order_line_id IS '订单行ID';
                    
      COMMENT ON COLUMN erp_pur_receive_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_pur_receive_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_pur_receive_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_pur_receive_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_receive_line.quantity IS '实收数量';
                    
      COMMENT ON COLUMN erp_pur_receive_line.rejected_quantity IS '拒收数量';
                    
      COMMENT ON COLUMN erp_pur_receive_line.unit_price IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_receive_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_receive_line.tax_amount IS '税额';
                    
      COMMENT ON COLUMN erp_pur_receive_line.amount IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_receive_line.warehouse_id IS '入库库位';
                    
      COMMENT ON COLUMN erp_pur_receive_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_pur_receive_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_receive_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_receive_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_receive_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_receive_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_receive_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_receive_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_return IS '采购退货单';
                
      COMMENT ON COLUMN erp_pur_return.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_return.code IS '单号';
                    
      COMMENT ON COLUMN erp_pur_return.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_pur_return.receive_id IS '入库单';
                    
      COMMENT ON COLUMN erp_pur_return.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_pur_return.warehouse_id IS '出库仓库';
                    
      COMMENT ON COLUMN erp_pur_return.business_date IS '退货日期';
                    
      COMMENT ON COLUMN erp_pur_return.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_pur_return.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_pur_return.amount_source IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_pur_return.amount_functional IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_pur_return.total_amount IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_return.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_pur_return.total_amount_with_tax IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_pur_return.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_pur_return.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_pur_return.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_pur_return.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_pur_return.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_pur_return.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_pur_return.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_pur_return.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_return.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_return.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_return.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_return.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_return.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_return.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_quotation_line IS '供应商报价单行';
                
      COMMENT ON COLUMN erp_pur_quotation_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.quotation_id IS '报价单ID';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.unit_price IS '报价单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.lead_time_days IS '交货周期(天)';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_quotation_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_pur_return_line IS '采购退货单行';
                
      COMMENT ON COLUMN erp_pur_return_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_pur_return_line.return_id IS '退货单ID';
                    
      COMMENT ON COLUMN erp_pur_return_line.receive_line_id IS '入库行ID';
                    
      COMMENT ON COLUMN erp_pur_return_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_pur_return_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_pur_return_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_pur_return_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_pur_return_line.quantity IS '退货数量';
                    
      COMMENT ON COLUMN erp_pur_return_line.unit_price IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_pur_return_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_pur_return_line.tax_amount IS '税额';
                    
      COMMENT ON COLUMN erp_pur_return_line.amount IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_pur_return_line.reason IS '退货原因';
                    
      COMMENT ON COLUMN erp_pur_return_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_pur_return_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_pur_return_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_pur_return_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_pur_return_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_pur_return_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_pur_return_line.update_time IS '修改时间';
                    
