package app.erp.drp.service;

import app.erp.drp.biz.IErpInvDrpCrossDockBiz;
import app.erp.drp.dao.entity.ErpInvDrpCrossDock;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionTemplate;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.drp.service.job.ErpDrpCrossDockStagingTimeoutJob;
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
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.81 / P1-RC-081 越库执行引擎测试（UC-DRP-07；owner doc cross-dock.md §越库状态机 + §三匹配策略）。
 *
 * <p>覆盖：状态机合法迁移（含直连 PENDING→MATCHED）/ 非法迁移拒绝 / 三策略（PRE_ALLOCATED 读预分配、
 * ON_RECEIPT 按承诺发货日期 ASC 扫描待出库销售订单、MANUAL 显式指定）/ 超时转正常入库 /
 * 质检守卫（D2 裁决选项 A：有效检验模板载体 + 快检通过凭证 + config 关闭跳过 + 无模板物料放行）/
 * config 总门关闭整体拒绝 / CANCELLED 终态 / purchase 收货 Facade 幂等（双收货同记录）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpCrossDock extends JunitAutoTestCase {

    static final String ORG_ID = "6401";
    static final String UOM_ID = "6501";
    static final String WH_ID = "6101";
    static final String LOC_ID = "6151";   // 越库暂存库位
    static final String CUST_ID = "6601";
    static final String CUR_ID = "6701";
    static final String M1 = "6201";
    static final String M2 = "6202";       // 无检验模板物料
    static final String MOVE_ID = "6301";  // 入站移动单

    @RegisterExtension
    static DrpFrozenClockExtension frozenClock = new DrpFrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpInvDrpCrossDockBiz crossDockBiz;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    @AfterEach
    public void resetXdockConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpDrpConfigs.CONFIG_DRP_XDOCK_ENABLED, "false");
        AppConfig.getConfigProvider().assignConfigValue(ErpDrpConfigs.CONFIG_DRP_XDOCK_QUALITY_GATE_ENABLED, "false");
        AppConfig.getConfigProvider().assignConfigValue(ErpDrpConfigs.CONFIG_DRP_XDOCK_STAGING_TIMEOUT_CRON, "");
    }

    // ---------- ① 收货识别合法迁移 PENDING→STAGING ----------

    @Test
    public void testReceiveMarkLegalTransition() {
        enableXdock();
        seedBaseData();
        String dockId = seedDock("6801", "XDK-1", ErpDrpConstants.XDOCK_STATUS_PENDING, null, M1, null, null);

        rpcOk("receiveMark", args("id", dockId, "inboundMoveId", MOVE_ID));

        ErpInvDrpCrossDock dock = reload(dockId);
        assertEquals(ErpDrpConstants.XDOCK_STATUS_STAGING, dock.getStatus(), "收货识别后应 STAGING");
        assertEquals(MOVE_ID, dock.getInboundMoveId(), "应回写 inboundMoveId");
    }

    // ---------- ② 非法迁移拒绝 ----------

    @Test
    public void testIllegalTransitionsRejected() {
        enableXdock();
        seedBaseData();
        String pending = seedDock("6802", "XDK-2", ErpDrpConstants.XDOCK_STATUS_PENDING, null, M1, null, null);
        String staging = seedDock("6803", "XDK-3", ErpDrpConstants.XDOCK_STATUS_STAGING, null, M1, null, null);
        String matched = seedDock("6804", "XDK-4", ErpDrpConstants.XDOCK_STATUS_MATCHED, null, M1, null, null);
        String completed = seedDock("6805", "XDK-5", ErpDrpConstants.XDOCK_STATUS_COMPLETED, null, M1, null, null);

        assertTrue(rpc("load", args("id", pending)).getStatus() != 0, "PENDING 不可直接 load（须先 MATCHED）");
        assertTrue(rpc("complete", args("id", matched)).getStatus() != 0, "MATCHED 不可 complete（须先 LOADED）");
        assertTrue(rpc("receiveMark", args("id", staging, "inboundMoveId", MOVE_ID)).getStatus() != 0,
                "STAGING 不可重复 receiveMark");
        assertTrue(rpc("cancel", args("id", completed)).getStatus() != 0, "COMPLETED 终态不可 cancel");
        assertTrue(rpc("match", args("id", completed, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-X"))
                .getStatus() != 0, "COMPLETED 终态不可 match");

        assertEquals(ErpDrpConstants.XDOCK_STATUS_PENDING, reload(pending).getStatus());
        assertEquals(ErpDrpConstants.XDOCK_STATUS_COMPLETED, reload(completed).getStatus());
    }

    // ---------- ③ PRE_ALLOCATED：读记录预分配目标 / 缺目标拒绝 ----------

    @Test
    public void testPreAllocatedMatchUsesRecordTarget() {
        enableXdock();
        seedBaseData();
        String withTarget = seedDock("6806", "XDK-6", ErpDrpConstants.XDOCK_STATUS_STAGING,
                ErpDrpConstants.XDOCK_STRATEGY_PRE_ALLOCATED, M1, "SAL_ORDER", "SO-PRE");
        String noTarget = seedDock("6807", "XDK-7", ErpDrpConstants.XDOCK_STATUS_STAGING,
                ErpDrpConstants.XDOCK_STRATEGY_PRE_ALLOCATED, M1, null, null);

        rpcOk("match", args("id", withTarget, "targetBillType", null, "targetBillCode", null));

        ErpInvDrpCrossDock dock = reload(withTarget);
        assertEquals(ErpDrpConstants.XDOCK_STATUS_MATCHED, dock.getStatus());
        assertEquals("SO-PRE", dock.getTargetBillCode(), "PRE_ALLOCATED 应使用创建时预分配目标");
        assertNotNull(dock.getMatchedAt());

        assertTrue(rpc("match", args("id", noTarget, "targetBillType", null, "targetBillCode", null)).getStatus() != 0,
                "PRE_ALLOCATED 无预分配目标应拒绝");
    }

    // ---------- ④ ON_RECEIPT：扫描待出库销售订单（承诺发货日期 ASC）/ 无候选拒绝 ----------

    @Test
    public void testOnReceiptMatchPicksEarliestDeliveryDate() {
        enableXdock();
        seedBaseData();
        seedSalOrder("7101", "SO-EARLY", LocalDate.of(2026, 7, 10), LocalDateTime.of(2026, 7, 8, 9, 0));
        seedSalOrder("7102", "SO-LATE", LocalDate.of(2026, 7, 15), LocalDateTime.of(2026, 7, 1, 9, 0));
        seedSalOrderLine("7201", "7101", M1, bd("100"), bd("0"));
        seedSalOrderLine("7202", "7102", M1, bd("200"), bd("0"));

        String dockId = seedDock("6808", "XDK-8", ErpDrpConstants.XDOCK_STATUS_STAGING,
                ErpDrpConstants.XDOCK_STRATEGY_ON_RECEIPT, M1, null, null);

        rpcOk("match", args("id", dockId, "targetBillType", null, "targetBillCode", null));

        ErpInvDrpCrossDock dock = reload(dockId);
        assertEquals(ErpDrpConstants.XDOCK_STATUS_MATCHED, dock.getStatus());
        assertEquals("SO-EARLY", dock.getTargetBillCode(),
                "ON_RECEIPT 应选承诺发货日期最早的待出库销售订单（而非创建最早）");
        assertEquals("SAL_ORDER", dock.getTargetBillType());

        // 无候选（M2 无销售订单行）→ 拒绝
        String noCandidate = seedDock("6809", "XDK-9", ErpDrpConstants.XDOCK_STATUS_STAGING,
                ErpDrpConstants.XDOCK_STRATEGY_ON_RECEIPT, M2, null, null);
        assertTrue(rpc("match", args("id", noCandidate, "targetBillType", null, "targetBillCode", null))
                .getStatus() != 0, "无待出库销售订单候选应拒绝");
    }

    // ---------- ⑤ MANUAL：显式指定目标 / 缺目标拒绝 ----------

    @Test
    public void testManualMatchRequiresExplicitTarget() {
        enableXdock();
        seedBaseData();
        String dockId = seedDock("6810", "XDK-10", ErpDrpConstants.XDOCK_STATUS_STAGING,
                ErpDrpConstants.XDOCK_STRATEGY_MANUAL, M1, null, null);

        assertTrue(rpc("match", args("id", dockId, "targetBillType", null, "targetBillCode", null)).getStatus() != 0,
                "MANUAL 无显式目标应拒绝");

        rpcOk("match", args("id", dockId, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-MANUAL"));
        assertEquals("SO-MANUAL", reload(dockId).getTargetBillCode());
    }

    // ---------- ⑥ 直连 PENDING→MATCHED（owner doc `[inbound 到达 + 匹配目标订单] → MATCHED` 边） ----------

    @Test
    public void testDirectPendingToMatched() {
        enableXdock();
        seedBaseData();
        String dockId = seedDock("6811", "XDK-11", ErpDrpConstants.XDOCK_STATUS_PENDING,
                ErpDrpConstants.XDOCK_STRATEGY_MANUAL, M1, null, null);

        rpcOk("match", args("id", dockId, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-DIRECT"));

        assertEquals(ErpDrpConstants.XDOCK_STATUS_MATCHED, reload(dockId).getStatus(),
                "收货即匹配场景应支持直连 PENDING→MATCHED");
    }

    // ---------- ⑦ 全链路：load 生成出站移动 → LOADED → complete → COMPLETED 终态 ----------

    @Test
    public void testLoadGeneratesOutboundMoveThenComplete() {
        enableXdock();
        seedBaseData();
        seedBalance(M1, WH_ID, LOC_ID, bd("500"));
        String dockId = seedDock("6812", "XDK-12", ErpDrpConstants.XDOCK_STATUS_PENDING,
                ErpDrpConstants.XDOCK_STRATEGY_MANUAL, M1, null, null);

        rpcOk("receiveMark", args("id", dockId, "inboundMoveId", MOVE_ID));
        rpcOk("match", args("id", dockId, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-FULL"));
        rpcOk("load", args("id", dockId));

        ErpInvDrpCrossDock dock = reload(dockId);
        assertEquals(ErpDrpConstants.XDOCK_STATUS_LOADED, dock.getStatus());
        assertNotNull(dock.getOutboundMoveId(), "应回写 outboundMoveId");
        assertNotNull(dock.getLoadedAt());

        ErpInvStockMove outbound = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(dock.getOutboundMoveId());
        assertNotNull(outbound, "出站移动单应已生成");
        assertEquals(ErpDrpConstants.MOVE_TYPE_OUTGOING, outbound.getMoveType());
        assertEquals("DONE", outbound.getDocStatus(), "business-linked 出站移动应自动推进 DONE");
        assertEquals("XDK-12", outbound.getRelatedBillCode(), "出站移动弱指针应回链越库记录");

        rpcOk("complete", args("id", dockId));
        assertEquals(ErpDrpConstants.XDOCK_STATUS_COMPLETED, reload(dockId).getStatus());

        assertTrue(rpc("complete", args("id", dockId)).getStatus() != 0, "COMPLETED 终态不可重复 complete");
    }

    // ---------- ⑧ 超时回退：STAGING 超时 → 转正常入库移动 + CANCELLED ----------

    @Test
    public void testStagingTimeoutFallbackJob() {
        enableXdock();
        seedBaseData();
        seedBalance(M1, WH_ID, LOC_ID, bd("500"));
        // 冻结时钟 2026-07-17，超时阈值默认 24h → updateTime 早于 2026-07-16 的 STAGING 记录被扫描
        Timestamp old = Timestamp.valueOf(LocalDateTime.of(2026, 7, 15, 0, 0));
        String dockId = seedDockWithUpdateTime("6813", "XDK-13", ErpDrpConstants.XDOCK_STATUS_STAGING,
                ErpDrpConstants.XDOCK_STRATEGY_ON_RECEIPT, M1, null, null, old);

        AppConfig.getConfigProvider().assignConfigValue(ErpDrpConfigs.CONFIG_DRP_XDOCK_STAGING_TIMEOUT_CRON,
                "0 15 */2 * * ?");

        newWiredJob().execute();

        ErpInvDrpCrossDock dock = reload(dockId);
        assertEquals(ErpDrpConstants.XDOCK_STATUS_CANCELLED, dock.getStatus(), "超时未匹配应转 CANCELLED");
        assertNotNull(dock.getRemark(), "应备注超时回退");

        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpDrpConstants.RELATED_BILL_TYPE_DRP_XDOCK_PUTAWAY));
        q.addFilter(eq("relatedBillCode", "XDK-13"));
        List<ErpInvStockMove> putaway = stockMoveBiz.findList(q, null, new io.nop.core.context.ServiceContextImpl());
        assertEquals(1, putaway.size(), "应生成 staging→正常存储位移动单");
        assertEquals(ErpDrpConstants.MOVE_TYPE_INTERNAL_TRANSFER, putaway.get(0).getMoveType());
        assertEquals("DONE", putaway.get(0).getDocStatus());
    }

    // ---------- ⑨ 质检守卫：需质检物料未快检拒绝 / 快检合格放行 ----------

    @Test
    public void testQualityGateBlockedWithoutQuickCheck() {
        enableXdock();
        enableQualityGate();
        seedBaseData();
        seedQaTemplate("7301", "QA-TPL-1", M1, 1);
        String dockId = seedDock("6814", "XDK-14", ErpDrpConstants.XDOCK_STATUS_STAGING,
                ErpDrpConstants.XDOCK_STRATEGY_MANUAL, M1, null, null);

        assertTrue(rpc("match", args("id", dockId, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-QA"))
                .getStatus() != 0, "需质检物料未快检应拒绝匹配");
        assertEquals(ErpDrpConstants.XDOCK_STATUS_STAGING, reload(dockId).getStatus());

        // 暂存区快检（关联本越库记录，结果 ACCEPTED）→ 放行
        seedQaInspection("7401", "QA-INS-1", ErpDrpConstants.RELATED_BILL_TYPE_DRP_XDOCK, "XDK-14", M1,
                ErpDrpConstants.QA_INSPECTION_RESULT_ACCEPTED);

        rpcOk("match", args("id", dockId, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-QA"));
        assertEquals(ErpDrpConstants.XDOCK_STATUS_MATCHED, reload(dockId).getStatus(),
                "快检合格后应放行匹配");
    }

    // ---------- ⑩ 质检守卫：无模板物料放行（D2 载体负路径） ----------

    @Test
    public void testQualityGateSkippedForMaterialWithoutTemplate() {
        enableXdock();
        enableQualityGate();
        seedBaseData();
        seedQaTemplate("7302", "QA-TPL-2", M1, 1); // 仅 M1 有模板
        String dockId = seedDock("6815", "XDK-15", ErpDrpConstants.XDOCK_STATUS_STAGING,
                ErpDrpConstants.XDOCK_STRATEGY_MANUAL, M2, null, null);

        rpcOk("match", args("id", dockId, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-NO-TPL"));
        assertEquals(ErpDrpConstants.XDOCK_STATUS_MATCHED, reload(dockId).getStatus(),
                "无有效检验模板物料（M2）应视为无需质检，直接放行");
    }

    // ---------- ⑪ 质检守卫：config 关闭整体跳过 ----------

    @Test
    public void testQualityGateConfigOffSkipsGuard() {
        enableXdock(); // 质检门保持默认 false
        seedBaseData();
        seedQaTemplate("7303", "QA-TPL-3", M1, 1); // 有模板但 config 关闭
        String dockId = seedDock("6816", "XDK-16", ErpDrpConstants.XDOCK_STATUS_STAGING,
                ErpDrpConstants.XDOCK_STRATEGY_MANUAL, M1, null, null);

        rpcOk("match", args("id", dockId, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-GATE-OFF"));
        assertEquals(ErpDrpConstants.XDOCK_STATUS_MATCHED, reload(dockId).getStatus(),
                "质检门 config 关闭时应跳过守卫（默认 opt-in）");
    }

    // ---------- ⑫ config 总门关闭：全部 mutation 拒绝 ----------

    @Test
    public void testConfigDisabledBlocksAllMutations() {
        // 保持默认 xdock-enabled=false
        seedBaseData();
        String dockId = seedDock("6817", "XDK-17", ErpDrpConstants.XDOCK_STATUS_PENDING, null, M1, null, null);

        assertTrue(rpc("receiveMark", args("id", dockId, "inboundMoveId", MOVE_ID)).getStatus() != 0,
                "功能未启用应拒绝 receiveMark");
        assertTrue(rpc("match", args("id", dockId, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-OFF"))
                .getStatus() != 0, "功能未启用应拒绝 match");
        assertEquals(ErpDrpConstants.XDOCK_STATUS_PENDING, reload(dockId).getStatus());
    }

    // ---------- ⑬ purchase 收货 Facade：双收货同记录幂等（并发组） ----------

    @Test
    public void testMarkReceivedFromPurchaseIdempotent() {
        enableXdock();
        seedBaseData();
        String dockId = seedDock("6818", "XDK-18", ErpDrpConstants.XDOCK_STATUS_PENDING, null, M1, null, null);
        // sourceBillType=PUR_ORDER + sourceBillCode=PO-1
        setSourceBill(dockId, ErpDrpConstants.XDOCK_SOURCE_BILL_TYPE_PUR_ORDER, "PO-1");

        Map<String, Object> args = args("purchaseOrderCode", "PO-1", "inboundMoveId", MOVE_ID,
                "materialIds", List.of(M1));
        ApiResponse<?> first = rpc("markReceivedFromPurchase", args);
        assertEquals(0, first.getStatus());
        assertEquals(1, ((Number) first.getData()).intValue(), "首次应收货标记 1 条");

        ApiResponse<?> second = rpc("markReceivedFromPurchase", args);
        assertEquals(0, second.getStatus());
        assertEquals(0, ((Number) second.getData()).intValue(), "双收货同记录第二次应为无操作（仅 PENDING 可迁移）");

        ErpInvDrpCrossDock dock = reload(dockId);
        assertEquals(ErpDrpConstants.XDOCK_STATUS_STAGING, dock.getStatus());
        assertEquals(MOVE_ID, dock.getInboundMoveId());
    }

    // ---------- ⑭ CANCELLED 终态与 STAGING 取消 ----------

    @Test
    public void testCancelFromStagingIsTerminal() {
        enableXdock();
        seedBaseData();
        String dockId = seedDock("6819", "XDK-19", ErpDrpConstants.XDOCK_STATUS_STAGING, null, M1, null, null);

        rpcOk("cancel", args("id", dockId));
        assertEquals(ErpDrpConstants.XDOCK_STATUS_CANCELLED, reload(dockId).getStatus());

        assertTrue(rpc("match", args("id", dockId, "targetBillType", "SAL_ORDER", "targetBillCode", "SO-C"))
                .getStatus() != 0, "CANCELLED 终态不可 match");
    }

    // ---------- helpers ----------

    private void enableXdock() {
        AppConfig.getConfigProvider().assignConfigValue(ErpDrpConfigs.CONFIG_DRP_XDOCK_ENABLED, "true");
    }

    private void enableQualityGate() {
        AppConfig.getConfigProvider().assignConfigValue(ErpDrpConfigs.CONFIG_DRP_XDOCK_QUALITY_GATE_ENABLED, "true");
    }

    private ErpDrpCrossDockStagingTimeoutJob newWiredJob() {
        ErpDrpCrossDockStagingTimeoutJob job = new ErpDrpCrossDockStagingTimeoutJob();
        job.setCrossDockBiz(crossDockBiz);
        job.setStockMoveBiz(stockMoveBiz);
        job.setDaoProvider(daoProvider);
        job.setOrmTemplate(ormTemplate);
        return job;
    }

    private Map<String, Object> args(Object... kv) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private void rpcOk(String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private ApiResponse<?> rpc(String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation,
                "ErpInvDrpCrossDock__" + action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpInvDrpCrossDock reload(String id) {
        return daoProvider.daoFor(ErpInvDrpCrossDock.class).getEntityById(id);
    }

    private void setSourceBill(String dockId, String billType, String billCode) {
        ormTemplate.runInSession(() -> {
            ErpInvDrpCrossDock dock = daoProvider.daoFor(ErpInvDrpCrossDock.class).getEntityById(dockId);
            dock.setSourceBillType(billType);
            dock.setSourceBillCode(billCode);
            daoProvider.daoFor(ErpInvDrpCrossDock.class).updateEntity(dock);
        });
    }

    private void seedBaseData() {
        seedMaterial(M1);
        seedMaterial(M2);
        seedWarehouse();
        seedLocation();
        seedPartner();
        seedCurrency();
    }

    private String seedDock(String id, String code, String status, String strategy, String materialId,
                          String targetBillType, String targetBillCode) {
        return seedDockWithUpdateTime(id, code, status, strategy, materialId, targetBillType, targetBillCode, null);
    }

    private String seedDockWithUpdateTime(String id, String code, String status, String strategy, String materialId,
                                        String targetBillType, String targetBillCode, Timestamp updateTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvDrpCrossDock> dao = daoProvider.daoFor(ErpInvDrpCrossDock.class);
            ErpInvDrpCrossDock d = dao.newEntity();
            if (updateTime != null) {
                d.orm_disableAutoStamp(true);
                d.setCreateTime(updateTime);
                d.setUpdateTime(updateTime);
                d.setCreatedBy("drp-test");
                d.setUpdatedBy("drp-test");
            }
            d.orm_propValueByName("id", id);
            d.setCode(code);
            d.setOrgId(ORG_ID);
            d.setMaterialId(materialId);
            d.setQuantity(bd("100"));
            d.setStagingLocationId(LOC_ID);
            d.orm_propValueByName("status", status);
            if (strategy != null) {
                d.orm_propValueByName("matchingStrategy", strategy);
            }
            if (targetBillType != null) {
                d.setTargetBillType(targetBillType);
            }
            if (targetBillCode != null) {
                d.setTargetBillCode(targetBillCode);
            }
            dao.saveEntity(d);
        });
        return id;
    }

    private void seedSalOrder(String id, String code, LocalDate deliveryDate, LocalDateTime createTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
            ErpSalOrder o = dao.newEntity();
            o.orm_disableAutoStamp(true);
            o.orm_propValueByName("id", id);
            o.setCode(code);
            o.setOrgId(ORG_ID);
            o.setCustomerId(CUST_ID);
            o.setWarehouseId(WH_ID);
            o.setBusinessDate(LocalDate.of(2026, 7, 1));
            o.setDeliveryDate(deliveryDate);
            o.setCurrencyId(CUR_ID);
            o.setCreateTime(Timestamp.valueOf(createTime));
            o.setUpdateTime(Timestamp.valueOf(createTime));
            o.setCreatedBy("drp-test");
            o.setUpdatedBy("drp-test");
            o.orm_propValueByName("docStatus", "APPROVED");
            o.orm_propValueByName("approveStatus", ErpDrpConstants.SAL_ORDER_APPROVE_STATUS_APPROVED);
            dao.saveEntity(o);
        });
    }

    private void seedSalOrderLine(String id, String orderId, String materialId, BigDecimal quantity,
                                  BigDecimal deliveredQuantity) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
            ErpSalOrderLine l = dao.newEntity();
            l.orm_propValueByName("id", id);
            l.setOrderId(orderId);
            l.setLineNo(10);
            l.setMaterialId(materialId);
            l.setUoMId(UOM_ID);
            l.setQuantity(quantity);
            l.setUnitPrice(bd("10"));
            l.setAmount(bd("1000"));
            l.setAmountWithTax(bd("1130"));
            l.setDeliveredQuantity(deliveredQuantity);
            dao.saveEntity(l);
        });
    }

    private void seedQaTemplate(String id, String code, String materialId, int isActive) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspectionTemplate> dao = daoProvider.daoFor(ErpQaInspectionTemplate.class);
            ErpQaInspectionTemplate t = dao.newEntity();
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setName("Template " + code);
            t.orm_propValueByName("inspectionType", ErpDrpConstants.QA_INSPECTION_TYPE_INCOMING);
            t.setMaterialId(materialId);
            t.setIsActive(isActive);
            dao.saveEntity(t);
        });
    }

    private void seedQaInspection(String id, String code, String relatedBillType, String relatedBillCode,
                                  String materialId, String result) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspection> dao = daoProvider.daoFor(ErpQaInspection.class);
            ErpQaInspection i = dao.newEntity();
            i.orm_propValueByName("id", id);
            i.setCode(code);
            i.setOrgId(ORG_ID);
            i.orm_propValueByName("inspectionType", ErpDrpConstants.QA_INSPECTION_TYPE_INCOMING);
            i.setRelatedBillType(relatedBillType);
            i.setRelatedBillCode(relatedBillCode);
            i.setMaterialId(materialId);
            i.setBusinessDate(LocalDate.of(2026, 7, 16));
            i.setInspectionDate(LocalDate.of(2026, 7, 16));
            i.orm_propValueByName("result", result);
            i.orm_propValueByName("docStatus", "APPROVED");
            i.orm_propValueByName("approveStatus", "APPROVED");
            dao.saveEntity(i);
        });
    }

    private void seedBalance(String materialId, String warehouseId, String locationId, BigDecimal available) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = dao.newEntity();
            b.orm_propValueByName("id", String.valueOf(6900L + Long.parseLong(materialId)));
            b.setOrgId(ORG_ID);
            b.setMaterialId(materialId);
            b.setWarehouseId(warehouseId);
            b.setLocationId(locationId);
            b.setTotalQuantity(available);
            b.setAvailableQuantity(available);
            dao.saveEntity(b);
        });
    }

    private void seedMaterial(String id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = dao.newEntity();
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
            ErpMdWarehouse w = dao.newEntity();
            w.orm_propValueByName("id", WH_ID);
            w.setCode("WH-" + WH_ID);
            w.setName("Warehouse " + WH_ID);
            w.setStatus("ACTIVE");
            dao.saveEntity(w);
        });
    }

    private void seedLocation() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdLocation> dao = daoProvider.daoFor(ErpMdLocation.class);
            ErpMdLocation l = dao.newEntity();
            l.orm_propValueByName("id", LOC_ID);
            l.setWarehouseId(WH_ID);
            l.setCode("XD-STAGE-" + LOC_ID);
            l.setName("Cross Dock Staging " + LOC_ID);
            dao.saveEntity(l);
        });
    }

    private void seedPartner() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
            ErpMdPartner p = dao.newEntity();
            p.orm_propValueByName("id", CUST_ID);
            p.setCode("CUST-" + CUST_ID);
            p.setName("Customer " + CUST_ID);
            p.orm_propValueByName("partnerType", "CUSTOMER");
            p.orm_propValueByName("status", "ACTIVE");
            dao.saveEntity(p);
        });
    }

    private void seedCurrency() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
            ErpMdCurrency c = dao.newEntity();
            c.orm_propValueByName("id", CUR_ID);
            c.setCode("CNY");
            c.setName("人民币");
            dao.saveEntity(c);
        });
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
