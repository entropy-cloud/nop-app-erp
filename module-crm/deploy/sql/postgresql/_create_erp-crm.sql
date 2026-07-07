
CREATE TABLE erp_md_partner(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  partner_type INT4  ,
  status INT4  ,
  credit_limit VARCHAR(50)  ,
  constraint PK_erp_md_partner primary key (id)
);

CREATE TABLE erp_crm_source(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  sequence INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_source primary key (id)
);

CREATE TABLE erp_crm_lead_status(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  sequence INT4 default 0   ,
  is_default BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_status primary key (id)
);

CREATE TABLE erp_crm_lost_reason(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  sequence INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lost_reason primary key (id)
);

CREATE TABLE erp_md_organization(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  org_type INT4  ,
  parent_id INT8  ,
  status INT4  ,
  constraint PK_erp_md_organization primary key (id)
);

CREATE TABLE erp_crm_event_category(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  color VARCHAR(20)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_event_category primary key (id)
);

CREATE TABLE erp_md_partner_contact(
  id INT8  ,
  contact_name VARCHAR(100)  ,
  phone VARCHAR(50)  ,
  email VARCHAR(200)  ,
  constraint PK_erp_md_partner_contact primary key (id)
);

CREATE TABLE erp_crm_quote_template(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  template_content VARCHAR(4000)  ,
  is_default BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_quote_template primary key (id)
);

CREATE TABLE erp_md_currency(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_currency primary key (id)
);

CREATE TABLE erp_md_material(
  id INT8  ,
  code VARCHAR(50)  ,
  name VARCHAR(200)  ,
  constraint PK_erp_md_material primary key (id)
);

CREATE TABLE erp_crm_team(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  team_leader_id VARCHAR(36)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_team primary key (id)
);

CREATE TABLE erp_crm_campaign(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  campaign_name VARCHAR(200) NOT NULL ,
  medium VARCHAR(100)  ,
  source VARCHAR(100)  ,
  start_date DATE  ,
  end_date DATE  ,
  budget_amount NUMERIC(20,4)  ,
  actual_cost NUMERIC(20,4)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_campaign primary key (id)
);

CREATE TABLE erp_crm_lead_score_config(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  config_name VARCHAR(200) NOT NULL ,
  is_active BOOLEAN default false   ,
  effective_from DATE  ,
  effective_to DATE  ,
  auto_qualify_threshold INT4  ,
  min_score_for_follow_up INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_score_config primary key (id)
);

CREATE TABLE erp_crm_forecast_period(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  period_type VARCHAR(20) NOT NULL ,
  period_start DATE NOT NULL ,
  period_end DATE NOT NULL ,
  label VARCHAR(100)  ,
  status VARCHAR(20) NOT NULL ,
  is_current BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_forecast_period primary key (id)
);

CREATE TABLE erp_crm_territory(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  parent_id INT8  ,
  territory_type VARCHAR(20) NOT NULL ,
  manager_id INT8  ,
  description VARCHAR(500)  ,
  full_path VARCHAR(500)  ,
  level INT4 default 0   ,
  is_active BOOLEAN default true   ,
  is_leaf BOOLEAN default false   ,
  sort_order INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_territory primary key (id)
);

CREATE TABLE erp_crm_product_configurator(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  product_type VARCHAR(100)  ,
  config_name VARCHAR(200)  ,
  wizard_layout VARCHAR(4000)  ,
  is_active BOOLEAN default true   ,
  effective_from DATE  ,
  effective_to DATE  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_product_configurator primary key (id)
);

CREATE TABLE erp_crm_bundle_pricing(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  bundle_name VARCHAR(200) NOT NULL ,
  description VARCHAR(1000)  ,
  discount_type VARCHAR(20)  ,
  discount_value NUMERIC(20,4)  ,
  bundle_amount NUMERIC(20,4)  ,
  effective_from DATE  ,
  effective_to DATE  ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_bundle_pricing primary key (id)
);

CREATE TABLE erp_crm_sequence(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  template_type VARCHAR(30) NOT NULL ,
  description VARCHAR(500)  ,
  expected_duration INT4  ,
  is_active BOOLEAN default true   ,
  is_default BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_sequence primary key (id)
);

CREATE TABLE erp_crm_price_rule(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  org_id INT8  ,
  rule_type VARCHAR(30) NOT NULL ,
  priority INT4 default 0   ,
  product_id INT8  ,
  product_category VARCHAR(100)  ,
  customer_id INT8  ,
  customer_category VARCHAR(100)  ,
  min_quantity NUMERIC(20,4)  ,
  max_quantity NUMERIC(20,4)  ,
  price_override NUMERIC(20,4)  ,
  discount_percent NUMERIC(10,2)  ,
  discount_amount NUMERIC(20,4)  ,
  currency_id INT8  ,
  effective_from DATE  ,
  effective_to DATE  ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_price_rule primary key (id)
);

CREATE TABLE erp_crm_stage(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  stage_name VARCHAR(200) NOT NULL ,
  sequence INT4 default 0  NOT NULL ,
  team_id INT8  ,
  default_probability INT4  ,
  is_won_stage BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_stage primary key (id)
);

