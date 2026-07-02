
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.erp.fin.dao.entity.ErpFinNotesReceivable;

/**
 * 应收票据 Biz 契约（{@code treasury.md §状态机}）。CRUD 之外承载 7 态状态机：
 * receive（置 RECEIVED）→ discount/endorse/collect → honor/dishonor；任何非终态 → writeOff。
 *
 * <p>贴现（discount）计算 discountInterest=票面×贴现率×剩余天数/360、netAmount=票面−贴现息，
 * 生成 {@code ErpFinNotesDiscount} 并置票据 status=DISCOUNTED。
 */
public interface IErpFinNotesReceivableBiz extends ICrudBiz<ErpFinNotesReceivable> {

    @BizMutation
    ErpFinNotesReceivable receive(@Name("notesId") Long notesId, IServiceContext context);

    @BizMutation
    ErpFinNotesReceivable discount(@Name("notesId") Long notesId,
                                   @Name("discountDate") LocalDate discountDate,
                                   @Name("bankId") Long bankId,
                                   @Name("discountRate") BigDecimal discountRate,
                                   IServiceContext context);

    @BizMutation
    ErpFinNotesReceivable endorse(@Name("notesId") Long notesId,
                                  @Name("endorsementFromId") Long endorsementFromId,
                                  IServiceContext context);

    @BizMutation
    ErpFinNotesReceivable collect(@Name("notesId") Long notesId, IServiceContext context);

    @BizMutation
    ErpFinNotesReceivable honor(@Name("notesId") Long notesId, IServiceContext context);

    @BizMutation
    ErpFinNotesReceivable dishonor(@Name("notesId") Long notesId, IServiceContext context);

    @BizMutation
    ErpFinNotesReceivable writeOff(@Name("notesId") Long notesId, IServiceContext context);
}
