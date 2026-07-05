-- app-erp-sales indexes generated from model/app-erp-sales.orm.xml
-- Dialect: oracle.
-- This file is maintained separately from _create_erp-sales.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-sales.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (66 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_SAL_QUOTATION_ORG_DOC_STATUS ON erp_sal_quotation (org_id, doc_status);
CREATE INDEX IDX_SAL_QUOTATION_ORG_BUSINESS_DATE ON erp_sal_quotation (org_id, business_date);
CREATE INDEX IDX_SAL_QUOTATION_CUSTOMER_ID ON erp_sal_quotation (customer_id);
CREATE INDEX IDX_SAL_QUOTATION_CURRENCY_ID ON erp_sal_quotation (currency_id);
CREATE INDEX IDX_SAL_QUOTATION_LINE_QUOTATION_ID ON erp_sal_quotation_line (quotation_id);
CREATE INDEX IDX_SAL_QUOTATION_LINE_MATERIAL_ID ON erp_sal_quotation_line (material_id);
CREATE INDEX IDX_SAL_QUOTATION_LINE_UOM_ID ON erp_sal_quotation_line (uom_id);
CREATE INDEX IDX_SAL_CONTRACT_ORG_DOC_STATUS ON erp_sal_contract (org_id, doc_status);
CREATE INDEX IDX_SAL_CONTRACT_ORG_BUSINESS_DATE ON erp_sal_contract (org_id, business_date);
CREATE INDEX IDX_SAL_CONTRACT_CUSTOMER_ID ON erp_sal_contract (customer_id);
CREATE INDEX IDX_SAL_CONTRACT_CURRENCY_ID ON erp_sal_contract (currency_id);
CREATE INDEX IDX_SAL_ORDER_ORG_DOC_STATUS ON erp_sal_order (org_id, doc_status);
CREATE INDEX IDX_SAL_ORDER_ORG_BUSINESS_DATE ON erp_sal_order (org_id, business_date);
CREATE INDEX IDX_SAL_ORDER_QUOTATION_ID ON erp_sal_order (quotation_id);
CREATE INDEX IDX_SAL_ORDER_CONTRACT_ID ON erp_sal_order (contract_id);
CREATE INDEX IDX_SAL_ORDER_CUSTOMER_ID ON erp_sal_order (customer_id);
CREATE INDEX IDX_SAL_ORDER_WAREHOUSE_ID ON erp_sal_order (warehouse_id);
CREATE INDEX IDX_SAL_ORDER_CURRENCY_ID ON erp_sal_order (currency_id);
CREATE INDEX IDX_SAL_ORDER_SETTLEMENT_METHOD_ID ON erp_sal_order (settlement_method_id);
CREATE INDEX IDX_SAL_ORDER_LINE_ORDER_ID ON erp_sal_order_line (order_id);
CREATE INDEX IDX_SAL_ORDER_LINE_MATERIAL_ID ON erp_sal_order_line (material_id);
CREATE INDEX IDX_SAL_ORDER_LINE_SKU_ID ON erp_sal_order_line (sku_id);
CREATE INDEX IDX_SAL_ORDER_LINE_UOM_ID ON erp_sal_order_line (uom_id);
CREATE INDEX IDX_SAL_ORDER_LINE_TAX_RATE_ID ON erp_sal_order_line (tax_rate_id);
CREATE INDEX IDX_SAL_ORDER_LINE_WAREHOUSE_ID ON erp_sal_order_line (warehouse_id);
CREATE INDEX IDX_SAL_ORDER_LINE_PROJECT_ID ON erp_sal_order_line (project_id);
CREATE INDEX IDX_SAL_DELIVERY_ORG_DOC_STATUS ON erp_sal_delivery (org_id, doc_status);
CREATE INDEX IDX_SAL_DELIVERY_ORG_BUSINESS_DATE ON erp_sal_delivery (org_id, business_date);
CREATE INDEX IDX_SAL_DELIVERY_ORDER_ID ON erp_sal_delivery (order_id);
CREATE INDEX IDX_SAL_DELIVERY_CUSTOMER_ID ON erp_sal_delivery (customer_id);
CREATE INDEX IDX_SAL_DELIVERY_WAREHOUSE_ID ON erp_sal_delivery (warehouse_id);
CREATE INDEX IDX_SAL_DELIVERY_CURRENCY_ID ON erp_sal_delivery (currency_id);
CREATE INDEX IDX_SAL_DELIVERY_LINE_DELIVERY_ID ON erp_sal_delivery_line (delivery_id);
CREATE INDEX IDX_SAL_DELIVERY_LINE_ORDER_LINE_ID ON erp_sal_delivery_line (order_line_id);
CREATE INDEX IDX_SAL_DELIVERY_LINE_MATERIAL_ID ON erp_sal_delivery_line (material_id);
CREATE INDEX IDX_SAL_DELIVERY_LINE_SKU_ID ON erp_sal_delivery_line (sku_id);
CREATE INDEX IDX_SAL_DELIVERY_LINE_UOM_ID ON erp_sal_delivery_line (uom_id);
CREATE INDEX IDX_SAL_DELIVERY_LINE_WAREHOUSE_ID ON erp_sal_delivery_line (warehouse_id);
CREATE INDEX IDX_SAL_INVOICE_ORG_DOC_STATUS ON erp_sal_invoice (org_id, doc_status);
CREATE INDEX IDX_SAL_INVOICE_ORG_BUSINESS_DATE ON erp_sal_invoice (org_id, business_date);
CREATE INDEX IDX_SAL_INVOICE_CUSTOMER_ID ON erp_sal_invoice (customer_id);
CREATE INDEX IDX_SAL_INVOICE_CURRENCY_ID ON erp_sal_invoice (currency_id);
CREATE INDEX IDX_SAL_INVOICE_LINE_INVOICE_ID ON erp_sal_invoice_line (invoice_id);
CREATE INDEX IDX_SAL_INVOICE_LINE_DELIVERY_LINE_ID ON erp_sal_invoice_line (delivery_line_id);
CREATE INDEX IDX_SAL_INVOICE_LINE_MATERIAL_ID ON erp_sal_invoice_line (material_id);
CREATE INDEX IDX_SAL_INVOICE_LINE_UOM_ID ON erp_sal_invoice_line (uom_id);
CREATE INDEX IDX_SAL_RECEIPT_ORG_DOC_STATUS ON erp_sal_receipt (org_id, doc_status);
CREATE INDEX IDX_SAL_RECEIPT_ORG_BUSINESS_DATE ON erp_sal_receipt (org_id, business_date);
CREATE INDEX IDX_SAL_RECEIPT_CUSTOMER_ID ON erp_sal_receipt (customer_id);
CREATE INDEX IDX_SAL_RECEIPT_CURRENCY_ID ON erp_sal_receipt (currency_id);
CREATE INDEX IDX_SAL_RECEIPT_SETTLEMENT_METHOD_ID ON erp_sal_receipt (settlement_method_id);
CREATE INDEX IDX_SAL_RECEIPT_BANK_ACCOUNT_ID ON erp_sal_receipt (bank_account_id);
CREATE INDEX IDX_SAL_RECEIPT_PARTNER_BANK_ACCOUNT_ID ON erp_sal_receipt (partner_bank_account_id);
CREATE INDEX IDX_SAL_RECEIPT_LINE_RECEIPT_ID ON erp_sal_receipt_line (receipt_id);
CREATE INDEX IDX_SAL_RECEIPT_LINE_INVOICE_ID ON erp_sal_receipt_line (invoice_id);
CREATE INDEX IDX_SAL_RETURN_ORG_DOC_STATUS ON erp_sal_return (org_id, doc_status);
CREATE INDEX IDX_SAL_RETURN_ORG_BUSINESS_DATE ON erp_sal_return (org_id, business_date);
CREATE INDEX IDX_SAL_RETURN_DELIVERY_ID ON erp_sal_return (delivery_id);
CREATE INDEX IDX_SAL_RETURN_CUSTOMER_ID ON erp_sal_return (customer_id);
CREATE INDEX IDX_SAL_RETURN_WAREHOUSE_ID ON erp_sal_return (warehouse_id);
CREATE INDEX IDX_SAL_RETURN_CURRENCY_ID ON erp_sal_return (currency_id);
CREATE INDEX IDX_SAL_RETURN_LINE_RETURN_ID ON erp_sal_return_line (return_id);
CREATE INDEX IDX_SAL_RETURN_LINE_DELIVERY_LINE_ID ON erp_sal_return_line (delivery_line_id);
CREATE INDEX IDX_SAL_RETURN_LINE_MATERIAL_ID ON erp_sal_return_line (material_id);
CREATE INDEX IDX_SAL_RETURN_LINE_SKU_ID ON erp_sal_return_line (sku_id);
CREATE INDEX IDX_SAL_RETURN_LINE_UOM_ID ON erp_sal_return_line (uom_id);
