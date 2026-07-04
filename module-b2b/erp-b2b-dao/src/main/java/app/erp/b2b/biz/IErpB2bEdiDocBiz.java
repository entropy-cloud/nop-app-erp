
package app.erp.b2b.biz;

import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * EDI 事务信封 Biz 接口。除标准 CRUD 外，定义 EDI 信封状态机契约（对齐 {@code edi-formats.md §七}）：
 *
 * <ul>
 *   <li>{@link #createOutbound}：→TO_SEND（按 Registry 派发 Provider.generatePayload）。</li>
 *   <li>{@link #markSent}：TO_SEND→SENT（写 sentAt，经 TransportManager 回填）。</li>
 *   <li>{@link #markAcknowledged}：SENT→ACKNOWLEDGED（终态）。</li>
 *   <li>{@link #markError}：任意→ERROR（retryCount 不变）。</li>
 *   <li>{@link #retry}：ERROR→TO_SEND（retryCount++）。</li>
 *   <li>{@link #cancel}：TO_SEND/SENT→CANCELLED（终态）。</li>
 *   <li>{@link #createInbound}：→RECEIVED（入站报文接收）。</li>
 *   <li>{@link #archive}：RECEIVED→ARCHIVED（终态，入站处理完成）。</li>
 * </ul>
 *
 * <p>每次迁移写 {@code ErpB2bEdiLog}（动作语义编码到 direction+resultCode+resultMsg，不新增列）。
 * {@code UNIQUE(formatId,relatedBillType,relatedBillCode)} 守门防重。
 */
public interface IErpB2bEdiDocBiz extends ICrudBiz<ErpB2bEdiDoc> {

    @BizMutation
    ErpB2bEdiDoc createOutbound(@Name("relatedBillType") String relatedBillType,
                                @Name("relatedBillCode") String relatedBillCode,
                                IServiceContext context);

    @BizMutation
    ErpB2bEdiDoc markSent(@Name("ediDocId") Long ediDocId, IServiceContext context);

    @BizMutation
    ErpB2bEdiDoc markAcknowledged(@Name("ediDocId") Long ediDocId, IServiceContext context);

    @BizMutation
    ErpB2bEdiDoc markError(@Name("ediDocId") Long ediDocId,
                           @Name("error") String error,
                           IServiceContext context);

    @BizMutation
    ErpB2bEdiDoc retry(@Name("ediDocId") Long ediDocId, IServiceContext context);

    @BizMutation
    ErpB2bEdiDoc cancel(@Name("ediDocId") Long ediDocId, IServiceContext context);

    @BizMutation
    ErpB2bEdiDoc createInbound(@Name("relatedBillType") String relatedBillType,
                               @Name("relatedBillCode") String relatedBillCode,
                               @Name("rawPayload") String rawPayload,
                               @Name("formatCode") String formatCode,
                               IServiceContext context);

    @BizMutation
    ErpB2bEdiDoc archive(@Name("ediDocId") Long ediDocId, IServiceContext context);
}
