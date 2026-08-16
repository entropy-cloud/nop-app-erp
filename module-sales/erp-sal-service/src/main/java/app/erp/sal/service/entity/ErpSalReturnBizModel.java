
package app.erp.sal.service.entity;

import app.erp.sal.biz.ErpSalExchangeDeliveryLine;
import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.processor.ErpSalReturnCancelProcessor;
import app.erp.sal.service.processor.ErpSalReturnGenerateExchangeDeliveryProcessor;
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
 * {@code <source x:override="replace">} 注入 {@code ErpSalReturnProcessor}。
 * 非审批动作（cancel）经 per-mutation {@link ErpSalReturnCancelProcessor}；
 * 换货出库单生成（UC-SAL-06）经 per-mutation {@link ErpSalReturnGenerateExchangeDeliveryProcessor}。
 */
@BizModel("ErpSalReturn")
public class ErpSalReturnBizModel extends CrudBizModel<ErpSalReturn> implements IErpSalReturnBiz {

    @Inject
    ErpSalReturnCancelProcessor cancelProcessor;

    @Inject
    ErpSalReturnGenerateExchangeDeliveryProcessor generateExchangeDeliveryProcessor;

    public ErpSalReturnBizModel() {
        setEntityName(ErpSalReturn.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalReturn cancel(@Name("returnId") Long returnId, IServiceContext context) {
        return cancelProcessor.cancel(String.valueOf(returnId), context);
    }

    @Override
    @BizMutation
    public ErpSalReturn generateExchangeDelivery(@Name("returnId") Long returnId,
                                                 @Name("lines") List<ErpSalExchangeDeliveryLine> lines,
                                                 IServiceContext context) {
        return generateExchangeDeliveryProcessor.generateExchangeDelivery(returnId, lines, context);
    }

    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

}
