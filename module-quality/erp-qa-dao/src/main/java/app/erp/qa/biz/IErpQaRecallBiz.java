
package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaRecall;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import java.util.Map;

/**
 * 召回事件业务接口。除标准 CRUD 外，定义召回 5 态状态机
 * （{@code docs/design/quality/recall.md §召回状态机`}）。
 *
 * <p>标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）由 {@link IApprovableBiz}
 * 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供；recall.status 联动（approve→APPROVED、
 * reject→CANCELLED）经 {@code ErpQaRecall.xbiz} 的 {@code <source>} 内联。
 *
 * <p>状态机方法（{@link BizMutation}）：
 * <ul>
 *   <li>{@link #register}：创建召回 → status=OPEN / approveStatus=UNSUBMITTED。</li>
 *   <li>{@code submitForApproval}（{@link IApprovableBiz}）：approveStatus UNSUBMITTED→SUBMITTED（须 status=OPEN）。</li>
 *   <li>{@code approve}（{@link IApprovableBiz}）：approveStatus SUBMITTED→APPROVED；status OPEN→APPROVED。</li>
 *   <li>{@code reject}（{@link IApprovableBiz}）：approveStatus SUBMITTED→REJECTED；status→CANCELLED。</li>
 *   <li>{@link #cancel}：非终态（OPEN/APPROVED/IN_PROGRESS）→ status=CANCELLED。</li>
 *   <li>{@link #locateTargets}：APPROVED → 经库存批次追溯定位受影响客户/出库 → IN_PROGRESS。</li>
 *   <li>{@link #notifyCustomers}：标记目标已通知 + recall.notifyCustomer=true。</li>
 *   <li>{@link #generateReturns}：为每个目标经销售退货标准流程生成退货单。</li>
 *   <li>{@link #close}：IN_PROGRESS → CLOSED，门控 {@code erp-qua.recall-notify-required-to-close}。</li>
 * </ul>
 *
 * <p>非法迁移抛 {@code ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION}。
 */
public interface IErpQaRecallBiz extends ICrudBiz<ErpQaRecall>, IApprovableBiz<ErpQaRecall> {

    @BizMutation
    ErpQaRecall register(@Name("data") Map<String, Object> data, IServiceContext context);

    @BizMutation
    ErpQaRecall cancel(@Name("recallId") Long recallId, IServiceContext context);

    @BizMutation
    ErpQaRecall locateTargets(@Name("recallId") Long recallId, IServiceContext context);

    @BizMutation
    ErpQaRecall notifyCustomers(@Name("recallId") Long recallId, IServiceContext context);

    @BizMutation
    ErpQaRecall generateReturns(@Name("recallId") Long recallId, IServiceContext context);

    @BizMutation
    ErpQaRecall close(@Name("recallId") Long recallId, IServiceContext context);
}