CREATE TABLE erp_crm_lead_score_config_line(
  id INT8 NOT NULL ,
  config_id INT8 NOT NULL ,
  org_id INT8  ,
  criterion_code VARCHAR(50) NOT NULL ,
  criterion_name VARCHAR(200)  ,
  weight INT4 default 0   ,
  scoring_method VARCHAR(20) NOT NULL ,
  lookup_table VARCHAR(4000)  ,
  formula VARCHAR(500)  ,
  max_score INT4 default 0   ,
  sequence INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_score_config_line primary key (id)
);

CREATE TABLE erp_crm_forecast(
  id INT8 NOT NULL ,
  org_id INT8  ,
  period_id INT8 NOT NULL ,
  territory_id INT8  ,
  team_id INT8  ,
  owner_id VARCHAR(36)  ,
  currency_id INT8  ,
  commit_amount NUMERIC(20,4)  ,
  upside_amount NUMERIC(20,4)  ,
  weighted_amount NUMERIC(20,4)  ,
  best_case_amount NUMERIC(20,4)  ,
  opportunity_count INT4 default 0   ,
  commit_opportunity_count INT4 default 0   ,
  expected_closed_revenue NUMERIC(20,4)  ,
  last_calculated_at TIMESTAMP  ,
  notes VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_forecast primary key (id)
);

CREATE TABLE erp_crm_territory_assignment_rule(
  id INT8 NOT NULL ,
  org_id INT8  ,
  rule_name VARCHAR(200) NOT NULL ,
  priority INT4 default 0   ,
  territory_id INT8 NOT NULL ,
  condition_type VARCHAR(30) NOT NULL ,
  condition_value VARCHAR(4000)  ,
  assignment_method VARCHAR(20) NOT NULL ,
  group_id INT8  ,
  is_default BOOLEAN default false   ,
  is_active BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_territory_assignment_rule primary key (id)
);

CREATE TABLE erp_crm_quota(
  id INT8 NOT NULL ,
  org_id INT8  ,
  territory_id INT8  ,
  team_id INT8  ,
  owner_id VARCHAR(36)  ,
  period_type VARCHAR(20) NOT NULL ,
  fiscal_year INT4  ,
  period_label VARCHAR(100)  ,
  quota_amount NUMERIC(20,4)  ,
  currency_id INT8  ,
  is_finalized BOOLEAN default false   ,
  notes VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_quota primary key (id)
);

CREATE TABLE erp_crm_lead_funnel(
  id INT8 NOT NULL ,
  org_id INT8  ,
  funnel_name VARCHAR(200) NOT NULL ,
  period_start DATE NOT NULL ,
  period_end DATE NOT NULL ,
  territory_id INT8  ,
  team_id INT8  ,
  source_id INT8  ,
  total_leads_at_top INT4 default 0   ,
  total_opportunities INT4 default 0   ,
  total_won INT4 default 0   ,
  total_lost INT4 default 0   ,
  total_revenue NUMERIC(20,4)  ,
  lost_revenue NUMERIC(20,4)  ,
  weighted_revenue NUMERIC(20,4)  ,
  avg_deal_size NUMERIC(20,4)  ,
  avg_sales_cycle_days NUMERIC(10,2)  ,
  calculated_at TIMESTAMP  ,
  calculated_by VARCHAR(50)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_funnel primary key (id)
);

CREATE TABLE erp_crm_config_rule(
  id INT8 NOT NULL ,
  configurator_id INT8 NOT NULL ,
  org_id INT8  ,
  rule_type VARCHAR(20) NOT NULL ,
  source_feature_code VARCHAR(100)  ,
  source_feature_value VARCHAR(200)  ,
  target_feature_code VARCHAR(100)  ,
  target_feature_value VARCHAR(200)  ,
  condition_expression VARCHAR(500)  ,
  sequence INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_config_rule primary key (id)
);

CREATE TABLE erp_crm_bundle_pricing_line(
  id INT8 NOT NULL ,
  bundle_id INT8 NOT NULL ,
  org_id INT8  ,
  product_id INT8  ,
  quantity NUMERIC(20,4)  ,
  unit_price NUMERIC(20,4)  ,
  sequence INT4 default 0   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_bundle_pricing_line primary key (id)
);

CREATE TABLE erp_crm_sequence_step(
  id INT8 NOT NULL ,
  sequence_id INT8 NOT NULL ,
  org_id INT8  ,
  step_name VARCHAR(200) NOT NULL ,
  step_order INT4 default 0  NOT NULL ,
  due_days INT4 default 0   ,
  activity_type VARCHAR(20) NOT NULL ,
  step_description VARCHAR(1000)  ,
  completion_condition VARCHAR(30) NOT NULL ,
  is_mandatory BOOLEAN default true   ,
  auto_create_event BOOLEAN default true   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_sequence_step primary key (id)
);

CREATE TABLE erp_crm_sequence_assignment(
  id INT8 NOT NULL ,
  org_id INT8  ,
  sequence_id INT8 NOT NULL ,
  priority INT4 default 0   ,
  condition_type VARCHAR(30) NOT NULL ,
  condition_value VARCHAR(4000)  ,
  is_active BOOLEAN default true   ,
  is_default BOOLEAN default false   ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_sequence_assignment primary key (id)
);

