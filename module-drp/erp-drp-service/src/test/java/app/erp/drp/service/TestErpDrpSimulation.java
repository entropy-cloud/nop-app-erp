package app.erp.drp.service;

import app.erp.drp.dao.dto.DrpSimulationDiffResult;
import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.dao.entity.ErpDrpScenario;
import app.erp.drp.dao.entity.ErpDrpScenarioParam;
import app.erp.drp.dao.entity.ErpDrpScenarioVersion;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.drp.service.simulation.DrpSimulationVersionComparator;
import app.erp.drp.service.simulation.ErpDrpSimulationParamResolver;
import app.erp.drp.service.simulation.SimulationDrpEngine;
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
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DRP 仿真引擎测试（plan 2026-07-22-1000-2 Phase 3 §DRP 对应物）。
 *
 * <p>覆盖 SAFETY_STOCK/REPLENISHMENT_QTY 场景覆盖、compareVersions 2 维 diff、promoteToFormalPlan、
 * config-gate 默认 false 保护既有单次 DRP 测试零回归。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpSimulation extends JunitAutoTestCase {

    static final Long ORG_ID = 7401L;
    static final Long UOM_ID = 7501L;
    static final Long WAREHOUSE_ID = 7801L;
    static final Long M1 = 9101L;
    static final Long M2 = 9102L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    SimulationDrpEngine simulationDrpEngine;
    @Inject
    ErpDrpSimulationParamResolver paramResolver;
    @Inject
    DrpSimulationVersionComparator simulationComparator;

    @AfterEach
    public void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpDrpConstants.CONFIG_DRP_SIMULATION_ENABLED, "false");
        paramResolver.invalidateCache();
    }

    @Test
    public void testConfigGateDisabledRejectsRunSimulation() {
        Long scenarioId = seedScenario("DRP-SIM-DISABLED", null);
        ApiResponse<?> resp = runSimulationRpc(scenarioId);
        assertNotEquals(0, resp.getStatus());
        assertEquals(ErpDrpErrors.ERR_DRP_SIMULATION_DISABLED.getErrorCode(), resp.getCode());
    }

    @Test
    public void testSafetyStockOverrideAffectsSuggestedQty() {
        // 物料 M1，仓库 W1；参数 safetyStock=10；库存 0；基线净需求=10-0=10
        // 场景覆盖 SAFETY_STOCK=20 → 仿真净需求=20-0=20
        seedMaterial(M1);
        seedWarehouse(WAREHOUSE_ID);
        seedParameter(M1, WAREHOUSE_ID, bd("10"), null);
        seedBalance(M1, WAREHOUSE_ID, bd("0"));

        Long basePlanId = seedPlan("DRP-BASE-SS");
        runDrpOnce(basePlanId);
        ErpDrpLine baseLine = findLine(basePlanId, M1, WAREHOUSE_ID);
        assertEquals(0, baseLine.getSuggestedQty().compareTo(bd("10")), "基线 SS=10 → suggested=10");

        enableSimulation();
        Long scenarioId = seedScenario("DRP-SIM-SS", basePlanId);
        seedParam(scenarioId, M1, WAREHOUSE_ID, ErpDrpConstants.SIMULATION_PARAM_TYPE_SAFETY_STOCK, bd("20"));

        ErpDrpScenarioVersion version = runSimulationViaRpc(scenarioId);
        ErpDrpLine simLine = findLine(version.getComputedDrpPlanId(), M1, WAREHOUSE_ID);
        assertEquals(0, simLine.getSafetyStock().compareTo(bd("20")), "仿真 SS=20");
        assertEquals(0, simLine.getSuggestedQty().compareTo(bd("20")),
                "仿真 SS=20 库存=0 → suggested=20");
        assertEquals(ErpDrpConstants.SIMULATION_STATUS_COMPLETED, version.getStatus());
    }

    @Test
    public void testReplenishmentQtyOverrideAffectsRounding() {
        // 物料 M2，仓库 W1；safetyStock=15；orderMultiple=1（不取整）→ suggested=15
        // 场景覆盖 REPLENISHMENT_QTY=10 → suggested=ceil(15/10)*10=20
        seedMaterial(M2);
        seedWarehouse(WAREHOUSE_ID);
        seedParameter(M2, WAREHOUSE_ID, bd("15"), bd("1"));
        seedBalance(M2, WAREHOUSE_ID, bd("0"));

        Long basePlanId = seedPlan("DRP-BASE-RQ");
        runDrpOnce(basePlanId);
        ErpDrpLine baseLine = findLine(basePlanId, M2, WAREHOUSE_ID);
        assertEquals(0, baseLine.getSuggestedQty().compareTo(bd("15")), "基线 multiple=1 → suggested=15");

        enableSimulation();
        Long scenarioId = seedScenario("DRP-SIM-RQ", basePlanId);
        seedParam(scenarioId, M2, WAREHOUSE_ID, ErpDrpConstants.SIMULATION_PARAM_TYPE_REPLENISHMENT_QTY, bd("10"));

        ErpDrpScenarioVersion version = runSimulationViaRpc(scenarioId);
        ErpDrpLine simLine = findLine(version.getComputedDrpPlanId(), M2, WAREHOUSE_ID);
        assertEquals(0, simLine.getSuggestedQty().compareTo(bd("20")),
                "仿真 REPLENISHMENT_QTY=10 → ceil(15/10)*10=20");
    }

    @Test
    public void testCompareVersionsProducesStructuredDiff() {
        seedMaterial(M1);
        seedWarehouse(WAREHOUSE_ID);
        seedParameter(M1, WAREHOUSE_ID, bd("10"), null);
        seedBalance(M1, WAREHOUSE_ID, bd("0"));

        Long basePlanId = seedPlan("DRP-BASE-CMP");
        runDrpOnce(basePlanId);

        enableSimulation();
        Long scenarioId = seedScenario("DRP-SIM-CMP", basePlanId);

        // 版本 A：SS=10 → suggested=10
        seedParam(scenarioId, M1, WAREHOUSE_ID, ErpDrpConstants.SIMULATION_PARAM_TYPE_SAFETY_STOCK, bd("10"));
        ErpDrpScenarioVersion vA = runSimulationViaRpc(scenarioId);

        resetScenarioToDraft(scenarioId);
        paramResolver.invalidateCache();
        // 版本 B：SS=30 → suggested=30
        updateSafetyStockParam(scenarioId, M1, WAREHOUSE_ID, bd("30"));
        ErpDrpScenarioVersion vB = runSimulationViaRpc(scenarioId);

        // 对比 B - A：补货量差 = 30 - 10 = +20
        DrpSimulationDiffResult diff = simulationComparator.compareDrpVersions(vA.getId(), vB.getId());
        assertNotNull(diff);
        assertEquals(0, diff.getTotalReplenishmentQtyDelta().compareTo(bd("20")),
                "B(SS=30) - A(SS=10) suggested diff = +20");
        assertEquals(0, diff.getTotalSafetyStockDelta().compareTo(bd("20")),
                "B(SS=30) - A(SS=10) safetyStock diff = +20");
        assertTrue(diff.getLineDiffs().size() >= 1, "至少 1 行 diff");
    }

    @Test
    public void testPromoteToFormalPlanCreatesDraft() {
        seedMaterial(M1);
        seedWarehouse(WAREHOUSE_ID);
        seedParameter(M1, WAREHOUSE_ID, bd("10"), null);
        seedBalance(M1, WAREHOUSE_ID, bd("0"));

        Long basePlanId = seedPlan("DRP-BASE-PROM");
        runDrpOnce(basePlanId);

        enableSimulation();
        Long scenarioId = seedScenario("DRP-SIM-PROM", basePlanId);
        seedParam(scenarioId, M1, WAREHOUSE_ID, ErpDrpConstants.SIMULATION_PARAM_TYPE_SAFETY_STOCK, bd("20"));

        ErpDrpScenarioVersion version = runSimulationViaRpc(scenarioId);

        ErpDrpPlan promoted = promoteToFormalPlanViaRpc(version.getId());
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT, promoted.getStatus());
        assertTrue(promoted.getCode().contains("-PROMOTED-1"));

        // 复制了行
        List<ErpDrpLine> promotedLines = linesOf(promoted.getId());
        assertTrue(promotedLines.size() > 0, "promoted plan 含 DRP 行");

        // 版本 ARCHIVED
        ErpDrpScenarioVersion reloaded = daoProvider.daoFor(ErpDrpScenarioVersion.class)
                .getEntityById(version.getId());
        assertEquals(ErpDrpConstants.SIMULATION_STATUS_ARCHIVED, reloaded.getStatus());

        // 重复转正 → 拒绝
        ApiResponse<?> resp = promoteToFormalPlanRpc(version.getId());
        assertNotEquals(0, resp.getStatus());
        assertEquals(ErpDrpErrors.ERR_DRP_SIMULATION_VERSION_ALREADY_PROMOTED.getErrorCode(),
                resp.getCode());
    }

    // ---------- helpers ----------

    private void enableSimulation() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpDrpConstants.CONFIG_DRP_SIMULATION_ENABLED, "true");
    }

    private void runDrpOnce(Long planId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("planId", planId);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation,
                "ErpDrpPlan__runDrp", ApiRequest.build(args));
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), "基线 runDrp 应成功: " + resp);
    }

    private ErpDrpScenarioVersion runSimulationViaRpc(Long scenarioId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("scenarioId", scenarioId);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation,
                "ErpDrpScenario__runSimulation", ApiRequest.build(args));
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), "runSimulation 应成功: " + resp);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        Long versionId = Long.valueOf(String.valueOf(data.get("id")));
        return daoProvider.daoFor(ErpDrpScenarioVersion.class).getEntityById(versionId);
    }

    private ApiResponse<?> runSimulationRpc(Long scenarioId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("scenarioId", scenarioId);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation,
                "ErpDrpScenario__runSimulation", ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpDrpPlan promoteToFormalPlanViaRpc(Long versionId) {
        ApiResponse<?> resp = promoteToFormalPlanRpc(versionId);
        assertEquals(0, resp.getStatus(), "promoteToFormalPlan 应成功: " + resp);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        Long id = Long.valueOf(String.valueOf(data.get("id")));
        return daoProvider.daoFor(ErpDrpPlan.class).getEntityById(id);
    }

    private ApiResponse<?> promoteToFormalPlanRpc(Long versionId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("scenarioVersionId", versionId);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation,
                "ErpDrpScenario__promoteToFormalPlan", ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpDrpLine findLine(Long planId, Long materialId, Long warehouseId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        List<ErpDrpLine> list = daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpDrpLine> linesOf(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        return daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
    }

    private Long seedPlan(String code) {
        Long id = 8700L + (long) Math.abs(code.hashCode() % 200);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpPlan> dao = daoProvider.daoFor(ErpDrpPlan.class);
            ErpDrpPlan p = new ErpDrpPlan();
            p.orm_propValueByName("id", id);
            p.setCode(code);
            p.setPlanName("Plan " + code);
            p.setOrgId(ORG_ID);
            p.setBusinessDate(LocalDate.of(2026, 8, 1));
            p.setPeriodFrom(LocalDate.of(2026, 8, 1));
            p.setPeriodTo(LocalDate.of(2026, 8, 31));
            p.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
            dao.saveEntity(p);
        });
        return id;
    }

    private Long seedScenario(String code, Long basePlanId) {
        Long id = 9500L + (long) Math.abs(code.hashCode() % 100);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpScenario> dao = daoProvider.daoFor(ErpDrpScenario.class);
            ErpDrpScenario s = new ErpDrpScenario();
            s.orm_propValueByName("id", id);
            s.setCode(code);
            s.setOrgId(ORG_ID);
            s.setBaseDrpPlanId(basePlanId);
            s.setStatus(ErpDrpConstants.SIMULATION_STATUS_DRAFT);
            dao.saveEntity(s);
        });
        return id;
    }

    private void seedParam(Long scenarioId, Long materialId, Long warehouseId, String paramType, BigDecimal value) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpScenarioParam> dao = daoProvider.daoFor(ErpDrpScenarioParam.class);
            ErpDrpScenarioParam p = new ErpDrpScenarioParam();
            p.orm_propValueByName("id",
                    9900L + (long) Math.abs((scenarioId + "" + materialId + warehouseId + paramType).hashCode() % 80));
            p.setScenarioId(scenarioId);
            p.setMaterialId(materialId);
            p.setWarehouseId(warehouseId);
            p.setParamType(paramType);
            p.setParamValue(value);
            dao.saveEntity(p);
        });
        paramResolver.invalidateCache();
    }

    private void updateSafetyStockParam(Long scenarioId, Long materialId, Long warehouseId, BigDecimal newValue) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpScenarioParam> dao = daoProvider.daoFor(ErpDrpScenarioParam.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("scenarioId", scenarioId));
            q.addFilter(eq("materialId", materialId));
            q.addFilter(eq("warehouseId", warehouseId));
            q.addFilter(eq("paramType", ErpDrpConstants.SIMULATION_PARAM_TYPE_SAFETY_STOCK));
            for (ErpDrpScenarioParam p : dao.findAllByQuery(q)) {
                p.setParamValue(newValue);
                dao.saveOrUpdateEntity(p);
            }
        });
        paramResolver.invalidateCache();
    }

    private void resetScenarioToDraft(Long scenarioId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpScenario> dao = daoProvider.daoFor(ErpDrpScenario.class);
            ErpDrpScenario s = dao.getEntityById(scenarioId);
            if (s != null) {
                s.setStatus(ErpDrpConstants.SIMULATION_STATUS_DRAFT);
                dao.saveOrUpdateEntity(s);
            }
        });
    }

    private void seedParameter(Long materialId, Long warehouseId, BigDecimal safetyStock, BigDecimal orderMultiple) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpParameter> dao = daoProvider.daoFor(ErpDrpParameter.class);
            ErpDrpParameter p = new ErpDrpParameter();
            p.orm_propValueByName("id", 9200L + materialId);
            p.setWarehouseId(warehouseId);
            p.setMaterialId(materialId);
            p.setOrgId(ORG_ID);
            if (safetyStock != null) p.setSafetyStock(safetyStock);
            if (orderMultiple != null) p.setOrderMultiple(orderMultiple);
            p.setReplenishmentMethod(ErpDrpConstants.REPLENISHMENT_METHOD_LOT_FOR_LOT);
            dao.saveEntity(p);
        });
    }

    private void seedBalance(Long materialId, Long warehouseId, BigDecimal available) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = new ErpInvStockBalance();
            b.orm_propValueByName("id", 9500L + materialId);
            b.setOrgId(ORG_ID);
            b.setMaterialId(materialId);
            b.setWarehouseId(warehouseId);
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

    private void seedWarehouse(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
            ErpMdWarehouse w = new ErpMdWarehouse();
            w.orm_propValueByName("id", id);
            w.setCode("WH-" + id);
            w.setName("Warehouse " + id);
            w.orm_propValueByName("status", "ACTIVE");
            dao.saveEntity(w);
        });
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
