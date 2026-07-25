
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurRfq;

public interface IErpPurRfqBiz extends ICrudBiz<ErpPurRfq>{

    /**
     * 作废/流标询价单：docStatus→CANCELLED。
     */
    @BizMutation
    ErpPurRfq cancel(@Name("rfqId") Long rfqId, IServiceContext context);
}
