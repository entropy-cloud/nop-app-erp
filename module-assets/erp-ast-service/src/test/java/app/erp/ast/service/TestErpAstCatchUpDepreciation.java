package app.erp.ast.service;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 折旧漏提补提方式B（RC-R1.52，L1 UC-AST-07）+ 出售补提接线（reuse P1-RC-029 投影，L1 UC-AST-05 ⑤）端到端单测。
 *
 * <p>覆盖 plan Proof 场景：单漏提期补提金额=漏提月额 / 多漏提期累加单张汇总凭证 / 补提后累计折旧+净值一致 /
 * 已结账当前期间拒绝 / 幂等重补提不双计 / IDLE 不允许补提 / 漏提期晚于当前期间拒绝 / 出售补提接线后 gainLoss 正确 +
 * 凭证标注可追溯（billHeadCode #CATCHUP 后缀 + 凭证行 memo「补提 {periods}」）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstCatchUpDepreciation extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpAstDepreciationScheduleBiz scheduleBiz;

    // ---------- 方式B 补提 ----------

    @Test
    public void testSingleMissedPeriodCatchUpAmount() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-CU-S", "补提单期类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-CU-S", "补提单期资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        List<ErpAstDepreciationSchedule> created = ormTemplate.runInSession(session ->
                scheduleBiz.catchUpDepreciation(assetId, "2026-07", List.of("2026-06"), CTX));

        // 单漏提期补提金额 = 漏提月额 1000
        assertEquals(1, created.size(), "补提落行 1 条");
        ErpAstDepreciationSchedule schedule = created.get(0);
        assertEquals("2026-06", schedule.getPeriod(), "漏提期=2026-06");
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, schedule.getStatus());
        assertEquals(0, schedule.getActualAmount().compareTo(new BigDecimal("1000")), "补提金额=漏提月额 1000");
        assertTrue(Boolean.TRUE.equals(schedule.getPosted()), "补提汇总凭证 posted=true");

        // 补提后累计折旧/净值一致
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("1000")), "累计折旧=1000");
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(new BigDecimal("11000")), "净值=11000");

        // 凭证标注可追溯：billHeadCode 后缀 #CATCHUP + 行 memo「补提 {periods}」
        assertCatchUpVoucherTraceable("AST-CU-S", "2026-07", "2026-06");
        output("1_single_missed_schedule.json5", scheduleState(schedule));
        output("2_asset_state.json5", assetState(asset));
    }

    @Test
    public void testMultiMissedPeriodsAggregatedSingleVoucher() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-CU-M", "补提多期类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-CU-M", "补提多期资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        List<ErpAstDepreciationSchedule> created = ormTemplate.runInSession(session ->
                scheduleBiz.catchUpDepreciation(assetId, "2026-07", List.of("2026-06", "2026-05"), CTX));

        // 多漏提期累加：Σ = 1000×2 = 2000，单张汇总凭证
        assertEquals(2, created.size(), "补提落行 2 条");
        assertEquals(0, created.stream().map(s -> nz(s.getActualAmount())).reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(new BigDecimal("2000")), "Σ漏提期补提额=2000");
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("2000")), "累计折旧=2000");
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(new BigDecimal("10000")), "净值=10000");

        // 两漏提期均 EXECUTED，共享同一张汇总凭证（voucherId 一致）
        assertEquals(created.get(0).getVoucherId(), created.get(1).getVoucherId(), "共享同一张汇总凭证");
        assertCatchUpVoucherAmount("AST-CU-M", "2026-07", new BigDecimal("2000"));
        output("1_multi_missed_schedules.json5", created.stream().map(this::scheduleState)
                .collect(java.util.stream.Collectors.toList()));
        output("2_asset_state.json5", assetState(asset));
    }

    @Test
    public void testClosedCurrentPeriodRejected() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            // 当前期间取开放序列之外的期间（2026-04）标记 CLOSED
            AstTestSupport.seedPeriod(daoProvider, "2026-04", 2026, 4, ErpAstConstants.PERIOD_STATUS_CLOSED);
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-CU-C", "补提结账拒绝类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-CU-C", "补提结账拒绝资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 当前期间已结账 → 拒绝补提（补提凭证须记账于开放期间）
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session ->
                        scheduleBiz.catchUpDepreciation(assetId, "2026-04", List.of("2026-03"), CTX)));
        assertEquals(ErpAstErrors.ERR_DEPRECIATION_PERIOD_CLOSED.getErrorCode(), ex.getErrorCode(),
                "已结账当前期间拒绝补提");
    }

    @Test
    public void testIdleAssetRejectedFromCatchUp() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-CU-IDLE", "补提闲置拒绝类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-CU-IDLE", "补提闲置拒绝资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IDLE);
        });

        // IDLE 不允许补提（闲置期无折旧义务，恢复至 IN_SERVICE 后方可补提）
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session ->
                        scheduleBiz.catchUpDepreciation(assetId, "2026-07", List.of("2026-06"), CTX)));
        assertEquals(ErpAstErrors.ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE.getErrorCode(), ex.getErrorCode(),
                "IDLE 资产拒绝补提");
    }

    @Test
    public void testFutureMissedPeriodRejected() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-CU-F", "补提未来期拒绝类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-CU-F", "补提未来期拒绝资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 漏提期晚于当前期间 → 拒绝（不提前记账未来期间）
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session ->
                        scheduleBiz.catchUpDepreciation(assetId, "2026-07", List.of("2026-08"), CTX)));
        assertEquals(ErpAstErrors.ERR_DEPRECIATION_CATCHUP_PERIOD_INVALID.getErrorCode(), ex.getErrorCode(),
                "晚于当前期间的漏提期拒绝");
    }

    @Test
    public void testIdempotentReCatchUpNoDoubleCount() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-CU-IDEM", "补提幂等类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-CU-IDEM", "补提幂等资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        List<String> missed = List.of("2026-06", "2026-05");
        ormTemplate.runInSession(session -> scheduleBiz.catchUpDepreciation(assetId, "2026-07", missed, CTX));

        // 重补提同漏提期：已 EXECUTED 跳过，零新行零凭证，不双计
        List<ErpAstDepreciationSchedule> second = ormTemplate.runInSession(session ->
                scheduleBiz.catchUpDepreciation(assetId, "2026-07", missed, CTX));
        assertTrue(second.isEmpty(), "重补提零新行");
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("2000")),
                "重补提不双计（累计折旧仍=2000）");

        List<ErpFinVoucherBillR> links = findBillLinks("AST-CU-IDEM#2026-07#CATCHUP", "DEPRECIATION");
        assertEquals(1, links.size(), "幂等：仅 1 张补提汇总凭证回链");
    }

    // ---------- 出售补提接线（reuse P1-RC-029 投影） ----------

    @Test
    public void testDisposalSaleCatchUpWiringCorrectsGainLoss() {
        setUser();
        long[] assetIdHolder = new long[1];
        Long disposalId = ormTemplate.runInSession(session -> {
            seedBasics();
            Long gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-CU-DISP", "出售接线类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-CU-DISP", "出售接线资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            // 已执行 2026-05/06 两期折旧（各 1000，累计 2000，净值 10000）
            AstTestSupport.seedExecutedPostedSchedule(daoProvider, assetId, 1L, "2026-05",
                    new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("11000"));
            AstTestSupport.seedExecutedPostedSchedule(daoProvider, assetId, 1L, "2026-06",
                    new BigDecimal("1000"), new BigDecimal("2000"), new BigDecimal("10000"));
            ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
            asset.setAccumulatedDepreciation(new BigDecimal("2000"));
            asset.setNetBookValue(new BigDecimal("10000"));
            daoProvider.daoFor(ErpAstAsset.class).saveEntity(asset);
            // 后续未执行折旧计划（处置后应 CANCELLED）
            AstTestSupport.seedPendingSchedule(daoProvider, assetId, 1L, "2026-08");
            return seedDisposal("DISP-CU-001", assetId, ErpAstConstants.DISPOSAL_TYPE_SOLD,
                    new BigDecimal("7000"), LocalDate.of(2026, 7, 15));
        });

        executeRpc("ErpAstDisposal__submitForApproval", Map.of("id", String.valueOf(disposalId)));
        executeRpc("ErpAstDisposal__approve", Map.of("id", String.valueOf(disposalId)));
        ErpAstDisposal disposal = daoProvider.daoFor(ErpAstDisposal.class).getEntityById(disposalId);

        // 接线：处置前补提 2026-07 当期折旧 1000 → 累计折旧 3000 → 净值 9000
        // gainLoss = 7000 − 9000 = −2000（损失），非未补提的 7000 − 10000 = −3000
        assertTrue(Boolean.TRUE.equals(disposal.getPosted()), "出售过账 posted=true");
        assertEquals(0, disposal.getGainLoss().compareTo(new BigDecimal("-2000")),
                "出售补提接线后 gainLoss 正确（−2000）");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_SOLD, asset.getStatus(), "资产终态=SOLD");
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("3000")),
                "补提后累计折旧=3000");

        // 补提期间 2026-07 计划 EXECUTED + posted（补提汇总凭证）
        ErpAstDepreciationSchedule catchUpSchedule = findSchedule(assetIdHolder[0], "2026-07");
        assertNotNull(catchUpSchedule, "出售期补提计划落行");
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, catchUpSchedule.getStatus());
        assertTrue(Boolean.TRUE.equals(catchUpSchedule.getPosted()), "出售期补提计划 posted=true");
        assertEquals(0, catchUpSchedule.getActualAmount().compareTo(new BigDecimal("1000")), "出售期补提额=1000");

        // 后续折旧计划 CANCELLED
        ErpAstDepreciationSchedule future = findSchedule(assetIdHolder[0], "2026-08");
        assertNotNull(future);
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_CANCELLED, future.getStatus(), "后续折旧 CANCELLED");

        // 补提凭证标注可追溯 + 处置凭证回链
        assertCatchUpVoucherTraceable("AST-CU-DISP", "2026-07", "2026-07");
        assertTrue(!findBillLinks("DISP-CU-001", "DISPOSAL").isEmpty(), "DISPOSAL 凭证回链已落库");
        output("1_disposal_state.json5", disposalState(disposal));
        output("2_catchup_schedule.json5", scheduleState(catchUpSchedule));
    }

    // ---------- helpers ----------

    private void seedBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, 1L);
        AstTestSupport.seedSubject(daoProvider, "6602", "管理费用");
        AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧");
        AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
        // RC-R1.53：处置凭证 1606 固定资产清理中间科目腿（两步流 Provider 引用，缺失则过账 ERR_SUBJECT_NOT_FOUND）
        AstTestSupport.seedSubject(daoProvider, "1606", "固定资产清理");
        for (int i = -2; i < 3; i++) {
            YearMonth ym = YearMonth.parse("2026-07").plusMonths(i);
            AstTestSupport.seedPeriod(daoProvider, ym.toString(), ym.getYear(), ym.getMonthValue(),
                    ErpAstConstants.PERIOD_STATUS_OPEN);
        }
    }

    /** 补提凭证可追溯断言：billHeadCode 后缀 #CATCHUP 回链 + 凭证行 memo 含「补提 {periods}」。 */
    private void assertCatchUpVoucherTraceable(String assetCode, String currentPeriod, String attributedPeriod) {
        List<ErpFinVoucherBillR> links = findBillLinks(assetCode + "#" + currentPeriod + "#CATCHUP", "DEPRECIATION");
        assertTrue(!links.isEmpty(), "补提汇总凭证回链已落库（billHeadCode 后缀 #CATCHUP）");
        ErpFinVoucherBillR link = links.get(0);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", link.getVoucherId()));
        List<ErpFinVoucherLine> lines = lineDao.findAllByQuery(q);
        assertFalse(lines.isEmpty(), "补提凭证行存在");
        boolean memoHit = lines.stream().anyMatch(l -> l.getMemo() != null && l.getMemo().contains(attributedPeriod));
        assertTrue(memoHit, "补提凭证行 memo 标注归属期间「补提 {" + attributedPeriod + "}」");
    }

    private void assertCatchUpVoucherAmount(String assetCode, String currentPeriod, BigDecimal expectedTotal) {
        List<ErpFinVoucherBillR> links = findBillLinks(assetCode + "#" + currentPeriod + "#CATCHUP", "DEPRECIATION");
        assertTrue(!links.isEmpty(), "补提汇总凭证回链已落库");
        ErpFinVoucherBillR link = links.get(0);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", link.getVoucherId()));
        BigDecimal total = lineDao.findAllByQuery(q).stream()
                .map(l -> nz(l.getDebitAmount() != null ? l.getDebitAmount() : l.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, total.compareTo(expectedTotal), "汇总凭证金额=Σ漏提期补提额");
    }

    private ApiResponse<?> executeRpc(String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    private void setUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    private Long seedDisposal(String code, Long assetId, String disposalType, BigDecimal disposalAmount,
                              LocalDate businessDate) {
        IEntityDao<ErpAstDisposal> dao = daoProvider.daoFor(ErpAstDisposal.class);
        ErpAstDisposal disposal = new ErpAstDisposal();
        disposal.setCode(code);
        disposal.setOrgId(1L);
        disposal.setAssetId(assetId);
        disposal.setDisposalType(disposalType);
        disposal.setDisposalAmount(disposalAmount);
        disposal.setCurrencyId(1L);
        disposal.setExchangeRate(BigDecimal.ONE);
        disposal.setBusinessDate(businessDate);
        disposal.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(disposal);
        return disposal.getId();
    }

    private ErpAstDepreciationSchedule findSchedule(Long assetId, String period) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assetId), eq("period", period)));
        List<ErpAstDepreciationSchedule> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private java.util.Map<String, Object> scheduleState(ErpAstDepreciationSchedule s) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("period", s.getPeriod());
        m.put("status", s.getStatus());
        m.put("actualAmount", s.getActualAmount());
        m.put("netBookValue", s.getNetBookValue());
        m.put("posted", s.getPosted());
        m.put("voucherId", s.getVoucherId());
        return m;
    }

    private java.util.Map<String, Object> assetState(ErpAstAsset a) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("code", a.getCode());
        m.put("accumulatedDepreciation", a.getAccumulatedDepreciation());
        m.put("netBookValue", a.getNetBookValue());
        return m;
    }

    private java.util.Map<String, Object> disposalState(ErpAstDisposal d) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("code", d.getCode());
        m.put("gainLoss", d.getGainLoss());
        m.put("posted", d.getPosted());
        m.put("approveStatus", d.getApproveStatus());
        return m;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
