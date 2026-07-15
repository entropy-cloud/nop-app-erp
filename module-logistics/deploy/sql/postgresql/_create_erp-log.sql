
CREATE TABLE erp_md_partner(
  id INT8 NOT NULL ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_partner primary key (id)
);

CREATE TABLE erp_md_organization(
  id INT8 NOT NULL ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_organization primary key (id)
);

CREATE TABLE erp_md_employee(
  id INT8 NOT NULL ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_employee primary key (id)
);

CREATE TABLE erp_md_currency(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_currency primary key (id)
);

CREATE TABLE erp_md_material(
  id INT8 NOT NULL ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_material primary key (id)
);

CREATE TABLE erp_log_carrier(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  carrier_name VARCHAR(200) NOT NULL ,
  carrier_type VARCHAR(20) NOT NULL ,
  gateway_id VARCHAR(50) NOT NULL ,
  partner_id INT8  ,
  is_active INT4 default 1  NOT NULL ,
  tracking_url_template VARCHAR(500)  ,
  max_parcel_weight NUMERIC(12,3)  ,
  supported_service_types TEXT  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_log_carrier primary key (id)
);

CREATE TABLE erp_log_delivery_window(
  id INT8 NOT NULL ,
  partner_id INT8 NOT NULL ,
  org_id INT8  ,
  weekday INT4 NOT NULL ,
  start_time VARCHAR(8) NOT NULL ,
  end_time VARCHAR(8) NOT NULL ,
  max_capacity INT4 default 10  NOT NULL ,
  current_booked INT4 default 0  NOT NULL ,
  is_active BOOLEAN default true   ,
  effective_from DATE  ,
  effective_to DATE  ,
  allowed_shipment_types VARCHAR(200)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_log_delivery_window primary key (id)
);

CREATE TABLE erp_log_carrier_config(
  id INT8 NOT NULL ,
  carrier_id INT8 NOT NULL ,
  org_id INT8  ,
  config_code VARCHAR(50) NOT NULL ,
  service_type VARCHAR(50)  ,
  api_endpoint VARCHAR(500)  ,
  api_key VARCHAR(500)  ,
  api_secret VARCHAR(500)  ,
  credentials TEXT  ,
  tracking_url_template VARCHAR(500)  ,
  print_format VARCHAR(50)  ,
  additional_properties TEXT  ,
  is_active INT4 default 1  NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_log_carrier_config primary key (id)
);

CREATE TABLE erp_log_shipment(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  carrier_id INT8 NOT NULL ,
  carrier_config_id INT8  ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  shipment_date DATE  ,
  tracking_no VARCHAR(100)  ,
  label_url VARCHAR(500)  ,
  freight_amount NUMERIC(20,4)  ,
  freight_currency_id INT8  ,
  freight_terms VARCHAR(20)  ,
  freight_settlement_status VARCHAR(20)  ,
  total_weight NUMERIC(12,3)  ,
  total_volume NUMERIC(12,6)  ,
  total_parcels INT4 default 1   ,
  receiver_name VARCHAR(100)  ,
  receiver_phone VARCHAR(30)  ,
  receiver_address VARCHAR(500)  ,
  receiver_country VARCHAR(100)  ,
  receiver_province VARCHAR(100)  ,
  receiver_city VARCHAR(100)  ,
  receiver_district VARCHAR(100)  ,
  sender_name VARCHAR(100)  ,
  sender_phone VARCHAR(30)  ,
  sender_address VARCHAR(500)  ,
  estimated_delivery_date DATE  ,
  actual_delivery_date DATE  ,
  signed_by VARCHAR(100)  ,
  shipper_id INT8  ,
  status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  business_date DATE NOT NULL ,
  posted BOOLEAN default false   ,
  constraint PK_erp_log_shipment primary key (id)
);

