
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

import java.util.List;

/**
 * 采购退货单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由 xbiz 一行委托注入 Processor；非审批动作（cancel）在本类完成
 * Long→String 转换后委托 Processor。
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
    public ErpPurReturn cancel(@Name("returnId") Long returnId, IServiceContext context) {
        return returnProcessor.cancel(String.valueOf(returnId), context);
    }

    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

}
