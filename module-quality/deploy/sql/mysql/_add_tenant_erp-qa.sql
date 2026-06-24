
    alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_inspection_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_risk_register add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_review add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_sampling_plan add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_inspection_template_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_quality_goal add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_calibration add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_inspection add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_inspection_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_non_conformance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_qa_action add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material drop primary key;
alter table erp_md_material add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_inspection_template drop primary key;
alter table erp_qa_inspection_template add primary key (NOP_TENANT_ID, ID);

alter table erp_md_partner drop primary key;
alter table erp_md_partner add primary key (NOP_TENANT_ID, ID);

alter table erp_md_warehouse drop primary key;
alter table erp_md_warehouse add primary key (NOP_TENANT_ID, ID);

alter table erp_md_employee drop primary key;
alter table erp_md_employee add primary key (NOP_TENANT_ID, ID);

alter table erp_md_organization drop primary key;
alter table erp_md_organization add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_risk_register drop primary key;
alter table erp_qa_risk_register add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_review drop primary key;
alter table erp_qa_review add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_sampling_plan drop primary key;
alter table erp_qa_sampling_plan add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_inspection_template_line drop primary key;
alter table erp_qa_inspection_template_line add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_quality_goal drop primary key;
alter table erp_qa_quality_goal add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_calibration drop primary key;
alter table erp_qa_calibration add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_inspection drop primary key;
alter table erp_qa_inspection add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_inspection_line drop primary key;
alter table erp_qa_inspection_line add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_non_conformance drop primary key;
alter table erp_qa_non_conformance add primary key (NOP_TENANT_ID, ID);

alter table erp_qa_action drop primary key;
alter table erp_qa_action add primary key (NOP_TENANT_ID, ID);


