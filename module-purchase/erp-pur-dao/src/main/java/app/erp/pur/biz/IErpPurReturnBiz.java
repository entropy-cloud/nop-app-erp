
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurReturn;

/**
 * 采购退货单业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/purchase/returns.md}
 * 与 {@code state-machine.md}；复用现有 {@code docStatus}+{@code approveStatus} 两轴，不新增 returnStatus 字段）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前置供应商启用 + 源入库已审核 + 行非空 + 退货原因必填（按配置））。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED，并触发库存反向出库移动（{@code IErpInvStockMoveBiz.generateMove}，
 *       {@code relatedBillType=PUR_RETURN}）+ PURCHASE_RETURN 过账（红字冲减暂估应付/存货，{@code posted=true}）。
 *       退货数量上限 ≤ 入库已审核未退货量（聚合查询，{@code returns.md §退货数量限制}）。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（反审核，前置冲销已生成库存移动单 + 红字冲销已过账凭证）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED（已 APPROVED 者须先冲销）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpPurReturnBiz extends ICrudBiz<ErpPurReturn> {

    @BizMutation
    ErpPurReturn submit(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpPurReturn withdrawSubmit(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpPurReturn approve(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpPurReturn reject(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpPurReturn reverseApprove(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpPurReturn cancel(@Name("returnId") Long returnId, IServiceContext context);
}
