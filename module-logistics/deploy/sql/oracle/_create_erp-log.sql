
CREATE TABLE erp_md_md_partner(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_partner primary key (ID)
);

CREATE TABLE erp_md_md_organization(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_organization primary key (ID)
);

CREATE TABLE erp_md_md_employee(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_employee primary key (ID)
);

CREATE TABLE erp_md_currency(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_currency primary key (ID)
);

CREATE TABLE erp_md_md_material(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_material primary key (ID)
);

CREATE TABLE erp_log_carrier(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CARRIER_NAME VARCHAR2(200) NOT NULL ,
  CARRIER_TYPE VARCHAR2(20) NOT NULL ,
  GATEWAY_ID VARCHAR2(50) NOT NULL ,
  PARTNER_ID NUMBER(20)  ,
  IS_ACTIVE INTEGER default 1  NOT NULL ,
  TRACKING_URL_TEMPLATE VARCHAR2(500)  ,
  MAX_PARCEL_WEIGHT NUMBER(12,3)  ,
  SUPPORTED_SERVICE_TYPES CLOB  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_log_carrier primary key (ID)
);

CREATE TABLE erp_log_delivery_window(
  ID NUMBER(20) NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  WEEKDAY INTEGER NOT NULL ,
  START_TIME VARCHAR2(8) NOT NULL ,
  END_TIME VARCHAR2(8) NOT NULL ,
  MAX_CAPACITY INTEGER default 10  NOT NULL ,
  CURRENT_BOOKED INTEGER default 0  NOT NULL ,
  IS_ACTIVE CHAR(1) default 1   ,
  EFFECTIVE_FROM DATE  ,
  EFFECTIVE_TO DATE  ,
  ALLOWED_SHIPMENT_TYPES VARCHAR2(200)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_log_delivery_window primary key (ID)
);

CREATE TABLE erp_log_carrier_config(
  ID NUMBER(20) NOT NULL ,
  CARRIER_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONFIG_CODE VARCHAR2(50) NOT NULL ,
  SERVICE_TYPE VARCHAR2(50)  ,
  API_ENDPOINT VARCHAR2(500)  ,
  API_KEY VARCHAR2(500)  ,
  API_SECRET VARCHAR2(500)  ,
  CREDENTIALS CLOB  ,
  TRACKING_URL_TEMPLATE VARCHAR2(500)  ,
  PRINT_FORMAT VARCHAR2(50)  ,
  ADDITIONAL_PROPERTIES CLOB  ,
  IS_ACTIVE INTEGER default 1  NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_log_carrier_config primary key (ID)
);

CREATE TABLE erp_log_shipment(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CARRIER_ID NUMBER(20) NOT NULL ,
  CARRIER_CONFIG_ID NUMBER(20)  ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  SHIPMENT_DATE DATE  ,
  TRACKING_NO VARCHAR2(100)  ,
  LABEL_URL VARCHAR2(500)  ,
  FREIGHT_AMOUNT NUMBER(20,4)  ,
  FREIGHT_CURRENCY_ID NUMBER(20)  ,
  FREIGHT_TERMS VARCHAR2(20)  ,
  FREIGHT_SETTLEMENT_STATUS VARCHAR2(20)  ,
  TOTAL_WEIGHT NUMBER(12,3)  ,
  TOTAL_VOLUME NUMBER(12,6)  ,
  TOTAL_PARCELS INTEGER default 1   ,
  RECEIVER_NAME VARCHAR2(100)  ,
  RECEIVER_PHONE VARCHAR2(30)  ,
  RECEIVER_ADDRESS VARCHAR2(500)  ,
  RECEIVER_COUNTRY VARCHAR2(100)  ,
  RECEIVER_PROVINCE VARCHAR2(100)  ,
  RECEIVER_CITY VARCHAR2(100)  ,
  RECEIVER_DISTRICT VARCHAR2(100)  ,
  SENDER_NAME VARCHAR2(100)  ,
  SENDER_PHONE VARCHAR2(30)  ,
  SENDER_ADDRESS VARCHAR2(500)  ,
  ESTIMATED_DELIVERY_DATE DATE  ,
  ACTUAL_DELIVERY_DATE DATE  ,
  SIGNED_BY VARCHAR2(100)  ,
  SHIPPER_ID NUMBER(20)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_log_shipment primary key (ID)
);

