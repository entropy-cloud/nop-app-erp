
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaCalibrationBiz;
import app.erp.qa.dao.entity.ErpQaCalibration;

@BizModel("ErpQaCalibration")
public class ErpQaCalibrationBizModel extends CrudBizModel<ErpQaCalibration> implements IErpQaCalibrationBiz{
    public ErpQaCalibrationBizModel(){
        setEntityName(ErpQaCalibration.class.getName());
    }
}
