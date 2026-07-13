
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMovementBiz;
import app.erp.ast.dao.entity.ErpAstMovement;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpAstMovement")
public class ErpAstMovementBizModel extends CrudBizModel<ErpAstMovement> implements IErpAstMovementBiz {
    public ErpAstMovementBizModel() {
        setEntityName(ErpAstMovement.class.getName());
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> orgName(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> assetCode(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("asset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getAsset() != null ? row.getAsset().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> fromDepartmentName(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("fromDepartment"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getFromDepartment() != null ? row.getFromDepartment().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> toDepartmentName(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("toDepartment"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getToDepartment() != null ? row.getToDepartment().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> fromStaffName(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("fromStaff"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getFromStaff() != null ? row.getFromStaff().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> toStaffName(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("toStaff"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getToStaff() != null ? row.getToStaff().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> fromLocationName(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("fromLocation"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getFromLocation() != null ? row.getFromLocation().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> toLocationName(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("toLocation"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getToLocation() != null ? row.getToLocation().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> handlerName(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("handler"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getHandler() != null ? row.getHandler().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMovement.class)
    public List<String> currencyName(@ContextSource List<ErpAstMovement> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMovement row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }
}
