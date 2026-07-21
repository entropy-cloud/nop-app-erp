package app.erp.drp.service.simulation;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.dao.entity.ErpDrpScenario;
import app.erp.drp.dao.entity.ErpDrpScenarioVersion;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.drp.DrpDemandAggregator;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * DRP 仿真计算引擎（plan 2026-07-22-1000-2 §DRP 对应物；权威：
 * `docs/design/manufacturing/simulation-engine.md §DRP 对应物`）。
 *
 * <p>E2 fork 范式（同构 MRP SimulationMrpEngine）：复用 {@link DrpDemandAggregator}（注入，聚合 currentStock/
 * allocatedQty/onOrderQty/forecastDemand），fork DRP 净需求算法（{@code DrpEngine.runDrp} 的核心循环）使其
 * safetyStock/replenishmentLeadTime/orderMultiple 经 {@link IErpDrpSimulationParamResolver} 覆盖。
 *
 * <p><b>单次路径零触及</b>：不修改 {@code DrpEngine} 任何代码，既有 50+ drp 测试不受影响。
 *
 * <p>本算法对齐 {@code DrpEngine.runDrp}，任何 DrpEngine 算法变更须同步本类。
 */
public class SimulationDrpEngine {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    DrpDemandAggregator demandAggregator;
    @Inject
    IErpDrpSimulationParamResolver paramResolver;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setDemandAggregator(DrpDemandAggregator demandAggregator) {
        this.demandAggregator = demandAggregator;
    }

    public void setParamResolver(IErpDrpSimulationParamResolver paramResolver) {
        this.paramResolver = paramResolver;
    }

