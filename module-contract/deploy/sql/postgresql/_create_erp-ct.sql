
CREATE TABLE erp_md_md_partner(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_partner primary key (id)
);

CREATE TABLE erp_ct_template(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  contract_type INT4 NOT NULL ,
  content_template VARCHAR(4000)  ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_template primary key (id)
);

CREATE TABLE erp_md_md_organization(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_organization primary key (id)
);

CREATE TABLE erp_ct_contract(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  contract_name VARCHAR(200) NOT NULL ,
  contract_type INT4 NOT NULL ,
  contract_direction INT4 NOT NULL ,
  partner_id INT8 NOT NULL ,
  currency_id INT8  ,
  total_amount NUMERIC(20,4)  ,
  start_date DATE NOT NULL ,
  end_date DATE NOT NULL ,
  sign_date DATE  ,
  status INT4 NOT NULL ,
  template_id INT8  ,
  parent_contract_id INT8  ,
  description VARCHAR(4000)  ,
  attachment_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_contract primary key (id)
);

CREATE TABLE erp_ct_contract_line(
  id INT8 NOT NULL ,
  contract_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8  ,
  description VARCHAR(500)  ,
  quantity NUMERIC(20,4)  ,
  unit_price NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_contract_line primary key (id)
);

CREATE TABLE erp_ct_contract_version(
  id INT8 NOT NULL ,
  contract_id INT8 NOT NULL ,
  version_no INT4 NOT NULL ,
  version_date DATE NOT NULL ,
  content VARCHAR(4000)  ,
  attachment_id INT8  ,
  is_current BOOLEAN default false   ,
  status INT4 NOT NULL ,
  approved_by INT8  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_contract_version primary key (id)
);

CREATE TABLE erp_ct_invoice_plan(
  id INT8 NOT NULL ,
  contract_line_id INT8 NOT NULL ,
  plan_date DATE  ,
  amount NUMERIC(20,4)  ,
  is_invoiced BOOLEAN default false   ,
  invoice_bill_code VARCHAR(50)  ,
  invoice_date DATE  ,
  invoice_term INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_invoice_plan primary key (id)
);

CREATE TABLE erp_ct_consumption_line(
  id INT8 NOT NULL ,
  contract_line_id INT8 NOT NULL ,
  consumption_date DATE NOT NULL ,
  quantity NUMERIC(20,4)  ,
  unit_price NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  source_bill_type VARCHAR(50)  ,
  source_bill_code VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_consumption_line primary key (id)
);


      COMMENT ON TABLE erp_md_md_partner IS 'ErpMdPartner';
                
      COMMENT ON TABLE erp_ct_template IS '合同模板';
                
      COMMENT ON COLUMN erp_ct_template.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_template.code IS '编码';
                    
      COMMENT ON COLUMN erp_ct_template.name IS '名称';
                    
      COMMENT ON COLUMN erp_ct_template.contract_type IS '适用合同类型';
                    
      COMMENT ON COLUMN erp_ct_template.content_template IS '模板内容';
                    
      COMMENT ON COLUMN erp_ct_template.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_ct_template.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_template.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_template.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_template.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_template.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_template.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_template.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_ct_contract IS '合同';
                
      COMMENT ON COLUMN erp_ct_contract.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_contract.code IS '合同编号';
                    
      COMMENT ON COLUMN erp_ct_contract.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_contract.contract_name IS '合同名称';
                    
      COMMENT ON COLUMN erp_ct_contract.contract_type IS '合同类型';
                    
      COMMENT ON COLUMN erp_ct_contract.contract_direction IS '合同方向';
                    
      COMMENT ON COLUMN erp_ct_contract.partner_id IS '合作方';
                    
      COMMENT ON COLUMN erp_ct_contract.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_ct_contract.total_amount IS '合同总额';
                    
      COMMENT ON COLUMN erp_ct_contract.start_date IS '生效日期';
                    
      COMMENT ON COLUMN erp_ct_contract.end_date IS '到期日期';
                    
      COMMENT ON COLUMN erp_ct_contract.sign_date IS '签署日期';
                    
      COMMENT ON COLUMN erp_ct_contract.status IS '合同状态';
                    
      COMMENT ON COLUMN erp_ct_contract.template_id IS '合同模板';
                    
      COMMENT ON COLUMN erp_ct_contract.parent_contract_id IS '父合同';
                    
      COMMENT ON COLUMN erp_ct_contract.description IS '描述';
                    
      COMMENT ON COLUMN erp_ct_contract.attachment_id IS '合同附件';
                    
      COMMENT ON COLUMN erp_ct_contract.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_contract.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_contract.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_contract.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_contract.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_contract.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_contract.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_contract_line IS '合同行';
                
      COMMENT ON COLUMN erp_ct_contract_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_contract_line.contract_id IS '合同ID';
                    
      COMMENT ON COLUMN erp_ct_contract_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_ct_contract_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_ct_contract_line.description IS '行描述';
                    
      COMMENT ON COLUMN erp_ct_contract_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_ct_contract_line.unit_price IS '单价';
                    
      COMMENT ON COLUMN erp_ct_contract_line.amount IS '金额';
                    
      COMMENT ON COLUMN erp_ct_contract_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_contract_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_contract_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_contract_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_contract_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_contract_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_contract_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_contract_version IS '合同版本';
                
      COMMENT ON COLUMN erp_ct_contract_version.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_contract_version.contract_id IS '合同ID';
                    
      COMMENT ON COLUMN erp_ct_contract_version.version_no IS '版本号';
                    
      COMMENT ON COLUMN erp_ct_contract_version.version_date IS '版本日期';
                    
      COMMENT ON COLUMN erp_ct_contract_version.content IS '版本内容';
                    
      COMMENT ON COLUMN erp_ct_contract_version.attachment_id IS '版本附件';
                    
      COMMENT ON COLUMN erp_ct_contract_version.is_current IS '是否当前版本';
                    
      COMMENT ON COLUMN erp_ct_contract_version.status IS '版本状态';
                    
      COMMENT ON COLUMN erp_ct_contract_version.approved_by IS '批准人';
                    
      COMMENT ON COLUMN erp_ct_contract_version.approved_at IS '批准时间';
                    
      COMMENT ON COLUMN erp_ct_contract_version.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_contract_version.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_contract_version.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_contract_version.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_contract_version.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_contract_version.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_contract_version.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_invoice_plan IS '开票计划';
                
      COMMENT ON COLUMN erp_ct_invoice_plan.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.contract_line_id IS '合同行ID';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.plan_date IS '计划开票日期';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.amount IS '开票金额';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.is_invoiced IS '是否已开票';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.invoice_bill_code IS '关联发票号';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.invoice_date IS '实际开票日期';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.invoice_term IS '开票条款';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_consumption_line IS '消耗计费行';
                
      COMMENT ON COLUMN erp_ct_consumption_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.contract_line_id IS '合同行ID';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.consumption_date IS '消耗日期';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.quantity IS '消耗数量';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.unit_price IS '单价';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.amount IS '金额';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.source_bill_type IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.source_bill_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.update_time IS '修改时间';
                    
