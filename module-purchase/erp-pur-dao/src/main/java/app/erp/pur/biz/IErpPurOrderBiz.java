
package app.erp.pur.biz;

import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurOrder;

/**
 * 采购订单业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/purchase/state-machine.md} §2
 * 「采购订单｜仅状态推进」，订单审核不直接触发库存/凭证——下游单据才触发）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前置供应商启用 + 行非空）。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED，仅状态推进（不触发库存/凭证）。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（反审核目标态 REJECTED，对齐 §3/§11.4）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpPurOrderBiz extends ICrudBiz<ErpPurOrder> {

    @SingleSession
    @Transactional
    ErpPurOrder submit(Long orderId);

    @SingleSession
    @Transactional
    ErpPurOrder withdrawSubmit(Long orderId);

    @SingleSession
    @Transactional
    ErpPurOrder approve(Long orderId);

    @SingleSession
    @Transactional
    ErpPurOrder reject(Long orderId);

    @SingleSession
    @Transactional
    ErpPurOrder reverseApprove(Long orderId);

    @SingleSession
    @Transactional
    ErpPurOrder cancel(Long orderId);
}
