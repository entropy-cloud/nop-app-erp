package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
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
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 测试：工单 10 态状态机（DRAFT→SUBMITTED→NOT_STARTED→STOCK_RESERVED/STOCK_PARTIAL→IN_PROCESS
 * →COMPLETED/STOPPED/CLOSED/CANCELLED）+ 齐套校验（全齐/部分）+ 非法迁移拒绝。
 *
 * <p>覆盖 {@code docs/design/manufacturing/state-machine.md §迁移完整性} 主路径与异常路径。
 * 经 {@link IGraphQLEngine} 调 {@code ErpMfgWorkOrder__submitForApproval/approve/...}，引擎负责建 session/事务/管道。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgWorkOrderStateMachine extends JunitAutoTestCase {

    @RegisterExtension
    static MfgFrozenClockExtension frozenClock = new MfgFrozenClockExtension();

    static final Long UOM_ID = 5101L;
    static final Long WAREHOUSE_ID = 3001L;
    static final Long P = 1001L;   // 产成品
    static final Long M1 = 1002L;  // 子件
    static final Long M2 = 1003L;  // 子件

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testHappyPathFullLifecycle() {
        seedComponentBomAndStock(bd("5"), bd("5"));   // M1=5, M2=5 充足
        Long woId = seedWorkOrder("WO-HAPPY");

        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT, statusOf(woId));
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED, statusOf(woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, statusOf(woId));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED, statusOf(woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, statusOf(woId));

        // 完工达量 → COMPLETED（Phase 2 基础实现：累加完工数量 + 状态迁移）
        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("1"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, statusOf(woId));
    }

    @Test
    public void testKitCheckFullAvailableGoesStockReserved() {
        seedComponentBomAndStock(bd("5"), bd("3"));   // 需 M1=2, M2=3，充足
        Long woId = seedWorkOrder("WO-FULL");

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED, statusOf(woId),
                "全齐套 → STOCK_RESERVED");
    }

    @Test
    public void testKitCheckPartialGoesStockPartial() {
        seedComponentBomAndStock(bd("5"), bd("1"));   // M2 仅 1 < 需 3 → 部分齐套
        Long woId = seedWorkOrder("WO-PARTIAL");

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL, statusOf(woId),
                "M2 缺料 → STOCK_PARTIAL");

        // 默认配置 erp-mfg.allow-partial-kit-start=false → 强制开工被拒
        ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));
        assertEquals(ErpMfgErrors.ERR_PARTIAL_KIT_START_FORBIDDEN.getErrorCode(), resp.getCode(),
                "部分齐套 + 配置禁止 → 拒绝开工");
    }

    @Test
    public void testPartialKitStartAllowedWhenConfigEnabled() {
        seedComponentBomAndStock(bd("5"), bd("1"));
        Long woId = seedWorkOrder("WO-PARTIAL-START");

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL, statusOf(woId));

        setConfig(ErpMfgConstants.CONFIG_ALLOW_PARTIAL_KIT_START, "true");
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));
            assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, statusOf(woId),
                    "配置允许 → 部分齐套可强制开工");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_ALLOW_PARTIAL_KIT_START, "false");
        }
    }

    @Test
    public void testStopResumeAndClose() {
        seedComponentBomAndStock(bd("5"), bd("5"));
        Long woId = seedWorkOrder("WO-STOP");
        moveToInProcess(woId);

        rpcOk(mutation, "ErpMfgWorkOrder__stop", Map.of("workOrderId", woId));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_STOPPED, statusOf(woId));
        rpcOk(mutation, "ErpMfgWorkOrder__resume", Map.of("workOrderId", woId));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, statusOf(woId));
        rpcOk(mutation, "ErpMfgWorkOrder__stop", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__close", Map.of("workOrderId", woId));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_CLOSED, statusOf(woId));
    }

    @Test
    public void testCancelFromSubmitted() {
        seedComponentBomAndStock(bd("5"), bd("5"));
        Long woId = seedWorkOrder("WO-CANCEL");
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__cancel", Map.of("workOrderId", woId));
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED, statusOf(woId));
    }

    @Test
    public void testIllegalTransitionsRejected() {
        seedComponentBomAndStock(bd("5"), bd("5"));
        Long woId = seedWorkOrder("WO-ILLEGAL");

        // DRAFT 直接开工（未审核/未齐套）→ 非法
        ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));
        assertEquals(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "DRAFT→IN_PROCESS 非法迁移应拒绝");

        // 终态 COMPLETED 后再 stop → 非法
        Long doneId = seedWorkOrder("WO-DONE");
        ormTemplate.runInSession(() -> {
            ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(doneId);
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
            daoProvider.daoFor(ErpMfgWorkOrder.class).updateEntity(wo);
        });
        resp = rpc(mutation, "ErpMfgWorkOrder__stop", Map.of("workOrderId", doneId));
        assertEquals(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "COMPLETED→STOPPED 终态非法迁移应拒绝");
    }

    @Test
    public void testOverReportRejected() {
        seedComponentBomAndStock(bd("5"), bd("5"));
        Long woId = seedWorkOrder("WO-OVER");
        moveToInProcess(woId);

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("workOrderId", woId);
        req.put("completedQty", bd("2"));   // plannedQuantity=1 → 超产
        ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrder__reportCompletion", req);
        assertEquals(ErpMfgErrors.ERR_OVER_REPORT.getErrorCode(), resp.getCode(),
                "超产（未启用超产配置）应拒绝");
    }

    // ---------- helpers ----------

    private String statusOf(Long woId) {
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        return wo.getDocStatus();
    }

    private void moveToInProcess(Long woId) {
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功，但返回: " + resp);
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private void seedComponentBomAndStock(BigDecimal m1Available, BigDecimal m2Available) {
        seedMaterial(P);
        seedMaterial(M1);
        seedMaterial(M2);
        Long bomP = seedBom(9001L, P);
        seedBomLine(9101L, bomP, M1, bd("2"));
        seedBomLine(9102L, bomP, M2, bd("3"));
        seedBalance(M1, m1Available);
        seedBalance(M2, m2Available);
    }

    private Long seedWorkOrder(String code) {
        Long id = 8000L + (long) Math.abs(code.hashCode() % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(9001L);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });
        return id;
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

    private Long seedBom(Long id, Long productId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", id);
            bom.setCode("BOM-" + id);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(bd("1"));
            dao.saveEntity(bom);
        });
        return id;
    }

    private void seedBomLine(Long id, Long bomId, Long materialId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBomLine> dao = daoProvider.daoFor(ErpMfgBomLine.class);
            ErpMfgBomLine line = new ErpMfgBomLine();
            line.orm_propValueByName("id", id);
            line.setBomId(bomId);
            line.setLineNo((int) (id % 100));
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            dao.saveEntity(line);
        });
    }

    private void seedBalance(Long materialId, BigDecimal available) {
        Long id = 8500L + materialId;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = new ErpInvStockBalance();
            b.orm_propValueByName("id", id);
            b.setMaterialId(materialId);
            b.setWarehouseId(WAREHOUSE_ID);
            b.setTotalQuantity(available);
            b.setAvailableQuantity(available);
            dao.saveEntity(b);
        });
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
