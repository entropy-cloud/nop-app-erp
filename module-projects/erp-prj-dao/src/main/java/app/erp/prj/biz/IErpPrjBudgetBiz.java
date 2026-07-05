
package app.erp.prj.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.prj.dao.entity.ErpPrjBudget;

public interface IErpPrjBudgetBiz extends ICrudBiz<ErpPrjBudget>, IApprovableBiz<ErpPrjBudget>{

}
