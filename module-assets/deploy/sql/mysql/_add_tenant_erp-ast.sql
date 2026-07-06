
    alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_location add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_subject add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material_category add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_asset_category add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_asset add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_asset_capitalization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_depreciation_schedule add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_movement add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_value_adjustment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_disposal add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_cip add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_split add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_merge add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_cip_cost_item add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_cip_progress_billing add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_split_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_merge_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_location drop primary key;
alter table erp_md_location add primary key (NOP_TENANT_ID, ID);

alter table erp_md_employee drop primary key;
alter table erp_md_employee add primary key (NOP_TENANT_ID, ID);

alter table erp_md_currency drop primary key;
alter table erp_md_currency add primary key (NOP_TENANT_ID, ID);

alter table erp_md_subject drop primary key;
alter table erp_md_subject add primary key (NOP_TENANT_ID, ID);

alter table erp_md_material_category drop primary key;
alter table erp_md_material_category add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_asset_category drop primary key;
alter table erp_ast_asset_category add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_asset drop primary key;
alter table erp_ast_asset add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_asset_capitalization drop primary key;
alter table erp_ast_asset_capitalization add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_depreciation_schedule drop primary key;
alter table erp_ast_depreciation_schedule add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_movement drop primary key;
alter table erp_ast_movement add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_value_adjustment drop primary key;
alter table erp_ast_value_adjustment add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_disposal drop primary key;
alter table erp_ast_disposal add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_cip drop primary key;
alter table erp_ast_cip add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_split drop primary key;
alter table erp_ast_split add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_merge drop primary key;
alter table erp_ast_merge add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_cip_cost_item drop primary key;
alter table erp_ast_cip_cost_item add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_cip_progress_billing drop primary key;
alter table erp_ast_cip_progress_billing add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_split_line drop primary key;
alter table erp_ast_split_line add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_merge_line drop primary key;
alter table erp_ast_merge_line add primary key (NOP_TENANT_ID, ID);


