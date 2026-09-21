package app.erp.mfg.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.mfg.service.posting.MfgPostingExecutor;
import app.erp.mfg.service.posting.ProductionVarianceDispatcher;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 行为测试（plan 2026-07-18-2251-1）：生产差异重算孤儿凭证修复 —— `reverseIfExists` 红冲闭环。
 *
 * <p>覆盖 {@link ProductionVarianceDispatcher#reverseIfExists} 红冲闭环链路：
 * <ul>
 *   <li>(a) Call site A 单凭证反向：首次 calculateVariances 产 PRODUCTION_VARIANCE NORMAL 凭证 →
 *       不变成本重算 calculateVariances → reverseIfExists 红冲原凭证（isReversed=true + 红字凭证行同向取负）
 *       + 新凭证派发（dispatchIfApplicable 成功）+ 数据行全 posted=true。</li>
 *   <li>(b) 关键一致性断言：{@code ErpFinVoucherBillR} 反查 {@code {wo.code}-PV} PRODUCTION_VARIANCE
 *       仅 1 条 {@code isReversed=false} 凭证（确认无孤儿）+ 数据行与凭证行金额一致。</li>
 *   <li>(c) Call site B 完工自动重算反向：config 开 + 建工单 + reportCompletion（willFinish=true）
 *       → ErpMfgWorkOrderProcessor:229 三步链经 reverseIfExists 红冲既有凭证 + 重算 + 派发新凭证。</li>
 *   <li>(d) 红冲失败容错路径：手工注入抛非 SOURCE_NOT_FOUND 异常的 executor → reverseIfExists 仍 log warn
 *       不阻断 calculateVariances，孤儿凭证风险可观测。</li>
 *   <li>(e) 首次 calculateVariances 容错：首次调用 reverseIfExists 触发 ERR_REVERSE_SOURCE_NOT_FOUND
 *       （无原凭证）→ 吞异常 log warn + calculateVariances 正常完成 + 新凭证派发成功。</li>
 * </ul>
 *
 * <p>权威：{@code docs/design/manufacturing/variance-analysis.md §重算幂等实现注记}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgVarianceRecomputeReversal extends JunitAutoTestCase {

    @RegisterExtension
    static MfgFrozenClockExtension frozenClock = new MfgFrozenClockExtension();

    static final String ORG_ID = "1451";
    static final String UOM_ID = "5451";
    static final String CURRENCY_ID = "6451";
    static final String WC1 = "6251";
    static final String PERIOD_CODE = "2026-07-RC";
    static final String VOUCHER_STATUS_POSTED = "POSTED";
    static final String ACCT_SCHEMA_ID = "7451";

    static final String SUBJECT_MATERIAL_VARIANCE = "1410";
    static final String SUBJECT_WIP_MATERIAL = "1411";
    static final String SUBJECT_LABOR_VARIANCE = "1412";
    static final String SUBJECT_WIP_LABOR = "1413";
    static final String SUBJECT_OVERHEAD_VARIANCE = "1414";
    static final String SUBJECT_WIP_OVERHEAD = "1415";

    // 测试专用 ID 段（与 TestErpMfgProductionVariance 1410/8201 段隔离）
    static final String P = "1251";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ProductionVarianceCalculator productionVarianceCalculator;
    @Inject
    ProductionVarianceDispatcher productionVarianceDispatcher;

    /**
     * (a) Call site A 单凭证反向：不变成本重算后原凭证被红冲 + 红字凭证生成 + 新 NORMAL 凭证派发 + 数据行 posted=true。
     *
     * <p>红绿反转证明：本测试断言「ErpFinVoucherBillR 反查 {wo.code}-PV 仅 1 条 isReversed=false」——若移除
     * {@code reverseIfExists} 调用，第二次 calculateVariances 不红冲原凭证 → 原 NORMAL 凭证保持 isReversed=false
     * 且新数据行经 dispatchIfApplicable 派发时被平台 alreadyPosted 守护返回 null（同 billHeadCode+businessType 幂等）
     * → 数据行 posted=false 永久滞留 + 反查 isReversed=false 凭证数 = 1 但数据行新金额与凭证旧金额分叉。
     */
    @Test
    public void testRecomputeReversesOriginalVoucherAndRepostsNew() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        String bomId = seedBom("9251", P);
        seedBomOperation("4251", bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();

        seedCompletedWorkOrder("8251", "WO-RC-A", bomId, P,
                bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
        seedTimeLog("5651", "8251", bd("150"));

        // 第一次 calculateVariances：首次 reverseIfExists 抛 SOURCE_NOT_FOUND 被吞（覆盖 (e)）→ 计算差异 → 派发凭证
        ApiResponse<?> r1 = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", "8251")));
        assertEquals(0, r1.getStatus(), "第一次 calculateVariances 应成功: " + r1);

        ErpFinVoucher firstNormal = findVoucher("WO-RC-A-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "NORMAL");
        assertNotNull(firstNormal, "首次计算应派发 NORMAL PRODUCTION_VARIANCE 凭证");
        assertEquals(VOUCHER_STATUS_POSTED, firstNormal.getDocStatus());
        assertFalse(Boolean.TRUE.equals(firstNormal.getIsReversed()), "首次 NORMAL 凭证未被红冲");
        List<ErpMfgCostVariance> lines1 = productionVarianceCalculator.findByWorkOrder("8251");
        assertTrue(lines1.stream().allMatch(l -> Boolean.TRUE.equals(l.getPosted())),
                "首次计算后全部差异行 posted=true");

        // 第二次 calculateVariances（不变成本重算）：reverseIfExists 红冲原凭证 → deleteByWorkOrder → 重算 → dispatchIfApplicable 派发新凭证
        ApiResponse<?> r2 = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", "8251")));
        assertEquals(0, r2.getStatus(), "第二次 calculateVariances（重算）应成功: " + r2);

        // 原NORMAL 凭证已被红冲
        ErpFinVoucher firstAfter = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(firstNormal.getId());
        assertTrue(Boolean.TRUE.equals(firstAfter.getIsReversed()),
                "重算后原 NORMAL 凭证应被标记 isReversed=true");

        // 新 REVERSAL 红字凭证存在 + reversalOfVoucherId 指向原 NORMAL 凭证
        ErpFinVoucher reversal = findVoucher("WO-RC-A-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "REVERSAL");
        assertNotNull(reversal, "重算后应生成 REVERSAL 红字凭证");
        assertEquals(firstNormal.getId(), reversal.getReversalOfVoucherId(),
                "REVERSAL 凭证 reversalOfVoucherId 应指向原 NORMAL 凭证");

        // 新 NORMAL 凭证存在（重算后 dispatchIfApplicable 重新派发成功）+ 未红冲
        List<ErpFinVoucher> normalVouchers = findVouchers("WO-RC-A-PV",
                ErpFinBusinessType.PRODUCTION_VARIANCE, "NORMAL");
        long activeCount = normalVouchers.stream()
                .filter(v -> !Boolean.TRUE.equals(v.getIsReversed())).count();
        assertEquals(1, activeCount,
                "重算后应仅 1 条 isReversed=false 的 NORMAL 凭证（新派发的当前凭证）");

        // 数据行全 posted=true（重算后 dispatchIfApplicable 成功 markPosted）
        List<ErpMfgCostVariance> lines2 = productionVarianceCalculator.findByWorkOrder("8251");
        assertTrue(lines2.stream().allMatch(l -> Boolean.TRUE.equals(l.getPosted())),
                "重算后全部差异行 posted=true（dispatchIfApplicable 重新派发成功）");

        // (b) 关键一致性断言：ErpFinVoucherBillR 反查 {wo.code}-PV PRODUCTION_VARIANCE
        // 仅 1 条 isReversed=false 凭证（确认无孤儿）+ 数据行与凭证行金额一致
        assertNoOrphanVoucher("WO-RC-A-PV");
        assertVoucherLineMatchesVariance("WO-RC-A-PV", ErpFinBusinessType.PRODUCTION_VARIANCE,
                SUBJECT_MATERIAL_VARIANCE, lines2);
    }

    /**
     * (c) Call site B 完工自动重算反向：config 开 + 建工单 + reportCompletion（willFinish=true）
     * → ErpMfgWorkOrderProcessor:229 三步链经 reverseIfExists 红冲既有凭证 + 重算 + 派发新凭证。
     *
     * <p>触发条件：相同工单再次完工达量（重新打开 + 再次 reportCompletion）。验证 config-gated 完工自动重算路径
     * 同样经 reverseIfExists 闭合红冲链路（与 Call site A 同型，规则 14 bundling）。
     */
    @Test
    public void testCompletionAutoRecomputeReversesViaCallSiteB() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        String bomId = seedBom("9252", P);
        seedBomOperation("4252", bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();

        // 建工单（IN_PROCESS，配 OUTPUT 行）
        String woId = seedInProcessWorkOrderWithOutputLine("8252", "WO-RC-B", bomId, P,
                bd("2"), bd("25"), bd("35"), bd("8"));

        setVarianceAutoCalc(true);
        try {
            // 第一次 reportCompletion（willFinish=true）→ Call site B 触发首次差异计算/过账
            Map<String, Object> req1 = new LinkedHashMap<>();
            req1.put("workOrderId", woId);
            req1.put("completedQty", bd("2"));
            ApiResponse<?> r1 = executeRpc(mutation, "ErpMfgWorkOrder__reportCompletion", ApiRequest.build(req1));
            assertEquals(0, r1.getStatus(), "第一次 reportCompletion 应成功: " + r1);

            ErpFinVoucher firstNormal = findVoucher("WO-RC-B-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "NORMAL");
            assertNotNull(firstNormal, "首次完工应派发 NORMAL PRODUCTION_VARIANCE 凭证");

            // 把工单状态改回 IN_PROCESS 模拟「错误完工」回退（同 mnt-visit-cancel-reversal 范式）
            ormTemplate.runInSession(() -> {
                IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
                ErpMfgWorkOrder wo = dao.getEntityById(woId);
                wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
                wo.setCompletedQuantity(bd("0"));
                dao.updateEntity(wo);
            });

            // 第二次 reportCompletion（willFinish=true 再次触发）→ Call site B 经 reverseIfExists 红冲既有凭证
            Map<String, Object> req2 = new LinkedHashMap<>();
            req2.put("workOrderId", woId);
            req2.put("completedQty", bd("2"));
            ApiResponse<?> r2 = executeRpc(mutation, "ErpMfgWorkOrder__reportCompletion", ApiRequest.build(req2));
            assertEquals(0, r2.getStatus(), "第二次 reportCompletion（重算）应成功: " + r2);

            // 原 NORMAL 凭证已被红冲
            ErpFinVoucher firstAfter = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(firstNormal.getId());
            assertTrue(Boolean.TRUE.equals(firstAfter.getIsReversed()),
                    "Call site B 重算后原 NORMAL 凭证应被标记 isReversed=true");

            // REVERSAL 红字凭证存在
            ErpFinVoucher reversal = findVoucher("WO-RC-B-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "REVERSAL");
            assertNotNull(reversal, "Call site B 重算后应生成 REVERSAL 红字凭证");

            // (b) 同 (a) 关键一致性断言
            assertNoOrphanVoucher("WO-RC-B-PV");
        } finally {
            setVarianceAutoCalc(false);
        }
    }

    /**
     * (d) 红冲失败容错路径：手工注入抛非 SOURCE_NOT_FOUND 异常的 executor → reverseIfExists 不抛出、
     * 返回 {@code false}（P2-CK-mfg3-009 C2 裁决：真实红冲失败须中止派发段）。
     *
     * <p>验证 plan 2026-07-18-2251-1 Phase 1 Decision (a) 异常 mask 可观测性 + plan 2026-09-17-0800-1
     * 返回值分级契约（良性 SOURCE_NOT_FOUND → true；真实失败 → false）。链级中止
     * （reversalOk=false 跳过 dispatchIfApplicable）经 (f) 端到端覆盖。
     */
    @Test
    public void testReverseFailureDoesNotBlockRecompute() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        String bomId = seedBom("9253", P);
        seedBomOperation("4253", bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();
        seedCompletedWorkOrder("8253", "WO-RC-D", bomId, P,
                bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
        seedTimeLog("5653", "8253", bd("150"));

        // 经计算器直接产生差异行（不入账 posted——避免派发面写入），使新门控「差异行存在」放行触达 executor；
        //（无差异行时 reverseIfExists 直接放行返回 true，不触 executor）
        ormTemplate.runInSession(session -> {
            List<ErpMfgCostVariance> seeded = productionVarianceCalculator.calculateVariances("8253");
            assertFalse(seeded.isEmpty(), "差异行已计算（门控前置）");
            return null;
        });

        // 直接调 dispatcher.reverseIfExists，使用一个抛 RuntimeException 的执行器：
        // 差异行存在 → 门控放行触达 executor → reverse 抛 RuntimeException → 不抛出、返回 false
        //（C2 裁决：真实红冲失败须中止派发段；链级中止行为另经 testReversalFailureAbortsDispatchKeepsLinesUnposted 覆盖）
        ProductionVarianceDispatcher failingDispatcher = new ProductionVarianceDispatcher();
        failingDispatcher.setDaoProvider(daoProvider);
        failingDispatcher.setVarianceCalculator(productionVarianceCalculator);
        failingDispatcher.setExecutor(new ThrowingMfgPostingExecutor());
        assertFalse(failingDispatcher.reverseIfExists("8253"),
                "非 SOURCE_NOT_FOUND 红冲失败应返回 false（C2：中止派发段）");
        // 注：不阻断数据行重算的语义经链级测试覆盖；此处聚焦 dispatcher 返回值契约本身
    }

    /**
     * (e) 首次 calculateVariances 容错：首次调用 reverseIfExists 触发 ERR_REVERSE_SOURCE_NOT_FOUND
     * （无原凭证）→ 吞异常 log warn + calculateVariances 正常完成 + 新凭证派发成功。
     *
     * <p>验证 Phase 1 Decision (a) 异常 mask 范式对「首次计算无原凭证」场景的友好降级。
     */
    @Test
    public void testFirstCalculateVariancesToleratesNoSourceVoucher() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        String bomId = seedBom("9254", P);
        seedBomOperation("4254", bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();
        seedCompletedWorkOrder("8254", "WO-RC-E", bomId, P,
                bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
        seedTimeLog("5654", "8254", bd("150"));

        // 首次调用：reverseIfExists 触发 ERR_REVERSE_SOURCE_NOT_FOUND → 吞异常 → 计算差异 → 派发凭证
        ApiResponse<?> resp = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", "8254")));
        assertEquals(0, resp.getStatus(),
                "首次 calculateVariances 应成功（reverseIfExists 吞 SOURCE_NOT_FOUND 异常）: " + resp);

        ErpFinVoucher normal = findVoucher("WO-RC-E-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "NORMAL");
        assertNotNull(normal, "首次计算应派发 NORMAL PRODUCTION_VARIANCE 凭证");
        assertFalse(Boolean.TRUE.equals(normal.getIsReversed()), "首次 NORMAL 凭证未被红冲");

        // REVERSAL 凭证不应存在（首次无原凭证可红冲，reverseIfExists 抛 SOURCE_NOT_FOUND 被吞）
        ErpFinVoucher reversal = findVoucher("WO-RC-E-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "REVERSAL");
        assertEquals(null, reversal, "首次计算时无 REVERSAL 凭证（无原凭证可红冲）");

        // 数据行全 posted=true
        List<ErpMfgCostVariance> lines = productionVarianceCalculator.findByWorkOrder("8254");
        assertTrue(lines.stream().allMatch(l -> Boolean.TRUE.equals(l.getPosted())),
                "首次计算后全部差异行 posted=true");
    }

    /**
     * (f) P2-CK-mfg3-009 C2 裁决（plan 2026-09-17-0800-1）：红冲真实失败（期间锁定）→ 重算链派发段中止，
     * 新差异行保持 posted=false，不因 fin 侧 post 幂等命中旧凭证而误标 posted=true。
     *
     * <p>红绿反转证明：修复前 reverseIfExists 吞一切异常继续派发 → fin 侧 post 幂等命中（billHeadCode 相同、
     * 旧凭证未冲销、期间校验位于幂等短路之后不阻断命中）返回旧凭证 ID → markPosted 把新金额差异行误标
     * posted=true（GL=旧金额、差异行=新金额数据分叉）。修复后红冲真实失败返回 false → 派发段中止 →
     * 差异行诚实保持 posted=false，待期间重开后下次重算自动重试红冲。
     */
    @Test
    public void testReversalFailureAbortsDispatchKeepsLinesUnposted() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        String bomId = seedBom("9255", P);
        seedBomOperation("4255", bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();
        seedCompletedWorkOrder("8255", "WO-RC-F", bomId, P,
                bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
        seedTimeLog("5655", "8255", bd("150"));

        // 第一次重算：期间 OPEN → 正常派发 V1
        ApiResponse<?> r1 = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", "8255")));
        assertEquals(0, r1.getStatus(), "第一次 calculateVariances 应成功: " + r1);
        ErpFinVoucher v1 = findVoucher("WO-RC-F-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "NORMAL");
        assertNotNull(v1, "首次计算应派发 NORMAL 凭证 V1");

        // 锁定期间（真实红冲失败场景：reverseProcess 的 resolveOpenPeriod 抛 ERR_PERIOD_CLOSED）
        setPeriodStatus("CLOSED");
        try {
            ApiResponse<?> r2 = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                    ApiRequest.build(Map.of("workOrderId", "8255")));
            assertEquals(0, r2.getStatus(), "红冲失败容错下重算 RPC 仍应成功: " + r2);

            // 旧凭证未被红冲（红冲失败被 dispatcher 容错吞掉）
            ErpFinVoucher v1After = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(v1.getId());
            assertFalse(Boolean.TRUE.equals(v1After.getIsReversed()),
                    "期间锁定下红冲失败，V1 保持 isReversed=false");
            assertEquals(null, findVoucher("WO-RC-F-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "REVERSAL"),
                    "红冲失败不应生成 REVERSAL 凭证");

            // 核心断言（C2）：派发段中止，新差异行保持 posted=false（修复前经幂等命中误标 posted=true）
            List<ErpMfgCostVariance> lines = productionVarianceCalculator.findByWorkOrder("8255");
            assertFalse(lines.isEmpty(), "重算应重建差异行");
            assertTrue(lines.stream().noneMatch(l -> Boolean.TRUE.equals(l.getPosted())),
                    "红冲失败时新差异行必须保持 posted=false（C2：不得经幂等命中旧凭证回写）");
        } finally {
            setPeriodStatus("OPEN");
        }
    }

    /**
     * (g) P2-CK-mfg3-009 门控修复红绿反转：差异行全部 posted=false 时下次重算仍重试红冲（悬挂解除）。
     *
     * <p>复现「红冲失败后重算」悬挂前态：V1 未冲销 + 差异行均 posted=false。修复前门控按
     * 「posted=true 行存在」判定 → 跳过红冲 → dispatch 幂等命中 V1 → 永不红冲；修复后门控按
     * 「差异行存在」判定 → 重试红冲成功 → V1 isReversed=true + REVERSAL 凭证 + 新 NORMAL 凭证 + 行 posted=true。
     */
    @Test
    public void testReversalRetriedWhenLinesUnpostedAfterFailure() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        String bomId = seedBom("9256", P);
        seedBomOperation("4256", bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();
        seedCompletedWorkOrder("8256", "WO-RC-G", bomId, P,
                bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
        seedTimeLog("5656", "8256", bd("150"));

        // 第一次重算：V1 + 行 posted=true
        ApiResponse<?> r1 = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", "8256")));
        assertEquals(0, r1.getStatus(), "第一次 calculateVariances 应成功: " + r1);
        ErpFinVoucher v1 = findVoucher("WO-RC-G-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "NORMAL");
        assertNotNull(v1, "首次计算应派发 NORMAL 凭证 V1");

        // 构造悬挂前态：差异行全部置回 posted=false（等价于「红冲失败 + 重算后行未回写」状态）
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgCostVariance> dao = daoProvider.daoFor(ErpMfgCostVariance.class);
            for (ErpMfgCostVariance l : productionVarianceCalculator.findByWorkOrder("8256")) {
                l.setPosted(false);
                dao.updateEntity(l);
            }
        });

        // 期间 OPEN：下次重算必须重试红冲（门控修复点）→ 红冲成功 → 新凭证派发
        ApiResponse<?> r2 = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", "8256")));
        assertEquals(0, r2.getStatus(), "第二次 calculateVariances 应成功: " + r2);

        // 红绿判据：V1 被红冲（修复前门控跳过红冲，V1 永远 isReversed=false）
        ErpFinVoucher v1After = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(v1.getId());
        assertTrue(Boolean.TRUE.equals(v1After.getIsReversed()),
                "行全 posted=false 时重算仍须重试红冲（门控修复），V1 应被红冲");
        assertNotNull(findVoucher("WO-RC-G-PV", ErpFinBusinessType.PRODUCTION_VARIANCE, "REVERSAL"),
                "红冲重试成功应生成 REVERSAL 凭证");

        // 无孤儿 + 行全 posted=true（完全收敛）
        assertNoOrphanVoucher("WO-RC-G-PV");
        List<ErpMfgCostVariance> lines = productionVarianceCalculator.findByWorkOrder("8256");
        assertTrue(lines.stream().allMatch(l -> Boolean.TRUE.equals(l.getPosted())),
                "重算收敛后全部差异行 posted=true");
    }

    // ---------- 查询 helper ----------

    private ErpFinVoucher findVoucher(String billHeadCode, ErpFinBusinessType businessType, String postingType) {
        List<ErpFinVoucher> list = findVouchers(billHeadCode, businessType, postingType);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucher> findVouchers(String billHeadCode, ErpFinBusinessType businessType, String postingType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", businessType.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        return links.stream()
                .map(lnk -> daoProvider.daoFor(ErpFinVoucher.class).getEntityById(lnk.getVoucherId()))
                .filter(v -> v != null && (postingType == null || postingType.equals(v.getPostingType())))
                .collect(Collectors.toList());
    }

    private void assertNoOrphanVoucher(String billHeadCode) {
        List<ErpFinVoucher> all = findVouchers(billHeadCode, ErpFinBusinessType.PRODUCTION_VARIANCE, null);
        long activeNormal = all.stream()
                .filter(v -> "NORMAL".equals(v.getPostingType()) && !Boolean.TRUE.equals(v.getIsReversed()))
                .count();
        assertEquals(1, activeNormal,
                "ErpFinVoucherBillR 反查 " + billHeadCode + " 应仅 1 条 isReversed=false NORMAL 凭证（无孤儿）");
    }

    private void assertVoucherLineMatchesVariance(String billHeadCode, ErpFinBusinessType businessType,
                                                  String subjectCode, List<ErpMfgCostVariance> lines) {
        ErpFinVoucher activeNormal = findVouchers(billHeadCode, businessType, "NORMAL").stream()
                .filter(v -> !Boolean.TRUE.equals(v.getIsReversed()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("应存在 1 条 isReversed=false NORMAL 凭证"));
        ErpFinVoucherLine voucherLine = findVoucherLine(activeNormal.getId(), subjectCode);
        assertNotNull(voucherLine, "凭证行存在 subjectCode=" + subjectCode);

        BigDecimal expectedMaterialVariance = lines.stream()
                .filter(l -> ErpMfgConstants.COST_ELEMENT_MATERIAL.equals(l.getCostElement()))
                .map(l -> nullToZero(l.getVarianceAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
        BigDecimal voucherAmount = voucherLine.getDebitAmount() != null
                && voucherLine.getDebitAmount().signum() != 0
                ? voucherLine.getDebitAmount()
                : nullToZero(voucherLine.getCreditAmount());
        assertEquals(0, expectedMaterialVariance.compareTo(voucherAmount.abs()),
                "凭证行 " + subjectCode + " 金额 = 数据行 MATERIAL 净差异绝对值（一致不变量）");
    }

    private ErpFinVoucherLine findVoucherLine(String voucherId, String subjectCode) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        List<ErpFinVoucherLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void setVarianceAutoCalc(boolean value) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_VARIANCE_AUTO_CALC_ENABLED, String.valueOf(value));
    }

    private void setPeriodStatus(String status) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("code", PERIOD_CODE));
            List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
            assertFalse(list.isEmpty(), "期间 " + PERIOD_CODE + " 应已种子化");
            ErpFinAccountingPeriod period = list.get(0);
            period.orm_propValueByName("status", status);
            dao.updateEntity(period);
        });
    }

    // ---------- seed helper（与 TestErpMfgProductionVariance 范式对齐，独立 ID 段避免冲突） ----------

    private void seedProduct(String id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Product " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            dao.saveEntity(m);
        });
    }

    private void seedWorkcenter(String id, BigDecimal hourlyRate) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenter> dao = daoProvider.daoFor(ErpMfgWorkcenter.class);
            ErpMfgWorkcenter wc = new ErpMfgWorkcenter();
            wc.orm_propValueByName("id", id);
            wc.setCode("WC-" + id);
            wc.setName("Workcenter " + id);
            wc.setHourlyRate(hourlyRate);
            dao.saveEntity(wc);
        });
    }

    private String seedBom(String id, String productId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", id);
            bom.setCode("BOM-" + id);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(bd("1"));
            dao.saveEntity(bom);
        });
        return id;
    }

    private void seedBomOperation(String id, String bomId, String workcenterId, BigDecimal standardTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBomOperation> dao = daoProvider.daoFor(ErpMfgBomOperation.class);
            ErpMfgBomOperation op = new ErpMfgBomOperation();
            op.orm_propValueByName("id", id);
            op.setBomId(bomId);
            op.setLineNo(10);
            op.setOperationId("9000");
            op.setWorkcenterId(workcenterId);
            op.setStandardTime(standardTime);
            dao.saveEntity(op);
        });
    }

    private void seedFirmedRollup(String productId, BigDecimal materialCost, BigDecimal laborCost,
                                  BigDecimal overheadCost, BigDecimal unitCost) {
        ormTemplate.runInSession(() -> {
            String headerId = String.valueOf(Long.parseLong(productId) * 10000 + 51);
            IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
            ErpMfgCostRollup header = new ErpMfgCostRollup();
            header.orm_propValueByName("id", headerId);
            header.setCode("ROLLUP-RC-" + productId);
            header.setOrgId(ORG_ID);
            header.setBusinessDate(LocalDate.of(2026, 6, 1));
            header.orm_propValueByName("status", ErpMfgConstants.COST_ROLLUP_STATUS_FIRMED);
            headerDao.saveEntity(header);

            IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
            ErpMfgCostRollupLine line = new ErpMfgCostRollupLine();
            line.orm_propValueByName("id", String.valueOf(Long.parseLong(productId) * 10000 + 52));
            line.setCostRollupId(headerId);
            line.setLineNo(10);
            line.setMaterialId(productId);
            line.setUoMId(UOM_ID);
            line.setMaterialCost(materialCost);
            line.setLaborCost(laborCost);
            line.setOverheadCost(overheadCost);
            line.setUnitCost(unitCost);
            line.setTotalCost(unitCost);
            line.setCurrencyId(CURRENCY_ID);
            lineDao.saveEntity(line);
        });
    }

    private void seedCompletedWorkOrder(String id, String code, String bomId, String productId,
                                        BigDecimal planned, BigDecimal completed,
                                        BigDecimal materialCost, BigDecimal laborCost,
                                        BigDecimal overheadCost) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(productId);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(planned);
            wo.setCompletedQuantity(completed);
            wo.setMaterialCost(materialCost);
            wo.setLaborCost(laborCost);
            wo.setOverheadCost(overheadCost);
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
            dao.saveEntity(wo);
        });
    }

    private String seedInProcessWorkOrderWithOutputLine(String id, String code, String bomId, String productId,
                                                        BigDecimal planned, BigDecimal materialCost,
                                                        BigDecimal laborCost, BigDecimal overheadCost) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(productId);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(planned);
            wo.setCompletedQuantity(BigDecimal.ZERO);
            wo.setMaterialCost(materialCost);
            wo.setLaborCost(laborCost);
            wo.setOverheadCost(overheadCost);
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
            dao.saveEntity(wo);

            // OUTPUT 行：成品产出（generateCompletionMove 必读 destWarehouseId 才能生成入库移动）
            // 经查 generateCompletionMove 在 destWarehouseId/productId/uomId 均可解析时生成移动；
            // 本测试 focus on 差异重算红冲链路，不需要实际库存通道，但完工路径会调 generateCompletionMove。
            // destWarehouseId/uomId 缺失时 generateCompletionMove 静默 return（不阻断完工主流程）。
            ErpMfgWorkOrderLine out = new ErpMfgWorkOrderLine();
            out.orm_propValueByName("id", String.valueOf(Long.parseLong(id) * 10 + 1));
            out.setWorkOrderId(id);
            out.setLineNo(1);
            out.orm_propValueByName("lineType", ErpMfgConstants.WORK_ORDER_LINE_TYPE_OUTPUT);
            out.setMaterialId(productId);
            out.setUoMId(UOM_ID);
            out.setPlannedQuantity(planned);
            daoProvider.daoFor(ErpMfgWorkOrderLine.class).saveEntity(out);
        });
        return id;
    }

    private void seedTimeLog(String id, String workOrderId, BigDecimal durationMins) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgJobCardTimeLog> dao = daoProvider.daoFor(ErpMfgJobCardTimeLog.class);
            ErpMfgJobCardTimeLog log = new ErpMfgJobCardTimeLog();
            log.orm_propValueByName("id", id);
            log.setJobCardId("9001");
            log.setWorkOrderId(workOrderId);
            log.setOperatorId("OP-001");
            log.setWorkDate(LocalDate.of(2026, 7, 1));
            log.setDurationMins(durationMins);
            dao.saveEntity(log);
        });
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdAcctSchema> asDao = daoProvider.daoFor(ErpMdAcctSchema.class);
            ErpMdAcctSchema acctSchema = new ErpMdAcctSchema();
            acctSchema.orm_propValueByName("id", ACCT_SCHEMA_ID);
            acctSchema.setCode("ACCT-" + ORG_ID);
            acctSchema.setName("账套 " + ORG_ID);
            acctSchema.setOrgId(ORG_ID);
            acctSchema.orm_propValueByName("nature", "FINANCIAL");
            acctSchema.setFunctionalCurrencyId(CURRENCY_ID);
            acctSchema.orm_propValueByName("status", "ACTIVE");
            asDao.saveEntity(acctSchema);

            IEntityDao<ErpFinAccountingPeriod> pdao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
            period.setCode(PERIOD_CODE);
            period.setName(PERIOD_CODE);
            period.setOrgId(ORG_ID);
            period.orm_propValueByName("year", 2026);
            period.orm_propValueByName("month", 7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.orm_propValueByName("status", "OPEN");
            pdao.saveEntity(period);

            seedSubject(SUBJECT_MATERIAL_VARIANCE, "制造差异-材料", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP_MATERIAL, "在制品-材料", "ASSET", "DEBIT");
            seedSubject(SUBJECT_LABOR_VARIANCE, "制造差异-人工", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP_LABOR, "在制品-人工", "ASSET", "DEBIT");
            seedSubject(SUBJECT_OVERHEAD_VARIANCE, "制造差异-制造费用", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP_OVERHEAD, "在制品-制造费用", "ASSET", "DEBIT");
        });
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", subjectClass);
        subject.orm_propValueByName("direction", direction);
        subject.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(subject);
    }

    // ---------- misc helpers ----------

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                                      ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** 抛 RuntimeException 的 MfgPostingExecutor，用于 (d) 红冲失败容错路径验证。 */
    private static final class ThrowingMfgPostingExecutor extends MfgPostingExecutor {
        @Override
        public void reverse(String billHeadCode, ErpFinBusinessType businessType) {
            throw new RuntimeException("simulated reverse failure (not SOURCE_NOT_FOUND)");
        }
    }
}
