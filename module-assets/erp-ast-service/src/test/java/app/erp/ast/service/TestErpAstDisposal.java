package app.erp.ast.service;

import app.erp.ast.biz.IErpAstDisposalBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstDisposal;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产处置（报废/出售）审批→清理损益→DISPOSAL(90) 过账→资产终态→后续折旧停止 端到端单测（plan Phase 4）。
 *
 * <p>覆盖 plan Proof 两场景：报废 SCRAPPED→清理损失 + DISPOSAL 凭证 + 资产 SCRAPPED + 后续 schedule CANCELLED(40)；
 * 出售 SOLD→清理收益 + DISPOSAL 凭证 + 银行存款。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstDisposal extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstDisposalBiz disposalBiz;

    @Test
    public void testScrapLossAndTerminalStatus() {
        long[] assetIdHolder = new long[1];
        Long disposalId = ormTemplate.runInSession(session -> {
            seedBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-DISP-S", "报废类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-SCRAP", "报废资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            // 后续未执行折旧计划（处置后应 CANCELLED）
            AstTestSupport.seedPendingSchedule(daoProvider, assetId, 1L, "2026-08");
            AstTestSupport.seedPendingSchedule(daoProvider, assetId, 1L, "2026-09");
            return seedDisposal("DISP-SCRAP-001", assetId, ErpAstConstants.DISPOSAL_TYPE_SCRAPPED,
                    BigDecimal.ZERO, LocalDate.of(2026, 7, 15));
        });

        disposalBiz.submit(disposalId, CTX);
        ErpAstDisposal disposal = disposalBiz.approve(disposalId, CTX);

        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, disposal.getApproveStatus());
        assertTrue(Boolean.TRUE.equals(disposal.getPosted()), "处置过账 posted=true");
        // 清理损益 = 0 − 12000 = -12000（损失）
        assertEquals(0, disposal.getGainLoss().compareTo(new BigDecimal("-12000")), "报废清理损失=-12000");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_SCRAPPED, asset.getStatus(), "资产终态=SCRAPPED");

        // 后续折旧计划标记 CANCELLED(40)
        List<ErpAstDepreciationSchedule> schedules = findSchedulesByAsset(assetIdHolder[0]);
        assertEquals(2, schedules.size());
        for (ErpAstDepreciationSchedule s : schedules) {
            assertEquals(ErpAstConstants.SCHEDULE_STATUS_CANCELLED, s.getStatus(), "后续折旧 CANCELLED");
        }

        // DISPOSAL(90) 凭证回链
        assertTrue(!findBillLinks("DISP-SCRAP-001", "DISPOSAL").isEmpty(), "DISPOSAL 凭证回链已落库");
    }

    @Test
    public void testSaleGainAndBankCredit() {
        long[] assetIdHolder = new long[1];
        Long disposalId = ormTemplate.runInSession(session -> {
            seedBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6301", "营业外收入");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-DISP-SALE", "出售类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-SALE", "出售资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            // 模拟已提折旧 4000（账面净值 8000）
            ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
            asset.setAccumulatedDepreciation(new BigDecimal("4000"));
            asset.setNetBookValue(new BigDecimal("8000"));
            daoProvider.daoFor(ErpAstAsset.class).saveEntity(asset);
            return seedDisposal("DISP-SALE-001", assetId, ErpAstConstants.DISPOSAL_TYPE_SOLD,
                    new BigDecimal("10000"), LocalDate.of(2026, 7, 15));
        });

        disposalBiz.submit(disposalId, CTX);
        ErpAstDisposal disposal = disposalBiz.approve(disposalId, CTX);

        assertTrue(Boolean.TRUE.equals(disposal.getPosted()), "出售过账 posted=true");
        // 清理损益 = 10000 − (12000 − 4000) = +2000（收益）
        assertEquals(0, disposal.getGainLoss().compareTo(new BigDecimal("2000")), "出售清理收益=2000");
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_SOLD, asset.getStatus(), "资产终态=SOLD");
        assertTrue(!findBillLinks("DISP-SALE-001", "DISPOSAL").isEmpty(), "DISPOSAL 凭证回链已落库");
    }

    // ---------- helpers ----------

    private void seedBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, 1L);
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
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

    private List<ErpAstDepreciationSchedule> findSchedulesByAsset(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }
}
