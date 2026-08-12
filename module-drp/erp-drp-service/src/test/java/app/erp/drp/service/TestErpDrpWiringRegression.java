package app.erp.drp.service;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.inv.dao.entity.ErpInvStockBalance;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Phase 2 针对性接线回归（plan 2026-08-12-1841-1 Phase 2 Proof；契约 §10 层 3）。
 *
 * <p>覆盖既有层 3（{@link TestErpDrpEngine} 等）未直接覆盖的接线路径——经 BizModel/IGraphQLEngine 入口证明这些
 * 接线点经 Bean 后行为/错误码不变。覆盖的「已知覆盖缺口」（plan Current Baseline 登记）：
 * <ul>
 *   <li>{@code cancelLine} happy（SUGGESTED→CANCELLED + APPROVED→CANCELLED 多源）+ 终态拒绝（ORDERED/CANCELLED→抛
 *       {@code ERR_DRP_LINE_ILLEGAL_TRANSITION}）；</li>
 *   <li>{@code rejectLine} 同语义各一例（happy + 终态拒绝）；</li>
 *   <li>{@code approveLine}（SUGGESTED→APPROVED + 非 SUGGESTED 拒绝抛 {@code ERR_DRP_LINE_ILLEGAL_TRANSITION}）；</li>
 *   <li>{@code resetToDraft} 从 APPROVED（APPROVED→DRAFT）。</li>
 * </ul>
 *
 * <p>证明：接线后错误码（{@code ERR_DRP_LINE_ILLEGAL_TRANSITION} 仅 drpLineId/currentStatus）+ 行为不变；
 * cancel/reject 多源 + 终态拒绝经 Bean 后保持既有外部语义。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpWiringRegression extends JunitAutoTestCase {

    static final Long ORG_ID = 7401L;
    static final Long UOM_ID = 7501L;
    static final Long CURRENCY_ID = 7701L;
    static final Long SUPPLIER_ID = 7801L;
    static final Long WH_TARGET = 7101L;
    static final Long WH_SOURCE = 7102L;
    static final Long M_PURCHASE = 7201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- approveLine ----------

    @Test
    public void testApproveLineSuggestedToApproved() {
        Long planId = seedComputedPlanWithOneLine();
        ErpDrpLine line = singleLineOf(planId);
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED, line.getStatus());

        ApiResponse<?> resp = approveLine(line.getId());
        assertEquals(0, resp.getStatus(), "approveLine(SUGGESTED) 应成功: " + resp);

        ErpDrpLine after = daoProvider.daoFor(ErpDrpLine.class).getEntityById(line.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_APPROVED, after.getStatus(), "SUGGESTED→APPROVED");
    }

    @Test
    public void testApproveLineRejectsNonSuggested() {
        Long planId = seedComputedPlanWithOneLine();
        ErpDrpLine line = singleLineOf(planId);
        approveLineOk(line.getId()); // SUGGESTED→APPROVED

        // 再次 approveLine（APPROVED→非 SUGGESTED）→ 拒绝
        ApiResponse<?> again = approveLine(line.getId());
        assertNotEquals(0, again.getStatus(), "approveLine(APPROVED) 应拒绝");
        assertEquals(ErpDrpErrors.ERR_DRP_LINE_ILLEGAL_TRANSITION.getErrorCode(), again.getCode(),
                "非 SUGGESTED 行 approveLine 抛 ERR_DRP_LINE_ILLEGAL_TRANSITION（错误码不变）");

        // 状态不变
        ErpDrpLine after = daoProvider.daoFor(ErpDrpLine.class).getEntityById(line.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_APPROVED, after.getStatus());
    }

    // ---------- cancelLine ----------

    @Test
    public void testCancelLineFromSuggested() {
        Long planId = seedComputedPlanWithOneLine();
        ErpDrpLine line = singleLineOf(planId);

        cancelLineOk(line.getId()); // SUGGESTED→CANCELLED

        ErpDrpLine after = daoProvider.daoFor(ErpDrpLine.class).getEntityById(line.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_CANCELLED, after.getStatus(), "cancelLine(SUGGESTED)→CANCELLED");
    }

    @Test
    public void testCancelLineFromApproved() {
        // 多源 cancel：APPROVED→CANCELLED（plan §Current Baseline cancel 多源 SUGGESTED|APPROVED）
        Long planId = seedComputedPlanWithOneLine();
        ErpDrpLine line = singleLineOf(planId);
        approveLineOk(line.getId()); // SUGGESTED→APPROVED

        cancelLineOk(line.getId()); // APPROVED→CANCELLED

        ErpDrpLine after = daoProvider.daoFor(ErpDrpLine.class).getEntityById(line.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_CANCELLED, after.getStatus(), "cancelLine(APPROVED)→CANCELLED（多源合法）");
    }

    @Test
    public void testCancelLineRejectsOrderedTerminal() {
        Long planId = seedComputedPlanWithOneLine();
        // 走 canonical 路径：approvePlan 级联行 SUGGESTED→APPROVED + plan COMPUTED→APPROVED（设计文档 §场景 C 假定
        // 「计划头已批准」是 release 的前置），随后 releaseLine 使行进入 ORDERED 终态。
        approvePlanOk(planId);
        ErpDrpLine line = singleLineOf(planId);
        releaseLineOk(line.getId()); // →ORDERED（终态）

        ApiResponse<?> resp = cancelLine(line.getId());
        assertNotEquals(0, resp.getStatus(), "cancelLine(ORDERED) 应拒绝");
        assertEquals(ErpDrpErrors.ERR_DRP_LINE_ILLEGAL_TRANSITION.getErrorCode(), resp.getCode(),
                "终态 ORDERED cancel 抛 ERR_DRP_LINE_ILLEGAL_TRANSITION（错误码不变）");

        ErpDrpLine after = daoProvider.daoFor(ErpDrpLine.class).getEntityById(line.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_ORDERED, after.getStatus(), "状态不变（ORDERED）");
    }

    @Test
    public void testCancelLineRejectsCancelledTerminal() {
        Long planId = seedComputedPlanWithOneLine();
        ErpDrpLine line = singleLineOf(planId);
        cancelLineOk(line.getId()); // →CANCELLED（终态）

        ApiResponse<?> resp = cancelLine(line.getId());
        assertNotEquals(0, resp.getStatus(), "cancelLine(CANCELLED) 应拒绝");
        assertEquals(ErpDrpErrors.ERR_DRP_LINE_ILLEGAL_TRANSITION.getErrorCode(), resp.getCode(),
                "终态 CANCELLED cancel 抛 ERR_DRP_LINE_ILLEGAL_TRANSITION（错误码不变）");
    }

    // ---------- rejectLine（同 cancelLine 语义，各一例） ----------

    @Test
    public void testRejectLineFromSuggested() {
        Long planId = seedComputedPlanWithOneLine();
        ErpDrpLine line = singleLineOf(planId);

        ApiResponse<?> resp = rejectLine(line.getId());
        assertEquals(0, resp.getStatus(), "rejectLine(SUGGESTED) 应成功: " + resp);

        ErpDrpLine after = daoProvider.daoFor(ErpDrpLine.class).getEntityById(line.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_CANCELLED, after.getStatus(),
                "rejectLine 与 cancelLine 同语义（SUGGESTED→CANCELLED）");
    }

    @Test
    public void testRejectLineRejectsOrderedTerminal() {
        Long planId = seedComputedPlanWithOneLine();
        approvePlanOk(planId); // canonical 路径（见 testCancelLineRejectsOrderedTerminal 说明）
        ErpDrpLine line = singleLineOf(planId);
        releaseLineOk(line.getId()); // →ORDERED（终态）

        ApiResponse<?> resp = rejectLine(line.getId());
        assertNotEquals(0, resp.getStatus(), "rejectLine(ORDERED) 应拒绝");
        assertEquals(ErpDrpErrors.ERR_DRP_LINE_ILLEGAL_TRANSITION.getErrorCode(), resp.getCode(),
                "终态 ORDERED reject 抛 ERR_DRP_LINE_ILLEGAL_TRANSITION（与 cancelLine 同语义）");
    }

    // ---------- resetToDraft from APPROVED ----------

    @Test
    public void testResetToDraftFromApproved() {
        // D-DRP-1 / D-DRP-2：resetToDraft 多源含 APPROVED（owner doc §3 + 代码 DrpEngine.resetToDraft 接受 APPROVED）。
        Long planId = seedComputedPlanWithOneLine();
        approvePlanOk(planId); // COMPUTED→APPROVED；行 SUGGESTED→APPROVED
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED,
                daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId).getStatus());
        // 行经 approvePlan 级联进入 APPROVED（非 SUGGESTED），clearSuggestedLines 不清除 APPROVED 行——这是既有行为。
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_APPROVED, singleLineOf(planId).getStatus());

        resetToDraftOk(planId); // APPROVED→DRAFT

        ErpDrpPlan after = daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId);
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT, after.getStatus(),
                "resetToDraft(APPROVED)→DRAFT（多源合法，D-DRP-2）");
        // clearSuggestedLines 仅清 SUGGESTED 行（副作用保留原位）；APPROVED 行不在清除范围（既有行为不变）
        List<ErpDrpLine> linesAfter = linesOf(planId);
        assertEquals(1, linesAfter.size(), "APPROVED 行不被 clearSuggestedLines 清除（既有行为）");
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_APPROVED, linesAfter.get(0).getStatus());
    }

    // ---------- helpers ----------

    private Long seedComputedPlanWithOneLine() {
        seedMaterial();
        seedWarehouse();
        // safetyStock=100, stock=20 → net=80; PURCHASE 路径（preferredSupplier=SUPPLIER）
        seedParameter(bd("100"), bd("1"), null, SUPPLIER_ID);
        seedBalance(bd("20"));

        Long planId = seedPlan();
        runDrpOk(planId); // DRAFT→COMPUTED；写入 1 条 SUGGESTED 行
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED,
                daoProvider.daoFor(ErpDrpPlan.class).getEntityById(planId).getStatus());
        return planId;
    }

    private ErpDrpLine singleLineOf(Long planId) {
        List<ErpDrpLine> lines = linesOf(planId);
        assertEquals(1, lines.size(), "应有且仅有 1 条 SUGGESTED 行");
        return lines.get(0);
    }

    private void runDrpOk(Long planId) {
        ApiResponse<?> resp = rpc(mutation, "ErpDrpPlan__runDrp", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "runDrp 应成功: " + resp);
    }

    private void approvePlanOk(Long planId) {
        ApiResponse<?> resp = rpc(mutation, "ErpDrpPlan__approvePlan", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "approvePlan 应成功: " + resp);
    }

    private void resetToDraftOk(Long planId) {
        ApiResponse<?> resp = rpc(mutation, "ErpDrpPlan__resetToDraft", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "resetToDraft 应成功: " + resp);
    }

    private void approveLineOk(Long lineId) {
        ApiResponse<?> resp = approveLine(lineId);
        assertEquals(0, resp.getStatus(), "approveLine 应成功: " + resp);
    }

    private ApiResponse<?> approveLine(Long lineId) {
        return rpc(mutation, "ErpDrpLine__approveLine", Map.of("lineId", lineId));
    }

    private void cancelLineOk(Long lineId) {
        ApiResponse<?> resp = cancelLine(lineId);
        assertEquals(0, resp.getStatus(), "cancelLine 应成功: " + resp);
    }

    private ApiResponse<?> cancelLine(Long lineId) {
        return rpc(mutation, "ErpDrpLine__cancelLine", Map.of("lineId", lineId));
    }

    private ApiResponse<?> rejectLine(Long lineId) {
        return rpc(mutation, "ErpDrpLine__rejectLine", Map.of("lineId", lineId));
    }

    private void releaseLineOk(Long lineId) {
        ApiResponse<?> resp = rpc(mutation, "ErpDrpLine__releaseLine", Map.of("lineId", lineId));
        assertEquals(0, resp.getStatus(), "releaseLine 应成功: " + resp);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private List<ErpDrpLine> linesOf(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        return daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
    }

    private Long seedPlan() {
        Long id = 7001L;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpPlan> dao = daoProvider.daoFor(ErpDrpPlan.class);
            ErpDrpPlan plan = new ErpDrpPlan();
            plan.setBusinessDate(LocalDate.of(2026, 8, 1));
            plan.orm_propValueByName("id", id);
            plan.setCode("DRP-WIRE-1");
            plan.setPlanName("DRP Wiring Regression");
            plan.setPeriodFrom(LocalDate.of(2026, 8, 1));
            plan.setPeriodTo(LocalDate.of(2026, 8, 31));
            plan.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
            plan.setOrgId(ORG_ID);
            dao.saveEntity(plan);
        });
        return id;
    }

    private void seedParameter(BigDecimal safetyStock, BigDecimal orderMultiple,
                               Long sourceWarehouseId, Long supplierId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpParameter> dao = daoProvider.daoFor(ErpDrpParameter.class);
            ErpDrpParameter p = new ErpDrpParameter();
            p.orm_propValueByName("id", 7900L + M_PURCHASE);
            p.setMaterialId(M_PURCHASE);
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

    private void seedBalance(BigDecimal available) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = new ErpInvStockBalance();
            b.orm_propValueByName("id", 9000L + M_PURCHASE);
            b.setOrgId(ORG_ID);
            b.setMaterialId(M_PURCHASE);
            b.setWarehouseId(WH_TARGET);
            b.setTotalQuantity(available);
            b.setAvailableQuantity(available);
            dao.saveEntity(b);
        });
    }

    private void seedMaterial() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", M_PURCHASE);
            m.setCode("MAT-WIRE");
            m.setName("Material Wiring");
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
