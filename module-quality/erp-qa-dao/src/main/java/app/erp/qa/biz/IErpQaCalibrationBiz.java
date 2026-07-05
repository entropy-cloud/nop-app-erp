
package app.erp.qa.biz;

import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import app.erp.qa.dao.entity.ErpQaCalibration;

public interface IErpQaCalibrationBiz extends ICrudBiz<ErpQaCalibration>, IApprovableBiz<ErpQaCalibration>{

}
