
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstValueAdjustmentBiz;
import app.erp.ast.dao.entity.ErpAstValueAdjustment;
import app.erp.ast.service.processor.ErpAstValueAdjustmentProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 资产价值调整 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpAstValueAdjustmentProcessor} 全权处理。
 */
@BizModel("ErpAstValueAdjustment")
public class ErpAstValueAdjustmentBizModel extends CrudBizModel<ErpAstValueAdjustment>
        implements IErpAstValueAdjustmentBiz {

    @Inject
    ErpAstValueAdjustmentProcessor adjustmentProcessor;

    public ErpAstValueAdjustmentBizModel() {
        setEntityName(ErpAstValueAdjustment.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstValueAdjustment cancel(@Name("id") Long id, IServiceContext context) {
        return adjustmentProcessor.cancel(id, context);
    }
}
