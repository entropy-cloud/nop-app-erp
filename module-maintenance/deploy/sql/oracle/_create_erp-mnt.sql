
CREATE TABLE erp_md_location(
  ID NUMBER(20)  ,
  WAREHOUSE_ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARENT_ID NUMBER(20)  ,
  constraint PK_erp_md_location primary key (ID)
);

CREATE TABLE erp_mnt_equipment_category(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  PARENT_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_equipment_category primary key (ID)
);

CREATE TABLE erp_ast_asset(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  constraint PK_erp_ast_asset primary key (ID)
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

CREATE TABLE erp_md_employee(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  ORG_ID NUMBER(20)  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_employee primary key (ID)
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

CREATE TABLE erp_md_material(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  MATERIAL_TYPE INTEGER  ,
  STATUS INTEGER  ,
  constraint PK_erp_md_material primary key (ID)
);

CREATE TABLE erp_md_uom(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  UOM_GROUP VARCHAR2(50)  ,
  IS_BASE CHAR(1)  ,
  constraint PK_erp_md_uom primary key (ID)
);

CREATE TABLE erp_md_material_category(
  ID NUMBER(20)  ,
  CODE VARCHAR2(50)  ,
  NAME VARCHAR2(200)  ,
  PARENT_ID NUMBER(20)  ,
  constraint PK_erp_md_material_category primary key (ID)
);

CREATE TABLE erp_mnt_equipment(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  ASSET_ID NUMBER(20)  ,
  WORKCENTER_ID NUMBER(20)  ,
  LOCATION_ID NUMBER(20)  ,
  CATEGORY_ID NUMBER(20)  ,
  STATUS VARCHAR2(20) NOT NULL ,
  SERIAL_NO VARCHAR2(100)  ,
  MANUFACTURER VARCHAR2(200)  ,
  MODEL VARCHAR2(200)  ,
  INSTALL_DATE DATE  ,
  WARRANTY_EXPIRY DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_equipment primary key (ID)
);

CREATE TABLE erp_mnt_maintenance_team(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  LEADER_ID NUMBER(20)  ,
  DESCRIPTION VARCHAR2(2000)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_maintenance_team primary key (ID)
);

CREATE TABLE erp_mnt_schedule(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  EQUIPMENT_ID NUMBER(20) NOT NULL ,
  SCHEDULE_TYPE VARCHAR2(20) NOT NULL ,
  FREQUENCY INTEGER  ,
  RECURRENCE_TYPE VARCHAR2(20)  ,
  DAYS_OF_WEEK VARCHAR2(50)  ,
  START_DATE DATE NOT NULL ,
  END_DATE DATE  ,
  NEXT_DUE_DATE DATE  ,
  IS_ACTIVE INTEGER default 1   ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_schedule primary key (ID)
);

CREATE TABLE erp_mnt_request(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  EQUIPMENT_ID NUMBER(20) NOT NULL ,
  REQUEST_DATE DATE NOT NULL ,
  DESCRIPTION VARCHAR2(2000) NOT NULL ,
  PRIORITY VARCHAR2(20) NOT NULL ,
  STATUS VARCHAR2(20) NOT NULL ,
  REQUESTED_BY NUMBER(20) NOT NULL ,
  ASSIGNED_TO NUMBER(20)  ,
  ACCEPTED_BY NUMBER(20)  ,
  COMPLETED_BY NUMBER(20)  ,
  COMPLETED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_request primary key (ID)
);

CREATE TABLE erp_mnt_downtime_entry(
  ID NUMBER(20) NOT NULL ,
  EQUIPMENT_ID NUMBER(20) NOT NULL ,
  START_TIME DATE NOT NULL ,
  END_TIME DATE  ,
  TOTAL_MINUTES NUMBER(12,2)  ,
  REASON VARCHAR2(500)  ,
  RELATED_JOB_ORDER_ID NUMBER(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_downtime_entry primary key (ID)
);

CREATE TABLE erp_mnt_calibration(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  EQUIPMENT_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  STANDARD_REF VARCHAR2(200)  ,
  MEASURED_VALUE NUMBER(20,6)  ,
  TARGET_VALUE NUMBER(20,6)  ,
  TOLERANCE NUMBER(20,6)  ,
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
  constraint PK_erp_mnt_calibration primary key (ID)
);

CREATE TABLE erp_mnt_maintenance_team_member(
  ID NUMBER(20) NOT NULL ,
  TEAM_ID NUMBER(20) NOT NULL ,
  EMPLOYEE_ID NUMBER(20) NOT NULL ,
  ROLE VARCHAR2(50)  ,
  JOINED_AT DATE  ,
  LEFT_AT DATE  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_maintenance_team_member primary key (ID)
);

CREATE TABLE erp_mnt_visit(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  SCHEDULE_ID NUMBER(20)  ,
  EQUIPMENT_ID NUMBER(20) NOT NULL ,
  VISIT_DATE DATE NOT NULL ,
  STATUS VARCHAR2(20) NOT NULL ,
  ASSIGNED_TO NUMBER(20)  ,
  COMPLETED_BY NUMBER(20)  ,
  COMPLETED_AT DATE  ,
  START_TIME DATE  ,
  END_TIME DATE  ,
  TOTAL_MINUTES NUMBER(12,2)  ,
  VISIT_TYPE VARCHAR2(20)  ,
  RESULT VARCHAR2(20)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  ORG_ID NUMBER(20)  ,
  BUSINESS_DATE DATE  ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT TIMESTAMP  ,
  POSTED_BY VARCHAR2(50)  ,
  constraint PK_erp_mnt_visit primary key (ID)
);

CREATE TABLE erp_mnt_visit_task(
  ID NUMBER(20) NOT NULL ,
  VISIT_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  TASK_DESCRIPTION VARCHAR2(500) NOT NULL ,
  STATUS VARCHAR2(20) NOT NULL ,
  COMPLETED_BY NUMBER(20)  ,
  COMPLETED_AT DATE  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_visit_task primary key (ID)
);

CREATE TABLE erp_mnt_spare_part_usage(
  ID NUMBER(20) NOT NULL ,
  CODE VARCHAR2(50) NOT NULL ,
  ORG_ID NUMBER(20)  ,
  VISIT_ID NUMBER(20)  ,
  REQUEST_ID NUMBER(20)  ,
  EQUIPMENT_ID NUMBER(20) NOT NULL ,
  BUSINESS_DATE DATE NOT NULL ,
  WAREHOUSE_ID NUMBER(20)  ,
  TOTAL_AMOUNT NUMBER(20,4) default 0   ,
  DOC_STATUS VARCHAR2(20) NOT NULL ,
  APPROVE_STATUS VARCHAR2(20) NOT NULL ,
  POSTED CHAR(1) default 0   ,
  POSTED_AT DATE  ,
  POSTED_BY VARCHAR2(36)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_spare_part_usage primary key (ID)
);

CREATE TABLE erp_mnt_spare_part_usage_line(
  ID NUMBER(20) NOT NULL ,
  SPARE_PART_USAGE_ID NUMBER(20) NOT NULL ,
  LINE_NO INTEGER NOT NULL ,
  MATERIAL_ID NUMBER(20) NOT NULL ,
  UO_M_ID NUMBER(20) NOT NULL ,
  QUANTITY NUMBER(20,4) NOT NULL ,
  UNIT_COST NUMBER(20,4)  ,
  AMOUNT NUMBER(20,4)  ,
  BATCH_NO VARCHAR2(50)  ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_spare_part_usage_line primary key (ID)
);


      COMMENT ON TABLE erp_md_location IS '库位';
                
      COMMENT ON TABLE erp_mnt_equipment_category IS '设备分类';
                
      COMMENT ON COLUMN erp_mnt_equipment_category.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.CODE IS '分类编码';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.NAME IS '分类名称';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.PARENT_ID IS '上级分类ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_ast_asset IS '资产';
                
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_md_employee IS '职员';
                
      COMMENT ON TABLE erp_md_warehouse IS '仓库';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_md_uom IS '计量单位';
                
      COMMENT ON TABLE erp_md_material_category IS '物料分类';
                
      COMMENT ON TABLE erp_mnt_equipment IS '设备';
                
      COMMENT ON COLUMN erp_mnt_equipment.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment.CODE IS '设备编码';
                    
      COMMENT ON COLUMN erp_mnt_equipment.NAME IS '设备名称';
                    
      COMMENT ON COLUMN erp_mnt_equipment.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_mnt_equipment.ASSET_ID IS '资产卡片(关联 assets 域)';
                    
      COMMENT ON COLUMN erp_mnt_equipment.WORKCENTER_ID IS '关联工作中心(关联 mfg 域)';
                    
      COMMENT ON COLUMN erp_mnt_equipment.LOCATION_ID IS '位置ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment.CATEGORY_ID IS '分类ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_mnt_equipment.SERIAL_NO IS '序列号';
                    
      COMMENT ON COLUMN erp_mnt_equipment.MANUFACTURER IS '制造商';
                    
      COMMENT ON COLUMN erp_mnt_equipment.MODEL IS '型号';
                    
      COMMENT ON COLUMN erp_mnt_equipment.INSTALL_DATE IS '安装日期';
                    
      COMMENT ON COLUMN erp_mnt_equipment.WARRANTY_EXPIRY IS '保修到期';
                    
      COMMENT ON COLUMN erp_mnt_equipment.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_equipment.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_equipment.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_equipment.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_equipment.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_equipment.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_equipment.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_maintenance_team IS '维护团队';
                
      COMMENT ON COLUMN erp_mnt_maintenance_team.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.CODE IS '团队编码';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.NAME IS '团队名称';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.ORG_ID IS '所属组织';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.LEADER_ID IS '负责人(职员)';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.DESCRIPTION IS '描述';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_schedule IS '维护计划';
                
      COMMENT ON COLUMN erp_mnt_schedule.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_schedule.CODE IS '计划编码';
                    
      COMMENT ON COLUMN erp_mnt_schedule.NAME IS '计划名称';
                    
      COMMENT ON COLUMN erp_mnt_schedule.EQUIPMENT_ID IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_schedule.SCHEDULE_TYPE IS '计划类型';
                    
      COMMENT ON COLUMN erp_mnt_schedule.FREQUENCY IS '频率';
                    
      COMMENT ON COLUMN erp_mnt_schedule.RECURRENCE_TYPE IS '重复类型';
                    
      COMMENT ON COLUMN erp_mnt_schedule.DAYS_OF_WEEK IS '星期几';
                    
      COMMENT ON COLUMN erp_mnt_schedule.START_DATE IS '开始日期';
                    
      COMMENT ON COLUMN erp_mnt_schedule.END_DATE IS '结束日期';
                    
      COMMENT ON COLUMN erp_mnt_schedule.NEXT_DUE_DATE IS '下次到期日';
                    
      COMMENT ON COLUMN erp_mnt_schedule.IS_ACTIVE IS '是否启用';
                    
      COMMENT ON COLUMN erp_mnt_schedule.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_schedule.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_schedule.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_schedule.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_schedule.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_schedule.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_schedule.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_request IS '维护请求';
                
      COMMENT ON COLUMN erp_mnt_request.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_request.CODE IS '请求编码';
                    
      COMMENT ON COLUMN erp_mnt_request.EQUIPMENT_ID IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_request.REQUEST_DATE IS '请求日期';
                    
      COMMENT ON COLUMN erp_mnt_request.DESCRIPTION IS '问题描述';
                    
      COMMENT ON COLUMN erp_mnt_request.PRIORITY IS '优先级';
                    
      COMMENT ON COLUMN erp_mnt_request.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_mnt_request.REQUESTED_BY IS '请求人';
                    
      COMMENT ON COLUMN erp_mnt_request.ASSIGNED_TO IS '指派人';
                    
      COMMENT ON COLUMN erp_mnt_request.ACCEPTED_BY IS '受理人';
                    
      COMMENT ON COLUMN erp_mnt_request.COMPLETED_BY IS '完成人';
                    
      COMMENT ON COLUMN erp_mnt_request.COMPLETED_AT IS '完成时间';
                    
      COMMENT ON COLUMN erp_mnt_request.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_request.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_request.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_request.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_request.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_request.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_request.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_downtime_entry IS '停机记录';
                
      COMMENT ON COLUMN erp_mnt_downtime_entry.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.EQUIPMENT_ID IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.START_TIME IS '开始时间';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.END_TIME IS '结束时间';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.TOTAL_MINUTES IS '总分钟数';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.REASON IS '原因';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.RELATED_JOB_ORDER_ID IS '关联生产工单ID';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_calibration IS '校准记录';
                
      COMMENT ON COLUMN erp_mnt_calibration.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_calibration.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_mnt_calibration.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mnt_calibration.EQUIPMENT_ID IS '被校设备/量具';
                    
      COMMENT ON COLUMN erp_mnt_calibration.BUSINESS_DATE IS '校准日期';
                    
      COMMENT ON COLUMN erp_mnt_calibration.STANDARD_REF IS '参考标准';
                    
      COMMENT ON COLUMN erp_mnt_calibration.MEASURED_VALUE IS '测量值';
                    
      COMMENT ON COLUMN erp_mnt_calibration.TARGET_VALUE IS '目标值';
                    
      COMMENT ON COLUMN erp_mnt_calibration.TOLERANCE IS '允差';
                    
      COMMENT ON COLUMN erp_mnt_calibration.RESULT IS '校准结果';
                    
      COMMENT ON COLUMN erp_mnt_calibration.NEXT_CALIBRATION_DATE IS '下次校准日期';
                    
      COMMENT ON COLUMN erp_mnt_calibration.CALIBRATED_BY IS '校准人(职员)';
                    
      COMMENT ON COLUMN erp_mnt_calibration.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_mnt_calibration.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_mnt_calibration.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_calibration.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_calibration.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_calibration.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_calibration.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_calibration.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_calibration.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_maintenance_team_member IS '维护团队成员';
                
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.TEAM_ID IS '团队ID';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.EMPLOYEE_ID IS '成员(职员)';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.ROLE IS '角色';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.JOINED_AT IS '加入时间';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.LEFT_AT IS '退出时间';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_visit IS '维护访问';
                
      COMMENT ON COLUMN erp_mnt_visit.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_visit.CODE IS '访问编码';
                    
      COMMENT ON COLUMN erp_mnt_visit.SCHEDULE_ID IS '维护计划ID';
                    
      COMMENT ON COLUMN erp_mnt_visit.EQUIPMENT_ID IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_visit.VISIT_DATE IS '访问日期';
                    
      COMMENT ON COLUMN erp_mnt_visit.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_mnt_visit.ASSIGNED_TO IS '指派人';
                    
      COMMENT ON COLUMN erp_mnt_visit.COMPLETED_BY IS '完成人';
                    
      COMMENT ON COLUMN erp_mnt_visit.COMPLETED_AT IS '完成时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.START_TIME IS '开始时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.END_TIME IS '结束时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.TOTAL_MINUTES IS '总分钟数';
                    
      COMMENT ON COLUMN erp_mnt_visit.VISIT_TYPE IS '维护类型';
                    
      COMMENT ON COLUMN erp_mnt_visit.RESULT IS '执行结果';
                    
      COMMENT ON COLUMN erp_mnt_visit.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_visit.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_visit.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_visit.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_visit.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_visit.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mnt_visit.BUSINESS_DATE IS '业务日期';
                    
      COMMENT ON COLUMN erp_mnt_visit.POSTED IS '是否已过账';
                    
      COMMENT ON COLUMN erp_mnt_visit.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.POSTED_BY IS '过账人';
                    
      COMMENT ON TABLE erp_mnt_visit_task IS '维护任务';
                
      COMMENT ON COLUMN erp_mnt_visit_task.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.VISIT_ID IS '维护访问ID';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.TASK_DESCRIPTION IS '任务描述';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.COMPLETED_BY IS '完成人';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.COMPLETED_AT IS '完成时间';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_spare_part_usage IS '备件消耗';
                
      COMMENT ON COLUMN erp_mnt_spare_part_usage.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.CODE IS '单号';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.ORG_ID IS '业务组织';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.VISIT_ID IS '维护访问ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.REQUEST_ID IS '维护请求ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.EQUIPMENT_ID IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.BUSINESS_DATE IS '消耗日期';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.WAREHOUSE_ID IS '领料仓库';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.TOTAL_AMOUNT IS '金额合计';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.DOC_STATUS IS '单据状态';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.APPROVE_STATUS IS '审核状态';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.POSTED IS '已过账(库存已出库)';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.POSTED_AT IS '过账时间';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.POSTED_BY IS '过账人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_spare_part_usage_line IS '备件消耗行';
                
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.SPARE_PART_USAGE_ID IS '备件消耗单ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.LINE_NO IS '行号';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.MATERIAL_ID IS '备件(物料)';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.UO_M_ID IS '计量单位';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.QUANTITY IS '消耗数量';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.UNIT_COST IS '单位成本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.AMOUNT IS '金额';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.BATCH_NO IS '批号';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.UPDATE_TIME IS '修改时间';
                    
