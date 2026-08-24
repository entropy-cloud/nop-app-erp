package io.nop.app.all.it;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.dao.entity.ErpDrpScenario;
import app.erp.drp.dao.entity.ErpDrpScenarioParam;
import app.erp.drp.dao.entity.ErpDrpScenarioVersion;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.drp.service.simulation.ErpDrpSimulationParamResolver;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B10 C20b：DRP 仿真与正式计划提升（按 {@code docs/design/integration-testing.md §6 C20b} 规格）。
 *
 * <p>全链（引用部署 seed：组织 2 / MAT-004 物料 4 / WH-MAIN 仓库 1；drp 域无部署 seed，全自包含建数）：
 * 自包含基线 DRP 计划 + 补货参数（MAT-004@WH-MAIN 安全库存 10 / LOT_FOR_LOT）→
 * {@code ErpDrpPlan__runDrp}（基线 COMPUTED，行 suggested=10）→ 自包含仿真场景（基线计划回链 +
 * SAFETY_STOCK 覆盖参数 20）→ {@code ErpDrpScenario__runSimulation}（@NopTestProperty 开启
 * {@code erp-drp.simulation-enabled} → V1 仿真版本 COMPLETED + COMPUTED 计划
 * {@code IT-C20B-BASE-001-SIM-V1}，行 SS=20/suggested=20）→ 场景重置 DRAFT + 覆盖参数 20→30
 * （DAO fixture + paramResolver.invalidateCache，TestErpDrpSimulation 先例——进程内 ParamResolver
 * 缓存 computeIfAbsent 不刷新）→ 二次 runSimulation（V2，行 SS=30/suggested=30）→
 * {@code ErpDrpScenario__compareVersions}（B−A：totalReplenishmentQtyDelta=+10 /
 * totalSafetyStockDelta=+10 / lineDiffs=1——JUnit 层可 invalidateCache，浏览器层降级口径不受限）→
 * {@code ErpDrpScenario__promoteToFormalPlan}（V2 → 正式计划 DRAFT，code 后缀 -PROMOTED-2，
 * 复制 SUGGESTED 行 SS=30；V2 → ARCHIVED + promotedPlanId 回链）→
 * {@code ErpDrpPlan__runDrp}（正式计划重算：DRAFT→COMPUTED，行 SS/suggested **回落基线参数 10**——
 * 仿真覆盖仅经 SimulationDrpEngine 生效，不写回 ErpDrpParameter）。
 *
 * <p>Phase 2 Decision（三项裁决，落盘设计文档 §6 C20b 勘误）：
 * <ol>
 *   <li><b>仿真版本生成形态</b>：runSimulation 返回 {@code ErpDrpScenarioVersion}（versionNo 递增、
 *       status=COMPLETED、computedDrpPlanId 回链新建 COMPUTED 计划 {@code {baseCode}-SIM-V{n}}）；
 *       场景状态 DRAFT→COMPLETED，重跑须重置 DRAFT（无重置动作，DAO fixture 先例）。</li>
 *   <li><b>compareVersions 断言粒度</b>：JUnit 层可精确 delta（+10/+10）——进程内 invalidateCache
 *       可达，ParamResolver 缓存约束仅浏览器层（webServer 单实例无 invalidate mutation）降级为结构非空。</li>
 *   <li><b>promote 后正式计划状态与 runDrp 口径</b>：promoted 计划 = **DRAFT**（非 COMPUTED 直通），
 *       复制仿真行（SS=30）为 SUGGESTED；后续 runDrp 守卫 DRAFT 通过 → 重算**回落基线参数**
 *       （SS=10/suggested=10）——「提升后可直接运行」成立，但净需求口径以基线参数为准非仿真覆盖值。</li>
 * </ol>
 *
 * <p>冻结时钟 {@code B10FrozenClockExtension}（2026-07-17，与 C20a 共用）：计算计划 runAt/businessDate
 * 落库列取确定值。
 *
 * <p>三层验证：层 1 = JUnit 关键断言（V1/V2 行数值、compareVersions delta、promoted 计划状态/编码/
 * 行复制、V2 ARCHIVED、正式计划重算回落）；层 2 = 每步 response 快照；层 3 = output/tables 变更行
 * （drp_plan/line + scenario/version/param）。RECORDING→CHECKING 往返按 M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-drp.simulation-enabled", value = "true")
public class TestErpC20bDrpSimulationPromote extends ErpIntegrationTestCase {

    static final String BASE_CODE = "IT-C20B-BASE-001";
    static final String SCENARIO_CODE = "IT-C20B-SIM-001";

    static final BigDecimal BASE_SS = new BigDecimal("10");
    static final BigDecimal V1_SS = new BigDecimal("20");
    static final BigDecimal V2_SS = new BigDecimal("30");
    static final BigDecimal DELTA = new BigDecimal("10");

    @RegisterExtension
    static B10FrozenClockExtension frozenClock = new B10FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpDrpSimulationParamResolver paramResolver;

