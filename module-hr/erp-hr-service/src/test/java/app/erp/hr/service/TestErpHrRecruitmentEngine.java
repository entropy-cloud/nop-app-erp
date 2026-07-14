package app.erp.hr.service;

import app.erp.hr.biz.IErpHrRecruitmentBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrRecruitment;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 招聘状态机端到端测试（use-cases.md UC-HR-05）。覆盖：
 * <ul>
 *   <li>场景1：完整流程 OPEN→SCREENING→INTERVIEW→OFFERED→HIRED + 员工创建 + 合同创建 + employeeId 回写。</li>
 *   <li>场景2：非法迁移 OPEN 直接 hire → ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION。</li>
 *   <li>场景3：INTERVIEW → reject → REJECTED。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrRecruitmentEngine extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrRecruitmentBiz recruitmentBiz;

    @Test
    public void testFullRecruitmentFlowCreatesEmployeeAndContract() {
        Long recId = ormTemplate.runInSession(session -> seedRecruitment("CAND-FULL"));

        ormTemplate.runInSession(() -> recruitmentBiz.moveToScreening(String.valueOf(recId), CTX));
        ormTemplate.runInSession(() -> recruitmentBiz.scheduleInterview(String.valueOf(recId), null, LocalDate.of(2026, 7, 15), CTX));
        ormTemplate.runInSession(() -> recruitmentBiz.makeOffer(String.valueOf(recId), new BigDecimal("15000"), CTX));
        ormTemplate.runInSession(() -> recruitmentBiz.hire(String.valueOf(recId), LocalDate.of(2026, 7, 20), CTX));

        ErpHrRecruitment refreshed = daoProvider.daoFor(ErpHrRecruitment.class).getEntityById(recId);
        assertEquals(ErpHrConstants.RECRUITMENT_STATUS_HIRED, refreshed.getStatus());
        assertNotNull(refreshed.getEmployeeId(), "employeeId 应已回写");

        ErpHrEmployee employee = daoProvider.daoFor(ErpHrEmployee.class).getEntityById(refreshed.getEmployeeId());
        assertNotNull(employee, "应创建新员工");
        assertEquals(ErpHrConstants.EMPLOYMENT_ACTIVE, employee.getEmploymentStatus());
        assertEquals(LocalDate.of(2026, 7, 20), employee.getHireDate());
        assertEquals("CAND-FULL", employee.getFullName());

        ErpHrEmploymentContract contract = findActiveContract(refreshed.getEmployeeId());
        assertNotNull(contract, "应创建 ACTIVE 合同");
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, contract.getStatus());
        assertEquals(LocalDate.of(2026, 7, 20), contract.getStartDate());
        assertEquals(0, contract.getMonthlySalary().compareTo(new BigDecimal("15000")));
    }

    @Test
    public void testIllegalTransitionOpenToHire() {
        Long recId = ormTemplate.runInSession(session -> seedRecruitment("CAND-ILLEGAL"));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> recruitmentBiz.hire(String.valueOf(recId), LocalDate.of(2026, 7, 20), CTX)));
        assertEquals(ErpHrErrors.ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testRejectFromInterview() {
        Long recId = ormTemplate.runInSession(session -> seedRecruitment("CAND-REJECT"));

        ormTemplate.runInSession(() -> recruitmentBiz.moveToScreening(String.valueOf(recId), CTX));
        ormTemplate.runInSession(() -> recruitmentBiz.scheduleInterview(String.valueOf(recId), null, LocalDate.of(2026, 7, 15), CTX));
        ormTemplate.runInSession(() -> recruitmentBiz.reject(String.valueOf(recId), CTX));

        ErpHrRecruitment refreshed = daoProvider.daoFor(ErpHrRecruitment.class).getEntityById(recId);
        assertEquals(ErpHrConstants.RECRUITMENT_STATUS_REJECTED, refreshed.getStatus());
    }

    @Test
    public void testCloseFromHired() {
        Long recId = ormTemplate.runInSession(session -> seedRecruitment("CAND-CLOSE"));

        ormTemplate.runInSession(() -> recruitmentBiz.moveToScreening(String.valueOf(recId), CTX));
        ormTemplate.runInSession(() -> recruitmentBiz.scheduleInterview(String.valueOf(recId), null, LocalDate.of(2026, 7, 15), CTX));
        ormTemplate.runInSession(() -> recruitmentBiz.makeOffer(String.valueOf(recId), new BigDecimal("12000"), CTX));
        ormTemplate.runInSession(() -> recruitmentBiz.hire(String.valueOf(recId), LocalDate.of(2026, 7, 20), CTX));
        ormTemplate.runInSession(() -> recruitmentBiz.close(String.valueOf(recId), CTX));

        ErpHrRecruitment refreshed = daoProvider.daoFor(ErpHrRecruitment.class).getEntityById(recId);
        assertEquals(ErpHrConstants.RECRUITMENT_STATUS_CLOSED, refreshed.getStatus());
    }

    // ---------- helpers ----------

    private ErpHrEmploymentContract findActiveContract(Long employeeId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        q.addFilter(eq("status", ErpHrConstants.CONTRACT_STATUS_ACTIVE));
        q.setLimit(1);
        List<ErpHrEmploymentContract> list = daoProvider.daoFor(ErpHrEmploymentContract.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long seedRecruitment(String candidateName) {
        IEntityDao<ErpHrRecruitment> dao = daoProvider.daoFor(ErpHrRecruitment.class);
        ErpHrRecruitment r = new ErpHrRecruitment();
        r.setBusinessDate(LocalDate.of(2026, 7, 1));
        r.setCode("REC-" + candidateName + "-" + System.nanoTime());
        r.setCandidateName(candidateName);
        r.setHeadcount(1);
        r.setStatus(ErpHrConstants.RECRUITMENT_STATUS_OPEN);
        dao.saveEntity(r);
        return r.getId();
    }
}
