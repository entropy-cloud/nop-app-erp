
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

CREATE TABLE erp_b2b_partner_profile(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20)  ,
  PARTNER_NAME VARCHAR2(200) NOT NULL ,
  STATUS INTEGER NOT NULL ,
  PROTOCOL INTEGER NOT NULL ,
  TRANSPORT_ENDPOINT VARCHAR2(500)  ,
  AUTH_METHOD INTEGER NOT NULL ,
  WEBHOOK_SECRET VARCHAR2(500)  ,
  CERT_EXPIRY DATE  ,
  CERT_FINGERPRINT VARCHAR2(200)  ,
  ALLOWED_FORMATS VARCHAR2(500)  ,
  TIMEZONE VARCHAR2(50)  ,
  CONTACT_NAME VARCHAR2(100)  ,
  CONTACT_EMAIL VARCHAR2(200)  ,
  CONTACT_PHONE VARCHAR2(50)  ,
  NOTES VARCHAR2(1000)  ,
  GO_LIVE_DATE DATE  ,
  ARCHIVED_AT DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_partner_profile primary key (ID)
);

CREATE TABLE erp_b2b_mft_certificate(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20)  ,
  CERT_NAME VARCHAR2(200) NOT NULL ,
  CERT_TYPE VARCHAR2(50) NOT NULL ,
  ALGORITHM VARCHAR2(50) NOT NULL ,
  KEY_SIZE INTEGER default 2048   ,
  ISSUER_NAME VARCHAR2(200)  ,
  SUBJECT_NAME VARCHAR2(200)  ,
  SERIAL_NO VARCHAR2(100)  ,
  FINGERPRINT_SHA256 VARCHAR2(100)  ,
  ISSUED_AT DATE  ,
  EXPIRES_AT DATE NOT NULL ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_mft_certificate primary key (ID)
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
  ATTACHMENT_FILE_ID VARCHAR2(200)  ,
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

CREATE TABLE erp_b2b_partner_credential(
  ID NUMBER(20) NOT NULL ,
  PARTNER_PROFILE_ID NUMBER(20) NOT NULL ,
  CREDENTIAL_TYPE INTEGER NOT NULL ,
  CREDENTIAL_KEY VARCHAR2(200)  ,
  CREDENTIAL_VALUE VARCHAR2(2000)  ,
  ISSUED_AT DATE  ,
  EXPIRES_AT DATE  ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_partner_credential primary key (ID)
);

CREATE TABLE erp_b2b_test_exchange(
  ID NUMBER(20) NOT NULL ,
  PARTNER_PROFILE_ID NUMBER(20) NOT NULL ,
  DIRECTION INTEGER NOT NULL ,
  FORMAT_CODE VARCHAR2(50) NOT NULL ,
  TEST_CASE_CODE VARCHAR2(50)  ,
  SENT_PAYLOAD CLOB  ,
  RECEIVED_PAYLOAD CLOB  ,
  EXPECTED_RESULT VARCHAR2(500)  ,
  ACTUAL_RESULT VARCHAR2(500)  ,
  PASSED CHAR(1)  ,
  TESTED_BY VARCHAR2(50)  ,
  TESTED_AT DATE  ,
  NOTES VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_test_exchange primary key (ID)
);

CREATE TABLE erp_b2b_certification_checklist(
  ID NUMBER(20) NOT NULL ,
  PARTNER_PROFILE_ID NUMBER(20) NOT NULL ,
  CHECKLIST_ITEM VARCHAR2(500) NOT NULL ,
  REQUIRED_DOC_TYPE VARCHAR2(100)  ,
  IS_MANDATORY CHAR(1) default 1   ,
  IS_PASSED CHAR(1)  ,
  CHECKED_BY VARCHAR2(50)  ,
  CHECKED_AT DATE  ,
  EVIDENCE VARCHAR2(1000)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_certification_checklist primary key (ID)
);

