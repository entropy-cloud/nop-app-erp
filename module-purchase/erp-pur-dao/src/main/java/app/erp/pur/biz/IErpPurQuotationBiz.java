
package app.erp.pur.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.pur.dao.entity.ErpPurQuotation;

public interface IErpPurQuotationBiz extends ICrudBiz<ErpPurQuotation>{

    /**
     * 作废报价单：docStatus→CANCELLED。
     */
    @BizMutation
    ErpPurQuotation cancel(@Name("quotationId") Long quotationId, IServiceContext context);
}
