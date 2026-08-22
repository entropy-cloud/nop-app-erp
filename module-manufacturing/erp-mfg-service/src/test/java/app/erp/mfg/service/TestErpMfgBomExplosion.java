package app.erp.mfg.service;

import app.erp.mfg.biz.BomExplosionNode;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.service.bom.BomExpander;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 测试：默认 BOM 选择 + 多级展开（单级/多级数量乘积/phantom/环检测/深度上限）。
 *
 * <p>覆盖 {@code bom-and-routing.md §多级 BOM 展开}：默认选择；单级有效用量 {@code line.qty × req / BOM.qty}；
 * 多级递归数量乘积；phantom（{@code bomType=20}）展开子件并入父级（不产生独立项）；环检测截断；深度上限截断。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgBomExplosion extends JunitAutoTestCase {

    static final String UOM_ID = "5101";
    static final String P = "1001";   // 产成品
    static final String SA = "1002";  // 半成品（制造子件）
    static final String M1 = "1003";  // 采购件
    static final String M2 = "1004";  // 采购件
    static final String PH = "1005";  // 虚拟件
    static final String CA = "1006";  // 制造链 A
    static final String CB = "1007";  // 制造链 B
    static final String CC = "1008";  // 制造链 C

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    BomExpander bomExpander;

    @Test
    public void testFindDefaultBomPicksDefaultActive() {
        String bomDefault = seedBom("2001", P, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedBom("2002", P, ErpMfgConstants.BOM_TYPE_MANUFACTURED, false, true, bd("1")); // 非默认
        seedBom("2003", P, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, false, bd("1")); // 默认但停用

        ApiResponse<?> resp = executeRpc("ErpMfgBom__findDefaultBom",
                ApiRequest.build(Map.of("productId", P)));
        assertEquals(0, resp.getStatus(), "findDefaultBom 应成功");
        Map<?, ?> data = (Map<?, ?>) resp.get();
        assertEquals(bomDefault, data.get("id").toString(), "取默认且有效的 BOM");
    }

    @Test
    public void testFindDefaultBomNotFound() {
        ApiResponse<?> resp = executeRpc("ErpMfgBom__findDefaultBom",
                ApiRequest.build(Map.of("productId", "99999")));
        assertNotEquals(0, resp.getStatus(), "无默认 BOM 应返回错误");
    }

    @Test
    public void testSingleLevelExplosionQuantities() {
        String bomP = seedBom("2101", P, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3101", bomP, M1, bd("2"), 10);
        seedLine("3102", bomP, M2, bd("3"), 20);

        List<BomExplosionNode> nodes = bomExpander.explode(bomP, bd("1"), false);
        assertEquals(2, nodes.size(), "单级展开两个直接子件");
        BomExplosionNode n1 = byMaterial(nodes, M1);
        BomExplosionNode n2 = byMaterial(nodes, M2);
        assertEquals(0, n1.getQuantity().compareTo(bd("2")), "req=1 → M1 有效用量 2");
        assertEquals(0, n2.getQuantity().compareTo(bd("3")), "req=1 → M2 有效用量 3");
        assertEquals(1, n1.getLevel());

        List<BomExplosionNode> nodes2 = bomExpander.explode(bomP, bd("2"), false);
        assertEquals(0, byMaterial(nodes2, M1).getQuantity().compareTo(bd("4")), "req=2 → M1 有效用量 4");
        assertEquals(0, byMaterial(nodes2, M2).getQuantity().compareTo(bd("6")), "req=2 → M2 有效用量 6");
    }

    @Test
    public void testMultiLevelExplosionQtyProduct() {
        // P(qty1) → SA(qty2, 制造)；SA(qty1) → M1(qty5)
        String bomP = seedBom("2201", P, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3201", bomP, SA, bd("2"), 10);
        String bomSA = seedBom("2202", SA, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3202", bomSA, M1, bd("5"), 10);

        List<BomExplosionNode> nodes = bomExpander.explode(bomP, bd("1"), true);
        BomExplosionNode saNode = byMaterial(nodes, SA);
        assertEquals(1, saNode.getLevel(), "SA 为 1 级制造子件");
        assertEquals(0, saNode.getQuantity().compareTo(bd("2")), "SA 有效用量 2");
        assertTrue(saNode.isManufactured(), "SA 标记为制造件");
        BomExplosionNode m1Node = byMaterial(nodes, M1);
        assertEquals(2, m1Node.getLevel(), "M1 为 2 级");
        assertEquals(0, m1Node.getQuantity().compareTo(bd("10")), "M1 有效用量 2×5=10（逐层乘积）");
    }

    @Test
    public void testPhantomExpandsIntoParentLevel() {
        // P(qty1) → PH(qty1, phantom)；PH(qty1) → M1(qty3)
        String bomP = seedBom("2301", P, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3301", bomP, PH, bd("1"), 10);
        seedBom("2302", PH, ErpMfgConstants.BOM_TYPE_PHANTOM, true, true, bd("1"));
        seedLine("3302", "2302", M1, bd("3"), 10);

        List<BomExplosionNode> nodes = bomExpander.explode(bomP, bd("1"), true);
        assertEquals(1, nodes.size(), "phantom 不产生独立项，仅其子件");
        BomExplosionNode m1 = nodes.get(0);
        assertEquals(M1, m1.getMaterialId());
        assertEquals(1, m1.getLevel(), "phantom 子件并入父级（1 级）");
        assertEquals(0, m1.getQuantity().compareTo(bd("3")), "M1 有效用量 1×3=3");
        assertTrue(nodes.stream().noneMatch(n -> n.getMaterialId().equals(PH)), "phantom 物料本身不出现");
    }

    @Test
    public void testCycleDetection() {
        // P → SA → P（环）
        String bomP = seedBom("2401", P, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3401", bomP, SA, bd("1"), 10);
        String bomSA = seedBom("2402", SA, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3402", bomSA, P, bd("1"), 10);

        NopException ex = assertThrows(NopException.class,
                () -> bomExpander.explode(bomP, bd("1"), true));
        assertEquals(ErpMfgErrors.ERR_BOM_CYCLE.getErrorCode(), ex.getCode(), "成环抛 ERR_BOM_CYCLE");
    }

    @Test
    public void testDepthLimitTruncation() {
        // 制造链 P → CA → CB → CC（深度 4），设上限 2
        String bomP = seedBom("2501", P, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3501", bomP, CA, bd("1"), 10);
        String bomA = seedBom("2502", CA, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3502", bomA, CB, bd("1"), 10);
        String bomB = seedBom("2503", CB, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3503", bomB, CC, bd("1"), 10);
        seedBom("2504", CC, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));

        io.nop.api.core.config.AppConfig.getConfigProvider()
                .assignConfigValue(ErpMfgConstants.CONFIG_BOM_MAX_DEPTH, "2");
        try {
            NopException ex = assertThrows(NopException.class,
                    () -> bomExpander.explode(bomP, bd("1"), true));
            assertEquals(ErpMfgErrors.ERR_BOM_MAX_DEPTH_EXCEEDED.getErrorCode(), ex.getCode(),
                    "深度超限抛 ERR_BOM_MAX_DEPTH_EXCEEDED");
        } finally {
            io.nop.api.core.config.AppConfig.getConfigProvider()
                    .assignConfigValue(ErpMfgConstants.CONFIG_BOM_MAX_DEPTH,
                            String.valueOf(ErpMfgConstants.DEFAULT_BOM_MAX_DEPTH));
        }
    }

    @Test
    public void testExplodeViaGraphQLWiring() {
        String bomP = seedBom("2601", P, ErpMfgConstants.BOM_TYPE_MANUFACTURED, true, true, bd("1"));
        seedLine("3601", bomP, M1, bd("2"), 10);
        seedLine("3602", bomP, M2, bd("3"), 20);

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("bomId", bomP);
        req.put("qty", bd("1"));
        req.put("useMultiLevel", Boolean.FALSE);
        ApiResponse<?> resp = executeRpc("ErpMfgBom__explode", ApiRequest.build(req));
        assertEquals(0, resp.getStatus(), "explode 经 GraphQL 调用应成功（BizModel→BomExpander 装配正确）");
        List<?> data = (List<?>) resp.get();
        assertEquals(2, data.size(), "单级展开两个子件");
    }

    // ---------- helpers ----------

    private ApiResponse<?> executeRpc(String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(query, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private static BomExplosionNode byMaterial(List<BomExplosionNode> nodes, String materialId) {
        return nodes.stream().filter(n -> n.getMaterialId().equals(materialId)).findFirst()
                .orElseThrow(() -> new AssertionError("未找到物料 " + materialId));
    }

    private String seedBom(String id, String productId, String bomType, boolean isDefault, boolean isActive, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", id);
            bom.setCode("BOM-" + id);
            bom.setProductId(productId);
            bom.setBomType(bomType);
            bom.setIsDefault(isDefault);
            bom.setIsActive(isActive);
            bom.setQty(qty);
            dao.saveEntity(bom);
        });
        return id;
    }

    private void seedLine(String id, String bomId, String materialId, BigDecimal quantity, int lineNo) {
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

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
