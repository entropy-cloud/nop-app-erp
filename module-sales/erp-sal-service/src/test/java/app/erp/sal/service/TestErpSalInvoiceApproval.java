package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 服务层集成测试：销售发票三轴审批状态机 + 客户启用校验。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpSalInvoice__submit/withdrawSubmit/approve/reject/cancel}，
 * 引擎负责建 session/事务/管道。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalInvoiceApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1201L;
    static final Long CUSTOMER_ID = 2201L;
    static final Long MATERIAL_ID = 4201L;
    static final Long UOM_ID = 5201L;
    static final Long CURRENCY_ID = 6201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSubmitApproveReverseRejectResubmit() {
        ErpSalInvoice invoice = newInvoice("SI-APP-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(invoice).getApproveStatus(),
                "提交 → SUBMITTED");

        assertEquals(0, approve(invoice.getId()).getStatus());
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(invoice).getApproveStatus(),
                "审核 → APPROVED");

        // 反审核（未过账，红字冲销跳过）→ REJECTED
        assertEquals(0, reverseApprove(invoice.getId()).getStatus());
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reload(invoice).getApproveStatus(),
                "反审核 → REJECTED");

        // REJECTED 重新提交 → SUBMITTED
        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(invoice).getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testWithdrawSubmit() {
        ErpSalInvoice invoice = newInvoice("SI-WD-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(0, withdrawSubmit(invoice.getId()).getStatus());
        assertEquals(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED, reload(invoice).getApproveStatus(),
                "撤回提交 → UNSUBMITTED");
    }

    @Test
    public void testIllegalTransitionFromApproved() {
        ErpSalInvoice invoice = newInvoice("SI-ILL-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(0, approve(invoice.getId()).getStatus());

        ApiResponse<?> bad = submit(invoice.getId());
        assertTrue(bad.getStatus() != 0, "APPROVED 不可再提交，应被状态守卫拒绝");
        bad = withdrawSubmit(invoice.getId());
        assertTrue(bad.getStatus() != 0, "APPROVED 不可撤回提交，应被状态守卫拒绝");
    }

    @Test
    public void testInactiveCustomerRejected() {
        ErpSalInvoice invoice = newInvoice("SI-INACTIVE-001");
        ormTemplate.runInSession(() -> {
            seedCustomer(CUSTOMER_ID, "INACTIVE");
            saveInvoiceWithLine(invoice);
        });

        ApiResponse<?> bad = submit(invoice.getId());
        assertEquals(ErpSalErrors.ERR_PARTNER_INACTIVE.getErrorCode(), bad.getCode(),
                "客户停用 → submit 应返回 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testCancelFromDraft() {
        ErpSalInvoice invoice = newInvoice("SI-CANCEL-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveInvoiceWithLine(invoice);
        });

        assertEquals(0, cancel(invoice.getId()).getStatus());
        assertEquals(ErpSalConstants.DOC_STATUS_CANCELLED, reload(invoice).getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        ApiResponse<?> bad = submit(invoice.getId());
        assertEquals(ErpSalErrors.ERR_INVOICE_ILLEGAL_DOC_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "已作废单据不可提交");
    }

    // ---------- helpers ----------

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> withdrawSubmit(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__withdrawApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reject(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__reject", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApprove(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> cancel(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__cancel", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpSalInvoice reload(ErpSalInvoice invoice) {
        return daoProvider.daoFor(ErpSalInvoice.class).getEntityById(invoice.getId());
    }

    private ErpSalInvoice newInvoice(String code) {
        ErpSalInvoice invoice = new ErpSalInvoice();
        invoice.setCode(code);
        invoice.setOrgId(ORG_ID);
        invoice.setCustomerId(CUSTOMER_ID);
        invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
        invoice.setCurrencyId(CURRENCY_ID);
        invoice.setExchangeRate(new BigDecimal("1"));
        invoice.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        invoice.setReceivedStatus(ErpSalConstants.RECEIVED_STATUS_UNRECEIVED);
        invoice.setReceivedAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("100"));
        invoice.setTotalTaxAmount(new BigDecimal("13"));
        invoice.setTotalAmountWithTax(new BigDecimal("113"));
        invoice.setPosted(false);
        return invoice;
    }

    private void saveInvoiceWithLine(ErpSalInvoice invoice) {
        daoProvider.daoFor(ErpSalInvoice.class).saveEntity(invoice);
        IEntityDao<ErpSalInvoiceLine> lineDao = daoProvider.daoFor(ErpSalInvoiceLine.class);
        ErpSalInvoiceLine line = new ErpSalInvoiceLine();
        line.setInvoiceId(invoice.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("10"));
        line.setTaxRate(new BigDecimal("13"));
        lineDao.saveEntity(line);
    }

    private void seedActiveCustomer(Long id) {
        seedCustomer(id, ErpSalConstants.PARTNER_STATUS_ACTIVE);
    }

    private void seedCustomer(Long id, String status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("SUPPLIER");
        partner.setStatus(status);
        dao.saveEntity(partner);
    }
}
