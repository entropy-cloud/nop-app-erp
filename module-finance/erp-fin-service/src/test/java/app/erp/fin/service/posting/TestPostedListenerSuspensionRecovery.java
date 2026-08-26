package app.erp.fin.service.posting;

import app.erp.common.service.ErpCrudStatusLock;
import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F2.1（P1-CK-fin-001）：悬挂闭环集成——模拟「凭证已落账 + 源单 posted=false + PENDING 异常」，
 * sweep retry 幂等命中（F1.1 返回既有 id）→ dispatchPostedEvent → FinPostedListener 回写 posted=true。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestPostedListenerSuspensionRecovery extends app.erp.fin.service.entity.PeriodCloseTestSupport {

    static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IErpFinVoucherBiz voucherBiz;
    @Inject
    ErpFinDeferredPostingRetryHelper retryHelper;
    @Inject
    ErpFinPostedListenerRegistry postedListenerRegistry;

    @Test
    public void testSweepRetryWritesBackPosted() {
        // 1. 种子：开放期间 + 科目 + APPROVED 未过账报销单（posted=false）
        ormTemplate.runInSession(sess -> {
            seedOpenPeriod("2026-10", 2026, 10);
            seedSubject("6602", "管理费用", "EXPENSE", ErpFinConstants.DC_DEBIT);
            seedSubject("2221", "应交税费-进项税额", "ASSET", ErpFinConstants.DC_DEBIT);
            seedSubject("1002", "银行存款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedSubject("2241", "其他应付款-员工", "ASSET", ErpFinConstants.DC_CREDIT);

            ErpFinExpenseClaim claim = new ErpFinExpenseClaim();
            claim.setCode("EC-F21-SUSP");
            claim.setOrgId("1");
            claim.setBusinessDate(LocalDate.of(2026, 10, 5));
            claim.setCurrencyId("1");
            claim.setExchangeRate(BigDecimal.ONE);
            claim.setAmountSource(new BigDecimal("200"));
            claim.setAmountFunctional(new BigDecimal("200"));
            claim.setPaymentMode("OWN_ACCOUNT");
            claim.setClaimantId("1");
            claim.setReason("F2.1悬挂闭环测试");
            claim.setDocStatus("APPROVED");
            claim.setApproveStatus("APPROVED");
            claim.setPosted(false);
            daoProvider.daoFor(ErpFinExpenseClaim.class).saveEntity(claim);
            return claim.getId();
        });
        String claimId = ormTemplate.runInSession(sess -> {
            for (ErpFinExpenseClaim c : daoProvider.daoFor(ErpFinExpenseClaim.class).findAllByQuery(
                    new io.nop.api.core.beans.query.QueryBean())) {
                if ("EC-F21-SUSP".equals(c.getCode())) {
                    return c.getId();
                }
            }
            return null;
        });
        assertNotNull(claimId, "种子报销单就绪");

        // 2. 模拟悬挂：首次过账成功（凭证落账）但调用方侧回滚/置位失败 → posted 仍 false
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.EXPENSE_CLAIM);
        event.setBillHeadCode("EC-F21-SUSP");
        event.setOrgId("1");
        event.setAcctSchemaId("1");
        event.setCurrencyId("1");
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(LocalDate.of(2026, 10, 5));
        event.getBillData().put("TOTAL_AMOUNT", new BigDecimal("200"));
        event.getBillData().put("TOTAL_TAX_AMOUNT", BigDecimal.ZERO);
        event.getBillData().put("TOTAL_AMOUNT_WITH_TAX", new BigDecimal("200"));
        event.getBillData().put("PAYMENT_MODE", "OWN_ACCOUNT");
        event.getBillData().put("EMPLOYEE_ID", "1");
        event.getBillData().put("claimCode", "EC-F21-SUSP");
        String voucherId = ormTemplate.runInSession(sess -> voucherBiz.post(event, CTX));
        assertNotNull(voucherId, "首次过账成功（凭证落账）");
        Boolean postedAfterPost = ormTemplate.runInSession(sess ->
                daoProvider.daoFor(ErpFinExpenseClaim.class).getEntityById(claimId).getPosted());
        assertTrue(postedAfterPost == null || !Boolean.TRUE.equals(postedAfterPost),
                "模拟悬挂：调用方置位失败，posted 仍 false");

        // 3. 种 PENDING 异常记录（sweep 输入）
        String exId = ormTemplate.runInSession(sess -> {
            app.erp.fin.dao.entity.ErpFinPostingException ex =
                    daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class).newEntity();
            ex.setTraceId("F21-SUSP-TEST");
            ex.setBillHeadCode("EC-F21-SUSP");
            ex.setBusinessType(ErpFinBusinessType.EXPENSE_CLAIM.name());
            ex.setPostingType("NORMAL");
            ex.setErrorCode("test.suspended");
            ex.setErrorMessage("simulated suspension");
            ex.setFailedStage("persistVoucher");
            ex.setVoucherDate(LocalDate.of(2026, 10, 5));
            ex.setOrgId("1");
            ex.setAcctSchemaId("1");
            ex.setCurrencyId("1");
            ex.setExchangeRate(BigDecimal.ONE);
            ex.setOccurrenceTime(java.sql.Timestamp.valueOf("2026-10-05 10:00:00"));
            ex.setRetryCount(0);
            ex.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING);
            ex.setEventData(ErpFinPostingExceptionRecorder.serializeEventData(event.getBillData()));
            daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class).saveEntity(ex);
            return ex.getId();
        });

        // 4. sweep retry：幂等命中返回既有 id → dispatchPostedEvent → listener 回写 posted=true
        // 对齐既有范式（TestErpFinPostingExceptionWorkbench L228）：runInSession 内调 retry
        boolean retried = ormTemplate.runInSession(session -> retryHelper.retry(exId, CTX));
        assertTrue(retried, "重试成功（幂等命中 + 派发）");

        Boolean postedFinal = ormTemplate.runInSession(sess ->
                daoProvider.daoFor(ErpFinExpenseClaim.class).getEntityById(claimId).getPosted());
        assertEquals(Boolean.TRUE, postedFinal,
                "F2.1 核心断言：悬挂闭环——sweep 重试后源单 posted=true（修复前永久 false）");
    }

    /**
     * F2.1 失败隔离变体：监听者抛错 → 不中断其他监听者（FinPostedListener 仍回写 posted=true）、
     * 不回滚 RETRIED、失败经 recorder 落工作台（errorCode=posted-listener-failed +
     * failedStage=notify-posted-listener + eventData 透传形成下轮 sweep 自愈）。
     */
    @Test
    public void testListenerFailureIsolatedToWorkbench() {
        String billCode = "EC-F21-FAIL";
        // 捕获派发（验证 REVERSAL 不派发的对偶断言亦复用此桩注册范式）
        postedListenerRegistry.addListener(new FailingListener(billCode));

        String claimId = ormTemplate.runInSession(sess -> {
            seedOpenPeriod("2026-10", 2026, 10);
            seedSubject("6602", "管理费用", "EXPENSE", ErpFinConstants.DC_DEBIT);
            seedSubject("2221", "应交税费-进项税额", "ASSET", ErpFinConstants.DC_DEBIT);
            seedSubject("1002", "银行存款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedSubject("2241", "其他应付款-员工", "ASSET", ErpFinConstants.DC_CREDIT);
            ErpFinExpenseClaim claim = new ErpFinExpenseClaim();
            claim.setCode(billCode);
            claim.setOrgId("1");
            claim.setBusinessDate(LocalDate.of(2026, 10, 5));
            claim.setCurrencyId("1");
            claim.setExchangeRate(BigDecimal.ONE);
            claim.setAmountSource(new BigDecimal("300"));
            claim.setAmountFunctional(new BigDecimal("300"));
            claim.setPaymentMode(ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
            claim.setClaimantId("1");
            claim.setReason("F2.1失败隔离测试");
            claim.setDocStatus("APPROVED");
            claim.setApproveStatus("APPROVED");
            claim.setPosted(false);
            daoProvider.daoFor(ErpFinExpenseClaim.class).saveEntity(claim);
            return claim.getId();
        });

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.EXPENSE_CLAIM);
        event.setBillHeadCode(billCode);
        event.setOrgId("1");
        event.setAcctSchemaId("1");
        event.setCurrencyId("1");
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(LocalDate.of(2026, 10, 5));
        event.getBillData().put("TOTAL_AMOUNT", new BigDecimal("300"));
        event.getBillData().put("TOTAL_TAX_AMOUNT", BigDecimal.ZERO);
        event.getBillData().put("TOTAL_AMOUNT_WITH_TAX", new BigDecimal("300"));
        event.getBillData().put("PAYMENT_MODE", ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        event.getBillData().put("EMPLOYEE_ID", "1");
        event.getBillData().put("claimCode", billCode);
        assertNotNull(ormTemplate.runInSession(sess -> voucherBiz.post(event, CTX)), "凭证落账");

        String exId = ormTemplate.runInSession(sess -> {
            app.erp.fin.dao.entity.ErpFinPostingException ex =
                    daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class).newEntity();
            ex.setTraceId("F21-FAIL-TEST");
            ex.setBillHeadCode(billCode);
            ex.setBusinessType(ErpFinBusinessType.EXPENSE_CLAIM.name());
            ex.setPostingType("NORMAL");
            ex.setErrorCode("test.suspended");
            ex.setErrorMessage("simulated suspension");
            ex.setFailedStage("persistVoucher");
            ex.setVoucherDate(LocalDate.of(2026, 10, 5));
            ex.setOrgId("1");
            ex.setAcctSchemaId("1");
            ex.setCurrencyId("1");
            ex.setExchangeRate(BigDecimal.ONE);
            ex.setOccurrenceTime(java.sql.Timestamp.valueOf("2026-10-05 10:00:00"));
            ex.setRetryCount(0);
            ex.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING);
            ex.setEventData(ErpFinPostingExceptionRecorder.serializeEventData(event.getBillData()));
            daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class).saveEntity(ex);
            return ex.getId();
        });

        boolean retried = ormTemplate.runInSession(session -> retryHelper.retry(exId, CTX));
        assertTrue(retried, "监听者失败不阻断：原异常记录仍 RETRIED（凭证法律效力不回滚）");

        assertEquals(Boolean.TRUE, ormTemplate.runInSession(sess ->
                        daoProvider.daoFor(ErpFinExpenseClaim.class).getEntityById(claimId).getPosted()),
                "失败隔离：FinPostedListener（其他监听者）不受失败监听者影响，仍回写 posted=true");

        java.util.List<app.erp.fin.dao.entity.ErpFinPostingException> failures =
                ormTemplate.runInSession(sess -> {
                    io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
                    q.addFilter(io.nop.api.core.beans.FilterBeans.eq("errorCode",
                            ErpFinPostingErrors.ERR_POSTED_LISTENER_FAILED.getErrorCode()));
                    return daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class)
                            .findAllByQuery(q);
                });
        assertEquals(1, failures.size(), "监听者失败落工作台（自愈入口）");
        app.erp.fin.dao.entity.ErpFinPostingException failure = failures.get(0);
        assertEquals(ErpFinConstants.FAILED_STAGE_NOTIFY_POSTED_LISTENER, failure.getFailedStage(),
                "failedStage=notify-posted-listener");
        assertEquals(billCode, failure.getBillHeadCode(), "失败记录指向同源单");
        assertEquals(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING, failure.getStatus(),
                "失败记录 PENDING 供下轮 sweep 重派");
        assertTrue(failure.getEventData() != null && !failure.getEventData().isEmpty(),
                "eventData 透传（下轮 sweep 幂等命中再派发自愈）");
    }

    /**
     * F2.1 REVERSAL 分支不派发守卫：红冲重试成功后不构造 VoucherPostedEvent（反向语义经
     * ReversalListenerRegistry 独立通道，posted 事件仅正向两通道派发）。
     */
    @Test
    public void testReversalRetryDoesNotDispatchPostedEvent() {
        CapturingListener capturing = new CapturingListener();
        postedListenerRegistry.addListener(capturing);

        String billCode = "EC-F21-REV";
        ormTemplate.runInSession(sess -> {
            seedOpenPeriod("2026-10", 2026, 10);
            seedSubject("6602", "管理费用", "EXPENSE", ErpFinConstants.DC_DEBIT);
            seedSubject("2221", "应交税费-进项税额", "ASSET", ErpFinConstants.DC_DEBIT);
            seedSubject("1002", "银行存款", "ASSET", ErpFinConstants.DC_DEBIT);
            seedSubject("2241", "其他应付款-员工", "ASSET", ErpFinConstants.DC_CREDIT);
            ErpFinExpenseClaim claim = new ErpFinExpenseClaim();
            claim.setCode(billCode);
            claim.setOrgId("1");
            claim.setBusinessDate(LocalDate.of(2026, 10, 5));
            claim.setCurrencyId("1");
            claim.setExchangeRate(BigDecimal.ONE);
            claim.setAmountSource(new BigDecimal("150"));
            claim.setAmountFunctional(new BigDecimal("150"));
            claim.setPaymentMode(ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
            claim.setClaimantId("1");
            claim.setReason("F2.1红冲不派发测试");
            claim.setDocStatus("APPROVED");
            claim.setApproveStatus("APPROVED");
            claim.setPosted(true);
            daoProvider.daoFor(ErpFinExpenseClaim.class).saveEntity(claim);
            return null;
        });

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.EXPENSE_CLAIM);
        event.setBillHeadCode(billCode);
        event.setOrgId("1");
        event.setAcctSchemaId("1");
        event.setCurrencyId("1");
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(LocalDate.of(2026, 10, 5));
        event.getBillData().put("TOTAL_AMOUNT", new BigDecimal("150"));
        event.getBillData().put("TOTAL_TAX_AMOUNT", BigDecimal.ZERO);
        event.getBillData().put("TOTAL_AMOUNT_WITH_TAX", new BigDecimal("150"));
        event.getBillData().put("PAYMENT_MODE", ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        event.getBillData().put("EMPLOYEE_ID", "1");
        event.getBillData().put("claimCode", billCode);
        assertNotNull(ormTemplate.runInSession(sess -> voucherBiz.post(event, CTX)), "正向凭证落账");

        String exId = ormTemplate.runInSession(sess -> {
            app.erp.fin.dao.entity.ErpFinPostingException ex =
                    daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class).newEntity();
            ex.setTraceId("F21-REV-TEST");
            ex.setBillHeadCode(billCode);
            ex.setBusinessType(ErpFinBusinessType.EXPENSE_CLAIM.name());
            ex.setPostingType("REVERSAL");
            ex.setErrorCode("test.reversal-pending");
            ex.setErrorMessage("simulated reversal pending");
            ex.setFailedStage("reverseProcess");
            ex.setVoucherDate(LocalDate.of(2026, 10, 5));
            ex.setOrgId("1");
            ex.setAcctSchemaId("1");
            ex.setCurrencyId("1");
            ex.setExchangeRate(BigDecimal.ONE);
            ex.setOccurrenceTime(java.sql.Timestamp.valueOf("2026-10-05 10:00:00"));
            ex.setRetryCount(0);
            ex.setStatus(ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING);
            daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinPostingException.class).saveEntity(ex);
            return ex.getId();
        });

        boolean retried = ormTemplate.runInSession(session -> retryHelper.retry(exId, CTX));
        assertTrue(retried, "红冲重试成功（RETRIED）");
        assertEquals(0, capturing.callCount.get(),
                "REVERSAL 分支不派发 VoucherPostedEvent（posted 事件仅正向两通道派发）");
    }

    /** 捕获 posted 派发的桩监听者（编程式注册，镜像 TestErpFinReversalDispatch 范式）。 */
    private static class CapturingListener implements IErpFinVoucherPostedListener {
        final java.util.concurrent.atomic.AtomicInteger callCount =
                new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public void onVoucherPosted(VoucherPostedEvent event, io.nop.core.context.IServiceContext context) {
            callCount.incrementAndGet();
        }
    }

    /** 对指定 billHeadCode 抛错的桩监听者（验证失败隔离 + 工作台自愈落盘）。 */
    private static class FailingListener implements IErpFinVoucherPostedListener {
        private final String targetBillCode;

        FailingListener(String targetBillCode) {
            this.targetBillCode = targetBillCode;
        }

        @Override
        public void onVoucherPosted(VoucherPostedEvent event, io.nop.core.context.IServiceContext context) {
            if (targetBillCode.equals(event.getBillHeadCode())) {
                throw new io.nop.api.core.exceptions.NopException(
                        ErpFinPostingErrors.ERR_POSTED_LISTENER_FAILED)
                        .param(ErpFinPostingErrors.ARG_BILL_HEAD_CODE, event.getBillHeadCode());
            }
        }
    }
}
