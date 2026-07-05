package app.erp.fin.service.posting;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.entity.ErpFinExpenseClaimLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 员工→partnerId 解析端到端单测（Phase 3）。验证：
 * <ul>
 *   <li>claimant/employee.partnerId 为空时审核被拒（员工须有内部往来单位记录，否则辅助账 mandatory FK 违约）。</li>
 *   <li>billData 的 EMPLOYEE_ID 键携带 employee.partnerId（即 ErpMdPartner.id），生成的辅助账 partnerId = employee.partnerId
 *       <b>非 employee.id</b>（员工与 partner 是不同 id 空间）。</li>
 * </ul>
 * 对应 plan Task Route Decision「员工→partnerId 解析」。
 *
 * <p>审批动作经 {@link IGraphQLEngine} 调 {@code ErpFinExpenseClaim__approve}、
 * {@code ErpFinEmployeeAdvance__approve}（approval-support.xbiz 标准 source），引擎建 session/事务/管道。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinPartnerIdResolution extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testApproveRejectedWhenClaimantPartnerIdNull() {
        Long claimId = ormTemplate.runInSession(session -> {
            seedOpenPeriod();
            seedAcctSchema(1L);
            // 员工无 partnerId
            Long empId = seedEmployee(null, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedSubmittedClaim("EC-PID-001", empId);
        });
        // 直接置 SUBMITTED（绕过 submit 校验），approve 时 validateForApproval 拦截 partnerId 缺失
        assertTrue(approveClaim(claimId).getStatus() != 0,
                "claimant.partnerId 为空：approve 被 validateForApproval 拦截");
    }

    @Test
    public void testApproveRejectedWhenEmployeePartnerIdNull() {
        Long advanceId = ormTemplate.runInSession(session -> {
            seedOpenPeriod();
            seedAcctSchema(1L);
            Long empId = seedEmployee(null, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            return seedSubmittedAdvance("ADV-PID-001", empId, new BigDecimal("100"));
        });
        assertTrue(approveAdvance(advanceId).getStatus() != 0,
                "employee.partnerId 为空：approve 被 validateForApproval 拦截");
    }

    @Test
    public void testSubledgerPartnerIdIsEmployeePartnerIdNotEmployeeId() {
        long partnerId = 6601L;
        Long[] ids = ormTemplate.runInSession(session -> {
            seedOpenPeriod();
            seedAcctSchema(1L);
            seedSubject("6602", "管理费用");
            seedSubject("2221", "应交税费-进项税额");
            seedSubject("2241", "其他应付款-员工");
            seedSubject("1221", "其他应收款-员工预支");
            seedSubject("1002", "银行存款");
            Long empId = seedEmployee(partnerId, ErpFinConstants.EMPLOYEE_STATUS_ACTIVE);
            Long claimId = seedSubmittedClaimAmount("EC-PID-002", empId, new BigDecimal("113"));
            Long advanceId = seedSubmittedAdvance("ADV-PID-002", empId, new BigDecimal("200"));
            return new Long[]{empId, claimId, advanceId};
        });
        Long employeeId = ids[0];
        Long claimId = ids[1];
        Long advanceId = ids[2];

        assertEquals(0, approveClaim(claimId).getStatus());
        ErpFinArApItem payableItem = findItem("EXPENSE_CLAIM", "EC-PID-002");
        assertEquals(partnerId, payableItem.getPartnerId(), "报销辅助账 partnerId = employee.partnerId");
        assertNotEquals(employeeId, payableItem.getPartnerId(), "partnerId 非 employee.id");

        assertEquals(0, approveAdvance(advanceId).getStatus());
        ErpFinArApItem receivableItem = findItem("EMPLOYEE_ADVANCE", "ADV-PID-002");
        assertEquals(partnerId, receivableItem.getPartnerId(), "借款辅助账 partnerId = employee.partnerId");
        assertNotEquals(employeeId, receivableItem.getPartnerId(), "partnerId 非 employee.id");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> approveClaim(Long id) {
        return executeRpc(mutation, "ErpFinExpenseClaim__approve",
                ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveAdvance(Long id) {
        return executeRpc(mutation, "ErpFinEmployeeAdvance__approve",
                ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedOpenPeriod() {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode("2026-06");
        period.setName("2026-06");
        period.setOrgId(1L);
        period.setYear(2026);
        period.setMonth(6);
        period.setStartDate(LocalDate.of(2026, 6, 1));
        period.setEndDate(LocalDate.of(2026, 6, 30));
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }

    private Long seedEmployee(Long partnerId, String status) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = new ErpMdEmployee();
        emp.setCode("E-" + (partnerId != null ? partnerId : "nopartner"));
        emp.setName("员工");
        emp.setOrgId(1L);
        emp.setPartnerId(partnerId);
        emp.setStatus(status);
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedSubmittedClaim(String code, Long claimantId) {
        return seedSubmittedClaimAmount(code, claimantId, new BigDecimal("113"));
    }

    private Long seedSubmittedClaimAmount(String code, Long claimantId, BigDecimal withTax) {
        IEntityDao<ErpFinExpenseClaim> dao = daoProvider.daoFor(ErpFinExpenseClaim.class);
        ErpFinExpenseClaim claim = new ErpFinExpenseClaim();
        claim.setCode(code);
        claim.setOrgId(1L);
        claim.setClaimantId(claimantId);
        claim.setBusinessDate(LocalDate.of(2026, 6, 15));
        claim.setPaymentMode(ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        claim.setCurrencyId(1L);
        claim.setExchangeRate(BigDecimal.ONE);
        claim.setAmountWithoutTax(new BigDecimal("100"));
        claim.setTaxAmount(new BigDecimal("13"));
        claim.setAmountWithTax(withTax);
        claim.setReason("差旅");
        claim.setDocStatus(ErpFinConstants.DOC_STATUS_DRAFT);
        claim.setApproveStatus(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        dao.saveEntity(claim);

        IEntityDao<ErpFinExpenseClaimLine> lineDao = daoProvider.daoFor(ErpFinExpenseClaimLine.class);
        ErpFinExpenseClaimLine line = new ErpFinExpenseClaimLine();
        line.setClaimId(claim.getId());
        line.setLineNo(1);
        line.setExpenseType("TRAVEL");
        line.setAmountWithoutTax(new BigDecimal("100"));
        line.setTaxAmount(new BigDecimal("13"));
        line.setAmountWithTax(withTax);
        lineDao.saveEntity(line);
        return claim.getId();
    }

    private Long seedSubmittedAdvance(String code, Long employeeId, BigDecimal amount) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinEmployeeAdvance> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinEmployeeAdvance.class);
        app.erp.fin.dao.entity.ErpFinEmployeeAdvance advance = new app.erp.fin.dao.entity.ErpFinEmployeeAdvance();
        advance.setCode(code);
        advance.setOrgId(1L);
        advance.setEmployeeId(employeeId);
        advance.setAdvanceType("EXPENSE_ADVANCE");
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

    private ErpFinArApItem findItem(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        List<ErpFinArApItem> items = dao.findAllByQuery(q);
        return items.isEmpty() ? null : items.get(0);
    }
}
