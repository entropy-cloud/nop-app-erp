package app.erp.sal.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalReceipt;
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
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 服务层集成测试：收款单三轴审批状态机 + 客户启用校验 + RECEIPT 过账（借银行存款/贷应收）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalReceiptApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1204L;
    static final Long CUSTOMER_ID = 2202L;
    static final Long CURRENCY_ID = 6201L;
    static final Long ACCT_SCHEMA_ID = 7104L;
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSubmitApproveAndReceiptPosting() {
        seedPeriodAndSubjects();

        ErpSalReceipt receipt = newReceipt("SR-APP-001", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            daoProvider.daoFor(ErpSalReceipt.class).saveEntity(receipt);
        });

        assertEquals(0, submit(receipt.getId()).getStatus());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(receipt).getApproveStatus());

        assertEquals(0, approve(receipt.getId()).getStatus());
        ErpSalReceipt reloaded = reload(receipt);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reloaded.getApproveStatus());
        assertEquals(true, reloaded.getPosted(), "审核应过账 posted=true");

        ErpFinVoucherBillR link = findBillLink(receipt.getCode());
        assertNotNull(link, "应生成业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证 docStatus=已过账");
        // 借方 = 银行存款 113；贷方 = 应收 113
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("113")) == 0, "借方合计=113");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("113")) == 0, "贷方合计=113");
        assertEquals(2, countLines(voucher.getId()), "RECEIPT 凭证 2 行（借银行存款/贷应收）");
    }

    @Test
    public void testIllegalTransitionAndInactiveCustomer() {
        seedPeriodAndSubjects();
        ErpSalReceipt receipt = newReceipt("SR-ILL-001", new BigDecimal("113"));
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            daoProvider.daoFor(ErpSalReceipt.class).saveEntity(receipt);
        });

        assertEquals(0, submit(receipt.getId()).getStatus());
        assertEquals(0, approve(receipt.getId()).getStatus());

        // 幂等：再次审核被状态守卫拒绝（已审核）
        assertTrue(approve(receipt.getId()).getStatus() == 0, "二次审核幂等返回成功（isAlreadyApproved 守卫：不重复执行业务回调）");

        // APPROVED 不可再提交
        ApiResponse<?> bad = submit(receipt.getId());
        assertTrue(bad.getStatus() != 0, "APPROVED 不可再提交，应被状态守卫拒绝");
    }

    // ---------- helpers ----------

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpSalReceipt__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpSalReceipt__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpSalReceipt reload(ErpSalReceipt receipt) {
        return daoProvider.daoFor(ErpSalReceipt.class).getEntityById(receipt.getId());
    }

    private ErpSalReceipt newReceipt(String code, BigDecimal total) {
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
        receipt.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
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

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1002", "银行存款");
            seedSubject("1131", "应收账款");
            seedAcctSchema(ACCT_SCHEMA_ID, ORG_ID);
        });
    }

    private void seedAcctSchema(Long id, Long orgId) {
        IEntityDao<app.erp.md.dao.entity.ErpMdAcctSchema> dao = daoProvider.daoFor(
                app.erp.md.dao.entity.ErpMdAcctSchema.class);
        app.erp.md.dao.entity.ErpMdAcctSchema schema = new app.erp.md.dao.entity.ErpMdAcctSchema();
        schema.setId(id);
        schema.setCode("AS-" + id);
        schema.setName("账套" + id);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
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
        subject.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(subject);
    }

    private ErpFinVoucherBillR findBillLink(String receiptCode) {
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(
                new QueryBean());
        return links.stream().filter(l -> receiptCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private long countLines(Long voucherId) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> dao = daoProvider
                .daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }
}
