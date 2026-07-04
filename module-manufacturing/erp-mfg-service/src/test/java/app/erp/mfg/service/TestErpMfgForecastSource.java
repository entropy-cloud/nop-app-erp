package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgForecast;
import app.erp.mfg.dao.entity.ErpMfgForecastLine;
import app.erp.mfg.dao.entity.ErpMfgMrpDemand;
import app.erp.mfg.dao.entity.ErpMfgMrpPlan;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.md.dao.entity.ErpMdMaterial;
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
 * 需求预测实体 + MRP FORECAST 需求来源测试（plan 2026-07-05-0427-1 Phase 2 Exit Criteria）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>预测状态机 approve(DRAFT→APPROVED) / cancel(DRAFT|APPROVED→CANCELLED) + 非法迁移 ErrorCode</li>
 *   <li>MRP 运行含 approved 预测时，结果含 demandSource=FORECAST 的毛需求行</li>
 *   <li>无预测时行为与基线一致（无回归）</li>
 *   <li>config-gated erp-mfg.forecast-consume-enabled=false 时不消费</li>
 * </ul>
 */
@io.nop.api.core.annotations.autotest.NopTestConfig(localDb = true,
        initDatabaseSchema = io.nop.api.core.annotations.core.OptionalBoolean.TRUE,
        enableActionAuth = io.nop.api.core.annotations.core.OptionalBoolean.FALSE)
public class TestErpMfgForecastSource extends JunitAutoTestCase {

    static final Long ORG_ID = 9401L;
    static final Long UOM_ID = 9501L;
    static final Long M_FORECAST = 9101L;  // 仅预测来源的物料
    static final Long M_NOFORECAST = 9102L; // 无预测的物料（回归用）

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testStateMachineApproveCancel() {
        Long forecastId = seedForecast("FCST-SM", ErpMfgConstants.FORECAST_STATUS_DRAFT, M_FORECAST,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), bd("1"),
                null, null, null);

        // approve DRAFT → APPROVED
        assertEquals(0, rpcStatus(mutation, "ErpMfgForecast__approve", Map.of("id", String.valueOf(forecastId))));
        assertEquals(ErpMfgConstants.FORECAST_STATUS_APPROVED,
                daoProvider.daoFor(ErpMfgForecast.class).getEntityById(forecastId).getStatus());

        // approve again on APPROVED → rejected (illegal transition)
        ApiResponse<?> again = rpc(mutation, "ErpMfgForecast__approve", Map.of("id", String.valueOf(forecastId)));
        assertTrue(again.getStatus() != 0, "APPROVED 再次 approve 应拒绝");
        assertEquals(ErpMfgErrors.ERR_FORECAST_ILLEGAL_STATUS_TRANSITION.getErrorCode(), again.getCode());

        // cancel APPROVED → CANCELLED
        assertEquals(0, rpcStatus(mutation, "ErpMfgForecast__cancel", Map.of("id", String.valueOf(forecastId))));
        assertEquals(ErpMfgConstants.FORECAST_STATUS_CANCELLED,
                daoProvider.daoFor(ErpMfgForecast.class).getEntityById(forecastId).getStatus());

