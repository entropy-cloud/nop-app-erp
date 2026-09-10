package app.erp.drp.service.drp;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.statemachine.ErpDrpPlanStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.api.core.time.CoreMetrics;

/**
 * DRP 净需求计算引擎。服务于 {@code IErpDrpPlanBiz.runDrp}（{@code drp/README.md §DRP 流程}、
 * {@code drp/use-cases.md UC-DRP-02}）。
 *
 * <p>算法（扁平单级，无 BOM 展开/pegging，区别于 MRP）：
 * <ul>
 *   <li>聚合输入由 {@link DrpDemandAggregator#aggregate} 提供（currentStock/allocatedQty/onOrderQty/forecastDemand）。</li>
 *   <li>净需求公式：{@code netRequirement = max(0, safetyStock + forecastDemand − currentStock + allocatedQty − onOrderQty)}。</li>
 *   <li>取整：{@code suggestedQty = ceil(netRequirement / orderMultiple) × orderMultiple}（orderMultiple 空/≤0 时 lot-for-lot）。</li>
 *   <li>补货类型决策：{@code preferredSourceWarehouseId} 非空 → TRANSFER；否则 {@code preferredSupplierId} 非空 → PURCHASE；
 *       两者皆空按类型抛 {@link ErpDrpErrors#ERR_DRP_NO_SOURCE_WAREHOUSE} / {@link ErpDrpErrors#ERR_DRP_NO_PREFERRED_SUPPLIER}。</li>
 *   <li>写 {@link ErpDrpLine}（status=SUGGESTED）+ 回写 plan {@code totalReplenishmentQty/runAt/runBy} + status DRAFT→COMPUTED。</li>
 * </ul>
 *
 * <p><b>Non-Goal</b>：多级分销展开；联合变分安全库存（需 ORM 列，归 Deferred）。FORECAST 集成已落地
 * （plan 2026-07-05-0427-1 §Phase 3，DrpDemandAggregator 按 materialId+warehouseId 消费 APPROVED 预测）。
 *
 * <p>本类为非 BizModel 服务助手（对齐 MRP {@code MrpEngine} 范式），跨域只读聚合（inventory/purchase/manufacturing）直接用 {@link IDaoProvider}。
 */
public class DrpEngine {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    DrpDemandAggregator demandAggregator;
    @Inject
    ErpDrpPlanStateMachine planStateMachine;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setDemandAggregator(DrpDemandAggregator demandAggregator) {
        this.demandAggregator = demandAggregator;
    }

    public void setPlanStateMachine(ErpDrpPlanStateMachine planStateMachine) {
        this.planStateMachine = planStateMachine;
    }

    /**
     * 运行 DRP：计划状态 DRAFT→COMPUTED；清除既有 SUGGESTED 行后重算并写入 {@link ErpDrpLine}。
     *
     * @param aggregated 由 {@link DrpDemandAggregator#aggregate} 产出的聚合上下文，直接传入避免同事务查询可见性问题
     */
    public void runDrp(String planId, List<DrpDemandAggregator.AggregatedDemand> aggregated) {
        ErpDrpPlan plan = requirePlan(planId);
        // 固定来源态守卫经 StateMachine Bean（plan 2026-08-12-1841-1 Phase 2）。
        // 原代码 `status != null && !Objects.equals(status, DRAFT)` 对 null 放行（视为 DRAFT 等价，未初始化）；
        // 为保留既有外部行为，null 归一化为 DRAFT 再交 Bean 判定。
        String fromStatus = plan.getStatus() != null ? plan.getStatus() : ErpDrpConstants.DRP_PLAN_STATUS_DRAFT;
        assertCanPlan(plan, () -> planStateMachine.assertCanRunDrp(fromStatus));

        IEntityDao<ErpDrpLine> lineDao = daoProvider.daoFor(ErpDrpLine.class);
        clearSuggestedLines(lineDao, planId);

        int lineNo = 10;
        BigDecimal totalReplenishment = BigDecimal.ZERO;
        for (DrpDemandAggregator.AggregatedDemand ctx : aggregated) {
            ErpDrpParameter param = ctx.parameter;
            BigDecimal safetyStock = nz(param.getSafetyStock());
            BigDecimal net = safetyStock
                    .add(nz(ctx.forecastDemand))
                    .subtract(nz(ctx.currentStock))
                    .add(nz(ctx.allocatedQty))
                    .subtract(nz(ctx.onOrderQty));
            if (net.signum() < 0) {
                net = BigDecimal.ZERO;
            }
            // 补货方法语义接入（drp-022-r3 修复面）：在需求驱动净额上叠加水位策略
            net = applyReplenishmentMethod(net, param, ctx.currentStock);
            BigDecimal suggested = roundToMultiple(net, param.getOrderMultiple());

            ErpDrpLine line = lineDao.newEntity();
            line.setPlanId(plan.getId());
            line.setLineNo(lineNo);
            line.setMaterialId(param.getMaterialId());
            line.setWarehouseId(param.getWarehouseId());
            line.setReplenishmentType(decideReplenishmentType(param));
            line.setSourceWarehouseId(param.getPreferredSourceWarehouseId());
            line.setCurrentStock(ctx.currentStock);
            line.setAllocatedQty(ctx.allocatedQty);
            line.setOnOrderQty(ctx.onOrderQty);
            line.setForecastDemand(ctx.forecastDemand);
            line.setSafetyStock(safetyStock);
            line.setNetRequirement(net);
            line.setSuggestedQty(suggested);
            line.setApprovedQty(BigDecimal.ZERO);
            line.setStatus(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED);
            line.setOrgId(plan.getOrgId());
            lineDao.saveEntity(line);

            totalReplenishment = totalReplenishment.add(suggested);
            lineNo += 10;
        }

        plan.setTotalReplenishmentQty(totalReplenishment);
        plan.setRunAt(CoreMetrics.currentTimestamp());
        // runBy（运行人）由 BizModel 层从 IServiceContext 注入用户身份后回写；helper 引擎不持有用户上下文
        plan.setStatus(planStateMachine.runDrpTargetStatus());
        daoProvider.daoFor(ErpDrpPlan.class).updateEntity(plan);
    }

