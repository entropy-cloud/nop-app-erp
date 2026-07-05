package app.erp.mfg.service.mrp;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.mfg.biz.BomExplosionNode;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgMrpDemand;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.bom.BomExpander;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.api.core.time.CoreMetrics;

/**
 * MRP 计算引擎。服务于 {@code IErpMfgMrpPlanBiz.runMrp}（{@code mrp.md §MRP 流程}）。
 *
 * <p>算法（自顶向下递归，按计划量展开）：
 * <ul>
 *   <li>毛需求来源：{@link ErpMfgMrpDemand}（由 {@link DemandAggregator} 整合的销售订单/安全库存/需求预测/手工需求）。</li>
 *   <li>净需求 = 毛需求 − 可用量（{@link ErpInvStockBalance} 合计 total − reserved − locked），负值归零。</li>
 *   <li>计划订单类型：有默认且有效的 BOM（制造件）→ WORK_ORDER_REQUEST；否则（采购件）→ PURCHASE_REQUEST。</li>
 *   <li>BOM 多级展开：制造件经 {@link BomExpander#explode} 单级展开（按计划量缩放子件有效用量），递归到采购件；
 *       DFS 路径回溯防环（复用 BomExpander 环检测，本类额外维护访问集合兜底）。</li>
 *   <li>提前期偏移：采购件按 {@link ErpMdMaterial#getLeadTimeDays()}；制造件按 BOM 工序累计 standardTime（分钟）
 *       换算（hours × {@code erp-mfg.mfg-leadtime-days-per-routing-hour}）。</li>
 *   <li>按期分单：lot-for-lot（净需求即建议量）；{@code erp-mfg.default-lot-size}&gt;0 时按倍数向上取整。</li>
 *   <li>Pegging：每条计划行记录 {@code parentLineId}（多级 BOM 层级链）；需求来源追溯见 demand 行。</li>
 * </ul>
 *
 * <p><b>Non-Goal</b>：物料级批量策略（minOrderQty/fixedLotSize/lowLevelCode 物化列）、
 * CRP 产能校验、AUTO_SCHEDULED、需求时界、委外释放、scrapRate 纳入净需求（见计划 Non-Goals）。
 * FORECAST 来源已落地（plan 2026-07-05-0427-1）。
 *
 * <p>本类为非 BizModel 服务助手（对齐 {@code BomExpander}/{@code CostRollupService} 范式），跨域只读聚合
 * （inventory/master-data）直接用 {@link IDaoProvider}。
 */
public class MrpEngine {

    static final BigDecimal SIXTY = new BigDecimal("60");

    @Inject
    IDaoProvider daoProvider;
    @Inject
    BomExpander bomExpander;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setBomExpander(BomExpander bomExpander) {
        this.bomExpander = bomExpander;
    }

    /**
     * 运行 MRP：计划状态 DRAFT→RUNNING→COMPLETED；清除既有计划行后重算并写入 {@link ErpMfgMrpPlanLine}。
     *
     * @param demands 已整合的独立需求行（由 {@link DemandAggregator#aggregate} 产出，直接传入避免同事务查询可见性问题）
     */
    public void runMrp(Long planId, List<ErpMfgMrpDemand> demands) {
        ErpMfgMrpPlan plan = requirePlan(planId);
        if (plan.getStatus() != null && !Objects.equals(plan.getStatus(), ErpMfgConstants.MRP_STATUS_DRAFT)) {
            throw new NopException(ErpMfgErrors.ERR_MRP_INVALID_PLAN_STATUS)
                    .param(ErpMfgErrors.ARG_PLAN_CODE, plan.getCode())
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, plan.getStatus());
        }
        plan.setStatus(ErpMfgConstants.MRP_STATUS_RUNNING);
        daoProvider.daoFor(ErpMfgMrpPlan.class).updateEntity(plan);

        IEntityDao<ErpMfgMrpPlanLine> lineDao = daoProvider.daoFor(ErpMfgMrpPlanLine.class);
        clearLines(lineDao, planId);

        LocalDate defaultDate = plan.getBusinessDate() != null ? plan.getBusinessDate() : CoreMetrics.today();
        int[] lineNo = {10};

        for (TopDemand top : topDemandsByMaterial(demands)) {
            processMaterial(plan, top.materialId, top.gross, top.uoMId, top.requirementDate != null ? top.requirementDate : defaultDate,
                    null, new LinkedHashSet<>(), lineDao, lineNo);
        }

        plan.setStatus(ErpMfgConstants.MRP_STATUS_COMPLETED);
        daoProvider.daoFor(ErpMfgMrpPlan.class).updateEntity(plan);
    }

    private void processMaterial(ErpMfgMrpPlan plan, Long materialId, BigDecimal grossQty, Long uoMId,
                                 LocalDate requirementDate, Long parentLineId, Set<Long> path,
                                 IEntityDao<ErpMfgMrpPlanLine> lineDao, int[] lineNo) {
        if (materialId == null || grossQty == null || grossQty.signum() <= 0) {
            return;
        }
        if (path.contains(materialId)) {
            return; // 兜底防环（BomExpander 已检测显式环）
        }

        BigDecimal available = availableQuantity(materialId, plan.getOrgId());
        BigDecimal scheduled = BigDecimal.ZERO;
        BigDecimal net = grossQty.subtract(available).subtract(scheduled);
        if (net.signum() < 0) {
            net = BigDecimal.ZERO;
        }
        BigDecimal planned = lotSize(net);

        ErpMfgBom bom = bomExpander.findDefaultBomOrNull(materialId);
        boolean manufactured = bom != null;
        String orderType = manufactured
                ? ErpMfgConstants.MRP_ORDER_TYPE_WORK_ORDER_REQUEST
                : ErpMfgConstants.MRP_ORDER_TYPE_PURCHASE_REQUEST;
        long leadDays = manufactured ? mfgLeadDays(bom.getId()) : purLeadDays(materialId);
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
                            plannedDate, line.getId(), path, lineDao, lineNo);
                }
            } finally {
                path.remove(materialId);
            }
        }
    }

    private BigDecimal lotSize(BigDecimal net) {
        if (net == null || net.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        int defaultLot = AppConfig.var(ErpMfgConstants.CONFIG_MRP_DEFAULT_LOT_SIZE,
                ErpMfgConstants.DEFAULT_MRP_DEFAULT_LOT_SIZE);
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

    private long purLeadDays(Long materialId) {
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

    private void clearLines(IEntityDao<ErpMfgMrpPlanLine> dao, Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        List<ErpMfgMrpPlanLine> lines = dao.findAllByQuery(q);
        for (ErpMfgMrpPlanLine l : lines) {
            dao.deleteEntity(l);
        }
    }

    private ErpMfgMrpPlan requirePlan(Long planId) {
        if (planId == null) {
            throw new NopException(ErpMfgErrors.ERR_MRP_PLAN_NOT_FOUND).param(ErpMfgErrors.ARG_MRP_PLAN_ID, planId);
        }
        ErpMfgMrpPlan plan = daoProvider.daoFor(ErpMfgMrpPlan.class).getEntityById(planId);
        if (plan == null) {
            throw new NopException(ErpMfgErrors.ERR_MRP_PLAN_NOT_FOUND).param(ErpMfgErrors.ARG_MRP_PLAN_ID, planId);
        }
        return plan;
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
