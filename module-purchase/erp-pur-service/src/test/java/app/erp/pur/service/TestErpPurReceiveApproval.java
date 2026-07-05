package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
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
 * Phase 1 服务层集成测试：采购入库三轴审批状态机 + 供应商启用校验。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpPurReceive__submit/reject/approve/withdrawSubmit/cancel}，
 * 引擎负责建 session/事务/管道（直调缺 OrmSession 会报错，见 lessons/04）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReceiveApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long SUPPLIER_ID = 2101L;
    static final Long WAREHOUSE_ID = 3101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSubmitRejectResubmit() {
        ErpPurReceive receive = newReceive("PR-SUBMIT-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveReceiveWithLine(receive);
        });

        assertEquals(0, submit(receive.getId()).getStatus());
        ErpPurReceive submitted = dao().getEntityById(receive.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        assertEquals(0, reject(receive.getId()).getStatus());
        ErpPurReceive rejected = dao().getEntityById(receive.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        assertEquals(0, submit(receive.getId()).getStatus());
        ErpPurReceive resubmitted = dao().getEntityById(receive.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testIllegalTransitionRejected() {
        ErpPurReceive receive = newReceive("PR-ILL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveReceiveWithLine(receive);
        });

        assertEquals(0, submit(receive.getId()).getStatus());
        assertEquals(0, approve(receive.getId()).getStatus());
        ErpPurReceive approved = dao().getEntityById(receive.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        ApiResponse<?> bad = submit(receive.getId());
        assertEquals(-1, bad.getStatus(),
                "APPROVED 不可再提交：平台守卫仅接受 UNSUBMITTED/null/REJECTED 源态");
        bad = withdrawSubmit(receive.getId());
        assertEquals(-1, bad.getStatus(),
                "APPROVED 不可撤回审批：withdrawApproval 守卫仅接受 SUBMITTED");
    }

    @Test
    public void testInactiveSupplierRejected() {
        ErpPurReceive receive = newReceive("PR-INACTIVE-001");
        ormTemplate.runInSession(() -> {
            seedSupplier(SUPPLIER_ID, "INACTIVE");
            saveReceiveWithLine(receive);
        });

        ApiResponse<?> bad = submit(receive.getId());
        assertEquals(ErpPurErrors.ERR_PARTNER_INACTIVE.getErrorCode(), bad.getCode(),
                "供应商停用 → submit 应返回 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testCancelFromDraft() {
        ErpPurReceive receive = newReceive("PR-CANCEL-001");
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            saveReceiveWithLine(receive);
        });

        assertEquals(0, cancel(receive.getId()).getStatus());
        ErpPurReceive cancelled = dao().getEntityById(receive.getId());
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        ApiResponse<?> bad = submit(receive.getId());
        assertEquals(ErpPurErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "已作废单据不可提交，应返回非法单据状态迁移错误");
    }

    // ---------- helpers ----------

    private ApiResponse<?> submit(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(receiveId))));
    }

    private ApiResponse<?> withdrawSubmit(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__withdrawApproval", ApiRequest.build(Map.of("id", String.valueOf(receiveId))));
    }

    private ApiResponse<?> approve(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__approve", ApiRequest.build(Map.of("id", String.valueOf(receiveId))));
    }

    private ApiResponse<?> reject(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__reject", ApiRequest.build(Map.of("id", String.valueOf(receiveId))));
    }

    private ApiResponse<?> cancel(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__cancel", ApiRequest.build(Map.of("receiveId", receiveId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private IEntityDao<ErpPurReceive> dao() {
        return daoProvider.daoFor(ErpPurReceive.class);
    }

    private ErpPurReceive newReceive(String code) {
        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode(code);
        receive.setOrgId(ORG_ID);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setExchangeRate(new BigDecimal("1"));
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        return receive;
    }

    private void saveReceiveWithLine(ErpPurReceive receive) {
        dao().saveEntity(receive);
        IEntityDao<ErpPurReceiveLine> lineDao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setReceiveId(receive.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("5"));
        lineDao.saveEntity(line);
    }

    private void seedActiveSupplier(Long id) {
        seedSupplier(id, ErpPurConstants.PARTNER_STATUS_ACTIVE);
    }

    private void seedSupplier(Long id, String status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(status);
        dao.saveEntity(partner);
    }
}