    @Test
    public void testDrpSimulationPromote() {
        // ---------- 1. 自包含基线计划 + 补货参数（MAT-004@WH-MAIN，SS=10，LOT_FOR_LOT） ----------
        ApiResponse<?> planSave = rpcMutation("ErpDrpPlan__save", request("1_plan_save.json5", Map.class));
        output("1_plan_save_response.json5", planSave);
        assertEquals(0, planSave.getStatus(), "基线计划保存应成功");
        String basePlanId = idOf(planSave);
        addVar("basePlanId", basePlanId);

        ApiResponse<?> paramSave = rpcMutation("ErpDrpParameter__save", request("2_param_save.json5", Map.class));
        output("2_param_save_response.json5", paramSave);
        assertEquals(0, paramSave.getStatus(), "补货参数保存应成功");

        // ---------- 2. 基线 runDrp（COMPUTED，行 suggested=10） ----------
        ApiResponse<?> baseRun = rpcMutation("ErpDrpPlan__runDrp", request("3_run_drp.json5", Map.class));
        output("3_run_drp_response.json5", baseRun);
        assertEquals(0, baseRun.getStatus(), "基线 runDrp 应成功");
        ErpDrpLine baseLine = findLine(basePlanId, "4", "1");
        assertNotNull(baseLine, "基线行（MAT-004@WH-MAIN）应存在");
        assertEquals(0, BASE_SS.compareTo(baseLine.getSuggestedQty()), "基线 SS=10 → suggested=10");

        // ---------- 3. 仿真场景 + SAFETY_STOCK 覆盖参数（20） ----------
        ApiResponse<?> scenarioSave = rpcMutation("ErpDrpScenario__save", request("4_scenario_save.json5", Map.class));
        output("4_scenario_save_response.json5", scenarioSave);
        assertEquals(0, scenarioSave.getStatus(), "仿真场景保存应成功");
        String scenarioId = idOf(scenarioSave);
        addVar("scenarioId", scenarioId);

        ApiResponse<?> paramOverride = rpcMutation("ErpDrpScenarioParam__save",
                request("5_scenario_param_save.json5", Map.class));
        output("5_scenario_param_save_response.json5", paramOverride);
        assertEquals(0, paramOverride.getStatus(), "仿真覆盖参数保存应成功");

        // ---------- 4. runSimulation V1（仿真版本 + COMPUTED 计划，行 SS=20/suggested=20） ----------
        ApiResponse<?> sim1 = rpcMutation("ErpDrpScenario__runSimulation", request("6_run_simulation.json5", Map.class));
        output("6_run_simulation_response.json5", sim1);
        assertEquals(0, sim1.getStatus(), "runSimulation V1 应成功（simulation-enabled 门控开启）");
        String versionAId = idOf(sim1);
        addVar("versionAId", versionAId);
        ErpDrpScenarioVersion v1 = reload(ErpDrpScenarioVersion.class, versionAId);
        assertEquals(ErpDrpConstants.SIMULATION_STATUS_COMPLETED, v1.getStatus(), "V1 版本 COMPLETED");
        assertEquals(Integer.valueOf(1), v1.getVersionNo(), "V1 versionNo=1");
        ErpDrpLine v1Line = findLine(v1.getComputedDrpPlanId(), "4", "1");
        assertNotNull(v1Line, "V1 仿真行应存在");
        assertEquals(0, V1_SS.compareTo(v1Line.getSafetyStock()), "V1 仿真 SS=20（覆盖生效）");
        assertEquals(0, V1_SS.compareTo(v1Line.getSuggestedQty()), "V1 suggested=20");

        // ---------- 5. 场景重置 DRAFT + 覆盖参数 20→30（DAO fixture + 缓存失效） ----------
        ormTemplate.runInSession(() -> {
            ErpDrpScenario s = daoProvider.daoFor(ErpDrpScenario.class).getEntityById(scenarioId);
            s.setStatus(ErpDrpConstants.SIMULATION_STATUS_DRAFT);
            daoProvider.daoFor(ErpDrpScenario.class).saveOrUpdateEntity(s);
            for (ErpDrpScenarioParam p : daoProvider.daoFor(ErpDrpScenarioParam.class)
                    .findAllByQuery(eqFilterQuery("scenarioId", scenarioId))) {
                p.setParamValue(V2_SS);
                daoProvider.daoFor(ErpDrpScenarioParam.class).saveOrUpdateEntity(p);
            }
        });
        paramResolver.invalidateCache();

        // ---------- 6. runSimulation V2（行 SS=30/suggested=30） ----------
        ApiResponse<?> sim2 = rpcMutation("ErpDrpScenario__runSimulation", request("7_run_simulation_v2.json5", Map.class));
        output("7_run_simulation_v2_response.json5", sim2);
        assertEquals(0, sim2.getStatus(), "runSimulation V2 应成功");
        String versionBId = idOf(sim2);
        addVar("versionBId", versionBId);
        ErpDrpScenarioVersion v2 = reload(ErpDrpScenarioVersion.class, versionBId);
        assertEquals(Integer.valueOf(2), v2.getVersionNo(), "V2 versionNo=2");
        ErpDrpLine v2Line = findLine(v2.getComputedDrpPlanId(), "4", "1");
        assertNotNull(v2Line, "V2 仿真行应存在");
        assertEquals(0, V2_SS.compareTo(v2Line.getSafetyStock()), "V2 仿真 SS=30");
        assertEquals(0, V2_SS.compareTo(v2Line.getSuggestedQty()), "V2 suggested=30");
        assertEquals(ErpDrpConstants.SIMULATION_STATUS_COMPLETED,
                reload(ErpDrpScenario.class, scenarioId).getStatus(), "场景终态 COMPLETED");

        // ---------- 7. compareVersions（B−A：补货量/安全库存 delta=+10） ----------
        ApiResponse<?> compare = executeRpc(GraphQLOperationType.query, "ErpDrpScenario__compareVersions",
                request("8_compare_versions.json5", Map.class));
        output("8_compare_versions_response.json5", compare);
        assertEquals(0, compare.getStatus(), "compareVersions 应成功");
        Map<?, ?> diff = (Map<?, ?>) compare.getData();
        // 层 1 锚点：JUnit 层精确 delta（invalidateCache 可达，浏览器层降级口径不受限）
        assertEquals(0, DELTA.compareTo(new BigDecimal(String.valueOf(diff.get("totalReplenishmentQtyDelta")))),
                "B(SS=30) − A(SS=10覆盖) 补货量差 = +10");
        assertEquals(0, DELTA.compareTo(new BigDecimal(String.valueOf(diff.get("totalSafetyStockDelta")))),
                "安全库存差 = +10");
        assertEquals(1, ((List<?>) diff.get("lineDiffs")).size(), "恰 1 行 diff");

        // ---------- 8. promoteToFormalPlan（V2 → 正式 DRAFT 计划 + 复制行；V2 ARCHIVED） ----------
        ApiResponse<?> promote = rpcMutation("ErpDrpScenario__promoteToFormalPlan",
                request("9_promote_to_formal_plan.json5", Map.class));
        output("9_promote_to_formal_plan_response.json5", promote);
        assertEquals(0, promote.getStatus(), "promoteToFormalPlan 应成功");
        String promotedPlanId = idOf(promote);
        addVar("promotedPlanId", promotedPlanId);
        ErpDrpPlan promoted = reload(ErpDrpPlan.class, promotedPlanId);
        // 层 1 锚点：promote 后正式计划 DRAFT + 编码后缀 -PROMOTED-2
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT, promoted.getStatus(), "提升计划 DRAFT");
        assertTrue(promoted.getCode().endsWith("-PROMOTED-2"), "提升计划编码含 -PROMOTED-2: " + promoted.getCode());
        ErpDrpLine promotedLine = findLine(promotedPlanId, "4", "1");
        assertNotNull(promotedLine, "提升计划复制行应存在");
        assertEquals(0, V2_SS.compareTo(promotedLine.getSafetyStock()), "复制行 SS=30（仿真口径）");
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED, promotedLine.getStatus(), "复制行 SUGGESTED");
        ErpDrpScenarioVersion v2After = reload(ErpDrpScenarioVersion.class, versionBId);
        assertEquals(ErpDrpConstants.SIMULATION_STATUS_ARCHIVED, v2After.getStatus(), "V2 版本 ARCHIVED");
        assertEquals(promotedPlanId, String.valueOf(v2After.getPromotedPlanId()), "V2 promotedPlanId 回链");

        // ---------- 9. 正式计划 runDrp（DRAFT→COMPUTED，口径回落基线参数 SS=10） ----------
        ApiResponse<?> promotedRun = rpcMutation("ErpDrpPlan__runDrp", request("10_promoted_run_drp.json5", Map.class));
        output("10_promoted_run_drp_response.json5", promotedRun);
        assertEquals(0, promotedRun.getStatus(), "提升计划 runDrp 应成功");
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED,
                reload(ErpDrpPlan.class, promotedPlanId).getStatus(), "提升计划重算后 COMPUTED");
        ErpDrpLine rerunLine = findLine(promotedPlanId, "4", "1");
        assertNotNull(rerunLine, "提升计划重算行应存在");
        // 层 1 锚点：正式计划重算回落基线参数（仿真覆盖不写回 ErpDrpParameter）
        assertEquals(0, BASE_SS.compareTo(rerunLine.getSafetyStock()), "重算行 SS 回落基线 10");
        assertEquals(0, BASE_SS.compareTo(rerunLine.getSuggestedQty()), "重算行 suggested 回落基线 10");
    }

    // ---------- helpers ----------

    private ErpDrpLine findLine(String planId, String materialId, String warehouseId) {
        var q = eqFilterQuery("planId", planId);
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("materialId", materialId));
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("warehouseId", warehouseId));
        List<ErpDrpLine> list = daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
