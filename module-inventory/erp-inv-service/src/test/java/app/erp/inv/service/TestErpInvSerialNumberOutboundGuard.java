package app.erp.inv.service;

import app.erp.inv.dao.ErpInvDaoConstants;
import app.erp.inv.dao.entity.ErpInvSerialNumber;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * P2-CK-inv-012-r3：序列号出库状态守卫 + IN_STOCK→OUT 翻转 writer
 * （{@code docs/design/inventory/state-machine.md} §异常路径「序列号已售：出库时校验序列号状态；
 * 已售序列号拒绝再次出库」+ README §关键业务规则 6「序列号未售」）。
 *
 * <p>修复前 {@code validateBatchSerialPresence} 仅 presence 校验（已售序列号放行再次出库）、
 * {@code ErpInvSerialNumberBizModel} 为 CRUD 桩（出库后状态滞留 IN_STOCK，OUT 字典值零 writer）。
 *
 * <p>fixture 为 case 级直接落库（序列管控物料 + 序列台账行），不触 {@code _init-data/}；
 * 非序列管控物料控制组断言 presence 既有短路语义保持。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvSerialNumberOutboundGuard extends JunitAutoTestCase {

    static final String ORG_ID = "1001";
    static final String WAREHOUSE_ID = "3002";
    static final String LOCATION_ID = "4002";
    static final String UOM_ID = "5002";
    static final String CURRENCY_ID = "6002";
    static final String ACCT_SCHEMA_ID = "7002";

    static final String MATERIAL_SERIAL = "2002";        // isSerialManaged=true
    static final String MATERIAL_NON_SERIAL = "2302";    // isSerialManaged=false（控制组）

    static final String SERIAL_SOLD = "SN-SOLD-01";      // 已售 OUT
    static final String SERIAL_NO_STATUS = "SN-RSV-01";  // 已预留 RESERVED（非在库负路径）
    static final String SERIAL_IN_STOCK = "SN-IN-01";    // 在库（正路径翻转）

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- (a) 负路径：已售序列号拒绝再次出库 ----------

    @Test
    public void testSoldSerialRejectedOnOutgoing() {
        seedSerialLedger(SERIAL_SOLD, MATERIAL_SERIAL, ErpInvDaoConstants.SERIAL_STATUS_OUT);
        seedBalance(MATERIAL_SERIAL);

        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-SN-SOLD-001", MATERIAL_SERIAL,
                new BigDecimal("1"), SERIAL_SOLD));
        assertEquals(ErpInvErrors.ERR_SERIAL_NOT_IN_STOCK.getErrorCode(), resp.getCode(),
                "已售（OUT）序列号拒绝再次出库，修复前 presence 校验放行（status=0）");

        ErpInvStockMove move = findMove("SALES_SHIP", "SS-SN-SOLD-001");
        assertNull(move, "拒绝路径整笔回滚，不应残留移动单");
        ErpInvStockBalance balance = findBalance(MATERIAL_SERIAL);
        assertNotNull(balance, "既有余额行保持");
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("100")),
                "拒绝路径余额不扣减");
    }

    // ---------- (a) 负路径：非在库序列号拒绝出库（RESERVED 已预留） ----------

    @Test
    public void testReservedSerialRejectedOnOutgoing() {
        // 注：serial-status 列 DB 层 NOT NULL，「缺状态=null」不可持久化；非 IN_STOCK 负路径以字典 RESERVED 覆盖
        seedSerialLedger(SERIAL_NO_STATUS, MATERIAL_SERIAL, ErpInvDaoConstants.SERIAL_STATUS_RESERVED);
        seedBalance(MATERIAL_SERIAL);

        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-SN-NOST-001", MATERIAL_SERIAL,
                new BigDecimal("1"), SERIAL_NO_STATUS));
        assertEquals(ErpInvErrors.ERR_SERIAL_NOT_IN_STOCK.getErrorCode(), resp.getCode(),
                "非在库（RESERVED≠IN_STOCK）序列号拒绝出库");
        assertNull(findMove("SALES_SHIP", "SS-SN-NOST-001"), "拒绝路径整笔回滚");
    }

    // ---------- (b) 正路径：IN_STOCK 序列号出库成功后状态翻转为 OUT（同事务） ----------

    @Test
    public void testInStockSerialOutboundFlipsToOut() {
        seedSerialLedger(SERIAL_IN_STOCK, MATERIAL_SERIAL, ErpInvDaoConstants.SERIAL_STATUS_IN_STOCK);
        seedBalance(MATERIAL_SERIAL);

        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-SN-OUT-001", MATERIAL_SERIAL,
                new BigDecimal("1"), SERIAL_IN_STOCK));
        assertEquals(0, resp.getStatus(), "在库序列号出库应成功: " + resp.getMsg());

        ErpInvStockMove move = findMove("SALES_SHIP", "SS-SN-OUT-001");
        assertNotNull(move, "出库移动单存在");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "出库推进至 DONE");

        // 翻转 writer：IN_STOCK→OUT 同事务翻转 + 出库单号回链（修复前 CRUD 桩无翻转，状态滞留 IN_STOCK）
        ErpInvSerialNumber sn = findSerial(SERIAL_IN_STOCK, MATERIAL_SERIAL);
        assertNotNull(sn, "序列台账行存在");
        assertEquals(ErpInvDaoConstants.SERIAL_STATUS_OUT, sn.getStatus(),
                "出库记账同事务序列号状态应翻转为 OUT");
        assertEquals("SALES_SHIP", sn.getOutBillType(), "出库单类型回链");
        assertEquals("SS-SN-OUT-001", sn.getOutBillCode(), "出库单号回链");

        ErpInvStockBalance balance = findBalance(MATERIAL_SERIAL);
        assertNotNull(balance);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("99")), "出库 1 后 total=99");
    }

    // ---------- 控制组：非序列管控物料路径行为不变（presence 校验既有短路语义保持） ----------

    @Test
    public void testNonSerialManagedMaterialUnaffected() {
        seedSerialLedger("SN-NONMGT-01", MATERIAL_NON_SERIAL, ErpInvDaoConstants.SERIAL_STATUS_OUT);
        seedBalance(MATERIAL_NON_SERIAL);

        ApiResponse<?> resp = genMove(outgoingReq("SALES_SHIP", "SS-SN-NONMGT-001", MATERIAL_NON_SERIAL,
                new BigDecimal("1"), null));
        assertEquals(0, resp.getStatus(), "非序列管控物料不校验序列台账（管控关闭路径行为不变）");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE,
                findMove("SALES_SHIP", "SS-SN-NONMGT-001").getDocStatus());
    }

    // ---------- seed helpers ----------

    private void seedSerialLedger(String serialNo, String materialId, String status) {
        ormTemplate.runInSession(() -> {
            seedUoM();
            seedWarehouse();
            seedLocation();
            seedMaterial(materialId);
            IEntityDao<ErpInvSerialNumber> dao = daoProvider.daoFor(ErpInvSerialNumber.class);
            ErpInvSerialNumber sn = dao.newEntity();
            sn.setOrgId(ORG_ID);
            sn.setSerialNo(serialNo);
            sn.setMaterialId(materialId);
            sn.setWarehouseId(WAREHOUSE_ID);
            sn.setStatus(status);
            dao.saveEntity(sn);
        });
    }

    private void seedMaterial(String id) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("M-" + id);
        m.setName("Material " + id);
        m.setMaterialType("GOODS");
        m.setUoMId(UOM_ID);
        m.setStatus("ACTIVE");
        m.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        m.setIsSerialManaged(MATERIAL_SERIAL.equals(id));
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

    private void seedBalance(String materialId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = dao.newEntity();
            b.setOrgId(ORG_ID);
            b.setMaterialId(materialId);
            b.setWarehouseId(WAREHOUSE_ID);
            b.setLocationId(LOCATION_ID);
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

    // ---------- move request helpers（镜像 TestErpInvBatchExpiryInterception） ----------

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private Map<String, Object> outgoingReq(String billType, String billCode, String materialId,
                                            BigDecimal qty, String serialNo) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", billType);
        req.put("relatedBillCode", billCode);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("currencyId", CURRENCY_ID);
        if (serialNo != null) {
            line.put("serialNo", serialNo);
        }
        req.put("lines", Collections.singletonList(line));
        return req;
    }

    // ---------- query helpers ----------

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpInvSerialNumber findSerial(String serialNo, String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("serialNo", serialNo));
        q.addFilter(eq("materialId", materialId));
        List<ErpInvSerialNumber> list = daoProvider.daoFor(ErpInvSerialNumber.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockBalance findBalance(String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
