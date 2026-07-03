package app.erp.qa.service;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.dao.entity.ErpQaRecallTarget;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 3 召回端到端测试：
 * <ul>
 *   <li>NCR 升级（upgradeToRecall）→ NCR.status=ESCALATED_TO_RECALL + 生成 ErpQaRecall（BATCH_NCR_UPGRADE + sourceNcrId + 继承物料/严重程度）→
 *       submit/approve → locateTargets（追溯链反查生成 target）→ notifyCustomers → generateReturns → close 门控通过。</li>
 *   <li>MANUAL 触发全链路。</li>
 *   <li>severityLevel=CRITICAL 标记全链路。</li>
 * </ul>
 *
 * <p>覆盖 {@code docs/design/quality/recall.md §召回全场景}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/qa/beans/test-mock-sales.beans.xml")
public class TestErpQaRecallE2E extends JunitAutoTestCase {

    static final Long MATERIAL_ID = 28201L;
    static final Long WAREHOUSE_ID = 38201L;
    static final Long CUSTOMER_ID = 48201L;
    static final Long UOM_ID = 58201L;
    static final Long CURRENCY_ID = 68201L;
    static final Long BATCH_PK = 88201L;
    static final Long DELIVERY_PK = 78201L;
    static final Long MOVE_PK = 98201L;
    static final String BATCH_NO = "RC-BATCH-E2E";
    static final String DELIVERY_CODE = "DLV-RC-E2E";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testNcrUpgradeToRecallFullChain() {
        seedTraceFixture();
        Long ncrId = seedNcr("NCR-E2E-UPG", 30 /* erp-qa/severity HIGH=30 */);

        // NCR 升级 → 建召回
        Long recallId = upgradeToRecallAndGetId(ncrId);
        assertEquals(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
                daoProvider.daoFor(ErpQaNonConformance.class).getEntityById(ncrId).getStatus(),
                "NCR 升级后状态=ESCALATED_TO_RECALL");

        ErpQaRecall recall = reloadRecall(recallId);
        assertEquals(ErpQaConstants.RECALL_STATUS_OPEN, recall.getStatus(), "升级生成的召回=OPEN");
        assertEquals(ErpQaConstants.RECALL_TRIGGER_BATCH_NCR_UPGRADE, recall.getTriggerType());
        assertEquals(ncrId, recall.getSourceNcrId(), "召回关联来源 NCR");
        assertEquals(MATERIAL_ID, recall.getMaterialId(), "召回继承 NCR 物料");
        // NCR severity HIGH(30) → recall severity HIGH(30) 对齐
        assertEquals(ErpQaConstants.RECALL_SEVERITY_HIGH, recall.getSeverityLevel());

        // 升级生成的召回未带批次（NCR 无批次列）→ 质量组补登受影响批次后定位
        setRecallBatch(recallId, BATCH_PK);

        rpcOk(mutation, "ErpQaRecall__submit", Map.of("recallId", recallId));
        rpcOk(mutation, "ErpQaRecall__approve", Map.of("recallId", recallId));
        runFullChainFromApproved(recallId);
    }

    @Test
    public void testManualTriggerFullChain() {
        seedTraceFixture();
        Long recallId = registerManualRecall("RC-E2E-MANUAL", ErpQaConstants.RECALL_SEVERITY_MEDIUM);
        rpcOk(mutation, "ErpQaRecall__submit", Map.of("recallId", recallId));
        rpcOk(mutation, "ErpQaRecall__approve", Map.of("recallId", recallId));
        runFullChainFromApproved(recallId);
    }

    @Test
    public void testCriticalSeverityFullChain() {
        seedTraceFixture();
        Long recallId = registerManualRecall("RC-E2E-CRIT", ErpQaConstants.RECALL_SEVERITY_CRITICAL);
        assertEquals(ErpQaConstants.RECALL_SEVERITY_CRITICAL, reloadRecall(recallId).getSeverityLevel());
        rpcOk(mutation, "ErpQaRecall__submit", Map.of("recallId", recallId));
        rpcOk(mutation, "ErpQaRecall__approve", Map.of("recallId", recallId));
        runFullChainFromApproved(recallId);
    }

