
package app.erp.b2b.service.entity;

import app.erp.b2b.biz.IErpB2bAsnBiz;
import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.processor.ErpB2bAsnCreateReceiveFromAsnProcessor;
import app.erp.b2b.service.processor.ErpB2bAsnHandleInboundWebhookProcessor;
import app.erp.b2b.service.processor.ErpB2bAsnMatchPurchaseOrderProcessor;
import app.erp.b2b.service.processor.ErpB2bAsnRetryMatchProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * ASN 入站处理聚合根 Biz。承载 ASN 入站全流程（{@code asn-processing.md}）：
 *
 * <p>webhook 入站（{@link #handleInboundWebhook}）→ HMAC 校验 + 幂等 → 解析报文
 * → 建 ASN/AsnLine → 采购订单匹配（{@link #matchPurchaseOrder}，RECEIVED→MATCHED）
 * → config-gated 创建入库草稿（{@link #createReceiveFromAsn}，MATCHED→RECEIVED_TO_STOCK）。
 *
 * <p><b>核心零污染</b>：不在 {@code ErpPurReceive} 加 asnId 列，仅弱指针 orderId（→PO→ASN 经 relatedBillCode）。
 *
 * <p>编排已按 R6.7 每 mutation 一 Processor 拆分（Cat-B 自包含）。BizModel 仅保留 @BizMutation/@BizQuery 入口与单行委托。
 */
@BizModel("ErpB2bAsn")
public class ErpB2bAsnBizModel extends CrudBizModel<ErpB2bAsn> implements IErpB2bAsnBiz {

    @Inject
    ErpB2bAsnHandleInboundWebhookProcessor handleInboundWebhookProcessor;
    @Inject
    ErpB2bAsnMatchPurchaseOrderProcessor matchPurchaseOrderProcessor;
    @Inject
    ErpB2bAsnCreateReceiveFromAsnProcessor createReceiveFromAsnProcessor;
    @Inject
    ErpB2bAsnRetryMatchProcessor retryMatchProcessor;

    public ErpB2bAsnBizModel() {
        setEntityName(ErpB2bAsn.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpB2bAsn> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpB2bAsn entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public Long handleInboundWebhook(@Name("formatCode") String formatCode,
                                     @Name("partnerCode") String partnerCode,
                                     @Name("signature") String signature,
                                     @Name("eventId") String eventId,
                                     @Name("payload") String payload,
                                     IServiceContext context) {
        return handleInboundWebhookProcessor.handleInboundWebhook(formatCode, partnerCode, signature, eventId, payload, context);
    }

    @Override
    @BizMutation
    public ErpB2bAsn matchPurchaseOrder(@Name("asnId") Long asnId, IServiceContext context) {
        return matchPurchaseOrderProcessor.matchPurchaseOrder(asnId, context);
    }

    @Override
    @BizMutation
    public ErpB2bAsn createReceiveFromAsn(@Name("asnId") Long asnId, IServiceContext context) {
        return createReceiveFromAsnProcessor.createReceiveFromAsn(asnId, context);
    }

    @Override
    @BizMutation
    public ErpB2bAsn retryMatch(@Name("asnId") Long asnId, IServiceContext context) {
        return retryMatchProcessor.retryMatch(asnId, context);
    }

    @Override
    @BizQuery
    public List<ErpB2bAsn> findUnmatchedAsns(@Name("asOfDate") LocalDate asOfDate, IServiceContext context) {
        IEntityDao<ErpB2bAsn> dao = daoProvider().daoFor(ErpB2bAsn.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpB2bConstants.ASN_STATUS_RECEIVED));
        if (asOfDate != null) {
            q.addFilter(le("estimatedArrivalDate", asOfDate));
        }
        q.setLimit(200);
        return dao.findAllByQuery(q);
    }
}
