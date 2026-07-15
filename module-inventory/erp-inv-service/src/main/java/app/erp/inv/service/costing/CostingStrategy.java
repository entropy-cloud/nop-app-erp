package app.erp.inv.service.costing;

import app.erp.inv.service.ErpInvConfigs;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;

import java.math.BigDecimal;

/**
 * 成本方法策略。每个策略封装一种存货计价方法（{@code erp-md/cost-method}）的入库/出库成本计算 + 余额/成本层/流水维护。
 *
 * <p>策略写 {@link app.erp.inv.dao.entity.ErpInvStockLedger} 的 {@code unitCost/totalCost} 与既有
 * {@code InvPostingDispatcher}（读 {@code ledger.getTotalCost()}）零改动拾取——COGS 通道统一。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md}、{@code docs/design/inventory/cross-domain.md}。
 */
public interface CostingStrategy {

    /** 对应字典 {@code erp-md/cost-method} 的码值（MOVING_AVERAGE、FIFO…）。 */
    String costMethod();

    /**
     * 入库记账：维护成本层（FIFO）/ 重算 avgCost（移动加权平均）+ 更新余额 + 写不可变流水。
     *
     * @param unitCost 入库单价（移动单行 {@code line.unitCost}）；内部调拨时为源仓 carriedCost
     * @return 实际记入流水的单位成本（移动加权平均=输入；FIFO=入库单价）
     */
    BigDecimal onIncoming(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                          BigDecimal unitCost, BookingContext ctx);

    /**
     * 出库记账：消耗成本层（FIFO）/ 取 avgCost（移动加权平均）+ 扣减余额 + 写不可变流水。
     *
     * @return 实际记入流水的单位成本（移动加权平均=avgCost；FIFO=多层加权）
     */
    BigDecimal onOutgoing(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId, BookingContext ctx);
}
