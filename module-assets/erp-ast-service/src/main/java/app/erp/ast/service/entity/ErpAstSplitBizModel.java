
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstSplitBiz;
import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.service.processor.ErpAstSplitProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 资产拆分 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpAstSplitProcessor} 全权处理。
 *
 * <p>reverseApprove 经 Processor 抛 {@code ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED}
 * （owner doc {@code split-merge.md} §关键业务规则 5 不可逆契约）。
 */
@BizModel("ErpAstSplit")
public class ErpAstSplitBizModel extends CrudBizModel<ErpAstSplit> implements IErpAstSplitBiz {

    @Inject
    ErpAstSplitProcessor splitProcessor;

    public ErpAstSplitBizModel() {
        setEntityName(ErpAstSplit.class.getName());
    }

    @BizLoader(forType = ErpAstSplit.class)
    public List<String> orgName(@ContextSource List<ErpAstSplit> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstSplit row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstSplit.class)
    public List<String> sourceAssetCode(@ContextSource List<ErpAstSplit> rows) {
        orm().batchLoadProps(rows, Collections.singleton("sourceAsset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstSplit row : rows) {
            result.add(row.getSourceAsset() != null ? row.getSourceAsset().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstSplit.class)
    public List<String> currencyName(@ContextSource List<ErpAstSplit> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstSplit row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

    @Override
    @BizMutation
    public ErpAstSplit cancel(@Name("id") Long id, IServiceContext context) {
        return splitProcessor.cancel(id, context);
    }
}
