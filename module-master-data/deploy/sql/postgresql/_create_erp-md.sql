
CREATE TABLE erp_md_material_category(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  parent_id INT8  ,
  sort_num INT4  ,
  price_validation_level VARCHAR(20) default '20'   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_material_category primary key (id)
);

CREATE TABLE erp_md_uom(
  id INT8 NOT NULL ,
  code VARCHAR(20) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  uom_group VARCHAR(50)  ,
  is_base BOOLEAN default false   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_uom primary key (id)
);

CREATE TABLE erp_md_tax_rate(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  tax_type VARCHAR(20) NOT NULL ,
  rate NUMERIC(10,6) NOT NULL ,
  is_zero_rated BOOLEAN default false   ,
  is_exempt BOOLEAN default false   ,
  valid_from DATE  ,
  valid_to DATE  ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_tax_rate primary key (id)
);

CREATE TABLE erp_md_partner(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  partner_type VARCHAR(20) NOT NULL ,
  status VARCHAR(20) NOT NULL ,
  contact_person VARCHAR(100)  ,
  phone VARCHAR(50)  ,
  email VARCHAR(200)  ,
  address VARCHAR(500)  ,
  tax_no VARCHAR(50)  ,
  credit_limit NUMERIC(20,4)  ,
  credit_period_days INT4  ,
  receivable_balance NUMERIC(20,4) default 0   ,
  payable_balance NUMERIC(20,4) default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  customer_group VARCHAR(100)  ,
  constraint PK_erp_md_partner primary key (id)
);

CREATE TABLE erp_md_currency(
  id INT8 NOT NULL ,
  code VARCHAR(10) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  symbol VARCHAR(10)  ,
  decimal_places INT4 default 2   ,
  is_functional BOOLEAN default false   ,
  is_active BOOLEAN default true   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_currency primary key (id)
);

CREATE TABLE erp_md_settlement_method(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  settlement_type VARCHAR(20) NOT NULL ,
  default_fund_account_id INT8  ,
  default_days INT4  ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_settlement_method primary key (id)
);

CREATE TABLE erp_md_partner_address(
  id INT8 NOT NULL ,
  partner_id INT8 NOT NULL ,
  address_type VARCHAR(20) NOT NULL ,
  contact_person VARCHAR(100)  ,
  phone VARCHAR(50)  ,
  address VARCHAR(500) NOT NULL ,
  is_default BOOLEAN default false   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_partner_address primary key (id)
);

CREATE TABLE erp_md_partner_contact(
  id INT8 NOT NULL ,
  partner_id INT8 NOT NULL ,
  contact_person VARCHAR(100) NOT NULL ,
  position VARCHAR(100)  ,
  phone VARCHAR(50)  ,
  email VARCHAR(200)  ,
  is_default BOOLEAN default false   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_md_partner_contact primary key (id)
);

CREATE TABLE erp_md_bank_account(
  id INT8 NOT NULL ,
  partner_id INT8 NOT NULL ,
  bank_name VARCHAR(200) NOT NULL ,
  bank_branch VARCHAR(200)  ,
  bank_account VARCHAR(50) NOT NULL ,
  account_type VARCHAR(20)  ,
  account_holder VARCHAR(200)  ,
  is_default BOOLEAN default false   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_bank_account primary key (id)
);

CREATE TABLE erp_md_exchange_rate(
  id INT8 NOT NULL ,
  from_currency_id INT8 NOT NULL ,
  to_currency_id INT8 NOT NULL ,
  rate_type VARCHAR(30) default 'SPOT'   ,
  rate NUMERIC(20,8) NOT NULL ,
  valid_from DATE NOT NULL ,
  valid_to DATE  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_exchange_rate primary key (id)
);

