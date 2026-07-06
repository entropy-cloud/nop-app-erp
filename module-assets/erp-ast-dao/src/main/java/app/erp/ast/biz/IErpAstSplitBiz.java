
package app.erp.ast.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.ast.dao.entity.ErpAstSplit;

/**
 * 资产拆分业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时经 xbiz 委托 {@code ErpAstSplitProcessor} 处理。
 *
 * <p>reverseApprove 走 {@code ErpAstSplitProcessor} 抛 {@code ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED}
 * （遵守 owner doc {@code split-merge.md} §关键业务规则 5 不可逆契约）。
 */
public interface IErpAstSplitBiz extends ICrudBiz<ErpAstSplit>, IApprovableBiz<ErpAstSplit> {

    @BizMutation
    ErpAstSplit cancel(@Name("id") Long id, IServiceContext context);
}
