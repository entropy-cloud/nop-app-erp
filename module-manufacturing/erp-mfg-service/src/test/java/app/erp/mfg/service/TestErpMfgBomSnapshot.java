package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvReservation;
import app.erp.inv.dao.entity.ErpInvReservationLine;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomLineSnapshot;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomOperationSnapshot;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomSnapshot;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.mfg.service.bom.BomExpander;
import app.erp.mfg.service.workorder.KitAvailabilityChecker;
import app.erp.mfg.service.workorder.KitAvailabilityResult;
import app.erp.md.dao.entity.ErpMdMaterial;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.49 mfg BOM 快照（P1-RC-009，UC-MFG-10 断言④⑤⑥）集成测试。
 *
 * <p>覆盖（plan 2026-08-16-0904-1 Phase 5 七组）：
 * ① submit 后快照落库（头/行/工艺行内容 = 提交时点 + snapshotBomVersion）；
 * ② BOM 行编辑后已审核工单齐套展开不变（断言⑤物料需求面）；
 * ③ 工序标准成本编辑后已审核工单 variance 人工/工序维度不变（断言⑤成本面）；
 * ④ 新建工单（提交时点）用当前 BOM 内容（断言⑥）；
 * ⑤ AUTO_UPGRADE 配置下 re-resolve 最新 BOM（实时展开、不写回快照）；
 * ⑥ 幂等（快照已存在跳过，重复提交不重复快照）+ 无 BOM 跳过不阻断；
 * ⑦ R1.48 预留生命周期回归（approve→reservation 经快照化展开正常）+ GraphQL 冒烟（快照实体经 GraphQL 可达）。
 *
 * <p>权威：{@code docs/design/manufacturing/bom-and-routing.md §BOM 版本快照规则}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgBomSnapshot extends JunitAutoTestCase {

    static final Long ORG_ID = 1401L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long WC1 = 6201L;
    static final Long P = 1151L;     // 产成品
    static final Long P2 = 1152L;    // 无 BOM 产成品
    static final Long M1 = 1153L;    // 子件
    static final String MOVE_TYPE_INCOMING = "INCOMING";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    KitAvailabilityChecker kitAvailabilityChecker;
    @Inject
    ProductionVarianceCalculator productionVarianceCalculator;
    @Inject
    BomExpander bomExpander;

    // ---------- ① submit 后快照落库（断言④：审核时快照 BOM 内容 + snapshotBomVersion） ----------

    @Test
    public void testSnapshotCapturedOnSubmit() {
        seedProduct(P, "MOVING_AVERAGE");
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9151L, P, "V1", bd("1"));
        seedBomLine(6151L, bomId, M1, bd("2"), 10);
        seedBomOperation(7151L, bomId, WC1, bd("60"));
        Long woId = seedWorkOrder("WO-SNAP-CAP", bomId, P, bd("2"));

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));

        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals("V1", wo.getSnapshotBomVersion(), "snapshotBomVersion = 提交时点 BOM 版本号（版本追溯 :163）");
        assertNotNull(wo.getSnapshotBomId(), "snapshotBomId 回写（快照实体归属锚）");

        ErpMfgWorkOrderBomSnapshot snap = findSnapshot(woId);
        assertNotNull(snap, "提交后快照头落库");
        assertEquals(woId, snap.getWorkOrderId());
        assertEquals(bomId, snap.getBomId(), "快照头 bomId = 来源 BOM");
        assertEquals(P, snap.getProductId());
        assertEquals("V1", snap.getVersionLabel(), "快照头版本号 = 提交时点 BOM 版本");
        assertEquals(0, bd("1").compareTo(snap.getQty()), "快照头产出量 = 提交时点 BOM.qty");

        List<ErpMfgWorkOrderBomLineSnapshot> lines = findSnapshotLines(snap.getId());
        assertEquals(1, lines.size(), "快照子件行 = BOM 行数");
        ErpMfgWorkOrderBomLineSnapshot sl = lines.get(0);
        assertEquals(M1, sl.getMaterialId());
        assertEquals(0, bd("2").compareTo(sl.getQuantity()), "快照行数量 = 提交时点 BOM 行数量");
        assertEquals(10, sl.getLineNo());
        assertEquals(UOM_ID, sl.getUoMId());

        List<ErpMfgWorkOrderBomOperationSnapshot> ops = findSnapshotOperations(snap.getId());
        assertEquals(1, ops.size(), "快照工艺行 = BOM 工艺行数");
        ErpMfgWorkOrderBomOperationSnapshot so = ops.get(0);
        assertEquals(WC1, so.getWorkcenterId());
        assertEquals(0, bd("60").compareTo(so.getStandardTime()), "快照标准工时 = 提交时点值");
    }

    // ---------- ② BOM 行编辑后已审核工单齐套展开不变（断言⑤物料需求面） ----------

    @Test
    public void testBomLineEditAfterSubmitKitUnchanged() {
        seedProduct(P, "MOVING_AVERAGE");
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9152L, P, "V1", bd("1"));
        seedBomLine(6152L, bomId, M1, bd("2"), 10);
        Long woId = seedWorkOrder("WO-SNAP-KIT", bomId, P, bd("2"));
        // 可用 7：快照需求 2×2=4 ≤ 7 → 全齐；若走实时 BOM（编辑后 5×2=10）→ 部分齐套
        generateIncoming(M1, "PR-SNAP-KIT", bd("7"), bd("5"));
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);
        // 预留关闭：避免 approve 预留占用污染可用量口径（预留+快照交互由 ⑦ 专测）
        setReservationEnabled(false);
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
            rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));

            // BOM 行编辑（提交后 2 → 5）
            updateBomLineQuantity(6152L, bd("5"));

            KitAvailabilityResult result = ormTemplate.runInSession(
                    s -> kitAvailabilityChecker.check(woId));
            assertTrue(result.isFullyAvailable(), "已审核工单齐套读快照：需求 4 ≤ 可用 7 仍全齐（断言⑤）");
            // 需求侧直接断言：快照展开需求 = 2×2 = 4（非实时 5×2 = 10）
            List<app.erp.mfg.biz.BomExplosionNode> snapNodes = ormTemplate.runInSession(s -> {
                ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
                return kitAvailabilityChecker.explodeRequirements(wo, bd("2"));
            });
            assertEquals(1, snapNodes.size());
            assertEquals(0, bd("4").compareTo(snapNodes.get(0).getQuantity()),
                    "已审核工单需求侧 = 快照 2×2=4（断言⑤物料需求面）");
            // 对照：实时 BOM 展开需求 = 5×2 = 10 > 可用 7
            List<app.erp.mfg.biz.BomExplosionNode> live = bomExpanderExplode(bomId, bd("2"));
            assertEquals(0, bd("10").compareTo(live.get(0).getQuantity()), "实时 BOM 已变：5×2=10");
        } finally {
            setReservationEnabled(true);
        }
    }

    // ---------- ③ 工序标准成本编辑后已审核工单 variance 不变（断言⑤成本面） ----------

    @Test
    public void testBomOperationEditAfterSubmitVarianceUnchanged() {
        seedProduct(P, "MOVING_AVERAGE");
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9153L, P, "V1", bd("1"));
        seedBomLine(6153L, bomId, M1, bd("2"), 10);
        seedBomOperation(7153L, bomId, WC1, bd("60"));
        seedFirmedRollup(P);
        Long woId = seedWorkOrder("WO-SNAP-VAR", bomId, P, bd("2"));
        setReservationEnabled(false);
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
            rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));

            // 工序标准成本编辑（提交后 60 → 600 分钟）
            updateBomOperationStandardTime(7153L, bd("600"));

            // 完工置位（手动 calculateVariances 入口语义）
            ormTemplate.runInSession(() -> {
                ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
                wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
                wo.setCompletedQuantity(bd("2"));
                wo.setMaterialCost(bd("20"));
                wo.setLaborCost(bd("35"));
                wo.setOverheadCost(bd("8"));
                daoProvider.daoFor(ErpMfgWorkOrder.class).updateEntity(wo);
            });
            seedTimeLog(5653L, woId, bd("150"));

            ormTemplate.runInSession(s -> {
                productionVarianceCalculator.calculateVariances(woId);
                return null;
            });
            List<ErpMfgCostVariance> lines = productionVarianceCalculator.findByWorkOrder(woId);
            assertEquals(5, lines.size(), "5 类差异行");
            ErpMfgCostVariance eff = lineByType(lines, ErpMfgConstants.VARIANCE_TYPE_LABOR_EFFICIENCY);
            assertEquals(0, bd("120").compareTo(eff.getStandardQty()),
                    "标准工时 = 快照 60×2=120（非实时 600×2=1200）——断言⑤成本面");
            assertEquals(0, bd("20").compareTo(eff.getStandardAmount()),
                    "标准人工 = rollup.laborCost(10)×2=20（FIRMED 冻结，不受 BOM 编辑影响）");
        } finally {
            setReservationEnabled(true);
        }
    }

    // ---------- ④ 新建工单（提交时点）用当前 BOM 内容（断言⑥） ----------

    @Test
    public void testNewWorkOrderUsesCurrentBomOnSubmit() {
        seedProduct(P, "MOVING_AVERAGE");
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9154L, P, "V1", bd("1"));
        seedBomLine(6154L, bomId, M1, bd("2"), 10);
        Long wo1 = seedWorkOrder("WO-SNAP-NEW1", bomId, P, bd("1"));
        setReservationEnabled(false);
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(wo1)));
            ErpMfgWorkOrderBomSnapshot snap1 = findSnapshot(wo1);
            assertEquals(0, bd("2").compareTo(findSnapshotLines(snap1.getId()).get(0).getQuantity()),
                    "WO1 快照 = 提交时点 BOM（2）");

            // BOM 行编辑（2 → 5）后再建新工单
            updateBomLineQuantity(6154L, bd("5"));
            Long wo2 = seedWorkOrder("WO-SNAP-NEW2", bomId, P, bd("1"));
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(wo2)));

            ErpMfgWorkOrderBomSnapshot snap2 = findSnapshot(wo2);
            assertEquals(0, bd("5").compareTo(findSnapshotLines(snap2.getId()).get(0).getQuantity()),
                    "WO2 快照 = 提交时点当前 BOM 内容（5）——新建工单才用新 BOM（断言⑥）");
            assertEquals("V1", snap2.getVersionLabel(), "版本号追溯仍为当前 BOM 版本");
        } finally {
            setReservationEnabled(true);
        }
    }

    // ---------- ⑤ AUTO_UPGRADE 配置下 re-resolve 最新 BOM（实时展开、不写回快照） ----------

    @Test
    public void testAutoUpgradeReResolvesLatestBom() {
        seedProduct(P, "MOVING_AVERAGE");
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9155L, P, "V1", bd("1"));
        seedBomLine(6155L, bomId, M1, bd("2"), 10);
        Long woId = seedWorkOrder("WO-SNAP-AUTO", bomId, P, bd("2"));
        generateIncoming(M1, "PR-SNAP-AUTO", bd("7"), bd("5"));
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);
        // 预留关闭：避免 approve 预留占用污染可用量口径（区分快照 4≤7 全齐 vs AUTO_UPGRADE 10>7 部分）
        setReservationEnabled(false);
        setConfig(ErpMfgConstants.CONFIG_BOM_SNAPSHOT_STRATEGY,
                ErpMfgConstants.BOM_SNAPSHOT_STRATEGY_AUTO_UPGRADE);
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
            rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
            assertNotNull(findSnapshot(woId), "AUTO_UPGRADE 下提交仍落快照（写路径不变）");

            updateBomLineQuantity(6155L, bd("5"));

            // 读侧：re-resolve 默认 BOM 实时展开 → 需求 5×2=10 > 可用 7 → 部分齐套
            KitAvailabilityResult result = ormTemplate.runInSession(
                    s -> kitAvailabilityChecker.check(woId));
            assertFalse(result.isFullyAvailable(), "AUTO_UPGRADE 读侧 re-resolve 最新 BOM（需求 10 > 7）");
            assertFalse(result.getShortages().isEmpty(), "缺料明细含 M1");

            // 不写回快照：快照行仍为提交时点 2
            ErpMfgWorkOrderBomSnapshot snap = findSnapshot(woId);
            assertEquals(0, bd("2").compareTo(findSnapshotLines(snap.getId()).get(0).getQuantity()),
                    "AUTO_UPGRADE 实时展开不写回快照（快照内容仍为提交时点）");

            // 防御回退：默认 BOM 停用（升级目标缺失）→ 回退快照保持锁定内容 → 需求 4 ≤ 7 全齐
            deactivateDefaultBom(bomId);
            KitAvailabilityResult fallback = ormTemplate.runInSession(
                    s -> kitAvailabilityChecker.check(woId));
            assertTrue(fallback.isFullyAvailable(),
                    "AUTO_UPGRADE 无默认 BOM → 防御回退快照（需求 4 ≤ 7 仍全齐）");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_BOM_SNAPSHOT_STRATEGY,
                    ErpMfgConstants.DEFAULT_BOM_SNAPSHOT_STRATEGY);
            setReservationEnabled(true);
        }
    }

    // ---------- ⑥ 幂等（快照已存在跳过）+ 无 BOM 跳过不阻断 ----------

    @Test
    public void testIdempotentSubmitNoDuplicateSnapshot() {
        seedProduct(P, "MOVING_AVERAGE");
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9156L, P, "V1", bd("1"));
        seedBomLine(6156L, bomId, M1, bd("2"), 10);
        Long woId = seedWorkOrder("WO-SNAP-IDEM", bomId, P, bd("1"));
        setReservationEnabled(false);
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
            assertEquals(1, countSnapshots(woId), "首次提交落 1 个快照头");

            // 模拟 reject→修改→resubmit：直接复位状态后再次 submit（快照已存在 → 幂等跳过不重复）
            ormTemplate.runInSession(() -> {
                ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
                wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
                wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
                daoProvider.daoFor(ErpMfgWorkOrder.class).updateEntity(wo);
            });
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));

            assertEquals(1, countSnapshots(woId), "重复提交不重复快照（幂等：快照已存在跳过）");
            ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
            assertEquals("V1", wo.getSnapshotBomVersion(), "快照版本保持首次提交时点");
            assertEquals(1, findSnapshotLines(findSnapshot(woId).getId()).size(), "快照行不重复");
        } finally {
            setReservationEnabled(true);
        }
    }

    @Test
    public void testNoBomSubmitSkipsSnapshot() {
        seedProduct(P2, null);
        Long woId = seedWorkOrder("WO-SNAP-NOBOM", null, P2, bd("1"));
        setReservationEnabled(false);
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)),
                    "无 BOM 工单提交不阻断（空快照 + LOG.warn）");
            ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
            assertNull(wo.getSnapshotBomVersion(), "无 BOM → 无快照版本");
            assertNull(wo.getSnapshotBomId(), "无 BOM → 无快照归属锚");
            assertEquals(0, countSnapshots(woId), "无 BOM → 零快照落库");
        } finally {
            setReservationEnabled(true);
        }
    }

    // ---------- ⑦ R1.48 预留生命周期回归（approve→reservation 经快照化展开）+ GraphQL 冒烟 ----------

    @Test
    public void testApproveReservationFromSnapshot() {
        seedProduct(P, "MOVING_AVERAGE");
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9157L, P, "V1", bd("1"));
        seedBomLine(6157L, bomId, M1, bd("2"), 10);
        Long woId = seedWorkOrder("WO-SNAP-RSV", bomId, P, bd("2"));
        generateIncoming(M1, "PR-SNAP-RSV", bd("10"), bd("5"));
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        // BOM 行编辑（2 → 5）后再审核：预留必须仍按快照需求 4（非实时 10）
        updateBomLineQuantity(6157L, bd("5"));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));

        ErpInvReservation reservation = findReservation("WO-SNAP-RSV");
        assertNotNull(reservation, "审核后创建预留（R1.48 回归）");
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(1, lines.size());
        assertEquals(0, bd("4").compareTo(lines.get(0).getReservedQuantity()),
                "预留量 = min(快照需求 2×2=4, 可用 10) = 4——approve 预留经快照化展开");
    }

    @Test
    public void testSnapshotEntitiesReachableViaGraphQL() {
        seedProduct(P, "MOVING_AVERAGE");
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9158L, P, "V1", bd("1"));
        seedBomLine(6158L, bomId, M1, bd("2"), 10);
        seedBomOperation(7158L, bomId, WC1, bd("60"));
        Long woId = seedWorkOrder("WO-SNAP-GQL", bomId, P, bd("1"));
        setReservationEnabled(false);
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));

            ApiResponse<?> snapPage = rpc(query, "ErpMfgWorkOrderBomSnapshot__findPage",
                    ApiRequest.build(Map.of("limit", 10)));
            assertEquals(0, snapPage.getStatus(), "快照头经 GraphQL findPage 可达: " + snapPage);
            assertTrue(((Number) ((Map<?, ?>) snapPage.getData()).get("total")).intValue() >= 1,
                    "快照头查询应返回 ≥1 条");

            ApiResponse<?> linePage = rpc(query, "ErpMfgWorkOrderBomLineSnapshot__findPage",
                    ApiRequest.build(Map.of("limit", 10)));
            assertEquals(0, linePage.getStatus(), "快照行经 GraphQL findPage 可达: " + linePage);
            assertTrue(((Number) ((Map<?, ?>) linePage.getData()).get("total")).intValue() >= 1,
                    "快照行查询应返回 ≥1 条");
        } finally {
            setReservationEnabled(true);
        }
    }

    // ---------- helpers ----------

    private List<app.erp.mfg.biz.BomExplosionNode> bomExpanderExplode(Long bomId, BigDecimal qty) {
        return bomExpander.explode(bomId, qty, true);
    }

    private void seedProduct(Long id, String costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            m.setCostMethod(costMethod);
            dao.saveEntity(m);
        });
    }

    private void seedWorkcenter(Long id, BigDecimal hourlyRate) {
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

    private Long seedBom(Long id, Long productId, String versionLabel, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", id);
            bom.setCode("BOM-" + id);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(qty);
            bom.setVersionLabel(versionLabel);
            dao.saveEntity(bom);
        });
        return id;
    }

    private void seedBomLine(Long id, Long bomId, Long materialId, BigDecimal quantity, int lineNo) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBomLine> dao = daoProvider.daoFor(ErpMfgBomLine.class);
            ErpMfgBomLine line = new ErpMfgBomLine();
            line.orm_propValueByName("id", id);
            line.setBomId(bomId);
            line.setLineNo(lineNo);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(quantity);
            dao.saveEntity(line);
        });
    }

    private void seedBomOperation(Long id, Long bomId, Long workcenterId, BigDecimal standardTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBomOperation> dao = daoProvider.daoFor(ErpMfgBomOperation.class);
            ErpMfgBomOperation op = new ErpMfgBomOperation();
            op.orm_propValueByName("id", id);
            op.setBomId(bomId);
            op.setLineNo(10);
            op.setOperationId(9000L);
            op.setWorkcenterId(workcenterId);
            op.setStandardTime(standardTime);
            dao.saveEntity(op);
        });
    }

    private void updateBomLineQuantity(Long lineId, BigDecimal quantity) {
        ormTemplate.runInSession(() -> {
            ErpMfgBomLine line = daoProvider.daoFor(ErpMfgBomLine.class).getEntityById(lineId);
            line.setQuantity(quantity);
            daoProvider.daoFor(ErpMfgBomLine.class).updateEntity(line);
        });
    }

    private void updateBomOperationStandardTime(Long opId, BigDecimal standardTime) {
        ormTemplate.runInSession(() -> {
            ErpMfgBomOperation op = daoProvider.daoFor(ErpMfgBomOperation.class).getEntityById(opId);
            op.setStandardTime(standardTime);
            daoProvider.daoFor(ErpMfgBomOperation.class).updateEntity(op);
        });
    }

    private void deactivateDefaultBom(Long bomId) {
        ormTemplate.runInSession(() -> {
            ErpMfgBom bom = daoProvider.daoFor(ErpMfgBom.class).getEntityById(bomId);
            bom.setIsActive(Boolean.FALSE);
            daoProvider.daoFor(ErpMfgBom.class).updateEntity(bom);
        });
    }

    private void seedFirmedRollup(Long productId) {
        ormTemplate.runInSession(() -> {
            Long headerId = productId * 10000 + 1;
            IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
            ErpMfgCostRollup header = new ErpMfgCostRollup();
            header.orm_propValueByName("id", headerId);
            header.setCode("ROLLUP-" + productId);
            header.setOrgId(ORG_ID);
            header.setBusinessDate(LocalDate.of(2026, 6, 1));
            header.orm_propValueByName("status", ErpMfgConstants.COST_ROLLUP_STATUS_FIRMED);
            headerDao.saveEntity(header);

            IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
            ErpMfgCostRollupLine line = new ErpMfgCostRollupLine();
            line.orm_propValueByName("id", productId * 10000 + 2);
            line.setCostRollupId(headerId);
            line.setLineNo(10);
            line.setMaterialId(productId);
            line.setUoMId(UOM_ID);
            line.setMaterialCost(bd("10"));
            line.setLaborCost(bd("10"));
            line.setOverheadCost(bd("5"));
            line.setUnitCost(bd("25"));
            line.setTotalCost(bd("25"));
            line.setCurrencyId(CURRENCY_ID);
            lineDao.saveEntity(line);
        });
    }

    private Long seedWorkOrder(String code, Long bomId, Long productId, BigDecimal plannedQty) {
        Long id = 8300L + (long) Math.abs(code.hashCode() % 700);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(productId);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(plannedQty);
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });
        return id;
    }

    private Long seedWorkOrderLine(Long woId, Long materialId, BigDecimal plannedQty, String lineType,
                                   Long destWarehouseId, Long sourceWarehouseId) {
        long raw = (woId + "" + materialId + lineType).hashCode();
        Long id = 9300L + (long) Math.abs(raw % 700);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
            ErpMfgWorkOrderLine wol = new ErpMfgWorkOrderLine();
            wol.orm_propValueByName("id", id);
            wol.setWorkOrderId(woId);
            wol.setLineNo(materialId.intValue());
            wol.orm_propValueByName("lineType", lineType);
            wol.setMaterialId(materialId);
            wol.setUoMId(UOM_ID);
            wol.setPlannedQuantity(plannedQty);
            wol.setDestWarehouseId(destWarehouseId);
            wol.setSourceWarehouseId(sourceWarehouseId);
            dao.saveEntity(wol);
        });
        return id;
    }

    private void seedTimeLog(Long id, Long woId, BigDecimal durationMins) {
        ormTemplate.runInSession(() -> {
            app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog log = daoProvider.daoFor(
                    app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog.class).newEntity();
            log.orm_propValueByName("id", id);
            log.setJobCardId(9001L);
            log.setWorkOrderId(woId);
            log.setOperatorId("OP-001");
            log.setWorkDate(LocalDate.of(2026, 7, 1));
            log.setDurationMins(durationMins);
            daoProvider.daoFor(app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog.class).saveEntity(log);
        });
    }

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));
        rpcOk(mutation, "ErpInvStockMove__generateMove", Map.of("request", req));
    }

    private ErpMfgWorkOrderBomSnapshot findSnapshot(Long workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        List<ErpMfgWorkOrderBomSnapshot> list = daoProvider.daoFor(ErpMfgWorkOrderBomSnapshot.class)
                .findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private int countSnapshots(Long workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        return daoProvider.daoFor(ErpMfgWorkOrderBomSnapshot.class).findAllByQuery(q).size();
    }

    private List<ErpMfgWorkOrderBomLineSnapshot> findSnapshotLines(Long snapshotId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("snapshotId", snapshotId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpMfgWorkOrderBomLineSnapshot.class).findAllByQuery(q);
    }

    private List<ErpMfgWorkOrderBomOperationSnapshot> findSnapshotOperations(Long snapshotId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("snapshotId", snapshotId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpMfgWorkOrderBomOperationSnapshot.class).findAllByQuery(q);
    }

    private ErpMfgCostVariance lineByType(List<ErpMfgCostVariance> lines, String type) {
        return lines.stream().filter(l -> type.equals(l.getVarianceType())).findFirst()
                .orElseThrow(() -> new AssertionError("no variance line of type " + type));
    }

    private ErpInvReservation findReservation(String workOrderCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", ErpMfgConstants.SOURCE_BILL_TYPE_WORK_ORDER));
        q.addFilter(eq("sourceBillCode", workOrderCode));
        List<ErpInvReservation> list = daoProvider.daoFor(ErpInvReservation.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvReservationLine> findReservationLines(Long reservationId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("reservationId", reservationId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpInvReservationLine.class).findAllByQuery(q);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args,
                       String msg) {
        ApiResponse<?> resp = rpc(op, action, ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), msg + ": " + resp);
    }

    private void setConfig(String key, String value) {
        AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private void setReservationEnabled(boolean value) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_RESERVATION_ENABLED, String.valueOf(value));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
