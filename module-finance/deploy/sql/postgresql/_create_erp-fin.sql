
CREATE TABLE erp_fin_accounting_period(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  year INT4 NOT NULL ,
  month INT4 NOT NULL ,
  start_date DATE NOT NULL ,
  end_date DATE NOT NULL ,
  quarter INT4  ,
  is_adjustment BOOLEAN default false   ,
  status INT4 NOT NULL ,
  closed_by INT8  ,
  closed_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_accounting_period primary key (id)
);

CREATE TABLE erp_md_acct_schema(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  currency_id INT8  ,
  nature INT4  ,
  constraint PK_erp_md_acct_schema primary key (id)
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

CREATE TABLE erp_md_currency(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  symbol VARCHAR(50)  ,
  decimal_places INT4  ,
  is_functional BOOLEAN  ,
  constraint PK_erp_md_currency primary key (id)
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

CREATE TABLE erp_md_organization(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  org_type INT4  ,
  parent_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_organization primary key (id)
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

CREATE TABLE erp_md_material(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  material_type INT4  ,
  status INT4  ,
  constraint PK_erp_md_material primary key (id)
);

CREATE TABLE erp_prj_project(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  customer_id INT8  ,
  status INT4  ,
  constraint PK_erp_prj_project primary key (id)
);

CREATE TABLE erp_md_cost_center(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  org_id INT8  ,
  constraint PK_erp_md_cost_center primary key (id)
);

CREATE TABLE erp_ast_asset(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  asset_type INT4  ,
  status INT4  ,
  constraint PK_erp_ast_asset primary key (id)
);

CREATE TABLE erp_fin_voucher(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  voucher_type INT4 NOT NULL ,
  posting_type INT4  ,
  voucher_date DATE NOT NULL ,
  voucher_no VARCHAR(50)  ,
  org_id INT8 NOT NULL ,
  acct_schema_id INT8 NOT NULL ,
  period_id INT8 NOT NULL ,
  total_debit NUMERIC(20,4) default 0   ,
  total_credit NUMERIC(20,4) default 0   ,
  is_reversed BOOLEAN default false   ,
  reversal_of_voucher_id INT8  ,
  doc_status INT4 NOT NULL ,
  posted_by INT8  ,
  posted_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher primary key (id)
);

CREATE TABLE erp_fin_voucher_template(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  acct_schema_id INT8  ,
  business_type INT4 NOT NULL ,
  voucher_type INT4  ,
  template_type VARCHAR(50)  ,
  is_active BOOLEAN default true   ,
  valid_from DATE  ,
  valid_to DATE  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher_template primary key (id)
);

CREATE TABLE erp_fin_accounting_period_status(
  id INT8 NOT NULL ,
  period_id INT8 NOT NULL ,
  acct_schema_id INT8 NOT NULL ,
  total_vouchers INT4 default 0   ,
  posted_vouchers INT4 default 0   ,
  unposted_vouchers INT4 default 0   ,
  ar_status INT4 NOT NULL ,
  ap_status INT4 NOT NULL ,
  inv_status INT4 NOT NULL ,
  gl_status INT4 NOT NULL ,
  asset_status INT4 NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_accounting_period_status primary key (id)
);

CREATE TABLE erp_fin_trial_balance(
  id INT8 NOT NULL ,
  org_id INT8 NOT NULL ,
  acct_schema_id INT8 NOT NULL ,
  period_id INT8 NOT NULL ,
  subject_id INT8 NOT NULL ,
  subject_code VARCHAR(50) NOT NULL ,
  subject_name VARCHAR(200)  ,
  opening_debit NUMERIC(20,4) default 0   ,
  opening_credit NUMERIC(20,4) default 0   ,
  period_debit NUMERIC(20,4) default 0   ,
  period_credit NUMERIC(20,4) default 0   ,
  closing_debit NUMERIC(20,4) default 0   ,
  closing_credit NUMERIC(20,4) default 0   ,
  generated_at TIMESTAMP NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_trial_balance primary key (id)
);

CREATE TABLE erp_fin_gl_balance(
  id INT8 NOT NULL ,
  org_id INT8 NOT NULL ,
  acct_schema_id INT8 NOT NULL ,
  period_id INT8 NOT NULL ,
  subject_id INT8 NOT NULL ,
  currency_id INT8 NOT NULL ,
  opening_debit NUMERIC(20,4) default 0   ,
  opening_credit NUMERIC(20,4) default 0   ,
  period_debit NUMERIC(20,4) default 0   ,
  period_credit NUMERIC(20,4) default 0   ,
  closing_debit NUMERIC(20,4) default 0   ,
  closing_credit NUMERIC(20,4) default 0   ,
  year_opening_debit NUMERIC(20,4) default 0   ,
  year_opening_credit NUMERIC(20,4) default 0   ,
  partner_id INT8  ,
  department_id INT8  ,
  project_id INT8  ,
  warehouse_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_gl_balance primary key (id)
);

CREATE TABLE erp_fin_fund_account(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  account_type INT4 NOT NULL ,
  subject_id INT8  ,
  bank_name VARCHAR(200)  ,
  bank_account VARCHAR(50)  ,
  currency_id INT8 NOT NULL ,
  opening_balance NUMERIC(20,4) default 0   ,
  current_balance NUMERIC(20,4) default 0   ,
  status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_fund_account primary key (id)
);

CREATE TABLE erp_fin_ar_ap_item(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8 NOT NULL ,
  acct_schema_id INT8 NOT NULL ,
  direction INT4 NOT NULL ,
  partner_id INT8 NOT NULL ,
  source_bill_type VARCHAR(50) NOT NULL ,
  source_bill_code VARCHAR(50) NOT NULL ,
  business_date DATE NOT NULL ,
  due_date DATE  ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  amount_source NUMERIC(20,4) NOT NULL ,
  amount_functional NUMERIC(20,4) NOT NULL ,
  settled_amount_source NUMERIC(20,4) default 0   ,
  settled_amount_functional NUMERIC(20,4) default 0   ,
  open_amount_source NUMERIC(20,4) NOT NULL ,
  open_amount_functional NUMERIC(20,4) NOT NULL ,
  status INT4 NOT NULL ,
  period_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_ar_ap_item primary key (id)
);

CREATE TABLE erp_fin_reconciliation(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8 NOT NULL ,
  acct_schema_id INT8 NOT NULL ,
  direction INT4 NOT NULL ,
  partner_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  total_amount_source NUMERIC(20,4) default 0   ,
  total_amount_functional NUMERIC(20,4) default 0   ,
  fx_gain_loss NUMERIC(20,4) default 0   ,
  doc_status INT4 NOT NULL ,
  posted_by INT8  ,
  posted_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_reconciliation primary key (id)
);

CREATE TABLE erp_fin_voucher_line(
  id INT8 NOT NULL ,
  voucher_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  subject_id INT8 NOT NULL ,
  subject_code VARCHAR(50) NOT NULL ,
  subject_name VARCHAR(200)  ,
  dc_direction INT4 NOT NULL ,
  debit_amount NUMERIC(20,4) default 0   ,
  credit_amount NUMERIC(20,4) default 0   ,
  currency_id INT8 NOT NULL ,
  exchange_rate NUMERIC(20,8) default 1   ,
  amount_source NUMERIC(20,4)  ,
  amount_functional NUMERIC(20,4)  ,
  acct_schema_id INT8 NOT NULL ,
  org_id INT8  ,
  memo VARCHAR(1000)  ,
  partner_id INT8  ,
  department_id INT8  ,
  project_id INT8  ,
  warehouse_id INT8  ,
  material_id INT8  ,
  business_type INT4  ,
  cost_center_id INT8  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher_line primary key (id)
);

CREATE TABLE erp_fin_voucher_bill_r(
  id INT8 NOT NULL ,
  voucher_id INT8 NOT NULL ,
  bill_type VARCHAR(50) NOT NULL ,
  bill_code VARCHAR(50) NOT NULL ,
  bill_line_code VARCHAR(50)  ,
  business_type INT4  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher_bill_r primary key (id)
);

CREATE TABLE erp_fin_voucher_template_line(
  id INT8 NOT NULL ,
  template_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  subject_code VARCHAR(50) NOT NULL ,
  dc_direction INT4 NOT NULL ,
  amount_expression VARCHAR(200)  ,
  account_key VARCHAR(50)  ,
  amount_key VARCHAR(50)  ,
  memo_template VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_voucher_template_line primary key (id)
);

CREATE TABLE erp_fin_bank_statement(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8 NOT NULL ,
  fund_account_id INT8 NOT NULL ,
  statement_date DATE NOT NULL ,
  beginning_balance NUMERIC(20,4) default 0   ,
  ending_balance NUMERIC(20,4) default 0   ,
  total_debit NUMERIC(20,4) default 0   ,
  total_credit NUMERIC(20,4) default 0   ,
  import_time TIMESTAMP  ,
  doc_status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_bank_statement primary key (id)
);

CREATE TABLE erp_fin_reconciliation_line(
  id INT8 NOT NULL ,
  reconciliation_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  payment_item_id INT8 NOT NULL ,
  invoice_item_id INT8 NOT NULL ,
  settled_amount_source NUMERIC(20,4) NOT NULL ,
  settled_amount_functional NUMERIC(20,4) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_reconciliation_line primary key (id)
);

CREATE TABLE erp_fin_bank_statement_line(
  id INT8 NOT NULL ,
  statement_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  transaction_date DATE NOT NULL ,
  description VARCHAR(1000)  ,
  ref_no VARCHAR(50)  ,
  dc_direction INT4 NOT NULL ,
  amount NUMERIC(20,4) NOT NULL ,
  currency_id INT8 NOT NULL ,
  balance_after NUMERIC(20,4)  ,
  match_status INT4 NOT NULL ,
  matched_line_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_bank_statement_line primary key (id)
);

CREATE TABLE erp_fin_bank_reconciliation(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8 NOT NULL ,
  fund_account_id INT8 NOT NULL ,
  statement_id INT8  ,
  reconciliation_date DATE NOT NULL ,
  book_balance NUMERIC(20,4) NOT NULL ,
  statement_balance NUMERIC(20,4) NOT NULL ,
  unreconciled_diff NUMERIC(20,4) default 0   ,
  is_balanced BOOLEAN default false   ,
  reconciled_at TIMESTAMP  ,
  reconciled_by INT8  ,
  doc_status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_bank_reconciliation primary key (id)
);

CREATE TABLE erp_fin_bank_reconciliation_line(
  id INT8 NOT NULL ,
  reconciliation_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  adjustment_type VARCHAR(50) NOT NULL ,
  description VARCHAR(1000)  ,
  dc_direction INT4 NOT NULL ,
  amount NUMERIC(20,4) NOT NULL ,
  side VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_fin_bank_reconciliation_line primary key (id)
);


      COMMENT ON TABLE erp_fin_accounting_period IS '会计期间';
                
      COMMENT ON COLUMN erp_fin_accounting_period.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.code IS '期间编码';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.name IS '期间名称';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.org_id IS '组织';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.year IS '年度';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.month IS '月份';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.start_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.end_date IS '结束日期';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.quarter IS '季度';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.is_adjustment IS '是否调整期';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.status IS '期间状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.closed_by IS '结账人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.closed_at IS '结账时间';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_acct_schema IS '会计核算表(账套)';
                
      COMMENT ON TABLE erp_md_subject IS '会计科目';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_prj_project IS '项目';
                
      COMMENT ON TABLE erp_md_cost_center IS '成本中心';
                
      COMMENT ON TABLE erp_ast_asset IS '固定资产';
                
      COMMENT ON TABLE erp_fin_voucher IS '会计凭证';
                
      COMMENT ON COLUMN erp_fin_voucher.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher.code IS '凭证号';
                    
      COMMENT ON COLUMN erp_fin_voucher.voucher_type IS '凭证字';
                    
      COMMENT ON COLUMN erp_fin_voucher.posting_type IS '过账类型';
                    
      COMMENT ON COLUMN erp_fin_voucher.voucher_date IS '凭证日期';
                    
      COMMENT ON COLUMN erp_fin_voucher.voucher_no IS '凭证编号';
                    
      COMMENT ON COLUMN erp_fin_voucher.org_id IS '核算组织';
                    
      COMMENT ON COLUMN erp_fin_voucher.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_fin_voucher.period_id IS '会计期间';
                    
      COMMENT ON COLUMN erp_fin_voucher.total_debit IS '借方合计(本位币)';
                    
      COMMENT ON COLUMN erp_fin_voucher.total_credit IS '贷方合计(本位币)';
                    
      COMMENT ON COLUMN erp_fin_voucher.is_reversed IS '是否红字冲销凭证';
                    
      COMMENT ON COLUMN erp_fin_voucher.reversal_of_voucher_id IS '原冲销凭证ID';
                    
      COMMENT ON COLUMN erp_fin_voucher.doc_status IS '凭证状态';
                    
      COMMENT ON COLUMN erp_fin_voucher.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_fin_voucher.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_fin_voucher.remark IS '备注';
                    
      COMMENT ON COLUMN erp_fin_voucher.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_voucher_template IS '凭证模板';
                
      COMMENT ON COLUMN erp_fin_voucher_template.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.code IS '模板编码';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.name IS '模板名称';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.acct_schema_id IS '账套(空表示所有账套通用)';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.business_type IS '业务类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.voucher_type IS '凭证字';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.template_type IS '模板类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.valid_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.valid_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher_template.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_accounting_period_status IS '期间结账状态';
                
      COMMENT ON COLUMN erp_fin_accounting_period_status.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.period_id IS '期间ID';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.total_vouchers IS '凭证总数';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.posted_vouchers IS '已过账凭证数';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.unposted_vouchers IS '未过账凭证数';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.ar_status IS '应收模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.ap_status IS '应付模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.inv_status IS '存货模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.gl_status IS '总账模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.asset_status IS '资产模块状态';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_accounting_period_status.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_trial_balance IS '试算平衡表';
                
      COMMENT ON COLUMN erp_fin_trial_balance.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.org_id IS '组织';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.period_id IS '期间';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.subject_id IS '科目';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.subject_code IS '科目编码';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.subject_name IS '科目名称';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.opening_debit IS '期初借方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.opening_credit IS '期初贷方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.period_debit IS '本期借方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.period_credit IS '本期贷方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.closing_debit IS '期末借方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.closing_credit IS '期末贷方';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.generated_at IS '生成时间';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_trial_balance.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_gl_balance IS '总账余额';
                
      COMMENT ON COLUMN erp_fin_gl_balance.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.org_id IS '组织';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.period_id IS '期间';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.subject_id IS '科目';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.opening_debit IS '期初借方(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.opening_credit IS '期初贷方(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.period_debit IS '本期借方发生(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.period_credit IS '本期贷方发生(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.closing_debit IS '期末借方(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.closing_credit IS '期末贷方(本位币)';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.year_opening_debit IS '年初借方';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.year_opening_credit IS '年初贷方';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.partner_id IS '辅助-往来单位';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.department_id IS '辅助-部门';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.project_id IS '辅助-项目';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.warehouse_id IS '辅助-仓库';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_gl_balance.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_fund_account IS '资金账户';
                
      COMMENT ON COLUMN erp_fin_fund_account.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_fund_account.code IS '账户编码';
                    
      COMMENT ON COLUMN erp_fin_fund_account.name IS '账户名称';
                    
      COMMENT ON COLUMN erp_fin_fund_account.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_fin_fund_account.account_type IS '账户类型';
                    
      COMMENT ON COLUMN erp_fin_fund_account.subject_id IS '对应会计科目';
                    
      COMMENT ON COLUMN erp_fin_fund_account.bank_name IS '开户银行';
                    
      COMMENT ON COLUMN erp_fin_fund_account.bank_account IS '银行账号';
                    
      COMMENT ON COLUMN erp_fin_fund_account.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_fin_fund_account.opening_balance IS '期初余额';
                    
      COMMENT ON COLUMN erp_fin_fund_account.current_balance IS '当前余额';
                    
      COMMENT ON COLUMN erp_fin_fund_account.status IS '状态';
                    
      COMMENT ON COLUMN erp_fin_fund_account.remark IS '备注';
                    
      COMMENT ON COLUMN erp_fin_fund_account.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_fund_account.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_fund_account.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_fund_account.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_fund_account.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_fund_account.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_ar_ap_item IS '应收应付明细账';
                
      COMMENT ON COLUMN erp_fin_ar_ap_item.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.code IS '单号';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.org_id IS '组织';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.direction IS '应收应付';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.partner_id IS '往来单位';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.source_bill_type IS '来源单据类型(AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT)';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.source_bill_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.due_date IS '到期日';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.amount_functional IS '本位币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.settled_amount_source IS '已核销源币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.settled_amount_functional IS '已核销本位币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.open_amount_source IS '未核销源币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.open_amount_functional IS '未核销本位币金额';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.status IS '状态';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.period_id IS '所属期间';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.remark IS '备注';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_ar_ap_item.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_reconciliation IS '核销单';
                
      COMMENT ON COLUMN erp_fin_reconciliation.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.code IS '单号';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.org_id IS '组织';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.direction IS '应收应付';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.partner_id IS '往来单位';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.total_amount_source IS '核销总额(源币)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.total_amount_functional IS '核销总额(本位币)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.fx_gain_loss IS '汇兑损益';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.doc_status IS '状态';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.remark IS '备注';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_voucher_line IS '凭证分录行';
                
      COMMENT ON COLUMN erp_fin_voucher_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.voucher_id IS '凭证ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.subject_id IS '科目ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.subject_code IS '科目编码';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.subject_name IS '科目名称';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.dc_direction IS '借贷方向';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.debit_amount IS '借方金额(本位币)';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.credit_amount IS '贷方金额(本位币)';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.exchange_rate IS '汇率';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.amount_source IS '源币种金额';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.amount_functional IS '本位币金额';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.acct_schema_id IS '账套';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.org_id IS '核算组织';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.memo IS '摘要';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.partner_id IS '辅助-往来单位';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.department_id IS '辅助-部门';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.project_id IS '辅助-项目';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.warehouse_id IS '辅助-仓库';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.material_id IS '辅助-物料';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.business_type IS '业务类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.cost_center_id IS '成本中心';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_voucher_bill_r IS '业财回链';
                
      COMMENT ON COLUMN erp_fin_voucher_bill_r.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.voucher_id IS '凭证ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.bill_type IS '单据类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.bill_code IS '单据编号';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.bill_line_code IS '单据行号';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.business_type IS '业务类型';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher_bill_r.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_voucher_template_line IS '凭证模板行';
                
      COMMENT ON COLUMN erp_fin_voucher_template_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.template_id IS '模板ID';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.subject_code IS '科目编码(可含占位符)';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.dc_direction IS '借贷方向';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.amount_expression IS '金额表达式';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.account_key IS '科目映射键';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.amount_key IS '金额占位键';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.memo_template IS '摘要模板';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_voucher_template_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_bank_statement IS '银行对账单';
                
      COMMENT ON COLUMN erp_fin_bank_statement.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.code IS '单号';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.org_id IS '组织';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.fund_account_id IS '资金账户';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.statement_date IS '对账日期';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.beginning_balance IS '期初余额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.ending_balance IS '期末余额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.total_debit IS '借方发生额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.total_credit IS '贷方发生额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.import_time IS '导入时间';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.doc_status IS '状态';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.remark IS '备注';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_bank_statement.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_reconciliation_line IS '核销单行';
                
      COMMENT ON COLUMN erp_fin_reconciliation_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.reconciliation_id IS '核销单ID';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.payment_item_id IS '付款/收款项(invoice or payment item)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.invoice_item_id IS '被核销发票项';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.settled_amount_source IS '核销金额(源币)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.settled_amount_functional IS '核销金额(本位币)';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_reconciliation_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_bank_statement_line IS '银行对账单行';
                
      COMMENT ON COLUMN erp_fin_bank_statement_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.statement_id IS '对账单ID';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.transaction_date IS '交易日期';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.description IS '摘要(银行)';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.ref_no IS '银行参考号';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.dc_direction IS '借贷方向';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.amount IS '金额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.balance_after IS '交易后余额';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.match_status IS '匹配状态';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.matched_line_id IS '匹配凭证行ID';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_bank_statement_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_bank_reconciliation IS '银行对账(余额调节表)';
                
      COMMENT ON COLUMN erp_fin_bank_reconciliation.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.code IS '单号';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.org_id IS '组织';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.fund_account_id IS '资金账户';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.statement_id IS '银行对账单';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.reconciliation_date IS '对账日期';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.book_balance IS '账面余额';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.statement_balance IS '对账单余额';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.unreconciled_diff IS '未达差异';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.is_balanced IS '是否平衡';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.reconciled_at IS '对账完成时间';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.reconciled_by IS '对账人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.doc_status IS '状态';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.remark IS '备注';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_fin_bank_reconciliation_line IS '银行对账调整行';
                
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.reconciliation_id IS '对账记录ID';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.adjustment_type IS '调整类型';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.description IS '说明';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.dc_direction IS '借贷方向';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.amount IS '金额';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.side IS '调整方';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_fin_bank_reconciliation_line.update_time IS '修改时间';
                    
