package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinNotesReceivableBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinNotesDiscount;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应收票据 7 态状态机单测（Phase 2）。验证 receive/discount/endorse/collect/honor/dishonor/writeOff
 * 正向迁移、贴现计算（discountInterest=票面×贴现率×剩余天数/360、netAmount=票面−贴现息）、非法迁移抛异常。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinNotesReceivableStateMachine extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinNotesReceivableBiz notesBiz;

    @Test
    public void testReceive() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-001", null, new BigDecimal("10000")); });
        ErpFinNotesReceivable note = notesBiz.receive(noteId, CTX);
        assertEquals(ErpFinConstants.NOTES_RECV_RECEIVED, note.getStatus());
    }

    @Test
    public void testDiscountComputesInterestAndNetAmount() {
        // faceAmount=36000, rate=6%, remainingDays=60 → discountInterest=36000×0.06×60/360=360.00, net=35640.00
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            return seedReceivable("NR-002", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("36000"),
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 30));
        });

        ErpFinNotesReceivable note = notesBiz.discount(noteId, LocalDate.of(2026, 7, 1), 9001L, new BigDecimal("0.06"), CTX);
        assertEquals(ErpFinConstants.NOTES_RECV_DISCOUNTED, note.getStatus());

        ErpFinNotesDiscount discount = findDiscount(note.getId());
        assertEquals(0, new BigDecimal("360.00").compareTo(discount.getDiscountInterest()), "贴现息=360.00");
        assertEquals(0, new BigDecimal("35640.00").compareTo(discount.getNetAmount()), "实得=35640.00");
        assertEquals(note.getDiscountId(), discount.getId());
    }

    @Test
    public void testEndorse() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-003", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("5000")); });
        ErpFinNotesReceivable note = notesBiz.endorse(noteId, null, CTX);
        assertEquals(ErpFinConstants.NOTES_RECV_ENDORSED, note.getStatus());
    }

    @Test
    public void testCollectFromReceived() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-004", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("5000")); });
        ErpFinNotesReceivable note = notesBiz.collect(noteId, CTX);
        assertEquals(ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, note.getStatus());
    }

    @Test
    public void testCollectFromDiscounted() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-005", ErpFinConstants.NOTES_RECV_DISCOUNTED, new BigDecimal("5000")); });
        ErpFinNotesReceivable note = notesBiz.collect(noteId, CTX);
        assertEquals(ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, note.getStatus());
    }

    @Test
    public void testHonorAndDishonor() {
        Long honoredId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-006", ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, new BigDecimal("5000")); });
        assertEquals(ErpFinConstants.NOTES_RECV_HONORED, notesBiz.honor(honoredId, CTX).getStatus());

        Long dishonoredId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-007", ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, new BigDecimal("5000")); });
        assertEquals(ErpFinConstants.NOTES_RECV_DISHONORED, notesBiz.dishonor(dishonoredId, CTX).getStatus());
    }

    @Test
    public void testWriteOffFromReceived() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-008", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("5000")); });
        assertEquals(ErpFinConstants.NOTES_RECV_WRITE_OFF, notesBiz.writeOff(noteId, CTX).getStatus());
    }

    @Test
    public void testIllegalTransitionHonorFromReceived() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-009", ErpFinConstants.NOTES_RECV_RECEIVED, new BigDecimal("5000")); });
        // 承兑须从托收中迁入，从 RECEIVED 直接承兑为非法迁移。
        assertThrows(NopException.class, () -> notesBiz.honor(noteId, CTX));
    }

    @Test
    public void testIllegalTransitionWriteOffFromTerminal() {
        Long noteId = ormTemplate.runInSession(s -> { seedBase(); return seedReceivable("NR-010", ErpFinConstants.NOTES_RECV_HONORED, new BigDecimal("5000")); });
        // HONORED 为终态，不可再注销。
        assertThrows(NopException.class, () -> notesBiz.writeOff(noteId, CTX));
    }

    // ---------- seed helpers ----------

    private void seedBase() {
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        seedAcctSchema(1L);
        // 状态机迁移会触发业财过账（receive/discount/endorse/honor），预置全部相关科目使过账成功。
        seedSubject("1121", "应收票据");
        seedSubject("1122", "应收账款");
        seedSubject("2202", "应付账款");
        seedSubject("1002", "银行存款");
        seedSubject("6603", "财务费用-利息支出");
    }

    private Long seedReceivable(String code, Integer status, BigDecimal amountFunctional) {
        return seedReceivable(code, status, amountFunctional, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));
    }

    private Long seedReceivable(String code, Integer status, BigDecimal amountFunctional,
                                LocalDate issueDate, LocalDate dueDate) {
        IEntityDao<ErpFinNotesReceivable> dao = daoProvider.daoFor(ErpFinNotesReceivable.class);
        ErpFinNotesReceivable note = new ErpFinNotesReceivable();
        note.setCode(code);
        note.setOrgId(1L);
        note.setNotesType(ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE);
        note.setNotesNo("N-" + code);
        note.setIssueDate(issueDate);
        note.setDueDate(dueDate);
        note.setCurrencyId(1L);
        note.setExchangeRate(BigDecimal.ONE);
        note.setAmountFunctional(amountFunctional);
        note.setAmountSource(amountFunctional);
        note.setPartnerId(7000L);
        note.setStatus(status);
        note.setPosted(false);
        dao.saveEntity(note);
        return note.getId();
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(10);
        subject.setDirection(10);
        subject.setStatus(10);
        dao.saveEntity(subject);
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature(10);
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus(10);
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(10);
        dao.saveEntity(period);
    }

    private ErpFinNotesDiscount findDiscount(Long notesReceivableId) {
        IEntityDao<ErpFinNotesDiscount> dao = daoProvider.daoFor(ErpFinNotesDiscount.class);
        return dao.findAllByQuery(new io.nop.api.core.beans.query.QueryBean().addFilter(
                io.nop.api.core.beans.FilterBeans.eq("notesReceivableId", notesReceivableId))).get(0);
    }
}
