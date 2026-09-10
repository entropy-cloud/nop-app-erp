package app.erp.ast.service;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CATCHUP 汇总凭证引擎侧红冲回退闭环测试（P2-CK-ast2-024-r3，plan 2026-09-10-0705-2）。
 *
 * <p>覆盖 finding 报告缺陷链：财务员红冲已过账 CATCHUP 汇总凭证（billHeadCode = 资产码#当期#CATCHUP）
 * → 经引擎派发通道（{@code IErpFinVoucherBiz.reverse} → {@code VoucherReversedEvent} →
 * {@code ErpAstDepreciationReversalListener}，财务员视角发起，非域内直调 listener）→ 修复前 listener
 * 对 {@code #CATCHUP} 后缀静默 return → 计划行滞 posted=true/voucherId 不清、资产累计折旧/净值不回退、
 * 无告警；自愈双断（重补提幂等跳过 + 逐期 reverseDepreciation 键失配 ERR_REVERSE_SOURCE_NOT_FOUND）。
 *
 * <p>修复后语义（方案 (a)，{@code backfillCatchUpSchedules} 逆操作）：按 reversalOfVoucherId 反查全部
 * 映射计划行聚合回退（逐行 REVERSED/posted=false/voucherId=null + 资产累计折旧/净值按 ΣactualAmount
 * 回退），语义与逐期红冲先例（{@code TestErpAstPostingReverse.testDepreciationReverseRollsBackAssetCard}）
 * 一致。对照组：逐期凭证红冲既有绿路径经同一引擎通道保持绿。
 *
 * <p>纯显式断言（层 1，实体重读观察可观察行为），无快照输出。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstCatchUpReversal extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstDepreciationScheduleBiz scheduleBiz;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testCatchUpVoucherEngineReversalRollsBackSchedulesAndAsset() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-CUR-M", "补提红冲多期类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-CUR-01", "补提红冲多期资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        // 前置：补提两漏提期 → 单张汇总凭证（billHeadCode = AST-CUR-01#2026-07#CATCHUP），计划行 posted=true
        List<ErpAstDepreciationSchedule> created = ormTemplate.runInSession(session ->
                scheduleBiz.catchUpDepreciation(assetId, "2026-07", List.of("2026-06", "2026-05"), CTX));
        assertEquals(2, created.size(), "前置：补提落行 2 条");
        String catchUpVoucherId = created.get(0).getVoucherId();
        assertNotNull(catchUpVoucherId, "前置：补提汇总凭证回填 voucherId");
        assertEquals(catchUpVoucherId, created.get(1).getVoucherId(), "前置：两漏提期共享同一汇总凭证");
        assertTrue(!findBillLinks("AST-CUR-01#2026-07#CATCHUP", "DEPRECIATION").isEmpty(),
                "前置：CATCHUP 汇总凭证回链已落库");
        ErpAstAsset before = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(before.getAccumulatedDepreciation()).compareTo(new BigDecimal("2000")),
                "前置：补提后累计折旧=2000");
        assertEquals(0, nz(before.getNetBookValue()).compareTo(new BigDecimal("10000")), "前置：补提后净值=10000");

        // 引擎派发通道红冲（财务员视角，voucherBiz.reverse 链路）
        String redVoucherId = ormTemplate.runInSession(session ->
                voucherBiz.reverse("AST-CUR-01#2026-07#CATCHUP", ErpFinBusinessType.DEPRECIATION, CTX));
        assertNotNull(redVoucherId, "红冲红字凭证生成");

        // 断言：全部映射计划行回退（REVERSED/posted=false/voucherId=null，与逐期红冲先例语义一致）
        ErpAstDepreciationSchedule first = findSchedule(assetId, "2026-05");
        ErpAstDepreciationSchedule second = findSchedule(assetId, "2026-06");
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_REVERSED, first.getStatus(),
                "漏提期 2026-05 计划行回退 REVERSED");
        assertFalse(Boolean.TRUE.equals(first.getPosted()), "漏提期 2026-05 posted=false");
        assertNull(first.getVoucherId(), "漏提期 2026-05 voucherId 清空");
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_REVERSED, second.getStatus(),
                "漏提期 2026-06 计划行回退 REVERSED");
        assertFalse(Boolean.TRUE.equals(second.getPosted()), "漏提期 2026-06 posted=false");
        assertNull(second.getVoucherId(), "漏提期 2026-06 voucherId 清空");

        // 断言：资产累计折旧/净值回退（ΣactualAmount=2000）
        ErpAstAsset after = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(after.getAccumulatedDepreciation()).compareTo(BigDecimal.ZERO),
                "累计折旧回退 0（红冲前滞留缺陷：不回退保持 2000）");
        assertEquals(0, nz(after.getNetBookValue()).compareTo(new BigDecimal("12000")),
                "净值回退=原值 12000");

        // GL 侧：原汇总凭证已标红冲（红冲本身生效，缺陷仅在其后的域回退段）
        assertTrue(isVoucherReversed("AST-CUR-01#2026-07#CATCHUP"), "CATCHUP 汇总凭证已红字冲销");
    }

    /** 对照组：逐期凭证红冲既有绿路径经同一引擎派发通道保持（非 CATCHUP 分支现状保持的回归钉）。 */
    @Test
    public void testPerPeriodVoucherEngineReversalGreenPath() {
        String assetId = ormTemplate.runInSession(session -> {
            seedBasics();
            String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-CUR-P", "补提红冲对照类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null, null, null);
            return AstTestSupport.seedAsset(daoProvider, "AST-CUR-P", "补提红冲对照资产", categoryId, "1",
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        ormTemplate.runInSession(() -> scheduleBiz.executeDepreciation(assetId, "2026-07", CTX));
        ErpAstAsset afterExec = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(afterExec.getAccumulatedDepreciation()).compareTo(new BigDecimal("1000")),
                "前置：执行后累计折旧=1000");

        String redVoucherId = ormTemplate.runInSession(session ->
                voucherBiz.reverse("AST-CUR-P#2026-07", ErpFinBusinessType.DEPRECIATION, CTX));
        assertNotNull(redVoucherId, "逐期红冲红字凭证生成");

        ErpAstDepreciationSchedule schedule = findSchedule(assetId, "2026-07");
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_REVERSED, schedule.getStatus(), "逐期先例：计划行 REVERSED");
        assertFalse(Boolean.TRUE.equals(schedule.getPosted()), "逐期先例：posted=false");
        assertNull(schedule.getVoucherId(), "逐期先例：voucherId 清空");

        ErpAstAsset after = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(after.getAccumulatedDepreciation()).compareTo(BigDecimal.ZERO), "逐期先例：累计折旧回退 0");
        assertEquals(0, nz(after.getNetBookValue()).compareTo(new BigDecimal("12000")), "逐期先例：净值回退=原值");
        assertTrue(isVoucherReversed("AST-CUR-P#2026-07"), "逐期凭证已红字冲销");
    }

    // ---------- helpers ----------

    private void seedBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, "1");
        AstTestSupport.seedSubject(daoProvider, "6602", "管理费用");
        AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧");
        AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
        for (int i = -2; i < 3; i++) {
            YearMonth ym = YearMonth.parse("2026-07").plusMonths(i);
            AstTestSupport.seedPeriod(daoProvider, ym.toString(), ym.getYear(), ym.getMonthValue(),
                    ErpAstConstants.PERIOD_STATUS_OPEN);
        }
    }

    private ErpAstDepreciationSchedule findSchedule(String assetId, String period) {
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

    private boolean isVoucherReversed(String billCode) {
        List<ErpFinVoucherBillR> links = findBillLinks(billCode, "DEPRECIATION");
        if (links.isEmpty()) {
            return false;
        }
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
        return voucher != null && Boolean.TRUE.equals(voucher.getIsReversed());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
