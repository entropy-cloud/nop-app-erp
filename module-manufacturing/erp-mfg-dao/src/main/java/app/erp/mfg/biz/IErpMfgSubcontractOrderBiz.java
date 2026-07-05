
package app.erp.mfg.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;

public interface IErpMfgSubcontractOrderBiz extends ICrudBiz<ErpMfgSubcontractOrder>, IApprovableBiz<ErpMfgSubcontractOrder>{

}
