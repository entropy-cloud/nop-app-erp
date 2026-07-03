package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 期末结账期间状态机单测（Phase 1）。验证四态关账机 OPEN→CLOSING→CLOSED→CLOSED_FINAL 正向迁移、
 * 非法迁移拒绝、反结账 CLOSED_FINAL→OPEN，以及前置检查非阻断模式（auto-post-on-close=true）。
 *
 * <p>测试配置（{@code period-close-state-machine-test.yaml}）：反结账审批门控关闭、auto-post 提示模式、
 * 折旧/汇兑门控关闭（聚焦状态机，期末凭证生成在 Phase 2/3 验证）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:period-close-state-machine-test.yaml")
public class TestErpFinPeriodStateMachine extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinAccountingPeriodBiz periodCloseBiz;

    @Test
    public void testForwardAndReverse() {
        Long periodId = seedReturn(() -> seedOpenPeriod("2026-07", 2026, 7,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                ErpFinConstants.PERIOD_STATUS_OPEN));

        // OPEN → CLOSED
        ErpFinAccountingPeriod period = periodCloseBiz.closePeriod(periodId, CTX);
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, period.getStatus(), "结账后期间 CLOSED");

        // CLOSED → CLOSED_FINAL
        period = periodCloseBiz.finalizePeriod(periodId, CTX);
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, period.getStatus(), "最终锁定 CLOSED_FINAL");

        // CLOSED_FINAL → OPEN（反结账，审批门控已关闭）
        period = periodCloseBiz.reverseClose(periodId, CTX);
        assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, period.getStatus(), "反结账后回开 OPEN");

        // 反结账后可重新结账（幂等重结，§反结账步骤7）
        period = periodCloseBiz.closePeriod(periodId, CTX);
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, period.getStatus(), "重新结账成功");
    }

    @Test
    public void testIllegalTransitionsRejected() {
        Long periodId = seedReturn(() -> seedOpenPeriod("2026-08", 2026, 8,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                ErpFinConstants.PERIOD_STATUS_OPEN));

        // finalizePeriod 要求 CLOSED，当前 OPEN → 拒绝
        assertThrows(NopException.class, () -> periodCloseBiz.finalizePeriod(periodId, CTX),
                "OPEN 不允许最终锁定");

        // reverseClose 要求 CLOSED_FINAL，当前 OPEN → 拒绝
        assertThrows(NopException.class, () -> periodCloseBiz.reverseClose(periodId, CTX),
                "OPEN 不允许反结账");

        // 结账到 CLOSED
        periodCloseBiz.closePeriod(periodId, CTX);
        // 再次 closePeriod 要求 OPEN，当前 CLOSED → 拒绝
        assertThrows(NopException.class, () -> periodCloseBiz.closePeriod(periodId, CTX),
                "CLOSED 不允许再次结账");

        // reverseClose 要求 CLOSED_FINAL，当前 CLOSED（未最终锁定）→ 拒绝
        assertThrows(NopException.class, () -> periodCloseBiz.reverseClose(periodId, CTX),
                "CLOSED（未最终锁定）不允许反结账");
    }

    @Test
    public void testNonBlockingCloseWithIssues() {
        // auto-post-on-close=true（提示模式）：存在未过账凭证仍允许结账。
        Long periodId = seedReturn(() -> {
            Long pid = seedOpenPeriod("2026-09", 2026, 9,
                    LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                    ErpFinConstants.PERIOD_STATUS_OPEN);
            seedUnpostedVoucher("V-DRAFT-001", pid, LocalDate.of(2026, 9, 10),
                    ErpFinConstants.VOUCHER_STATUS_DRAFT);
            return pid;
        });

        ErpFinAccountingPeriod period = periodCloseBiz.closePeriod(periodId, CTX);
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, period.getStatus(),
                "提示模式下未过账凭证不阻断结账");
    }

    // ---------- helpers ----------

    private void seed(Runnable action) {
        ormTemplate.runInSession(action);
    }

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private Long seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
        return period.getId();
    }

    private void seedUnpostedVoucher(String code, Long periodId, LocalDate date, String docStatus) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(code);
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(date);
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(BigDecimal.ZERO);
        v.setTotalCredit(BigDecimal.ZERO);
        v.setDocStatus(docStatus);
        dao.saveEntity(v);
    }
}
