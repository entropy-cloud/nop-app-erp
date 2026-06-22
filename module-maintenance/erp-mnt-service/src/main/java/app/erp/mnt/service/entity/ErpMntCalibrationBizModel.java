
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntCalibrationBiz;
import app.erp.mnt.dao.entity.ErpMntCalibration;

@BizModel("ErpMntCalibration")
public class ErpMntCalibrationBizModel extends CrudBizModel<ErpMntCalibration> implements IErpMntCalibrationBiz{
    public ErpMntCalibrationBizModel(){
        setEntityName(ErpMntCalibration.class.getName());
    }
}
