package app.erp.inv.service;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.InvFrozenClockExtension;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.Collections;
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
 * 批次效期拦截集成测试（RC-R1.20 / P1-RC-031，UC-INV-06 ④）。
 *
 * <p>覆盖 8 组场景：① 过期+批次管控+出库移动单拒绝（整笔回滚 + reserved/balance 不变）；
 * ⑦ 两步流（CRUD save DRAFT → confirm(moveId)）拒绝后移动单保持 DRAFT；② null expiryDate 通过；
 * ③ 未来效期通过；④ 非批次管控物料通过；⑤ config 放行（{@code erp-inv.batch-expiry-check-enabled=false}）；
 * ⑥ 负库存配置不豁免过期拦截；⑧ INCOMING 移动单不拦截（守卫类型范围边界）。
 *
 * <p>时间冻结在 {@link InvFrozenClockExtension#REFERENCE_DATE}（2026-07-17），保证过期/未过期
 * 日期判定与快照确定性。种子数据经 {@code ormTemplate.runInSession} + daoProvider 显式 ID 落库
 * （对齐 {@code TestErpInvDashboard} seed 范式）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvBatchExpiryInterception extends JunitAutoTestCase {

    @RegisterExtension
    static InvFrozenClockExtension frozenClock = new InvFrozenClockExtension();

    static final Long ORG_ID = 1001L;
    static final Long WAREHOUSE_ID = 3002L;
    static final Long LOCATION_ID = 4002L;
    static final Long UOM_ID = 5002L;
    static final Long CURRENCY_ID = 6002L;
    static final Long ACCT_SCHEMA_ID = 7002L;

    static final Long MATERIAL_BATCH = 2002L;          // isBatchManaged=true，批次已过期
    static final Long MATERIAL_BATCH_NULL = 2102L;     // isBatchManaged=true，批次 expiryDate=null
    static final Long MATERIAL_BATCH_FUTURE = 2202L;   // isBatchManaged=true，批次未来效期
    static final Long MATERIAL_NON_BATCH = 2302L;      // isBatchManaged=false，批次已过期
    static final Long MATERIAL_INCOMING = 2402L;       // isBatchManaged=true，批次已过期（INCOMING 边界）

    static final String BATCH_EXPIRED = "BAT-EXPIRED-01";
    static final String BATCH_NULL = "BAT-NULL-01";
    static final String BATCH_FUTURE = "BAT-FUTURE-01";
    static final String BATCH_NON_BATCH = "BAT-NB-01";
    static final String BATCH_INCOMING = "BAT-INC-01";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- ① 过期 + 批次管控 + 出库移动单 → ERR_BATCH_EXPIRED，整笔回滚，reserved/balance 不变 ----------

    @Test
    public void testExpiredBatchRejectedOnConfirm() {
        seedBatchLedger(BATCH_EXPIRED, MATERIAL_BATCH, CoreMetrics.currentDate().minusDays(1));

        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-EXP-001", MATERIAL_BATCH, new BigDecimal("5"), BATCH_EXPIRED));
        assertEquals(ErpInvErrors.ERR_BATCH_EXPIRED.getErrorCode(), resp.getCode(),
                "过期批次 + 批次管控 + 出库移动单应返回 ERR_BATCH_EXPIRED");
        assertTrue(resp.getMsg() != null && resp.getMsg().contains(BATCH_EXPIRED),
                "错误消息应含批次号参数: " + resp.getMsg());
        assertTrue(resp.getMsg() != null && resp.getMsg().contains(String.valueOf(MATERIAL_BATCH)),
                "错误消息应含物料参数: " + resp.getMsg());
        output("1_rejection_code.json5", resp.getCode());

        ErpInvStockMove move = findMove("SALES_SHIP", "SS-EXP-001");
        assertNull(move, "过期拦截整笔回滚，不应残留移动单");

        ErpInvStockBalance balance = findBalance(MATERIAL_BATCH, BATCH_EXPIRED);
        assertNull(balance, "拒绝路径不进入 applyReservation/upsertBalance，不应产生余额行");
    }

    // ---------- ⑦ 两步流：CRUD save DRAFT → confirm(moveId) 拒绝后保持 DRAFT + reserved 不变 ----------

    @Test
    public void testTwoStepConfirmRejectedKeepsDraft() {
        seedBatchLedger(BATCH_EXPIRED, MATERIAL_BATCH, CoreMetrics.currentDate().minusDays(1));

        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SS-EXP-TWOSTEP-001");
        headData.put("moveType", ErpInvConstants.MOVE_TYPE_OUTGOING);
        headData.put("businessDate", "2026-07-01");
        headData.put("docStatus", ErpInvConstants.DOC_STATUS_DRAFT);
        headData.put("approveStatus", "UNSUBMITTED");
        headData.put("sourceWarehouseId", WAREHOUSE_ID);
        headData.put("sourceLocationId", LOCATION_ID);
        ApiResponse<?> saved = executeRpc(mutation, "ErpInvStockMove__save",
                ApiRequest.build(Map.of("data", headData)));
        assertEquals(0, saved.getStatus(), "DRAFT 保存应成功");
        String moveId = String.valueOf(((Map<?, ?>) saved.getData()).get("id"));

        Map<String, Object> lineData = new LinkedHashMap<>();
        lineData.put("moveId", moveId);
        lineData.put("lineNo", 1);
        lineData.put("materialId", MATERIAL_BATCH);
        lineData.put("uoMId", UOM_ID);
        lineData.put("quantity", new BigDecimal("5"));
        lineData.put("batchNo", BATCH_EXPIRED);
        ApiResponse<?> lineSaved = executeRpc(mutation, "ErpInvStockMoveLine__save",
                ApiRequest.build(Map.of("data", lineData)));
        assertEquals(0, lineSaved.getStatus(), "行保存应成功");

        ApiResponse<?> confirmResp = executeRpc(mutation, "ErpInvStockMove__confirm",
                ApiRequest.build(Map.of("moveId", moveId)));
        assertEquals(ErpInvErrors.ERR_BATCH_EXPIRED.getErrorCode(), confirmResp.getCode(),
                "confirm 应返回 ERR_BATCH_EXPIRED");
        output("1_confirm_rejection_code.json5", confirmResp.getCode());

        ErpInvStockMove move = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(Long.parseLong(moveId));
        assertNotNull(move, "两步流 DRAFT 已落库，confirm 拒绝后移动单应保留");
        assertEquals(ErpInvConstants.DOC_STATUS_DRAFT, move.getDocStatus(),
                "confirm 拒绝后移动单应保持 DRAFT（applyReservation 未执行）");

        ErpInvStockBalance balance = findBalance(MATERIAL_BATCH, BATCH_EXPIRED);
        assertNull(balance, "拒绝路径未进入 applyReservation，不应占用预留量/产生余额行");
    }

    // ---------- ② null expiryDate → 通过（A4.2.78 null 语义：跳过拦截） ----------

    @Test
    public void testNullExpiryDateAllowed() {
        seedBatchLedger(BATCH_NULL, MATERIAL_BATCH_NULL, null);
        seedBalance(MATERIAL_BATCH_NULL, BATCH_NULL);

        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-NULL-001", MATERIAL_BATCH_NULL, new BigDecimal("5"), BATCH_NULL));
        assertEquals(0, resp.getStatus(), "expiryDate=null 应跳过拦截");
        ErpInvStockMove move = findMove("SALES_SHIP", "SS-NULL-001");
        assertNotNull(move, "null 效期批次出库应成功（移动单存在）");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "业务联动出库应推进至 DONE");

        ErpInvStockBalance balance = findBalance(MATERIAL_BATCH_NULL, BATCH_NULL);
        assertNotNull(balance);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("95")), "出库 5 后 total=95");
        assertEquals(0, balance.getReservedQuantity().compareTo(BigDecimal.ZERO), "DONE 后 reserved 释放=0");
        output("1_balance_state.json5", balanceState(balance));
    }

    // ---------- ③ 未来效期 → 通过 ----------

    @Test
    public void testFutureExpiryAllowed() {
        seedBatchLedger(BATCH_FUTURE, MATERIAL_BATCH_FUTURE, CoreMetrics.currentDate().plusDays(30));
        seedBalance(MATERIAL_BATCH_FUTURE, BATCH_FUTURE);

        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-FUT-001", MATERIAL_BATCH_FUTURE, new BigDecimal("5"), BATCH_FUTURE));
        assertEquals(0, resp.getStatus(), "未过期批次应通过");
        ErpInvStockMove move = findMove("SALES_SHIP", "SS-FUT-001");
        assertNotNull(move, "未来效期批次出库应成功");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus());

        ErpInvStockBalance balance = findBalance(MATERIAL_BATCH_FUTURE, BATCH_FUTURE);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("95")), "出库 5 后 total=95");
    }

    // ---------- ④ 非批次管控物料（isBatchManaged=false）→ 通过（L1「物料.批次管控 == 强制」条件） ----------

    @Test
    public void testNonBatchManagedMaterialAllowed() {
        seedBatchLedger(BATCH_NON_BATCH, MATERIAL_NON_BATCH, CoreMetrics.currentDate().minusDays(1), false);
        seedBalance(MATERIAL_NON_BATCH, BATCH_NON_BATCH);

        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-NB-001", MATERIAL_NON_BATCH, new BigDecimal("5"), BATCH_NON_BATCH));
        assertEquals(0, resp.getStatus(), "非批次管控物料即使批次过期也应放行");
        ErpInvStockMove move = findMove("SALES_SHIP", "SS-NB-001");
        assertNotNull(move, "非批次管控物料出库应成功");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus());

        ErpInvStockBalance balance = findBalance(MATERIAL_NON_BATCH, BATCH_NON_BATCH);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("95")), "出库 5 后 total=95");
    }

    // ---------- ⑤ config 放行（batch-expiry-check-enabled=false）→ 过期放行 ----------

    @Test
    public void testConfigDisabledAllowsExpired() {
        seedBatchLedger(BATCH_EXPIRED, MATERIAL_BATCH, CoreMetrics.currentDate().minusDays(1));
        seedBalance(MATERIAL_BATCH, BATCH_EXPIRED);
        setBatchExpiryCheck(false);
        try {
            ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-CFG-001", MATERIAL_BATCH, new BigDecimal("5"), BATCH_EXPIRED));
            assertEquals(0, resp.getStatus(), "config=false 时过期批次放行");
            ErpInvStockMove move = findMove("SALES_SHIP", "SS-CFG-001");
            assertNotNull(move, "放行后移动单存在");
            assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus());

            ErpInvStockBalance balance = findBalance(MATERIAL_BATCH, BATCH_EXPIRED);
            assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("95")), "放行后正常扣减 total=95");
            output("1_balance_state.json5", balanceState(balance));
        } finally {
            setBatchExpiryCheck(true);
        }
    }

    // ---------- ⑥ allow-negative-stock=true 下过期仍拒绝（合规门禁不豁免） ----------

    @Test
    public void testExpiredRejectedDespiteNegativeStock() {
        seedBatchLedger(BATCH_EXPIRED, MATERIAL_BATCH, CoreMetrics.currentDate().minusDays(1));
        setNegativeStock(true);
        try {
            ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-NEGEXP-001", MATERIAL_BATCH, new BigDecimal("5"), BATCH_EXPIRED));
            assertEquals(ErpInvErrors.ERR_BATCH_EXPIRED.getErrorCode(), resp.getCode(),
                    "负库存配置不豁免过期拦截（合规门禁）");
            output("1_rejection_code.json5", resp.getCode());

            ErpInvStockMove move = findMove("SALES_SHIP", "SS-NEGEXP-001");
            assertNull(move, "过期拦截整笔回滚");
            ErpInvStockBalance balance = findBalance(MATERIAL_BATCH, BATCH_EXPIRED);
            assertNull(balance, "拒绝路径不产生余额行");
        } finally {
            setNegativeStock(false);
        }
    }

    // ---------- ⑧ INCOMING 移动单（采购入库型）带过期批次 → 通过（守卫类型范围边界） ----------

    @Test
    public void testIncomingMoveWithExpiredBatchAllowed() {
        seedBatchLedger(BATCH_INCOMING, MATERIAL_INCOMING, CoreMetrics.currentDate().minusDays(1));
        seedBalance(MATERIAL_INCOMING, BATCH_INCOMING);

        ApiResponse<?> resp = genMove(incomingReq("PUR_RECEIPT", "PR-INC-001", MATERIAL_INCOMING, new BigDecimal("5"),
                new BigDecimal("10"), BATCH_INCOMING));
        assertEquals(0, resp.getStatus(), "INCOMING 移动单不在守卫类型范围（出库/内部转移），应放行");
        ErpInvStockMove move = findMove("PUR_RECEIPT", "PR-INC-001");
        assertNotNull(move, "入库移动单应成功");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus());

        ErpInvStockBalance balance = findBalance(MATERIAL_INCOMING, BATCH_INCOMING);
        assertNotNull(balance, "入库应写余额行");
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("105")), "入库 5 后 total=105");
    }

    // ---------- seed helpers ----------

    private void seedBatchLedger(String batchNo, Long materialId, java.time.LocalDate expiry) {
        seedBatchLedger(batchNo, materialId, expiry, true);
    }

    private void seedBatchLedger(String batchNo, Long materialId, java.time.LocalDate expiry, boolean batchManaged) {
        ormTemplate.runInSession(() -> {
            seedUoM();
            seedWarehouse();
            seedLocation();
            seedMaterial(materialId, batchManaged);
            IEntityDao<ErpInvBatch> dao = daoProvider.daoFor(ErpInvBatch.class);
            ErpInvBatch b = dao.newEntity();
            b.setOrgId(ORG_ID);
            b.setBatchNo(batchNo);
            b.setMaterialId(materialId);
            b.setWarehouseId(WAREHOUSE_ID);
            b.setTotalQuantity(new BigDecimal("100"));
            b.setAvailableQuantity(new BigDecimal("100"));
            b.setProductionDate(CoreMetrics.currentDate().minusDays(30));
            b.setExpiryDate(expiry);
            b.setStatus("OPEN");
            dao.saveEntity(b);
        });
    }

    private void seedMaterial(Long id, boolean batchManaged) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("M-" + id);
        m.setName("Material " + id);
        m.setMaterialType("GOODS");
        m.setUoMId(UOM_ID);
        m.setStatus("ACTIVE");
        m.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        m.setIsBatchManaged(batchManaged);
        dao.saveEntity(m);
    }

    private void seedUoM() {
        IEntityDao<ErpMdUoM> dao = daoProvider.daoFor(ErpMdUoM.class);
        if (dao.getEntityById(UOM_ID) != null) {
            return;
        }
        ErpMdUoM u = dao.newEntity();
        u.orm_propValue(1, UOM_ID);
        u.setCode("PCS");
        u.setName("个");
        dao.saveEntity(u);
    }

    private void seedWarehouse() {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        if (dao.getEntityById(WAREHOUSE_ID) != null) {
            return;
        }
        ErpMdWarehouse w = dao.newEntity();
        w.orm_propValue(1, WAREHOUSE_ID);
        w.setCode("WH-" + WAREHOUSE_ID);
        w.setName("测试仓");
        w.setStatus("ACTIVE");
        dao.saveEntity(w);
    }

    private void seedLocation() {
        IEntityDao<ErpMdLocation> dao = daoProvider.daoFor(ErpMdLocation.class);
        if (dao.getEntityById(LOCATION_ID) != null) {
            return;
        }
        ErpMdLocation l = dao.newEntity();
        l.orm_propValue(1, LOCATION_ID);
        l.setCode("LOC-" + LOCATION_ID);
        l.setName("测试库位");
        l.setWarehouseId(WAREHOUSE_ID);
        dao.saveEntity(l);
    }

    private void seedBalance(Long materialId, String batchNo) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = dao.newEntity();
            b.setOrgId(ORG_ID);
            b.setMaterialId(materialId);
            b.setWarehouseId(WAREHOUSE_ID);
            b.setLocationId(LOCATION_ID);
            b.setBatchNo(batchNo);
            b.setTotalQuantity(new BigDecimal("100"));
            b.setReservedQuantity(BigDecimal.ZERO);
            b.setLockedQuantity(BigDecimal.ZERO);
            b.setAvailableQuantity(new BigDecimal("100"));
            b.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
            b.setAvgCost(new BigDecimal("10"));
            b.setTotalCost(new BigDecimal("1000"));
            b.setCurrencyId(CURRENCY_ID);
            dao.saveEntity(b);
        });
    }

    // ---------- move request helpers（镜像 TestErpInvStockMoveBookkeeping） ----------

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private Map<String, Object> outgoingReq(String billType, String billCode, Long materialId,
                                            BigDecimal qty, String batchNo) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", billType);
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, null, batchNo)));
        return req;
    }

    private Map<String, Object> incomingReq(String billType, String billCode, Long materialId,
                                            BigDecimal qty, BigDecimal unitCost, String batchNo) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", billType);
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, unitCost, batchNo)));
        return req;
    }

    private Map<String, Object> baseReq(String moveType) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", moveType);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        return req;
    }

    private Map<String, Object> line(Long materialId, BigDecimal qty, BigDecimal unitCost, String batchNo) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        if (unitCost != null) {
            line.put("unitCost", unitCost);
        }
        line.put("currencyId", CURRENCY_ID);
        if (batchNo != null) {
            line.put("batchNo", batchNo);
        }
        return line;
    }

    // ---------- query helpers ----------

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpInvStockBalance findBalance(Long materialId, String batchNo) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        q.addFilter(eq("batchNo", batchNo));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Map<String, Object> balanceState(ErpInvStockBalance b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalQuantity", b.getTotalQuantity());
        m.put("availableQuantity", b.getAvailableQuantity());
        m.put("reservedQuantity", b.getReservedQuantity());
        m.put("lockedQuantity", b.getLockedQuantity());
        m.put("avgCost", b.getAvgCost());
        m.put("totalCost", b.getTotalCost());
        return m;
    }

    private void setBatchExpiryCheck(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_BATCH_EXPIRY_CHECK_ENABLED, String.valueOf(value));
    }

    private void setNegativeStock(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(value));
    }
}
