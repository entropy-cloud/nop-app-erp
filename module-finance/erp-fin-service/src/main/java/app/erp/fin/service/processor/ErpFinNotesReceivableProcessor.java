package app.erp.fin.service.processor;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinNotesDiscount;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.NotesPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 应收票据状态机编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * Facade {@code ErpFinNotesReceivableBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>配置余地：每个 {@code public} 动作方法只编排步骤顺序，各步骤为 {@code protected} 方法、单一职责、
 * 以 {@link IServiceContext} 为末参。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation}+{@code @SingleSession} 事务，本类不带 {@code @Transactional}。
 */
public class ErpFinNotesReceivableProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    NotesPostingDispatcher postingDispatcher;

    public ErpFinNotesReceivable receive(Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        // 幂等：已收到再次收到为空操作。
        if (isAlreadyReceived(note)) {
            return note;
        }
        validateNotTerminal(note, context);
        requireAmountPositive(note, context);
        return doReceive(notesId, note, context);
    }

    public ErpFinNotesReceivable discount(Long notesId, LocalDate discountDate, Long bankId,
                                          BigDecimal discountRate, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        validateTransitionForDiscount(note, context);
        requireDiscountInputs(note, discountDate, bankId, discountRate, context);
        ErpFinNotesDiscount discount = buildDiscount(note, discountDate, bankId, discountRate);
        return doDiscount(notesId, note, discount, context);
    }

    public ErpFinNotesReceivable endorse(Long notesId, Long endorsementFromId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        validateTransitionForEndorse(note, context);
        return doEndorse(notesId, note, endorsementFromId, context);
    }

    public ErpFinNotesReceivable collect(Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        validateTransitionForCollect(note, context);
        note.setStatus(ErpFinConstants.NOTES_RECV_COLLECTION_PENDING);
        noteDao().updateEntity(note);
        return note;
    }

    public ErpFinNotesReceivable honor(Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        validateTransitionForHonorOrDishonor(note, context);
        return doHonor(notesId, note, context);
    }

    public ErpFinNotesReceivable dishonor(Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        validateTransitionForHonorOrDishonor(note, context);
        // 拒付转应收（treasury.md §规则3）：仅标记终态，转挂应收账款科目；后续催收/坏账属信用管理面 Non-Goal。
        note.setStatus(ErpFinConstants.NOTES_RECV_DISHONORED);
        noteDao().updateEntity(note);
        return note;
    }

    public ErpFinNotesReceivable writeOff(Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        validateNotTerminal(note, context);
        doWriteOff(note, context);
        return note;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForDiscount(ErpFinNotesReceivable note, IServiceContext context) {
        Integer status = note.getStatus();
        if (status == null || status != ErpFinConstants.NOTES_RECV_RECEIVED) {
            throw illegalTransition(note, status, "RECEIVED");
        }
    }

    protected void validateTransitionForEndorse(ErpFinNotesReceivable note, IServiceContext context) {
        Integer status = note.getStatus();
        if (status == null || status != ErpFinConstants.NOTES_RECV_RECEIVED) {
            throw illegalTransition(note, status, "RECEIVED");
        }
    }

    protected void validateTransitionForCollect(ErpFinNotesReceivable note, IServiceContext context) {
        Integer status = note.getStatus();
        // 托收：已收到或已贴现（贴现后票据仍归本方，到期仍需托收承兑）均可进入托收中。
        if (status == null
                || (status != ErpFinConstants.NOTES_RECV_RECEIVED
                && status != ErpFinConstants.NOTES_RECV_DISCOUNTED)) {
            throw illegalTransition(note, status, "RECEIVED 或 DISCOUNTED");
        }
    }

    protected void validateTransitionForHonorOrDishonor(ErpFinNotesReceivable note, IServiceContext context) {
        Integer status = note.getStatus();
        if (status == null || status != ErpFinConstants.NOTES_RECV_COLLECTION_PENDING) {
            throw illegalTransition(note, status, "COLLECTION_PENDING");
        }
    }

    protected void validateNotTerminal(ErpFinNotesReceivable note, IServiceContext context) {
        Integer status = note.getStatus();
        if (isTerminal(status)) {
            throw illegalTransition(note, status, "非终态");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void requireAmountPositive(ErpFinNotesReceivable note, IServiceContext context) {
        BigDecimal amount = note.getAmountFunctional();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NopException(ErpFinErrors.ERR_NOTES_AMOUNT_INVALID)
                    .param(ErpFinErrors.ARG_NOTES_CODE, note.getCode());
        }
    }

    protected void requireDiscountInputs(ErpFinNotesReceivable note, LocalDate discountDate, Long bankId,
                                          BigDecimal discountRate, IServiceContext context) {
        if (discountDate == null || discountRate == null || bankId == null) {
            throw illegalTransition(note, note.getStatus(), "贴现日/贴现银行/贴现率非空");
        }
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected ErpFinNotesReceivable doReceive(Long notesId, ErpFinNotesReceivable note, IServiceContext context) {
        note.setStatus(ErpFinConstants.NOTES_RECV_RECEIVED);
        noteDao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostReceivable(note, ErpFinBusinessType.NOTES_RECEIVABLE_RECEIVED);
        note = reload(notesId);
        markPosted(note, posted);
        noteDao().updateEntity(note);
        return note;
    }

    protected ErpFinNotesReceivable doDiscount(Long notesId, ErpFinNotesReceivable note, ErpFinNotesDiscount discount,
                                               IServiceContext context) {
        IEntityDao<ErpFinNotesDiscount> discountDao = daoProvider.daoFor(ErpFinNotesDiscount.class);
        discountDao.saveEntity(discount);

        note.setStatus(ErpFinConstants.NOTES_RECV_DISCOUNTED);
        note.setDiscountId(discount.getId());
        noteDao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostReceivable(note, ErpFinBusinessType.NOTES_RECEIVABLE_DISCOUNTED);
        note = reload(notesId);
        markPosted(note, posted);
        noteDao().updateEntity(note);
        return note;
    }

    protected ErpFinNotesReceivable doEndorse(Long notesId, ErpFinNotesReceivable note, Long endorsementFromId,
                                              IServiceContext context) {
        if (endorsementFromId != null) {
            note.setEndorsementFromId(endorsementFromId);
        }
        note.setStatus(ErpFinConstants.NOTES_RECV_ENDORSED);
        noteDao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostReceivable(note, ErpFinBusinessType.NOTES_RECEIVABLE_ENDORSED);
        note = reload(notesId);
        markPosted(note, posted);
        noteDao().updateEntity(note);
        return note;
    }

    protected ErpFinNotesReceivable doHonor(Long notesId, ErpFinNotesReceivable note, IServiceContext context) {
        note.setStatus(ErpFinConstants.NOTES_RECV_HONORED);
        noteDao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostReceivable(note, ErpFinBusinessType.NOTES_RECEIVABLE_COLLECTION);
        note = reload(notesId);
        markPosted(note, posted);
        noteDao().updateEntity(note);
        return note;
    }

    protected void doWriteOff(ErpFinNotesReceivable note, IServiceContext context) {
        if (Boolean.TRUE.equals(note.getPosted())) {
            ErpFinBusinessType postedType = businessTypeForStatus(note.getStatus());
            if (postedType != null) {
                postingDispatcher.reverseReceivable(note, postedType);
            }
        }
        note.setStatus(ErpFinConstants.NOTES_RECV_WRITE_OFF);
        note.setPosted(false);
        note.setPostedAt(null);
        note.setPostedBy(null);
        noteDao().updateEntity(note);
    }

    // ---------- 实体构造（贴现计算） ----------

    protected ErpFinNotesDiscount buildDiscount(ErpFinNotesReceivable note, LocalDate discountDate, Long bankId,
                                                  BigDecimal discountRate) {
        BigDecimal faceAmount = nz(note.getAmountFunctional());
        long remainingDays = note.getDueDate() != null && discountDate.isBefore(note.getDueDate())
                ? ChronoUnit.DAYS.between(discountDate, note.getDueDate()) : 0L;
        // discountInterest = 票面 × 贴现率 × 剩余天数 / 360
        BigDecimal discountInterest = faceAmount
                .multiply(discountRate)
                .multiply(BigDecimal.valueOf(remainingDays))
                .divide(BigDecimal.valueOf(360), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = faceAmount.subtract(discountInterest);

        IEntityDao<ErpFinNotesDiscount> discountDao = daoProvider.daoFor(ErpFinNotesDiscount.class);
        ErpFinNotesDiscount discount = discountDao.newEntity();
        discount.setNotesReceivableId(note.getId());
        discount.setOrgId(note.getOrgId());
        discount.setDiscountDate(discountDate);
        discount.setBankId(bankId);
        discount.setFaceAmount(faceAmount);
        discount.setDiscountInterest(discountInterest);
        discount.setNetAmount(netAmount);
        discount.setCurrencyId(note.getCurrencyId());
        discount.setExchangeRate(note.getExchangeRate() != null ? note.getExchangeRate() : BigDecimal.ONE);
        discount.setExchangeGainLoss(BigDecimal.ZERO);
        return discount;
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    /** 票据当前状态对应的最末过账业务类型（writeOff 红冲用）。COLLECTION_PENDING 无独立过账，回退到收到时的资产确认。 */
    protected ErpFinBusinessType businessTypeForStatus(Integer status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case ErpFinConstants.NOTES_RECV_RECEIVED:
            case ErpFinConstants.NOTES_RECV_COLLECTION_PENDING:
                return ErpFinBusinessType.NOTES_RECEIVABLE_RECEIVED;
            case ErpFinConstants.NOTES_RECV_DISCOUNTED:
                return ErpFinBusinessType.NOTES_RECEIVABLE_DISCOUNTED;
            case ErpFinConstants.NOTES_RECV_ENDORSED:
                return ErpFinBusinessType.NOTES_RECEIVABLE_ENDORSED;
            default:
                return null;
        }
    }

    protected ErpFinNotesReceivable requireNote(Long notesId, IServiceContext context) {
        return requireNote(notesId);
    }

    protected ErpFinNotesReceivable requireNote(Long notesId) {
        ErpFinNotesReceivable note = noteDao().getEntityById(notesId);
        if (note == null) {
            throw new NopException(ErpFinErrors.ERR_NOTES_RECEIVABLE_NOT_FOUND)
                    .param(ErpFinErrors.ARG_NOTES_CODE, String.valueOf(notesId));
        }
        return note;
    }

    protected void markPosted(ErpFinNotesReceivable note, boolean posted) {
        if (posted) {
            note.setPosted(true);
            note.setPostedAt(CoreMetrics.currentDateTime());
            note.setPostedBy(currentUserId());
        } else {
            note.setPosted(false);
        }
    }

    protected boolean isAlreadyReceived(ErpFinNotesReceivable note) {
        Integer status = note.getStatus();
        return status != null && status == ErpFinConstants.NOTES_RECV_RECEIVED;
    }

    protected boolean isTerminal(Integer status) {
        return status != null
                && (status == ErpFinConstants.NOTES_RECV_HONORED
                || status == ErpFinConstants.NOTES_RECV_DISHONORED
                || status == ErpFinConstants.NOTES_RECV_WRITE_OFF);
    }

    protected ErpFinNotesReceivable reload(Long notesId) {
        return noteDao().getEntityById(notesId);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpFinNotesReceivable> noteDao() {
        return daoProvider.daoFor(ErpFinNotesReceivable.class);
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

    protected NopException illegalTransition(ErpFinNotesReceivable note, Integer current, String expected) {
        return new NopException(ErpFinErrors.ERR_NOTES_RECEIVABLE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_NOTES_CODE, note.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }
}
