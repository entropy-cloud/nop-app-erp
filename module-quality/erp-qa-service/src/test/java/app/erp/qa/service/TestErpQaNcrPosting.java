package app.erp.qa.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Phase 4 行为测试：NCR 财务过账引擎端到端（plan 2026-07-05-2352-2）。
 *
 * <p>覆盖 7 类场景：(a) SCRAP happy（报废凭证 + posted=true）；(b) RETURN 编排退货（returnCode 登记）；
 * (c) CONCESSION 无凭证（postNcr 拒 ERR_NCR_DISPOSITION_NOT_POSTABLE）；(d) AUTO_POST 自动 vs MANUAL_POST 延后；
 * (e) 重复过账防护（posted=true 再 post 拒 ERR_NCR_ALREADY_POSTED）；(f) reverseNcr 红冲（posted→false）；
 * (g) 未 RESOLVED 过账拒（ERR_NCR_NOT_RESOLVED）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/qa/beans/test-mock-posting.beans.xml")
public class TestErpQaNcrPosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1003L;
    static final Long MATERIAL_ID = 7401L;
    static final Long SUPPLIER_ID = 8001L;
    static final Long WAREHOUSE_ID = 3003L;
    static final Long CURRENCY_ID = 6003L;
    static final Long ACCT_SCHEMA_ID = 7003L;
    static final Long VERIFICATION_PERSON = 7501L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @BeforeEach
    void setUpPostingConfig() {
        System.setProperty(ErpQaConstants.CONFIG_NCR_DEFAULT_ACCT_SCHEMA, String.valueOf(ACCT_SCHEMA_ID));
    }

    @AfterEach
    void tearDownPostingConfig() {
        System.clearProperty(ErpQaConstants.CONFIG_NCR_DEFAULT_ACCT_SCHEMA);
        System.clearProperty(ErpQaConstants.CONFIG_NCR_POSTING_MODE);
    }

    @Test
    public void testScrapAutoPostedOnResolve() {
        seedPeriodAndSubjects();
        seedStockBalance(MATERIAL_ID, new BigDecimal("5"), new BigDecimal("10"));

        Long ncrId = seedNcr("NCR-SCRAP-001", ErpQaConstants.DISPOSITION_TYPE_SCRAP, new BigDecimal("3"));
        submitReviewAndResolveWithCapa(ncrId);

        ErpQaNonConformance ncr = reloadNcr(ncrId);
        assertEquals(ErpQaConstants.NCR_STATUS_RESOLVED, ncr.getStatus());
        assertEquals(Boolean.TRUE, ncr.getPosted(), "SCRAP AUTO_POST resolve 应自动过账");
        assertNotNull(ncr.getPostedAt(), "过账时间已记录");

        ErpFinVoucherBillR link = findBillLink(ncr.getCode());
        assertNotNull(link, "应生成业财回链（NCR code → 凭证）");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "凭证应落库");
        BigDecimal expectedAmount = new BigDecimal("3").multiply(new BigDecimal("10"));
        assertTrue(voucher.getTotalDebit().compareTo(expectedAmount) == 0,
                "借方合计=报废损失 30");
        assertTrue(voucher.getTotalCredit().compareTo(expectedAmount) == 0,
                "贷方合计=存货 30");
        assertEquals(2, countLines(voucher.getId()), "报废凭证 2 行（借营业外支出/贷存货）");
    }

    @Test
    public void testReturnDispositionOrchestratesPurchaseReturn() {
        seedPeriodAndSubjects();
        seedStockBalance(MATERIAL_ID, new BigDecimal("5"), new BigDecimal("10"));
        Long ncrId = seedNcrWithSupplier("NCR-RETURN-001", ErpQaConstants.DISPOSITION_TYPE_RETURN,
                new BigDecimal("2"), SUPPLIER_ID);
        submitReviewAndResolveWithCapa(ncrId);

        ErpQaNonConformance ncr = reloadNcr(ncrId);
        assertEquals(ErpQaConstants.NCR_STATUS_RESOLVED, ncr.getStatus());
        assertNotNull(ncr.getReturnCode(), "RETURN 处置应编排创建退货单并登记 returnCode");
        assertTrue(ncr.getReturnCode().startsWith("PR-FROM-NCR-"), "supplierId 非空 → 采购退货");
    }

    @Test
    public void testConcessionNotPostable() {
        seedPeriodAndSubjects();
        Long ncrId = seedNcr("NCR-CONC-001", ErpQaConstants.DISPOSITION_TYPE_CONCESSION, new BigDecimal("1"));
        submitReviewAndResolveWithCapa(ncrId);

        ApiResponse<?> resp = rpc(mutation, "ErpQaNonConformance__postNcr", Map.of("ncrId", ncrId));
        assertEquals(ErpQaErrors.ERR_NCR_DISPOSITION_NOT_POSTABLE.getErrorCode(), resp.getCode(),
                "CONCESSION 处置 postNcr 应拒 ERR_NCR_DISPOSITION_NOT_POSTABLE");
    }

    @Test
    public void testManualPostingModeDefersUntilPostNcr() {
        seedPeriodAndSubjects();
        seedStockBalance(MATERIAL_ID, new BigDecimal("5"), new BigDecimal("10"));
        System.setProperty(ErpQaConstants.CONFIG_NCR_POSTING_MODE, ErpQaConstants.NCR_POSTING_MODE_MANUAL);
        Long ncrId = seedNcr("NCR-MANUAL-001", ErpQaConstants.DISPOSITION_TYPE_SCRAP, new BigDecimal("2"));
        submitReviewAndResolveWithCapa(ncrId);

        ErpQaNonConformance ncr = reloadNcr(ncrId);
        assertEquals(ErpQaConstants.NCR_STATUS_RESOLVED, ncr.getStatus());
        assertFalse(Boolean.TRUE.equals(ncr.getPosted()), "MANUAL_POST resolve 不应自动过账");

        rpcOk(mutation, "ErpQaNonConformance__postNcr", Map.of("ncrId", ncrId));
        assertTrue(Boolean.TRUE.equals(reloadNcr(ncrId).getPosted()), "postNcr 后 posted=true");
    }

    @Test
    public void testDuplicatePostingRejected() {
        seedPeriodAndSubjects();
        seedStockBalance(MATERIAL_ID, new BigDecimal("5"), new BigDecimal("10"));
        Long ncrId = seedNcr("NCR-DUP-001", ErpQaConstants.DISPOSITION_TYPE_SCRAP, new BigDecimal("1"));
        submitReviewAndResolveWithCapa(ncrId);
        assertTrue(Boolean.TRUE.equals(reloadNcr(ncrId).getPosted()), "首次过账成功");

        ApiResponse<?> resp = rpc(mutation, "ErpQaNonConformance__postNcr", Map.of("ncrId", ncrId));
        assertEquals(ErpQaErrors.ERR_NCR_ALREADY_POSTED.getErrorCode(), resp.getCode(),
                "重复过账应拒 ERR_NCR_ALREADY_POSTED");
    }

    @Test
    public void testReverseNcrClearsPostedAndRedOffset() {
        seedPeriodAndSubjects();
        seedStockBalance(MATERIAL_ID, new BigDecimal("5"), new BigDecimal("10"));
        Long ncrId = seedNcr("NCR-REV-001", ErpQaConstants.DISPOSITION_TYPE_SCRAP, new BigDecimal("2"));
        submitReviewAndResolveWithCapa(ncrId);
        assertTrue(Boolean.TRUE.equals(reloadNcr(ncrId).getPosted()), "过账成功");

        rpcOk(mutation, "ErpQaNonConformance__reverseNcr", Map.of("ncrId", ncrId));
        ErpQaNonConformance ncr = reloadNcr(ncrId);
        assertFalse(Boolean.TRUE.equals(ncr.getPosted()), "reverseNcr 后 posted=false");
        assertNull(ncr.getPostedAt(), "过账时间已清除");
    }

    @Test
    public void testPostingBeforeResolvedRejected() {
        seedPeriodAndSubjects();
        Long ncrId = seedNcr("NCR-NOTRES-001", ErpQaConstants.DISPOSITION_TYPE_SCRAP, new BigDecimal("1"));

        ApiResponse<?> resp = rpc(mutation, "ErpQaNonConformance__postNcr", Map.of("ncrId", ncrId));
        assertEquals(ErpQaErrors.ERR_INVALID_NCR_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "未 RESOLVED 过账应拒（状态非法）");
    }

    // ---------- helpers ----------

    private void submitReviewAndResolveWithCapa(Long ncrId) {
        rpcOk(mutation, "ErpQaNonConformance__submitReview", Map.of("ncrId", ncrId));
        Long actionId = seedAction(ncrId, ErpQaConstants.ACTION_STATUS_PENDING);
        rpcOk(mutation, "ErpQaAction__startAction", Map.of("actionId", actionId));
        rpcOk(mutation, "ErpQaAction__completeAction", Map.of("actionId", actionId));
        Map<String, Object> verifyArgs = new LinkedHashMap<>();
        verifyArgs.put("actionId", actionId);
        verifyArgs.put("verificationPerson", VERIFICATION_PERSON);
        verifyArgs.put("verificationDate", CoreMetrics.currentDate().toString());
        rpcOk(mutation, "ErpQaAction__verifyAction", verifyArgs);
        rpcOk(mutation, "ErpQaNonConformance__resolve",
                Map.of("ncrId", ncrId, "resolution", "CAPA 验证通过，关闭+过账"));
    }

    private ErpQaNonConformance reloadNcr(Long ncrId) {
        return daoProvider.daoFor(ErpQaNonConformance.class).getEntityById(ncrId);
    }

    private ErpQaNonConformance findNcrByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpQaNonConformance> list = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long seedNcr(String code, String disposition, BigDecimal quantity) {
        return seedNcrWithSupplier(code, disposition, quantity, null);
    }

    private Long seedNcrWithSupplier(String code, String disposition, BigDecimal quantity, Long supplierId) {
        Long id = 6300L + (long) (Math.abs(code.hashCode()) % 10000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
            ErpQaNonConformance ncr = new ErpQaNonConformance();
            ncr.orm_propValueByName("id", id);
            ncr.setCode(code);
            ncr.setNcrDate(CoreMetrics.currentDate());
            ncr.setMaterialId(MATERIAL_ID);
            ncr.setQuantity(quantity);
            ncr.setDispositionType(disposition);
            ncr.setStatus(ErpQaConstants.NCR_STATUS_OPEN);
            ncr.setSeverity(ErpQaConstants.RECALL_SEVERITY_MEDIUM);
            if (supplierId != null) {
                ncr.setSupplierId(supplierId);
            }
            ncr.setDescription("测试NCR:" + code);
            dao.saveEntity(ncr);
        });
        return id;
    }

    private Long seedAction(Long ncrId, String status) {
        Long id = 9700L + (long) (Math.abs(ncrId.hashCode() + status.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaAction> dao = daoProvider.daoFor(ErpQaAction.class);
            ErpQaAction a = new ErpQaAction();
            a.orm_propValueByName("id", id);
            a.setNcrId(ncrId);
            a.setActionType("CAPA");
            a.setStatus(status);
            a.setDescription("纠正预防措施");
            dao.saveEntity(a);
        });
        return id;
    }

    private void seedStockBalance(Long materialId, BigDecimal totalQty, BigDecimal avgCost) {
        Long balanceId = 5500L + materialId;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance bal = new ErpInvStockBalance();
            bal.orm_propValueByName("id", balanceId);
            bal.setOrgId(ORG_ID);
            bal.setMaterialId(materialId);
            bal.setWarehouseId(WAREHOUSE_ID);
            bal.setTotalQuantity(totalQty);
            bal.setAvailableQuantity(totalQty);
            bal.setAvgCost(avgCost);
            bal.setTotalCost(totalQty.multiply(avgCost));
            bal.setCurrencyId(CURRENCY_ID);
            dao.saveEntity(bal);
        });
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("6711", "营业外支出-报废损失", "EXPENSE", "DEBIT");
            seedSubject("1401", "库存商品", "ASSET", "DEBIT");
        });
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
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

    private ErpFinVoucherBillR findBillLink(String ncrCode) {
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean());
        return links.stream().filter(l -> ncrCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private long countLines(Long voucherId) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> dao = daoProvider
                .daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功，但返回: " + resp);
    }
}
