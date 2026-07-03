package app.erp.inv.service.costing;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;

import java.math.BigDecimal;

/**
 * 记账上下文：策略实现经此访问共享记账基础设施（余额 upsert / 流水写入 / 余额可用量重算 / dao / orm）。
 *
 * <p>由 {@link app.erp.inv.service.stock.StockMoveBookkeeper} 实现，避免策略与记账器循环耦合，
 * 同时保留 {@code StockMoveBookkeeper.upsertBalance} 的既有调用方（状态机预留量）。
 */
public interface BookingContext {

    ErpInvStockBalance upsertBalance(ErpInvStockMove move, ErpInvStockMoveLine line,
                                     Long warehouseId, Long locationId);

    void writeLedger(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                     ErpInvStockBalance balance, Long warehouseId, Long locationId,
                     BigDecimal signedQty, BigDecimal unitCost, BigDecimal signedTotalCost, String costMethod);

    void recomputeAvailable(ErpInvStockBalance balance);

    IDaoProvider daoProvider();

    IOrmTemplate ormTemplate();
}
