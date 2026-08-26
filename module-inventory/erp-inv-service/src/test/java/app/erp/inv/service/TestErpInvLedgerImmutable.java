package app.erp.inv.service;

import app.erp.common.service.ErpCommonErrors;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * F1.3（P1-CK-inv-003）：库存流水不可变——AbstractErpImmutableCrudBizModel 拒绝全部
 * 通用 {@code __update}/{@code __delete}（台账由域内编排驱动，owner doc「流水不可变/余额由流水驱动」）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvLedgerImmutable extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private String seedLedger(String code) {
        return ormTemplate.runInSession(sess -> {
            ErpInvStockLedger ledger = new ErpInvStockLedger();
            ledger.setCode(code);
            ledger.setMoveId("9001");
            ledger.setMoveLineId("9002");
            ledger.setMaterialId("1001");
            ledger.setWarehouseId("3001");
            ledger.setQuantity(new BigDecimal("5"));
            ledger.setUnitCost(new BigDecimal("2"));
            ledger.setTotalCost(new BigDecimal("10"));
            ledger.setBalanceQuantity(new BigDecimal("5"));
            ledger.setBalanceTotalCost(new BigDecimal("10"));
            daoProvider.daoFor(ErpInvStockLedger.class).saveEntity(ledger);
            return ledger.getId();
        });
    }

    @Test
    public void testLedgerGenericUpdateAndDeleteRejected() {
        String id = seedLedger("SL-IMMUTABLE-001");

        ApiResponse<?> upd = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpInvStockLedger__update",
                ApiRequest.build(Map.of("data", Map.of("id", id, "code", "SL-TAMPERED")))));
        assertNotEquals(0, upd.getStatus(), "库存流水通用 update 应被拒（不可变台账）");
        assertEquals(ErpCommonErrors.ERR_CRUD_IMMUTABLE_ENTITY.getErrorCode(), upd.getCode());

        ApiResponse<?> del = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpInvStockLedger__delete", ApiRequest.build(Map.of("id", id))));
        assertNotEquals(0, del.getStatus(), "库存流水通用 delete 应被拒（不可变台账）");
        assertEquals(ErpCommonErrors.ERR_CRUD_IMMUTABLE_ENTITY.getErrorCode(), del.getCode());

        // 实体仍在（未被删）
        org.junit.jupiter.api.Assertions.assertNotNull(
                ormTemplate.runInSession(sess -> daoProvider.daoFor(ErpInvStockLedger.class).getEntityById(id)),
                "流水行未被通用 delete 移除");
    }
}
