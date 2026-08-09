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
        LOG.warn("配置 erp-sal.return-cost-method 非法值 [{}]，回退默认 original", trimmed);
        return ErpSalConstants.RETURN_COST_METHOD_ORIGINAL;
    }

    public static BigDecimal resolveUnitCost(IDaoProvider daoProvider, BigDecimal fallbackUnitPrice,
                                             Long materialId, Long warehouseId) {
        BigDecimal base = nz(fallbackUnitPrice);
        if (!ErpSalConstants.RETURN_COST_METHOD_CURRENT.equals(resolveStrategy())) {
            return base;
        }
        BigDecimal avgCost = findAvgCost(daoProvider, materialId, warehouseId);
        if (avgCost != null) {
            return avgCost;
        }
        LOG.warn("current 策略下未找到物料 {} 仓库 {} 的库存成本，回退行 unitPrice={}",
                materialId, warehouseId, base);
        return base;
    }

    private static BigDecimal findAvgCost(IDaoProvider daoProvider, Long materialId, Long warehouseId) {
        if (materialId == null || warehouseId == null) {
            return null;
        }
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("materialId", materialId), eq("warehouseId", warehouseId)));
        q.setLimit(1);
        for (ErpInvStockBalance balance : dao.findAllByQuery(q)) {
            return balance.getAvgCost();
        }
        return null;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
