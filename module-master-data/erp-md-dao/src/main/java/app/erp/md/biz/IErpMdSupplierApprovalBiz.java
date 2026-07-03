
package app.erp.md.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.md.dao.entity.ErpMdSupplierApproval;

/**
 * 供应商准入资格（AVL）状态机契约。
 *
 * <p>状态迁移（{@code docs/design/purchase/supplier-evaluation.md §ErpMdSupplierApproval 状态机}）：
 * <ul>
 *   <li>{@link #apply} → APPLIED</li>
 *   <li>{@link #approve}：APPLIED/PROBATION → APPROVED（+ approvedBy/At）</li>
 *   <li>{@link #probate}：APPROVED → PROBATION（新供应商试用）</li>
 *   <li>{@link #suspend}：APPLIED/APPROVED/PROBATION → SUSPENDED（评分 standing=RED 跨域联动调用入口）</li>
 *   <li>{@link #reinstate}：SUSPENDED → APPROVED（需审批恢复）</li>
 *   <li>{@link #reject}：APPLIED → REJECTED</li>
 * </ul>
 * 非法迁移抛 {@code ErpMdErrors.ERR_INVALID_APPROVAL_STATUS_TRANSITION}。
 */
public interface IErpMdSupplierApprovalBiz extends ICrudBiz<ErpMdSupplierApproval> {

    @BizMutation
    ErpMdSupplierApproval apply(@Name("approvalId") Long approvalId, IServiceContext context);

    @BizMutation
    ErpMdSupplierApproval approve(@Name("approvalId") Long approvalId, IServiceContext context);

    @BizMutation
    ErpMdSupplierApproval probate(@Name("approvalId") Long approvalId, IServiceContext context);

    @BizMutation
    ErpMdSupplierApproval suspend(@Name("approvalId") Long approvalId, IServiceContext context);

    /**
     * 按供应商暂停全部有效（APPLIED/APPROVED/PROBATION）准入资格。供评分 standing=RED 跨域联动调用
     * （purchase→master-data I*Biz，{@code docs/design/purchase/supplier-evaluation.md §业务规则4}）。
     *
     * @return 被暂停的资格记录数
     */
    @BizMutation
    int suspendByPartner(@Name("partnerId") Long partnerId, IServiceContext context);

    @BizMutation
    ErpMdSupplierApproval reinstate(@Name("approvalId") Long approvalId, IServiceContext context);

    @BizMutation
    ErpMdSupplierApproval reject(@Name("approvalId") Long approvalId, IServiceContext context);

    /**
     * 按供应商加载有效的（非 REJECTED）准入资格。供 RFQ 创建校验、评分联动读取 standing。
     */
    @BizQuery
    ErpMdSupplierApproval findEffectiveByPartner(@Name("partnerId") Long partnerId, IServiceContext context);
}
