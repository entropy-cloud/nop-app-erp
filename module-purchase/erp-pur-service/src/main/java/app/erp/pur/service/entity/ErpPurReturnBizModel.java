
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurReturnBiz;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.processor.ErpPurReturnProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 采购退货单 BizModel（Facade）。审批状态机 + 退货审核触发库存反向出库 + PURCHASE_RETURN 过账编排委托
 * {@link ErpPurReturnProcessor}（protected step 方法，下游可逐 step 覆盖）。
 */
@BizModel("ErpPurReturn")
public class ErpPurReturnBizModel extends CrudBizModel<ErpPurReturn> implements IErpPurReturnBiz {

    @Inject
    ErpPurReturnProcessor returnProcessor;

    public ErpPurReturnBizModel() {
        setEntityName(ErpPurReturn.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurReturn submit(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.submit(returnId, context);
    }

    @Override
    @BizMutation
    public ErpPurReturn withdrawSubmit(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.withdrawSubmit(returnId, context);
    }

    @Override
    @BizMutation
    public ErpPurReturn approve(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.approve(returnId, context);
    }

    @Override
    @BizMutation
    public ErpPurReturn reject(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.reject(returnId, context);
    }

    @Override
    @BizMutation
    public ErpPurReturn reverseApprove(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.reverseApprove(returnId, context);
    }

    @Override
    @BizMutation
    public ErpPurReturn cancel(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.cancel(returnId, context);
    }
}
