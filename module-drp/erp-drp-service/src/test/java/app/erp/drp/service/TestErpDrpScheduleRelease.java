package app.erp.drp.service;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.pur.dao.entity.ErpPurOrder;
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
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpScheduleRelease extends JunitAutoTestCase {

    static final Long ORG_ID = 6401L;
    static final Long UOM_ID = 6501L;
    static final Long CURRENCY_ID = 6701L;
    static final Long SUPPLIER_ID = 6801L;
    static final Long WH_TARGET = 6101L;
    static final Long WH_SOURCE = 6102L;

    static final Long M_TRANSFER = 6201L;
    static final Long M_PURCHASE = 6202L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testApprovePlanTransitionsLinesToApproved() {
        seedMaterial(M_TRANSFER);
        seedMaterial(M_PURCHASE);
        seedWarehouse();
        seedParameter(M_TRANSFER, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_TRANSFER, bd("30"));
        seedParameter(M_PURCHASE, bd("50"), bd("1"), null, SUPPLIER_ID);
        seedBalance(M_PURCHASE, bd("10"));

        Long planId = seedPlan("DRP-APPR");
        runDrpOk(planId);

        approvePlanOk(planId);
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED,
                daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId).getStatus());

        List<ErpDrpLine> lines = linesOf(planId);
        assertEquals(2, lines.size());
        for (ErpDrpLine line : lines) {
            assertEquals(ErpDrpConstants.DRP_LINE_STATUS_APPROVED, line.getStatus());
            assertNotNull(line.getApprovedQty());
            assertTrue(line.getApprovedQty().signum() > 0);
            assertEquals(0, line.getApprovedQty().compareTo(line.getSuggestedQty()));
        }
    }

    @Test
    public void testReleaseTransferLineGeneratesTransferOrder() {
        seedMaterial(M_TRANSFER);
        seedWarehouse();
        seedParameter(M_TRANSFER, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_TRANSFER, bd("30"));

        Long planId = seedPlan("DRP-REL-TO");
        runDrpOk(planId);
        approvePlanOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), M_TRANSFER);
        assertNotNull(line);
        String toCode = releaseLineOk(line.getId());

        ErpInvTransferOrder to = findTransferOrder(toCode);
        assertNotNull(to);
        assertEquals(WH_SOURCE, to.getFromWarehouseId());
        assertEquals(WH_TARGET, to.getToWarehouseId());

        ErpDrpLine after = daoProvider.daoFor(ErpDrpLine.class).getEntityById(line.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_ORDERED, after.getStatus());
        assertEquals(ErpDrpConstants.ORDER_BILL_TYPE_TRANSFER_ORDER, after.getOrderBillType());
        assertEquals(toCode, after.getOrderBillCode());
    }

    @Test
    public void testReleasePurchaseLineGeneratesPurchaseOrder() {
        seedMaterial(M_PURCHASE);
        seedWarehouse();
        seedParameter(M_PURCHASE, bd("50"), bd("1"), null, SUPPLIER_ID);
        seedBalance(M_PURCHASE, bd("10"));

        Long planId = seedPlan("DRP-REL-PO");
        runDrpOk(planId);
        approvePlanOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), M_PURCHASE);
        assertNotNull(line);
        String poCode = releaseLineOk(line.getId());

        ErpPurOrder po = findPurchaseOrder(poCode);
        assertNotNull(po);
        assertEquals(SUPPLIER_ID, po.getSupplierId());

        ErpDrpLine after = daoProvider.daoFor(ErpDrpLine.class).getEntityById(line.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_ORDERED, after.getStatus());
        assertEquals(ErpDrpConstants.ORDER_BILL_TYPE_PURCHASE_ORDER, after.getOrderBillType());
    }

    @Test
    public void testReleaseApprovedAdvancesPlanToExecuted() {
        seedMaterial(M_TRANSFER);
        seedMaterial(M_PURCHASE);
        seedWarehouse();
        seedParameter(M_TRANSFER, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_TRANSFER, bd("30"));
        seedParameter(M_PURCHASE, bd("50"), bd("1"), null, SUPPLIER_ID);
        seedBalance(M_PURCHASE, bd("10"));

        Long planId = seedPlan("DRP-EXEC");
        runDrpOk(planId);
        approvePlanOk(planId);

        for (ErpDrpLine line : linesOf(planId)) {
            releaseLineOk(line.getId());
        }

        releaseApprovedOk(planId);
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED,
                daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId).getStatus());
    }

    private void runDrpOk(Long planId) {
        ApiResponse<?> resp = runDrp(planId);
        assertEquals(0, resp.getStatus(), "runDrp 应成功: " + resp);
    }

    private ApiResponse<?> runDrp(Long planId) {
        return rpc(mutation, "ErpDrpPlan__runDrp", Map.of("planId", planId));
    }

    private void approvePlanOk(Long planId) {
        ApiResponse<?> resp = rpc(mutation, "ErpDrpPlan__approvePlan", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "approvePlan 应成功: " + resp);
    }

    private String releaseLineOk(Long lineId) {
        ApiResponse<?> resp = releaseLine(lineId);
        assertEquals(0, resp.getStatus(), "releaseLine 应成功: " + resp);
        ErpDrpLine released = daoProvider.daoFor(ErpDrpLine.class).getEntityById(lineId);
        return released.getOrderBillCode();
    }

    private ApiResponse<?> releaseLine(Long lineId) {
        return rpc(mutation, "ErpDrpLine__releaseLine", Map.of("lineId", lineId));
    }

    private void releaseApprovedOk(Long planId) {
        ApiResponse<?> resp = rpc(mutation, "ErpDrpLine__releaseApproved", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "releaseApproved 应成功: " + resp);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private List<ErpDrpLine> linesOf(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
    }

    private ErpDrpLine findLine(List<ErpDrpLine> lines, Long materialId) {
        for (ErpDrpLine l : lines) {
            if (materialId.equals(l.getMaterialId())) {
                return l;
            }
        }
        return null;
    }

    private ErpInvTransferOrder findTransferOrder(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpInvTransferOrder> list = daoProvider.daoFor(ErpInvTransferOrder.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpPurOrder findPurchaseOrder(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpPurOrder> list = daoProvider.daoFor(ErpPurOrder.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long seedPlan(String code) {
        Long id = 6001L + (long) Math.abs(code.hashCode() % 600);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpPlan> dao = daoProvider.daoFor(ErpDrpPlan.class);
            ErpDrpPlan plan = new ErpDrpPlan();
            plan.orm_propValueByName("id", id);
            plan.setCode(code);
            plan.setPlanName("DRP-" + code);
            plan.setPeriodFrom(LocalDate.of(2026, 7, 1));
            plan.setPeriodTo(LocalDate.of(2026, 7, 31));
            plan.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
            plan.setOrgId(ORG_ID);
            dao.saveEntity(plan);
        });
        return id;
    }

    private void seedParameter(Long materialId, BigDecimal safetyStock, BigDecimal orderMultiple,
                               Long sourceWarehouseId, Long supplierId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpParameter> dao = daoProvider.daoFor(ErpDrpParameter.class);
            ErpDrpParameter p = new ErpDrpParameter();
            p.orm_propValueByName("id", 6900L + materialId);
            p.setMaterialId(materialId);
            p.setWarehouseId(WH_TARGET);
            p.setSafetyStock(safetyStock);
            p.setOrderMultiple(orderMultiple);
            p.setPreferredSourceWarehouseId(sourceWarehouseId);
            p.setPreferredSupplierId(supplierId);
            p.setReplenishmentLeadTime(7);
            p.orm_propValueByName("replenishmentMethod", ErpDrpConstants.REPLENISHMENT_METHOD_MIN_MAX);
            p.setOrgId(ORG_ID);
            dao.saveEntity(p);
        });
    }

    private void seedBalance(Long materialId, BigDecimal available) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = new ErpInvStockBalance();
            b.orm_propValueByName("id", 8000L + materialId);
            b.setOrgId(ORG_ID);
            b.setMaterialId(materialId);
            b.setWarehouseId(WH_TARGET);
            b.setTotalQuantity(available);
            b.setAvailableQuantity(available);
            dao.saveEntity(b);
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

    private void seedWarehouse() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
            for (Long wid : new Long[]{WH_TARGET, WH_SOURCE}) {
                ErpMdWarehouse w = new ErpMdWarehouse();
                w.orm_propValueByName("id", wid);
                w.setCode("WH-" + wid);
                w.setName("Warehouse " + wid);
                w.setStatus("ACTIVE");
                dao.saveEntity(w);
            }
            IEntityDao<ErpMdCurrency> cdao = daoProvider.daoFor(ErpMdCurrency.class);
            ErpMdCurrency c = new ErpMdCurrency();
            c.orm_propValueByName("id", CURRENCY_ID);
            c.setCode("CNY");
            c.setName("人民币");
            c.orm_propValueByName("isActive", Boolean.TRUE);
            cdao.saveEntity(c);
        });
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
