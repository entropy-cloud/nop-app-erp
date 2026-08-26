
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.mnt.biz.IErpMntCalibrationBiz;
import app.erp.mnt.dao.entity.ErpMntCalibration;

@BizModel("ErpMntCalibration")
public class ErpMntCalibrationBizModel extends AbstractErpCrudBizModel<ErpMntCalibration> implements IErpMntCalibrationBiz{
    public ErpMntCalibrationBizModel(){
        setEntityName(ErpMntCalibration.class.getName());
    }

}
