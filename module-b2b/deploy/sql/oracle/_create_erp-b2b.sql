
CREATE TABLE erp_md_md_organization(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_organization primary key (ID)
);

CREATE TABLE erp_md_md_partner(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_partner primary key (ID)
);

CREATE TABLE erp_md_md_material(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_material primary key (ID)
);

CREATE TABLE erp_b2b_edi_format(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  FORMAT_NAME VARCHAR2(200) NOT NULL ,
  FORMAT_STANDARD INTEGER NOT NULL ,
  DIRECTION INTEGER NOT NULL ,
  NEEDS_WEB_SERVICE INTEGER default 0  NOT NULL ,
  IS_ACTIVE INTEGER default 1  NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_edi_format primary key (ID)
);

CREATE TABLE erp_b2b_code_mapping(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  MAPPING_TYPE INTEGER NOT NULL ,
  INTERNAL_CODE VARCHAR2(100) NOT NULL ,
  EXTERNAL_CODE VARCHAR2(100) NOT NULL ,
  PARTNER_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_code_mapping primary key (ID)
);

CREATE TABLE erp_b2b_edi_doc(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  FORMAT_ID NUMBER(20)  ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  STATE INTEGER NOT NULL ,
  BLOCKING_LEVEL INTEGER default 10  NOT NULL ,
  ERROR VARCHAR2(2000)  ,
  RETRY_COUNT INTEGER default 0  NOT NULL ,
  ATTACHMENT_ID NUMBER(20)  ,
  SENT_AT DATE  ,
  ACKNOWLEDGED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_edi_doc primary key (ID)
);

CREATE TABLE erp_b2b_asn(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SOURCE_EDI_DOC_ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20)  ,
  SHIPMENT_DATE DATE  ,
  ESTIMATED_ARRIVAL_DATE DATE  ,
  TRACKING_NO VARCHAR2(100)  ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  STATUS INTEGER NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_asn primary key (ID)
);

CREATE TABLE erp_b2b_edi_log(
  ID NUMBER(20) NOT NULL ,
  EDI_DOC_ID NUMBER(20)  ,
  ORG_ID NUMBER(20)  ,
  DIRECTION INTEGER NOT NULL ,
  REQUEST_PAYLOAD CLOB  ,
  RESPONSE_PAYLOAD CLOB  ,
  RESULT_CODE VARCHAR2(50)  ,
  RESULT_MSG VARCHAR2(500)  ,
  LOG_TIME DATE NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_edi_log primary key (ID)
);

CREATE TABLE erp_b2b_asn_line(
  ID NUMBER(20) NOT NULL ,
  ASN_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20)  ,
  SUPPLIER_PART_NO VARCHAR2(100)  ,
  QUANTITY NUMBER(20,4)  ,
  SHIPPED_QTY NUMBER(20,4)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_asn_line primary key (ID)
);


      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_md_partner IS 'ErpMdPartner';
                
      COMMENT ON TABLE erp_md_md_material IS 'ErpMdMaterial';
                
      COMMENT ON TABLE erp_b2b_edi_format IS 'EDI 格式配置';
                
      COMMENT ON COLUMN erp_b2b_edi_format.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.FORMAT_NAME IS '格式名称';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.FORMAT_STANDARD IS 'EDI 标准';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.DIRECTION IS '方向';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.NEEDS_WEB_SERVICE IS '需要 WebService';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_edi_format.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_code_mapping IS '内外代码映射';
                
      COMMENT ON COLUMN erp_b2b_code_mapping.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.MAPPING_TYPE IS '映射类型';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.INTERNAL_CODE IS '内部代码';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.EXTERNAL_CODE IS '外部代码';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.PARTNER_ID IS 'Trading Partner';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_code_mapping.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_edi_doc IS 'EDI 事务';
                
      COMMENT ON COLUMN erp_b2b_edi_doc.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.CODE IS '事务编码';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.FORMAT_ID IS 'EDI 格式';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.RELATED_BILL_TYPE IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.RELATED_BILL_CODE IS '关联单据号';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.STATE IS '状态';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.BLOCKING_LEVEL IS '阻断级别';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.ERROR IS '错误信息';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.RETRY_COUNT IS '重试次数';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.ATTACHMENT_ID IS '附件（EDI 报文文件）';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.SENT_AT IS '发送时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.ACKNOWLEDGED_AT IS '确认时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_asn IS '提前发货通知';
                
      COMMENT ON COLUMN erp_b2b_asn.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_asn.CODE IS 'ASN 编码';
                    
      COMMENT ON COLUMN erp_b2b_asn.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_asn.SOURCE_EDI_DOC_ID IS '来源 EDI 文档';
                    
      COMMENT ON COLUMN erp_b2b_asn.PARTNER_ID IS '发货方（供应商）';
                    
      COMMENT ON COLUMN erp_b2b_asn.SHIPMENT_DATE IS '发货日期';
                    
      COMMENT ON COLUMN erp_b2b_asn.ESTIMATED_ARRIVAL_DATE IS '预计到货日期';
                    
      COMMENT ON COLUMN erp_b2b_asn.TRACKING_NO IS '物流单号';
                    
      COMMENT ON COLUMN erp_b2b_asn.RELATED_BILL_TYPE IS '关联采购订单类型';
                    
      COMMENT ON COLUMN erp_b2b_asn.RELATED_BILL_CODE IS '关联采购订单号';
                    
      COMMENT ON COLUMN erp_b2b_asn.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_b2b_asn.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_asn.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_asn.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_asn.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_asn.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_asn.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_asn.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_edi_log IS 'EDI 交互日志';
                
      COMMENT ON COLUMN erp_b2b_edi_log.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.EDI_DOC_ID IS 'EDI 事务 ID';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.DIRECTION IS '方向';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.REQUEST_PAYLOAD IS '请求报文';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.RESPONSE_PAYLOAD IS '响应报文';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.RESULT_CODE IS '结果码';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.RESULT_MSG IS '结果消息';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.LOG_TIME IS '日志时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_edi_log.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_asn_line IS 'ASN 明细行';
                
      COMMENT ON COLUMN erp_b2b_asn_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.ASN_ID IS 'ASN ID';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.SUPPLIER_PART_NO IS '供应商料号';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.QUANTITY IS '订单数量';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.SHIPPED_QTY IS '发货数量';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_asn_line.UPDATE_TIME IS '修改时间';
                    
