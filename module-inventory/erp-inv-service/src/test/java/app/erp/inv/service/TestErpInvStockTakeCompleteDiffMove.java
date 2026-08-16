package app.erp.inv.service;

import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.dao.entity.ErpInvStockTake;
import app.erp.inv.dao.entity.ErpInvStockTakeLine;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
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
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.56 / P1-MA2-062（UC-INV-07）盘点完成自动差异移动单测试（plan 2026-08-16-0904-3 Phase 3）。
 *
 * <p>覆盖 8 组断言：① 盘盈（差异>0）生成 INCOMING 移动单（行量/方向 + D4-a remark 关联 + D2 停 CONFIRMED）；
 * ② 盘亏（差异<0）生成 OUTGOING 移动单；③ 零差异不生成；④ 盘点单本身不改余额（断言④）；⑤ 移动单 DONE 后
 * 余额变化（断言⑤，经 D2-A 库管员二次确认 complete 路径）；⑥ 幂等/重复 completeTake 守卫；⑦ 部分行失败隔离
 * （单行失败不阻断 + D4-b 告警派发 + 其余行移动单生成 + 孤立 DRAFT 补偿删除）；⑧ differenceQuantity/
 * differenceAmount 回填持久化 + D3 过账跳过（零凭证生成，跳过集生效）。
 *
 * <p>权威：{@code docs/design/inventory/use-cases.md} L1 UC-INV-07 五断言 + 本计划 Phase 1 D1-D4 裁决
 * + Phase 2 强制修正裁决 (a)/(b)。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvStockTakeCompleteDiffMove extends JunitAutoTestCase {

    static final Long ORG_ID = 1051L;
    static final Long WAREHOUSE_ID = 3051L;
    static final Long UOM_ID = 5051L;
    static final Long CURRENCY_ID = 6051L;
    static final Long MATERIAL_GAIN = 2051L;
    static final Long MATERIAL_LOSS = 2052L;
    static final Long MATERIAL_ZERO = 2053L;
    static final String NOTIFY_EVENT = "inv.stocktake-diff-generation-failed";
    static final String RECIPIENT = "inv-stocktake-recipient";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @AfterEach
    void restoreAlertConfig() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpInvConstants.CONFIG_STOCKTAKE_DIFF_ALERT_ENABLED, "false");
    }

    // ---------- ① 盘盈（差异>0）生成 INCOMING 移动单 ----------

    @Test
    public void testGainGeneratesIncomingMove() {
        seedBalance(MATERIAL_GAIN, new BigDecimal("10"));
        Long takeId = seedTake("TAKE-GAIN-001", MATERIAL_GAIN, bd("10"), bd("15"), bd("2"));

        startTake(takeId);
        ApiResponse<?> resp = completeTake(takeId);
        assertEquals(0, resp.getStatus(), "completeTake 应成功");

        ErpInvStockTake take = findTake(takeId);
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, take.getDocStatus(), "盘点单应 DONE");

        List<ErpInvStockMove> moves = findDiffMoves(take.getCode());
        assertEquals(1, moves.size(), "应生成 1 张差异移动单");
        ErpInvStockMove move = moves.get(0);
        assertEquals(ErpInvConstants.MOVE_TYPE_INCOMING, move.getMoveType(), "盘盈应生成 INCOMING 移动单");
        assertEquals(ErpInvConstants.DOC_STATUS_CONFIRMED, move.getDocStatus(),
                "D2 选项 A：独立移动单应停 CONFIRMED 待库管员二次确认");
        assertEquals(ErpInvConstants.RELATED_BILL_TYPE_STOCK_TAKE, move.getRelatedBillType(), "D2/D3 判别载体类型键");
        assertNull(move.getRelatedBillCode(), "D2：relatedBillCode 置空保持独立语义");
        assertTrue(move.getRemark().contains(take.getCode()) && move.getRemark().contains("盘盈"),
                "D4-a：remark 应承载「盘点差异 {code} 盘盈」关联");
        assertEquals(WAREHOUSE_ID, move.getDestWarehouseId(), "盘盈目的仓 = 盘点仓库");

        List<ErpInvStockMoveLine> lines = loadMoveLines(move.getId());
        assertEquals(1, lines.size(), "逐行差异移动单：单行");
        assertEquals(MATERIAL_GAIN, lines.get(0).getMaterialId(), "行物料 = 盘点行物料");
        assertEquals(0, lines.get(0).getQuantity().compareTo(new BigDecimal("5")),
                "行量 = |差异| = 实盘15 - 账面10 = 5");

        ErpInvStockTakeLine takeLine = findTakeLine(takeId, MATERIAL_GAIN);
        assertEquals(0, takeLine.getDifferenceQuantity().compareTo(new BigDecimal("5")),
                "回填 differenceQuantity = +5");
        assertEquals(0, takeLine.getDifferenceAmount().compareTo(new BigDecimal("10")),
                "回填 differenceAmount = 差异 × 单位成本 = 5 × 2 = 10");
    }

    // ---------- ② 盘亏（差异<0）生成 OUTGOING 移动单 ----------

    @Test
    public void testLossGeneratesOutgoingMove() {
        seedBalance(MATERIAL_LOSS, new BigDecimal("10"));
        Long takeId = seedTake("TAKE-LOSS-001", MATERIAL_LOSS, bd("10"), bd("6"), null);

        startTake(takeId);
        assertEquals(0, completeTake(takeId).getStatus(), "completeTake 应成功");

        List<ErpInvStockMove> moves = findDiffMoves(findTake(takeId).getCode());
        assertEquals(1, moves.size(), "应生成 1 张差异移动单");
        ErpInvStockMove move = moves.get(0);
        assertEquals(ErpInvConstants.MOVE_TYPE_OUTGOING, move.getMoveType(), "盘亏应生成 OUTGOING 移动单");
        assertEquals(ErpInvConstants.DOC_STATUS_CONFIRMED, move.getDocStatus(),
                "D2 选项 A：停 CONFIRMED 待库管员二次确认");
        assertTrue(move.getRemark().contains("盘亏"), "D4-a：remark 应含「盘亏」");
        assertEquals(WAREHOUSE_ID, move.getSourceWarehouseId(), "盘亏源仓 = 盘点仓库");

        List<ErpInvStockMoveLine> lines = loadMoveLines(move.getId());
        assertEquals(1, lines.size());
        assertEquals(MATERIAL_LOSS, lines.get(0).getMaterialId());
        assertEquals(0, lines.get(0).getQuantity().compareTo(new BigDecimal("4")),
                "行量 = |差异| = |实盘6 - 账面10| = 4");

        ErpInvStockTakeLine takeLine = findTakeLine(takeId, MATERIAL_LOSS);
        assertEquals(0, takeLine.getDifferenceQuantity().compareTo(new BigDecimal("-4")),
                "回填 differenceQuantity = -4（带符号）");
        assertNull(takeLine.getDifferenceAmount(), "unitCost 为空不回填金额");
    }

    // ---------- ③ 零差异不生成移动单 ----------

    @Test
    public void testZeroDifferenceGeneratesNoMove() {
        seedBalance(MATERIAL_ZERO, new BigDecimal("10"));
        Long takeId = seedTake("TAKE-ZERO-001", MATERIAL_ZERO, bd("10"), bd("10"), null);

        startTake(takeId);
        assertEquals(0, completeTake(takeId).getStatus(), "completeTake 应成功");

        ErpInvStockTake take = findTake(takeId);
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, take.getDocStatus());
        assertEquals(0, findDiffMoves(take.getCode()).size(), "零差异行应跳过，不生成移动单");
        ErpInvStockTakeLine takeLine = findTakeLine(takeId, MATERIAL_ZERO);
        assertEquals(0, takeLine.getDifferenceQuantity().compareTo(BigDecimal.ZERO), "零差异回填 0");
    }

    // ---------- ④ 盘点单本身不改余额（断言④） ----------

    @Test
    public void testTakeItselfDoesNotChangeBalance() {
        seedBalance(MATERIAL_GAIN, new BigDecimal("10"));
        Long takeId = seedTake("TAKE-NOBAL-001", MATERIAL_GAIN, bd("10"), bd("15"), bd("2"));

        startTake(takeId);
        assertEquals(0, completeTake(takeId).getStatus());

        ErpInvStockBalance balance = findBalance(MATERIAL_GAIN);
        assertEquals(0, balance.getTotalQuantity().compareTo(new BigDecimal("10")),
                "断言④：completeTake 后余额不变（盘点单本身不改余额）");
        assertEquals(0, balance.getAvailableQuantity().compareTo(new BigDecimal("10")),
                "断言④：可用量不变（INCOMING 不占预留）");
    }

    // ---------- ⑤ 移动单 DONE 后才影响余额（断言⑤，D2-A 库管员二次确认路径） ----------

    @Test
    public void testBalanceChangesOnlyAfterMoveDone() {
        seedBalance(MATERIAL_GAIN, new BigDecimal("10"));
        Long takeId = seedTake("TAKE-DONE-001", MATERIAL_GAIN, bd("10"), bd("15"), bd("2"));

        startTake(takeId);
        assertEquals(0, completeTake(takeId).getStatus());

        ErpInvStockMove move = findDiffMoves(findTake(takeId).getCode()).get(0);
        assertEquals(0, findBalance(MATERIAL_GAIN).getTotalQuantity().compareTo(new BigDecimal("10")),
                "移动单 CONFIRMED 阶段余额仍不变");

        ApiResponse<?> completeResp = executeRpc(mutation, "ErpInvStockMove__complete",
                ApiRequest.build(Map.of("moveId", move.getId())));
        assertEquals(0, completeResp.getStatus(), "库管员二次确认 complete 应成功");

        ErpInvStockMove after = moveDao().getEntityById(move.getId());
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, after.getDocStatus(), "移动单应 DONE");
        assertEquals(0, findBalance(MATERIAL_GAIN).getTotalQuantity().compareTo(new BigDecimal("15")),
                "断言⑤：移动单 DONE 后余额 +5（10→15）");
        assertFalse(Boolean.TRUE.equals(after.getPosted()), "D3：差异移动单 DONE 不触发存货过账（posted=false）");
    }

    // ---------- ⑥ 幂等/重复 completeTake 守卫 ----------

    @Test
    public void testRepeatedCompleteTakeRejected() {
        seedBalance(MATERIAL_GAIN, new BigDecimal("10"));
        Long takeId = seedTake("TAKE-REPEAT-001", MATERIAL_GAIN, bd("10"), bd("15"), bd("2"));

        startTake(takeId);
        assertEquals(0, completeTake(takeId).getStatus(), "首次 completeTake 成功");

        ApiResponse<?> second = completeTake(takeId);
        assertTrue(second.getStatus() != 0, "重复 completeTake 应被拒绝");
        assertEquals(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION.getErrorCode(), second.getCode(),
                "幂等守卫：CONFIRMED→DONE 单次迁移，DONE 后非法边拒绝");

        assertEquals(1, findDiffMoves(findTake(takeId).getCode()).size(),
                "重复触发不产生第二张差异移动单");
    }

    @Test
    public void testCompleteTakeFromDraftRejected() {
        Long takeId = seedTake("TAKE-DRAFT-001", MATERIAL_GAIN, bd("10"), bd("15"), bd("2"));

        ApiResponse<?> resp = completeTake(takeId);
        assertTrue(resp.getStatus() != 0, "DRAFT 态 completeTake 应被拒绝（须先 startTake）");
        assertEquals(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION.getErrorCode(), resp.getCode());
        assertEquals(0, findDiffMoves(findTake(takeId).getCode()).size(), "拒绝路径零移动单");
    }

    // ---------- ⑦ 部分行失败隔离 + D4-b 告警派发 ----------

    @Test
    public void testPartialLineFailureIsolatedAndAlertDispatched() {
        // 行 1 盘盈（book=0 actual=5 → +5 INCOMING 成功）；行 2 盘亏（book=10 actual=6 → -4 OUTGOING，
        // 无余额 seed → 可用量 0 < 4 → confirm 失败隔离）。config 开启 + ACTIVE 模板 → 告警落库。
        seedBalance(MATERIAL_GAIN, BigDecimal.ZERO);
        setAlertConfig(true);
        seedNotifyTemplate();

        Long takeId = seedTake("TAKE-PARTIAL-001", MATERIAL_GAIN, bd("0"), bd("5"), bd("1"),
                MATERIAL_LOSS, bd("10"), bd("6"), null);
        startTake(takeId);
        assertEquals(0, completeTake(takeId).getStatus(), "单行失败不阻断整单，completeTake 应成功");

        ErpInvStockTake take = findTake(takeId);
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, take.getDocStatus(), "盘点单应 DONE（失败隔离）");

        List<ErpInvStockMove> gainMoves = findDiffMovesByRemark(take.getCode(), "盘盈");
        List<ErpInvStockMove> lossMoves = findDiffMovesByRemark(take.getCode(), "盘亏");
        assertEquals(1, gainMoves.size(), "成功行移动单应生成");
        assertEquals(ErpInvConstants.MOVE_TYPE_INCOMING, gainMoves.get(0).getMoveType());
        assertEquals(0, lossMoves.size(), "失败行孤立 DRAFT 移动单应被同事务补偿删除（强制修正裁决 (a)）");

        ErpInvStockTakeLine gainLine = findTakeLine(takeId, MATERIAL_GAIN);
        assertEquals(0, gainLine.getDifferenceQuantity().compareTo(new BigDecimal("5")),
                "成功行回填 +5");
        ErpInvStockTakeLine lossLine = findTakeLine(takeId, MATERIAL_LOSS);
        assertEquals(0, lossLine.getDifferenceQuantity().compareTo(new BigDecimal("-4")),
                "失败行回填不依赖生成成败（强制修正裁决 (b)）");

        List<ErpSysNotification> notifications = findNotifications(NOTIFY_EVENT);
        assertTrue(notifications.size() > 0, "D4-b：config 开启时生成失败应派发 inv.stocktake-diff-generation-failed 告警");
        assertEquals(RECIPIENT, notifications.get(0).getRecipientUserId(), "接收人应匹配模板 USER_LIST");
    }

    @Test
    public void testPartialLineFailureConfigOffSkipsNotify() {
        setAlertConfig(false);
        seedNotifyTemplate();
        Long takeId = seedTake("TAKE-PARTIAL-002", MATERIAL_GAIN, bd("0"), bd("5"), bd("1"),
                MATERIAL_LOSS, bd("10"), bd("6"), null);

        startTake(takeId);
        assertEquals(0, completeTake(takeId).getStatus());

        assertEquals(1, findDiffMovesByRemark(findTake(takeId).getCode(), "盘盈").size(),
                "失败隔离语义与告警门控解耦：移动单生成行为不变");
        assertEquals(0, findNotifications(NOTIFY_EVENT).size(),
                "D4-b：config 默认 false 时静默跳过告警派发");
    }

    // ---------- ⑧ 回填持久化 + D3 过账跳过（零凭证） ----------

    @Test
    public void testBackfillPersistedAndNoVoucherPosted() {
        seedBalance(MATERIAL_GAIN, new BigDecimal("10"));
        Long takeId = seedTake("TAKE-POST-001", MATERIAL_GAIN, bd("10"), bd("15"), bd("2"));

        startTake(takeId);
        assertEquals(0, completeTake(takeId).getStatus());

        ErpInvStockMove move = findDiffMoves(findTake(takeId).getCode()).get(0);
        // 移动单 DONE（库管员二次确认）后，D3 跳过集生效：resolveBusinessType 返回 null → 零凭证生成
        assertEquals(0, executeRpc(mutation, "ErpInvStockMove__complete",
                ApiRequest.build(Map.of("moveId", move.getId()))).getStatus());

        ErpInvStockMove after = moveDao().getEntityById(move.getId());
        assertEquals(ErpInvConstants.DOC_STATUS_DONE, after.getDocStatus());
        assertFalse(Boolean.TRUE.equals(after.getPosted()),
                "D3：差异移动单 DONE 保持 posted=false（跳过集生效，无 PURCHASE_INPUT/SALES_OUTPUT 误派）");
        assertEquals(0, countBillLinks(after.getCode()), "D3：零凭证生成（零业财回链）");

        ErpInvStockTakeLine takeLine = findTakeLine(takeId, MATERIAL_GAIN);
        assertEquals(0, takeLine.getDifferenceQuantity().compareTo(new BigDecimal("5")),
                "回填 differenceQuantity 持久化");
        assertEquals(0, takeLine.getDifferenceAmount().compareTo(new BigDecimal("10")),
                "回填 differenceAmount 持久化");
    }

    // ---------- helpers ----------

    private void setAlertConfig(boolean enabled) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpInvConstants.CONFIG_STOCKTAKE_DIFF_ALERT_ENABLED, String.valueOf(enabled));
    }

    private void seedNotifyTemplate() {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", 7106L);
            t.setNotificationType(NOTIFY_EVENT);
            t.setName("盘点差异移动单生成失败告警");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("盘点差异移动单生成失败: ${takeCode}");
            t.setBodyTpl("盘点单 ${takeCode} 行 ${lineNo} 差异移动单生成失败：${errorMessage}");
            t.setRecipientResolver("USER_LIST");
            t.setRecipientConfig("{\"userIds\":[\"" + RECIPIENT + "\"]}");
            t.setMergeWindowSeconds(60);
            t.setMergeStrategy("MERGE_BY_USER_TYPE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
            return null;
        });
    }

    private Long seedTake(String code, Long materialId, BigDecimal book, BigDecimal actual, BigDecimal unitCost) {
        return seedTake(code, materialId, book, actual, unitCost, null, null, null, null);
    }

    private Long seedTake(String code, Long materialId1, BigDecimal book1, BigDecimal actual1, BigDecimal unitCost1,
                          Long materialId2, BigDecimal book2, BigDecimal actual2, BigDecimal unitCost2) {
        List<Long> ids = new ArrayList<>();
        ormTemplate.runInSession(session -> {
            ErpInvStockTake take = takeDao().newEntity();
            take.setCode(code);
            take.setOrgId(ORG_ID);
            take.setBusinessDate(LocalDate.of(2026, 8, 1));
            take.setWarehouseId(WAREHOUSE_ID);
            take.setDocStatus(ErpInvConstants.DOC_STATUS_DRAFT);
            take.setApproveStatus(ErpInvConstants.APPROVE_STATUS_UNSUBMITTED);
            takeDao().saveEntity(take);
            ids.add(take.getId());

            seedTakeLine(take.getId(), 1, materialId1, book1, actual1, unitCost1);
            if (materialId2 != null) {
                seedTakeLine(take.getId(), 2, materialId2, book2, actual2, unitCost2);
            }
            return null;
        });
        return ids.get(0);
    }

    private void seedTakeLine(Long takeId, int lineNo, Long materialId,
                              BigDecimal book, BigDecimal actual, BigDecimal unitCost) {
        IEntityDao<ErpInvStockTakeLine> dao = daoProvider.daoFor(ErpInvStockTakeLine.class);
        ErpInvStockTakeLine line = dao.newEntity();
        line.setTakeId(takeId);
        line.setLineNo(lineNo);
        line.setMaterialId(materialId);
        line.setUoMId(UOM_ID);
        line.setBookQuantity(book);
        line.setActualQuantity(actual);
        line.setDifferenceQuantity(BigDecimal.ZERO);
        line.setUnitCost(unitCost);
        dao.saveEntity(line);
    }

    private void seedBalance(Long materialId, BigDecimal total) {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = dao.newEntity();
            b.setOrgId(ORG_ID);
            b.setMaterialId(materialId);
            b.setWarehouseId(WAREHOUSE_ID);
            b.setTotalQuantity(total);
            b.setReservedQuantity(BigDecimal.ZERO);
            b.setLockedQuantity(BigDecimal.ZERO);
            b.setAvailableQuantity(total);
            b.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
            b.setAvgCost(BigDecimal.ZERO);
            b.setTotalCost(BigDecimal.ZERO);
            b.setOwnershipType(ErpInvConstants.OWNERSHIP_TYPE_OWNED);
            dao.saveEntity(b);
            return null;
        });
    }

    private ApiResponse<?> startTake(Long takeId) {
        return executeRpc(mutation, "ErpInvStockTake__startTake",
                ApiRequest.build(Map.of("takeId", takeId)));
    }

    private ApiResponse<?> completeTake(Long takeId) {
        return executeRpc(mutation, "ErpInvStockTake__completeTake",
                ApiRequest.build(Map.of("takeId", takeId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpInvStockTake findTake(Long takeId) {
        return takeDao().getEntityById(takeId);
    }

    private ErpInvStockTakeLine findTakeLine(Long takeId, Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("takeId", takeId));
        q.addFilter(eq("materialId", materialId));
        List<ErpInvStockTakeLine> list = takeLineDao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvStockMove> findDiffMoves(String takeCode) {
        return findDiffMovesByRemark(takeCode, null);
    }

    private List<ErpInvStockMove> findDiffMovesByRemark(String takeCode, String kind) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpInvConstants.RELATED_BILL_TYPE_STOCK_TAKE));
        List<ErpInvStockMove> all = moveDao().findAllByQuery(q);
        List<ErpInvStockMove> result = new ArrayList<>();
        for (ErpInvStockMove move : all) {
            if (move.getRemark() == null || !move.getRemark().contains(takeCode)) {
                continue;
            }
            if (kind == null || move.getRemark().contains(kind)) {
                result.add(move);
            }
        }
        return result;
    }

    private List<ErpInvStockMoveLine> loadMoveLines(Long moveId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return new ArrayList<>(moveLineDao().findAllByQuery(q));
    }

    private ErpInvStockBalance findBalance(Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = balanceDao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpSysNotification> findNotifications(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        q.addOrderField("createTime", true);
        return new ArrayList<>(daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q));
    }

    private long countBillLinks(String moveCode) {
        List<ErpFinVoucherBillR> links = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(
                new QueryBean());
        return links.stream().filter(l -> moveCode.equals(l.getBillCode())).count();
    }

    private IEntityDao<ErpInvStockTake> takeDao() {
        return daoProvider.daoFor(ErpInvStockTake.class);
    }

    private IEntityDao<ErpInvStockTakeLine> takeLineDao() {
        return daoProvider.daoFor(ErpInvStockTakeLine.class);
    }

    private IEntityDao<ErpInvStockMove> moveDao() {
        return daoProvider.daoFor(ErpInvStockMove.class);
    }

    private IEntityDao<ErpInvStockMoveLine> moveLineDao() {
        return daoProvider.daoFor(ErpInvStockMoveLine.class);
    }

    private IEntityDao<ErpInvStockBalance> balanceDao() {
        return daoProvider.daoFor(ErpInvStockBalance.class);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
