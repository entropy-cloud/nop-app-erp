package app.erp.mfg.service;

import app.erp.common.service.MaskAuditRecorder;
import app.erp.common.service.MaskHelper;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.audit.AuditRequest;
import io.nop.api.core.audit.IAuditService;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.config.AppConfig;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成本卷算 {@code findLatestFirmedStandardCost} GraphQL 读取面脱敏单测（plan 2026-08-25-1956-1 Phase 3 Proof）。
 *
 * <p>经 {@link IGraphQLEngine} 走真实 GraphQL 暴露面（该 @BizQuery 无前端调用方但 GraphQL 可达，正是本计划
 * 补齐的读取面）。范式 = {@code TestErpMfgResponseMasking}（loginAs 三态上下文）+ {@code TestMaskAuditRecorder}
 * （fake {@link IAuditService} 捕获 {@link AuditRequest}）：
 * <ol>
 *   <li>授权（管理员/财务员，与 {@code ErpMfgCostRollupLineBizModel} COST_ROLES 同源）→ 返回最近 FIRMED
 *       单位成本明文 + E4.2 披露审计写入（审计载体 = 命中 line 实体，fieldName=unitCost）；</li>
 *   <li>非授权角色 → null，无审计；</li>
 *   <li>无用户上下文 → fail-closed null，无审计。</li>
 * </ol>
 *
 * <p>E3.2 取值豁免保径（inv {@code StandardCostResolver} DAO 直读不受本 masking 影响）由
 * {@code TestErpInvStandardCostResolverValueExemptionInvariant} + inv 域
 * {@code TestErpInvResolverRawValueAfterReadPathMasking} 独立守卫。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgCostRollupReadPathMasking extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    private static final String MATERIAL_ID = "3001";
    private static final String HEADER_ID = "300101";
    private static final String LINE_ID = "300102";
    private static final BigDecimal UNIT_COST = new BigDecimal("87.5");

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    private CapturingAuditService auditService;
    private MaskAuditRecorder fakeRecorder;
    private MaskAuditRecorder prevRecorder;
    private Object prevConfigState;
    private IUserContext prevCtx;

    @BeforeEach
    void setUpAuditCapture() {
        prevCtx = IUserContext.get();
        auditService = new CapturingAuditService();
        fakeRecorder = new MaskAuditRecorder();
        fakeRecorder.setAuditService(auditService);
        prevRecorder = MaskAuditRecorder.instance();
        fakeRecorder.init();
        prevConfigState = AppConfig.var(MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED, Boolean.FALSE);
        AppConfig.getConfigProvider().assignConfigValue(MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED, true);
    }

    @AfterEach
    void tearDownAuditCapture() {
        AppConfig.getConfigProvider().assignConfigValue(
                MaskAuditRecorder.CONFIG_FIELD_READ_AUDIT_ENABLED, prevConfigState);
        if (prevRecorder != null) {
            prevRecorder.init();
        } else {
            fakeRecorder.destroy();
        }
        IUserContext.set(prevCtx);
    }

    @Test
    public void authorizedRolesSeePlaintextAndAuditWritten() {
        seedFirmedRollup();
        loginAs(MaskHelper.ROLE_BIZ_ADMIN);
        assertEquals(0, UNIT_COST.compareTo(queryLatestFirmedCost()),
                "管理员见 FIRMED 单位成本明文");
        assertAuditWritten();

        // 换角色前清捕获
        auditService.captured.clear();
        loginAs(MaskHelper.ROLE_FINANCE_STAFF);
        assertEquals(0, UNIT_COST.compareTo(queryLatestFirmedCost()),
                "财务员见 FIRMED 单位成本明文");
    }

    @Test
    public void unauthorizedRoleSeesNullNoAudit() {
        seedFirmedRollup();
        loginAs("STAFF");
        assertNull(queryLatestFirmedCost(), "非授权 = null");
        assertTrue(auditService.captured.isEmpty(), "非授权无明文披露 = 无审计");
    }

    @Test
    public void noContextFailClosedNullNoAudit() {
        seedFirmedRollup();
        IUserContext.set(null);
        assertNull(queryLatestFirmedCost(), "无上下文 fail-closed = null");
        assertTrue(auditService.captured.isEmpty(), "无上下文无披露 = 无审计");
    }

    // ---------- helpers ----------

    private BigDecimal queryLatestFirmedCost() {
        ApiResponse<?> resp = executeRpc(query, "ErpMfgCostRollup__findLatestFirmedStandardCost",
                io.nop.api.core.beans.ApiRequest.build(Map.of("materialId", MATERIAL_ID)));
        assertTrue(resp.isOk(), "GraphQL 调用成功: " + resp);
        Object data = resp.getData();
        if (data instanceof Map) {
            Object v = ((Map<?, ?>) data).get("findLatestFirmedStandardCost");
            return v instanceof BigDecimal ? (BigDecimal) v : (v == null ? null : new BigDecimal(v.toString()));
        }
        return data instanceof BigDecimal ? (BigDecimal) data : (data == null ? null : new BigDecimal(data.toString()));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action,
                                      io.nop.api.core.beans.ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void assertAuditWritten() {
        assertTrue(!auditService.captured.isEmpty(), "授权读取应写 E4.2 披露审计");
        AuditRequest req = auditService.captured.get(0);
        assertEquals(MaskAuditRecorder.OPERATION_FIELD_READ_DISCLOSURE, req.getOperation());
        assertTrue(req.getEntityId().startsWith("ErpMfgCostRollupLine"),
                "审计载体 = 命中 line 实体 ErpMfgCostRollupLine，实际 " + req.getEntityId());
        assertTrue(req.getRequestData().contains("\"field\":\"unitCost\""), "审计 fieldName=unitCost");
    }

    private void loginAs(String... roles) {
        UserContextImpl ctx = new UserContextImpl();
        ctx.setUserId("mfg-rollup-mask-test");
        ctx.setUserName("mfg-rollup-mask-test");
        ctx.setRoles(Set.of(roles));
        IUserContext.set(ctx);
    }

    private void seedFirmedRollup() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
            ErpMfgCostRollup header = headerDao.newEntity();
            header.orm_propValueByName("id", HEADER_ID);
            header.setCode("ROLLUP-" + HEADER_ID);
            header.setOrgId("1");
            header.setBusinessDate(LocalDate.of(2026, 6, 1));
            header.orm_propValueByName("status", "FIRMED");
            headerDao.saveEntity(header);

            IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
            ErpMfgCostRollupLine line = lineDao.newEntity();
            line.orm_propValueByName("id", LINE_ID);
            line.setCostRollupId(HEADER_ID);
            line.setLineNo(1);
            line.setMaterialId(MATERIAL_ID);
            line.setUoMId("1");
            line.setUnitCost(UNIT_COST);
            line.setTotalCost(UNIT_COST);
            line.setMaterialCost(UNIT_COST);
            line.setCurrencyId("1");
            lineDao.saveEntity(line);
        });
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
