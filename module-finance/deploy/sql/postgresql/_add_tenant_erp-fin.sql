
    alter table erp_md_acct_schema add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_organization add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_subject add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_currency add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_partner add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_warehouse add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_material add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_prj_project add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_cost_center add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_employee add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_ast_asset add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher_template add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_accounting_period add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_fund_account add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_reconciliation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_notes_receivable add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_employee_advance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher_template_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_accounting_period_status add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_ar_ap_item add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_gl_balance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_trial_balance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_bank_statement add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_credit_facility add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_cash_forecast add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_notes_discount add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_expense_claim add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_voucher_bill_r add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_reconciliation_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_bank_reconciliation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_notes_payable add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_expense_claim_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_bank_statement_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_fin_bank_reconciliation_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_md_acct_schema drop constraint PK_erp_md_acct_schema;
alter table erp_md_acct_schema add constraint PK_erp_md_acct_schema primary key (NOP_TENANT_ID, id);

alter table erp_md_organization drop constraint PK_erp_md_organization;
alter table erp_md_organization add constraint PK_erp_md_organization primary key (NOP_TENANT_ID, id);

alter table erp_md_subject drop constraint PK_erp_md_subject;
alter table erp_md_subject add constraint PK_erp_md_subject primary key (NOP_TENANT_ID, id);

alter table erp_md_currency drop constraint PK_erp_md_currency;
alter table erp_md_currency add constraint PK_erp_md_currency primary key (NOP_TENANT_ID, id);

alter table erp_md_partner drop constraint PK_erp_md_partner;
alter table erp_md_partner add constraint PK_erp_md_partner primary key (NOP_TENANT_ID, id);

alter table erp_md_warehouse drop constraint PK_erp_md_warehouse;
alter table erp_md_warehouse add constraint PK_erp_md_warehouse primary key (NOP_TENANT_ID, id);

alter table erp_md_material drop constraint PK_erp_md_material;
alter table erp_md_material add constraint PK_erp_md_material primary key (NOP_TENANT_ID, id);

alter table erp_prj_project drop constraint PK_erp_prj_project;
alter table erp_prj_project add constraint PK_erp_prj_project primary key (NOP_TENANT_ID, id);

alter table erp_md_cost_center drop constraint PK_erp_md_cost_center;
alter table erp_md_cost_center add constraint PK_erp_md_cost_center primary key (NOP_TENANT_ID, id);

alter table erp_md_employee drop constraint PK_erp_md_employee;
alter table erp_md_employee add constraint PK_erp_md_employee primary key (NOP_TENANT_ID, id);

alter table erp_ast_asset drop constraint PK_erp_ast_asset;
alter table erp_ast_asset add constraint PK_erp_ast_asset primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher_template drop constraint PK_erp_fin_voucher_template;
alter table erp_fin_voucher_template add constraint PK_erp_fin_voucher_template primary key (NOP_TENANT_ID, id);

alter table erp_fin_accounting_period drop constraint PK_erp_fin_accounting_period;
alter table erp_fin_accounting_period add constraint PK_erp_fin_accounting_period primary key (NOP_TENANT_ID, id);

alter table erp_fin_fund_account drop constraint PK_erp_fin_fund_account;
alter table erp_fin_fund_account add constraint PK_erp_fin_fund_account primary key (NOP_TENANT_ID, id);

alter table erp_fin_reconciliation drop constraint PK_erp_fin_reconciliation;
alter table erp_fin_reconciliation add constraint PK_erp_fin_reconciliation primary key (NOP_TENANT_ID, id);

alter table erp_fin_notes_receivable drop constraint PK_erp_fin_notes_receivable;
alter table erp_fin_notes_receivable add constraint PK_erp_fin_notes_receivable primary key (NOP_TENANT_ID, id);

alter table erp_fin_employee_advance drop constraint PK_erp_fin_employee_advance;
alter table erp_fin_employee_advance add constraint PK_erp_fin_employee_advance primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher_template_line drop constraint PK_erp_fin_voucher_template_line;
alter table erp_fin_voucher_template_line add constraint PK_erp_fin_voucher_template_line primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher drop constraint PK_erp_fin_voucher;
alter table erp_fin_voucher add constraint PK_erp_fin_voucher primary key (NOP_TENANT_ID, id);

