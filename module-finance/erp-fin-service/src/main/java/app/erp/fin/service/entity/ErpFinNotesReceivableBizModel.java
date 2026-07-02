
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinNotesReceivableBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinNotesDiscount;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.NotesPostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 应收票据 BizModel（{@code treasury.md §状态机}）。承载 7 态状态机：
 * receive（→RECEIVED）→ discount（→DISCOUNTED）/endorse（→ENDORSED）/collect（→COLLECTION_PENDING）；
 * COLLECTION_PENDING→honor（→HONORED）/dishonor（→DISHONORED）；任何非终态→writeOff（→WRITE_OFF）。
 *
 * <p>状态迁移成功后经 {@link NotesPostingDispatcher} 触发业财过账（RECEIVED/DISCOUNTED/ENDORSED/COLLECTION 四类型），
 * posted 标志在过账成功后置位（失败吞异常保持原态+posted=false，对齐 0700-2 合约）。writeOff 对已过账票据红字冲销。
 *
 * <p>{@code @BizMutation} 自动包装事务（不叠加 {@code @Transactional}），每迁移校验前置态，
 * 违例抛 {@link NopException}+{@link ErpFinErrors} 作用域码。
 */
@BizModel("ErpFinNotesReceivable")
public class ErpFinNotesReceivableBizModel extends CrudBizModel<ErpFinNotesReceivable> implements IErpFinNotesReceivableBiz {

    @Inject
    NotesPostingDispatcher postingDispatcher;