    // ---------- shared chain (APPROVED → locate → notify → return → close) ----------

    private void runFullChainFromApproved(Long recallId) {
        rpcOk(mutation, "ErpQaRecall__locateTargets", Map.of("recallId", recallId));
        assertEquals(ErpQaConstants.RECALL_STATUS_IN_PROGRESS, reloadRecall(recallId).getStatus());

        QueryBean q = new QueryBean();
        q.addFilter(eq("recallId", recallId));
        List<ErpQaRecallTarget> targets = daoProvider.daoFor(ErpQaRecallTarget.class).findAllByQuery(q);
        assertEquals(1, targets.size(), "定位到 1 个受影响客户");
        assertEquals(CUSTOMER_ID, targets.get(0).getPartnerId());

        rpcOk(mutation, "ErpQaRecall__notifyCustomers", Map.of("recallId", recallId));
        rpcOk(mutation, "ErpQaRecall__generateReturns", Map.of("recallId", recallId));
        assertEquals(ErpQaConstants.RECALL_TARGET_RETURN_RETURNED,
                daoProvider.daoFor(ErpQaRecallTarget.class).getEntityById(targets.get(0).getId()).getReturnStatus(),
                "target RETURNED");

        rpcOk(mutation, "ErpQaRecall__close", Map.of("recallId", recallId));
        assertEquals(ErpQaConstants.RECALL_STATUS_CLOSED, reloadRecall(recallId).getStatus(), "close→CLOSED");
    }

    // ---------- seed ----------

    private void seedTraceFixture() {
        ormTemplate.runInSession(session -> {
            seedMasterData();

            IEntityDao<ErpInvBatch> batchDao = daoProvider.daoFor(ErpInvBatch.class);
            ErpInvBatch batch = new ErpInvBatch();
            batch.orm_propValueByName("id", BATCH_PK);
            batch.setBatchNo(BATCH_NO);
            batch.setMaterialId(MATERIAL_ID);
            batch.setWarehouseId(WAREHOUSE_ID);
            batch.setTotalQuantity(new BigDecimal("100"));
            batch.setAvailableQuantity(new BigDecimal("100"));
            batch.setStatus(10);
            batchDao.saveEntity(batch);

            IEntityDao<ErpInvStockMove> moveDao = daoProvider.daoFor(ErpInvStockMove.class);
            ErpInvStockMove move = new ErpInvStockMove();
            move.orm_propValueByName("id", MOVE_PK);
            move.setCode("MV-" + DELIVERY_CODE);
            move.setMoveType(20);
            move.setBusinessDate(LocalDate.now());
            move.setDocStatus(30);
            move.setApproveStatus(30);
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
            moveLine.setQuantity(new BigDecimal("8"));
            moveLine.setBatchNo(BATCH_NO);
            moveLineDao.saveEntity(moveLine);
            return null;
        });
    }

