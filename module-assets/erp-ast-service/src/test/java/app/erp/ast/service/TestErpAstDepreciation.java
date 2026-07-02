package app.erp.ast.service;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-SL", "直线法类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-SL", "直线法资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 直线法每期等额 1000，12 期，最后一期到残值 0
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < 12; i++) {
            String period = periodAt(i);
            ErpAstDepreciationSchedule s = scheduleBiz.executeDepreciation(assetId, period, CTX);
            assertEquals(0, s.getActualAmount().compareTo(new BigDecimal("1000")),
                    "期间 " + period + " 直线法每期等额 1000");
            assertEquals(ErpAstConstants.SCHEDULE_STATUS_EXECUTED, s.getStatus());
            assertTrue(Boolean.TRUE.equals(s.getPosted()), "DEPRECIATION 过账 posted=true");
            total = total.add(s.getActualAmount());
        }
        assertEquals(0, total.compareTo(new BigDecimal("12000")), "12 期累计折旧=原值");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("12000")), "资产累计折旧汇总=12000");
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(BigDecimal.ZERO), "资产净值=残值 0");

        // DEPRECIATION(70) 凭证经业财回链可查
        assertTrue(!findBillLinks("AST-SL#" + START_PERIOD, 70).isEmpty(), "首期 DEPRECIATION 凭证回链已落库");
    }

    @Test
    public void testDoubleDecliningResidualConstraint() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-DDB", "双倍余额递减类别",
                    ErpAstConstants.DEPRECIATION_METHOD_DECLINING, 48, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-DDB", "双倍余额递减资产", categoryId, 1L,
                    new BigDecimal("48000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_DECLINING, 48,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 首期 DDB = 2 × 48000 / 48 = 2000
        ErpAstDepreciationSchedule first = scheduleBiz.executeDepreciation(assetId, periodAt(0), CTX);
        assertEquals(0, first.getActualAmount().compareTo(new BigDecimal("2000")), "首期 DDB=2000");

        // 执行剩余各期，残值约束：净值不低于残值 0（不出现负数）
        for (int i = 1; i < 48; i++) {
            ErpAstDepreciationSchedule s = scheduleBiz.executeDepreciation(assetId, periodAt(i), CTX);
            assertTrue(s.getActualAmount().signum() >= 0, "期间 " + i + " 折旧非负");
            assertTrue(nz(s.getNetBookValue()).signum() >= 0, "期间 " + i + " 净值不低于残值");
        }
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        // 末期满寿命净值收敛到残值（容许舍入误差）
        assertTrue(nz(asset.getNetBookValue()).compareTo(new BigDecimal("1")) <= 0,
                "末期满寿命净值收敛到残值，实际=" + asset.getNetBookValue());
    }

    @Test
    public void testBatchDepreciationProcessesAllAssets() {
        ormTemplate.runInSession(session -> {
            seedBasics();
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-BAT", "批量类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            AstTestSupport.seedAsset(daoProvider, "AST-BAT-1", "批量资产1", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            AstTestSupport.seedAsset(daoProvider, "AST-BAT-2", "批量资产2", categoryId, 1L,
                    new BigDecimal("6000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return null;
        });

        int processed = scheduleBiz.executeBatchDepreciation(START_PERIOD, CTX);
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
    }

    @Test
    public void testPeriodControlRejectsClosedAndMissing() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            // 目标期间已结账（取开放序列之外的期间，避免与 seedBasics 的开放期重复）
            AstTestSupport.seedPeriod(daoProvider, "2026-05", 2026, 5, ErpAstConstants.PERIOD_STATUS_CLOSED);
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-PC", "期间控制类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-PC", "期间控制资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 已结账期间拒绝补提折旧
        NopException closed = assertThrows(NopException.class,
                () -> scheduleBiz.executeDepreciation(assetId, "2026-05", CTX));
        assertEquals(ErpAstErrors.ERR_DEPRECIATION_PERIOD_CLOSED.getErrorCode(), closed.getErrorCode());

        // 未找到期间拒绝
        NopException notFound = assertThrows(NopException.class,
                () -> scheduleBiz.executeDepreciation(assetId, "2099-01", CTX));
        assertEquals(ErpAstErrors.ERR_DEPRECIATION_PERIOD_NOT_FOUND.getErrorCode(), notFound.getErrorCode());
    }

    @Test
    public void testIdempotentReExecuteReversesAndRegenerates() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-IDEM", "幂等类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-IDEM", "幂等资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        String period = START_PERIOD;
        scheduleBiz.executeDepreciation(assetId, period, CTX);
        // 同期间重复执行：先红冲再重新生成（幂等，不双计）
        ErpAstDepreciationSchedule second = scheduleBiz.executeDepreciation(assetId, period, CTX);

        assertEquals(0, second.getActualAmount().compareTo(new BigDecimal("1000")), "重生成金额=1000");
        assertTrue(Boolean.TRUE.equals(second.getPosted()), "重生成后 posted=true");

        // 资产累计折旧不双计（仍为 1000）
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("1000")),
                "幂等重执行后累计折旧不双计");

        // 三次过账产生 3 条回链：原始（已红冲）+ 红字冲销凭证 + 重生成（有效）
        List<ErpFinVoucherBillR> links = findBillLinks("AST-IDEM#" + period, 70);
        assertEquals(3, links.size(), "原始 + 红冲 + 重生成三条回链");
        long activeVouchers = links.stream()
                .map(l -> daoProvider.daoFor(ErpFinVoucher.class).getEntityById(l.getVoucherId()))
                .filter(v -> v != null && !Boolean.TRUE.equals(v.getIsReversed()))
                .count();
        assertEquals(1, activeVouchers, "仅一条有效（未红冲）凭证");
    }

    // ---------- helpers ----------

    private void seedBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, 1L);
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

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, int businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
