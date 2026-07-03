
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstDisposalBiz;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.processor.ErpAstDisposalProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 资产处置 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 三轴审批状态机 + 清理损益计算 + DISPOSAL 业财过账编排委托
 * {@link ErpAstDisposalProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义见 {@code depreciation-and-posting.md} §3；{@code @BizMutation}+{@code @SingleSession} 钉事务/会话边界。
 */
@BizModel("ErpAstDisposal")
public class ErpAstDisposalBizModel extends CrudBizModel<ErpAstDisposal> implements IErpAstDisposalBiz {

    @Inject
    ErpAstDisposalProcessor disposalProcessor;

    public ErpAstDisposalBizModel() {
        setEntityName(ErpAstDisposal.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDisposal submit(@Name("id") Long id, IServiceContext context) {
        return disposalProcessor.submit(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDisposal approve(@Name("id") Long id, IServiceContext context) {
        return disposalProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDisposal reject(@Name("id") Long id, IServiceContext context) {
        return disposalProcessor.reject(id, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDisposal reverseApprove(@Name("id") Long id, IServiceContext context) {
        return disposalProcessor.reverseApprove(id, context);
    }
}
