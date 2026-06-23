
    alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_inspection_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_non_conformance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_risk_register add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_review add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_sampling_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_quality_goal add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_calibration add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_inspection add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_inspection_template_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_action add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_inspection_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material drop constraint PK_erp_md_material;
alter table erp_md_material add constraint PK_erp_md_material primary key (NOP_TENANT_ID, id);

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, id);

alter table erp_md_warehouse drop constraint PK_erp_md_warehouse;
alter table erp_md_warehouse add constraint PK_erp_md_warehouse primary key (NOP_TENANT_ID, id);

alter table erp_md_employee drop constraint PK_erp_md_employee;
alter table erp_md_employee add constraint PK_erp_md_employee primary key (NOP_TENANT_ID, id);

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_qa_inspection_template drop constraint PK_erp_qa_inspection_template;
alter table erp_qa_inspection_template add constraint PK_erp_qa_inspection_template primary key (NOP_TENANT_ID, id);

alter table erp_qa_non_conformance drop constraint PK_erp_qa_non_conformance;
alter table erp_qa_non_conformance add constraint PK_erp_qa_non_conformance primary key (NOP_TENANT_ID, id);

alter table erp_qa_risk_register drop constraint PK_erp_qa_risk_register;
alter table erp_qa_risk_register add constraint PK_erp_qa_risk_register primary key (NOP_TENANT_ID, id);

alter table erp_qa_review drop constraint PK_erp_qa_review;
alter table erp_qa_review add constraint PK_erp_qa_review primary key (NOP_TENANT_ID, id);

alter table erp_qa_sampling_plan drop constraint PK_erp_qa_sampling_plan;
alter table erp_qa_sampling_plan add constraint PK_erp_qa_sampling_plan primary key (NOP_TENANT_ID, id);

alter table erp_qa_quality_goal drop constraint PK_erp_qa_quality_goal;
alter table erp_qa_quality_goal add constraint PK_erp_qa_quality_goal primary key (NOP_TENANT_ID, id);

alter table erp_qa_calibration drop constraint PK_erp_qa_calibration;
alter table erp_qa_calibration add constraint PK_erp_qa_calibration primary key (NOP_TENANT_ID, id);

alter table erp_qa_inspection drop constraint PK_erp_qa_inspection;
alter table erp_qa_inspection add constraint PK_erp_qa_inspection primary key (NOP_TENANT_ID, id);

alter table erp_qa_inspection_template_line drop constraint PK_erp_qa_inspection_template_line;
alter table erp_qa_inspection_template_line add constraint PK_erp_qa_inspection_template_line primary key (NOP_TENANT_ID, id);

alter table erp_qa_action drop constraint PK_erp_qa_action;
alter table erp_qa_action add constraint PK_erp_qa_action primary key (NOP_TENANT_ID, id);

alter table erp_qa_inspection_line drop constraint PK_erp_qa_inspection_line;
alter table erp_qa_inspection_line add constraint PK_erp_qa_inspection_line primary key (NOP_TENANT_ID, id);


