
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;

/**
 * 领料单业务接口。除标准 CRUD 外，定义领料确认→出库移动单 + 材料成本回写契约。
 *
 * <p>标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）由 {@link IApprovableBiz}
 * 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 *
 * <p>领料确认（{@link #confirm}）：issue-status DRAFT→CONFIRMED→DONE；DONE 时调
 * {@code IErpInvStockMoveBiz.generateMove}（出库方向、moveType=MANUFACTURING、relatedBillType=ERP_MFG_ISSUE），
 * 业务联动自动推进移动单至 DONE 并扣减库存；回写 {@code ErpMfgWorkOrderLine.actualQuantity}，
 * 汇总领料出库流水 {@code ErpInvStockLedger.totalCost} → {@code ErpMfgWorkOrder.materialCost}。
 *
 * <p>领料红冲（{@link #reverseConfirm}，plan 2026-07-18-1745-2）：守卫 posted=true + DONE 态 →
 * 红冲 MANUFACTURING_ISSUE 凭证 + 反向 OUTGOING 移动单（生成 REVERSAL 反向移动单）→ 翻 posted=false / docStatus=CANCELLED。
 *
 * <p>幂等键 {@code (ERP_MFG_ISSUE, issue.code)} 由 {@code generateMove} 防重复触发。
 * 权威：{@code docs/design/manufacturing/state-machine.md}、{@code docs/design/inventory/cross-domain.md}、
 * {@code docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md} Phase 3。
 */
public interface IErpMfgMaterialIssueBiz extends ICrudBiz<ErpMfgMaterialIssue>, IApprovableBiz<ErpMfgMaterialIssue> {

    @BizMutation
    ErpMfgMaterialIssue confirm(@Name("issueId") Long issueId, IServiceContext context);

    @BizMutation
    ErpMfgMaterialIssue reverseConfirm(@Name("issueId") Long issueId, IServiceContext context);
}
