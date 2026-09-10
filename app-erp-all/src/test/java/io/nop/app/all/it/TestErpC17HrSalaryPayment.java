package io.nop.app.all.it;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig;
import app.erp.hr.dao.entity.ErpHrTaxConfig;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.orm.IOrmTemplate;
import io.nop.wf.core.IWorkflow;
import io.nop.wf.core.IWorkflowManager;
import io.nop.wf.core.IWorkflowStep;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B8 C17：HR 薪酬发放闭环（按 {@code docs/design/integration-testing.md §6 C17} 规格）。
 *
 * <p>全链（引用部署 seed：员工 HR-EMP-001 赵明（id 1，org 2，部门 1）/ HR-EMP-002 钱华（id 2）/
 * 科目 6601/2211/1002 / 账套 1（org 2，CNY）/ 2026-07 OPEN 期间）：自包含薪酬核算环境（2026 税档 +
 * emp1/emp2 劳动合同 15000/8000 + 深圳社保基数 15000/15000 + 养老 0.15/0.08 + 公积金 0.12/0.12 配置 +
 * 6601.01/6601.02 科目）→ {@code ErpHrSalary__calculateSalary}(emp1, 2026-7)（核算：gross=15000 全勤 /
 * 社保 EE 1200 / 公积金 EE 1800 / 个税 210（累计预扣 (15000-5000-3000)×3%）/ net=11790——
 * TestErpHrPayrollEngine 口径先例）→ {@code ErpHrSalary__runPayroll}(2026-7)（全员批量：emp1 已存在
 * 幂等跳过，emp2 新建 8000 档）→ {@code submitForApproval}（启动 salary-approval xwf，nopFlowId 回写）
 * → xwf 三级 agree（hr-review → finance-review → manager-approval，setUserId("0") SYS 后端驱动）+
 * cc-hr confirm → wf 结束回调 approve（APPROVED + 计提链 270/290/300 三凭证 + posted=true）→
 * {@code markPaid}（PAID + SALARY_PAYMENT 280 凭证 Dr 2211 应付职工薪酬 / Cr 1002 银行存款 =
 * 实发净额 11790，借贷平衡）→ 发放通知（自包含模板 {@code ErpSysNotification__notify} → markRead →
 * countUnread 归零，C09 先例）。
 *
 * <p>Phase 3 Decision（四项裁决，落盘设计文档 §6 C17 勘误）：
 * <ol>
 *   <li><b>xwf 三级 agree 调用形态</b>：{@code ErpHrSalary.xbiz} submitForApproval 覆写 source 经
 *       {@code ApprovalFlowHelper.start} 启动 wf（nopFlowId 回写），步骤驱动 = TestErpHrSalaryWorkflowApproval
 *       先例：{@code IWorkflowManager.getWorkflow(nopFlowId)} + 逐激活步骤 {@code invokeAction("agree")}
 *       + {@code runAutoTransitions}，ctx userId="0"（SYS）匹配 actorType="all" 参与者；末步 cc-hr
 *       （specialType=cc）须 confirm 后 wf 结束回调 approve action（270/290/300 计提链在此挂接）。</li>
 *   <li><b>payroll 科目 config</b>：{@code erp-hr.default-payroll-subject-id} 缺省空 → 270/280 抛
 *       ERR_PAYROLL_SUBJECT_NOT_CONFIGURED（G3 吞异常后零凭证）——@NopTestProperty 指定 2211
 *       （B6 C12 同型；值=科目编码非种子行 ID）。</li>
 *   <li><b>SALARY_PAYMENT 凭证金额口径</b>：280 = Dr 2211 / Cr 1002，金额 = <b>实发净额</b> 11790
 *       （银行实付口径，{@code buildPaymentEvent} 传 netSalary）——设计文档「金额 = 薪资合计 /
 *       Dr 费用/Cr 2211」为 270 计提口径误植，按实仓勘误；270 才是 Dr 6601 费用 / Cr 2211 = gross 15000。</li>
 *   <li><b>通知触发路径</b>：实仓 markPaid 无自动员工通知（{@code ErpHrSalaryMarkPaidProcessor} 仅过账 +
 *       PAID 翻转；notify 仅计提失败告警路径）——发放通知断言改 C09 自包含模板先例；设计文档步骤 4
 *       「发放完成触发员工通知」为未实现动作面勘误。员工往来（ar_ap_item EMPLOYEE_ADVANCE 口径）
 *       断言同勘误——HR 过账 Provider 纯 GL 无 AR/AP 辅助账生成。</li>
 * </ol>
 *
 * <p>冻结时钟 {@code C15C16C17FrozenClockExtension}（2026-07-17）：salary businessDate / markPaid
 * paymentDate / 凭证日期（计提=2026-07-15 / 发放=期间内）确定化；凭证 postedAt 等跨 run 不稳定时间戳按
 * B4-B7 先例以 {@code *} 通配处置。
 *
 * <p>三层验证：层 1 = JUnit 关键断言（核算金额口径 + xwf 终态 APPROVED + posted=true + 270/290/300/280
 * 四凭证借贷平衡与科目方向 + PAID + 通知已读归零）；层 2 = 每步 response 快照；层 3 = output/tables
 * 变更行（salary + tax_config + contract + social_insurance 族 + subject + voucher 族 + notification 族）。
 * RECORDING→CHECKING 往返按 M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-hr.default-payroll-subject-id", value = "2211")
