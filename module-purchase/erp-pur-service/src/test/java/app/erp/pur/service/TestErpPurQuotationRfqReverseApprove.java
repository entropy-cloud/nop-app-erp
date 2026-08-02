package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurQuotation;
import app.erp.pur.dao.entity.ErpPurRfq;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.time.CoreMetrics;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 采购报价单 / 询价单 reverseApprove 目标态测试（P1-MA2-049，计划 {@code 2026-07-30-0341-3-r1-17} Phase 2）。
 *
 * <p>验证 Quotation/Rfq 两个无大 Processor 的实体的 INLINE reverseApprove 目标态为 REJECTED
 * （与大 Processor + owner doc §2 + domain-design-guidelines §16.4 对齐），并清空 approvedBy/At。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurQuotationRfqReverseApprove extends JunitAutoTestCase {

    static final Long SUPPLIER_ID = 31201L;
    static final Long CURRENCY_ID = 61201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testQuotationReverseApproveSetsRejectedAndClearsApprover() {
        ormTemplate.runInSession(() -> seedActiveSupplier(SUPPLIER_ID));
        Long id = ormTemplate.runInSession(session -> seedQuotationApproved("QT-RA-001"));
        ErpPurQuotation before = reloadQuotation(id);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, before.getApproveStatus());
        assertEquals("approver-x", before.getApprovedBy(), "前置：approve 已写入 approvedBy");

        assertEquals(0, rpc(mutation, "ErpPurQuotation__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus());

        ErpPurQuotation after = reloadQuotation(id);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, after.getApproveStatus(),
                "reverseApprove 目标态应为 REJECTED（非 SUBMITTED）");
        assertNull(after.getApprovedBy(), "approvedBy 应清空");
        assertNull(after.getApprovedAt(), "approvedAt 应清空");
    }

    @Test
    public void testQuotationSubmitApproveReverseApproveHappyPath() {
        ormTemplate.runInSession(() -> seedActiveSupplier(SUPPLIER_ID));
        Long id = ormTemplate.runInSession(session -> seedQuotation("QT-HP-001",
                ErpPurConstants.APPROVE_STATUS_UNSUBMITTED));

        assertEquals(0, rpc(mutation, "ErpPurQuotation__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(), "提交 → SUBMITTED");
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reloadQuotation(id).getApproveStatus());

        assertEquals(0, rpc(mutation, "ErpPurQuotation__approve",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(), "审核 → APPROVED");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reloadQuotation(id).getApproveStatus());

        assertEquals(0, rpc(mutation, "ErpPurQuotation__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(), "反审核 → REJECTED");
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reloadQuotation(id).getApproveStatus());
    }

    @Test
    public void testRfqReverseApproveSetsRejectedAndClearsApprover() {
        Long id = ormTemplate.runInSession(session -> seedRfqApproved("RFQ-RA-001"));
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reloadRfq(id).getApproveStatus());

        assertEquals(0, rpc(mutation, "ErpPurRfq__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus());

        ErpPurRfq after = reloadRfq(id);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, after.getApproveStatus(),
                "reverseApprove 目标态应为 REJECTED（非 SUBMITTED）");
        assertNull(after.getApprovedBy(), "approvedBy 应清空");
        assertNull(after.getApprovedAt(), "approvedAt 应清空");
    }

    @Test
    public void testRfqSubmitApproveReverseApproveHappyPath() {
        Long id = ormTemplate.runInSession(session -> seedRfq("RFQ-HP-001",
                ErpPurConstants.APPROVE_STATUS_UNSUBMITTED));

        assertEquals(0, rpc(mutation, "ErpPurRfq__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reloadRfq(id).getApproveStatus());

        assertEquals(0, rpc(mutation, "ErpPurRfq__approve",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus());
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reloadRfq(id).getApproveStatus());

        assertEquals(0, rpc(mutation, "ErpPurRfq__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(), "反审核 → REJECTED");
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reloadRfq(id).getApproveStatus());
    }

    // ---------- CANCELLED 守卫阻断（P1-MA2-050，Phase 3）----------

    @Test
    public void testQuotationCancelledDocRejectBlocked() {
        ormTemplate.runInSession(() -> seedActiveSupplier(SUPPLIER_ID));
        Long id = ormTemplate.runInSession(session -> seedQuotationCancelled("QT-CN-001",
                ErpPurConstants.APPROVE_STATUS_SUBMITTED));

        assertNotEquals(0, rpc(mutation, "ErpPurQuotation__reject",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(),
                "CANCELLED 单据 reject 应被 isCancelled 守卫阻断");
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reloadQuotation(id).getApproveStatus(),
                "阻断后 approveStatus 不变（无副轴漂移）");
    }

    @Test
    public void testQuotationCancelledDocWithdrawApprovalBlocked() {
        ormTemplate.runInSession(() -> seedActiveSupplier(SUPPLIER_ID));
        Long id = ormTemplate.runInSession(session -> seedQuotationCancelled("QT-CN-002",
                ErpPurConstants.APPROVE_STATUS_SUBMITTED));

        assertNotEquals(0, rpc(mutation, "ErpPurQuotation__withdrawApproval",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(),
                "CANCELLED 单据 withdrawApproval 应被 isCancelled 守卫阻断");
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, reloadQuotation(id).getApproveStatus());
    }

    @Test
    public void testRfqCancelledDocReverseApproveBlocked() {
        Long id = ormTemplate.runInSession(session -> seedRfqCancelled("RFQ-CN-001",
                ErpPurConstants.APPROVE_STATUS_APPROVED));

        assertNotEquals(0, rpc(mutation, "ErpPurRfq__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(),
                "CANCELLED 单据 reverseApprove 应被 isCancelled 守卫阻断");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, reloadRfq(id).getApproveStatus(),
                "阻断后 approveStatus 不变（无副轴漂移）");
    }

    // ---------- helpers ----------

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurQuotation reloadQuotation(Long id) {
        return daoProvider.daoFor(ErpPurQuotation.class).getEntityById(id);
    }

    private ErpPurRfq reloadRfq(Long id) {
        return daoProvider.daoFor(ErpPurRfq.class).getEntityById(id);
    }

    private Long seedQuotationApproved(String code) {
        return seedQuotation(code, ErpPurConstants.APPROVE_STATUS_APPROVED, true);
    }

    private Long seedQuotation(String code, String approveStatus) {
        return seedQuotation(code, approveStatus, false);
    }

    private Long seedQuotationCancelled(String code, String approveStatus) {
        IEntityDao<ErpPurQuotation> dao = daoProvider.daoFor(ErpPurQuotation.class);
        ErpPurQuotation q = new ErpPurQuotation();
        q.setCode(code);
        q.setSupplierId(SUPPLIER_ID);
        q.setCurrencyId(CURRENCY_ID);
        q.setExchangeRate(BigDecimal.ONE);
        q.setBusinessDate(LocalDate.of(2026, 7, 30));
        q.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        q.setApproveStatus(approveStatus);
        dao.saveEntity(q);
        return q.getId();
    }

    private Long seedQuotation(String code, String approveStatus, boolean withApprover) {
        IEntityDao<ErpPurQuotation> dao = daoProvider.daoFor(ErpPurQuotation.class);
        ErpPurQuotation q = new ErpPurQuotation();
        q.setCode(code);
        q.setSupplierId(SUPPLIER_ID);
        q.setCurrencyId(CURRENCY_ID);
        q.setExchangeRate(BigDecimal.ONE);
        q.setBusinessDate(LocalDate.of(2026, 7, 30));
        q.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        q.setApproveStatus(approveStatus);
        if (withApprover) {
            q.setApprovedBy("approver-x");
            q.setApprovedAt(CoreMetrics.currentTimestamp());
        }
        dao.saveEntity(q);
        return q.getId();
    }

    private Long seedRfqApproved(String code) {
        return seedRfq(code, ErpPurConstants.APPROVE_STATUS_APPROVED, true);
    }

    private Long seedRfq(String code, String approveStatus) {
        return seedRfq(code, approveStatus, false);
    }

    private Long seedRfqCancelled(String code, String approveStatus) {
        IEntityDao<ErpPurRfq> dao = daoProvider.daoFor(ErpPurRfq.class);
        ErpPurRfq rfq = new ErpPurRfq();
        rfq.setCode(code);
        rfq.setBusinessDate(LocalDate.of(2026, 7, 30));
        rfq.setDocStatus(ErpPurConstants.DOC_STATUS_CANCELLED);
        rfq.setApproveStatus(approveStatus);
        dao.saveEntity(rfq);
        return rfq.getId();
    }

    private Long seedRfq(String code, String approveStatus, boolean withApprover) {
        IEntityDao<ErpPurRfq> dao = daoProvider.daoFor(ErpPurRfq.class);
        ErpPurRfq rfq = new ErpPurRfq();
        rfq.setCode(code);
        rfq.setBusinessDate(LocalDate.of(2026, 7, 30));
        rfq.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        rfq.setApproveStatus(approveStatus);
        if (withApprover) {
            rfq.setApprovedBy("approver-x");
            rfq.setApprovedAt(CoreMetrics.currentTimestamp());
        }
        dao.saveEntity(rfq);
        return rfq.getId();
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
}
