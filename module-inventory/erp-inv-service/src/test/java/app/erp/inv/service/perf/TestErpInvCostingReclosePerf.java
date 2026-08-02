package app.erp.inv.service.perf;

import app.erp.common.test.PerfTiming;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.inv.biz.IErpInvCostingBiz;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.InvFrozenClockExtension;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 路径 3 性能基线测试：库存核算 reclose（成本层重算）（plan 2026-08-02-1121-2 Phase 4 / 设计文档 §4.3 + §5.2）。
 *
 * <p><b>被测链路</b>：{@link IErpInvCostingBiz#reclosePeriodCosts(Long, LocalDate, LocalDate, IServiceContext)}
 * （R6.9 已拆 {@code ErpInvCostingReclosePeriodCostsProcessor}），扫描本期 DONE FIFO 移动单，对成本层缺失的入库
 * 补建 {@link ErpInvCostLayer}、对 COGS 异常的出库重算。
 *
 * <p><b>复现性协议</b>（设计文档 §4 统一约定）：K=2 untimed warmup + N=10 timed 测量，
 * 方差比 = (max−min)/median，验收阈值 &lt; 20%。
 *
 * <p><b>每轮重置纪律</b>（设计文档 §4.3 + §5.2）：reclose 是幂等的——首次补建后再次调用为 no-op。为使每轮
 * 测到非零补算成本，每轮 reclose 后须删除其补建的 cost layer（计时窗口外），重置到「全缺失」状态。
 *
 * <p><b>方差稳定化（plan 2026-08-02-0650-2 Phase 2 / 设计文档 §3.4 successor）</b>：Phase 2 首基线方差比 117.2%
 * （超阈值），根因 = H2 in-memory 查询方差（每轮 500×~4 查询≈2000 查询/轮，在 heap 压力下查询时间方差大）。
 * 稳定化 = <b>per-round cost-layer reset（Phase 2 已落地，确认保留）+ heap-state stabilization（新增，计时窗口外
 * GC hint 降低跨轮 heap 压力方差）</b>。JMH fork 隔离否决——reclose 本质 DB-heavy（设计文档 §3.4 line 125，
 * §4.3「最可能 JMH 候选」假设经 Phase 2 实测反转）。残留 H2 查询方差超阈值时裁决 absolute-tolerance fallback。
 *
 * <p><b>计时窗口纪律</b>：seed 期间 / 科目 / 物料 / 移动单 / ledgers 全部在测量循环<b>之前</b>构造完成；
 * 每轮 cost-layer 重置也在计时窗口外；计时窗口仅包裹 reclose 调用。PerfTiming.measure 不支持 per-round
 * untimed setup，故用手动计时循环 + {@link PerfTiming#compute(long[])} 统计。
 *
 * <p><b>数据规模裁决</b>：plan §Phase 4 引用设计文档 §4.3 建议 N=5000/M=50，并明示「具体在 Phase 2 plan
 * 裁决」。首基线裁决 N=500/M=20——理由：generateMove 全链路（生成+确认+完成+过账）每单 ~50-100ms，
 * N=5000 单 seed 即 250-500s 超合理单测耗时，且 H2 in-memory 在万余实体后查询非线性退化；
 * N=500 仍能测到 reclose 扫描 + 补算的端到端成本，回归语义保持。生产规模 successor 见设计文档 §9。
 *
 * <p><b>路径 C 升级候选</b>：本路径偏计算（扫 FIFO 成本层 + 重算），是设计文档 §3 路径 C 升级（JMH）的
 * 最可能候选。首基线方差比 &gt; 20% 时登记 successor（plan Phase 4 Decision）。
 *
 * <p><b>@Tag("perf")</b> + inv-service pom {@code <excludedGroups>perf</excludedGroups>}：默认不进 per-commit
 * {@code mvn test}，经 {@code -Pperf} 激活。
 */
@Tag("perf")
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvCostingReclosePerf extends JunitAutoTestCase {

    @RegisterExtension
    static InvFrozenClockExtension frozenClock = new InvFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long ORG_ID = 7301L;
    static final Long WAREHOUSE_ID = 7302L;
    static final Long LOCATION_ID = 7303L;
    static final Long UOM_ID = 7304L;
    static final Long CURRENCY_ID = 7305L;
    static final Long ACCT_SCHEMA_ID = 7306L;
    static final String PERIOD_CODE = "2026-07";
    static final LocalDate PERIOD_START = LocalDate.of(2026, 7, 1);
    static final LocalDate PERIOD_END = LocalDate.of(2026, 7, 31);

    static final int WARMUP_K = 2;
    static final int TIMED_N = 10;
    static final double VARIANCE_THRESHOLD_PERCENT = 20.0;
    static final int MATERIAL_COUNT = 20;
    static final int MOVES_PER_MATERIAL = 25;
    static final int TOTAL_MOVES = MATERIAL_COUNT * MOVES_PER_MATERIAL;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testReclosePeriodCostsPerformanceBaseline() {
        seedMaterialsAndMoves();

        Long periodId = findPeriodId();
        assertTrue(periodId != null, "perf 测试应 seed 有效期间");

        long[] nanos = new long[TIMED_N];
        for (int round = -WARMUP_K; round < TIMED_N; round++) {
            resetCostLayers();
            System.gc();
            long start = CoreMetrics.nanoTime();
            ApiResponse<?> resp = reclose(periodId);
            long elapsed = CoreMetrics.nanoTimeDiff(start);
            assertEquals0(resp.getStatus(), "reclose 应成功 (round=" + round + ")");
            if (round >= 0) {
                nanos[round] = elapsed;
            }
        }

        PerfTiming.Measurement m = PerfTiming.compute(nanos);
        long costLayerCount = countCostLayers();

        System.out.println("[PERF] path=3 inv-reclose"
                + " dataScale=" + TOTAL_MOVES + "x" + MATERIAL_COUNT + "materials"
                + " warmupK=" + WARMUP_K
                + " timedN=" + TIMED_N
                + " medianMs=" + String.format("%.3f", m.medianMillis())
                + " p95Ms=" + String.format("%.3f", m.p95Millis())
                + " varianceRatioPercent=" + String.format("%.3f", m.varianceRatioPercent())
                + " withinThreshold(<" + VARIANCE_THRESHOLD_PERCENT + "%)=" + m.withinThreshold(VARIANCE_THRESHOLD_PERCENT)
                + " costLayersAfterLastRound=" + costLayerCount
                + " stabilization=per-round-costlayer-reset+gc-hint");

        assertTrue(costLayerCount > 0, "reclose 应至少补建 1 个 cost layer");
    }

    // ---------- seed ----------

    private void seedMaterialsAndMoves() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod();
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedSubject("6401", "主营业务成本");
        });
        for (long mid = 1; mid <= MATERIAL_COUNT; mid++) {
            final long materialId = mid;
            ormTemplate.runInSession(() -> seedFifoMaterial(materialId));
        }
        // 在 costing 关闭期间入库 → 不建 cost layer（reclose 的「补建缺失」语义触发）
        setCostingEnabled(false);
        try {
            for (long mid = 1; mid <= MATERIAL_COUNT; mid++) {
                final long materialId = mid;
                for (int i = 0; i < MOVES_PER_MATERIAL; i++) {
                    final int idx = i;
                    ormTemplate.runInSession(() -> generateIncoming(materialId,
                            "PR-PERF-M" + materialId + "-" + idx,
                            new BigDecimal("10"), new BigDecimal("8")));
                }
            }
        } finally {
            setCostingEnabled(true);
        }
    }

    private void seedFifoMaterial(Long id) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial material = new ErpMdMaterial();
        material.orm_propValueByName("id", id);
        material.setCode("MAT-PERF-" + id);
        material.setName("FIFO Material Perf " + id);
        material.orm_propValueByName("materialType", "GOODS");
        material.setUoMId(UOM_ID);
        material.setStatus("ACTIVE");
        material.setCostMethod(ErpInvConstants.COST_METHOD_FIFO);
        dao.saveEntity(material);
    }

    private void seedOpenPeriod() {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(PERIOD_CODE);
        period.setName(PERIOD_CODE);
        period.setOrgId(ORG_ID);
        period.orm_propValueByName("year", 2026);
        period.orm_propValueByName("month", 7);
        period.setStartDate(PERIOD_START);
        period.setEndDate(PERIOD_END);
        period.orm_propValueByName("status", "OPEN");
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", "ASSET");
        subject.orm_propValueByName("direction", "DEBIT");
        subject.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(subject);
    }

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-15");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertEquals0(resp.getStatus(), "generateMove 应成功: " + billCode);
    }

    private ApiResponse<?> reclose(Long periodId) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("periodId", periodId);
        args.put("startDate", PERIOD_START.toString());
        args.put("endDate", PERIOD_END.toString());
        return executeRpc(mutation, "ErpInvCosting__reclosePeriodCosts", ApiRequest.build(args));
    }

    private void resetCostLayers() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
            for (ErpInvCostLayer layer : dao.findAllByQuery(new QueryBean())) {
                dao.deleteEntity(layer);
            }
        });
    }

    private long countCostLayers() {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        return dao.findAllByQuery(new QueryBean()).size();
    }

    private Long findPeriodId() {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", PERIOD_CODE));
        List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private void setCostingEnabled(boolean value) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_COSTING_ENABLED, String.valueOf(value));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private static void assertEquals0(int status, String msg) {
        if (status != 0) {
            throw new AssertionError(msg + ": status=" + status);
        }
    }
}
