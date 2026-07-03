package app.erp.qa.service;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.dao.entity.ErpQaRecallTarget;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 2 召回目标定位 + 客户通知门控 + 批量退货编排测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>locateTargets 经 batchTrace 反查销售出库 → 生成 ErpQaRecallTarget（含 batchId→batchNo 类型桥、客户、发货数量）。</li>
 *   <li>追溯未启用（erp-inv.trace-chain-enabled=false）抛 ERR_TRACE_CHAIN_DISABLED。</li>
 *   <li>notifyCustomers 标记 notifiedAt/notifiedBy + returnStatus=NOTIFIED + recall.notifyCustomer=true。</li>
 *   <li>generateReturns 经 IErpSalReturnBiz 生成退货单 + target.returnStatus=RETURNED。</li>
 *   <li>close 门控：未通知全 target 抛 ERR_RECALL_NOTIFY_INCOMPLETE；通知后可 CLOSED。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/qa/beans/test-mock-sales.beans.xml")
public class TestErpQaRecallLocateNotifyReturn extends JunitAutoTestCase {

    static final Long MATERIAL_ID = 27101L;
    static final Long WAREHOUSE_ID = 37101L;
    static final Long CUSTOMER_ID = 47101L;
    static final Long UOM_ID = 57101L;
    static final Long CURRENCY_ID = 67101L;
    static final Long BATCH_PK = 87101L;
    static final Long DELIVERY_PK = 77101L;
    static final Long MOVE_PK = 97101L;
    static final String BATCH_NO = "RC-BATCH-LNR";
    static final String DELIVERY_CODE = "DLV-RC-LNR";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testLocateNotifyReturnCloseFullFlow() {
        seedTraceFixture();

        Long recallId = registerRecall("RC-LNR-FULL");
        submit(recallId);
        approve(recallId);

        // locateTargets → IN_PROGRESS + 生成 1 个 target
        rpcOk(mutation, "ErpQaRecall__locateTargets", Map.of("recallId", recallId));
        ErpQaRecall inProgress = reloadRecall(recallId);
        assertEquals(ErpQaConstants.RECALL_STATUS_IN_PROGRESS, inProgress.getStatus(), "locateTargets→IN_PROGRESS");

        ErpQaRecallTarget target = singleTargetOf(recallId);
        assertEquals(CUSTOMER_ID, target.getPartnerId(), "target 客户=出库单客户");
        assertEquals(DELIVERY_PK, target.getSalesDeliveryId(), "target 出库单=定位到的出库");
        assertEquals(BATCH_NO, target.getBatchNo(), "target 批号=召回批次解析值");
        assertEquals(0, new BigDecimal("12.0000").compareTo(target.getShippedQty()), "target 发货数量=移动单行数量合计");
        assertEquals(ErpQaConstants.RECALL_TARGET_RETURN_PENDING, target.getReturnStatus());

        // close 门控：未通知 → ERR_RECALL_NOTIFY_INCOMPLETE
        ApiResponse<?> closeBlocked = rpc(mutation, "ErpQaRecall__close", Map.of("recallId", recallId));
        assertEquals(ErpQaErrors.ERR_RECALL_NOTIFY_INCOMPLETE.getErrorCode(), closeBlocked.getCode(),
                "未通知全 target 时 close 应拒绝");

        // notifyCustomers → target NOTIFIED + recall.notifyCustomer=true
        rpcOk(mutation, "ErpQaRecall__notifyCustomers", Map.of("recallId", recallId));
        ErpQaRecallTarget notified = reloadTarget(target.getId());
        assertEquals(ErpQaConstants.RECALL_TARGET_RETURN_NOTIFIED, notified.getReturnStatus(), "通知后 NOTIFIED");
        assertNotNull(notified.getNotifiedAt(), "记录通知时间");
        assertEquals(Boolean.TRUE, reloadRecall(recallId).getNotifyCustomer(), "recall.notifyCustomer=true");

        // generateReturns → 生成退货单 + target RETURNED
        rpcOk(mutation, "ErpQaRecall__generateReturns", Map.of("recallId", recallId));
        ErpQaRecallTarget returned = reloadTarget(target.getId());
        assertEquals(ErpQaConstants.RECALL_TARGET_RETURN_RETURNED, returned.getReturnStatus(), "退货后 RETURNED");
        assertNotNull(returned.getGeneratedReturnId(), "记录生成的退货单 ID");
        assertNotEquals(DELIVERY_PK, returned.getGeneratedReturnId(), "退货单 ID 应为新生成值");

        // close → CLOSED（门控通过）
        rpcOk(mutation, "ErpQaRecall__close", Map.of("recallId", recallId));
        assertEquals(ErpQaConstants.RECALL_STATUS_CLOSED, reloadRecall(recallId).getStatus(), "close→CLOSED");
    }

