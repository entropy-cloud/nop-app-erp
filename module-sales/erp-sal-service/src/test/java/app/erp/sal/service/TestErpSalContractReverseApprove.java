package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalContract;
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
 * 销售合同 reverseApprove 目标态测试（P1-MA2-056，计划 {@code 2026-07-30-0341-3-r1-17} Phase 2）。
 *
 * <p>验证 ErpSalContract INLINE reverseApprove 目标态为 REJECTED（与大 Processor + owner doc §2 +
 * domain-design-guidelines §16.4 对齐），并清空 approvedBy/At。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalContractReverseApprove extends JunitAutoTestCase {

    static final Long CUSTOMER_ID = 22201L;
    static final Long CURRENCY_ID = 62201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testReverseApproveSetsRejectedAndClearsApprover() {
        ormTemplate.runInSession(() -> seedActiveCustomer(CUSTOMER_ID));
        Long id = ormTemplate.runInSession(session -> seedContractApproved("CT-RA-001"));
        ErpSalContract before = reload(id);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, before.getApproveStatus());
        assertEquals("approver-x", before.getApprovedBy(), "前置：approve 已写入 approvedBy");

        assertEquals(0, rpc(mutation, "ErpSalContract__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus());

        ErpSalContract after = reload(id);
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, after.getApproveStatus(),
                "reverseApprove 目标态应为 REJECTED（非 SUBMITTED）");
        assertNull(after.getApprovedBy(), "approvedBy 应清空");
        assertNull(after.getApprovedAt(), "approvedAt 应清空");
    }

    @Test
    public void testSubmitApproveReverseApproveHappyPath() {
        ormTemplate.runInSession(() -> seedActiveCustomer(CUSTOMER_ID));
        Long id = ormTemplate.runInSession(session -> seedContract("CT-HP-001",
                ErpSalConstants.APPROVE_STATUS_UNSUBMITTED));

        assertEquals(0, rpc(mutation, "ErpSalContract__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(), "提交 → SUBMITTED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(id).getApproveStatus());

        assertEquals(0, rpc(mutation, "ErpSalContract__approve",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(), "审核 → APPROVED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(id).getApproveStatus());

        assertEquals(0, rpc(mutation, "ErpSalContract__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(), "反审核 → REJECTED");
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, reload(id).getApproveStatus());
    }

    // ---------- CANCELLED 守卫阻断（P1-MA2-057，Phase 3）----------

    @Test
    public void testCancelledDocReverseApproveBlocked() {
        ormTemplate.runInSession(() -> seedActiveCustomer(CUSTOMER_ID));
        Long id = ormTemplate.runInSession(session -> seedContractCancelled("CT-CN-001",
                ErpSalConstants.APPROVE_STATUS_APPROVED));

        assertNotEquals(0, rpc(mutation, "ErpSalContract__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(),
                "CANCELLED 单据 reverseApprove 应被 isCancelled 守卫阻断");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(id).getApproveStatus(),
                "阻断后 approveStatus 不变（无副轴漂移）");
    }

    @Test
    public void testCancelledDocWithdrawApprovalBlocked() {
        ormTemplate.runInSession(() -> seedActiveCustomer(CUSTOMER_ID));
        Long id = ormTemplate.runInSession(session -> seedContractCancelled("CT-CN-002",
                ErpSalConstants.APPROVE_STATUS_SUBMITTED));

        assertNotEquals(0, rpc(mutation, "ErpSalContract__withdrawApproval",
                ApiRequest.build(Map.of("id", String.valueOf(id)))).getStatus(),
                "CANCELLED 单据 withdrawApproval 应被 isCancelled 守卫阻断");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(id).getApproveStatus());
    }

    // ---------- helpers ----------

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpSalContract reload(Long id) {
        return daoProvider.daoFor(ErpSalContract.class).getEntityById(id);
    }

    private Long seedContractApproved(String code) {
        return seedContract(code, ErpSalConstants.APPROVE_STATUS_APPROVED, true);
    }

    private Long seedContract(String code, String approveStatus) {
        return seedContract(code, approveStatus, false);
    }

    private Long seedContractCancelled(String code, String approveStatus) {
        IEntityDao<ErpSalContract> dao = daoProvider.daoFor(ErpSalContract.class);
        ErpSalContract c = new ErpSalContract();
        c.setCode(code);
        c.setCustomerId(CUSTOMER_ID);
        c.setContractName("合同-" + code);
        c.setCurrencyId(CURRENCY_ID);
        c.setExchangeRate(BigDecimal.ONE);
        c.setBusinessDate(LocalDate.of(2026, 7, 30));
        c.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        c.setApproveStatus(approveStatus);
        dao.saveEntity(c);
        return c.getId();
    }

    private Long seedContract(String code, String approveStatus, boolean withApprover) {
        IEntityDao<ErpSalContract> dao = daoProvider.daoFor(ErpSalContract.class);
        ErpSalContract c = new ErpSalContract();
        c.setCode(code);
        c.setCustomerId(CUSTOMER_ID);
        c.setContractName("合同-" + code);
        c.setCurrencyId(CURRENCY_ID);
        c.setExchangeRate(BigDecimal.ONE);
        c.setBusinessDate(LocalDate.of(2026, 7, 30));
        c.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        c.setApproveStatus(approveStatus);
        if (withApprover) {
            c.setApprovedBy("approver-x");
            c.setApprovedAt(CoreMetrics.currentTimestamp());
        }
        dao.saveEntity(c);
        return c.getId();
    }

    private void seedActiveCustomer(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }
}
