
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMergeBiz;
import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.service.processor.ErpAstMergeProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

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

    @Override
    @BizMutation
    public ErpAstMerge cancel(@Name("id") Long id, IServiceContext context) {
        return mergeProcessor.cancel(id, context);
    }
}