    @Test
    public void testLocateTargetsBlockedWhenTraceChainDisabled() {
        seedTraceFixture();
        Long recallId = registerRecall("RC-LNR-DISABLED");
        submit(recallId);
        approve(recallId);

        setTraceChainEnabled(false);
        try {
            ApiResponse<?> resp = rpc(mutation, "ErpQaRecall__locateTargets", Map.of("recallId", recallId));
            assertEquals(ErpQaErrors.ERR_TRACE_CHAIN_DISABLED.getErrorCode(), resp.getCode(),
                    "追溯链未启用时 locateTargets 应抛 ERR_TRACE_CHAIN_DISABLED");
            assertEquals(ErpQaConstants.RECALL_STATUS_APPROVED, reloadRecall(recallId).getStatus(),
                    "定位失败不应改变召回状态");
        } finally {
            setTraceChainEnabled(true);
        }
    }

    // ---------- seed ----------

    private void seedMasterData() {
        IEntityDao<ErpMdUoM> uomDao = daoProvider.daoFor(ErpMdUoM.class);
        ErpMdUoM uom = new ErpMdUoM();
        uom.orm_propValueByName("id", UOM_ID);
        uom.setCode("UOM-RC");
        uom.setName("召回测试单位");
        uomDao.saveEntity(uom);

        IEntityDao<ErpMdMaterial> matDao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial material = new ErpMdMaterial();
        material.orm_propValueByName("id", MATERIAL_ID);
        material.setCode("MAT-RC");
        material.setName("召回测试物料");
        material.setMaterialType(10);
        material.setUoMId(UOM_ID);
        material.setStatus(10);
        matDao.saveEntity(material);

        IEntityDao<ErpMdWarehouse> whDao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse warehouse = new ErpMdWarehouse();
        warehouse.orm_propValueByName("id", WAREHOUSE_ID);
        warehouse.setCode("WH-RC");
        warehouse.setName("召回测试仓库");
        warehouse.setStatus(10);
        whDao.saveEntity(warehouse);

        IEntityDao<ErpMdCurrency> curDao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency currency = new ErpMdCurrency();
        currency.orm_propValueByName("id", CURRENCY_ID);
        currency.setCode("CNY");
        currency.setName("人民币");
        curDao.saveEntity(currency);

        IEntityDao<ErpMdPartner> partnerDao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.orm_propValueByName("id", CUSTOMER_ID);
        partner.setCode("CUS-RC");
        partner.setName("召回测试客户");
        partner.setPartnerType(10);
        partner.setStatus(10);
        partnerDao.saveEntity(partner);
    }

