package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import app.erp.pur.dao.entity.ErpPurPayment;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.context.ContextProvider;
import io.nop.auth.dao.entity.NopAuthRole;
import io.nop.auth.dao.entity.NopAuthUser;
import io.nop.auth.dao.entity.NopAuthUserRole;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 付款单审批通知端到端测试（plan 2026-07-06-0642-2）。
 *
 * <p>Phase 1：审批结果通知提单人（USER_LIST ${submitterUserId}）—— agree→cc confirm→wf end→listener 调
 * notify('wf.pur-payment.result')，断言提单人收到 ErpSysNotification。
 * <p>Phase 2/3：cc step 产生实例 + CC/任务到达通知（ROLE 接收人，需种子角色用户）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurPaymentApprovalNotifications extends JunitAutoTestCase {

    static final Long ORG_ID = 1003L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long CURRENCY_ID = 6101L;
    static final Long ACCT_SCHEMA_ID = 7003L;
    static final String SUBMITTER = "0";
    static final String APPROVER_USER = "pur-wf-approver";
    static final String CC_USER = "pur-wf-cc";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IWorkflowManager workflowManager;

    @Test
    public void testApprovalResultNotifiesSubmitter() {
        setUser(SUBMITTER);
        seedPeriodAndSubjects();
        ErpPurPayment payment = paymentOf("PY-NOTIFY-001", new BigDecimal("100"));
        seedResultTemplate();

        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        assertEquals(0, submit(payment.getId()).getStatus(), "submitForApproval 应成功");

        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(payment).getNopFlowId());
            invokeStep(wf, "finance-approval", "agree", ctx);
            invokeStep(wf, "cc-finance", "confirm", ctx);
        });

        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reload(payment).getApproveStatus(),
                "agree + cc confirm 后应 APPROVED");

        List<ErpSysNotification> all = allNotifications();
        ErpPurPayment dbg = reload(payment);
        String submitter = dbg.getCreatedBy();
        List<ErpSysNotification> list = notificationsOf(submitter, "wf.pur-payment.result");
        assertEquals(1, list.size(), "提单人(" + submitter + ")应收到 1 条审批结果通知: " + list.size() + ", all=" + all.size());
        ErpSysNotification n = list.get(0);
        assertTrue(n.getSubject().contains("PY-NOTIFY-001"), "subject 应含单号: " + n.getSubject());
        assertTrue(n.getBody().contains("已通过"), "approve 结果应含已通过: " + n.getBody());
    }

    @Test
    public void testRejectedResultNotifiesSubmitter() {
        setUser(SUBMITTER);
        seedPeriodAndSubjects();
        seedResultTemplate();
        ErpPurPayment payment = paymentOf("PY-NOTIFY-002", new BigDecimal("100"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });
        assertEquals(0, submit(payment.getId()).getStatus());

        // disagree → to-end 直接（不经 cc），listener 回调 reject
        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(payment).getNopFlowId());
            invokeStep(wf, "finance-approval", "disagree", ctx);
        });

        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reload(payment).getApproveStatus());

        List<ErpSysNotification> list = notificationsOf(reload(payment).getCreatedBy(), "wf.pur-payment.result");
        assertEquals(1, list.size(), "驳回应通知提单人");
        assertTrue(list.get(0).getBody().contains("已驳回"), "reject 结果应含已驳回");
    }

    @Test
    public void testCcStepInstanceAndRoleNotifications() {
        setUser(SUBMITTER);
        seedPeriodAndSubjects();
        seedResultTemplate();
        // CC + 任务到达通知使用 ROLE 接收人，需种子角色与用户映射
        seedRole("财务员", APPROVER_USER);
        seedRole("财务经理", CC_USER);
        seedTemplate(7131L, "wf.pur-payment.cc", "付款单 ${docNo} 抄送知会",
                "付款单 ${docNo} 已审批通过，特此抄送知会",
                ErpNotifyConstants.RESOLVER_ROLE, "{\"roles\":[\"财务经理\"]}",
                ErpNotifyConstants.MERGE_BY_USER_TYPE, 300);
        seedTemplate(7121L, "wf.pur-payment.task-assigned", "付款单 ${docNo} 待您审批",
                "付款单 ${docNo} 到达${stepName}步骤，请及时处理",
                ErpNotifyConstants.RESOLVER_ROLE, "{\"roles\":[\"财务员\"]}",
                ErpNotifyConstants.MERGE_BY_USER_TYPE, 300);

        ErpPurPayment payment = paymentOf("PY-CC-001", new BigDecimal("100"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });
        assertEquals(0, submit(payment.getId()).getStatus());

        // agree + cc confirm 在同一 session 内完成（onEnter 触发任务到达 + 抄送通知）
        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(payment).getNopFlowId());
            invokeStep(wf, "finance-approval", "agree", ctx);
            invokeStep(wf, "cc-finance", "confirm", ctx);
        });

        // 任务到达通知（Phase 3）：审批人角色用户应收到
        List<ErpSysNotification> taskList = notificationsOf(APPROVER_USER, "wf.pur-payment.task-assigned");
        assertTrue(!taskList.isEmpty(), "候选审批人应收到任务到达通知");

        // cc step 实例存在（Phase 2）——历史步骤中含 cc-finance
        final boolean[] hasCc = {false};
        ormTemplate.runInSession(() -> {
            IWorkflow wf2 = workflowManager.getWorkflow(reload(payment).getNopFlowId());
            hasCc[0] = wf2.getSteps(true).stream().anyMatch(s -> "cc-finance".equals(s.getStepName()));
        });
        assertTrue(hasCc[0], "agree 后应产生 cc-finance step 实例");

        // CC 通知（Phase 2）：cc step onEnter 触发
        List<ErpSysNotification> ccList = notificationsOf(CC_USER, "wf.pur-payment.cc");
        assertTrue(!ccList.isEmpty(), "CC 接收人应收到抄送通知");

        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reload(payment).getApproveStatus());
    }

    // ---------- helpers ----------

    private IServiceContext newContext() {
        IServiceContext ctx = new ServiceContextImpl();
        ctx.getContext().setUserId("0");
        ctx.getContext().setUserName("SYS");
        return ctx;
    }

    private void setUser(String userId) {
        ContextProvider.getOrCreateContext().setUserId(userId);
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    @SuppressWarnings("unchecked")
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
                ApiRequest.build(java.util.Map.of("id", String.valueOf(id))));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurPayment reload(ErpPurPayment payment) {
        return daoProvider.daoFor(ErpPurPayment.class).getEntityById(payment.getId());
    }

    private List<ErpSysNotification> notificationsOf(String userId, String eventType) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private List<ErpSysNotification> allNotifications() {
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(new io.nop.api.core.beans.query.QueryBean());
    }

    private void seedResultTemplate() {
        seedTemplate(7111L, "wf.pur-payment.result", "付款单 ${docNo} 审批${resultText}",
                "您提交的付款单 ${docNo} 审批${resultText}，审批人 ${approverUserId}",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"${submitterUserId}\"]}",
                ErpNotifyConstants.MERGE_BY_USER_TYPE, 300);
    }

    private void seedTemplate(Long id, String notificationType, String subjectTpl, String bodyTpl,
                              String resolver, String recipientConfig, String mergeStrategy, int mergeWindow) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(notificationType);
            t.setName("TPL-" + notificationType);
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl(subjectTpl);
            t.setBodyTpl(bodyTpl);
            t.setRecipientResolver(resolver);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(mergeWindow);
            t.setMergeStrategy(mergeStrategy);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            dao.saveEntity(t);
        });
    }

    private void seedRole(String roleName, String userId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<NopAuthRole> roleDao = daoProvider.daoFor(NopAuthRole.class);
            NopAuthRole role = new NopAuthRole();
            role.setRoleId("role-" + roleName);
            role.setRoleName(roleName);
            roleDao.saveEntity(role);

            IEntityDao<NopAuthUser> userDao = daoProvider.daoFor(NopAuthUser.class);
            NopAuthUser user = new NopAuthUser();
            user.setUserId(userId);
            user.setUserName(userId);
            user.setNickName(userId);
            user.setPassword("dummy-pwd");
            user.setOpenId(userId);
            user.setGender(0);
            user.setUserType(0);
            user.setStatus(0);
            user.setTenantId("0");
            userDao.saveEntity(user);

            IEntityDao<NopAuthUserRole> urDao = daoProvider.daoFor(NopAuthUserRole.class);
            NopAuthUserRole ur = new NopAuthUserRole();
            ur.setUserId(userId);
            ur.setRoleId("role-" + roleName);
            urDao.saveEntity(ur);
        });
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