CREATE TABLE erp_log_shipment_line(
  id INT8 NOT NULL ,
  shipment_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8  ,
  quantity NUMERIC(20,4)  ,
  unit VARCHAR(50)  ,
  package_description VARCHAR(500)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_log_shipment_line primary key (id)
);

CREATE TABLE erp_log_shipment_parcel(
  id INT8 NOT NULL ,
  shipment_id INT8 NOT NULL ,
  parcel_no VARCHAR(50) NOT NULL ,
  tracking_no VARCHAR(100)  ,
  label_url VARCHAR(500)  ,
  weight NUMERIC(12,3)  ,
  length NUMERIC(12,2)  ,
  width NUMERIC(12,2)  ,
  height NUMERIC(12,2)  ,
  declared_value NUMERIC(20,4)  ,
  is_active INT4 default 1  NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_log_shipment_parcel primary key (id)
);

CREATE TABLE erp_log_shipment_log(
  id INT8 NOT NULL ,
  shipment_id INT8 NOT NULL ,
  org_id INT8  ,
  gateway_id VARCHAR(50) NOT NULL ,
  action_type VARCHAR(20) NOT NULL ,
  request_body TEXT  ,
  response_body TEXT  ,
  http_status INT4  ,
  error_code VARCHAR(100)  ,
  error_message VARCHAR(2000)  ,
  is_success BOOLEAN default true   ,
  executed_at TIMESTAMP NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_log_shipment_log primary key (id)
);


      COMMENT ON TABLE erp_md_partner IS 'ErpMdPartner';
                
      COMMENT ON TABLE erp_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_employee IS 'ErpMdEmployee';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_material IS 'ErpMdMaterial';
                
      COMMENT ON TABLE erp_log_carrier IS '承运商';
                
      COMMENT ON COLUMN erp_log_carrier.id IS 'ID';
                    
      COMMENT ON COLUMN erp_log_carrier.code IS '编码';
                    
      COMMENT ON COLUMN erp_log_carrier.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_carrier.carrier_name IS '承运商名称';
                    
      COMMENT ON COLUMN erp_log_carrier.carrier_type IS '承运商类型';
                    
      COMMENT ON COLUMN erp_log_carrier.gateway_id IS '网关标识';
                    
      COMMENT ON COLUMN erp_log_carrier.partner_id IS '承运商往来单位ID';
                    
      COMMENT ON COLUMN erp_log_carrier.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_log_carrier.tracking_url_template IS '追踪URL模板';
                    
      COMMENT ON COLUMN erp_log_carrier.max_parcel_weight IS '最大包裹重量(kg)';
                    
      COMMENT ON COLUMN erp_log_carrier.supported_service_types IS '支持服务类型';
                    
      COMMENT ON COLUMN erp_log_carrier.remark IS '备注';
                    
      COMMENT ON COLUMN erp_log_carrier.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_carrier.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_carrier.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_log_carrier.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_carrier.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_log_carrier.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_log_delivery_window IS '配送时间窗口';
                
      COMMENT ON COLUMN erp_log_delivery_window.id IS 'ID';
                    
      COMMENT ON COLUMN erp_log_delivery_window.partner_id IS '客户';
                    
      COMMENT ON COLUMN erp_log_delivery_window.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_delivery_window.weekday IS '星期';
                    
      COMMENT ON COLUMN erp_log_delivery_window.start_time IS '开始时间';
                    
      COMMENT ON COLUMN erp_log_delivery_window.end_time IS '结束时间';
                    
      COMMENT ON COLUMN erp_log_delivery_window.max_capacity IS '最大预约数';
                    
      COMMENT ON COLUMN erp_log_delivery_window.current_booked IS '当前已预约';
                    
      COMMENT ON COLUMN erp_log_delivery_window.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_log_delivery_window.effective_from IS '生效日期';
                    
      COMMENT ON COLUMN erp_log_delivery_window.effective_to IS '失效日期';
                    
      COMMENT ON COLUMN erp_log_delivery_window.allowed_shipment_types IS '允许发运类型';
                    
      COMMENT ON COLUMN erp_log_delivery_window.remark IS '备注';
                    
      COMMENT ON COLUMN erp_log_delivery_window.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_delivery_window.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_delivery_window.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_log_delivery_window.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_delivery_window.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_log_delivery_window.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_log_carrier_config IS '承运商配置';
                
      COMMENT ON COLUMN erp_log_carrier_config.id IS 'ID';
                    
      COMMENT ON COLUMN erp_log_carrier_config.carrier_id IS '承运商ID';
                    
      COMMENT ON COLUMN erp_log_carrier_config.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_carrier_config.config_code IS '配置编码';
                    
      COMMENT ON COLUMN erp_log_carrier_config.service_type IS '服务类型';
                    
      COMMENT ON COLUMN erp_log_carrier_config.api_endpoint IS '接口地址';
                    
      COMMENT ON COLUMN erp_log_carrier_config.api_key IS 'API密钥';
                    
      COMMENT ON COLUMN erp_log_carrier_config.api_secret IS 'API密钥';
                    
      COMMENT ON COLUMN erp_log_carrier_config.credentials IS '完整凭证';
                    
      COMMENT ON COLUMN erp_log_carrier_config.tracking_url_template IS '追踪URL模板';
                    
      COMMENT ON COLUMN erp_log_carrier_config.print_format IS '面单打印格式';
                    
      COMMENT ON COLUMN erp_log_carrier_config.additional_properties IS '扩展参数';
                    
      COMMENT ON COLUMN erp_log_carrier_config.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_log_carrier_config.remark IS '备注';
                    
      COMMENT ON COLUMN erp_log_carrier_config.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_carrier_config.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_carrier_config.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_log_carrier_config.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_carrier_config.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_log_carrier_config.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_log_shipment IS '发运单';
                
      COMMENT ON COLUMN erp_log_shipment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_log_shipment.code IS '单号';
                    
      COMMENT ON COLUMN erp_log_shipment.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_shipment.carrier_id IS '承运商ID';
                    
      COMMENT ON COLUMN erp_log_shipment.carrier_config_id IS '承运商配置ID';
                    
      COMMENT ON COLUMN erp_log_shipment.related_bill_type IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_log_shipment.related_bill_code IS '关联单据号';
                    
      COMMENT ON COLUMN erp_log_shipment.shipment_date IS '发运日期';
                    
      COMMENT ON COLUMN erp_log_shipment.tracking_no IS '运单号';
                    
      COMMENT ON COLUMN erp_log_shipment.label_url IS '面单URL';
                    
      COMMENT ON COLUMN erp_log_shipment.freight_amount IS '运费';
                    
      COMMENT ON COLUMN erp_log_shipment.freight_currency_id IS '运费币种';
                    
      COMMENT ON COLUMN erp_log_shipment.freight_terms IS '运费条款';
                    
      COMMENT ON COLUMN erp_log_shipment.freight_settlement_status IS '运费结算状态';
                    
      COMMENT ON COLUMN erp_log_shipment.total_weight IS '总重量(kg)';
                    
      COMMENT ON COLUMN erp_log_shipment.total_volume IS '总体积(m³)';
                    
      COMMENT ON COLUMN erp_log_shipment.total_parcels IS '总包裹数';
                    
      COMMENT ON COLUMN erp_log_shipment.receiver_name IS '收货人';
                    
      COMMENT ON COLUMN erp_log_shipment.receiver_phone IS '收货人电话';
                    
      COMMENT ON COLUMN erp_log_shipment.receiver_address IS '收货地址';
                    
      COMMENT ON COLUMN erp_log_shipment.receiver_country IS '国家';
                    
      COMMENT ON COLUMN erp_log_shipment.receiver_province IS '省份';
                    
      COMMENT ON COLUMN erp_log_shipment.receiver_city IS '城市';
                    
      COMMENT ON COLUMN erp_log_shipment.receiver_district IS '区县';
                    
      COMMENT ON COLUMN erp_log_shipment.sender_name IS '发货人';
                    
      COMMENT ON COLUMN erp_log_shipment.sender_phone IS '发货人电话';
                    
      COMMENT ON COLUMN erp_log_shipment.sender_address IS '发货地址';
                    
      COMMENT ON COLUMN erp_log_shipment.estimated_delivery_date IS '预计送达日期';
                    
      COMMENT ON COLUMN erp_log_shipment.actual_delivery_date IS '实际送达日期';
                    
      COMMENT ON COLUMN erp_log_shipment.signed_by IS '签收人';
                    
      COMMENT ON COLUMN erp_log_shipment.shipper_id IS '发货员(职员)';
                    
      COMMENT ON COLUMN erp_log_shipment.status IS '状态';
                    
      COMMENT ON COLUMN erp_log_shipment.remark IS '备注';
                    
      COMMENT ON COLUMN erp_log_shipment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_shipment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_shipment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_log_shipment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_shipment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_log_shipment.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_log_shipment.business_date IS '业务日期';
                    
      COMMENT ON COLUMN erp_log_shipment.posted IS '已过账';
                    
      COMMENT ON TABLE erp_log_shipment_line IS '发运明细';
                
      COMMENT ON COLUMN erp_log_shipment_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_log_shipment_line.shipment_id IS '发运单ID';
                    
      COMMENT ON COLUMN erp_log_shipment_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_log_shipment_line.material_id IS '物料';
                    
      COMMENT ON COLUMN erp_log_shipment_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_log_shipment_line.unit IS '单位';
                    
      COMMENT ON COLUMN erp_log_shipment_line.package_description IS '包装说明';
                    
      COMMENT ON COLUMN erp_log_shipment_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_log_shipment_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_shipment_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_shipment_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_log_shipment_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_shipment_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_log_shipment_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_log_shipment_parcel IS '包裹';
                
      COMMENT ON COLUMN erp_log_shipment_parcel.id IS 'ID';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.shipment_id IS '发运单ID';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.parcel_no IS '包裹编号';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.tracking_no IS '运单号';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.label_url IS '面单URL';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.weight IS '重量(kg)';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.length IS '长(cm)';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.width IS '宽(cm)';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.height IS '高(cm)';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.declared_value IS '申报价值';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.is_active IS '是否有效';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.remark IS '备注';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_log_shipment_log IS '网关交互日志';
                
      COMMENT ON COLUMN erp_log_shipment_log.id IS 'ID';
                    
      COMMENT ON COLUMN erp_log_shipment_log.shipment_id IS '发运单ID';
                    
      COMMENT ON COLUMN erp_log_shipment_log.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_shipment_log.gateway_id IS '网关标识';
                    
      COMMENT ON COLUMN erp_log_shipment_log.action_type IS '操作类型';
                    
      COMMENT ON COLUMN erp_log_shipment_log.request_body IS '请求报文';
                    
      COMMENT ON COLUMN erp_log_shipment_log.response_body IS '响应报文';
                    
      COMMENT ON COLUMN erp_log_shipment_log.http_status IS 'HTTP状态码';
                    
      COMMENT ON COLUMN erp_log_shipment_log.error_code IS '错误码';
                    
      COMMENT ON COLUMN erp_log_shipment_log.error_message IS '错误信息';
                    
      COMMENT ON COLUMN erp_log_shipment_log.is_success IS '是否成功';
                    
      COMMENT ON COLUMN erp_log_shipment_log.executed_at IS '执行时间';
                    
      COMMENT ON COLUMN erp_log_shipment_log.remark IS '备注';
                    
      COMMENT ON COLUMN erp_log_shipment_log.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_shipment_log.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_shipment_log.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_log_shipment_log.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_shipment_log.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_log_shipment_log.update_time IS '修改时间';
                    
