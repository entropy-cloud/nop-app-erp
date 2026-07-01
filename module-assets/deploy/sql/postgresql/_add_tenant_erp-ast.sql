
    alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_location add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_subject add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

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

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_md_location drop constraint PK_erp_md_location;
alter table erp_md_location add constraint PK_erp_md_location primary key (NOP_TENANT_ID, id);

alter table erp_md_employee drop constraint PK_erp_md_employee;
alter table erp_md_employee add constraint PK_erp_md_employee primary key (NOP_TENANT_ID, id);

alter table erp_md_currency drop constraint PK_erp_md_currency;
alter table erp_md_currency add constraint PK_erp_md_currency primary key (NOP_TENANT_ID, id);

alter table erp_md_subject drop constraint PK_erp_md_subject;
alter table erp_md_subject add constraint PK_erp_md_subject primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher drop constraint PK_erp_fin_voucher;
alter table erp_fin_voucher add constraint PK_erp_fin_voucher primary key (NOP_TENANT_ID, id);

alter table erp_md_material_category drop constraint PK_erp_md_material_category;
alter table erp_md_material_category add constraint PK_erp_md_material_category primary key (NOP_TENANT_ID, id);

alter table erp_ast_asset_category drop constraint PK_erp_ast_asset_category;
alter table erp_ast_asset_category add constraint PK_erp_ast_asset_category primary key (NOP_TENANT_ID, id);

alter table erp_ast_asset drop constraint PK_erp_ast_asset;
alter table erp_ast_asset add constraint PK_erp_ast_asset primary key (NOP_TENANT_ID, id);

alter table erp_ast_asset_capitalization drop constraint PK_erp_ast_asset_capitalization;
alter table erp_ast_asset_capitalization add constraint PK_erp_ast_asset_capitalization primary key (NOP_TENANT_ID, id);

alter table erp_ast_depreciation_schedule drop constraint PK_erp_ast_depreciation_schedule;
alter table erp_ast_depreciation_schedule add constraint PK_erp_ast_depreciation_schedule primary key (NOP_TENANT_ID, id);

alter table erp_ast_movement drop constraint PK_erp_ast_movement;
alter table erp_ast_movement add constraint PK_erp_ast_movement primary key (NOP_TENANT_ID, id);

alter table erp_ast_value_adjustment drop constraint PK_erp_ast_value_adjustment;
alter table erp_ast_value_adjustment add constraint PK_erp_ast_value_adjustment primary key (NOP_TENANT_ID, id);

alter table erp_ast_disposal drop constraint PK_erp_ast_disposal;
alter table erp_ast_disposal add constraint PK_erp_ast_disposal primary key (NOP_TENANT_ID, id);

alter table erp_ast_cip drop constraint PK_erp_ast_cip;
alter table erp_ast_cip add constraint PK_erp_ast_cip primary key (NOP_TENANT_ID, id);

alter table erp_ast_split drop constraint PK_erp_ast_split;
alter table erp_ast_split add constraint PK_erp_ast_split primary key (NOP_TENANT_ID, id);

alter table erp_ast_merge drop constraint PK_erp_ast_merge;
alter table erp_ast_merge add constraint PK_erp_ast_merge primary key (NOP_TENANT_ID, id);


