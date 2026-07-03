package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase 1 服务层集成测试：采购发票三轴审批状态机 + 供应商启用校验。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpPurInvoice__submit/withdrawSubmit/approve/reject/cancel}，
 * 引擎负责建 session/事务/管道。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurInvoiceApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSubmitApproveReverseRejectResubmit() {
        ErpPurInvoice invoice = newInvoice("PI-APP-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reload(invoice).getApproveStatus(),
                "提交 → SUBMITTED");

        assertEquals(0, approve(invoice.getId()).getStatus());
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reload(invoice).getApproveStatus(),
                "审核 → APPROVED");

        // 反审核（未过账，红字冲销跳过）→ REJECTED
        assertEquals(0, reverseApprove(invoice.getId()).getStatus());
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reload(invoice).getApproveStatus(),
                "反审核 → REJECTED");

        // REJECTED 重新提交 → SUBMITTED
        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reload(invoice).getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testWithdrawSubmit() {
        ErpPurInvoice invoice = newInvoice("PI-WD-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(0, withdrawSubmit(invoice.getId()).getStatus());
        assertEquals(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, reload(invoice).getApproveStatus(),
                "撤回提交 → UNSUBMITTED");
    }

    @Test
    public void testIllegalTransitionFromApproved() {
        ErpPurInvoice invoice = newInvoice("PI-ILL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(0, approve(invoice.getId()).getStatus());

        ApiResponse<?> bad = submit(invoice.getId());
        assertEquals(ErpPurErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "APPROVED 不可再提交");
        bad = withdrawSubmit(invoice.getId());
        assertEquals(ErpPurErrors.ERR_INVOICE_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "APPROVED 不可撤回提交");
    }

    @Test
    public void testInactiveSupplierRejected() {
        ErpPurInvoice invoice = newInvoice("PI-INACTIVE-001");
        ormTemplate.runInSession(() -> {
            seedSupplier(SUPPLIER_ID, "INACTIVE");
            saveInvoiceWithLine(invoice);
        });

        ApiResponse<?> bad = submit(invoice.getId());
        assertEquals(ErpPurErrors.ERR_PARTNER_INACTIVE.getErrorCode(), bad.getCode(),
                "供应商停用 → submit 应返回 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testCancelFromDraft() {
        ErpPurInvoice invoice = newInvoice("PI-CANCEL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, cancel(invoice.getId()).getStatus());
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, reload(invoice).getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        ApiResponse<?> bad = submit(invoice.getId());
        assertEquals(ErpPurErrors.ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "已作废单据不可提交");
    }

    // ---------- helpers ----------

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__submit", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> withdrawSubmit(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__withdrawSubmit", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__approve", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> reject(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__reject", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> reverseApprove(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__reverseApprove", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> cancel(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__cancel", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurInvoice reload(ErpPurInvoice invoice) {
        return daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoice.getId());
    }

    private ErpPurInvoice newInvoice(String code) {
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setSupplierId(SUPPLIER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(new BigDecimal("1"));
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("100"));
        invoice.setTotalTaxAmount(new BigDecimal("13"));
        invoice.setTotalAmountWithTax(new BigDecimal("113"));
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

    private void seedActiveSupplier(Long id) {
        seedSupplier(id, ErpPurConstants.PARTNER_STATUS_ACTIVE);
    }

    private void seedSupplier(Long id, String status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(status);
        dao.saveEntity(partner);
    }
}
