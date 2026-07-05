
package app.erp.sal.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.sal.dao.entity.ErpSalReturn;

/**
 * 销售退货单业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 *
 * <p>审批状态机（对齐 {@code docs/design/sales/returns.md} 与 {@code state-machine.md}；复用现有
 * {@code docStatus}+{@code approveStatus} 两轴，不新增 returnStatus 字段）。approve 触发库存反向入库移动 +
 * SALES_RETURN 过账 + 回减客户应收余额。reverseApprove 前置冲销已生成库存移动单 + 红字冲销已过账凭证。
 * 每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpSalReturnBiz extends ICrudBiz<ErpSalReturn>, IApprovableBiz<ErpSalReturn> {

    @BizMutation
    ErpSalReturn cancel(@Name("returnId") Long returnId, IServiceContext context);
}
