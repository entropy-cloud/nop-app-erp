-- app-erp-master-data indexes generated from model/app-erp-master-data.orm.xml
-- Dialect: oracle.
-- This file is maintained separately from _create_erp-master-data.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-master-data.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (32 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_MD_MATERIAL_CATEGORY_ID ON erp_md_material (category_id);
CREATE INDEX IDX_MD_MATERIAL_UOM_ID ON erp_md_material (uom_id);
CREATE INDEX IDX_MD_MATERIAL_DEFAULT_WAREHOUSE_ID ON erp_md_material (default_warehouse_id);
CREATE INDEX IDX_MD_MATERIAL_DEFAULT_TAX_RATE_ID ON erp_md_material (default_tax_rate_id);
CREATE INDEX IDX_MD_MATERIAL_CATEGORY_PARENT_ID ON erp_md_material_category (parent_id);
CREATE INDEX IDX_MD_MATERIAL_SKU_MATERIAL_ID ON erp_md_material_sku (material_id);
CREATE INDEX IDX_MD_MATERIAL_SKU_UOM_ID ON erp_md_material_sku (uom_id);
CREATE INDEX IDX_MD_MATERIAL_SKU_TAX_RATE_ID ON erp_md_material_sku (tax_rate_id);
CREATE INDEX IDX_MD_PARTNER_ADDRESS_PARTNER_ID ON erp_md_partner_address (partner_id);
CREATE INDEX IDX_MD_PARTNER_CONTACT_PARTNER_ID ON erp_md_partner_contact (partner_id);
CREATE INDEX IDX_MD_WAREHOUSE_MANAGER_ID ON erp_md_warehouse (manager_id);
CREATE INDEX IDX_MD_LOCATION_WAREHOUSE_ID ON erp_md_location (warehouse_id);
CREATE INDEX IDX_MD_LOCATION_PARENT_ID ON erp_md_location (parent_id);
CREATE INDEX IDX_MD_UOM_CONVERSION_MATERIAL_ID ON erp_md_uom_conversion (material_id);
CREATE INDEX IDX_MD_UOM_CONVERSION_FROM_UOM_ID ON erp_md_uom_conversion (from_uom_id);
CREATE INDEX IDX_MD_UOM_CONVERSION_TO_UOM_ID ON erp_md_uom_conversion (to_uom_id);
CREATE INDEX IDX_MD_EXCHANGE_RATE_FROM_CURRENCY_ID ON erp_md_exchange_rate (from_currency_id);
CREATE INDEX IDX_MD_EXCHANGE_RATE_TO_CURRENCY_ID ON erp_md_exchange_rate (to_currency_id);
CREATE INDEX IDX_MD_SETTLEMENT_METHOD_DEFAULT_FUND_ACCOUNT_ID ON erp_md_settlement_method (default_fund_account_id);
CREATE INDEX IDX_MD_BANK_ACCOUNT_PARTNER_ID ON erp_md_bank_account (partner_id);
CREATE INDEX IDX_MD_EMPLOYEE_PARTNER_ID ON erp_md_employee (partner_id);
CREATE INDEX IDX_MD_SUBJECT_PARENT_ID ON erp_md_subject (parent_id);
CREATE INDEX IDX_MD_SUBJECT_CURRENCY_ID ON erp_md_subject (currency_id);
CREATE INDEX IDX_MD_ACCT_SCHEMA_FUNCTIONAL_CURRENCY_ID ON erp_md_acct_schema (functional_currency_id);
CREATE INDEX IDX_MD_ACCT_SCHEMA_COA_ACCT_SCHEMA_ID ON erp_md_acct_schema_coa (acct_schema_id);
CREATE INDEX IDX_MD_ORGANIZATION_PARENT_ID ON erp_md_organization (parent_id);
CREATE INDEX IDX_MD_ORGANIZATION_FUNCTIONAL_CURRENCY_ID ON erp_md_organization (functional_currency_id);
CREATE INDEX IDX_MD_COST_CENTER_MANAGER_ID ON erp_md_cost_center (manager_id);
CREATE INDEX IDX_MD_COST_CENTER_PARENT_ID ON erp_md_cost_center (parent_id);
CREATE INDEX IDX_MD_SUPPLIER_APPROVAL_ORG_STATUS ON erp_md_supplier_approval (org_id, status);
CREATE INDEX IDX_MD_SUPPLIER_APPROVAL_PARTNER_ID ON erp_md_supplier_approval (partner_id);
CREATE INDEX IDX_MD_SUPPLIER_APPROVAL_MATERIAL_CATEGORY_ID ON erp_md_supplier_approval (material_category_id);
