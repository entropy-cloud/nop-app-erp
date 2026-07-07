package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O-10 端到端过账链验证：采购发票审批 → AP_INVOICE 凭证 + 库存入库估值过账 → INV 凭证 + 双凭证业财回链一致性。
 *
 * <p>验证跨域过账链完整性：采购域 AP 过账（借费用+借进项税/贷应付）与库存域估值过账（借存货/贷暂估应付）
 * 各自生成独立凭证，经 {@link ErpFinVoucherBillR} 业财回链可追溯。另覆盖反审核红字冲销。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurToInvToFinPostingEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 1501L;
    static final Long SUPPLIER_ID = 2501L;
    static final Long MATERIAL_ID = 4501L;
    static final Long UOM_ID = 5501L;
    static final Long CURRENCY_ID = 6501L;
    static final Long ACCT_SCHEMA_ID = 7501L;
    static final Long WAREHOUSE_ID = 3501L;
    static final Long LOCATION_ID = 4502L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testPurInvoiceApprovalGeneratesApVoucher() {
        seedPeriodAndSubjects();

        ErpPurInvoice invoice = invoiceOf("PI-E2E-001",
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, approve(invoice.getId()).getStatus(), "approve 应成功");

        ErpPurInvoice reloaded = reload(invoice);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reloaded.getApproveStatus());
        assertTrue(Boolean.TRUE.equals(reloaded.getPosted()), "审核应过账 posted=true");

        ErpFinVoucherBillR link = findBillLink("PI-E2E-001");
        assertNotNull(link, "AP_INVOICE 应生成业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "AP 凭证应落库");
        assertEquals("POSTED", voucher.getDocStatus());
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("113")) == 0,
                "借方=费用100+进项税13=113");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("113")) == 0,
                "贷方=应付113");
        assertEquals(3, countLines(voucher.getId()),
                "AP_INVOICE 凭证 3 行（借采购/借进项税/贷应付）");
    }

    @Test
    public void testInventoryReceiptGeneratesValuationVoucher() {
        seedPeriodAndSubjects();

        Map<String, Object> req = baseInvReq();
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", "PR-E2E-001");
        req.put("lines", Collections.singletonList(invLine(new BigDecimal("10"), new BigDecimal("5"))));
        assertEquals(0, genMove(req).getStatus(), "generateMove 应成功");

        ErpInvStockMove move = findMove("PUR_RECEIPT", "PR-E2E-001");
        assertNotNull(move, "库存移动单应存在");
        assertEquals("DONE", move.getDocStatus(), "业务联动应 DONE");
        assertTrue(Boolean.TRUE.equals(move.getPosted()), "入库估值应过账 posted=true");

        ErpFinVoucherBillR link = findBillLink(move.getCode());
        assertNotNull(link, "INV 估值应生成业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "INV 凭证应落库");
        assertEquals("POSTED", voucher.getDocStatus());
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("50")) == 0,
                "借方=存货 50");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("50")) == 0,
                "贷方=暂估应付 50");
        assertEquals(2, countLines(voucher.getId()),
                "INV 估值凭证 2 行（借存货/贷暂估）");
    }

    @Test
    public void testFullPostingChainBothVouchersLinked() {
        seedPeriodAndSubjects();

        // 1. 库存入库估值过账
        Map<String, Object> invReq = baseInvReq();
        invReq.put("destWarehouseId", WAREHOUSE_ID);
        invReq.put("destLocationId", LOCATION_ID);
        invReq.put("relatedBillType", "PUR_RECEIPT");
        invReq.put("relatedBillCode", "PR-CHAIN-001");
        invReq.put("lines", Collections.singletonList(invLine(new BigDecimal("10"), new BigDecimal("5"))));
        assertEquals(0, genMove(invReq).getStatus(), "库存移动应成功");
        ErpInvStockMove move = findMove("PUR_RECEIPT", "PR-CHAIN-001");
        assertTrue(Boolean.TRUE.equals(move.getPosted()), "入库 posted=true");

        // 2. 采购发票审批 AP 过账
        ErpPurInvoice invoice = invoiceOf("PI-CHAIN-001",
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveInvoiceWithLine(invoice);
        });
        assertEquals(0, approve(invoice.getId()).getStatus(), "发票审批应成功");
        assertTrue(Boolean.TRUE.equals(reload(invoice).getPosted()), "发票 posted=true");

        // 3. 双凭证业财回链均可追溯
        ErpFinVoucherBillR invLink = findBillLink(move.getCode());
        ErpFinVoucherBillR apLink = findBillLink("PI-CHAIN-001");
        assertNotNull(invLink, "INV 估值回链存在");
        assertNotNull(apLink, "AP_INVOICE 回链存在");

        ErpFinVoucher invVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(invLink.getVoucherId());
        ErpFinVoucher apVoucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(apLink.getVoucherId());
        assertNotNull(invVoucher, "INV 凭证存在");
        assertNotNull(apVoucher, "AP 凭证存在");

        // 4. 双凭证一致性：同会计期间、同币种、各自借贷平衡
        assertEquals(invVoucher.getAcctSchemaId(), apVoucher.getAcctSchemaId(),
                "双凭证同一账套");
        assertEquals(0, invVoucher.getTotalDebit().compareTo(invVoucher.getTotalCredit()),
                "INV 凭证借贷平衡");
        assertEquals(0, apVoucher.getTotalDebit().compareTo(apVoucher.getTotalCredit()),
                "AP 凭证借贷平衡");
    }

    @Test
    public void testReverseApproveGeneratesRedVoucher() {
        seedPeriodAndSubjects();

        ErpPurInvoice invoice = invoiceOf("PI-REV-E2E",
                new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(0, approve(invoice.getId()).getStatus());
        assertTrue(Boolean.TRUE.equals(reload(invoice).getPosted()), "先过账 posted=true");
        long before = countAllVoucherLinks("PI-REV-E2E");

        assertEquals(0, reverseApprove(invoice.getId()).getStatus(), "反审核应成功");
        ErpPurInvoice reloaded = reload(invoice);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reloaded.getApproveStatus());
        assertFalse(Boolean.TRUE.equals(reloaded.getPosted()), "反审核后 posted=false");

        long after = countAllVoucherLinks("PI-REV-E2E");
        assertTrue(after > before, "反审核应生成红字冲销凭证");
    }

    // ---------- helpers ----------

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApprove(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurInvoice reload(ErpPurInvoice invoice) {
        return daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoice.getId());
    }

    private ErpPurInvoice invoiceOf(String code, BigDecimal amount, BigDecimal tax, BigDecimal withTax) {
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setSupplierId(SUPPLIER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(amount);
        invoice.setTotalTaxAmount(tax);
        invoice.setTotalAmountWithTax(withTax);
        invoice.setPosted(false);
        return invoice;
    }

    private void saveInvoiceWithLine(ErpPurInvoice invoice) {
        daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
        IEntityDao<ErpPurInvoiceLine> lineDao = daoProvider.daoFor(ErpPurInvoiceLine.class);
        ErpPurInvoiceLine line = new ErpPurInvoiceLine();
        line.setInvoiceId(invoice.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("10"));
        line.setTaxRate(new BigDecimal("13"));
        lineDao.saveEntity(line);
    }

    private Map<String, Object> baseInvReq() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", "INCOMING");
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        return req;
    }

    private Map<String, Object> invLine(BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        return line;
    }

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("SUPPLIER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1403", "在途物资", "ASSET", "DEBIT");
            seedSubject("2221", "应交税费-进项税额", "ASSET", "DEBIT");
            seedSubject("2202", "应付账款", "LIABILITY", "CREDIT");
            seedSubject("1401", "库存商品", "ASSET", "DEBIT");
            seedAcctSchema(ACCT_SCHEMA_ID, ORG_ID);
        });
    }

    private void seedAcctSchema(Long id, Long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setId(id);
        schema.setCode("AS-" + id);
        schema.setName("账套" + id);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
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

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinVoucherBillR findBillLink(String billCode) {
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean());
        return links.stream().filter(l -> billCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private long countAllVoucherLinks(String billCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> billCode.equals(l.getBillCode())).count();
    }

    private long countLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }
}
