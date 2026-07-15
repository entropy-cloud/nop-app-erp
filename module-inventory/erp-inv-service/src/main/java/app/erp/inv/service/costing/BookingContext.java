package app.erp.inv.service.costing;

import app.erp.inv.service.ErpInvConfigs;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;

import java.math.BigDecimal;
import java.util.function.Consumer;

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

    /**
     * 在乐观锁保护下更新余额（UC-INV-08 并发扣减加固，plan 2026-07-07-0024-2）。
     *
     * <p>{@code applyDelta} 接收当前 baseline 余额（每次冲突重试时刷新为 DB 最新值），在其上设置新字段值
     * （totalQuantity/totalCost/avgCost/reservedQuantity/lockedQuantity/availableQuantity 等）。
     * 必须为纯函数——只读 baseline 字段、写新字段值，无其他副作用（不写流水、不消耗成本层），以保证重试幂等。
     *
     * <p>实现内部按 {@code erp-inv.concurrent-deduct-max-retry} 有限重试：经
     * {@link io.nop.orm.dao.IOrmEntityDao#tryUpdateWithVersionCheck} 提交（{@code UPDATE WHERE id=? AND version=?}）；
     * 冲突时 evict 当前实例 + {@link io.nop.dao.api.IEntityDao#requireEntityById} 重新加载 baseline，重新执行
     * {@code applyDelta}；重试耗尽抛 {@link app.erp.inv.service.ErpInvErrors#ERR_INV_CONCURRENT_DEDUCT_CONFLICT}。
     *
     * @return 最终落盘的余额实例（version 已自增）。策略应使用此返回值（而非入参 {@code initialBaseline}）
     *         进行后续操作（如 {@link #writeLedger} 的结存快照），保证读取到正确的新字段值。
     */
    ErpInvStockBalance updateBalanceWithRetry(ErpInvStockBalance initialBaseline,
                                              Consumer<ErpInvStockBalance> applyDelta);

    IDaoProvider daoProvider();

    IOrmTemplate ormTemplate();
}
