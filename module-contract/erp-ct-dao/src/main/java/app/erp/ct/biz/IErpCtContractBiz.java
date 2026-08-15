
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.contract.dao.entity.ErpCtContract;

import java.util.List;

/**
 * 合同头业务接口。除标准 CRUD 外，定义合同全生命周期状态机契约
 * （对齐 {@code docs/design/contract/state-machine.md}）：
 *
 * <ul>
 *   <li>{@link #submit}：DRAFT → NEGOTIATION（提交谈判，零版本自动创建 v1，已有版本保留既有 DRAFT 当前版本不动）。</li>
 *   <li>{@link #activate}：NEGOTIATION → ACTIVE（前置 contractType↔contractDirection 组合合法、当前版本定稿或同步签署）。</li>
 *   <li>{@link #suspend}：ACTIVE → SUSPENDED。</li>
 *   <li>{@link #resume}：SUSPENDED → ACTIVE。</li>
 *   <li>{@link #terminate}：两段化（RC-R1.34，P1-RC-076）——发起终止申请（reason/attachment 落
 *       法务审批记录 remark），合同保持原状态；法务经 {@link #approveTermination} 通过后执行终止操作
 *       （TERMINATED + 版本归档 + InvoicePlan 截停 + 善后通知）；{@link #rejectTermination} 驳回 →
 *       合同保持原状态。</li>
 *   <li>{@link #expire}：ACTIVE → EXPIRED（终态）。</li>
 *   <li>{@link #amend}：ACTIVE → DRAFT 修订，新建版本（versionNo 递增，原子翻转 isCurrent）。</li>
 *   <li>{@link #rejectAmend}：DRAFT → ACTIVE 驳回恢复（恢复前任 current 版本 isCurrent=true——D5 选项 B：
 *       优先 SIGNED 最大 versionNo，无 SIGNED 回落 FINALIZED 最大者）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpCtContractBiz extends ICrudBiz<ErpCtContract> {

    @BizMutation
    ErpCtContract submit(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract activate(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract suspend(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract resume(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract terminate(@Name("contractId") Long contractId,
                            @Optional @Name("reason") String reason,
                            @Optional @Name("attachmentId") Long attachmentId,
                            IServiceContext context);

    /**
     * 法务通过终止申请（RC-R1.34，P1-RC-076）：守卫记录 PENDING + 终止记录
     * （approvalMatrixId=null）+ 审批人匹配；通过后执行终止操作（合同→TERMINATED +
     * 当前版本 isCurrent=false 归档 + 未执行 InvoicePlan 逻辑删除截停 + 善后 TODO 通知）。
     */
    @BizMutation
    ErpCtContract approveTermination(@Name("recordId") Long recordId,
                                     @Optional @Name("comment") String comment,
                                     IServiceContext context);

    /**
     * 法务驳回终止申请（RC-R1.34，P1-RC-076 异常路径）：记录 → REJECTED + 通知经办人，
     * 合同保持原状态（ACTIVE/NEGOTIATION）。
     */
    @BizMutation
    ErpCtContract rejectTermination(@Name("recordId") Long recordId,
                                    @Optional @Name("comment") String comment,
                                    IServiceContext context);

    @BizMutation
    ErpCtContract expire(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract amend(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract rejectAmend(@Name("contractId") Long contractId, IServiceContext context);

    /**
     * 扫描到期预警合同（UC-CT-05，RC-R1.35）：status=ACTIVE 且 endDate 在 [today, today+warningDays] 区间。
     *
     * @param warningDays 预警提前天数；null 取 config erp-ct.contract-expiry-warning-days-30（默认 30）
     */
    @BizQuery
    List<ErpCtContract> scanExpiringContracts(@Optional @Name("warningDays") Integer warningDays,
                                              IServiceContext context);

    /**
     * 批量推进已过期合同（UC-CT-05，RC-R1.35）：status=ACTIVE 且 endDate &lt; today。
     * 逐合同（失败隔离，单条 WARN 不阻断）：① D3 异常路径——到期前存在 isInvoiced=false 且
     * planDate ≤ today 的 InvoicePlan 时先经 triggerInvoice 完成开票（逐条失败隔离）；
     * ② D4 续期草稿——config erp-ct.auto-create-renewal-draft（默认 false）时创建续期草稿
     * （parentContractId 关联原合同，DRAFT，幂等守卫防重复）；③ 置 EXPIRED（stateMachine 守卫）。
     */
    @BizMutation
    @SingleSession
    List<ErpCtContract> expireOverdueContracts(IServiceContext context);
}