CREATE TABLE erp_crm_lead(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  lead_type VARCHAR(20) NOT NULL ,
  partner_id INT8  ,
  contact_name VARCHAR(100)  ,
  contact_phone VARCHAR(50)  ,
  contact_email VARCHAR(200)  ,
  company_name VARCHAR(200)  ,
  job_title VARCHAR(100)  ,
  department VARCHAR(100)  ,
  source_id INT8  ,
  lead_status_id INT8  ,
  stage_id INT8  ,
  expected_revenue NUMERIC(20,4)  ,
  best_case_amount NUMERIC(20,4)  ,
  worst_case_amount NUMERIC(20,4)  ,
  recurring_revenue NUMERIC(20,4)  ,
  recurring_plan VARCHAR(50)  ,
  expected_close_date DATE  ,
  probability INT4  ,
  campaign_id INT8  ,
  utm_medium VARCHAR(100)  ,
  utm_source VARCHAR(100)  ,
  owner_id VARCHAR(36)  ,
  team_id INT8  ,
  lost_reason_id INT8  ,
  lost_reason_desc VARCHAR(500)  ,
  last_contact_date TIMESTAMP  ,
  next_activity_date TIMESTAMP  ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  doc_status VARCHAR(20) NOT NULL ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  territory_id INT8  ,
  constraint PK_erp_crm_lead primary key (id)
);

CREATE TABLE erp_crm_forecast_accuracy(
  id INT8 NOT NULL ,
  forecast_id INT8  ,
  org_id INT8  ,
  period_id INT8 NOT NULL ,
  owner_id VARCHAR(36)  ,
  team_id INT8  ,
  territory_id INT8  ,
  commit_amount NUMERIC(20,4)  ,
  upside_amount NUMERIC(20,4)  ,
  actual_closed_revenue NUMERIC(20,4)  ,
  commit_accuracy NUMERIC(10,4)  ,
  upside_accuracy NUMERIC(10,4)  ,
  deviation_amount NUMERIC(20,4)  ,
  calculated_by VARCHAR(50)  ,
  calculated_at TIMESTAMP  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_forecast_accuracy primary key (id)
);

CREATE TABLE erp_crm_funnel_stage_metrics(
  id INT8 NOT NULL ,
  funnel_id INT8 NOT NULL ,
  org_id INT8  ,
  stage_id INT8 NOT NULL ,
  stage_order INT4 default 0   ,
  stage_name VARCHAR(200)  ,
  lead_count_in INT4 default 0   ,
  lead_count_out INT4 default 0   ,
  lead_count_remaining INT4 default 0   ,
  conversion_rate NUMERIC(10,4)  ,
  drop_off_rate NUMERIC(10,4)  ,
  avg_days_in_stage NUMERIC(10,2)  ,
  lost_count INT4 default 0   ,
  lost_amount NUMERIC(20,4)  ,
  lost_reason_top VARCHAR(4000)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_funnel_stage_metrics primary key (id)
);

CREATE TABLE erp_crm_event(
  id INT8 NOT NULL ,
  code VARCHAR(50) NOT NULL ,
  org_id INT8  ,
  event_type VARCHAR(20) NOT NULL ,
  event_category_id INT8  ,
  subject VARCHAR(200) NOT NULL ,
  description VARCHAR(2000)  ,
  start_date_time TIMESTAMP NOT NULL ,
  end_date_time TIMESTAMP  ,
  duration INT4  ,
  related_lead_id INT8  ,
  related_bill_type VARCHAR(50)  ,
  related_bill_code VARCHAR(50)  ,
  partner_id INT8  ,
  contact_id INT8  ,
  owner_id VARCHAR(36)  ,
  status VARCHAR(20) NOT NULL ,
  priority VARCHAR(20) NOT NULL ,
  is_recurrent BOOLEAN default false   ,
  parent_event_id INT8  ,
  reminder_minutes_before INT4  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_event primary key (id)
);

CREATE TABLE erp_crm_activity(
  id INT8 NOT NULL ,
  lead_id INT8 NOT NULL ,
  org_id INT8  ,
  activity_type VARCHAR(20) NOT NULL ,
  activity_date TIMESTAMP NOT NULL ,
  summary VARCHAR(500)  ,
  owner_id VARCHAR(36)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_activity primary key (id)
);

CREATE TABLE erp_crm_lead_conv_log(
  id INT8 NOT NULL ,
  lead_id INT8 NOT NULL ,
  org_id INT8  ,
  from_stage_id INT8  ,
  to_stage_id INT8  ,
  changed_at TIMESTAMP NOT NULL ,
  changed_by VARCHAR(36)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_conv_log primary key (id)
);

CREATE TABLE erp_crm_lead_score(
  id INT8 NOT NULL ,
  lead_id INT8 NOT NULL ,
  org_id INT8  ,
  config_id INT8  ,
  total_score INT4 default 0   ,
  score_breakdown VARCHAR(4000)  ,
  auto_qualified BOOLEAN default false   ,
  triggered_action VARCHAR(20)  ,
  calculated_at TIMESTAMP  ,
  trigger_event VARCHAR(20)  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_score primary key (id)
);

CREATE TABLE erp_crm_forecast_line(
  id INT8 NOT NULL ,
  forecast_id INT8 NOT NULL ,
  org_id INT8  ,
  lead_id INT8 NOT NULL ,
  probability INT4  ,
  expected_revenue NUMERIC(20,4)  ,
  weighted_revenue NUMERIC(20,4)  ,
  forecast_category VARCHAR(20) NOT NULL ,
  included_in_commit BOOLEAN default false   ,
  stage_name VARCHAR(200)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_forecast_line primary key (id)
);

