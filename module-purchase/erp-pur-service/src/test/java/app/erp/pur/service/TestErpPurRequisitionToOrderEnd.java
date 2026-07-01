package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.biz.ConvertToOrderRequest;
import app.erp.pur.biz.IErpPurOrderBiz;
import app.erp.pur.biz.IErpPurRequisitionBiz;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 3 端到端集成测试：请购→订单前端循环打通 + 与 Phase 1/2 衔接。
 *
 * <p>完整链路：建请购→提交→审核 APPROVED→转化生成订单(UNSUBMITTED)→提交→审核 APPROVED
 * →（订单审核纯状态，不下游触发）→作废订单→可重新转化。
 * 证明请购→订单前端循环打通；订单审核 = 纯状态推进（不触发库存/凭证，对齐 state-machine §2）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurRequisitionToOrderEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 1501L;
    static final Long REQUESTER_ID = 2501L;
    static final Long SUPPLIER_ID = 2511L;
    static final Long WAREHOUSE_ID = 3501L;
    static final Long MATERIAL_ID = 4501L;
    static final Long UOM_ID = 5501L;
    static final Long CURRENCY_ID = 6501L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPurRequisitionBiz reqBiz;
    @Inject
    IErpPurOrderBiz orderBiz;

    @Test
    public void testRequisitionToOrderToEnd() {
        ormTemplate.runInSession(() -> {
            seedActiveSupplier(SUPPLIER_ID);
            seedRequisitionWithLine();
        });

        Long reqId = 8501L;
        // 1. 建请购 UNSUBMITTED → 提交 → 审核 APPROVED
        ErpPurRequisition submitted = reqBiz.submit(reqId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus());
        ErpPurRequisition approved = reqBiz.approve(reqId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        // 2. APPROVED 请购 + 补充字段 → 转化生成订单(UNSUBMITTED)
        ConvertToOrderRequest request = newRequest();
        ErpPurOrder order = reqBiz.convertToOrder(reqId, request);
        assertEquals(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, order.getApproveStatus());
        assertEquals(reqId, order.getRequisitionId(), "回链 requisitionId");

        // 3. 订单 UNSUBMITTED → 提交 → 审核 APPROVED（订单审核纯状态推进，不下游触发）
        ErpPurOrder orderSubmitted = orderBiz.submit(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, orderSubmitted.getApproveStatus());
        ErpPurOrder orderApproved = orderBiz.approve(order.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, orderApproved.getApproveStatus());
        // 订单审核不触发库存移动单（与入库单审核触发 generateMove 实质性不同）
        assertEquals(false, orderApproved.getPosted(), "订单审核 posted=false（纯状态推进，不触发库存/凭证）");

        // 4. 作废订单 → 可重新转化（幂等防重复转化的恢复路径）
        ErpPurOrder cancelled = orderBiz.cancel(order.getId());
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus());

        ErpPurOrder secondOrder = reqBiz.convertToOrder(reqId, request);
        assertNotNull(secondOrder.getId());
        assertEquals(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED, secondOrder.getApproveStatus(),
                "作废原订单后可重新转化");
    }

    // ---------- seed helpers ----------

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType(10);
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private void seedRequisitionWithLine() {
        Long reqId = 8501L;
        ErpPurRequisition req = new ErpPurRequisition();
        req.setId(reqId);
        req.setCode("PR-E2E-001");
        req.setOrgId(ORG_ID);
        req.setRequesterId(REQUESTER_ID);
        req.setBusinessDate(LocalDate.of(2026, 7, 1));
        req.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        daoProvider.daoFor(ErpPurRequisition.class).saveEntity(req);

        ErpPurRequisitionLine line = new ErpPurRequisitionLine();
        line.setRequisitionId(reqId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setSuggestedSupplierId(SUPPLIER_ID);
        daoProvider.daoFor(ErpPurRequisitionLine.class).saveEntity(line);
    }

    private ConvertToOrderRequest newRequest() {
        ConvertToOrderRequest request = new ConvertToOrderRequest();
        request.setWarehouseId(WAREHOUSE_ID);
        request.setCurrencyId(CURRENCY_ID);
        Map<Integer, String> prices = new HashMap<>();
        prices.put(1, "5");
        request.setLineUnitPrices(prices);
        return request;
    }
}
