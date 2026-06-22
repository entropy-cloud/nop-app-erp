
    alter table erp_inv_stock_move add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_balance add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_reservation add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_cost_layer add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_transfer_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_take add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_picking_order add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_batch add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_serial_number add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_move_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_reservation_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_transfer_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_take_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_picking_order_line add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_ledger add NOP_TENANT_ID VARCHAR(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_move drop primary key;
alter table erp_inv_stock_move add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_balance drop primary key;
alter table erp_inv_stock_balance add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_reservation drop primary key;
alter table erp_inv_reservation add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_cost_layer drop primary key;
alter table erp_inv_cost_layer add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_transfer_order drop primary key;
alter table erp_inv_transfer_order add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_take drop primary key;
alter table erp_inv_stock_take add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_picking_order drop primary key;
alter table erp_inv_picking_order add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_batch drop primary key;
alter table erp_inv_batch add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_serial_number drop primary key;
alter table erp_inv_serial_number add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_move_line drop primary key;
alter table erp_inv_stock_move_line add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_reservation_line drop primary key;
alter table erp_inv_reservation_line add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_transfer_order_line drop primary key;
alter table erp_inv_transfer_order_line add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_take_line drop primary key;
alter table erp_inv_stock_take_line add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_picking_order_line drop primary key;
alter table erp_inv_picking_order_line add primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_ledger drop primary key;
alter table erp_inv_stock_ledger add primary key (NOP_TENANT_ID, ID);


