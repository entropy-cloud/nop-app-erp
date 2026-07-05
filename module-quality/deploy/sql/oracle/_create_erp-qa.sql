
CREATE TABLE erp_md_material(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  MATERIAL_TYPE INTEGER  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_material primary key (ID)
);

CREATE TABLE erp_md_partner(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARTNER_TYPE INTEGER  ,
  STATUS INTEGER  ,
  CREDIT_LIMIT VARCHAR2(50)  ,
  constraint PK_erp_md_partner primary key (ID)
);

CREATE TABLE erp_md_warehouse(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  WAREHOUSE_TYPE INTEGER  ,
  ORG_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_warehouse primary key (ID)
);

CREATE TABLE erp_md_employee(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_employee primary key (ID)
);

CREATE TABLE erp_md_organization(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_TYPE INTEGER  ,
  PARENT_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_organization primary key (ID)
);

CREATE TABLE erp_qa_risk_register(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  RISK_DATE DATE NOT NULL ,
  DESCRIPTION VARCHAR2(2000)  ,
  CATEGORY VARCHAR2(100)  ,
  LIKELIHOOD INTEGER NOT NULL ,
  SEVERITY INTEGER NOT NULL ,
  RISK_SCORE INTEGER  ,
  MITIGATION VARCHAR2(2000)  ,
  OWNER_ID VARCHAR2(36)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_risk_register primary key (ID)
);

CREATE TABLE erp_qa_sampling_plan(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  AQL_LEVEL VARCHAR2(20) NOT NULL ,
  LOT_SIZE_FROM NUMBER(20,4)  ,
  LOT_SIZE_TO NUMBER(20,4)  ,
  SAMPLE_SIZE NUMBER(20,4) NOT NULL ,
  ACCEPT_NUMBER INTEGER NOT NULL ,
  REJECT_NUMBER INTEGER NOT NULL ,
  IS_ACTIVE CHAR(1) default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_sampling_plan primary key (ID)
);

CREATE TABLE erp_qa_inspection_template(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  INSPECTION_TYPE VARCHAR2(20) NOT NULL ,
  MATERIAL_ID NUMBER(20)  ,
  IS_ACTIVE INTEGER default 1  NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_inspection_template primary key (ID)
);

CREATE TABLE erp_qa_quality_goal(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  DESCRIPTION VARCHAR2(2000)  ,
  TARGET_VALUE NUMBER(20,4)  ,
  CURRENT_VALUE NUMBER(20,4)  ,
  UNIT VARCHAR2(50)  ,
  RESPONSIBLE_PERSON_ID NUMBER(20)  ,
  START_DATE DATE  ,
  END_DATE DATE  ,
  STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_quality_goal primary key (ID)
);

CREATE TABLE erp_qa_review(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  REVIEW_DATE DATE NOT NULL ,
  REVIEW_TYPE VARCHAR2(20) NOT NULL ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  PARTICIPANTS VARCHAR2(500)  ,
  CONCLUSION VARCHAR2(2000)  ,
  ACTION_REQUIRED INTEGER default 0   ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  constraint PK_erp_qa_review primary key (ID)
);

CREATE TABLE erp_qa_calibration(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  INSTRUMENT_NAME VARCHAR2(200) NOT NULL ,
  INSTRUMENT_CODE VARCHAR2(100)  ,
  BUSINESS_DATE DATE NOT NULL ,
  STANDARD_REF VARCHAR2(200)  ,
  MEASURED_VALUE VARCHAR2(100)  ,
  TARGET_VALUE VARCHAR2(20)  ,
  TOLERANCE VARCHAR2(20)  ,
  RESULT VARCHAR2(20) NOT NULL ,
  NEXT_CALIBRATION_DATE DATE  ,
  CALIBRATED_BY NUMBER(20)  ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  constraint PK_erp_qa_calibration primary key (ID)
);

CREATE TABLE erp_qa_inspection(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  INSPECTION_TYPE VARCHAR2(20) NOT NULL ,
  RELATED_BILL_TYPE VARCHAR2(50)  ,
  RELATED_BILL_CODE VARCHAR2(50)  ,
  RELATED_LINE_CODE VARCHAR2(50)  ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  TEMPLATE_ID NUMBER(20)  ,
  SUPPLIER_ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50)  ,
  INSPECTION_DATE DATE NOT NULL ,
  LOT_QUANTITY NUMBER(20,4)  ,
  SAMPLE_QUANTITY NUMBER(20,4)  ,
  INSPECTOR_ID NUMBER(20)  ,
  RESULT VARCHAR2(20) NOT NULL ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  constraint PK_erp_qa_inspection primary key (ID)
);

