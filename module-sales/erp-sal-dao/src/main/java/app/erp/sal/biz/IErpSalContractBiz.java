
package app.erp.sal.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.sal.dao.entity.ErpSalContract;

public interface IErpSalContractBiz extends ICrudBiz<ErpSalContract>, IApprovableBiz<ErpSalContract>{

}
