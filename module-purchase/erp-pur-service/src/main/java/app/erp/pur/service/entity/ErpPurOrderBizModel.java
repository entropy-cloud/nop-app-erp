
package app.erp.pur.service.entity;

import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.processor.ErpPurOrderProcessor;
import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购订单 BizModel（聚合根 Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 三轴审批状态机编排委托 {@link ErpPurOrderProcessor}（protected step 方法，下游可逐 step 覆盖）；
 * 跨聚合写契约（请购→订单转化、收货进度回写、防重查询）留 Facade。
 *
 * <p>订单审核 = 纯状态推进，不触发库存/凭证（下游单据才触发）——与入库单审核触发
 * {@code generateMove} 实质性不同。
 */
@BizModel("ErpPurOrder")
public class ErpPurOrderBizModel extends CrudBizModel<ErpPurOrder> implements IErpPurOrderBiz {

    @Inject
    RequisitionToOrderConverter converter;

    @Inject
    ErpPurOrderProcessor orderProcessor;

    public ErpPurOrderBizModel() {
        setEntityName(ErpPurOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurOrder submit(@Name("orderId") Long orderId, IServiceContext context) {
        return orderProcessor.submit(orderId, context);
    }

    @Override
    @BizMutation
    public ErpPurOrder withdrawSubmit(@Name("orderId") Long orderId, IServiceContext context) {
        return orderProcessor.withdrawSubmit(orderId, context);
    }

    @Override
    @BizMutation
    public ErpPurOrder approve(@Name("orderId") Long orderId, IServiceContext context) {
        return orderProcessor.approve(orderId, context);
    }

    @Override
    @BizMutation
    public ErpPurOrder reject(@Name("orderId") Long orderId, IServiceContext context) {
        return orderProcessor.reject(orderId, context);
    }

    @Override
    @BizMutation
    public ErpPurOrder reverseApprove(@Name("orderId") Long orderId, IServiceContext context) {
        return orderProcessor.reverseApprove(orderId, context);
    }

    @Override
    @BizMutation
    public ErpPurOrder cancel(@Name("orderId") Long orderId, IServiceContext context) {
        return orderProcessor.cancel(orderId, context);
    }

    // ---------- 跨聚合写契约（请购→订单转化、收货进度回写） ----------

    @Override
    @BizAction
    public ErpPurOrder createFromRequisition(@Name("requisition") ErpPurRequisition requisition,
                                             @Name("lines") List<ErpPurRequisitionLine> lines,
                                             @Name("supplierId") Long supplierId,
                                             @Name("request") ConvertToOrderRequest request,
                                             IServiceContext context) {
        ErpPurOrder order = converter.build(requisition, lines, supplierId, request);
        dao().saveEntity(order);
        for (ErpPurOrderLine orderLine : converter.buildLines(order, lines, request)) {
            daoFor(ErpPurOrderLine.class).saveEntity(orderLine);
        }
        return order;
    }

    @Override
    @BizAction
    public boolean existsActiveByRequisition(@Name("requisitionId") Long requisitionId, IServiceContext context) {
        if (requisitionId == null) {
            return false;
        }
        // docStatus 的 xmeta 仅允许 eq/in 过滤（不支持 ne），故按 requisitionId 经管道 findList 后于内存剔除已作废单。
        QueryBean q = new QueryBean();
        q.addFilter(eq("requisitionId", requisitionId));
        for (ErpPurOrder order : findList(q, null, context)) {
            if (!Objects.equals(order.getDocStatus(), ErpPurConstants.DOC_STATUS_CANCELLED)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @BizAction
    public void updateReceiveStatus(@Name("orderId") Long orderId,
                                    @Name("receiveStatus") String receiveStatus,
                                    IServiceContext context) {
        if (orderId == null) {
            return;
        }
        ErpPurOrder order = get(String.valueOf(orderId), true, context);
        if (order == null) {
            return;
        }
        order.setReceiveStatus(receiveStatus);
        dao().updateEntity(order);
    }
}