CREATE TABLE erp_qa_inspection_template_line(
  ID NUMBER(20) NOT NULL ,
  TEMPLATE_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  PARAMETER_NAME VARCHAR2(200) NOT NULL ,
  SPEC_MIN NUMBER(20,6)  ,
  SPEC_MAX NUMBER(20,6)  ,
  UNIT VARCHAR2(50)  ,
  IS_REQUIRED INTEGER default 1  NOT NULL ,
  INSPECTION_METHOD VARCHAR2(200)  ,
  SORT_NUM INTEGER default 0   ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_inspection_template_line primary key (ID)
);

CREATE TABLE erp_qa_inspection_line(
  ID NUMBER(20) NOT NULL ,
  INSPECTION_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  PARAMETER_ID NUMBER(20)  ,
  PARAMETER_NAME VARCHAR2(200)  ,
  SPEC_MIN NUMBER(20,6)  ,
  SPEC_MAX NUMBER(20,6)  ,
  MEASURED_VALUE VARCHAR2(100)  ,
  UNIT VARCHAR2(50)  ,
  RESULT VARCHAR2(20) NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_inspection_line primary key (ID)
);

CREATE TABLE erp_qa_non_conformance(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NCR_DATE DATE NOT NULL ,
  SOURCE_TYPE VARCHAR2(50)  ,
  SOURCE_CODE VARCHAR2(50)  ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  INSPECTION_ID NUMBER(20)  ,
  QUANTITY NUMBER(20,4)  ,
  DESCRIPTION VARCHAR2(2000)  ,
  SEVERITY VARCHAR2(20) NOT NULL ,
  DISPOSITION_TYPE VARCHAR2(20)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  SUPPLIER_ID NUMBER(20)  ,
  PARAMETER_NAME VARCHAR2(200)  ,
  MEASURED_VALUE VARCHAR2(100)  ,
  SPEC_MIN NUMBER(20,6)  ,
  SPEC_MAX NUMBER(20,6)  ,
  ASSIGNED_TO NUMBER(20)  ,
  RESOLVED_BY NUMBER(20)  ,
  RESOLVED_AT DATE  ,
  RESOLUTION VARCHAR2(2000)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  RETURN_CODE VARCHAR2(50)  ,
  constraint PK_erp_qa_non_conformance primary key (ID)
);

CREATE TABLE erp_qa_action(
  ID NUMBER(20) NOT NULL ,
  NCR_ID NUMBER(20) NOT NULL ,
  ACTION_TYPE VARCHAR2(20) NOT NULL ,
  DESCRIPTION VARCHAR2(2000)  ,
  RESPONSIBLE_PERSON NUMBER(20)  ,
  DUE_DATE DATE  ,
  STATUS VARCHAR2(20) NOT NULL ,
  COMPLETED_BY NUMBER(20)  ,
  COMPLETED_AT DATE  ,
  VERIFICATION_PERSON NUMBER(20)  ,
  VERIFICATION_DATE DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_action primary key (ID)
);

