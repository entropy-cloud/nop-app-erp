package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurPayment;
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
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：付款单三轴审批状态机 + PAYMENT 过账（借应付/贷银行存款）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurPaymentApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1003L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long CURRENCY_ID = 6101L;
    static final Long ACCT_SCHEMA_ID = 7003L;
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSubmitApproveReverse() {
        seedPeriodAndSubjects();
        ErpPurPayment payment = paymentOf("PY-APP-001", new BigDecimal("113"));
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        // 已 SUBMITTED，approve 触发 PAYMENT 过账
        assertEquals(0, approve(payment.getId()).getStatus());
        ErpPurPayment reloaded = reload(payment);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reloaded.getApproveStatus());
        assertEquals(true, reloaded.getPosted(), "PAYMENT 审核应过账 posted=true");

        ErpFinVoucherBillR link = findBillLink(payment.getCode());
        assertNotNull(link, "应生成业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus());
        // PAYMENT：借应付 113 / 贷银行存款 113
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("113")) == 0, "借方=应付 113");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("113")) == 0, "贷方=银行存款 113");
        assertEquals(2, countLines(voucher.getId()), "PAYMENT 凭证 2 行");

        // 反审核红字冲销 → REJECTED，posted 反转
        assertEquals(0, reverseApprove(payment.getId()).getStatus());
        reloaded = reload(payment);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reloaded.getApproveStatus());
        assertTrue(!Boolean.TRUE.equals(reloaded.getPosted()), "反审核后 posted=false");
    }

    @Test
    public void testIllegalTransition() {
        ErpPurPayment payment = paymentOf("PY-ILL-001", new BigDecimal("100"));
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        // 草稿(UNSUBMITTED) 直接 approve 应被拒
        ApiResponse<?> bad = approve(payment.getId());
        assertEquals(-1, bad.getStatus(),
                "UNSUBMITTED 不可直接审核：平台守卫仅接受 SUBMITTED");
    }

    @Test
    public void testInactiveSupplierRejected() {
        ErpPurPayment payment = paymentOf("PY-INACTIVE-001", new BigDecimal("100"));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
            ErpMdPartner partner = new ErpMdPartner();
            partner.setId(SUPPLIER_ID);
            partner.setCode("SUP-X");
            partner.setName("停用供应商");
            partner.setPartnerType("CUSTOMER");
            partner.setStatus("INACTIVE");
            dao.saveEntity(partner);
            daoProvider.daoFor(ErpPurPayment.class).saveEntity(payment);
        });

        ApiResponse<?> bad = submit(payment.getId());
        assertEquals(ErpPurErrors.ERR_PARTNER_INACTIVE.getErrorCode(), bad.getCode(),
                "供应商停用 → submit 应返回 ERR_PARTNER_INACTIVE");
    }

    // ---------- helpers ----------

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpPurPayment__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpPurPayment__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApprove(Long id) {
        return executeRpc(mutation, "ErpPurPayment__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurPayment reload(ErpPurPayment payment) {
        return daoProvider.daoFor(ErpPurPayment.class).getEntityById(payment.getId());
    }

    private ErpPurPayment paymentOf(String code, BigDecimal total) {
        ErpPurPayment payment = new ErpPurPayment();
        payment.setCode(code);
        payment.setOrgId(ORG_ID);
        payment.setSupplierId(SUPPLIER_ID);
        payment.setBusinessDate(LocalDate.of(2026, 7, 1));
        payment.setCurrencyId(CURRENCY_ID);
        payment.setExchangeRate(BigDecimal.ONE);
        payment.setTotalAmount(total);
        payment.setAmountSource(total);
        payment.setAmountFunctional(total);
        payment.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        payment.setWrittenOffStatus(ErpPurConstants.PAID_STATUS_UNPAID);
        payment.setPosted(false);
        return payment;
    }

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("2202", "应付账款");
            seedSubject("1002", "银行存款");
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

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private ErpFinVoucherBillR findBillLink(String paymentCode) {
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(
                new io.nop.api.core.beans.query.QueryBean());
        return links.stream().filter(l -> paymentCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private long countLines(Long voucherId) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> dao = daoProvider
                .daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }
}
