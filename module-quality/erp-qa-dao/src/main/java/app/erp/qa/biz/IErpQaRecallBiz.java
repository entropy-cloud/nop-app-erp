
package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaRecall;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.Map;

/**
 * 召回事件业务接口。除标准 CRUD 外，定义召回 5 态状态机
 * （{@code docs/design/quality/recall.md §召回状态机`}）。
 *
 * <p>状态机方法（{@link BizMutation}）：
 * <ul>
 *   <li>{@link #register}：创建召回 → status=OPEN / approveStatus=UNSUBMITTED。</li>
 *   <li>{@link #submit}：OPEN + UNSUBMITTED → approveStatus=SUBMITTED（请求审批）。</li>
 *   <li>{@link #approve}：OPEN → status=APPROVED / approveStatus=APPROVED。
 *       配置 {@code erp-qua.recall-require-approval=true}（默认）时须先 submit。</li>
 *   <li>{@link #reject}：SUBMITTED → status=CANCELLED / approveStatus=REJECTED。</li>
 *   <li>{@link #cancel}：非终态（OPEN/APPROVED/IN_PROGRESS）→ status=CANCELLED。</li>
 *   <li>{@link #locateTargets}：APPROVED → 经库存批次追溯定位受影响客户/出库 → IN_PROGRESS。</li>
 *   <li>{@link #notifyCustomers}：标记目标已通知 + recall.notifyCustomer=true。</li>
 *   <li>{@link #generateReturns}：为每个目标经销售退货标准流程生成退货单。</li>
 *   <li>{@link #close}：IN_PROGRESS → CLOSED，门控 {@code erp-qua.recall-notify-required-to-close}。</li>
 * </ul>
 *
 * <p>非法迁移抛 {@code ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION}。
 */
public interface IErpQaRecallBiz extends ICrudBiz<ErpQaRecall> {

    @BizMutation
    ErpQaRecall register(@Name("data") Map<String, Object> data, IServiceContext context);

    @BizMutation
    ErpQaRecall submit(@Name("recallId") Long recallId, IServiceContext context);

    @BizMutation
    ErpQaRecall approve(@Name("recallId") Long recallId, IServiceContext context);

    @BizMutation
    ErpQaRecall reject(@Name("recallId") Long recallId, IServiceContext context);

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
