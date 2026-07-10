package app.erp.inv.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.dao.entity.ErpInvLandedCostLine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
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

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 到岸成本端到端集成测试（plan 2026-07-10-1100-3）。
 *
 * <p>覆盖 4 类：(a) BY_AMOUNT 分摊 + 成本层更新 + GL 凭证；(b) BY_QUANTITY 分摊；
 * (c) 多应付对象（多贷方行）；(d) 防重复分摊拦截。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvLandedCostEndToEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 1601L;
    static final Long WAREHOUSE_ID = 3601L;
    static final Long LOCATION_ID = 4601L;
    static final Long UOM_ID = 5601L;
    static final Long CURRENCY_ID = 6601L;
    static final Long ACCT_SCHEMA_ID = 7601L;
    static final String PERIOD_CODE = "2026-07";
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_AP = "2202";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    /**
     * 场景1（BY_AMOUNT）：入库单 2 行（物料A amount=1000/物料B amount=500）+ 到岸成本 180
     * → 分摊 A=120/B=60 → 成本层更新 → 凭证（借存货 180 / 贷应付 180）
     */
    @Test
    public void testAllocateByAmount() {
        Long matA = 2601L;
        Long matB = 2602L;
        seedMaterial(matA, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedMaterial(matB, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();

        generateIncoming(matA, "PR-LC-001A", new BigDecimal("100"), new BigDecimal("10"));
        generateIncoming(matB, "PR-LC-001B", new BigDecimal("50"), new BigDecimal("10"));

        Long receiveId = seedReceive("RCV-001", new BigDecimal("100"), new BigDecimal("1000"), matA,
                new BigDecimal("50"), new BigDecimal("500"), matB);

        Long landedCostId = createLandedCost("LC-001", receiveId, ErpInvConstants.ALLOC_METHOD_BY_AMOUNT,
                new String[][]{{"FREIGHT", "150"}, {"INSURANCE", "30"}}, null);

        Long approvedId = approve(landedCostId);
        assertNotNull(approvedId);

        ErpInvStockBalance balA = findBalance(matA);
        ErpInvStockBalance balB = findBalance(matB);
        assertNotNull(balA, "物料A有余额");
        assertNotNull(balB, "物料B有余额");
        BigDecimal addedA = balA.getTotalCost().subtract(new BigDecimal("1000"));
        BigDecimal addedB = balB.getTotalCost().subtract(new BigDecimal("500"));
        assertTrue(addedA.compareTo(new BigDecimal("120")) == 0,
                "物料A分摊金额=120: " + addedA);
        assertTrue(addedB.compareTo(new BigDecimal("60")) == 0,
                "物料B分摊金额=60: " + addedB);

        ErpFinVoucher voucher = findVoucherByBillCode("LC-001");
        assertNotNull(voucher, "到岸成本生成凭证");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus());
        assertEquals(0, voucher.getTotalDebit().compareTo(new BigDecimal("180")),
                "借方合计=180");

        ErpFinVoucherLine invDebit = findVoucherLine(voucher.getId(), SUBJECT_INVENTORY, "DEBIT");
        assertNotNull(invDebit, "借存货行存在");

        ErpFinVoucherLine apCredit = findVoucherLine(voucher.getId(), SUBJECT_AP, "CREDIT");
        assertNotNull(apCredit, "贷应付行存在");
    }

    /**
     * 场景2（BY_QUANTITY）：按数量分摊
     */
    @Test
    public void testAllocateByQuantity() {
        Long matA = 2603L;
        Long matB = 2604L;
        seedMaterial(matA, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedMaterial(matB, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();

        generateIncoming(matA, "PR-LC-002A", new BigDecimal("100"), new BigDecimal("10"));
        generateIncoming(matB, "PR-LC-002B", new BigDecimal("50"), new BigDecimal("10"));

        Long receiveId = seedReceive("RCV-002", new BigDecimal("100"), new BigDecimal("1000"), matA,
                new BigDecimal("50"), new BigDecimal("500"), matB);

        Long landedCostId = createLandedCost("LC-002", receiveId, ErpInvConstants.ALLOC_METHOD_BY_QUANTITY,
                new String[][]{{"FREIGHT", "180"}}, null);

        approve(landedCostId);

        ErpInvStockBalance balA = findBalance(matA);
        ErpInvStockBalance balB = findBalance(matB);
        // 按数量：A=100/150=2/3, B=50/150=1/3
        // 180 × 2/3 = 120, 180 × 1/3 = 60
        BigDecimal addedA = balA.getTotalCost().subtract(new BigDecimal("1000"));
        BigDecimal addedB = balB.getTotalCost().subtract(new BigDecimal("500"));
        assertTrue(addedA.compareTo(new BigDecimal("119")) > 0 && addedA.compareTo(new BigDecimal("121")) < 0,
                "物料A按数量分摊≈120: " + addedA);
        assertTrue(addedB.compareTo(new BigDecimal("59")) > 0 && addedB.compareTo(new BigDecimal("61")) < 0,
                "物料B按数量分摊≈60: " + addedB);
    }

    /**
     * 场景3（多应付对象）：运费 150 应付物流商 + 保险 30 应付保险公司 → 凭证两条贷方行
     */
    @Test
    public void testMultipleAPPARTNERS() {
        Long matA = 2605L;
        seedMaterial(matA, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();

        generateIncoming(matA, "PR-LC-003A", new BigDecimal("100"), new BigDecimal("10"));

        Long logisticsPartnerId = 8001L;
        Long insurancePartnerId = 8002L;

        Long receiveId = seedReceiveSingle("RCV-003", new BigDecimal("100"), new BigDecimal("1000"), matA);

        Long landedCostId = createLandedCostWithPartners("LC-003", receiveId,
                ErpInvConstants.ALLOC_METHOD_BY_AMOUNT,
                new Object[][]{
                        {"FREIGHT", new BigDecimal("150"), logisticsPartnerId},
                        {"INSURANCE", new BigDecimal("30"), insurancePartnerId}
                });

        approve(landedCostId);

        ErpFinVoucher voucher = findVoucherByBillCode("LC-003");
        assertNotNull(voucher);

        List<ErpFinVoucherLine> apLines = findVoucherLines(voucher.getId(), SUBJECT_AP);
        assertEquals(2, apLines.size(), "两条贷方应付行（运费+保险各一）");

        ErpFinVoucherLine invDebit = findVoucherLine(voucher.getId(), SUBJECT_INVENTORY, "DEBIT");
        assertNotNull(invDebit);
        assertEquals(0, invDebit.getDebitAmount().compareTo(new BigDecimal("180")),
                "借存货=150+30=180");
    }

    /**
     * 场景4（防重复分摊）：同一入库单第二张到岸成本单审核 → 拦截
     */
    @Test
    public void testDuplicateAllocationRejected() {
        Long matA = 2606L;
        seedMaterial(matA, ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        seedPeriodAndSubjects();

        generateIncoming(matA, "PR-LC-004A", new BigDecimal("100"), new BigDecimal("10"));

        Long receiveId = seedReceiveSingle("RCV-004", new BigDecimal("100"), new BigDecimal("1000"), matA);

        Long lc1 = createLandedCost("LC-004A", receiveId, ErpInvConstants.ALLOC_METHOD_BY_AMOUNT,
                new String[][]{{"FREIGHT", "100"}}, null);
        approve(lc1);

        Long lc2 = createLandedCost("LC-004B", receiveId, ErpInvConstants.ALLOC_METHOD_BY_AMOUNT,
                new String[][]{{"INSURANCE", "50"}}, null);
        ApiResponse<?> resp = approveResp(lc2);
        assertEquals(ErpInvErrors.ERR_LANDED_COST_ALREADY_ALLOCATED.getErrorCode(), resp.getCode(),
                "同一入库单重复分摊 → ERR_LANDED_COST_ALREADY_ALLOCATED");
    }

    // ---------- 到岸成本单创建 ----------

    private Long createLandedCost(String code, Long receiveId, String allocationMethod,
                                    String[][] costElements, Long apPartnerId) {
        Object[][] elements = new Object[costElements.length][];
        for (int i = 0; i < costElements.length; i++) {
            elements[i] = new Object[]{
                    costElements[i][0],
                    new BigDecimal(costElements[i][1]),
                    apPartnerId
            };
        }
        return createLandedCostWithPartners(code, receiveId, allocationMethod, elements);
    }

    private Long createLandedCostWithPartners(String code, Long receiveId, String allocationMethod,
                                                Object[][] costElements) {
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
            for (Object[] e : costElements) {
                total = total.add((BigDecimal) e[1]);
            }
            head.setTotalCostAmount(total);
            head.setAllocationMethod(allocationMethod);
            head.setDocStatus(ErpInvConstants.DOC_STATUS_DRAFT);
            head.setApproveStatus("UNSUBMITTED");
            head.setPosted(false);
            head.setBusinessDate(LocalDate.of(2026, 7, 1));
            headDao.saveEntity(head);

            IEntityDao<ErpInvLandedCostLine> lineDao = daoProvider.daoFor(ErpInvLandedCostLine.class);
            int lineNo = 1;
            for (Object[] e : costElements) {
                ErpInvLandedCostLine line = new ErpInvLandedCostLine();
                line.orm_propValueByName("id", (long) code.hashCode() * 100 + lineNo);
                line.setLandedCostId((long) code.hashCode());
                line.setLineNo(lineNo++);
                line.orm_propValueByName("costElement", e[0]);
                line.orm_propValueByName("amount", e[1]);
                if (e[2] != null) {
                    line.setApPartnerId((Long) e[2]);
                }
                lineDao.saveEntity(line);
            }
        });
        return (long) code.hashCode();
    }

    private Long approve(Long id) {
        ApiResponse<?> resp = approveResp(id);
        if (resp.getData() == null) {
            throw new RuntimeException("approve failed: code=" + resp.getCode() + ", msg=" + resp.getMsg());
        }
        return idOf(resp);
    }

    private ApiResponse<?> approveResp(Long id) {
        return executeRpc(mutation, "ErpInvLandedCost__approve",
                ApiRequest.build(Map.of("id", id)));
    }

    // ---------- 采购入库单 seed ----------

    private Long seedReceive(String code, BigDecimal qtyA, BigDecimal amountA, Long matA,
                              BigDecimal qtyB, BigDecimal amountB, Long matB) {
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
            line1.setMaterialId(matA);
            line1.setWarehouseId(WAREHOUSE_ID);
            line1.setQuantity(qtyA);
            line1.orm_propValueByName("unitPrice", amountA.divide(qtyA, 4, java.math.RoundingMode.HALF_UP));
            line1.orm_propValueByName("amount", amountA);
            line1.setUoMId(UOM_ID);
            lineDao.saveEntity(line1);

            ErpPurReceiveLine line2 = new ErpPurReceiveLine();
            line2.orm_propValueByName("id", (long) code.hashCode() * 10 + 2);
            line2.setReceiveId((long) code.hashCode());
            line2.setLineNo(2);
            line2.setMaterialId(matB);
            line2.setWarehouseId(WAREHOUSE_ID);
            line2.setQuantity(qtyB);
            line2.orm_propValueByName("unitPrice", amountB.divide(qtyB, 4, java.math.RoundingMode.HALF_UP));
            line2.orm_propValueByName("amount", amountB);
            line2.setUoMId(UOM_ID);
            lineDao.saveEntity(line2);
        });
        return (long) code.hashCode();
    }

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

    private ErpFinVoucherLine findVoucherLine(Long voucherId, String subjectCode, String dcDirection) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        List<ErpFinVoucherLine> list = dao.findAllByQuery(q);
        return list.stream().filter(l -> dcDirection.equals(l.getDcDirection())).findFirst().orElse(null);
    }

    private List<ErpFinVoucherLine> findVoucherLines(Long voucherId, String subjectCode) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        return dao.findAllByQuery(q);
    }

    // ---------- seed ----------

    private void seedMaterial(Long id, String costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MATLC-" + id);
            material.setName("Landed Cost Material " + id);
            material.orm_propValueByName("materialType", "GOODS");
            material.setUoMId(UOM_ID);
            material.setStatus("ACTIVE");
            material.setCostMethod(costMethod);
            dao.saveEntity(material);
        });
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedAcctSchema();
            seedOpenPeriod();
            seedSubject(SUBJECT_INVENTORY, "库存商品", "ASSET", "DEBIT");
            seedSubject(SUBJECT_AP, "应付账款", "LIABILITY", "CREDIT");
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
