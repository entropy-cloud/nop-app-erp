
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.processor.ErpSalReturnProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 销售退货单 BizModel（Facade）。审批状态机 + 退货审核触发库存反向入库 + SALES_RETURN 过账 + 退款编排委托
 * {@link ErpSalReturnProcessor}（protected step 方法，下游可逐 step 覆盖）。
 */
@BizModel("ErpSalReturn")
public class ErpSalReturnBizModel extends CrudBizModel<ErpSalReturn> implements IErpSalReturnBiz {

    @Inject
    ErpSalReturnProcessor returnProcessor;

    public ErpSalReturnBizModel() {
        setEntityName(ErpSalReturn.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalReturn submit(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.submit(returnId, context);
    }

    @Override
    @BizMutation
    public ErpSalReturn withdrawSubmit(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.withdrawSubmit(returnId, context);
    }

    @Override
    @BizMutation
    public ErpSalReturn approve(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.approve(returnId, context);
    }

    @Override
    @BizMutation
    public ErpSalReturn reject(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.reject(returnId, context);
    }

    @Override
    @BizMutation
    public ErpSalReturn reverseApprove(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.reverseApprove(returnId, context);
    }

    @Override
    @BizMutation
    public ErpSalReturn cancel(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.cancel(returnId, context);
    }
}
