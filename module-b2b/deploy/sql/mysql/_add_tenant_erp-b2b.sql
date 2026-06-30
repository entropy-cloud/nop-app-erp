
    alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_edi_format add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_code_mapping add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_edi_doc add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_asn add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_edi_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_asn_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization drop primary key;
alter table erp_md_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_partner drop primary key;
alter table erp_md_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_md_md_material drop primary key;
alter table erp_md_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_b2b_edi_format drop primary key;
alter table erp_b2b_edi_format add primary key (NOP_TENANT_ID, ID);

alter table erp_b2b_code_mapping drop primary key;
alter table erp_b2b_code_mapping add primary key (NOP_TENANT_ID, ID);

alter table erp_b2b_edi_doc drop primary key;
alter table erp_b2b_edi_doc add primary key (NOP_TENANT_ID, ID);

alter table erp_b2b_asn drop primary key;
alter table erp_b2b_asn add primary key (NOP_TENANT_ID, ID);

alter table erp_b2b_edi_log drop primary key;
alter table erp_b2b_edi_log add primary key (NOP_TENANT_ID, ID);

alter table erp_b2b_asn_line drop primary key;
alter table erp_b2b_asn_line add primary key (NOP_TENANT_ID, ID);


