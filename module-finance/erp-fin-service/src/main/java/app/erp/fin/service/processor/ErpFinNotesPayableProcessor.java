package app.erp.fin.service.processor;

import app.erp.fin.biz.IErpFinCreditFacilityBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.NotesPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;

/**
 * 应付票据状态机编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * Facade {@code ErpFinNotesPayableBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>配置余地：每个 {@code public} 动作方法只编排步骤顺序，各步骤为 {@code protected} 方法、单一职责、
 * 以 {@link IServiceContext} 为末参。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation}+{@code @SingleSession} 事务，本类不带 {@code @Transactional}。
 */
public class ErpFinNotesPayableProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpFinCreditFacilityBiz creditFacilityBiz;

    @Inject
    NotesPostingDispatcher postingDispatcher;

    public ErpFinNotesPayable issue(Long notesId, IServiceContext context) {
        ErpFinNotesPayable note = requireNote(notesId, context);
        // 幂等：已签发再次签发为空操作。
        if (isAlreadyIssued(note)) {
            return note;
        }
        validateNotTerminal(note, context);
        requireAmountPositive(note, context);
        reserveCreditIfNeeded(note, context);
        return doIssue(notesId, note, context);
    }

    public ErpFinNotesPayable honor(Long notesId, IServiceContext context) {
        ErpFinNotesPayable note = requireNote(notesId, context);
        validateTransitionForHonor(note, context);
        releaseOccupiedCredit(note, context);
        return doHonor(notesId, note, context);
    }

    public ErpFinNotesPayable dishonor(Long notesId, IServiceContext context) {
        ErpFinNotesPayable note = requireNote(notesId, context);
        validateTransitionForHonor(note, context);
        releaseOccupiedCredit(note, context);
        doDishonor(note, context);
        return note;
    }

    public ErpFinNotesPayable writeOff(Long notesId, IServiceContext context) {
        ErpFinNotesPayable note = requireNote(notesId, context);
        validateNotTerminal(note, context);
        releaseOccupiedCredit(note, context);
        doWriteOff(note, context);
        return note;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForHonor(ErpFinNotesPayable note, IServiceContext context) {
        String status = note.getStatus();
        if (status == null || !Objects.equals(status, ErpFinConstants.NOTES_PAY_ISSUED)) {
            throw illegalTransition(note, status, "ISSUED");
        }
    }

    protected void validateNotTerminal(ErpFinNotesPayable note, IServiceContext context) {
        String status = note.getStatus();
        if (isTerminal(status)) {
            throw illegalTransition(note, status, "非终态");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void requireAmountPositive(ErpFinNotesPayable note, IServiceContext context) {
        BigDecimal amount = note.getAmountFunctional();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NopException(ErpFinErrors.ERR_NOTES_AMOUNT_INVALID)
                    .param(ErpFinErrors.ARG_NOTES_CODE, note.getCode());
        }
    }

    protected void reserveCreditIfNeeded(ErpFinNotesPayable note, IServiceContext context) {
        // 开银承时强一致校验授信可用额度并占用（treasury.md §规则1）。
        if (isBankAcceptance(note) && isCreditCheckOnIssue() && note.getCreditFacilityId() != null) {
            creditFacilityBiz.reserveCredit(note.getCreditFacilityId(), nz(note.getAmountFunctional()), context);
        }
    }

    protected void releaseOccupiedCredit(ErpFinNotesPayable note, IServiceContext context) {
        if (isBankAcceptance(note) && note.getCreditFacilityId() != null) {
            creditFacilityBiz.releaseCredit(note.getCreditFacilityId(), nz(note.getAmountFunctional()), context);
        }
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected ErpFinNotesPayable doIssue(Long notesId, ErpFinNotesPayable note, IServiceContext context) {
        note.setStatus(ErpFinConstants.NOTES_PAY_ISSUED);
        noteDao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostPayable(note, ErpFinBusinessType.NOTES_PAYABLE_ISSUED);
        note = reload(notesId);
        markPosted(note, posted);
        noteDao().updateEntity(note);
        return note;
    }

    protected ErpFinNotesPayable doHonor(Long notesId, ErpFinNotesPayable note, IServiceContext context) {
        note.setStatus(ErpFinConstants.NOTES_PAY_HONORED);
        noteDao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostPayable(note, ErpFinBusinessType.NOTES_PAYABLE_HONORED);
        note = reload(notesId);
        markPosted(note, posted);
        noteDao().updateEntity(note);
        return note;
    }

    protected void doDishonor(ErpFinNotesPayable note, IServiceContext context) {
        note.setStatus(ErpFinConstants.NOTES_PAY_DISHONORED);
        noteDao().updateEntity(note);
    }

    protected void doWriteOff(ErpFinNotesPayable note, IServiceContext context) {
        if (Boolean.TRUE.equals(note.getPosted())) {
            postingDispatcher.reversePayable(note, ErpFinBusinessType.NOTES_PAYABLE_ISSUED);
        }
        note.setStatus(ErpFinConstants.NOTES_PAY_WRITE_OFF);
        note.setPosted(false);
        note.setPostedAt(null);
        note.setPostedBy(null);
        noteDao().updateEntity(note);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpFinNotesPayable requireNote(Long notesId, IServiceContext context) {
        return requireNote(notesId);
    }

    protected ErpFinNotesPayable requireNote(Long notesId) {
        ErpFinNotesPayable note = noteDao().getEntityById(notesId);
        if (note == null) {
            throw new NopException(ErpFinErrors.ERR_NOTES_PAYABLE_NOT_FOUND)
                    .param(ErpFinErrors.ARG_NOTES_CODE, String.valueOf(notesId));
        }
        return note;
    }

    protected void markPosted(ErpFinNotesPayable note, boolean posted) {
        if (posted) {
            note.setPosted(true);
            note.setPostedAt(CoreMetrics.currentDateTime());
            note.setPostedBy(currentUserId());
        } else {
            note.setPosted(false);
        }
    }

    protected boolean isAlreadyIssued(ErpFinNotesPayable note) {
        String status = note.getStatus();
        return status != null && Objects.equals(status, ErpFinConstants.NOTES_PAY_ISSUED);
    }

    protected boolean isBankAcceptance(ErpFinNotesPayable note) {
        return note.getNotesType() != null && Objects.equals(note.getNotesType(), ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE);
    }

    protected boolean isCreditCheckOnIssue() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_CREDIT_CHECK_ON_ISSUE, Boolean.TRUE);
        return flag == null ? true : flag;
    }

    protected boolean isTerminal(String status) {
        return status != null
                && (Objects.equals(status, ErpFinConstants.NOTES_PAY_HONORED)
                || Objects.equals(status, ErpFinConstants.NOTES_PAY_DISHONORED)
                || Objects.equals(status, ErpFinConstants.NOTES_PAY_WRITE_OFF));
    }

    protected ErpFinNotesPayable reload(Long notesId) {
        return noteDao().getEntityById(notesId);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpFinNotesPayable> noteDao() {
        return daoProvider.daoFor(ErpFinNotesPayable.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) noteDao()).getOrmTemplate();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected NopException illegalTransition(ErpFinNotesPayable note, String current, String expected) {
        return new NopException(ErpFinErrors.ERR_NOTES_PAYABLE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_NOTES_CODE, note.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }
}
