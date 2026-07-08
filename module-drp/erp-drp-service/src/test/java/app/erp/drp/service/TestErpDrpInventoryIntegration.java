package app.erp.drp.service;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import app.erp.inv.dao.entity.ErpInvTransferOrderLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdWarehouse;
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

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpInventoryIntegration extends JunitAutoTestCase {

    static final Long ORG_ID = 6401L;
    static final Long UOM_ID = 6501L;
    static final Long CURRENCY_ID = 6701L;
    static final Long WH_TARGET = 6101L;
    static final Long WH_SOURCE = 6102L;

    static final Long M_STOCK = 6301L;
    static final Long M_ONORDER = 6302L;
    static final Long M_ZERO = 6303L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testDemandAggregationReadsStockBalance() {
        seedMaterial(M_STOCK);
        seedWarehouse();
        seedParameter(M_STOCK, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_STOCK, bd("30"));

        Long planId = seedPlan("DRP-STOCK");
        runDrpOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), M_STOCK);
        assertNotNull(line);
        assertEquals(0, line.getCurrentStock().compareTo(bd("30")));
        assertEquals(0, line.getNetRequirement().compareTo(bd("70")),
                "net = safetyStock(100) - currentStock(30) = 70");
        assertEquals(0, line.getSuggestedQty().compareTo(bd("70")));
    }

    @Test
    public void testDemandAggregationIncludesOnOrderQty() {
        seedMaterial(M_ONORDER);
        seedWarehouse();
        seedParameter(M_ONORDER, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_ONORDER, bd("30"));
        seedTransferOrder(M_ONORDER, bd("20"));

        Long planId = seedPlan("DRP-ONORDER");
        runDrpOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), M_ONORDER);
        assertNotNull(line);
        assertEquals(0, line.getCurrentStock().compareTo(bd("30")));
        assertEquals(0, line.getOnOrderQty().compareTo(bd("20")));
        assertEquals(0, line.getNetRequirement().compareTo(bd("50")),
                "net = safetyStock(100) - currentStock(30) - onOrderQty(20) = 50");
        assertEquals(0, line.getSuggestedQty().compareTo(bd("50")));
    }

    @Test
    public void testDemandAggregationWithZeroStockGeneratesFullReplenishment() {
        seedMaterial(M_ZERO);
        seedWarehouse();
        seedParameter(M_ZERO, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_ZERO, bd("0"));

        Long planId = seedPlan("DRP-ZERO");
        runDrpOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), M_ZERO);
        assertNotNull(line);
        assertEquals(0, line.getCurrentStock().compareTo(bd("0")));
        assertEquals(0, line.getNetRequirement().compareTo(bd("100")),
                "net = safetyStock(100) - currentStock(0) = 100");
        assertEquals(0, line.getSuggestedQty().compareTo(bd("100")));
    }

    private void seedTransferOrder(Long materialId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvTransferOrder> dao = daoProvider.daoFor(ErpInvTransferOrder.class);
            ErpInvTransferOrder to = new ErpInvTransferOrder();
            to.setCode("TO-DRP-" + materialId);
            to.setOrgId(ORG_ID);
            to.setBusinessDate(LocalDate.of(2026, 7, 1));
            to.setFromWarehouseId(WH_SOURCE);
            to.setToWarehouseId(WH_TARGET);
            to.setDocStatus("APPROVED");
            to.setApproveStatus("APPROVED");
            dao.saveEntity(to);

            IEntityDao<ErpInvTransferOrderLine> lineDao = daoProvider.daoFor(ErpInvTransferOrderLine.class);
            ErpInvTransferOrderLine line = new ErpInvTransferOrderLine();
            line.setTransferId(to.getId());
            line.setLineNo(1);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            lineDao.saveEntity(line);
        });
    }

    private void runDrpOk(Long planId) {
        ApiResponse<?> resp = rpc(mutation, "ErpDrpPlan__runDrp", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "runDrp 应成功: " + resp);
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

    private Long seedPlan(String code) {
        Long id = 6001L + (long) Math.abs(code.hashCode() % 600);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpPlan> dao = daoProvider.daoFor(ErpDrpPlan.class);
            ErpDrpPlan plan = new ErpDrpPlan();
            plan.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
            plan.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
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