CREATE TABLE erp_log_shipment_line(
  ID NUMBER(20) NOT NULL ,
  SHIPMENT_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20)  ,
  QUANTITY NUMBER(20,4)  ,
  UNIT VARCHAR2(50)  ,
  PACKAGE_DESCRIPTION VARCHAR2(500)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_log_shipment_line primary key (ID)
);

CREATE TABLE erp_log_shipment_parcel(
  ID NUMBER(20) NOT NULL ,
  SHIPMENT_ID NUMBER(20) NOT NULL ,
  PARCEL_NO VARCHAR2(50) NOT NULL ,
  TRACKING_NO VARCHAR2(100)  ,
  LABEL_URL VARCHAR2(500)  ,
  WEIGHT NUMBER(12,3)  ,
  LENGTH NUMBER(12,2)  ,
  WIDTH NUMBER(12,2)  ,
  HEIGHT NUMBER(12,2)  ,
  DECLARED_VALUE NUMBER(20,4)  ,
  IS_ACTIVE INTEGER default 1  NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_log_shipment_parcel primary key (ID)
);

CREATE TABLE erp_log_shipment_log(
  ID NUMBER(20) NOT NULL ,
  SHIPMENT_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  GATEWAY_ID VARCHAR2(50) NOT NULL ,
  ACTION_TYPE VARCHAR2(20) NOT NULL ,
  REQUEST_BODY CLOB  ,
  RESPONSE_BODY CLOB  ,
  HTTP_STATUS INTEGER  ,
  ERROR_CODE VARCHAR2(100)  ,
  ERROR_MESSAGE VARCHAR2(2000)  ,
  IS_SUCCESS CHAR(1) default 1   ,
  EXECUTED_AT DATE NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_log_shipment_log primary key (ID)
);


      COMMENT ON TABLE erp_md_md_partner IS 'ErpMdPartner';
                
      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_md_employee IS 'ErpMdEmployee';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_md_material IS 'ErpMdMaterial';
                
      COMMENT ON TABLE erp_log_carrier IS '承运商';
                
      COMMENT ON COLUMN erp_log_carrier.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_log_carrier.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_log_carrier.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_carrier.CARRIER_NAME IS '承运商名称';
                    
      COMMENT ON COLUMN erp_log_carrier.CARRIER_TYPE IS '承运商类型';
                    
      COMMENT ON COLUMN erp_log_carrier.GATEWAY_ID IS '网关标识';
                    
      COMMENT ON COLUMN erp_log_carrier.PARTNER_ID IS '承运商往来单位ID';
                    
      COMMENT ON COLUMN erp_log_carrier.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_log_carrier.TRACKING_URL_TEMPLATE IS '追踪URL模板';
                    
      COMMENT ON COLUMN erp_log_carrier.MAX_PARCEL_WEIGHT IS '最大包裹重量(kg)';
                    
      COMMENT ON COLUMN erp_log_carrier.SUPPORTED_SERVICE_TYPES IS '支持服务类型';
                    
      COMMENT ON COLUMN erp_log_carrier.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_log_carrier.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_carrier.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_carrier.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_log_carrier.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_carrier.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_log_carrier.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_log_delivery_window IS '配送时间窗口';
                
      COMMENT ON COLUMN erp_log_delivery_window.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_log_delivery_window.PARTNER_ID IS '客户';
                    
      COMMENT ON COLUMN erp_log_delivery_window.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_delivery_window.WEEKDAY IS '星期';
                    
      COMMENT ON COLUMN erp_log_delivery_window.START_TIME IS '开始时间';
                    
      COMMENT ON COLUMN erp_log_delivery_window.END_TIME IS '结束时间';
                    
      COMMENT ON COLUMN erp_log_delivery_window.MAX_CAPACITY IS '最大预约数';
                    
      COMMENT ON COLUMN erp_log_delivery_window.CURRENT_BOOKED IS '当前已预约';
                    
      COMMENT ON COLUMN erp_log_delivery_window.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_log_delivery_window.EFFECTIVE_FROM IS '生效日期';
                    
      COMMENT ON COLUMN erp_log_delivery_window.EFFECTIVE_TO IS '失效日期';
                    
      COMMENT ON COLUMN erp_log_delivery_window.ALLOWED_SHIPMENT_TYPES IS '允许发运类型';
                    
      COMMENT ON COLUMN erp_log_delivery_window.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_log_delivery_window.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_delivery_window.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_delivery_window.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_log_delivery_window.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_delivery_window.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_log_delivery_window.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_log_carrier_config IS '承运商配置';
                
      COMMENT ON COLUMN erp_log_carrier_config.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_log_carrier_config.CARRIER_ID IS '承运商ID';
                    
      COMMENT ON COLUMN erp_log_carrier_config.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_carrier_config.CONFIG_CODE IS '配置编码';
                    
      COMMENT ON COLUMN erp_log_carrier_config.SERVICE_TYPE IS '服务类型';
                    
      COMMENT ON COLUMN erp_log_carrier_config.API_ENDPOINT IS '接口地址';
                    
      COMMENT ON COLUMN erp_log_carrier_config.API_KEY IS 'API密钥';
                    
      COMMENT ON COLUMN erp_log_carrier_config.API_SECRET IS 'API密钥';
                    
      COMMENT ON COLUMN erp_log_carrier_config.CREDENTIALS IS '完整凭证';
                    
      COMMENT ON COLUMN erp_log_carrier_config.TRACKING_URL_TEMPLATE IS '追踪URL模板';
                    
      COMMENT ON COLUMN erp_log_carrier_config.PRINT_FORMAT IS '面单打印格式';
                    
      COMMENT ON COLUMN erp_log_carrier_config.ADDITIONAL_PROPERTIES IS '扩展参数';
                    
      COMMENT ON COLUMN erp_log_carrier_config.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_log_carrier_config.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_log_carrier_config.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_carrier_config.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_carrier_config.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_log_carrier_config.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_carrier_config.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_log_carrier_config.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_log_shipment IS '发运单';
                
      COMMENT ON COLUMN erp_log_shipment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_log_shipment.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_log_shipment.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_shipment.CARRIER_ID IS '承运商ID';
                    
      COMMENT ON COLUMN erp_log_shipment.CARRIER_CONFIG_ID IS '承运商配置ID';
                    
      COMMENT ON COLUMN erp_log_shipment.RELATED_BILL_TYPE IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_log_shipment.RELATED_BILL_CODE IS '关联单据号';
                    
      COMMENT ON COLUMN erp_log_shipment.SHIPMENT_DATE IS '发运日期';
                    
      COMMENT ON COLUMN erp_log_shipment.TRACKING_NO IS '运单号';
                    
      COMMENT ON COLUMN erp_log_shipment.LABEL_URL IS '面单URL';
                    
      COMMENT ON COLUMN erp_log_shipment.FREIGHT_AMOUNT IS '运费';
                    
      COMMENT ON COLUMN erp_log_shipment.FREIGHT_CURRENCY_ID IS '运费币种';
                    
      COMMENT ON COLUMN erp_log_shipment.FREIGHT_TERMS IS '运费条款';
                    
      COMMENT ON COLUMN erp_log_shipment.FREIGHT_SETTLEMENT_STATUS IS '运费结算状态';
                    
      COMMENT ON COLUMN erp_log_shipment.TOTAL_WEIGHT IS '总重量(kg)';
                    
      COMMENT ON COLUMN erp_log_shipment.TOTAL_VOLUME IS '总体积(m³)';
                    
      COMMENT ON COLUMN erp_log_shipment.TOTAL_PARCELS IS '总包裹数';
                    
      COMMENT ON COLUMN erp_log_shipment.RECEIVER_NAME IS '收货人';
                    
      COMMENT ON COLUMN erp_log_shipment.RECEIVER_PHONE IS '收货人电话';
                    
      COMMENT ON COLUMN erp_log_shipment.RECEIVER_ADDRESS IS '收货地址';
                    
      COMMENT ON COLUMN erp_log_shipment.RECEIVER_COUNTRY IS '国家';
                    
      COMMENT ON COLUMN erp_log_shipment.RECEIVER_PROVINCE IS '省份';
                    
      COMMENT ON COLUMN erp_log_shipment.RECEIVER_CITY IS '城市';
                    
      COMMENT ON COLUMN erp_log_shipment.RECEIVER_DISTRICT IS '区县';
                    
      COMMENT ON COLUMN erp_log_shipment.SENDER_NAME IS '发货人';
                    
      COMMENT ON COLUMN erp_log_shipment.SENDER_PHONE IS '发货人电话';
                    
      COMMENT ON COLUMN erp_log_shipment.SENDER_ADDRESS IS '发货地址';
                    
      COMMENT ON COLUMN erp_log_shipment.ESTIMATED_DELIVERY_DATE IS '预计送达日期';
                    
      COMMENT ON COLUMN erp_log_shipment.ACTUAL_DELIVERY_DATE IS '实际送达日期';
                    
      COMMENT ON COLUMN erp_log_shipment.SIGNED_BY IS '签收人';
                    
      COMMENT ON COLUMN erp_log_shipment.SHIPPER_ID IS '发货员(职员)';
                    
      COMMENT ON COLUMN erp_log_shipment.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_log_shipment.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_log_shipment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_shipment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_shipment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_log_shipment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_shipment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_log_shipment.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_log_shipment_line IS '发运明细';
                
      COMMENT ON COLUMN erp_log_shipment_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_log_shipment_line.SHIPMENT_ID IS '发运单ID';
                    
      COMMENT ON COLUMN erp_log_shipment_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_log_shipment_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_log_shipment_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_log_shipment_line.UNIT IS '单位';
                    
      COMMENT ON COLUMN erp_log_shipment_line.PACKAGE_DESCRIPTION IS '包装说明';
                    
      COMMENT ON COLUMN erp_log_shipment_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_log_shipment_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_shipment_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_shipment_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_log_shipment_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_shipment_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_log_shipment_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_log_shipment_parcel IS '包裹';
                
      COMMENT ON COLUMN erp_log_shipment_parcel.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.SHIPMENT_ID IS '发运单ID';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.PARCEL_NO IS '包裹编号';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.TRACKING_NO IS '运单号';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.LABEL_URL IS '面单URL';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.WEIGHT IS '重量(kg)';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.LENGTH IS '长(cm)';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.WIDTH IS '宽(cm)';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.HEIGHT IS '高(cm)';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.DECLARED_VALUE IS '申报价值';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.IS_ACTIVE IS '是否有效';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_log_shipment_parcel.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_log_shipment_log IS '网关交互日志';
                
      COMMENT ON COLUMN erp_log_shipment_log.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_log_shipment_log.SHIPMENT_ID IS '发运单ID';
                    
      COMMENT ON COLUMN erp_log_shipment_log.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_log_shipment_log.GATEWAY_ID IS '网关标识';
                    
      COMMENT ON COLUMN erp_log_shipment_log.ACTION_TYPE IS '操作类型';
                    
      COMMENT ON COLUMN erp_log_shipment_log.REQUEST_BODY IS '请求报文';
                    
      COMMENT ON COLUMN erp_log_shipment_log.RESPONSE_BODY IS '响应报文';
                    
      COMMENT ON COLUMN erp_log_shipment_log.HTTP_STATUS IS 'HTTP状态码';
                    
      COMMENT ON COLUMN erp_log_shipment_log.ERROR_CODE IS '错误码';
                    
      COMMENT ON COLUMN erp_log_shipment_log.ERROR_MESSAGE IS '错误信息';
                    
      COMMENT ON COLUMN erp_log_shipment_log.IS_SUCCESS IS '是否成功';
                    
      COMMENT ON COLUMN erp_log_shipment_log.EXECUTED_AT IS '执行时间';
                    
      COMMENT ON COLUMN erp_log_shipment_log.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_log_shipment_log.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_log_shipment_log.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_log_shipment_log.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_log_shipment_log.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_log_shipment_log.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_log_shipment_log.UPDATE_TIME IS '修改时间';
                    
