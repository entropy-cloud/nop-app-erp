package app.erp.inv.service.processor;

import app.erp.fin.biz.IErpFinIntercompanyTransferBiz;
import app.erp.inv.biz.IErpInvTransferOrderBiz;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import app.erp.inv.dao.entity.ErpInvTransferOrderLine;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.statemachine.ErpInvTransferOrderStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpInvTransferOrder confirm per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含确认编排：require（经 {@link IErpInvTransferOrderBiz} 实体管道）→ DRAFT 守卫（委托
 * {@link ErpInvTransferOrderStateMachine}）→ 翻 CONFIRMED →
 * A3 跨法人内部交易凭证后置钩子（config-gated，失败不阻塞库存确认）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvTransferOrderConfirmProcessor {

    @Inject
    IErpInvTransferOrderBiz transferOrderBiz;

    @Inject
    IErpFinIntercompanyTransferBiz intercompanyTransferBiz;

    @Inject
    ErpInvTransferOrderStateMachine stateMachine;

    public ErpInvTransferOrder confirm(String transferOrderId, IServiceContext context) {
        ErpInvTransferOrder order = transferOrderBiz.requireEntity(String.valueOf(transferOrderId), null, context);
        validateDraft(order, transferOrderId);
        order.setDocStatus(stateMachine.confirmTargetStatus());
        transferOrderBiz.updateEntity(order, null, context);
        dispatchIntercompanyPosting(order, context);
        return order;
    }

    protected void validateDraft(ErpInvTransferOrder order, String transferOrderId) {
        String status = order.getDocStatus();
        // 固定来源态守卫委托 StateMachine Bean（Bean 直抛领域码 ERR_ILLEGAL_STATUS_TRANSITION，本处同码补参 moveCode）。
        // 映射冲突修正（plan 2026-09-07-2200-1）：原 remap 误用 StockTake 码 ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION
        // + ARG_TAKE_ID（copy-paste 缺陷，原 Deferred successor fix），随 StateMachine 直抛领域码一并纠正为域通用码。
        try {
            stateMachine.assertCanConfirm(status);
        } catch (NopException e) {
            throw e.param(ErpInvErrors.ARG_MOVE_CODE, order.getCode());
        }
    }

    protected void dispatchIntercompanyPosting(ErpInvTransferOrder order, IServiceContext context) {
        if (intercompanyTransferBiz != null && order.getFromWarehouseId() != null
                && order.getToWarehouseId() != null && order.getBusinessDate() != null) {
            try {
                // P1-CK-fin4-003：传调拨行数量聚合（materialId → Σquantity），使凭证金额 = 单价 × 数量。
                java.util.Map<String, java.math.BigDecimal> qtyByMaterial = new java.util.HashMap<>();
                for (ErpInvTransferOrderLine line : order.getLines()) {
                    if (line.getMaterialId() == null || line.getQuantity() == null) {
                        continue;
                    }
                    qtyByMaterial.merge(line.getMaterialId(), line.getQuantity(), java.math.BigDecimal::add);
                }
                intercompanyTransferBiz.onTransferConfirmed(order.getId(), order.getFromWarehouseId(),
                        order.getToWarehouseId(), qtyByMaterial, order.getBusinessDate(), context);
            } catch (RuntimeException e) {
                // P2-CK-fin4-008（rethrow 强一致，反转「失败不阻塞」设计意图——posting.md L573/
                // inventory-intercompany 语义随批同步）：intercompany 链不走引擎无异常工作台通道，
                // 静默 warn 会致双法人账套缺凭证且调拨 DONE 后无重试入口；改为失败时调拨确认
                // 整体回滚（真实故障阻断）。转移定价缺失 ERR_TRANSFER_PRICE_NOT_FOUND 同样传播（预期）。
                throw e;
            }
        }
    }
}
