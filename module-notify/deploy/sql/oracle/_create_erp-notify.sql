
CREATE TABLE erp_sys_notification_template(
  ID NUMBER(20) NOT NULL ,
  NOTIFICATION_TYPE VARCHAR2(100) NOT NULL ,
  NAME VARCHAR2(200) NOT NULL ,
  CHANNEL_SET VARCHAR2(100) default 'IN_APP'  NOT NULL ,
  SUBJECT_TPL VARCHAR2(2000)  ,
  BODY_TPL VARCHAR2(4000)  ,
  RECIPIENT_RESOLVER VARCHAR2(20) default 'ROLE'  NOT NULL ,
  RECIPIENT_CONFIG VARCHAR2(2000)  ,
  MERGE_WINDOW_SECONDS INTEGER default 300   ,
  MERGE_STRATEGY VARCHAR2(30) default 'MERGE_BY_USER_TYPE'  NOT NULL ,
  STATUS VARCHAR2(20) default 'DRAFT'  NOT NULL ,
  REMARK VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_sys_notification_template primary key (ID)
);

CREATE TABLE erp_sys_notification(
  ID NUMBER(20) NOT NULL ,
  TEMPLATE_ID NUMBER(20)  ,
  NOTIFICATION_TYPE VARCHAR2(100) NOT NULL ,
  RECIPIENT_USER_ID VARCHAR2(36)  ,
  RECIPIENT_PARTNER_ID NUMBER(20)  ,
  RECIPIENT_DEPT_ID NUMBER(20)  ,
  CHANNEL VARCHAR2(20) default 'IN_APP'  NOT NULL ,
  SUBJECT VARCHAR2(500)  ,
  BODY VARCHAR2(4000)  ,
  PAYLOAD_JSON VARCHAR2(4000)  ,
  STATUS VARCHAR2(20) default 'PENDING'  NOT NULL ,
  MERGE_GROUP_ID VARCHAR2(200)  ,
  MERGE_COUNT INTEGER default 1   ,
  SENT_AT TIMESTAMP  ,
  ERROR_MSG VARCHAR2(1000)  ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_sys_notification primary key (ID)
);

CREATE TABLE erp_sys_notification_read(
  ID NUMBER(20) NOT NULL ,
  NOTIFICATION_ID NUMBER(20) NOT NULL ,
  USER_ID VARCHAR2(36) NOT NULL ,
  READ_TIME TIMESTAMP NOT NULL ,
  DEL_VERSION NUMBER(20) default 0  NOT NULL ,
  VERSION INTEGER default 0  NOT NULL ,
  CREATED_BY VARCHAR2(50) NOT NULL ,
  CREATE_TIME TIMESTAMP NOT NULL ,
  UPDATED_BY VARCHAR2(50) NOT NULL ,
  UPDATE_TIME TIMESTAMP NOT NULL ,
  constraint PK_erp_sys_notification_read primary key (ID)
);


      COMMENT ON TABLE erp_sys_notification_template IS '通知模板';
                
      COMMENT ON COLUMN erp_sys_notification_template.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sys_notification_template.NOTIFICATION_TYPE IS '业务事件键';
                    
      COMMENT ON COLUMN erp_sys_notification_template.NAME IS '模板名称';
                    
      COMMENT ON COLUMN erp_sys_notification_template.CHANNEL_SET IS '渠道集合';
                    
      COMMENT ON COLUMN erp_sys_notification_template.SUBJECT_TPL IS '标题模板';
                    
      COMMENT ON COLUMN erp_sys_notification_template.BODY_TPL IS '正文模板';
                    
      COMMENT ON COLUMN erp_sys_notification_template.RECIPIENT_RESOLVER IS '接收人解析器';
                    
      COMMENT ON COLUMN erp_sys_notification_template.RECIPIENT_CONFIG IS '接收人配置(JSON)';
                    
      COMMENT ON COLUMN erp_sys_notification_template.MERGE_WINDOW_SECONDS IS '合并窗口(秒)';
                    
      COMMENT ON COLUMN erp_sys_notification_template.MERGE_STRATEGY IS '合并策略';
                    
      COMMENT ON COLUMN erp_sys_notification_template.STATUS IS '模板状态';
                    
      COMMENT ON COLUMN erp_sys_notification_template.REMARK IS '备注';
                    
      COMMENT ON COLUMN erp_sys_notification_template.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sys_notification_template.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sys_notification_template.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sys_notification_template.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sys_notification_template.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sys_notification_template.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_sys_notification IS '通知实例';
                
      COMMENT ON COLUMN erp_sys_notification.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sys_notification.TEMPLATE_ID IS '模板ID';
                    
      COMMENT ON COLUMN erp_sys_notification.NOTIFICATION_TYPE IS '业务事件键';
                    
      COMMENT ON COLUMN erp_sys_notification.RECIPIENT_USER_ID IS '接收用户';
                    
      COMMENT ON COLUMN erp_sys_notification.RECIPIENT_PARTNER_ID IS '接收业务伙伴';
                    
      COMMENT ON COLUMN erp_sys_notification.RECIPIENT_DEPT_ID IS '接收部门';
                    
      COMMENT ON COLUMN erp_sys_notification.CHANNEL IS '渠道';
                    
      COMMENT ON COLUMN erp_sys_notification.SUBJECT IS '标题';
                    
      COMMENT ON COLUMN erp_sys_notification.BODY IS '正文';
                    
      COMMENT ON COLUMN erp_sys_notification.PAYLOAD_JSON IS '负载JSON';
                    
      COMMENT ON COLUMN erp_sys_notification.STATUS IS '状态';
                    
      COMMENT ON COLUMN erp_sys_notification.MERGE_GROUP_ID IS '合并组ID';
                    
      COMMENT ON COLUMN erp_sys_notification.MERGE_COUNT IS '合并次数';
                    
      COMMENT ON COLUMN erp_sys_notification.SENT_AT IS '发送时间';
                    
      COMMENT ON COLUMN erp_sys_notification.ERROR_MSG IS '错误信息';
                    
      COMMENT ON COLUMN erp_sys_notification.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sys_notification.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sys_notification.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sys_notification.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sys_notification.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sys_notification.UPDATE_TIME IS '修改时间';
                    
      COMMENT ON TABLE erp_sys_notification_read IS '通知已读记录';
                
      COMMENT ON COLUMN erp_sys_notification_read.ID IS 'ID';
                    
      COMMENT ON COLUMN erp_sys_notification_read.NOTIFICATION_ID IS '通知ID';
                    
      COMMENT ON COLUMN erp_sys_notification_read.USER_ID IS '用户';
                    
      COMMENT ON COLUMN erp_sys_notification_read.READ_TIME IS '阅读时间';
                    
      COMMENT ON COLUMN erp_sys_notification_read.DEL_VERSION IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sys_notification_read.VERSION IS '数据版本';
                    
      COMMENT ON COLUMN erp_sys_notification_read.CREATED_BY IS '创建人';
                    
      COMMENT ON COLUMN erp_sys_notification_read.CREATE_TIME IS '创建时间';
                    
      COMMENT ON COLUMN erp_sys_notification_read.UPDATED_BY IS '修改人';
                    
      COMMENT ON COLUMN erp_sys_notification_read.UPDATE_TIME IS '修改时间';
                    
