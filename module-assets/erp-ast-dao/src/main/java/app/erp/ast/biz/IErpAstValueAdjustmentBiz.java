
package app.erp.ast.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.ast.dao.entity.ErpAstValueAdjustment;

/**
 * 资产价值调整业务接口。标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 由 {@link IApprovableBiz} 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 */
public interface IErpAstValueAdjustmentBiz extends ICrudBiz<ErpAstValueAdjustment>,
        IApprovableBiz<ErpAstValueAdjustment> {

    @BizMutation
    ErpAstValueAdjustment cancel(@Name("id") Long id, IServiceContext context);
}
