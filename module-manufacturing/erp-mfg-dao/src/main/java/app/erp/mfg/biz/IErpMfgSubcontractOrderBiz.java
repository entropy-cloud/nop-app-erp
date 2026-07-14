
package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;

/**
 * 委外加工单业务接口。除标准 CRUD + 三轴审批（{@link IApprovableBiz}）外，定义委外生命周期契约
 * （{@code docs/design/manufacturing/subcontracting.md §状态机设计}）。
 *
 * <p>标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）由 {@link IApprovableBiz}
 * 声明；状态机业务动作（issueMaterials/receiveFinished/postProcessingFee）由本接口声明。
 *
 * <p>状态机（{@link BizMutation}，自动事务包装）：
 * <ul>
 *   <li>提交/审核/驳回/取消：经审批轴驱动 docStatus DRAFT→SUBMITTED→APPROVED / REJECTED / CANCELLED。</li>
 *   <li>{@link #issueMaterials}：APPROVED→ISSUED，发料出库（inventory OUTGOING 移动单）。</li>
 *   <li>{@link #receiveFinished}：ISSUED→RECEIVED，成品入库（inventory INCOMING 移动单）。</li>
 *   <li>{@link #postProcessingFee}：RECEIVED→COMPLETED，加工费过账（生成应付凭证，posted=true）。</li>
 * </ul>
 *
 * <p>权威：{@code docs/design/manufacturing/subcontracting.md}；计划见
 * {@code docs/plans/2026-07-13-0455-1-manufacturing-subcontracting-engine.md}。
 */
public interface IErpMfgSubcontractOrderBiz extends ICrudBiz<ErpMfgSubcontractOrder>, IApprovableBiz<ErpMfgSubcontractOrder> {

    /**
     * 取消委外单。DRAFT/SUBMITTED/APPROVED→CANCELLED。
     */
    @BizMutation
    ErpMfgSubcontractOrder cancel(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context);

    /**
     * 发料给供应商。APPROVED→ISSUED，生成库存出库移动单（材料成本出库）。
     *
     * @param sourceWarehouseId 发料源仓库（可选，null 时由库存域默认仓库处理）
     */
    @BizMutation
    ErpMfgSubcontractOrder issueMaterials(@Name("subcontractOrderId") Long subcontractOrderId,
                                          @io.nop.api.core.annotations.core.Optional @Name("sourceWarehouseId") Long sourceWarehouseId,
                                          IServiceContext context);

    /**
     * 收回加工品入库。ISSUED→RECEIVED，生成库存入库移动单（成品入库，含加工费成本）。
     *
     * @param destWarehouseId 收货目标仓库（可选，null 时由库存域默认仓库处理）
     */
    @BizMutation
    ErpMfgSubcontractOrder receiveFinished(@Name("subcontractOrderId") Long subcontractOrderId,
                                           @Name("receivedQty") java.math.BigDecimal receivedQty,
                                           @io.nop.api.core.annotations.core.Optional @Name("destWarehouseId") Long destWarehouseId,
                                           IServiceContext context);

    /**
     * 加工费过账。RECEIVED→COMPLETED，生成应付凭证（借委外物资/贷应付账款），posted=true。
     * config-gated {@code erp-mfg.subcontract-posting-enabled}（默认 false）。
     */
    @BizMutation
    ErpMfgSubcontractOrder postProcessingFee(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context);

    /**
     * 红冲完工。COMPLETED→CANCELLED，红冲三段 GL 凭证（SI/SR/SF 经 {@code IErpFinVoucherBiz.reverse}）
     * + 反向两段库存移动（issue/receipt 经 {@code IErpInvStockMoveBiz.reverse}）+ posted=false。
     * 仅 COMPLETED 且 posted==true 的委外单可红冲。
     */
    @BizMutation
    ErpMfgSubcontractOrder reverseCompletion(@Name("subcontractOrderId") Long subcontractOrderId, IServiceContext context);
}
