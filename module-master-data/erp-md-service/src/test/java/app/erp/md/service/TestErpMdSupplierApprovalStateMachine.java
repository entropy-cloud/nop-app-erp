package app.erp.md.service;

import app.erp.md.biz.IErpMdSupplierApprovalBiz;
import app.erp.md.dao.entity.ErpMdSupplierApproval;
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

import java.time.LocalDate;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * AVL 准入 6 态状态机集成测试（{@code docs/plans/2026-07-03-1707-2} Phase 1）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpMdSupplierApproval__apply/approve/probate/suspend/reinstate/reject/suspendByPartner}，
 * 直接断言状态迁移与非法迁移错误码。{@code @NopTestConfig} 启用本地 H2 + 建表，无快照录制（精确断言场景）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdSupplierApprovalStateMachine extends JunitAutoTestCase {

    static final Long PARTNER_ID = 7001L;
    static final Long CATEGORY_ID = 7101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testFullHappyPath() {
        ErpMdSupplierApproval approval = seedApproval(ErpMdConstants.APPROVAL_STATUS_APPLIED, "ISO9001");
        ormTemplate.runInSession(() -> approvalDao().saveEntity(approval));

        assertEquals(0, approve(approval.getId()).getStatus(), "APPLIED → APPROVED");
        assertEquals(ErpMdConstants.APPROVAL_STATUS_APPROVED, reload(approval.getId()).getStatus());

        assertEquals(0, probate(approval.getId()).getStatus(), "APPROVED → PROBATION");
        assertEquals(ErpMdConstants.APPROVAL_STATUS_PROBATION, reload(approval.getId()).getStatus());

        assertEquals(0, approve(approval.getId()).getStatus(), "PROBATION → APPROVED（试用通过）");
        assertEquals(ErpMdConstants.APPROVAL_STATUS_APPROVED, reload(approval.getId()).getStatus());

        assertEquals(0, suspend(approval.getId()).getStatus(), "APPROVED → SUSPENDED");
        assertEquals(ErpMdConstants.APPROVAL_STATUS_SUSPENDED, reload(approval.getId()).getStatus());

        assertEquals(0, reinstate(approval.getId()).getStatus(), "SUSPENDED → APPROVED（恢复需审批）");
        assertEquals(ErpMdConstants.APPROVAL_STATUS_APPROVED, reload(approval.getId()).getStatus());
    }

    @Test
    public void testIllegalTransitions() {
        ErpMdSupplierApproval approval = seedApproval(ErpMdConstants.APPROVAL_STATUS_APPROVED, "ISO9001");
        ormTemplate.runInSession(() -> approvalDao().saveEntity(approval));

        // approve 要求 APPLIED/PROBATION，当前 APPROVED → 非法
        ApiResponse<?> bad = approve(approval.getId());
        assertEquals(ErpMdErrors.ERR_INVALID_APPROVAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "APPROVED 不可再次 approve");

        // reject 要求 APPLIED，当前 APPROVED → 非法
        bad = reject(approval.getId());
        assertEquals(ErpMdErrors.ERR_INVALID_APPROVAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "APPROVED 不可 reject");

        // reinstate 要求 SUSPENDED，当前 APPROVED → 非法
        bad = reinstate(approval.getId());
        assertEquals(ErpMdErrors.ERR_INVALID_APPROVAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "APPROVED 不可 reinstate");
    }

    @Test
    public void testApproveRequiresQualification() {
        ErpMdSupplierApproval approval = seedApproval(ErpMdConstants.APPROVAL_STATUS_APPLIED, null);
        approval.setValidTo(null); // 缺失效日期 → 资质不完整
        ormTemplate.runInSession(() -> approvalDao().saveEntity(approval));

        ApiResponse<?> bad = approve(approval.getId());
        assertEquals(ErpMdErrors.ERR_APPROVAL_QUALIFICATION_MISSING.getErrorCode(), bad.getCode(),
                "缺资质/有效期不可批准");
    }

    @Test
    public void testRejectFromApplied() {
        ErpMdSupplierApproval approval = seedApproval(ErpMdConstants.APPROVAL_STATUS_APPLIED, "ISO9001");
        ormTemplate.runInSession(() -> approvalDao().saveEntity(approval));

        assertEquals(0, reject(approval.getId()).getStatus(), "APPLIED → REJECTED");
        assertEquals(ErpMdConstants.APPROVAL_STATUS_REJECTED, reload(approval.getId()).getStatus());
    }

    @Test
    public void testSuspendByPartnerSuspendsAllActive() {
        ErpMdSupplierApproval a1 = seedApproval(ErpMdConstants.APPROVAL_STATUS_APPROVED, "ISO9001");
        a1.setPartnerId(7701L);
        ErpMdSupplierApproval a2 = seedApproval(ErpMdConstants.APPROVAL_STATUS_PROBATION, "ISO14001");
        a2.setPartnerId(7701L);
        ErpMdSupplierApproval a3 = seedApproval(ErpMdConstants.APPROVAL_STATUS_REJECTED, null);
        a3.setPartnerId(7701L);
        ormTemplate.runInSession(() -> {
            approvalDao().saveEntity(a1);
            approvalDao().saveEntity(a2);
            approvalDao().saveEntity(a3);
        });

        ApiResponse<?> resp = executeRpc(mutation, "ErpMdSupplierApproval__suspendByPartner",
                ApiRequest.build(Map.of("partnerId", 7701L)));
        assertEquals(0, resp.getStatus(), "suspendByPartner 应成功");
        assertEquals(2, ((Number) resp.getData()).intValue(), "仅暂停 APPROVED+PROBATION 共 2 条，REJECTED 跳过");

        assertEquals(ErpMdConstants.APPROVAL_STATUS_SUSPENDED, reload(a1.getId()).getStatus());
        assertEquals(ErpMdConstants.APPROVAL_STATUS_SUSPENDED, reload(a2.getId()).getStatus());
        assertEquals(ErpMdConstants.APPROVAL_STATUS_REJECTED, reload(a3.getId()).getStatus(), "REJECTED 不变");
    }

    @Test
    public void testFindEffectiveByPartner() {
        ErpMdSupplierApproval approval = seedApproval(ErpMdConstants.APPROVAL_STATUS_APPROVED, "ISO9001");
        approval.setPartnerId(7801L);
        ormTemplate.runInSession(() -> approvalDao().saveEntity(approval));

        ApiResponse<?> resp = executeRpc(query, "ErpMdSupplierApproval__findEffectiveByPartner",
                ApiRequest.build(Map.of("partnerId", 7801L)));
        assertEquals(0, resp.getStatus());
        assertEquals(ErpMdConstants.APPROVAL_STATUS_APPROVED,
                ((Number) ((Map<?, ?>) resp.getData()).get("status")).intValue());

        // 不存在的供应商返回 null（无有效资格）
        ApiResponse<?> none = executeRpc(query, "ErpMdSupplierApproval__findEffectiveByPartner",
                ApiRequest.build(Map.of("partnerId", 99999L)));
        assertEquals(0, none.getStatus());
        assertNull(none.getData(), "无有效资格 → 返回 null");
    }

    // ---------- helpers ----------

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpMdSupplierApproval__approve", ApiRequest.build(Map.of("approvalId", id)));
    }

    private ApiResponse<?> probate(Long id) {
        return executeRpc(mutation, "ErpMdSupplierApproval__probate", ApiRequest.build(Map.of("approvalId", id)));
    }

    private ApiResponse<?> suspend(Long id) {
        return executeRpc(mutation, "ErpMdSupplierApproval__suspend", ApiRequest.build(Map.of("approvalId", id)));
    }

    private ApiResponse<?> reinstate(Long id) {
        return executeRpc(mutation, "ErpMdSupplierApproval__reinstate", ApiRequest.build(Map.of("approvalId", id)));
    }

    private ApiResponse<?> reject(Long id) {
        return executeRpc(mutation, "ErpMdSupplierApproval__reject", ApiRequest.build(Map.of("approvalId", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpMdSupplierApproval seedApproval(int status, String qualificationDoc) {
        ErpMdSupplierApproval approval = new ErpMdSupplierApproval();
        approval.setPartnerId(PARTNER_ID);
        approval.setApprovalType(ErpMdConstants.APPROVAL_TYPE_NEW);
        approval.setMaterialCategoryId(CATEGORY_ID);
        approval.setValidFrom(LocalDate.of(2026, 1, 1));
        approval.setValidTo(LocalDate.of(2027, 1, 1));
        approval.setQualificationDoc(qualificationDoc);
        approval.setStatus(status);
        return approval;
    }

    private ErpMdSupplierApproval reload(Long id) {
        return approvalDao().getEntityById(id);
    }

    private IEntityDao<ErpMdSupplierApproval> approvalDao() {
        return daoProvider.daoFor(ErpMdSupplierApproval.class);
    }
}
