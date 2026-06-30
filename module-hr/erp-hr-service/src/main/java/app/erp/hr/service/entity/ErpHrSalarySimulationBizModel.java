
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSalarySimulationBiz;
import app.erp.hr.dao.entity.ErpHrSalarySimulation;

@BizModel("ErpHrSalarySimulation")
public class ErpHrSalarySimulationBizModel extends CrudBizModel<ErpHrSalarySimulation> implements IErpHrSalarySimulationBiz{
    public ErpHrSalarySimulationBizModel(){
        setEntityName(ErpHrSalarySimulation.class.getName());
    }
}
