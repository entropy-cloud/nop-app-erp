
package app.erp.mnt.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mnt.biz.IErpMntCalibrationBiz;
import app.erp.mnt.dao.entity.ErpMntCalibration;

@BizModel("ErpMntCalibration")
public class ErpMntCalibrationBizModel extends CrudBizModel<ErpMntCalibration> implements IErpMntCalibrationBiz{
    public ErpMntCalibrationBizModel(){
        setEntityName(ErpMntCalibration.class.getName());
    }

    @BizLoader(forType = ErpMntCalibration.class)
    public List<String> orgName(@ContextSource List<ErpMntCalibration> list) {
        orm().batchLoadProps(list, Collections.singleton("org"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntCalibration entity : list) {
            result.add(entity.getOrg() != null ? entity.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMntCalibration.class)
    public List<String> equipmentCode(@ContextSource List<ErpMntCalibration> list) {
        orm().batchLoadProps(list, Collections.singleton("equipment"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpMntCalibration entity : list) {
            result.add(entity.getEquipment() != null ? entity.getEquipment().getCode() : null);
        }
        return result;
    }
}
