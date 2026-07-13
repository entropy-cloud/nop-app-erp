
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpAstAsset")
public class ErpAstAssetBizModel extends CrudBizModel<ErpAstAsset> implements IErpAstAssetBiz {
    public ErpAstAssetBizModel() {
        setEntityName(ErpAstAsset.class.getName());
    }

    @BizLoader(forType = ErpAstAsset.class)
    public List<String> orgName(@ContextSource List<ErpAstAsset> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAsset row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAsset.class)
    public List<String> categoryName(@ContextSource List<ErpAstAsset> rows) {
        orm().batchLoadProps(rows, Collections.singleton("category"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAsset row : rows) {
            result.add(row.getCategory() != null ? row.getCategory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAsset.class)
    public List<String> currencyName(@ContextSource List<ErpAstAsset> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAsset row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAsset.class)
    public List<String> departmentName(@ContextSource List<ErpAstAsset> rows) {
        orm().batchLoadProps(rows, Collections.singleton("department"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAsset row : rows) {
            result.add(row.getDepartment() != null ? row.getDepartment().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAsset.class)
    public List<String> locationName(@ContextSource List<ErpAstAsset> rows) {
        orm().batchLoadProps(rows, Collections.singleton("location"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAsset row : rows) {
            result.add(row.getLocation() != null ? row.getLocation().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAsset.class)
    public List<String> employeeName(@ContextSource List<ErpAstAsset> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAsset row : rows) {
            result.add(row.getEmployee() != null ? row.getEmployee().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAsset.class)
    public List<String> staffName(@ContextSource List<ErpAstAsset> rows) {
        orm().batchLoadProps(rows, Collections.singleton("staff"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAsset row : rows) {
            result.add(row.getStaff() != null ? row.getStaff().getName() : null);
        }
        return result;
    }
}
