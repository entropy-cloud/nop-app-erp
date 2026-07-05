
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalOrderBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.dao.entity.ErpSalQuotationLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.processor.ErpSalOrderProcessor;
import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售订单 BizModel（聚合根 Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalOrderProcessor#onSubmit}/{@link ErpSalOrderProcessor#onApproved}；
 * 跨聚合写契约（报价→订单转化、发货进度回写、防重查询）留 Facade。
 */
@BizModel("ErpSalOrder")
public class ErpSalOrderBizModel extends CrudBizModel<ErpSalOrder> implements IErpSalOrderBiz {

    @Inject
    QuotationToOrderConverter quotationToOrderConverter;

    @Inject
    ErpSalOrderProcessor orderProcessor;

    public ErpSalOrderBizModel() {
        setEntityName(ErpSalOrder.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalOrder cancel(@Name("orderId") Long orderId, IServiceContext context) {
        return orderProcessor.cancel(String.valueOf(orderId), context);
    }

    // ---------- 跨聚合写契约（报价→订单转化、发货进度回写） ----------

    @Override
    @BizAction
    public ErpSalOrder createFromQuotation(@Name("quotation") ErpSalQuotation quotation,
                                           @Name("lines") List<ErpSalQuotationLine> lines,
                                           IServiceContext context) {
        ErpSalOrder order = quotationToOrderConverter.build(quotation, lines);
        if (order.getCode() == null) {
            order.setCode("SO-FROM-Q-" + StringHelper.generateUUID());
        }
        dao().saveEntity(order);
        IEntityDao<ErpSalOrderLine> lineDao = daoFor(ErpSalOrderLine.class);
        for (ErpSalOrderLine line : order.getLines() == null ? new ArrayList<ErpSalOrderLine>() : order.getLines()) {
            line.setOrderId(order.getId());
            lineDao.saveEntity(line);
        }
        return order;
    }

    @Override
    @BizAction
    public boolean existsActiveByQuotation(@Name("quotationId") Long quotationId, IServiceContext context) {
        if (quotationId == null) {
            return false;
        }
        // docStatus 的 xmeta 仅允许 eq/in 过滤（不支持 ne），故按 quotationId 经管道 findList 后于内存剔除已作废单。
        QueryBean q = new QueryBean();
        q.addFilter(eq("quotationId", quotationId));
        for (ErpSalOrder order : findList(q, null, context)) {
            if (!Objects.equals(ErpSalConstants.DOC_STATUS_CANCELLED, order.getDocStatus())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @BizAction
    public void updateDeliveryStatus(@Name("orderId") Long orderId,
                                     @Name("deliveryStatus") String deliveryStatus,
                                     IServiceContext context) {
        if (orderId == null) {
            return;
        }
        ErpSalOrder order = get(String.valueOf(orderId), true, context);
        if (order == null) {
            return;
        }
        order.setDeliveryStatus(deliveryStatus);
        dao().updateEntity(order);
    }
}
