package app.erp.ast.service;

import app.erp.ast.dao.entity.ErpAstAssetActionLog;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.dao.entity.ErpAstMaintenance;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E3.8 MAINTENANCE/DISPOSAL 审计事件 Proof（P2-14 非 fin 部分，plan 2026-08-28-0219-2）：
 * 经 {@code ErpAstMaintenanceCompleteWorkProcessor}（completeWork）与 {@code ErpAstDisposalProcessor}
 * （approve）触达 recorder 调用点，断言事件类型/资产关联/回链落账。
 *
 * <p>至此 7 审计事件类型全部有 JUnit 覆盖：CREATE/UPDATE/STATUS_CHANGE/TRANSFER/VALUATION 归
 * {@code TestErpAstExtFieldsAndAuditTrail}，本类收口 MAINTENANCE/DISPOSAL。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstMaintenanceDisposalAuditEvents extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testMaintenanceCompleteWorkRecordsAuditEvent() {
        String[] assetHolder = new String[1];
        String mntId = ormTemplate.runInSession(session -> {
            seedMaintenanceBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-MNT-AUD", "维修审计类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601-AUD", "固定资产-审计"),
                    AstTestSupport.seedSubject(daoProvider, "1602-AUD", "累计折旧-审计"),
                    AstTestSupport.seedSubject(daoProvider, "6602-AUD", "维修费用-审计"));
            String assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-AUD", "维修审计资产", categoryId, "1",
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetHolder[0] = assetId;
            return createMaintenance(assetId, "MNT-AUD-001");
        });

        // DRAFT → SUBMITTED → IN_PROGRESS → COMPLETED（completeWork 落 MAINTENANCE 审计事件）
        assertEquals(0, executeRpc("ErpAstMaintenance__submit", Map.of("id", mntId)).getStatus());
        assertEquals(0, executeRpc("ErpAstMaintenance__startWork", Map.of("id", mntId)).getStatus());
        assertEquals(0, executeRpc("ErpAstMaintenance__completeWork", Map.of("id", mntId)).getStatus(),
                "维修完工应成功");

        ErpAstAssetActionLog log = findAudit(assetHolder[0], "MAINTENANCE");
        assertNotNull(log, "completeWork 应落 MAINTENANCE 审计事件");
        assertEquals("ErpAstMaintenance", log.getRefEntityName(), "回链维修工单实体");
        assertEquals(mntId, log.getRefEntityId(), "回链维修工单 id");
        assertEquals(assetHolder[0], log.getAssetId(), "事件关联资产");
        assertTrue(String.valueOf(log.getSummary()).contains("MNT-AUD-001"), "remark 含工单编码: " + log.getSummary());
    }

    @Test
    public void testDisposalApproveRecordsAuditEvent() {
        setUser();
        String[] assetHolder = new String[1];
        String disposalId = ormTemplate.runInSession(session -> {
            seedDisposalBasics();
            String gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711-AUD", "营业外支出-审计");
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-DISP-AUD", "处置审计类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601-DISP-AUD", "固定资产-处置审计"),
                    AstTestSupport.seedSubject(daoProvider, "1602-DISP-AUD", "累计折旧-处置审计"),
                    AstTestSupport.seedSubject(daoProvider, "6602-DISP-AUD", "管理费用-处置审计"));
            daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            String assetId = AstTestSupport.seedAsset(daoProvider, "AST-DISP-AUD", "处置审计资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetHolder[0] = assetId;
            return createDisposal("DISP-AUD-001", assetId, ErpAstConstants.DISPOSAL_TYPE_SCRAPPED,
                    BigDecimal.ZERO, LocalDate.of(2026, 7, 15));
        });

        assertEquals(0, executeRpc("ErpAstDisposal__submitForApproval", Map.of("id", disposalId)).getStatus());
        assertEquals(0, executeRpc("ErpAstDisposal__approve", Map.of("id", disposalId)).getStatus(),
                "处置审核应成功");

        ErpAstAssetActionLog log = findAudit(assetHolder[0], "DISPOSAL");
        assertNotNull(log, "处置 approve 应落 DISPOSAL 审计事件");
        assertEquals("ErpAstDisposal", log.getRefEntityName(), "回链处置单实体");
        assertEquals(disposalId, log.getRefEntityId(), "回链处置单 id");
        assertEquals(assetHolder[0], log.getAssetId(), "事件关联资产");
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, log.getFromStatus(), "from 快照 = 处置前状态");
        assertEquals(ErpAstConstants.ASSET_STATUS_SCRAPPED, log.getToStatus(), "to 快照 = 终态 SCRAPPED");
    }

    // ---------- helpers ----------

    private ErpAstAssetActionLog findAudit(String assetId, String eventType) {
        return ormTemplate.runInSession(sess -> {
            IEntityDao<ErpAstAssetActionLog> dao = daoProvider.daoFor(ErpAstAssetActionLog.class);
            QueryBean q = new QueryBean();
            q.addFilter(and(eq("assetId", assetId), eq("eventType", eventType)));
            List<ErpAstAssetActionLog> logs = dao.findAllByQuery(q);
            return logs.isEmpty() ? null : logs.get(0);
        });
    }

    private void seedMaintenanceBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, "1");
        AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
        AstTestSupport.seedSubject(daoProvider, "1002-AUD", "银行存款-审计");
    }

    private void seedDisposalBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, "1");
        AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
        AstTestSupport.seedSubject(daoProvider, "1002-DISP", "银行存款-处置审计");
        AstTestSupport.seedSubject(daoProvider, "1606-DISP", "固定资产清理-处置审计");
    }

    private String createMaintenance(String assetId, String code) {
        ErpAstMaintenance m = daoProvider.daoFor(ErpAstMaintenance.class).newEntity();
        m.setCode(code);
        m.setName(code + "-name");
        m.setOrgId("1");
        m.setAssetId(assetId);
        m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_DRAFT);
        m.setBusinessDate(LocalDate.of(2026, 7, 15));
        m.setCurrencyId("1");
        m.setExchangeRate(BigDecimal.ONE);
        m.setCapitalizedAmount(BigDecimal.ZERO);
        m.setTotalCostAmount(BigDecimal.ZERO);
        m.setPosted(false);
        m.setReversed(false);
        daoProvider.daoFor(ErpAstMaintenance.class).saveEntity(m);
        return m.getId();
    }

    private String createDisposal(String code, String assetId, String disposalType, BigDecimal disposalAmount,
                                  LocalDate businessDate) {
        ErpAstDisposal disposal = daoProvider.daoFor(ErpAstDisposal.class).newEntity();
        disposal.setCode(code);
        disposal.setOrgId("1");
        disposal.setAssetId(assetId);
        disposal.setDisposalType(disposalType);
        disposal.setDisposalAmount(disposalAmount);
        disposal.setCurrencyId("1");
        disposal.setExchangeRate(BigDecimal.ONE);
        disposal.setBusinessDate(businessDate);
        disposal.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        daoProvider.daoFor(ErpAstDisposal.class).saveEntity(disposal);
        return disposal.getId();
    }

    // WORKFLOW 模式下 submit 会启动 wf 实例，wf 引擎校验 caller 需 resolved 用户（对齐 TestErpAstDisposal）
    private void setUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    private ApiResponse<?> executeRpc(String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }
}
