
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;

/**
 * 领料单业务接口。除标准 CRUD 外，定义领料确认→出库移动单 + 材料成本回写契约。
 *
 * <p>领料确认（{@link #confirm}）：issue-status DRAFT→CONFIRMED→DONE；DONE 时调
 * {@code IErpInvStockMoveBiz.generateMove}（出库方向、moveType=MANUFACTURING、relatedBillType=ERP_MFG_ISSUE），
 * 业务联动自动推进移动单至 DONE 并扣减库存；回写 {@code ErpMfgWorkOrderLine.actualQuantity}，
 * 汇总领料出库流水 {@code ErpInvStockLedger.totalCost} → {@code ErpMfgWorkOrder.materialCost}。
 *
 * <p>幂等键 {@code (ERP_MFG_ISSUE, issue.code)} 由 {@code generateMove} 防重复触发。
 * 权威：{@code docs/design/manufacturing/state-machine.md}、{@code docs/design/inventory/cross-domain.md}、
 * {@code docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md} Phase 3。
 */
public interface IErpMfgMaterialIssueBiz extends ICrudBiz<ErpMfgMaterialIssue> {

    @BizMutation
    ErpMfgMaterialIssue confirm(@Name("issueId") Long issueId, IServiceContext context);
}
