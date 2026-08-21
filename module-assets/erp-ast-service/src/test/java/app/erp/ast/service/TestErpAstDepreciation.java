package app.erp.ast.service;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import io.nop.api.core.annotations.autotest.EnableSnapshot;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 折旧计算/执行/批量 + DEPRECIATION(70) 过账 + 残值约束 + 期间控制 + 幂等 端到端单测（plan Phase 3）。
 *
 * <p>覆盖 plan Proof 四场景：直线法每期等额+最后一期到残值+DEPRECIATION 凭证、双倍余额递减+残值约束、
 * 批量按资产处理+期间 CLOSED 拒绝+幂等冲销重生成、期间控制（缺失/已结账拒绝）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstDepreciation extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final String START_PERIOD = "2026-07";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstDepreciationScheduleBiz scheduleBiz;

    @Test
    public void testStraightLinePerPeriodEqualAndLastToResidual() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-SL", "直线法类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-SL", "直线法资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 直线法每期等额 1000，12 期，最后一期到残值 0
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < 12; i++) {
            String period = periodAt(i);
            ErpAstDepreciationSchedule s = ormTemplate.runInSession(session -> scheduleBiz.executeDepreciation(assetId, period, CTX));
            assertEquals(0, s.getActualAmount().compareTo(new BigDecimal("1000")),
                    "期间 " + period + " 直线法每期等额 1000");
            assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, s.getStatus());
            assertTrue(Boolean.TRUE.equals(s.getPosted()), "DEPRECIATION 过账 posted=true");
            total = total.add(s.getActualAmount());
        }
        assertEquals(0, total.compareTo(new BigDecimal("12000")), "12 期累计折旧=原值");
        output("1_straight_line_schedules.json5", daoProvider.daoFor(ErpAstDepreciationSchedule.class)
                .findAllByQuery(new QueryBean()).stream()
                .filter(s -> s.getAssetId().equals(assetId)).map(this::scheduleState)
                .collect(java.util.stream.Collectors.toList()));

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("12000")), "资产累计折旧汇总=12000");
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(BigDecimal.ZERO), "资产净值=残值 0");
        output("2_asset_state.json5", assetState(asset));

        // DEPRECIATION(70) 凭证经业财回链可查
        assertTrue(!findBillLinks("AST-SL#" + START_PERIOD, "DEPRECIATION").isEmpty(), "首期 DEPRECIATION 凭证回链已落库");
    }

    @Test
    public void testDoubleDecliningResidualConstraint() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-DDB", "双倍余额递减类别",
                    ErpAstConstants.DEPRECIATION_METHOD_DECLINING, 48, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-DDB", "双倍余额递减资产", categoryId, "1",
                    new BigDecimal("48000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_DECLINING, 48,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 首期 DDB = 2 × 48000 / 48 = 2000
        ErpAstDepreciationSchedule first = ormTemplate.runInSession(session -> scheduleBiz.executeDepreciation(assetId, periodAt(0), CTX));
        assertEquals(0, first.getActualAmount().compareTo(new BigDecimal("2000")), "首期 DDB=2000");

        // 执行剩余各期，残值约束：净值不低于残值 0（不出现负数）
        for (int i = 1; i < 48; i++) {
            final int fi = i;
            ErpAstDepreciationSchedule s = ormTemplate.runInSession(session -> scheduleBiz.executeDepreciation(assetId, periodAt(fi), CTX));
            assertTrue(s.getActualAmount().signum() >= 0, "期间 " + i + " 折旧非负");
            assertTrue(nz(s.getNetBookValue()).signum() >= 0, "期间 " + i + " 净值不低于残值");
        }
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        // 末期满寿命净值收敛到残值（容许舍入误差）
        assertTrue(nz(asset.getNetBookValue()).compareTo(new BigDecimal("1")) <= 0,
                "末期满寿命净值收敛到残值，实际=" + asset.getNetBookValue());
        output("1_ddb_asset_state.json5", assetState(asset));
    }

    @Test
    public void testBatchDepreciationProcessesAllAssets() {
        ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-BAT", "批量类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            AstTestSupport.seedAsset(daoProvider, "AST-BAT-1", "批量资产1", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            AstTestSupport.seedAsset(daoProvider, "AST-BAT-2", "批量资产2", categoryId, "1",
                    new BigDecimal("6000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return null;
        });

        int processed = ormTemplate.runInSession(session -> scheduleBiz.executeBatchDepreciation(START_PERIOD, CTX));
        assertEquals(2, processed, "批量折旧处理 2 个使用中资产");

        ErpAstDepreciationSchedule s1 = findSchedule("AST-BAT-1", START_PERIOD);
        ErpAstDepreciationSchedule s2 = findSchedule("AST-BAT-2", START_PERIOD);
        assertNotNull(s1);
        assertNotNull(s2);
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, s1.getStatus());
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, s2.getStatus());
        assertTrue(Boolean.TRUE.equals(s1.getPosted()) && Boolean.TRUE.equals(s2.getPosted()));
        // 批量错误隔离：非使用中资产不计提
        assertEquals(0, s1.getActualAmount().compareTo(new BigDecimal("1000")), "AST-BAT-1 月折旧 1000");
        assertEquals(0, s2.getActualAmount().compareTo(new BigDecimal("500")), "AST-BAT-2 月折旧 500");
        output("1_batch_schedules.json5", java.util.Arrays.asList(scheduleState(s1), scheduleState(s2)));
    }

    @Test
    public void testPeriodControlRejectsClosedAndMissing() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            // 目标期间已结账（取开放序列之外的期间，避免与 seedBasics 的开放期重复）
            AstTestSupport.seedPeriod(daoProvider, "2026-05", 2026, 5, ErpAstConstants.PERIOD_STATUS_CLOSED);
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-PC", "期间控制类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-PC", "期间控制资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 已结账期间拒绝补提折旧
        NopException closed = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> scheduleBiz.executeDepreciation(assetId, "2026-05", CTX)));
        assertEquals(ErpAstErrors.ERR_DEPRECIATION_PERIOD_CLOSED.getErrorCode(), closed.getErrorCode());

        // 未找到期间拒绝
        NopException notFound = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> scheduleBiz.executeDepreciation(assetId, "2099-01", CTX)));
        assertEquals(ErpAstErrors.ERR_DEPRECIATION_PERIOD_NOT_FOUND.getErrorCode(), notFound.getErrorCode());
    }

    @Test
    public void testIdempotentReExecuteReversesAndRegenerates() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-IDEM", "幂等类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-IDEM", "幂等资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        String period = START_PERIOD;
        ormTemplate.runInSession(() -> scheduleBiz.executeDepreciation(assetId, period, CTX));
        // 同期间重复执行：先红冲再重新生成（幂等，不双计）
        ErpAstDepreciationSchedule second = ormTemplate.runInSession(session -> scheduleBiz.executeDepreciation(assetId, period, CTX));

        assertEquals(0, second.getActualAmount().compareTo(new BigDecimal("1000")), "重生成金额=1000");
        assertTrue(Boolean.TRUE.equals(second.getPosted()), "重生成后 posted=true");

        // 资产累计折旧不双计（仍为 1000）
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("1000")),
                "幂等重执行后累计折旧不双计");

        // 三次过账产生 3 条回链：原始（已红冲）+ 红字冲销凭证 + 重生成（有效）
        List<ErpFinVoucherBillR> links = findBillLinks("AST-IDEM#" + period, "DEPRECIATION");
        assertEquals(3, links.size(), "原始 + 红冲 + 重生成三条回链");
        long activeVouchers = links.stream()
                .map(l -> daoProvider.daoFor(ErpFinVoucher.class).getEntityById(l.getVoucherId()))
                .filter(v -> v != null && !Boolean.TRUE.equals(v.getIsReversed()))
                .count();
        assertEquals(1, activeVouchers, "仅一条有效（未红冲）凭证");
    }

    /**
     * G2 批量折旧部分失败隔离（plan 2026-07-31-0744-2-r2-12 P1-MA4-014(c) 残差）。
     *
     * <p>seed 一资产持孤儿态 EXECUTED+posted=true 计划（posted=true 但无对应已过账凭证/业财回链——数据不一致）。
     * 批量重执行该资产时，幂等前置红冲（{@code postingDispatcher.reverse}）因找不到原凭证抛
     * {@code ERR_REVERSE_SOURCE_NOT_FOUND}（红冲为硬前置，向上抛出）。{@code executeBatchDepreciation} 的
     * try/catch 隔离该失败：失败资产不计入 processed，其余资产仍正常计提，无整批事务回滚
     * （{@code IErpFinVoucherBiz.reverse} 的 REQUIRES_NEW 子事务回滚独立，不影响外层批量事务）。
     */
    @Test
    public void testBatchDepreciationIsolatesFailingAsset() {
        ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-BAT-ISO", "批量隔离类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null,
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            // 资产 A：IN_SERVICE 但持孤儿 EXECUTED+posted=true 计划（无凭证回链）→ 批量重执行触发红冲硬前置失败
            String assetA = AstTestSupport.seedAsset(daoProvider, "AST-BAT-FAIL", "批量失败资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            AstTestSupport.seedExecutedPostedSchedule(daoProvider, assetA, "1", START_PERIOD,
                    new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("11000"));
            // 资产 B：干净 IN_SERVICE → 正常计提并过账
            AstTestSupport.seedAsset(daoProvider, "AST-BAT-OK", "批量正常资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return null;
        });

        int processed = ormTemplate.runInSession(session -> scheduleBiz.executeBatchDepreciation(START_PERIOD, CTX));
        // 仅成功资产计入 processed；失败资产被隔离跳过（红冲硬前置异常被 try/catch 吞）
        assertEquals(1, processed, "批量折旧隔离：仅正常资产计入 processed（失败资产跳过）");

        // 正常资产 B：计提 + 过账成功
        ErpAstDepreciationSchedule okSchedule = findSchedule("AST-BAT-OK", START_PERIOD);
        assertNotNull(okSchedule, "正常资产生成折旧计划");
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, okSchedule.getStatus());
        assertTrue(Boolean.TRUE.equals(okSchedule.getPosted()), "正常资产过账成功 posted=true");
        assertNotNull(okSchedule.getVoucherId(), "正常资产生成凭证");

        // 失败资产 A：未被批量改写（红冲在写库前抛出，状态保持种子态）
        ErpAstDepreciationSchedule failSchedule = findSchedule("AST-BAT-FAIL", START_PERIOD);
        assertNotNull(failSchedule);
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, failSchedule.getStatus(), "失败资产计划状态未变");
        assertTrue(Boolean.TRUE.equals(failSchedule.getPosted()), "失败资产 posted 标志未被批量重置");
        assertEquals(0, failSchedule.getActualAmount().compareTo(new BigDecimal("1000")), "失败资产折旧金额未被重算");
    }

    /**
     * G3 折旧 tryPost 悬挂状态 + 重跑自愈（plan 2026-07-31-0744-2-r2-12 P1-MA4-014(d) 残差 / P1-MA4-013）。
     *
     * <p>确定性诱导 {@code DepreciationPostingDispatcher.tryPost} 失败（复用 inventory 无 mock 范式：
     * seed OPEN 期间通过 BizModel 前置守卫 + 省略折旧费用/累计折旧科目映射致引擎 resolveSubjects 抛
     * ERR_SUBJECT_NOT_FOUND → tryPost catch → 吞异常返回 null）。断言 schedule posted=false + voucherId=null
     * 持续（悬挂态）。随后补齐科目映射重跑 executeDepreciation，自愈路径（幂等重算+重试过账）置 posted=true。
     */
    @Test
    public void testDepreciationTryPostFailureLeavesSuspendedThenSelfHeals() {
        String assetId = ormTemplate.runInSession(session -> {
            // seedBasicsNoDepSubjects：seed 会计账套 + OPEN 期间序列，但省略折旧科目 6602/1602
            seedBasicsNoDepSubjects();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-SUSP", "悬挂态类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-SUSP", "悬挂态资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 首次折旧：BizModel 守卫通过（期间 OPEN），计算并落库计划，但 tryPost 因科目缺失失败 → posted=false 悬挂
        ErpAstDepreciationSchedule suspended = ormTemplate.runInSession(session ->
                scheduleBiz.executeDepreciation(assetId, START_PERIOD, CTX));
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, suspended.getStatus(), "折旧已执行（计算+落库）");
        assertFalse(Boolean.TRUE.equals(suspended.getPosted()), "过账失败保持 posted=false（悬挂态）");
        assertNull(suspended.getVoucherId(), "voucherId=null 持续（悬挂态）");
        assertEquals(0, suspended.getActualAmount().compareTo(new BigDecimal("1000")), "折旧金额已计算 1000");

        // 悬挂态持久：重新查询确认
        ErpAstDepreciationSchedule persisted = findSchedule("AST-SUSP", START_PERIOD);
        assertFalse(Boolean.TRUE.equals(persisted.getPosted()), "DB 中 posted=false 持续");
        assertNull(persisted.getVoucherId(), "DB 中 voucherId=null 持续");

        // 自愈路径：补齐科目映射后重跑 executeDepreciation（幂等：posted=false 不触发红冲，重算+重试过账）
        ormTemplate.runInSession(session -> {
            AstTestSupport.seedSubject(daoProvider, "6602", "管理费用");
            AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧");
            return null;
        });
        ErpAstDepreciationSchedule healed = ormTemplate.runInSession(session ->
                scheduleBiz.executeDepreciation(assetId, START_PERIOD, CTX));
        assertTrue(Boolean.TRUE.equals(healed.getPosted()), "自愈：重跑后 posted=true");
        assertNotNull(healed.getVoucherId(), "自愈：重跑后生成凭证 voucherId");

        // 资产累计折旧不双计（幂等重算，单期 1000）
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("1000")),
                "自愈后累计折旧不双计（单期 1000）");
    }

    /**
     * G4 非零残值折旧算术集成测试（plan 2026-07-31-0744-2-r2-12 P1-MA4-014(e)）。
     *
     * <p>seed 残值≠0（原值 10000/残值 2000/3 期直线法）。直线法每期 (10000−2000)/3=2666.6667（HALF_UP 向上舍入），
     * 第 3 期净值 4666.6666−2666.6667=1999.9999&lt;残值 2000 触发截断分支（DepreciationCalculator:71-73）→
     * amount=nbv−残值=2666.6666，末期净值精确收敛到残值 2000（非 0）。闭合既有测试 residualValue 恒为 ZERO 的缺口。
     * 截断/返 0 分支的纯函数覆盖见 {@code TestDepreciationCalculator}。
     */
    @Test
    public void testNonZeroResidualStraightLineClampsToResidual() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-RES", "非零残值类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 3, null,
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            return AstTestSupport.seedAsset(daoProvider, "AST-RES", "非零残值资产", categoryId, "1",
                    new BigDecimal("10000"), new BigDecimal("2000"),
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 3,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 期 1、2：直线法等额 2666.6667（未触残值约束）
        ErpAstDepreciationSchedule s1 = ormTemplate.runInSession(session ->
                scheduleBiz.executeDepreciation(assetId, periodAt(0), CTX));
        assertEquals(0, s1.getActualAmount().compareTo(new BigDecimal("2666.6667")), "期 1 直线法 2666.6667");
        assertTrue(Boolean.TRUE.equals(s1.getPosted()));

        ErpAstDepreciationSchedule s2 = ormTemplate.runInSession(session ->
                scheduleBiz.executeDepreciation(assetId, periodAt(1), CTX));
        assertEquals(0, s2.getActualAmount().compareTo(new BigDecimal("2666.6667")), "期 2 直线法 2666.6667");

        // 期 3（末期）：截断分支触发，amount=2666.6666（非 2666.6667），净值精确=残值 2000
        ErpAstDepreciationSchedule s3 = ormTemplate.runInSession(session ->
                scheduleBiz.executeDepreciation(assetId, periodAt(2), CTX));
        assertEquals(0, s3.getActualAmount().compareTo(new BigDecimal("2666.6666")),
                "末期截断分支：amount=nbv−残值=2666.6666");
        assertEquals(0, nz(s3.getNetBookValue()).compareTo(new BigDecimal("2000")), "末期净值=残值 2000（非 0）");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(new BigDecimal("2000")), "资产净值收敛到残值 2000（非 0）");
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("8000")),
                "累计折旧=原值−残值=8000");
    }

    /**
     * 并发首次折旧 UK 兜底（plan 2026-07-30-0841-2 R1.28 P1-MA2-089）：2 线程同时为同资产同期首次计提，
     * UK_AST_DEPRECIATION_ASSET_PERIOD 保证仅 1 条 active 计划行 + 累计折旧不双计；冲突方抛友好错误码。
     */
    // 并发赢家的 schedule id 非确定（两事务抢占序 56/57 随机），DB 快照基线不可比对——
    // 关闭输出表校验，以方法内确定性断言为准（nop-testing「非确定路径不录制不可比基线」）。
    @EnableSnapshot(checkOutput = false)
    @Test
    public void testConcurrentFirstDepreciationNoDuplicate() throws Exception {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-UK089", "UK089类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-UK089", "UK089资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });
        String period = START_PERIOD;

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ContextProvider.newContext();
                    try {
                        startGate.await();
                        ormTemplate.runInSession(s -> scheduleBiz.executeDepreciation(assetId, period, CTX));
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                    } finally {
                        ContextProvider.instance().detachContext();
                        doneLatch.countDown();
                    }
                });
            }
            startGate.countDown();
            assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "全部 worker 应在 60s 内完成");
        } finally {
            pool.shutdownNow();
        }

        // 数据完整性：UK 兜底 + 资产乐观锁共同保证仅 1 条 active 计划行 + 累计折旧不双计（单期 1000）。
        // 注：同资产并发场景下资产 version 冲突（update-entity-not-found）会先于/协同 schedule UK 拦截第二事务，
        // 二者均使第二事务回滚——schedule UK 作为纵深防御。
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assetId), eq("period", period)));
        List<ErpAstDepreciationSchedule> schedules = daoProvider.daoFor(ErpAstDepreciationSchedule.class)
                .findAllByQuery(q);
        assertEquals(1L, schedules.size(), "并发首次折旧：仅 1 条计划行（无重复，UK + 乐观锁兜底）");
        ErpAstDepreciationSchedule schedule = schedules.get(0);
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, schedule.getStatus(), "并发赢家计划行状态 EXECUTED");
        assertEquals(0, schedule.getActualAmount().compareTo(new BigDecimal("1000")), "赢家单期折旧额 1000");
        assertEquals(0, schedule.getAccumulatedDepreciation().compareTo(new BigDecimal("1000")), "计划行累计折旧 1000");
        assertEquals(0, schedule.getNetBookValue().compareTo(new BigDecimal("11000")), "计划行净值 12000-1000");
        assertTrue(Boolean.TRUE.equals(schedule.getPosted()), "赢家计划行已过账 posted=true");
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("1000")),
                "累计折旧不双计（单期 1000）");
        // 若有线程抛错，应为并发拒绝（schedule UK 友好码 或 资产乐观锁冲突），而非静默双计
        if (firstError.get() != null) {
            Throwable c = firstError.get();
            String code = (c instanceof NopException) ? ((NopException) c).getErrorCode() : c.getClass().getName();
            assertTrue(
                    ErpAstErrors.ERR_AST_DEPRECIATION_ALREADY_EXECUTED.getErrorCode().equals(code)
                            || code.contains("update-entity-not-found")
                            || code.contains("version"),
                    "并发冲突应被拦截（UK 友好码或乐观锁冲突），实际: " + code);
        }
    }

    // ---------- helpers ----------

    private java.util.Map<String, Object> scheduleState(ErpAstDepreciationSchedule s) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("period", s.getPeriod());
        m.put("status", s.getStatus());
        m.put("actualAmount", s.getActualAmount());
        m.put("netBookValue", s.getNetBookValue());
        m.put("posted", s.getPosted());
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

    private void seedBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, "1");
        AstTestSupport.seedSubject(daoProvider, "6602", "管理费用");
        AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧");
        AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
        // 默认开放期间序列（直线法/双倍余额递减各期所需）
        for (int i = 0; i < 48; i++) {
            YearMonth ym = YearMonth.parse(START_PERIOD).plusMonths(i);
            AstTestSupport.seedPeriod(daoProvider, ym.toString(), ym.getYear(), ym.getMonthValue(),
                    ErpAstConstants.PERIOD_STATUS_OPEN);
        }
    }

    /**
     * seed 会计账套 + OPEN 期间序列，但故意省略折旧费用/累计折旧科目（6602/1602），
     * 确定性诱导 tryPost 失败（引擎 resolveSubjects 抛 ERR_SUBJECT_NOT_FOUND），构造 posted=false 悬挂态。
     */
    private void seedBasicsNoDepSubjects() {
        AstTestSupport.seedAcctSchema(daoProvider, "1");
        for (int i = 0; i < 48; i++) {
            YearMonth ym = YearMonth.parse(START_PERIOD).plusMonths(i);
            AstTestSupport.seedPeriod(daoProvider, ym.toString(), ym.getYear(), ym.getMonthValue(),
                    ErpAstConstants.PERIOD_STATUS_OPEN);
        }
    }

    private static String periodAt(int offset) {
        return YearMonth.parse(START_PERIOD).plusMonths(offset).toString();
    }

    private ErpAstDepreciationSchedule findSchedule(String assetCode, String period) {
        IEntityDao<ErpAstAsset> assetDao = daoProvider.daoFor(ErpAstAsset.class);
        QueryBean aq = new QueryBean();
        aq.addFilter(eq("code", assetCode));
        List<ErpAstAsset> assets = assetDao.findAllByQuery(aq);
        if (assets.isEmpty()) {
            return null;
        }
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assets.get(0).getId()), eq("period", period)));
        List<ErpAstDepreciationSchedule> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
