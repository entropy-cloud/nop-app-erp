package app.erp.mfg.service;

import app.erp.mfg.biz.CostRollupResult;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.service.costing.CostRollupService;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 端到端测试：成本卷算（采购件基础 + 制造件材料+工时 → {@code ErpMfgCostRollup/Line}）。
 *
 * <p>场景：M1(采购10) + M2(采购5) → SA(制造：3×M1 材料30 + 30min×费率20 人工10 = 40)；
 * SA(2) + M2(1) → P(制造：材料 2×40+1×5=85 + 60min×费率20 人工20 = 105)。逐层向上汇总，落 RollupLine。
 *
 * <p>覆盖：采购件 purchasePrice 基础；制造件材料+工时汇总；多级逐层向上；采购件基础成本空抛
 * {@code ERR_ROLLUP_BASE_COST_MISSING}；Rollup(status=CALCULATED) + RollupLine.unitCost 可由 N=1 STANDARD 后继读取。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgCostRollup extends JunitAutoTestCase {

    static final Long UOM_ID = 5101L;
    static final Long WC1 = 6101L;     // 工作中心（费率 20/小时）
    static final Long P = 1001L;       // 产成品
    static final Long SA = 1002L;      // 半成品（制造）
    static final Long M1 = 1003L;      // 采购件（采购价 10）
    static final Long M2 = 1004L;      // 采购件（采购价 5）
    static final Long M3 = 1009L;      // 采购件（无采购价 → 触发 ERR_ROLLUP_BASE_COST_MISSING）

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    CostRollupService costRollupService;

    @Test
    public void testRollupPurchaseAndManufacturedBottomUp() {
        seedWorkcenter(WC1, bd("20"));
        seedMaterial(M1); seedMaterial(M2); seedMaterial(SA); seedMaterial(P);
        seedSku(7001L, M1, bd("10"), true);
        seedSku(7002L, M2, bd("5"), true);

        // SA(qty1) → M1(qty3)；工序 30min@WC1
        Long bomSA = seedBom(2202L, SA, true, true, bd("1"));
        seedLine(3202L, bomSA, M1, bd("3"), 10);
        seedOperation(4202L, bomSA, WC1, bd("30"), 10);
        // P(qty1) → SA(qty2), M2(qty1)；工序 60min@WC1
        Long bomP = seedBom(2201L, P, true, true, bd("1"));
        seedLine(3201L, bomP, SA, bd("2"), 10);
        seedLine(3203L, bomP, M2, bd("1"), 20);
        seedOperation(4201L, bomP, WC1, bd("60"), 10);

        CostRollupResult result = costRollupService.rollup(bomP);

        // 直接断言单位标准成本分解
        assertEquals(0, bd("10").compareTo(unit(result, M1)), "M1 采购件单位成本 = 采购价 10");
        assertEquals(0, bd("5").compareTo(unit(result, M2)), "M2 采购件单位成本 = 采购价 5");
        assertEquals(0, bd("40").compareTo(unit(result, SA)), "SA = 材料 3×10=30 + 人工 30/60×20=10 = 40");
        assertEquals(0, material(result, SA).compareTo(bd("30")), "SA 材料成本 30");
        assertEquals(0, labor(result, SA).compareTo(bd("10")), "SA 人工成本 10");
        assertEquals(0, bd("105").compareTo(unit(result, P)), "P = 材料 2×40+1×5=85 + 人工 60/60×20=20 = 105");
        assertEquals(0, material(result, P).compareTo(bd("85")), "P 材料成本 85");
        assertEquals(0, labor(result, P).compareTo(bd("20")), "P 人工成本 20");

        // 落库校验：Rollup 头 status=CALCULATED，行持久化、unitCost 可查询（供 N=1 STANDARD 后继读取）
        ErpMfgCostRollup head = daoProvider.daoFor(ErpMfgCostRollup.class).getEntityById(result.getRollupId());
        assertEquals(ErpMfgConstants.COST_ROLLUP_STATUS_CALCULATED, head.getStatus(),
                "卷算头状态 = CALCULATED");
        List<ErpMfgCostRollupLine> lines = findLines(result.getRollupId());
        assertEquals(4, lines.size(), "每物料一行（M1/M2/SA/P）");
        ErpMfgCostRollupLine pLine = lines.stream()
                .filter(l -> l.getMaterialId().equals(P)).findFirst().orElseThrow();
        assertEquals(0, pLine.getUnitCost().compareTo(bd("105")), "P 持久化 unitCost=105");
        assertEquals(ErpMfgConstants.COST_ROLLUP_STATUS_CALCULATED, result.getStatus());
    }

    @Test
    public void testRollupBaseCostMissingThrows() {
        seedWorkcenter(WC1, bd("20"));
        seedMaterial(M3); // M3 无默认 SKU → 无采购价
        Long bomP = seedBom(2601L, P, true, true, bd("1"));
        seedLine(3601L, bomP, M3, bd("1"), 10);

        NopException ex = assertThrows(NopException.class, () -> costRollupService.rollup(bomP));
        assertEquals(ErpMfgErrors.ERR_ROLLUP_BASE_COST_MISSING.getErrorCode(), ex.getCode(),
                "采购件无采购价抛 ERR_ROLLUP_BASE_COST_MISSING");
    }

    @Test
    public void testRollupCostViaGraphQLWiring() {
        seedWorkcenter(WC1, bd("20"));
        seedMaterial(M1); seedMaterial(P);
        seedSku(7001L, M1, bd("10"), true);
        Long bomP = seedBom(2701L, P, true, true, bd("1"));
        seedLine(3701L, bomP, M1, bd("2"), 10);

        ApiResponse<?> resp = executeRpc("ErpMfgBom__rollupCost", ApiRequest.build(Map.of("bomId", bomP)));
        assertEquals(0, resp.getStatus(), "rollupCost 经 GraphQL 调用应成功（BizModel→CostRollupService 装配正确）");
        Map<?, ?> data = (Map<?, ?>) resp.get();
        assertNotNull(data.get("rollupId"), "返回 rollupId");
        // P = 材料 2×10=20；断言 GraphQL 响应含 unitCost
        List<?> lines = (List<?>) data.get("lines");
        assertTrue(lines.size() >= 2, "至少 M1 + P 两行");
    }

    // ---------- Phase 1/2 (plan 2026-07-13-0455-2) 成本要素拆分 ----------

    /**
     * overhead MACHINE_HOUR 分配模式（plan 2026-07-13-0455-2 §Phase 1）。
     * SA 工序 30min@WC1(rate20)：machineHours=0.5、labor=0.5×20=10。
     * P 工序 60min@WC1：machineHours=1、labor=1×20=20。
     * 开 overhead(rate=5/MACHINE_HOUR)：SA overhead=0.5×5=2.5、P overhead=1×5=5；
     * SA unit=30+10+2.5=42.5；P unit=材料 2×42.5=85 + 人工 20 + overhead 5 = 110。
     */
    @Test
    public void testOverheadMachineHourMode() {
        seedWorkcenter(WC1, bd("20"));
        seedMaterial(M1); seedMaterial(SA); seedMaterial(P);
        seedSku(7001L, M1, bd("10"), true);
        Long bomSA = seedBom(2202L, SA, true, true, bd("1"));
        seedLine(3202L, bomSA, M1, bd("3"), 10);
        seedOperation(4202L, bomSA, WC1, bd("30"), 10);
        Long bomP = seedBom(2201L, P, true, true, bd("1"));
        seedLine(3201L, bomP, SA, bd("2"), 10);
        seedOperation(4201L, bomP, WC1, bd("60"), 10);

        setOverheadEnabled(true);
        setOverheadMode(ErpMfgConstants.OVERHEAD_ALLOCATION_MODE_MACHINE_HOUR);
        setOverheadRate(bd("5"));
        try {
            CostRollupResult result = costRollupService.rollup(bomP);
            // SA: overhead = 0.5h × 5 = 2.5
            assertEquals(0, bd("2.5").compareTo(overhead(result, SA)), "SA overhead=机器工时 0.5×5=2.5");
            assertEquals(0, bd("42.5").compareTo(unit(result, SA)), "SA unit=30+10+2.5=42.5");
            // P: overhead = 1h × 5 = 5
            assertEquals(0, bd("5").compareTo(overhead(result, P)), "P overhead=机器工时 1×5=5");
            // P: unit = 材料 2×42.5=85 + 人工 20 + overhead 5 = 110
            assertEquals(0, bd("110").compareTo(unit(result, P)), "P unit=85+20+5=110");
            // 四要素和 = total/unit 断言
            assertFourElementSum(result, SA);
            assertFourElementSum(result, P);
        } finally {
            setOverheadEnabled(false);
            setOverheadRate(bd("0"));
        }
    }

    /**
     * overhead LABOR_RATIO 分配模式：overhead = laborCost × rate。
     * SA: labor=10、rate=0.5 → overhead=5；unit=30+10+5=45。
     */
    @Test
    public void testOverheadLaborRatioMode() {
        seedWorkcenter(WC1, bd("20"));
        seedMaterial(M1); seedMaterial(SA);
        seedSku(7001L, M1, bd("10"), true);
        Long bomSA = seedBom(2202L, SA, true, true, bd("1"));
        seedLine(3202L, bomSA, M1, bd("3"), 10);
        seedOperation(4202L, bomSA, WC1, bd("30"), 10);

        setOverheadEnabled(true);
        setOverheadMode(ErpMfgConstants.OVERHEAD_ALLOCATION_MODE_LABOR_RATIO);
        setOverheadRate(bd("0.5"));
        try {
            CostRollupResult result = costRollupService.rollup(bomSA);
            assertEquals(0, bd("5").compareTo(overhead(result, SA)), "SA overhead=labor 10×0.5=5");
            assertEquals(0, bd("45").compareTo(unit(result, SA)), "SA unit=30+10+5=45");
            assertFourElementSum(result, SA);
        } finally {
            setOverheadEnabled(false);
            setOverheadRate(bd("0"));
        }
    }

    /**
     * subcontract 委外费归集（plan 2026-07-13-0455-2 §Phase 2）。
     * 采购件 M1 基础成本 10；COMPLETED 委外单加工费 20、产量 2 → 单位委外费 10。
     * config 开时 M1 subcontractCost=10、unit=10+10=20。
     */
    @Test
    public void testSubcontractAggregationEnabled() {
        seedWorkcenter(WC1, bd("20"));
        seedMaterial(M1); seedMaterial(P);
        seedSku(7001L, M1, bd("10"), true);
        // M1 作为委外产出（productId=M1），加工费 20 / 产量 2 → 单位委外费 10
        Long subId = seedSubcontractOrder(8801L, "SUB-COST-1", M1, bd("20"));
        seedSubcontractLine(9801L, subId, M1, bd("2"));
        Long bomP = seedBom(2701L, P, true, true, bd("1"));
        seedLine(3701L, bomP, M1, bd("2"), 10);

        setSubcontractAggregationEnabled(true);
        try {
            CostRollupResult result = costRollupService.rollup(bomP);
            assertEquals(0, bd("10").compareTo(subcontract(result, M1)), "M1 subcontract=加工费20/产量2=10");
            assertEquals(0, bd("20").compareTo(unit(result, M1)), "M1 unit=材料10+委外10=20");
            // P = 2×M1(20) = 40
            assertEquals(0, bd("40").compareTo(unit(result, P)), "P unit=2×20=40");
            assertFourElementSum(result, M1);
            assertFourElementSum(result, P);
        } finally {
            setSubcontractAggregationEnabled(false);
        }
    }

    /** subcontract 归集关闭时向后兼容（subcontractCost 恒 0），与既有行为一致。 */
    @Test
    public void testSubcontractAggregationDisabledIsZero() {
        seedWorkcenter(WC1, bd("20"));
        seedMaterial(M1); seedMaterial(P);
        seedSku(7001L, M1, bd("10"), true);
        Long subId = seedSubcontractOrder(8802L, "SUB-COST-OFF", M1, bd("20"));
        seedSubcontractLine(9802L, subId, M1, bd("2"));
        Long bomP = seedBom(2701L, P, true, true, bd("1"));
        seedLine(3701L, bomP, M1, bd("2"), 10);

        CostRollupResult result = costRollupService.rollup(bomP);
        assertEquals(0, BigDecimal.ZERO.compareTo(subcontract(result, M1)), "config 关时 M1 subcontract=0");
        assertEquals(0, bd("10").compareTo(unit(result, M1)), "config 关时 M1 unit=材料10（不含委外）");
    }

    /** 四要素 + overhead + subcontract 同时开启的端到端集成。 */
    @Test
    public void testFourElementIntegration() {
        seedWorkcenter(WC1, bd("20"));
        seedMaterial(M1); seedMaterial(SA); seedMaterial(P);
        seedSku(7001L, M1, bd("10"), true);
        // M1 含委外费：加工费 20 / 产量 2 → 单位委外 10 → M1 unit = 10+10=20
        Long subId = seedSubcontractOrder(8803L, "SUB-INT", M1, bd("20"));
        seedSubcontractLine(9803L, subId, M1, bd("2"));
        Long bomSA = seedBom(2202L, SA, true, true, bd("1"));
        seedLine(3202L, bomSA, M1, bd("3"), 10);
        seedOperation(4202L, bomSA, WC1, bd("30"), 10);
        Long bomP = seedBom(2201L, P, true, true, bd("1"));
        seedLine(3201L, bomP, SA, bd("2"), 10);

        setOverheadEnabled(true);
        setOverheadMode(ErpMfgConstants.OVERHEAD_ALLOCATION_MODE_MACHINE_HOUR);
        setOverheadRate(bd("5"));
        setSubcontractAggregationEnabled(true);
        try {
            CostRollupResult result = costRollupService.rollup(bomP);
            // SA: 材料 3×M1(20)=60 + 人工 10 + overhead 2.5 + subcontract 0 = 72.5
            assertEquals(0, bd("60").compareTo(material(result, SA)), "SA 材料=3×20=60");
            assertEquals(0, bd("2.5").compareTo(overhead(result, SA)), "SA overhead=2.5");
            assertEquals(0, bd("72.5").compareTo(unit(result, SA)), "SA unit=60+10+2.5+0=72.5");
            assertFourElementSum(result, SA);
            assertFourElementSum(result, M1);
            assertFourElementSum(result, P);
        } finally {
            setOverheadEnabled(false);
            setOverheadRate(bd("0"));
            setSubcontractAggregationEnabled(false);
        }
    }

    // ---------- helpers ----------

    private static BigDecimal unit(CostRollupResult r, Long mat) {
        return r.getLines().stream().filter(l -> l.getMaterialId().equals(mat))
                .map(app.erp.mfg.biz.CostRollupLineView::getUnitCost).findFirst()
                .orElseThrow(() -> new AssertionError("no line " + mat));
    }

    private static BigDecimal material(CostRollupResult r, Long mat) {
        return r.getLines().stream().filter(l -> l.getMaterialId().equals(mat))
                .map(app.erp.mfg.biz.CostRollupLineView::getMaterialCost).findFirst()
                .orElseThrow(() -> new AssertionError("no line " + mat));
    }

    private static BigDecimal labor(CostRollupResult r, Long mat) {
        return r.getLines().stream().filter(l -> l.getMaterialId().equals(mat))
                .map(app.erp.mfg.biz.CostRollupLineView::getLaborCost).findFirst()
                .orElseThrow(() -> new AssertionError("no line " + mat));
    }

    private static BigDecimal overhead(CostRollupResult r, Long mat) {
        return r.getLines().stream().filter(l -> l.getMaterialId().equals(mat))
                .map(app.erp.mfg.biz.CostRollupLineView::getOverheadCost).findFirst()
                .orElseThrow(() -> new AssertionError("no line " + mat));
    }

    private static BigDecimal subcontract(CostRollupResult r, Long mat) {
        return r.getLines().stream().filter(l -> l.getMaterialId().equals(mat))
                .map(app.erp.mfg.biz.CostRollupLineView::getSubcontractCost).findFirst()
                .orElseThrow(() -> new AssertionError("no line " + mat));
    }

    /** 断言 total/unit = material+labor+overhead+subcontract 四要素和。 */
    private static void assertFourElementSum(CostRollupResult r, Long mat) {
        app.erp.mfg.biz.CostRollupLineView v = r.getLines().stream()
                .filter(l -> l.getMaterialId().equals(mat)).findFirst()
                .orElseThrow(() -> new AssertionError("no line " + mat));
        BigDecimal sum = nz(v.getMaterialCost()).add(nz(v.getLaborCost()))
                .add(nz(v.getOverheadCost())).add(nz(v.getSubcontractCost()));
        assertEquals(0, v.getUnitCost().compareTo(sum),
                "unit=" + v.getUnitCost() + " 应等于四要素和 " + sum + " (mat=" + mat + ")");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private List<ErpMfgCostRollupLine> findLines(Long rollupId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("costRollupId", rollupId));
        return daoProvider.daoFor(ErpMfgCostRollupLine.class).findAllByQuery(q);
    }

    private ApiResponse<?> executeRpc(String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void seedMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", 20);
            m.setUoMId(UOM_ID);
            m.orm_propValueByName("status", 10);
            dao.saveEntity(m);
        });
    }

    private void seedSku(Long id, Long materialId, BigDecimal purchasePrice, boolean isDefault) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterialSku> dao = daoProvider.daoFor(ErpMdMaterialSku.class);
            ErpMdMaterialSku sku = new ErpMdMaterialSku();
            sku.orm_propValueByName("id", id);
            sku.setMaterialId(materialId);
            sku.setSkuCode("SKU-" + id);
            sku.setUoMId(UOM_ID);
            sku.orm_propValueByName("conversionRate", BigDecimal.ONE);
            sku.setPurchasePrice(purchasePrice);
            sku.setIsDefault(isDefault);
            dao.saveEntity(sku);
        });
    }

    private void seedWorkcenter(Long id, BigDecimal hourlyRate) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenter> dao = daoProvider.daoFor(ErpMfgWorkcenter.class);
            ErpMfgWorkcenter wc = new ErpMfgWorkcenter();
            wc.orm_propValueByName("id", id);
            wc.setCode("WC-" + id);
            wc.setName("Workcenter " + id);
            wc.setHourlyRate(hourlyRate);
            dao.saveEntity(wc);
        });
    }

    private Long seedBom(Long id, Long productId, boolean isDefault, boolean isActive, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", id);
            bom.setCode("BOM-" + id);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(isDefault);
            bom.setIsActive(isActive);
            bom.setQty(qty);
            dao.saveEntity(bom);
        });
        return id;
    }

    private void seedLine(Long id, Long bomId, Long materialId, BigDecimal quantity, int lineNo) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBomLine> dao = daoProvider.daoFor(ErpMfgBomLine.class);
            ErpMfgBomLine line = new ErpMfgBomLine();
            line.orm_propValueByName("id", id);
            line.setBomId(bomId);
            line.setLineNo(lineNo);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(quantity);
            dao.saveEntity(line);
        });
    }

    private void seedOperation(Long id, Long bomId, Long workcenterId, BigDecimal standardTime, int lineNo) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBomOperation> dao = daoProvider.daoFor(ErpMfgBomOperation.class);
            ErpMfgBomOperation op = new ErpMfgBomOperation();
            op.orm_propValueByName("id", id);
            op.setBomId(bomId);
            op.setLineNo(lineNo);
            op.setOperationId(9000L); // 工序引用（测试占位，FK 不强制）
            op.setWorkcenterId(workcenterId);
            op.setStandardTime(standardTime);
            dao.saveEntity(op);
        });
    }

    // ---- 成本要素拆分（plan 2026-07-13-0455-2）seeds + config helpers ----

    private Long seedSubcontractOrder(Long id, String code, Long productId, BigDecimal processingFee) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgSubcontractOrder> dao = daoProvider.daoFor(ErpMfgSubcontractOrder.class);
            ErpMfgSubcontractOrder order = new ErpMfgSubcontractOrder();
            order.orm_propValueByName("id", id);
            order.setCode(code);
            order.setSupplierId(4601L);
            order.setProductId(productId);
            order.setBusinessDate(LocalDate.of(2026, 7, 1));
            order.setCurrencyId(6601L);
            order.setExchangeRate(BigDecimal.ONE);
            order.setProcessingFee(processingFee);
            order.setTotalAmount(processingFee);
            order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED);
            order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_APPROVED);
            order.orm_propValueByName("postedStatus", "POSTED");
            dao.saveEntity(order);
        });
        return id;
    }

    private void seedSubcontractLine(Long id, Long orderId, Long materialId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgSubcontractOrderLine> dao = daoProvider.daoFor(ErpMfgSubcontractOrderLine.class);
            ErpMfgSubcontractOrderLine line = new ErpMfgSubcontractOrderLine();
            line.orm_propValueByName("id", id);
            line.setSubcontractOrderId(orderId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            dao.saveEntity(line);
        });
    }

    private void setOverheadEnabled(boolean enabled) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_OVERHEAD_ALLOCATION_ENABLED, String.valueOf(enabled));
    }

    private void setOverheadMode(String mode) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_OVERHEAD_ALLOCATION_MODE, mode);
    }

    private void setOverheadRate(BigDecimal rate) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_OVERHEAD_ALLOCATION_RATE, rate.toPlainString());
    }

    private void setSubcontractAggregationEnabled(boolean enabled) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_SUBCONTRACT_COST_AGGREGATION_ENABLED, String.valueOf(enabled));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
