
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetCapitalizationBiz;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.service.processor.ErpAstAssetCapitalizationProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 资产资本化（转固）BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpAstAssetCapitalizationProcessor} 全权处理。
 */
@BizModel("ErpAstAssetCapitalization")
public class ErpAstAssetCapitalizationBizModel extends CrudBizModel<ErpAstAssetCapitalization>
        implements IErpAstAssetCapitalizationBiz {

    @Inject
    ErpAstAssetCapitalizationProcessor capitalizationProcessor;

    public ErpAstAssetCapitalizationBizModel() {
        setEntityName(ErpAstAssetCapitalization.class.getName());
    }

    @BizLoader(forType = ErpAstAssetCapitalization.class)
    public List<String> orgName(@ContextSource List<ErpAstAssetCapitalization> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAssetCapitalization row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAssetCapitalization.class)
    public List<String> categoryName(@ContextSource List<ErpAstAssetCapitalization> rows) {
        orm().batchLoadProps(rows, Collections.singleton("category"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAssetCapitalization row : rows) {
            result.add(row.getCategory() != null ? row.getCategory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstAssetCapitalization.class)
    public List<String> currencyName(@ContextSource List<ErpAstAssetCapitalization> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstAssetCapitalization row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }
}
