package app.erp.aps.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsDemandPlanning extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    static final String ORG_ID = "1601";
    static final String MATERIAL_ID = "4601";
    static final String WAREHOUSE_ID = "3601";

    @Test
    public void testAtpAvailableWhenStockSufficient() {
        seedStockBalance(MATERIAL_ID, new BigDecimal("20"));

        ApiResponse<?> resp = executeRpc(query, "ErpApsOperationOrder__checkFeasibility",
                ApiRequest.build(Map.of(
                        "materialId", MATERIAL_ID,
                        "qty", new BigDecimal("10"),
                        "desiredDate", "2026-07-30T00:00:00")));

        assertEquals(0, resp.getStatus());
        Object feasible = ((Map<?, ?>) resp.getData()).get("feasible");
        assertNotNull(feasible);
        assertTrue(Boolean.TRUE.equals(feasible));
    }

    @Test
    public void testAtpUnavailableWhenStockInsufficient() {
        seedStockBalance(MATERIAL_ID, new BigDecimal("5"));

        ApiResponse<?> resp = executeRpc(query, "ErpApsOperationOrder__checkFeasibility",
                ApiRequest.build(Map.of(
                        "materialId", MATERIAL_ID,
                        "qty", new BigDecimal("10"),
                        "desiredDate", "2026-07-30T00:00:00")));

        assertEquals(0, resp.getStatus());
        Object feasible = ((Map<?, ?>) resp.getData()).get("feasible");
        assertNotNull(feasible);
        assertFalse(Boolean.TRUE.equals(feasible));
    }

    @Test
    public void testEarliestCompletionDateReturnsNotNull() {
        seedStockBalance(MATERIAL_ID, new BigDecimal("20"));

        ApiResponse<?> resp = executeRpc(query, "ErpApsOperationOrder__earliestCompletionDate",
                ApiRequest.build(Map.of(
                        "materialId", MATERIAL_ID,
                        "qty", new BigDecimal("10"))));

        assertEquals(0, resp.getStatus());
        assertNotNull(resp.getData());
    }

    private void seedStockBalance(String materialId, BigDecimal available) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = new ErpInvStockBalance();
            b.orm_propValueByName("id", String.valueOf(8000L + Long.parseLong(materialId)));
            b.setOrgId(ORG_ID);
            b.setMaterialId(materialId);
            b.setWarehouseId(WAREHOUSE_ID);
            b.setTotalQuantity(available);
            b.setAvailableQuantity(available);
            dao.saveEntity(b);
        });
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
