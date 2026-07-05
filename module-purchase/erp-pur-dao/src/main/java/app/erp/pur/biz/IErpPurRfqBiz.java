
package app.erp.pur.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.pur.dao.entity.ErpPurRfq;

public interface IErpPurRfqBiz extends ICrudBiz<ErpPurRfq>, IApprovableBiz<ErpPurRfq>{

}