CREATE TABLE erp_qa_recall(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  RECALL_NAME VARCHAR2(200) NOT NULL ,
  TRIGGER_TYPE VARCHAR2(20) NOT NULL ,
  SOURCE_NCR_ID NUMBER(20)  ,
  MATERIAL_ID NUMBER(20)  ,
  BATCH_ID NUMBER(20)  ,
  SERIAL_NO VARCHAR2(50)  ,
  ROOT_CAUSE VARCHAR2(2000)  ,
  SEVERITY_LEVEL VARCHAR2(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  NOTIFY_CUSTOMER CHAR(1) default 0   ,
  STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  APPROVED_BY VARCHAR2(36)  ,
  APPROVED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_recall primary key (ID)
);

CREATE TABLE erp_qa_recall_target(
  ID NUMBER(20) NOT NULL ,
  RECALL_ID NUMBER(20) NOT NULL ,
  PARTNER_ID NUMBER(20)  ,
  BATCH_NO VARCHAR2(50)  ,
  SERIAL_NO VARCHAR2(50)  ,
  SALES_DELIVERY_ID NUMBER(20)  ,
  SHIPPED_QTY NUMBER(20,4)  ,
  NOTIFIED_AT DATE  ,
  NOTIFIED_BY VARCHAR2(36)  ,
  RETURN_STATUS VARCHAR2(20) NOT NULL ,
  GENERATED_RETURN_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_qa_recall_target primary key (ID)
);


      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_qa_risk_register IS '风险登记';
                
      COMMENT ON COLUMN erp_qa_risk_register.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_risk_register.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_qa_risk_register.RISK_DATE IS '登记日期';
                    
      COMMENT ON COLUMN erp_qa_risk_register.DESCRIPTION IS '风险描述';
                    
      COMMENT ON COLUMN erp_qa_risk_register.CATEGORY IS '风险类别';
                    
      COMMENT ON COLUMN erp_qa_risk_register.LIKELIHOOD IS '发生可能性';
                    
      COMMENT ON COLUMN erp_qa_risk_register.SEVERITY IS '影响严重性';
                    
      COMMENT ON COLUMN erp_qa_risk_register.RISK_SCORE IS '风险评分';
                    
      COMMENT ON COLUMN erp_qa_risk_register.MITIGATION IS '缓解措施';
                    
      COMMENT ON COLUMN erp_qa_risk_register.OWNER_ID IS '责任人';
                    
      COMMENT ON COLUMN erp_qa_risk_register.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_qa_risk_register.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_risk_register.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_risk_register.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_risk_register.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_risk_register.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_risk_register.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_risk_register.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_sampling_plan IS '抽样方案';
                
      COMMENT ON COLUMN erp_qa_sampling_plan.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.AQL_LEVEL IS 'AQL 检验水平(如 II)';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.LOT_SIZE_FROM IS '批量下限';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.LOT_SIZE_TO IS '批量上限';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.SAMPLE_SIZE IS '样本量';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.ACCEPT_NUMBER IS '合格判定数(Ac)';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.REJECT_NUMBER IS '不合格判定数(Re)';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_sampling_plan.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_inspection_template IS '质检模板';
                
      COMMENT ON COLUMN erp_qa_inspection_template.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.INSPECTION_TYPE IS '检验类型';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.MATERIAL_ID IS '适用物料';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_inspection_template.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_quality_goal IS '质量目标';
                
      COMMENT ON COLUMN erp_qa_quality_goal.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.CODE IS '编码';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.NAME IS '名称';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.DESCRIPTION IS '描述';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.TARGET_VALUE IS '目标值';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.CURRENT_VALUE IS '当前值';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.UNIT IS '单位';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.RESPONSIBLE_PERSON_ID IS '责任人';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.START_DATE IS '开始日期';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.END_DATE IS '结束日期';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_quality_goal.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_review IS '质量评审';
                
      COMMENT ON COLUMN erp_qa_review.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_review.CODE IS '编号';
                    
      COMMENT ON COLUMN erp_qa_review.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_qa_review.REVIEW_DATE IS '评审日期';
                    
      COMMENT ON COLUMN erp_qa_review.REVIEW_TYPE IS '评审类型';
                    
      COMMENT ON COLUMN erp_qa_review.RELATED_BILL_TYPE IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_qa_review.RELATED_BILL_CODE IS '关联单据号';
                    
      COMMENT ON COLUMN erp_qa_review.PARTICIPANTS IS '参与人员';
                    
      COMMENT ON COLUMN erp_qa_review.CONCLUSION IS '评审结论';
                    
      COMMENT ON COLUMN erp_qa_review.ACTION_REQUIRED IS '是否需要措施';
                    
      COMMENT ON COLUMN erp_qa_review.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_qa_review.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_qa_review.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_review.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_review.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_review.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_review.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_review.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_review.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_qa_review.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_qa_review.APPROVED_AT IS '审核时间';
                    
      COMMENT ON TABLE erp_qa_calibration IS '量具校准';
                
      COMMENT ON COLUMN erp_qa_calibration.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_calibration.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_qa_calibration.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_qa_calibration.INSTRUMENT_NAME IS '量具/设备名称';
                    
      COMMENT ON COLUMN erp_qa_calibration.INSTRUMENT_CODE IS '量具编号';
                    
      COMMENT ON COLUMN erp_qa_calibration.BUSINESS_DATE IS '校准日期';
                    
      COMMENT ON COLUMN erp_qa_calibration.STANDARD_REF IS '参考标准';
                    
      COMMENT ON COLUMN erp_qa_calibration.MEASURED_VALUE IS '测量值';
                    
      COMMENT ON COLUMN erp_qa_calibration.TARGET_VALUE IS '目标值';
                    
      COMMENT ON COLUMN erp_qa_calibration.TOLERANCE IS '允差';
                    
      COMMENT ON COLUMN erp_qa_calibration.RESULT IS '校准结果';
                    
      COMMENT ON COLUMN erp_qa_calibration.NEXT_CALIBRATION_DATE IS '下次校准日期';
                    
      COMMENT ON COLUMN erp_qa_calibration.CALIBRATED_BY IS '校准人(职员)';
                    
      COMMENT ON COLUMN erp_qa_calibration.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_qa_calibration.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_qa_calibration.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_calibration.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_calibration.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_calibration.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_calibration.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_calibration.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_calibration.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_qa_calibration.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_qa_calibration.APPROVED_AT IS '审核时间';
                    
      COMMENT ON TABLE erp_qa_inspection IS '质检单';
                
      COMMENT ON COLUMN erp_qa_inspection.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_inspection.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_qa_inspection.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_qa_inspection.INSPECTION_TYPE IS '检验类型';
                    
      COMMENT ON COLUMN erp_qa_inspection.RELATED_BILL_TYPE IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_qa_inspection.RELATED_BILL_CODE IS '关联单据号';
                    
      COMMENT ON COLUMN erp_qa_inspection.RELATED_LINE_CODE IS '关联单据行号';
                    
      COMMENT ON COLUMN erp_qa_inspection.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_qa_inspection.TEMPLATE_ID IS '质检模板';
                    
      COMMENT ON COLUMN erp_qa_inspection.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_qa_inspection.WAREHOUSE_ID IS '仓库';
                    
      COMMENT ON COLUMN erp_qa_inspection.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_qa_inspection.INSPECTION_DATE IS '检验日期';
                    
      COMMENT ON COLUMN erp_qa_inspection.LOT_QUANTITY IS '批量';
                    
      COMMENT ON COLUMN erp_qa_inspection.SAMPLE_QUANTITY IS '抽样数量';
                    
      COMMENT ON COLUMN erp_qa_inspection.INSPECTOR_ID IS '检验员(职员)';
                    
      COMMENT ON COLUMN erp_qa_inspection.RESULT IS '质检结果';
                    
      COMMENT ON COLUMN erp_qa_inspection.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_qa_inspection.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_qa_inspection.POSTED IS '已过账(质量结论已回写业务单据)';
                    
      COMMENT ON COLUMN erp_qa_inspection.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_qa_inspection.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_qa_inspection.APPROVED_BY IS '审核人';
                    
      COMMENT ON COLUMN erp_qa_inspection.APPROVED_AT IS '审核时间';
                    
      COMMENT ON COLUMN erp_qa_inspection.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_inspection.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_inspection.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_inspection.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_inspection.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_inspection.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_inspection.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_qa_inspection.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON TABLE erp_qa_inspection_template_line IS '质检模板行';
                
      COMMENT ON COLUMN erp_qa_inspection_template_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.TEMPLATE_ID IS '模板ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.PARAMETER_NAME IS '检验参数名称';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.SPEC_MIN IS '规格下限';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.SPEC_MAX IS '规格上限';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.UNIT IS '计量单位';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.IS_REQUIRED IS '是否必检';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.INSPECTION_METHOD IS '检验方法';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.SORT_NUM IS '排序';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_inspection_template_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_inspection_line IS '质检单行';
                
      COMMENT ON COLUMN erp_qa_inspection_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.INSPECTION_ID IS '质检单ID';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.PARAMETER_ID IS '检验参数';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.PARAMETER_NAME IS '检验参数名称';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.SPEC_MIN IS '规格下限';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.SPEC_MAX IS '规格上限';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.MEASURED_VALUE IS '实测值';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.UNIT IS '计量单位';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.RESULT IS '行结果';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_inspection_line.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_non_conformance IS '不合格品报告';
                
      COMMENT ON COLUMN erp_qa_non_conformance.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.NCR_DATE IS '报告日期';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.SOURCE_TYPE IS '来源类型';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.SOURCE_CODE IS '来源单号';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.MATERIAL_ID IS '物料';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.INSPECTION_ID IS '质检单ID';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.QUANTITY IS '不合格数量';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.DESCRIPTION IS '问题描述';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.SEVERITY IS '严重程度';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.DISPOSITION_TYPE IS '处理决定';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.SUPPLIER_ID IS '供应商';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.PARAMETER_NAME IS '不合格参数';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.MEASURED_VALUE IS '实测值';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.SPEC_MIN IS '规格下限';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.SPEC_MAX IS '规格上限';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.ASSIGNED_TO IS '责任人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.RESOLVED_BY IS '解决人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.RESOLVED_AT IS '解决时间';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.RESOLUTION IS '解决措施';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.POSTED IS '已过账(报废处置已生成凭证)';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_qa_non_conformance.RETURN_CODE IS '关联退货单号(RETURN 处置编排退货域后登记)';
                    
      COMMENT ON TABLE erp_qa_action IS '纠正预防措施';
                
      COMMENT ON COLUMN erp_qa_action.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_action.NCR_ID IS '不合格品报告ID';
                    
      COMMENT ON COLUMN erp_qa_action.ACTION_TYPE IS '措施类型';
                    
      COMMENT ON COLUMN erp_qa_action.DESCRIPTION IS '措施描述';
                    
      COMMENT ON COLUMN erp_qa_action.RESPONSIBLE_PERSON IS '负责人';
                    
      COMMENT ON COLUMN erp_qa_action.DUE_DATE IS '计划完成日期';
                    
      COMMENT ON COLUMN erp_qa_action.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_qa_action.COMPLETED_BY IS '完成人';
                    
      COMMENT ON COLUMN erp_qa_action.COMPLETED_AT IS '完成时间';
                    
      COMMENT ON COLUMN erp_qa_action.VERIFICATION_PERSON IS '验证人';
                    
      COMMENT ON COLUMN erp_qa_action.VERIFICATION_DATE IS '验证日期';
                    
      COMMENT ON COLUMN erp_qa_action.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_action.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_action.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_action.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_action.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_action.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_action.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_recall IS '召回事件';
                
      COMMENT ON COLUMN erp_qa_recall.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_recall.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_qa_recall.RECALL_NAME IS '召回名称';
                    
      COMMENT ON COLUMN erp_qa_recall.TRIGGER_TYPE IS '触发类型';
                    
      COMMENT ON COLUMN erp_qa_recall.SOURCE_NCR_ID IS '来源NCR(弱指针)';
                    
      COMMENT ON COLUMN erp_qa_recall.MATERIAL_ID IS '召回物料';
                    
      COMMENT ON COLUMN erp_qa_recall.BATCH_ID IS '召回批次(弱指针→ErpInvBatch)';
                    
      COMMENT ON COLUMN erp_qa_recall.SERIAL_NO IS '召回序列号';
                    
      COMMENT ON COLUMN erp_qa_recall.ROOT_CAUSE IS '根本原因';
                    
      COMMENT ON COLUMN erp_qa_recall.SEVERITY_LEVEL IS '严重程度';
                    
      COMMENT ON COLUMN erp_qa_recall.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_qa_recall.NOTIFY_CUSTOMER IS '已通知客户';
                    
      COMMENT ON COLUMN erp_qa_recall.STATUS IS '召回状态';
                    
      COMMENT ON COLUMN erp_qa_recall.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_qa_recall.APPROVED_BY IS '审批人';
                    
      COMMENT ON COLUMN erp_qa_recall.APPROVED_AT IS '审批时间';
                    
      COMMENT ON COLUMN erp_qa_recall.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_recall.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_recall.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_recall.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_recall.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_recall.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_recall.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_qa_recall_target IS '召回目标';
                
      COMMENT ON COLUMN erp_qa_recall_target.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_qa_recall_target.RECALL_ID IS '召回事件';
                    
      COMMENT ON COLUMN erp_qa_recall_target.PARTNER_ID IS '受影响客户';
                    
      COMMENT ON COLUMN erp_qa_recall_target.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_qa_recall_target.SERIAL_NO IS '序列号';
                    
      COMMENT ON COLUMN erp_qa_recall_target.SALES_DELIVERY_ID IS '销售出库单(弱指针)';
                    
      COMMENT ON COLUMN erp_qa_recall_target.SHIPPED_QTY IS '发货数量';
                    
      COMMENT ON COLUMN erp_qa_recall_target.NOTIFIED_AT IS '通知时间';
                    
      COMMENT ON COLUMN erp_qa_recall_target.NOTIFIED_BY IS '通知人';
                    
      COMMENT ON COLUMN erp_qa_recall_target.RETURN_STATUS IS '退货状态';
                    
      COMMENT ON COLUMN erp_qa_recall_target.GENERATED_RETURN_ID IS '已生成退货单(弱指针)';
                    
      COMMENT ON COLUMN erp_qa_recall_target.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_qa_recall_target.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_qa_recall_target.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_qa_recall_target.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_qa_recall_target.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_qa_recall_target.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_qa_recall_target.UPDATE_TIME IS '修改时间';
                    
