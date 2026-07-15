
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

import java.util.List;

/**
 * 销售退货单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalReturnProcessor#onSubmit}/{@link ErpSalReturnProcessor#onApproved}/{@link ErpSalReturnProcessor#onReverseApproved}。
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
    public ErpSalReturn cancel(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.cancel(String.valueOf(returnId), context);
    }

    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

}
