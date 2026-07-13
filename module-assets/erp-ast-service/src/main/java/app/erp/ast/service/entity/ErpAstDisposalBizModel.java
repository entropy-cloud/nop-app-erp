
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstDisposalBiz;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.processor.ErpAstDisposalProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 资产处置 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpAstDisposalProcessor} 全权处理。
 */
@BizModel("ErpAstDisposal")
public class ErpAstDisposalBizModel extends CrudBizModel<ErpAstDisposal> implements IErpAstDisposalBiz {

    @Inject
    ErpAstDisposalProcessor disposalProcessor;

    public ErpAstDisposalBizModel() {
        setEntityName(ErpAstDisposal.class.getName());
    }

    @BizLoader(forType = ErpAstDisposal.class)
    public List<String> orgName(@ContextSource List<ErpAstDisposal> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstDisposal row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstDisposal.class)
    public List<String> assetCode(@ContextSource List<ErpAstDisposal> rows) {
        orm().batchLoadProps(rows, Collections.singleton("asset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstDisposal row : rows) {
            result.add(row.getAsset() != null ? row.getAsset().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstDisposal.class)
    public List<String> currencyName(@ContextSource List<ErpAstDisposal> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstDisposal row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }
}
