
    alter table erp_mfg_bom add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_workcenter add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_routing add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_mrp_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_cost_rollup add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom_byproduct add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom_operation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_routing_operation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_production_version add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_mrp_plan_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_mrp_demand add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_cost_rollup_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_work_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_work_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_subcontract_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_job_card add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_subcontract_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_material_issue add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_job_card_time_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_material_issue_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom drop primary key;
alter table erp_mfg_bom add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_workcenter drop primary key;
alter table erp_mfg_workcenter add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_routing drop primary key;
alter table erp_mfg_routing add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_mrp_plan drop primary key;
alter table erp_mfg_mrp_plan add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_cost_rollup drop primary key;
alter table erp_mfg_cost_rollup add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_bom_line drop primary key;
alter table erp_mfg_bom_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_bom_byproduct drop primary key;
alter table erp_mfg_bom_byproduct add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_bom_operation drop primary key;
alter table erp_mfg_bom_operation add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_routing_operation drop primary key;
alter table erp_mfg_routing_operation add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_production_version drop primary key;
alter table erp_mfg_production_version add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_mrp_plan_line drop primary key;
alter table erp_mfg_mrp_plan_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_mrp_demand drop primary key;
alter table erp_mfg_mrp_demand add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_cost_rollup_line drop primary key;
alter table erp_mfg_cost_rollup_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_work_order drop primary key;
alter table erp_mfg_work_order add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_work_order_line drop primary key;
alter table erp_mfg_work_order_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_subcontract_order drop primary key;
alter table erp_mfg_subcontract_order add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_job_card drop primary key;
alter table erp_mfg_job_card add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_subcontract_order_line drop primary key;
alter table erp_mfg_subcontract_order_line add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_material_issue drop primary key;
alter table erp_mfg_material_issue add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_job_card_time_log drop primary key;
alter table erp_mfg_job_card_time_log add primary key (NOP_TENANT_ID, ID);

alter table erp_mfg_material_issue_line drop primary key;
alter table erp_mfg_material_issue_line add primary key (NOP_TENANT_ID, ID);


