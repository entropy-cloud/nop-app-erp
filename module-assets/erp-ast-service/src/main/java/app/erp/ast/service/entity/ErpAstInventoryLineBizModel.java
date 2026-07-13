
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstInventoryLineBiz;
import app.erp.ast.dao.entity.ErpAstInventoryLine;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpAstInventoryLine")
public class ErpAstInventoryLineBizModel extends CrudBizModel<ErpAstInventoryLine> implements IErpAstInventoryLineBiz {
    public ErpAstInventoryLineBizModel() {
        setEntityName(ErpAstInventoryLine.class.getName());
    }

    @BizLoader(forType = ErpAstInventoryLine.class)
    public List<String> inventoryCode(@ContextSource List<ErpAstInventoryLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("inventory"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventoryLine row : rows) {
            result.add(row.getInventory() != null ? row.getInventory().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventoryLine.class)
    public List<String> orgName(@ContextSource List<ErpAstInventoryLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventoryLine row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventoryLine.class)
    public List<String> categoryName(@ContextSource List<ErpAstInventoryLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("category"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventoryLine row : rows) {
            result.add(row.getCategory() != null ? row.getCategory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventoryLine.class)
    public List<String> newAssetCode(@ContextSource List<ErpAstInventoryLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("newAsset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventoryLine row : rows) {
            result.add(row.getNewAsset() != null ? row.getNewAsset().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventoryLine.class)
    public List<String> capitalizationCode(@ContextSource List<ErpAstInventoryLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("capitalization"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventoryLine row : rows) {
            result.add(row.getCapitalization() != null ? row.getCapitalization().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstInventoryLine.class)
    public List<String> disposalCode(@ContextSource List<ErpAstInventoryLine> rows) {
        orm().batchLoadProps(rows, Collections.singleton("disposal"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstInventoryLine row : rows) {
            result.add(row.getDisposal() != null ? row.getDisposal().getCode() : null);
        }
        return result;
    }
}
