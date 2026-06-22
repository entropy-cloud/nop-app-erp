
    alter table erp_inv_stock_move add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_balance add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_reservation add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_cost_layer add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_transfer_order add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_take add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_picking_order add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_batch add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_serial_number add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_move_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_reservation_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_transfer_order_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_take_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_picking_order_line add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_ledger add NOP_TENANT_ID VARCHAR2(32) DEFAULT '0' NOT NULL;

alter table erp_inv_stock_move drop constraint PK_erp_inv_stock_move;
alter table erp_inv_stock_move add constraint PK_erp_inv_stock_move primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_balance drop constraint PK_erp_inv_stock_balance;
alter table erp_inv_stock_balance add constraint PK_erp_inv_stock_balance primary key (NOP_TENANT_ID, ID);

alter table erp_inv_reservation drop constraint PK_erp_inv_reservation;
alter table erp_inv_reservation add constraint PK_erp_inv_reservation primary key (NOP_TENANT_ID, ID);

alter table erp_inv_cost_layer drop constraint PK_erp_inv_cost_layer;
alter table erp_inv_cost_layer add constraint PK_erp_inv_cost_layer primary key (NOP_TENANT_ID, ID);

alter table erp_inv_transfer_order drop constraint PK_erp_inv_transfer_order;
alter table erp_inv_transfer_order add constraint PK_erp_inv_transfer_order primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_take drop constraint PK_erp_inv_stock_take;
alter table erp_inv_stock_take add constraint PK_erp_inv_stock_take primary key (NOP_TENANT_ID, ID);

alter table erp_inv_picking_order drop constraint PK_erp_inv_picking_order;
alter table erp_inv_picking_order add constraint PK_erp_inv_picking_order primary key (NOP_TENANT_ID, ID);

alter table erp_inv_batch drop constraint PK_erp_inv_batch;
alter table erp_inv_batch add constraint PK_erp_inv_batch primary key (NOP_TENANT_ID, ID);

alter table erp_inv_serial_number drop constraint PK_erp_inv_serial_number;
alter table erp_inv_serial_number add constraint PK_erp_inv_serial_number primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_move_line drop constraint PK_erp_inv_stock_move_line;
alter table erp_inv_stock_move_line add constraint PK_erp_inv_stock_move_line primary key (NOP_TENANT_ID, ID);

alter table erp_inv_reservation_line drop constraint PK_erp_inv_reservation_line;
alter table erp_inv_reservation_line add constraint PK_erp_inv_reservation_line primary key (NOP_TENANT_ID, ID);

alter table erp_inv_transfer_order_line drop constraint PK_erp_inv_transfer_order_line;
alter table erp_inv_transfer_order_line add constraint PK_erp_inv_transfer_order_line primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_take_line drop constraint PK_erp_inv_stock_take_line;
alter table erp_inv_stock_take_line add constraint PK_erp_inv_stock_take_line primary key (NOP_TENANT_ID, ID);

alter table erp_inv_picking_order_line drop constraint PK_erp_inv_picking_order_line;
alter table erp_inv_picking_order_line add constraint PK_erp_inv_picking_order_line primary key (NOP_TENANT_ID, ID);

alter table erp_inv_stock_ledger drop constraint PK_erp_inv_stock_ledger;
alter table erp_inv_stock_ledger add constraint PK_erp_inv_stock_ledger primary key (NOP_TENANT_ID, ID);


