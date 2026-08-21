
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.contract.dao.entity.ErpCtApprovalRecord;

/**
 * 审批记录业务接口（RC-R1.34，P1-RC-077，UC-CT-07）。
 *
 * <p>除标准 CRUD 外，定义审批工作流编排契约（对齐 {@code docs/design/contract/approval-workflow.md}）：
 * <ul>
 *   <li>{@link #approve}：PENDING→APPROVED（记录审批人守卫 + D3 超限锁定守卫）→ 激活下一 WAITING 节点；</li>
 *   <li>{@link #reject}：PENDING→REJECTED（守卫同 approve）+ 通知经办人 + D3 超限锁定（派生计数 ≥ max-retries → 后续操作拒绝 + 强制升级通知）；</li>
 *   <li>{@link #resubmit}（D7）：合同 NEGOTIATION → 以最新被驳回节点为界，对驳回节点及其后续节点
 *       追加新 ApprovalRecord 行（首 PENDING 其余 WAITING），已 APPROVED 节点保持不动。</li>
 * </ul>
 *
 * <p>守卫仅作用于链记录（approvalMatrixId != null）；terminate 法务记录
 * （approvalMatrixId=null）经 {@code IErpCtContractBiz#approveTermination/rejectTermination}
 * 独立操作（D1 选项 B，双轨入口区分）。
 */
public interface IErpCtApprovalRecordBiz extends ICrudBiz<ErpCtApprovalRecord>{

    @BizMutation
    ErpCtApprovalRecord approve(@Name("recordId") String recordId,
                                @Optional @Name("comment") String comment,
                                IServiceContext context);

    @BizMutation
    ErpCtApprovalRecord reject(@Name("recordId") String recordId,
                               @Optional @Name("comment") String comment,
                               IServiceContext context);

    @BizMutation
    int resubmit(@Name("contractId") String contractId, IServiceContext context);
}
