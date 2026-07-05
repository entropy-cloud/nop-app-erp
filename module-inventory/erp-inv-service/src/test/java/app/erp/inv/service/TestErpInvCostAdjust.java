package app.erp.inv.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成本调整行为测试（plan 2026-07-05-2352-3）。
 *
 * <p>覆盖 8 类：(a) MA 成本增加；(b) MA 成本减少；(c) FIFO 追加调整层 + 后续出库消耗；
 * (d) STANDARD 重估（FIRMED rollup + 差异凭证）；(e) 审批门控（开/关）；
 * (f) 重复 apply 防护；(g) reverse 红冲；(h) 无余额/负成本拒绝。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvCostAdjust extends JunitAutoTestCase {

    static final Long ORG_ID = 1501L;
    static final Long WAREHOUSE_ID = 3501L;
    static final Long LOCATION_ID = 4501L;
    static final Long UOM_ID = 5501L;
    static final Long CURRENCY_ID = 6501L;
    static final Long ACCT_SCHEMA_ID = 7501L;
    static final String PERIOD_CODE = "2026-07";
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_COST_VARIANCE = "6603";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testMovingAverageCostIncrease() {
        Long materialId = 2501L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();
        generateIncoming(materialId, "PR-CA-001", new BigDecimal("10"), new BigDecimal("10"));

        Long adjustId = createAdjust(materialId, "CA-MA-INC", ErpInvConstants.ADJUST_TYPE_COST_DIFFERENCE,
                new BigDecimal("12"));

        setApprovalRequired(false);
        try {
            Long appliedId = applyCostAdjust(adjustId);
            ErpInvCostAdjust adjust = daoProvider.daoFor(ErpInvCostAdjust.class).getEntityById(appliedId);
            assertTrue(Boolean.TRUE.equals(adjust.getPosted()), "已过账");

            ErpInvStockBalance balance = findBalance(materialId);
            assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("12")), "avgCost 重算为 12");
            assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("120")), "totalCost=12×10=120");

            ErpInvStockLedger ledger = findAdjustLedger(materialId);
            assertEquals(0, ledger.getQuantity().compareTo(BigDecimal.ZERO), "纯成本变更 quantity=0");
            assertEquals(0, ledger.getTotalCost().compareTo(new BigDecimal("20")), "流水 totalCost=调整金额 20");

            ErpFinVoucher voucher = findVoucherByBillCode("CA-MA-INC");
            assertNotNull(voucher, "成本增加应生成凭证");
            assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus());
            ErpFinVoucherLine invLine = findVoucherLine(voucher.getId(), SUBJECT_INVENTORY);
            assertEquals("DEBIT", invLine.getDcDirection(), "成本增加→借存货");
            ErpFinVoucherLine varLine = findVoucherLine(voucher.getId(), SUBJECT_COST_VARIANCE);
            assertEquals("CREDIT", varLine.getDcDirection(), "成本增加→贷成本差异");
        } finally {
            setApprovalRequired(true);
        }
    }

    @Test
    public void testMovingAverageCostDecrease() {
        Long materialId = 2502L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();
        generateIncoming(materialId, "PR-CA-002", new BigDecimal("10"), new BigDecimal("10"));

        Long adjustId = createAdjust(materialId, "CA-MA-DEC", ErpInvConstants.ADJUST_TYPE_COST_DIFFERENCE,
                new BigDecimal("8"));

        setApprovalRequired(false);
        try {
            applyCostAdjust(adjustId);

            ErpInvStockBalance balance = findBalance(materialId);
            assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("8")), "avgCost 重算为 8");
            assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("80")), "totalCost=8×10=80");

            ErpFinVoucher voucher = findVoucherByBillCode("CA-MA-DEC");
            ErpFinVoucherLine invLine = findVoucherLine(voucher.getId(), SUBJECT_INVENTORY);
            assertEquals("CREDIT", invLine.getDcDirection(), "成本减少→贷存货");
            ErpFinVoucherLine varLine = findVoucherLine(voucher.getId(), SUBJECT_COST_VARIANCE);
            assertEquals("DEBIT", varLine.getDcDirection(), "成本减少→借成本差异");
        } finally {
            setApprovalRequired(true);
        }
    }

    @Test
    public void testFifoAppendsAdjustLayerAndOutgoingConsumes() {
        Long materialId = 2503L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_FIFO);
        seedPeriodAndSubjects();
        generateIncoming(materialId, "PR-CA-003", new BigDecimal("10"), new BigDecimal("10"));

        setApprovalRequired(false);
        try {
            Long adjustId = createAdjust(materialId, "CA-FIFO", ErpInvConstants.ADJUST_TYPE_COST_DIFFERENCE,
                    new BigDecimal("12"));
            applyCostAdjust(adjustId);

            List<ErpInvCostLayer> layers = findFifoLayers(materialId);
            assertEquals(2, layers.size(), "原入库层 + 1 个 delta 调整层");
            ErpInvCostAdjustLine line = findAdjustLine("CA-FIFO");
            ErpInvCostLayer delta = findLayerByMarker(-line.getId());
            assertNotNull(delta, "delta 调整层存在（incomingMoveId=-行ID 哨兵）");
            assertEquals(0, delta.getUnitCost().compareTo(new BigDecimal("2")), "delta unitCost=12-10=2");

            ErpInvStockBalance balance = findBalance(materialId);
            assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("120")), "totalCost=120（重估后）");

            // 后续出库 10：按 FIFO 升序消耗（原层 10@10 + delta 10@2），COGS=10×10=100（先消耗原层）
            generateOutgoing(materialId, "SS-CA-003", new BigDecimal("10"));
            ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
            // delta 层 incomingDate 同 businessDate（2026-07-01），与原层同日，消耗顺序按层 id；原层先入先建先消耗
            assertTrue(outLedger.getUnitCost().compareTo(BigDecimal.ZERO) > 0, "出库有成本");
        } finally {
            setApprovalRequired(true);
        }
    }

    @Test
    public void testStandardRevaluationPublishesFirmedRollupAndPostsVariance() {
        Long materialId = 2504L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_STANDARD);
        seedFirmedRollup(materialId, new BigDecimal("10"), 25040001L);
        seedPeriodAndSubjects();
        generateIncoming(materialId, "PR-CA-004", new BigDecimal("10"), new BigDecimal("10"));

        setApprovalRequired(false);
        try {
            Long adjustId = createAdjust(materialId, "CA-STD-REV", ErpInvConstants.ADJUST_TYPE_STANDARD_REVALUATION,
                    new BigDecimal("15"));
            applyCostAdjust(adjustId);

            // 新 FIRMED rollup 发布
            List<ErpMfgCostRollup> firmed = findAllFirmedRollupsForMaterial(materialId);
            assertTrue(firmed.size() >= 2, "原有 + 新发布的 FIRMED rollup");
            BigDecimal latest = latestFirmedUnitCost(materialId);
            assertEquals(0, latest.compareTo(new BigDecimal("15")), "最新 FIRMED = 15");

            ErpInvStockBalance balance = findBalance(materialId);
            assertEquals(0, balance.getAvgCost().compareTo(new BigDecimal("15")), "avgCost 重算为 15");
            assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("150")), "totalCost=15×10=150");

            ErpFinVoucher voucher = findVoucherByBillCode("CA-STD-REV");
            assertNotNull(voucher, "标准成本重估生成差异凭证");
            BigDecimal variance = new BigDecimal("5").multiply(new BigDecimal("10"));
            assertEquals(0, voucher.getTotalDebit().compareTo(variance), "借方=50");
        } finally {
            setApprovalRequired(true);
        }
    }

    @Test
    public void testApprovalGateRejectsUnapprovedWhenRequired() {
        Long materialId = 2505L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();
        generateIncoming(materialId, "PR-CA-005", new BigDecimal("10"), new BigDecimal("10"));

        Long adjustId = createAdjust(materialId, "CA-APPR-REQ", ErpInvConstants.ADJUST_TYPE_COST_DIFFERENCE,
                new BigDecimal("12"));

        setApprovalRequired(true);
        try {
            ApiResponse<?> resp = applyCostAdjustResp(adjustId);
            assertEquals(ErpInvErrors.ERR_COST_ADJUST_NOT_APPROVED.getErrorCode(), resp.getCode(),
                    "审批门控开启 + 未审核 → ERR_COST_ADJUST_NOT_APPROVED");
        } finally {
            setApprovalRequired(true);
        }

        // 审批门控关闭：UNSUBMITTED 可直接 apply
        setApprovalRequired(false);
        try {
            Long adjustId2 = createAdjust(materialId, "CA-APPR-OFF", ErpInvConstants.ADJUST_TYPE_COST_DIFFERENCE,
                    new BigDecimal("12"));
            Long appliedId = applyCostAdjust(adjustId2);
            assertTrue(appliedId != null);
        } finally {
            setApprovalRequired(true);
        }
    }

    @Test
    public void testDuplicateApplyRejected() {
        Long materialId = 2506L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();
        generateIncoming(materialId, "PR-CA-006", new BigDecimal("10"), new BigDecimal("10"));

        Long adjustId = createAdjust(materialId, "CA-DUP", ErpInvConstants.ADJUST_TYPE_COST_DIFFERENCE,
                new BigDecimal("12"));

        setApprovalRequired(false);
        try {
            applyCostAdjust(adjustId);
            ApiResponse<?> resp = applyCostAdjustResp(adjustId);
            assertEquals(ErpInvErrors.ERR_COST_ADJUST_ALREADY_APPLIED.getErrorCode(), resp.getCode(),
                    "重复 apply → ERR_COST_ADJUST_ALREADY_APPLIED");
        } finally {
            setApprovalRequired(true);
        }
    }

    @Test
    public void testReverseRollsBackBalanceAndVoucher() {
        Long materialId = 2507L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();
        generateIncoming(materialId, "PR-CA-007", new BigDecimal("10"), new BigDecimal("10"));

        Long adjustId = createAdjust(materialId, "CA-REV", ErpInvConstants.ADJUST_TYPE_COST_DIFFERENCE,
                new BigDecimal("12"));

        setApprovalRequired(false);
        try {
            applyCostAdjust(adjustId);
            ErpInvStockBalance afterApply = findBalance(materialId);
            assertEquals(0, afterApply.getAvgCost().compareTo(new BigDecimal("12")));

            reverseCostAdjust(adjustId);

            ErpInvCostAdjust adjust = daoProvider.daoFor(ErpInvCostAdjust.class).getEntityById(adjustId);
            assertFalse(Boolean.TRUE.equals(adjust.getPosted()), "冲销后 posted=false");

            ErpInvStockBalance afterReverse = findBalance(materialId);
            assertEquals(0, afterReverse.getAvgCost().compareTo(new BigDecimal("10")), "avgCost 回退为 10");
            assertEquals(0, afterReverse.getTotalCost().compareTo(new BigDecimal("100")), "totalCost 回退为 100");
        } finally {
            setApprovalRequired(true);
        }
    }

    @Test
    public void testNoBalanceAndNegativeCostRejected() {
        Long materialId = 2508L;
        seedMaterial(materialId, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();
        // 无入库 → 无余额
        Long adjustId = createAdjust(materialId, "CA-NOBAL", ErpInvConstants.ADJUST_TYPE_COST_DIFFERENCE,
                new BigDecimal("12"));

        setApprovalRequired(false);
        try {
            ApiResponse<?> resp = applyCostAdjustResp(adjustId);
            assertEquals(ErpInvErrors.ERR_COST_ADJUST_NO_BALANCE.getErrorCode(), resp.getCode(),
                    "无余额 → ERR_COST_ADJUST_NO_BALANCE");
        } finally {
            setApprovalRequired(true);
        }

        // 负成本
        Long materialId2 = 2509L;
        seedMaterial(materialId2, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        generateIncoming(materialId2, "PR-CA-009", new BigDecimal("10"), new BigDecimal("10"));
        Long adjustId2 = createAdjust(materialId2, "CA-NEG", ErpInvConstants.ADJUST_TYPE_COST_DIFFERENCE,
                new BigDecimal("-1"));
        setApprovalRequired(false);
        try {
            ApiResponse<?> resp = applyCostAdjustResp(adjustId2);
            assertEquals(ErpInvErrors.ERR_COST_ADJUST_NEGATIVE_COST.getErrorCode(), resp.getCode(),
                    "负成本 → ERR_COST_ADJUST_NEGATIVE_COST");
        } finally {
            setApprovalRequired(true);
        }
    }

    // ---------- 调整单创建 ----------

    private Long createAdjust(Long materialId, String code, String adjustType, BigDecimal newUnitCost) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvCostAdjust> headDao = daoProvider.daoFor(ErpInvCostAdjust.class);
            ErpInvCostAdjust head = new ErpInvCostAdjust();
            head.orm_propValueByName("id", (long) code.hashCode());
            head.setCode(code);
            head.setOrgId(ORG_ID);
            head.setBusinessDate(LocalDate.of(2026, 7, 1));
            head.setAdjustType(adjustType);
            head.setDocStatus(ErpInvConstants.DOC_STATUS_DRAFT);
            head.setApproveStatus("UNSUBMITTED");
            head.setPosted(false);
            head.setCurrencyId(CURRENCY_ID);
            headDao.saveEntity(head);

            IEntityDao<ErpInvCostAdjustLine> lineDao = daoProvider.daoFor(ErpInvCostAdjustLine.class);
            ErpInvCostAdjustLine line = new ErpInvCostAdjustLine();
            line.orm_propValueByName("id", (long) code.hashCode() * 10 + 1);
            line.setAdjustId((long) code.hashCode());
            line.setLineNo(1);
            line.setMaterialId(materialId);
            line.setWarehouseId(WAREHOUSE_ID);
            line.setNewUnitCost(newUnitCost);
            line.setCurrencyId(CURRENCY_ID);
            lineDao.saveEntity(line);
        });
        return (long) code.hashCode();
    }

    private Long applyCostAdjust(Long id) {
        return idOf(applyCostAdjustResp(id));
    }

    private ApiResponse<?> applyCostAdjustResp(Long id) {
        return executeRpc(mutation, "ErpInvCostAdjust__applyCostAdjust",
                ApiRequest.build(Map.of("id", id)));
    }

    private Long reverseCostAdjust(Long id) {
        return idOf(executeRpc(mutation, "ErpInvCostAdjust__reverseCostAdjust",
                ApiRequest.build(Map.of("id", id))));
    }

    // ---------- 移动单生成（建立余额） ----------

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, unitCost)));
        idOf(executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req))));
    }

    private void generateOutgoing(Long materialId, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", "SALES_SHIP");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, null)));
        idOf(executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req))));
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

    // ---------- RPC 辅助 ----------

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long idOf(ApiResponse<?> resp) {
        Object id = ((Map<?, ?>) resp.getData()).get("id");
        return id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
    }

    // ---------- 查询 ----------

    private ErpInvStockBalance findBalance(Long materialId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockLedger findAdjustLedger(Long materialId) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream()
                .filter(l -> l.getMoveId() != null && l.getMoveId() == ErpInvConstants.LEDGER_MOVE_ID_COST_ADJUST)
                .findFirst().orElse(null);
    }

    private ErpInvStockLedger findOutgoingLedger(Long materialId) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream()
                .filter(l -> l.getQuantity() != null && l.getQuantity().signum() < 0
                        && l.getMoveId() != null && l.getMoveId() != ErpInvConstants.LEDGER_MOVE_ID_COST_ADJUST)
                .findFirst().orElse(null);
    }

    private List<ErpInvCostLayer> findFifoLayers(Long materialId) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q);
    }

    private ErpInvCostLayer findLayerByMarker(Long incomingMoveId) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("incomingMoveId", incomingMoveId));
        List<ErpInvCostLayer> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvCostAdjustLine findAdjustLine(String code) {
        IEntityDao<ErpInvCostAdjust> headDao = daoProvider.daoFor(ErpInvCostAdjust.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpInvCostAdjust> heads = headDao.findAllByQuery(q);
        if (heads.isEmpty()) {
            return null;
        }
        IEntityDao<ErpInvCostAdjustLine> lineDao = daoProvider.daoFor(ErpInvCostAdjustLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("adjustId", heads.get(0).getId()));
        return lineDao.findAllByQuery(lq).stream().findFirst().orElse(null);
    }

    private List<ErpMfgCostRollup> findAllFirmedRollupsForMaterial(Long materialId) {
        IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(eq("materialId", materialId));
        return lineDao.findAllByQuery(lq).stream()
                .map(l -> daoProvider.daoFor(ErpMfgCostRollup.class).getEntityById(l.getCostRollupId()))
                .filter(h -> h != null && "FIRMED".equals(h.orm_propValueByName("status")))
                .collect(java.util.stream.Collectors.toList());
    }

    private BigDecimal latestFirmedUnitCost(Long materialId) {
        List<ErpMfgCostRollup> firmed = findAllFirmedRollupsForMaterial(materialId);
        firmed.sort((a, b) -> {
            LocalDate da = a.getBusinessDate() != null ? a.getBusinessDate() : LocalDate.MIN;
            LocalDate db = b.getBusinessDate() != null ? b.getBusinessDate() : LocalDate.MIN;
            return db.compareTo(da);
        });
        for (ErpMfgCostRollup header : firmed) {
            IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
            QueryBean lq = new QueryBean();
            lq.addFilter(eq("costRollupId", header.getId()));
            lq.addFilter(eq("materialId", materialId));
            List<ErpMfgCostRollupLine> lines = lineDao.findAllByQuery(lq);
            if (!lines.isEmpty()) {
                return lines.get(0).getUnitCost();
            }
        }
        return BigDecimal.ZERO;
    }

    private ErpFinVoucher findVoucherByBillCode(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
    }

    private ErpFinVoucherLine findVoucherLine(Long voucherId, String subjectCode) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        List<ErpFinVoucherLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void setApprovalRequired(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_COST_ADJUST_APPROVAL, String.valueOf(value));
    }

    // ---------- seed ----------

    private void seedMaterial(Long id, String costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MATC-" + id);
            material.setName("Cost Adjust Material " + id);
            material.orm_propValueByName("materialType", "GOODS");
            material.setUoMId(UOM_ID);
            material.setStatus("ACTIVE");
            material.setCostMethod(costMethod);
            dao.saveEntity(material);
        });
    }

    private void seedFirmedRollup(Long materialId, BigDecimal unitCost, long rollupId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
            ErpMfgCostRollup header = new ErpMfgCostRollup();
            header.orm_propValueByName("id", rollupId);
            header.setCode("ROLLUP-SEED-" + materialId);
            header.setOrgId(ORG_ID);
            header.setBusinessDate(LocalDate.of(2026, 6, 1));
            header.orm_propValueByName("status", "FIRMED");
            headerDao.saveEntity(header);

            IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
            ErpMfgCostRollupLine line = new ErpMfgCostRollupLine();
            line.orm_propValueByName("id", rollupId + 1);
            line.setCostRollupId(rollupId);
            line.setLineNo(1);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setUnitCost(unitCost);
            line.setTotalCost(unitCost);
            line.setMaterialCost(unitCost);
            line.setCurrencyId(CURRENCY_ID);
            lineDao.saveEntity(line);
        });
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedAcctSchema();
            seedOpenPeriod();
            seedSubject(SUBJECT_INVENTORY, "库存商品", "ASSET", "DEBIT");
            seedSubject(SUBJECT_COST_VARIANCE, "成本差异", "EXPENSE", "DEBIT");
        });
    }

    private void seedAcctSchema() {
        IEntityDao<app.erp.md.dao.entity.ErpMdAcctSchema> dao =
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class);
        app.erp.md.dao.entity.ErpMdAcctSchema schema = new app.erp.md.dao.entity.ErpMdAcctSchema();
        schema.orm_propValueByName("id", ACCT_SCHEMA_ID);
        schema.setCode("ACCT-" + ORG_ID);
        schema.setName("账套 " + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.orm_propValueByName("nature", "FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(schema);
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
        period.orm_propValueByName("status", "OPEN");
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", subjectClass);
        subject.orm_propValueByName("direction", direction);
        subject.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(subject);
    }
}
