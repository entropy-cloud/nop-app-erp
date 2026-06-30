
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

CREATE TABLE erp_b2b_partner_profile(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  partner_id INT8  ,
  partner_name VARCHAR(200) NOT NULL ,
  status INT4 NOT NULL ,
  protocol INT4 NOT NULL ,
  transport_endpoint VARCHAR(500)  ,
  auth_method INT4 NOT NULL ,
  webhook_secret VARCHAR(500)  ,
  cert_expiry DATE  ,
  cert_fingerprint VARCHAR(200)  ,
  allowed_formats VARCHAR(500)  ,
  timezone VARCHAR(50)  ,
  contact_name VARCHAR(100)  ,
  contact_email VARCHAR(200)  ,
  contact_phone VARCHAR(50)  ,
  notes VARCHAR(1000)  ,
  go_live_date DATE  ,
  archived_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_partner_profile primary key (id)
);

CREATE TABLE erp_b2b_mft_config(
  id INT8 NOT NULL ,
  org_id INT8  ,
  partner_id INT8 NOT NULL ,
  protocol VARCHAR(50) NOT NULL ,
  transport_endpoint VARCHAR(500) NOT NULL ,
  local_as2_id VARCHAR(100)  ,
  remote_as2_id VARCHAR(100)  ,
  sftp_username VARCHAR(100)  ,
  sftp_port INT4 default 22   ,
  ftps_port INT4 default 990   ,
  ftps_implicit_tls BOOLEAN default true   ,
  compression BOOLEAN default false   ,
  encryption BOOLEAN default false   ,
  encryption_algo VARCHAR(50)  ,
  signature BOOLEAN default false   ,
  signature_algo VARCHAR(50)  ,
  cert_id INT8  ,
  active BOOLEAN default true   ,
  max_retries INT4 default 3   ,
  retry_interval_min INT4 default 15   ,
  dead_letter_enabled BOOLEAN default true   ,
  monitor_directory VARCHAR(500)  ,
  monitor_interval_sec INT4 default 60   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_mft_config primary key (id)
);

