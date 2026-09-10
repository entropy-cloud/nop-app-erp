package app.erp.inv.service;

import app.erp.common.service.ErpCommonErrors;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * M2.8 分片③ common-011-r3 红态先行（F 族 immutable 全拒语义）：不可变台账实体
 * （{@code ErpInvStockBalanceBizModel} extends AbstractErpImmutableCrudBizModel）经通用
 * {@code __save}（create/upsert）通道写入应全拒——F1.3 仅封 __update/__delete，
 * __save 通道（defaultPrepareSave）零守卫为最后盲区（373 生成页面暴露 save 通道）。
 *
 * <p>修复前本测试红（__save 成功创建台账行）；补 immutable 全拒 override 后绿。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvImmutableSaveChannel extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    io.nop.dao.api.IDaoProvider daoProvider;
    @Inject
    io.nop.orm.IOrmTemplate ormTemplate;

    private Map<String, Object> seedRefsAndBuildPayload() {
        java.util.concurrent.atomic.AtomicReference<String> materialId = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<String> warehouseId = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<String> locationId = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<String> currencyId = new java.util.concurrent.atomic.AtomicReference<>();
        ormTemplate.runInSession((io.nop.orm.IOrmSession sess) -> {
            ErpMdCurrency cur = new ErpMdCurrency();
            cur.setCode("M28-CUR");
            cur.setName("M2.8 币种");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(cur);
            currencyId.set(cur.getId());

            ErpMdUoM uom = new ErpMdUoM();
            uom.setCode("M28-UOM");
            uom.setName("M2.8 单位");
            daoProvider.daoFor(ErpMdUoM.class).saveEntity(uom);

            ErpMdMaterial m = new ErpMdMaterial();
            m.setCode("M28-IMM-MAT");
            m.setName("M2.8 不可变测试物料");
            m.setMaterialType("RAW");
            m.setUoMId(uom.getId());
            m.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdMaterial.class).saveEntity(m);
            materialId.set(m.getId());

            ErpMdWarehouse w = new ErpMdWarehouse();
            w.setCode("M28-WH");
            w.setName("M2.8 测试仓");
            w.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdWarehouse.class).saveEntity(w);
            warehouseId.set(w.getId());

            ErpMdLocation loc = new ErpMdLocation();
            loc.setWarehouseId(w.getId());
            loc.setCode("M28-LOC");
            loc.setName("M2.8 测试库位");
            daoProvider.daoFor(ErpMdLocation.class).saveEntity(loc);
            locationId.set(loc.getId());
            return null;
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("materialId", materialId.get());
        data.put("warehouseId", warehouseId.get());
        data.put("locationId", locationId.get());
        data.put("totalQuantity", new BigDecimal("1"));
        data.put("reservedQuantity", BigDecimal.ZERO);
        data.put("lockedQuantity", BigDecimal.ZERO);
        data.put("availableQuantity", new BigDecimal("1"));
        data.put("costMethod", ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        data.put("avgCost", new BigDecimal("2"));
        data.put("totalCost", new BigDecimal("2"));
        data.put("currencyId", currencyId.get());
        return data;
    }

    @Test
    public void testSaveChannelRejectsImmutableEntity() {
        Map<String, Object> data = seedRefsAndBuildPayload();

        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpInvStockBalance__save", ApiRequest.build(Map.of("data", data))));
        assertNotEquals(0, resp.getStatus(),
                "不可变台账实体经通用 __save（create/upsert）通道写入应全拒（common-011-r3 immutable 语义）");
        assertEquals(ErpCommonErrors.ERR_CRUD_IMMUTABLE_ENTITY.getErrorCode(), resp.getCode(),
                "错误码 = 台账不可变");
    }
}