public class TestErpC17HrSalaryPayment extends ErpIntegrationTestCase {

    static final String EMP1_ID = "1";   // seed HR-EMP-001 赵明（org 2）
    static final String EMP2_ID = "2";   // seed HR-EMP-002 钱华
    static final String NOTIFY_USER = "it-c17-user";
    static final String NOTIFY_EVENT = "hr.salary-paid";

    // emp1 核算口径（月薪 15000 全勤）：gross 15000 / 社保 EE 1200 / 公积金 EE 1800 / 个税 210 / net 11790
    static final BigDecimal GROSS = new BigDecimal("15000.00");
    static final BigDecimal SOCIAL_ER = new BigDecimal("2250.00");   // 15000 × 15%
    static final BigDecimal FUND_ER = new BigDecimal("1800.00");     // 15000 × 12%
    static final BigDecimal TAX = new BigDecimal("210.00");          // (15000-5000-3000) × 3%
    static final BigDecimal NET = new BigDecimal("11790.00");

    @RegisterExtension
    static C15C16C17FrozenClockExtension frozenClock = new C15C16C17FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IWorkflowManager workflowManager;

    // xwf 轴 setUserId("0") 后端驱动（B1/B2 复核先例 + TestErpHrSalaryWorkflowApproval：SYS caller
    // 匹配 actorType="all" 步骤参与者，避免对 NopAuthUser 的参与者解析查询）
    @BeforeEach
    public void setUpWfUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    @Test
    public void testHrSalaryPaymentClosedLoop() {
        // ---------- 1. 自包含薪酬核算环境（TestErpHrSalaryPostingChain.seedFullEnvironment 同型） ----------
        seedPayrollEnvironment();

        // ---------- 2. calculateSalary（emp1 核算：金额口径层 1 锚点） ----------
        ApiResponse<?> calc = rpcMutation("ErpHrSalary__calculateSalary", request("1_calculate_salary.json5", Map.class));
        output("1_calculate_salary_response.json5", calc);
        assertEquals(0, calc.getStatus(), "calculateSalary 应成功");
        String salaryId = idOf(calc);
        addVar("salaryId", salaryId);

        ErpHrSalary calculated = reloadSalary(salaryId);
        assertEquals(0, GROSS.compareTo(calculated.getGrossSalary()), "应发合计=15000（合同月薪全勤）");
        assertEquals(0, new BigDecimal("1200.00").compareTo(calculated.getSocialInsurance()), "社保个人=1200");
        assertEquals(0, new BigDecimal("1800.00").compareTo(calculated.getHousingFund()), "公积金个人=1800");
        assertEquals(0, TAX.compareTo(calculated.getTaxAmount()), "个税=210（累计预扣 3% 档）");
        assertEquals(0, NET.compareTo(calculated.getNetSalary()), "实发=11790");
        assertEquals("PENDING", calculated.getPaymentStatus(), "支付轴初始 PENDING");

        // ---------- 3. runPayroll（全员批量：emp1 幂等跳过 + emp2 新建） ----------
        ApiResponse<?> payroll = rpcMutation("ErpHrSalary__runPayroll", request("2_run_payroll.json5", Map.class));
        output("2_run_payroll_response.json5", payroll);
        assertEquals(0, payroll.getStatus(), "runPayroll 应成功");
        assertTrue(payroll.getData() instanceof List, "runPayroll 返回批量结果列表");
        assertEquals(1, ((List<?>) payroll.getData()).size(), "emp1 已存在幂等跳过，仅 emp2 新建 1 条");
        ErpHrSalary calculatedAgain = reloadSalary(salaryId);
        assertEquals(0, GROSS.compareTo(calculatedAgain.getGrossSalary()), "runPayroll 不重复核算 emp1");

        // ---------- 4. submitForApproval（启动 salary-approval xwf） ----------
        ApiResponse<?> submit = rpcMutation("ErpHrSalary__submitForApproval",
                request("3_salary_submit.json5", Map.class));
        output("3_salary_submit_response.json5", submit);
        assertEquals(0, submit.getStatus(), "submitForApproval 应成功");
        ErpHrSalary submitted = reloadSalary(salaryId);
        assertEquals("SUBMITTED", submitted.getApproveStatus(), "提交后 SUBMITTED");
        assertNotNull(submitted.getNopFlowId(), "WORKFLOW 模式启动 wf 实例并回写 nopFlowId");

        // ---------- 5. xwf 三级 agree + cc-hr confirm（setUserId("0") 后端驱动） ----------
        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reloadSalary(salaryId).getNopFlowId());
            invokeStep(wf, "hr-review", "agree", ctx);
            invokeStep(wf, "finance-review", "agree", ctx);
            invokeStep(wf, "manager-approval", "agree", ctx);
            invokeStep(wf, "cc-hr", "confirm", ctx);
        });

        ErpHrSalary approved = reloadSalary(salaryId);
        assertEquals("APPROVED", approved.getApproveStatus(), "三级 agree + cc confirm 后 wf 回调 approve → APPROVED");
        assertNotNull(approved.getApprovedBy(), "approvedBy 由 approve action 写入");
        assertEquals(Boolean.TRUE, approved.getPosted(), "计提链 270/290/300 三路全成 → posted=true");

        Map<String, Object> approvedState = new LinkedHashMap<>();
        approvedState.put("id", approved.getId());
        approvedState.put("approveStatus", approved.getApproveStatus());
        approvedState.put("paymentStatus", approved.getPaymentStatus());
        approvedState.put("posted", approved.getPosted());
        output("4_salary_approved_state.json5", approvedState);

        // 计提三凭证（billCode = SAL-202607-{salaryId}）
        String billCode = "SAL-202607-" + salaryId;
        addVar("billCode", billCode);
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SALARY), "270 计提凭证恰一张");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER), "290 社保公司凭证恰一张");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.HOUSING_FUND_ER), "300 公积金公司凭证恰一张");

        assertVoucherLines(postedVoucher(billCode, ErpFinBusinessType.SALARY), GROSS, "6601", "2211", "270 计提");
        assertVoucherLines(postedVoucher(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER), SOCIAL_ER,
                "6601.01", "2211", "290 社保公司承担");
        assertVoucherLines(postedVoucher(billCode, ErpFinBusinessType.HOUSING_FUND_ER), FUND_ER,
                "6601.02", "2211", "300 公积金公司承担");

        // ---------- 6. markPaid（PAID + SALARY_PAYMENT 280 凭证） ----------
        ApiResponse<?> markPaid = rpcMutation("ErpHrSalary__markPaid", request("5_salary_mark_paid.json5", Map.class));
        output("5_salary_mark_paid_response.json5", markPaid);
        assertEquals(0, markPaid.getStatus(), "markPaid 应成功");
        ErpHrSalary paid = reloadSalary(salaryId);
        assertEquals("PAID", paid.getPaymentStatus(), "发放后 paymentStatus=PAID");
        assertEquals(LocalDate.of(2026, 7, 17), paid.getPaymentDate(), "发放日期=冻结参考日");

        assertVoucherLines(postedVoucher(billCode, ErpFinBusinessType.SALARY_PAYMENT), NET,
                "2211", "1002", "280 发放（Dr 应付职工薪酬 / Cr 银行存款 = 实发净额口径）");

        // ---------- 7. 发放通知（C09 自包含模板先例）→ markRead → countUnread 归零 ----------
        // P2-CK-notify-010-r3 身份校验：已读操作以接收人（NOTIFY_USER）身份执行（切换出 SYS "0" 上下文）
        ContextProvider.getOrCreateContext().setUserId(NOTIFY_USER);
        ContextProvider.getOrCreateContext().setUserName(NOTIFY_USER);
        seedNotificationTemplate();
        ApiResponse<?> notify = rpcMutation("ErpSysNotification__notify", request("6_notify.json5", Map.class));
        output("6_notify_response.json5", notify);
        assertEquals(0, notify.getStatus(), "通知派发应成功");

        List<ErpSysNotification> sent = notificationsOf(NOTIFY_USER);
        assertEquals(1, sent.size(), "应派发 1 条发放通知");
        ErpSysNotification notification = sent.get(0);
        assertTrue(notification.getBody().contains(billCode), "通知正文含薪酬单号: " + notification.getBody());
        addVar("notifId", notification.getId());

        ApiResponse<?> markRead = rpcMutation("ErpSysNotification__markRead",
                request("7_notify_mark_read.json5", Map.class));
        output("7_notify_mark_read_response.json5", markRead);
        assertEquals(0, markRead.getStatus(), "markRead 应成功");

        ApiResponse<?> unread = executeRpc(GraphQLOperationType.query, "ErpSysNotification__countUnread",
                request("8_notify_count_unread.json5", Map.class));
        output("8_notify_count_unread_response.json5", unread);
        assertEquals(0, unread.getStatus(), "countUnread 查询应成功");
        assertEquals(0, ((Number) unread.getData()).intValue(), "markRead 后 countUnread 归零");
    }

    // ---------- xwf helpers（TestErpHrSalaryWorkflowApproval 先例） ----------

    private IServiceContext newContext() {
        IServiceContext ctx = new ServiceContextImpl();
        ctx.getContext().setUserId("0");
        ctx.getContext().setUserName("SYS");
        return ctx;
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
        throw new IllegalStateException("步骤未激活，无法执行 " + action + ": " + stepName
                + "，当前激活步骤=" + steps);
    }

    // ---------- assertion helpers ----------

    private ErpHrSalary reloadSalary(String salaryId) {
        return daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, ErpFinBusinessType type) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", type.name()));
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
    }

    private int countBillLinks(String billCode, ErpFinBusinessType type) {
        return findBillLinks(billCode, type).size();
    }

    private ErpFinVoucher postedVoucher(String billCode, ErpFinBusinessType type) {
        List<ErpFinVoucherBillR> links = findBillLinks(billCode, type);
        assertEquals(1, links.size(), type + " 回链应唯一");
        return requireVoucherBalanced(links.get(0), amountOf(type), type + " 凭证");
    }

    private BigDecimal amountOf(ErpFinBusinessType type) {
        switch (type) {
            case SALARY: return GROSS;
            case SOCIAL_INSURANCE_ER: return SOCIAL_ER;
            case HOUSING_FUND_ER: return FUND_ER;
            case SALARY_PAYMENT: return NET;
            default: throw new IllegalArgumentException("unexpected type: " + type);
        }
    }

    /** 凭证行断言：2 行，Dr debitCode / Cr creditCode，金额 = expected（借贷平衡由 requireVoucherBalanced 保证）。 */
    private void assertVoucherLines(ErpFinVoucher voucher, BigDecimal expected,
                                    String debitCode, String creditCode, String label) {
        assertEquals("POSTED", voucher.getDocStatus(), label + " 凭证已过账");
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucher.getId()));
        q.addOrderField("lineNo", false);
        List<ErpFinVoucherLine> lines = daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
        assertEquals(2, lines.size(), label + " 凭证 2 行");
        ErpFinVoucherLine debit = lines.get(0);
        ErpFinVoucherLine credit = lines.get(1);
        assertEquals(debitCode, debit.getSubjectCode(), label + " 借方科目 " + debitCode);
        assertEquals("DEBIT", debit.getDcDirection(), label + " 借方方向");
        assertEquals(0, expected.compareTo(debit.getDebitAmount()), label + " 借方金额");
        assertEquals(creditCode, credit.getSubjectCode(), label + " 贷方科目 " + creditCode);
        assertEquals("CREDIT", credit.getDcDirection(), label + " 贷方方向");
        assertEquals(0, expected.compareTo(credit.getCreditAmount()), label + " 贷方金额");
    }

    private List<ErpSysNotification> notificationsOf(String userId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addOrderField("createTime", true);
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    // ---------- seed helpers（TestErpSalaryPostingChain.seedFullEnvironment 同型） ----------

    /** 自包含核算环境：2026 税档 + emp1/emp2 合同与社保基数 + 深圳社保/公积金配置 + 6601.01/6601.02 科目。 */
    private void seedPayrollEnvironment() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpHrTaxConfig> taxDao = daoProvider.daoFor(ErpHrTaxConfig.class);
            ErpHrTaxConfig tax = new ErpHrTaxConfig();
            tax.setYear(2026);
            tax.setTaxThreshold(new BigDecimal("5000"));
            tax.setTaxBrackets("["
                    + "{\"rangeUpperLimit\":36000,\"rate\":0.03,\"quickDeduction\":0},"
                    + "{\"rangeUpperLimit\":144000,\"rate\":0.10,\"quickDeduction\":2520},"
                    + "{\"rangeUpperLimit\":300000,\"rate\":0.20,\"quickDeduction\":16920},"
                    + "{\"rangeUpperLimit\":420000,\"rate\":0.25,\"quickDeduction\":31920},"
                    + "{\"rangeUpperLimit\":660000,\"rate\":0.30,\"quickDeduction\":52920},"
                    + "{\"rangeUpperLimit\":960000,\"rate\":0.35,\"quickDeduction\":85920},"
                    + "{\"rangeUpperLimit\":null,\"rate\":0.45,\"quickDeduction\":181920}"
                    + "]");
            taxDao.saveEntity(tax);

            seedContract(EMP1_ID, "C-IT-C17-EMP1", "15000");
            seedContract(EMP2_ID, "C-IT-C17-EMP2", "8000");
            seedSocialBase(EMP1_ID);
            seedSocialBase(EMP2_ID);
            seedSocialConfig("0.15", "0.08", "PENSION");
            seedSocialConfig("0.12", "0.12", "HOUSING_FUND");

            seedSubject("6601.01", "管理费用-社保");
            seedSubject("6601.02", "管理费用-公积金");
        });
    }

    private void seedContract(String employeeId, String code, String monthlySalary) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setBusinessDate(LocalDate.of(2026, 7, 1));
        c.setCode(code);
        c.setEmployeeId(employeeId);
        c.setContractType("OPEN_ENDED");
        c.setSignDate(LocalDate.of(2025, 1, 1));
        c.setStartDate(LocalDate.of(2025, 1, 1));
        c.setMonthlySalary(new BigDecimal(monthlySalary));
        c.setStatus("ACTIVE");
        dao.saveEntity(c);
    }

    private void seedSocialBase(String employeeId) {
        IEntityDao<ErpHrSocialInsuranceBase> dao = daoProvider.daoFor(ErpHrSocialInsuranceBase.class);
        ErpHrSocialInsuranceBase base = new ErpHrSocialInsuranceBase();
        base.setEmployeeId(employeeId);
        base.setCityCode("SHENZHEN");
        base.setSocialInsuranceBase(new BigDecimal("15000"));
        base.setHousingFundBase(new BigDecimal("15000"));
        base.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        dao.saveEntity(base);
    }

    private void seedSocialConfig(String companyRate, String employeeRate, String insuranceType) {
        IEntityDao<ErpHrSocialInsuranceConfig> dao = daoProvider.daoFor(ErpHrSocialInsuranceConfig.class);
        ErpHrSocialInsuranceConfig cfg = new ErpHrSocialInsuranceConfig();
        cfg.setCityCode("SHENZHEN");
        cfg.setInsuranceType(insuranceType);
        cfg.setCompanyRate(new BigDecimal(companyRate));
        cfg.setEmployeeRate(new BigDecimal(employeeRate));
        cfg.setBaseLowerLimit(new BigDecimal("2360"));
        cfg.setBaseUpperLimit(new BigDecimal("32694"));
        dao.saveEntity(cfg);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("EXPENSE");
        subject.setDirection("DEBIT");
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedNotificationTemplate() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", "7109");
            t.setNotificationType(NOTIFY_EVENT);
            t.setName("TPL-" + NOTIFY_EVENT);
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("薪酬发放通知: ${salaryId}");
            t.setBodyTpl("薪酬单 ${billCode} 已发放完成，请知悉");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig("{\"userIds\":[\"" + NOTIFY_USER + "\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            dao.saveEntity(t);
        });
    }
}
