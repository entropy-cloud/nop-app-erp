package app.erp.hr.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig;
import app.erp.hr.dao.entity.ErpHrTaxConfig;
import app.erp.hr.service.payroll.SocialInsuranceCalculator;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 薪酬计提过账链端到端测试（RC-R1.89 / P1-MA4-017，payroll.md §6.5/§9.1）。验证 approve→APPROVED 联动：
 * <ul>
 *   <li>① approve → SALARY(270) + SOCIAL_INSURANCE_ER(290) + HOUSING_FUND_ER(300) 三凭证生成 +
 *       businessType/科目/金额断言（含 ER 重算值）+ 三凭证 Dr==Cr 试算平衡收敛（Q4 关键证据）。</li>
 *   <li>② billData ER 键持久化：290/300 失败路径经 ErpFinPostingException.eventData JSON 断言
 *       SOCIAL_INSURANCE_ER/HOUSING_FUND_ER 键与金额（成功路径 billData 为 transient，ER 金额经凭证行持久化）。</li>
 *   <li>③ 计提链部分失败 → posted=false + G3 告警派发（hr.salary-posting-failure）+ 280 markPaid 不受阻断。</li>
 *   <li>④ markPaid→SALARY_PAYMENT(280) 零回归。</li>
 *   <li>⑤ 重算口径一致性：同基数据 SocialInsuranceCalculator 重算值 == calculateSalary 局部口径 == 凭证金额。</li>
 *   <li>⑥ REJECTED/UNSUBMITTED 零过账（posting 仅 APPROVED 触发）。</li>
 *   <li>⑦ 幂等：重复 approve 不重复计提（审批轴守卫拒二次 approve + posted=true 短路防御）。</li>
 *   <li>⑧ GraphQL 冒烟：approve 链经 GraphQL 引擎走 xbiz 覆写 source 全链。</li>
 *   <li>⑨ reverseApprove→再 approve 去重：无重复凭证 + 补投失败条目可续投（posted 收敛 true）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrSalaryPostingChain extends JunitAutoTestCase {

    @RegisterExtension
    static HrFrozenClockExtension frozenClock = new HrFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    /** 与 SalaryPostingDispatcher.NOTIFY_EVENT_SALARY_FAILURE 一致（跨包不可见，本地镜像字面量）。 */
    private static final String NOTIFY_EVENT = "hr.salary-posting-failure";

    @BeforeEach
    public void setUpWfUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrSalaryBiz salaryBiz;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    SocialInsuranceCalculator socialInsuranceCalculator;

    /**
     * ①⑤⑧：approve 联动生成 270/290/300 三凭证（科目/金额/ER 重算值断言）+ 三凭证 Dr==Cr 平衡 +
     * posted=true + ④ markPaid→280 零回归 + ⑧ GraphQL 全链冒烟。
     * 口径（15000 基数）：gross=15000.00；社保 ER=15000×15%=2250.00；公积金 ER=15000×12%=1800.00。
     */
    @Test
    public void testApprovePostsAccrualChainBalancedAndPaymentZeroRegression() {
        String employeeId = seedFullEnvironment(true);

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 4, CTX));
        String salaryId = salary.getId();
        BigDecimal gross = salary.getGrossSalary();
        assertEquals(0, new BigDecimal("15000.00").compareTo(gross), "应发合计=合同月薪（全勤）");

        // ⑧ GraphQL 冒烟：submit + approve 经 GraphQL 引擎走 xbiz 覆写 source（含委托接线）全链
        assertEquals(0, submitSalary(salaryId).getStatus(), "GraphQL submit 应成功");
        ApiResponse<?> approveResp = approveSalary(salaryId);
        assertEquals(0, approveResp.getStatus(), "GraphQL approve 应成功（⑧ 计提链经 xbiz 委托全链）");

        // ① posted writer（D3：三路全成才 true）
        ErpHrSalary approved = daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId);
        assertEquals(ErpHrConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());
        assertEquals(Boolean.TRUE, approved.getPosted(), "三路计提全成功 → posted=true（D3 语义）");

        // ① 三凭证 businessType 断言（280 此时为零——发放未触发）
        String billCode = billCode(2026, 4, salaryId);
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SALARY), "270 计提凭证恰一张");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER), "290 社保公司承担凭证恰一张");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.HOUSING_FUND_ER), "300 公积金公司承担凭证恰一张");
        assertEquals(0, countBillLinks(billCode, ErpFinBusinessType.SALARY_PAYMENT), "发放前无 280 凭证");

        // ①⑤ 270：借 6601 管理费用-工资 / 贷 2211 应付职工薪酬，金额=gross，Dr==Cr
        ErpFinVoucher v270 = postedVoucher(billCode, ErpFinBusinessType.SALARY);
        assertBalanced(v270, gross, "270 计提凭证");
        List<ErpFinVoucherLine> lines270 = voucherLines(v270);
        assertEquals(2, lines270.size());
        assertLine(lines270.get(0), "6601", gross, "270 借方 管理费用-工资");
        assertLine(lines270.get(1), "2211", gross, "270 贷方 应付职工薪酬");

        // ①⑤ 290：借 6601.01 管理费用-社保 / 贷 2211，金额=ER 重算值 2250.00（镜像 calculateSalary 口径）
        BigDecimal socialER = new BigDecimal("2250.00");
        assertEquals(0, socialER.compareTo(recomputedSocialER(employeeId, 2026, 4)),
                "⑤ ER 重算口径一致性：SocialInsuranceCalculator[1] 与硬算值一致");
        ErpFinVoucher v290 = postedVoucher(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER);
        assertBalanced(v290, socialER, "290 社保公司承担凭证");
        List<ErpFinVoucherLine> lines290 = voucherLines(v290);
        assertEquals(2, lines290.size());
        assertLine(lines290.get(0), "6601.01", socialER, "290 借方 管理费用-社保");
        assertLine(lines290.get(1), "2211", socialER, "290 贷方 应付职工薪酬-社保");

        // ①⑤ 300：借 6601.02 管理费用-公积金 / 贷 2211，金额=ER 重算值 1800.00
        BigDecimal fundER = new BigDecimal("1800.00");
        assertEquals(0, fundER.compareTo(recomputedHousingFundER(employeeId, 2026, 4)),
                "⑤ ER 重算口径一致性：calculateHousingFund[1] 与硬算值一致");
        ErpFinVoucher v300 = postedVoucher(billCode, ErpFinBusinessType.HOUSING_FUND_ER);
        assertBalanced(v300, fundER, "300 公积金公司承担凭证");
        List<ErpFinVoucherLine> lines300 = voucherLines(v300);
        assertEquals(2, lines300.size());
        assertLine(lines300.get(0), "6601.02", fundER, "300 借方 管理费用-公积金");
        assertLine(lines300.get(1), "2211", fundER, "300 贷方 应付职工薪酬-公积金");

        // ④ markPaid→280 零回归：发放照常 PAID + 280 凭证（净额）生成，计提三凭证不受影响
        ErpHrSalary paid = ormTemplate.runInSession(session -> salaryBiz.markPaid(salaryId, CTX));
        assertEquals(ErpHrConstants.PAYMENT_PAID, paid.getPaymentStatus(), "④ 发放正常 PAID");
        ErpFinVoucher v280 = postedVoucher(billCode, ErpFinBusinessType.SALARY_PAYMENT);
        assertBalanced(v280, paid.getNetSalary(), "280 发放凭证");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SALARY), "④ 计提凭证不受发放影响");
        assertEquals(Boolean.TRUE, daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId).getPosted());
    }

    /**
     * ③②：计提链部分失败（270 成功、290/300 因缺 6601.01/6601.02 科目失败）→ posted=false +
     * G3 告警派发 + 280 markPaid 不受阻断；② 失败路径 billData ER 键经 eventData JSON 持久化断言。
     */
    @Test
    public void testPartialFailureKeepsPostedFalseAlertsAndPaymentUnblocked() {
        String employeeId = seedFullEnvironment(false);

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 4, CTX));
        String salaryId = salary.getId();
        assertEquals(0, submitSalary(salaryId).getStatus(), "提交应成功");
        assertEquals(0, approveSalary(salaryId).getStatus(), "审核应成功（部分失败不阻塞 APPROVED）");

        ErpHrSalary approved = daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId);
        assertEquals(ErpHrConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(), "③ 过账失败不阻塞审批终态");
        assertFalse(Boolean.TRUE.equals(approved.getPosted()), "③ 290/300 失败 → posted=false（D3 语义）");

        String billCode = billCode(2026, 4, salaryId);
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SALARY), "270 已过账");
        assertEquals(0, countBillLinks(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER), "290 失败未过账");
        assertEquals(0, countBillLinks(billCode, ErpFinBusinessType.HOUSING_FUND_ER), "300 失败未过账");

        // ③ G3 告警闭环：hr.salary-posting-failure 派发（模板 MERGE_BY_USER_TYPE + 300s 合并窗口
        // 将社保/公积金两 stage 告警合并为单条——闭环可观测性以 ≥1 条断言）
        assertTrue(countNotifications(NOTIFY_EVENT) >= 1,
                "③ G3 告警已派发（社保+公积金 stage，合并窗口内单条化），实测=" + countNotifications(NOTIFY_EVENT));

        // ② billData ER 键持久化：290 失败事件的 eventData JSON 含 SOCIAL_INSURANCE_ER 键与重算金额
        ErpFinPostingException ex290 = findPostingException(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER);
        assertNotNull(ex290, "290 失败已落过账异常工作台");
        Map<String, Object> eventData = JsonTool.parseBeanFromText(ex290.getEventData(), Map.class);
        assertEquals(0, new BigDecimal("2250.00").compareTo(new BigDecimal(eventData.get("SOCIAL_INSURANCE_ER").toString())),
                "② eventData 含 SOCIAL_INSURANCE_ER 键与 ER 重算金额");
        assertEquals(0, new BigDecimal("2250.00").compareTo(new BigDecimal(eventData.get("GROSS_AMOUNT").toString())),
                "② GROSS_AMOUNT 消费键承载 ER 金额");
        assertFalse(eventData.containsKey("NET_AMOUNT"), "② 290 事件不带 NET_AMOUNT（防 ER=0 回退读净额）");

        // ③ 280 markPaid 不受计提失败阻断：照常 PAID + 发放凭证
        ErpHrSalary paid = ormTemplate.runInSession(session -> salaryBiz.markPaid(salaryId, CTX));
        assertEquals(ErpHrConstants.PAYMENT_PAID, paid.getPaymentStatus(), "③ 发放不受计提失败阻断");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SALARY_PAYMENT), "280 发放凭证正常生成");
        assertFalse(Boolean.TRUE.equals(daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId).getPosted()),
                "③ 发放不修复 posted（计提链仍不完整）");
    }

    /** ⑥：UNSUBMITTED 直接 approve 被守卫拒（零过账）；SUBMITTED→reject → REJECTED（零过账）。 */
    @Test
    public void testUnsubmittedAndRejectedDoNotPost() {
        String employeeId = seedFullEnvironment(true);

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 4, CTX));
        String salaryId = salary.getId();

        ApiResponse<?> badApprove = approveSalary(salaryId);
        assertEquals(-1, badApprove.getStatus(), "⑥ UNSUBMITTED 直接 approve 被守卫拒");
        assertEquals(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION.getErrorCode(), badApprove.getCode());

        assertEquals(0, submitSalary(salaryId).getStatus(), "提交应成功");
        assertEquals(0, rejectSalary(salaryId).getStatus(), "驳回应成功");
        assertEquals(ErpHrConstants.APPROVE_STATUS_REJECTED,
                daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId).getApproveStatus());

        String billCode = billCode(2026, 4, salaryId);
        assertEquals(0, countBillLinks(billCode, ErpFinBusinessType.SALARY), "⑥ REJECTED 零计提过账");
        assertEquals(0, countBillLinks(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER), "⑥ 零 290 过账");
        assertEquals(0, countBillLinks(billCode, ErpFinBusinessType.HOUSING_FUND_ER), "⑥ 零 300 过账");
        assertFalse(Boolean.TRUE.equals(daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId).getPosted()));
    }

    /** ⑦：幂等——二次 approve 被审批轴守卫拒（SUBMITTED 单源），凭证计数不变。 */
    @Test
    public void testIdempotentSecondApproveDoesNotDuplicate() {
        String employeeId = seedFullEnvironment(true);

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 4, CTX));
        String salaryId = salary.getId();
        assertEquals(0, submitSalary(salaryId).getStatus());
        assertEquals(0, approveSalary(salaryId).getStatus());

        String billCode = billCode(2026, 4, salaryId);
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SALARY));
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER));
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.HOUSING_FUND_ER));

        ApiResponse<?> second = approveSalary(salaryId);
        assertEquals(-1, second.getStatus(), "⑦ 二次 approve 被守卫拒（APPROVED 非 SUBMITTED 源态）");
        assertEquals(ErpHrErrors.ERR_SALARY_ILLEGAL_STATUS_TRANSITION.getErrorCode(), second.getCode());

        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SALARY), "⑦ 无重复 270");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER), "⑦ 无重复 290");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.HOUSING_FUND_ER), "⑦ 无重复 300");
    }

    /**
     * ⑨：reverseApprove→再 approve 去重——首_approve 部分失败（仅 270 过账）后反审核，补齐缺失科目再审核：
     * 270 去重守卫命中跳过（无重复凭证）+ 290/300 补投成功 → posted 收敛 true（补投失败条目可续投）。
     */
    @Test
    public void testReverseApproveReApproveDedupAndCatchUp() {
        String employeeId = seedFullEnvironment(false);

        ErpHrSalary salary = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 4, CTX));
        String salaryId = salary.getId();
        assertEquals(0, submitSalary(salaryId).getStatus());
        assertEquals(0, approveSalary(salaryId).getStatus());
        String billCode = billCode(2026, 4, salaryId);
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SALARY), "首_approve：270 已过账");
        assertFalse(Boolean.TRUE.equals(daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId).getPosted()),
                "首_approve：290/300 失败 posted=false");

        // 反审核：APPROVED→SUBMITTED（计提凭证不红冲——Deferred successor，去重守卫防重复计提）
        assertEquals(0, reverseApproveSalary(salaryId).getStatus(), "反审核应成功");
        assertEquals(ErpHrConstants.APPROVE_STATUS_SUBMITTED,
                daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId).getApproveStatus());

        // 补投条件修复：补种 6601.01/6601.02 科目后再审核
        ormTemplate.runInSession(session -> {
            seedSubject("6601.01", "管理费用-社保");
            seedSubject("6601.02", "管理费用-公积金");
            return null;
        });
        assertEquals(0, approveSalary(salaryId).getStatus(), "再审核应成功");

        // ⑨ 去重：270 不重复（守卫命中跳过）+ 290/300 补投成功 + posted 收敛 true
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SALARY), "⑨ 270 去重守卫：无重复计提凭证");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER), "⑨ 290 补投成功");
        assertEquals(1, countBillLinks(billCode, ErpFinBusinessType.HOUSING_FUND_ER), "⑨ 300 补投成功");
        assertEquals(Boolean.TRUE, daoProvider.daoFor(ErpHrSalary.class).getEntityById(salaryId).getPosted(),
                "⑨ 补投失败条目续投后 posted 收敛 true");
        assertBalanced(postedVoucher(billCode, ErpFinBusinessType.SOCIAL_INSURANCE_ER), new BigDecimal("2250.00"),
                "⑨ 补投 290 凭证平衡");
    }

    // ---------- assertion helpers ----------

    private void assertBalanced(ErpFinVoucher voucher, BigDecimal amount, String label) {
        assertEquals(0, amount.compareTo(voucher.getTotalDebit()), label + " 借方合计=金额");
        assertEquals(0, amount.compareTo(voucher.getTotalCredit()), label + " 贷方合计=金额");
        assertEquals(0, voucher.getTotalDebit().compareTo(voucher.getTotalCredit()), label + " Dr==Cr 试算平衡收敛");
        assertEquals("POSTED", voucher.getDocStatus(), label + " 已过账");
        assertFalse(Boolean.TRUE.equals(voucher.getIsReversed()), label + " 未冲销");
    }

    private void assertLine(ErpFinVoucherLine line, String subjectCode, BigDecimal amount, String label) {
        assertEquals(subjectCode, line.getSubjectCode(), label + " 科目");
        assertEquals(0, amount.compareTo(line.getDebitAmount().add(line.getCreditAmount())), label + " 金额");
    }

    private ErpFinVoucher postedVoucher(String billCode, ErpFinBusinessType type) {
        List<ErpFinVoucherBillR> links = findBillLinks(billCode, type);
        assertEquals(1, links.size(), type + " 回链应唯一");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
        assertNotNull(voucher);
        return voucher;
    }

    private List<ErpFinVoucherLine> voucherLines(ErpFinVoucher voucher) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucher.getId()));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, ErpFinBusinessType type) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        q.addFilter(eq("businessType", type.name()));
        return dao.findAllByQuery(q);
    }

    private int countBillLinks(String billCode, ErpFinBusinessType type) {
        return findBillLinks(billCode, type).size();
    }

    private ErpFinPostingException findPostingException(String billCode, ErpFinBusinessType type) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billHeadCode", billCode));
        q.addFilter(eq("businessType", type.name()));
        q.setLimit(1);
        List<ErpFinPostingException> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private int countNotifications(String eventType) {
        IEntityDao<ErpSysNotification> dao = daoProvider.daoFor(ErpSysNotification.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        return dao.findAllByQuery(q).size();
    }

    private BigDecimal recomputedSocialER(String employeeId, int year, int month) {
        return socialInsuranceCalculator.calculate(employeeId, year, month)[1]
                .setScale(ErpHrConfigs.salaryRoundingScale(), java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal recomputedHousingFundER(String employeeId, int year, int month) {
        return socialInsuranceCalculator.calculateHousingFund(employeeId, year, month)[1]
                .setScale(ErpHrConfigs.salaryRoundingScale(), java.math.RoundingMode.HALF_UP);
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submitSalary(String salaryId) {
        return executeRpc(mutation, "ErpHrSalary__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> approveSalary(String salaryId) {
        return executeRpc(mutation, "ErpHrSalary__approve", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> rejectSalary(String salaryId) {
        return executeRpc(mutation, "ErpHrSalary__reject", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> reverseApproveSalary(String salaryId) {
        return executeRpc(mutation, "ErpHrSalary__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    /**
     * 完整计提过账环境。withCompanyBorneSubjects=false 时不种 6601.01/6601.02（构造 290/300 失败于
     * resolveSubjects 的部分失败场景——270 只需 6601+2211）。
     * 凭证日期：计提=期间 15 日（2026-04-15 → 2026-04 OPEN 期间）；发放=paymentDate（冻结时钟
     * 2026-07-17 → 2026-07 OPEN 期间）。
     */
    private String seedFullEnvironment(boolean withCompanyBorneSubjects) {
        return ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            String empId = seedEmployee("EMP-CHAIN");
            seedContract(empId, "15000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "15000", "15000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");

            seedSubject("6601", "管理费用-工资");
            if (withCompanyBorneSubjects) {
                seedSubject("6601.01", "管理费用-社保");
                seedSubject("6601.02", "管理费用-公积金");
            }
            seedSubject("2211", "应付职工薪酬");
            seedSubject("1002", "银行存款");
            seedAcctSchema("1");
            seedPeriod(2026, 4, "OPEN");
            seedPeriod(2026, 7, "OPEN");
            seedNotifyTemplate(NOTIFY_EVENT);
            return empId;
        });
    }

    /** 种组织主账套（引擎 resolveTargetSchemas 对 null acctSchemaId 返回空集 → 静默零凭证的补齐前置）。 */
    private void seedAcctSchema(String orgId) {
        IEntityDao<app.erp.md.dao.entity.ErpMdAcctSchema> dao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class);
        app.erp.md.dao.entity.ErpMdAcctSchema schema = new app.erp.md.dao.entity.ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId("1");
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedTaxConfig(int year) {
        IEntityDao<ErpHrTaxConfig> dao = daoProvider.daoFor(ErpHrTaxConfig.class);
        ErpHrTaxConfig cfg = new ErpHrTaxConfig();
        cfg.setYear(year);
        cfg.setTaxThreshold(new BigDecimal("5000"));
        cfg.setTaxBrackets("["
                + "{\"rangeUpperLimit\":36000,\"rate\":0.03,\"quickDeduction\":0},"
                + "{\"rangeUpperLimit\":144000,\"rate\":0.10,\"quickDeduction\":2520},"
                + "{\"rangeUpperLimit\":300000,\"rate\":0.20,\"quickDeduction\":16920},"
                + "{\"rangeUpperLimit\":420000,\"rate\":0.25,\"quickDeduction\":31920},"
                + "{\"rangeUpperLimit\":660000,\"rate\":0.30,\"quickDeduction\":52920},"
                + "{\"rangeUpperLimit\":960000,\"rate\":0.35,\"quickDeduction\":85920},"
                + "{\"rangeUpperLimit\":null,\"rate\":0.45,\"quickDeduction\":181920}"
                + "]");
        dao.saveEntity(cfg);
    }

    private String seedEmployee(String code) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName("计提链测试员工");
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(ErpHrConstants.EMPLOYMENT_ACTIVE);
        emp.setEmployeeType("FULL_TIME");
        // 业务组织（PostingEvent.orgId/账套解析回退源，对齐 assets 域过账测试范式）
        emp.setOrgId("1");
        dao.saveEntity(emp);
        return emp.getId();
    }

    private void seedContract(String employeeId, String monthlySalary) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setBusinessDate(LocalDate.of(2026, 7, 1));
        c.setCode("C-" + employeeId);
        c.setEmployeeId(employeeId);
        c.setContractType("OPEN_ENDED");
        c.setSignDate(LocalDate.of(2025, 1, 1));
        c.setStartDate(LocalDate.of(2025, 1, 1));
        c.setMonthlySalary(new BigDecimal(monthlySalary));
        c.setStatus("ACTIVE");
        dao.saveEntity(c);
    }

    private void seedSocialInsuranceBase(String employeeId, String cityCode,
                                         String socialInsuranceBase, String housingFundBase) {
        IEntityDao<ErpHrSocialInsuranceBase> dao = daoProvider.daoFor(ErpHrSocialInsuranceBase.class);
        ErpHrSocialInsuranceBase base = new ErpHrSocialInsuranceBase();
        base.setEmployeeId(employeeId);
        base.setCityCode(cityCode);
        base.setSocialInsuranceBase(new BigDecimal(socialInsuranceBase));
        base.setHousingFundBase(new BigDecimal(housingFundBase));
        base.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        dao.saveEntity(base);
    }

    private void seedSocialInsuranceConfig(String cityCode, String insuranceType,
                                           String companyRate, String employeeRate,
                                           String lower, String upper) {
        IEntityDao<ErpHrSocialInsuranceConfig> dao = daoProvider.daoFor(ErpHrSocialInsuranceConfig.class);
        ErpHrSocialInsuranceConfig cfg = new ErpHrSocialInsuranceConfig();
        cfg.setCityCode(cityCode);
        cfg.setInsuranceType(insuranceType);
        cfg.setCompanyRate(new BigDecimal(companyRate));
        cfg.setEmployeeRate(new BigDecimal(employeeRate));
        cfg.setBaseLowerLimit(new BigDecimal(lower));
        cfg.setBaseUpperLimit(new BigDecimal(upper));
        dao.saveEntity(cfg);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<app.erp.md.dao.entity.ErpMdSubject> dao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdSubject.class);
        app.erp.md.dao.entity.ErpMdSubject subject = new app.erp.md.dao.entity.ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(code.startsWith("66") ? "EXPENSE" : "LIABILITY");
        subject.setDirection(code.startsWith("66") ? "DEBIT" : "CREDIT");
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedPeriod(int year, int month, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(year + "-" + String.format("%02d", month));
        period.setName(year + "年" + month + "月");
        period.setOrgId("1");
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(LocalDate.of(year, month, 1));
        period.setEndDate(LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth()));
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private void seedNotifyTemplate(String eventType) {
        IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
        ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
        t.setNotificationType(eventType);
        t.setName("薪酬过账失败告警");
        t.setChannelSet("IN_APP");
        t.setSubjectTpl("薪酬过账失败: ${postingNo}");
        t.setBodyTpl("薪酬 ${salaryId} 过账失败 stage=${stage}");
        t.setRecipientResolver("USER_LIST");
        t.setRecipientConfig("{\"userIds\":[\"0\"]}");
        t.setMergeWindowSeconds(300);
        t.setMergeStrategy("MERGE_BY_USER_TYPE");
        t.setStatus("ACTIVE");
        dao.saveEntity(t);
    }

    /** 与 SalaryPostingDispatcher.buildBillCode 一致。 */
    private String billCode(int year, int month, String salaryId) {
        return "SAL-" + year + String.format("%02d", month) + "-" + salaryId;
    }
}