alter table erp_fin_accounting_period_status drop constraint PK_erp_fin_accounting_period_status;
alter table erp_fin_accounting_period_status add constraint PK_erp_fin_accounting_period_status primary key (NOP_TENANT_ID, id);

alter table erp_fin_ar_ap_item drop constraint PK_erp_fin_ar_ap_item;
alter table erp_fin_ar_ap_item add constraint PK_erp_fin_ar_ap_item primary key (NOP_TENANT_ID, id);

alter table erp_fin_gl_balance drop constraint PK_erp_fin_gl_balance;
alter table erp_fin_gl_balance add constraint PK_erp_fin_gl_balance primary key (NOP_TENANT_ID, id);

alter table erp_fin_trial_balance drop constraint PK_erp_fin_trial_balance;
alter table erp_fin_trial_balance add constraint PK_erp_fin_trial_balance primary key (NOP_TENANT_ID, id);

alter table erp_fin_bank_statement drop constraint PK_erp_fin_bank_statement;
alter table erp_fin_bank_statement add constraint PK_erp_fin_bank_statement primary key (NOP_TENANT_ID, id);

alter table erp_fin_credit_facility drop constraint PK_erp_fin_credit_facility;
alter table erp_fin_credit_facility add constraint PK_erp_fin_credit_facility primary key (NOP_TENANT_ID, id);

alter table erp_fin_cash_forecast drop constraint PK_erp_fin_cash_forecast;
alter table erp_fin_cash_forecast add constraint PK_erp_fin_cash_forecast primary key (NOP_TENANT_ID, id);

alter table erp_fin_notes_discount drop constraint PK_erp_fin_notes_discount;
alter table erp_fin_notes_discount add constraint PK_erp_fin_notes_discount primary key (NOP_TENANT_ID, id);

alter table erp_fin_expense_claim drop constraint PK_erp_fin_expense_claim;
alter table erp_fin_expense_claim add constraint PK_erp_fin_expense_claim primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher_line drop constraint PK_erp_fin_voucher_line;
alter table erp_fin_voucher_line add constraint PK_erp_fin_voucher_line primary key (NOP_TENANT_ID, id);

alter table erp_fin_voucher_bill_r drop constraint PK_erp_fin_voucher_bill_r;
alter table erp_fin_voucher_bill_r add constraint PK_erp_fin_voucher_bill_r primary key (NOP_TENANT_ID, id);

alter table erp_fin_reconciliation_line drop constraint PK_erp_fin_reconciliation_line;
alter table erp_fin_reconciliation_line add constraint PK_erp_fin_reconciliation_line primary key (NOP_TENANT_ID, id);

alter table erp_fin_bank_reconciliation drop constraint PK_erp_fin_bank_reconciliation;
alter table erp_fin_bank_reconciliation add constraint PK_erp_fin_bank_reconciliation primary key (NOP_TENANT_ID, id);

alter table erp_fin_notes_payable drop constraint PK_erp_fin_notes_payable;
alter table erp_fin_notes_payable add constraint PK_erp_fin_notes_payable primary key (NOP_TENANT_ID, id);

alter table erp_fin_expense_claim_line drop constraint PK_erp_fin_expense_claim_line;
alter table erp_fin_expense_claim_line add constraint PK_erp_fin_expense_claim_line primary key (NOP_TENANT_ID, id);

alter table erp_fin_bank_statement_line drop constraint PK_erp_fin_bank_statement_line;
alter table erp_fin_bank_statement_line add constraint PK_erp_fin_bank_statement_line primary key (NOP_TENANT_ID, id);

alter table erp_fin_bank_reconciliation_line drop constraint PK_erp_fin_bank_reconciliation_line;
alter table erp_fin_bank_reconciliation_line add constraint PK_erp_fin_bank_reconciliation_line primary key (NOP_TENANT_ID, id);


