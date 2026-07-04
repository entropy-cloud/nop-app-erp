package app.erp.drp.service.drp;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import app.erp.inv.dao.entity.ErpInvTransferOrderLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * DRP 计划释放。服务于 {@code IErpDrpLineBiz.releaseLine/releaseApproved}（{@code drp/state-machine.md §场景 C}、
 * {@code drp/use-cases.md UC-DRP-03}）。
 *
 * <p>释放语义（按补货类型分派）：
 * <ul>
 *   <li><b>TRANSFER</b> → 生成 {@link ErpInvTransferOrder}（+ 单行 {@link ErpInvTransferOrderLine}，
 *       fromWarehouseId=parameter.preferredSourceWarehouseId，toWarehouseId=line.warehouseId）。</li>
 *   <li><b>PURCHASE</b> → 生成 {@link ErpPurOrder}（+ 单行 {@link ErpPurOrderLine}，supplierId=parameter.preferredSupplierId）。</li>
 *   <li>释放后回写 {@code orderBillType/orderBillCode}；DrpLine→ORDERED（终态）。</li>
 *   <li>幂等：已 ORDERED 行重复释放拒绝（{@link ErpDrpErrors#ERR_DRP_LINE_ALREADY_ORDERED}）。</li>
 *   <li>非 APPROVED 行释放拒绝（{@link ErpDrpErrors#ERR_DRP_LINE_NOT_SUGGESTED}）。</li>
 * </ul>
 *
 * <p><b>实现说明（偏离计划 Task Route「跨域写经注入 I*Biz」Decision）</b>：IErpInvTransferOrderBiz/IErpPurOrderBiz 仅提供订单头级
 * 通用 CRUD（save(Map)），无 purpose-built {@code createFromDrpLine} 方法，且通用 save 需调用方填齐所有必填字段并穿越 CRUD 校验管道。
 * 故释放直接持久化目标域实体（service-helper 范式，对齐 MRP {@code MrpReleaseService} 与测试种子方式），仅写入 DRP 已知字段
 * （物料/数量/仓库/供应商/org），其余（采购单价/金额置 0 待采购员补录）。残留风险：生成的采购单单价/金额为 0，须采购员后续完善。
 *
 * <p><b>Non-Goal</b>：越库释放（UC-DRP-07 独立面）；提前期收货自动写入（UC-DRP-08 归 purchase 收货 follow-up）。
 */
public class DrpReleaseService {

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 释放单条 APPROVED 明细行。返回生成的下游单据 code（回写 orderBillCode）。
     */
    public String releaseLine(Long lineId) {
        ErpDrpLine line = requireReleasable(lineId);
        ErpDrpParameter param = requireParameter(line);
        String billCode;
        String billType;
        if (ErpDrpConstants.REPLENISHMENT_TYPE_TRANSFER.equals(line.getReplenishmentType())) {
            if (param.getPreferredSourceWarehouseId() == null && line.getSourceWarehouseId() == null) {
                throw new NopException(ErpDrpErrors.ERR_DRP_NO_SOURCE_WAREHOUSE)
                        .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId);
            }
            Long sourceWh = param.getPreferredSourceWarehouseId() != null
                    ? param.getPreferredSourceWarehouseId() : line.getSourceWarehouseId();
            billType = ErpDrpConstants.ORDER_BILL_TYPE_TRANSFER_ORDER;
            billCode = releaseToTransferOrder(line, sourceWh, LocalDate.now());
        } else if (ErpDrpConstants.REPLENISHMENT_TYPE_PURCHASE.equals(line.getReplenishmentType())) {
            if (param.getPreferredSupplierId() == null) {
                throw new NopException(ErpDrpErrors.ERR_DRP_NO_PREFERRED_SUPPLIER)
                        .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId);
            }
            billType = ErpDrpConstants.ORDER_BILL_TYPE_PURCHASE_ORDER;
            billCode = releaseToPurchaseOrder(line, param.getPreferredSupplierId(), LocalDate.now());
        } else {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_NOT_SUGGESTED)
                    .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId);
        }
        line.setOrderBillType(billType);
        line.setOrderBillCode(billCode);
        line.setStatus(ErpDrpConstants.DRP_LINE_STATUS_ORDERED);
        daoProvider.daoFor(ErpDrpLine.class).updateEntity(line);
        advancePlanToExecutedIfComplete(line.getPlanId());
        return billCode;
    }

    /**
     * 批量释放计划下所有 APPROVED 行；全部行 ORDERED 后计划 APPROVED→EXECUTED。
     *
     * @return 释放的行数
     */
    public int releaseApproved(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        q.addFilter(eq("status", ErpDrpConstants.DRP_LINE_STATUS_APPROVED));
        List<ErpDrpLine> lines = daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
        int released = 0;
        for (ErpDrpLine line : lines) {
            releaseLine(line.getId());
            released++;
        }
        if (released > 0) {
            advancePlanToExecutedIfComplete(planId);
        }
        return released;
    }

    private void advancePlanToExecutedIfComplete(Long planId) {
        ErpDrpPlan plan = daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId);
        if (plan == null) {
            return;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        List<ErpDrpLine> all = daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
        if (all.isEmpty()) {
            return;
        }
        for (ErpDrpLine l : all) {
            if (!Objects.equals(l.getStatus(), ErpDrpConstants.DRP_LINE_STATUS_ORDERED)
                    && !Objects.equals(l.getStatus(), ErpDrpConstants.DRP_LINE_STATUS_CANCELLED)) {
                return; // 尚有未释放/未取消的行
            }
        }
        plan.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED);
        daoProvider.daoFor(ErpDrpPlan.class).updateEntity(plan);
    }

    private ErpDrpLine requireReleasable(Long lineId) {
        if (lineId == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_NOT_SUGGESTED).param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId);
        }
        ErpDrpLine line = daoProvider.daoFor(ErpDrpLine.class).getEntityById(lineId);
        if (line == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_NOT_SUGGESTED).param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId);
        }
        if (Objects.equals(line.getStatus(), ErpDrpConstants.DRP_LINE_STATUS_ORDERED)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_ALREADY_ORDERED).param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId);
        }
        if (!Objects.equals(line.getStatus(), ErpDrpConstants.DRP_LINE_STATUS_APPROVED)) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_NOT_SUGGESTED).param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId);
        }
        return line;
    }

    private ErpDrpParameter requireParameter(ErpDrpLine line) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", line.getMaterialId()));
        q.addFilter(eq("warehouseId", line.getWarehouseId()));
        if (line.getOrgId() != null) {
            q.addFilter(eq("orgId", line.getOrgId()));
        }
        q.setLimit(1);
        List<ErpDrpParameter> list = daoProvider.daoFor(ErpDrpParameter.class).findAllByQuery(q);
        if (list.isEmpty()) {
            throw new NopException(ErpDrpErrors.ERR_DRP_PARAMETER_MISSING)
                    .param(ErpDrpErrors.ARG_MATERIAL_ID, line.getMaterialId())
                    .param(ErpDrpErrors.ARG_WAREHOUSE_ID, line.getWarehouseId());
        }
        return list.get(0);
    }

    private String releaseToTransferOrder(ErpDrpLine line, Long sourceWarehouseId, LocalDate today) {
        IEntityDao<ErpInvTransferOrder> orderDao = daoProvider.daoFor(ErpInvTransferOrder.class);
        ErpInvTransferOrder order = orderDao.newEntity();
        String code = ErpDrpConstants.RELEASE_TO_CODE_PREFIX + "TO-" + line.getId();
        order.setCode(code);
        order.setOrgId(line.getOrgId());
        order.setBusinessDate(today);
        order.setFromWarehouseId(sourceWarehouseId);
        order.setToWarehouseId(line.getWarehouseId());
        order.setDocStatus(ErpDrpConstants.DOWNSTREAM_DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpDrpConstants.DOWNSTREAM_APPROVE_STATUS_UNSUBMITTED);
        orderDao.saveEntity(order);

        IEntityDao<ErpInvTransferOrderLine> lineDao = daoProvider.daoFor(ErpInvTransferOrderLine.class);
        ErpInvTransferOrderLine toLine = lineDao.newEntity();
        toLine.setTransferId(order.getId());
        toLine.setLineNo(10);
        toLine.setMaterialId(line.getMaterialId());
        toLine.setUoMId(resolveUoM(line.getMaterialId()));
        toLine.setQuantity(nz(line.getApprovedQty()));
        lineDao.saveEntity(toLine);
        return code;
    }

    private String releaseToPurchaseOrder(ErpDrpLine line, Long supplierId, LocalDate today) {
        IEntityDao<ErpPurOrder> orderDao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder order = orderDao.newEntity();
        String code = ErpDrpConstants.RELEASE_TO_CODE_PREFIX + "PO-" + line.getId();
        order.setCode(code);
        order.setOrgId(line.getOrgId());
        order.setSupplierId(supplierId);
        order.setBusinessDate(today);
        order.setDeliveryDate(today);
        order.setCurrencyId(resolveDefaultCurrencyId());
        order.setDocStatus(ErpDrpConstants.DOWNSTREAM_DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpDrpConstants.DOWNSTREAM_APPROVE_STATUS_UNSUBMITTED);
        orderDao.saveEntity(order);

        IEntityDao<ErpPurOrderLine> lineDao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine poLine = lineDao.newEntity();
        poLine.setOrderId(order.getId());
        poLine.setLineNo(10);
        poLine.setMaterialId(line.getMaterialId());
        poLine.setUoMId(resolveUoM(line.getMaterialId()));
        poLine.setQuantity(nz(line.getApprovedQty()));
        poLine.setUnitPrice(BigDecimal.ZERO);
        poLine.setAmount(BigDecimal.ZERO);
        lineDao.saveEntity(poLine);
        return code;
    }

    private Long resolveUoM(Long materialId) {
        if (materialId == null) {
            return null;
        }
        ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
        return material != null ? material.getUoMId() : null;
    }

    /**
     * 解析默认币种：ErpDrpParameter 无币种字段，生成的采购单为草稿（单价/金额=0 待采购员补录），
     * 取首个可用币种作为占位。残留风险：采购员需复核币种。
     */
    private Long resolveDefaultCurrencyId() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.setLimit(1);
        List<ErpMdCurrency> list = daoProvider.daoFor(ErpMdCurrency.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
