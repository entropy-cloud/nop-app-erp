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
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 5 测试：DRP 净需求计算 + 释放 + 状态机全路径 + 失败路径。
 *
 * <p>覆盖 {@code docs/design/drp/README.md §DRP 流程}、{@code drp/state-machine.md}、{@code drp/use-cases.md UC-DRP-02/03}：
 * 净需求公式、orderMultiple 向上取整、TRANSFER/PURCHASE 决策、释放生成调拨单/采购单、状态机全路径、失败路径。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpEngine extends JunitAutoTestCase {

    @RegisterExtension
    static DrpFrozenClockExtension frozenClock = new DrpFrozenClockExtension();

    static final Long ORG_ID = 6401L;
    static final Long UOM_ID = 6501L;
    static final Long CURRENCY_ID = 6701L;
    static final Long SUPPLIER_ID = 6801L;
    static final Long WH_TARGET = 6101L;  // 目标仓（DRP 补货目标）
    static final Long WH_SOURCE = 6102L;  // 来源仓（TRANSFER 调出仓）

    static final Long M_TRANSFER = 6201L; // TRANSFER 路径
    static final Long M_PURCHASE = 6202L; // PURCHASE 路径
    static final Long M_MULTIPLE = 6203L; // orderMultiple 取整
    static final Long M_NEG = 6204L;      // 负净需求归零

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testNetRequirementAndReplenishmentTypeDecision() {
        seedMaterial(M_TRANSFER);
        seedMaterial(M_PURCHASE);
        seedWarehouse();
        // M_TRANSFER: safetyStock=100, stock=30 → net=70; preferredSourceWarehouse=WH_SOURCE → TRANSFER
        seedParameter(M_TRANSFER, bd("100"), bd("10"), WH_SOURCE, null);
        seedBalance(M_TRANSFER, bd("30"));
        // M_PURCHASE: safetyStock=50, stock=10 → net=40; preferredSupplier=SUPPLIER → PURCHASE
        seedParameter(M_PURCHASE, bd("50"), bd("1"), null, SUPPLIER_ID);
        seedBalance(M_PURCHASE, bd("10"));

        Long planId = seedPlan("DRP-1");
        runDrpOk(planId);

        ErpDrpPlan plan = daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId);
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED, plan.getStatus());
        assertEquals(0, plan.getTotalReplenishmentQty().compareTo(bd("110")),
                "总补货量=70(TRANSFER向上取整到10倍数=70)+40(PURCHASE)=110");

        List<ErpDrpLine> lines = linesOf(planId);
        ErpDrpLine transferLine = findLine(lines, M_TRANSFER);
        ErpDrpLine purchaseLine = findLine(lines, M_PURCHASE);

        assertNotNull(transferLine, "TRANSFER 行应存在");
        assertEquals(ErpDrpConstants.REPLENISHMENT_TYPE_TRANSFER, transferLine.getReplenishmentType());
        assertEquals(WH_SOURCE, transferLine.getSourceWarehouseId());
        assertEquals(0, transferLine.getNetRequirement().compareTo(bd("70")));
        assertEquals(0, transferLine.getSuggestedQty().compareTo(bd("70")), "orderMultiple=10, 70 已是倍数");
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED, transferLine.getStatus());

        assertNotNull(purchaseLine, "PURCHASE 行应存在");
        assertEquals(ErpDrpConstants.REPLENISHMENT_TYPE_PURCHASE, purchaseLine.getReplenishmentType());
        assertEquals(0, purchaseLine.getNetRequirement().compareTo(bd("40")));
        assertEquals(0, purchaseLine.getSuggestedQty().compareTo(bd("40")));
    }

    @Test
    public void testOrderMultipleRounding() {
        seedMaterial(M_MULTIPLE);
        seedWarehouse();
        // safetyStock=100, stock=45 → net=55; orderMultiple=10 → suggestedQty=60
        seedParameter(M_MULTIPLE, bd("100"), bd("10"), WH_SOURCE, null);
        seedBalance(M_MULTIPLE, bd("45"));

        Long planId = seedPlan("DRP-MULT");
        runDrpOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), M_MULTIPLE);
        assertEquals(0, line.getNetRequirement().compareTo(bd("55")));
        assertEquals(0, line.getSuggestedQty().compareTo(bd("60")), "55 向上取整到 10 倍数 = 60");
    }

    @Test
    public void testNegativeNetClampedToZero() {
        seedMaterial(M_NEG);
        seedWarehouse();
        // safetyStock=20, stock=50 → net = 20 - 50 = -30 → 0
        seedParameter(M_NEG, bd("20"), bd("1"), WH_SOURCE, null);
        seedBalance(M_NEG, bd("50"));

        Long planId = seedPlan("DRP-NEG");
        runDrpOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), M_NEG);
        assertEquals(0, line.getNetRequirement().compareTo(bd("0")), "负净需求归零");
        assertEquals(0, line.getSuggestedQty().compareTo(bd("0")));
    }

    @Test
    public void testStateMachineAndReleaseEndToEnd() {
        seedMaterial(M_TRANSFER);
        seedMaterial(M_PURCHASE);
        seedWarehouse();
        seedParameter(M_TRANSFER, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_TRANSFER, bd("30"));
        seedParameter(M_PURCHASE, bd("50"), bd("1"), null, SUPPLIER_ID);
        seedBalance(M_PURCHASE, bd("10"));

        Long planId = seedPlan("DRP-E2E");
        runDrpOk(planId);
        // DRAFT→COMPUTED
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED,
                daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId).getStatus());

        approvePlanOk(planId);
        // COMPUTED→APPROVED；行 SUGGESTED→APPROVED
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED,
                daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId).getStatus());
        for (ErpDrpLine l : linesOf(planId)) {
            assertEquals(ErpDrpConstants.DRP_LINE_STATUS_APPROVED, l.getStatus());
        }

        // 释放 TRANSFER 行 → 生成调拨单
        ErpDrpLine transferLine = findLine(linesOf(planId), M_TRANSFER);
        String toCode = releaseLineOk(transferLine.getId());
        assertEquals(ErpDrpConstants.RELEASE_TO_CODE_PREFIX + "TO-" + transferLine.getId(), toCode);
        ErpInvTransferOrder to = findTransferOrder(toCode);
        assertNotNull(to, "调拨单应生成");
        assertEquals(WH_SOURCE, to.getFromWarehouseId());
        assertEquals(WH_TARGET, to.getToWarehouseId());

        ErpDrpLine transferAfter = daoProvider.daoFor(ErpDrpLine.class).getEntityById(transferLine.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_ORDERED, transferAfter.getStatus());
        assertEquals(ErpDrpConstants.ORDER_BILL_TYPE_TRANSFER_ORDER, transferAfter.getOrderBillType());
        assertEquals(toCode, transferAfter.getOrderBillCode());

        // 释放 PURCHASE 行 → 生成采购订单
        ErpDrpLine purchaseLine = findLine(linesOf(planId), M_PURCHASE);
        String poCode = releaseLineOk(purchaseLine.getId());
        assertEquals(ErpDrpConstants.RELEASE_TO_CODE_PREFIX + "PO-" + purchaseLine.getId(), poCode);
        ErpPurOrder po = findPurchaseOrder(poCode);
        assertNotNull(po, "采购订单应生成");
        assertEquals(SUPPLIER_ID, po.getSupplierId());

        ErpDrpLine purchaseAfter = daoProvider.daoFor(ErpDrpLine.class).getEntityById(purchaseLine.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_ORDERED, purchaseAfter.getStatus());
        assertEquals(ErpDrpConstants.ORDER_BILL_TYPE_PURCHASE_ORDER, purchaseAfter.getOrderBillType());

        // 全部行 ORDERED → 计划 EXECUTED（经 releaseApproved 兜底推进）
        releaseApprovedOk(planId);
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED,
                daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId).getStatus(),
                "全部行 ORDERED 后计划 EXECUTED");
    }

    @Test
    public void testRunDrpRejectsNonDraftPlan() {
        seedMaterial(M_TRANSFER);
        seedWarehouse();
        seedParameter(M_TRANSFER, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_TRANSFER, bd("30"));

        Long planId = seedPlan("DRP-STATUS");
        runDrpOk(planId); // → COMPUTED
        // 再跑 → 拒绝（非 DRAFT）
        ApiResponse<?> resp = runDrp(planId);
        assertTrue(resp.getStatus() != 0, "非 DRAFT 计划重跑应拒绝");
        assertEquals(ErpDrpErrors.ERR_DRP_PLAN_ILLEGAL_TRANSITION.getErrorCode(), resp.getCode());
    }

    @Test
    public void testReleaseRejectsAlreadyOrderedLine() {
        seedMaterial(M_TRANSFER);
        seedWarehouse();
        seedParameter(M_TRANSFER, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_TRANSFER, bd("30"));

        Long planId = seedPlan("DRP-REORD");
        runDrpOk(planId);
        approvePlanOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), M_TRANSFER);
        releaseLineOk(line.getId()); // → ORDERED
        // 重复释放 → 拒绝
        ApiResponse<?> again = releaseLine(line.getId());
        assertTrue(again.getStatus() != 0, "已 ORDERED 行再释放应拒绝");
        assertEquals(ErpDrpErrors.ERR_DRP_LINE_ALREADY_ORDERED.getErrorCode(), again.getCode());
    }

    @Test
    public void testResetToDraftClearsSuggestedLines() {
        seedMaterial(M_TRANSFER);
        seedWarehouse();
        seedParameter(M_TRANSFER, bd("100"), bd("1"), WH_SOURCE, null);
        seedBalance(M_TRANSFER, bd("30"));

        Long planId = seedPlan("DRP-RESET");
        runDrpOk(planId);
        assertEquals(1, linesOf(planId).size(), "计算后有 1 条 SUGGESTED 行");

        resetToDraftOk(planId);
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT,
                daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId).getStatus());
        assertEquals(0, linesOf(planId).size(), "重置 DRAFT 后 SUGGESTED 行已清除");
    }

    // ---------- helpers ----------

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

    private void resetToDraftOk(Long planId) {
        ApiResponse<?> resp = rpc(mutation, "ErpDrpPlan__resetToDraft", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "resetToDraft 应成功: " + resp);
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
