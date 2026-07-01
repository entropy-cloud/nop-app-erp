
package app.erp.pur.biz;

import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurOrder;

/**
 * 采购请购单业务接口。除标准 CRUD 外，定义三轴审批状态机 + 请购→订单转化契约
 * （对齐 {@code docs/design/purchase/requisition.md} + {@code docs/design/purchase/state-machine.md}）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前置行非空）。请购头无供应商，不做供应商校验。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED，仅状态推进（请购无自动下游触发，转化是显式独立动作）。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（反审核目标态 REJECTED，对齐 §3/§11.4）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED。</li>
 *   <li>{@link #convertToOrder}：APPROVED 请购 + 调用方补充字段 → 创建 {@link ErpPurOrder}(UNSUBMITTED/DRAFT) + 行，
 *       回链 {@code order.requisitionId}，幂等防重复转化。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpPurRequisitionBiz extends ICrudBiz<ErpPurRequisition> {

    @SingleSession
    @Transactional
    ErpPurRequisition submit(Long requisitionId);

    @SingleSession
    @Transactional
    ErpPurRequisition withdrawSubmit(Long requisitionId);

    @SingleSession
    @Transactional
    ErpPurRequisition approve(Long requisitionId);

    @SingleSession
    @Transactional
    ErpPurRequisition reject(Long requisitionId);

    @SingleSession
    @Transactional
    ErpPurRequisition reverseApprove(Long requisitionId);

    @SingleSession
    @Transactional
    ErpPurRequisition cancel(Long requisitionId);

    /**
     * 将 APPROVED 请购单转化为采购订单。
     *
     * <p>转化是请购的生命周期动作（请购 APPROVED → 派生订单），入口在请购侧。内部组装 {@link ErpPurOrder} + 行后持久化。
     *
     * @param requisitionId 请购单 ID（须为 APPROVED）
     * @param request       调用方补充字段（仓库/币种/每行单价等，请购头/行无这些字段）
     * @return 新建的采购订单（approveStatus=UNSUBMITTED, docStatus=DRAFT，回链 requisitionId）
     */
    @SingleSession
    @Transactional
    ErpPurOrder convertToOrder(Long requisitionId, ConvertToOrderRequest request);
}
