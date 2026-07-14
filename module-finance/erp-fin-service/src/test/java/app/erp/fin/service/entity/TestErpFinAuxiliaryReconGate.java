package app.erp.fin.service.entity;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 辅助账跨年对账门控测试（plan 2026-07-05-0540-2 Phase 3）。年度结账前校验 AR/AP 辅助账合计与总账 AR/AP 科目余额
 * 一致；不一致（差异超精度）抛 {@code ERR_AUXILIARY_RECON_MISMATCH} 阻止年度结账。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:auxiliary-recon-gate-test.yaml")
public class TestErpFinAuxiliaryReconGate extends PeriodCloseTestSupport {

    /** AR 辅助账开口合计 ≠ 总账 AR 科目余额 → 年度结账被阻止。 */
    @Test
    public void testAuxiliaryReconMismatchBlocksAnnualClose() {
        Long periodId = ormTemplate.runInSession(session -> {
            Long pid = seedOpenPeriod("2024-12", 2024, 12);
            Map<String, ErpMdSubject> subjects = new HashMap<>();
            subjects.put("1122", seedSubject("1122", "应收账款", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            seedCurrency(1L, "CNY", true);
            // 总账 AR 科目借方 1000（已过账）。
            seedPostedVoucher("V-AR-GL", pid, LocalDate.of(2024, 12, 10), subjects,
                    new Object[]{"1122", "应收账款", ErpFinConstants.DC_DEBIT, new BigDecimal("1000")},
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_CREDIT, new BigDecimal("1000")});
            // AR 辅助账开口合计 500（≠ 总账 1000，差异 500 超精度）。
            seedOpenArAp("ARI-RECON-001", pid, LocalDate.of(2024, 12, 11),
                    ErpFinConstants.DIRECTION_RECEIVABLE, 1L, new BigDecimal("500"), new BigDecimal("500"));
            return pid;
        });

        NopException ex = assertThrows(NopException.class, () -> ormTemplate.runInSession(session -> periodBiz.closePeriod(periodId, CTX)),
                "辅助账与总账不一致时年度结账被阻止");
        assertTrue(ex.getErrorCode() != null
                        && ex.getErrorCode().contains("auxiliary-recon-mismatch"),
                "错误码为辅助账对账不一致");
    }
}