CREATE TABLE erp_md_subject(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  parent_id INT8  ,
  subject_class VARCHAR(20) NOT NULL ,
  direction VARCHAR(20) NOT NULL ,
  balance_type VARCHAR(20)  ,
  currency_id INT8  ,
  is_auxiliary_partner BOOLEAN default false   ,
  is_auxiliary_department BOOLEAN default false   ,
  is_auxiliary_project BOOLEAN default false   ,
  is_auxiliary_warehouse BOOLEAN default false   ,
  is_auxiliary_product BOOLEAN default false   ,
  is_auxiliary_cost_center BOOLEAN default false   ,
  is_budgetable BOOLEAN default false   ,
  is_leaf BOOLEAN default true   ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_md_subject primary key (id)
);

CREATE TABLE erp_md_organization(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  parent_id INT8  ,
  org_type VARCHAR(20) NOT NULL ,
  functional_currency_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_organization primary key (id)
);

CREATE TABLE erp_md_employee(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  position VARCHAR(100)  ,
  phone VARCHAR(50)  ,
  email VARCHAR(200)  ,
  partner_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_employee primary key (id)
);

CREATE TABLE erp_md_acct_schema(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8 NOT NULL ,
  nature VARCHAR(20) NOT NULL ,
  functional_currency_id INT8 NOT NULL ,
  costing_method VARCHAR(20)  ,
  is_adjust_currency BOOLEAN default true   ,
  is_propagate BOOLEAN default false   ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_acct_schema primary key (id)
);

CREATE TABLE erp_md_supplier_approval(
  id INT8 NOT NULL ,
  partner_id INT8 NOT NULL ,
  org_id INT8  ,
  approval_type VARCHAR(20) NOT NULL ,
  material_category_id INT8 NOT NULL ,
  valid_from DATE  ,
  valid_to DATE  ,
  qualification_doc VARCHAR(500)  ,
  status VARCHAR(20) default '10'  NOT NULL ,
  approved_by VARCHAR(50)  ,
  approved_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_md_supplier_approval primary key (id)
);

CREATE TABLE erp_sys_config(
  id INT8 NOT NULL ,
  config_key VARCHAR(200) NOT NULL ,
  config_value VARCHAR(2000) NOT NULL ,
  org_id INT8  ,
  description VARCHAR(500)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_sys_config primary key (id)
);

CREATE TABLE erp_md_warehouse(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  warehouse_type VARCHAR(20)  ,
  org_id INT8  ,
  address VARCHAR(500)  ,
  manager_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  batch_selection_strategy VARCHAR(20)  ,
  constraint PK_erp_md_warehouse primary key (id)
);

CREATE TABLE erp_md_cost_center(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8 NOT NULL ,
  manager_id INT8  ,
  parent_id INT8  ,
  is_budgetable BOOLEAN default false   ,
  status VARCHAR(20) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_md_cost_center primary key (id)
);

CREATE TABLE erp_md_acct_schema_coa(
  id INT8 NOT NULL ,
  acct_schema_id INT8 NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  is_primary BOOLEAN default false   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_acct_schema_coa primary key (id)
);

CREATE TABLE erp_md_subject_mapping(
  id INT8 NOT NULL ,
  source_subject_id INT8 NOT NULL ,
  target_acct_schema_id INT8 NOT NULL ,
  target_subject_id INT8 NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_subject_mapping primary key (id)
);

CREATE TABLE erp_md_material(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  material_type VARCHAR(20) NOT NULL ,
  category_id INT8  ,
  uom_id INT8 NOT NULL ,
  status VARCHAR(20) NOT NULL ,
  cost_method VARCHAR(20)  ,
  is_batch_managed BOOLEAN default false   ,
  is_serial_managed BOOLEAN default false   ,
  default_warehouse_id INT8  ,
  min_stock NUMERIC(20,4)  ,
  max_stock NUMERIC(20,4)  ,
  safety_stock NUMERIC(20,4)  ,
  lead_time_days INT4  ,
  weight NUMERIC(12,4)  ,
  volume NUMERIC(12,6)  ,
  default_tax_rate_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  constraint PK_erp_md_material primary key (id)
);

CREATE TABLE erp_md_location(
  id INT8 NOT NULL ,
  warehouse_id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  parent_id INT8  ,
  is_active BOOLEAN default true   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_location primary key (id)
);

