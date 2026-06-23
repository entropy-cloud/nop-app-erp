
    alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_sku add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_uom add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_workcenter add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_routing add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_mrp_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_cost_rollup add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_location add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_routing_operation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_mrp_plan_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_mrp_demand add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_cost_rollup_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom_operation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_bom_byproduct add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_production_version add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_work_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_work_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_subcontract_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_job_card add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_subcontract_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_material_issue add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_job_card_time_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_mfg_material_issue_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material drop constraint PK_erp_md_material;
alter table erp_md_material add constraint PK_erp_md_material primary key (NOP_TENANT_ID, id);

alter table erp_md_material_sku drop constraint PK_erp_md_material_sku;
alter table erp_md_material_sku add constraint PK_erp_md_material_sku primary key (NOP_TENANT_ID, id);

alter table erp_md_uom drop constraint PK_erp_md_uom;
alter table erp_md_uom add constraint PK_erp_md_uom primary key (NOP_TENANT_ID, id);

alter table erp_mfg_workcenter drop constraint PK_erp_mfg_workcenter;
alter table erp_mfg_workcenter add constraint PK_erp_mfg_workcenter primary key (NOP_TENANT_ID, id);

alter table erp_mfg_routing drop constraint PK_erp_mfg_routing;
alter table erp_mfg_routing add constraint PK_erp_mfg_routing primary key (NOP_TENANT_ID, id);

alter table erp_md_currency drop constraint PK_erp_md_currency;
alter table erp_md_currency add constraint PK_erp_md_currency primary key (NOP_TENANT_ID, id);

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_mfg_mrp_plan drop constraint PK_erp_mfg_mrp_plan;
alter table erp_mfg_mrp_plan add constraint PK_erp_mfg_mrp_plan primary key (NOP_TENANT_ID, id);

alter table erp_md_warehouse drop constraint PK_erp_md_warehouse;
alter table erp_md_warehouse add constraint PK_erp_md_warehouse primary key (NOP_TENANT_ID, id);

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, id);

alter table erp_mfg_cost_rollup drop constraint PK_erp_mfg_cost_rollup;
alter table erp_mfg_cost_rollup add constraint PK_erp_mfg_cost_rollup primary key (NOP_TENANT_ID, id);

alter table erp_md_location drop constraint PK_erp_md_location;
alter table erp_md_location add constraint PK_erp_md_location primary key (NOP_TENANT_ID, id);

alter table erp_mfg_bom drop constraint PK_erp_mfg_bom;
alter table erp_mfg_bom add constraint PK_erp_mfg_bom primary key (NOP_TENANT_ID, id);

alter table erp_mfg_routing_operation drop constraint PK_erp_mfg_routing_operation;
alter table erp_mfg_routing_operation add constraint PK_erp_mfg_routing_operation primary key (NOP_TENANT_ID, id);

alter table erp_mfg_mrp_plan_line drop constraint PK_erp_mfg_mrp_plan_line;
alter table erp_mfg_mrp_plan_line add constraint PK_erp_mfg_mrp_plan_line primary key (NOP_TENANT_ID, id);

alter table erp_mfg_mrp_demand drop constraint PK_erp_mfg_mrp_demand;
alter table erp_mfg_mrp_demand add constraint PK_erp_mfg_mrp_demand primary key (NOP_TENANT_ID, id);

alter table erp_mfg_cost_rollup_line drop constraint PK_erp_mfg_cost_rollup_line;
alter table erp_mfg_cost_rollup_line add constraint PK_erp_mfg_cost_rollup_line primary key (NOP_TENANT_ID, id);

alter table erp_mfg_bom_line drop constraint PK_erp_mfg_bom_line;
alter table erp_mfg_bom_line add constraint PK_erp_mfg_bom_line primary key (NOP_TENANT_ID, id);

alter table erp_mfg_bom_operation drop constraint PK_erp_mfg_bom_operation;
alter table erp_mfg_bom_operation add constraint PK_erp_mfg_bom_operation primary key (NOP_TENANT_ID, id);

alter table erp_mfg_bom_byproduct drop constraint PK_erp_mfg_bom_byproduct;
alter table erp_mfg_bom_byproduct add constraint PK_erp_mfg_bom_byproduct primary key (NOP_TENANT_ID, id);

alter table erp_mfg_production_version drop constraint PK_erp_mfg_production_version;
alter table erp_mfg_production_version add constraint PK_erp_mfg_production_version primary key (NOP_TENANT_ID, id);

alter table erp_mfg_work_order drop constraint PK_erp_mfg_work_order;
alter table erp_mfg_work_order add constraint PK_erp_mfg_work_order primary key (NOP_TENANT_ID, id);

alter table erp_mfg_work_order_line drop constraint PK_erp_mfg_work_order_line;
alter table erp_mfg_work_order_line add constraint PK_erp_mfg_work_order_line primary key (NOP_TENANT_ID, id);

alter table erp_mfg_subcontract_order drop constraint PK_erp_mfg_subcontract_order;
alter table erp_mfg_subcontract_order add constraint PK_erp_mfg_subcontract_order primary key (NOP_TENANT_ID, id);

alter table erp_mfg_job_card drop constraint PK_erp_mfg_job_card;
alter table erp_mfg_job_card add constraint PK_erp_mfg_job_card primary key (NOP_TENANT_ID, id);

alter table erp_mfg_subcontract_order_line drop constraint PK_erp_mfg_subcontract_order_line;
alter table erp_mfg_subcontract_order_line add constraint PK_erp_mfg_subcontract_order_line primary key (NOP_TENANT_ID, id);

alter table erp_mfg_material_issue drop constraint PK_erp_mfg_material_issue;
alter table erp_mfg_material_issue add constraint PK_erp_mfg_material_issue primary key (NOP_TENANT_ID, id);

alter table erp_mfg_job_card_time_log drop constraint PK_erp_mfg_job_card_time_log;
alter table erp_mfg_job_card_time_log add constraint PK_erp_mfg_job_card_time_log primary key (NOP_TENANT_ID, id);

alter table erp_mfg_material_issue_line drop constraint PK_erp_mfg_material_issue_line;
alter table erp_mfg_material_issue_line add constraint PK_erp_mfg_material_issue_line primary key (NOP_TENANT_ID, id);


