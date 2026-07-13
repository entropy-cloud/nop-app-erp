
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSalarySimulationItemAdjustmentBiz;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrSalarySimulationItemAdjustment")
public class ErpHrSalarySimulationItemAdjustmentBizModel extends CrudBizModel<ErpHrSalarySimulationItemAdjustment> implements IErpHrSalarySimulationItemAdjustmentBiz{
    public ErpHrSalarySimulationItemAdjustmentBizModel(){
        setEntityName(ErpHrSalarySimulationItemAdjustment.class.getName());
    }

    @BizLoader(forType = ErpHrSalarySimulationItemAdjustment.class)
    public List<String> simulationCode(@ContextSource List<ErpHrSalarySimulationItemAdjustment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("simulation"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSalarySimulationItemAdjustment row : rows) {
            result.add(row.orm_attached() && row.getSimulation() != null ? row.getSimulation().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrSalarySimulationItemAdjustment.class)
    public List<String> employeeDisplayName(@ContextSource List<ErpHrSalarySimulationItemAdjustment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSalarySimulationItemAdjustment row : rows) {
            result.add(row.orm_attached() && row.getEmployee() != null ? row.getEmployee().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrSalarySimulationItemAdjustment.class)
    public List<String> orgName(@ContextSource List<ErpHrSalarySimulationItemAdjustment> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSalarySimulationItemAdjustment row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
