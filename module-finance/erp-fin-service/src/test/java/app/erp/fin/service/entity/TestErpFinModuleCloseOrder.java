package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.processor.ErpFinAccountingPeriodProcessor;
import app.erp.fin.service.processor.ErpFinAccountingPeriodProcessor.Module;
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

import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 期末结账模块关账顺序单测（Phase 1）。验证 AR→AP→INV→AST→GL 按序推进、跨序关账拒绝、
 * 以及反结账审批门控（默认 required=true 阻断）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinModuleCloseOrder extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinAccountingPeriodBiz periodCloseBiz;
    @Inject
    ErpFinAccountingPeriodProcessor periodProcessor;

    @Test
    public void testModuleCloseOrder() {
        Long periodId = seedReturn(() -> seedOpenPeriod("2025-01", 2025, 1,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                ErpFinConstants.PERIOD_STATUS_OPEN));

        periodCloseBiz.closePeriod(periodId, CTX);

        ErpFinAccountingPeriodStatus status = loadStatus(periodId);
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, status.getArStatus(), "AR 已关账");
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, status.getApStatus(), "AP 已关账");
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, status.getInvStatus(), "INV 已关账");
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, status.getAssetStatus(), "AST 已关账");
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, status.getGlStatus(), "GL 已关账");
    }

    @Test
    public void testModuleOutOfOrderRejected() {
        // 直接构造 AR=OPEN 的状态记录，跳过 AR 直接关 AP → 应拒绝（AP 前置 AR 未关账）。
        Long periodId = seedReturn(() -> seedOpenPeriod("2025-02", 2025, 2,
                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28),
                ErpFinConstants.PERIOD_STATUS_OPEN));

        ErpFinAccountingPeriodStatus[] holder = new ErpFinAccountingPeriodStatus[1];
        seed(() -> holder[0] = seedPeriodStatus(periodId));

        assertThrows(NopException.class,
                () -> periodProcessor.advanceModule(holder[0], Module.AP),
                "AP 关账前置 AR 未关账，应拒绝");
    }

    @Test
    public void testReverseCloseApprovalBlocked() {
        // 默认 reverse-close-approval-required=true：反结账应被审批门控阻断。
        Long periodId = seedReturn(() -> seedOpenPeriod("2025-03", 2025, 3,
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31),
                ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL));

        assertThrows(NopException.class, () -> periodCloseBiz.reverseClose(periodId, CTX),
                "审批门控默认开启，反结账应被阻止");

        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, period.getStatus(), "门控阻断后状态不变");
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

    private ErpFinAccountingPeriodStatus seedPeriodStatus(Long periodId) {
        IEntityDao<ErpFinAccountingPeriodStatus> dao = daoProvider.daoFor(ErpFinAccountingPeriodStatus.class);
        ErpFinAccountingPeriodStatus status = new ErpFinAccountingPeriodStatus();
        status.setPeriodId(periodId);
        status.setAcctSchemaId(1L);
        status.setArStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setApStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setInvStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setGlStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        status.setAssetStatus(ErpFinConstants.MODULE_CLOSE_OPEN);
        dao.saveEntity(status);
        return status;
    }

    private ErpFinAccountingPeriodStatus loadStatus(Long periodId) {
        IEntityDao<ErpFinAccountingPeriodStatus> dao = daoProvider.daoFor(ErpFinAccountingPeriodStatus.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(eq("periodId", periodId));
        return dao.findAllByQuery(q).get(0);
    }
}
