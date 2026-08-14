package app.erp.inv.service.processor;

import app.erp.fin.biz.IErpFinIntercompanyTransferBiz;
import app.erp.inv.biz.IErpInvTransferOrderBiz;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
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

    public ErpInvTransferOrder confirm(Long transferOrderId, IServiceContext context) {
        ErpInvTransferOrder order = transferOrderBiz.requireEntity(String.valueOf(transferOrderId), null, context);
        validateDraft(order, transferOrderId);
        order.setDocStatus(stateMachine.confirmTargetStatus());
        transferOrderBiz.updateEntity(order, null, context);
        dispatchIntercompanyPosting(order, context);
        return order;
    }

    protected void validateDraft(ErpInvTransferOrder order, Long transferOrderId) {
        String status = order.getDocStatus();
        // 固定来源态守卫委托 StateMachine Bean（非法边 Bean 抛 common 层码，映射为领域码 + common 作 cause）
        try {
            stateMachine.assertCanConfirm(status);
        } catch (NopException e) {
            // 行为保持：既有错误码缺陷（copy-paste 抛 StockTake 的 ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION +
            // ARG_TAKE_ID）按路线图 Non-Goal「不借迁移改变既有错误码」原样保留，successor Fix 见计划
            // Deferred But Adjudicated「TransferOrder 错误码缺陷修正」。
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION, e)
                    .param(ErpInvErrors.ARG_TAKE_ID, transferOrderId)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status);
        }
    }

    protected void dispatchIntercompanyPosting(ErpInvTransferOrder order, IServiceContext context) {
        if (intercompanyTransferBiz != null && order.getFromWarehouseId() != null
                && order.getToWarehouseId() != null && order.getBusinessDate() != null) {
            try {
                intercompanyTransferBiz.onTransferConfirmed(order.getId(), order.getFromWarehouseId(),
                        order.getToWarehouseId(), order.getBusinessDate(), context);
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(ErpInvTransferOrderConfirmProcessor.class)
                        .warn("intercompany posting failed for transfer {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}
