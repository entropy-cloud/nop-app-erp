
package app.erp.pur.biz;

import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurReceive;

/**
 * 采购入库单业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/purchase/state-machine.md}）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前置供应商启用 + 行非空）。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED，并触发库存入库移动（{@code IErpInvStockMoveBiz.generateMove}）。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（反审核，前置冲销已生成的入库移动单）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED（已 APPROVED 者须先冲销）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpPurReceiveBiz extends ICrudBiz<ErpPurReceive> {

    @SingleSession
    @Transactional
    ErpPurReceive submit(Long receiveId);

    @SingleSession
    @Transactional
    ErpPurReceive withdrawSubmit(Long receiveId);

    @SingleSession
    @Transactional
    ErpPurReceive approve(Long receiveId);

    @SingleSession
    @Transactional
    ErpPurReceive reject(Long receiveId);

    @SingleSession
    @Transactional
    ErpPurReceive reverseApprove(Long receiveId);

    @SingleSession
    @Transactional
    ErpPurReceive cancel(Long receiveId);
}
