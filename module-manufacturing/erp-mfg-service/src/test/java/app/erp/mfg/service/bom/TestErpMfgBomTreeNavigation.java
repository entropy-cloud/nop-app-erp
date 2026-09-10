package app.erp.mfg.service.bom;

import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmEntity;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.8 分片④ mfg2-027-r3（DIM-T 覆盖缺口）：{@code findBomTree} 栈算法行为断言——
 * explode 返回 pre-order DFS 平铺列表，findBomTree 以栈重建父子嵌套（父先于子、level 回退弹栈）。
 * 修复前零测试断言；种子两级 BOM（产成品→半成品→采购件）验证嵌套层级、数量乘链与 manufactured 标记。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgBomTreeNavigation extends JunitAutoTestCase {

    static final String UOM_ID = "5101";
    static final String P = "1001";   // 产成品
    static final String SA = "1002";  // 半成品（制造子件）
    static final String M1 = "1003";  // 采购件

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @SuppressWarnings("unchecked")
    @Test
    public void testFindBomTreeRebuildsTwoLevelNesting() {
        String bomP = seedBom("9301", P);
        String bomSA = seedBom("9302", SA);
        seedLine("9311", bomP, SA, new BigDecimal("2"), 10);
        seedLine("9321", bomSA, M1, new BigDecimal("3"), 10);

        ApiResponse<?> resp = rpc(query, "ErpMfgBom__findBomTree",
                ApiRequest.build(Map.of("bomId", bomP, "qty", new BigDecimal("1"), "useMultiLevel", true)));
        assertEquals(0, resp.getStatus(), "findBomTree 查询应成功");
        // findBomTree 节点 = BOM 组件（产成品自身不入树）：根 = 首个 top 组件 SA（level 0），
        // M1 为其子节点（level 1）——栈算法以 level 回退弹栈重建 explode 的 pre-order DFS 平铺。
        List<Map<String, Object>> roots = (List<Map<String, Object>>) resp.getData();
        assertEquals(1, roots.size(), "单根树（根 = 首组件 SA）");
        Map<String, Object> sa = roots.get(0);
        assertEquals(SA, String.valueOf(sa.get("materialId")));
        assertEquals(Boolean.TRUE, sa.get("manufactured"), "SA 为制造子件");
        assertEquals(1, ((Number) sa.get("level")).intValue(), "SA level=1（explode 层级从 1 起）");

        List<Map<String, Object>> m1Children = (List<Map<String, Object>>) sa.get("children");
        assertEquals(1, m1Children.size(), "SA 下挂采购件 M1（栈算法第二层嵌套）");
        Map<String, Object> m1 = m1Children.get(0);
        assertEquals(M1, String.valueOf(m1.get("materialId")));
        assertEquals(Boolean.FALSE, m1.get("manufactured"), "M1 为采购子件");
        assertEquals(2, ((Number) m1.get("level")).intValue(), "M1 level=2");

        // 数量乘链：根 qty=1，P→SA 用量 2，SA→M1 用量 3 → 展开节点 SA qty=2、M1 qty=6
        assertEquals(0, new BigDecimal("2").compareTo(new BigDecimal(String.valueOf(sa.get("quantity")))),
                "SA 展开数量 = 根 qty × 用量 2");
        assertEquals(0, new BigDecimal("6").compareTo(new BigDecimal(String.valueOf(m1.get("quantity")))),
                "M1 展开数量 = 2 × 3（乘链）");
    }

    private String seedBom(String id, String productId) {
        ormTemplate.runInSession((io.nop.orm.IOrmSession sess) -> {
            ErpMfgBom bom = new ErpMfgBom();
            ((IOrmEntity) bom).orm_propValueByName("id", id);
            bom.setCode("BOM-" + id);
            bom.setProductId(productId);
            bom.setBomType("MANUFACTURED");
            bom.setIsDefault(true);
            bom.setIsActive(true);
            bom.setQty(BigDecimal.ONE);
            daoProvider.daoFor(ErpMfgBom.class).saveEntity(bom);
            return null;
        });
        return id;
    }

    private void seedLine(String id, String bomId, String materialId, BigDecimal quantity, int lineNo) {
        ormTemplate.runInSession((io.nop.orm.IOrmSession sess) -> {
            ErpMfgBomLine line = new ErpMfgBomLine();
            ((IOrmEntity) line).orm_propValueByName("id", id);
            line.setBomId(bomId);
            line.setLineNo(lineNo);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(quantity);
            daoProvider.daoFor(ErpMfgBomLine.class).saveEntity(line);
            return null;
        });
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
