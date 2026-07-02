package app.erp.fin.service.posting;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdEmployee;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 员工借款单业财过账端到端单测（Phase 3）。验证 APPROVED→EMPLOYEE_ADVANCE 凭证落库（借其他应收-员工预支/贷银行）
 * + DIRECTION_RECEIVABLE 辅助账（partnerId=employee.partnerId）+ posted=true；reverseApprove→红冲 + 辅助账 CANCELLED。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinEmployeeAdvancePosting extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinEmployeeAdvanceBiz advanceBiz;

    @Test
    public void testApprovePostsAndGeneratesReceivableSubledger() {
        long partnerId = 8801L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, 10);
            return seedAdvance("ADV-POST-001", empId, new BigDecimal("500"));
        });

        ErpFinEmployeeAdvance advance = advanceBiz.approve(advanceId, CTX);
        assertTrue(Boolean.TRUE.equals(advance.getPosted()), "过账成功 posted=true");

        ErpFinArApItem item = findItem("EMPLOYEE_ADVANCE", "ADV-POST-001");
        assertEquals(ErpFinConstants.DIRECTION_RECEIVABLE, item.getDirection(), "方向=应收");
        assertEquals(partnerId, item.getPartnerId(), "partnerId = employee.partnerId");
        assertEquals(0, item.getAmountFunctional().compareTo(new BigDecimal("500")));
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, item.getStatus());
    }

    @Test
    public void testReverseApproveCancelsSubledger() {
        long partnerId = 8802L;
        Long advanceId = ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-06", 2026, 6, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            seedAcctSchema(1L);
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, 10);
            return seedAdvance("ADV-POST-002", empId, new BigDecimal("300"));
        });

        advanceBiz.approve(advanceId, CTX);
        ErpFinEmployeeAdvance advance = advanceBiz.reverseApprove(advanceId, CTX);
        assertTrue(!Boolean.TRUE.equals(advance.getPosted()), "反审核后 posted=false");

        ErpFinArApItem item = findItem("EMPLOYEE_ADVANCE", "ADV-POST-002");
        assertEquals(ErpFinConstants.AR_AP_STATUS_CANCELLED, item.getStatus());
    }

    // ---------- seed helpers ----------

    private Long seedAdvance(String code, Long employeeId, BigDecimal amount) {
        IEntityDao<ErpFinEmployeeAdvance> dao = daoProvider.daoFor(ErpFinEmployeeAdvance.class);
        ErpFinEmployeeAdvance advance = new ErpFinEmployeeAdvance();
        advance.setCode(code);
        advance.setOrgId(1L);
        advance.setEmployeeId(employeeId);
        advance.setAdvanceType(10);
        advance.setBusinessDate(LocalDate.of(2026, 6, 10));
        advance.setCurrencyId(1L);
        advance.setExchangeRate(BigDecimal.ONE);
        advance.setAmountFunctional(amount);
        advance.setAmountSource(amount);
        advance.setSettledAmount(BigDecimal.ZERO);
        advance.setOutstandingAmount(amount);
        advance.setDocStatus(ErpFinConstants.DOC_STATUS_DRAFT);
        advance.setApproveStatus(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        dao.saveEntity(advance);
        return advance.getId();
    }

    private Long seedEmployee(long partnerId, int status) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = new ErpMdEmployee();
        emp.setCode("E-" + partnerId);
        emp.setName("员工-" + partnerId);
        emp.setOrgId(1L);
        emp.setPartnerId(partnerId);
        emp.setStatus(status);
        dao.saveEntity(emp);
        return emp.getId();
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

    private ErpFinArApItem findItem(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        List<ErpFinArApItem> items = dao.findAllByQuery(q);
        return items.isEmpty() ? null : items.get(0);
    }
}
