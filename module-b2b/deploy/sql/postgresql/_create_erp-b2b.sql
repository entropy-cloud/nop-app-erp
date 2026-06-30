
CREATE TABLE erp_md_md_organization(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_organization primary key (id)
);

CREATE TABLE erp_md_md_partner(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_partner primary key (id)
);

CREATE TABLE erp_md_md_material(
  id INT8 NOT NULL ,
  constraint PK_erp_md_md_material primary key (id)
);

CREATE TABLE erp_b2b_edi_format(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  format_name VARCHAR(200) NOT NULL ,
  format_standard INT4 NOT NULL ,
  direction INT4 NOT NULL ,
  needs_web_service INT4 default 0  NOT NULL ,
  is_active INT4 default 1  NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_edi_format primary key (id)
);

CREATE TABLE erp_b2b_code_mapping(
  id INT8 NOT NULL ,
  org_id INT8  ,
  mapping_type INT4 NOT NULL ,
  internal_code VARCHAR(100) NOT NULL ,
  external_code VARCHAR(100) NOT NULL ,
  partner_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_code_mapping primary key (id)
);

CREATE TABLE erp_b2b_edi_doc(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  format_id INT8  ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  state INT4 NOT NULL ,
  blocking_level INT4 default 10  NOT NULL ,
  error VARCHAR(2000)  ,
  retry_count INT4 default 0  NOT NULL ,
  attachment_id INT8  ,
  sent_at TIMESTAMP  ,
  acknowledged_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_edi_doc primary key (id)
);

CREATE TABLE erp_b2b_asn(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  source_edi_doc_id INT8  ,
  partner_id INT8  ,
  shipment_date DATE  ,
  estimated_arrival_date DATE  ,
  tracking_no VARCHAR(100)  ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_asn primary key (id)
);

CREATE TABLE erp_b2b_edi_log(
  id INT8 NOT NULL ,
  edi_doc_id INT8  ,
  org_id INT8  ,
  direction INT4 NOT NULL ,
  request_payload TEXT  ,
  response_payload TEXT  ,
  result_code VARCHAR(50)  ,
  result_msg VARCHAR(500)  ,
  log_time TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_edi_log primary key (id)
);

CREATE TABLE erp_b2b_asn_line(
  id INT8 NOT NULL ,
  asn_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8  ,
  supplier_part_no VARCHAR(100)  ,
  quantity NUMERIC(20,4)  ,
  shipped_qty NUMERIC(20,4)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_asn_line primary key (id)
);


      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_md_partner IS 'ErpMdPartner';
                
      COMMENT ON TABLE erp_md_md_material IS 'ErpMdMaterial';
                
      COMMENT ON TABLE erp_b2b_edi_format IS 'EDI 格式配置';
                
      COMMENT ON COLUMN erp_b2b_edi_format.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.code IS '编码';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.format_name IS '格式名称';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.format_standard IS 'EDI 标准';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.direction IS '方向';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.needs_web_service IS '需要 WebService';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_code_mapping IS '内外代码映射';
                
      COMMENT ON COLUMN erp_b2b_code_mapping.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.mapping_type IS '映射类型';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.internal_code IS '内部代码';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.external_code IS '外部代码';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.partner_id IS 'Trading Partner';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_edi_doc IS 'EDI 事务';
                
      COMMENT ON COLUMN erp_b2b_edi_doc.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.code IS '事务编码';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.format_id IS 'EDI 格式';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.related_bill_type IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.related_bill_code IS '关联单据号';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.state IS '状态';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.blocking_level IS '阻断级别';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.error IS '错误信息';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.retry_count IS '重试次数';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.attachment_id IS '附件（EDI 报文文件）';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.sent_at IS '发送时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.acknowledged_at IS '确认时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_asn IS '提前发货通知';
                
      COMMENT ON COLUMN erp_b2b_asn.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_asn.code IS 'ASN 编码';
                    
      COMMENT ON COLUMN erp_b2b_asn.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_asn.source_edi_doc_id IS '来源 EDI 文档';
                    
      COMMENT ON COLUMN erp_b2b_asn.partner_id IS '发货方（供应商）';
                    
      COMMENT ON COLUMN erp_b2b_asn.shipment_date IS '发货日期';
                    
      COMMENT ON COLUMN erp_b2b_asn.estimated_arrival_date IS '预计到货日期';
                    
      COMMENT ON COLUMN erp_b2b_asn.tracking_no IS '物流单号';
                    
      COMMENT ON COLUMN erp_b2b_asn.related_bill_type IS '关联采购订单类型';
                    
      COMMENT ON COLUMN erp_b2b_asn.related_bill_code IS '关联采购订单号';
                    
      COMMENT ON COLUMN erp_b2b_asn.status IS '状态';
                    
      COMMENT ON COLUMN erp_b2b_asn.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_asn.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_asn.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_asn.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_asn.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_asn.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_asn.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_edi_log IS 'EDI 交互日志';
                
      COMMENT ON COLUMN erp_b2b_edi_log.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.edi_doc_id IS 'EDI 事务 ID';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.direction IS '方向';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.request_payload IS '请求报文';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.response_payload IS '响应报文';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.result_code IS '结果码';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.result_msg IS '结果消息';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.log_time IS '日志时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_asn_line IS 'ASN 明细行';
                
      COMMENT ON COLUMN erp_b2b_asn_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.asn_id IS 'ASN ID';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.supplier_part_no IS '供应商料号';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.quantity IS '订单数量';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.shipped_qty IS '发货数量';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.update_time IS '修改时间';
                    
