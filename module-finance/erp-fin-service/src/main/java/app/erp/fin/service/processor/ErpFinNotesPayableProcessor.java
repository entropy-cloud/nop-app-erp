package app.erp.fin.service.processor;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.biz.IErpFinCreditFacilityBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.NotesPostingDispatcher;
import app.erp.fin.service.statemachine.ErpFinNotesPayableStateMachine;
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
 * <p>事务边界：跟随 Facade {@code @BizMutation} 事务，本类不带 {@code @Transactional}。
 */
public class ErpFinNotesPayableProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpFinCreditFacilityBiz creditFacilityBiz;

    @Inject
    NotesPostingDispatcher postingDispatcher;

    @Inject
    ErpFinNotesPayableStateMachine stateMachine;

    // ---------- step：迁移校验（protected，下游可逐个覆盖；固定来源态矩阵判断已迁移至
    //           ErpFinNotesPayableStateMachine Bean，本层仅作 common→领域码映射 wrapper） ----------

    protected void validateTransitionForHonor(ErpFinNotesPayable note, IServiceContext context) {
        assertTransition(note, () -> stateMachine.assertCanHonor(note.getStatus()));
    }

    protected void validateTransitionForDishonor(ErpFinNotesPayable note, IServiceContext context) {
        assertTransition(note, () -> stateMachine.assertCanDishonor(note.getStatus()));
    }

    /** issue 入口守卫（有意收窄：assertCanIssue 仅接受 null initial 写入 / ISSUED 幂等）。 */
    protected void validateTransitionForIssue(ErpFinNotesPayable note, IServiceContext context) {
        assertTransition(note, () -> stateMachine.assertCanIssue(note.getStatus()));
    }

    /** writeOff 入口守卫（loose：assertCanWriteOff 校验非终态）。 */
    protected void validateTransitionForWriteOff(ErpFinNotesPayable note, IServiceContext context) {
        assertTransition(note, () -> stateMachine.assertCanWriteOff(note.getStatus()));
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

    protected ErpFinNotesPayable doIssue(String notesId, ErpFinNotesPayable note, IServiceContext context) {
        note.setStatus(stateMachine.issueTargetStatus());
        noteDao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostPayable(note, ErpFinBusinessType.NOTES_PAYABLE_ISSUED);
        note = reload(notesId);
        markPosted(note, posted);
        noteDao().updateEntity(note);
        return note;
    }

    protected ErpFinNotesPayable doHonor(String notesId, ErpFinNotesPayable note, IServiceContext context) {
        note.setStatus(stateMachine.honorTargetStatus());
        noteDao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostPayable(note, ErpFinBusinessType.NOTES_PAYABLE_HONORED);
        note = reload(notesId);
        markPosted(note, posted);
        noteDao().updateEntity(note);
        return note;
    }

    protected void doDishonor(ErpFinNotesPayable note, IServiceContext context) {
        note.setStatus(stateMachine.dishonorTargetStatus());
        noteDao().updateEntity(note);
    }

    protected void doWriteOff(ErpFinNotesPayable note, IServiceContext context) {
        if (Boolean.TRUE.equals(note.getPosted())) {
            postingDispatcher.reversePayable(note, ErpFinBusinessType.NOTES_PAYABLE_ISSUED);
        }
        note.setStatus(stateMachine.writeOffTargetStatus());
        note.setPosted(false);
        note.setPostedAt(null);
        note.setPostedBy(null);
        noteDao().updateEntity(note);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpFinNotesPayable requireNote(String notesId, IServiceContext context) {
        return requireNote(notesId);
    }

    protected ErpFinNotesPayable requireNote(String notesId) {
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
            note.setPostedAt(CoreMetrics.currentTimestamp());
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

    protected ErpFinNotesPayable reload(String notesId) {
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

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * Bean common 码 → 领域码映射（common 作 cause 保留，契约 §7）。
     * 参数从 Bean 异常提取（currentStatus/expectedStatus 单真相源在 Bean），notesCode 由本层组装。
     */
    protected NopException illegalTransition(ErpFinNotesPayable note, NopException common) {
        return new NopException(ErpFinErrors.ERR_NOTES_PAYABLE_ILLEGAL_STATUS_TRANSITION, common)
                .param(ErpFinErrors.ARG_NOTES_CODE, note.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, common.getParam(ErpCommonErrors.ARG_CURRENT_STATUS))
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, common.getParam(ErpCommonErrors.ARG_EXPECTED_STATUS));
    }

    private void assertTransition(ErpFinNotesPayable note, Runnable assertion) {
        try {
            assertion.run();
        } catch (NopException e) {
            throw illegalTransition(note, e);
        }
    }
}
