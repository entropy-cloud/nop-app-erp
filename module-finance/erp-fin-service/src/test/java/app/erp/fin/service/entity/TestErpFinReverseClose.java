package app.erp.fin.service.entity;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 反结账单测（Phase 4）。验证 CLOSED_FINAL→OPEN、结转/汇兑凭证红字冲销、损益类科目余额恢复、
 * 模块回开、以及反结账后重新结账（幂等）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:period-close-end-to-end-test.yaml")
public class TestErpFinReverseClose extends PeriodCloseTestSupport {

    @Test
    public void testReverseCloseRestoresBalance() {
        // 使用 6 月（非年末）期间隔离月度反结账行为；年度反结账由 TestErpFinAnnualClose 覆盖。
        Long periodId = seedFullPeriod("2024-06", 2024, 6);

        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));
        ormTemplate.runInSession(() -> periodBiz.finalizePeriod(periodId, CTX));
        assertEquals(0, netCredit("6001", periodId).compareTo(BigDecimal.ZERO),
                "结账后收入科目净额清零");

        ErpFinAccountingPeriod period = ormTemplate.runInSession(session -> periodBiz.reverseClose(periodId, CTX));
        assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, period.getStatus(), "反结账后期间回开 OPEN");

        assertEquals(0, netCredit("6001", periodId).compareTo(new BigDecimal("100")),
                "反结账后收入科目余额恢复");

        ErpFinAccountingPeriodStatus status = loadStatus(periodId);
        assertEquals(ErpFinConstants.MODULE_CLOSE_OPEN, status.getGlStatus(), "GL 模块回开");
        assertEquals(ErpFinConstants.MODULE_CLOSE_OPEN, status.getAssetStatus(), "AST 模块回开");

        assertTrue(hasReversalVoucher("PERIOD-CLOSE-2024-06", ErpFinBusinessType.PERIOD_CLOSE.name()),
                "结转凭证已红冲");

        period = ormTemplate.runInSession(session -> periodBiz.closePeriod(periodId, CTX));
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, period.getStatus(), "重新结账成功");
    }

    private boolean hasReversalVoucher(String billCode, String businessType) {
        return countVouchersByBillCode(billCode, businessType) >= 2;
    }
}
