package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.dao.entity.ErpSalReceiptLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：收款→发票域级核销（{@link app.erp.sal.service.entity.ReceiptSettler}）。
 *
 * <p>覆盖部分核销（PARTIAL）、全额核销（RECEIVED）、跨客户拒绝、超额拒绝、reverseSettlement 恢复余额。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReceiptSettlement extends JunitAutoTestCase {

    static final Long ORG_ID = 1205L;
    static final Long CUSTOMER_ID = 2203L;
    static final Long CUSTOMER_ID_2 = 2204L;
    static final Long CURRENCY_ID = 6201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testPartialSettlement() {
        ErpSalInvoice invoice = newApprovedInvoice("SI-SET-001", new BigDecimal("113"));
        ErpSalReceipt receipt = newApprovedReceipt("SR-SET-001", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            daoProvider.daoFor(ErpSalInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpSalReceipt.class).saveEntity(receipt);
        });

        // 部分核销 60
        assertEquals(0, settle(receipt.getId(), invoice.getId(), new BigDecimal("60")).getStatus());
        ErpSalInvoice inv = reloadInvoice(invoice);
        ErpSalReceipt rec = reloadReceipt(receipt);
        assertEquals(0, new BigDecimal("60").compareTo(inv.getReceivedAmount()), "发票已收=60");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_PARTIAL, inv.getReceivedStatus(), "发票 receivedStatus=PARTIAL");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_PARTIAL, rec.getWrittenOffStatus(), "收款 writtenOffStatus=PARTIAL");
    }

    @Test
    public void testFullSettlement() {
        ErpSalInvoice invoice = newApprovedInvoice("SI-SET-002", new BigDecimal("113"));
        ErpSalReceipt receipt = newApprovedReceipt("SR-SET-002", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            daoProvider.daoFor(ErpSalInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpSalReceipt.class).saveEntity(receipt);
        });

        assertEquals(0, settle(receipt.getId(), invoice.getId(), new BigDecimal("113")).getStatus());
        assertEquals(ErpSalConstants.RECEIVED_STATUS_RECEIVED, reloadInvoice(invoice).getReceivedStatus(),
                "发票 receivedStatus=RECEIVED");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_RECEIVED, reloadReceipt(receipt).getWrittenOffStatus(),
                "收款 writtenOffStatus=RECEIVED");
    }

    @Test
    public void testCrossCustomerRejected() {
        ErpSalInvoice invoice = newApprovedInvoice("SI-SET-003", new BigDecimal("113"));
        invoice.setCustomerId(CUSTOMER_ID_2);
        ErpSalReceipt receipt = newApprovedReceipt("SR-SET-003", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            seedActiveCustomer(CUSTOMER_ID_2);
            daoProvider.daoFor(ErpSalInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpSalReceipt.class).saveEntity(receipt);
        });

        ApiResponse<?> bad = settle(receipt.getId(), invoice.getId(), new BigDecimal("50"));
        assertEquals(ErpSalErrors.ERR_SETTLE_CUSTOMER_MISMATCH.getErrorCode(), bad.getCode(),
                "跨客户核销应拒绝");
    }

    @Test
    public void testOverInvoiceBalanceRejected() {
        ErpSalInvoice invoice = newApprovedInvoice("SI-SET-004", new BigDecimal("113"));
        ErpSalReceipt receipt = newApprovedReceipt("SR-SET-004", new BigDecimal("500"));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            daoProvider.daoFor(ErpSalInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpSalReceipt.class).saveEntity(receipt);
        });

        // 核销 200 > 发票余额 113
        ApiResponse<?> bad = settle(receipt.getId(), invoice.getId(), new BigDecimal("200"));
        assertEquals(ErpSalErrors.ERR_SETTLE_OVER_INVOICE_BALANCE.getErrorCode(), bad.getCode(),
                "超发票未收余额应拒绝");
    }

    @Test
    public void testReverseSettlementRestoresBalance() {
        ErpSalInvoice invoice = newApprovedInvoice("SI-SET-005", new BigDecimal("113"));
        ErpSalReceipt receipt = newApprovedReceipt("SR-SET-005", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            daoProvider.daoFor(ErpSalInvoice.class).saveEntity(invoice);
            daoProvider.daoFor(ErpSalReceipt.class).saveEntity(receipt);
        });

        assertEquals(0, settle(receipt.getId(), invoice.getId(), new BigDecimal("113")).getStatus());
        assertEquals(ErpSalConstants.RECEIVED_STATUS_RECEIVED, reloadInvoice(invoice).getReceivedStatus());

        // 冲销核销 → 余额恢复
        assertEquals(0, reverseSettlement(receipt.getId(), invoice.getId()).getStatus());
        ErpSalInvoice inv = reloadInvoice(invoice);
        ErpSalReceipt rec = reloadReceipt(receipt);
        assertEquals(0, BigDecimal.ZERO.compareTo(inv.getReceivedAmount()), "冲销后发票已收回 0");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED, inv.getReceivedStatus(), "发票回 UNRECEIVED");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED, rec.getWrittenOffStatus(), "收款回 UNRECEIVED");
        // 反向负金额行存在（审计轨迹）
        assertTrue(hasNegativeLine(receipt.getId(), invoice.getId()), "应存在反向负金额 ReceiptLine");
    }

    // ---------- helpers ----------

    private boolean hasNegativeLine(Long receiptId, Long invoiceId) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("receiptId", receiptId));
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("invoiceId", invoiceId));
        for (ErpSalReceiptLine l : daoProvider.daoFor(ErpSalReceiptLine.class).findAllByQuery(q)) {
            if (l.getAmount() != null && l.getAmount().signum() < 0) {
                return true;
            }
        }
        return false;
    }

    private ApiResponse<?> settle(Long receiptId, Long invoiceId, BigDecimal amount) {
        Map<String, Object> alloc = new LinkedHashMap<>();
        alloc.put("invoiceId", invoiceId);
        alloc.put("amount", amount);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("receiptId", receiptId);
        req.put("allocations", Collections.singletonList(alloc));
        return executeRpc(mutation, "ErpSalReceipt__settle", ApiRequest.build(req));
    }

    private ApiResponse<?> reverseSettlement(Long receiptId, Long invoiceId) {
        return executeRpc(mutation, "ErpSalReceipt__reverseSettlement",
                ApiRequest.build(Map.of("receiptId", receiptId, "invoiceId", invoiceId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpSalInvoice reloadInvoice(ErpSalInvoice invoice) {
        return daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoice.getId());
    }

    private ErpSalReceipt reloadReceipt(ErpSalReceipt receipt) {
        return daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receipt.getId());
    }

    private ErpSalInvoice newApprovedInvoice(String code, BigDecimal withTax) {
        ErpSalInvoice invoice = new ErpSalInvoice();
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setCustomerId(CUSTOMER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        invoice.setReceivedStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        invoice.setReceivedAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(withTax);
        invoice.setTotalTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmountWithTax(withTax);
        invoice.setPosted(false);
        return invoice;
    }

    private ErpSalReceipt newApprovedReceipt(String code, BigDecimal total) {
        ErpSalReceipt receipt = new ErpSalReceipt();
        receipt.setCode(code);
        receipt.setOrgId(ORG_ID);
        receipt.setCustomerId(CUSTOMER_ID);
        receipt.setBusinessDate(LocalDate.of(2026, 7, 1));
        receipt.setCurrencyId(CURRENCY_ID);
        receipt.setExchangeRate(BigDecimal.ONE);
        receipt.setTotalAmount(total);
        receipt.setAmountSource(total);
        receipt.setAmountFunctional(total);
        receipt.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        receipt.setWrittenOffStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        receipt.setPosted(false);
        return receipt;
    }

    private void seedActiveCustomer(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("SUPPLIER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }
}
