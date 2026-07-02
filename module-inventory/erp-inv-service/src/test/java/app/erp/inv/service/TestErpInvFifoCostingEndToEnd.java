package app.erp.inv.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 端到端集成测试：FIFO 全链（入库 cost layer → 出库 COGS → SALES_OUTPUT 过账）+ period-close 兜底重算。
 *
 * <p>覆盖 plan Phase 3 退出标准：采购入库 FIFO 物料建 cost layer；销售出库跨层消耗 COGS 经 ledger.totalCost
 * 流入 InvPostingDispatcher.TOTAL_COST 并生成 SALES_OUTPUT 凭证；{@code reclosePeriodCosts} 对正常数据为 no-op、
 * 对缺失成本层的入库兜底补建。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvFifoCostingEndToEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long WAREHOUSE_ID = 3301L;
    static final Long LOCATION_ID = 4301L;
    static final Long UOM_ID = 5301L;
    static final Long CURRENCY_ID = 6301L;
    static final Long ACCT_SCHEMA_ID = 7301L;
    static final String PERIOD_CODE = "2026-07";
    static final int VOUCHER_STATUS_POSTED = 20;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testFifoEndToEndCostLayerCogsAndPosting() {
        seedFifoMaterial(2301L);
        seedPeriodAndSubjects();

        // 入库 20@10 + 40@12 → 两个 cost layer
        generateIncoming(2301L, "PR-E2E-001", new BigDecimal("20"), new BigDecimal("10"));
        generateIncoming(2301L, "PR-E2E-002", new BigDecimal("40"), new BigDecimal("12"));

        List<ErpInvCostLayer> layers = findCostLayers(2301L);
        assertEquals(2, layers.size(), "两入库建两 cost layer");

        // 销售出库 60 → FIFO 跨层消耗 20@10 + 40@12 = 680
        Long outMoveId = generateOutgoing(2301L, "SS-E2E-001", new BigDecimal("60"));

        ErpInvStockLedger outLedger = findOutgoingLedger(2301L);
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-680")),
                "FIFO 跨层 COGS=20×10+40×12=680（ledger.totalCost 负号，派发器 .abs() 拾取）");
        assertEquals(ErpInvConstants.COST_METHOD_FIFO, outLedger.getCostMethod(), "流水 costMethod=FIFO");

        // SALES_OUTPUT 过账：TOTAL_COST 来自 ledger.totalCost.abs()=680
        ErpInvStockMove outMove = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(outMoveId);
        assertEquals(true, outMove.getPosted(), "销售出库 DONE 应 SALES_OUTPUT 过账 posted=true");
        ErpFinVoucher outVoucher = findVoucherByMoveCode(outMove.getCode());
        assertNotNull(outVoucher, "SALES_OUTPUT 凭证应落库");
        assertEquals(VOUCHER_STATUS_POSTED, outVoucher.getDocStatus(), "凭证已过账");
        assertTrue(outVoucher.getTotalDebit().compareTo(new BigDecimal("680")) == 0,
                "SALES_OUTPUT 借方（主营业务成本）=680");
        assertTrue(outVoucher.getTotalCredit().compareTo(new BigDecimal("680")) == 0,
                "SALES_OUTPUT 贷方（库存商品）=680");
    }

    @Test
    public void testReclosePeriodCostsNormalDataIsNoOp() {
        seedFifoMaterial(2302L);
        seedPeriodAndSubjects();
        generateIncoming(2302L, "PR-E2E-NOOP-001", new BigDecimal("20"), new BigDecimal("10"));

        Map<String, Object> report = reclosePeriodCosts();

        assertTrue(((Number) report.get("scannedMoves")).intValue() >= 1, "应扫描到本期 DONE 移动单");
        assertEquals(0, ((Number) report.get("recomputedIncomingLayers")).intValue(),
                "正常数据成本层已建，兜底不应补算入库层");
    }

    @Test
    public void testReclosePeriodCostsRebuildsMissingLayer() {
        Long materialId = 2303L;
        seedFifoMaterial(materialId);
        seedPeriodAndSubjects();

        // 模拟异常：关闭成本核算开关期间入库（记账器退化为移动加权平均，不建 cost layer）
        setCostingEnabled(false);
        try {
            Long inMoveId = generateIncoming(materialId, "PR-E2E-REBUILD-001",
                    new BigDecimal("25"), new BigDecimal("8"));
            assertTrue(findCostLayers(materialId).isEmpty(), "成本核算关闭期间入库不应建 cost layer");
        } finally {
            setCostingEnabled(true);
        }

        // 重新开启后兜底重算：应补建缺失的 cost layer
        Map<String, Object> report = reclosePeriodCosts();
        assertEquals(1, ((Number) report.get("recomputedIncomingLayers")).intValue(), "兜底应补建 1 个缺失入库层");

        List<ErpInvCostLayer> rebuilt = findCostLayers(materialId);
        assertEquals(1, rebuilt.size(), "补建后恢复 1 层");
        ErpInvCostLayer layer = rebuilt.get(0);
        assertEquals(0, layer.getIncomingQuantity().compareTo(new BigDecimal("25")), "补建 incomingQuantity=25");
        assertEquals(0, layer.getRemainingQuantity().compareTo(new BigDecimal("25")), "补建 remainingQuantity=25");
        assertEquals(0, layer.getUnitCost().compareTo(new BigDecimal("8")), "补建 unitCost=8（取 ledger.unitCost）");
        assertEquals(ErpInvConstants.COST_METHOD_FIFO, layer.getCostMethod(), "补建 costMethod=FIFO");
    }

    // ---------- move generation ----------

    private Long generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, unitCost)));
        return idOf(genMove(req));
    }

    private Long generateOutgoing(Long materialId, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", "SALES_SHIP");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, null)));
        return idOf(genMove(req));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> reclosePeriodCosts() {
        Long periodId = findPeriodId();
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("periodId", periodId);
        args.put("startDate", "2026-07-01");
        args.put("endDate", "2026-07-31");
        ApiResponse<?> resp = executeRpc(mutation, "ErpInvCosting__reclosePeriodCosts", ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), "reclosePeriodCosts 应成功");
        return (Map<String, Object>) resp.getData();
    }

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private Map<String, Object> baseReq(Long materialId, Integer moveType) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", moveType);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        return req;
    }

    private Map<String, Object> line(Long materialId, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        if (unitCost != null) {
            line.put("unitCost", unitCost);
        }
        line.put("currencyId", CURRENCY_ID);
        return line;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long idOf(ApiResponse<?> resp) {
        Object id = ((Map<?, ?>) resp.getData()).get("id");
        return id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
    }

    // ---------- queries ----------

    private List<ErpInvCostLayer> findCostLayers(Long materialId) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q);
    }

    private ErpInvStockLedger findOutgoingLedger(Long materialId) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream()
                .filter(l -> l.getQuantity() != null && l.getQuantity().signum() < 0)
                .findFirst()
                .orElse(null);
    }

    private ErpFinVoucher findVoucherByMoveCode(String moveCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> moveCode.equals(l.getBillCode()))
                .map(l -> daoProvider.daoFor(ErpFinVoucher.class).getEntityById(l.getVoucherId()))
                .findFirst()
                .orElse(null);
    }

    private Long findPeriodId() {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", PERIOD_CODE));
        List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private void setCostingEnabled(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_COSTING_ENABLED, String.valueOf(value));
    }

    // ---------- seed ----------

    private void seedFifoMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MATF-E2E-" + id);
            material.setName("FIFO Material E2E " + id);
            material.orm_propValueByName("materialType", 10);
            material.setUoMId(UOM_ID);
            material.setStatus(10);
            material.setCostMethod(ErpInvConstants.COST_METHOD_FIFO);
            dao.saveEntity(material);
        });
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod();
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedSubject("6401", "主营业务成本");
        });
    }

    private void seedOpenPeriod() {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(PERIOD_CODE);
        period.setName(PERIOD_CODE);
        period.setOrgId(ORG_ID);
        period.orm_propValueByName("year", 2026);
        period.orm_propValueByName("month", 7);
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        period.orm_propValueByName("status", 10);
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", 10);
        subject.orm_propValueByName("direction", 10);
        subject.orm_propValueByName("status", 10);
        dao.saveEntity(subject);
    }
}
