package app.erp.qa.service;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.dao.entity.ErpQaInspectionTemplate;
import app.erp.qa.dao.entity.ErpQaInspectionTemplateLine;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-RC-040 关键项否决测试（RC-R1.58）：UC-QA-06 关键项否决（L1 {@code use-cases.md:107-109}
 * 「关键项不合格 → 整体 REJECTED，无论其他项」）+ UC-QA-03 复用断言 + 模板行 isCritical 复制链。
 *
 * <p>覆盖：①关键项 REJECTED + allowConcession=true → REJECTED（否决覆盖让步，核心断言）；
 * ②关键项 ACCEPTED + 非关键项 REJECTED + 让步 → CONDITIONAL（不否决）；③关键项 REJECTED + 无让步 → REJECTED
 * （既有语义保持）；④模板行 isCritical=1 → 质检单行 isCritical=1（复制链，createForBusinessBill 路径）；
 * ⑤手工建行 isCritical 直设 + aggregate 否决。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaCriticalItemVeto extends JunitAutoTestCase {

    @RegisterExtension
    static QaFrozenClockExtension frozenClock = new QaFrozenClockExtension();

    static final Long MATERIAL_ID = 7001L;
    static final Long SUPPLIER_ID = 7201L;
    static final Long WAREHOUSE_ID = 7301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ① 关键项 REJECTED + allowConcession=true → REJECTED（否决覆盖让步）
    @Test
    public void testCriticalRejectedWithConcessionGoesRejected() {
        Long insId = seedInspection("INS-VETO-CR", withLine("长度", "10", "20", 1), withLine("重量", "0", "100", null));
        // 关键项行 5 < min 10 → REJECTED；非关键项行 50 合格；allowConcession=true 但关键项否决覆盖让步
        recordMeasured(insId, true, lineInput(1, "5"), lineInput(2, "50"));

        ErpQaInspection ins = loadInspection(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, ins.getResult(),
                "关键项不合格 + 让步 → 关键项否决强制 REJECTED");
        assertEquals(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED, ins.getApproveStatus(),
                "关键项否决跳过让步审批（不置 APPROVED）");
        assertTrue(Boolean.TRUE.equals(ins.getPosted()), "posted=true");
        assertNotNull(findNcrBySourceCode(ins.getCode()), "REJECTED 自动建 NCR");
    }

    // ② 关键项 ACCEPTED + 非关键项 REJECTED + 让步 → CONDITIONAL（不否决）
    @Test
    public void testCriticalAcceptedNonCriticalRejectedWithConcessionGoesConditional() {
        Long insId = seedInspection("INS-VETO-COND", withLine("长度", "10", "20", 1), withLine("重量", "0", "100", null));
        // 关键项行 15 ∈ [10,20] 合格；非关键项行 200 > 100 不合格 + 让步 → CONDITIONAL
        recordMeasured(insId, true, lineInput(1, "15"), lineInput(2, "200"));

        ErpQaInspection ins = loadInspection(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_CONDITIONAL, ins.getResult(),
                "关键项合格 + 非关键项不合格 + 让步 → CONDITIONAL（不否决）");
        assertEquals(ErpQaConstants.APPROVE_STATUS_APPROVED, ins.getApproveStatus(), "让步须审批 APPROVED");
    }

    // ③ 关键项 REJECTED + 无让步 → REJECTED（既有语义保持）
    @Test
    public void testCriticalRejectedWithoutConcessionGoesRejected() {
        Long insId = seedInspection("INS-VETO-NOCOND", withLine("长度", "10", "20", 1));
        recordMeasured(insId, false, lineInput(1, "5"));

        ErpQaInspection ins = loadInspection(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, ins.getResult(),
                "关键项不合格 + 无让步 → REJECTED");
    }

    // ④ 模板行 isCritical=1 → 质检单行 isCritical=1（复制链，createForBusinessBill 路径）
    @Test
    public void testTemplateCriticalLineCopiedToInspectionLine() {
        seedTemplate("TPL-VETO", MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_INCOMING,
                tplLine("长度", "10", "20", 1), tplLine("重量", "0", "100", null));

        Long insId = createForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-VETO",
                MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_INCOMING);

        List<ErpQaInspectionLine> lines = loadLines(insId);
        assertEquals(2, lines.size(), "模板 2 行复制到质检单");
        ErpQaInspectionLine critical = lines.get(0);
        ErpQaInspectionLine normal = lines.get(1);
        assertEquals(Integer.valueOf(1), critical.getIsCritical(), "模板关键项行 isCritical=1 复制到质检单行");
        assertNull(normal.getIsCritical(), "模板非关键项行 isCritical null 复制到质检单行");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_PENDING, critical.getResult(), "复制行结果 PENDING");
    }

    // ⑤ 手工建行 isCritical 直设 + aggregate 否决
    @Test
    public void testManualLineIsCriticalDirectSetVetoApplied() {
        Long insId = seedInspection("INS-VETO-MANUAL", withLine("长度", "10", "20", null));
        // 手工直设 isCritical=1（无模板路径，CRUD 直设）+ 实测 5 < min 10 不合格 + 让步
        ormTemplate.runInSession(() -> {
            ErpQaInspectionLine line = daoProvider.daoFor(ErpQaInspectionLine.class)
                    .getEntityById(insId * 100 + 1);
            line.setIsCritical(1);
            daoProvider.daoFor(ErpQaInspectionLine.class).updateEntity(line);
        });

        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__recordResult",
                recordResultArgs(insId, List.of(lineInput(1, "5")), true));
        assertEquals(0, resp.getStatus(), "recordResult 应成功: " + resp);

        ErpQaInspection ins = loadInspection(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED, ins.getResult(),
                "手工直设关键项行不合格 + 让步 → 否决强制 REJECTED");
    }

    // ---------- helpers ----------

    private ErpQaInspection loadInspection(Long insId) {
        return daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId);
    }

    private ErpQaNonConformance findNcrBySourceCode(String sourceCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceCode", sourceCode));
        q.setLimit(1);
        List<ErpQaNonConformance> list = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void recordMeasured(Long insId, boolean allowConcession, Map<String, Object>... lines) {
        List<Map<String, Object>> lineList = new ArrayList<>();
        for (Map<String, Object> l : lines) {
            lineList.add(l);
        }
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__recordResult", recordResultArgs(insId, lineList, allowConcession));
        assertEquals(0, resp.getStatus(), "recordResult 应成功，但返回: " + resp);
    }

    private Map<String, Object> lineInput(int lineNo, String measuredValue) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lineNo", lineNo);
        m.put("measuredValue", measuredValue);
        return m;
    }

    private ApiRequest<?> recordResultArgs(Long insId, List<Map<String, Object>> lines, boolean allowConcession) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("inspectionId", insId);
        args.put("lineResults", lines);
        args.put("allowConcession", allowConcession);
        return ApiRequest.build(args);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long seedInspection(String code, LineSpec... lines) {
        Long id = 6100L + (long) (Math.abs(code.hashCode()) % 900);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
            ErpQaInspection ins = new ErpQaInspection();
            ins.orm_propValueByName("id", id);
            ins.setCode(code);
            ins.setInspectionType(ErpQaConstants.INSPECTION_TYPE_INCOMING);
            ins.setMaterialId(MATERIAL_ID);
            ins.setResult(ErpQaConstants.INSPECTION_RESULT_PENDING);
            ins.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            ins.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
            ins.setPosted(Boolean.FALSE);
            ins.setInspectionDate(CoreMetrics.currentDate());
            ins.setBusinessDate(CoreMetrics.currentDate());
            ins.setRelatedBillType("ERP_PUR_RECEIPT");
            ins.setRelatedBillCode("BILL-" + code.replace("INS-", ""));
            dao.saveEntity(ins);

            IEntityDao<ErpQaInspectionLine> lineDao = daoProvider.daoFor(ErpQaInspectionLine.class);
            int lineNo = 1;
            for (LineSpec spec : lines) {
                ErpQaInspectionLine line = new ErpQaInspectionLine();
                line.orm_propValueByName("id", id * 100 + lineNo);
                line.setInspectionId(id);
                line.setLineNo(lineNo);
                line.setParameterName(spec.parameterName == null ? "参数" + lineNo : spec.parameterName);
                line.setSpecMin(spec.specMin);
                line.setSpecMax(spec.specMax);
                line.setIsCritical(spec.isCritical);
                line.setResult(ErpQaConstants.INSPECTION_RESULT_PENDING);
                lineDao.saveEntity(line);
                lineNo++;
            }
        });
        return id;
    }

    private Long seedTemplate(String code, Long materialId, String inspectionType, TplLineSpec... lines) {
        Long id = 5100L + (long) (Math.abs(code.hashCode()) % 900);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspectionTemplate> dao = daoProvider.daoFor(ErpQaInspectionTemplate.class);
            ErpQaInspectionTemplate t = new ErpQaInspectionTemplate();
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setName(code);
            t.setInspectionType(inspectionType);
            t.setMaterialId(materialId);
            t.setIsActive(1);
            dao.saveEntity(t);

            IEntityDao<ErpQaInspectionTemplateLine> lineDao = daoProvider.daoFor(ErpQaInspectionTemplateLine.class);
            int lineNo = 1;
            for (TplLineSpec spec : lines) {
                ErpQaInspectionTemplateLine tl = new ErpQaInspectionTemplateLine();
                tl.orm_propValueByName("id", id * 100 + lineNo);
                tl.setTemplateId(id);
                tl.setLineNo(lineNo);
                tl.setParameterName(spec.parameterName);
                tl.setSpecMin(spec.specMin);
                tl.setSpecMax(spec.specMax);
                tl.setIsRequired(1);
                tl.setIsCritical(spec.isCritical);
                lineDao.saveEntity(tl);
                lineNo++;
            }
        });
        return id;
    }

    private Long createForBusinessBill(String billType, String billCode, Long materialId, String inspectionType) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("billType", billType);
        args.put("billCode", billCode);
        args.put("materialId", materialId);
        args.put("inspectionType", inspectionType);
        args.put("lotQuantity", 100);
        args.put("supplierId", SUPPLIER_ID);
        args.put("warehouseId", WAREHOUSE_ID);
        args.put("batchNo", "B01");
        ApiResponse<?> resp = rpc(mutation, "ErpQaInspection__createForBusinessBill", ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), "createForBusinessBill 应成功: " + resp);
        Object idVal = ((Map<?, ?>) resp.getData()).get("id");
        return idVal instanceof Number ? ((Number) idVal).longValue() : Long.valueOf(String.valueOf(idVal));
    }

    private List<ErpQaInspectionLine> loadLines(Long insId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("inspectionId", insId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpQaInspectionLine.class).findAllByQuery(q);
    }

    private LineSpec withLine(String parameterName, String specMin, String specMax, Integer isCritical) {
        return new LineSpec(parameterName, toBigDecimal(specMin), toBigDecimal(specMax), isCritical);
    }

    private TplLineSpec tplLine(String parameterName, String specMin, String specMax, Integer isCritical) {
        return new TplLineSpec(parameterName, toBigDecimal(specMin), toBigDecimal(specMax), isCritical);
    }

    private static BigDecimal toBigDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static final class LineSpec {
        final String parameterName;
        final BigDecimal specMin;
        final BigDecimal specMax;
        final Integer isCritical;

        LineSpec(String parameterName, BigDecimal specMin, BigDecimal specMax, Integer isCritical) {
            this.parameterName = parameterName;
            this.specMin = specMin;
            this.specMax = specMax;
            this.isCritical = isCritical;
        }
    }

    private static final class TplLineSpec {
        final String parameterName;
        final BigDecimal specMin;
        final BigDecimal specMax;
        final Integer isCritical;

        TplLineSpec(String parameterName, BigDecimal specMin, BigDecimal specMax, Integer isCritical) {
            this.parameterName = parameterName;
            this.specMin = specMin;
            this.specMax = specMax;
            this.isCritical = isCritical;
        }
    }
}
