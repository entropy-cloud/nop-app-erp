
    alter table erp_fin_accounting_period add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_ar_ap_item add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_reconciliation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_fund_account add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_accounting_period_status add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_gl_balance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_trial_balance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher_template_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_reconciliation_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher_bill_r add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_accounting_period drop primary key;
alter table erp_fin_accounting_period add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_voucher_template drop primary key;
alter table erp_fin_voucher_template add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_ar_ap_item drop primary key;
alter table erp_fin_ar_ap_item add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_reconciliation drop primary key;
alter table erp_fin_reconciliation add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_fund_account drop primary key;
alter table erp_fin_fund_account add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_voucher drop primary key;
alter table erp_fin_voucher add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_accounting_period_status drop primary key;
alter table erp_fin_accounting_period_status add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_gl_balance drop primary key;
alter table erp_fin_gl_balance add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_trial_balance drop primary key;
alter table erp_fin_trial_balance add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_voucher_template_line drop primary key;
alter table erp_fin_voucher_template_line add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_reconciliation_line drop primary key;
alter table erp_fin_reconciliation_line add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_voucher_line drop primary key;
alter table erp_fin_voucher_line add primary key (NOP_TENANT_ID, ID);

alter table erp_fin_voucher_bill_r drop primary key;
alter table erp_fin_voucher_bill_r add primary key (NOP_TENANT_ID, ID);


