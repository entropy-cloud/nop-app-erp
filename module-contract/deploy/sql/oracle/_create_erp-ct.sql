
CREATE TABLE erp_md_md_partner(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_partner primary key (ID)
);

CREATE TABLE erp_ct_template(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  CONTRACT_TYPE VARCHAR2(20) NOT NULL ,
  CONTENT_TEMPLATE VARCHAR2(4000)  ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_template primary key (ID)
);

CREATE TABLE erp_md_md_organization(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_organization primary key (ID)
);

CREATE TABLE erp_md_currency(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_currency primary key (ID)
);

CREATE TABLE erp_md_material(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_md_material primary key (ID)
);

CREATE TABLE erp_ct_approval_matrix(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  MIN_AMOUNT NUMBER(18,2)  ,
  MAX_AMOUNT NUMBER(18,2)  ,
  APPROVER_ROLE VARCHAR2(50) NOT NULL ,
  APPROVAL_ORDER INTEGER NOT NULL ,
  CONTRACT_TYPE VARCHAR2(50)  ,
  ALLOW_SKIP CHAR(1) default 0   ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_approval_matrix primary key (ID)
);

CREATE TABLE erp_ct_contract(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONTRACT_NAME VARCHAR2(200) NOT NULL ,
  CONTRACT_TYPE VARCHAR2(20) NOT NULL ,
  CONTRACT_DIRECTION VARCHAR2(20) NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  TOTAL_AMOUNT NUMBER(20,4)  ,
  START_DATE DATE NOT NULL ,
  END_DATE DATE NOT NULL ,
  SIGN_DATE DATE  ,
  STATUS VARCHAR2(20) NOT NULL ,
  TEMPLATE_ID NUMBER(20)  ,
  PARENT_CONTRACT_ID NUMBER(20)  ,
  DESCRIPTION VARCHAR2(4000)  ,
  ATTACHMENT_FILE_ID VARCHAR2(200)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_contract primary key (ID)
);

CREATE TABLE erp_ct_contract_line(
  ID NUMBER(20) NOT NULL ,
  CONTRACT_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20)  ,
  DESCRIPTION VARCHAR2(500)  ,
  QUANTITY NUMBER(20,4)  ,
  UNIT_PRICE NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_contract_line primary key (ID)
);

CREATE TABLE erp_ct_contract_version(
  ID NUMBER(20) NOT NULL ,
  CONTRACT_ID NUMBER(20) NOT NULL ,
  VERSION_NO INTEGER NOT NULL ,
  VERSION_DATE DATE NOT NULL ,
  CONTENT VARCHAR2(4000)  ,
  ATTACHMENT_FILE_ID VARCHAR2(200)  ,
  IS_CURRENT CHAR(1) default 0   ,
  STATUS VARCHAR2(20) NOT NULL ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_contract_version primary key (ID)
);

CREATE TABLE erp_ct_approval_record(
  ID NUMBER(20) NOT NULL ,
  CONTRACT_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  APPROVAL_MATRIX_ID NUMBER(20)  ,
  APPROVAL_ORDER INTEGER  ,
  APPROVER_ID VARCHAR2(36)  ,
  APPROVAL_STATUS VARCHAR2(20) NOT NULL ,
  "COMMENT" VARCHAR2(1000)  ,
  APPROVED_AT DATE  ,
  REJECTED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_approval_record primary key (ID)
);

CREATE TABLE erp_ct_rebate_agreement(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONTRACT_ID NUMBER(20)  ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  REBATE_TYPE VARCHAR2(20) NOT NULL ,
  AGREEMENT_DATE DATE  ,
  START_DATE DATE NOT NULL ,
  END_DATE DATE NOT NULL ,
  ACCRUAL_METHOD VARCHAR2(20) NOT NULL ,
  STATUS VARCHAR2(20) NOT NULL ,
  TOTAL_ACCUMULATED_AMOUNT NUMBER(18,2)  ,
  ESTIMATED_REBATE_AMOUNT NUMBER(18,2)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_rebate_agreement primary key (ID)
);