    private void seedTraceFixture() {
        ormTemplate.runInSession(session -> {
            seedMasterData();

            // 批次台账（类型桥 batchId→batchNo）
            IEntityDao<ErpInvBatch> batchDao = daoProvider.daoFor(ErpInvBatch.class);
            ErpInvBatch batch = new ErpInvBatch();
            batch.orm_propValueByName("id", BATCH_PK);
            batch.setBatchNo(BATCH_NO);
            batch.setMaterialId(MATERIAL_ID);
            batch.setWarehouseId(WAREHOUSE_ID);
            batch.setTotalQuantity(new BigDecimal("100"));
            batch.setAvailableQuantity(new BigDecimal("100"));
            batch.setStatus(10); // erp-inv/batch-status OPEN
            batchDao.saveEntity(batch);

            // 销售出库单 + 行
            IEntityDao<ErpSalDelivery> dlvDao = daoProvider.daoFor(ErpSalDelivery.class);
            ErpSalDelivery delivery = new ErpSalDelivery();
            delivery.orm_propValueByName("id", DELIVERY_PK);
            delivery.setCode(DELIVERY_CODE);
            delivery.setCustomerId(CUSTOMER_ID);
            delivery.setWarehouseId(WAREHOUSE_ID);
            delivery.setCurrencyId(CURRENCY_ID);
            delivery.setBusinessDate(LocalDate.now());
            delivery.setDocStatus(20); // erp-sal/doc-status ACTIVE
            delivery.setApproveStatus(30); // erp-sal/approve-status APPROVED
            delivery.setPosted(Boolean.FALSE);
            dlvDao.saveEntity(delivery);

            IEntityDao<ErpSalDeliveryLine> dlvLineDao = daoProvider.daoFor(ErpSalDeliveryLine.class);
            ErpSalDeliveryLine dlvLine = new ErpSalDeliveryLine();
            dlvLine.orm_propValueByName("id", DELIVERY_PK * 10 + 1);
            dlvLine.setDeliveryId(DELIVERY_PK);
            dlvLine.setLineNo(1);
            dlvLine.setMaterialId(MATERIAL_ID);
            dlvLine.setUoMId(UOM_ID);
            dlvLine.setQuantity(new BigDecimal("12"));
            dlvLineDao.saveEntity(dlvLine);

            // 库存销售出库移动单（DONE，relatedBillType=ERP_SAL_DELIVERY，行含召回批号）
            IEntityDao<ErpInvStockMove> moveDao = daoProvider.daoFor(ErpInvStockMove.class);
            ErpInvStockMove move = new ErpInvStockMove();
            move.orm_propValueByName("id", MOVE_PK);
            move.setCode("MV-" + DELIVERY_CODE);
            move.setMoveType(20); // erp-inv/operation-type OUTGOING
            move.setBusinessDate(LocalDate.now());
            move.setDocStatus(30); // erp-inv/move-status DONE
            move.setApproveStatus(30); // APPROVED
            move.setRelatedBillType(ErpQaConstants.RELATED_BILL_TYPE_SAL_DELIVERY_INV);
            move.setRelatedBillCode(DELIVERY_CODE);
            move.setPosted(Boolean.FALSE);
            moveDao.saveEntity(move);

            IEntityDao<ErpInvStockMoveLine> moveLineDao = daoProvider.daoFor(ErpInvStockMoveLine.class);
            ErpInvStockMoveLine moveLine = new ErpInvStockMoveLine();
            moveLine.orm_propValueByName("id", MOVE_PK * 10 + 1);
            moveLine.setMoveId(MOVE_PK);
            moveLine.setLineNo(1);
            moveLine.setMaterialId(MATERIAL_ID);
            moveLine.setUoMId(UOM_ID);
            moveLine.setQuantity(new BigDecimal("12"));
            moveLine.setBatchNo(BATCH_NO);
            moveLineDao.saveEntity(moveLine);
            return null;
        });
    }

    // ---------- recall helpers ----------

    private Long registerRecall(String code) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("recallName", "召回-" + code);
        data.put("triggerType", ErpQaConstants.RECALL_TRIGGER_MANUAL);
        data.put("severityLevel", ErpQaConstants.RECALL_SEVERITY_HIGH);
        data.put("businessDate", LocalDate.now().toString());
        data.put("materialId", MATERIAL_ID);
        data.put("batchId", BATCH_PK);
        rpcOk(mutation, "ErpQaRecall__register", Map.of("data", data));
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpQaRecall> list = daoProvider.daoFor(ErpQaRecall.class).findAllByQuery(q);
        assertEquals(1, list.size(), "register 应生成 1 条召回 " + code);
        return list.get(0).getId();
    }

    private void submit(Long recallId) {
        rpcOk(mutation, "ErpQaRecall__submit", Map.of("recallId", recallId));
    }

    private void approve(Long recallId) {
        rpcOk(mutation, "ErpQaRecall__approve", Map.of("recallId", recallId));
    }

    private ErpQaRecall reloadRecall(Long recallId) {
        return daoProvider.daoFor(ErpQaRecall.class).getEntityById(recallId);
    }

    private ErpQaRecallTarget reloadTarget(Long targetId) {
        return daoProvider.daoFor(ErpQaRecallTarget.class).getEntityById(targetId);
    }

    private ErpQaRecallTarget singleTargetOf(Long recallId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recallId", recallId));
        List<ErpQaRecallTarget> list = daoProvider.daoFor(ErpQaRecallTarget.class).findAllByQuery(q);
        assertEquals(1, list.size(), "应生成恰好 1 个召回目标");
        return list.get(0);
    }

    private void setTraceChainEnabled(boolean enabled) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpQaConstants.CONFIG_INV_TRACE_CHAIN_ENABLED, String.valueOf(enabled));
    }

    // ---------- rpc primitives ----------

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功，但返回: " + resp);
    }
}
