
package app.erp.sal.biz;

import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalOrder;

/**
 * 销售订单业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/sales/state-machine.md} §2
 * 「销售订单｜仅状态推进」，不触发库存/凭证）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前置客户启用 + 行非空）。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED，**仅状态推进**（不触发库存/凭证）+ 客户启用校验 +
 *       客户信用额度校验（按 {@code erp-sal.credit-check-level}：SOFT_WARNING 放行 / HARD_BLOCK 拒绝）。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（反审核，纯状态迁移——订单审核未触发库存，无需冲销）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED（无库存冲销前置——订单审核未触发下游）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpSalOrderBiz extends ICrudBiz<ErpSalOrder> {

    @SingleSession
    @Transactional
    ErpSalOrder submit(Long orderId);

    @SingleSession
    @Transactional
    ErpSalOrder withdrawSubmit(Long orderId);

    @SingleSession
    @Transactional
    ErpSalOrder approve(Long orderId);

    @SingleSession
    @Transactional
    ErpSalOrder reject(Long orderId);

    @SingleSession
    @Transactional
    ErpSalOrder reverseApprove(Long orderId);

    @SingleSession
    @Transactional
    ErpSalOrder cancel(Long orderId);
}
