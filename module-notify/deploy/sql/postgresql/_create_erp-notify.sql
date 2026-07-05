
CREATE TABLE erp_sys_notification_template(
  id INT8 NOT NULL ,
  notification_type VARCHAR(100) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  channel_set VARCHAR(100) default 'IN_APP'  NOT NULL ,
  subject_tpl VARCHAR(2000)  ,
  body_tpl VARCHAR(4000)  ,
  recipient_resolver VARCHAR(20) default 'ROLE'  NOT NULL ,
  recipient_config VARCHAR(2000)  ,
  merge_window_seconds INT4 default 300   ,
  merge_strategy VARCHAR(30) default 'MERGE_BY_USER_TYPE'  NOT NULL ,
  status VARCHAR(20) default 'DRAFT'  NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_sys_notification_template primary key (id)
);

CREATE TABLE erp_sys_notification(
  id INT8 NOT NULL ,
  template_id INT8  ,
  notification_type VARCHAR(100) NOT NULL ,
  recipient_user_id VARCHAR(36)  ,
  recipient_partner_id INT8  ,
  recipient_dept_id INT8  ,
  channel VARCHAR(20) default 'IN_APP'  NOT NULL ,
  subject VARCHAR(500)  ,
  body VARCHAR(4000)  ,
  payload_json VARCHAR(4000)  ,
  status VARCHAR(20) default 'PENDING'  NOT NULL ,
  merge_group_id VARCHAR(200)  ,
  merge_count INT4 default 1   ,
  sent_at TIMESTAMP  ,
  error_msg VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_sys_notification primary key (id)
);

CREATE TABLE erp_sys_notification_read(
  id INT8 NOT NULL ,
  notification_id INT8 NOT NULL ,
  user_id VARCHAR(36) NOT NULL ,
  read_time TIMESTAMP NOT NULL ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_sys_notification_read primary key (id)
);


      COMMENT ON TABLE erp_sys_notification_template IS '通知模板';
                
      COMMENT ON COLUMN erp_sys_notification_template.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sys_notification_template.notification_type IS '业务事件键';
                    
      COMMENT ON COLUMN erp_sys_notification_template.name IS '模板名称';
                    
      COMMENT ON COLUMN erp_sys_notification_template.channel_set IS '渠道集合';
                    
      COMMENT ON COLUMN erp_sys_notification_template.subject_tpl IS '标题模板';
                    
      COMMENT ON COLUMN erp_sys_notification_template.body_tpl IS '正文模板';
                    
      COMMENT ON COLUMN erp_sys_notification_template.recipient_resolver IS '接收人解析器';
                    
      COMMENT ON COLUMN erp_sys_notification_template.recipient_config IS '接收人配置(JSON)';
                    
      COMMENT ON COLUMN erp_sys_notification_template.merge_window_seconds IS '合并窗口(秒)';
                    
      COMMENT ON COLUMN erp_sys_notification_template.merge_strategy IS '合并策略';
                    
      COMMENT ON COLUMN erp_sys_notification_template.status IS '模板状态';
                    
      COMMENT ON COLUMN erp_sys_notification_template.remark IS '备注';
                    
      COMMENT ON COLUMN erp_sys_notification_template.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sys_notification_template.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sys_notification_template.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sys_notification_template.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sys_notification_template.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sys_notification_template.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_sys_notification IS '通知实例';
                
      COMMENT ON COLUMN erp_sys_notification.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sys_notification.template_id IS '模板ID';
                    
      COMMENT ON COLUMN erp_sys_notification.notification_type IS '业务事件键';
                    
      COMMENT ON COLUMN erp_sys_notification.recipient_user_id IS '接收用户';
                    
      COMMENT ON COLUMN erp_sys_notification.recipient_partner_id IS '接收业务伙伴';
                    
      COMMENT ON COLUMN erp_sys_notification.recipient_dept_id IS '接收部门';
                    
      COMMENT ON COLUMN erp_sys_notification.channel IS '渠道';
                    
      COMMENT ON COLUMN erp_sys_notification.subject IS '标题';
                    
      COMMENT ON COLUMN erp_sys_notification.body IS '正文';
                    
      COMMENT ON COLUMN erp_sys_notification.payload_json IS '负载JSON';
                    
      COMMENT ON COLUMN erp_sys_notification.status IS '状态';
                    
      COMMENT ON COLUMN erp_sys_notification.merge_group_id IS '合并组ID';
                    
      COMMENT ON COLUMN erp_sys_notification.merge_count IS '合并次数';
                    
      COMMENT ON COLUMN erp_sys_notification.sent_at IS '发送时间';
                    
      COMMENT ON COLUMN erp_sys_notification.error_msg IS '错误信息';
                    
      COMMENT ON COLUMN erp_sys_notification.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sys_notification.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sys_notification.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sys_notification.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sys_notification.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sys_notification.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_sys_notification_read IS '通知已读记录';
                
      COMMENT ON COLUMN erp_sys_notification_read.id IS 'ID';
                    
      COMMENT ON COLUMN erp_sys_notification_read.notification_id IS '通知ID';
                    
      COMMENT ON COLUMN erp_sys_notification_read.user_id IS '用户';
                    
      COMMENT ON COLUMN erp_sys_notification_read.read_time IS '阅读时间';
                    
      COMMENT ON COLUMN erp_sys_notification_read.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_sys_notification_read.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_sys_notification_read.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_sys_notification_read.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_sys_notification_read.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_sys_notification_read.update_time IS '修改时间';
                    
