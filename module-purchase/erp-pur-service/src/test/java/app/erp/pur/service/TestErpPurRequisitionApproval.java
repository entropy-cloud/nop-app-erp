package app.erp.pur.service;

import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
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
 * Phase 1 服务层集成测试：采购请购单三轴审批状态机。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpPurRequisition__submit/approve/reject/withdrawSubmit/reverseApprove/cancel}，
 * 引擎负责建 session/事务/管道（直调缺 OrmSession 会报错，见 lessons/04）。
 * 请购头无供应商，状态机不做供应商校验；请购 approve 仅状态推进（请购无自动下游触发，转化是显式独立动作）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurRequisitionApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1201L;
    static final Long REQUESTER_ID = 2201L;
    static final Long MATERIAL_ID = 4201L;
    static final Long UOM_ID = 5201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testReqSubmitApproveRejectResubmit() {
        ErpPurRequisition req = newRequisition("PR-SUBMIT-001");
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req));

        assertEquals(0, submit(req.getId()).getStatus());
        ErpPurRequisition submitted = daoProvider.daoFor(ErpPurRequisition.class).getEntityById(req.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        assertEquals(0, approve(req.getId()).getStatus());
        ErpPurRequisition approved = daoProvider.daoFor(ErpPurRequisition.class).getEntityById(req.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "审核通过 → APPROVED");
    }

    @Test
    public void testReqRejectAndResubmit() {
        ErpPurRequisition req = newRequisition("PR-REJ-001");
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req));

        assertEquals(0, submit(req.getId()).getStatus());
        assertEquals(0, reject(req.getId()).getStatus());
        ErpPurRequisition rejected = daoProvider.daoFor(ErpPurRequisition.class).getEntityById(req.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        assertEquals(0, submit(req.getId()).getStatus());
        ErpPurRequisition resubmitted = daoProvider.daoFor(ErpPurRequisition.class).getEntityById(req.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testReqIllegalTransitionRejected() {
        ErpPurRequisition req = newRequisition("PR-ILL-001");
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req));

        assertEquals(0, submit(req.getId()).getStatus());
        assertEquals(0, approve(req.getId()).getStatus());
        ErpPurRequisition approved = daoProvider.daoFor(ErpPurRequisition.class).getEntityById(req.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        ApiResponse<?> bad = submit(req.getId());
        assertEquals(ErpPurErrors.ERR_REQ_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "APPROVED 不可再提交，应返回非法迁移错误");
        bad = withdrawSubmit(req.getId());
        assertEquals(ErpPurErrors.ERR_REQ_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "APPROVED 不可撤回提交，应返回非法迁移错误");

        assertEquals(0, reverseApprove(req.getId()).getStatus());
        ErpPurRequisition reversed = daoProvider.daoFor(ErpPurRequisition.class).getEntityById(req.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus(),
                "反审核目标态 = REJECTED 非 UNSUBMITTED");
    }

    @Test
    public void testReqCancelFromDraft() {
        ErpPurRequisition req = newRequisition("PR-CANCEL-001");
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req));

        assertEquals(0, cancel(req.getId()).getStatus());
        ErpPurRequisition cancelled = daoProvider.daoFor(ErpPurRequisition.class).getEntityById(req.getId());
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        ApiResponse<?> bad = submit(req.getId());
        assertEquals(ErpPurErrors.ERR_REQ_ILLEGAL_DOC_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "已作废请购单不可提交，应返回非法单据状态迁移错误");
    }

    // ---------- helpers ----------

    private ApiResponse<?> submit(Long requisitionId) {
        return executeRpc(mutation, "ErpPurRequisition__submit",
                ApiRequest.build(Map.of("requisitionId", requisitionId)));
    }

    private ApiResponse<?> withdrawSubmit(Long requisitionId) {
        return executeRpc(mutation, "ErpPurRequisition__withdrawSubmit",
                ApiRequest.build(Map.of("requisitionId", requisitionId)));
    }

    private ApiResponse<?> approve(Long requisitionId) {
        return executeRpc(mutation, "ErpPurRequisition__approve",
                ApiRequest.build(Map.of("requisitionId", requisitionId)));
    }

    private ApiResponse<?> reject(Long requisitionId) {
        return executeRpc(mutation, "ErpPurRequisition__reject",
                ApiRequest.build(Map.of("requisitionId", requisitionId)));
    }

    private ApiResponse<?> reverseApprove(Long requisitionId) {
        return executeRpc(mutation, "ErpPurRequisition__reverseApprove",
                ApiRequest.build(Map.of("requisitionId", requisitionId)));
    }

    private ApiResponse<?> cancel(Long requisitionId) {
        return executeRpc(mutation, "ErpPurRequisition__cancel",
                ApiRequest.build(Map.of("requisitionId", requisitionId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpPurRequisition newRequisition(String code) {
        ErpPurRequisition req = new ErpPurRequisition();
        req.setCode(code);
        req.setOrgId(ORG_ID);
        req.setRequesterId(REQUESTER_ID);
        req.setBusinessDate(LocalDate.of(2026, 7, 1));
        req.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        return req;
    }

    private void saveRequisitionWithLine(ErpPurRequisition req) {
        daoProvider.daoFor(ErpPurRequisition.class).saveEntity(req);
        IEntityDao<ErpPurRequisitionLine> lineDao = daoProvider.daoFor(ErpPurRequisitionLine.class);
        ErpPurRequisitionLine line = new ErpPurRequisitionLine();
        line.setRequisitionId(req.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        lineDao.saveEntity(line);
    }
}