CREATE TABLE erp_md_material_sku(
  id INT8 NOT NULL ,
  material_id INT8 NOT NULL ,
  sku_code VARCHAR(50) NOT NULL ,
  barcode VARCHAR(50)  ,
  uom_id INT8 NOT NULL ,
  conversion_rate NUMERIC(12,4) default 1   ,
  purchase_price NUMERIC(20,4)  ,
  sale_price NUMERIC(20,4)  ,
  wholesale_price NUMERIC(20,4)  ,
  retail_price NUMERIC(20,4)  ,
  tax_rate_id INT8  ,
  is_default BOOLEAN default false   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_material_sku primary key (id)
);

CREATE TABLE erp_md_uom_conversion(
  id INT8 NOT NULL ,
  material_id INT8  ,
  from_uom_id INT8 NOT NULL ,
  to_uom_id INT8 NOT NULL ,
  conversion_rate NUMERIC(20,8) NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_md_uom_conversion primary key (id)
);


      COMMENT ON TABLE erp_md_material_category IS '物料分类';
                
      COMMENT ON COLUMN erp_md_material_category.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_material_category.code IS '编码';
                    
      COMMENT ON COLUMN erp_md_material_category.name IS '名称';
                    
      COMMENT ON COLUMN erp_md_material_category.parent_id IS '父级ID';
                    
      COMMENT ON COLUMN erp_md_material_category.sort_num IS '排序';
                    
      COMMENT ON COLUMN erp_md_material_category.price_validation_level IS '价格校验级别';
                    
      COMMENT ON COLUMN erp_md_material_category.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_material_category.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_material_category.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_material_category.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_material_category.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_material_category.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_uom IS '计量单位';
                
      COMMENT ON COLUMN erp_md_uom.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_uom.code IS '编码';
                    
      COMMENT ON COLUMN erp_md_uom.name IS '名称';
                    
      COMMENT ON COLUMN erp_md_uom.uom_group IS '单位组';
                    
      COMMENT ON COLUMN erp_md_uom.is_base IS '是否基本单位';
                    
      COMMENT ON COLUMN erp_md_uom.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_uom.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_uom.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_uom.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_uom.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_uom.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_tax_rate IS '税率';
                
      COMMENT ON COLUMN erp_md_tax_rate.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_tax_rate.code IS '税率编码';
                    
      COMMENT ON COLUMN erp_md_tax_rate.name IS '税率名称';
                    
      COMMENT ON COLUMN erp_md_tax_rate.tax_type IS '税种';
                    
      COMMENT ON COLUMN erp_md_tax_rate.rate IS '税率(%)';
                    
      COMMENT ON COLUMN erp_md_tax_rate.is_zero_rated IS '是否零税率';
                    
      COMMENT ON COLUMN erp_md_tax_rate.is_exempt IS '是否免税';
                    
      COMMENT ON COLUMN erp_md_tax_rate.valid_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_md_tax_rate.valid_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_md_tax_rate.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_tax_rate.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_tax_rate.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_tax_rate.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_tax_rate.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_tax_rate.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_tax_rate.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON COLUMN erp_md_partner.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_partner.code IS '编码';
                    
      COMMENT ON COLUMN erp_md_partner.name IS '名称';
                    
      COMMENT ON COLUMN erp_md_partner.partner_type IS '类型';
                    
      COMMENT ON COLUMN erp_md_partner.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_partner.contact_person IS '联系人';
                    
      COMMENT ON COLUMN erp_md_partner.phone IS '电话';
                    
      COMMENT ON COLUMN erp_md_partner.email IS '邮箱';
                    
      COMMENT ON COLUMN erp_md_partner.address IS '地址';
                    
      COMMENT ON COLUMN erp_md_partner.tax_no IS '税号';
                    
      COMMENT ON COLUMN erp_md_partner.credit_limit IS '信用额度';
                    
      COMMENT ON COLUMN erp_md_partner.credit_period_days IS '信用期(天)';
                    
      COMMENT ON COLUMN erp_md_partner.receivable_balance IS '应收余额';
                    
      COMMENT ON COLUMN erp_md_partner.payable_balance IS '应付余额';
                    
      COMMENT ON COLUMN erp_md_partner.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_partner.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_partner.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_partner.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_partner.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_partner.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_partner.remark IS '备注';
                    
      COMMENT ON COLUMN erp_md_partner.customer_group IS '客户组';
                    
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON COLUMN erp_md_currency.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_currency.code IS '币种代码';
                    
      COMMENT ON COLUMN erp_md_currency.name IS '名称';
                    
      COMMENT ON COLUMN erp_md_currency.symbol IS '符号';
                    
      COMMENT ON COLUMN erp_md_currency.decimal_places IS '小数位数';
                    
      COMMENT ON COLUMN erp_md_currency.is_functional IS '是否本位币';
                    
      COMMENT ON COLUMN erp_md_currency.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_md_currency.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_currency.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_currency.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_currency.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_currency.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_currency.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_settlement_method IS '结算方式';
                
      COMMENT ON COLUMN erp_md_settlement_method.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_settlement_method.code IS '编码';
                    
      COMMENT ON COLUMN erp_md_settlement_method.name IS '名称';
                    
      COMMENT ON COLUMN erp_md_settlement_method.settlement_type IS '结算类型';
                    
      COMMENT ON COLUMN erp_md_settlement_method.default_fund_account_id IS '默认资金账户';
                    
      COMMENT ON COLUMN erp_md_settlement_method.default_days IS '默认账期(天)';
                    
      COMMENT ON COLUMN erp_md_settlement_method.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_settlement_method.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_settlement_method.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_settlement_method.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_settlement_method.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_settlement_method.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_settlement_method.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_partner_address IS '往来单位地址';
                
      COMMENT ON COLUMN erp_md_partner_address.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_partner_address.partner_id IS '往来单位';
                    
      COMMENT ON COLUMN erp_md_partner_address.address_type IS '地址类型';
                    
      COMMENT ON COLUMN erp_md_partner_address.contact_person IS '联系人';
                    
      COMMENT ON COLUMN erp_md_partner_address.phone IS '电话';
                    
      COMMENT ON COLUMN erp_md_partner_address.address IS '详细地址';
                    
      COMMENT ON COLUMN erp_md_partner_address.is_default IS '是否默认';
                    
      COMMENT ON COLUMN erp_md_partner_address.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_partner_address.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_partner_address.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_partner_address.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_partner_address.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_partner_address.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_partner_contact IS '往来单位联系人';
                
      COMMENT ON COLUMN erp_md_partner_contact.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_partner_contact.partner_id IS '往来单位';
                    
      COMMENT ON COLUMN erp_md_partner_contact.contact_person IS '联系人姓名';
                    
      COMMENT ON COLUMN erp_md_partner_contact.position IS '职务';
                    
      COMMENT ON COLUMN erp_md_partner_contact.phone IS '电话';
                    
      COMMENT ON COLUMN erp_md_partner_contact.email IS '邮箱';
                    
      COMMENT ON COLUMN erp_md_partner_contact.is_default IS '是否默认联系人';
                    
      COMMENT ON COLUMN erp_md_partner_contact.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_partner_contact.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_partner_contact.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_partner_contact.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_partner_contact.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_partner_contact.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_partner_contact.remark IS '备注';
                    
      COMMENT ON TABLE erp_md_bank_account IS '银行账户';
                
      COMMENT ON COLUMN erp_md_bank_account.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_bank_account.partner_id IS '往来单位';
                    
      COMMENT ON COLUMN erp_md_bank_account.bank_name IS '开户银行';
                    
      COMMENT ON COLUMN erp_md_bank_account.bank_branch IS '支行';
                    
      COMMENT ON COLUMN erp_md_bank_account.bank_account IS '银行账号';
                    
      COMMENT ON COLUMN erp_md_bank_account.account_type IS '账户类型';
                    
      COMMENT ON COLUMN erp_md_bank_account.account_holder IS '户名';
                    
      COMMENT ON COLUMN erp_md_bank_account.is_default IS '是否默认';
                    
      COMMENT ON COLUMN erp_md_bank_account.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_bank_account.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_bank_account.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_bank_account.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_bank_account.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_bank_account.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_exchange_rate IS '汇率';
                
      COMMENT ON COLUMN erp_md_exchange_rate.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.from_currency_id IS '源币种';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.to_currency_id IS '目标币种';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.rate_type IS '汇率类型';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.rate IS '汇率';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.valid_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.valid_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_exchange_rate.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_subject IS '会计科目';
                
      COMMENT ON COLUMN erp_md_subject.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_subject.code IS '科目编码';
                    
      COMMENT ON COLUMN erp_md_subject.name IS '科目名称';
                    
      COMMENT ON COLUMN erp_md_subject.parent_id IS '父级科目';
                    
      COMMENT ON COLUMN erp_md_subject.subject_class IS '科目类别';
                    
      COMMENT ON COLUMN erp_md_subject.direction IS '余额方向';
                    
      COMMENT ON COLUMN erp_md_subject.balance_type IS '余额类型';
                    
      COMMENT ON COLUMN erp_md_subject.currency_id IS '核算币种';
                    
      COMMENT ON COLUMN erp_md_subject.is_auxiliary_partner IS '辅助-往来单位';
                    
      COMMENT ON COLUMN erp_md_subject.is_auxiliary_department IS '辅助-部门';
                    
      COMMENT ON COLUMN erp_md_subject.is_auxiliary_project IS '辅助-项目';
                    
      COMMENT ON COLUMN erp_md_subject.is_auxiliary_warehouse IS '辅助-仓库';
                    
      COMMENT ON COLUMN erp_md_subject.is_auxiliary_product IS '辅助-物料';
                    
      COMMENT ON COLUMN erp_md_subject.is_auxiliary_cost_center IS '辅助-成本中心';
                    
      COMMENT ON COLUMN erp_md_subject.is_budgetable IS '是否预算控制';
                    
      COMMENT ON COLUMN erp_md_subject.is_leaf IS '是否明细科目';
                    
      COMMENT ON COLUMN erp_md_subject.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_subject.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_subject.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_subject.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_subject.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_subject.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_subject.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_subject.remark IS '备注';
                    
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON COLUMN erp_md_organization.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_organization.code IS '编码';
                    
      COMMENT ON COLUMN erp_md_organization.name IS '名称';
                    
      COMMENT ON COLUMN erp_md_organization.parent_id IS '父级组织';
                    
      COMMENT ON COLUMN erp_md_organization.org_type IS '组织类型';
                    
      COMMENT ON COLUMN erp_md_organization.functional_currency_id IS '本位币';
                    
      COMMENT ON COLUMN erp_md_organization.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_organization.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_organization.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_organization.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_organization.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_organization.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_organization.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON COLUMN erp_md_employee.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_employee.code IS '工号';
                    
      COMMENT ON COLUMN erp_md_employee.name IS '姓名';
                    
      COMMENT ON COLUMN erp_md_employee.org_id IS '所属组织/部门';
                    
      COMMENT ON COLUMN erp_md_employee.position IS '职务';
                    
      COMMENT ON COLUMN erp_md_employee.phone IS '电话';
                    
      COMMENT ON COLUMN erp_md_employee.email IS '邮箱';
                    
      COMMENT ON COLUMN erp_md_employee.partner_id IS '对应内部往来单位';
                    
      COMMENT ON COLUMN erp_md_employee.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_employee.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_employee.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_employee.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_employee.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_employee.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_employee.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_acct_schema IS '会计核算表(账套)';
                
      COMMENT ON COLUMN erp_md_acct_schema.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_acct_schema.code IS '账套编码';
                    
      COMMENT ON COLUMN erp_md_acct_schema.name IS '账套名称';
                    
      COMMENT ON COLUMN erp_md_acct_schema.org_id IS '核算组织';
                    
      COMMENT ON COLUMN erp_md_acct_schema.nature IS '账套性质';
                    
      COMMENT ON COLUMN erp_md_acct_schema.functional_currency_id IS '本位币';
                    
      COMMENT ON COLUMN erp_md_acct_schema.costing_method IS '默认成本方法';
                    
      COMMENT ON COLUMN erp_md_acct_schema.is_adjust_currency IS '是否调整汇率差异';
                    
      COMMENT ON COLUMN erp_md_acct_schema.is_propagate IS '是否自动复制分录到其它账套';
                    
      COMMENT ON COLUMN erp_md_acct_schema.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_acct_schema.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_acct_schema.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_acct_schema.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_acct_schema.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_acct_schema.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_acct_schema.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_supplier_approval IS '供应商准入资格';
                
      COMMENT ON COLUMN erp_md_supplier_approval.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.partner_id IS '供应商';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.approval_type IS '准入类型';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.material_category_id IS '准入物料类别';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.valid_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.valid_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.qualification_doc IS '资质文件';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.status IS '准入状态';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.approved_by IS '批准人';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.approved_at IS '批准时间';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_supplier_approval.remark IS '备注';
                    
      COMMENT ON TABLE erp_sys_config IS '系统配置';
                
      COMMENT ON COLUMN erp_sys_config.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sys_config.config_key IS '配置键';
                    
      COMMENT ON COLUMN erp_sys_config.config_value IS '配置值';
                    
      COMMENT ON COLUMN erp_sys_config.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_sys_config.description IS '说明';
                    
      COMMENT ON COLUMN erp_sys_config.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sys_config.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sys_config.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sys_config.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sys_config.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sys_config.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON COLUMN erp_md_warehouse.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_warehouse.code IS '编码';
                    
      COMMENT ON COLUMN erp_md_warehouse.name IS '名称';
                    
      COMMENT ON COLUMN erp_md_warehouse.warehouse_type IS '仓库类型';
                    
      COMMENT ON COLUMN erp_md_warehouse.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_md_warehouse.address IS '地址';
                    
      COMMENT ON COLUMN erp_md_warehouse.manager_id IS '负责人(职员)';
                    
      COMMENT ON COLUMN erp_md_warehouse.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_warehouse.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_warehouse.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_warehouse.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_warehouse.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_warehouse.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_warehouse.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_warehouse.remark IS '备注';
                    
      COMMENT ON COLUMN erp_md_warehouse.batch_selection_strategy IS '批次选择策略';
                    
      COMMENT ON TABLE erp_md_cost_center IS '成本中心';
                
      COMMENT ON COLUMN erp_md_cost_center.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_cost_center.code IS '编码';
                    
      COMMENT ON COLUMN erp_md_cost_center.name IS '名称';
                    
      COMMENT ON COLUMN erp_md_cost_center.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_md_cost_center.manager_id IS '负责人';
                    
      COMMENT ON COLUMN erp_md_cost_center.parent_id IS '上级成本中心';
                    
      COMMENT ON COLUMN erp_md_cost_center.is_budgetable IS '是否预算管控';
                    
      COMMENT ON COLUMN erp_md_cost_center.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_cost_center.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_cost_center.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_cost_center.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_cost_center.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_cost_center.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_cost_center.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_cost_center.remark IS '备注';
                    
      COMMENT ON TABLE erp_md_acct_schema_coa IS '账套科目表';
                
      COMMENT ON COLUMN erp_md_acct_schema_coa.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.name IS '科目表名称';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.is_primary IS '是否主科目表';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_acct_schema_coa.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_subject_mapping IS '科目映射';
                
      COMMENT ON COLUMN erp_md_subject_mapping.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_subject_mapping.source_subject_id IS '源科目';
                    
      COMMENT ON COLUMN erp_md_subject_mapping.target_acct_schema_id IS '目标账套';
                    
      COMMENT ON COLUMN erp_md_subject_mapping.target_subject_id IS '目标科目';
                    
      COMMENT ON COLUMN erp_md_subject_mapping.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_subject_mapping.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_subject_mapping.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_subject_mapping.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_subject_mapping.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_subject_mapping.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON COLUMN erp_md_material.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_material.code IS '物料编码';
                    
      COMMENT ON COLUMN erp_md_material.name IS '物料名称';
                    
      COMMENT ON COLUMN erp_md_material.material_type IS '物料类型';
                    
      COMMENT ON COLUMN erp_md_material.category_id IS '分类ID';
                    
      COMMENT ON COLUMN erp_md_material.uom_id IS '主计量单位';
                    
      COMMENT ON COLUMN erp_md_material.status IS '状态';
                    
      COMMENT ON COLUMN erp_md_material.cost_method IS '存货计价方法';
                    
      COMMENT ON COLUMN erp_md_material.is_batch_managed IS '是否批次管理';
                    
      COMMENT ON COLUMN erp_md_material.is_serial_managed IS '是否序列号管理';
                    
      COMMENT ON COLUMN erp_md_material.default_warehouse_id IS '默认仓库';
                    
      COMMENT ON COLUMN erp_md_material.min_stock IS '最低库存';
                    
      COMMENT ON COLUMN erp_md_material.max_stock IS '最高库存';
                    
      COMMENT ON COLUMN erp_md_material.safety_stock IS '安全库存';
                    
      COMMENT ON COLUMN erp_md_material.lead_time_days IS '采购提前期(天)';
                    
      COMMENT ON COLUMN erp_md_material.weight IS '重量(kg)';
                    
      COMMENT ON COLUMN erp_md_material.volume IS '体积(m3)';
                    
      COMMENT ON COLUMN erp_md_material.default_tax_rate_id IS '默认税率';
                    
      COMMENT ON COLUMN erp_md_material.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_material.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_material.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_material.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_material.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_material.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_md_material.remark IS '备注';
                    
      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON COLUMN erp_md_location.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_location.warehouse_id IS '仓库ID';
                    
      COMMENT ON COLUMN erp_md_location.code IS '编码';
                    
      COMMENT ON COLUMN erp_md_location.name IS '名称';
                    
      COMMENT ON COLUMN erp_md_location.parent_id IS '父级库位';
                    
      COMMENT ON COLUMN erp_md_location.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_md_location.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_location.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_location.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_location.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_location.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_location.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_material_sku IS '物料SKU';
                
      COMMENT ON COLUMN erp_md_material_sku.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_material_sku.material_id IS '物料ID';
                    
      COMMENT ON COLUMN erp_md_material_sku.sku_code IS 'SKU编码';
                    
      COMMENT ON COLUMN erp_md_material_sku.barcode IS '条码';
                    
      COMMENT ON COLUMN erp_md_material_sku.uom_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_md_material_sku.conversion_rate IS '换算系数';
                    
      COMMENT ON COLUMN erp_md_material_sku.purchase_price IS '采购价(不含税)';
                    
      COMMENT ON COLUMN erp_md_material_sku.sale_price IS '销售价(不含税)';
                    
      COMMENT ON COLUMN erp_md_material_sku.wholesale_price IS '批发价(不含税)';
                    
      COMMENT ON COLUMN erp_md_material_sku.retail_price IS '零售价(不含税)';
                    
      COMMENT ON COLUMN erp_md_material_sku.tax_rate_id IS '默认税率';
                    
      COMMENT ON COLUMN erp_md_material_sku.is_default IS '是否默认SKU';
                    
      COMMENT ON COLUMN erp_md_material_sku.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_material_sku.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_material_sku.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_material_sku.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_material_sku.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_material_sku.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_uom_conversion IS '单位换算';
                
      COMMENT ON COLUMN erp_md_uom_conversion.id IS 'ID';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.material_id IS '物料(空表示通用)';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.from_uom_id IS '源单位';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.to_uom_id IS '目标单位';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.conversion_rate IS '换算系数';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_md_uom_conversion.update_time IS '修改时间';
                    
