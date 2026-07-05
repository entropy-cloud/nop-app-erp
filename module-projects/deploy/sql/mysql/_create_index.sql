-- app-erp-projects indexes generated from model/app-erp-projects.orm.xml
-- Dialect: mysql.
-- This file is maintained separately from _create_erp-projects.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-projects.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (45 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_PRJ_PROJECT_ORG_STATUS ON erp_prj_project (org_id, status);
CREATE INDEX IDX_PRJ_PROJECT_PROJECT_TYPE_ID ON erp_prj_project (project_type_id);
CREATE INDEX IDX_PRJ_PROJECT_CUSTOMER_ID ON erp_prj_project (customer_id);
CREATE INDEX IDX_PRJ_PROJECT_CURRENCY_ID ON erp_prj_project (currency_id);
CREATE INDEX IDX_PRJ_PROJECT_MANAGER_ID ON erp_prj_project (manager_id);
CREATE INDEX IDX_PRJ_TASK_STATUS ON erp_prj_task (status);
CREATE INDEX IDX_PRJ_TASK_PROJECT_ID ON erp_prj_task (project_id);
CREATE INDEX IDX_PRJ_TASK_PARENT_TASK_ID ON erp_prj_task (parent_task_id);
CREATE INDEX IDX_PRJ_TASK_ASSIGNEE_ID ON erp_prj_task (assignee_id);
CREATE INDEX IDX_PRJ_TASK_DEPENDS_ON_ID ON erp_prj_task (depends_on_id);
CREATE INDEX IDX_PRJ_TIMESHEET_ORG_STATUS ON erp_prj_timesheet (org_id, status);
CREATE INDEX IDX_PRJ_TIMESHEET_PROJECT_ID ON erp_prj_timesheet (project_id);
CREATE INDEX IDX_PRJ_TIMESHEET_TASK_ID ON erp_prj_timesheet (task_id);
CREATE INDEX IDX_PRJ_TIMESHEET_USER_ID ON erp_prj_timesheet (user_id);
CREATE INDEX IDX_PRJ_TIMESHEET_ACTIVITY_TYPE_ID ON erp_prj_timesheet (activity_type_id);
CREATE INDEX IDX_PRJ_TIMESHEET_CURRENCY_ID ON erp_prj_timesheet (currency_id);
CREATE INDEX IDX_PRJ_PROJECT_TYPE_DEFAULT_SUBJECT_ID ON erp_prj_project_type (default_subject_id);
CREATE INDEX IDX_PRJ_ACTIVITY_TYPE_SUBJECT_ID ON erp_prj_activity_type (subject_id);
CREATE INDEX IDX_PRJ_PROJECT_USER_PROJECT_ID ON erp_prj_project_user (project_id);
CREATE INDEX IDX_PRJ_PROJECT_USER_USER_ID ON erp_prj_project_user (user_id);
CREATE INDEX IDX_PRJ_BUDGET_ORG_DOC_STATUS ON erp_prj_budget (org_id, doc_status);
CREATE INDEX IDX_PRJ_BUDGET_ORG_BUSINESS_DATE ON erp_prj_budget (org_id, business_date);
CREATE INDEX IDX_PRJ_BUDGET_PROJECT_ID ON erp_prj_budget (project_id);
CREATE INDEX IDX_PRJ_BUDGET_CURRENCY_ID ON erp_prj_budget (currency_id);
CREATE INDEX IDX_PRJ_BUDGET_LINE_BUDGET_ID ON erp_prj_budget_line (budget_id);
CREATE INDEX IDX_PRJ_BUDGET_LINE_SUBJECT_ID ON erp_prj_budget_line (subject_id);
CREATE INDEX IDX_PRJ_BUDGET_LINE_TASK_ID ON erp_prj_budget_line (task_id);
CREATE INDEX IDX_PRJ_COST_COLLECTION_ORG_DOC_STATUS ON erp_prj_cost_collection (org_id, doc_status);
CREATE INDEX IDX_PRJ_COST_COLLECTION_ORG_BUSINESS_DATE ON erp_prj_cost_collection (org_id, business_date);
CREATE INDEX IDX_PRJ_COST_COLLECTION_PROJECT_ID ON erp_prj_cost_collection (project_id);
CREATE INDEX IDX_PRJ_COST_COLLECTION_CURRENCY_ID ON erp_prj_cost_collection (currency_id);
CREATE INDEX IDX_PRJ_COST_COLLECTION_LINE_COST_COLLECTION_ID ON erp_prj_cost_collection_line (cost_collection_id);
CREATE INDEX IDX_PRJ_COST_COLLECTION_LINE_SUBJECT_ID ON erp_prj_cost_collection_line (subject_id);
CREATE INDEX IDX_PRJ_COST_COLLECTION_LINE_TASK_ID ON erp_prj_cost_collection_line (task_id);
CREATE INDEX IDX_PRJ_MILESTONE_STATUS ON erp_prj_milestone (status);
CREATE INDEX IDX_PRJ_MILESTONE_PROJECT_ID ON erp_prj_milestone (project_id);
CREATE INDEX IDX_PRJ_BILLING_ORG_DOC_STATUS ON erp_prj_billing (org_id, doc_status);
CREATE INDEX IDX_PRJ_BILLING_ORG_BUSINESS_DATE ON erp_prj_billing (org_id, business_date);
CREATE INDEX IDX_PRJ_BILLING_PROJECT_ID ON erp_prj_billing (project_id);
CREATE INDEX IDX_PRJ_BILLING_CUSTOMER_ID ON erp_prj_billing (customer_id);
CREATE INDEX IDX_PRJ_BILLING_MILESTONE_ID ON erp_prj_billing (milestone_id);
CREATE INDEX IDX_PRJ_BILLING_CURRENCY_ID ON erp_prj_billing (currency_id);
CREATE INDEX IDX_PRJ_BILLING_LINE_BILLING_ID ON erp_prj_billing_line (billing_id);
CREATE INDEX IDX_PRJ_BILLING_LINE_TASK_ID ON erp_prj_billing_line (task_id);
CREATE INDEX IDX_PRJ_BILLING_LINE_SUBJECT_ID ON erp_prj_billing_line (subject_id);
