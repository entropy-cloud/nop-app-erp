package app.erp.mfg.service.report;

import app.erp.mfg.biz.IErpMfgCrpLoadBiz;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgForecast;
import app.erp.mfg.dao.entity.ErpMfgForecastLine;
import app.erp.mfg.dao.entity.ErpMfgRouting;
import app.erp.mfg.dao.entity.ErpMfgRoutingOperation;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCalendar;
import app.erp.mfg.dao.entity.ErpMfgWorkcenterCapacity;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 制造运营报表渲染端到端测试（plan 2026-07-06-0935-2 Phase 1/2/3 Proof）。
 *
 * <p>覆盖三张制造运营报表（CRP 负荷 / 生产差异 / 预测差异）的 {@code renderHtml}/{@code download(xlsx|pdf)}
 * 渲染管线、数据集口径断言、以及路径注入防护（非法 reportName 抛 {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final Long ORG_ID = 1L;
    static final Long UOM_ID = 1L;
    static final Long MAT_P = 7001L;
    static final Long WC1 = 7101L;
    static final Long ROUTING_1 = 7102L;

    @Inject
    ErpMfgReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpMfgCrpLoadBiz crpLoadBiz;

    // ===================== Phase 1: CRP 负荷报表 =====================

    @Test
    public void testCrpLoadReportRenderHtml() {
        seedCrpBaseline();
        Map<String, Object> data = new HashMap<>();
        data.put("startDate", LocalDate.of(2026, 7, 1));
        data.put("endDate", LocalDate.of(2026, 7, 1));
        String html = reportBiz.renderHtml("crp-load-report", data, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
    }

    @Test
    public void testCrpLoadReportDownloadXlsxAndPdf() {
        seedCrpBaseline();
        Map<String, Object> data = new HashMap<>();
        data.put("startDate", LocalDate.of(2026, 7, 1));
        data.put("endDate", LocalDate.of(2026, 7, 1));
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("crp-load-report", renderType, data, CTX);
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
    public void testCrpLoadDataset() {
        seedCrpBaseline();
        List<Map<String, Object>> ds = reportBiz.buildCrpLoadDataset(WC1,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1), CTX);
        assertFalse(ds.isEmpty(), "CRP 负荷数据集非空");
        Map<String, Object> row = ds.get(0);
        // loadHours=9h, setupHours=1h, capacityHours=9×0.5=4.5h, loadRate=2.0, overloaded=true
        assertEquals(0, bd("9").compareTo(toBd(row.get("loadHours"))), "loadHours=9h");
        assertEquals(Boolean.TRUE, row.get("overloaded"), "loadRate>1.0 → overloaded");
    }

    @Test
    public void testCrpLoadEmptyDatasetNoError() {
        // 无数据：不报错，返回空（deriveCrpWindow 返回 null）
        List<Map<String, Object>> ds = reportBiz.buildCrpLoadDataset(null, null, null, CTX);
        assertNotNull(ds, "空数据集不报错");
    }

    // ===================== Phase 2: 生产差异报表 =====================

    @Test
    public void testProductionVarianceReportRenderHtml() {
        seedVarianceBaseline();
        String html = reportBiz.renderHtml("production-variance-report", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
    }

    @Test
    public void testProductionVarianceReportDownloadXlsxAndPdf() {
        seedVarianceBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("production-variance-report", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testProductionVarianceDataset() {
        seedVarianceBaseline();
        List<Map<String, Object>> ds = reportBiz.buildProductionVarianceDataset(null, null, null);
        assertFalse(ds.isEmpty(), "差异数据集非空");
        boolean hasMaterialUsage = false;
        BigDecimal varianceSum = BigDecimal.ZERO;
        for (Map<String, Object> r : ds) {
            if (ErpMfgConstants.VARIANCE_TYPE_MATERIAL_USAGE.equals(r.get("varianceType"))) {
                hasMaterialUsage = true;
            }
            varianceSum = varianceSum.add(toBd(r.get("varianceAmount")));
        }
        assertTrue(hasMaterialUsage, "差异数据集覆盖 MATERIAL_USAGE 类型");
        assertTrue(varianceSum.signum() != 0, "差异金额合计非零");
    }

    @Test
    public void testProductionVarianceEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildProductionVarianceDataset(null, null, null);
        assertNotNull(ds, "空差异数据集不报错");
        assertTrue(ds.isEmpty(), "无差异记录 → 空列表");
    }

    // ===================== Phase 3: 预测差异报表 =====================

    @Test
    public void testForecastVarianceReportRenderHtml() {
        seedForecastBaseline();
        String html = reportBiz.renderHtml("forecast-variance-report", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
    }

    @Test
    public void testForecastVarianceReportDownloadXlsxAndPdf() {
        seedForecastBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("forecast-variance-report", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testForecastVarianceDataset() {
        seedForecastBaseline();
        List<Map<String, Object>> ds = reportBiz.buildForecastVarianceDataset(null, null, null);
        assertFalse(ds.isEmpty(), "预测差异数据集非空");
        Map<String, Object> row = ds.get(0);
        // forecastQty=100, actualQty=80 → variance=-20, ratio=-0.2
        assertEquals(0, bd("100").compareTo(toBd(row.get("forecastQty"))), "forecastQty=100");
        assertEquals(0, bd("80").compareTo(toBd(row.get("actualQty"))), "actualQty=80");
        assertEquals(0, bd("-20").compareTo(toBd(row.get("variance"))), "variance=actual-forecast=-20");
        assertTrue(toBd(row.get("varianceRatio")).signum() != 0, "varianceRatio 非零");
    }

    @Test
    public void testForecastVarianceEmptyDatasetNoError() {
        List<Map<String, Object>> ds = reportBiz.buildForecastVarianceDataset(null, null, null);
        assertNotNull(ds, "空预测差异数据集不报错");
        assertTrue(ds.isEmpty(), "无预测/实际 → 空列表");
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
        seedCrpBaseline();
        assertThrows(NopException.class,
                () -> reportBiz.download("crp-load-report", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== CRP 数据准备（复用 TestErpMfgCrpLoad 范式） =====================

    private void seedCrpBaseline() {
        seedMaterial(MAT_P);
        seedWorkcenter(WC1, "WC-RPT");
        seedCalendar(WC1, "08:00", "17:00", ErpMfgConstants.WORK_DATE_PATTERN_ALL_WEEK);
        seedCapacity(WC1, MAT_P, bd("10"), bd("0.5"));
        seedRouting(ROUTING_1, "R-RPT");
        seedRoutingOperation(ROUTING_1, WC1, bd("540"), bd("60"));
        seedWorkOrder("WO-RPT-1", ROUTING_1,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1),
                ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, MAT_P);
        calculateLoad(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1));
    }

    private void calculateLoad(LocalDate from, LocalDate to) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("periodFrom", from);
        args.put("periodTo", to);
        ApiResponse<?> resp = execute(mutation, "ErpMfgCrpLoad__calculateLoad", args);
        assertEquals(0, resp.getStatus(), "calculateLoad 应成功: " + resp);
    }

    // ===================== 生产差异数据准备 =====================

    private void seedVarianceBaseline() {
        Long woId = 8301L;
        seedWorkOrderById(woId, "WO-VAR-1", LocalDate.of(2026, 7, 10), MAT_P);
        saveVarianceLine(woId, 10, ErpMfgConstants.VARIANCE_TYPE_MATERIAL_USAGE, ErpMfgConstants.COST_ELEMENT_MATERIAL,
                bd("1000"), bd("1100"), bd("-100"), LocalDate.of(2026, 7, 10));
        saveVarianceLine(woId, 20, ErpMfgConstants.VARIANCE_TYPE_LABOR_EFFICIENCY, ErpMfgConstants.COST_ELEMENT_LABOR,
                bd("500"), bd("450"), bd("50"), LocalDate.of(2026, 7, 10));
    }

    private void saveVarianceLine(Long woId, int lineNo, String varianceType, String costElement,
                                  BigDecimal standard, BigDecimal actual, BigDecimal variance,
                                  LocalDate businessDate) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgCostVariance> dao = daoProvider.daoFor(ErpMfgCostVariance.class);
            ErpMfgCostVariance v = new ErpMfgCostVariance();
            v.orm_propValueByName("id", woId * 1000 + lineNo);
            v.setWorkOrderId(woId);
            v.orm_propValueByName("lineNo", lineNo);
            v.setVarianceType(varianceType);
            v.setCostElement(costElement);
            v.setStandardAmount(standard);
            v.setActualAmount(actual);
            v.setVarianceAmount(variance);
            v.setBusinessDate(businessDate);
            dao.saveEntity(v);
        });
    }

    private void seedWorkOrderById(Long id, String code, LocalDate date, Long productId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(productId);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(date);
            wo.setPlannedStartDate(date);
            wo.setPlannedEndDate(date);
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
            dao.saveEntity(wo);
        });
    }

    // ===================== 预测差异数据准备 =====================

    private void seedForecastBaseline() {
        seedMaterial(MAT_P);
        // APPROVED 预测：forecastQty=100
        Long headId = 9301L;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgForecast> headDao = daoProvider.daoFor(ErpMfgForecast.class);
            ErpMfgForecast head = new ErpMfgForecast();
            head.orm_propValueByName("id", headId);
            head.setCode("FC-RPT-1");
            head.setOrgId(ORG_ID);
            head.setPlanName("预测-RPT");
            head.setPeriodFrom(LocalDate.of(2026, 7, 1));
            head.setPeriodTo(LocalDate.of(2026, 7, 31));
            head.setStatus(ErpMfgConstants.FORECAST_STATUS_APPROVED);
            headDao.saveEntity(head);

            IEntityDao<ErpMfgForecastLine> lineDao = daoProvider.daoFor(ErpMfgForecastLine.class);
            ErpMfgForecastLine line = new ErpMfgForecastLine();
            line.orm_propValueByName("id", headId * 1000 + 10);
            line.setForecastId(headId);
            line.orm_propValueByName("lineNo", 10);
            line.setMaterialId(MAT_P);
            line.setUoMId(UOM_ID);
            line.setPeriodStart(LocalDate.of(2026, 7, 1));
            line.setPeriodEnd(LocalDate.of(2026, 7, 31));
            line.setForecastQty(bd("100"));
            lineDao.saveEntity(line);
        });
        // 实际完工工单：completedQuantity=80
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", 8401L);
            wo.setCode("WO-FC-ACT");
            wo.setProductId(MAT_P);
            wo.setPlannedQuantity(bd("100"));
            wo.setCompletedQuantity(bd("80"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 15));
            wo.setPlannedStartDate(LocalDate.of(2026, 7, 1));
            wo.setPlannedEndDate(LocalDate.of(2026, 7, 31));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
            dao.saveEntity(wo);
        });
    }

    // ===================== 通用 helpers =====================

    private ApiResponse<?> execute(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void seedMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            dao.saveEntity(m);
        });
    }

    private void seedWorkcenter(Long id, String code) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenter> dao = daoProvider.daoFor(ErpMfgWorkcenter.class);
            ErpMfgWorkcenter wc = new ErpMfgWorkcenter();
            wc.orm_propValueByName("id", id);
            wc.setCode(code);
            wc.setName("WC " + code);
            dao.saveEntity(wc);
        });
    }

    private void seedCalendar(Long workcenterId, String start, String end, String pattern) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenterCalendar> dao = daoProvider.daoFor(ErpMfgWorkcenterCalendar.class);
            ErpMfgWorkcenterCalendar c = new ErpMfgWorkcenterCalendar();
            c.orm_propValueByName("id", 60000L + workcenterId);
            c.setWorkcenterId(workcenterId);
            c.setCalendarName("CAL-" + workcenterId);
            c.orm_propValueByName("shiftType", ErpMfgConstants.SHIFT_TYPE_ONE_SHIFT);
            c.orm_propValueByName("workDatePattern", pattern);
            c.setStartTime(start);
            c.setEndTime(end);
            c.setIsActive(Boolean.TRUE);
            dao.saveEntity(c);
        });
    }

    private void seedCapacity(Long workcenterId, Long materialId, BigDecimal capPerHour, BigDecimal efficiency) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenterCapacity> dao = daoProvider.daoFor(ErpMfgWorkcenterCapacity.class);
            ErpMfgWorkcenterCapacity cap = new ErpMfgWorkcenterCapacity();
            cap.orm_propValueByName("id", 61000L + workcenterId);
            cap.setWorkcenterId(workcenterId);
            cap.setMaterialId(materialId);
            cap.setCapacityPerHour(capPerHour);
            cap.setEfficiencyFactor(efficiency);
            cap.setIsActive(Boolean.TRUE);
            dao.saveEntity(cap);
        });
    }

    private void seedRouting(Long id, String code) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgRouting> dao = daoProvider.daoFor(ErpMfgRouting.class);
            ErpMfgRouting r = new ErpMfgRouting();
            r.orm_propValueByName("id", id);
            r.setCode(code);
            r.setIsActive(Boolean.TRUE);
            dao.saveEntity(r);
        });
    }

    private void seedRoutingOperation(Long routingId, Long workcenterId, BigDecimal standardTime, BigDecimal setupTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgRoutingOperation> dao = daoProvider.daoFor(ErpMfgRoutingOperation.class);
            ErpMfgRoutingOperation op = new ErpMfgRoutingOperation();
            op.orm_propValueByName("id", 62000L + routingId + workcenterId);
            op.setRoutingId(routingId);
            op.orm_propValueByName("lineNo", 10);
            op.setWorkcenterId(workcenterId);
            op.setStandardTime(standardTime);
            op.setSetupTime(setupTime);
            dao.saveEntity(op);
        });
    }

    private void seedWorkOrder(String code, Long routingId, LocalDate start, LocalDate end, String docStatus, Long productId) {
        Long id = 8000L + (long) Math.abs(code.hashCode() % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(productId);
            wo.setRoutingId(routingId);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(start);
            wo.setPlannedStartDate(start);
            wo.setPlannedEndDate(end);
            wo.setDocStatus(docStatus);
            dao.saveEntity(wo);
        });
    }

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