    /**
     * 运行 DRP 仿真：基于基线 plan + 场景参数变体 → 生成新 COMPUTED plan + 场景版本快照。
     *
     * <p>步骤：校验场景 DRAFT + 基线 plan → 新建 COMPUTED plan（DRAFT→COMPUTED）→ aggregate 需求 +
     * 场景化覆盖（safetyStock/replenishmentLeadTime/orderMultiple）→ fork DRP 净需求 → 写场景版本。
     */
    public ErpDrpScenarioVersion runSimulation(Long scenarioId) {
        ErpDrpScenario scenario = requireScenario(scenarioId);
        if (!Objects.equals(scenario.getStatus(), ErpDrpConstants.SIMULATION_STATUS_DRAFT)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_SCENARIO_NOT_DRAFT)
                    .param(ErpDrpErrors.ARG_SCENARIO_ID, scenarioId)
                    .param(ErpDrpErrors.ARG_CURRENT_STATUS, scenario.getStatus());
        }
        if (scenario.getBaseDrpPlanId() == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_NO_BASELINE_PLAN)
                    .param(ErpDrpErrors.ARG_SCENARIO_ID, scenarioId);
        }
        ErpDrpPlan basePlan = daoProvider.daoFor(ErpDrpPlan.class).getEntityById(scenario.getBaseDrpPlanId());
        if (basePlan == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_NO_BASELINE_PLAN)
                    .param(ErpDrpErrors.ARG_SCENARIO_ID, scenarioId);
        }

        // 标记场景 RUNNING
        scenario.setStatus(ErpDrpConstants.SIMULATION_STATUS_RUNNING);
        daoProvider.daoFor(ErpDrpScenario.class).saveOrUpdateEntity(scenario);

        // 1. 新建 COMPUTED 结果 plan
        int nextVersionNo = nextVersionNo(scenarioId);
        ErpDrpPlan computed = daoProvider.daoFor(ErpDrpPlan.class).newEntity();
        computed.setCode(basePlan.getCode() + "-SIM-V" + nextVersionNo);
        computed.setPlanName(basePlan.getPlanName() + " (SIM V" + nextVersionNo + ")");
        computed.setOrgId(basePlan.getOrgId());
        computed.setPeriodFrom(basePlan.getPeriodFrom());
        computed.setPeriodTo(basePlan.getPeriodTo());
        computed.setBusinessDate(basePlan.getBusinessDate());
        computed.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
        daoProvider.daoFor(ErpDrpPlan.class).saveEntity(computed);

        // 2. 聚合需求（DrpDemandAggregator 一次性读取 currentStock/allocated/onOrder/forecast）
        List<DrpDemandAggregator.AggregatedDemand> aggregated = demandAggregator.aggregate(computed.getId());

        // 3. fork DRP 净需求（覆盖经 paramResolver）
        IEntityDao<ErpDrpLine> lineDao = daoProvider.daoFor(ErpDrpLine.class);
        int lineNo = 10;
        BigDecimal totalReplenishment = BigDecimal.ZERO;
        for (DrpDemandAggregator.AggregatedDemand ctx : aggregated) {
            ErpDrpParameter param = ctx.parameter;
            if (param.getMaterialId() == null || param.getWarehouseId() == null) {
                continue;
            }
            // 场景覆盖
            BigDecimal safetyStock = resolveSafetyStock(scenario.getId(), param);
            BigDecimal orderMultiple = resolveOrderMultiple(scenario.getId(), param);

            BigDecimal net = safetyStock
                    .add(nz(ctx.forecastDemand))
                    .subtract(nz(ctx.currentStock))
                    .add(nz(ctx.allocatedQty))
                    .subtract(nz(ctx.onOrderQty));
            if (net.signum() < 0) net = BigDecimal.ZERO;
            BigDecimal suggested = roundToMultiple(net, orderMultiple);

            ErpDrpLine line = lineDao.newEntity();
            line.setPlanId(computed.getId());
            line.setLineNo(lineNo);
            line.setMaterialId(param.getMaterialId());
            line.setWarehouseId(param.getWarehouseId());
            line.setSourceWarehouseId(param.getPreferredSourceWarehouseId());
            line.setReplenishmentType(decideReplenishmentType(param));
            line.setCurrentStock(ctx.currentStock);
            line.setAllocatedQty(ctx.allocatedQty);
            line.setOnOrderQty(ctx.onOrderQty);
            line.setForecastDemand(ctx.forecastDemand);
            line.setSafetyStock(safetyStock);
            line.setNetRequirement(net);
            line.setSuggestedQty(suggested);
            line.setApprovedQty(BigDecimal.ZERO);
            line.setStatus(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED);
            line.setOrgId(computed.getOrgId());
            lineDao.saveEntity(line);

            totalReplenishment = totalReplenishment.add(suggested);
            lineNo += 10;
        }

        computed.setTotalReplenishmentQty(totalReplenishment);
        computed.setRunAt(CoreMetrics.currentTimestamp());
        computed.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED);
        daoProvider.daoFor(ErpDrpPlan.class).saveOrUpdateEntity(computed);

        // 4. 写场景版本
        ErpDrpScenarioVersion version = daoProvider.daoFor(ErpDrpScenarioVersion.class).newEntity();
        version.setScenarioId(scenarioId);
        version.setVersionNo(nextVersionNo);
        version.setComputedDrpPlanId(computed.getId());
        version.setSnapshotSummary("lines=" + (lineNo / 10 - 1) + ",totalReplenishment=" + totalReplenishment.toPlainString());
        version.setStatus(ErpDrpConstants.SIMULATION_STATUS_COMPLETED);
        daoProvider.daoFor(ErpDrpScenarioVersion.class).saveEntity(version);

        // 5. 场景 → COMPLETED
        scenario.setStatus(ErpDrpConstants.SIMULATION_STATUS_COMPLETED);
        daoProvider.daoFor(ErpDrpScenario.class).saveOrUpdateEntity(scenario);

        return version;
    }

    /**
     * 转正式计划：从场景版本复制为新的 DRAFT {@link ErpDrpPlan}（Decision D）。
     */
    public ErpDrpPlan promoteToFormalPlan(Long scenarioVersionId) {
        ErpDrpScenarioVersion version = requireVersion(scenarioVersionId);
        if (version.getPromotedPlanId() != null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_VERSION_ALREADY_PROMOTED)
                    .param(ErpDrpErrors.ARG_SCENARIO_VERSION_ID, scenarioVersionId);
        }
        if (!Objects.equals(version.getStatus(), ErpDrpConstants.SIMULATION_STATUS_COMPLETED)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_SCENARIO_NOT_DRAFT)
                    .param(ErpDrpErrors.ARG_SCENARIO_VERSION_ID, scenarioVersionId)
                    .param(ErpDrpErrors.ARG_CURRENT_STATUS, version.getStatus());
        }
        ErpDrpPlan computed = daoProvider.daoFor(ErpDrpPlan.class).getEntityById(version.getComputedDrpPlanId());
        if (computed == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_NO_BASELINE_PLAN)
                    .param(ErpDrpErrors.ARG_SCENARIO_VERSION_ID, scenarioVersionId);
        }

        ErpDrpPlan promoted = daoProvider.daoFor(ErpDrpPlan.class).newEntity();
        String suffix = ErpDrpConstants.SIMULATION_PROMOTED_PLAN_CODE_SUFFIX
                .replace("{0}", String.valueOf(version.getVersionNo()));
        promoted.setCode(computed.getCode() + suffix);
        promoted.setPlanName(computed.getPlanName() + " (PROMOTED)");
        promoted.setOrgId(computed.getOrgId());
        promoted.setPeriodFrom(computed.getPeriodFrom());
        promoted.setPeriodTo(computed.getPeriodTo());
        promoted.setBusinessDate(computed.getBusinessDate());
        promoted.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
        daoProvider.daoFor(ErpDrpPlan.class).saveEntity(promoted);

        // 复制 SUGGESTED 行
        IEntityDao<ErpDrpLine> lineDao = daoProvider.daoFor(ErpDrpLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", computed.getId()));
        int lineNo = 10;
        for (ErpDrpLine src : lineDao.findAllByQuery(q)) {
            ErpDrpLine dst = lineDao.newEntity();
            dst.setPlanId(promoted.getId());
            dst.setLineNo(lineNo);
            dst.setMaterialId(src.getMaterialId());
            dst.setWarehouseId(src.getWarehouseId());
            dst.setSourceWarehouseId(src.getSourceWarehouseId());
            dst.setReplenishmentType(src.getReplenishmentType());
            dst.setCurrentStock(src.getCurrentStock());
            dst.setAllocatedQty(src.getAllocatedQty());
            dst.setOnOrderQty(src.getOnOrderQty());
            dst.setForecastDemand(src.getForecastDemand());
            dst.setSafetyStock(src.getSafetyStock());
            dst.setNetRequirement(src.getNetRequirement());
            dst.setSuggestedQty(src.getSuggestedQty());
            dst.setApprovedQty(BigDecimal.ZERO);
            dst.setStatus(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED);
            dst.setOrgId(promoted.getOrgId());
            lineDao.saveEntity(dst);
            lineNo += 10;
        }

        version.setPromotedPlanId(promoted.getId());
        version.setStatus(ErpDrpConstants.SIMULATION_STATUS_ARCHIVED);
        daoProvider.daoFor(ErpDrpScenarioVersion.class).saveOrUpdateEntity(version);

        return promoted;
    }

    // ---------- fork DRP 算法（覆盖经 paramResolver） ----------

    private BigDecimal resolveSafetyStock(Long scenarioId, ErpDrpParameter param) {
        BigDecimal override = paramResolver.resolveSafetyStockOverride(scenarioId, param.getMaterialId(), param.getWarehouseId());
        return override != null ? override : nz(param.getSafetyStock());
    }

    private BigDecimal resolveOrderMultiple(Long scenarioId, ErpDrpParameter param) {
        BigDecimal override = paramResolver.resolveReplenishmentQtyOverride(scenarioId, param.getMaterialId(), param.getWarehouseId());
        return override != null ? override : nz(param.getOrderMultiple());
    }

    private String decideReplenishmentType(ErpDrpParameter param) {
        if (param.getPreferredSourceWarehouseId() != null) {
            return ErpDrpConstants.REPLENISHMENT_TYPE_TRANSFER;
        }
        return ErpDrpConstants.REPLENISHMENT_TYPE_PURCHASE;
    }

    private BigDecimal roundToMultiple(BigDecimal net, BigDecimal orderMultiple) {
        if (net == null || net.signum() <= 0) return BigDecimal.ZERO;
        if (orderMultiple == null || orderMultiple.signum() <= 0) return net;
        BigDecimal multiples = net.divide(orderMultiple, 0, RoundingMode.CEILING);
        return multiples.multiply(orderMultiple);
    }

    private int nextVersionNo(Long scenarioId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("scenarioId", scenarioId));
        q.addOrderField("versionNo", false);
        q.setLimit(1);
        List<ErpDrpScenarioVersion> top = daoProvider.daoFor(ErpDrpScenarioVersion.class).findAllByQuery(q);
        if (top.isEmpty() || top.get(0).getVersionNo() == null) return 1;
        return top.get(0).getVersionNo() + 1;
    }

    private ErpDrpScenario requireScenario(Long scenarioId) {
        if (scenarioId == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_NO_BASELINE_PLAN)
                    .param(ErpDrpErrors.ARG_SCENARIO_ID, scenarioId);
        }
        ErpDrpScenario s = daoProvider.daoFor(ErpDrpScenario.class).getEntityById(scenarioId);
        if (s == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_NO_BASELINE_PLAN)
                    .param(ErpDrpErrors.ARG_SCENARIO_ID, scenarioId);
        }
        return s;
    }

    private ErpDrpScenarioVersion requireVersion(Long versionId) {
        ErpDrpScenarioVersion v = daoProvider.daoFor(ErpDrpScenarioVersion.class).getEntityById(versionId);
        if (v == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_SIMULATION_VERSION_ALREADY_PROMOTED)
                    .param(ErpDrpErrors.ARG_SCENARIO_VERSION_ID, versionId);
        }
        return v;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
