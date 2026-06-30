
CREATE TABLE erp_md_md_partner(
  ID NUMBER(20) NOT NULL ,
  constraint PK_erp_md_md_partner primary key (ID)
);

CREATE TABLE erp_ct_template(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  CONTRACT_TYPE INTEGER NOT NULL ,
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

CREATE TABLE erp_ct_contract(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  CONTRACT_NAME VARCHAR2(200) NOT NULL ,
  CONTRACT_TYPE INTEGER NOT NULL ,
  CONTRACT_DIRECTION INTEGER NOT NULL ,
  PARTNER_ID NUMBER(20) NOT NULL ,
  CURRENCY_ID NUMBER(20)  ,
  TOTAL_AMOUNT NUMBER(20,4)  ,
  START_DATE DATE NOT NULL ,
  END_DATE DATE NOT NULL ,
  SIGN_DATE DATE  ,
  STATUS INTEGER NOT NULL ,
  TEMPLATE_ID NUMBER(20)  ,
  PARENT_CONTRACT_ID NUMBER(20)  ,
  DESCRIPTION VARCHAR2(4000)  ,
  ATTACHMENT_ID NUMBER(20)  ,
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
  ATTACHMENT_ID NUMBER(20)  ,
  IS_CURRENT CHAR(1) default 0   ,
  STATUS INTEGER NOT NULL ,
  APPROVED_BY NUMBER(20)  ,
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

CREATE TABLE erp_ct_invoice_plan(
  ID NUMBER(20) NOT NULL ,
  CONTRACT_LINE_ID NUMBER(20) NOT NULL ,
  PLAN_DATE DATE  ,
  AMOUNT NUMBER(20,4)  ,
  IS_INVOICED CHAR(1) default 0   ,
  INVOICE_BILL_CODE VARCHAR2(50)  ,
  INVOICE_DATE DATE  ,
  INVOICE_TERM INTEGER NOT NULL ,
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
                    
      COMMENT ON COLUMN erp_ct_contract.ATTACHMENT_ID IS '合同附件';
                    
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
                    
      COMMENT ON COLUMN erp_ct_contract_version.ATTACHMENT_ID IS '版本附件';
                    
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
                    
