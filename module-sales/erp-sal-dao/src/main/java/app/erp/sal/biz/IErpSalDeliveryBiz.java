
package app.erp.sal.biz;

import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalDelivery;

/**
 * 销售出库单业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/sales/state-machine.md}，
 * 与采购域镜像对称）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前置客户启用 + 行非空）。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED，并触发库存出库移动（{@code IErpInvStockMoveBiz.generateMove}，
 *       出库类在库存域 CONFIRM 校验可用量，不足抛 {@link io.nop.api.core.exceptions.NopException} 致整个审核回滚）。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（反审核，前置冲销已生成的出库移动单）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED（已 APPROVED 者须先冲销）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpSalDeliveryBiz extends ICrudBiz<ErpSalDelivery> {

    @SingleSession
    @Transactional
    ErpSalDelivery submit(Long deliveryId);

    @SingleSession
    @Transactional
    ErpSalDelivery withdrawSubmit(Long deliveryId);

    @SingleSession
    @Transactional
    ErpSalDelivery approve(Long deliveryId);

    @SingleSession
    @Transactional
    ErpSalDelivery reject(Long deliveryId);

    @SingleSession
    @Transactional
    ErpSalDelivery reverseApprove(Long deliveryId);

    @SingleSession
    @Transactional
    ErpSalDelivery cancel(Long deliveryId);
}
