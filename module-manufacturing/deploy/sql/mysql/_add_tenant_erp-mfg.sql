
    alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_sku add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_workcenter add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_routing add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_location add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_batch add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_routing_operation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_workcenter_calendar add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_workcenter_capacity add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_mrp_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_forecast add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_cost_rollup add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom_byproduct add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_production_version add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom_operation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_mrp_plan_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_mrp_demand add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_forecast_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_cost_rollup_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_work_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_crp_load add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_work_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_subcontract_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_job_card add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_cost_variance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_subcontract_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_material_issue add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_job_card_time_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_batch_genealogy add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_material_issue_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material drop primary key;
alter table erp_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material_sku drop primary key;
alter table erp_md_material_sku add primary key (NOP_TENANT_ID, ID);

alter table erp_md_uom drop primary key;
alter table erp_md_uom add primary key (NOP_TENANT_ID, ID);

alter table erp_md_warehouse drop primary key;
alter table erp_md_warehouse add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_workcenter drop primary key;
alter table erp_mfg_workcenter add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_routing drop primary key;
alter table erp_mfg_routing add primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_md_location drop primary key;
alter table erp_md_location add primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_md_employee drop primary key;
alter table erp_md_employee add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_batch drop primary key;
alter table erp_inv_batch add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_bom drop primary key;
alter table erp_mfg_bom add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_routing_operation drop primary key;
alter table erp_mfg_routing_operation add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_workcenter_calendar drop primary key;
alter table erp_mfg_workcenter_calendar add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_workcenter_capacity drop primary key;
alter table erp_mfg_workcenter_capacity add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_mrp_plan drop primary key;
alter table erp_mfg_mrp_plan add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_forecast drop primary key;
alter table erp_mfg_forecast add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_cost_rollup drop primary key;
alter table erp_mfg_cost_rollup add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_bom_byproduct drop primary key;
alter table erp_mfg_bom_byproduct add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_production_version drop primary key;
alter table erp_mfg_production_version add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_bom_operation drop primary key;
alter table erp_mfg_bom_operation add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_mrp_plan_line drop primary key;
alter table erp_mfg_mrp_plan_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_mrp_demand drop primary key;
alter table erp_mfg_mrp_demand add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_forecast_line drop primary key;
alter table erp_mfg_forecast_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_cost_rollup_line drop primary key;
alter table erp_mfg_cost_rollup_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_work_order drop primary key;
alter table erp_mfg_work_order add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_bom_line drop primary key;
alter table erp_mfg_bom_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_crp_load drop primary key;
alter table erp_mfg_crp_load add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_work_order_line drop primary key;
alter table erp_mfg_work_order_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_subcontract_order drop primary key;
alter table erp_mfg_subcontract_order add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_job_card drop primary key;
alter table erp_mfg_job_card add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_cost_variance drop primary key;
alter table erp_mfg_cost_variance add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_subcontract_order_line drop primary key;
alter table erp_mfg_subcontract_order_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_material_issue drop primary key;
alter table erp_mfg_material_issue add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_job_card_time_log drop primary key;
alter table erp_mfg_job_card_time_log add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_batch_genealogy drop primary key;
alter table erp_mfg_batch_genealogy add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_material_issue_line drop primary key;
alter table erp_mfg_material_issue_line add primary key (NOP_TENANT_ID, ID);


