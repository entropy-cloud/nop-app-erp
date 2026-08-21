package app.erp.ast.service;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstMovement;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 资产移动单 reverseApprove 目标态测试（P1-MA2-058，计划 {@code 2026-07-30-0341-3-r1-17} Phase 2）。
 *
 * <p>验证 ErpAstMovement INLINE reverseApprove 目标态为 REJECTED（与大 Processor + owner doc §2 +
 * domain-design-guidelines §16.4 对齐），并清空 approvedBy/At。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstMovementReverseApprove extends JunitAutoTestCase {

    static final String ORG_ID = "1";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testReverseApproveSetsRejectedAndClearsApprover() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovementApproved("MV-RA-001", assetId));
        ErpAstMovement before = reload(id);
        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, before.getApproveStatus());
        assertEquals("approver-x", before.getApprovedBy(), "前置：approve 已写入 approvedBy");

        assertEquals(0, rpc(mutation, "ErpAstMovement__reverseApprove",
                ApiRequest.build(Map.of("id", id))).getStatus());

        ErpAstMovement after = reload(id);
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, after.getApproveStatus(),
                "reverseApprove 目标态应为 REJECTED（非 SUBMITTED）");
        assertNull(after.getApprovedBy(), "approvedBy 应清空");
        assertNull(after.getApprovedAt(), "approvedAt 应清空");
    }

    @Test
    public void testSubmitApproveReverseApproveHappyPath() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovement("MV-HP-001", assetId,
                ErpAstConstants.APPROVE_STATUS_UNSUBMITTED));

        assertEquals(0, rpc(mutation, "ErpAstMovement__submitForApproval",
                ApiRequest.build(Map.of("id", id))).getStatus(), "提交 → SUBMITTED");
        assertEquals(ErpAstConstants.APPROVE_STATUS_SUBMITTED, reload(id).getApproveStatus());

        assertEquals(0, rpc(mutation, "ErpAstMovement__approve",
                ApiRequest.build(Map.of("id", id))).getStatus(), "审核 → APPROVED");
        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, reload(id).getApproveStatus());

        assertEquals(0, rpc(mutation, "ErpAstMovement__reverseApprove",
                ApiRequest.build(Map.of("id", id))).getStatus(), "反审核 → REJECTED");
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, reload(id).getApproveStatus());
    }

    // ---------- CANCELLED 守卫阻断（P1-MA2-059，Phase 3）----------

    @Test
    public void testCancelledDocRejectBlocked() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovementCancelled("MV-CN-001", assetId,
                ErpAstConstants.APPROVE_STATUS_SUBMITTED));

        assertNotEquals(0, rpc(mutation, "ErpAstMovement__reject",
                ApiRequest.build(Map.of("id", id))).getStatus(),
                "CANCELLED 单据 reject 应被 isCancelled 守卫阻断");
        assertEquals(ErpAstConstants.APPROVE_STATUS_SUBMITTED, reload(id).getApproveStatus(),
                "阻断后 approveStatus 不变（无副轴漂移）");
    }

    @Test
    public void testCancelledDocReverseApproveBlocked() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovementCancelled("MV-CN-002", assetId,
                ErpAstConstants.APPROVE_STATUS_APPROVED));

        assertNotEquals(0, rpc(mutation, "ErpAstMovement__reverseApprove",
                ApiRequest.build(Map.of("id", id))).getStatus(),
                "CANCELLED 单据 reverseApprove 应被 isCancelled 守卫阻断");
        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, reload(id).getApproveStatus());
    }

    // ---------- withdrawApproval 回归 + 剩余 3 处 CANCELLED 守卫 + 非法态拒绝（plan 2026-08-13-0805-2 Phase 2 补缺） ----------

    @Test
    public void testWithdrawApprovalHappyPath() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovement("MV-WD-001", assetId,
                ErpAstConstants.APPROVE_STATUS_SUBMITTED));

        assertEquals(0, rpc(mutation, "ErpAstMovement__withdrawApproval",
                ApiRequest.build(Map.of("id", id))).getStatus(), "撤回 → UNSUBMITTED");
        assertEquals(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED, reload(id).getApproveStatus());
    }

    @Test
    public void testWithdrawApprovalIllegalStatusBlocked() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovement("MV-WD-002", assetId,
                ErpAstConstants.APPROVE_STATUS_APPROVED));

        assertNotEquals(0, rpc(mutation, "ErpAstMovement__withdrawApproval",
                ApiRequest.build(Map.of("id", id))).getStatus(),
                "APPROVED 状态 withdrawApproval 应被 Bean 矩阵守卫阻断");
        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, reload(id).getApproveStatus());
    }

    @Test
    public void testCancelledDocSubmitForApprovalBlocked() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovementCancelled("MV-CN-003", assetId,
                ErpAstConstants.APPROVE_STATUS_UNSUBMITTED));

        assertNotEquals(0, rpc(mutation, "ErpAstMovement__submitForApproval",
                ApiRequest.build(Map.of("id", id))).getStatus(),
                "CANCELLED 单据 submitForApproval 应被 isCancelled 守卫阻断");
        assertEquals(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED, reload(id).getApproveStatus());
    }

    @Test
    public void testCancelledDocApproveBlocked() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovementCancelled("MV-CN-004", assetId,
                ErpAstConstants.APPROVE_STATUS_SUBMITTED));

        assertNotEquals(0, rpc(mutation, "ErpAstMovement__approve",
                ApiRequest.build(Map.of("id", id))).getStatus(),
                "CANCELLED 单据 approve 应被 isCancelled 守卫阻断");
        assertEquals(ErpAstConstants.APPROVE_STATUS_SUBMITTED, reload(id).getApproveStatus());
    }

    @Test
    public void testCancelledDocWithdrawApprovalBlocked() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovementCancelled("MV-CN-005", assetId,
                ErpAstConstants.APPROVE_STATUS_SUBMITTED));

        assertNotEquals(0, rpc(mutation, "ErpAstMovement__withdrawApproval",
                ApiRequest.build(Map.of("id", id))).getStatus(),
                "CANCELLED 单据 withdrawApproval 应被 isCancelled 守卫阻断");
        assertEquals(ErpAstConstants.APPROVE_STATUS_SUBMITTED, reload(id).getApproveStatus());
    }

    @Test
    public void testRejectSetsApprovedByAndApprovedAt() {
        String assetId = ormTemplate.runInSession(session -> seedAsset());
        String id = ormTemplate.runInSession(session -> seedMovement("MV-RJ-001", assetId,
                ErpAstConstants.APPROVE_STATUS_SUBMITTED));

        assertEquals(0, rpc(mutation, "ErpAstMovement__reject",
                ApiRequest.build(Map.of("id", id))).getStatus(), "驳回 → REJECTED");
        ErpAstMovement after = reload(id);
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, after.getApproveStatus());
        assertNotNull(after.getApprovedBy(), "reject 应写入 approvedBy");
        assertNotNull(after.getApprovedAt(), "reject 应写入 approvedAt");
    }

    // ---------- helpers ----------

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpAstMovement reload(String id) {
        return daoProvider.daoFor(ErpAstMovement.class).getEntityById(id);
    }

    private String seedAsset() {
        String subjectId = AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
        String categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-MV", "设备类",
                "STRAIGHT_LINE", 60, subjectId, subjectId, subjectId);
        return AstTestSupport.seedAsset(daoProvider, "AST-MV-001", "测试设备", categoryId, ORG_ID,
                new BigDecimal("10000"), new BigDecimal("500"), "STRAIGHT_LINE", 60, "IN_SERVICE");
    }

    private String seedMovementApproved(String code, String assetId) {
        return seedMovement(code, assetId, ErpAstConstants.APPROVE_STATUS_APPROVED, true);
    }

    private String seedMovement(String code, String assetId, String approveStatus) {
        return seedMovement(code, assetId, approveStatus, false);
    }

    private String seedMovementCancelled(String code, String assetId, String approveStatus) {
        IEntityDao<ErpAstMovement> dao = daoProvider.daoFor(ErpAstMovement.class);
        ErpAstMovement m = new ErpAstMovement();
        m.setCode(code);
        m.setAssetId(assetId);
        m.setBusinessDate(LocalDate.of(2026, 7, 30));
        m.setFromDate(LocalDate.of(2026, 7, 30));
        m.setCurrencyId("1");
        m.setExchangeRate(BigDecimal.ONE);
        m.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
        m.setApproveStatus(approveStatus);
        dao.saveEntity(m);
        return m.getId();
    }

    private String seedMovement(String code, String assetId, String approveStatus, boolean withApprover) {
        IEntityDao<ErpAstMovement> dao = daoProvider.daoFor(ErpAstMovement.class);
        ErpAstMovement m = new ErpAstMovement();
        m.setCode(code);
        m.setAssetId(assetId);
        m.setBusinessDate(LocalDate.of(2026, 7, 30));
        m.setFromDate(LocalDate.of(2026, 7, 30));
        m.setCurrencyId("1");
        m.setExchangeRate(BigDecimal.ONE);
        m.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        m.setApproveStatus(approveStatus);
        if (withApprover) {
            m.setApprovedBy("approver-x");
            m.setApprovedAt(CoreMetrics.currentTimestamp());
        }
        dao.saveEntity(m);
        return m.getId();
    }
}
