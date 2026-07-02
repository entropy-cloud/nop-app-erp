
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.entity.ErpFinNotesPayable;

/**
 * 应付票据 Biz 契约（{@code treasury.md §状态机}）。CRUD 之外承载状态机：
 * issue（置 ISSUED）→ honor/dishonor；任何非终态 → writeOff。
 *
 * <p>开出银承（notesType=BANK_ACCEPTANCE）时按配置 {@code erp-fin.credit-check-on-issue}（默认 true）
 * 强一致校验授信可用额度并占用（{@link IErpFinCreditFacilityBiz#reserveCredit}）；
 * 兑付/注销时释放额度。
 */
public interface IErpFinNotesPayableBiz extends ICrudBiz<ErpFinNotesPayable> {

    @BizMutation
    ErpFinNotesPayable issue(@Name("notesId") Long notesId, IServiceContext context);

    @BizMutation
    ErpFinNotesPayable honor(@Name("notesId") Long notesId, IServiceContext context);

    @BizMutation
    ErpFinNotesPayable dishonor(@Name("notesId") Long notesId, IServiceContext context);

    @BizMutation
    ErpFinNotesPayable writeOff(@Name("notesId") Long notesId, IServiceContext context);
}
