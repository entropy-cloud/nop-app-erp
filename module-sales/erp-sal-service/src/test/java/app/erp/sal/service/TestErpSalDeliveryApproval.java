package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
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
 * Phase 1 服务层集成测试：销售出库三轴审批状态机 + 客户启用校验。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpSalDelivery__submit/approve/reject/withdrawSubmit/cancel}，
 * 引擎负责建 session/事务/管道（直调缺 OrmSession 会报错，见 lessons/04）。测试自建客户/行明细后断言状态迁移。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalDeliveryApproval extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long CUSTOMER_ID = 2101L;
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
        ErpSalDelivery delivery = newDelivery("SD-SUBMIT-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveDeliveryWithLine(delivery);
        });

        assertEquals(0, submit(delivery.getId()).getStatus(), "提交应成功");
        ErpSalDelivery submitted = reload(delivery.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        assertEquals(0, reject(delivery.getId()).getStatus(), "驳回应成功");
        ErpSalDelivery rejected = reload(delivery.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        assertEquals(0, submit(delivery.getId()).getStatus());
        ErpSalDelivery resubmitted = reload(delivery.getId());
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testIllegalTransitionRejected() {
        ErpSalDelivery delivery = newDelivery("SD-ILL-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveDeliveryWithLine(delivery);
        });

        // UNSUBMITTED→approve 非法（仅 SUBMITTED 可审核）
        ApiResponse<?> bad = approve(delivery.getId());
        assertEquals(ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "未提交不可直接审核，应返回非法迁移错误");

        // submit→withdrawSubmit 后再 withdrawSubmit（此时 UNSUBMITTED）非法
        assertEquals(0, submit(delivery.getId()).getStatus());
        assertEquals(0, withdrawSubmit(delivery.getId()).getStatus());
        bad = withdrawSubmit(delivery.getId());
        assertEquals(ErpSalErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "UNSUBMITTED 不可撤回提交，应返回非法迁移错误");
    }

    @Test
    public void testInactiveCustomerRejected() {
        ErpSalDelivery delivery = newDelivery("SD-INACTIVE-001");
        ormTemplate.runInSession(() -> {
            seedCustomer(CUSTOMER_ID, 20);
            saveDeliveryWithLine(delivery);
        });

        ApiResponse<?> bad = submit(delivery.getId());
        assertEquals(ErpSalErrors.ERR_PARTNER_INACTIVE.getErrorCode(), bad.getCode(),
                "客户停用 → submit 应返回 ERR_PARTNER_INACTIVE");
    }

    @Test
    public void testCancelFromDraft() {
        ErpSalDelivery delivery = newDelivery("SD-CANCEL-001");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID);
            saveDeliveryWithLine(delivery);
        });

        assertEquals(0, cancel(delivery.getId()).getStatus(), "作废应成功");
        ErpSalDelivery cancelled = reload(delivery.getId());
        assertEquals(ErpSalConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        ApiResponse<?> bad = submit(delivery.getId());
        assertEquals(ErpSalErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION.getErrorCode(), bad.getCode(),
                "已作废单据不可提交，应返回非法单据状态迁移错误");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__submit", ApiRequest.build(Map.of("deliveryId", deliveryId)));
    }

    private ApiResponse<?> withdrawSubmit(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__withdrawSubmit", ApiRequest.build(Map.of("deliveryId", deliveryId)));
    }

    private ApiResponse<?> approve(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__approve", ApiRequest.build(Map.of("deliveryId", deliveryId)));
    }

    private ApiResponse<?> reject(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__reject", ApiRequest.build(Map.of("deliveryId", deliveryId)));
    }

    private ApiResponse<?> cancel(Long deliveryId) {
        return executeRpc(mutation, "ErpSalDelivery__cancel", ApiRequest.build(Map.of("deliveryId", deliveryId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- helpers ----------

    private ErpSalDelivery reload(Long deliveryId) {
        return daoProvider.daoFor(ErpSalDelivery.class).getEntityById(deliveryId);
    }

    private ErpSalDelivery newDelivery(String code) {
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.setCode(code);
        delivery.setOrgId(ORG_ID);
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setWarehouseId(WAREHOUSE_ID);
        delivery.setBusinessDate(LocalDate.of(2026, 7, 1));
        delivery.setCurrencyId(CURRENCY_ID);
        delivery.setExchangeRate(new BigDecimal("1"));
        delivery.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        delivery.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        delivery.setPosted(false);
        return delivery;
    }

    private void saveDeliveryWithLine(ErpSalDelivery delivery) {
        daoProvider.daoFor(ErpSalDelivery.class).saveEntity(delivery);
        IEntityDao<ErpSalDeliveryLine> lineDao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        ErpSalDeliveryLine line = new ErpSalDeliveryLine();
        line.setDeliveryId(delivery.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        lineDao.saveEntity(line);
    }

    private void seedActiveCustomer(Long id) {
        seedCustomer(id, ErpSalConstants.PARTNER_STATUS_ACTIVE);
    }

    private void seedCustomer(Long id, int status) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType(10);
        partner.setStatus(status);
        dao.saveEntity(partner);
    }
}
