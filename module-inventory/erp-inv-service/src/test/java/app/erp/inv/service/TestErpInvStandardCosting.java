package app.erp.inv.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STANDARD 计价方法集成测试（plan 2026-07-05-0427-2）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>STANDARD 出库按标准成本 COGS（ledger.unitCost = 标准成本，经 InvPostingDispatcher 既有通道）</li>
 *   <li>无标准成本抛 ERR_STANDARD_COST_NOT_AVAILABLE</li>
 *   <li>PPV 双向（实际>标准→借价差；实际<标准→贷价差；金额=|实际−标准|×qty）</li>
 *   <li>config 关闭 PPV 不生成（标准成本仍记账）</li>
 *   <li>移动加权平均 + FIFO 零回归由既有 TestErpInvStockMoveBookkeeping / TestErpInvFifoCosting 套件覆盖</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvStandardCosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1401L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long LOCATION_ID = 4401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long ACCT_SCHEMA_ID = 7401L;
    static final String PERIOD_CODE = "2026-07";
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_ESTIMATED_AP = "2202";
    static final String SUBJECT_COGS = "6401";
    static final String SUBJECT_PPV = "1404";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testStandardOutgoingUsesStandardCostForCogs() {
        Long materialId = 2401L;
        seedStandardMaterial(materialId);
        seedFirmedRollup(materialId, new BigDecimal("10"));
        seedPeriodAndSubjects();

        generateIncoming(materialId, "PR-STD-001", new BigDecimal("20"), new BigDecimal("12"));
        generateOutgoing(materialId, "SS-STD-001", new BigDecimal("8"));

        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        assertEquals(0, outLedger.getUnitCost().compareTo(new BigDecimal("10")),
                "STANDARD 出库 ledger.unitCost = 标准成本 10");
        assertEquals(0, outLedger.getTotalCost().compareTo(new BigDecimal("-80")),
                "STANDARD 出库 totalCost = -8×10 = -80（负号，InvPostingDispatcher .abs() 拾取）");
        assertEquals(ErpInvConstants.COST_METHOD_STANDARD, outLedger.getCostMethod(),
                "流水 costMethod=STANDARD");

        ErpInvStockBalance balance = findBalance(materialId);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("12")),
                "余额 20-8=12");
        assertEquals(0, balance.getTotalCost().compareTo(new BigDecimal("120")),
                "余额 totalCost = 12×10 = 120（标准成本）");
    }

    @Test
    public void testNoStandardCostThrowsError() {
        Long materialId = 2402L;
        seedStandardMaterial(materialId);
        seedPeriodAndSubjects();

        setNegativeStock(true);
        try {
            ApiResponse<?> resp = genMove(outgoingReq(materialId, "SS-STD-ERR", new BigDecimal("5")));
            assertEquals(ErpInvErrors.ERR_STANDARD_COST_NOT_AVAILABLE.getErrorCode(), resp.getCode(),
                    "无标准成本应返回 ERR_STANDARD_COST_NOT_AVAILABLE");
        } finally {
            setNegativeStock(false);
        }
    }

    @Test
    public void testPpvUnfavorableActualGreaterThanStandard() {
        Long materialId = 2403L;
        seedStandardMaterial(materialId);
        seedFirmedRollup(materialId, new BigDecimal("10"));
        seedPeriodAndSubjects();

        Long moveId = generateIncoming(materialId, "PR-STD-PPV-U", new BigDecimal("20"), new BigDecimal("12"));
        String moveCode = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(moveId).getCode();

        ErpInvStockLedger inLedger = findIncomingLedger(materialId);
        assertEquals(0, inLedger.getUnitCost().compareTo(new BigDecimal("10")),
                "入库 ledger.unitCost = 标准成本 10（非实际 12）");

        ErpFinVoucher ppvVoucher = findVoucherByBillCode(moveCode + "-PPV");
        assertNotNull(ppvVoucher, "实际>标准应生成 PPV 凭证");
        assertEquals(VOUCHER_STATUS_POSTED, ppvVoucher.getDocStatus(), "PPV 凭证已过账");

        BigDecimal variance = new BigDecimal("2").multiply(new BigDecimal("20"));
        assertEquals(0, ppvVoucher.getTotalDebit().compareTo(variance),
                "PPV 借方（材料成本差异）= |12-10|×20 = 40");
        assertEquals(0, ppvVoucher.getTotalCredit().compareTo(variance),
                "PPV 贷方（暂估应付）= 40");

        ErpFinVoucherLine ppvLine = findVoucherLine(ppvVoucher.getId(), SUBJECT_PPV);
        assertNotNull(ppvLine, "应有 PPV 科目行");
        assertEquals("DEBIT", ppvLine.getDcDirection(), "实际>标准→PPV 科目借方");
    }

    @Test
    public void testPpvFavorableActualLessThanStandard() {
        Long materialId = 2404L;
        seedStandardMaterial(materialId);
        seedFirmedRollup(materialId, new BigDecimal("10"));
        seedPeriodAndSubjects();

        Long moveId = generateIncoming(materialId, "PR-STD-PPV-F", new BigDecimal("20"), new BigDecimal("8"));
        String moveCode = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(moveId).getCode();

        ErpFinVoucher ppvVoucher = findVoucherByBillCode(moveCode + "-PPV");
        assertNotNull(ppvVoucher, "实际<标准应生成 PPV 凭证");

        BigDecimal variance = new BigDecimal("2").multiply(new BigDecimal("20"));
        assertEquals(0, ppvVoucher.getTotalDebit().compareTo(variance),
                "PPV 借方（暂估应付）= |8-10|×20 = 40");
        assertEquals(0, ppvVoucher.getTotalCredit().compareTo(variance),
                "PPV 贷方（材料成本差异）= 40");

        ErpFinVoucherLine ppvLine = findVoucherLine(ppvVoucher.getId(), SUBJECT_PPV);
        assertNotNull(ppvLine, "应有 PPV 科目行");
        assertEquals("CREDIT", ppvLine.getDcDirection(), "实际<标准→PPV 科目贷方");
    }

    @Test
    public void testPpvDisabledSkipsVoucherButStandardCostStillRecorded() {
        Long materialId = 2405L;
        seedStandardMaterial(materialId);
        seedFirmedRollup(materialId, new BigDecimal("10"));
        seedPeriodAndSubjects();

        setPpvEnabled(false);
        try {
            Long moveId = generateIncoming(materialId, "PR-STD-PPV-OFF", new BigDecimal("20"), new BigDecimal("12"));
            String moveCode = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(moveId).getCode();

            ErpInvStockLedger inLedger = findIncomingLedger(materialId);
            assertEquals(0, inLedger.getUnitCost().compareTo(new BigDecimal("10")),
                    "config 关闭 PPV 时标准成本仍记账（ledger.unitCost = 标准 10）");

            ErpFinVoucher ppvVoucher = findVoucherByBillCode(moveCode + "-PPV");
            assertNull(ppvVoucher, "config 关闭 PPV 不应生成 PPV 凭证");
        } finally {
            setPpvEnabled(true);
        }
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

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private Map<String, Object> outgoingReq(Long materialId, String billCode, BigDecimal qty) {
        Map<String, Object> req = baseReq(materialId, ErpInvConstants.MOVE_TYPE_OUTGOING);
        req.put("sourceWarehouseId", WAREHOUSE_ID);
        req.put("sourceLocationId", LOCATION_ID);
        req.put("relatedBillType", "SALES_SHIP");
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(materialId, qty, null)));
        return req;
    }

    private Map<String, Object> baseReq(Long materialId, String moveType) {
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

    private ErpInvStockBalance findBalance(Long materialId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
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

    private ErpInvStockLedger findIncomingLedger(Long materialId) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        return dao.findAllByQuery(q).stream()
                .filter(l -> l.getQuantity() != null && l.getQuantity().signum() > 0)
                .findFirst()
                .orElse(null);
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

    private void setNegativeStock(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(value));
    }

    private void setPpvEnabled(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_STANDARD_COST_PPV_ENABLED, String.valueOf(value));
    }

    // ---------- seed ----------

    private void seedStandardMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial material = new ErpMdMaterial();
            material.orm_propValueByName("id", id);
            material.setCode("MATS-" + id);
            material.setName("STANDARD Material " + id);
            material.orm_propValueByName("materialType", "GOODS");
            material.setUoMId(UOM_ID);
            material.setStatus("ACTIVE");
            material.setCostMethod(ErpInvConstants.COST_METHOD_STANDARD);
            dao.saveEntity(material);
        });
    }

    private void seedFirmedRollup(Long materialId, BigDecimal unitCost) {
        ormTemplate.runInSession(() -> {
            Long headerId = materialId * 10000 + 1;
            IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
            ErpMfgCostRollup header = new ErpMfgCostRollup();
            header.orm_propValueByName("id", headerId);
            header.setCode("ROLLUP-" + materialId);
            header.setOrgId(ORG_ID);
            header.setBusinessDate(LocalDate.of(2026, 6, 1));
            header.orm_propValueByName("status", "FIRMED");
            headerDao.saveEntity(header);

            IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
            ErpMfgCostRollupLine line = new ErpMfgCostRollupLine();
            line.orm_propValueByName("id", materialId * 10000 + 2);
            line.setCostRollupId(headerId);
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
            seedOpenPeriod();
            seedSubject(SUBJECT_INVENTORY, "库存商品", "ASSET", "DEBIT");
            seedSubject(SUBJECT_ESTIMATED_AP, "应付账款-暂估", "LIABILITY", "CREDIT");
            seedSubject(SUBJECT_COGS, "主营业务成本", "EXPENSE", "DEBIT");
            seedSubject(SUBJECT_PPV, "材料成本差异", "ASSET", "DEBIT");
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
