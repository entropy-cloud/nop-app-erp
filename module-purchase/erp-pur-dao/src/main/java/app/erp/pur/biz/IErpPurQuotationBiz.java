
package app.erp.pur.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.pur.dao.entity.ErpPurQuotation;

public interface IErpPurQuotationBiz extends ICrudBiz<ErpPurQuotation>, IApprovableBiz<ErpPurQuotation>{

}
