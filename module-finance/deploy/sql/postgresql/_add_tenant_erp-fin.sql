
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

alter table erp_fin_accounting_period drop constraint PK_erp_fin_accounting_period;
alter table erp_fin_accounting_period add constraint PK_erp_fin_accounting_period primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher_template drop constraint PK_erp_fin_voucher_template;
alter table erp_fin_voucher_template add constraint PK_erp_fin_voucher_template primary key (NOP_TENANT_ID, id);

alter table erp_fin_ar_ap_item drop constraint PK_erp_fin_ar_ap_item;
alter table erp_fin_ar_ap_item add constraint PK_erp_fin_ar_ap_item primary key (NOP_TENANT_ID, id);

alter table erp_fin_reconciliation drop constraint PK_erp_fin_reconciliation;
alter table erp_fin_reconciliation add constraint PK_erp_fin_reconciliation primary key (NOP_TENANT_ID, id);

alter table erp_fin_fund_account drop constraint PK_erp_fin_fund_account;
alter table erp_fin_fund_account add constraint PK_erp_fin_fund_account primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher drop constraint PK_erp_fin_voucher;
alter table erp_fin_voucher add constraint PK_erp_fin_voucher primary key (NOP_TENANT_ID, id);

alter table erp_fin_accounting_period_status drop constraint PK_erp_fin_accounting_period_status;
alter table erp_fin_accounting_period_status add constraint PK_erp_fin_accounting_period_status primary key (NOP_TENANT_ID, id);

alter table erp_fin_gl_balance drop constraint PK_erp_fin_gl_balance;
alter table erp_fin_gl_balance add constraint PK_erp_fin_gl_balance primary key (NOP_TENANT_ID, id);

alter table erp_fin_trial_balance drop constraint PK_erp_fin_trial_balance;
alter table erp_fin_trial_balance add constraint PK_erp_fin_trial_balance primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher_template_line drop constraint PK_erp_fin_voucher_template_line;
alter table erp_fin_voucher_template_line add constraint PK_erp_fin_voucher_template_line primary key (NOP_TENANT_ID, id);

alter table erp_fin_reconciliation_line drop constraint PK_erp_fin_reconciliation_line;
alter table erp_fin_reconciliation_line add constraint PK_erp_fin_reconciliation_line primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher_line drop constraint PK_erp_fin_voucher_line;
alter table erp_fin_voucher_line add constraint PK_erp_fin_voucher_line primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher_bill_r drop constraint PK_erp_fin_voucher_bill_r;
alter table erp_fin_voucher_bill_r add constraint PK_erp_fin_voucher_bill_r primary key (NOP_TENANT_ID, id);


