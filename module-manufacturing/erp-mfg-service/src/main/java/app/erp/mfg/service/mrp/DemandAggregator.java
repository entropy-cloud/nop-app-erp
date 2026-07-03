package app.erp.mfg.service.mrp;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.mfg.dao.entity.ErpMfgMrpDemand;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * MRP 独立需求整合。服务于 {@code IErpMfgMrpPlanBiz.runMrp} 的需求整合阶段（{@code mrp.md §MRP 流程：需求来源}）。
 *
 * <p>整合三类独立需求为 {@link ErpMfgMrpDemand} 行，作为 MRP 运行的统一输入：
 * <ul>
 *   <li><b>销售订单</b>（demandSource=SALES_ORDER）：经 {@link ErpSalOrderLine} 只读未交量
 *       （quantity − deliveredQuantity &gt; 0），排除作废订单（docStatus≠CANCELLED）。</li>
 *   <li><b>安全库存补货</b>（demandSource=SAFETY_STOCK）：{@link ErpMdMaterial#getSafetyStock()} − 当前可用量
 *       （{@link ErpInvStockBalance} 合计 total − reserved − locked）&lt; 0 时补货。</li>
 *   <li><b>手工需求</b>（demandSource=MANUAL）：既有 {@link ErpMfgMrpDemand} 行原样保留，不重算。</li>
 * </ul>
 *
 * <p><b>Non-Goal</b>：FORECAST（销售预测）来源——ORM 无 ErpMfgForecast 实体，本期不支持。
 *
 * <p>本类为非 BizModel 服务助手（对齐 {@code BomExpander}/{@code CostRollupService} 范式），跨域只读聚合
 * （sales/inventory/master-data）直接用 {@link IDaoProvider}：I*Biz 以订单头为粒度，不便支撑行级批量聚合读取。
 */
public class DemandAggregator {

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 整合 planId 关联计划的所有独立需求，写入 {@link ErpMfgMrpDemand}。
     *
     * <p>幂等：先清除该计划下 demandSource∈{SALES_ORDER, SAFETY_STOCK} 的合成行（保留 MANUAL 手工行），
     * 再按当前销售订单/库存余额重算。同物料同期需求不在此处合并——合并发生在 {@link MrpEngine} 按物料汇总毛需求时，
     * 此处逐来源行写入以保留 pegging 追溯（sourceBillType/sourceBillCode）。
     *
     * @return 整合后该计划的全部活跃需求行（手工 + 合成），供 {@link MrpEngine} 直接消费，避免同事务查询可见性问题
     */
    public List<ErpMfgMrpDemand> aggregate(Long planId) {
        ErpMfgMrpPlan plan = requirePlan(planId);
        IEntityDao<ErpMfgMrpDemand> dao = daoProvider.daoFor(ErpMfgMrpDemand.class);

        List<ErpMfgMrpDemand> active = clearSynthesized(dao, planId);

        LocalDate planDate = plan.getBusinessDate() != null ? plan.getBusinessDate() : LocalDate.now();
        int lineNo = nextLineNo(dao, planId);

        lineNo = collectSalesOrderDemands(dao, plan, planDate, lineNo, active);
        lineNo = collectSafetyStockDemands(dao, plan, planDate, lineNo, active);

        return active;
    }

    private int collectSalesOrderDemands(IEntityDao<ErpMfgMrpDemand> dao, ErpMfgMrpPlan plan,
                                         LocalDate planDate, int lineNo, List<ErpMfgMrpDemand> sink) {
        IEntityDao<ErpSalOrder> orderDao = daoProvider.daoFor(ErpSalOrder.class);
        IEntityDao<ErpSalOrderLine> lineDao = daoProvider.daoFor(ErpSalOrderLine.class);

        QueryBean oq = new QueryBean();
        oq.addFilter(ne("docStatus", ErpMfgConstants.SAL_DOC_STATUS_CANCELLED));
        if (plan.getOrgId() != null) {
            oq.addFilter(eq("orgId", plan.getOrgId()));
        }
        List<ErpSalOrder> orders = orderDao.findAllByQuery(oq);

        for (ErpSalOrder order : orders) {
            QueryBean lq = new QueryBean();
            lq.addFilter(eq("orderId", order.getId()));
            List<ErpSalOrderLine> lines = lineDao.findAllByQuery(lq);
            LocalDate reqDate = order.getDeliveryDate() != null ? order.getDeliveryDate() : planDate;
            for (ErpSalOrderLine line : lines) {
                BigDecimal qty = nz(line.getQuantity());
                BigDecimal undelivered = qty.subtract(nz(line.getDeliveredQuantity()));
                if (undelivered.signum() <= 0 || line.getMaterialId() == null) {
                    continue;
                }
                ErpMfgMrpDemand demand = newDemand(plan, lineNo);
                demand.setMaterialId(line.getMaterialId());
                demand.setUoMId(line.getUoMId());
                demand.setDemandSource(ErpMfgConstants.MRP_DEMAND_SOURCE_SALES_ORDER);
                demand.setSourceBillType(ErpMfgConstants.SOURCE_BILL_TYPE_SAL_ORDER);
                demand.setSourceBillCode(order.getCode());
                demand.setQuantity(undelivered);
                demand.setRequirementDate(reqDate);
                dao.saveEntity(demand);
                sink.add(demand);
                lineNo += 10;
            }
        }
        return lineNo;
    }

    private int collectSafetyStockDemands(IEntityDao<ErpMfgMrpDemand> dao, ErpMfgMrpPlan plan,
                                          LocalDate planDate, int lineNo, List<ErpMfgMrpDemand> sink) {
        IEntityDao<ErpMdMaterial> matDao = daoProvider.daoFor(ErpMdMaterial.class);
        List<ErpMdMaterial> materials = matDao.findAllByQuery(new QueryBean());
        for (ErpMdMaterial material : materials) {
            BigDecimal safety = nz(material.getSafetyStock());
            if (safety.signum() <= 0) {
                continue;
            }
            BigDecimal available = availableQuantity(material.getId(), plan.getOrgId());
            BigDecimal shortfall = safety.subtract(available);
            if (shortfall.signum() <= 0) {
                continue;
            }
            ErpMfgMrpDemand demand = newDemand(plan, lineNo);
            demand.setMaterialId(material.getId());
            demand.setUoMId(material.getUoMId());
            demand.setDemandSource(ErpMfgConstants.MRP_DEMAND_SOURCE_SAFETY_STOCK);
            demand.setSourceBillType(ErpMfgConstants.SOURCE_BILL_TYPE_MD_MATERIAL);
            demand.setSourceBillCode(material.getCode());
            demand.setQuantity(shortfall);
            demand.setRequirementDate(planDate);
            dao.saveEntity(demand);
            sink.add(demand);
            lineNo += 10;
        }
        return lineNo;
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

    private ErpMfgMrpDemand newDemand(ErpMfgMrpPlan plan, int lineNo) {
        ErpMfgMrpDemand demand = daoProvider.daoFor(ErpMfgMrpDemand.class).newEntity();
        demand.setMrpPlanId(plan.getId());
        demand.setLineNo(lineNo);
        return demand;
    }

    private List<ErpMfgMrpDemand> clearSynthesized(IEntityDao<ErpMfgMrpDemand> dao, Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        List<ErpMfgMrpDemand> all = dao.findAllByQuery(q);
        List<ErpMfgMrpDemand> kept = new ArrayList<>();
        for (ErpMfgMrpDemand d : all) {
            String src = d.getDemandSource();
            if (src != null && !Objects.equals(src, ErpMfgConstants.MRP_DEMAND_SOURCE_MANUAL)) {
                dao.deleteEntity(d);
            } else {
                kept.add(d);
            }
        }
        return kept;
    }

    private int nextLineNo(IEntityDao<ErpMfgMrpDemand> dao, Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        q.addOrderField("lineNo", true);
        q.setLimit(1);
        List<ErpMfgMrpDemand> top = dao.findAllByQuery(q);
        if (top.isEmpty() || top.get(0).getLineNo() == null) {
            return 10;
        }
        return top.get(0).getLineNo() + 10;
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
}
