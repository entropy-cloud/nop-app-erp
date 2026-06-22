
    alter table erp_ast_asset_category add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_asset add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_asset_capitalization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_cip add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_depreciation_schedule add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_movement add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_value_adjustment add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_disposal add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_split add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_merge add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_asset_category drop primary key;
alter table erp_ast_asset_category add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_asset drop primary key;
alter table erp_ast_asset add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_asset_capitalization drop primary key;
alter table erp_ast_asset_capitalization add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_cip drop primary key;
alter table erp_ast_cip add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_depreciation_schedule drop primary key;
alter table erp_ast_depreciation_schedule add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_movement drop primary key;
alter table erp_ast_movement add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_value_adjustment drop primary key;
alter table erp_ast_value_adjustment add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_disposal drop primary key;
alter table erp_ast_disposal add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_split drop primary key;
alter table erp_ast_split add primary key (NOP_TENANT_ID, ID);

alter table erp_ast_merge drop primary key;
alter table erp_ast_merge add primary key (NOP_TENANT_ID, ID);


