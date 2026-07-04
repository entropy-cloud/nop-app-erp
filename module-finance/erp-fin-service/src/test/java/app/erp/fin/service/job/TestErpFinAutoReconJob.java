package app.erp.fin.service.job;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定时自动核销 Job Bean 测试（plan 2026-07-05-0115-1 Phase 2）。
 *
 * <p>覆盖：cron 空值跳过、cron 非空触发 runAutoReconciliation（RECEIVABLE + PAYABLE）、
 * BeanMethodJobInvoker 反射调用 execute() 签名正确（无参 public method）。
 *
 * <p>不依赖 Mockito；用可重写 {@code resolveCronConfig()} 的匿名子类 + 计数 runDirection 装饰器验证门控语义。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:auto-recon-job-test.yaml")
public class TestErpFinAutoReconJob extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinReconciliationBiz reconciliationBiz;

    @Test
    public void testCronConfiguredTriggersBothDirections() {
        // auto-recon-job-test.yaml 设 erp-fin.ar-ap-auto-recon-cron 非空 + auto-reconcile=true
        long partnerId = 2100L;
        seedItem(partnerId, ErpFinConstants.SOURCE_BILL_PAYMENT, "2100-PAY",
                new BigDecimal("100"), LocalDate.of(2026, 6, 15));
        seedItem(partnerId, ErpFinConstants.SOURCE_BILL_AP_INVOICE, "2100-AP",
                new BigDecimal("100"), LocalDate.of(2026, 5, 20));

        CountingJob job = new CountingJob(reconciliationBiz, "0 0 1 * * ?");
        job.execute();

        assertEquals(1, job.recvCalls, "RECEIVABLE 方向应被调用 1 次");
        assertEquals(1, job.payableCalls, "PAYABLE 方向应被调用 1 次");
        // PAYABLE 方向应已核销（job 调 runAutoReconciliation(PAYABLE,null,FIFO)）
        ErpFinArApItem inv = findItem("2100-AP");
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, inv.getStatus(),
                "PAYABLE 方向发票应被 job 自动核销");
    }

    @Test
    public void testCronEmptySkipsExecution() {
        CountingJob job = new CountingJob(reconciliationBiz, "");
        job.execute();
        assertEquals(0, job.recvCalls, "cron 空值应跳过 RECEIVABLE");
        assertEquals(0, job.payableCalls, "cron 空值应跳过 PAYABLE");
    }

    @Test
    public void testExecuteIsNoArgPublicMethod() throws NoSuchMethodException {
        // BeanMethodJobInvoker 反射查找 public no-arg method named "execute"
        java.lang.reflect.Method m = ErpFinAutoReconJob.class.getMethod("execute");
        assertEquals("execute", m.getName());
        assertEquals(0, m.getParameterCount(), "execute() 必须无参以适配 BeanMethodJobInvoker");
        assertTrue(java.lang.reflect.Modifier.isPublic(m.getModifiers()),
                "execute() 必须为 public 以便 BeanMethodJobInvoker 反射调用");
    }

    // ---------- helpers ----------

    private void seedItem(long partnerId, String sourceBillType, String sourceBillCode,
                          BigDecimal amount, LocalDate businessDate) {
        ormTemplate.runInSession(() -> {
            seedPartner(partnerId);
            IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
            ErpFinArApItem it = dao.newEntity();
            it.setCode("ARI-" + sourceBillCode);
            it.setOrgId(1L);
            it.setAcctSchemaId(1L);
            it.setDirection(ErpFinConstants.DIRECTION_PAYABLE);
            it.setPartnerId(partnerId);
            it.setSourceBillType(sourceBillType);
            it.setSourceBillCode(sourceBillCode);
            it.setBusinessDate(businessDate);
            it.setCurrencyId(1L);
            it.setExchangeRate(BigDecimal.ONE);
            it.setAmountSource(amount);
            it.setAmountFunctional(amount);
            it.setSettledAmountSource(BigDecimal.ZERO);
            it.setSettledAmountFunctional(BigDecimal.ZERO);
            it.setOpenAmountSource(amount);
            it.setOpenAmountFunctional(amount);
            it.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
            dao.saveEntity(it);
        });
    }

    private void seedPartner(long partnerId) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        if (dao.getEntityById(partnerId) != null) {
            return;
        }
        ErpMdPartner partner = new ErpMdPartner();
        partner.orm_propValue(1, partnerId);
        partner.setCode("P-" + partnerId);
        partner.setName("Partner " + partnerId);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus("ACTIVE");
        partner.setReceivableBalance(BigDecimal.ZERO);
        partner.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(partner);
    }

    private ErpFinArApItem findItem(String sourceBillCode) {
        for (ErpFinArApItem it : daoProvider.daoFor(ErpFinArApItem.class).findAll()) {
            if (sourceBillCode.equals(it.getSourceBillCode())) {
                return it;
            }
        }
        return null;
    }

    /** Job 子类：固定 cron + 统计 runDirection 各方向调用次数。 */
    private static class CountingJob extends ErpFinAutoReconJob {
        private final String cron;
        int recvCalls;
        int payableCalls;

        CountingJob(IErpFinReconciliationBiz biz, String cron) {
            this.reconciliationBiz = biz;
            this.cron = cron;
        }

        @Override
        protected String resolveCronConfig() {
            return cron;
        }

        @Override
        protected void runDirection(String direction, IServiceContext ctx) {
            if (ErpFinConstants.DIRECTION_RECEIVABLE.equals(direction)) {
                recvCalls++;
            } else if (ErpFinConstants.DIRECTION_PAYABLE.equals(direction)) {
                payableCalls++;
            }
            super.runDirection(direction, ctx);
        }
    }
}
