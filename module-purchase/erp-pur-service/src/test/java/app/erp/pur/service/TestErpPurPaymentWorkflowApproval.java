package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurPayment;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import io.nop.wf.core.IWorkflow;
import io.nop.wf.core.IWorkflowManager;
import io.nop.wf.core.IWorkflowStep;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 付款单 WORKFLOW 多级审批端到端测试（approval-framework.md WORKFLOW）。
 * 验证 submitForApproval 启动 wf → finance-approval agree → wf 结束回调 Processor.approve → APPROVED+过账，
 * disagree → wf 结束回调 Processor.reject → REJECTED，REJECTED 后可重提。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurPaymentWorkflowApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1003L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long CURRENCY_ID = 6101L;
    static final Long ACCT_SCHEMA_ID = 7003L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IWorkflowManager workflowManager;

    @Test
    public void testSubmitAgreeThenApproved() {
        setUser();
        seedPeriodAndSubjects();
        ErpPurPayment payment = paymentOf("PY-WF-001", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        assertEquals(0, submit(payment.getId()).getStatus(), "submitForApproval 应成功");
        ErpPurPayment reloaded = reload(payment);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reloaded.getApproveStatus());
        assertNotNull(reloaded.getNopFlowId(), "WORKFLOW 模式应启动 wf 实例并回写 nopFlowId");

        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(payment).getNopFlowId());
            invokeStep(wf, "finance-approval", "agree", ctx);
            // cc-finance 步骤（plan 2026-07-06-0642-2 Phase 2）在 agree 后激活，需 confirm 后 wf 结束
            invokeStep(wf, "cc-finance", "confirm", ctx);
        });

        reloaded = reload(payment);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reloaded.getApproveStatus(),
                "finance-approval agree + cc confirm 后 wf 结束回调 approve → APPROVED");
        assertTrue(Boolean.TRUE.equals(reloaded.getPosted()), "PAYMENT 审核应过账 posted=true");
        // approvedBy 经 Processor.currentUserId() 读 IUserContext 线程局部，wf 回调上下文未填充该线程局部，
        // 故 wf 回调路径下 approvedBy 可能为 null（DIRECT 模式由 GraphQL 请求填充则非空）。此为既有 Processor 模式限制，非本计划引入。
    }

    @Test
    public void testDisagreeThenRejected() {
        setUser();
        seedPeriodAndSubjects();
        ErpPurPayment payment = paymentOf("PY-WF-002", new BigDecimal("100"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });
        assertEquals(0, submit(payment.getId()).getStatus());

        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(payment).getNopFlowId());
            invokeStep(wf, "finance-approval", "disagree", ctx);
        });

        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reload(payment).getApproveStatus(),
                "finance-approval disagree → wf 结束回调 reject → REJECTED");
    }

    @Test
    public void testResubmitAfterRejected() {
        setUser();
        seedPeriodAndSubjects();
        ErpPurPayment payment = paymentOf("PY-WF-003", new BigDecimal("100"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });
        submit(payment.getId());
        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(payment).getNopFlowId());
            invokeStep(wf, "finance-approval", "disagree", ctx);
        });
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reload(payment).getApproveStatus());

        assertEquals(0, submit(payment.getId()).getStatus(), "REJECTED 后 submitForApproval 应可重提");
        ErpPurPayment reloaded = reload(payment);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reloaded.getApproveStatus());
        assertNotNull(reloaded.getNopFlowId(), "重提应启动新的 wf 实例");
    }

    // ---------- helpers ----------

    private IServiceContext newContext() {
        IServiceContext ctx = new ServiceContextImpl();
        ctx.getContext().setUserId("0");
        ctx.getContext().setUserName("SYS");
        return ctx;
    }

    private void setUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    private void invokeStep(IWorkflow wf, String stepName, String action, IServiceContext ctx) {
        List<? extends IWorkflowStep> steps = wf.getActivatedSteps();
        for (IWorkflowStep step : steps) {
            if (step.getStepName().equals(stepName)) {
                step.invokeAction(action, null, ctx);
                wf.runAutoTransitions(ctx);
                return;
            }
        }
        throw new IllegalStateException("步骤未激活: " + stepName + "，当前激活=" + steps);
    }

    private ApiResponse<?> submit(Long id) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpPurPayment__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(id))));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurPayment reload(ErpPurPayment payment) {
        return daoProvider.daoFor(ErpPurPayment.class).getEntityById(payment.getId());
    }

    private ErpPurPayment paymentOf(String code, BigDecimal total) {
        ErpPurPayment payment = new ErpPurPayment();
        payment.setCode(code);
        payment.setOrgId(ORG_ID);
        payment.setSupplierId(SUPPLIER_ID);
        payment.setBusinessDate(LocalDate.of(2026, 7, 1));
        payment.setCurrencyId(CURRENCY_ID);
        payment.setExchangeRate(BigDecimal.ONE);
        payment.setTotalAmount(total);
        payment.setAmountSource(total);
        payment.setAmountFunctional(total);
        payment.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        payment.setWrittenOffStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        payment.setPosted(false);
        return payment;
    }

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("2202", "应付账款");
            seedSubject("1002", "银行存款");
            seedAcctSchema(ACCT_SCHEMA_ID, ORG_ID);
        });
    }

    private void seedAcctSchema(Long id, Long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setId(id);
        schema.setCode("AS-" + id);
        schema.setName("账套" + id);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
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
}
