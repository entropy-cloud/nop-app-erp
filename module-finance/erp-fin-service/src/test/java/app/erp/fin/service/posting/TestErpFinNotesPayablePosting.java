package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinNotesPayableBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinCreditFacility;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应付票据业财过账端到端单测（Phase 3）。验证：开出→NOTES_PAYABLE_ISSUED 凭证 + 授信占用；
 * 兑付→NOTES_PAYABLE_HONORED 凭证 + 授信释放。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinNotesPayablePosting extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinNotesPayableBiz notesPayableBiz;

    @Test
    public void testIssuePostsAndOccupiesCredit() {
        long partnerId = 6601L;
        Long[] ids = ormTemplate.runInSession(s -> {
            seedBase();
            seedSubject("2202", "应付账款");
            seedSubject("2203", "应付票据");
            Long facilityId = seedCreditFacility("CF-POST-001", new BigDecimal("2000"));
            Long noteId = seedPayable("NP-POST-001", ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE,
                    facilityId, new BigDecimal("800"), partnerId);
            return new Long[]{facilityId, noteId};
        });
        Long facilityId = ids[0];
        Long noteId = ids[1];

        ErpFinNotesPayable note = ormTemplate.runInSession(session -> notesPayableBiz.issue(noteId, CTX));
        assertTrue(Boolean.TRUE.equals(note.getPosted()), "开出过账成功 posted=true");
        assertFalse(findBillLinks("NP-POST-001", ErpFinBusinessType.NOTES_PAYABLE_ISSUED.name()).isEmpty(), "ISSUED 凭证回链已落库");

        ErpFinCreditFacility facility = reloadFacility(facilityId);
        assertEquals(0, new BigDecimal("800").compareTo(facility.getUsedAmount()), "授信占用 800");
    }

    @Test
    public void testHonorPostsAndReleasesCredit() {
        long partnerId = 6602L;
        Long[] ids = ormTemplate.runInSession(s -> {
            seedBase();
            seedSubject("2202", "应付账款");
            seedSubject("2203", "应付票据");
            seedSubject("1002", "银行存款");
            Long facilityId = seedCreditFacility("CF-POST-002", new BigDecimal("2000"));
            Long noteId = seedPayable("NP-POST-002", ErpFinConstants.NOTES_TYPE_BANK_ACCEPTANCE,
                    facilityId, new BigDecimal("800"), partnerId);
            return new Long[]{facilityId, noteId};
        });
        Long facilityId = ids[0];
        Long noteId = ids[1];

        ormTemplate.runInSession(() -> notesPayableBiz.issue(noteId, CTX));
        ErpFinNotesPayable note = ormTemplate.runInSession(session -> notesPayableBiz.honor(noteId, CTX));
        assertTrue(Boolean.TRUE.equals(note.getPosted()), "兑付过账成功 posted=true");
        assertFalse(findBillLinks("NP-POST-002", ErpFinBusinessType.NOTES_PAYABLE_HONORED.name()).isEmpty(), "HONORED 凭证回链已落库");

        ErpFinCreditFacility facility = reloadFacility(facilityId);
        assertEquals(0, BigDecimal.ZERO.compareTo(facility.getUsedAmount()), "兑付释放后授信已用归 0");
    }

    // ---------- seed helpers ----------

    private void seedBase() {
        seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        seedAcctSchema(1L);
    }

    private Long seedPayable(String code, String notesType, Long creditFacilityId, BigDecimal amountFunctional, Long partnerId) {
        IEntityDao<ErpFinNotesPayable> dao = daoProvider.daoFor(ErpFinNotesPayable.class);
        ErpFinNotesPayable note = new ErpFinNotesPayable();
        note.setCode(code);
        note.setOrgId(1L);
        note.setNotesType(notesType);
        note.setNotesNo("N-" + code);
        note.setIssueDate(LocalDate.of(2026, 7, 1));
        note.setDueDate(LocalDate.of(2026, 7, 25));
        note.setCurrencyId(1L);
        note.setExchangeRate(BigDecimal.ONE);
        note.setAmountFunctional(amountFunctional);
        note.setAmountSource(amountFunctional);
        note.setPartnerId(partnerId);
        note.setCreditFacilityId(creditFacilityId);
        note.setPosted(false);
        dao.saveEntity(note);
        return note.getId();
    }

    private Long seedCreditFacility(String code, BigDecimal total) {
        IEntityDao<ErpFinCreditFacility> dao = daoProvider.daoFor(ErpFinCreditFacility.class);
        ErpFinCreditFacility facility = new ErpFinCreditFacility();
        facility.setCode(code);
        facility.setOrgId(1L);
        facility.setFacilityType("BANK_ACCEPTANCE_LINE");
        facility.setTotalAmount(total);
        facility.setUsedAmount(BigDecimal.ZERO);
        facility.setAvailableAmount(total);
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
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }
}
