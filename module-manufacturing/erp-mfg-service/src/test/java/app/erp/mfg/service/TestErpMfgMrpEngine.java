package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMrpDemand;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.md.dao.entity.ErpMdMaterial;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 测试：MRP 运行（需求整合 + BOM 多级展开 + 净需求 + 计划订单类型判定 + lot sizing + 提前期偏移 + pegging）。
 *
 * <p>覆盖 {@code docs/design/manufacturing/mrp.md §MRP 流程}：销售订单/安全库存/手工需求整合、制造件多级展开净需求、
 * 采购件不展开直接净需求、WORK_ORDER_REQUEST/PURCHASE_REQUEST 类型判定、lot-for-lot 与固定批量取整、
 * 提前期偏移、负净需求归零、parentLineId pegging 链。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgMrpEngine extends JunitAutoTestCase {

    static final Long ORG_ID = 7401L;
    static final Long UOM_ID = 7501L;
    static final Long CUSTOMER_ID = 7601L;
    static final Long CURRENCY_ID = 7701L;
    static final Long WAREHOUSE_ID = 7801L;

    static final Long P = 7101L;   // 产成品（制造件）
    static final Long M1 = 7102L;  // 采购件（P 的子件）
    static final Long M2 = 7103L;  // 采购件（安全库存补货）
    static final Long M3 = 7104L;  // 采购件（lot sizing / 提前期）
    static final Long M4 = 7105L;  // 采购件（负净需求归零）
    static final Long A = 7106L;   // 制造件（多级 pegging 链顶层）
    static final Long B = 7107L;   // 制造件（多级 pegging 中层）
    static final Long C = 7108L;   // 采购件（多级 pegging 底层）

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testDemandAggregationExpansionOrderTypePegging() {
        seedMaterial(P, null, null);
        seedMaterial(M1, 5, null);   // leadTimeDays=5
        seedMaterial(M2, null, bd("8"));  // safetyStock=8
        seedBom(8101L, P, M1, bd("2"));   // P 需要 2×M1，无工序 → mfg 提前期 0
        seedBalance(M2, bd("3"));         // M2 可用 3 < 安全库存 8 → 补货 5
        seedSalesOrder("SO-MRP-1", P, bd("10"), LocalDate.of(2026, 7, 15));  // P 未交 10

        Long planId = seedPlan("MRP-1");
        seedManualDemand(planId, M1, bd("3"), LocalDate.of(2026, 7, 15));  // 手工 M1 需求 3

        runMrpOk(planId);

        // 计划状态 COMPLETED
        ErpMfgMrpPlan plan = daoProvider.daoFor(ErpMfgMrpPlan.class).getEntityById(planId);
        assertEquals(ErpMfgConstants.MRP_STATUS_COMPLETED, plan.getStatus());

        // 需求整合：SALES_ORDER(P=10) + SAFETY_STOCK(M2=5) + MANUAL(M1=3)
        assertEquals(0, sumDemand(planId, P, ErpMfgConstants.MRP_DEMAND_SOURCE_SALES_ORDER).compareTo(bd("10")));
        assertEquals(0, sumDemand(planId, M2, ErpMfgConstants.MRP_DEMAND_SOURCE_SAFETY_STOCK).compareTo(bd("5")));
        assertEquals(0, sumDemand(planId, M1, ErpMfgConstants.MRP_DEMAND_SOURCE_MANUAL).compareTo(bd("3")));

        List<ErpMfgMrpPlanLine> lines = linesOf(planId);

        // P：制造件 → WORK_ORDER_REQUEST，gross=10 net=10 planned=10，无父行
        ErpMfgMrpPlanLine pLine = findLine(lines, P, null);
        assertNotNull(pLine, "P 计划行应存在");
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_WORK_ORDER_REQUEST, pLine.getOrderType());
        assertEquals(0, pLine.getGrossRequirement().compareTo(bd("10")));
        assertEquals(0, pLine.getNetRequirement().compareTo(bd("10")));
        assertEquals(0, pLine.getPlannedQuantity().compareTo(bd("10")));
        assertNull(pLine.getParentLineId());
        assertEquals(LocalDate.of(2026, 7, 15), pLine.getPlannedDate(), "P 无工序→mfg 提前期 0→plannedDate=需求日");

        // M1（P 展开）：采购件 → PURCHASE_REQUEST，gross=2×10=20，parentLineId=P 行
        ErpMfgMrpPlanLine m1Exploded = findLine(lines, M1, pLine.getId());
        assertNotNull(m1Exploded, "M1(P 展开) 计划行应存在");
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_PURCHASE_REQUEST, m1Exploded.getOrderType());
        assertEquals(0, m1Exploded.getGrossRequirement().compareTo(bd("20")));
        assertEquals(pLine.getId(), m1Exploded.getParentLineId(), "pegging: M1 父行=P");
        assertEquals(LocalDate.of(2026, 7, 10), m1Exploded.getPlannedDate(),
                "M1 提前期 5 天 → plannedDate = 2026-07-15 - 5 = 2026-07-10");

        // M2：安全库存补货，采购件 → PURCHASE_REQUEST，gross=5，无父行
        ErpMfgMrpPlanLine m2Line = findLine(lines, M2, null);
        assertNotNull(m2Line, "M2 计划行应存在");
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_PURCHASE_REQUEST, m2Line.getOrderType());
        assertEquals(0, m2Line.getGrossRequirement().compareTo(bd("5")));

        // M1（手工需求）：gross=3，无父行（独立需求）
        ErpMfgMrpPlanLine m1Manual = findLine(lines, M1, null);
        assertNotNull(m1Manual, "M1(手工) 计划行应存在");
        assertEquals(0, m1Manual.getGrossRequirement().compareTo(bd("3")));
        assertNull(m1Manual.getParentLineId());
    }

    @Test
    public void testLotSizingAndLeadTimeOffset() {
        // lot-for-lot（默认）：净需求即建议量
        seedMaterial(M3, 7, null);
        Long planId = seedPlan("MRP-LOT");
        seedManualDemand(planId, M3, bd("12"), LocalDate.of(2026, 7, 20));
        runMrpOk(planId);

        ErpMfgMrpPlanLine m3Line = findLine(linesOf(planId), M3, null);
        assertEquals(0, m3Line.getPlannedQuantity().compareTo(bd("12")), "lot-for-lot: planned=net=12");
        assertEquals(LocalDate.of(2026, 7, 13), m3Line.getPlannedDate(), "采购提前期 7 → 2026-07-20 - 7");

        // 固定批量取整：default-lot-size=10 → planned=ceil(12/10)*10=20
        setConfig(ErpMfgConstants.CONFIG_MRP_DEFAULT_LOT_SIZE, "10");
        try {
            Long plan2 = seedPlan("MRP-LOT2");
            seedManualDemand(plan2, M3, bd("12"), LocalDate.of(2026, 7, 20));
            runMrpOk(plan2);
            ErpMfgMrpPlanLine line2 = findLine(linesOf(plan2), M3, null);
            assertEquals(0, line2.getPlannedQuantity().compareTo(bd("20")), "固定批量 10: planned=20");
            assertEquals(0, line2.getNetRequirement().compareTo(bd("12")), "net 仍为 12");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_MRP_DEFAULT_LOT_SIZE, "0");
        }
    }

    @Test
    public void testNegativeNetClampedToZero() {
        seedMaterial(M4, null, null);
        seedBalance(M4, bd("10"));  // 库存 10
        Long planId = seedPlan("MRP-ZERO");
        seedManualDemand(planId, M4, bd("4"), LocalDate.of(2026, 7, 20));  // 需求 4 < 库存 10
        runMrpOk(planId);

        ErpMfgMrpPlanLine line = findLine(linesOf(planId), M4, null);
        assertEquals(0, line.getGrossRequirement().compareTo(bd("4")));
        assertEquals(0, line.getOnHand().compareTo(bd("10")));
        assertEquals(0, line.getNetRequirement().compareTo(bd("0")), "负净需求归零");
        assertEquals(0, line.getPlannedQuantity().compareTo(bd("0")));
    }

    @Test
    public void testMultiLevelPeggingChain() {
        seedMaterial(A, null, null);
        seedMaterial(B, null, null);
        seedMaterial(C, null, null);
        seedBom(8201L, A, B, bd("1"));   // A→B（1:1）
        seedBom(8202L, B, C, bd("1"));   // B→C（1:1）
        Long planId = seedPlan("MRP-PEG");
        seedManualDemand(planId, A, bd("1"), LocalDate.of(2026, 7, 15));
        runMrpOk(planId);

        List<ErpMfgMrpPlanLine> lines = linesOf(planId);
        ErpMfgMrpPlanLine aLine = findLine(lines, A, null);
        ErpMfgMrpPlanLine bLine = findLine(lines, B, aLine.getId());
        ErpMfgMrpPlanLine cLine = findLine(lines, C, bLine.getId());

        assertNotNull(aLine);
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_WORK_ORDER_REQUEST, aLine.getOrderType());
        assertNotNull(bLine, "B 应作为 A 的子件展开");
        assertEquals(aLine.getId(), bLine.getParentLineId());
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_WORK_ORDER_REQUEST, bLine.getOrderType());
        assertNotNull(cLine, "C 应作为 B 的子件展开");
        assertEquals(bLine.getId(), cLine.getParentLineId());
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_PURCHASE_REQUEST, cLine.getOrderType(), "C 无 BOM → 采购件");
    }

    @Test
    public void testRunMrpRejectsNonDraftPlan() {
        seedMaterial(M3, null, null);
        Long planId = seedPlan("MRP-STATUS");
        // 先跑一次 → COMPLETED
        seedManualDemand(planId, M3, bd("1"), LocalDate.of(2026, 7, 20));
        runMrpOk(planId);
        // 再跑 → 拒绝（非 DRAFT）
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("planId", planId);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpMfgMrpPlan__runMrp", ApiRequest.build(args));
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertTrue(resp.getStatus() != 0, "非 DRAFT 计划重跑应拒绝");
        assertEquals(ErpMfgErrors.ERR_MRP_INVALID_PLAN_STATUS.getErrorCode(), resp.getCode());
    }

    // ---------- helpers ----------

    private void runMrpOk(Long planId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("planId", planId);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpMfgMrpPlan__runMrp", ApiRequest.build(args));
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), "runMrp 应成功: " + resp);
    }

    private BigDecimal sumDemand(Long planId, Long materialId, String demandSource) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("demandSource", demandSource));
        List<ErpMfgMrpDemand> list = daoProvider.daoFor(ErpMfgMrpDemand.class).findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpMfgMrpDemand d : list) {
            sum = sum.add(d.getQuantity() != null ? d.getQuantity() : BigDecimal.ZERO);
        }
        return sum;
    }

    private List<ErpMfgMrpPlanLine> linesOf(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpMfgMrpPlanLine.class).findAllByQuery(q);
    }

    private ErpMfgMrpPlanLine findLine(List<ErpMfgMrpPlanLine> lines, Long materialId, Long parentLineId) {
        ErpMfgMrpPlanLine fallback = null;
        for (ErpMfgMrpPlanLine l : lines) {
            if (materialId.equals(l.getMaterialId())) {
                if (parentLineId == null) {
                    if (l.getParentLineId() == null) {
                        return l;
                    }
                    if (fallback == null) {
                        fallback = l;
                    }
                } else if (parentLineId.equals(l.getParentLineId())) {
                    return l;
                }
            }
        }
        return fallback;
    }

    private Long seedPlan(String code) {
        Long id = 7001L + (long) Math.abs(code.hashCode() % 600);
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
            IEntityDao<ErpMfgMrpDemand> dao = daoProvider.daoFor(ErpMfgMrpDemand.class);
            ErpMfgMrpDemand d = new ErpMfgMrpDemand();
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
        Long orderId = 7300L + (long) Math.abs(code.hashCode() % 600);
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

    private void seedBalance(Long materialId, BigDecimal available) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = new ErpInvStockBalance();
            b.orm_propValueByName("id", 8000L + materialId);
            b.setOrgId(ORG_ID);
            b.setMaterialId(materialId);
            b.setWarehouseId(WAREHOUSE_ID);
            b.setTotalQuantity(available);
            b.setAvailableQuantity(available);
            dao.saveEntity(b);
        });
    }

    private void seedMaterial(Long id, Integer leadTimeDays, BigDecimal safetyStock) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            if (leadTimeDays != null) {
                m.setLeadTimeDays(leadTimeDays);
            }
            if (safetyStock != null) {
                m.setSafetyStock(safetyStock);
            }
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

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