CREATE TABLE erp_ct_document(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONTRACT_ID NUMBER(20)  ,
  CODE VARCHAR2(50) NOT NULL ,
  DOC_NAME VARCHAR2(200) NOT NULL ,
  DOC_TYPE VARCHAR2(50) NOT NULL ,
  ATTACHMENT_FILE_ID VARCHAR2(200)  ,
  FILE_SIZE NUMBER(20)  ,
  FILE_HASH VARCHAR2(100)  ,
  MIME_TYPE VARCHAR2(100)  ,
  OCR_TEXT VARCHAR2(4000)  ,
  OCR_STATUS VARCHAR2(50)  ,
  FULL_TEXT_SEARCH VARCHAR2(4000)  ,
  METADATA_TAGS VARCHAR2(2000)  ,
  RETENTION_DATE DATE  ,
  ARCHIVE_DATE DATE  ,
  PURGE_DATE DATE  ,
  IS_ARCHIVED CHAR(1) default 0   ,
  VERSION_NO INTEGER default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_document primary key (ID)
);

CREATE TABLE erp_ct_invoice_plan(
  ID NUMBER(20) NOT NULL ,
  CONTRACT_LINE_ID NUMBER(20) NOT NULL ,
  PLAN_DATE DATE  ,
  AMOUNT NUMBER(20,4)  ,
  IS_INVOICED CHAR(1) default 0   ,
  INVOICE_BILL_CODE VARCHAR2(50)  ,
  INVOICE_DATE DATE  ,
  INVOICE_TERM VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_invoice_plan primary key (ID)
);

CREATE TABLE erp_ct_consumption_line(
  ID NUMBER(20) NOT NULL ,
  CONTRACT_LINE_ID NUMBER(20) NOT NULL ,
  CONSUMPTION_DATE DATE NOT NULL ,
  QUANTITY NUMBER(20,4)  ,
  UNIT_PRICE NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  SOURCE_BILL_TYPE VARCHAR2(50)  ,
  SOURCE_BILL_CODE VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_consumption_line primary key (ID)
);

CREATE TABLE erp_ct_volume_discount(
  ID NUMBER(20) NOT NULL ,
  CONTRACT_LINE_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  FROM_QTY NUMBER(20,4) NOT NULL ,
  TO_QTY NUMBER(20,4)  ,
  DISCOUNT_PERCENT NUMBER(10,2) NOT NULL ,
  UNIT_PRICE NUMBER(20,4)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_volume_discount primary key (ID)
);

CREATE TABLE erp_ct_signature_request(
  ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONTRACT_VERSION_ID NUMBER(20) NOT NULL ,
  PROVIDER VARCHAR2(50) NOT NULL ,
  PROVIDER_REQUEST_ID VARCHAR2(200)  ,
  STATUS VARCHAR2(50) NOT NULL ,
  SIGNERS VARCHAR2(2000)  ,
  SIGNING_DEADLINE DATE  ,
  COMPLETED_AT DATE  ,
  CERTIFICATE_URL VARCHAR2(1000)  ,
  EVIDENCE_NO VARCHAR2(200)  ,
  ATTACHMENT_FILE_ID VARCHAR2(200)  ,
  ERROR_MSG VARCHAR2(1000)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_signature_request primary key (ID)
);

CREATE TABLE erp_ct_rebate_tier(
  ID NUMBER(20) NOT NULL ,
  REBATE_AGREEMENT_ID NUMBER(20) NOT NULL ,
  FROM_AMOUNT NUMBER(18,2) NOT NULL ,
  TO_AMOUNT NUMBER(18,2)  ,
  REBATE_PERCENT NUMBER(10,2)  ,
  REBATE_AMOUNT NUMBER(18,2)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_rebate_tier primary key (ID)
);

CREATE TABLE erp_ct_rebate_accrual(
  ID NUMBER(20) NOT NULL ,
  REBATE_AGREEMENT_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SOURCE_BILL_TYPE VARCHAR2(50)  ,
  SOURCE_BILL_CODE VARCHAR2(50)  ,
  BILL_AMOUNT_SOURCE NUMBER(18,2)  ,
  ACCRUED_REBATE NUMBER(18,2)  ,
  ACCRUAL_DATE DATE  ,
  IS_SETTLED CHAR(1) default 0   ,
  SETTLED_DATE DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_rebate_accrual primary key (ID)
);

