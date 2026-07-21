
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import app.erp.fin.biz.IErpFinIntercompanyTransferBiz;
import app.erp.inv.biz.IErpInvTransferOrderBiz;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;

import java.util.Objects;

@BizModel("ErpInvTransferOrder")
public class ErpInvTransferOrderBizModel extends CrudBizModel<ErpInvTransferOrder> implements IErpInvTransferOrderBiz {
    public ErpInvTransferOrderBizModel(){
        setEntityName(ErpInvTransferOrder.class.getName());
    }

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpFinIntercompanyTransferBiz intercompanyTransferBiz;

    @Override
    @BizMutation
    public ErpInvTransferOrder confirm(@Name("transferOrderId") Long transferOrderId, IServiceContext context) {
        ErpInvTransferOrder order = requireEntity(String.valueOf(transferOrderId), null, context);
        if (!Objects.equals(order.getDocStatus(), ErpInvConstants.DOC_STATUS_DRAFT)) {
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION)
                    .param(ErpInvErrors.ARG_TAKE_ID, transferOrderId)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, order.getDocStatus());
        }
        order.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
        updateEntity(order, null, context);

        // A3 跨法人内部交易凭证后置钩子（plan 2026-07-22-1000-1，multi-company.md §跨公司交易生命周期状态机）。
        // config-gated（erp-fin.intercompany-posting-enabled 默认 false）：关闭时 SPI 返回空列表，既有行为完全不变。
        // 同法人调拨 SPI 内部判定后跳过；仅跨法人时经转移定价生成配对内部销售/采购凭证。
        if (intercompanyTransferBiz != null && order.getFromWarehouseId() != null
                && order.getToWarehouseId() != null && order.getBusinessDate() != null) {
            try {
                intercompanyTransferBiz.onTransferConfirmed(order.getId(), order.getFromWarehouseId(),
                        order.getToWarehouseId(), order.getBusinessDate(), context);
            } catch (RuntimeException e) {
                // 不阻塞库存确认（凭证生成失败由 finance 异常工作台兜底，保持库存移动与凭证解耦）
                org.slf4j.LoggerFactory.getLogger(ErpInvTransferOrderBizModel.class)
                        .warn("intercompany posting failed for transfer {}: {}", order.getId(), e.getMessage());
            }
        }
        return order;
    }
}
