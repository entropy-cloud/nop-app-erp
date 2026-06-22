
CREATE TABLE erp_mnt_equipment_category(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  parent_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_equipment_category primary key (id)
);

CREATE TABLE erp_mnt_maintenance_team(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  leader_id INT8  ,
  description VARCHAR(2000)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_maintenance_team primary key (id)
);

CREATE TABLE erp_mnt_equipment(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  asset_id INT8  ,
  workcenter_id INT8  ,
  location_id INT8  ,
  category_id INT8  ,
  status INT4 NOT NULL ,
  serial_no VARCHAR(100)  ,
  manufacturer VARCHAR(200)  ,
  model VARCHAR(200)  ,
  install_date DATE  ,
  warranty_expiry DATE  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_equipment primary key (id)
);

CREATE TABLE erp_mnt_maintenance_team_member(
  id INT8 NOT NULL ,
  team_id INT8 NOT NULL ,
  employee_id INT8 NOT NULL ,
  role VARCHAR(50)  ,
  joined_at TIMESTAMP  ,
  left_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_maintenance_team_member primary key (id)
);

CREATE TABLE erp_mnt_schedule(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  equipment_id INT8 NOT NULL ,
  schedule_type INT4 NOT NULL ,
  frequency INT4  ,
  recurrence_type INT4  ,
  days_of_week VARCHAR(50)  ,
  start_date DATE NOT NULL ,
  end_date DATE  ,
  next_due_date DATE  ,
  is_active INT4 default 1   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_schedule primary key (id)
);

CREATE TABLE erp_mnt_request(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  equipment_id INT8 NOT NULL ,
  request_date DATE NOT NULL ,
  description VARCHAR(2000) NOT NULL ,
  priority INT4 NOT NULL ,
  status INT4 NOT NULL ,
  requested_by INT8 NOT NULL ,
  assigned_to INT8  ,
  accepted_by INT8  ,
  completed_by INT8  ,
  completed_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_request primary key (id)
);

CREATE TABLE erp_mnt_downtime_entry(
  id INT8 NOT NULL ,
  equipment_id INT8 NOT NULL ,
  start_time TIMESTAMP NOT NULL ,
  end_time TIMESTAMP  ,
  total_minutes NUMERIC(12,2)  ,
  reason VARCHAR(500)  ,
  related_job_order_id INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_downtime_entry primary key (id)
);

CREATE TABLE erp_mnt_calibration(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  equipment_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  standard_ref VARCHAR(200)  ,
  measured_value NUMERIC(20,6)  ,
  target_value NUMERIC(20,6)  ,
  tolerance NUMERIC(20,6)  ,
  result INT4 NOT NULL ,
  next_calibration_date DATE  ,
  calibrated_by INT8  ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_calibration primary key (id)
);

CREATE TABLE erp_mnt_visit(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  schedule_id INT8  ,
  equipment_id INT8 NOT NULL ,
  visit_date DATE NOT NULL ,
  status INT4 NOT NULL ,
  assigned_to INT8  ,
  completed_by INT8  ,
  completed_at TIMESTAMP  ,
  start_time TIMESTAMP  ,
  end_time TIMESTAMP  ,
  total_minutes NUMERIC(12,2)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_visit primary key (id)
);

CREATE TABLE erp_mnt_visit_task(
  id INT8 NOT NULL ,
  visit_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  task_description VARCHAR(500) NOT NULL ,
  status INT4 NOT NULL ,
  completed_by INT8  ,
  completed_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_visit_task primary key (id)
);

CREATE TABLE erp_mnt_spare_part_usage(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  visit_id INT8  ,
  request_id INT8  ,
  equipment_id INT8 NOT NULL ,
  business_date DATE NOT NULL ,
  warehouse_id INT8  ,
  total_amount NUMERIC(20,4) default 0   ,
  doc_status INT4 NOT NULL ,
  approve_status INT4 NOT NULL ,
  posted BOOLEAN default false   ,
  posted_at TIMESTAMP  ,
  posted_by INT8  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_spare_part_usage primary key (id)
);

