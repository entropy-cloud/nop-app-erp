
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgMaterialIssueBiz;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.service.processor.ErpMfgMaterialIssueConfirmProcessor;
import app.erp.mfg.service.processor.ErpMfgMaterialIssueReverseConfirmProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 领料单 BizModel（Facade，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 领料确认→出库移动单 + 材料成本回写（{@code confirm}）与领料红冲（{@code reverseConfirm}）各委托独立
 * per-mutation Processor（R6.2 拆分）；共享 protected helper 单一真相源在
 * {@code AbstractErpMfgMaterialIssueProcessor}。CRUD 由 {@link CrudBizModel} 默认提供。
 *
 * <p>权威：{@code docs/design/manufacturing/state-machine.md}、{@code docs/design/inventory/cross-domain.md}。
 */
@BizModel("ErpMfgMaterialIssue")
public class ErpMfgMaterialIssueBizModel extends CrudBizModel<ErpMfgMaterialIssue> implements IErpMfgMaterialIssueBiz {

    @Inject
    ErpMfgMaterialIssueConfirmProcessor confirmProcessor;
    @Inject
    ErpMfgMaterialIssueReverseConfirmProcessor reverseConfirmProcessor;

    public ErpMfgMaterialIssueBizModel() {
        setEntityName(ErpMfgMaterialIssue.class.getName());
    }

    @Override
    @BizMutation
    public ErpMfgMaterialIssue confirm(@Name("issueId") Long issueId, IServiceContext context) {
        return confirmProcessor.confirm(issueId, context);
    }

    @Override
    @BizMutation
    public ErpMfgMaterialIssue reverseConfirm(@Name("issueId") Long issueId, IServiceContext context) {
        return reverseConfirmProcessor.reverseConfirm(issueId, context);
    }
}
