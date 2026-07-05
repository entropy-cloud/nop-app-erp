package app.erp.mfg.service.costing;

import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 生产成本差异计算引擎（plan 2026-07-05-1838-2）。服务于完工触发（{@code ErpMfgWorkOrderProcessor.reportCompletion}
 * 的 {@code willFinish} 分支，config-gated）与手动重算入口（{@code IErpMfgCostVarianceBiz.calculateVariances}）。
 *
 * <p>算法（{@code docs/design/manufacturing/variance-analysis.md §核心计算逻辑}）：
 * <ul>
 *   <li>标准成本：取最近一条 status=FIRMED 的 {@link ErpMfgCostRollupLine}（产品维度的单位标准成本分解），
 *       按完工数量 × 单位标准成本 = 标准成本。人工标准工时来自 BOM 工艺（{@link ErpMfgBomOperation#getStandardTime}）。</li>
 *   <li>实际成本：取 {@link ErpMfgWorkOrder} 已累加的四要素（materialCost/laborCost/overheadCost/subcontractCost）。
 *       实际工时来自 {@link ErpMfgJobCardTimeLog#getDurationMins} 求和。</li>
 *   <li>差异类型（5 类，{@code erp-mfg/variance-type}）：
 *     <ul>
 *       <li>{@code MATERIAL_USAGE}（材料用量）：实际材料 − 标准材料。材料价格差异由 PPV（plan 2026-07-05-0427-2）
 *           在采购入库捕获，本期材料段仅算用量差异避免重复计入。</li>
 *       <li>{@code LABOR_EFFICIENCY}（人工效率）：(实际工时 − 标准工时) × 标准费率。</li>
 *       <li>{@code LABOR_RATE}（人工费率）：实际人工成本 − 实际工时 × 标准费率（残差为费率差异）。</li>
 *       <li>{@code OVERHEAD}（制造费用）：实际制造费用 − 标准制造费用。</li>
 *       <li>{@code VOLUME}（产量）：(实际产出 − 计划产出) × 标准单位成本。
 *           <b>注</b>：完工触发时实际产出 = 计划产出（{@code ERR_OVER_REPORT} 拒绝超产），故通常为 0；
 *           此行始终写入以保留可追溯性，完工 ≠ 计划时（如部分完工手动触发）非 0。</li>
 *     </ul></li>
 * </ul>
 *
 * <p><b>成本要素归属</b>：{@code VOLUME} 类型按 {@code MATERIAL} 要素归集（量差主导影响材料消耗，承接设计文档
 * 简化建模，按类型分科目精度归 Deferred）。{@code SUBCONTRACT} 要素本期不算差异（5 类差异类型未含 SUBCONTRACT，
 * 委外差异需求落地时新增类型码）。
 *
 * <p>本类为非 BizModel 服务助手（对齐 {@link CostRollupService} 范式），直接用 {@link IDaoProvider}。
 * 幂等由调用方负责：完工触发首次写入；手动重算入口先删该工单旧行再重算。
 *
 * <p>权威：{@code docs/design/manufacturing/variance-analysis.md}、{@code docs/design/finance/costing-methods.md}。
 */
public class ProductionVarianceCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(ProductionVarianceCalculator.class);

    static final BigDecimal SIXTY = new BigDecimal("60");
    static final int SCALE = 4;
    static final RoundingMode RM = RoundingMode.HALF_UP;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    /**
     * 计算并持久化指定工单的生产差异行。无 FIRMED 标准成本时抛 {@link ErpMfgErrors#ERR_VARIANCE_NO_STANDARD_COST}。
     *
     * <p>本方法不做状态校验（COMPLETED 与否由调用方门控）；不做幂等（由调用方先删旧行）。
     * 完工数量 ≤ 0 时返回空列表（无可算产出）。
     */
    public List<ErpMfgCostVariance> calculateVariances(Long workOrderId) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId);
        Long productId = wo.getProductId();
        BigDecimal completed = nz(wo.getCompletedQuantity());
        if (completed.signum() <= 0) {
            return new ArrayList<>();
        }

        ErpMfgCostRollupLine stdLine = findFirmedRollupLine(productId);
        if (stdLine == null) {
            throw new NopException(ErpMfgErrors.ERR_VARIANCE_NO_STANDARD_COST)
                    .param(ErpMfgErrors.ARG_PRODUCT_ID, productId);
        }

        BigDecimal planned = nz(wo.getPlannedQuantity());
        LocalDate bizDate = wo.getBusinessDate() != null ? wo.getBusinessDate() : CoreMetrics.today();
        Long bomId = wo.getBomId();
        Long workcenterId = resolvePrimaryWorkcenterId(bomId);

        List<ErpMfgCostVariance> lines = new ArrayList<>();
        int lineNo = 10;

        // 1. 材料用量差异：标准 = rollup.materialCost × completed；实际 = wo.materialCost
        BigDecimal stdMaterial = nz(stdLine.getMaterialCost()).multiply(completed);
        BigDecimal actMaterial = nz(wo.getMaterialCost());
        lines.add(buildLine(workOrderId, lineNo, ErpMfgConstants.VARIANCE_TYPE_MATERIAL_USAGE,
                ErpMfgConstants.COST_ELEMENT_MATERIAL, productId, workcenterId, bizDate,
                stdMaterial, actMaterial,
                completed, completed,
                nz(stdLine.getMaterialCost()),
                divideSafe(actMaterial, completed)));
        lineNo += 10;

        // 2/3. 人工效率 + 费率差异：基于 BOM 工艺标准工时 + 作业卡实际工时
        BigDecimal standardMinsPerUnit = sumBomOperationStandardMins(bomId);
        BigDecimal standardMins = standardMinsPerUnit.multiply(completed);
        BigDecimal actualMins = sumJobCardActualMins(workOrderId);
        BigDecimal stdLaborPerUnit = nz(stdLine.getLaborCost());
        BigDecimal stdLaborTotal = stdLaborPerUnit.multiply(completed);
        BigDecimal actLabor = nz(wo.getLaborCost());
        // 标准小时费率：优先来自 BOM 工艺工作中心均值（与 CostRollupService 同口径），无工艺时由 rollup 反推
        BigDecimal stdHourlyRate = deriveStandardLaborRate(bomId, stdLaborPerUnit, standardMinsPerUnit);

        BigDecimal laborEfficiencyStdAmount = stdLaborTotal;
        BigDecimal laborEfficiencyActAtStdRate = actualMins.divide(SIXTY, SCALE, RM).multiply(stdHourlyRate);
        BigDecimal laborEffVariance = laborEfficiencyActAtStdRate.subtract(laborEfficiencyStdAmount);
        lines.add(buildLine(workOrderId, lineNo, ErpMfgConstants.VARIANCE_TYPE_LABOR_EFFICIENCY,
                ErpMfgConstants.COST_ELEMENT_LABOR, productId, workcenterId, bizDate,
                laborEfficiencyStdAmount, laborEfficiencyActAtStdRate,
                standardMins, actualMins,
                stdHourlyRate, stdHourlyRate));
        lineNo += 10;

        // 费率差异：残差 = 实际人工成本 − 实际工时 × 标准费率
        BigDecimal laborRateStdAmount = laborEfficiencyActAtStdRate;
        lines.add(buildLine(workOrderId, lineNo, ErpMfgConstants.VARIANCE_TYPE_LABOR_RATE,
                ErpMfgConstants.COST_ELEMENT_LABOR, productId, workcenterId, bizDate,
                laborRateStdAmount, actLabor,
                actualMins, actualMins,
                stdHourlyRate,
                actualMins.signum() == 0 ? BigDecimal.ZERO : actLabor.divide(actualMins.divide(SIXTY, SCALE, RM), SCALE, RM)));
        lineNo += 10;

        // 4. 制造费用差异
        BigDecimal stdOverhead = nz(stdLine.getOverheadCost()).multiply(completed);
        BigDecimal actOverhead = nz(wo.getOverheadCost());
        lines.add(buildLine(workOrderId, lineNo, ErpMfgConstants.VARIANCE_TYPE_OVERHEAD,
                ErpMfgConstants.COST_ELEMENT_OVERHEAD, productId, workcenterId, bizDate,
                stdOverhead, actOverhead,
                completed, completed,
                nz(stdLine.getOverheadCost()),
                divideSafe(actOverhead, completed)));
        lineNo += 10;

        // 5. 产量差异：(completed - planned) × unitCost；costElement=MATERIAL（量差主导影响材料，承接简化建模）
        BigDecimal stdUnit = nz(stdLine.getUnitCost());
        BigDecimal volumeStdAmount = planned.multiply(stdUnit);
        BigDecimal volumeActAmount = completed.multiply(stdUnit);
        lines.add(buildLine(workOrderId, lineNo, ErpMfgConstants.VARIANCE_TYPE_VOLUME,
                ErpMfgConstants.COST_ELEMENT_MATERIAL, productId, workcenterId, bizDate,
                volumeStdAmount, volumeActAmount,
                planned, completed,
                stdUnit, stdUnit));

        IEntityDao<ErpMfgCostVariance> dao = daoProvider.daoFor(ErpMfgCostVariance.class);
        for (ErpMfgCostVariance line : lines) {
            dao.saveEntity(line);
        }
        if (ormTemplate != null) {
            ormTemplate.flushSession();
        }
        // 差异阈值告警旁路（plan 2026-07-06-0642-1 §Phase 3 Decision）：在差异计算结果落定后按阈值判定，
        // 超阈值调 notify；与过账 Dispatcher 解耦避免回滚耦合（告警是观察侧职责）。
        dispatchVarianceAlertIfOverThreshold(wo, lines);
        return lines;
    }

    /**
     * 差异阈值告警旁路派发（config-gated by {@code erp-mfg.variance-alert-enabled}）。
     *
     * <p>按本位币净差异金额绝对值最大的一行判定，超 {@code erp-mfg.variance-alert-threshold}（默认 100）时
     * 派发 {@code mfg.production-variance} 通知（接收人=生产主管 ROLE）。阈值可调；config 关闭路径静默跳过。
     * 通知失败降级（warn）不阻断差异计算结果。
     */
    private void dispatchVarianceAlertIfOverThreshold(ErpMfgWorkOrder wo, List<ErpMfgCostVariance> lines) {
        if (!isVarianceAlertEnabled() || notificationBiz == null || lines.isEmpty()) {
            return;
        }
        BigDecimal threshold = resolveVarianceAlertThreshold();
        ErpMfgCostVariance topLine = null;
        BigDecimal topAbs = BigDecimal.ZERO;
        for (ErpMfgCostVariance line : lines) {
            BigDecimal v = line.getVarianceAmount();
            if (v == null) {
                continue;
            }
            BigDecimal abs = v.abs();
            if (abs.compareTo(topAbs) > 0) {
                topAbs = abs;
                topLine = line;
            }
        }
        if (topLine == null || topAbs.compareTo(threshold) < 0) {
            return;
        }
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("workOrderId", wo.getId());
            ctx.put("workOrderCode", wo.getCode());
            ctx.put("productId", wo.getProductId());
            ctx.put("productCode", String.valueOf(wo.getProductId()));
            ctx.put("varianceType", topLine.getVarianceType());
            ctx.put("varianceAmount", topLine.getVarianceAmount());
            ctx.put("threshold", threshold);
            IServiceContext serviceCtx = new ServiceContextImpl();
            notificationBiz.notify(ErpMfgConstants.NOTIFY_EVENT_PRODUCTION_VARIANCE, ctx, serviceCtx);
        } catch (Exception e) {
            LOG.warn("生产差异阈值告警派发失败（降级，主计算流程继续）：workOrderId={}, reason={}",
                    wo.getId(), e.getMessage());
        }
    }

    private boolean isVarianceAlertEnabled() {
        String raw = AppConfig.var(ErpMfgConstants.CONFIG_VARIANCE_ALERT_ENABLED, "true");
        return !"false".equalsIgnoreCase(raw == null ? "" : raw.trim());
    }

    private BigDecimal resolveVarianceAlertThreshold() {
        String raw = AppConfig.var(ErpMfgConstants.CONFIG_VARIANCE_ALERT_THRESHOLD, null);
        if (raw == null || raw.trim().isEmpty()) {
            return ErpMfgConstants.DEFAULT_VARIANCE_ALERT_THRESHOLD;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return ErpMfgConstants.DEFAULT_VARIANCE_ALERT_THRESHOLD;
        }
    }

    /**
     * 删除指定工单的全部差异行（手动重算幂等前置）。
     */
    public void deleteByWorkOrder(Long workOrderId) {
        IEntityDao<ErpMfgCostVariance> dao = daoProvider.daoFor(ErpMfgCostVariance.class);
        List<ErpMfgCostVariance> existing = findByWorkOrder(workOrderId);
        if (!existing.isEmpty()) {
            dao.batchDeleteEntities(existing);
        }
    }

    /**
     * 查询指定工单的全部差异行（按行号升序）。
     */
    public List<ErpMfgCostVariance> findByWorkOrder(Long workOrderId) {
        IEntityDao<ErpMfgCostVariance> dao = daoProvider.daoFor(ErpMfgCostVariance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
    }

    private ErpMfgCostVariance buildLine(Long workOrderId, int lineNo, String varianceType, String costElement,
                                         Long materialId, Long workcenterId, LocalDate bizDate,
                                         BigDecimal standardAmount, BigDecimal actualAmount,
                                         BigDecimal standardQty, BigDecimal actualQty,
                                         BigDecimal standardPrice, BigDecimal actualPrice) {
        IEntityDao<ErpMfgCostVariance> dao = daoProvider.daoFor(ErpMfgCostVariance.class);
        ErpMfgCostVariance line = dao.newEntity();
        line.setWorkOrderId(workOrderId);
        line.setLineNo(lineNo);
        line.setVarianceType(varianceType);
        line.setCostElement(costElement);
        line.setMaterialId(materialId);
        line.setStandardAmount(scale(standardAmount));
        line.setActualAmount(scale(actualAmount));
        BigDecimal variance = scale(actualAmount).subtract(scale(standardAmount));
        line.setVarianceAmount(variance);
        line.setVariancePercent(percent(scale(standardAmount), variance));
        line.setStandardQty(scale(standardQty));
        line.setActualQty(scale(actualQty));
        line.setStandardPrice(scale(standardPrice));
        line.setActualPrice(scale(actualPrice));
        line.setWorkcenterId(workcenterId);
        line.setBusinessDate(bizDate);
        // posted 默认 false（由差异过账 dispatcher 回写）
        return line;
    }

    private ErpMfgWorkOrder requireWorkOrder(Long workOrderId) {
        if (workOrderId == null) {
            throw new NopException(ErpMfgErrors.ERR_WORK_ORDER_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_ID, workOrderId);
        }
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(workOrderId);
        if (wo == null) {
            throw new NopException(ErpMfgErrors.ERR_WORK_ORDER_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_ID, workOrderId);
        }
        return wo;
    }

    private ErpMfgCostRollupLine findFirmedRollupLine(Long productId) {
        if (ormTemplate != null) {
            ormTemplate.flushSession();
        }
        IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
        List<ErpMfgCostRollup> firmedList = headerDao.findAllByQuery(
                new QueryBean().addFilter(eq("status", ErpMfgConstants.COST_ROLLUP_STATUS_FIRMED)));
        if (firmedList.isEmpty()) {
            return null;
        }
        firmedList.sort(Comparator.comparing(
                h -> h.getBusinessDate() != null ? h.getBusinessDate() : LocalDate.MIN,
                Comparator.reverseOrder()));

        IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
        for (ErpMfgCostRollup header : firmedList) {
            List<ErpMfgCostRollupLine> lines = lineDao.findAllByQuery(
                    new QueryBean()
                            .addFilter(eq("costRollupId", header.getId()))
                            .addFilter(eq("materialId", productId)));
            if (!lines.isEmpty()) {
                return lines.get(0);
            }
        }
        return null;
    }

    private BigDecimal sumBomOperationStandardMins(Long bomId) {
        if (bomId == null) {
            return BigDecimal.ZERO;
        }
        IEntityDao<ErpMfgBomOperation> dao = daoProvider.daoFor(ErpMfgBomOperation.class);
        List<ErpMfgBomOperation> ops = dao.findAllByQuery(new QueryBean().addFilter(eq("bomId", bomId)));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpMfgBomOperation op : ops) {
            sum = sum.add(nz(op.getStandardTime()));
        }
        return sum;
    }

    private BigDecimal sumJobCardActualMins(Long workOrderId) {
        IEntityDao<ErpMfgJobCardTimeLog> dao = daoProvider.daoFor(ErpMfgJobCardTimeLog.class);
        List<ErpMfgJobCardTimeLog> logs = dao.findAllByQuery(
                new QueryBean().addFilter(eq("workOrderId", workOrderId)));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpMfgJobCardTimeLog log : logs) {
            sum = sum.add(nz(log.getDurationMins()));
        }
        return sum;
    }

    /**
     * 标准人工小时费率：优先取 BOM 工艺工作中心均值（与 {@link CostRollupService} 同口径）；
     * 无工艺工时数据时由 rollup 标准人工成本反推（rollupLaborPerUnit / (stdMinsPerUnit/60)）。
     */
    private BigDecimal deriveStandardLaborRate(Long bomId, BigDecimal stdLaborPerUnit, BigDecimal stdMinsPerUnit) {
        if (bomId != null) {
            IEntityDao<ErpMfgBomOperation> opDao = daoProvider.daoFor(ErpMfgBomOperation.class);
            List<ErpMfgBomOperation> ops = opDao.findAllByQuery(new QueryBean().addFilter(eq("bomId", bomId)));
            BigDecimal rateSum = BigDecimal.ZERO;
            int rateCount = 0;
            for (ErpMfgBomOperation op : ops) {
                Long wcId = op.getWorkcenterId();
                if (wcId == null) {
                    continue;
                }
                ErpMfgWorkcenter wc = daoProvider.daoFor(ErpMfgWorkcenter.class).getEntityById(wcId);
                if (wc != null && nz(wc.getHourlyRate()).signum() > 0) {
                    rateSum = rateSum.add(wc.getHourlyRate());
                    rateCount++;
                }
            }
            if (rateCount > 0) {
                return rateSum.divide(new BigDecimal(rateCount), SCALE, RM);
            }
        }
        if (stdMinsPerUnit.signum() > 0) {
            return stdLaborPerUnit.divide(stdMinsPerUnit.divide(SIXTY, SCALE, RM), SCALE, RM);
        }
        return BigDecimal.ZERO;
    }

    private Long resolvePrimaryWorkcenterId(Long bomId) {
        if (bomId == null) {
            return null;
        }
        IEntityDao<ErpMfgBomOperation> opDao = daoProvider.daoFor(ErpMfgBomOperation.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("bomId", bomId));
        q.setLimit(1);
        List<ErpMfgBomOperation> ops = opDao.findAllByQuery(q);
        if (ops.isEmpty()) {
            return null;
        }
        return ops.get(0).getWorkcenterId();
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    static BigDecimal scale(BigDecimal v) {
        return nz(v).setScale(SCALE, RM);
    }

    static BigDecimal divideSafe(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return nz(numerator).divide(denominator, SCALE, RM);
    }

    static BigDecimal percent(BigDecimal base, BigDecimal variance) {
        if (base.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return variance.divide(base, SCALE, RM).multiply(new BigDecimal("100"));
    }
}