    private void seedMasterData() {
        IEntityDao<ErpMdUoM> uomDao = daoProvider.daoFor(ErpMdUoM.class);
        ErpMdUoM uom = new ErpMdUoM();
        uom.orm_propValueByName("id", UOM_ID);
        uom.setCode("UOM-E2E");
        uom.setName("E2E单位");
        uomDao.saveEntity(uom);

        IEntityDao<ErpMdMaterial> matDao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial material = new ErpMdMaterial();
        material.orm_propValueByName("id", MATERIAL_ID);
        material.setCode("MAT-E2E");
        material.setName("E2E物料");
        material.setMaterialType(10);
        material.setUoMId(UOM_ID);
        material.setStatus(10);
        matDao.saveEntity(material);

        IEntityDao<ErpMdWarehouse> whDao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse warehouse = new ErpMdWarehouse();
        warehouse.orm_propValueByName("id", WAREHOUSE_ID);
        warehouse.setCode("WH-E2E");
        warehouse.setName("E2E仓库");
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
        partner.setCode("CUS-E2E");
        partner.setName("E2E客户");
        partner.setPartnerType(10);
        partner.setStatus(10);
        partnerDao.saveEntity(partner);

        // 销售出库单 + 行（generateReturns 经 IErpSalDeliveryBiz 读 warehouse/currency/uoM）
        IEntityDao<ErpSalDelivery> dlvDao = daoProvider.daoFor(ErpSalDelivery.class);
        ErpSalDelivery delivery = new ErpSalDelivery();
        delivery.orm_propValueByName("id", DELIVERY_PK);
        delivery.setCode(DELIVERY_CODE);
        delivery.setCustomerId(CUSTOMER_ID);
        delivery.setWarehouseId(WAREHOUSE_ID);
        delivery.setCurrencyId(CURRENCY_ID);
        delivery.setBusinessDate(LocalDate.now());
        delivery.setDocStatus(20);
        delivery.setApproveStatus(30);
        delivery.setPosted(Boolean.FALSE);
        dlvDao.saveEntity(delivery);

        IEntityDao<ErpSalDeliveryLine> dlvLineDao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        ErpSalDeliveryLine dlvLine = new ErpSalDeliveryLine();
        dlvLine.orm_propValueByName("id", DELIVERY_PK * 10 + 1);
        dlvLine.setDeliveryId(DELIVERY_PK);
        dlvLine.setLineNo(1);
        dlvLine.setMaterialId(MATERIAL_ID);
        dlvLine.setUoMId(UOM_ID);
        dlvLine.setQuantity(new BigDecimal("8"));
        dlvLineDao.saveEntity(dlvLine);
    }

    private Long seedNcr(String code, int severity) {
        Long id = 30200L + (long) (Math.abs(code.hashCode()) % 1000);
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
            ErpQaNonConformance ncr = new ErpQaNonConformance();
            ncr.orm_propValueByName("id", id);
            ncr.setCode(code);
            ncr.setNcrDate(LocalDate.now());
            ncr.setMaterialId(MATERIAL_ID);
            ncr.setSeverity(severity);
            ncr.setStatus(ErpQaConstants.NCR_STATUS_IN_REVIEW);
            ncr.setSourceType(ErpQaConstants.NCR_SOURCE_TYPE_INSPECTION);
            ncr.setSourceCode("INS-" + code);
            ncr.setDescription("E2E 不合格描述");
            dao.saveEntity(ncr);
            return null;
        });
        return id;
    }

    private void setRecallBatch(Long recallId, Long batchId) {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpQaRecall> dao = daoProvider.daoFor(ErpQaRecall.class);
            ErpQaRecall recall = dao.getEntityById(recallId);
            recall.setBatchId(batchId);
            dao.saveOrUpdateEntity(recall);
            return null;
        });
    }

    // ---------- recall helpers ----------

    private Long registerManualRecall(String code, int severity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("recallName", "召回-" + code);
        data.put("triggerType", ErpQaConstants.RECALL_TRIGGER_MANUAL);
        data.put("severityLevel", severity);
        data.put("businessDate", LocalDate.now().toString());
        data.put("materialId", MATERIAL_ID);
        data.put("batchId", BATCH_PK);
        rpcOk(mutation, "ErpQaRecall__register", Map.of("data", data));
        return recallIdByCode(code);
    }

    private Long upgradeToRecallAndGetId(Long ncrId) {
        rpcOk(mutation, "ErpQaNonConformance__upgradeToRecall", Map.of("ncrId", ncrId));
        return recallIdByCode("RC-FROM-NCR-" + ncrId);
    }

    private Long recallIdByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpQaRecall> list = daoProvider.daoFor(ErpQaRecall.class).findAllByQuery(q);
        assertEquals(1, list.size(), "应存在 1 条召回 " + code);
        return list.get(0).getId();
    }

    private ErpQaRecall reloadRecall(Long recallId) {
        return daoProvider.daoFor(ErpQaRecall.class).getEntityById(recallId);
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
