package app.erp.qa.service;

import app.erp.qa.dao.entity.ErpQaRecall;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 召回状态机测试：register（→OPEN）、submit（→SUBMITTED）、approve（→APPROVED，强制审批）、
 * reject（→CANCELLED）、cancel（非终态→CANCELLED）、非法迁移抛错、CRITICAL 标记。
 *
 * <p>覆盖 {@code docs/design/quality/recall.md §召回状态机}（不含 locateTargets/notifyCustomers/generateReturns，
 * 属 Phase 2 跨域编排）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaRecallStateMachine extends JunitAutoTestCase {

    static final Long MATERIAL_ID = 7401L;
    static final Long BATCH_ID = 8801L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testRegisterSubmitApproveFullFlow() {
        Long recallId = registerRecall("RC-FLOW", ErpQaConstants.RECALL_SEVERITY_HIGH);
        ErpQaRecall recall = reload(recallId);
        assertEquals(ErpQaConstants.RECALL_STATUS_OPEN, recall.getStatus(), "register→OPEN");
        assertEquals(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED, recall.getApproveStatus());
        assertEquals(Boolean.FALSE, recall.getNotifyCustomer());

        rpcOk(mutation, "ErpQaRecall__submitForApproval", Map.of("id", String.valueOf(recallId)));
        assertEquals(ErpQaConstants.APPROVE_STATUS_SUBMITTED, reload(recallId).getApproveStatus());

        rpcOk(mutation, "ErpQaRecall__approve", Map.of("id", String.valueOf(recallId)));
        ErpQaRecall approved = reload(recallId);
        assertEquals(ErpQaConstants.RECALL_STATUS_APPROVED, approved.getStatus(), "approve→APPROVED");
        assertEquals(ErpQaConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());
        assertNotNull(approved.getApprovedAt(), "记录审批时间");
        assertNotNull(approved.getApprovedBy(), "记录审批人");
    }

    @Test
    public void testApproveWithoutSubmitBlockedByForcedApproval() {
        Long recallId = registerRecall("RC-NOAPP", ErpQaConstants.RECALL_SEVERITY_MEDIUM);
        // 强制审批（默认 true）：未经 submit 直接 approve → ERR_RECALL_APPROVAL_REQUIRED
        ApiResponse<?> resp = rpc(mutation, "ErpQaRecall__approve", Map.of("id", String.valueOf(recallId)));
        assertTrue(resp.getStatus() != 0,
                "强制审批下未 submit 直接 approve 应拒绝");
        assertEquals(ErpQaConstants.RECALL_STATUS_OPEN, reload(recallId).getStatus());
    }

    @Test
    public void testRejectFromSubmitted() {
        Long recallId = registerRecall("RC-REJ", ErpQaConstants.RECALL_SEVERITY_LOW);
        rpcOk(mutation, "ErpQaRecall__submitForApproval", Map.of("id", String.valueOf(recallId)));
        rpcOk(mutation, "ErpQaRecall__reject", Map.of("id", String.valueOf(recallId)));
        ErpQaRecall rejected = reload(recallId);
        assertEquals(ErpQaConstants.RECALL_STATUS_CANCELLED, rejected.getStatus(), "reject→CANCELLED");
        assertEquals(ErpQaConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus());
    }

    @Test
    public void testCancelFromOpen() {
        Long recallId = registerRecall("RC-CANCEL", ErpQaConstants.RECALL_SEVERITY_MEDIUM);
        rpcOk(mutation, "ErpQaRecall__cancel", Map.of("recallId", recallId));
        assertEquals(ErpQaConstants.RECALL_STATUS_CANCELLED, reload(recallId).getStatus(), "cancel→CANCELLED");
    }

    @Test
    public void testIllegalTransitionsRejected() {
        Long recallId = registerRecall("RC-ILLEGAL", ErpQaConstants.RECALL_SEVERITY_MEDIUM);

        // close 从 OPEN → 非法（须 IN_PROGRESS）
        ApiResponse<?> closeResp = rpc(mutation, "ErpQaRecall__close", Map.of("recallId", recallId));
        assertEquals(ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION.getErrorCode(), closeResp.getCode(),
                "OPEN→close 非法");

        // reject 未经 submit → 非法
        ApiResponse<?> rejectResp = rpc(mutation, "ErpQaRecall__reject", Map.of("id", String.valueOf(recallId)));
        assertTrue(rejectResp.getStatus() != 0,
                "UNSUBMITTED→reject 非法");
    }

    @Test
    public void testCriticalSeverityRecall() {
        // CRITICAL 严重程度召回全流程（标记需高层，本期以状态机为准）
        Long recallId = registerRecall("RC-CRIT", ErpQaConstants.RECALL_SEVERITY_CRITICAL);
        assertEquals(ErpQaConstants.RECALL_SEVERITY_CRITICAL, reload(recallId).getSeverityLevel());
        rpcOk(mutation, "ErpQaRecall__submitForApproval", Map.of("id", String.valueOf(recallId)));
        rpcOk(mutation, "ErpQaRecall__approve", Map.of("id", String.valueOf(recallId)));
        assertEquals(ErpQaConstants.RECALL_STATUS_APPROVED, reload(recallId).getStatus(),
                "CRITICAL 召回正常审批");
    }

    /**
     * withdrawApproval inline-script 提取验证（plan 2026-07-30-2046-1 R5.7 Phase 3）。
     * 原 xbiz withdrawApproval 为 inline {@code <c:script>}（NopScriptError），提取为 per-mutation
     * {@link app.erp.qa.service.processor.ErpQaRecallWithdrawApprovalProcessor} Java hook + xbiz inject 委托。
     *
     * <p>验证两点：
     * <ol>
     *   <li>负向守卫：非 SUBMITTED 状态 withdraw → {@link ErpQaErrors#ERR_INVALID_RECALL_STATUS_TRANSITION}
     *       （语义等价替代原 wf {@code nop.err.wf.approve.invalid-status}，param 键改为域 recallCode/
     *       currentStatus/expectedStatus）。</li>
     *   <li>正向激活：submit→SUBMITTED → withdraw→UNSUBMITTED，证明 inline 提取后 per-mutation 运行时路径生效。</li>
     * </ol>
     */
    @Test
    public void testWithdrawApprovalGuardAndExtraction() {
        Long recallId = registerRecall("RC-WITHDRAW", ErpQaConstants.RECALL_SEVERITY_MEDIUM);

        // 负向守卫：UNSUBMITTED → withdrawApproval 非法（须 SUBMITTED）
        ApiResponse<?> guardResp = rpc(mutation, "ErpQaRecall__withdrawApproval",
                Map.of("id", String.valueOf(recallId)));
        assertEquals(ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION.getErrorCode(), guardResp.getCode(),
                "UNSUBMITTED→withdrawApproval 须抛域非法迁移错误码（替代原 wf invalid-status）");
        assertEquals(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED, reload(recallId).getApproveStatus(),
                "守卫拒绝不应改变状态");

        // 正向激活：submit → SUBMITTED → withdraw → UNSUBMITTED
        rpcOk(mutation, "ErpQaRecall__submitForApproval", Map.of("id", String.valueOf(recallId)));
        assertEquals(ErpQaConstants.APPROVE_STATUS_SUBMITTED, reload(recallId).getApproveStatus(),
                "submit→SUBMITTED");
        rpcOk(mutation, "ErpQaRecall__withdrawApproval", Map.of("id", String.valueOf(recallId)));
        assertEquals(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED, reload(recallId).getApproveStatus(),
                "withdraw→UNSUBMITTED（inline 提取激活 per-mutation 运行时路径）");
    }

    // ---------- helpers ----------

    private ErpQaRecall reload(Long recallId) {
        return daoProvider.daoFor(ErpQaRecall.class).getEntityById(recallId);
    }

    private Long registerRecall(String code, String severity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("recallName", "召回-" + code);
        data.put("triggerType", ErpQaConstants.RECALL_TRIGGER_MANUAL);
        data.put("severityLevel", severity);
        data.put("businessDate", CoreMetrics.currentDate().toString());
        data.put("materialId", MATERIAL_ID);
        data.put("batchId", BATCH_ID);
        ApiResponse<?> resp = rpc(mutation, "ErpQaRecall__register", Map.of("data", data));
        assertEquals(0, resp.getStatus(), "register 应成功: " + resp);
        // 经 code 反查生成的召回（id 由 seq 生成）
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpQaRecall> list = daoProvider.daoFor(ErpQaRecall.class).findAllByQuery(q);
        assertEquals(1, list.size(), "register 应生成 1 条召回 " + code);
        return list.get(0).getId();
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功，但返回: " + resp);
    }
}
