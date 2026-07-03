package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinNotesPayableBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinCreditFacility;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应付票据状态机 + 授信强一致校验单测（Phase 2）。验证 issue→honor/dishonor/writeOff 迁移、
 * 银承开出占用授信额度（usedAmount↑）、兑付/注销释放额度（usedAmount↓）、可用额度不足拒绝开票。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinNotesPayableStateMachine extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinNotesPayableBiz notesPayableBiz;

    @Test
    public void testIssueCommercialAcceptanceNoCreditCheck() {
        // 商业承兑不占用授信额度，直接开出。
        Long noteId = ormTemplate.runInSession(s -> {
            seedBase();
            return seedPayable("NP-001",
                    ErpFinConstants.NOTES_TYPE_COMMERCIAL_ACCEPTANCE, null, new BigDecimal("5000"));
        });
        ErpFinNotesPayable note = notesPayableBiz.issue(noteId, CTX);
        assertEquals(ErpFinConstants.NOTES_PAY_ISSUED, note.getStatus());
    }

    @Test
    public void testIssueBankAcceptanceOccupiesCredit() {
        Long facilityId = ormTemplate.runInSession(s -> { seedBase(); return seedCreditFacility("CF-001", new BigDecimal("1000"), BigDecimal.ZERO); });
        Long noteId = ormTemplate.runInSession(s -> seedPayable("NP-002",
                ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE, facilityId, new BigDecimal("500")));

        ErpFinNotesPayable note = notesPayableBiz.issue(noteId, CTX);
        assertEquals(ErpFinConstants.NOTES_PAY_ISSUED, note.getStatus());

        ErpFinCreditFacility facility = reloadFacility(facilityId);
        assertEquals(0, new BigDecimal("500").compareTo(facility.getUsedAmount()), "占用 500");
        assertEquals(0, new BigDecimal("500").compareTo(facility.getAvailableAmount()), "可用 500");
    }

    @Test
    public void testIssueBankAcceptanceInsufficientCreditRejected() {
        // 总额 1000，已用 800，可用 200 < 票面 500 → 拒绝开票（credit 校验先于过账，无需 seedBase）。
        Long facilityId = ormTemplate.runInSession(s -> seedCreditFacility("CF-002", new BigDecimal("1000"), new BigDecimal("800")));
        Long noteId = ormTemplate.runInSession(s -> seedPayable("NP-003",
                ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE, facilityId, new BigDecimal("500")));

        NopException ex = assertThrows(NopException.class, () -> notesPayableBiz.issue(noteId, CTX));
        assertEquals(ErpFinErrors.ERR_CREDIT_FACILITY_INSUFFICIENT.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testHonorReleasesCredit() {
        Long facilityId = ormTemplate.runInSession(s -> { seedBase(); return seedCreditFacility("CF-003", new BigDecimal("1000"), BigDecimal.ZERO); });
        Long noteId = ormTemplate.runInSession(s -> seedPayable("NP-004",
                ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE, facilityId, new BigDecimal("500")));

        notesPayableBiz.issue(noteId, CTX);
        notesPayableBiz.honor(noteId, CTX);

        ErpFinCreditFacility facility = reloadFacility(facilityId);
        assertEquals(0, BigDecimal.ZERO.compareTo(facility.getUsedAmount()), "兑付释放后已用归 0");
    }

    @Test
    public void testWriteOffReleasesCredit() {
        Long facilityId = ormTemplate.runInSession(s -> { seedBase(); return seedCreditFacility("CF-004", new BigDecimal("1000"), BigDecimal.ZERO); });
        Long noteId = ormTemplate.runInSession(s -> seedPayable("NP-005",
                ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE, facilityId, new BigDecimal("500")));

        notesPayableBiz.issue(noteId, CTX);
        notesPayableBiz.writeOff(noteId, CTX);

        ErpFinCreditFacility facility = reloadFacility(facilityId);
        assertEquals(0, BigDecimal.ZERO.compareTo(facility.getUsedAmount()), "注销释放后已用归 0");
    }

    @Test
    public void testIllegalTransitionHonorFromWriteOff() {
        Long noteId = ormTemplate.runInSession(s -> seedPayable("NP-006",
                ErpFinConstants.NOTES_TYPE_COMMERCIAL_ACCEPTANCE, null, new BigDecimal("500"),
                ErpFinConstants.NOTES_PAY_WRITE_OFF));
        assertThrows(NopException.class, () -> notesPayableBiz.honor(noteId, CTX));
    }

    // ---------- seed helpers ----------

    private void seedBase() {
        seedOpenPeriod("2026-07", 2026, 7, java.time.LocalDate.of(2026, 7, 1), java.time.LocalDate.of(2026, 7, 31));
        seedAcctSchema(1L);
        seedSubject("2202", "应付账款");
        seedSubject("2203", "应付票据");
        seedSubject("1002", "银行存款");
    }

    private Long seedPayable(String code, String notesType, Long creditFacilityId, BigDecimal amountFunctional) {
        return seedPayable(code, notesType, creditFacilityId, amountFunctional, null);
    }

    private Long seedPayable(String code, String notesType, Long creditFacilityId, BigDecimal amountFunctional, String status) {
        IEntityDao<ErpFinNotesPayable> dao = daoProvider.daoFor(ErpFinNotesPayable.class);
        ErpFinNotesPayable note = new ErpFinNotesPayable();
        note.setCode(code);
        note.setOrgId(1L);
        note.setNotesType(notesType);
        note.setNotesNo("N-" + code);
        note.setCurrencyId(1L);
        note.setExchangeRate(BigDecimal.ONE);
        note.setAmountFunctional(amountFunctional);
        note.setAmountSource(amountFunctional);
        note.setCreditFacilityId(creditFacilityId);
        note.setStatus(status);
        note.setPosted(false);
        dao.saveEntity(note);
        return note.getId();
    }

    private Long seedCreditFacility(String code, BigDecimal total, BigDecimal used) {
        IEntityDao<ErpFinCreditFacility> dao = daoProvider.daoFor(ErpFinCreditFacility.class);
        ErpFinCreditFacility facility = new ErpFinCreditFacility();
        facility.setCode(code);
        facility.setOrgId(1L);
        facility.setFacilityType("BANK_ACCEPTANCE_LINE");
        facility.setTotalAmount(total);
        facility.setUsedAmount(used);
        facility.setAvailableAmount(total.subtract(used));
        facility.setStatus("ACTIVE");
        dao.saveEntity(facility);
        return facility.getId();
    }

    private ErpFinCreditFacility reloadFacility(Long facilityId) {
        IEntityDao<ErpFinCreditFacility> dao = daoProvider.daoFor(ErpFinCreditFacility.class);
        return dao.getEntityById(facilityId);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, java.time.LocalDate start, java.time.LocalDate end) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }
}
