
CREATE TABLE erp_md_material(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  material_type INT4  ,
  status INT4  ,
  constraint PK_erp_md_material primary key (id)
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

CREATE TABLE erp_md_warehouse(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  warehouse_type INT4  ,
  org_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_warehouse primary key (id)
);

CREATE TABLE erp_md_employee(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  org_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_employee primary key (id)
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

CREATE TABLE erp_qa_risk_register(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  risk_date DATE NOT NULL ,
  description VARCHAR(2000)  ,
  category VARCHAR(100)  ,
  likelihood INT4 NOT NULL ,
  severity INT4 NOT NULL ,
  risk_score INT4  ,
  mitigation VARCHAR(2000)  ,
  owner_id VARCHAR(36)  ,
  status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_risk_register primary key (id)
);

CREATE TABLE erp_qa_sampling_plan(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  aql_level VARCHAR(20) NOT NULL ,
  lot_size_from NUMERIC(20,4)  ,
  lot_size_to NUMERIC(20,4)  ,
  sample_size NUMERIC(20,4) NOT NULL ,
  accept_number INT4 NOT NULL ,
  reject_number INT4 NOT NULL ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_sampling_plan primary key (id)
);

CREATE TABLE erp_qa_inspection_template(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  inspection_type VARCHAR(20) NOT NULL ,
  material_id INT8  ,
  is_active INT4 default 1  NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_inspection_template primary key (id)
);

CREATE TABLE erp_qa_quality_goal(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  description VARCHAR(2000)  ,
  target_value NUMERIC(20,4)  ,
  current_value NUMERIC(20,4)  ,
  unit VARCHAR(50)  ,
  responsible_person_id INT8  ,
  start_date DATE  ,
  end_date DATE  ,
  status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_quality_goal primary key (id)
);

CREATE TABLE erp_qa_review(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  review_date DATE NOT NULL ,
  review_type VARCHAR(20) NOT NULL ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  participants VARCHAR(500)  ,
  conclusion VARCHAR(2000)  ,
  action_required INT4 default 0   ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  constraint PK_erp_qa_review primary key (id)
);

CREATE TABLE erp_qa_calibration(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  instrument_name VARCHAR(200) NOT NULL ,
  instrument_code VARCHAR(100)  ,
  business_date DATE NOT NULL ,
  standard_ref VARCHAR(200)  ,
  measured_value VARCHAR(100)  ,
  target_value VARCHAR(20)  ,
  tolerance VARCHAR(20)  ,
  result VARCHAR(20) NOT NULL ,
  next_calibration_date DATE  ,
  calibrated_by INT8  ,
  doc_status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  constraint PK_erp_qa_calibration primary key (id)
);

CREATE TABLE erp_qa_inspection(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  inspection_type VARCHAR(20) NOT NULL ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  related_line_code VARCHAR(50)  ,
  material_id INT8 NOT NULL ,
  template_id INT8  ,
  supplier_id INT8  ,
  warehouse_id INT8  ,
  batch_no VARCHAR(50)  ,
  inspection_date DATE NOT NULL ,
  lot_quantity NUMERIC(20,4)  ,
  sample_quantity NUMERIC(20,4)  ,
  inspector_id INT8  ,
  result VARCHAR(20) NOT NULL ,
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
  business_date DATE NOT NULL ,
  constraint PK_erp_qa_inspection primary key (id)
);

CREATE TABLE erp_qa_inspection_template_line(
  id INT8 NOT NULL ,
  template_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  parameter_name VARCHAR(200) NOT NULL ,
  spec_min NUMERIC(20,6)  ,
  spec_max NUMERIC(20,6)  ,
  unit VARCHAR(50)  ,
  is_required INT4 default 1  NOT NULL ,
  inspection_method VARCHAR(200)  ,
  sort_num INT4 default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_inspection_template_line primary key (id)
);

CREATE TABLE erp_qa_inspection_line(
  id INT8 NOT NULL ,
  inspection_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  parameter_id INT8  ,
  parameter_name VARCHAR(200)  ,
  spec_min NUMERIC(20,6)  ,
  spec_max NUMERIC(20,6)  ,
  measured_value VARCHAR(100)  ,
  unit VARCHAR(50)  ,
  result VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_inspection_line primary key (id)
);

CREATE TABLE erp_qa_non_conformance(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  ncr_date DATE NOT NULL ,
  source_type VARCHAR(50)  ,
  source_code VARCHAR(50)  ,
  material_id INT8 NOT NULL ,
  inspection_id INT8  ,
  quantity NUMERIC(20,4)  ,
  description VARCHAR(2000)  ,
  severity VARCHAR(20) NOT NULL ,
  disposition_type VARCHAR(20)  ,
  status VARCHAR(20) NOT NULL ,
  supplier_id INT8  ,
  parameter_name VARCHAR(200)  ,
  measured_value VARCHAR(100)  ,
  spec_min NUMERIC(20,6)  ,
  spec_max NUMERIC(20,6)  ,
  assigned_to INT8  ,
  resolved_by INT8  ,
  resolved_at TIMESTAMP  ,
  resolution VARCHAR(2000)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by VARCHAR(36)  ,
  return_code VARCHAR(50)  ,
  constraint PK_erp_qa_non_conformance primary key (id)
);

CREATE TABLE erp_qa_action(
  id INT8 NOT NULL ,
  ncr_id INT8 NOT NULL ,
  action_type VARCHAR(20) NOT NULL ,
  description VARCHAR(2000)  ,
  responsible_person INT8  ,
  due_date DATE  ,
  status VARCHAR(20) NOT NULL ,
  completed_by INT8  ,
  completed_at TIMESTAMP  ,
  verification_person INT8  ,
  verification_date DATE  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_action primary key (id)
);

CREATE TABLE erp_qa_recall(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  recall_name VARCHAR(200) NOT NULL ,
  trigger_type VARCHAR(20) NOT NULL ,
  source_ncr_id INT8  ,
  material_id INT8  ,
  batch_id INT8  ,
  serial_no VARCHAR(50)  ,
  root_cause VARCHAR(2000)  ,
  severity_level VARCHAR(20) NOT NULL ,
  business_date DATE NOT NULL ,
  notify_customer BOOLEAN default false   ,
  status VARCHAR(20) NOT NULL ,
  approve_status VARCHAR(20) NOT NULL ,
  approved_by VARCHAR(36)  ,
  approved_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_recall primary key (id)
);

CREATE TABLE erp_qa_recall_target(
  id INT8 NOT NULL ,
  recall_id INT8 NOT NULL ,
  partner_id INT8  ,
  batch_no VARCHAR(50)  ,
  serial_no VARCHAR(50)  ,
  sales_delivery_id INT8  ,
  shipped_qty NUMERIC(20,4)  ,
  notified_at TIMESTAMP  ,
  notified_by VARCHAR(36)  ,
  return_status VARCHAR(20) NOT NULL ,
  generated_return_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_recall_target primary key (id)
);


      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_qa_risk_register IS '风险登记';
                
      COMMENT ON COLUMN erp_qa_risk_register.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_risk_register.code IS '编号';
                    
      COMMENT ON COLUMN erp_qa_risk_register.risk_date IS '登记日期';
                    
      COMMENT ON COLUMN erp_qa_risk_register.description IS '风险描述';
                    
      COMMENT ON COLUMN erp_qa_risk_register.category IS '风险类别';
                    
      COMMENT ON COLUMN erp_qa_risk_register.likelihood IS '发生可能性';
                    
      COMMENT ON COLUMN erp_qa_risk_register.severity IS '影响严重性';
                    
      COMMENT ON COLUMN erp_qa_risk_register.risk_score IS '风险评分';
                    
      COMMENT ON COLUMN erp_qa_risk_register.mitigation IS '缓解措施';
                    
      COMMENT ON COLUMN erp_qa_risk_register.owner_id IS '责任人';
                    
      COMMENT ON COLUMN erp_qa_risk_register.status IS '状态';
                    
      COMMENT ON COLUMN erp_qa_risk_register.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_risk_register.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_risk_register.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_risk_register.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_risk_register.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_risk_register.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_risk_register.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_sampling_plan IS '抽样方案';
                
      COMMENT ON COLUMN erp_qa_sampling_plan.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.code IS '编码';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.name IS '名称';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.aql_level IS 'AQL 检验水平(如 II)';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.lot_size_from IS '批量下限';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.lot_size_to IS '批量上限';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.sample_size IS '样本量';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.accept_number IS '合格判定数(Ac)';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.reject_number IS '不合格判定数(Re)';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_inspection_template IS '质检模板';
                
      COMMENT ON COLUMN erp_qa_inspection_template.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.code IS '编码';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.name IS '名称';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.inspection_type IS '检验类型';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.material_id IS '适用物料';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_quality_goal IS '质量目标';
                
      COMMENT ON COLUMN erp_qa_quality_goal.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.code IS '编码';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.name IS '名称';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.description IS '描述';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.target_value IS '目标值';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.current_value IS '当前值';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.unit IS '单位';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.responsible_person_id IS '责任人';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.start_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.end_date IS '结束日期';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.status IS '状态';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_review IS '质量评审';
                
      COMMENT ON COLUMN erp_qa_review.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_review.code IS '编号';
                    
      COMMENT ON COLUMN erp_qa_review.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_qa_review.review_date IS '评审日期';
                    
      COMMENT ON COLUMN erp_qa_review.review_type IS '评审类型';
                    
      COMMENT ON COLUMN erp_qa_review.related_bill_type IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_qa_review.related_bill_code IS '关联单据号';
                    
      COMMENT ON COLUMN erp_qa_review.participants IS '参与人员';
                    
      COMMENT ON COLUMN erp_qa_review.conclusion IS '评审结论';
                    
      COMMENT ON COLUMN erp_qa_review.action_required IS '是否需要措施';
                    
      COMMENT ON COLUMN erp_qa_review.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_qa_review.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_qa_review.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_review.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_review.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_review.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_review.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_review.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_review.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_qa_review.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_qa_review.approved_at IS '审核时间';
                    
      COMMENT ON TABLE erp_qa_calibration IS '量具校准';
                
      COMMENT ON COLUMN erp_qa_calibration.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_calibration.code IS '单号';
                    
      COMMENT ON COLUMN erp_qa_calibration.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_qa_calibration.instrument_name IS '量具/设备名称';
                    
      COMMENT ON COLUMN erp_qa_calibration.instrument_code IS '量具编号';
                    
      COMMENT ON COLUMN erp_qa_calibration.business_date IS '校准日期';
                    
      COMMENT ON COLUMN erp_qa_calibration.standard_ref IS '参考标准';
                    
      COMMENT ON COLUMN erp_qa_calibration.measured_value IS '测量值';
                    
      COMMENT ON COLUMN erp_qa_calibration.target_value IS '目标值';
                    
      COMMENT ON COLUMN erp_qa_calibration.tolerance IS '允差';
                    
      COMMENT ON COLUMN erp_qa_calibration.result IS '校准结果';
                    
      COMMENT ON COLUMN erp_qa_calibration.next_calibration_date IS '下次校准日期';
                    
      COMMENT ON COLUMN erp_qa_calibration.calibrated_by IS '校准人(职员)';
                    
      COMMENT ON COLUMN erp_qa_calibration.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_qa_calibration.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_qa_calibration.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_calibration.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_calibration.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_calibration.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_calibration.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_calibration.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_calibration.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_qa_calibration.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_qa_calibration.approved_at IS '审核时间';
                    
      COMMENT ON TABLE erp_qa_inspection IS '质检单';
                
      COMMENT ON COLUMN erp_qa_inspection.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_inspection.code IS '单号';
                    
      COMMENT ON COLUMN erp_qa_inspection.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_qa_inspection.inspection_type IS '检验类型';
                    
      COMMENT ON COLUMN erp_qa_inspection.related_bill_type IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_qa_inspection.related_bill_code IS '关联单据号';
                    
      COMMENT ON COLUMN erp_qa_inspection.related_line_code IS '关联单据行号';
                    
      COMMENT ON COLUMN erp_qa_inspection.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_qa_inspection.template_id IS '质检模板';
                    
      COMMENT ON COLUMN erp_qa_inspection.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_qa_inspection.warehouse_id IS '仓库';
                    
      COMMENT ON COLUMN erp_qa_inspection.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_qa_inspection.inspection_date IS '检验日期';
                    
      COMMENT ON COLUMN erp_qa_inspection.lot_quantity IS '批量';
                    
      COMMENT ON COLUMN erp_qa_inspection.sample_quantity IS '抽样数量';
                    
      COMMENT ON COLUMN erp_qa_inspection.inspector_id IS '检验员(职员)';
                    
      COMMENT ON COLUMN erp_qa_inspection.result IS '质检结果';
                    
      COMMENT ON COLUMN erp_qa_inspection.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_qa_inspection.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_qa_inspection.posted IS '已过账(质量结论已回写业务单据)';
                    
      COMMENT ON COLUMN erp_qa_inspection.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_qa_inspection.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_qa_inspection.approved_by IS '审核人';
                    
      COMMENT ON COLUMN erp_qa_inspection.approved_at IS '审核时间';
                    
      COMMENT ON COLUMN erp_qa_inspection.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_inspection.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_inspection.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_inspection.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_inspection.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_inspection.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_inspection.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_qa_inspection.business_date IS '业务日期';
                    
      COMMENT ON TABLE erp_qa_inspection_template_line IS '质检模板行';
                
      COMMENT ON COLUMN erp_qa_inspection_template_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.template_id IS '模板ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.parameter_name IS '检验参数名称';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.spec_min IS '规格下限';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.spec_max IS '规格上限';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.unit IS '计量单位';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.is_required IS '是否必检';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.inspection_method IS '检验方法';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.sort_num IS '排序';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_inspection_line IS '质检单行';
                
      COMMENT ON COLUMN erp_qa_inspection_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.inspection_id IS '质检单ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.parameter_id IS '检验参数';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.parameter_name IS '检验参数名称';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.spec_min IS '规格下限';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.spec_max IS '规格上限';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.measured_value IS '实测值';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.unit IS '计量单位';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.result IS '行结果';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_non_conformance IS '不合格品报告';
                
      COMMENT ON COLUMN erp_qa_non_conformance.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.code IS '单号';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.ncr_date IS '报告日期';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.source_type IS '来源类型';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.source_code IS '来源单号';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.inspection_id IS '质检单ID';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.quantity IS '不合格数量';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.description IS '问题描述';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.severity IS '严重程度';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.disposition_type IS '处理决定';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.status IS '状态';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.supplier_id IS '供应商';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.parameter_name IS '不合格参数';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.measured_value IS '实测值';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.spec_min IS '规格下限';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.spec_max IS '规格上限';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.assigned_to IS '责任人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.resolved_by IS '解决人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.resolved_at IS '解决时间';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.resolution IS '解决措施';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.posted IS '已过账(报废处置已生成凭证)';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.return_code IS '关联退货单号(RETURN 处置编排退货域后登记)';
                    
      COMMENT ON TABLE erp_qa_action IS '纠正预防措施';
                
      COMMENT ON COLUMN erp_qa_action.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_action.ncr_id IS '不合格品报告ID';
                    
      COMMENT ON COLUMN erp_qa_action.action_type IS '措施类型';
                    
      COMMENT ON COLUMN erp_qa_action.description IS '措施描述';
                    
      COMMENT ON COLUMN erp_qa_action.responsible_person IS '负责人';
                    
      COMMENT ON COLUMN erp_qa_action.due_date IS '计划完成日期';
                    
      COMMENT ON COLUMN erp_qa_action.status IS '状态';
                    
      COMMENT ON COLUMN erp_qa_action.completed_by IS '完成人';
                    
      COMMENT ON COLUMN erp_qa_action.completed_at IS '完成时间';
                    
      COMMENT ON COLUMN erp_qa_action.verification_person IS '验证人';
                    
      COMMENT ON COLUMN erp_qa_action.verification_date IS '验证日期';
                    
      COMMENT ON COLUMN erp_qa_action.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_action.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_action.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_action.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_action.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_action.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_action.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_recall IS '召回事件';
                
      COMMENT ON COLUMN erp_qa_recall.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_recall.code IS '单号';
                    
      COMMENT ON COLUMN erp_qa_recall.recall_name IS '召回名称';
                    
      COMMENT ON COLUMN erp_qa_recall.trigger_type IS '触发类型';
                    
      COMMENT ON COLUMN erp_qa_recall.source_ncr_id IS '来源NCR(弱指针)';
                    
      COMMENT ON COLUMN erp_qa_recall.material_id IS '召回物料';
                    
      COMMENT ON COLUMN erp_qa_recall.batch_id IS '召回批次(弱指针→ErpInvBatch)';
                    
      COMMENT ON COLUMN erp_qa_recall.serial_no IS '召回序列号';
                    
      COMMENT ON COLUMN erp_qa_recall.root_cause IS '根本原因';
                    
      COMMENT ON COLUMN erp_qa_recall.severity_level IS '严重程度';
                    
      COMMENT ON COLUMN erp_qa_recall.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_qa_recall.notify_customer IS '已通知客户';
                    
      COMMENT ON COLUMN erp_qa_recall.status IS '召回状态';
                    
      COMMENT ON COLUMN erp_qa_recall.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_qa_recall.approved_by IS '审批人';
                    
      COMMENT ON COLUMN erp_qa_recall.approved_at IS '审批时间';
                    
      COMMENT ON COLUMN erp_qa_recall.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_recall.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_recall.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_recall.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_recall.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_recall.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_recall.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_recall_target IS '召回目标';
                
      COMMENT ON COLUMN erp_qa_recall_target.id IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_recall_target.recall_id IS '召回事件';
                    
      COMMENT ON COLUMN erp_qa_recall_target.partner_id IS '受影响客户';
                    
      COMMENT ON COLUMN erp_qa_recall_target.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_qa_recall_target.serial_no IS '序列号';
                    
      COMMENT ON COLUMN erp_qa_recall_target.sales_delivery_id IS '销售出库单(弱指针)';
                    
      COMMENT ON COLUMN erp_qa_recall_target.shipped_qty IS '发货数量';
                    
      COMMENT ON COLUMN erp_qa_recall_target.notified_at IS '通知时间';
                    
      COMMENT ON COLUMN erp_qa_recall_target.notified_by IS '通知人';
                    
      COMMENT ON COLUMN erp_qa_recall_target.return_status IS '退货状态';
                    
      COMMENT ON COLUMN erp_qa_recall_target.generated_return_id IS '已生成退货单(弱指针)';
                    
      COMMENT ON COLUMN erp_qa_recall_target.remark IS '备注';
                    
      COMMENT ON COLUMN erp_qa_recall_target.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_recall_target.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_recall_target.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_recall_target.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_recall_target.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_recall_target.update_time IS '修改时间';
                    
