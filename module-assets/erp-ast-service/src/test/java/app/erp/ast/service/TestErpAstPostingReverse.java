package app.erp.ast.service;

import app.erp.ast.biz.IErpAstAssetCapitalizationBiz;
import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.biz.IErpAstDisposalBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstDisposal;
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
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 红字冲减（资本化/折旧/处置）回滚正确性单测（plan Phase 4）。验证已过账单据 reverseApprove/reverseDepreciation：
 * 红字冲销凭证（原凭证 isReversed=true）+ 资产卡片/状态回滚 + posted=false。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstPostingReverse extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstAssetCapitalizationBiz capBiz;
    @Inject
    IErpAstDepreciationScheduleBiz scheduleBiz;
    @Inject
    IErpAstDisposalBiz disposalBiz;

    @Test
    public void testCapitalizationReverseApproveRollsBack() {
        Long capId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-06", 2026, 6, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long fixedAssetSubjectId = AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-REV-CAP", "资本化冲销类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, fixedAssetSubjectId, null, null);
            return seedCapitalization("CAP-REV-001", categoryId,
                    ErpAstConstants.SOURCE_TYPE_DIRECT_PURCHASE, new BigDecimal("12000"),
                    LocalDate.of(2026, 6, 15), "AST-CAP-REV-001");
        });

        capBiz.submit(capId, CTX);
        capBiz.approve(capId, CTX);
        ErpAstAsset assetBefore = findAsset("AST-CAP-REV-001");
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, assetBefore.getStatus());

        ErpAstAssetCapitalization cap = capBiz.reverseApprove(capId, CTX);
        assertFalse(Boolean.TRUE.equals(cap.getPosted()), "反审核后 posted=false");
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, cap.getApproveStatus());

        ErpAstAsset assetAfter = findAsset("AST-CAP-REV-001");
        assertEquals(ErpAstConstants.ASSET_STATUS_DRAFT, assetAfter.getStatus(), "资产状态回滚 DRAFT");
        assertEquals(0, nz(assetAfter.getAccumulatedDepreciation()).compareTo(BigDecimal.ZERO), "累计折旧回滚 0");

        // 折旧计划全部 CANCELLED
        List<ErpAstDepreciationSchedule> schedules = findSchedulesByAsset(assetAfter.getId());
        assertFalse(schedules.isEmpty());
        for (ErpAstDepreciationSchedule s : schedules) {
            assertEquals(ErpAstConstants.SCHEDULE_STATUS_CANCELLED, s.getStatus());
        }
        // CAPITALIZATION 凭证已红冲
        assertTrue(isAllVouchersReversed("CAP-REV-001", 80), "资本化凭证已红字冲销");
    }

    @Test
    public void testDepreciationReverseRollsBackAssetCard() {
        Long assetId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-REV-DEP", "折旧冲销类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, null,
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            return AstTestSupport.seedAsset(daoProvider, "AST-REV-DEP", "折旧冲销资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        });

        scheduleBiz.executeDepreciation(assetId, "2026-07", CTX);
        ErpAstAsset afterExec = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(afterExec.getAccumulatedDepreciation()).compareTo(new BigDecimal("1000")), "执行后累计=1000");

        ErpAstDepreciationSchedule reversed = scheduleBiz.reverseDepreciation(assetId, "2026-07", CTX);
        assertEquals(ErpAstConstants.SCHEDULE_STATUS_REVERSED, reversed.getStatus(), "计划条目 REVERSED");
        assertFalse(Boolean.TRUE.equals(reversed.getPosted()), "posted=false");

        ErpAstAsset afterReverse = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertEquals(0, nz(afterReverse.getAccumulatedDepreciation()).compareTo(BigDecimal.ZERO), "回滚后累计折旧=0");
        assertEquals(0, nz(afterReverse.getNetBookValue()).compareTo(new BigDecimal("12000")), "回滚后净值=原值");
        assertTrue(isAllVouchersReversed("AST-REV-DEP#2026-07", 70), "折旧凭证已红字冲销");
    }

    @Test
    public void testDisposalReverseApproveRestoresAsset() {
        long[] assetIdHolder = new long[1];
        Long disposalId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-REV-DISP", "处置冲销类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-REV-DISP", "处置冲销资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            AstTestSupport.seedPendingSchedule(daoProvider, assetId, 1L, "2026-08");
            return seedDisposal("DISP-REV-001", assetId, ErpAstConstants.DISPOSAL_TYPE_SCRAPPED,
                    BigDecimal.ZERO, LocalDate.of(2026, 7, 15));
        });

        disposalBiz.submit(disposalId, CTX);
        disposalBiz.approve(disposalId, CTX);
        ErpAstAsset scrapped = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_SCRAPPED, scrapped.getStatus());

        ErpAstDisposal disposal = disposalBiz.reverseApprove(disposalId, CTX);
        assertFalse(Boolean.TRUE.equals(disposal.getPosted()), "处置反审核后 posted=false");
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, disposal.getApproveStatus());

        ErpAstAsset restored = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, restored.getStatus(), "资产状态恢复 IN_SERVICE");
        // CANCELLED 折旧计划恢复 PENDING
        List<ErpAstDepreciationSchedule> schedules = findSchedulesByAsset(assetIdHolder[0]);
        for (ErpAstDepreciationSchedule s : schedules) {
            assertEquals(ErpAstConstants.SCHEDULE_STATUS_PENDING, s.getStatus(), "折旧计划恢复 PENDING");
        }
        assertTrue(isAllVouchersReversed("DISP-REV-001", 90), "处置凭证已红字冲销");
    }

    // ---------- helpers ----------

    private void seedCoreBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, 1L);
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
    }

    @Test
    public void testEndToEndCapitalizationDepreciationDisposal() {
        // 端到端：资本化 → 多期折旧 → 处置，全程状态与汇总一致（plan Phase 4 端到端退出标准）
        long[] assetIdHolder = new long[1];
        Long capId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-06", 2026, 6, ErpAstConstants.PERIOD_STATUS_OPEN);
            AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
            AstTestSupport.seedPeriod(daoProvider, "2026-08", 2026, 8, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long fixedAssetSubjectId = AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
            Long accumDepSubjectId = AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧");
            Long expenseSubjectId = AstTestSupport.seedSubject(daoProvider, "6602", "管理费用");
            Long gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-E2E", "端到端类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12, fixedAssetSubjectId,
                    accumDepSubjectId, expenseSubjectId);
            daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            return seedCapitalization("CAP-E2E-001", categoryId,
                    ErpAstConstants.SOURCE_TYPE_DIRECT_PURCHASE, new BigDecimal("12000"),
                    LocalDate.of(2026, 6, 15), "AST-E2E-001");
        });

        // 1. 资本化建卡
        capBiz.submit(capId, CTX);
        capBiz.approve(capId, CTX);
        ErpAstAsset asset = findAsset("AST-E2E-001");
        assetIdHolder[0] = asset.getId();
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, asset.getStatus());
        assertTrue(Boolean.TRUE.equals(
                daoProvider.daoFor(ErpAstAssetCapitalization.class).getEntityById(capId).getPosted()));

        // 2. 两期折旧（每期 1000）
        scheduleBiz.executeDepreciation(assetIdHolder[0], "2026-07", CTX);
        scheduleBiz.executeDepreciation(assetIdHolder[0], "2026-08", CTX);
        asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(0, nz(asset.getAccumulatedDepreciation()).compareTo(new BigDecimal("2000")), "两期累计折旧=2000");
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(new BigDecimal("10000")), "净值=10000");

        // 3. 处置（账面净值 10000，报废收入 0 → 损失 -10000）
        Long disposalId = ormTemplate.runInSession(session -> seedDisposal("DISP-E2E-001", assetIdHolder[0],
                ErpAstConstants.DISPOSAL_TYPE_SCRAPPED, BigDecimal.ZERO, LocalDate.of(2026, 8, 20)));
        disposalBiz.submit(disposalId, CTX);
        ErpAstDisposal disposal = disposalBiz.approve(disposalId, CTX);
        assertTrue(Boolean.TRUE.equals(disposal.getPosted()), "端到端处置过账成功");
        assertEquals(0, disposal.getGainLoss().compareTo(new BigDecimal("-10000")), "报废损失=-10000");

        ErpAstAsset terminal = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_SCRAPPED, terminal.getStatus(), "端到端资产终态=SCRAPPED");
    }

    private QueryBean singleCodeQuery(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return q;
    }

    private Long seedCapitalization(String code, Long categoryId, int sourceType, BigDecimal originalValue,
                                    LocalDate capitalizationDate, String assetCode) {
        IEntityDao<ErpAstAssetCapitalization> dao = daoProvider.daoFor(ErpAstAssetCapitalization.class);
        ErpAstAssetCapitalization cap = new ErpAstAssetCapitalization();
        cap.setCode(code);
        cap.setOrgId(1L);
        cap.setAssetCode(assetCode);
        cap.setAssetName("资产-" + code);
        cap.setCategoryId(categoryId);
        cap.setCurrencyId(1L);
        cap.setCapitalizationDate(capitalizationDate);
        cap.setOriginalValue(originalValue);
        cap.setSourceType(sourceType);
        cap.setExchangeRate(BigDecimal.ONE);
        cap.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(cap);
        return cap.getId();
    }

    private Long seedDisposal(String code, Long assetId, int disposalType, BigDecimal disposalAmount,
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

    private ErpAstAsset findAsset(String code) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpAstAsset> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpAstDepreciationSchedule> findSchedulesByAsset(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        return dao.findAllByQuery(q);
    }

    private boolean isAllVouchersReversed(String billCode, int businessType) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        if (links.isEmpty()) {
            return false;
        }
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = voucherDao.getEntityById(link.getVoucherId());
            // 正常凭证（非红字 REVERSAL）须 isReversed=true；红字凭证自身 isReversed=true
            if (v != null && !Boolean.TRUE.equals(v.getIsReversed())
                    && (v.getPostingType() == null || v.getPostingType() == 10)) {
                return false;
            }
        }
        return true;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
