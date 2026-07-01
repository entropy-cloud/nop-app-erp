
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurRequisition;

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

    @BizMutation
    ErpPurRequisition submit(@Name("requisitionId") Long requisitionId, IServiceContext context);

    @BizMutation
    ErpPurRequisition withdrawSubmit(@Name("requisitionId") Long requisitionId, IServiceContext context);

    @BizMutation
    ErpPurRequisition approve(@Name("requisitionId") Long requisitionId, IServiceContext context);

    @BizMutation
    ErpPurRequisition reject(@Name("requisitionId") Long requisitionId, IServiceContext context);

    @BizMutation
    ErpPurRequisition reverseApprove(@Name("requisitionId") Long requisitionId, IServiceContext context);

    @BizMutation
    ErpPurRequisition cancel(@Name("requisitionId") Long requisitionId, IServiceContext context);

    /**
     * 将 APPROVED 请购单转化为采购订单。入口在请购侧（请购 APPROVED → 派生订单）。
     *
     * @param requisitionId 请购单 ID（须为 APPROVED）
     * @param request       调用方补充字段（仓库/币种/每行单价等）
     * @return 新建的采购订单（approveStatus=UNSUBMITTED, docStatus=DRAFT，回链 requisitionId）
     */
    @BizMutation
    ErpPurOrder convertToOrder(@Name("requisitionId") Long requisitionId,
                               @Name("request") ConvertToOrderRequest request,
                               IServiceContext context);
}
