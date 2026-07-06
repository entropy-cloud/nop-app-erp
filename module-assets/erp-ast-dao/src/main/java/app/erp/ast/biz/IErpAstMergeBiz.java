
package app.erp.ast.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.ast.dao.entity.ErpAstMerge;

/**
 * 资产合并业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时经 xbiz 委托 {@code ErpAstMergeProcessor} 处理。
 *
 * <p>reverseApprove 走 {@code ErpAstMergeProcessor} 抛 {@code ERR_AST_MERGE_REVERSE_NOT_SUPPORTED}
 * （遵守 owner doc {@code split-merge.md} §关键业务规则 5 不可逆契约）。
 */
public interface IErpAstMergeBiz extends ICrudBiz<ErpAstMerge>, IApprovableBiz<ErpAstMerge> {

    @BizMutation
    ErpAstMerge cancel(@Name("id") Long id, IServiceContext context);
}
