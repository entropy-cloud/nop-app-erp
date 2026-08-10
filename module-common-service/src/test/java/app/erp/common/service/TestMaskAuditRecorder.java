package app.erp.common.service;

import io.nop.api.core.audit.AuditRequest;
import io.nop.api.core.audit.IAuditService;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.json.JSON;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.core.lang.json.JsonTool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E4.2 MaskAuditRecorder + MaskHelper chokepoint 单元测试（plan 2026-08-11-1030-1 Phase 3 Proof）。
 *
 * <p>验证四组行为：
 * <ol>
 *   <li>授权角色读 masking 字段 → 审计记录写入（断言 AuditRequest 字段）+ masking 返明文；</li>
 *   <li>非授权角色读 → 不披露明文（null）→ 无审计记录；</li>
 *   <li>config-gate OFF → 无记录 + masking 行为不变；</li>
 *   <li>粒度策略生效（Decision (c) 按实体去重窗口：同一 user×entity×objId×field 单条记录）。</li>
 * </ol>
 *
 * <p>纯逻辑测试：无 DB / 无 IoC，{@link MaskAuditRecorder} 经手写 fake {@link IAuditService}
 * 捕获 {@link AuditRequest}；config 经 {@link AppConfig#getConfigProvider()} 直设；role 经
 * {@link IUserContext#set}（同 {@code TestErpHrResponseMasking} 范式）。
 */
public class TestMaskAuditRecorder {

    private static final String CONFIG = MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED;
    private static final BigDecimal AMOUNT = new BigDecimal("12345.67");

    private CapturingAuditService auditService;
    private MaskAuditRecorder recorder;
    private IUserContext prevCtx;
    private Object prevProviderState;

    @BeforeAll
    static void initJsonProvider() {
        JSON.registerProvider(JsonTool.instance());
    }

    @BeforeEach
    void setUp() {
        prevCtx = IUserContext.get();
        auditService = new CapturingAuditService();
        recorder = new MaskAuditRecorder();
        recorder.setAuditService(auditService);
        MaskAuditRecorder.setInstance(recorder);
        enableAuditFlag(true);
    }

    @AfterEach
    void tearDown() {
        if (prevProviderState != null) {
            AppConfig.getConfigProvider().assignConfigValue(CONFIG, prevProviderState);
        }
        IUserContext.set(prevCtx);
        MaskAuditRecorder.setInstance(null);
    }

    private void enableAuditFlag(boolean enabled) {
        AppConfig.getConfigProvider().assignConfigValue(CONFIG, enabled);
    }

    // ---- (1) 授权角色读 → 审计写入 + 明文 ----

    @Test
    public void authorizedRoleEmitsAuditAndReturnsPlaintext() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();

        BigDecimal result = MaskHelper.maskDecimal(AMOUNT,
                Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");

        assertEquals(0, AMOUNT.compareTo(result), "授权见明文");
        assertEquals(1, auditService.captured.size(), "应写入 1 条审计");
        AuditRequest req = auditService.captured.get(0);
        assertEquals(MaskAuditRecorder.OPERATION_FIELD_READ_DISCLOSURE, req.getOperation());
        assertEquals("mask-test", req.getUserId(), "userId 来自 IUserContext");
        assertEquals("PlainEntity", req.getEntityId(), "entityId 为类简单名（无 objId）");
        assertNotNull(req.getRequestData(), "opRequest JSON 非空");
        assertTrue(req.getRequestData().contains("\"field\":\"basicSalary\""), "JSON 含字段名");
        assertTrue(req.getRequestData().contains("\"authorizedRole\":\"" + MaskHelper.ROLE_SALARY_APPROVER + "\""),
                "JSON 含命中角色");
        assertNotNull(req.getActionTime(), "actionTime 已设");
    }

    // ---- (2) 非授权角色读 → 不披露 + 无审计 ----

    @Test
    public void unauthorizedRoleNoDisclosureNoAudit() {
        loginAs("STAFF");
        Object entity = new PlainEntity();

        BigDecimal result = MaskHelper.maskDecimal(AMOUNT,
                Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");

        assertNull(result, "非授权 = null");
        assertTrue(auditService.captured.isEmpty(), "无审计记录");
    }

    @Test
    public void noContextFailClosedNoAudit() {
        IUserContext.set(null);
        Object entity = new PlainEntity();

        BigDecimal result = MaskHelper.maskDecimal(AMOUNT,
                Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");

        assertNull(result, "无上下文 fail-closed = null");
        assertTrue(auditService.captured.isEmpty(), "无披露事件 = 无审计");
    }

    // ---- (3) config-gate OFF → 无记录 + masking 行为不变 ----

    @Test
    public void configGateOffNoAuditMaskingUnchanged() {
        enableAuditFlag(false);
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();

        BigDecimal result = MaskHelper.maskDecimal(AMOUNT,
                Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");

        assertEquals(0, AMOUNT.compareTo(result), "config OFF 但 masking 行为不变：授权仍见明文");
        assertTrue(auditService.captured.isEmpty(), "config OFF = 无审计");
        assertFalse(MaskAuditRecorder.isEnabled(), "isEnabled() 返 false");
    }

    @Test
    public void configGateOnIsEnabledReturnsTrue() {
        enableAuditFlag(true);
        assertTrue(MaskAuditRecorder.isEnabled(), "config ON = isEnabled() 返 true");
    }

    // ---- (4) 粒度策略：按 (user, entity, objId, field) 去重 ----

    @Test
    public void dedupSameKeySingleAudit() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();

        for (int i = 0; i < 10; i++) {
            MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");
        }

        assertEquals(1, auditService.captured.size(), "同 key 10 次调用 = 1 条审计");
    }

    @Test
    public void dedupDifferentFieldSeparateAudits() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();

        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "netSalary");
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");

        assertEquals(2, auditService.captured.size(), "不同字段 = 2 条审计（basicSalary, netSalary）");
    }

    @Test
    public void dedupDifferentUserSeparateAudits() {
        Object entity = new PlainEntity();

        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");
        loginAsUserId("OTHER_USER", MaskHelper.ROLE_SALARY_APPROVER);
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");

        assertEquals(2, auditService.captured.size(), "不同用户 = 2 条审计");
    }

    @Test
    public void noInstanceNoAuditNoFailure() {
        MaskAuditRecorder.setInstance(null);
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();

        BigDecimal result = MaskHelper.maskDecimal(AMOUNT,
                Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");

        assertEquals(0, AMOUNT.compareTo(result), "masking 行为不受 instance 缺失影响");
        assertTrue(auditService.captured.isEmpty(), "无 instance = 无审计（fail-safe）");
    }

    @Test
    public void varcharMaskingAlsoEmitsAudit() {
        loginAs(MaskHelper.ROLE_HR_SPECIALIST);
        Object entity = new PlainEntity();

        String result = MaskHelper.maskString("110101199001011234",
                StringMaskFormat.ID_CARD, Set.of(MaskHelper.ROLE_HR_SPECIALIST), entity, "idCardNo");

        assertEquals("110101199001011234", result, "授权 = 明文");
        assertEquals(1, auditService.captured.size(), "VARCHAR masking 同样写审计");
        assertTrue(auditService.captured.get(0).getRequestData().contains("\"field\":\"idCardNo\""));
    }

    // ---- helpers ----

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("mask-test");
        ctx.setUserName("mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private void loginAsUserId(String userId, String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId(userId);
        ctx.setUserName(userId);
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private static class PlainEntity {
        // 用于审计上下文测试；MaskAuditRecorder 用类简单名作 entityName，orm_idString 路径不适用
    }

    private static class CapturingAuditService implements IAuditService {
        final List<AuditRequest> captured = new ArrayList<>();

        @Override
        public void saveAudit(AuditRequest request) {
            captured.add(request);
        }

        @Override
        public boolean isAllProcessed() {
            return true;
        }
    }
}
