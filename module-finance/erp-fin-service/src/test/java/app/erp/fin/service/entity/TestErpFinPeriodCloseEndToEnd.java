package app.erp.fin.service.entity;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.dao.PeriodPreCheckReport;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 期末结账端到端全链单测（Phase 4）。验证 前置检查 → 模块关账 → 汇兑重估 → 损益结转 →
 * 最终锁定 → 反结账 → 重新结账 全链路。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:period-close-end-to-end-test.yaml")
public class TestErpFinPeriodCloseEndToEnd extends PeriodCloseTestSupport {

    @Test
    public void testFullChain() {
        Long periodId = seedFullPeriod("2025-06", 2025, 6);

        // 前置检查（auto-post-on-close=true，未核销 AR 仅提示不阻断）。
        PeriodPreCheckReport report = ormTemplate.runInSession(session -> periodBiz.preCheck(periodId, CTX));
        assertTrue(report.getUnsettledArApCodes().size() >= 1, "前置检查列出未核销外币应收");
        output("1_pre_check_response.json5",
                java.util.Collections.singletonMap("unsettledArApCodes", report.getUnsettledArApCodes()));

        // 模块关账 + 汇兑重估 + 损益结转 → 期间 CLOSED。
        ErpFinAccountingPeriod period = ormTemplate.runInSession(session -> periodBiz.closePeriod(periodId, CTX));
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, period.getStatus(), "结账后 CLOSED");
        output("2_close_period_response.json5", periodState(period));

        // 汇兑重估 + 损益结转凭证均已生成。
        assertTrue(countVouchersByBillCode("FX-REVAL-2025-06", ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name()) >= 1,
                "汇兑重估凭证已生成");
        assertTrue(countVouchersByBillCode("PERIOD-CLOSE-2025-06", ErpFinBusinessType.PERIOD_CLOSE.name()) >= 1,
                "损益结转凭证已生成");

        ErpFinAccountingPeriodStatus status = loadStatus(periodId);
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, status.getGlStatus(), "GL 模块已关账");
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, status.getAssetStatus(), "AST 模块已关账");

        // 最终锁定。
        period = ormTemplate.runInSession(session -> periodBiz.finalizePeriod(periodId, CTX));
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, period.getStatus(), "最终锁定 CLOSED_FINAL");
        output("3_finalize_period_response.json5", periodState(period));

        // 反结账 → OPEN。
        period = ormTemplate.runInSession(session -> periodBiz.reverseClose(periodId, CTX));
        assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, period.getStatus(), "反结账后 OPEN");
        output("4_reverse_close_response.json5", periodState(period));

        // 重新结账（生成新凭证，幂等）。
        period = ormTemplate.runInSession(session -> periodBiz.closePeriod(periodId, CTX));
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, period.getStatus(), "重新结账成功");
        output("5_reclose_response.json5", periodState(period));
        assertTrue(countVouchersByBillCode("PERIOD-CLOSE-2025-06", ErpFinBusinessType.PERIOD_CLOSE.name()) >= 2,
                "重新结账生成新的结转凭证");
    }

    private java.util.Map<String, Object> periodState(ErpFinAccountingPeriod p) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("code", p.getCode());
        m.put("status", p.getStatus());
        return m;
    }
}
