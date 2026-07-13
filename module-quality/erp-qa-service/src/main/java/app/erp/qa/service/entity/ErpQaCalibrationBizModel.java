
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaCalibrationBiz;
import app.erp.qa.dao.entity.ErpQaCalibration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpQaCalibration")
public class ErpQaCalibrationBizModel extends CrudBizModel<ErpQaCalibration> implements IErpQaCalibrationBiz{
    public ErpQaCalibrationBizModel(){
        setEntityName(ErpQaCalibration.class.getName());
    }

    @BizLoader(forType = ErpQaCalibration.class)
    public List<String> orgName(@ContextSource List<ErpQaCalibration> list) {
        orm().batchLoadProps(list, Collections.singleton("org"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaCalibration entity : list) {
            result.add(entity.getOrg() != null ? entity.getOrg().getName() : null);
        }
        return result;
    }
}