CREATE TABLE erp_crm_lead_seq_progress(
  id INT8 NOT NULL ,
  lead_id INT8 NOT NULL ,
  sequence_id INT8 NOT NULL ,
  org_id INT8  ,
  current_step_index INT4 default 0   ,
  status VARCHAR(20) NOT NULL ,
  started_at TIMESTAMP  ,
  completed_at TIMESTAMP  ,
  remark VARCHAR(1000)  ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_seq_progress primary key (id)
);

CREATE TABLE erp_crm_lead_score_line(
  id INT8 NOT NULL ,
  score_id INT8 NOT NULL ,
  org_id INT8  ,
  config_line_id INT8  ,
  criterion_code VARCHAR(50)  ,
  criterion_name VARCHAR(200)  ,
  raw_value VARCHAR(500)  ,
  lookup_value VARCHAR(200)  ,
  raw_score INT4 default 0   ,
  weighted_score INT4 default 0   ,
  sequence INT4 default 0   ,
  del_version INT8 default 0  NOT NULL ,
  version INT4 default 0  NOT NULL ,
  created_by VARCHAR(50) NOT NULL ,
  create_time TIMESTAMP NOT NULL ,
  updated_by VARCHAR(50) NOT NULL ,
  update_time TIMESTAMP NOT NULL ,
  constraint PK_erp_crm_lead_score_line primary key (id)
);


      COMMENT ON TABLE erp_md_partner IS '往来单位';
                
      COMMENT ON TABLE erp_crm_source IS '线索来源';
                
      COMMENT ON COLUMN erp_crm_source.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_source.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_source.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_source.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_source.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_source.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_source.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_source.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_source.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_source.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_source.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_status IS '线索状态';
                
      COMMENT ON COLUMN erp_crm_lead_status.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_status.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lead_status.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_lead_status.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lead_status.is_default IS '是否默认';
                    
      COMMENT ON COLUMN erp_crm_lead_status.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_status.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_status.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_status.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_status.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_status.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_status.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lost_reason IS '丢单原因';
                
      COMMENT ON COLUMN erp_crm_lost_reason.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lost_reason.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_organization IS '组织';
                
      COMMENT ON TABLE erp_crm_event_category IS '活动类别';
                
      COMMENT ON COLUMN erp_crm_event_category.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_event_category.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_event_category.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_event_category.color IS '日历颜色';
                    
      COMMENT ON COLUMN erp_crm_event_category.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_event_category.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_event_category.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_event_category.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_event_category.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_event_category.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_event_category.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_partner_contact IS '往来单位联系人';
                
      COMMENT ON TABLE erp_crm_quote_template IS '报价模板';
                
      COMMENT ON COLUMN erp_crm_quote_template.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_quote_template.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_quote_template.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_quote_template.template_content IS '模板内容';
                    
      COMMENT ON COLUMN erp_crm_quote_template.is_default IS '是否默认';
                    
      COMMENT ON COLUMN erp_crm_quote_template.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_quote_template.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_quote_template.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_quote_template.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_quote_template.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_quote_template.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_quote_template.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_md_currency IS '币种';
                
      COMMENT ON TABLE erp_md_material IS '物料';
                
      COMMENT ON TABLE erp_crm_team IS '销售团队';
                
      COMMENT ON COLUMN erp_crm_team.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_team.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_team.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_team.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_team.team_leader_id IS '团队负责人';
                    
      COMMENT ON COLUMN erp_crm_team.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_team.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_team.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_team.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_team.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_team.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_team.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_campaign IS '营销活动';
                
      COMMENT ON COLUMN erp_crm_campaign.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_campaign.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_campaign.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_campaign.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_campaign.campaign_name IS '活动名称';
                    
      COMMENT ON COLUMN erp_crm_campaign.medium IS 'UTM Medium';
                    
      COMMENT ON COLUMN erp_crm_campaign.source IS 'UTM Source';
                    
      COMMENT ON COLUMN erp_crm_campaign.start_date IS '开始日期';
                    
      COMMENT ON COLUMN erp_crm_campaign.end_date IS '结束日期';
                    
      COMMENT ON COLUMN erp_crm_campaign.budget_amount IS '预算金额';
                    
      COMMENT ON COLUMN erp_crm_campaign.actual_cost IS '实际成本';
                    
      COMMENT ON COLUMN erp_crm_campaign.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_campaign.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_campaign.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_campaign.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_campaign.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_campaign.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_campaign.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_score_config IS '评分规则配置';
                
      COMMENT ON COLUMN erp_crm_lead_score_config.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.config_name IS '规则集名称';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.is_active IS '是否生效';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.effective_from IS '生效开始日期';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.effective_to IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.auto_qualify_threshold IS '自动转商机阈值';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.min_score_for_follow_up IS '最低跟进分数';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_forecast_period IS '预测期间';
                
      COMMENT ON COLUMN erp_crm_forecast_period.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.period_type IS '期间类型';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.period_start IS '期间开始日期';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.period_end IS '期间结束日期';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.label IS '期间标签';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.status IS '期间状态';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.is_current IS '是否当前期间';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_forecast_period.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_territory IS '销售区域';
                
      COMMENT ON COLUMN erp_crm_territory.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_territory.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_territory.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_territory.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_territory.parent_id IS '父区域';
                    
      COMMENT ON COLUMN erp_crm_territory.territory_type IS '区域类型';
                    
      COMMENT ON COLUMN erp_crm_territory.manager_id IS '区域负责人';
                    
      COMMENT ON COLUMN erp_crm_territory.description IS '区域描述';
                    
      COMMENT ON COLUMN erp_crm_territory.full_path IS '路径冗余';
                    
      COMMENT ON COLUMN erp_crm_territory.level IS '层级深度';
                    
      COMMENT ON COLUMN erp_crm_territory.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_territory.is_leaf IS '是否叶子节点';
                    
      COMMENT ON COLUMN erp_crm_territory.sort_order IS '排序';
                    
      COMMENT ON COLUMN erp_crm_territory.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_territory.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_territory.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_territory.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_territory.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_territory.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_territory.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_product_configurator IS '产品配置器';
                
      COMMENT ON COLUMN erp_crm_product_configurator.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.product_type IS '适用产品品类';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.config_name IS '配置器名称';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.wizard_layout IS '向导步骤布局(JSON)';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.effective_from IS '生效开始日期';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.effective_to IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_product_configurator.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_bundle_pricing IS '捆绑定价';
                
      COMMENT ON COLUMN erp_crm_bundle_pricing.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.bundle_name IS '捆绑包名称';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.description IS '描述';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.discount_type IS '折扣类型';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.discount_value IS '折扣值';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.bundle_amount IS '捆绑包总价';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.effective_from IS '生效开始日期';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.effective_to IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_sequence IS '销售序列';
                
      COMMENT ON COLUMN erp_crm_sequence.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_sequence.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_sequence.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_sequence.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_sequence.template_type IS '序列模板类型';
                    
      COMMENT ON COLUMN erp_crm_sequence.description IS '描述';
                    
      COMMENT ON COLUMN erp_crm_sequence.expected_duration IS '预期完成天数';
                    
      COMMENT ON COLUMN erp_crm_sequence.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_sequence.is_default IS '是否默认序列';
                    
      COMMENT ON COLUMN erp_crm_sequence.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_sequence.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_sequence.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_sequence.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_sequence.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_sequence.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_sequence.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_price_rule IS '价格规则';
                
      COMMENT ON COLUMN erp_crm_price_rule.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_price_rule.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_price_rule.name IS '名称';
                    
      COMMENT ON COLUMN erp_crm_price_rule.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_price_rule.rule_type IS '规则类型';
                    
      COMMENT ON COLUMN erp_crm_price_rule.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_crm_price_rule.product_id IS '适用产品';
                    
      COMMENT ON COLUMN erp_crm_price_rule.product_category IS '适用产品品类';
                    
      COMMENT ON COLUMN erp_crm_price_rule.customer_id IS '适用客户';
                    
      COMMENT ON COLUMN erp_crm_price_rule.customer_category IS '适用客户类别';
                    
      COMMENT ON COLUMN erp_crm_price_rule.min_quantity IS '最小数量';
                    
      COMMENT ON COLUMN erp_crm_price_rule.max_quantity IS '最大数量';
                    
      COMMENT ON COLUMN erp_crm_price_rule.price_override IS '覆盖单价';
                    
      COMMENT ON COLUMN erp_crm_price_rule.discount_percent IS '折扣百分比';
                    
      COMMENT ON COLUMN erp_crm_price_rule.discount_amount IS '折扣固定金额';
                    
      COMMENT ON COLUMN erp_crm_price_rule.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_crm_price_rule.effective_from IS '生效开始日期';
                    
      COMMENT ON COLUMN erp_crm_price_rule.effective_to IS '生效结束日期';
                    
      COMMENT ON COLUMN erp_crm_price_rule.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_price_rule.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_price_rule.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_price_rule.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_price_rule.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_price_rule.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_price_rule.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_price_rule.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_stage IS '漏斗阶段';
                
      COMMENT ON COLUMN erp_crm_stage.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_stage.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_stage.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_stage.stage_name IS '阶段名称';
                    
      COMMENT ON COLUMN erp_crm_stage.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_stage.team_id IS '团队作用域';
                    
      COMMENT ON COLUMN erp_crm_stage.default_probability IS '默认成交概率%';
                    
      COMMENT ON COLUMN erp_crm_stage.is_won_stage IS '是否赢单阶段';
                    
      COMMENT ON COLUMN erp_crm_stage.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_stage.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_stage.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_stage.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_stage.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_stage.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_stage.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_score_config_line IS '评分准则明细';
                
      COMMENT ON COLUMN erp_crm_lead_score_config_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.config_id IS '评分规则配置';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.criterion_code IS '准则编码';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.criterion_name IS '准则名称';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.weight IS '权重系数';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.scoring_method IS '评分方法';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.lookup_table IS '值表(JSON)';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.formula IS '公式表达式';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.max_score IS '最高分';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_config_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_forecast IS '预测数据';
                
      COMMENT ON COLUMN erp_crm_forecast.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_forecast.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_forecast.period_id IS '预测期间';
                    
      COMMENT ON COLUMN erp_crm_forecast.territory_id IS '销售区域';
                    
      COMMENT ON COLUMN erp_crm_forecast.team_id IS '销售团队';
                    
      COMMENT ON COLUMN erp_crm_forecast.owner_id IS '销售员';
                    
      COMMENT ON COLUMN erp_crm_forecast.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_crm_forecast.commit_amount IS '承诺金额';
                    
      COMMENT ON COLUMN erp_crm_forecast.upside_amount IS '乐观金额';
                    
      COMMENT ON COLUMN erp_crm_forecast.weighted_amount IS '加权金额';
                    
      COMMENT ON COLUMN erp_crm_forecast.best_case_amount IS '最佳金额';
                    
      COMMENT ON COLUMN erp_crm_forecast.opportunity_count IS '商机总数';
                    
      COMMENT ON COLUMN erp_crm_forecast.commit_opportunity_count IS '承诺商机数';
                    
      COMMENT ON COLUMN erp_crm_forecast.expected_closed_revenue IS '实际已关闭收入';
                    
      COMMENT ON COLUMN erp_crm_forecast.last_calculated_at IS '最近计算时间';
                    
      COMMENT ON COLUMN erp_crm_forecast.notes IS '备注';
                    
      COMMENT ON COLUMN erp_crm_forecast.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_forecast.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_forecast.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_forecast.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_forecast.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_forecast.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_territory_assignment_rule IS '区域分配规则';
                
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.rule_name IS '规则名称';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.territory_id IS '目标区域';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.condition_type IS '条件类型';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.condition_value IS '条件值(JSON)';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.assignment_method IS '分配方法';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.group_id IS '目标团队';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.is_default IS '是否默认规则';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_territory_assignment_rule.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_quota IS '销售配额';
                
      COMMENT ON COLUMN erp_crm_quota.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_quota.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_quota.territory_id IS '区域';
                    
      COMMENT ON COLUMN erp_crm_quota.team_id IS '团队';
                    
      COMMENT ON COLUMN erp_crm_quota.owner_id IS '销售员';
                    
      COMMENT ON COLUMN erp_crm_quota.period_type IS '配额期间类型';
                    
      COMMENT ON COLUMN erp_crm_quota.fiscal_year IS '财年';
                    
      COMMENT ON COLUMN erp_crm_quota.period_label IS '期间标签';
                    
      COMMENT ON COLUMN erp_crm_quota.quota_amount IS '配额金额';
                    
      COMMENT ON COLUMN erp_crm_quota.currency_id IS '币种';
                    
      COMMENT ON COLUMN erp_crm_quota.is_finalized IS '是否已定稿';
                    
      COMMENT ON COLUMN erp_crm_quota.notes IS '备注';
                    
      COMMENT ON COLUMN erp_crm_quota.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_quota.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_quota.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_quota.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_quota.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_quota.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_funnel IS '线索漏斗';
                
      COMMENT ON COLUMN erp_crm_lead_funnel.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.funnel_name IS '漏斗名称';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.period_start IS '分析期间开始';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.period_end IS '分析期间结束';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.territory_id IS '区域维度';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.team_id IS '团队维度';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.source_id IS '来源维度';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.total_leads_at_top IS '漏斗顶部线索量';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.total_opportunities IS '商机总量';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.total_won IS '赢单总量';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.total_lost IS '丢单总量';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.total_revenue IS '赢单总收入';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.lost_revenue IS '丢失金额合计';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.weighted_revenue IS '加权收入合计';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.avg_deal_size IS '平均赢单金额';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.avg_sales_cycle_days IS '平均销售周期天数';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.calculated_at IS '聚合计算时间';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.calculated_by IS '聚合计算人';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_funnel.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_config_rule IS '配置规则';
                
      COMMENT ON COLUMN erp_crm_config_rule.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_config_rule.configurator_id IS '所属配置器';
                    
      COMMENT ON COLUMN erp_crm_config_rule.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_config_rule.rule_type IS '规则类型';
                    
      COMMENT ON COLUMN erp_crm_config_rule.source_feature_code IS '条件特征编码';
                    
      COMMENT ON COLUMN erp_crm_config_rule.source_feature_value IS '条件特征值';
                    
      COMMENT ON COLUMN erp_crm_config_rule.target_feature_code IS '目标特征编码';
                    
      COMMENT ON COLUMN erp_crm_config_rule.target_feature_value IS '目标特征值';
                    
      COMMENT ON COLUMN erp_crm_config_rule.condition_expression IS '复杂条件表达式';
                    
      COMMENT ON COLUMN erp_crm_config_rule.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_config_rule.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_config_rule.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_config_rule.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_config_rule.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_config_rule.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_config_rule.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_config_rule.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_bundle_pricing_line IS '捆绑包明细';
                
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.bundle_id IS '所属捆绑包';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.product_id IS '产品';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.quantity IS '数量';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.unit_price IS '单价';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_bundle_pricing_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_sequence_step IS '序列步骤';
                
      COMMENT ON COLUMN erp_crm_sequence_step.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.sequence_id IS '所属序列';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.step_name IS '步骤名称';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.step_order IS '步骤序号';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.due_days IS '距上一步间隔天数';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.activity_type IS '步骤活动类型';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.step_description IS '步骤说明';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.completion_condition IS '完成条件';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.is_mandatory IS '是否必须完成';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.auto_create_event IS '是否自动创建事件';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_sequence_step.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_sequence_assignment IS '序列分配规则';
                
      COMMENT ON COLUMN erp_crm_sequence_assignment.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.sequence_id IS '目标序列';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.condition_type IS '条件类型';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.condition_value IS '条件值(JSON)';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.is_active IS '是否启用';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.is_default IS '是否默认规则';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_sequence_assignment.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead IS '线索/商机';
                
      COMMENT ON COLUMN erp_crm_lead.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_lead.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead.lead_type IS '线索/商机类型';
                    
      COMMENT ON COLUMN erp_crm_lead.partner_id IS '客户';
                    
      COMMENT ON COLUMN erp_crm_lead.contact_name IS '联系人姓名';
                    
      COMMENT ON COLUMN erp_crm_lead.contact_phone IS '联系电话';
                    
      COMMENT ON COLUMN erp_crm_lead.contact_email IS '联系邮箱';
                    
      COMMENT ON COLUMN erp_crm_lead.company_name IS '公司名称';
                    
      COMMENT ON COLUMN erp_crm_lead.job_title IS '职位';
                    
      COMMENT ON COLUMN erp_crm_lead.department IS '部门';
                    
      COMMENT ON COLUMN erp_crm_lead.source_id IS '线索来源';
                    
      COMMENT ON COLUMN erp_crm_lead.lead_status_id IS '线索状态';
                    
      COMMENT ON COLUMN erp_crm_lead.stage_id IS '漏斗阶段';
                    
      COMMENT ON COLUMN erp_crm_lead.expected_revenue IS '预期收入';
                    
      COMMENT ON COLUMN erp_crm_lead.best_case_amount IS '乐观预期';
                    
      COMMENT ON COLUMN erp_crm_lead.worst_case_amount IS '悲观预期';
                    
      COMMENT ON COLUMN erp_crm_lead.recurring_revenue IS '周期性收入';
                    
      COMMENT ON COLUMN erp_crm_lead.recurring_plan IS '周期性计划';
                    
      COMMENT ON COLUMN erp_crm_lead.expected_close_date IS '预期签单日';
                    
      COMMENT ON COLUMN erp_crm_lead.probability IS '成交概率';
                    
      COMMENT ON COLUMN erp_crm_lead.campaign_id IS '营销活动';
                    
      COMMENT ON COLUMN erp_crm_lead.utm_medium IS 'UTM Medium';
                    
      COMMENT ON COLUMN erp_crm_lead.utm_source IS 'UTM Source';
                    
      COMMENT ON COLUMN erp_crm_lead.owner_id IS '负责人';
                    
      COMMENT ON COLUMN erp_crm_lead.team_id IS '销售团队';
                    
      COMMENT ON COLUMN erp_crm_lead.lost_reason_id IS '丢单原因';
                    
      COMMENT ON COLUMN erp_crm_lead.lost_reason_desc IS '丢单描述';
                    
      COMMENT ON COLUMN erp_crm_lead.last_contact_date IS '最后联系日期';
                    
      COMMENT ON COLUMN erp_crm_lead.next_activity_date IS '下次活动日期';
                    
      COMMENT ON COLUMN erp_crm_lead.related_bill_type IS '转化结果单据类型';
                    
      COMMENT ON COLUMN erp_crm_lead.related_bill_code IS '转化结果单据号';
                    
      COMMENT ON COLUMN erp_crm_lead.doc_status IS '单据状态';
                    
      COMMENT ON COLUMN erp_crm_lead.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead.update_time IS '修改时间';
                    
      COMMENT ON COLUMN erp_crm_lead.territory_id IS '销售区域';
                    
      COMMENT ON TABLE erp_crm_forecast_accuracy IS '预测准确率';
                
      COMMENT ON COLUMN erp_crm_forecast_accuracy.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.forecast_id IS '预测数据';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.period_id IS '预测期间';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.owner_id IS '销售员';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.team_id IS '销售团队';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.territory_id IS '销售区域';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.commit_amount IS '承诺金额';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.upside_amount IS '乐观金额';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.actual_closed_revenue IS '实际关闭收入';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.commit_accuracy IS '承诺准确率';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.upside_accuracy IS '乐观准确率';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.deviation_amount IS '偏差绝对值';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.calculated_by IS '计算人';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.calculated_at IS '计算时间';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_forecast_accuracy.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_funnel_stage_metrics IS '漏斗阶段度量';
                
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.funnel_id IS '所属漏斗';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.stage_id IS '阶段';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.stage_order IS '阶段排序';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.stage_name IS '阶段名称(快照)';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.lead_count_in IS '进入本阶段线索数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.lead_count_out IS '流出本阶段线索数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.lead_count_remaining IS '期末仍在本阶段数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.conversion_rate IS '转化率';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.drop_off_rate IS '流失率';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.avg_days_in_stage IS '平均停留天数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.lost_count IS '本阶段丢失数';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.lost_amount IS '本阶段丢失金额';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.lost_reason_top IS 'TOP丢失原因(JSON)';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_funnel_stage_metrics.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_event IS '活动/事件';
                
      COMMENT ON COLUMN erp_crm_event.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_event.code IS '编码';
                    
      COMMENT ON COLUMN erp_crm_event.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_event.event_type IS '事件类型';
                    
      COMMENT ON COLUMN erp_crm_event.event_category_id IS '活动类别';
                    
      COMMENT ON COLUMN erp_crm_event.subject IS '主题';
                    
      COMMENT ON COLUMN erp_crm_event.description IS '描述';
                    
      COMMENT ON COLUMN erp_crm_event.start_date_time IS '开始时间';
                    
      COMMENT ON COLUMN erp_crm_event.end_date_time IS '结束时间';
                    
      COMMENT ON COLUMN erp_crm_event.duration IS '时长(分钟)';
                    
      COMMENT ON COLUMN erp_crm_event.related_lead_id IS '关联线索/商机';
                    
      COMMENT ON COLUMN erp_crm_event.related_bill_type IS '关联单据类型';
                    
      COMMENT ON COLUMN erp_crm_event.related_bill_code IS '关联单据号';
                    
      COMMENT ON COLUMN erp_crm_event.partner_id IS '关联客户';
                    
      COMMENT ON COLUMN erp_crm_event.contact_id IS '联系人';
                    
      COMMENT ON COLUMN erp_crm_event.owner_id IS '负责人';
                    
      COMMENT ON COLUMN erp_crm_event.status IS '活动状态';
                    
      COMMENT ON COLUMN erp_crm_event.priority IS '优先级';
                    
      COMMENT ON COLUMN erp_crm_event.is_recurrent IS '是否重复事件';
                    
      COMMENT ON COLUMN erp_crm_event.parent_event_id IS '父事件';
                    
      COMMENT ON COLUMN erp_crm_event.reminder_minutes_before IS '提醒提前分钟数';
                    
      COMMENT ON COLUMN erp_crm_event.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_event.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_event.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_event.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_event.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_event.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_event.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_activity IS '活动记录';
                
      COMMENT ON COLUMN erp_crm_activity.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_activity.lead_id IS '关联线索/商机';
                    
      COMMENT ON COLUMN erp_crm_activity.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_activity.activity_type IS '活动类型';
                    
      COMMENT ON COLUMN erp_crm_activity.activity_date IS '活动日期';
                    
      COMMENT ON COLUMN erp_crm_activity.summary IS '内容摘要';
                    
      COMMENT ON COLUMN erp_crm_activity.owner_id IS '负责人';
                    
      COMMENT ON COLUMN erp_crm_activity.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_activity.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_activity.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_activity.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_activity.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_activity.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_activity.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_conv_log IS '阶段流转日志';
                
      COMMENT ON COLUMN erp_crm_lead_conv_log.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.lead_id IS '线索/商机';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.from_stage_id IS '前阶段';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.to_stage_id IS '后阶段';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.changed_at IS '变更时间';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.changed_by IS '变更人';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_conv_log.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_score IS '线索评分记录';
                
      COMMENT ON COLUMN erp_crm_lead_score.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_score.lead_id IS '线索/商机';
                    
      COMMENT ON COLUMN erp_crm_lead_score.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_score.config_id IS '评分规则版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score.total_score IS '总分';
                    
      COMMENT ON COLUMN erp_crm_lead_score.score_breakdown IS '评分快照(JSON)';
                    
      COMMENT ON COLUMN erp_crm_lead_score.auto_qualified IS '是否自动转商机';
                    
      COMMENT ON COLUMN erp_crm_lead_score.triggered_action IS '触发动作';
                    
      COMMENT ON COLUMN erp_crm_lead_score.calculated_at IS '计算时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score.trigger_event IS '触发计算事件';
                    
      COMMENT ON COLUMN erp_crm_lead_score.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_score.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_score.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_score.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_forecast_line IS '预测明细';
                
      COMMENT ON COLUMN erp_crm_forecast_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.forecast_id IS '预测数据';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.lead_id IS '商机';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.probability IS '概率快照';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.expected_revenue IS '预期收入快照';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.weighted_revenue IS '加权收入';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.forecast_category IS '预测分类';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.included_in_commit IS '是否计入承诺';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.stage_name IS '阶段名快照';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_forecast_line.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_seq_progress IS '线索序列进度';
                
      COMMENT ON COLUMN erp_crm_lead_seq_progress.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.lead_id IS '线索/商机';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.sequence_id IS '应用序列';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.current_step_index IS '当前步骤序号';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.status IS '状态';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.started_at IS '开始时间';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.completed_at IS '完成时间';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.remark IS '备注';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_seq_progress.update_time IS '修改时间';
                    
      COMMENT ON TABLE erp_crm_lead_score_line IS '评分记录明细';
                
      COMMENT ON COLUMN erp_crm_lead_score_line.id IS 'ID';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.score_id IS '评分记录';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.org_id IS '业务组织';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.config_line_id IS '对应准则行';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.criterion_code IS '准则编码(快照)';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.criterion_name IS '准则名称(快照)';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.raw_value IS '原始值';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.lookup_value IS '匹配档次';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.raw_score IS '原始得分';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.weighted_score IS '加权得分';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.sequence IS '排序';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.del_version IS '逻辑删除版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.version IS '数据版本';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.created_by IS '创建人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.create_time IS '创建时间';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.updated_by IS '修改人';
                    
      COMMENT ON COLUMN erp_crm_lead_score_line.update_time IS '修改时间';
                    
