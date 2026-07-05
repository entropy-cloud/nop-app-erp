
package app.erp.prj.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.prj.dao.entity.ErpPrjBilling;

public interface IErpPrjBillingBiz extends ICrudBiz<ErpPrjBilling>, IApprovableBiz<ErpPrjBilling>{

}