CREATE TABLE erp_mnt_spare_part_usage_line(
  id INT8 NOT NULL ,
  spare_part_usage_id INT8 NOT NULL ,
  line_no INT4 NOT NULL ,
  material_id INT8 NOT NULL ,
  uo_m_id INT8 NOT NULL ,
  quantity NUMERIC(20,4) NOT NULL ,
  unit_cost NUMERIC(20,4)  ,
  amount NUMERIC(20,4)  ,
  batch_no VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_mnt_spare_part_usage_line primary key (id)
);


      COMMENT ON TABLE erp_mnt_equipment_category IS '设备分类';
                
      COMMENT ON COLUMN erp_mnt_equipment_category.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.code IS '分类编码';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.name IS '分类名称';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.parent_id IS '上级分类ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_equipment_category.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_maintenance_team IS '维护团队';
                
      COMMENT ON COLUMN erp_mnt_maintenance_team.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.code IS '团队编码';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.name IS '团队名称';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.leader_id IS '负责人(职员)';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.description IS '描述';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_equipment IS '设备';
                
      COMMENT ON COLUMN erp_mnt_equipment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment.code IS '设备编码';
                    
      COMMENT ON COLUMN erp_mnt_equipment.name IS '设备名称';
                    
      COMMENT ON COLUMN erp_mnt_equipment.org_id IS '所属组织';
                    
      COMMENT ON COLUMN erp_mnt_equipment.asset_id IS '资产卡片(关联 assets 域)';
                    
      COMMENT ON COLUMN erp_mnt_equipment.workcenter_id IS '关联工作中心(关联 mfg 域)';
                    
      COMMENT ON COLUMN erp_mnt_equipment.location_id IS '位置ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment.category_id IS '分类ID';
                    
      COMMENT ON COLUMN erp_mnt_equipment.status IS '状态';
                    
      COMMENT ON COLUMN erp_mnt_equipment.serial_no IS '序列号';
                    
      COMMENT ON COLUMN erp_mnt_equipment.manufacturer IS '制造商';
                    
      COMMENT ON COLUMN erp_mnt_equipment.model IS '型号';
                    
      COMMENT ON COLUMN erp_mnt_equipment.install_date IS '安装日期';
                    
      COMMENT ON COLUMN erp_mnt_equipment.warranty_expiry IS '保修到期';
                    
      COMMENT ON COLUMN erp_mnt_equipment.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_equipment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_equipment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_equipment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_equipment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_equipment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_equipment.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_maintenance_team_member IS '维护团队成员';
                
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.team_id IS '团队ID';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.employee_id IS '成员(职员)';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.role IS '角色';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.joined_at IS '加入时间';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.left_at IS '退出时间';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_maintenance_team_member.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_schedule IS '维护计划';
                
      COMMENT ON COLUMN erp_mnt_schedule.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_schedule.code IS '计划编码';
                    
      COMMENT ON COLUMN erp_mnt_schedule.name IS '计划名称';
                    
      COMMENT ON COLUMN erp_mnt_schedule.equipment_id IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_schedule.schedule_type IS '计划类型';
                    
      COMMENT ON COLUMN erp_mnt_schedule.frequency IS '频率';
                    
      COMMENT ON COLUMN erp_mnt_schedule.recurrence_type IS '重复类型';
                    
      COMMENT ON COLUMN erp_mnt_schedule.days_of_week IS '星期几';
                    
      COMMENT ON COLUMN erp_mnt_schedule.start_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_mnt_schedule.end_date IS '结束日期';
                    
      COMMENT ON COLUMN erp_mnt_schedule.next_due_date IS '下次到期日';
                    
      COMMENT ON COLUMN erp_mnt_schedule.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_mnt_schedule.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_schedule.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_schedule.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_schedule.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_schedule.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_schedule.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_schedule.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_request IS '维护请求';
                
      COMMENT ON COLUMN erp_mnt_request.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_request.code IS '请求编码';
                    
      COMMENT ON COLUMN erp_mnt_request.equipment_id IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_request.request_date IS '请求日期';
                    
      COMMENT ON COLUMN erp_mnt_request.description IS '问题描述';
                    
      COMMENT ON COLUMN erp_mnt_request.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_mnt_request.status IS '状态';
                    
      COMMENT ON COLUMN erp_mnt_request.requested_by IS '请求人';
                    
      COMMENT ON COLUMN erp_mnt_request.assigned_to IS '指派人';
                    
      COMMENT ON COLUMN erp_mnt_request.accepted_by IS '受理人';
                    
      COMMENT ON COLUMN erp_mnt_request.completed_by IS '完成人';
                    
      COMMENT ON COLUMN erp_mnt_request.completed_at IS '完成时间';
                    
      COMMENT ON COLUMN erp_mnt_request.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_request.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_request.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_request.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_request.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_request.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_request.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_downtime_entry IS '停机记录';
                
      COMMENT ON COLUMN erp_mnt_downtime_entry.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.equipment_id IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.start_time IS '开始时间';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.end_time IS '结束时间';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.total_minutes IS '总分钟数';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.reason IS '原因';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.related_job_order_id IS '关联生产工单ID';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_downtime_entry.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_calibration IS '校准记录';
                
      COMMENT ON COLUMN erp_mnt_calibration.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_calibration.code IS '单号';
                    
      COMMENT ON COLUMN erp_mnt_calibration.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mnt_calibration.equipment_id IS '被校设备/量具';
                    
      COMMENT ON COLUMN erp_mnt_calibration.business_date IS '校准日期';
                    
      COMMENT ON COLUMN erp_mnt_calibration.standard_ref IS '参考标准';
                    
      COMMENT ON COLUMN erp_mnt_calibration.measured_value IS '测量值';
                    
      COMMENT ON COLUMN erp_mnt_calibration.target_value IS '目标值';
                    
      COMMENT ON COLUMN erp_mnt_calibration.tolerance IS '允差';
                    
      COMMENT ON COLUMN erp_mnt_calibration.result IS '校准结果';
                    
      COMMENT ON COLUMN erp_mnt_calibration.next_calibration_date IS '下次校准日期';
                    
      COMMENT ON COLUMN erp_mnt_calibration.calibrated_by IS '校准人(职员)';
                    
      COMMENT ON COLUMN erp_mnt_calibration.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_mnt_calibration.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_mnt_calibration.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_calibration.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_calibration.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_calibration.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_calibration.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_calibration.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_calibration.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_visit IS '维护访问';
                
      COMMENT ON COLUMN erp_mnt_visit.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_visit.code IS '访问编码';
                    
      COMMENT ON COLUMN erp_mnt_visit.schedule_id IS '维护计划ID';
                    
      COMMENT ON COLUMN erp_mnt_visit.equipment_id IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_visit.visit_date IS '访问日期';
                    
      COMMENT ON COLUMN erp_mnt_visit.status IS '状态';
                    
      COMMENT ON COLUMN erp_mnt_visit.assigned_to IS '指派人';
                    
      COMMENT ON COLUMN erp_mnt_visit.completed_by IS '完成人';
                    
      COMMENT ON COLUMN erp_mnt_visit.completed_at IS '完成时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.start_time IS '开始时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.end_time IS '结束时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.total_minutes IS '总分钟数';
                    
      COMMENT ON COLUMN erp_mnt_visit.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_visit.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_visit.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_visit.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_visit.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_visit.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_visit.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_visit_task IS '维护任务';
                
      COMMENT ON COLUMN erp_mnt_visit_task.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.visit_id IS '维护访问ID';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.task_description IS '任务描述';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.status IS '状态';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.completed_by IS '完成人';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.completed_at IS '完成时间';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_visit_task.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_spare_part_usage IS '备件消耗';
                
      COMMENT ON COLUMN erp_mnt_spare_part_usage.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.code IS '单号';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.visit_id IS '维护访问ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.request_id IS '维护请求ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.equipment_id IS '设备ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.business_date IS '消耗日期';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.warehouse_id IS '领料仓库';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.total_amount IS '金额合计';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.approve_status IS '审核状态';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.posted IS '已过账(库存已出库)';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.posted_at IS '过账时间';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.posted_by IS '过账人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_mnt_spare_part_usage_line IS '备件消耗行';
                
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.spare_part_usage_id IS '备件消耗单ID';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.line_no IS '行号';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.material_id IS '备件(物料)';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.uo_m_id IS '计量单位';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.quantity IS '消耗数量';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.unit_cost IS '单位成本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.amount IS '金额';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.batch_no IS '批号';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_mnt_spare_part_usage_line.update_time IS '修改时间';
                    
