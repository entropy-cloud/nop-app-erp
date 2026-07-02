
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinCreditFacilityBiz;
import app.erp.fin.biz.IErpFinNotesPayableBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.NotesPostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * 应付票据 BizModel（{@code treasury.md §状态机}）。承载状态机：
 * issue（→ISSUED）→ honor（→HONORED）/dishonor（→DISHONORED）；任何非终态→writeOff（→WRITE_OFF）。
 *
 * <p>授信额度强一致校验（{@code treasury.md §规则1}）：开银承（notesType=BANK_ACCEPTANCE）时，
 * 若配置 {@code erp-fin.credit-check-on-issue}=true（默认），经 {@link IErpFinCreditFacilityBiz#reserveCredit}
 * 校验 availableAmount>=票面 并占用；honor/writeOff 时 {@link IErpFinCreditFacilityBiz#releaseCredit} 释放。
 * 并发竞争由 CreditFacility.version 乐观锁兜底。
 *
 * <p>状态迁移成功后经 {@link NotesPostingDispatcher} 触发业财过账（ISSUED/HONORED 两类型），
 * posted 标志在过账成功后置位（失败吞异常保持原态+posted=false）。writeOff 对已过账票据红字冲销。
 */
@BizModel("ErpFinNotesPayable")
public class ErpFinNotesPayableBizModel extends CrudBizModel<ErpFinNotesPayable> implements IErpFinNotesPayableBiz {

    @Inject
    IErpFinCreditFacilityBiz creditFacilityBiz;
    @Inject
    NotesPostingDispatcher postingDispatcher;

    public ErpFinNotesPayableBizModel() {
        setEntityName(ErpFinNotesPayable.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesPayable issue(@Name("notesId") Long notesId, IServiceContext context) {
        ErpFinNotesPayable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        if (status != null && status == ErpFinConstants.NOTES_PAY_ISSUED) {
            return note;
        }
        if (isTerminal(status)) {
            throw illegalTransition(note, status, "非终态");
        }
        requireAmountPositive(note);

        // 开银承时强一致校验授信可用额度并占用（treasury.md §规则1）。
        if (isBankAcceptance(note) && isCreditCheckOnIssue() && note.getCreditFacilityId() != null) {
            creditFacilityBiz.reserveCredit(note.getCreditFacilityId(), nz(note.getAmountFunctional()), context);
        }

        note.setStatus(ErpFinConstants.NOTES_PAY_ISSUED);
        dao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostPayable(note, ErpFinBusinessType.NOTES_PAYABLE_ISSUED);
        note = requireNote(notesId, context);
        markPosted(note, posted);
        dao().updateEntity(note);
        return note;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesPayable honor(@Name("notesId") Long notesId, IServiceContext context) {
        ErpFinNotesPayable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        if (status == null || status != ErpFinConstants.NOTES_PAY_ISSUED) {
            throw illegalTransition(note, status, "ISSUED");
        }
        releaseOccupiedCredit(note, context);
        note.setStatus(ErpFinConstants.NOTES_PAY_HONORED);
        dao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostPayable(note, ErpFinBusinessType.NOTES_PAYABLE_HONORED);
        note = requireNote(notesId, context);
        markPosted(note, posted);
        dao().updateEntity(note);
        return note;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesPayable dishonor(@Name("notesId") Long notesId, IServiceContext context) {
        ErpFinNotesPayable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        if (status == null || status != ErpFinConstants.NOTES_PAY_ISSUED) {
            throw illegalTransition(note, status, "ISSUED");
        }
        releaseOccupiedCredit(note, context);
        note.setStatus(ErpFinConstants.NOTES_PAY_DISHONORED);
        dao().updateEntity(note);
        return note;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesPayable writeOff(@Name("notesId") Long notesId, IServiceContext context) {
        ErpFinNotesPayable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        if (isTerminal(status)) {
            throw illegalTransition(note, status, "非终态");
        }
        releaseOccupiedCredit(note, context);
        if (Boolean.TRUE.equals(note.getPosted())) {
            postingDispatcher.reversePayable(note, ErpFinBusinessType.NOTES_PAYABLE_ISSUED);
        }
        note.setStatus(ErpFinConstants.NOTES_PAY_WRITE_OFF);
        note.setPosted(false);
        note.setPostedAt(null);
        note.setPostedBy(null);
        dao().updateEntity(note);
        return note;
    }

    // ---------- helpers ----------

    private void releaseOccupiedCredit(ErpFinNotesPayable note, IServiceContext context) {
        if (isBankAcceptance(note) && note.getCreditFacilityId() != null) {
            creditFacilityBiz.releaseCredit(note.getCreditFacilityId(), nz(note.getAmountFunctional()), context);
        }
    }

    private void markPosted(ErpFinNotesPayable note, boolean posted) {
        if (posted) {
            note.setPosted(true);
            note.setPostedAt(CoreMetrics.currentDateTime());
            note.setPostedBy(currentUserId());
        } else {
            note.setPosted(false);
        }
    }

    private boolean isBankAcceptance(ErpFinNotesPayable note) {
        return note.getNotesType() != null && note.getNotesType() == ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE;
    }

    private boolean isCreditCheckOnIssue() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_CREDIT_CHECK_ON_ISSUE, Boolean.TRUE);
        return flag == null ? true : flag;
    }

    private ErpFinNotesPayable requireNote(Long notesId, IServiceContext context) {
        return requireEntity(String.valueOf(notesId), null, context);
    }

    private boolean isTerminal(Integer status) {
        return status != null
                && (status == ErpFinConstants.NOTES_PAY_HONORED
                || status == ErpFinConstants.NOTES_PAY_DISHONORED
                || status == ErpFinConstants.NOTES_PAY_WRITE_OFF);
    }

    private void requireAmountPositive(ErpFinNotesPayable note) {
        BigDecimal amount = note.getAmountFunctional();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NopException(ErpFinErrors.ERR_NOTES_AMOUNT_INVALID)
                    .param(ErpFinErrors.ARG_NOTES_CODE, note.getCode());
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private NopException illegalTransition(ErpFinNotesPayable note, Integer current, String expected) {
        return new NopException(ErpFinErrors.ERR_NOTES_PAYABLE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_NOTES_CODE, note.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }
}
