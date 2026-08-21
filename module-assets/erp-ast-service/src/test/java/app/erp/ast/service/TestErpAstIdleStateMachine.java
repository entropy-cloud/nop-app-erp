package app.erp.ast.service;

import app.erp.ast.biz.IErpAstAssetBiz;
import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstDisposal;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.api.core.exceptions.NopException;
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
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产闲置状态机 suspend/resume（RC-R1.54，L1 UC-AST-03）端到端单测。
 *
 * <p>覆盖 plan Proof 场景：suspend 后批量折旧跳过（IDLE 不入 batch）/ suspend 后单资产 executeDepreciation 拒绝 /
 * resume 后恢复计提 / 非法迁移拒绝（IN_SERVICE→IN_SERVICE 等）/ 处置路径 IDLE→SCRAPPED 放行（Phase 3 处置
 * Decision 选项 A）+ PENDING 计划 suspend 期间保留（Decision A）+ 暂停时点 remark 强制记录。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstIdleStateMachine extends JunitAutoTestCase {

    @RegisterExtension
    static AstFrozenClockExtension astClock = new AstFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpAstAssetBiz assetBiz;
    @Inject
    IErpAstDepreciationScheduleBiz scheduleBiz;

    // ---------- suspend/resume 迁移 + 折旧语义 ----------

    @Test
    public void testSuspendSkipsBatchDepreciation() {
        ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-IDLE-B", "闲置批量类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            AstTestSupport.seedAsset(daoProvider, "AST-IDLE-B1", "闲置批量资产1", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            AstTestSupport.seedAsset(daoProvider, "AST-IDLE-B2", "闲置批量资产2", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return null;
        });

        String idleAssetId = findAssetByCode("AST-IDLE-B1").getId();
        ormTemplate.runInSession(session -> assetBiz.suspend(idleAssetId, CTX));

        // 批量折旧：IDLE 不入 batch（仅 IN_SERVICE 资产）
        int processed = ormTemplate.runInSession(session -> scheduleBiz.executeBatchDepreciation("2026-07", CTX));
        assertEquals(1, processed, "批量折旧仅处理使用中资产（IDLE 跳过）");
        assertNull(findScheduleByAssetCode("AST-IDLE-B1", "2026-07"), "IDLE 资产不生成折旧计划行（闲置期间不计提）");
        ErpAstDepreciationSchedule active = findScheduleByAssetCode("AST-IDLE-B2", "2026-07");
        assertNotNull(active, "使用中资产正常计提");
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, active.getStatus());
        output("1_batch_skip_idle.json5", java.util.Arrays.asList(
                java.util.Map.of("assetCode", "AST-IDLE-B1", "status", "IDLE", "schedule202607", "absent"),
                scheduleState(active)));
    }

    @Test
    public void testSuspendRejectsSingleDepreciation() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-IDLE-S", "闲置单资产类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-IDLE-S", "闲置单资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });
        ormTemplate.runInSession(session -> assetBiz.suspend(assetId, CTX));

        // suspend 后单资产 executeDepreciation 拒绝（validateAssetInService 守卫）
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> scheduleBiz.executeDepreciation(assetId, "2026-07", CTX)));
        assertEquals(ErpAstErrors.ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE.getErrorCode(), ex.getErrorCode(),
                "IDLE 资产单资产折旧拒绝");
    }

    @Test
    public void testResumeRestoresDepreciation() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-IDLE-R", "闲置恢复类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-IDLE-R", "闲置恢复资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        ErpAstAsset suspended = ormTemplate.runInSession(session -> assetBiz.suspend(assetId, CTX));
        assertEquals(ErpAstConstants.ASSET_STATUS_IDLE, suspended.getStatus(), "suspend 后 IDLE");

        ErpAstAsset resumed = ormTemplate.runInSession(session -> assetBiz.resume(assetId, CTX));
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, resumed.getStatus(), "resume 后 IN_SERVICE");

        // 恢复计提：executeDepreciation 正常执行
        ErpAstDepreciationSchedule s = ormTemplate.runInSession(session ->
                scheduleBiz.executeDepreciation(assetId, "2026-07", CTX));
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, s.getStatus(), "resume 后恢复计提");
        assertEquals(0, s.getActualAmount().compareTo(new BigDecimal("1000")), "恢复后月折旧 1000");
        output("1_resume_state.json5", java.util.Arrays.asList(
                java.util.Map.of("status", resumed.getStatus()),
                scheduleState(s)));
    }

    @Test
    public void testIllegalTransitionsRejected() {
        String[] draftAsset = new String[1];
        String[] idleAsset = new String[1];
        ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-IDLE-I", "闲置非法类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            draftAsset[0] = AstTestSupport.seedAsset(daoProvider, "AST-IDLE-DRAFT", "草稿资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_DRAFT);
            idleAsset[0] = AstTestSupport.seedAsset(daoProvider, "AST-IDLE-I2", "已闲置资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return null;
        });
        ormTemplate.runInSession(session -> assetBiz.suspend(idleAsset[0], CTX));

        // 非法迁移拒绝：DRAFT suspend / IDLE 重复 suspend / DRAFT resume
        assertIllegalTransition(() -> assetBiz.suspend(draftAsset[0], CTX), "DRAFT 不允许 suspend");
        assertIllegalTransition(() -> assetBiz.suspend(idleAsset[0], CTX), "IDLE 不允许重复 suspend");
        assertIllegalTransition(() -> assetBiz.resume(draftAsset[0], CTX), "DRAFT 不允许 resume");
    }

    private void assertIllegalTransition(Runnable r, String message) {
        NopException ex = assertThrows(NopException.class, r::run, message);
        assertEquals(ErpAstErrors.ERR_AST_ASSET_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "非法迁移拒绝携带领域码");
        assertNotNull(ex.getParam(ErpAstErrors.ARG_ASSET_CODE), "拒绝携带资产编码");
    }

    @Test
    public void testSuspendRecordsIdleSinceInRemark() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-IDLE-M", "闲置标注类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-IDLE-M", "闲置标注资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        ormTemplate.runInSession(session -> assetBiz.suspend(assetId, CTX));
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertNotNull(asset.getRemark(), "suspend 强制记录暂停时点 remark");
        assertTrue(asset.getRemark().contains("闲置自 "), "remark 含「闲置自 {date}」（闲置时长派生的时间基准）");
    }

    // ---------- PENDING 计划 suspend 期间保留（Decision A）+ 处置路径（Decision 选项 A） ----------

    @Test
    public void testPendingSchedulesKeptDuringSuspendThenCancelledAtDisposal() {
        String[] assetIdHolder = new String[1];
        String disposalId = ormTemplate.runInSession(session -> {
            seedBasics();
            String gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-IDLE-P", "闲置处置类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            String assetId = AstTestSupport.seedAsset(daoProvider, "AST-IDLE-P", "闲置处置资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            AstTestSupport.seedPendingSchedule(daoProvider, assetId, "1", "2026-08");
            return seedDisposal("DISP-IDLE-001", assetId, ErpAstConstants.DISPOSAL_TYPE_SCRAPPED,
                    BigDecimal.ZERO, LocalDate.of(2026, 7, 20));
        });

        // suspend：PENDING 计划保留（Decision A——不 cancel 不重建，闲置期引擎跳过执行）
        ormTemplate.runInSession(session -> assetBiz.suspend(assetIdHolder[0], CTX));
        ErpAstDepreciationSchedule pending = findSchedule(assetIdHolder[0], "2026-08");
        assertNotNull(pending, "PENDING 计划 suspend 期间保留");
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_PENDING, pending.getStatus(), "suspend 不 cancel PENDING");

        // 处置路径：IDLE→SCRAPPED 放行（Phase 3 处置 Decision 选项 A——扩展 StateMachine Bean 对齐 owner doc §2）
        setUser();
        executeRpc("ErpAstDisposal__submitForApproval", Map.of("id", disposalId));
        executeRpc("ErpAstDisposal__approve", Map.of("id", disposalId));

        ErpAstDisposal disposal = daoProvider.daoFor(ErpAstDisposal.class).getEntityById(disposalId);
        assertTrue(Boolean.TRUE.equals(disposal.getPosted()), "IDLE 资产处置过账成功");
        assertEquals(0, disposal.getGainLoss().compareTo(new BigDecimal("-12000")),
                "IDLE 资产处置清理损失 = −原值（无补提，闲置期无折旧义务）");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_SCRAPPED, asset.getStatus(), "IDLE 处置终态=SCRAPPED");

        ErpAstDepreciationSchedule cancelled = findSchedule(assetIdHolder[0], "2026-08");
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_CANCELLED, cancelled.getStatus(), "处置后 PENDING 计划 CANCELLED");
        output("1_idle_disposal_state.json5", java.util.Arrays.asList(
                java.util.Map.of("assetStatus", asset.getStatus()),
                java.util.Map.of("gainLoss", disposal.getGainLoss(), "posted", disposal.getPosted()),
                java.util.Map.of("schedule202608", cancelled.getStatus())));
    }

    // ---------- helpers ----------

    private void seedBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, "1");
        AstTestSupport.seedSubject(daoProvider, "6602", "管理费用");
        AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧");
        AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
        AstTestSupport.seedSubject(daoProvider, "1606", "固定资产清理");
        AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
        AstTestSupport.seedPeriod(daoProvider, "2026-08", 2026, 8, ErpAstConstants.PERIOD_STATUS_OPEN);
        AstTestSupport.seedPeriod(daoProvider, "2026-09", 2026, 9, ErpAstConstants.PERIOD_STATUS_OPEN);
    }

    private ErpAstAsset findAssetByCode(String code) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpAstAsset> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpAstDepreciationSchedule findScheduleByAssetCode(String assetCode, String period) {
        ErpAstAsset asset = findAssetByCode(assetCode);
        if (asset == null) {
            return null;
        }
        return findSchedule(asset.getId(), period);
    }

    private ErpAstDepreciationSchedule findSchedule(String assetId, String period) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assetId), eq("period", period)));
        List<ErpAstDepreciationSchedule> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private String seedDisposal(String code, String assetId, String disposalType, BigDecimal disposalAmount,
                              LocalDate businessDate) {
        IEntityDao<ErpAstDisposal> dao = daoProvider.daoFor(ErpAstDisposal.class);
        ErpAstDisposal disposal = new ErpAstDisposal();
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
        dao.saveEntity(disposal);
        return disposal.getId();
    }

    private ApiResponse<?> executeRpc(String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    private void setUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    private java.util.Map<String, Object> scheduleState(ErpAstDepreciationSchedule s) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("period", s.getPeriod());
        m.put("status", s.getStatus());
        m.put("actualAmount", s.getActualAmount());
        m.put("posted", s.getPosted());
        return m;
    }
}
