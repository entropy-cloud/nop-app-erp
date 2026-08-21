package app.erp.fin.service.entity;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.auth.core.login.UserContextImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RC-9 反结账审计轨迹单测（plan 2026-08-15-2119-1，P1-RC-006 修复）。
 * 验证 reverseClose(periodId, reason) 契约：① reason 落库审计三列（reversedBy/reverseCloseReason/reverseCloseAt，
 * 对齐 ErpFinPostingException resolutionNote/resolvedBy/resolvedAt 断言范式）；② reason 缺失拒绝
 * （ERR_REVERSE_CLOSE_REASON_REQUIRED + 状态保持 CLOSED_FINAL + 零审计字段写入）；③ 既有 6 测试类
 * 签名适配后由本类所在 suite 全绿保证（零回归）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:period-close-end-to-end-test.yaml")
public class TestErpFinReverseCloseAuditTrail extends PeriodCloseTestSupport {

    private static final String AUDIT_USER = "u-rc9-audit";

    @AfterEach
    public void cleanupUserContext() {
        IUserContext.set(null);
    }

    /** ① reason 落库断言：reverseClose 传 reason → 行字段 reversedBy/reverseCloseReason/reverseCloseAt 全量写入。 */
    @Test
    public void testReasonAuditTrailPersisted() {
        String periodId = seedFullPeriod("2024-06", 2024, 6);
        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));
        ormTemplate.runInSession(() -> periodBiz.finalizePeriod(periodId, CTX));

        UserContextImpl uc = new UserContextImpl();
        uc.setUserId(AUDIT_USER);
        IUserContext.set(uc);

        ErpFinAccountingPeriod period = ormTemplate.runInSession(
                session -> periodBiz.reverseClose(periodId, "更正凭证错误反结账", CTX));

        assertEquals(ErpFinConstants.PERIOD_STATUS_OPEN, period.getStatus(), "反结账后期间回开 OPEN");
        assertEquals("更正凭证错误反结账", period.getReverseCloseReason(),
                "reverseCloseReason 应落库为传入 reason");
        assertEquals(AUDIT_USER, period.getReversedBy(),
                "reversedBy 应落库为当前操作人（IUserContext.userId）");
        assertNotNull(period.getReverseCloseAt(), "reverseCloseAt 应落库为专属时间戳（非空）");
    }

    /** ② reason 缺失拒绝断言：必填守卫 ERR_REVERSE_CLOSE_REASON_REQUIRED + 状态保持 CLOSED_FINAL + 零审计字段写入。 */
    @Test
    public void testMissingReasonRejected() {
        String periodId = seedFullPeriod("2024-07", 2024, 7);
        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));
        ormTemplate.runInSession(() -> periodBiz.finalizePeriod(periodId, CTX));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> periodBiz.reverseClose(periodId, null, CTX)),
                "reason 缺失应抛 ERR_REVERSE_CLOSE_REASON_REQUIRED");
        assertEquals(ErpFinErrors.ERR_REVERSE_CLOSE_REASON_REQUIRED.getErrorCode(), ex.getErrorCode(),
                "缺失 reason 应抛专用错误码（reason 审计硬伤守卫）");

        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, period.getStatus(),
                "拒绝后期间状态保持 CLOSED_FINAL（无状态翻转）");
        assertNull(period.getReverseCloseReason(), "拒绝后 reverseCloseReason 零写入");
        assertNull(period.getReversedBy(), "拒绝后 reversedBy 零写入");
        assertNull(period.getReverseCloseAt(), "拒绝后 reverseCloseAt 零写入");
    }

    /** ③ 空白 reason（空格串）同样拒绝（必填语义覆盖 isBlank）。 */
    @Test
    public void testBlankReasonRejected() {
        String periodId = seedFullPeriod("2024-08", 2024, 8);
        ormTemplate.runInSession(() -> periodBiz.closePeriod(periodId, CTX));
        ormTemplate.runInSession(() -> periodBiz.finalizePeriod(periodId, CTX));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> periodBiz.reverseClose(periodId, "   ", CTX)),
                "空白 reason 应抛 ERR_REVERSE_CLOSE_REASON_REQUIRED");
        assertEquals(ErpFinErrors.ERR_REVERSE_CLOSE_REASON_REQUIRED.getErrorCode(), ex.getErrorCode());

        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL, period.getStatus(),
                "空白 reason 拒绝后期间状态保持 CLOSED_FINAL");
        assertNull(period.getReverseCloseAt(), "空白 reason 拒绝后 reverseCloseAt 零写入");
    }
}
