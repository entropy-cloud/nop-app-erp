package app.erp.inv.service;

import app.erp.inv.dao._ErpInvDaoConstants;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdUoM;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.FieldSelectionBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
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

/**
 * 库存交易行实体 materialName 名称解析 BizLoader 测试（机制 D：xmeta 派生 materialName + @BizLoader 批量加载）。
 *
 * <p>覆盖 ErpInvStockMoveLine（materialName）。经 {@link IGraphQLEngine} findList + FieldSelectionBean
 * 触发 BizLoader 字段解析，验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvFkNameLoader extends JunitAutoTestCase {

    static final Long UOM_ID = 6301L;
    static final Long MATERIAL_ID = 6401L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testStockMoveLineMaterialNameResolution() {
        ormTemplate.runInSession(() -> {
            seedUoM(UOM_ID, "个");
            seedMaterial(MATERIAL_ID, "物料Theta");
            seedStockMove(6001L);
            seedStockMoveLine(6101L, 6001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpInvStockMoveLine__findList",
                "id", "materialName");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条库存移动行");
        Map<String, Object> first = rows.get(0);
        assertEquals("物料Theta", first.get("materialName"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryWithSelection(String action, String... fields) {
        FieldSelectionBean selection = new FieldSelectionBean();
        for (String f : fields) {
            selection.addField(f);
        }
        ApiRequest<?> request = ApiRequest.build(Map.of());
        request.setSelection(selection);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                GraphQLOperationType.query, action, request);
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), action + " 查询成功");
        Object data = resp.getData();
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        return (List<Map<String, Object>>) ((Map<?, ?>) data).get("items");
    }

    private void seedUoM(long id, String name) {
        IEntityDao<ErpMdUoM> dao = daoProvider.daoFor(ErpMdUoM.class);
        ErpMdUoM u = dao.newEntity();
        u.orm_propValue(1, id);
        u.setCode("UOM-" + id);
        u.setName(name);
        dao.saveEntity(u);
    }

    private void seedMaterial(long id, String name) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("MAT-" + id);
        m.setName(name);
        m.orm_propValueByName("materialType", "GOODS");
        m.setUoMId(UOM_ID);
        m.setStatus("ACTIVE");
        dao.saveEntity(m);
    }

    private void seedStockMove(long id) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        ErpInvStockMove move = dao.newEntity();
        move.orm_propValue(1, id);
        move.setCode("MV-FK-" + id);
        move.orm_propValueByName("moveType", ErpInvConstants.MOVE_TYPE_INTERNAL_TRANSFER);
        move.setBusinessDate(LocalDate.of(2026, 7, 1));
        move.setDocStatus(ErpInvConstants.DOC_STATUS_DRAFT);
        move.setApproveStatus(_ErpInvDaoConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(move);
    }

    private void seedStockMoveLine(long id, long moveId) {
        IEntityDao<ErpInvStockMoveLine> dao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        ErpInvStockMoveLine line = dao.newEntity();
        line.orm_propValue(1, id);
        line.setMoveId(moveId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(BigDecimal.TEN);
        dao.saveEntity(line);
    }
}