CREATE TABLE erp_ct_rebate_settlement(
  ID NUMBER(20) NOT NULL ,
  REBATE_AGREEMENT_ID NUMBER(20) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  SETTLEMENT_DATE DATE NOT NULL ,
  TOTAL_REBATE_AMOUNT NUMBER(18,2)  ,
  CREDIT_MEMO_BILL_TYPE VARCHAR2(50)  ,
  CREDIT_MEMO_BILL_CODE VARCHAR2(50)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_ct_rebate_settlement primary key (ID)
);


      COMMENT ON TABLE erp_md_md_partner IS 'ErpMdPartner';
                
      COMMENT ON TABLE erp_ct_template IS '合同模板';
                
      COMMENT ON COLUMN erp_ct_template.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_template.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_ct_template.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_ct_template.CONTRACT_TYPE IS '适用合同类型';
                    
      COMMENT ON COLUMN erp_ct_template.CONTENT_TEMPLATE IS '模板内容';
                    
      COMMENT ON COLUMN erp_ct_template.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_ct_template.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_template.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_template.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_template.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_template.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_template.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_template.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_md_md_organization IS 'ErpMdOrganization';
                
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_ct_approval_matrix IS '审批矩阵';
                
      COMMENT ON COLUMN erp_ct_approval_matrix.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.MIN_AMOUNT IS '最小金额';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.MAX_AMOUNT IS '最大金额';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.APPROVER_ROLE IS '审批角色';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.APPROVAL_ORDER IS '审批顺序';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.CONTRACT_TYPE IS '适用合同类型';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.ALLOW_SKIP IS '可跳过';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.IS_ACTIVE IS '启用';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_approval_matrix.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_contract IS '合同';
                
      COMMENT ON COLUMN erp_ct_contract.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_contract.CODE IS '合同编号';
                    
      COMMENT ON COLUMN erp_ct_contract.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_contract.CONTRACT_NAME IS '合同名称';
                    
      COMMENT ON COLUMN erp_ct_contract.CONTRACT_TYPE IS '合同类型';
                    
      COMMENT ON COLUMN erp_ct_contract.CONTRACT_DIRECTION IS '合同方向';
                    
      COMMENT ON COLUMN erp_ct_contract.PARTNER_ID IS '合作方';
                    
      COMMENT ON COLUMN erp_ct_contract.CURRENCY_ID IS '币种';
                    
      COMMENT ON COLUMN erp_ct_contract.TOTAL_AMOUNT IS '合同总额';
                    
      COMMENT ON COLUMN erp_ct_contract.START_DATE IS '生效日期';
                    
      COMMENT ON COLUMN erp_ct_contract.END_DATE IS '到期日期';
                    
      COMMENT ON COLUMN erp_ct_contract.SIGN_DATE IS '签署日期';
                    
      COMMENT ON COLUMN erp_ct_contract.STATUS IS '合同状态';
                    
      COMMENT ON COLUMN erp_ct_contract.TEMPLATE_ID IS '合同模板';
                    
      COMMENT ON COLUMN erp_ct_contract.PARENT_CONTRACT_ID IS '父合同';
                    
      COMMENT ON COLUMN erp_ct_contract.DESCRIPTION IS '描述';
                    
      COMMENT ON COLUMN erp_ct_contract.ATTACHMENT_FILE_ID IS '合同附件';
                    
      COMMENT ON COLUMN erp_ct_contract.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_contract.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_contract.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_contract.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_contract.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_contract.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_contract.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_contract_line IS '合同行';
                
      COMMENT ON COLUMN erp_ct_contract_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_contract_line.CONTRACT_ID IS '合同ID';
                    
      COMMENT ON COLUMN erp_ct_contract_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_ct_contract_line.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_ct_contract_line.DESCRIPTION IS '行描述';
                    
      COMMENT ON COLUMN erp_ct_contract_line.QUANTITY IS '数量';
                    
      COMMENT ON COLUMN erp_ct_contract_line.UNIT_PRICE IS '单价';
                    
      COMMENT ON COLUMN erp_ct_contract_line.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_ct_contract_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_contract_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_contract_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_contract_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_contract_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_contract_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_contract_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_contract_version IS '合同版本';
                
      COMMENT ON COLUMN erp_ct_contract_version.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_contract_version.CONTRACT_ID IS '合同ID';
                    
      COMMENT ON COLUMN erp_ct_contract_version.VERSION_NO IS '版本号';
                    
      COMMENT ON COLUMN erp_ct_contract_version.VERSION_DATE IS '版本日期';
                    
      COMMENT ON COLUMN erp_ct_contract_version.CONTENT IS '版本内容';
                    
      COMMENT ON COLUMN erp_ct_contract_version.ATTACHMENT_FILE_ID IS '版本附件';
                    
      COMMENT ON COLUMN erp_ct_contract_version.IS_CURRENT IS '是否当前版本';
                    
      COMMENT ON COLUMN erp_ct_contract_version.STATUS IS '版本状态';
                    
      COMMENT ON COLUMN erp_ct_contract_version.APPROVED_BY IS '批准人';
                    
      COMMENT ON COLUMN erp_ct_contract_version.APPROVED_AT IS '批准时间';
                    
      COMMENT ON COLUMN erp_ct_contract_version.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_contract_version.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_contract_version.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_contract_version.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_contract_version.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_contract_version.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_contract_version.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_approval_record IS '审批记录';
                
      COMMENT ON COLUMN erp_ct_approval_record.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_approval_record.CONTRACT_ID IS '合同';
                    
      COMMENT ON COLUMN erp_ct_approval_record.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_approval_record.APPROVAL_MATRIX_ID IS '审批矩阵';
                    
      COMMENT ON COLUMN erp_ct_approval_record.APPROVAL_ORDER IS '顺序号';
                    
      COMMENT ON COLUMN erp_ct_approval_record.APPROVER_ID IS '审批人';
                    
      COMMENT ON COLUMN erp_ct_approval_record.APPROVAL_STATUS IS '审批状态';
                    
      COMMENT ON COLUMN erp_ct_approval_record."COMMENT" IS '审批意见';
                    
      COMMENT ON COLUMN erp_ct_approval_record.APPROVED_AT IS '通过时间';
                    
      COMMENT ON COLUMN erp_ct_approval_record.REJECTED_AT IS '驳回时间';
                    
      COMMENT ON COLUMN erp_ct_approval_record.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_approval_record.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_approval_record.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_approval_record.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_approval_record.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_approval_record.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_approval_record.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_rebate_agreement IS '返利协议';
                
      COMMENT ON COLUMN erp_ct_rebate_agreement.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.CODE IS '协议编号';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.CONTRACT_ID IS '关联合同';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.PARTNER_ID IS '对方伙伴';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.REBATE_TYPE IS '返利类型';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.AGREEMENT_DATE IS '签订日期';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.START_DATE IS '有效期开始';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.END_DATE IS '有效期结束';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.ACCRUAL_METHOD IS '计提方法';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.TOTAL_ACCUMULATED_AMOUNT IS '当前累计金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.ESTIMATED_REBATE_AMOUNT IS '预估返利金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_rebate_agreement.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_document IS '合同文档';
                
      COMMENT ON COLUMN erp_ct_document.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_document.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_document.CONTRACT_ID IS '关联合同';
                    
      COMMENT ON COLUMN erp_ct_document.CODE IS '文档编码';
                    
      COMMENT ON COLUMN erp_ct_document.DOC_NAME IS '文档名称';
                    
      COMMENT ON COLUMN erp_ct_document.DOC_TYPE IS '文档类型';
                    
      COMMENT ON COLUMN erp_ct_document.ATTACHMENT_FILE_ID IS '附件';
                    
      COMMENT ON COLUMN erp_ct_document.FILE_SIZE IS '文件大小(字节)';
                    
      COMMENT ON COLUMN erp_ct_document.FILE_HASH IS '文件哈希';
                    
      COMMENT ON COLUMN erp_ct_document.MIME_TYPE IS 'MIME类型';
                    
      COMMENT ON COLUMN erp_ct_document.OCR_TEXT IS 'OCR文本';
                    
      COMMENT ON COLUMN erp_ct_document.OCR_STATUS IS 'OCR状态';
                    
      COMMENT ON COLUMN erp_ct_document.FULL_TEXT_SEARCH IS '全文检索';
                    
      COMMENT ON COLUMN erp_ct_document.METADATA_TAGS IS '元数据标签(JSON)';
                    
      COMMENT ON COLUMN erp_ct_document.RETENTION_DATE IS '保留截止日期';
                    
      COMMENT ON COLUMN erp_ct_document.ARCHIVE_DATE IS '归档日期';
                    
      COMMENT ON COLUMN erp_ct_document.PURGE_DATE IS '销毁日期';
                    
      COMMENT ON COLUMN erp_ct_document.IS_ARCHIVED IS '已归档';
                    
      COMMENT ON COLUMN erp_ct_document.VERSION_NO IS '文档版本';
                    
      COMMENT ON COLUMN erp_ct_document.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_document.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_document.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_document.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_document.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_document.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_document.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_invoice_plan IS '开票计划';
                
      COMMENT ON COLUMN erp_ct_invoice_plan.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.CONTRACT_LINE_ID IS '合同行ID';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.PLAN_DATE IS '计划开票日期';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.AMOUNT IS '开票金额';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.IS_INVOICED IS '是否已开票';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.INVOICE_BILL_CODE IS '关联发票号';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.INVOICE_DATE IS '实际开票日期';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.INVOICE_TERM IS '开票条款';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_invoice_plan.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_consumption_line IS '消耗计费行';
                
      COMMENT ON COLUMN erp_ct_consumption_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.CONTRACT_LINE_ID IS '合同行ID';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.CONSUMPTION_DATE IS '消耗日期';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.QUANTITY IS '消耗数量';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.UNIT_PRICE IS '单价';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.SOURCE_BILL_TYPE IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.SOURCE_BILL_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_consumption_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_volume_discount IS '批量折扣';
                
      COMMENT ON COLUMN erp_ct_volume_discount.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.CONTRACT_LINE_ID IS '合同行';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.FROM_QTY IS '起始数量';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.TO_QTY IS '截止数量';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.DISCOUNT_PERCENT IS '折扣百分比';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.UNIT_PRICE IS '覆盖单价';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_volume_discount.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_signature_request IS '签章请求';
                
      COMMENT ON COLUMN erp_ct_signature_request.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_signature_request.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_signature_request.CONTRACT_VERSION_ID IS '合同版本';
                    
      COMMENT ON COLUMN erp_ct_signature_request.PROVIDER IS '签名提供商';
                    
      COMMENT ON COLUMN erp_ct_signature_request.PROVIDER_REQUEST_ID IS '提供商请求ID';
                    
      COMMENT ON COLUMN erp_ct_signature_request.STATUS IS '签章状态';
                    
      COMMENT ON COLUMN erp_ct_signature_request.SIGNERS IS '签署人(JSON)';
                    
      COMMENT ON COLUMN erp_ct_signature_request.SIGNING_DEADLINE IS '签署截止日期';
                    
      COMMENT ON COLUMN erp_ct_signature_request.COMPLETED_AT IS '签署完成时间';
                    
      COMMENT ON COLUMN erp_ct_signature_request.CERTIFICATE_URL IS '完成证书URL';
                    
      COMMENT ON COLUMN erp_ct_signature_request.EVIDENCE_NO IS '存证编号';
                    
      COMMENT ON COLUMN erp_ct_signature_request.ATTACHMENT_FILE_ID IS '已签署文件';
                    
      COMMENT ON COLUMN erp_ct_signature_request.ERROR_MSG IS '错误信息';
                    
      COMMENT ON COLUMN erp_ct_signature_request.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_signature_request.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_signature_request.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_signature_request.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_signature_request.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_signature_request.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_signature_request.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_rebate_tier IS '返利阶梯';
                
      COMMENT ON COLUMN erp_ct_rebate_tier.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.REBATE_AGREEMENT_ID IS '返利协议';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.FROM_AMOUNT IS '起始金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.TO_AMOUNT IS '截止金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.REBATE_PERCENT IS '返利比例';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.REBATE_AMOUNT IS '固定返利金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_rebate_tier.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_rebate_accrual IS '返利计提明细';
                
      COMMENT ON COLUMN erp_ct_rebate_accrual.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.REBATE_AGREEMENT_ID IS '返利协议';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.SOURCE_BILL_TYPE IS '来源单据类型';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.SOURCE_BILL_CODE IS '来源单据号';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.BILL_AMOUNT_SOURCE IS '单据金额(源币种)';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.ACCRUED_REBATE IS '计提返利金额';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.ACCRUAL_DATE IS '计提日期';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.IS_SETTLED IS '已结算';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.SETTLED_DATE IS '结算日期';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_rebate_accrual.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ct_rebate_settlement IS '返利结算单';
                
      COMMENT ON COLUMN erp_ct_rebate_settlement.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.REBATE_AGREEMENT_ID IS '返利协议';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.SETTLEMENT_DATE IS '结算日期';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.TOTAL_REBATE_AMOUNT IS '结算返利总额';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.CREDIT_MEMO_BILL_TYPE IS '信用单类型';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.CREDIT_MEMO_BILL_CODE IS '信用单号';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_ct_rebate_settlement.UPDATE_TIME IS '修改时间';
                    
