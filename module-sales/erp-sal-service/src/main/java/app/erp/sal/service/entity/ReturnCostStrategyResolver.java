package app.erp.sal.service.entity;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 退货入库成本策略解析工具（UC-SAL-07，P1-RC-026）。按配置 {@link ErpSalConstants#CONFIG_RETURN_COST_METHOD}
 * 解析每行单位成本，供 {@link ReturnStockMoveBuilder#buildLines}（库存移动单 unitCost）与
 * {@link app.erp.sal.service.posting.SalReturnPostingDispatcher}（GL 凭证 TOTAL_COST）**同源**消费，
 * 维持「库存 ledger totalCost 与 GL 凭证 TOTAL_COST 同源」不变量（dispatcher javadoc 自述契约）。
 *
 * <p>静态工具形态：两调用方各自传入既有 {@link IDaoProvider}（builder 经新增注入，dispatcher 既有），
 * 避免新增 IoC bean 注册——current 策略的库存成本查询需 DAO 直读以同时服务 builder（有 IServiceContext）
 * 与 dispatcher（REQUIRES_NEW 隔离事务，IServiceContext 不可达），对齐 dispatcher 既有的
 * {@code AcctSchemaResolver.resolvePrimarySchemaId(daoProvider,...)} 跨模块 DAO 直读先例。
 *
 * <p>三策略：
 * <ul>
 *   <li><b>original</b>（默认）= 行 unitPrice（按原出库成本冲减存货估值口径）；</li>
 *   <li><b>current</b> = 库存域 {@code ErpInvStockBalance.avgCost}（按 materialId+warehouseId 查询当前库存成本，
 *       缺失则回退 unitPrice + LOG.warn，不静默——见 returns.md §退货成本处理）；</li>
 *   <li><b>agreement</b> = 行 unitPrice（退货协议价语义，退货行单价即协议价）。</li>
 * </ul>
 */
public final class ReturnCostStrategyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ReturnCostStrategyResolver.class);

    private ReturnCostStrategyResolver() {
    }

    public static String resolveStrategy() {
        String value = AppConfig.var(ErpSalConstants.CONFIG_RETURN_COST_METHOD,
                ErpSalConstants.RETURN_COST_METHOD_ORIGINAL);
        if (value == null || value.trim().isEmpty()) {
            return ErpSalConstants.RETURN_COST_METHOD_ORIGINAL;
        }
        String trimmed = value.trim();
        if (ErpSalConstants.RETURN_COST_METHOD_ORIGINAL.equals(trimmed)
                || ErpSalConstants.RETURN_COST_METHOD_CURRENT.equals(trimmed)
                || ErpSalConstants.RETURN_COST_METHOD_AGREEMENT.equals(trimmed)) {
            return trimmed;
        }
        LOG.warn("Invalid value [{}] for config erp-sal.return-cost-method, falling back to default original", trimmed);
        return ErpSalConstants.RETURN_COST_METHOD_ORIGINAL;
    }

    public static BigDecimal resolveUnitCost(IDaoProvider daoProvider, BigDecimal fallbackUnitPrice,
                                             String materialId, String warehouseId) {
        BigDecimal base = nz(fallbackUnitPrice);
        if (!ErpSalConstants.RETURN_COST_METHOD_CURRENT.equals(resolveStrategy())) {
            return base;
        }
        BigDecimal avgCost = findAvgCost(daoProvider, materialId, warehouseId);
        if (avgCost != null) {
            return avgCost;
        }
        LOG.warn("current strategy: inventory cost not found for material {} warehouse {}, falling back to line unitPrice={}",
                materialId, warehouseId, base);
        return base;
    }

    /**
     * P2-CK-sal-009：current 成本按数量加权平均——余额表批次/库位/SKU 拆分多行时，原 setLimit(1)
     * 取任一批次行 avgCost 源头行不确定（不同批次 avgCost 不同，GL TOTAL_COST 与库存 ledger 同源
     * 但口径漂移）。加权 = Σ(avgCost×totalQty)/Σ(totalQty)，总量为零时回退任一非空 avgCost。
     */
    private static BigDecimal findAvgCost(IDaoProvider daoProvider, String materialId, String warehouseId) {
        if (materialId == null || warehouseId == null) {
            return null;
        }
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("materialId", materialId), eq("warehouseId", warehouseId)));
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal fallback = null;
        for (ErpInvStockBalance balance : dao.findAllByQuery(q)) {
            BigDecimal avgCost = balance.getAvgCost();
            if (avgCost == null) {
                continue;
            }
            if (fallback == null) {
                fallback = avgCost;
            }
            BigDecimal qty = balance.getTotalQuantity() == null ? BigDecimal.ZERO : balance.getTotalQuantity();
            weightedSum = weightedSum.add(avgCost.multiply(qty));
            totalQty = totalQty.add(qty);
        }
        if (totalQty.signum() > 0) {
            return weightedSum.divide(totalQty, 6, java.math.RoundingMode.HALF_UP);
        }
        return fallback;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
