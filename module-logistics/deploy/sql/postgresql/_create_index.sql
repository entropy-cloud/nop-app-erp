-- app-erp-logistics indexes generated from model/app-erp-logistics.orm.xml
-- Dialect: postgresql.
-- This file is maintained separately from _create_erp-logistics.sql because the
-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.
-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.
-- Run AFTER _create_erp-logistics.sql during manual production deployment.
-- Source of truth: model <indexes> definitions (14 indexes).
-- Regenerate: node scripts/add-orm-indexes.js
CREATE INDEX IDX_LOG_CARRIER_GATEWAY_ID ON erp_log_carrier (gateway_id);
CREATE INDEX IDX_LOG_CARRIER_PARTNER_ID ON erp_log_carrier (partner_id);
CREATE INDEX IDX_LOG_CARRIER_CONFIG_CARRIER_ID ON erp_log_carrier_config (carrier_id);
CREATE INDEX IDX_LOG_SHIPMENT_ORG_STATUS ON erp_log_shipment (org_id, status);
CREATE INDEX IDX_LOG_SHIPMENT_CARRIER_ID ON erp_log_shipment (carrier_id);
CREATE INDEX IDX_LOG_SHIPMENT_CARRIER_CONFIG_ID ON erp_log_shipment (carrier_config_id);
CREATE INDEX IDX_LOG_SHIPMENT_FREIGHT_CURRENCY_ID ON erp_log_shipment (freight_currency_id);
CREATE INDEX IDX_LOG_SHIPMENT_SHIPPER_ID ON erp_log_shipment (shipper_id);
CREATE INDEX IDX_LOG_SHIPMENT_LINE_SHIPMENT_ID ON erp_log_shipment_line (shipment_id);
CREATE INDEX IDX_LOG_SHIPMENT_LINE_MATERIAL_ID ON erp_log_shipment_line (material_id);
CREATE INDEX IDX_LOG_SHIPMENT_PARCEL_SHIPMENT_ID ON erp_log_shipment_parcel (shipment_id);
CREATE INDEX IDX_LOG_SHIPMENT_LOG_SHIPMENT_ID ON erp_log_shipment_log (shipment_id);
CREATE INDEX IDX_LOG_SHIPMENT_LOG_GATEWAY_ID ON erp_log_shipment_log (gateway_id);
CREATE INDEX IDX_LOG_DELIVERY_WINDOW_PARTNER_ID ON erp_log_delivery_window (partner_id);
