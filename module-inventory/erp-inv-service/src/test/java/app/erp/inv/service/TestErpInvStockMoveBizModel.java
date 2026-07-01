package app.erp.inv.service;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 服务层集成测试：generateMove 契约 + 状态机 + 幂等。
 *
 * <p>直接调用 {@link IErpInvStockMoveBiz} 的 Java API（不走 GraphQL 快照），断言实体落库状态。
 * 测试自包含：seed 物料/仓库/库位占位 ID（BizModel 不做跨域 ref 校验，余额维度按这些 ID 聚合）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvStockMoveBizModel extends JunitAutoTestCase {

    static final Long ORG_ID = 1001L;
    static final Long MATERIAL_ID = 2001L;
    static final Long WAREHOUSE_ID = 3001L;
    static final Long LOCATION_ID = 4001L;
    static final Long UOM_ID = 5001L;
    static final Long CURRENCY_ID = 6001L;
    static final Long ACCT_SCHEMA_ID = 7001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Test
    public void testGenerateMoveBusinessLinkedAutoCompletes() {
        ErpInvStockMove move = generateIncoming("PUR_RECEIPT", "PR-001", new BigDecimal("10"));

        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(),
                "业务联动应自动推进到 DONE");
        assertEquals(false, move.getPosted(), "未接入过账前 posted=false");
        assertEquals(1, countLines(move.getId()), "应生成 1 行");
    }

    @Test
    public void testGenerateMoveIdempotent() {
        ErpInvStockMove first = generateIncoming("PUR_RECEIPT", "PR-IDEM-001", new BigDecimal("10"));
        ErpInvStockMove second = generateIncoming("PUR_RECEIPT", "PR-IDEM-001", new BigDecimal("10"));

        assertEquals(first.getId(), second.getId(), "同源单重复触发应返回同一移动单");
        assertEquals(1, countMovesByRelatedBill("PUR_RECEIPT", "PR-IDEM-001"),
                "幂等：不应产生第二张移动单");
    }

    @Test
    public void testManualMoveStopsAtConfirmed() {
        StockMoveRequest request = incomingRequest(null, null, new BigDecimal("10"));
        ErpInvStockMove move = stockMoveBiz.generateMove(request);

        assertEquals(ErpInvConstants.DOC_STATUS_CONFIRMED, move.getDocStatus(),
                "独立移动单（无源单）应停在 CONFIRMED");
    }

    @Test
    public void testIllegalTransitionRejected() {
        ErpInvStockMove done = generateIncoming("PUR_RECEIPT", "PR-ILL-001", new BigDecimal("10"));
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, done.getDocStatus());

        assertThrows(NopException.class, () -> stockMoveBiz.confirm(done.getId()),
                "DONE→CONFIRMED 非法迁移应抛 NopException");
    }

    @Test
    public void testCancelReleasesReservation() {
        // 先入库 10 建立库存（业务联动 → DONE，total=10）
        generateIncoming("PUR_RECEIPT", "PR-CANCEL-STOCK", new BigDecimal("10"));

        // 独立出库单（无源单）停在 CONFIRMED：校验可用量（10≥5）通过，占预留 5
        StockMoveRequest manualOut = outgoingRequest(null, null, new BigDecimal("5"));
        ErpInvStockMove manual = stockMoveBiz.generateMove(manualOut);
        assertEquals(ErpInvConstants.DOC_STATUS_CONFIRMED, manual.getDocStatus());

        ErpInvStockBalance reserved = findBalance();
        assertEquals(0, new BigDecimal(reserved.getReservedQuantity()).compareTo(new BigDecimal("5")),
                "CONFIRMED 应占预留 5");
        assertEquals(0, new BigDecimal(reserved.getAvailableQuantity()).compareTo(new BigDecimal("5")),
                "可用量 = total(10) - reserved(5) - locked(0) = 5");

        stockMoveBiz.cancel(manual.getId());
        ErpInvStockBalance released = findBalance();
        assertEquals(0, new BigDecimal(released.getReservedQuantity()).compareTo(BigDecimal.ZERO),
                "CANCELLED 应释放预留");
        assertEquals(0, new BigDecimal(released.getAvailableQuantity()).compareTo(new BigDecimal("10")),
                "释放后可用量恢复为 total(10)");
    }

    @Test
    public void testReverseCreatesReverseMove() {
        ErpInvStockMove original = generateIncoming("PUR_RECEIPT", "PR-REV-001", new BigDecimal("12"));
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, original.getDocStatus());

        ErpInvStockMove reversal = stockMoveBiz.reverse(original.getId());
        assertNotNull(reversal.getId(), "冲销应生成新移动单");
        assertNotEquals(original.getId(), reversal.getId(), "冲销单是新单，非原单");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, reversal.getDocStatus(), "冲销单自动推进到 DONE");
        assertEquals("REVERSAL", reversal.getRelatedBillType(), "冲销单关联原单");
        assertEquals(original.getCode(), reversal.getRelatedBillCode());
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, original.getDocStatus(), "原单保持 DONE（非反审核）");
    }

    // ---------- helpers ----------

    private ErpInvStockMove generateIncoming(String billType, String billCode, BigDecimal qty) {
        return stockMoveBiz.generateMove(incomingRequest(billType, billCode, qty));
    }

    private StockMoveRequest incomingRequest(String billType, String billCode, BigDecimal qty) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpInvConstants.MOVE_TYPE_INCOMING);
        request.setOrgId(ORG_ID);
        request.setBusinessDate(LocalDate.of(2026, 7, 1));
        request.setDestWarehouseId(WAREHOUSE_ID);
        request.setDestLocationId(LOCATION_ID);
        request.setAcctSchemaId(ACCT_SCHEMA_ID);
        request.setCurrencyId(CURRENCY_ID);
        request.setRelatedBillType(billType);
        request.setRelatedBillCode(billCode);
        request.setLines(Collections.singletonList(line(qty)));
        return request;
    }

    private StockMoveRequest outgoingRequest(String billType, String billCode, BigDecimal qty) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpInvConstants.MOVE_TYPE_OUTGOING);
        request.setOrgId(ORG_ID);
        request.setBusinessDate(LocalDate.of(2026, 7, 1));
        request.setSourceWarehouseId(WAREHOUSE_ID);
        request.setSourceLocationId(LOCATION_ID);
        request.setAcctSchemaId(ACCT_SCHEMA_ID);
        request.setCurrencyId(CURRENCY_ID);
        request.setRelatedBillType(billType);
        request.setRelatedBillCode(billCode);
        request.setLines(Collections.singletonList(line(qty)));
        return request;
    }

    private StockMoveLineRequest line(BigDecimal qty) {
        StockMoveLineRequest line = new StockMoveLineRequest();
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setCurrencyId(CURRENCY_ID);
        return line;
    }

    private long countLines(Long moveId) {
        IEntityDao<app.erp.inv.dao.entity.ErpInvStockMoveLine> dao = daoProvider
                .daoFor(app.erp.inv.dao.entity.ErpInvStockMoveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q).size();
    }

    private long countMovesByRelatedBill(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        return daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q).size();
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        java.util.List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
