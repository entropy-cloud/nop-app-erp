package app.erp.mfg.service.simulation;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.mfg.biz.BomExplosionNode;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgMrpDemand;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.dao.entity.ErpMfgMrpScenario;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioParam;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioVersion;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.bom.BomExpander;
import app.erp.mfg.service.mrp.DemandAggregator;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * MRP 仿真计算引擎（plan 2026-07-22-1000-2 §与单次引擎关系 Decision E2；权威：
 * `docs/design/manufacturing/simulation-engine.md`）。
 *
 * <p>E2 fork 范式：复用既有 {@link BomExpander}（既有 bean，注入）+ {@link DemandAggregator}（既有，注入，
 * 用于 SALES_ORDER/FORECAST/MANUAL 需求整合），fork MRP 核心算法（processMaterial / lotSize / mfgLeadDays /
 * purLeadDays / availableQuantity / topDemandsByMaterial）使其从 {@link IErpMfgSimulationParamResolver}
 * 读取覆盖值而非全局配置/主数据。
 *
 * <p><b>单次路径零触及</b>：本类不修改 {@code MrpEngine} / {@code DemandAggregator} 任何代码，
 * 既有 200+ manufacturing 测试不受影响（Decision E 残留风险：算法漂移由头部注释 + Non-Goals 限定）。
 *
 * <p>SAFETY_STOCK 场景化重算：先调用 {@link DemandAggregator#aggregate} 产出全量需求行，
 * 再过滤掉 demandSource=SAFETY_STOCK（master data 值），按场景覆盖值重新生成。
 *
 * <p>本算法对齐 {@code MrpEngine.runMrp}，任何 MrpEngine 算法变更须同步本类。
 */
public class SimulationMrpEngine {

    static final BigDecimal SIXTY = new BigDecimal("60");

    @Inject
    IDaoProvider daoProvider;
    @Inject
    BomExpander bomExpander;
    @Inject
    DemandAggregator demandAggregator;
    @Inject
    IErpMfgSimulationParamResolver paramResolver;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setBomExpander(BomExpander bomExpander) {
        this.bomExpander = bomExpander;
    }

    public void setDemandAggregator(DemandAggregator demandAggregator) {
        this.demandAggregator = demandAggregator;
    }

    public void setParamResolver(IErpMfgSimulationParamResolver paramResolver) {
        this.paramResolver = paramResolver;
    }

    /**
     * 运行 MRP 仿真：基于基线 plan + 场景参数变体 → 生成新 COMPUTED plan + 场景版本快照。
     *
     * <p>步骤：
     * <ol>
     *   <li>校验场景状态 DRAFT + 基线 plan 存在</li>
     *   <li>创建新 {@link ErpMfgMrpPlan}（DRAFT，复制基线 plan 元数据，code 加版本后缀）</li>
     *   <li>调用 {@link DemandAggregator#aggregate} 整合需求 → 过滤 SAFETY_STOCK → 按场景覆盖重算 SAFETY_STOCK</li>
     *   <li>fork MRP 计算（lotSize/leadTime/safetyStock 经 paramResolver 覆盖）→ 写 {@link ErpMfgMrpPlanLine}</li>
     *   <li>写场景版本（COMPUTED + computedMrpPlanId 引用）+ 场景状态 RUNNING → COMPLETED</li>
     * </ol>
     *
     * @return 新生成的场景版本
     */
    public ErpMfgMrpScenarioVersion runSimulation(Long scenarioId) {
        ErpMfgMrpScenario scenario = requireScenario(scenarioId);
        if (!Objects.equals(scenario.getStatus(), ErpMfgConstants.SIMULATION_STATUS_DRAFT)) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_SCENARIO_NOT_DRAFT)
                    .param(ErpMfgErrors.ARG_SCENARIO_ID, scenarioId)
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, scenario.getStatus());
        }
        if (scenario.getBaseMrpPlanId() == null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_NO_BASELINE_PLAN)
                    .param(ErpMfgErrors.ARG_SCENARIO_ID, scenarioId);
        }
        ErpMfgMrpPlan basePlan = daoProvider.daoFor(ErpMfgMrpPlan.class)
                .getEntityById(scenario.getBaseMrpPlanId());
        if (basePlan == null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_NO_BASELINE_PLAN)
                    .param(ErpMfgErrors.ARG_SCENARIO_ID, scenarioId);
        }

        // 标记场景 RUNNING
        scenario.setStatus(ErpMfgConstants.SIMULATION_STATUS_RUNNING);
        daoProvider.daoFor(ErpMfgMrpScenario.class).saveOrUpdateEntity(scenario);

        // 1. 新建 COMPUTED 结果 plan（基于基线元数据）
        int nextVersionNo = nextVersionNo(scenarioId);
        ErpMfgMrpPlan computed = daoProvider.daoFor(ErpMfgMrpPlan.class).newEntity();
        computed.setCode(basePlan.getCode() + "-SIM-V" + nextVersionNo);
        computed.setOrgId(basePlan.getOrgId());
        computed.setBusinessDate(basePlan.getBusinessDate());
        computed.setPlanningHorizonDays(basePlan.getPlanningHorizonDays());
        computed.setStatus(ErpMfgConstants.MRP_STATUS_DRAFT);
        computed.setRemark("仿真计算结果（场景 " + scenario.getCode() + " v" + nextVersionNo + "）");
        daoProvider.daoFor(ErpMfgMrpPlan.class).saveEntity(computed);

        // 2. 整合需求：从基线 plan 加载已整合的 demand 行（基线 plan 已 COMPUTED，其 demands 为整合后的快照）。
        //    仿真直接消费基线 demands（不复制到 computed plan，避免 demand 数据冗余；computed plan 的 lines
        //    即为仿真结果）。SAFETY_STOCK 段随后按场景覆盖值在内存中重算。
        List<ErpMfgMrpDemand> demands = loadDemands(basePlan.getId());
        demands = applySafetyStockOverride(demands, scenario, computed);

        // 3. 仿真 MRP 计算（fork）
        computed.setStatus(ErpMfgConstants.MRP_STATUS_RUNNING);
        daoProvider.daoFor(ErpMfgMrpPlan.class).saveOrUpdateEntity(computed);

        LocalDate defaultDate = computed.getBusinessDate() != null ? computed.getBusinessDate() : CoreMetrics.today();
        int[] lineNo = {10};
        IEntityDao<ErpMfgMrpPlanLine> lineDao = daoProvider.daoFor(ErpMfgMrpPlanLine.class);
        for (TopDemand top : topDemandsByMaterial(demands)) {
            processMaterial(computed, top.materialId, top.gross, top.uoMId,
                    top.requirementDate != null ? top.requirementDate : defaultDate,
                    null, new LinkedHashSet<>(), lineDao, lineNo, scenarioId);
        }

        computed.setStatus(ErpMfgConstants.MRP_STATUS_COMPLETED);
        daoProvider.daoFor(ErpMfgMrpPlan.class).saveOrUpdateEntity(computed);

        // 4. 写场景版本
        ErpMfgMrpScenarioVersion version = daoProvider.daoFor(ErpMfgMrpScenarioVersion.class).newEntity();
        version.setScenarioId(scenarioId);
        version.setVersionNo(nextVersionNo);
        version.setComputedMrpPlanId(computed.getId());
        version.setSnapshotSummary(buildSnapshotSummary(computed.getId()));
        version.setStatus(ErpMfgConstants.SIMULATION_STATUS_COMPLETED);
        daoProvider.daoFor(ErpMfgMrpScenarioVersion.class).saveEntity(version);

        // 5. 场景 → COMPLETED
        scenario.setStatus(ErpMfgConstants.SIMULATION_STATUS_COMPLETED);
        daoProvider.daoFor(ErpMfgMrpScenario.class).saveOrUpdateEntity(scenario);

        return version;
    }

    /**
     * 转正式计划：从场景版本复制为新的 DRAFT {@link ErpMfgMrpPlan}（Decision D；plan 2026-07-22-1000-2）。
     *
     * <p>不自动释放为采购单/工单；既有单次释放路径（{@code MrpReleaseService}）不变。
     */
    public ErpMfgMrpPlan promoteToFormalPlan(Long scenarioVersionId) {
        ErpMfgMrpScenarioVersion version = requireVersion(scenarioVersionId);
        if (version.getPromotedPlanId() != null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED)
                    .param(ErpMfgErrors.ARG_SCENARIO_VERSION_ID, scenarioVersionId);
        }
        if (!Objects.equals(version.getStatus(), ErpMfgConstants.SIMULATION_STATUS_COMPLETED)) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_SCENARIO_NOT_DRAFT)
                    .param(ErpMfgErrors.ARG_SCENARIO_VERSION_ID, scenarioVersionId)
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, version.getStatus());
        }
        ErpMfgMrpPlan computed = daoProvider.daoFor(ErpMfgMrpPlan.class)
                .getEntityById(version.getComputedMrpPlanId());
        if (computed == null) {
            throw new NopException(ErpMfgErrors.ERR_MRP_PLAN_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_MRP_PLAN_ID, version.getComputedMrpPlanId());
        }

        ErpMfgMrpPlan promoted = daoProvider.daoFor(ErpMfgMrpPlan.class).newEntity();
        String suffix = ErpMfgConstants.SIMULATION_PROMOTED_PLAN_CODE_SUFFIX
                .replace("{0}", String.valueOf(version.getVersionNo()));
        promoted.setCode(computed.getCode() + suffix);
        promoted.setOrgId(computed.getOrgId());
        promoted.setBusinessDate(computed.getBusinessDate());
        promoted.setPlanningHorizonDays(computed.getPlanningHorizonDays());
        promoted.setStatus(ErpMfgConstants.MRP_STATUS_DRAFT);
        promoted.setRemark("仿真版本 v" + version.getVersionNo() + " 转正式计划");
        daoProvider.daoFor(ErpMfgMrpPlan.class).saveEntity(promoted);

        // 复制计划行（重置 isFirmed / convertedBillCode）
        IEntityDao<ErpMfgMrpPlanLine> lineDao = daoProvider.daoFor(ErpMfgMrpPlanLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", computed.getId()));
        int lineNo = 10;
        for (ErpMfgMrpPlanLine src : lineDao.findAllByQuery(q)) {
            ErpMfgMrpPlanLine dst = lineDao.newEntity();
            dst.setMrpPlanId(promoted.getId());
            dst.setLineNo(lineNo);
            dst.setMaterialId(src.getMaterialId());
            dst.setUoMId(src.getUoMId());
            dst.setOrderType(src.getOrderType());
            dst.setGrossRequirement(src.getGrossRequirement());
            dst.setScheduledReceipt(src.getScheduledReceipt());
            dst.setOnHand(src.getOnHand());
            dst.setNetRequirement(src.getNetRequirement());
            dst.setPlannedQuantity(src.getPlannedQuantity());
            dst.setPlannedDate(src.getPlannedDate());
            dst.setParentLineId(null); // 重置 pegging，转正后用户自行重算或保留
            dst.setIsFirmed(Boolean.FALSE);
            dst.setConvertedBillCode(null);
            lineDao.saveEntity(dst);
            lineNo += 10;
        }

        // 回写版本
        version.setPromotedPlanId(promoted.getId());
        version.setStatus(ErpMfgConstants.SIMULATION_STATUS_ARCHIVED);
        daoProvider.daoFor(ErpMfgMrpScenarioVersion.class).saveOrUpdateEntity(version);

        return promoted;
    }

    private List<ErpMfgMrpDemand> loadDemands(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        return daoProvider.daoFor(ErpMfgMrpDemand.class).findAllByQuery(q);
    }

    // ---------- 仿真 SAFETY_STOCK 重算 ----------

    /**
     * 在内存中重算 SAFETY_STOCK 需求：从 demands 列表移除 SAFETY_STOCK 段（master data 值），
     * 按场景覆盖值重新生成（不写 DB——computed plan 仅承载 lines 结果，demands 来自基线快照）。
     *
     * <p>覆盖规则（Decision B 回退顺序）：场景物料级 safetyStock → null（回退主数据 safetyStock）。
     */
    private List<ErpMfgMrpDemand> applySafetyStockOverride(List<ErpMfgMrpDemand> demands,
                                                            ErpMfgMrpScenario scenario,
                                                            ErpMfgMrpPlan plan) {
        LocalDate planDate = plan.getBusinessDate() != null ? plan.getBusinessDate() : CoreMetrics.today();

        // 内存过滤：保留非 SAFETY_STOCK 行
        java.util.List<ErpMfgMrpDemand> filtered = new java.util.ArrayList<>();
        for (ErpMfgMrpDemand d : demands) {
            if (!Objects.equals(d.getDemandSource(), ErpMfgConstants.MRP_DEMAND_SOURCE_SAFETY_STOCK)) {
                filtered.add(d);
            }
        }

        // 按物料重算 SAFETY_STOCK（扫描全部物料 + 场景覆盖）
        IEntityDao<ErpMdMaterial> matDao = daoProvider.daoFor(ErpMdMaterial.class);
        for (ErpMdMaterial material : matDao.findAllByQuery(new QueryBean())) {
            BigDecimal override = paramResolver.resolveSafetyStockOverride(scenario.getId(), material.getId());
            BigDecimal safety = override != null ? override : nz(material.getSafetyStock());
            if (safety.signum() <= 0) {
                continue;
            }
            BigDecimal available = availableQuantity(material.getId(), plan.getOrgId());
            BigDecimal shortfall = safety.subtract(available);
            if (shortfall.signum() <= 0) {
                continue;
            }
            // 内存构造 demand（不持久化）
            ErpMfgMrpDemand demand = new ErpMfgMrpDemand();
            demand.setMaterialId(material.getId());
            demand.setUoMId(material.getUoMId());
            demand.setDemandSource(ErpMfgConstants.MRP_DEMAND_SOURCE_SAFETY_STOCK);
            demand.setSourceBillType(ErpMfgConstants.SOURCE_BILL_TYPE_MD_MATERIAL);
            demand.setSourceBillCode(material.getCode());
            demand.setQuantity(shortfall);
            demand.setRequirementDate(planDate);
            filtered.add(demand);
        }
        return filtered;
    }

    // ---------- fork MRP 算法（对齐 MrpEngine，覆盖经 paramResolver） ----------

    private void processMaterial(ErpMfgMrpPlan plan, Long materialId, BigDecimal grossQty, Long uoMId,
                                  LocalDate requirementDate, Long parentLineId, Set<Long> path,
                                  IEntityDao<ErpMfgMrpPlanLine> lineDao, int[] lineNo, Long scenarioId) {
        if (materialId == null || grossQty == null || grossQty.signum() <= 0) {
            return;
        }
        if (path.contains(materialId)) {
            return;
        }

        BigDecimal available = availableQuantity(materialId, plan.getOrgId());
        BigDecimal scheduled = BigDecimal.ZERO;
        BigDecimal net = grossQty.subtract(available).subtract(scheduled);
        if (net.signum() < 0) {
            net = BigDecimal.ZERO;
        }
        BigDecimal planned = lotSize(net, scenarioId);

        ErpMfgBom bom = bomExpander.findDefaultBomOrNull(materialId);
        boolean manufactured = bom != null;
        String orderType = manufactured
                ? ErpMfgConstants.MRP_ORDER_TYPE_WORK_ORDER_REQUEST
                : ErpMfgConstants.MRP_ORDER_TYPE_PURCHASE_REQUEST;
        long leadDays = manufactured ? mfgLeadDays(bom.getId()) : purLeadDays(materialId, scenarioId);
        LocalDate plannedDate = requirementDate.minusDays(leadDays);

        ErpMfgMrpPlanLine line = lineDao.newEntity();
        line.setMrpPlanId(plan.getId());
        line.setLineNo(lineNo[0]);
        line.setMaterialId(materialId);
        line.setUoMId(resolveUoM(uoMId, materialId));
        line.setOrderType(orderType);
        line.setGrossRequirement(grossQty);
        line.setScheduledReceipt(scheduled);
        line.setOnHand(available);
        line.setNetRequirement(net);
        line.setPlannedQuantity(planned);
        line.setPlannedDate(plannedDate);
        line.setIsFirmed(Boolean.FALSE);
        line.setParentLineId(parentLineId);
        lineDao.saveEntity(line);
        lineNo[0] += 10;

        if (manufactured && planned.signum() > 0) {
            path.add(materialId);
            try {
                List<BomExplosionNode> children = bomExpander.explode(bom.getId(), planned, false);
                for (BomExplosionNode child : children) {
                    processMaterial(plan, child.getMaterialId(), child.getQuantity(), null,
                            plannedDate, line.getId(), path, lineDao, lineNo, scenarioId);
                }
            } finally {
                path.remove(materialId);
            }
        }
    }

    /**
     * 仿真 lotSize：场景覆盖 LOT_SIZE → 全局配置 CONFIG_MRP_DEFAULT_LOT_SIZE → 默认 0（lot-for-lot）。
     */
    private BigDecimal lotSize(BigDecimal net, Long scenarioId) {
        if (net == null || net.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal override = paramResolver.resolveLotSizeOverride(scenarioId);
        int defaultLot;
        if (override != null) {
            defaultLot = override.intValue();
        } else {
            defaultLot = AppConfig.var(ErpMfgConstants.CONFIG_MRP_DEFAULT_LOT_SIZE,
                    ErpMfgConstants.DEFAULT_MRP_DEFAULT_LOT_SIZE);
        }
        if (defaultLot <= 0) {
            return net; // lot-for-lot
        }
        BigDecimal lot = new BigDecimal(defaultLot);
        BigDecimal multiples = net.divide(lot, 0, RoundingMode.CEILING);
        return multiples.multiply(lot);
    }

    private long mfgLeadDays(Long bomId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("bomId", bomId));
        List<ErpMfgBomOperation> ops = daoProvider.daoFor(ErpMfgBomOperation.class).findAllByQuery(q);
        BigDecimal minutes = BigDecimal.ZERO;
        for (ErpMfgBomOperation op : ops) {
            minutes = minutes.add(nz(op.getStandardTime()));
        }
        if (minutes.signum() <= 0) {
            return 0L;
        }
        BigDecimal hours = minutes.divide(SIXTY, 6, RoundingMode.HALF_UP);
        double daysPerHour = AppConfig.var(ErpMfgConstants.CONFIG_MFG_LEADTIME_DAYS_PER_ROUTING_HOUR,
                ErpMfgConstants.DEFAULT_MFG_LEADTIME_DAYS_PER_ROUTING_HOUR);
        double days = hours.doubleValue() * daysPerHour;
        return Math.max(0L, (long) Math.ceil(days));
    }

    /**
     * 仿真 purLeadDays：场景覆盖 LEAD_TIME → 主数据 material.leadTimeDays → 0。
     */
    private long purLeadDays(Long materialId, Long scenarioId) {
        BigDecimal override = paramResolver.resolveLeadTimeOverride(scenarioId, materialId);
        if (override != null) {
            return Math.max(0L, override.longValue());
        }
        ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
        if (material == null || material.getLeadTimeDays() == null) {
            return 0L;
        }
        return Math.max(0L, material.getLeadTimeDays());
    }

    private Long resolveUoM(Long uoMId, Long materialId) {
        if (uoMId != null) {
            return uoMId;
        }
        ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
        return material != null ? material.getUoMId() : null;
    }

    private BigDecimal availableQuantity(Long materialId, Long orgId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        if (orgId != null) {
            q.addFilter(eq("orgId", orgId));
        }
        List<ErpInvStockBalance> balances = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        BigDecimal total = BigDecimal.ZERO;
        for (ErpInvStockBalance b : balances) {
            BigDecimal avail = b.getAvailableQuantity();
            if (avail == null) {
                avail = nz(b.getTotalQuantity()).subtract(nz(b.getReservedQuantity())).subtract(nz(b.getLockedQuantity()));
            }
            total = total.add(avail);
        }
        return total;
    }

    private List<TopDemand> topDemandsByMaterial(List<ErpMfgMrpDemand> demands) {
        java.util.Map<Long, TopDemand> byMaterial = new java.util.LinkedHashMap<>();
        for (ErpMfgMrpDemand d : demands) {
            if (d.getMaterialId() == null) {
                continue;
            }
            TopDemand t = byMaterial.computeIfAbsent(d.getMaterialId(), k -> new TopDemand(k));
            t.gross = t.gross.add(nz(d.getQuantity()));
            if (t.uoMId == null) {
                t.uoMId = d.getUoMId();
            }
            if (d.getRequirementDate() != null && (t.requirementDate == null || d.getRequirementDate().isAfter(t.requirementDate))) {
                t.requirementDate = d.getRequirementDate();
            }
        }
        return new java.util.ArrayList<>(byMaterial.values());
    }

    // ---------- 辅助 ----------

    private String buildSnapshotSummary(Long computedPlanId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", computedPlanId));
        List<ErpMfgMrpPlanLine> lines = daoProvider.daoFor(ErpMfgMrpPlanLine.class).findAllByQuery(q);
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalPlanned = BigDecimal.ZERO;
        int shortageCount = 0;
        for (ErpMfgMrpPlanLine l : lines) {
            totalNet = totalNet.add(nz(l.getNetRequirement()));
            totalPlanned = totalPlanned.add(nz(l.getPlannedQuantity()));
            if (nz(l.getNetRequirement()).signum() > 0) {
                shortageCount++;
            }
        }
        return String.format("lines=%d,totalNet=%s,totalPlanned=%s,shortageMaterials=%d",
                lines.size(), totalNet.toPlainString(), totalPlanned.toPlainString(), shortageCount);
    }

    private int nextVersionNo(Long scenarioId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("scenarioId", scenarioId));
        q.addOrderField("versionNo", false);
        q.setLimit(1);
        List<ErpMfgMrpScenarioVersion> top = daoProvider.daoFor(ErpMfgMrpScenarioVersion.class).findAllByQuery(q);
        if (top.isEmpty() || top.get(0).getVersionNo() == null) {
            return 1;
        }
        return top.get(0).getVersionNo() + 1;
    }

    private ErpMfgMrpScenario requireScenario(Long scenarioId) {
        if (scenarioId == null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_NO_BASELINE_PLAN)
                    .param(ErpMfgErrors.ARG_SCENARIO_ID, scenarioId);
        }
        ErpMfgMrpScenario scenario = daoProvider.daoFor(ErpMfgMrpScenario.class).getEntityById(scenarioId);
        if (scenario == null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_NO_BASELINE_PLAN)
                    .param(ErpMfgErrors.ARG_SCENARIO_ID, scenarioId);
        }
        return scenario;
    }

    private ErpMfgMrpScenarioVersion requireVersion(Long versionId) {
        ErpMfgMrpScenarioVersion version = daoProvider.daoFor(ErpMfgMrpScenarioVersion.class).getEntityById(versionId);
        if (version == null) {
            throw new NopException(ErpMfgErrors.ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED)
                    .param(ErpMfgErrors.ARG_SCENARIO_VERSION_ID, versionId);
        }
        return version;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static class TopDemand {
        final Long materialId;
        BigDecimal gross = BigDecimal.ZERO;
        Long uoMId;
        LocalDate requirementDate;

        TopDemand(Long materialId) {
            this.materialId = materialId;
        }
    }
}
