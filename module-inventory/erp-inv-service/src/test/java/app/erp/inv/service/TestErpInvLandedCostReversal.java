package app.erp.inv.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.dao.entity.ErpInvLandedCostLine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 到岸成本 reverseApprove 红冲测试（plan 2026-07-18-1745-2 Phase 4）。
 *
 * <p>覆盖 {@code ErpInvLandedCostBizModel.reverseApprove}：approve 产 LANDED_COST 凭证 + 成本层更新后，
 * 调 reverseApprove → 红冲凭证（原 isReversed=true + 红字凭证 postingType=REVERSAL）+ 反向应用成本层
 * （{@code CostAdjustmentService.reverseCostAdjust} 回退 StockBalance.avgCost/totalCost）+
 * posted=false / approveStatus=REJECTED / docStatus=CANCELLED。
 *
 * <p>镜像 {@code TestErpMntSparePartUsageReversal} 范式（红冲正路径 + 非法态守卫）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvLandedCostReversal extends JunitAutoTestCase {

    static final Long ORG_ID = 1751L;
    static final Long WAREHOUSE_ID = 3751L;
    static final Long LOCATION_ID = 4751L;
    static final Long UOM_ID = 5751L;
    static final Long CURRENCY_ID = 6751L;
    static final Long ACCT_SCHEMA_ID = 7751L;

    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_AP = "2202";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(500300L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    // ---------- 场景 1：approve → reverseApprove 正路径（红冲凭证 + 反向应用成本层） ----------

    @Test
    public void testReverseApproveRedReversesVoucherAndCostLayer() {
        Long matA = 2751L;
        seedMaterial(matA, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();

        generateIncoming(matA, "PR-LC-RV-SEED", new BigDecimal("10"), new BigDecimal("10"));

        Long receiveId = seedReceiveSingle("RCV-LC-RV", new BigDecimal("10"), new BigDecimal("100"), matA);

        Long landedCostId = createLandedCost("LC-RV-001", receiveId,
                new String[][]{{"FREIGHT", "50"}}, null);

        // approve 产 LANDED_COST 凭证 + 成本层更新 + posted=true
        assertEquals(0, approve(landedCostId).getStatus(), "approve 应成功");
        ErpInvLandedCost approved = loadLandedCost(landedCostId);
        assertTrue(Boolean.TRUE.equals(approved.getPosted()), "前置：posted=true");
        assertEquals("APPROVED", approved.getApproveStatus());

        ErpFinVoucher original = findVoucher("LC-RV-001", ErpFinBusinessType.LANDED_COST);
        assertNotNull(original, "前置：应存在 LANDED_COST 凭证");

        // 前置：成本层 avgCost 已被更新（100/10 + 50/10 = 15）
        ErpInvStockBalance balBefore = findBalance(matA);
        assertNotNull(balBefore, "前置：物料应有余额");
        BigDecimal avgCostBefore = balBefore.getAvgCost();
        assertTrue(avgCostBefore.compareTo(new BigDecimal("15")) == 0,
                "前置：approve 后 avgCost=15, 实际=" + avgCostBefore);

        // 执行 reverseApprove
        assertEquals(0, reverseApprove(landedCostId).getStatus(), "reverseApprove 应成功");

        // 1. 状态翻转：posted=false + approveStatus=REJECTED + docStatus=CANCELLED
        ErpInvLandedCost reversed = loadLandedCost(landedCostId);
        assertFalse(Boolean.TRUE.equals(reversed.getPosted()), "红冲后 posted=false");
        assertEquals("REJECTED", reversed.getApproveStatus(),
                "红冲后 approveStatus=REJECTED");
        assertEquals(ErpInvConstants.DOC_STATUS_CANCELLED, reversed.getDocStatus(),
                "红冲后 docStatus=CANCELLED");

        // 2. 原 LANDED_COST 凭证 isReversed=true
        ErpFinVoucher originalAfter = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(original.getId());
        assertTrue(Boolean.TRUE.equals(originalAfter.getIsReversed()),
                "原 LANDED_COST 凭证应被标记 isReversed=true");

        // 3. 红字凭证存在（同 billHeadCode，postingType=REVERSAL）
        ErpFinVoucher redVoucher = findReversalVoucher("LC-RV-001", ErpFinBusinessType.LANDED_COST);
        assertNotNull(redVoucher, "应存在 LANDED_COST 红字凭证");
        assertEquals("REVERSAL", redVoucher.getPostingType());

        // 4. 成本层反向应用：avgCost 回退至原始 10（reverseLine 按 oldUnitCost 回退 avgCost）
        ErpInvStockBalance balAfter = findBalance(matA);
        assertNotNull(balAfter, "红冲后物料仍应有余额");
        BigDecimal avgCostAfter = balAfter.getAvgCost();
        assertTrue(avgCostAfter.compareTo(new BigDecimal("10")) == 0,
                "红冲后 avgCost 应回退至 10, 实际=" + avgCostAfter);
    }

    // ---------- 场景 2：未过账守卫（approve 前 reverseApprove 抛 ERR_LANDED_COST_NOT_POSTED） ----------

    @Test
    public void testReverseApproveRejectsNotPosted() {
        Long matA = 2752L;
        seedMaterial(matA, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();

        generateIncoming(matA, "PR-LC-RV-REJ", new BigDecimal("10"), new BigDecimal("10"));

        Long receiveId = seedReceiveSingle("RCV-LC-REJ", new BigDecimal("10"), new BigDecimal("100"), matA);

        Long landedCostId = createLandedCost("LC-RV-REJ", receiveId,
                new String[][]{{"FREIGHT", "50"}}, null);

        // 未 approve 直接 reverseApprove → 守卫拒绝
        ApiResponse<?> resp = reverseApprove(landedCostId);
        assertEquals(ErpInvErrors.ERR_LANDED_COST_NOT_POSTED.getErrorCode(), resp.getCode(),
                "未过账到岸成本单 reverseApprove 应被守卫拒绝");

        // 状态不变（守卫前置，未进入红冲步骤）
        ErpInvLandedCost lc = loadLandedCost(landedCostId);
        assertFalse(Boolean.TRUE.equals(lc.getPosted()));
        assertEquals("UNSUBMITTED", lc.getApproveStatus());
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpInvLandedCost__approve",
                ApiRequest.build(Map.of("id", id)));
    }

    private ApiResponse<?> reverseApprove(Long id) {
        return executeRpc(mutation, "ErpInvLandedCost__reverseApprove",
                ApiRequest.build(Map.of("id", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action,
                                        ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- 采购入库单 seed ----------

    private Long seedReceiveSingle(String code, BigDecimal qty, BigDecimal amount, Long matId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpPurReceive> recvDao = daoProvider.daoFor(ErpPurReceive.class);
            ErpPurReceive recv = new ErpPurReceive();
            recv.orm_propValueByName("id", (long) code.hashCode());
            recv.setCode(code);
            recv.setOrgId(ORG_ID);
            recv.setSupplierId(7001L);
            recv.setWarehouseId(WAREHOUSE_ID);
            recv.setBusinessDate(LocalDate.of(2026, 7, 1));
            recv.setCurrencyId(CURRENCY_ID);
            recv.setExchangeRate(BigDecimal.ONE);
            recv.setApproveStatus("APPROVED");
            recv.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
            recv.setPosted(false);
            recvDao.saveEntity(recv);

            IEntityDao<ErpPurReceiveLine> lineDao = daoProvider.daoFor(ErpPurReceiveLine.class);
            ErpPurReceiveLine line1 = new ErpPurReceiveLine();
            line1.orm_propValueByName("id", (long) code.hashCode() * 10 + 1);
            line1.setReceiveId((long) code.hashCode());
            line1.setLineNo(1);
            line1.setMaterialId(matId);
            line1.setWarehouseId(WAREHOUSE_ID);
            line1.setQuantity(qty);
            line1.orm_propValueByName("unitPrice", amount.divide(qty, 4, java.math.RoundingMode.HALF_UP));
            line1.orm_propValueByName("amount", amount);
            line1.setUoMId(UOM_ID);
            lineDao.saveEntity(line1);
        });
        return (long) code.hashCode();
    }

    // ---------- 到岸成本单创建 ----------

    private Long createLandedCost(String code, Long receiveId, String[][] costElements, Long apPartnerId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvLandedCost> headDao = daoProvider.daoFor(ErpInvLandedCost.class);
            ErpInvLandedCost head = new ErpInvLandedCost();
            head.orm_propValueByName("id", (long) code.hashCode());
            head.setCode(code);
            head.setOrgId(ORG_ID);
            head.setReceiveId(receiveId);
            head.setSupplierId(7001L);
            head.setCurrencyId(CURRENCY_ID);
            head.setExchangeRate(BigDecimal.ONE);
            BigDecimal total = BigDecimal.ZERO;
            for (String[] e : costElements) {
                total = total.add(new BigDecimal(e[1]));
            }
            head.setTotalCostAmount(total);
            head.setAllocationMethod(ErpInvConstants.ALLOC_METHOD_BY_AMOUNT);
            head.setDocStatus(ErpInvConstants.DOC_STATUS_DRAFT);
            head.setApproveStatus("UNSUBMITTED");
            head.setPosted(false);
            head.setBusinessDate(LocalDate.of(2026, 7, 1));
            headDao.saveEntity(head);

            IEntityDao<ErpInvLandedCostLine> lineDao = daoProvider.daoFor(ErpInvLandedCostLine.class);
            int lineNo = 1;
            for (String[] e : costElements) {
                ErpInvLandedCostLine line = new ErpInvLandedCostLine();
                line.orm_propValueByName("id", (long) code.hashCode() * 100 + lineNo);
                line.setLandedCostId((long) code.hashCode());
                line.setLineNo(lineNo++);
                line.orm_propValueByName("costElement", e[0]);
                line.orm_propValueByName("amount", new BigDecimal(e[1]));
                if (apPartnerId != null) {
                    line.setApPartnerId(apPartnerId);
                } else {
                    line.setApPartnerId(7001L);
                }
                lineDao.saveEntity(line);
            }
        });
        return (long) code.hashCode();
    }

    // ---------- 移动单生成（建立余额） ----------

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertEquals(0, resp.getStatus(), "generateIncoming 应成功");
    }

    // ---------- finance / material seed ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedAcctSchema();
            seedOpenPeriod();
            seedSubject(SUBJECT_INVENTORY, "库存商品", "ASSET", "DEBIT");
            seedSubject(SUBJECT_AP, "应付账款", "LIABILITY", "CREDIT");
        });
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
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
        period.setCode("2026-07-RV");
        period.setName("2026-07-RV");
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

    private void seedMaterial(Long id, String costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MATLC-" + id);
            m.setName("Landed Cost Reversal Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            m.setCostMethod(costMethod);
            dao.saveEntity(m);
        });
    }

    // ---------- query helpers ----------

    private ErpInvLandedCost loadLandedCost(Long id) {
        return daoProvider.daoFor(ErpInvLandedCost.class).getEntityById(id);
    }

    private ErpInvStockBalance findBalance(Long materialId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinVoucher findVoucher(String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", businessType.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && "NORMAL".equals(v.getPostingType())) {
                return v;
            }
        }
        return null;
    }

    private ErpFinVoucher findReversalVoucher(String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", businessType.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && "REVERSAL".equals(v.getPostingType())) {
                return v;
            }
        }
        return null;
    }
}
