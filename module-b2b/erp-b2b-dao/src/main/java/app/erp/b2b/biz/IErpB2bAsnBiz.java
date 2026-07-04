
package app.erp.b2b.biz;

import app.erp.b2b.dao.entity.ErpB2bAsn;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;
import java.util.List;

/**
 * ASN 入站处理 Biz 接口。除标准 CRUD 外，定义 ASN 入站全流程契约（对齐 {@code asn-processing.md}）：
 *
 * <ul>
 *   <li>{@link #handleInboundWebhook}：webhook 入站端点（HMAC 校验 + 幂等 → parse → 建 ASN）。</li>
 *   <li>{@link #matchPurchaseOrder}：ASN→采购订单匹配（RECEIVED→MATCHED）。</li>
 *   <li>{@link #createReceiveFromAsn}：config-gated 创建采购入库草稿（MATCHED→RECEIVED_TO_STOCK）。</li>
 *   <li>{@link #retryMatch}：手动重试匹配（幂等）。</li>
 *   <li>{@link #findUnmatchedAsns}：查询未匹配 ASN（超时提示）。</li>
 * </ul>
 */
public interface IErpB2bAsnBiz extends ICrudBiz<ErpB2bAsn> {

    @BizMutation
    Long handleInboundWebhook(@Name("formatCode") String formatCode,
                              @Name("partnerCode") String partnerCode,
                              @Name("signature") String signature,
                              @Name("eventId") String eventId,
                              @Name("payload") String payload,
                              IServiceContext context);

    @BizMutation
    ErpB2bAsn matchPurchaseOrder(@Name("asnId") Long asnId, IServiceContext context);

    @BizMutation
    ErpB2bAsn createReceiveFromAsn(@Name("asnId") Long asnId, IServiceContext context);

    @BizMutation
    ErpB2bAsn retryMatch(@Name("asnId") Long asnId, IServiceContext context);

    @BizQuery
    List<ErpB2bAsn> findUnmatchedAsns(@Name("asOfDate") LocalDate asOfDate, IServiceContext context);
}
