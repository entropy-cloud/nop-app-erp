
CREATE TABLE erp_md_partner(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  partner_type INT4  ,
  status INT4  ,
  credit_limit VARCHAR(50)  ,
  constraint PK_erp_md_partner primary key (id)
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

CREATE TABLE erp_md_material(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  material_type INT4  ,
  status INT4  ,
  constraint PK_erp_md_material primary key (id)
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

CREATE TABLE erp_md_settlement_method(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  settlement_type INT4  ,
  status INT4  ,
  constraint PK_erp_md_settlement_method primary key (id)
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

CREATE TABLE erp_md_material_sku(
  id INT8  ,
  material_id INT8  ,
  sku_code VARCHAR(50)  ,
  barcode VARCHAR(50)  ,
  constraint PK_erp_md_material_sku primary key (id)
);

CREATE TABLE erp_md_tax_rate(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  tax_type INT4  ,
  rate VARCHAR(50)  ,
  constraint PK_erp_md_tax_rate primary key (id)
);

CREATE TABLE erp_md_bank_account(
  id INT8  ,
  partner_id INT8  ,
  bank_name VARCHAR(50)  ,
  bank_account VARCHAR(50)  ,
  account_type INT4  ,
  constraint PK_erp_md_bank_account primary key (id)
);

CREATE TABLE erp_sal_quotation(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  customer_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  valid_from DATE  ,
  valid_to DATE  ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  total_amount NUMERIC(20,4) default 0   ,
  total_tax_amount NUMERIC(20,4) default 0   ,
  total_amount_with_tax NUMERIC(20,4) default 0   ,
  is_accepted BOOLEAN default false   ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_quotation primary key (id)
);

CREATE TABLE erp_sal_contract(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  customer_id INT8 NOT NULL ,
  contract_name VARCHAR(200) NOT NULL ,
  business_date DATE NOT NULL ,
  valid_from DATE  ,
  valid_to DATE  ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  total_amount NUMERIC(20,4) default 0   ,
  total_tax_amount NUMERIC(20,4) default 0   ,
  total_amount_with_tax NUMERIC(20,4) default 0   ,
  signed_by INT8  ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_contract primary key (id)
);

CREATE TABLE erp_sal_invoice(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  customer_id INT8 NOT NULL ,
  invoice_no VARCHAR(50)  ,
  invoice_type INT4  ,
  business_date DATE NOT NULL ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  amount_source NUMERIC(20,4) default 0   ,
  amount_functional NUMERIC(20,4) default 0   ,
  total_amount NUMERIC(20,4) default 0   ,
  total_tax_amount NUMERIC(20,4) default 0   ,
  total_amount_with_tax NUMERIC(20,4) default 0   ,
  received_amount NUMERIC(20,4) default 0   ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  received_status INT4  ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_invoice primary key (id)
);

CREATE TABLE erp_sal_receipt(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  customer_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  amount_source NUMERIC(20,4) NOT NULL ,
  amount_functional NUMERIC(20,4) NOT NULL ,
  total_amount NUMERIC(20,4) NOT NULL ,
  settlement_method_id INT8  ,
  receipt_method VARCHAR(50)  ,
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
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_receipt primary key (id)
);

CREATE TABLE erp_sal_order(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  quotation_id INT8  ,
  contract_id INT8  ,
  customer_id INT8 NOT NULL ,
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
  received_amount NUMERIC(20,4) default 0   ,
  settlement_method_id INT8  ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  received_status INT4  ,
  delivery_status INT4  ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_order primary key (id)
);

CREATE TABLE erp_sal_quotation_line(
  id INT8 NOT NULL ,
  quotation_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uom_id INT8 NOT NULL ,
  quantity NUMERIC(20,4)  ,
  unit_price NUMERIC(20,4) NOT NULL ,
  tax_rate NUMERIC(10,4)  ,
  tax_amount NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  amount_with_tax NUMERIC(20,4)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_quotation_line primary key (id)
);

CREATE TABLE erp_sal_invoice_line(
  id INT8 NOT NULL ,
  invoice_id INT8 NOT NULL ,
  delivery_line_id INT8  ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uom_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_price NUMERIC(20,4)  ,
  tax_rate NUMERIC(10,4)  ,
  tax_amount NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_invoice_line primary key (id)
);

CREATE TABLE erp_sal_receipt_line(
  id INT8 NOT NULL ,
  receipt_id INT8 NOT NULL ,
  invoice_id INT8 NOT NULL ,
  amount NUMERIC(20,4) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_receipt_line primary key (id)
);

CREATE TABLE erp_sal_order_line(
  id INT8 NOT NULL ,
  order_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uom_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_price NUMERIC(20,4) NOT NULL ,
  tax_rate NUMERIC(10,4)  ,
  tax_rate_id INT8  ,
  tax_amount NUMERIC(20,4) default 0   ,
  amount NUMERIC(20,4) NOT NULL ,
  amount_with_tax NUMERIC(20,4)  ,
  delivered_quantity NUMERIC(20,4) default 0   ,
  invoiced_quantity NUMERIC(20,4) default 0   ,
  warehouse_id INT8  ,
  project_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_order_line primary key (id)
);

CREATE TABLE erp_sal_delivery(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  order_id INT8  ,
  customer_id INT8 NOT NULL ,
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
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_delivery primary key (id)
);

CREATE TABLE erp_sal_delivery_line(
  id INT8 NOT NULL ,
  delivery_id INT8 NOT NULL ,
  order_line_id INT8  ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uom_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_price NUMERIC(20,4)  ,
  tax_rate NUMERIC(10,4)  ,
  tax_amount NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  warehouse_id INT8  ,
  batch_no VARCHAR(50)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_delivery_line primary key (id)
);

CREATE TABLE erp_sal_return(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  delivery_id INT8  ,
  customer_id INT8 NOT NULL ,
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
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_return primary key (id)
);

CREATE TABLE erp_sal_return_line(
  id INT8 NOT NULL ,
  return_id INT8 NOT NULL ,
  delivery_line_id INT8  ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_id INT8  ,
  uom_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_price NUMERIC(20,4)  ,
  tax_rate NUMERIC(10,4)  ,
  tax_amount NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  reason VARCHAR(500)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_sal_return_line primary key (id)
);


      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_uom IS '计量单位';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_settlement_method IS '结算方式';
                
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_material_sku IS '物料SKU';
                
      COMMENT ON TABLE erp_md_tax_rate IS '税率';
                
      COMMENT ON TABLE erp_md_bank_account IS '银行账户';
                
      COMMENT ON TABLE erp_sal_quotation IS '销售报价单';
                
      COMMENT ON COLUMN erp_sal_quotation.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_quotation.code IS '单号';
                    
      COMMENT ON COLUMN erp_sal_quotation.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_quotation.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_sal_quotation.business_date IS '报价日期';
                    
      COMMENT ON COLUMN erp_sal_quotation.valid_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_sal_quotation.valid_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_sal_quotation.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_sal_quotation.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_quotation.total_amount IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_quotation.total_amount_with_tax IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation.is_accepted IS '是否已转订单';
                    
      COMMENT ON COLUMN erp_sal_quotation.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_quotation.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_quotation.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_quotation.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_quotation.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_quotation.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_quotation.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_quotation.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_quotation.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_contract IS '销售合同';
                
      COMMENT ON COLUMN erp_sal_contract.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_contract.code IS '合同号';
                    
      COMMENT ON COLUMN erp_sal_contract.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_contract.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_sal_contract.contract_name IS '合同名称';
                    
      COMMENT ON COLUMN erp_sal_contract.business_date IS '签订日期';
                    
      COMMENT ON COLUMN erp_sal_contract.valid_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_sal_contract.valid_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_sal_contract.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_sal_contract.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_contract.total_amount IS '合同金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_contract.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_contract.total_amount_with_tax IS '合同金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_contract.signed_by IS '签约人(职员)';
                    
      COMMENT ON COLUMN erp_sal_contract.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_contract.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_contract.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_contract.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_contract.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_contract.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_contract.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_contract.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_contract.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_invoice IS '销售发票';
                
      COMMENT ON COLUMN erp_sal_invoice.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_invoice.code IS '单号';
                    
      COMMENT ON COLUMN erp_sal_invoice.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_invoice.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_sal_invoice.invoice_no IS '发票号码';
                    
      COMMENT ON COLUMN erp_sal_invoice.invoice_type IS '发票类型';
                    
      COMMENT ON COLUMN erp_sal_invoice.business_date IS '发票日期';
                    
      COMMENT ON COLUMN erp_sal_invoice.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_sal_invoice.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_invoice.amount_source IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_sal_invoice.amount_functional IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_sal_invoice.total_amount IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_invoice.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_invoice.total_amount_with_tax IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_invoice.received_amount IS '已收金额';
                    
      COMMENT ON COLUMN erp_sal_invoice.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_invoice.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_invoice.received_status IS '收款进度';
                    
      COMMENT ON COLUMN erp_sal_invoice.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_invoice.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_invoice.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_invoice.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_invoice.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_invoice.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_invoice.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_invoice.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_invoice.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_invoice.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_invoice.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_invoice.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_receipt IS '收款单';
                
      COMMENT ON COLUMN erp_sal_receipt.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_receipt.code IS '单号';
                    
      COMMENT ON COLUMN erp_sal_receipt.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_receipt.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_sal_receipt.business_date IS '收款日期';
                    
      COMMENT ON COLUMN erp_sal_receipt.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_sal_receipt.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_receipt.amount_source IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_sal_receipt.amount_functional IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_sal_receipt.total_amount IS '合计金额';
                    
      COMMENT ON COLUMN erp_sal_receipt.settlement_method_id IS '结算方式';
                    
      COMMENT ON COLUMN erp_sal_receipt.receipt_method IS '收款方式';
                    
      COMMENT ON COLUMN erp_sal_receipt.bank_account_id IS '收款账户(资金账户)';
                    
      COMMENT ON COLUMN erp_sal_receipt.partner_bank_account_id IS '客户付款账户(银行账户主数据)';
                    
      COMMENT ON COLUMN erp_sal_receipt.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_receipt.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_receipt.written_off_status IS '核销状态';
                    
      COMMENT ON COLUMN erp_sal_receipt.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_receipt.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_receipt.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_receipt.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_receipt.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_receipt.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_receipt.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_receipt.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_receipt.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_receipt.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_receipt.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_receipt.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_order IS '销售订单';
                
      COMMENT ON COLUMN erp_sal_order.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_order.code IS '单号';
                    
      COMMENT ON COLUMN erp_sal_order.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_order.quotation_id IS '报价单';
                    
      COMMENT ON COLUMN erp_sal_order.contract_id IS '销售合同';
                    
      COMMENT ON COLUMN erp_sal_order.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_sal_order.warehouse_id IS '发货仓库';
                    
      COMMENT ON COLUMN erp_sal_order.business_date IS '订单日期';
                    
      COMMENT ON COLUMN erp_sal_order.delivery_date IS '交货日期';
                    
      COMMENT ON COLUMN erp_sal_order.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_sal_order.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_order.amount_source IS '合计金额(源币不含税)';
                    
      COMMENT ON COLUMN erp_sal_order.amount_functional IS '合计金额(本位币不含税)';
                    
      COMMENT ON COLUMN erp_sal_order.total_amount IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_order.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_order.total_amount_with_tax IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_order.discount_rate IS '整单折扣率(%)';
                    
      COMMENT ON COLUMN erp_sal_order.discount_amount IS '折扣金额';
                    
      COMMENT ON COLUMN erp_sal_order.received_amount IS '已收金额';
                    
      COMMENT ON COLUMN erp_sal_order.settlement_method_id IS '结算方式';
                    
      COMMENT ON COLUMN erp_sal_order.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_order.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_order.received_status IS '收款进度';
                    
      COMMENT ON COLUMN erp_sal_order.delivery_status IS '发货进度';
                    
      COMMENT ON COLUMN erp_sal_order.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_order.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_order.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_order.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_order.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_order.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_order.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_order.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_order.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_order.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_order.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_order.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_quotation_line IS '销售报价单行';
                
      COMMENT ON COLUMN erp_sal_quotation_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.quotation_id IS '报价单ID';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.uom_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.unit_price IS '报价单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.tax_amount IS '税额';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.amount IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.amount_with_tax IS '金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_quotation_line.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_invoice_line IS '销售发票行';
                
      COMMENT ON COLUMN erp_sal_invoice_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.invoice_id IS '发票ID';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.delivery_line_id IS '出库行ID';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.uom_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.unit_price IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.tax_amount IS '税额';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.amount IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_invoice_line.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_receipt_line IS '收款核销行';
                
      COMMENT ON COLUMN erp_sal_receipt_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.receipt_id IS '收款单ID';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.invoice_id IS '发票ID';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.amount IS '核销金额';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_receipt_line.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_order_line IS '销售订单行';
                
      COMMENT ON COLUMN erp_sal_order_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_order_line.order_id IS '订单ID';
                    
      COMMENT ON COLUMN erp_sal_order_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_sal_order_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_sal_order_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_sal_order_line.uom_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_order_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_sal_order_line.unit_price IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_order_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_order_line.tax_rate_id IS '税率主数据';
                    
      COMMENT ON COLUMN erp_sal_order_line.tax_amount IS '税额';
                    
      COMMENT ON COLUMN erp_sal_order_line.amount IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_order_line.amount_with_tax IS '金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_order_line.delivered_quantity IS '已出库数量';
                    
      COMMENT ON COLUMN erp_sal_order_line.invoiced_quantity IS '已开票数量';
                    
      COMMENT ON COLUMN erp_sal_order_line.warehouse_id IS '发货仓库';
                    
      COMMENT ON COLUMN erp_sal_order_line.project_id IS '项目';
                    
      COMMENT ON COLUMN erp_sal_order_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_order_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_order_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_order_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_order_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_order_line.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_order_line.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_delivery IS '销售出库单';
                
      COMMENT ON COLUMN erp_sal_delivery.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_delivery.code IS '单号';
                    
      COMMENT ON COLUMN erp_sal_delivery.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_delivery.order_id IS '销售订单';
                    
      COMMENT ON COLUMN erp_sal_delivery.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_sal_delivery.warehouse_id IS '出库仓库';
                    
      COMMENT ON COLUMN erp_sal_delivery.business_date IS '出库日期';
                    
      COMMENT ON COLUMN erp_sal_delivery.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_sal_delivery.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_delivery.amount_source IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_sal_delivery.amount_functional IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_sal_delivery.total_amount IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_delivery.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_delivery.total_amount_with_tax IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_delivery.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_delivery.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_delivery.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_delivery.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_delivery.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_delivery.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_delivery.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_delivery.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_delivery.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_delivery.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_delivery.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_delivery.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_delivery.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_delivery.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_delivery_line IS '销售出库单行';
                
      COMMENT ON COLUMN erp_sal_delivery_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.delivery_id IS '出库单ID';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.order_line_id IS '订单行ID';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.uom_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.quantity IS '实发数量';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.unit_price IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.tax_amount IS '税额';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.amount IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.warehouse_id IS '出库库位';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_delivery_line.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_return IS '销售退货单';
                
      COMMENT ON COLUMN erp_sal_return.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_return.code IS '单号';
                    
      COMMENT ON COLUMN erp_sal_return.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_sal_return.delivery_id IS '出库单';
                    
      COMMENT ON COLUMN erp_sal_return.customer_id IS '客户';
                    
      COMMENT ON COLUMN erp_sal_return.warehouse_id IS '入库仓库';
                    
      COMMENT ON COLUMN erp_sal_return.business_date IS '退货日期';
                    
      COMMENT ON COLUMN erp_sal_return.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_sal_return.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_sal_return.amount_source IS '合计金额(源币)';
                    
      COMMENT ON COLUMN erp_sal_return.amount_functional IS '合计金额(本位币)';
                    
      COMMENT ON COLUMN erp_sal_return.total_amount IS '合计金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_return.total_tax_amount IS '合计税额';
                    
      COMMENT ON COLUMN erp_sal_return.total_amount_with_tax IS '合计金额(含税)';
                    
      COMMENT ON COLUMN erp_sal_return.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_sal_return.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_sal_return.posted IS '已过账';
                    
      COMMENT ON COLUMN erp_sal_return.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_sal_return.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_sal_return.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_sal_return.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_sal_return.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_return.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_return.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_return.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_return.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_return.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_return.remark IS '备注';
                    
      COMMENT ON TABLE erp_sal_return_line IS '销售退货单行';
                
      COMMENT ON COLUMN erp_sal_return_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sal_return_line.return_id IS '退货单ID';
                    
      COMMENT ON COLUMN erp_sal_return_line.delivery_line_id IS '出库行ID';
                    
      COMMENT ON COLUMN erp_sal_return_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_sal_return_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_sal_return_line.sku_id IS 'SKU';
                    
      COMMENT ON COLUMN erp_sal_return_line.uom_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_sal_return_line.quantity IS '退货数量';
                    
      COMMENT ON COLUMN erp_sal_return_line.unit_price IS '单价(不含税)';
                    
      COMMENT ON COLUMN erp_sal_return_line.tax_rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_sal_return_line.tax_amount IS '税额';
                    
      COMMENT ON COLUMN erp_sal_return_line.amount IS '金额(不含税)';
                    
      COMMENT ON COLUMN erp_sal_return_line.reason IS '退货原因';
                    
      COMMENT ON COLUMN erp_sal_return_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sal_return_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sal_return_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sal_return_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sal_return_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sal_return_line.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_sal_return_line.remark IS '备注';
                    
