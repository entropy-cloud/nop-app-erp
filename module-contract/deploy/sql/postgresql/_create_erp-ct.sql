
CREATE TABLE erp_md_md_partner(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_partner primary key (id)
);

CREATE TABLE erp_ct_template(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  contract_type VARCHAR(20) NOT NULL ,
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

CREATE TABLE erp_md_currency(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_currency primary key (id)
);

CREATE TABLE erp_md_material(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_material primary key (id)
);

CREATE TABLE erp_ct_approval_matrix(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  min_amount NUMERIC(18,2)  ,
  max_amount NUMERIC(18,2)  ,
  approver_role VARCHAR(50) NOT NULL ,
  approval_order INT4 NOT NULL ,
  contract_type VARCHAR(50)  ,
  allow_skip BOOLEAN default false   ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_approval_matrix primary key (id)
);

CREATE TABLE erp_ct_contract(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  contract_name VARCHAR(200) NOT NULL ,
  contract_type VARCHAR(20) NOT NULL ,
  contract_direction VARCHAR(20) NOT NULL ,
  partner_id INT8 NOT NULL ,
  currency_id INT8  ,
  total_amount NUMERIC(20,4)  ,
  start_date DATE NOT NULL ,
  end_date DATE NOT NULL ,
  sign_date DATE  ,
  status VARCHAR(20) NOT NULL ,
  template_id INT8  ,
  parent_contract_id INT8  ,
  description VARCHAR(4000)  ,
  attachment_file_id VARCHAR(200)  ,
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
  attachment_file_id VARCHAR(200)  ,
  is_current BOOLEAN default false   ,
  status VARCHAR(20) NOT NULL ,
  approved_by VARCHAR(36)  ,
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

CREATE TABLE erp_ct_approval_record(
  id INT8 NOT NULL ,
  contract_id INT8 NOT NULL ,
  org_id INT8  ,
  approval_matrix_id INT8  ,
  approval_order INT4  ,
  approver_id VARCHAR(36)  ,
  approval_status VARCHAR(20) NOT NULL ,
  comment VARCHAR(1000)  ,
  approved_at TIMESTAMP  ,
  rejected_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_approval_record primary key (id)
);

CREATE TABLE erp_ct_rebate_agreement(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  contract_id INT8  ,
  partner_id INT8 NOT NULL ,
  rebate_type VARCHAR(20) NOT NULL ,
  agreement_date DATE  ,
  start_date DATE NOT NULL ,
  end_date DATE NOT NULL ,
  accrual_method VARCHAR(20) NOT NULL ,
  status VARCHAR(20) NOT NULL ,
  total_accumulated_amount NUMERIC(18,2)  ,
  estimated_rebate_amount NUMERIC(18,2)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_rebate_agreement primary key (id)
);

CREATE TABLE erp_ct_document(
  id INT8 NOT NULL ,
  org_id INT8  ,
  contract_id INT8  ,
  code VARCHAR(50) NOT NULL ,
  doc_name VARCHAR(200) NOT NULL ,
  doc_type VARCHAR(50) NOT NULL ,
  attachment_file_id VARCHAR(200)  ,
  file_size INT8  ,
  file_hash VARCHAR(100)  ,
  mime_type VARCHAR(100)  ,
  ocr_text VARCHAR(4000)  ,
  ocr_status VARCHAR(50)  ,
  full_text_search VARCHAR(4000)  ,
  metadata_tags VARCHAR(2000)  ,
  retention_date DATE  ,
  archive_date DATE  ,
  purge_date DATE  ,
  is_archived BOOLEAN default false   ,
  version_no INT4 default 1   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_document primary key (id)
);

CREATE TABLE erp_ct_invoice_plan(
  id INT8 NOT NULL ,
  contract_line_id INT8 NOT NULL ,
  plan_date DATE  ,
  amount NUMERIC(20,4)  ,
  is_invoiced BOOLEAN default false   ,
  invoice_bill_code VARCHAR(50)  ,
  invoice_date DATE  ,
  invoice_term VARCHAR(20) NOT NULL ,
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

CREATE TABLE erp_ct_volume_discount(
  id INT8 NOT NULL ,
  contract_line_id INT8 NOT NULL ,
  org_id INT8  ,
  from_qty NUMERIC(20,4) NOT NULL ,
  to_qty NUMERIC(20,4)  ,
  discount_percent NUMERIC(10,2) NOT NULL ,
  unit_price NUMERIC(20,4)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_volume_discount primary key (id)
);

CREATE TABLE erp_ct_signature_request(
  id INT8 NOT NULL ,
  org_id INT8  ,
  contract_version_id INT8 NOT NULL ,
  provider VARCHAR(50) NOT NULL ,
  provider_request_id VARCHAR(200)  ,
  status VARCHAR(50) NOT NULL ,
  signers VARCHAR(2000)  ,
  signing_deadline DATE  ,
  completed_at TIMESTAMP  ,
  certificate_url VARCHAR(1000)  ,
  evidence_no VARCHAR(200)  ,
  attachment_file_id VARCHAR(200)  ,
  error_msg VARCHAR(1000)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_signature_request primary key (id)
);

CREATE TABLE erp_ct_rebate_tier(
  id INT8 NOT NULL ,
  rebate_agreement_id INT8 NOT NULL ,
  from_amount NUMERIC(18,2) NOT NULL ,
  to_amount NUMERIC(18,2)  ,
  rebate_percent NUMERIC(10,2)  ,
  rebate_amount NUMERIC(18,2)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_rebate_tier primary key (id)
);

CREATE TABLE erp_ct_rebate_accrual(
  id INT8 NOT NULL ,
  rebate_agreement_id INT8 NOT NULL ,
  org_id INT8  ,
  source_bill_type VARCHAR(50)  ,
  source_bill_code VARCHAR(50)  ,
  bill_amount_source NUMERIC(18,2)  ,
  accrued_rebate NUMERIC(18,2)  ,
  accrual_date DATE  ,
  is_settled BOOLEAN default false   ,
  settled_date DATE  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_rebate_accrual primary key (id)
);

CREATE TABLE erp_ct_rebate_settlement(
  id INT8 NOT NULL ,
  rebate_agreement_id INT8 NOT NULL ,
  org_id INT8  ,
  settlement_date DATE NOT NULL ,
  total_rebate_amount NUMERIC(18,2)  ,
  credit_memo_bill_type VARCHAR(50)  ,
  credit_memo_bill_code VARCHAR(50)  ,
  status VARCHAR(20) NOT NULL ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_rebate_settlement primary key (id)
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
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_ct_approval_matrix IS '审批矩阵';
                
      COMMENT ON COLUMN erp_ct_approval_matrix.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.code IS '编码';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.min_amount IS '最小金额';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.max_amount IS '最大金额';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.approver_role IS '审批角色';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.approval_order IS '审批顺序';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.contract_type IS '适用合同类型';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.allow_skip IS '可跳过';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.is_active IS '启用';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.update_time IS '修改时间';
                    
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
                    
      COMMENT ON COLUMN erp_ct_contract.attachment_file_id IS '合同附件';
                    
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
                    
      COMMENT ON COLUMN erp_ct_contract_version.attachment_file_id IS '版本附件';
                    
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
                    
      COMMENT ON TABLE erp_ct_approval_record IS '审批记录';
                
      COMMENT ON COLUMN erp_ct_approval_record.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_approval_record.contract_id IS '合同';
                    
      COMMENT ON COLUMN erp_ct_approval_record.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_approval_record.approval_matrix_id IS '审批矩阵';
                    
      COMMENT ON COLUMN erp_ct_approval_record.approval_order IS '顺序号';
                    
      COMMENT ON COLUMN erp_ct_approval_record.approver_id IS '审批人';
                    
      COMMENT ON COLUMN erp_ct_approval_record.approval_status IS '审批状态';
                    
      COMMENT ON COLUMN erp_ct_approval_record.comment IS '审批意见';
                    
      COMMENT ON COLUMN erp_ct_approval_record.approved_at IS '通过时间';
                    
      COMMENT ON COLUMN erp_ct_approval_record.rejected_at IS '驳回时间';
                    
      COMMENT ON COLUMN erp_ct_approval_record.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_approval_record.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_approval_record.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_approval_record.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_approval_record.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_approval_record.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_approval_record.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_rebate_agreement IS '返利协议';
                
      COMMENT ON COLUMN erp_ct_rebate_agreement.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.code IS '协议编号';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.contract_id IS '关联合同';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.partner_id IS '对方伙伴';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.rebate_type IS '返利类型';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.agreement_date IS '签订日期';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.start_date IS '有效期开始';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.end_date IS '有效期结束';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.accrual_method IS '计提方法';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.status IS '状态';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.total_accumulated_amount IS '当前累计金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.estimated_rebate_amount IS '预估返利金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_document IS '合同文档';
                
      COMMENT ON COLUMN erp_ct_document.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_document.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_document.contract_id IS '关联合同';
                    
      COMMENT ON COLUMN erp_ct_document.code IS '文档编码';
                    
      COMMENT ON COLUMN erp_ct_document.doc_name IS '文档名称';
                    
      COMMENT ON COLUMN erp_ct_document.doc_type IS '文档类型';
                    
      COMMENT ON COLUMN erp_ct_document.attachment_file_id IS '附件';
                    
      COMMENT ON COLUMN erp_ct_document.file_size IS '文件大小(字节)';
                    
      COMMENT ON COLUMN erp_ct_document.file_hash IS '文件哈希';
                    
      COMMENT ON COLUMN erp_ct_document.mime_type IS 'MIME类型';
                    
      COMMENT ON COLUMN erp_ct_document.ocr_text IS 'OCR文本';
                    
      COMMENT ON COLUMN erp_ct_document.ocr_status IS 'OCR状态';
                    
      COMMENT ON COLUMN erp_ct_document.full_text_search IS '全文检索';
                    
      COMMENT ON COLUMN erp_ct_document.metadata_tags IS '元数据标签(JSON)';
                    
      COMMENT ON COLUMN erp_ct_document.retention_date IS '保留截止日期';
                    
      COMMENT ON COLUMN erp_ct_document.archive_date IS '归档日期';
                    
      COMMENT ON COLUMN erp_ct_document.purge_date IS '销毁日期';
                    
      COMMENT ON COLUMN erp_ct_document.is_archived IS '已归档';
                    
      COMMENT ON COLUMN erp_ct_document.version_no IS '文档版本';
                    
      COMMENT ON COLUMN erp_ct_document.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_document.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_document.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_document.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_document.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_document.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_document.update_time IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_ct_volume_discount IS '批量折扣';
                
      COMMENT ON COLUMN erp_ct_volume_discount.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.contract_line_id IS '合同行';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.from_qty IS '起始数量';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.to_qty IS '截止数量';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.discount_percent IS '折扣百分比';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.unit_price IS '覆盖单价';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_signature_request IS '签章请求';
                
      COMMENT ON COLUMN erp_ct_signature_request.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_signature_request.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_signature_request.contract_version_id IS '合同版本';
                    
      COMMENT ON COLUMN erp_ct_signature_request.provider IS '签名提供商';
                    
      COMMENT ON COLUMN erp_ct_signature_request.provider_request_id IS '提供商请求ID';
                    
      COMMENT ON COLUMN erp_ct_signature_request.status IS '签章状态';
                    
      COMMENT ON COLUMN erp_ct_signature_request.signers IS '签署人(JSON)';
                    
      COMMENT ON COLUMN erp_ct_signature_request.signing_deadline IS '签署截止日期';
                    
      COMMENT ON COLUMN erp_ct_signature_request.completed_at IS '签署完成时间';
                    
      COMMENT ON COLUMN erp_ct_signature_request.certificate_url IS '完成证书URL';
                    
      COMMENT ON COLUMN erp_ct_signature_request.evidence_no IS '存证编号';
                    
      COMMENT ON COLUMN erp_ct_signature_request.attachment_file_id IS '已签署文件';
                    
      COMMENT ON COLUMN erp_ct_signature_request.error_msg IS '错误信息';
                    
      COMMENT ON COLUMN erp_ct_signature_request.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_signature_request.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_signature_request.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_signature_request.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_signature_request.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_signature_request.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_signature_request.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_rebate_tier IS '返利阶梯';
                
      COMMENT ON COLUMN erp_ct_rebate_tier.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.rebate_agreement_id IS '返利协议';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.from_amount IS '起始金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.to_amount IS '截止金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.rebate_percent IS '返利比例';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.rebate_amount IS '固定返利金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_rebate_accrual IS '返利计提明细';
                
      COMMENT ON COLUMN erp_ct_rebate_accrual.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.rebate_agreement_id IS '返利协议';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.source_bill_type IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.source_bill_code IS '来源单据号';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.bill_amount_source IS '单据金额(源币种)';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.accrued_rebate IS '计提返利金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.accrual_date IS '计提日期';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.is_settled IS '已结算';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.settled_date IS '结算日期';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_rebate_settlement IS '返利结算单';
                
      COMMENT ON COLUMN erp_ct_rebate_settlement.id IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.rebate_agreement_id IS '返利协议';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.settlement_date IS '结算日期';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.total_rebate_amount IS '结算返利总额';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.credit_memo_bill_type IS '信用单类型';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.credit_memo_bill_code IS '信用单号';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.status IS '状态';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.remark IS '备注';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.update_time IS '修改时间';
                    
