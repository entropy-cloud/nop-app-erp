
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinNotesPayableBiz;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.service.processor.ErpFinNotesPayableProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 应付票据 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 状态机 + 授信额度校验 + NOTES_PAYABLE 业财过账编排委托
 * {@link ErpFinNotesPayableProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义与配置门控见 {@code treasury.md}；{@code @BizMutation}+{@code @SingleSession} 钉事务/会话边界。
 */
@BizModel("ErpFinNotesPayable")
public class ErpFinNotesPayableBizModel extends CrudBizModel<ErpFinNotesPayable> implements IErpFinNotesPayableBiz {

    @Inject
    ErpFinNotesPayableProcessor notesPayableProcessor;

    public ErpFinNotesPayableBizModel() {
        setEntityName(ErpFinNotesPayable.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesPayable issue(@Name("notesId") Long notesId, IServiceContext context) {
        return notesPayableProcessor.issue(notesId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesPayable honor(@Name("notesId") Long notesId, IServiceContext context) {
        return notesPayableProcessor.honor(notesId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesPayable dishonor(@Name("notesId") Long notesId, IServiceContext context) {
        return notesPayableProcessor.dishonor(notesId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesPayable writeOff(@Name("notesId") Long notesId, IServiceContext context) {
        return notesPayableProcessor.writeOff(notesId, context);
    }
}
