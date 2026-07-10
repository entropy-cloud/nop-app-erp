package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
import app.erp.sal.dao.entity.ErpSalOrder;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 信用冻结（credit hold）发票审核环节集成测试（plan 2026-07-10-1100-2 Phase 3）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpSalInvoice__approve}，验证 {@code erp-sal.credit-check-on-invoice}
 * 门控下 {@link app.erp.sal.service.entity.CreditLimitChecker#checkCreditHold} 的三级策略与向后兼容。
 * 发票过账（SalInvoicePostingDispatcher）在无完整财务前置时优雅降级（posted=false），审核本身仍成功，
 * 故放行场景无需重财务前置（与 {@link TestErpSalInvoiceApproval} 一致）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalCreditHoldOnInvoice extends JunitAutoTestCase {

    static final Long ORG_ID = 1701L;
    static final Long CUSTOMER_ID = 2701L;
    static final Long MATERIAL_ID = 4701L;
    static final Long UOM_ID = 5701L;
    static final Long CURRENCY_ID = 6701L;
    static final Long ACCT_SCHEMA_ID = 7701L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // 场景 1（HARD_BLOCK 拦截）：creditLimit=10000，已审核未发货订单 8000 + AR open 3000 → available=-1000<0 → 发票审核被拦截
    @Test
    public void testHardBlockHoldsInvoice() {
        setCreditCheckOnInvoice(true);
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        enableArInclusion(true);

        ErpSalInvoice invoice = newInvoice("SI-HOLD-HB-001");
        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("10000"));
            seedApprovedOrder("SO-INV-HB-001", CUSTOMER_ID, new BigDecimal("8000"), BigDecimal.ONE);
            seedArOpenItem(CUSTOMER_ID, "SI-AR-HB-001", new BigDecimal("3000"), BigDecimal.ONE);
            saveInvoiceWithLine(invoice);
            return null;
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        ApiResponse<?> bad = approve(invoice.getId());
        assertEquals(ErpSalErrors.ERR_CREDIT_HOLD_INVOICE.getErrorCode(), bad.getCode(),
                "HARD_BLOCK 客户当前已超额 → 发票审核被信用冻结");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(invoice).getApproveStatus(),
                "拦截后状态保持 SUBMITTED");
    }

    // 场景 2（SOFT_WARNING 放行）：同上超额但 level=SOFT_WARNING → 发票审核通过
    @Test
    public void testSoftWarningAllowsInvoice() {
        setCreditCheckOnInvoice(true);
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        enableArInclusion(true);

        ErpSalInvoice invoice = newInvoice("SI-HOLD-SW-001");
        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("10000"));
            seedApprovedOrder("SO-INV-SW-001", CUSTOMER_ID, new BigDecimal("8000"), BigDecimal.ONE);
            seedArOpenItem(CUSTOMER_ID, "SI-AR-SW-001", new BigDecimal("3000"), BigDecimal.ONE);
            saveInvoiceWithLine(invoice);
            return null;
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(0, approve(invoice.getId()).getStatus(), "SOFT_WARNING 超额度放行（发票审核通过）");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(invoice).getApproveStatus(),
                "SOFT_WARNING 放行 → APPROVED");
    }

    // 场景 3（config 关闭 → 向后兼容）：超额但 credit-check-on-invoice=false → 即使 HARD_BLOCK 也通过
    @Test
    public void testConfigOffBackwardCompat() {
        setCreditCheckOnInvoice(false);
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        enableArInclusion(true);

        ErpSalInvoice invoice = newInvoice("SI-HOLD-OFF-001");
        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("10000"));
            seedApprovedOrder("SO-INV-OFF-001", CUSTOMER_ID, new BigDecimal("8000"), BigDecimal.ONE);
            seedArOpenItem(CUSTOMER_ID, "SI-AR-OFF-001", new BigDecimal("3000"), BigDecimal.ONE);
            saveInvoiceWithLine(invoice);
            return null;
        });

        assertEquals(0, submit(invoice.getId()).getStatus());
        assertEquals(0, approve(invoice.getId()).getStatus(), "config 关闭 → 即使超额也放行（向后兼容）");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(invoice).getApproveStatus(),
                "config 关闭 → APPROVED");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpSalInvoice__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedActiveCustomer(Long id, BigDecimal creditLimit) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        partner.setCreditLimit(creditLimit);
        dao.saveEntity(partner);
    }

    private void seedApprovedOrder(String code, Long customerId, BigDecimal totalAmountWithTax, BigDecimal exchangeRate) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(customerId);
        order.setWarehouseId(3601L);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(exchangeRate);
        order.setTotalAmountWithTax(totalAmountWithTax);
        order.setTotalAmount(totalAmountWithTax);
        order.setDocStatus(ErpSalConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setPosted(false);
        dao.saveEntity(order);
    }

    private void seedArOpenItem(Long customerId, String sourceBillCode, BigDecimal openAmountSource,
                                BigDecimal exchangeRate) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = dao.newEntity();
        item.setCode("ARI-" + sourceBillCode);
        item.setOrgId(ORG_ID);
        item.setAcctSchemaId(ACCT_SCHEMA_ID);
        item.setDirection(ErpFinConstants.DIRECTION_RECEIVABLE);
        item.setPartnerId(customerId);
        item.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        item.setSourceBillCode(sourceBillCode);
        item.setBusinessDate(LocalDate.of(2026, 6, 15));
        item.setCurrencyId(CURRENCY_ID);
        item.setExchangeRate(exchangeRate);
        BigDecimal functional = openAmountSource.multiply(exchangeRate);
        item.setAmountSource(openAmountSource);
        item.setAmountFunctional(functional);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(openAmountSource);
        item.setOpenAmountFunctional(functional);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        dao.saveEntity(item);
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

    // ---------- config helpers ----------

    private void setCreditCheckOnInvoice(boolean enabled) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_ON_INVOICE, enabled);
    }

    private void setCreditCheckLevel(String level) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_LEVEL, level);
    }

    private void enableArInclusion(boolean enabled) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_INCLUDE_AR, enabled);
    }

    // ---------- query helpers ----------

    private ErpSalInvoice reload(ErpSalInvoice invoice) {
        return daoProvider.daoFor(ErpSalInvoice.class).findAllByQuery(new QueryBean()).stream()
                .filter(i -> invoice.getCode().equals(i.getCode())).findFirst().orElse(invoice);
    }
}
