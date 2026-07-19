
package app.erp.inv.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.inv.dao.entity.ErpInvTransferOrder;

public interface IErpInvTransferOrderBiz extends ICrudBiz<ErpInvTransferOrder>{

    /**
     * 调拨确认：DRAFT→CONFIRMED。审核后自动生成出入库移动单（如配置启用）。
     */
    @BizMutation
    ErpInvTransferOrder confirm(@Name("transferOrderId") Long transferOrderId, IServiceContext context);

}
