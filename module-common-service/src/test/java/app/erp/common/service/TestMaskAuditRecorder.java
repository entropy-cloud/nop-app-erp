package app.erp.common.service;

import io.nop.api.core.audit.AuditRequest;
import io.nop.api.core.audit.IAuditService;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.json.JSON;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.core.lang.json.JsonTool;
import io.nop.api.core.context.ContextProvider;
import io.nop.api.core.context.IContext;
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
 *   <li>粒度策略生效（Decision (c) 请求作用域去重：user×entity×objId×field×traceId 单条记录；跨请求各自留审计）。</li>
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
        io.nop.api.core.context.BaseContextProvider.contextHolder().remove();
        MaskAuditRecorder.setInstance(null);
    }

    /** 生产同形路径设置请求 traceId（ContextHttpServerFilter 同款 setTraceId 语义） */
    private void setRequestTraceId(String traceId) {
        IContext ctx = ContextProvider.getOrCreateContext();
        ctx.setTraceId(traceId);
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

    // ---- (4) 粒度策略：按 (user, entity, objId, field, traceId) 去重（0400-4 修订：请求作用域）----
    // 去重需请求上下文（traceId 进键）；无 traceId 的保守多记语义见 nullTraceIdRecordsConservatively

    @Test
    public void dedupSameKeySingleAudit() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();
        setRequestTraceId("req-dedup");

        for (int i = 0; i < 10; i++) {
            MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");
        }

        assertEquals(1, auditService.captured.size(), "同 key 10 次调用 = 1 条审计");
    }

    @Test
    public void dedupDifferentFieldSeparateAudits() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();
        setRequestTraceId("req-dedup");

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

    // ---- (5) plan 2026-10-05-0400-4：去重作用域=单请求（E4.2 :112 语义；traceId 进键）----

    @Test
    public void crossRequestSameFieldRecordedTwice() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();

        // 请求 1（traceId=req-A）：同字段首读 → 1 条审计
        setRequestTraceId("req-A");
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");
        assertEquals(1, auditService.captured.size(), "请求 1 首读 = 1 条");
        // 请求 2（traceId=req-B）：跨请求同字段重读 → 必须再记（跨请求漏报回归断言；
        // 旧 4 分量键实现下此断言红——thread 级键跨请求命中被去重）
        setRequestTraceId("req-B");
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");
        assertEquals(2, auditService.captured.size(),
                "跨请求同字段重读必须各自留审计（E4.2 请求作用域语义）");
    }

    @Test
    public void sameRequestDedupMaintained() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();

        setRequestTraceId("req-A");
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");

        assertEquals(1, auditService.captured.size(), "同请求内去重语义维持（E4.2）");
    }

    // ---- (6) plan 2026-10-06-0355-5：分桶换代——>500 distinct cell 单请求各记一次 ----

    @Test
    public void over500DistinctCellsInSingleRequestEachRecordedOnce() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);

        setRequestTraceId("req-bulk");
        int cells = 520;
        for (int i = 0; i < cells; i++) {
            Object entity = new PlainEntity();
            // 每次读不同 cell（不同 entity 实例无 objId 区分——用不同 field 名构造 distinct cell）
            MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "field-" + i);
        }
        assertEquals(cells, auditService.captured.size(),
                ">500 distinct cell 单请求内各恰记一次（旧 500-cap clear-all 实现下自第 501 cell 起"
                + "已记录 cell 重记 → 红的本用例；分桶换代后绿）");

        // 同请求内重复读已记录 cell 仍去重（换代不放大审计量）
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), new PlainEntity(), "field-0");
        assertEquals(cells, auditService.captured.size(), "已记录 cell 重读仍去重（E4.2 精确语义）");
    }

    @Test
    public void nullTraceIdRecordsConservatively() {
        loginAs(MaskHelper.ROLE_SALARY_APPROVER);
        Object entity = new PlainEntity();

        // 无请求上下文（traceId 不可得）：跳过去重保守多记——审计漏报最小化方向
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");
        MaskHelper.maskDecimal(AMOUNT, Set.of(MaskHelper.ROLE_SALARY_APPROVER), entity, "basicSalary");

        assertEquals(2, auditService.captured.size(), "null-traceId 保守多记（每次均留审计）");
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
