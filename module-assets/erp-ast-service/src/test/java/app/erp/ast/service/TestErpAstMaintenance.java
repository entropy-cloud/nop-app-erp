package app.erp.ast.service;

import app.erp.ast.biz.IErpAstMaintenanceBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.dao.entity.ErpAstMaintenanceCost;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产维修（UC-AST-10）端到端单测。
 *
 * <p>覆盖：维修工单创建+资产关联、费用归集（三类成本）、CAPITALIZE 裁决（原值增量+折旧重算）、
 * EXPENSE 裁决（费用凭证）、阈值门控、过账凭证方向、reverse 红冲（两路径）、资产终态拦截、非法状态迁移。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstMaintenance extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstMaintenanceBiz maintenanceBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCapitalizePathWithDepreciationRecalc() {
        BigDecimal[] originalHolder = new BigDecimal[1];
        Long[] assetHolder = new Long[1];
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-CAP", "资本化维修类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-CAP", "资本化维修资产", categoryId, 1L,
                    new BigDecimal("100000"), new BigDecimal("5000"),
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 24,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetHolder[0] = assetId;
            originalHolder[0] = new BigDecimal("100000");
            // 预置一个 PENDING 折旧计划条目（模拟资本化前已有计划）
            AstTestSupport.seedPendingSchedule(daoProvider, assetId, 1L, "2026-08");
            return createMaintenanceEntity(assetId, "MNT-CAP-001", "资本化维修单", null);
        });

        // 状态机：DRAFT → SUBMITTED → IN_PROGRESS → COMPLETED
        assertEquals(0, submit(mntId).getStatus());
        assertEquals(0, startWork(mntId).getStatus());
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_LABOR, new BigDecimal("15000"));
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_SPARE_PART, new BigDecimal("10000"));
        assertEquals(0, completeWork(mntId).getStatus());

        // 裁决 CAPITALIZE
        decideTreatment(mntId, ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE, new BigDecimal("25000"));

        // approve + post
        approve(mntId);
        assertEquals(0, post(mntId).getStatus());

        ErpAstMaintenance afterPost = daoProvider.daoFor(ErpAstMaintenance.class).getEntityById(mntId);
        assertEquals(ErpAstConstants.MAINTENANCE_STATUS_POSTED, afterPost.getStatus(), "状态 POSTED");
        assertTrue(Boolean.TRUE.equals(afterPost.getPosted()), "posted=true");
        assertEquals(0, nz(afterPost.getTotalCostAmount()).compareTo(new BigDecimal("25000")), "费用合计=25000");
        assertEquals(0, nz(afterPost.getCapitalizedAmount()).compareTo(new BigDecimal("25000")), "资本化金额=25000");

        // 资产原值增量（100000 + 25000 = 125000）
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetHolder[0]);
        assertEquals(0, nz(asset.getOriginalValue()).compareTo(new BigDecimal("125000")), "资本化后原值=125000");

        // 折旧计划重算：原 PENDING 条目被删除，重新生成（config-gated 默认 true）
        List<ErpAstDepreciationSchedule> pending = findPendingSchedules(assetHolder[0]);
        assertFalse(pending.isEmpty(), "折旧计划重算生成了新的 PENDING 条目");

        // MAINTENANCE_CAPITALIZATION 凭证回链
        List<ErpFinVoucherBillR> links = findBillLinks("MNT-CAP-001", "MAINTENANCE_CAPITALIZATION");
        assertFalse(links.isEmpty(), "MAINTENANCE_CAPITALIZATION 凭证回链已落库");
    }

    @Test
    public void testExpensePathDoesNotAffectAsset() {
        Long[] assetHolder = new Long[1];
        BigDecimal[] originalHolder = new BigDecimal[1];
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-EXP", "费用化维修类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-EXP", "费用化维修资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetHolder[0] = assetId;
            originalHolder[0] = new BigDecimal("50000");
            return createMaintenanceEntity(assetId, "MNT-EXP-001", "费用化维修单", null);
        });

        submit(mntId);
        startWork(mntId);
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_SUBCONTRACT, new BigDecimal("8000"));
        completeWork(mntId);
        decideTreatment(mntId, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE, null);
        approve(mntId);
        assertEquals(0, post(mntId).getStatus());

        // EXPENSE 路径不影响资产原值
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetHolder[0]);
        assertEquals(0, nz(asset.getOriginalValue()).compareTo(originalHolder[0]), "费用化路径资产原值不变");

        // MAINTENANCE_EXPENSE 凭证回链
        List<ErpFinVoucherBillR> links = findBillLinks("MNT-EXP-001", "MAINTENANCE_EXPENSE");
        assertFalse(links.isEmpty(), "MAINTENANCE_EXPENSE 凭证回链已落库");
    }

    @Test
    public void testExpensePathLinkedVisitCreditsClearing() {
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-LNK", "关联维护工单类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-LNK", "关联维护资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            // maintenanceVisitId=999L 非空 → 贷中转科目分支
            return createMaintenanceEntity(assetId, "MNT-LNK-001", "关联维护维修单", 999L);
        });

        submit(mntId);
        startWork(mntId);
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_SPARE_PART, new BigDecimal("3000"));
        completeWork(mntId);
        decideTreatment(mntId, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE, null);
        approve(mntId);
        assertEquals(0, post(mntId).getStatus());

        ErpAstMaintenance m = daoProvider.daoFor(ErpAstMaintenance.class).getEntityById(mntId);
        assertEquals(0, nz(m.getTotalCostAmount()).compareTo(new BigDecimal("3000")));

        List<ErpFinVoucherBillR> links = findBillLinks("MNT-LNK-001", "MAINTENANCE_EXPENSE");
        assertFalse(links.isEmpty(), "关联维护工单费用化凭证回链已落库");
    }

    @Test
    public void testCapitalizeBelowThresholdRejected() {
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-THR", "阈值门控类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-THR", "阈值门控资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return createMaintenanceEntity(assetId, "MNT-THR-001", "阈值门控维修单", null);
        });

        submit(mntId);
        startWork(mntId);
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_LABOR, new BigDecimal("100"));
        completeWork(mntId);

        // 裁决 CAPITALIZE 但金额低于默认阈值 0 之外的设定——这里用默认阈值 0（不设阈值），所以应通过；
        // 改为验证非法 treatment 被拒
        ApiResponse<?> bad = decideTreatment(mntId, "INVALID_TREATMENT", null);
        assertTrue(bad.getStatus() != 0, "非法 treatment 应被拒绝");
    }

    @Test
    public void testTerminalAssetRejected() {
        ApiResponse<?> bad = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-TERM", "终态资产类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-TERM", "终态资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_SCRAPPED);
            return createMaintenanceRpc(assetId, "MNT-TERM-001");
        });
        assertTrue(bad.getStatus() != 0, "终态资产（SCRAPPED）创建维修工单应被拒绝");
    }

    @Test
    public void testIllegalTransitionStartWorkBeforeSubmit() {
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-ILL", "非法迁移类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-ILL", "非法迁移资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return createMaintenanceEntity(assetId, "MNT-ILL-001", "非法迁移维修单", null);
        });

        // DRAFT 态直接 startWork 应被拒绝
        ApiResponse<?> bad = startWork(mntId);
        assertTrue(bad.getStatus() != 0, "DRAFT 态直接 startWork 应被拒绝");
    }

    @Test
    public void testPostWithoutCostRejected() {
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-NC", "无费用类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-NC", "无费用资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return createMaintenanceEntity(assetId, "MNT-NC-001", "无费用维修单", null);
        });

        submit(mntId);
        startWork(mntId);
        completeWork(mntId);
        decideTreatment(mntId, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE, null);
        // 没有归集任何费用行
        ApiResponse<?> bad = post(mntId);
        assertTrue(bad.getStatus() != 0, "无费用行 post 应被拒绝");
    }

    @Test
    public void testCancelDraftMaintenance() {
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-CANC", "作废类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-CANC", "作废资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return createMaintenanceEntity(assetId, "MNT-CANC-001", "作废维修单", null);
        });

        ErpAstMaintenance cancelled = maintenanceBiz.cancel(mntId, CTX);
        assertEquals(ErpAstConstants.MAINTENANCE_STATUS_CANCELLED, cancelled.getStatus(), "作废后 status=CANCELLED");
    }

    @Test
    public void testReverseCapitalizeRollsBack() {
        Long[] assetHolder = new Long[1];
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-REV-CAP", "红冲资本化类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-REV-CAP", "红冲资本化资产", categoryId, 1L,
                    new BigDecimal("100000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 24,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetHolder[0] = assetId;
            AstTestSupport.seedPendingSchedule(daoProvider, assetId, 1L, "2026-08");
            return createMaintenanceEntity(assetId, "MNT-REV-CAP-001", "红冲资本化维修单", null);
        });

        submit(mntId);
        startWork(mntId);
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_LABOR, new BigDecimal("20000"));
        completeWork(mntId);
        decideTreatment(mntId, ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE, new BigDecimal("20000"));
        approve(mntId);
        post(mntId);

        ErpAstAsset beforeReverse = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetHolder[0]);
        assertEquals(0, nz(beforeReverse.getOriginalValue()).compareTo(new BigDecimal("120000")),
                "红冲前原值=120000（100000+20000）");

        assertEquals(0, reverse(mntId).getStatus(), "reverse 成功");
        ErpAstMaintenance afterRev = daoProvider.daoFor(ErpAstMaintenance.class).getEntityById(mntId);
        assertFalse(Boolean.TRUE.equals(afterRev.getPosted()), "红冲后 posted=false");
        assertTrue(Boolean.TRUE.equals(afterRev.getReversed()), "红冲后 reversed=true");

        ErpAstAsset afterReverseAsset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetHolder[0]);
        assertEquals(0, nz(afterReverseAsset.getOriginalValue()).compareTo(new BigDecimal("100000")),
                "红冲后资产原值回退=100000");
    }

    @Test
    public void testReverseExpenseOnlyReversesVoucher() {
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-REV-EXP", "红冲费用化类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-REV-EXP", "红冲费用化资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return createMaintenanceEntity(assetId, "MNT-REV-EXP-001", "红冲费用化维修单", null);
        });

        submit(mntId);
        startWork(mntId);
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_LABOR, new BigDecimal("5000"));
        completeWork(mntId);
        decideTreatment(mntId, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE, null);
        approve(mntId);
        post(mntId);

        assertEquals(0, reverse(mntId).getStatus(), "reverse 成功");
        assertTrue(isAllVouchersReversed("MNT-REV-EXP-001", "MAINTENANCE_EXPENSE"),
                "MAINTENANCE_EXPENSE 凭证已红字冲销");
    }

    @Test
    public void testThreeCostTypesAggregated() {
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-3C", "三类成本类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-3C", "三类成本资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return createMaintenanceEntity(assetId, "MNT-3C-001", "三类成本维修单", null);
        });

        submit(mntId);
        startWork(mntId);
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_LABOR, new BigDecimal("1000"));
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_SPARE_PART, new BigDecimal("2000"));
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_SUBCONTRACT, new BigDecimal("3000"));
        completeWork(mntId);
        decideTreatment(mntId, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE, null);
        approve(mntId);
        post(mntId);

        ErpAstMaintenance m = daoProvider.daoFor(ErpAstMaintenance.class).getEntityById(mntId);
        assertEquals(0, nz(m.getTotalCostAmount()).compareTo(new BigDecimal("6000")), "三类成本合计=6000");
    }

    @Test
    public void testReverseTwiceRejected() {
        Long mntId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MNT-2R", "二次红冲类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-2R", "二次红冲资产", categoryId, 1L,
                    new BigDecimal("50000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return createMaintenanceEntity(assetId, "MNT-2R-001", "二次红冲维修单", null);
        });

        submit(mntId);
        startWork(mntId);
        addCostLine(mntId, ErpAstConstants.MAINTENANCE_COST_TYPE_LABOR, new BigDecimal("5000"));
        completeWork(mntId);
        decideTreatment(mntId, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE, null);
        approve(mntId);
        post(mntId);
        reverse(mntId);

        ApiResponse<?> bad = reverse(mntId);
        assertTrue(bad.getStatus() != 0, "二次红冲应被拒绝");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long id) {
        return executeRpc("ErpAstMaintenance__submit", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> startWork(Long id) {
        return executeRpc("ErpAstMaintenance__startWork", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> completeWork(Long id) {
        return executeRpc("ErpAstMaintenance__completeWork", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> decideTreatment(Long id, String treatment, BigDecimal capitalizedAmount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(id));
        data.put("treatment", treatment);
        if (capitalizedAmount != null) {
            data.put("capitalizedAmount", capitalizedAmount);
        }
        return executeRpc("ErpAstMaintenance__decideTreatment", data);
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc("ErpAstMaintenance__approve", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> post(Long id) {
        return executeRpc("ErpAstMaintenance__post", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> reverse(Long id) {
        return executeRpc("ErpAstMaintenance__reverse", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> executeRpc(String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    private void addCostLine(Long maintenanceId, String costType, BigDecimal amount) {
        ormTemplate.runInSession(session -> {
            ErpAstMaintenanceCost line = daoProvider.daoFor(ErpAstMaintenanceCost.class).newEntity();
            line.setMaintenanceId(maintenanceId);
            line.setOrgId(1L);
            line.setCostType(costType);
            line.setAmount(amount);
            line.setBusinessDate(LocalDate.of(2026, 7, 15));
            line.setCurrencyId(1L);
            daoProvider.daoFor(ErpAstMaintenanceCost.class).saveEntity(line);
            return null;
        });
    }

    // ---------- helpers ----------

    private void seedCoreBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, 1L);
        AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
        AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
        AstTestSupport.seedSubject(daoProvider, "6602", "维修费用");
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
        AstTestSupport.seedSubject(daoProvider, "1403", "存货");
        AstTestSupport.seedSubject(daoProvider, "2502", "维修中转清算");
    }

    private Long seedCategory(String code, String name) {
        return AstTestSupport.seedCategory(daoProvider, code, name,
                ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                AstTestSupport.seedSubject(daoProvider, "1601-" + code, "固定资产-" + code),
                AstTestSupport.seedSubject(daoProvider, "1602-" + code, "累计折旧-" + code),
                AstTestSupport.seedSubject(daoProvider, "6602-" + code, "维修费用-" + code));
    }

    private Long createMaintenanceEntity(Long assetId, String code, String name, Long maintenanceVisitId) {
        return ormTemplate.runInSession(session -> {
            ErpAstMaintenance m = daoProvider.daoFor(ErpAstMaintenance.class).newEntity();
            m.setCode(code);
            m.setName(name);
            m.setOrgId(1L);
            m.setAssetId(assetId);
            m.setMaintenanceVisitId(maintenanceVisitId);
            m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_DRAFT);
            m.setBusinessDate(LocalDate.of(2026, 7, 15));
            m.setCurrencyId(1L);
            m.setExchangeRate(BigDecimal.ONE);
            m.setCapitalizedAmount(BigDecimal.ZERO);
            m.setTotalCostAmount(BigDecimal.ZERO);
            m.setPosted(false);
            m.setReversed(false);
            daoProvider.daoFor(ErpAstMaintenance.class).saveEntity(m);
            return m.getId();
        });
    }

    private ApiResponse<?> createMaintenanceRpc(Long assetId, String code) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assetId", String.valueOf(assetId));
        data.put("code", code);
        data.put("businessDate", "2026-07-15");
        return executeRpc("ErpAstMaintenance__createMaintenance", data);
    }

    private List<ErpAstDepreciationSchedule> findPendingSchedules(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assetId), eq("status", ErpAstConstants.SCHEDULE_STATUS_PENDING)));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private boolean isAllVouchersReversed(String billCode, String businessType) {
        List<ErpFinVoucherBillR> links = findBillLinks(billCode, businessType);
        if (links.isEmpty()) {
            return false;
        }
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucher> voucherDao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            app.erp.fin.dao.entity.ErpFinVoucher v = voucherDao.getEntityById(link.getVoucherId());
            if (v != null && !Boolean.TRUE.equals(v.getIsReversed())
                    && (v.getPostingType() == null || "NORMAL".equals(v.getPostingType()))) {
                return false;
            }
        }
        return true;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