    /**
     * 重置计划为 DRAFT：清除该计划下所有 SUGGESTED 行（保留 APPROVED/ORDERED/CANCELLED 终态或已审批行不动），
     * 计划状态 COMPUTED→DRAFT。供调参后重算（{@code state-machine.md §场景 B}）。
     */
    public void resetToDraft(String planId) {
        ErpDrpPlan plan = requirePlan(planId);
        String status = plan.getStatus();
        // 固定来源态守卫经 StateMachine Bean（plan 2026-08-12-1841-1 Phase 2）：合法来源 {COMPUTED, APPROVED}。
        assertCanPlan(plan, () -> planStateMachine.assertCanResetToDraft(status));
        IEntityDao<ErpDrpLine> lineDao = daoProvider.daoFor(ErpDrpLine.class);
        clearSuggestedLines(lineDao, planId);
        plan.setStatus(planStateMachine.resetToDraftTargetStatus());
        plan.setTotalReplenishmentQty(null);
        daoProvider.daoFor(ErpDrpPlan.class).updateEntity(plan);
    }

    private String decideReplenishmentType(ErpDrpParameter param) {
        if (param.getPreferredSourceWarehouseId() != null) {
            return ErpDrpConstants.REPLENISHMENT_TYPE_TRANSFER;
        }
        if (param.getPreferredSupplierId() != null) {
            return ErpDrpConstants.REPLENISHMENT_TYPE_PURCHASE;
        }
        // 两者皆空：默认按 PURCHASE 路径，释放阶段会抛 ERR_DRP_NO_PREFERRED_SUPPLIER
        return ErpDrpConstants.REPLENISHMENT_TYPE_PURCHASE;
    }

    /**
     * 补货方法语义（drp-022-r3 修复面，消费 {@code erp-drp/drp-replenishment-method}）：
     * 在需求驱动净额上叠加水位策略，消除「配置静默无效」死值面。
     * <ul>
     *   <li>LOT_FOR_LOT / null：纯需求驱动（既有语义，不变）。</li>
     *   <li>MIN_MAX：库存低于 minStockLevel 时补至 maxStockLevel（缺省回落 min）——「库存低于最低时补到最高」；
     *       取 max(demand, topUp) 保证不低于需求净额。</li>
     *   <li>PERIODIC：定期审视 order-up-to——每次 runDrp 视为一次审视，补至目标水位（max 优先，回落 min）。</li>
     * </ul>
     * min/max 未配置时回落需求驱动（兼容既有 seed 形态）。reviewPeriodDays 为运营审视节奏（批引擎无逐日触发语义，
     * 每次 runDrp 即一次审视），不参与数量计算。
     */
    public static BigDecimal applyReplenishmentMethod(BigDecimal demandNet, ErpDrpParameter param,
                                                      BigDecimal currentStock) {
        String method = param.getReplenishmentMethod();
        if (method == null || ErpDrpConstants.REPLENISHMENT_METHOD_LOT_FOR_LOT.equals(method)) {
            return demandNet;
        }
        BigDecimal current = nz(currentStock);
        if (ErpDrpConstants.REPLENISHMENT_METHOD_MIN_MAX.equals(method)) {
            BigDecimal min = param.getMinStockLevel();
            if (min == null || current.compareTo(min) >= 0) {
                return demandNet;
            }
            BigDecimal target = param.getMaxStockLevel() != null ? param.getMaxStockLevel() : min;
            return demandNet.max(target.subtract(current));
        }
        if (ErpDrpConstants.REPLENISHMENT_METHOD_PERIODIC.equals(method)) {
            BigDecimal target = param.getMaxStockLevel() != null ? param.getMaxStockLevel() : param.getMinStockLevel();
            if (target == null) {
                return demandNet;
            }
            return demandNet.max(target.subtract(current));
        }
        return demandNet;
    }

    private BigDecimal roundToMultiple(BigDecimal net, BigDecimal orderMultiple) {
        if (net == null || net.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (orderMultiple == null || orderMultiple.signum() <= 0) {
            return net; // lot-for-lot
        }
        BigDecimal multiples = net.divide(orderMultiple, 0, RoundingMode.CEILING);
        return multiples.multiply(orderMultiple);
    }

    private void clearSuggestedLines(IEntityDao<ErpDrpLine> dao, String planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        for (ErpDrpLine l : dao.findAllByQuery(q)) {
            if (Objects.equals(l.getStatus(), ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED)) {
                dao.deleteEntity(l);
            }
        }
    }

    private ErpDrpPlan requirePlan(String planId) {
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

    /**
     * 经 Plan StateMachine Bean 断言来源态合法；非法边由 Bean 直抛领域码
     * {@code ERR_DRP_PLAN_ILLEGAL_TRANSITION}（plan 2026-09-07-2200-1），本处同码补参 planCode。
     *
     * @param plan     被操作的计划（提供 planCode 实体元数据）
     * @param beanCall 实际触发 Bean {@code assertCan<Action>} 的调用
     */
    private void assertCanPlan(ErpDrpPlan plan, Runnable beanCall) {
        try {
            beanCall.run();
        } catch (NopException e) {
            throw e.param(ErpDrpErrors.ARG_PLAN_CODE, plan.getCode());
        }
    }
}