    public ErpFinNotesReceivableBizModel() {
        setEntityName(ErpFinNotesReceivable.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesReceivable receive(@Name("notesId") Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        if (status != null && status == ErpFinConstants.NOTES_RECV_RECEIVED) {
            return note;
        }
        if (isTerminal(status)) {
            throw illegalTransition(note, status, "非终态");
        }
        requireAmountPositive(note);
        note.setStatus(ErpFinConstants.NOTES_RECV_RECEIVED);
        dao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostReceivable(note, ErpFinBusinessType.NOTES_RECEIVABLE_RECEIVED);
        note = requireNote(notesId, context);
        markPosted(note, posted);
        dao().updateEntity(note);
        return note;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesReceivable discount(@Name("notesId") Long notesId,
                                          @Name("discountDate") LocalDate discountDate,
                                          @Name("bankId") Long bankId,
                                          @Name("discountRate") BigDecimal discountRate,
                                          IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        if (status == null || status != ErpFinConstants.NOTES_RECV_RECEIVED) {
            throw illegalTransition(note, status, "RECEIVED");
        }
        if (discountDate == null || discountRate == null || bankId == null) {
            throw illegalTransition(note, status, "贴现日/贴现银行/贴现率非空");
        }
        BigDecimal faceAmount = nz(note.getAmountFunctional());
        long remainingDays = note.getDueDate() != null && discountDate.isBefore(note.getDueDate())
                ? ChronoUnit.DAYS.between(discountDate, note.getDueDate()) : 0L;
        // discountInterest = 票面 × 贴现率 × 剩余天数 / 360
        BigDecimal discountInterest = faceAmount
                .multiply(discountRate)
                .multiply(BigDecimal.valueOf(remainingDays))
                .divide(BigDecimal.valueOf(360), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = faceAmount.subtract(discountInterest);

        IEntityDao<ErpFinNotesDiscount> discountDao = daoProvider().daoFor(ErpFinNotesDiscount.class);
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
        discountDao.saveEntity(discount);

        note.setStatus(ErpFinConstants.NOTES_RECV_DISCOUNTED);
        note.setDiscountId(discount.getId());
        dao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostReceivable(note, ErpFinBusinessType.NOTES_RECEIVABLE_DISCOUNTED);
        note = requireNote(notesId, context);
        markPosted(note, posted);
        dao().updateEntity(note);
        return note;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesReceivable endorse(@Name("notesId") Long notesId,
                                         @Name("endorsementFromId") Long endorsementFromId,
                                         IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        if (status == null || status != ErpFinConstants.NOTES_RECV_RECEIVED) {
            throw illegalTransition(note, status, "RECEIVED");
        }
        if (endorsementFromId != null) {
            note.setEndorsementFromId(endorsementFromId);
        }
        note.setStatus(ErpFinConstants.NOTES_RECV_ENDORSED);
        dao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostReceivable(note, ErpFinBusinessType.NOTES_RECEIVABLE_ENDORSED);
        note = requireNote(notesId, context);
        markPosted(note, posted);
        dao().updateEntity(note);
        return note;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesReceivable collect(@Name("notesId") Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        // 托收：已收到或已贴现（贴现后票据仍归本方，到期仍需托收承兑）均可进入托收中。
        if (status == null
                || (status != ErpFinConstants.NOTES_RECV_RECEIVED
                && status != ErpFinConstants.NOTES_RECV_DISCOUNTED)) {
            throw illegalTransition(note, status, "RECEIVED 或 DISCOUNTED");
        }
        note.setStatus(ErpFinConstants.NOTES_RECV_COLLECTION_PENDING);
        dao().updateEntity(note);
        return note;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesReceivable honor(@Name("notesId") Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        if (status == null || status != ErpFinConstants.NOTES_RECV_COLLECTION_PENDING) {
            throw illegalTransition(note, status, "COLLECTION_PENDING");
        }
        note.setStatus(ErpFinConstants.NOTES_RECV_HONORED);
        dao().updateEntity(note);

        boolean posted = postingDispatcher.tryPostReceivable(note, ErpFinBusinessType.NOTES_RECEIVABLE_COLLECTION);
        note = requireNote(notesId, context);
        markPosted(note, posted);
        dao().updateEntity(note);
        return note;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesReceivable dishonor(@Name("notesId") Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        if (status == null || status != ErpFinConstants.NOTES_RECV_COLLECTION_PENDING) {
            throw illegalTransition(note, status, "COLLECTION_PENDING");
        }
        // 拒付转应收（treasury.md §规则3）：仅标记终态，转挂应收账款科目；后续催收/坏账属信用管理面 Non-Goal。
        note.setStatus(ErpFinConstants.NOTES_RECV_DISHONORED);
        dao().updateEntity(note);
        return note;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinNotesReceivable writeOff(@Name("notesId") Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = requireNote(notesId, context);
        Integer status = note.getStatus();
        // 任何非终态 → WRITE_OFF（终态：HONORED/DISHONORED/WRITE_OFF）。配置门控 erp-fin.notes-writeoff-approval-required
        // （treasury.md §规则4）：按 Non-Goal 不接入 nop-wf，WRITE_OFF 为直接状态迁移 + 配置门控记录。
        if (isTerminal(status)) {
            throw illegalTransition(note, status, "非终态");
        }
        if (Boolean.TRUE.equals(note.getPosted())) {
            ErpFinBusinessType postedType = businessTypeForStatus(status);
            if (postedType != null) {
                postingDispatcher.reverseReceivable(note, postedType);
            }
        }
        note.setStatus(ErpFinConstants.NOTES_RECV_WRITE_OFF);
        note.setPosted(false);
        note.setPostedAt(null);
        note.setPostedBy(null);
        dao().updateEntity(note);
        return note;
    }

    // ---------- helpers ----------

    /** 票据当前状态对应的最末过账业务类型（writeOff 红冲用）。COLLECTION_PENDING 无独立过账，回退到收到时的资产确认。 */
    private ErpFinBusinessType businessTypeForStatus(Integer status) {
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

    private void markPosted(ErpFinNotesReceivable note, boolean posted) {
        if (posted) {
            note.setPosted(true);
            note.setPostedAt(CoreMetrics.currentDateTime());
            note.setPostedBy(currentUserId());
        } else {
            note.setPosted(false);
        }
    }

    private ErpFinNotesReceivable requireNote(Long notesId, IServiceContext context) {
        return requireEntity(String.valueOf(notesId), null, context);
    }

    private boolean isTerminal(Integer status) {
        return status != null
                && (status == ErpFinConstants.NOTES_RECV_HONORED
                || status == ErpFinConstants.NOTES_RECV_DISHONORED
                || status == ErpFinConstants.NOTES_RECV_WRITE_OFF);
    }

    private void requireAmountPositive(ErpFinNotesReceivable note) {
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

    private NopException illegalTransition(ErpFinNotesReceivable note, Integer current, String expected) {
        return new NopException(ErpFinErrors.ERR_NOTES_RECEIVABLE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_NOTES_CODE, note.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }
}
