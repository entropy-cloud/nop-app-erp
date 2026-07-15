
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSalarySimulationItemAdjustmentBiz;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;

import java.util.List;

@BizModel("ErpHrSalarySimulationItemAdjustment")
public class ErpHrSalarySimulationItemAdjustmentBizModel extends CrudBizModel<ErpHrSalarySimulationItemAdjustment> implements IErpHrSalarySimulationItemAdjustmentBiz{
    public ErpHrSalarySimulationItemAdjustmentBizModel(){
        setEntityName(ErpHrSalarySimulationItemAdjustment.class.getName());
    }

}
