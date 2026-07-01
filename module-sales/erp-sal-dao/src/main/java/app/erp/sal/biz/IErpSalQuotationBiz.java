
package app.erp.sal.biz;

import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;

/**
 * 销售报价单业务接口。除标准 CRUD 外，定义审核/客户确认状态机 + 报价→订单转化契约（对齐 {@code docs/design/sales/quotation.md}）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前行非空）。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（纯状态迁移）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED。</li>
 *   <li>{@link #confirmCustomerAccepted}：APPROVED 且 {@code validTo ≥ today} → {@code isAccepted=true}
 *       （「客户确认」=布尔标记，设计-模型差异映射；过期抛 {@link io.nop.api.core.exceptions.NopException}）。</li>
 *   <li>{@link #convertToOrder}：APPROVED + {@code isAccepted} + 未过期 → 创建 {@link ErpSalOrder}(UNSUBMITTED/DRAFT)
 *       + 行（回链 {@code quotationId}）；幂等防重复转化。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 *
 * <p>模型边界：报价单无 {@code approvedBy}/{@code approvedAt} 列——审核仅翻转 {@code approveStatus}，
 * 不记录审核人/时间（已知缺口，不改 ORM）。
 */
public interface IErpSalQuotationBiz extends ICrudBiz<ErpSalQuotation> {

    @SingleSession
    @Transactional
    ErpSalQuotation submit(Long quotationId);

    @SingleSession
    @Transactional
    ErpSalQuotation withdrawSubmit(Long quotationId);

    @SingleSession
    @Transactional
    ErpSalQuotation approve(Long quotationId);

    @SingleSession
    @Transactional
    ErpSalQuotation reject(Long quotationId);

    @SingleSession
    @Transactional
    ErpSalQuotation reverseApprove(Long quotationId);

    @SingleSession
    @Transactional
    ErpSalQuotation cancel(Long quotationId);

    @SingleSession
    @Transactional
    ErpSalQuotation confirmCustomerAccepted(Long quotationId);

    @SingleSession
    @Transactional
    ErpSalOrder convertToOrder(Long quotationId);
}
