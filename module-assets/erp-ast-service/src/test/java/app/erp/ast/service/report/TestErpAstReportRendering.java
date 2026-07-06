package app.erp.ast.service.report;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstDisposal;
import app.erp.ast.service.ErpAstConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产域报表渲染端到端测试（plan 2026-07-06-1815-1 Phase 1 Proof）。
 *
 * <p>覆盖两张资产报表（资产折旧明细 / 资产处置明细）的 {@code renderHtml}/{@code download(xlsx|pdf)} 渲染管线、
 * 数据集口径断言、空数据集不报错、以及路径注入防护（非法 reportName 抛 {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final Long ORG_ID = 1L;
    static final Long CURRENCY_ID = 1L;

    @Inject
    ErpAstReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== Phase 1: 资产折旧明细报表 =====================

    @Test
    public void testAssetDepreciationDetailRenderHtml() {
        seedDepreciationBaseline();
        String html = reportBiz.renderHtml("asset-depreciation-detail", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("AST-DEP-1"), "renderHtml 含资产编码");
    }

    @Test
    public void testAssetDepreciationDetailDownloadXlsxAndPdf() {
        seedDepreciationBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("asset-depreciation-detail", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertNotNull(content, "download 内容非空: " + renderType);
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testAssetDepreciationDetailDataset() {
        seedDepreciationBaseline();
        List<Map<String, Object>> ds = reportBiz.buildAssetDepreciationDetailDataset(null, null, null);
        assertFalse(ds.isEmpty(), "折旧明细数据集非空");
        Map<String, Object> row = ds.get(0);
        // originalValue=12000, accumulatedDepreciation=2000, netBookValue=10000, periodDepreciation=1000
        assertEquals(0, bd("12000").compareTo(toBd(row.get("originalValue"))), "originalValue=12000");
        assertEquals(0, bd("2000").compareTo(toBd(row.get("accumulatedDepreciation"))), "accumulatedDepreciation=2000");
        assertEquals(0, bd("10000").compareTo(toBd(row.get("netBookValue"))), "netBookValue=10000");
        assertEquals(0, bd("1000").compareTo(toBd(row.get("periodDepreciation"))), "periodDepreciation=1000");
    }

    @Test
    public void testAssetDepreciationDetailEmptyDatasetNoError() {
        // 无数据：不报错，返回空列表
        List<Map<String, Object>> ds = reportBiz.buildAssetDepreciationDetailDataset(null, null, null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无资产记录 → 空列表");
    }

    // ===================== Phase 1: 资产处置明细报表 =====================

    @Test
    public void testAssetDisposalDetailRenderHtml() {
        seedDisposalBaseline();
        String html = reportBiz.renderHtml("asset-disposal-detail", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("DISP-RPT-1"), "renderHtml 含处置单编码");
    }

    @Test
    public void testAssetDisposalDetailDownloadXlsxAndPdf() {
        seedDisposalBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("asset-disposal-detail", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testAssetDisposalDetailDataset() {
        seedDisposalBaseline();
        List<Map<String, Object>> ds = reportBiz.buildAssetDisposalDetailDataset(null, null);
        assertFalse(ds.isEmpty(), "处置明细数据集非空");
        Map<String, Object> row = ds.get(0);
        // disposalAmount=8000, gainLoss=-2000（清理损失）
        assertEquals(0, bd("8000").compareTo(toBd(row.get("disposalAmount"))), "disposalAmount=8000");
        assertEquals(0, bd("-2000").compareTo(toBd(row.get("gainLoss"))), "gainLoss=-2000");
        assertEquals(ErpAstConstants.DISPOSAL_TYPE_SOLD, row.get("disposalType"), "disposalType=SOLD");
    }

    @Test
    public void testAssetDisposalDetailEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildAssetDisposalDetailDataset(null, null);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无处置记录 → 空列表");
    }

    // ===================== 路径注入防护 =====================

    @Test
    public void testPathInjectionRejected() {
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml("../etc/passwd", null, CTX),
                "非法 reportName（含 ../）抛 NopException");
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml(null, null, CTX),
                "空 reportName 抛 NopException");
        seedDepreciationBaseline();
        assertThrows(NopException.class,
                () -> reportBiz.download("asset-depreciation-detail", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== 数据准备 =====================

    private void seedDepreciationBaseline() {
        ormTemplate.runInSession(() -> {
            Long categoryId = seedCategory("CAT-RPT-DEP", "折旧报表类别");
            Long assetId = 2001L;
            seedAsset(assetId, "AST-DEP-1", "折旧报表资产", categoryId,
                    bd("12000"), bd("2000"), bd("10000"),
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            // 本期已执行折旧计划：actualAmount=1000
            seedSchedule(9001L, assetId, "2026-07", bd("1000"), LocalDate.of(2026, 7, 31));
        });
    }

    private void seedDisposalBaseline() {
        ormTemplate.runInSession(() -> {
            Long categoryId = seedCategory("CAT-RPT-DISP", "处置报表类别");
            Long assetId = 2002L;
            seedAsset(assetId, "AST-DISP-1", "处置报表资产", categoryId,
                    bd("12000"), bd("2000"), bd("10000"),
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            // 处置单：disposalAmount=8000, gainLoss=8000-(12000-2000)=-2000
            IEntityDao<ErpAstDisposal> dao = daoProvider.daoFor(ErpAstDisposal.class);
            ErpAstDisposal d = new ErpAstDisposal();
            d.orm_propValueByName("id", 2011L);
            d.setCode("DISP-RPT-1");
            d.setOrgId(ORG_ID);
            d.setAssetId(assetId);
            d.orm_propValueByName("disposalType", ErpAstConstants.DISPOSAL_TYPE_SOLD);
            d.setDisposalAmount(bd("8000"));
            d.setCurrencyId(CURRENCY_ID);
            d.setBusinessDate(LocalDate.of(2026, 7, 20));
            d.setGainLoss(bd("-2000"));
            d.orm_propValueByName("reason", "出售清理");
            d.orm_propValueByName("docStatus", "APPROVED");
            d.orm_propValueByName("approveStatus", ErpAstConstants.APPROVE_STATUS_APPROVED);
            dao.saveEntity(d);
        });
    }

    private Long seedCategory(String code, String name) {
        IEntityDao<ErpAstAssetCategory> dao = daoProvider.daoFor(ErpAstAssetCategory.class);
        ErpAstAssetCategory c = new ErpAstAssetCategory();
        long id = 3000L + (long) Math.abs(code.hashCode() % 1000);
        c.orm_propValueByName("id", id);
        c.setCode(code);
        c.setName(name);
        dao.saveEntity(c);
        return id;
    }

    private void seedAsset(Long id, String code, String name, Long categoryId,
                           BigDecimal originalValue, BigDecimal accumulatedDep,
                           BigDecimal netBookValue, String status) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset a = new ErpAstAsset();
        a.orm_propValueByName("id", id);
        a.setCode(code);
        a.setName(name);
        a.setOrgId(ORG_ID);
        a.setCategoryId(categoryId);
        a.setAcquisitionDate(LocalDate.of(2026, 1, 1));
        a.setCurrencyId(CURRENCY_ID);
        a.setOriginalValue(originalValue);
        a.setAccumulatedDepreciation(accumulatedDep);
        a.setNetBookValue(netBookValue);
        a.orm_propValueByName("depreciationMethod", ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE);
        a.orm_propValueByName("usefulLifeMonths", 12);
        a.setStatus(status);
        dao.saveEntity(a);
    }

    private void seedSchedule(Long id, Long assetId, String period, BigDecimal actualAmount, LocalDate businessDate) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        ErpAstDepreciationSchedule s = new ErpAstDepreciationSchedule();
        s.orm_propValueByName("id", id);
        s.setAssetId(assetId);
        s.setOrgId(ORG_ID);
        s.orm_propValueByName("period", period);
        s.setPlannedAmount(actualAmount);
        s.setActualAmount(actualAmount);
        s.setBusinessDate(businessDate);
        s.setCurrencyId(CURRENCY_ID);
        s.orm_propValueByName("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED);
        dao.saveEntity(s);
    }

    // ===================== helpers =====================

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(String.valueOf(v));
    }
}
