
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMaintenanceCostBiz;
import app.erp.ast.dao.entity.ErpAstMaintenanceCost;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpAstMaintenanceCost")
public class ErpAstMaintenanceCostBizModel extends CrudBizModel<ErpAstMaintenanceCost> implements IErpAstMaintenanceCostBiz {
    public ErpAstMaintenanceCostBizModel() {
        setEntityName(ErpAstMaintenanceCost.class.getName());
    }

    @BizLoader(forType = ErpAstMaintenanceCost.class)
    public List<String> maintenanceCode(@ContextSource List<ErpAstMaintenanceCost> rows) {
        orm().batchLoadProps(rows, Collections.singleton("maintenance"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMaintenanceCost row : rows) {
            result.add(row.getMaintenance() != null ? row.getMaintenance().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMaintenanceCost.class)
    public List<String> orgName(@ContextSource List<ErpAstMaintenanceCost> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMaintenanceCost row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMaintenanceCost.class)
    public List<String> currencyName(@ContextSource List<ErpAstMaintenanceCost> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMaintenanceCost row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }
}
