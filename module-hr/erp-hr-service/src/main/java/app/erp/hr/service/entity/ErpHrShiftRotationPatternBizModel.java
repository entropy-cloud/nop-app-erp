
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrShiftRotationPatternBiz;
import app.erp.hr.dao.entity.ErpHrShiftRotationPattern;

@BizModel("ErpHrShiftRotationPattern")
public class ErpHrShiftRotationPatternBizModel extends CrudBizModel<ErpHrShiftRotationPattern> implements IErpHrShiftRotationPatternBiz{
    public ErpHrShiftRotationPatternBizModel(){
        setEntityName(ErpHrShiftRotationPattern.class.getName());
    }
}
