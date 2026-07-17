package app.erp.qa.service;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.dao.entity.ErpQaInspectionTemplate;
import app.erp.qa.dao.entity.ErpQaInspectionTemplateLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 测试：业务触发 createForBusinessBill（采购入库→INCOMING / 销售出库→OUTGOING / 工单完工→FINAL）
 * + 模板匹配（materialId×inspectionType → 复制模板行；无匹配走全局默认；仍无则无行）
 * + 强制质检阻塞（mandatory-inspection-bill-types 配置门控，PENDING 时阻塞，ACCEPTED 后放行）。
 *
 * <p>覆盖 {@code docs/design/quality/state-machine.md §适用对象一 异常路径}（强制质检阻塞 / 模板缺失）。
 * 业务域 confirm→createForBusinessBill 的 API 契约经本测试验证（业务域 Processors 调同一 I*Biz 入口）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaInspectionTrigger extends JunitAutoTestCase {

    @RegisterExtension
    static QaFrozenClockExtension frozenClock = new QaFrozenClockExtension();

    static final Long MATERIAL_ID = 7101L;
    static final Long SUPPLIER_ID = 7201L;
    static final Long WAREHOUSE_ID = 7301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @AfterEach
    void clearConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_MANDATORY_INSPECTION_BILL_TYPES, "");
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_DEFAULT_INSPECTION_TEMPLATE, "");
    }

    @Test
    public void testPurchaseReceiptTriggerGeneratesIncomingWithTemplateLines() {
        Long templateId = seedTemplate("TPL-INCOMING", MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_INCOMING,
                tplLine("长度", "10", "20"), tplLine("重量", "0", "100"));

        Long insId = createForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-1",
                MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_INCOMING);

        ErpQaInspection ins = daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId);
        assertEquals(ErpQaConstants.INSPECTION_RESULT_PENDING, ins.getResult(), "生成质检单结果=PENDING");
        assertEquals(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, ins.getRelatedBillType());
        assertEquals("RCV-1", ins.getRelatedBillCode());
        assertEquals(templateId, ins.getTemplateId(), "模板匹配写入 templateId");
        assertEquals(2, loadLines(insId).size(), "模板行复制到质检单行（2 行）");
    }

    @Test
    public void testSalesOutgoingTrigger() {
        seedTemplate("TPL-OUT", MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_OUTGOING, tplLine("外观", null, null));

        Long insId = createForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_SAL_DELIVERY, "DLV-1",
                MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_OUTGOING);

        ErpQaInspection ins = daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId);
        assertEquals(ErpQaConstants.INSPECTION_TYPE_OUTGOING, ins.getInspectionType(), "销售出库→OUTGOING");
        assertEquals(1, loadLines(insId).size());
    }

    @Test
    public void testWorkOrderFinalTrigger() {
        seedTemplate("TPL-FINAL", MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_FINAL, tplLine("尺寸", "0", "50"));

        Long insId = createForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-1",
                MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_FINAL);

        ErpQaInspection ins = daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId);
        assertEquals(ErpQaConstants.INSPECTION_TYPE_FINAL, ins.getInspectionType(), "工单完工→FINAL");
    }

    @Test
    public void testNoTemplateFallsToGlobalDefault() {
        // 无物料×类型匹配模板 → 走全局默认模板
        Long defaultTplId = seedTemplate("TPL-DEFAULT", 9999L, ErpQaConstants.INSPECTION_TYPE_INCOMING,
                tplLine("默认项", "0", "10"));
        AppConfig.getConfigProvider().assignConfigValue(
                ErpQaConstants.CONFIG_DEFAULT_INSPECTION_TEMPLATE, String.valueOf(defaultTplId));

        Long insId = createForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-DEFAULT",
                MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_INCOMING);

        ErpQaInspection ins = daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId);
        assertEquals(defaultTplId, ins.getTemplateId(), "无匹配走全局默认模板");
        assertEquals(1, loadLines(insId).size(), "默认模板行复制");
    }

    @Test
    public void testNoTemplateNoLinesManualEntry() {
        // 无匹配 + 无全局默认 → 质检单无行（人工补录）
        Long insId = createForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-NOTPL",
                MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_INCOMING);
        assertTrue(loadLines(insId).isEmpty(), "无模板且无默认 → 质检单无行");
    }

    @Test
    public void testMandatoryInspectionBlockedWhenPendingClearedWhenAccepted() {
        setConfig(ErpQaConstants.CONFIG_MANDATORY_INSPECTION_BILL_TYPES,
                ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT);
        Long insId = createForBusinessBill(ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT, "RCV-MAND",
                MATERIAL_ID, ErpQaConstants.INSPECTION_TYPE_INCOMING);

        // PENDING：强制质检阻塞 → isInspectionCleared=false（业务域 confirm 应拒绝）
        org.junit.jupiter.api.Assertions.assertFalse(cleared("RCV-MAND"), "PENDING 时强制质检阻塞");

        // 直接置 ACCEPTED（模拟质检合格）→ 放行
        ormTemplate.runInSession(() -> {
            ErpQaInspection ins = daoProvider.daoFor(ErpQaInspection.class).getEntityById(insId);
            ins.setResult(ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            daoProvider.daoFor(ErpQaInspection.class).updateEntity(ins);
        });
        assertTrue(cleared("RCV-MAND"), "ACCEPTED 后放行");
    }

    // ---------- helpers ----------

    private boolean cleared(String billCode) {
        ApiResponse<?> resp = rpc(query, "ErpQaInspection__isInspectionCleared",
                ApiRequest.build(Map.of("billType", ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT,
                        "billCode", billCode)));
        assertEquals(0, resp.getStatus());
        return Boolean.TRUE.equals(resp.getData());
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
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("inspectionId", insId));
        return daoProvider.daoFor(ErpQaInspectionLine.class).findAllByQuery(q);
    }

    private Long seedTemplate(String code, Long materialId, String inspectionType, TplLineSpec... lines) {
        Long id = 5000L + (long) (Math.abs(code.hashCode()) % 1000);
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
                lineDao.saveEntity(tl);
                lineNo++;
            }
        });
        return id;
    }

    private TplLineSpec tplLine(String parameterName, String specMin, String specMax) {
        return new TplLineSpec(parameterName, toBigDecimal(specMin), toBigDecimal(specMax));
    }

    private static BigDecimal toBigDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private void setConfig(String key, String value) {
        AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private static final class TplLineSpec {
        final String parameterName;
        final BigDecimal specMin;
        final BigDecimal specMax;

        TplLineSpec(String parameterName, BigDecimal specMin, BigDecimal specMax) {
            this.parameterName = parameterName;
            this.specMin = specMin;
            this.specMax = specMax;
        }
    }
}
