package app.erp.drp.service.drp;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpConfigs;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import app.erp.inv.dao.entity.ErpInvTransferOrderLine;
import app.erp.mfg.dao.entity.ErpMfgForecast;
import app.erp.mfg.dao.entity.ErpMfgForecastLine;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * DRP 需求聚合器。服务于 {@code IErpDrpPlanBiz.runDrp} 的聚合阶段（{@code drp/README.md §DRP 流程}）。
 *
 * <p>按 plan 范围逐 {@code (materialId, warehouseId)} 读 {@link ErpDrpParameter}，聚合净需求计算所需输入：
 * <ul>
 *   <li><b>currentStock / allocatedQty</b>：经 {@link ErpInvStockBalance} 按 material+warehouse 只读合计
 *       （available 作为 currentStock，reserved 作为 allocatedQty）。</li>
 *   <li><b>onOrderQty</b>：在途调拨（{@link ErpInvTransferOrder} toWarehouseId=目标仓且未作废）+
 *       未到货采购（{@link ErpPurOrder} 未作废且行 quantity−receivedQuantity&gt;0）。</li>
 *   <li><b>forecastDemand</b>：消费制造域 {@link ErpMfgForecast}/{@link ErpMfgForecastLine}——头 status=APPROVED
 *       且区间与 plan 区间相交，按 materialId + 目标 warehouseId 过滤聚合 forecastQty
 *       （plan 2026-07-05-0427-1 §Phase 3）。config-gated {@code erp-drp.forecast-consume-enabled}。</li>
 *   <li><b>safetyStock</b>：取 {@link ErpDrpParameter#getSafetyStock()}（SS 优化结果经
 *       {@code IErpInvDrpSafetyStockCalcBiz.findEffectiveSafetyStock} 注入，见 {@link DrpEngine}）。</li>
 * </ul>
 *
 * <p><b>Non-Goal</b>：多级分销网络展开（本期扁平单级）。
 *
 * <p>本类为非 BizModel 服务助手（对齐 MRP {@code DemandAggregator}/{@code MrpEngine} 范式），跨域只读聚合
 * （inventory/purchase/manufacturing）直接用 {@link IDaoProvider}：I*Biz 以订单头为粒度，不便支撑行级批量聚合读取。
 * drp→manufacturing 单向（R），DAG 无环；plan 2026-07-05-0427-1 §Task Route 选用 service-helper 范式
 * 而非跨域 I*Biz——与既有 DrpDemandAggregator 跨域聚合 inventory/purchase 一致（避免单一新增来源改架构风格）。
 */
public class DrpDemandAggregator {

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 聚合 planId 关联计划范围内所有 {@code (materialId, warehouseId)} 的净需求输入上下文。
     *
     * <p>参数范围来自该 plan 关联的所有 {@link ErpDrpParameter} 行（每个参数行=一个物料+仓库补货组合）。
     * 参数缺失时跳过（不抛错），与设计 {@code state-machine.md §4 异常路径} 一致："物料参数不完整→跳过并告警"。
     *
     * @return 聚合上下文列表（每条=一个待计算行），供 {@link DrpEngine} 直接消费
     */
    public List<AggregatedDemand> aggregate(Long planId) {
        ErpDrpPlan plan = requirePlan(planId);

        List<ErpDrpParameter> parameters = loadParametersInScope(plan);
        // 预先聚合预测：按 (materialId, warehouseId) → sum(forecastQty)，仅 plan 区间内 APPROVED 行
        Map<String, BigDecimal> forecastIndex = indexForecastByMaterialWarehouse(plan);

        List<AggregatedDemand> result = new ArrayList<>();
        for (ErpDrpParameter param : parameters) {
            if (param.getMaterialId() == null || param.getWarehouseId() == null) {
                continue;
            }
            AggregatedDemand ctx = new AggregatedDemand();
            ctx.parameter = param;
            ctx.currentStock = sumAvailable(param.getMaterialId(), param.getWarehouseId());
            ctx.allocatedQty = sumReserved(param.getMaterialId(), param.getWarehouseId());
            ctx.onOrderQty = onOrderQty(param.getMaterialId(), param.getWarehouseId());
            ctx.forecastDemand = forecastIndex.getOrDefault(forecastKey(param.getMaterialId(), param.getWarehouseId()),
                    BigDecimal.ZERO);
            result.add(ctx);
        }
        return result;
    }

    /**
     * 聚合预测：返回 (materialId, warehouseId) → sum(forecastQty) 索引。
     *
     * <p>消费规则（与 MRP FORECAST 来源一致，差异在 DRP 额外按 warehouseId 过滤）：
     * <ul>
     *   <li>仅头 status=APPROVED 的预测行进入消费</li>
     *   <li>区间 [periodStart, periodEnd] 与 plan 区间 [periodFrom, periodTo] 相交</li>
     *   <li>按 materialId + warehouseId 匹配（warehouseId 为 null 的产品级预测行不进入 DRP 仓级消费）</li>
     *   <li>同 (materialId, warehouseId) 多桶累加 forecastQty</li>
     * </ul>
     *
     * <p>config-gated {@code erp-drp.forecast-consume-enabled}（默认 true）：false 时返回空 map（不消费）。
     */
    private Map<String, BigDecimal> indexForecastByMaterialWarehouse(ErpDrpPlan plan) {
        Map<String, BigDecimal> result = new HashMap<>();
        boolean enabled = AppConfig.var(ErpDrpConfigs.CONFIG_DRP_FORECAST_CONSUME_ENABLED,
                ErpDrpConfigs.DEFAULT_DRP_FORECAST_CONSUME_ENABLED);
        if (!enabled) {
            return result;
        }
        LocalDate planStart = plan.getPeriodFrom();
        LocalDate planEnd = plan.getPeriodTo();
        if (planStart == null || planEnd == null) {
            return result;
        }

        IEntityDao<ErpMfgForecast> headDao = daoProvider.daoFor(ErpMfgForecast.class);
        QueryBean hq = new QueryBean();
        hq.addFilter(eq("status", "APPROVED"));
        if (plan.getOrgId() != null) {
            hq.addFilter(eq("orgId", plan.getOrgId()));
        }
        List<ErpMfgForecast> heads = headDao.findAllByQuery(hq);
        if (heads.isEmpty()) {
            return result;
        }
        List<Long> headIds = new ArrayList<>();
        for (ErpMfgForecast h : heads) {
            headIds.add(h.getId());
        }

        IEntityDao<ErpMfgForecastLine> lineDao = daoProvider.daoFor(ErpMfgForecastLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(in("forecastId", headIds));
        // 区间相交：line.periodStart <= planEnd AND line.periodEnd >= planStart
        lq.addFilter(le("periodStart", planEnd));
        lq.addFilter(ge("periodEnd", planStart));
        // 仅消费仓级预测（warehouseId 非空），产品级（warehouseId 为 null）由 MRP 消费
        lq.addFilter(ne("warehouseId", null));

        for (ErpMfgForecastLine fl : lineDao.findAllByQuery(lq)) {
            if (fl.getMaterialId() == null || fl.getWarehouseId() == null) {
                continue;
            }
            BigDecimal qty = nz(fl.getForecastQty());
            if (qty.signum() <= 0) {
                continue;
            }
            String key = forecastKey(fl.getMaterialId(), fl.getWarehouseId());
            result.merge(key, qty, BigDecimal::add);
        }
        return result;
    }

    private static String forecastKey(Long materialId, Long warehouseId) {
        return materialId + "#" + warehouseId;
    }

    private List<ErpDrpParameter> loadParametersInScope(ErpDrpPlan plan) {
        QueryBean q = new QueryBean();
        if (plan.getOrgId() != null) {
            q.addFilter(eq("orgId", plan.getOrgId()));
        }
        return daoProvider.daoFor(ErpDrpParameter.class).findAllByQuery(q);
    }

    private BigDecimal sumAvailable(Long materialId, Long warehouseId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        BigDecimal total = BigDecimal.ZERO;
        for (ErpInvStockBalance b : daoProvider.<ErpInvStockBalance>daoFor(ErpInvStockBalance.class).findAllByQuery(q)) {
            BigDecimal avail = b.getAvailableQuantity();
            if (avail == null) {
                avail = nz(b.getTotalQuantity()).subtract(nz(b.getReservedQuantity())).subtract(nz(b.getLockedQuantity()));
            }
            total = total.add(avail);
        }
        return total;
    }

    private BigDecimal sumReserved(Long materialId, Long warehouseId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        BigDecimal total = BigDecimal.ZERO;
        for (ErpInvStockBalance b : daoProvider.<ErpInvStockBalance>daoFor(ErpInvStockBalance.class).findAllByQuery(q)) {
            total = total.add(nz(b.getReservedQuantity()));
        }
        return total;
    }

    private BigDecimal onOrderQty(Long materialId, Long warehouseId) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(inboundTransferQty(materialId, warehouseId));
        total = total.add(unreceivedPurchaseQty(materialId, warehouseId));
        return total;
    }

    private BigDecimal inboundTransferQty(Long materialId, Long warehouseId) {
        IEntityDao<ErpInvTransferOrder> orderDao = daoProvider.daoFor(ErpInvTransferOrder.class);
        IEntityDao<ErpInvTransferOrderLine> lineDao = daoProvider.daoFor(ErpInvTransferOrderLine.class);

        QueryBean oq = new QueryBean();
        oq.addFilter(eq("toWarehouseId", warehouseId));
        oq.addFilter(ne("docStatus", "CANCELLED"));
        List<ErpInvTransferOrder> orders = orderDao.findAllByQuery(oq);
        if (orders.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<Long> orderIds = new ArrayList<>();
        Map<Long, ErpInvTransferOrder> byId = new HashMap<>();
        for (ErpInvTransferOrder o : orders) {
            orderIds.add(o.getId());
            byId.put(o.getId(), o);
        }
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("materialId", materialId));
        lq.addFilter(in("transferId", orderIds));
        BigDecimal total = BigDecimal.ZERO;
        for (ErpInvTransferOrderLine l : lineDao.findAllByQuery(lq)) {
            total = total.add(nz(l.getQuantity()));
        }
        return total;
    }

    private BigDecimal unreceivedPurchaseQty(Long materialId, Long warehouseId) {
        IEntityDao<ErpPurOrder> orderDao = daoProvider.daoFor(ErpPurOrder.class);
        IEntityDao<ErpPurOrderLine> lineDao = daoProvider.daoFor(ErpPurOrderLine.class);

        QueryBean oq = new QueryBean();
        oq.addFilter(ne("docStatus", "CANCELLED"));
        List<ErpPurOrder> orders = orderDao.findAllByQuery(oq);
        if (orders.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<Long> orderIds = new ArrayList<>();
        for (ErpPurOrder o : orders) {
            orderIds.add(o.getId());
        }
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("materialId", materialId));
        lq.addFilter(in("orderId", orderIds));
        BigDecimal total = BigDecimal.ZERO;
        for (ErpPurOrderLine l : lineDao.findAllByQuery(lq)) {
            BigDecimal undelivered = nz(l.getQuantity()).subtract(nz(l.getReceivedQuantity()));
            if (undelivered.signum() > 0) {
                total = total.add(undelivered);
            }
        }
        return total;
    }

    private ErpDrpPlan requirePlan(Long planId) {
        if (planId == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_PLAN_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_DRP_PLAN_ID, planId);
        }
        ErpDrpPlan plan = daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId);
        if (plan == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_PLAN_ILLEGAL_TRANSITION)
                    .param(ErpDrpErrors.ARG_DRP_PLAN_ID, planId);
        }
        return plan;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public static class AggregatedDemand {
        public ErpDrpParameter parameter;
        public BigDecimal currentStock;
        public BigDecimal allocatedQty;
        public BigDecimal onOrderQty;
        public BigDecimal forecastDemand;
    }
}
