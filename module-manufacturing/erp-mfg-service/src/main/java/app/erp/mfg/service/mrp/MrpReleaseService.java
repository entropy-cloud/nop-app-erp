package app.erp.mfg.service.mrp;

import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * MRP 计划订单释放。服务于 {@code IErpMfgMrpPlanLineBiz} 的释放方法（{@code mrp.md §建议单释放}）。
 *
 * <p>释放语义（按建议类型分派，对外暴露两个 purpose-built 方法，避免 GraphQL 可空参数问题）：
 * <ul>
 *   <li><b>PURCHASE_REQUEST</b>（{@link #releasePurchaseRequest}）→ 生成 {@link ErpPurOrder}（+ 单行
 *       {@link ErpPurOrderLine}，物料/数量/计划到货日）。ErpPurOrder.supplierId/currencyId 为必填，由调用方提供。</li>
 *   <li><b>WORK_ORDER_REQUEST</b>（{@link #releaseWorkRequest}）→ 生成 {@link ErpMfgWorkOrder}
 *       （productId=物料/bomId=默认BOM/计划开工日）。</li>
 *   <li>释放后回写 {@code isFirmed=true} + {@code convertedBillCode}；计划全部行 firmed 后 status→FIRMED。</li>
 *   <li>幂等：已 firmed 行重复释放拒绝（{@link ErpMfgErrors#ERR_MRP_LINE_ALREADY_FIRMED}）。</li>
 * </ul>
 *
 * <p><b>实现说明（偏离计划 Task Route「调 I*Biz」Decision）</b>：IErpPurOrderBiz/IErpMfgWorkOrderBiz 仅提供订单头级
 * 通用 CRUD（save(Map)），无 purpose-built {@code createFromMrpLine} 方法，且通用 save 需调用方填齐所有必填字段
 * 并穿越 CRUD 校验管道。故释放直接持久化目标域实体（service-helper 范式，对齐 {@code BomExpander}/{@code CostRollupService}
 * 与测试种子方式），仅写入 MRP 已知字段（物料/数量/日期/org），其余（供应商由参数提供，单价/金额置 0 待采购员补录）。
 * 残留风险：生成的采购单单价/金额为 0、币种由参数提供，须计划员/采购员后续完善。
 *
 * <p><b>数据权限边界</b>：释放由 {@code ErpMfgMrpPlanLineBizModel} 的 {@code @BizMutation} 入口触发，权限校验在该入口完成；
 * 生成的采购单/工单继承 MRP 计划的 {@code orgId}；{@code @BizMutation} 自动事务保证 MRP 行 firmed 与目标单据生成原子。
 * 跨域持久化经 {@code IDaoProvider}（非 {@code IErpPurOrderBiz}），因目标单为骨架草稿（单价/金额 0），通用 save 管道会因
 * 必填校验拒绝；待采购域提供 purpose-built {@code createFromMrpLine} 时可收敛为 I*Biz 调用（successor）。
 *
 * <p><b>Non-Goal</b>：SUBCONTRACT_REQUEST（委外）释放——委外流程独立面，本期不支持。
 */
public class MrpReleaseService {

    static final String PUR_DOC_STATUS_DRAFT = "DRAFT";
    static final String PUR_APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 释放采购建议行为采购订单。返回生成的采购单号（回写 convertedBillCode）。
     */
    public String releasePurchaseRequest(Long planLineId, Long supplierId, Long currencyId) {
        if (supplierId == null) {
            throw new NopException(ErpMfgErrors.ERR_MRP_RELEASE_MISSING_SUPPLIER)
                    .param(ErpMfgErrors.ARG_MRP_LINE_ID, planLineId);
        }
        ErpMfgMrpPlanLine line = requireReleasable(planLineId, ErpMfgConstants.MRP_ORDER_TYPE_PURCHASE_REQUEST);
        ErpMfgMrpPlan plan = daoProvider.daoFor(ErpMfgMrpPlan.class).getEntityById(line.getMrpPlanId());
        String billCode = releaseToPurchaseOrder(line, plan, supplierId, currencyId, CoreMetrics.today());
        markFirmed(line, billCode);
        advancePlanToFirmedIfComplete(plan);
        return billCode;
    }

    /**
     * 释放工单建议行为工单。返回生成的工单号（回写 convertedBillCode）。
     */
    public String releaseWorkRequest(Long planLineId) {
        ErpMfgMrpPlanLine line = requireReleasable(planLineId, ErpMfgConstants.MRP_ORDER_TYPE_WORK_ORDER_REQUEST);
        ErpMfgMrpPlan plan = daoProvider.daoFor(ErpMfgMrpPlan.class).getEntityById(line.getMrpPlanId());
        String billCode = releaseToWorkOrder(line, plan, CoreMetrics.today());
        markFirmed(line, billCode);
        advancePlanToFirmedIfComplete(plan);
        return billCode;
    }

    private ErpMfgMrpPlanLine requireReleasable(Long planLineId, String expectedOrderType) {
        ErpMfgMrpPlanLine line = requireLine(planLineId);
        if (Boolean.TRUE.equals(line.getIsFirmed())) {
            throw new NopException(ErpMfgErrors.ERR_MRP_LINE_ALREADY_FIRMED)
                    .param(ErpMfgErrors.ARG_MRP_LINE_ID, planLineId);
        }
        String orderType = line.getOrderType();
        if (orderType == null || !Objects.equals(orderType, expectedOrderType)) {
            throw new NopException(ErpMfgErrors.ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE)
                    .param(ErpMfgErrors.ARG_MRP_LINE_ID, planLineId);
        }
        return line;
    }

    private void markFirmed(ErpMfgMrpPlanLine line, String billCode) {
        line.setIsFirmed(Boolean.TRUE);
        line.setConvertedBillCode(billCode);
        daoProvider.daoFor(ErpMfgMrpPlanLine.class).updateEntity(line);
    }

    private String releaseToPurchaseOrder(ErpMfgMrpPlanLine line, ErpMfgMrpPlan plan, Long supplierId,
                                          Long currencyId, LocalDate today) {
        IEntityDao<ErpPurOrder> orderDao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder order = orderDao.newEntity();
        String code = ErpMfgConstants.RELEASE_PO_CODE_PREFIX + line.getId();
        order.setCode(code);
        order.setOrgId(plan != null ? plan.getOrgId() : null);
        order.setSupplierId(supplierId);
        order.setCurrencyId(currencyId);
        order.setBusinessDate(today);
        order.setDeliveryDate(line.getPlannedDate());
        order.setDocStatus(PUR_DOC_STATUS_DRAFT);
        order.setApproveStatus(PUR_APPROVE_STATUS_UNSUBMITTED);
        // O-4 架构豁免：MRP 自动释放不走人工审批管道，跨模块直接持久化采购单骨架草稿（单价/金额 0 待采购员补录）。
        // 理由/风险/补偿见 docs/architecture/posting-exemptions.md §MrpReleaseService
        orderDao.saveEntity(order);

        IEntityDao<ErpPurOrderLine> lineDao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine poLine = lineDao.newEntity();
        poLine.setOrderId(order.getId());
        poLine.setLineNo(10);
        poLine.setMaterialId(line.getMaterialId());
        poLine.setUoMId(line.getUoMId());
        poLine.setQuantity(nz(line.getPlannedQuantity()));
        poLine.setUnitPrice(BigDecimal.ZERO);
        poLine.setAmount(BigDecimal.ZERO);
        lineDao.saveEntity(poLine);
        return code;
    }

    private String releaseToWorkOrder(ErpMfgMrpPlanLine line, ErpMfgMrpPlan plan, LocalDate today) {
        IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
        ErpMfgWorkOrder wo = dao.newEntity();
        String code = ErpMfgConstants.RELEASE_WO_CODE_PREFIX + line.getId();
        wo.setCode(code);
        wo.setProductId(line.getMaterialId());
        ErpMfgBom defaultBom = findDefaultBomOrNull(line.getMaterialId());
        if (defaultBom != null) {
            wo.setBomId(defaultBom.getId());
        }
        wo.setPlannedQuantity(nz(line.getPlannedQuantity()));
        wo.setPlannedStartDate(line.getPlannedDate());
        wo.setBusinessDate(today);
        wo.setOrgId(plan != null ? plan.getOrgId() : null);
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
        wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(wo);
        return code;
    }

    private void advancePlanToFirmedIfComplete(ErpMfgMrpPlan plan) {
        if (plan == null) {
            return;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", plan.getId()));
        List<ErpMfgMrpPlanLine> lines = daoProvider.daoFor(ErpMfgMrpPlanLine.class).findAllByQuery(q);
        boolean allFirmed = !lines.isEmpty();
        for (ErpMfgMrpPlanLine l : lines) {
            if (!Boolean.TRUE.equals(l.getIsFirmed())) {
                allFirmed = false;
                break;
            }
        }
        if (allFirmed) {
            plan.setStatus(ErpMfgConstants.MRP_STATUS_FIRMED);
            daoProvider.daoFor(ErpMfgMrpPlan.class).updateEntity(plan);
        }
    }

    private ErpMfgBom findDefaultBomOrNull(Long productId) {
        if (productId == null) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("productId", productId));
        q.addFilter(eq("isDefault", Boolean.TRUE));
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.setLimit(1);
        List<ErpMfgBom> list = daoProvider.daoFor(ErpMfgBom.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpMfgMrpPlanLine requireLine(Long planLineId) {
        if (planLineId == null) {
            throw new NopException(ErpMfgErrors.ERR_MRP_PLAN_LINE_NOT_FOUND).param(ErpMfgErrors.ARG_MRP_LINE_ID, planLineId);
        }
        ErpMfgMrpPlanLine line = daoProvider.daoFor(ErpMfgMrpPlanLine.class).getEntityById(planLineId);
        if (line == null) {
            throw new NopException(ErpMfgErrors.ERR_MRP_PLAN_LINE_NOT_FOUND).param(ErpMfgErrors.ARG_MRP_LINE_ID, planLineId);
        }
        return line;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
