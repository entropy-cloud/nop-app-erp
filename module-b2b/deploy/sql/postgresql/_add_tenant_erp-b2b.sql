
    alter table erp_md_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_edi_format add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_code_mapping add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_partner_profile add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_mft_config add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_mft_certificate add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_edi_doc add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_partner_credential add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_test_exchange add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_certification_checklist add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_mft_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_asn add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_edi_log add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_b2b_asn_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_md_organization drop constraint PK_erp_md_md_organization;
alter table erp_md_md_organization add constraint PK_erp_md_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_md_md_partner drop constraint PK_erp_md_md_partner;
alter table erp_md_md_partner add constraint PK_erp_md_md_partner primary key (NOP_TENANT_ID, id);

alter table erp_md_md_material drop constraint PK_erp_md_md_material;
alter table erp_md_md_material add constraint PK_erp_md_md_material primary key (NOP_TENANT_ID, id);

alter table erp_b2b_edi_format drop constraint PK_erp_b2b_edi_format;
alter table erp_b2b_edi_format add constraint PK_erp_b2b_edi_format primary key (NOP_TENANT_ID, id);

alter table erp_b2b_code_mapping drop constraint PK_erp_b2b_code_mapping;
alter table erp_b2b_code_mapping add constraint PK_erp_b2b_code_mapping primary key (NOP_TENANT_ID, id);

alter table erp_b2b_partner_profile drop constraint PK_erp_b2b_partner_profile;
alter table erp_b2b_partner_profile add constraint PK_erp_b2b_partner_profile primary key (NOP_TENANT_ID, id);

alter table erp_b2b_mft_config drop constraint PK_erp_b2b_mft_config;
alter table erp_b2b_mft_config add constraint PK_erp_b2b_mft_config primary key (NOP_TENANT_ID, id);

alter table erp_b2b_mft_certificate drop constraint PK_erp_b2b_mft_certificate;
alter table erp_b2b_mft_certificate add constraint PK_erp_b2b_mft_certificate primary key (NOP_TENANT_ID, id);

alter table erp_b2b_edi_doc drop constraint PK_erp_b2b_edi_doc;
alter table erp_b2b_edi_doc add constraint PK_erp_b2b_edi_doc primary key (NOP_TENANT_ID, id);

alter table erp_b2b_partner_credential drop constraint PK_erp_b2b_partner_credential;
alter table erp_b2b_partner_credential add constraint PK_erp_b2b_partner_credential primary key (NOP_TENANT_ID, id);

alter table erp_b2b_test_exchange drop constraint PK_erp_b2b_test_exchange;
alter table erp_b2b_test_exchange add constraint PK_erp_b2b_test_exchange primary key (NOP_TENANT_ID, id);

alter table erp_b2b_certification_checklist drop constraint PK_erp_b2b_certification_checklist;
alter table erp_b2b_certification_checklist add constraint PK_erp_b2b_certification_checklist primary key (NOP_TENANT_ID, id);

alter table erp_b2b_mft_log drop constraint PK_erp_b2b_mft_log;
alter table erp_b2b_mft_log add constraint PK_erp_b2b_mft_log primary key (NOP_TENANT_ID, id);

alter table erp_b2b_asn drop constraint PK_erp_b2b_asn;
alter table erp_b2b_asn add constraint PK_erp_b2b_asn primary key (NOP_TENANT_ID, id);

alter table erp_b2b_edi_log drop constraint PK_erp_b2b_edi_log;
alter table erp_b2b_edi_log add constraint PK_erp_b2b_edi_log primary key (NOP_TENANT_ID, id);

alter table erp_b2b_asn_line drop constraint PK_erp_b2b_asn_line;
alter table erp_b2b_asn_line add constraint PK_erp_b2b_asn_line primary key (NOP_TENANT_ID, id);