CREATE TABLE erp_b2b_mft_certificate(
  id INT8 NOT NULL ,
  org_id INT8  ,
  partner_id INT8  ,
  cert_name VARCHAR(200) NOT NULL ,
  cert_type VARCHAR(50) NOT NULL ,
  algorithm VARCHAR(50) NOT NULL ,
  key_size INT4 default 2048   ,
  issuer_name VARCHAR(200)  ,
  subject_name VARCHAR(200)  ,
  serial_no VARCHAR(100)  ,
  fingerprint_sha256 VARCHAR(100)  ,
  issued_at DATE  ,
  expires_at DATE NOT NULL ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_mft_certificate primary key (id)
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

CREATE TABLE erp_b2b_partner_credential(
  id INT8 NOT NULL ,
  partner_profile_id INT8 NOT NULL ,
  credential_type INT4 NOT NULL ,
  credential_key VARCHAR(200)  ,
  credential_value VARCHAR(2000)  ,
  issued_at DATE  ,
  expires_at DATE  ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_partner_credential primary key (id)
);

CREATE TABLE erp_b2b_test_exchange(
  id INT8 NOT NULL ,
  partner_profile_id INT8 NOT NULL ,
  direction INT4 NOT NULL ,
  format_code VARCHAR(50) NOT NULL ,
  test_case_code VARCHAR(50)  ,
  sent_payload TEXT  ,
  received_payload TEXT  ,
  expected_result VARCHAR(500)  ,
  actual_result VARCHAR(500)  ,
  passed BOOLEAN  ,
  tested_by VARCHAR(50)  ,
  tested_at TIMESTAMP  ,
  notes VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_test_exchange primary key (id)
);

CREATE TABLE erp_b2b_certification_checklist(
  id INT8 NOT NULL ,
  partner_profile_id INT8 NOT NULL ,
  checklist_item VARCHAR(500) NOT NULL ,
  required_doc_type VARCHAR(100)  ,
  is_mandatory BOOLEAN default true   ,
  is_passed BOOLEAN  ,
  checked_by VARCHAR(50)  ,
  checked_at TIMESTAMP  ,
  evidence VARCHAR(1000)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_certification_checklist primary key (id)
);

CREATE TABLE erp_b2b_mft_log(
  id INT8 NOT NULL ,
  org_id INT8  ,
  config_id INT8 NOT NULL ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  direction VARCHAR(50) NOT NULL ,
  file_name VARCHAR(500)  ,
  file_size INT8  ,
  file_hash VARCHAR(100)  ,
  message_id VARCHAR(200)  ,
  mdn_status VARCHAR(50)  ,
  protocol VARCHAR(50) NOT NULL ,
  status VARCHAR(50) NOT NULL ,
  start_time TIMESTAMP  ,
  end_time TIMESTAMP  ,
  duration_ms INT8  ,
  error_code VARCHAR(50)  ,
  error_msg VARCHAR(1000)  ,
  retry_count INT4 default 0   ,
  is_compressed BOOLEAN default false   ,
  is_encrypted BOOLEAN default false   ,
  is_signed BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_mft_log primary key (id)
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
                    
      COMMENT ON TABLE erp_b2b_partner_profile IS '合作伙伴档案';
                
      COMMENT ON COLUMN erp_b2b_partner_profile.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.code IS '编码';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.partner_id IS '关联伙伴';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.partner_name IS '伙伴名称';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.status IS '状态';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.protocol IS '传输协议';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.transport_endpoint IS '传输端点';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.auth_method IS '认证方式';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.webhook_secret IS 'Webhook密钥';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.cert_expiry IS '证书过期日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.cert_fingerprint IS '证书指纹';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.allowed_formats IS '支持格式(JSON)';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.timezone IS '时区';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.contact_name IS '联系人姓名';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.contact_email IS '联系邮箱';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.contact_phone IS '联系电话';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.notes IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.go_live_date IS '上线日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.archived_at IS '归档日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_mft_config IS 'MFT 配置';
                
      COMMENT ON COLUMN erp_b2b_mft_config.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.partner_id IS '合作伙伴';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.protocol IS '传输协议';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.transport_endpoint IS '传输端点';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.local_as2_id IS '本地AS2 ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.remote_as2_id IS '对方AS2 ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.sftp_username IS 'SFTP用户名';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.sftp_port IS 'SFTP端口';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.ftps_port IS 'FTPS端口';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.ftps_implicit_tls IS '隐式TLS';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.compression IS '启用压缩';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.encryption IS '启用加密';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.encryption_algo IS '加密算法';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.signature IS '启用签名';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.signature_algo IS '签名算法';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.cert_id IS '关联证书';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.active IS '启用';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.max_retries IS '最大重试次数';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.retry_interval_min IS '重试间隔(分钟)';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.dead_letter_enabled IS '启用死信队列';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.monitor_directory IS '监控目录';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.monitor_interval_sec IS '监控间隔(秒)';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_mft_certificate IS 'MFT 证书';
                
      COMMENT ON COLUMN erp_b2b_mft_certificate.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.partner_id IS '合作伙伴';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.cert_name IS '证书名称';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.cert_type IS '证书类型';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.algorithm IS '算法';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.key_size IS '密钥长度';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.issuer_name IS '颁发者';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.subject_name IS '主题';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.serial_no IS '序列号';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.fingerprint_sha256 IS 'SHA-256指纹';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.issued_at IS '颁发日期';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.expires_at IS '过期日期';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.is_active IS '启用';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.update_time IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_b2b_partner_credential IS '伙伴凭证';
                
      COMMENT ON COLUMN erp_b2b_partner_credential.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.partner_profile_id IS '伙伴档案';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.credential_type IS '凭证类型';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.credential_key IS '凭证标识';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.credential_value IS '凭证值';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.issued_at IS '颁发日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.expires_at IS '过期日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.is_active IS '启用';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_test_exchange IS '测试消息交换';
                
      COMMENT ON COLUMN erp_b2b_test_exchange.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.partner_profile_id IS '伙伴档案';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.direction IS '方向';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.format_code IS 'EDI格式';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.test_case_code IS '测试用例编号';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.sent_payload IS '发送报文';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.received_payload IS '接收报文';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.expected_result IS '预期结果';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.actual_result IS '实际结果';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.passed IS '测试通过';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.tested_by IS '测试人';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.tested_at IS '测试时间';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.notes IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_certification_checklist IS '认证检查清单';
                
      COMMENT ON COLUMN erp_b2b_certification_checklist.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.partner_profile_id IS '伙伴档案';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.checklist_item IS '检查项';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.required_doc_type IS '对应文档类型';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.is_mandatory IS '必检';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.is_passed IS '通过';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.checked_by IS '检查人';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.checked_at IS '检查时间';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.evidence IS '检查证据';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_mft_log IS 'MFT 传输日志';
                
      COMMENT ON COLUMN erp_b2b_mft_log.id IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.config_id IS 'MFT配置';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.related_bill_type IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.related_bill_code IS '关联单据号';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.direction IS '方向';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.file_name IS '文件名';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.file_size IS '文件大小(字节)';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.file_hash IS '文件哈希';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.message_id IS 'AS2 Message-ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.mdn_status IS 'MDN状态';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.protocol IS '传输协议';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.status IS '传输状态';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.start_time IS '开始时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.end_time IS '结束时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.duration_ms IS '耗时(毫秒)';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.error_code IS '错误码';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.error_msg IS '错误消息';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.retry_count IS '重试次数';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.is_compressed IS '压缩';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.is_encrypted IS '加密';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.is_signed IS '签名';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.remark IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.update_time IS '修改时间';
                    
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
                    
