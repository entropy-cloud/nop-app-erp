
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

alter table erp_ast_asset_category drop constraint PK_erp_ast_asset_category;
alter table erp_ast_asset_category add constraint PK_erp_ast_asset_category primary key (NOP_TENANT_ID, id);

alter table erp_ast_asset drop constraint PK_erp_ast_asset;
alter table erp_ast_asset add constraint PK_erp_ast_asset primary key (NOP_TENANT_ID, id);

alter table erp_ast_asset_capitalization drop constraint PK_erp_ast_asset_capitalization;
alter table erp_ast_asset_capitalization add constraint PK_erp_ast_asset_capitalization primary key (NOP_TENANT_ID, id);

alter table erp_ast_cip drop constraint PK_erp_ast_cip;
alter table erp_ast_cip add constraint PK_erp_ast_cip primary key (NOP_TENANT_ID, id);

alter table erp_ast_depreciation_schedule drop constraint PK_erp_ast_depreciation_schedule;
alter table erp_ast_depreciation_schedule add constraint PK_erp_ast_depreciation_schedule primary key (NOP_TENANT_ID, id);

alter table erp_ast_movement drop constraint PK_erp_ast_movement;
alter table erp_ast_movement add constraint PK_erp_ast_movement primary key (NOP_TENANT_ID, id);

alter table erp_ast_value_adjustment drop constraint PK_erp_ast_value_adjustment;
alter table erp_ast_value_adjustment add constraint PK_erp_ast_value_adjustment primary key (NOP_TENANT_ID, id);

alter table erp_ast_disposal drop constraint PK_erp_ast_disposal;
alter table erp_ast_disposal add constraint PK_erp_ast_disposal primary key (NOP_TENANT_ID, id);

alter table erp_ast_split drop constraint PK_erp_ast_split;
alter table erp_ast_split add constraint PK_erp_ast_split primary key (NOP_TENANT_ID, id);

alter table erp_ast_merge drop constraint PK_erp_ast_merge;
alter table erp_ast_merge add constraint PK_erp_ast_merge primary key (NOP_TENANT_ID, id);


