package app.erp.mnt.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntSparePartUsageLine;
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
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 测试：维修备件消耗 GL 过账（plan 2026-07-10-1100-6）。
 *
 * <p>覆盖 MAINTENANCE_ISSUE 凭证生成（Dr: 维修费用 6602 / Cr: 存货 1403），config 门控（默认关，
 * 向后兼容），多物料分列，以及幂等防双重扣减。镜像 {@code TestErpMfgIssuePosting} 范式。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntSparePartPosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1251L;
    static final Long EQUIPMENT_ID = 151L;
    static final Long WAREHOUSE_ID = 3251L;
    static final Long UOM_ID = 5251L;
    static final Long CURRENCY_ID = 6251L;
    static final Long ACCT_SCHEMA_ID = 7251L;
    static final Long M1 = 4251L;
    static final Long M2 = 4252L;

    static final String SUBJECT_INVENTORY = "1403";
    static final String SUBJECT_EXPENSE = "6602";
    static final String SUBJECT_CLEARING = "2502";
    static final String VOUCHER_STATUS_POSTED = "POSTED";
    static final String MOVE_TYPE_INCOMING = "INCOMING";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    app.erp.mnt.service.posting.MaintenanceIssuePostingDispatcher issuePostingDispatcher;

    private final AtomicLong idSeq = new AtomicLong(200000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    @AfterEach
    void resetPostingConfig() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMntConstants.CONFIG_SPARE_PART_POSTING_ENABLED,
                String.valueOf(ErpMntConstants.DEFAULT_SPARE_PART_POSTING_ENABLED));
    }

    // ---------- 场景 1：基本过账 ----------

    @Test
    public void testSparePartPostingBasic() {
        enablePosting(true);
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedEquipment(EQUIPMENT_ID);
        seedStock("SEED-MNT-BASIC", M1, bd("20"), bd("5"));

        Long usageId = nextId();
        Long lineId = nextId();
        ormTemplate.runInSession(session -> {
            seedUsage(usageId, EQUIPMENT_ID, "SP-POST-BASIC");
            seedUsageLine(lineId, usageId, M1, bd("10"), bd("5"));
            return null;
        });

        assertEquals(0, confirm(usageId).getStatus(), "备件消耗确认应成功");

        ErpMntSparePartUsage usage = loadUsage(usageId);
        assertTrue(usage.getPosted(), "posted=true（库存已出库）");

        ErpInvStockMove move = findMove("SP-POST-BASIC");
        assertNotNull(move, "应生成出库移动单");
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, move.getDocStatus());

        ErpFinVoucher voucher = findVoucher("SP-POST-BASIC-MI");
        assertNotNull(voucher, "应生成 MAINTENANCE_ISSUE 凭证");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus());

        ErpFinVoucherLine drLine = findVoucherLine(voucher.getId(), SUBJECT_EXPENSE);
        assertNotNull(drLine, "借方 维修费用 6602 行存在");
        assertEquals("DEBIT", drLine.getDcDirection());
        assertEquals(0, bd("50").compareTo(drLine.getDebitAmount()), "借方 = 10×5=50");

        ErpFinVoucherLine crLine = findVoucherLine(voucher.getId(), SUBJECT_INVENTORY);
        assertNotNull(crLine, "贷方 存货 1403 行存在");
        assertEquals("CREDIT", crLine.getDcDirection());
        assertEquals(0, drLine.getDebitAmount().compareTo(crLine.getCreditAmount()), "借贷平衡");
    }

    // ---------- 场景 2：多物料 ----------

    @Test
    public void testSparePartPostingMultiMaterial() {
        enablePosting(true);
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(M2, "MOVING_AVERAGE");
        seedEquipment(EQUIPMENT_ID);
        seedStock("SEED-MNT-MM1", M1, bd("10"), bd("5"));
        seedStock("SEED-MNT-MM2", M2, bd("10"), bd("8"));

        Long usageId = nextId();
        ormTemplate.runInSession(session -> {
            seedUsage(usageId, EQUIPMENT_ID, "SP-POST-MM");
            seedUsageLine(nextId(), usageId, M1, bd("2"), bd("5"));
            seedUsageLine(nextId(), usageId, M2, bd("1"), bd("8"));
            return null;
        });

        assertEquals(0, confirm(usageId).getStatus(), "多物料备件消耗确认应成功");

        ErpFinVoucher voucher = findVoucher("SP-POST-MM-MI");
        assertNotNull(voucher, "多物料消耗应生成凭证");

        List<ErpFinVoucherLine> creditLines = findVoucherLines(voucher.getId(), SUBJECT_INVENTORY, "CREDIT");
        assertEquals(2, creditLines.size(), "2 行贷方（各物料存货科目）");

        BigDecimal totalCredit = creditLines.stream()
                .map(l -> nz(l.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, bd("18").compareTo(totalCredit), "贷方合计 = M1(2×5=10) + M2(1×8=8) = 18");

        ErpFinVoucherLine drLine = findVoucherLine(voucher.getId(), SUBJECT_EXPENSE);
        assertNotNull(drLine, "借方 维修费用 汇总行存在");
        assertEquals(0, totalCredit.compareTo(drLine.getDebitAmount()), "借方汇总 = 贷方合计");

        assertEquals(3, countVoucherLines(voucher.getId()), "2 贷 + 1 借 = 3 行");
    }

    // ---------- 场景 3：config 关闭（向后兼容）----------

    @Test
    public void testSparePartPostingConfigDisabled() {
        enablePosting(false);
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedEquipment(EQUIPMENT_ID);
        seedStock("SEED-MNT-OFF", M1, bd("20"), bd("5"));

        Long usageId = nextId();
        Long lineId = nextId();
        ormTemplate.runInSession(session -> {
            seedUsage(usageId, EQUIPMENT_ID, "SP-POST-OFF");
            seedUsageLine(lineId, usageId, M1, bd("10"), bd("5"));
            return null;
        });

        assertEquals(0, confirm(usageId).getStatus(), "备件消耗确认应成功");

        ErpMntSparePartUsage usage = loadUsage(usageId);
        assertTrue(usage.getPosted(), "posted=true（库存已出库，向后兼容语义不变）");

        ErpInvStockMove move = findMove("SP-POST-OFF");
        assertNotNull(move, "应生成出库移动单");

        assertNull(findVoucher("SP-POST-OFF-MI"), "config 关闭 → 不生成 GL 凭证");
    }

    // ---------- 场景 4：幂等防双重扣减 ----------

    @Test
    public void testSparePartPostingIdempotentNoDoubleDeduction() {
        enablePosting(true);
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedEquipment(EQUIPMENT_ID);
        seedStock("SEED-MNT-IDEM", M1, bd("20"), bd("5"));

        Long usageId = nextId();
        Long lineId = nextId();
        ormTemplate.runInSession(session -> {
            seedUsage(usageId, EQUIPMENT_ID, "SP-POST-IDEM");
            seedUsageLine(lineId, usageId, M1, bd("10"), bd("5"));
            return null;
        });

        assertEquals(0, confirm(usageId).getStatus(), "备件消耗确认应成功");
        ErpFinVoucher voucher = findVoucher("SP-POST-IDEM-MI");
        assertNotNull(voucher, "首次确认应生成凭证");

        // 再次触发派发（模拟兜底重扫）：不应生成第二张凭证（billHeadCode 幂等判重）
        issuePostingDispatcher.dispatchIfApplicable(usageId);

        assertEquals(1, countVouchers("SP-POST-IDEM-MI"),
                "幂等：同一消耗单仅一张 MAINTENANCE_ISSUE 凭证，存货不双重扣减");

        // 与 assets 域 MAINTENANCE_EXPENSE 防双重扣减由不同业务类型保证：MNT_ISSUE 贷存货(1403)，
        // MAINTENANCE_EXPENSE(linkedVisit) 贷中转清算(2502)，不冲突（assets 侧已由 plan 0842-2 覆盖）。
        ErpFinVoucherLine crLine = findVoucherLine(voucher.getId(), SUBJECT_INVENTORY);
        assertNotNull(crLine, "贷方 存货 1403（实物出库 GL 对应）");
        assertEquals(0, bd("50").compareTo(crLine.getCreditAmount()), "存货扣减 = 10×5=50（未翻倍）");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> confirm(Long usageId) {
        ApiResponse<?> resp = executeRpc(mutation, "ErpMntSparePartUsage__confirm",
                ApiRequest.build(Map.of("usageId", usageId)));
        return resp;
    }

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType opType, String action,
                                      ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void enablePosting(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpMntConstants.CONFIG_SPARE_PART_POSTING_ENABLED, String.valueOf(value));
    }

    // ---------- finance / material / stock seed ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpMdAcctSchema> asDao = daoProvider.daoFor(ErpMdAcctSchema.class);
            ErpMdAcctSchema schema = new ErpMdAcctSchema();
            schema.setId(ACCT_SCHEMA_ID);
            schema.setCode("AS-" + ORG_ID);
            schema.setName("账套" + ORG_ID);
            schema.setOrgId(ORG_ID);
            schema.setNature("FINANCIAL");
            schema.setFunctionalCurrencyId(CURRENCY_ID);
            schema.setStatus("ACTIVE");
            asDao.saveEntity(schema);

            IEntityDao<ErpFinAccountingPeriod> pdao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
            period.setCode("2026-07");
            period.setName("2026-07");
            period.setOrgId(ORG_ID);
            period.setYear(2026);
            period.setMonth(7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.setStatus("OPEN");
            pdao.saveEntity(period);

            seedSubject(SUBJECT_INVENTORY, "存货", "ASSET", "DEBIT");
            seedSubject(SUBJECT_EXPENSE, "维修费用", "EXPENSE", "DEBIT");
            seedSubject(SUBJECT_CLEARING, "维修中转清算", "LIABILITY", "CREDIT");
            return null;
        });
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(subjectClass);
        subject.setDirection(direction);
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedMaterial(Long id, String costMethod) {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("备件 " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            m.setCostMethod(costMethod);
            dao.saveEntity(m);
            return null;
        });
    }

    private void seedStock(String billCode, Long materialId, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("relatedBillType", "SEED_STOCK");
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
        assertEquals(0, resp.getStatus(), "seedStock generateMove 应成功");
    }

    // ---------- maintenance seed ----------

    private void seedEquipment(Long id) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = new ErpMntEquipment();
        equipment.setId(id);
        equipment.setCode("EQ-" + id);
        equipment.setName("设备" + id);
        equipment.setStatus(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
        dao.saveEntity(equipment);
    }

    private void seedUsage(Long id, Long equipmentId, String code) {
        IEntityDao<ErpMntSparePartUsage> dao = daoProvider.daoFor(ErpMntSparePartUsage.class);
        ErpMntSparePartUsage usage = new ErpMntSparePartUsage();
        usage.setId(id);
        usage.setCode(code);
        usage.setEquipmentId(equipmentId);
        usage.setWarehouseId(WAREHOUSE_ID);
        usage.setOrgId(ORG_ID);
        usage.setBusinessDate(LocalDate.of(2026, 7, 1));
        usage.setDocStatus(ErpMntDaoConstants.DOC_STATUS_DRAFT);
        usage.setApproveStatus(ErpMntConstants.APPROVE_STATUS_UNSUBMITTED);
        usage.setPosted(false);
        dao.saveEntity(usage);
    }

    private void seedUsageLine(Long id, Long usageId, Long materialId, BigDecimal qty, BigDecimal unitCost) {
        IEntityDao<ErpMntSparePartUsageLine> dao = daoProvider.daoFor(ErpMntSparePartUsageLine.class);
        ErpMntSparePartUsageLine line = new ErpMntSparePartUsageLine();
        line.setId(id);
        line.setSparePartUsageId(usageId);
        line.setLineNo(materialId.intValue());
        line.setMaterialId(materialId);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitCost(unitCost);
        line.setAmount(qty.multiply(unitCost));
        dao.saveEntity(line);
    }

    // ---------- query helpers ----------

    private ErpMntSparePartUsage loadUsage(Long usageId) {
        return daoProvider.daoFor(ErpMntSparePartUsage.class).getEntityById(usageId);
    }

    private ErpInvStockMove findMove(String usageCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpMntConstants.RELATED_BILL_TYPE_MNT_SPARE_PART));
        q.addFilter(eq("relatedBillCode", usageCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpFinVoucher findVoucher(String billHeadCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", ErpFinBusinessType.MAINTENANCE_ISSUE.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
    }

    private long countVouchers(String billHeadCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", ErpFinBusinessType.MAINTENANCE_ISSUE.name())));
        return dao.findAllByQuery(q).size();
    }

    private ErpFinVoucherLine findVoucherLine(Long voucherId, String subjectCode) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private List<ErpFinVoucherLine> findVoucherLines(Long voucherId, String subjectCode, String dcDirection) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        q.addFilter(eq("dcDirection", dcDirection));
        return dao.findAllByQuery(q);
    }

    private long countVoucherLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
