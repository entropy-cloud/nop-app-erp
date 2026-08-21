
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstSplitBiz;
import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.service.processor.ErpAstSplitCancelProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 资产拆分 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 per-mutation Processor 全权处理；非审批动作（cancel）经
 * per-mutation {@link ErpAstSplitCancelProcessor}。
 *
 * <p>reverseApprove 经 Processor 抛 {@code ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED}
 * （owner doc {@code split-merge.md} §关键业务规则 5 不可逆契约）。
 */
@BizModel("ErpAstSplit")
public class ErpAstSplitBizModel extends CrudBizModel<ErpAstSplit> implements IErpAstSplitBiz {

    @Inject
    ErpAstSplitCancelProcessor cancelProcessor;

    public ErpAstSplitBizModel() {
        setEntityName(ErpAstSplit.class.getName());
    }

    @Override
    @BizMutation
    public ErpAstSplit cancel(@Name("id") String id, IServiceContext context) {
        return cancelProcessor.cancel(id, context);
    }
}
