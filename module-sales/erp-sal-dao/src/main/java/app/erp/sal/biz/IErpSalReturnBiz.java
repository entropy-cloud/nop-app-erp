
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.sal.dao.entity.ErpSalReturn;

/**
 * 销售退货单业务接口。除标准 CRUD 外，定义三轴审批状态机契约（对齐 {@code docs/design/sales/returns.md}
 * 与 {@code state-machine.md}；复用现有 {@code docStatus}+{@code approveStatus} 两轴，不新增 returnStatus 字段）：
 *
 * <ul>
 *   <li>{@link #submit}：UNSUBMITTED/REJECTED → SUBMITTED（前置客户启用 + 行非空）。</li>
 *   <li>{@link #withdrawSubmit}：SUBMITTED → UNSUBMITTED。</li>
 *   <li>{@link #approve}：SUBMITTED → APPROVED，并触发库存反向入库移动（{@code IErpInvStockMoveBiz.generateMove}，
 *       {@code relatedBillType=SAL_RETURN}，库存增加）+ SALES_RETURN 过账（反向 SALES_OUTPUT：
 *       借库存商品/贷主营业务成本，{@code posted=true}）+ 回减客户应收余额（负 AR 辅助账 credit-memo）。
 *       退货数量上限 ≤ 出库已审核未退货量（聚合查询，{@code returns.md §退货数量限制}）。</li>
 *   <li>{@link #reject}：SUBMITTED → REJECTED。</li>
 *   <li>{@link #reverseApprove}：APPROVED → REJECTED（反审核，前置冲销已生成库存移动单 + 红字冲销已过账凭证）。</li>
 *   <li>{@link #cancel}：任意非终态 → docStatus=CANCELLED（已 APPROVED 者须先冲销）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpSalReturnBiz extends ICrudBiz<ErpSalReturn> {

    @BizMutation
    ErpSalReturn submit(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpSalReturn withdrawSubmit(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpSalReturn approve(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpSalReturn reject(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpSalReturn reverseApprove(@Name("returnId") Long returnId, IServiceContext context);

    @BizMutation
    ErpSalReturn cancel(@Name("returnId") Long returnId, IServiceContext context);
}
