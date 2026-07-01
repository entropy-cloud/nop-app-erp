package app.erp.inv.service;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
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
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：不可变流水 + 余额驱动（移动加权平均）+ 可用量校验 + 负库存配置。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvStockMoveBookkeeping extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();


    static final Long ORG_ID = 1001L;
    static final Long MATERIAL_ID = 2002L;
    static final Long WAREHOUSE_ID = 3002L;
    static final Long LOCATION_ID = 4002L;
    static final Long UOM_ID = 5002L;
    static final Long CURRENCY_ID = 6002L;
    static final Long ACCT_SCHEMA_ID = 7002L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @Test
    public void testCompleteWritesImmutableLedger() {
        ErpInvStockMove move = generateIncoming("PR-LEDG-001", new BigDecimal("10"), new BigDecimal("5"));

        List<ErpInvStockLedger> ledgers = findLedgers(move.getId());
        assertEquals(1, ledgers.size(), "应写 1 条不可变流水");
        ErpInvStockLedger ledger = ledgers.get(0);
        assertEquals(0, ledger.getQuantity().compareTo(new BigDecimal("10")), "入库 quantity 为正 10");
        assertEquals(0, ledger.getUnitCost().compareTo(new BigDecimal("5")), "单位成本 5");
        assertEquals(0, ledger.getTotalCost().compareTo(new BigDecimal("50")), "总成本 50");
        assertEquals(0, ledger.getBalanceQuantity().compareTo(new BigDecimal("10")),
                "结存快照 balanceQuantity=10");
        assertEquals(0, ledger.getBalanceTotalCost().compareTo(new BigDecimal("50")),
                "结存快照 balanceTotalCost=50");
        assertNotEquals(null, ledger.getCode(), "流水号非空");

        // 不可变：状态机禁止 DONE 后再 complete，故不会产生第二条流水
        assertThrows(NopException.class, () -> stockMoveBiz.complete(move.getId(), CTX));
        assertEquals(1, findLedgers(move.getId()).size(), "DONE 后不可再写流水");
    }

    @Test
    public void testIncomingUpdatesBalanceAvgCost() {
        generateIncoming("PR-AVG-001", new BigDecimal("10"), new BigDecimal("6"));
        ErpInvStockBalance balance = findBalance();
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("10")), "totalQty=10");
        assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("6")), "avgCost=6");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("60")), "totalCost=60");

        // 第二次入库不同单价 → 移动加权平均
        generateIncoming("PR-AVG-002", new BigDecimal("10"), new BigDecimal("8"));
        ErpInvStockBalance updated = findBalance();
        assertEquals(0, updated.getTotalQuantity().compareTo(new BigDecimal("20")), "totalQty=20");
        assertEquals(0, updated.getAvgCost().compareTo(new BigDecimal("7")), "avgCost=(60+80)/20=7");
        assertEquals(0, updated.getTotalCost().compareTo(new BigDecimal("140")), "totalCost=140");
    }

    @Test
    public void testOutgoingDeductsBalance() {
        // 先入库 20 @ 10 = 200
        generateIncoming("PR-OUT-001", new BigDecimal("20"), new BigDecimal("10"));
        // 再出库 8 → 按 avgCost=10 扣减
        generateOutgoing("SS-OUT-001", new BigDecimal("8"));

        ErpInvStockBalance balance = findBalance();
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("12")), "totalQty=20-8=12");
        assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("10")), "avgCost 不变 10");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("120")), "totalCost=12*10=120");

        List<ErpInvStockLedger> outLedgers = daoProvider.daoFor(ErpInvStockLedger.class).findAllByQuery(
                new QueryBean());
        ErpInvStockLedger outLedger = outLedgers.stream()
                .filter(l -> l.getQuantity().signum() < 0).findFirst().orElseThrow();
        assertEquals(0, outLedger.getUnitCost().compareTo(new BigDecimal("10")),
                "出库流水 unitCost 快照=当前 avgCost 10");
        assertEquals(0, outLedger.getQuantity().compareTo(new BigDecimal("-8")), "出库 quantity 负 -8");
    }

    @Test
    public void testConfirmInsufficientAvailableRejected() {
        // 无库存直接出库 → 可用量 0 < 需要 5
        NopException ex = assertThrows(NopException.class,
                () -> generateOutgoing("SS-INSUF-001", new BigDecimal("5")));
        assertEquals(ErpInvErrors.ERR_AVAILABLE_INSUFFICIENT.getErrorCode(), ex.getErrorCode(),
                "可用量不足应抛 ERR_AVAILABLE_INSUFFICIENT");

        // 拒绝后：移动单未推进到 DONE（仍 DRAFT，整个业务审核回滚由调用方事务保证；本会话内单据停在 DRAFT）
        ErpInvStockMove move = findMove("SALES_SHIP", "SS-INSUF-001");
        assertNotNull(move, "DRAFT 移动单已建（推进被拒）");
        assertEquals(ErpInvConstants.DOC_STATUS_DRAFT, move.getDocStatus(), "拒绝后移动单不应推进到 DONE");

        ErpInvStockBalance balance = findBalance();
        assertTrue(balance == null
                        || balance.getReservedQuantity().compareTo(BigDecimal.ZERO) == 0,
                "拒绝不应增加预留量");
    }

    @Test
    public void testNegativeStockConfigAllowsShortage() {
        setNegativeStock(true);
        try {
            // allow-negative-stock=true → 不足仍可确认并完成
            ErpInvStockMove move = generateOutgoing("SS-NEG-001", new BigDecimal("5"));
            assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus(), "负库存放行应完成");

            ErpInvStockBalance balance = findBalance();
            assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("-5")),
                    "totalQty 允许为负 -5");
        } finally {
            setNegativeStock(false);
        }
    }

    // ---------- helpers ----------

    private ErpInvStockMove generateIncoming(String billCode, BigDecimal qty, BigDecimal unitCost) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpInvConstants.MOVE_TYPE_INCOMING);
        request.setOrgId(ORG_ID);
        request.setBusinessDate(LocalDate.of(2026, 7, 1));
        request.setDestWarehouseId(WAREHOUSE_ID);
        request.setDestLocationId(LOCATION_ID);
        request.setAcctSchemaId(ACCT_SCHEMA_ID);
        request.setCurrencyId(CURRENCY_ID);
        request.setRelatedBillType("PUR_RECEIPT");
        request.setRelatedBillCode(billCode);
        request.setLines(Collections.singletonList(line(qty, unitCost)));
        return stockMoveBiz.generateMove(request, CTX);
    }

    private ErpInvStockMove generateOutgoing(String billCode, BigDecimal qty) {
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpInvConstants.MOVE_TYPE_OUTGOING);
        request.setOrgId(ORG_ID);
        request.setBusinessDate(LocalDate.of(2026, 7, 1));
        request.setSourceWarehouseId(WAREHOUSE_ID);
        request.setSourceLocationId(LOCATION_ID);
        request.setAcctSchemaId(ACCT_SCHEMA_ID);
        request.setCurrencyId(CURRENCY_ID);
        request.setRelatedBillType("SALES_SHIP");
        request.setRelatedBillCode(billCode);
        request.setLines(Collections.singletonList(line(qty, null)));
        return stockMoveBiz.generateMove(request, CTX);
    }

    private StockMoveLineRequest line(BigDecimal qty, BigDecimal unitCost) {
        StockMoveLineRequest line = new StockMoveLineRequest();
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitCost(unitCost);
        line.setCurrencyId(CURRENCY_ID);
        return line;
    }

    private ErpInvStockBalance findBalance() {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", MATERIAL_ID));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvStockLedger> findLedgers(Long moveId) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q);
    }

    private long countMoves(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        return daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q).size();
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void setNegativeStock(boolean value) {
        io.nop.api.core.config.AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(value));
    }
}
