
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMergeBiz;
import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.service.processor.ErpAstMergeProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 资产合并 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpAstMergeProcessor} 全权处理。
 *
 * <p>reverseApprove 经 Processor 抛 {@code ERR_AST_MERGE_REVERSE_NOT_SUPPORTED}
 * （owner doc {@code split-merge.md} §关键业务规则 5 不可逆契约）。
 */
@BizModel("ErpAstMerge")
public class ErpAstMergeBizModel extends CrudBizModel<ErpAstMerge> implements IErpAstMergeBiz {

    @Inject
    ErpAstMergeProcessor mergeProcessor;

    public ErpAstMergeBizModel() {
        setEntityName(ErpAstMerge.class.getName());
    }

    @BizLoader(forType = ErpAstMerge.class)
    public List<String> orgName(@ContextSource List<ErpAstMerge> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMerge row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMerge.class)
    public List<String> targetAssetCode(@ContextSource List<ErpAstMerge> rows) {
        orm().batchLoadProps(rows, Collections.singleton("targetAsset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMerge row : rows) {
            result.add(row.getTargetAsset() != null ? row.getTargetAsset().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstMerge.class)
    public List<String> currencyName(@ContextSource List<ErpAstMerge> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstMerge row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstMerge cancel(@Name("id") Long id, IServiceContext context) {
        return mergeProcessor.cancel(id, context);
    }
}
