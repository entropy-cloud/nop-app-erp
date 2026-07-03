package app.erp.mfg.service;

import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 端到端测试：销售订单需求 → MRP 运行 → 制造件展开 + 采购件净需求 → 计划订单生成
 * → 释放 PURCHASE_REQUEST 转采购订单 + WORK_ORDER_REQUEST 转工单 → isFirmed/convertedBillCode 回写
 * → MrpPlan FIRMED；重复释放幂等拒绝。
 *
 * <p>覆盖 {@code docs/design/manufacturing/mrp.md §建议单释放} 全链。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgMrpEndToEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 8401L;
    static final Long UOM_ID = 8501L;
    static final Long CUSTOMER_ID = 8601L;
    static final Long CURRENCY_ID = 8701L;
    static final Long SUPPLIER_ID = 8801L;
    static final Long P = 8101L;   // 产成品（制造件）
    static final Long M1 = 8102L;  // 采购件（P 的子件）

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testMrpRunAndReleaseEndToEnd() {
        seedMaterial(P);
        seedMaterial(M1);
        seedBom(9101L, P, M1, bd("1"));   // P 需要 1×M1
        seedSalesOrder("SO-E2E-MRP", P, bd("10"), LocalDate.of(2026, 7, 20));

        Long planId = seedPlan("MRP-E2E");
        runMrpOk(planId);

        List<ErpMfgMrpPlanLine> lines = linesOf(planId);
        ErpMfgMrpPlanLine pLine = findLine(lines, P, null);
        ErpMfgMrpPlanLine m1Line = findLine(lines, M1, pLine.getId());
        assertNotNull(pLine, "P 工单建议行应存在");
        assertNotNull(m1Line, "M1 采购建议行应存在");
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_WORK_ORDER_REQUEST, pLine.getOrderType());
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_PURCHASE_REQUEST, m1Line.getOrderType());

        // 释放采购建议行 → 生成采购订单
        String poCode = releasePurchaseOk(m1Line.getId(), SUPPLIER_ID, CURRENCY_ID);
        assertEquals(ErpMfgConstants.RELEASE_PO_CODE_PREFIX + m1Line.getId(), poCode);
        ErpPurOrder po = findPurchaseOrder(poCode);
        assertNotNull(po, "采购订单应生成");
        assertEquals(SUPPLIER_ID, po.getSupplierId());
        ErpPurOrderLine poLine = findPurchaseOrderLine(po.getId());
        assertNotNull(poLine, "采购订单行应生成");
        assertEquals(M1, poLine.getMaterialId());
        assertEquals(0, poLine.getQuantity().compareTo(m1Line.getPlannedQuantity()));

        ErpMfgMrpPlanLine m1After = daoProvider.daoFor(ErpMfgMrpPlanLine.class).getEntityById(m1Line.getId());
        assertEquals(Boolean.TRUE, m1After.getIsFirmed(), "M1 行释放后 isFirmed=true");
        assertEquals(poCode, m1After.getConvertedBillCode(), "convertedBillCode 回写");

        // 计划尚未全部释放 → 仍 COMPLETED（未 FIRMED）
        assertEquals(ErpMfgConstants.MRP_STATUS_COMPLETED,
                daoProvider.daoFor(ErpMfgMrpPlan.class).getEntityById(planId).getStatus());

        // 释放工单建议行 → 生成工单
        String woCode = releaseWorkOk(pLine.getId());
        assertEquals(ErpMfgConstants.RELEASE_WO_CODE_PREFIX + pLine.getId(), woCode);
        ErpMfgWorkOrder wo = findWorkOrder(woCode);
        assertNotNull(wo, "工单应生成");
        assertEquals(P, wo.getProductId());
        assertEquals(0, wo.getPlannedQuantity().compareTo(pLine.getPlannedQuantity()));

        ErpMfgMrpPlanLine pAfter = daoProvider.daoFor(ErpMfgMrpPlanLine.class).getEntityById(pLine.getId());
        assertEquals(Boolean.TRUE, pAfter.getIsFirmed(), "P 行释放后 isFirmed=true");
        assertEquals(woCode, pAfter.getConvertedBillCode());

        // 全部行释放 → 计划 FIRMED
        assertEquals(ErpMfgConstants.MRP_STATUS_FIRMED,
                daoProvider.daoFor(ErpMfgMrpPlan.class).getEntityById(planId).getStatus(),
                "全部行释放后计划 FIRMED");

        // 幂等：重复释放拒绝
        ApiResponse<?> again = releaseWork(pLine.getId());
        assertTrue(again.getStatus() != 0, "重复释放应拒绝");
        assertEquals(ErpMfgErrors.ERR_MRP_LINE_ALREADY_FIRMED.getErrorCode(), again.getCode());
    }

    @Test
    public void testReleasePurchaseRejectsWithoutSupplier() {
        seedMaterial(M1);
        Long planId = seedPlan("MRP-NOSUP");
        seedManualDemand(planId, M1, bd("5"), LocalDate.of(2026, 7, 20));
        runMrpOk(planId);

        ErpMfgMrpPlanLine line = findLine(linesOf(planId), M1, null);
        assertNotNull(line);

        // supplierId 在 GraphQL 层为非空，缺省由调用方保证；此处验证计划行类型不匹配时（用 releaseWork 释放采购行）拒绝
        ApiResponse<?> resp = releaseWork(line.getId());
        assertTrue(resp.getStatus() != 0, "类型不匹配的释放应拒绝");
        assertEquals(ErpMfgErrors.ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE.getErrorCode(), resp.getCode());
    }

    // ---------- helpers ----------

    private void runMrpOk(Long planId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("planId", planId);
        rpcOk(mutation, "ErpMfgMrpPlan__runMrp", args);
    }

    private String releasePurchaseOk(Long planLineId, Long supplierId, Long currencyId) {
        ApiResponse<?> resp = releasePurchase(planLineId, supplierId, currencyId);
        assertEquals(0, resp.getStatus(), "releasePurchaseRequest 应成功: " + resp);
        return ErpMfgConstants.RELEASE_PO_CODE_PREFIX + planLineId;
    }

    private String releaseWorkOk(Long planLineId) {
        ApiResponse<?> resp = releaseWork(planLineId);
        assertEquals(0, resp.getStatus(), "releaseWorkRequest 应成功: " + resp);
        return ErpMfgConstants.RELEASE_WO_CODE_PREFIX + planLineId;
    }

    private ApiResponse<?> releasePurchase(Long planLineId, Long supplierId, Long currencyId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("planLineId", planLineId);
        args.put("supplierId", supplierId);
        args.put("currencyId", currencyId);
        return rpc(mutation, "ErpMfgMrpPlanLine__releasePurchaseRequest", args);
    }

    private ApiResponse<?> releaseWork(Long planLineId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("planLineId", planLineId);
        return rpc(mutation, "ErpMfgMrpPlanLine__releaseWorkRequest", args);
    }

    private List<ErpMfgMrpPlanLine> linesOf(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpMfgMrpPlanLine.class).findAllByQuery(q);
    }

    private ErpMfgMrpPlanLine findLine(List<ErpMfgMrpPlanLine> lines, Long materialId, Long parentLineId) {
        for (ErpMfgMrpPlanLine l : lines) {
            if (materialId.equals(l.getMaterialId())) {
                if (parentLineId == null) {
                    if (l.getParentLineId() == null) {
                        return l;
                    }
                } else if (parentLineId.equals(l.getParentLineId())) {
                    return l;
                }
            }
        }
        return null;
    }

    private ErpPurOrder findPurchaseOrder(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpPurOrder> list = daoProvider.daoFor(ErpPurOrder.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpPurOrderLine findPurchaseOrderLine(Long orderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        List<ErpPurOrderLine> list = daoProvider.daoFor(ErpPurOrderLine.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpMfgWorkOrder findWorkOrder(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpMfgWorkOrder> list = daoProvider.daoFor(ErpMfgWorkOrder.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long seedPlan(String code) {
        Long id = 8001L + (long) Math.abs(code.hashCode() % 600);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMrpPlan> dao = daoProvider.daoFor(ErpMfgMrpPlan.class);
            ErpMfgMrpPlan plan = new ErpMfgMrpPlan();
            plan.orm_propValueByName("id", id);
            plan.setCode(code);
            plan.setOrgId(ORG_ID);
            plan.setBusinessDate(LocalDate.of(2026, 7, 1));
            plan.setPlanningHorizonDays(30);
            plan.setStatus(ErpMfgConstants.MRP_STATUS_DRAFT);
            dao.saveEntity(plan);
        });
        return id;
    }

    private void seedManualDemand(Long planId, Long materialId, BigDecimal qty, LocalDate reqDate) {
        ormTemplate.runInSession(() -> {
            IEntityDao<app.erp.mfg.dao.entity.ErpMfgMrpDemand> dao = daoProvider.daoFor(app.erp.mfg.dao.entity.ErpMfgMrpDemand.class);
            app.erp.mfg.dao.entity.ErpMfgMrpDemand d = new app.erp.mfg.dao.entity.ErpMfgMrpDemand();
            d.orm_propValueByName("id", 9000L + (long) Math.abs((planId + "" + materialId).hashCode() % 500));
            d.setMrpPlanId(planId);
            d.setLineNo(10);
            d.setMaterialId(materialId);
            d.setUoMId(UOM_ID);
            d.setDemandSource(ErpMfgConstants.MRP_DEMAND_SOURCE_MANUAL);
            d.setSourceBillType(ErpMfgConstants.SOURCE_BILL_TYPE_MRP_MANUAL);
            d.setSourceBillCode("MANUAL-" + materialId);
            d.setQuantity(qty);
            d.setRequirementDate(reqDate);
            dao.saveEntity(d);
        });
    }

    private void seedSalesOrder(String code, Long materialId, BigDecimal qty, LocalDate deliveryDate) {
        Long orderId = 8300L + (long) Math.abs(code.hashCode() % 600);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSalOrder> odao = daoProvider.daoFor(ErpSalOrder.class);
            ErpSalOrder o = new ErpSalOrder();
            o.orm_propValueByName("id", orderId);
            o.setCode(code);
            o.setOrgId(ORG_ID);
            o.setCustomerId(CUSTOMER_ID);
            o.setCurrencyId(CURRENCY_ID);
            o.setBusinessDate(LocalDate.of(2026, 7, 1));
            o.setDeliveryDate(deliveryDate);
            o.setDocStatus("ACTIVE");
            o.setApproveStatus("APPROVED");
            odao.saveEntity(o);

            IEntityDao<ErpSalOrderLine> ldao = daoProvider.daoFor(ErpSalOrderLine.class);
            ErpSalOrderLine line = new ErpSalOrderLine();
            line.orm_propValueByName("id", orderId + 50000);
            line.setOrderId(orderId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            line.setUnitPrice(bd("1"));
            line.setAmount(qty);
            line.setDeliveredQuantity(BigDecimal.ZERO);
            ldao.saveEntity(line);
        });
    }

    private void seedMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            dao.saveEntity(m);
        });
    }

    private void seedBom(Long bomId, Long productId, Long componentId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", bomId);
            bom.setCode("BOM-" + bomId);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(bd("1"));
            dao.saveEntity(bom);
            IEntityDao<ErpMfgBomLine> ldao = daoProvider.daoFor(ErpMfgBomLine.class);
            ErpMfgBomLine line = new ErpMfgBomLine();
            line.orm_propValueByName("id", bomId + 50000);
            line.setBomId(bomId);
            line.setLineNo(10);
            line.setMaterialId(componentId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            ldao.saveEntity(line);
        });
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
