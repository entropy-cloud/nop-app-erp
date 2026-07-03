
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.processor.ErpFinEmployeeAdvanceProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 员工借款单 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 三轴审批状态机编排委托 {@link ErpFinEmployeeAdvanceProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义与配置门控见 {@code expense-claim.md}；{@code @BizMutation}+{@code @SingleSession} 钉事务/会话边界。
 */
@BizModel("ErpFinEmployeeAdvance")
public class ErpFinEmployeeAdvanceBizModel extends CrudBizModel<ErpFinEmployeeAdvance> implements IErpFinEmployeeAdvanceBiz {

    @Inject
    ErpFinEmployeeAdvanceProcessor advanceProcessor;

    public ErpFinEmployeeAdvanceBizModel() {
        setEntityName(ErpFinEmployeeAdvance.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance submit(@Name("advanceId") Long advanceId, IServiceContext context) {
        return advanceProcessor.submit(advanceId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance withdrawSubmit(@Name("advanceId") Long advanceId, IServiceContext context) {
        return advanceProcessor.withdrawSubmit(advanceId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance approve(@Name("advanceId") Long advanceId, IServiceContext context) {
        return advanceProcessor.approve(advanceId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance reject(@Name("advanceId") Long advanceId, IServiceContext context) {
        return advanceProcessor.reject(advanceId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance reverseApprove(@Name("advanceId") Long advanceId, IServiceContext context) {
        return advanceProcessor.reverseApprove(advanceId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinEmployeeAdvance cancel(@Name("advanceId") Long advanceId, IServiceContext context) {
        return advanceProcessor.cancel(advanceId, context);
    }
}