CREATE TABLE erp_b2b_mft_config(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  PROTOCOL VARCHAR2(50) NOT NULL ,
  TRANSPORT_ENDPOINT VARCHAR2(500) NOT NULL ,
  LOCAL_AS2_ID VARCHAR2(100)  ,
  REMOTE_AS2_ID VARCHAR2(100)  ,
  SFTP_USERNAME VARCHAR2(100)  ,
  SFTP_PORT INTEGER default 22   ,
  FTPS_PORT INTEGER default 990   ,
  FTPS_IMPLICIT_TLS CHAR(1) default 1   ,
  COMPRESSION CHAR(1) default 0   ,
  ENCRYPTION CHAR(1) default 0   ,
  ENCRYPTION_ALGO VARCHAR2(50)  ,
  SIGNATURE CHAR(1) default 0   ,
  SIGNATURE_ALGO VARCHAR2(50)  ,
  CERT_ID NUMBER(20)  ,
  ACTIVE CHAR(1) default 1   ,
  MAX_RETRIES INTEGER default 3   ,
  RETRY_INTERVAL_MIN INTEGER default 15   ,
  DEAD_LETTER_ENABLED CHAR(1) default 1   ,
  MONITOR_DIRECTORY VARCHAR2(500)  ,
  MONITOR_INTERVAL_SEC INTEGER default 60   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_mft_config primary key (ID)
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

CREATE TABLE erp_b2b_mft_log(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONFIG_ID NUMBER(20) NOT NULL ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  DIRECTION VARCHAR2(50) NOT NULL ,
  FILE_NAME VARCHAR2(500)  ,
  FILE_SIZE NUMBER(20)  ,
  FILE_HASH VARCHAR2(100)  ,
  MESSAGE_ID VARCHAR2(200)  ,
  MDN_STATUS VARCHAR2(50)  ,
  PROTOCOL VARCHAR2(50) NOT NULL ,
  STATUS VARCHAR2(50) NOT NULL ,
  START_TIME DATE  ,
  END_TIME DATE  ,
  DURATION_MS NUMBER(20)  ,
  ERROR_CODE VARCHAR2(50)  ,
  ERROR_MSG VARCHAR2(1000)  ,
  RETRY_COUNT INTEGER default 0   ,
  IS_COMPRESSED CHAR(1) default 0   ,
  IS_ENCRYPTED CHAR(1) default 0   ,
  IS_SIGNED CHAR(1) default 0   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_b2b_mft_log primary key (ID)
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
                    
      COMMENT ON TABLE erp_b2b_partner_profile IS '合作伙伴档案';
                
      COMMENT ON COLUMN erp_b2b_partner_profile.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.PARTNER_ID IS '关联伙伴';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.PARTNER_NAME IS '伙伴名称';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.PROTOCOL IS '传输协议';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.TRANSPORT_ENDPOINT IS '传输端点';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.AUTH_METHOD IS '认证方式';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.WEBHOOK_SECRET IS 'Webhook密钥';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.CERT_EXPIRY IS '证书过期日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.CERT_FINGERPRINT IS '证书指纹';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.ALLOWED_FORMATS IS '支持格式(JSON)';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.TIMEZONE IS '时区';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.CONTACT_NAME IS '联系人姓名';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.CONTACT_EMAIL IS '联系邮箱';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.CONTACT_PHONE IS '联系电话';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.NOTES IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.GO_LIVE_DATE IS '上线日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.ARCHIVED_AT IS '归档日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_partner_profile.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_mft_certificate IS 'MFT 证书';
                
      COMMENT ON COLUMN erp_b2b_mft_certificate.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.PARTNER_ID IS '合作伙伴';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.CERT_NAME IS '证书名称';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.CERT_TYPE IS '证书类型';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.ALGORITHM IS '算法';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.KEY_SIZE IS '密钥长度';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.ISSUER_NAME IS '颁发者';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.SUBJECT_NAME IS '主题';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.SERIAL_NO IS '序列号';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.FINGERPRINT_SHA256 IS 'SHA-256指纹';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.ISSUED_AT IS '颁发日期';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.EXPIRES_AT IS '过期日期';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.IS_ACTIVE IS '启用';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_mft_certificate.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.ATTACHMENT_FILE_ID IS '附件（EDI 报文文件）';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.SENT_AT IS '发送时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.ACKNOWLEDGED_AT IS '确认时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_edi_doc.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_partner_credential IS '伙伴凭证';
                
      COMMENT ON COLUMN erp_b2b_partner_credential.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.PARTNER_PROFILE_ID IS '伙伴档案';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.CREDENTIAL_TYPE IS '凭证类型';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.CREDENTIAL_KEY IS '凭证标识';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.CREDENTIAL_VALUE IS '凭证值';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.ISSUED_AT IS '颁发日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.EXPIRES_AT IS '过期日期';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.IS_ACTIVE IS '启用';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_partner_credential.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_test_exchange IS '测试消息交换';
                
      COMMENT ON COLUMN erp_b2b_test_exchange.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.PARTNER_PROFILE_ID IS '伙伴档案';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.DIRECTION IS '方向';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.FORMAT_CODE IS 'EDI格式';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.TEST_CASE_CODE IS '测试用例编号';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.SENT_PAYLOAD IS '发送报文';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.RECEIVED_PAYLOAD IS '接收报文';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.EXPECTED_RESULT IS '预期结果';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.ACTUAL_RESULT IS '实际结果';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.PASSED IS '测试通过';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.TESTED_BY IS '测试人';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.TESTED_AT IS '测试时间';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.NOTES IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_test_exchange.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_certification_checklist IS '认证检查清单';
                
      COMMENT ON COLUMN erp_b2b_certification_checklist.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.PARTNER_PROFILE_ID IS '伙伴档案';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.CHECKLIST_ITEM IS '检查项';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.REQUIRED_DOC_TYPE IS '对应文档类型';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.IS_MANDATORY IS '必检';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.IS_PASSED IS '通过';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.CHECKED_BY IS '检查人';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.CHECKED_AT IS '检查时间';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.EVIDENCE IS '检查证据';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_certification_checklist.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_b2b_mft_config IS 'MFT 配置';
                
      COMMENT ON COLUMN erp_b2b_mft_config.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.PARTNER_ID IS '合作伙伴';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.PROTOCOL IS '传输协议';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.TRANSPORT_ENDPOINT IS '传输端点';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.LOCAL_AS2_ID IS '本地AS2 ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.REMOTE_AS2_ID IS '对方AS2 ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.SFTP_USERNAME IS 'SFTP用户名';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.SFTP_PORT IS 'SFTP端口';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.FTPS_PORT IS 'FTPS端口';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.FTPS_IMPLICIT_TLS IS '隐式TLS';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.COMPRESSION IS '启用压缩';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.ENCRYPTION IS '启用加密';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.ENCRYPTION_ALGO IS '加密算法';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.SIGNATURE IS '启用签名';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.SIGNATURE_ALGO IS '签名算法';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.CERT_ID IS '关联证书';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.ACTIVE IS '启用';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.MAX_RETRIES IS '最大重试次数';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.RETRY_INTERVAL_MIN IS '重试间隔(分钟)';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.DEAD_LETTER_ENABLED IS '启用死信队列';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.MONITOR_DIRECTORY IS '监控目录';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.MONITOR_INTERVAL_SEC IS '监控间隔(秒)';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_mft_config.UPDATE_TIME IS '修改时间';
                    
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
                    
      COMMENT ON TABLE erp_b2b_mft_log IS 'MFT 传输日志';
                
      COMMENT ON COLUMN erp_b2b_mft_log.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.CONFIG_ID IS 'MFT配置';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.RELATED_BILL_TYPE IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.RELATED_BILL_CODE IS '关联单据号';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.DIRECTION IS '方向';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.FILE_NAME IS '文件名';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.FILE_SIZE IS '文件大小(字节)';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.FILE_HASH IS '文件哈希';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.MESSAGE_ID IS 'AS2 Message-ID';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.MDN_STATUS IS 'MDN状态';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.PROTOCOL IS '传输协议';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.STATUS IS '传输状态';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.START_TIME IS '开始时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.END_TIME IS '结束时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.DURATION_MS IS '耗时(毫秒)';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.ERROR_CODE IS '错误码';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.ERROR_MSG IS '错误消息';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.RETRY_COUNT IS '重试次数';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.IS_COMPRESSED IS '压缩';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.IS_ENCRYPTED IS '加密';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.IS_SIGNED IS '签名';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_b2b_mft_log.UPDATE_TIME IS '修改时间';
                    
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
                    