        // cancel on CANCELLED → rejected (terminal)
        ApiResponse<?> cancelAgain = rpc(mutation, "ErpMfgForecast__cancel", Map.of("id", String.valueOf(forecastId)));
        assertTrue(cancelAgain.getStatus() != 0, "CANCELLED 再次 cancel 应拒绝");
    }

    @Test
    public void testForecastDemandAggregation() {
        seedMaterial(M_FORECAST);
        // APPROVED 预测：M_FORECAST 区间内 100 + 区间内 50（同物料多桶累加）
        Long approvedHead = seedForecast("FCST-OK", ErpMfgConstants.FORECAST_STATUS_APPROVED, M_FORECAST,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), bd("100"),
                LocalDate.of(2026, 7, 11), LocalDate.of(2026, 7, 20), bd("50"));
        // DRAFT 预测：不应消费
        seedForecast("FCST-DRAFT", ErpMfgConstants.FORECAST_STATUS_DRAFT, M_FORECAST,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), bd("999"),
                null, null, null);
        // APPROVED 但区间外：不应消费（plan 在 7-1，horizon=10 → 7-11；预测行 8-1~8-10 不相交）
        seedForecast("FCST-OUT", ErpMfgConstants.FORECAST_STATUS_APPROVED, M_FORECAST,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), bd("888"),
                null, null, null);

        Long planId = seedPlan("MRP-FCST");
        runMrpOk(planId);

        // FORECAST 需求聚合 = 100 + 50 = 150
        assertEquals(0, sumDemand(planId, M_FORECAST, ErpMfgConstants.MRP_DEMAND_SOURCE_FORECAST).compareTo(bd("150")),
                "APPROVED + 区间内 同物料多桶累加");

        // DRAFT/区间外 未消费
        List<ErpMfgMrpDemand> all = demandsOf(planId, M_FORECAST);
        assertEquals(1, all.size(), "仅 1 条 FORECAST 需求行（DRAFT/区间外未消费）");
        assertEquals(ErpMfgConstants.MRP_DEMAND_SOURCE_FORECAST, all.get(0).getDemandSource());
    }

    @Test
    public void testForecastDisabledNoRegression() {
        seedMaterial(M_FORECAST);
        seedForecast("FCST-OFF", ErpMfgConstants.FORECAST_STATUS_APPROVED, M_FORECAST,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), bd("100"),
                null, null, null);

        setConfig(ErpMfgConstants.CONFIG_MFG_FORECAST_CONSUME_ENABLED, "false");
        try {
            Long planId = seedPlan("MRP-OFF");
            runMrpOk(planId);
            assertEquals(BigDecimal.ZERO, sumDemand(planId, M_FORECAST, ErpMfgConstants.MRP_DEMAND_SOURCE_FORECAST),
                    "config-gated 关闭时不消费");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_MFG_FORECAST_CONSUME_ENABLED, "true");
        }
    }

    @Test
    public void testForecastConsumedByMrp() {
        seedMaterial(M_NOFORECAST);
        Long planId = seedPlan("MRP-NOFCST");
        runMrpOk(planId);
        // 无预测 → 无 FORECAST 需求行（基线一致）
        assertEquals(BigDecimal.ZERO, sumDemand(planId, M_NOFORECAST, ErpMfgConstants.MRP_DEMAND_SOURCE_FORECAST),
                "无预测时 FORECAST 行为 0（无回归）");
        // 同时计划行应存在（即便 net 为 0，因无任何独立需求 → 无计划行）
        List<ErpMfgMrpPlanLine> lines = linesOf(planId);
        assertTrue(lines.isEmpty() || lines.stream().noneMatch(l -> M_NOFORECAST.equals(l.getMaterialId())),
                "无独立需求的物料不应产生计划行");
    }

    // ---------- helpers ----------

    private void runMrpOk(Long planId) {
        ApiResponse<?> resp = rpc(mutation, "ErpMfgMrpPlan__runMrp", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "runMrp 应成功: " + resp);
    }

    private int rpcStatus(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        return rpc(op, action, args).getStatus();
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private BigDecimal sumDemand(Long planId, Long materialId, String demandSource) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("demandSource", demandSource));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpMfgMrpDemand d : daoProvider.daoFor(ErpMfgMrpDemand.class).findAllByQuery(q)) {
            sum = sum.add(d.getQuantity() != null ? d.getQuantity() : BigDecimal.ZERO);
        }
        return sum;
    }

    private List<ErpMfgMrpDemand> demandsOf(Long planId, Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        q.addFilter(eq("materialId", materialId));
        return daoProvider.daoFor(ErpMfgMrpDemand.class).findAllByQuery(q);
    }

    private List<ErpMfgMrpPlanLine> linesOf(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        return daoProvider.daoFor(ErpMfgMrpPlanLine.class).findAllByQuery(q);
    }

    private Long seedPlan(String code) {
        Long id = 9001L + (long) Math.abs(code.hashCode() % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMrpPlan> dao = daoProvider.daoFor(ErpMfgMrpPlan.class);
            ErpMfgMrpPlan plan = new ErpMfgMrpPlan();
            plan.orm_propValueByName("id", id);
            plan.setCode(code);
            plan.setOrgId(ORG_ID);
            plan.setBusinessDate(LocalDate.of(2026, 7, 1));
            plan.setPlanningHorizonDays(10);
            plan.setStatus(ErpMfgConstants.MRP_STATUS_DRAFT);
            dao.saveEntity(plan);
        });
        return id;
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

    /** 创建预测头 + 1 或 2 行；status=预测头状态。返回头 ID。 */
    private Long seedForecast(String code, String status, Long materialId,
                              LocalDate p1Start, LocalDate p1End, BigDecimal q1,
                              LocalDate p2Start, LocalDate p2End, BigDecimal q2) {
        Long headId = 9200L + (long) Math.abs(code.hashCode() % 400);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgForecast> headDao = daoProvider.daoFor(ErpMfgForecast.class);
            ErpMfgForecast head = new ErpMfgForecast();
            head.orm_propValueByName("id", headId);
            head.setCode(code);
            head.setOrgId(ORG_ID);
            head.setPlanName("预测-" + code);
            head.setPeriodFrom(p1Start);
            head.setPeriodTo(p2End != null ? p2End : p1End);
            head.setStatus(status);
            headDao.saveEntity(head);

            int lineNo = 10;
            if (q1 != null) {
                saveForecastLine(headId, lineNo, materialId, p1Start, p1End, q1);
                lineNo += 10;
            }
            if (q2 != null) {
                saveForecastLine(headId, lineNo, materialId, p2Start, p2End, q2);
            }
        });
        return headId;
    }

    private void saveForecastLine(Long headId, int lineNo, Long materialId,
                                  LocalDate pStart, LocalDate pEnd, BigDecimal qty) {
        IEntityDao<ErpMfgForecastLine> dao = daoProvider.daoFor(ErpMfgForecastLine.class);
        ErpMfgForecastLine line = new ErpMfgForecastLine();
        line.orm_propValueByName("id", headId * 1000 + lineNo);
        line.setForecastId(headId);
        line.setLineNo(lineNo);
        line.setMaterialId(materialId);
        line.setUoMId(UOM_ID);
        line.setPeriodStart(pStart);
        line.setPeriodEnd(pEnd);
        line.setForecastQty(qty);
        dao.saveEntity(line);
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
