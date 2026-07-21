package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.dto.SimulationDiffResult;
import app.erp.mfg.dao.entity.ErpMfgMrpDemand;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.dao.entity.ErpMfgMrpScenario;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioParam;
import app.erp.mfg.dao.entity.ErpMfgMrpScenarioVersion;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.mfg.service.simulation.ErpMfgSimulationParamResolver;
import app.erp.mfg.service.simulation.SimulationMrpEngine;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MRP 仿真引擎测试（plan 2026-07-22-1000-2 Phase 2）。
 *
 * <p>覆盖 {@code docs/design/manufacturing/simulation-engine.md §Decision E2}：
 * 参数变体覆盖（lead time / lot size / safety stock）经 fork 引擎生效；
 * 回退顺序（场景覆盖 → 全局配置）；config-gated 默认 false 保护既有单次 MRP 测试零回归；
 * promoteToFormalPlan 生成 DRAFT plan + 防重复转正。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgMrpSimulation extends JunitAutoTestCase {

    static final Long ORG_ID = 7401L;
    static final Long UOM_ID = 7501L;
    static final Long WAREHOUSE_ID = 7801L;

    static final Long M1 = 8201L;  // 采购件（lot size 测试）
    static final Long M2 = 8202L;  // 采购件（safety stock 测试）
    static final Long M3 = 8203L;  // 采购件（lead time 测试）

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    SimulationMrpEngine simulationMrpEngine;
    @Inject
    ErpMfgSimulationParamResolver paramResolver;
    @Inject
    app.erp.mfg.service.simulation.SimulationVersionComparator simulationComparator;

    @AfterEach
    public void resetConfig() {
        // 确保 config-gate 默认值复原
        AppConfig.getConfigProvider().assignConfigValue(ErpMfgConstants.CONFIG_MFG_SIMULATION_ENABLED, "false");
        AppConfig.getConfigProvider().assignConfigValue(ErpMfgConstants.CONFIG_MRP_DEFAULT_LOT_SIZE, "0");
        paramResolver.invalidateCache();
    }

    @Test
    public void testConfigGateDisabledRejectsRunSimulation() {
        // config 默认 false → 仿真入口抛 ERR_MFG_SIMULATION_DISABLED
        Long scenarioId = seedScenario("SIM-DISABLED", null);
        ApiResponse<?> resp = runSimulationRpc(scenarioId);
        assertNotEquals(0, resp.getStatus());
        assertEquals(ErpMfgErrors.ERR_MFG_SIMULATION_DISABLED.getErrorCode(), resp.getCode());
    }

    @Test
    public void testLotSizeOverrideAffectsPlannedQuantity() {
        // 物料 M1，net=12；场景覆盖 LOT_SIZE=10 → planned=ceil(12/10)*10=20
        // 基线（无覆盖）→ planned=12（lot-for-lot，全局配置默认 0）
        seedMaterial(M1, null, null);

        // 基线 plan（先跑一次单次 MRP 建立基线）
        Long basePlanId = seedPlan("BASE-LOT");
        seedManualDemand(basePlanId, M1, bd("12"), LocalDate.of(2026, 8, 15));
        runMrpOnce(basePlanId);
        ErpMfgMrpPlanLine baseLine = findLine(basePlanId, M1);
        assertEquals(0, baseLine.getPlannedQuantity().compareTo(bd("12")), "基线 lot-for-lot=12");

        // 仿真场景（LOT_SIZE 覆盖 10）
        enableSimulation();
        Long scenarioId = seedScenario("SIM-LOT", basePlanId);
        seedParam(scenarioId, null, ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE, bd("10"));

        ErpMfgMrpScenarioVersion version = runSimulationViaRpc(scenarioId);

        // 验证：仿真版本产 COMPUTED plan，lot size=10 → planned=20
        ErpMfgMrpPlanLine simLine = findLine(version.getComputedMrpPlanId(), M1);
        assertEquals(0, simLine.getPlannedQuantity().compareTo(bd("20")), "仿真 LOT_SIZE=10 → planned=20");
        assertEquals(0, simLine.getNetRequirement().compareTo(bd("12")), "净需求不变=12");
        assertEquals(ErpMfgConstants.SIMULATION_STATUS_COMPLETED, version.getStatus());
        assertNotNull(version.getSnapshotSummary());
    }

    @Test
    public void testLeadTimeOverrideAffectsPlannedDate() {
        // 物料 M3，需求日 2026-08-20；基线 leadTimeDays=7 → plannedDate=2026-08-13
        // 场景覆盖 LEAD_TIME=3 → plannedDate=2026-08-17
        seedMaterial(M3, 7, null);

        Long basePlanId = seedPlan("BASE-LT");
        seedManualDemand(basePlanId, M3, bd("5"), LocalDate.of(2026, 8, 20));
        runMrpOnce(basePlanId);
        ErpMfgMrpPlanLine baseLine = findLine(basePlanId, M3);
        assertEquals(LocalDate.of(2026, 8, 13), baseLine.getPlannedDate(),
                "基线 leadTime=7 → plannedDate=2026-08-20 - 7");

        enableSimulation();
        Long scenarioId = seedScenario("SIM-LT", basePlanId);
        seedParam(scenarioId, M3, ErpMfgConstants.SIMULATION_PARAM_TYPE_LEAD_TIME, bd("3"));

        ErpMfgMrpScenarioVersion version = runSimulationViaRpc(scenarioId);
        ErpMfgMrpPlanLine simLine = findLine(version.getComputedMrpPlanId(), M3);
        assertEquals(LocalDate.of(2026, 8, 17), simLine.getPlannedDate(),
                "仿真 LEAD_TIME=3 → plannedDate=2026-08-20 - 3");
    }

    @Test
    public void testSafetyStockOverrideAffectsDemand() {
        // 物料 M2，主数据 safetyStock=5；库存 0；基线 SAFETY_STOCK 需求 = 5 - 0 = 5
        // 场景覆盖 SAFETY_STOCK=10 → 仿真 SAFETY_STOCK 需求 = 10 - 0 = 10
        seedMaterial(M2, null, bd("5"));
        seedBalance(M2, bd("0"));

        Long basePlanId = seedPlan("BASE-SS");
        runMrpOnce(basePlanId);
        ErpMfgMrpPlanLine baseLine = findLine(basePlanId, M2);
        assertEquals(0, baseLine.getGrossRequirement().compareTo(bd("5")),
                "基线 SAFETY_STOCK=5 → gross=5");

        enableSimulation();
        Long scenarioId = seedScenario("SIM-SS", basePlanId);
        seedParam(scenarioId, M2, ErpMfgConstants.SIMULATION_PARAM_TYPE_SAFETY_STOCK, bd("10"));

        ErpMfgMrpScenarioVersion version = runSimulationViaRpc(scenarioId);
        ErpMfgMrpPlanLine simLine = findLine(version.getComputedMrpPlanId(), M2);
        assertEquals(0, simLine.getGrossRequirement().compareTo(bd("10")),
                "仿真 SAFETY_STOCK=10 → gross=10");
    }

    @Test
    public void testParamResolverFallbackOrder() {
        // 场景：全局 LOT_SIZE=10 + 物料 M1 精确覆盖 LOT_SIZE=20
        // 解析顺序（Decision B 回退）：精确 materialId → 全局 materialId=null
        // M1 → 20（精确优先）；M2 → 10（全局回退）
        seedMaterial(M1, null, null);
        Long scenarioId = seedScenario("SIM-RESOLVE", null);
        seedParam(scenarioId, null, ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE, bd("10"));
        seedParam(scenarioId, M1, ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE, bd("20"));

        Long M2 = 8299L;
        seedMaterial(M2, null, null);

        // M1 精确覆盖=20
        assertEquals(0, paramResolver.resolveOverride(scenarioId, M1, ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE)
                .compareTo(bd("20")), "M1 精确覆盖=20");
        // M2 无物料级覆盖 → 全局回退=10
        assertEquals(0, paramResolver.resolveOverride(scenarioId, M2, ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE)
                .compareTo(bd("10")), "M2 全局回退=10");
        // 未设置场景 → null
        assertNull(paramResolver.resolveOverride(999999L, M1, ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE),
                "空场景 → null");
    }

    @Test
    public void testPromoteToFormalPlanCreatesDraftAndArchivesVersion() {
        seedMaterial(M1, null, null);
        Long basePlanId = seedPlan("BASE-PROM");
        seedManualDemand(basePlanId, M1, bd("8"), LocalDate.of(2026, 8, 15));
        runMrpOnce(basePlanId);

        enableSimulation();
        Long scenarioId = seedScenario("SIM-PROM", basePlanId);
        seedParam(scenarioId, null, ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE, bd("10"));

        ErpMfgMrpScenarioVersion version = runSimulationViaRpc(scenarioId);

        // 转正式计划
        ErpMfgMrpPlan promoted = promoteToFormalPlanViaRpc(version.getId());
        assertEquals(ErpMfgConstants.MRP_STATUS_DRAFT, promoted.getStatus());
        assertTrue(promoted.getCode().contains("-PROMOTED-1"),
                "promoted plan code 含 -PROMOTED-{versionNo} 后缀");

        // 验证复制了计划行
        List<ErpMfgMrpPlanLine> promotedLines = linesOf(promoted.getId());
        assertTrue(promotedLines.size() > 0, "promoted plan 含计划行");
        for (ErpMfgMrpPlanLine line : promotedLines) {
            assertEquals(Boolean.FALSE, line.getIsFirmed());
            assertNull(line.getConvertedBillCode());
        }

        // 版本 → ARCHIVED + promotedPlanId 已设置
        ErpMfgMrpScenarioVersion reloaded = daoProvider.daoFor(ErpMfgMrpScenarioVersion.class)
                .getEntityById(version.getId());
        assertEquals(ErpMfgConstants.SIMULATION_STATUS_ARCHIVED, reloaded.getStatus());
        assertEquals(promoted.getId(), reloaded.getPromotedPlanId());

        // 重复转正 → 拒绝
        ApiResponse<?> resp = promoteToFormalPlanRpc(version.getId());
        assertNotEquals(0, resp.getStatus());
        assertEquals(ErpMfgErrors.ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED.getErrorCode(),
                resp.getCode());
    }

    @Test
    public void testRunSimulationRejectsNonDraftScenario() {
        enableSimulation();
        seedMaterial(M1, null, null);
        Long basePlanId = seedPlan("BASE-ND");
        seedManualDemand(basePlanId, M1, bd("1"), LocalDate.of(2026, 8, 15));
        runMrpOnce(basePlanId);

        Long scenarioId = seedScenario("SIM-NONDRAFT", basePlanId);
        runSimulationViaRpc(scenarioId); // → COMPLETED

        // 再跑 → 拒绝（非 DRAFT）
        ApiResponse<?> resp = runSimulationRpc(scenarioId);
        assertNotEquals(0, resp.getStatus());
        assertEquals(ErpMfgErrors.ERR_MFG_SIMULATION_SCENARIO_NOT_DRAFT.getErrorCode(),
                resp.getCode());
    }

    @Test
    public void testCompareVersionsProducesStructuredDiff() {
        // 同场景两版本（不同 LOT_SIZE 覆盖），对比应观测到建议量差
        seedMaterial(M1, null, null);
        Long basePlanId = seedPlan("BASE-CMP");
        seedManualDemand(basePlanId, M1, bd("25"), LocalDate.of(2026, 8, 15));
        runMrpOnce(basePlanId);

        enableSimulation();
        Long scenarioId = seedScenario("SIM-CMP", basePlanId);

        // 版本 A：LOT_SIZE=10 → planned=ceil(25/10)*10=30
        seedParam(scenarioId, null, ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE, bd("10"));
        ErpMfgMrpScenarioVersion vA = runSimulationViaRpc(scenarioId);

        // 重置场景为 DRAFT（仿真后场景 COMPLETED，要新建版本须先重置）
        resetScenarioToDraft(scenarioId);
        paramResolver.invalidateCache();
        // 版本 B：LOT_SIZE=20 → planned=ceil(25/20)*20=40
        updateLotSizeParam(scenarioId, bd("20"));
        ErpMfgMrpScenarioVersion vB = runSimulationViaRpc(scenarioId);

        // 对比 B - A：建议量差 = 40 - 30 = 10
        SimulationDiffResult diff = simulationComparator.compareMrpVersions(vA.getId(), vB.getId());
        assertNotNull(diff);
        assertEquals(vA.getId(), diff.getVersionIdA());
        assertEquals(vB.getId(), diff.getVersionIdB());
        assertEquals(0, diff.getTotalPlannedQuantityDelta().compareTo(bd("10")),
                "B(LOT=20,planned=40) - A(LOT=10,planned=30) = +10");
        assertTrue(diff.getLineDiffs().size() >= 1, "至少 1 行 diff");
        // 缺料物料集：M1 在两版本均缺料（net=25>0）
        assertTrue(diff.getShortageInBoth().contains(M1), "M1 在两版本均缺料");
    }

    // ---------- helpers ----------

    private void enableSimulation() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_MFG_SIMULATION_ENABLED, "true");
    }

    private void runMrpOnce(Long planId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("planId", planId);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpMfgMrpPlan__runMrp", ApiRequest.build(args));
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), "基线 runMrp 应成功: " + resp);
    }

    private ErpMfgMrpScenarioVersion runSimulationViaRpc(Long scenarioId) {
        ApiResponse<?> resp = runSimulationRpc(scenarioId);
        assertEquals(0, resp.getStatus(), "runSimulation 应成功: " + resp);
        // GraphQL RPC 返回 Map，按 id 取出实体后读取
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        Long versionId = Long.valueOf(String.valueOf(data.get("id")));
        return daoProvider.daoFor(ErpMfgMrpScenarioVersion.class).getEntityById(versionId);
    }

    private ApiResponse<?> runSimulationRpc(Long scenarioId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("scenarioId", scenarioId);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation,
                "ErpMfgMrpScenario__runSimulation", ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpMfgMrpPlan promoteToFormalPlanViaRpc(Long versionId) {
        ApiResponse<?> resp = promoteToFormalPlanRpc(versionId);
        assertEquals(0, resp.getStatus(), "promoteToFormalPlan 应成功: " + resp);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        Long promotedId = Long.valueOf(String.valueOf(data.get("id")));
        return daoProvider.daoFor(ErpMfgMrpPlan.class).getEntityById(promotedId);
    }

    private ApiResponse<?> promoteToFormalPlanRpc(Long versionId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("scenarioVersionId", versionId);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation,
                "ErpMfgMrpScenario__promoteToFormalPlan", ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpMfgMrpPlanLine findLine(Long planId, Long materialId) {
        List<ErpMfgMrpPlanLine> lines = linesOf(planId);
        for (ErpMfgMrpPlanLine l : lines) {
            if (materialId.equals(l.getMaterialId())) {
                return l;
            }
        }
        return null;
    }

    private List<ErpMfgMrpPlanLine> linesOf(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        return daoProvider.daoFor(ErpMfgMrpPlanLine.class).findAllByQuery(q);
    }

    private Long seedPlan(String code) {
        Long id = 8600L + (long) Math.abs(code.hashCode() % 200);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMrpPlan> dao = daoProvider.daoFor(ErpMfgMrpPlan.class);
            ErpMfgMrpPlan plan = new ErpMfgMrpPlan();
            plan.orm_propValueByName("id", id);
            plan.setCode(code);
            plan.setOrgId(ORG_ID);
            plan.setBusinessDate(LocalDate.of(2026, 8, 1));
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
            d.orm_propValueByName("id", 9200L + (long) Math.abs((planId + "" + materialId).hashCode() % 300));
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

    private Long seedScenario(String code, Long basePlanId) {
        Long id = 9800L + (long) Math.abs(code.hashCode() % 100);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMrpScenario> dao = daoProvider.daoFor(ErpMfgMrpScenario.class);
            ErpMfgMrpScenario s = new ErpMfgMrpScenario();
            s.orm_propValueByName("id", id);
            s.setCode(code);
            s.setOrgId(ORG_ID);
            s.setBaseMrpPlanId(basePlanId);
            s.setStatus(ErpMfgConstants.SIMULATION_STATUS_DRAFT);
            dao.saveEntity(s);
        });
        return id;
    }

    private void seedParam(Long scenarioId, Long materialId, String paramType, BigDecimal value) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMrpScenarioParam> dao = daoProvider.daoFor(ErpMfgMrpScenarioParam.class);
            ErpMfgMrpScenarioParam p = new ErpMfgMrpScenarioParam();
            p.orm_propValueByName("id",
                    9900L + (long) Math.abs((scenarioId + "" + materialId + paramType).hashCode() % 80));
            p.setScenarioId(scenarioId);
            p.setMaterialId(materialId);
            p.setParamType(paramType);
            p.setParamValue(value);
            dao.saveEntity(p);
        });
        paramResolver.invalidateCache();
    }

    private void updateLotSizeParam(Long scenarioId, BigDecimal newValue) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMrpScenarioParam> dao = daoProvider.daoFor(ErpMfgMrpScenarioParam.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("scenarioId", scenarioId));
            q.addFilter(eq("paramType", ErpMfgConstants.SIMULATION_PARAM_TYPE_LOT_SIZE));
            for (ErpMfgMrpScenarioParam p : dao.findAllByQuery(q)) {
                p.setParamValue(newValue);
                dao.saveOrUpdateEntity(p);
            }
        });
        paramResolver.invalidateCache();
    }

    private void resetScenarioToDraft(Long scenarioId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMrpScenario> dao = daoProvider.daoFor(ErpMfgMrpScenario.class);
            ErpMfgMrpScenario s = dao.getEntityById(scenarioId);
            if (s != null) {
                s.setStatus(ErpMfgConstants.SIMULATION_STATUS_DRAFT);
                dao.saveOrUpdateEntity(s);
            }
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

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
