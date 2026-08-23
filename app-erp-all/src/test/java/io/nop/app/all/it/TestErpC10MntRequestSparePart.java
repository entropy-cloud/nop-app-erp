package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * B5 C10：维护工单与备件消耗过账（按 {@code docs/design/integration-testing.md §6 C10} 规格）。
 *
 * <p>全链（自包含请求/访问/消耗单/备件物料，引用部署 seed：设备 EQ-2026-001（id=1，assetId=2→AST-2026-002）、
 * 组织 2 / 仓库 2 / CNY 币种 1 / 2026-07 OPEN 期间 + 科目 6602/1403 + 账套 1）：自包含维护请求 save
 * （OPEN，引用 seed 设备 EQ-2026-001）→
 * 非法迁移守卫（OPEN 直接 startRepair 拒 {@code erp.err.mnt.request-illegal-status-transition}）→
 * {@code accept}（OPEN→ACCEPTED，响应式生成 DRAFT RESPONSIVE Visit 且回填 requestId）→
 * {@code startRepair}（IN_PROGRESS）→ {@code complete}（六态状态机终态 COMPLETED）→
 * {@code ErpMntVisit__schedule}（DRAFT→SCHEDULED）→ {@code start}（SCHEDULED→IN_PROGRESS，
 * 设备置 UNDER_MAINTENANCE）→ {@code complete}（IN_PROGRESS→COMPLETED，设备恢复 RUNNING；
 * 关联请求已 COMPLETED 终态，D6 写回 no-op）→ 自包含备件物料 M（MOVING_AVERAGE）+ 入库（10 @ 5）→
 * 自包含备件消耗单 + 行（M ×2）→
 * {@code ErpMntSparePartUsage__confirm}（@NopTestProperty 开启过账门控 → OUTGOING 库存出库 +
 * MAINTENANCE_ISSUE 凭证 Dr 6602/Cr 1403 = 2×5=10 + 余额 10→8）→
 * {@code ErpAstAsset__get}（设备关联资产卡片 AST-2026-002 核对）。
 *
 * <p>Phase 2 Decision（两裁决，落盘设计文档 §6 C10 勘误）：
 * <ol>
 *   <li><b>过账门控配置</b>：`erp-mnt.spare-part-posting-enabled` 默认 false（实仓
 *       {@code DEFAULT_SPARE_PART_POSTING_ENABLED=false}，MaintenanceIssuePostingDispatcher 注释「关闭时仅
 *       库存出库，不生成凭证」）→ 类级 {@code @NopTestProperty} 开启断言 MAINTENANCE_ISSUE 凭证
 *       （对齐 C08 simulation 门控同型处理）；门控默认关闭态下凭证不存在，不作断言源（残留风险记录）。</li>
 *   <li><b>visit_task 漂移</b>：设计文档 §6 C10 步骤 2「访问完成 + visit_task」不成立——实仓
 *       {@code ErpMntVisitTask} 仅由 {@code ScheduleDueGenerator} 对 PLANNED 访问（计划到期生成）套用任务模板
 *       逐行创建；accept 生成的 RESPONSIVE 访问明确不套任务模板（{@code ErpMntRequestAcceptProcessor} javadoc）
 *       → 断言替换为 visit complete 侧语义：访问终态 COMPLETED + 设备状态恢复 RUNNING + 关联请求终态
 *       COMPLETED 写回 no-op（D6 联动，{@code ErpMntVisitCompleteProcessor}）。</li>
 *   <li><b>备件库存数据来源</b>：seed MAT-003（WEIGHTED_AVERAGE）余额行无 location 与出库路径
 *       location=sourceWarehouseId 不匹配（B4 C07 同型裁决）→ 备件物料/库存自包含建数
 *       （MOVING_AVERAGE M + generateMove 入库，TestErpMntSparePartPosting 同型），未触发 seed 修正授权。</li>
 * </ol>
 *
 * <p>三层验证：层 1 = JUnit 关键断言（错误码守卫 + 六态状态机终态 + MAINTENANCE_ISSUE 凭证借贷平衡 +
 * 备件库存扣减 + 设备状态/资产联动）；层 2 = 每步 response 快照；层 3 = output/tables 变更行。
 * RECORDING→CHECKING 往返已按 M0.2 试点范式完成（录制后切回默认 CHECKING 复跑全绿）。冻结时钟
 * 2026-07-17（复用 C07 扩展）保证 accept 生成 Visit 的 visitDate 等自动日期列确定性。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-mnt.spare-part-posting-enabled", value = "true")
public class TestErpC10MntRequestSparePart extends ErpIntegrationTestCase {

    static final String REQ_CODE = "IT-C10-REQ-001";
    static final String SPU_CODE = "IT-C10-SPU-001";
    static final String EQUIPMENT_ID = "1";     // seed EQ-2026-001（assetId=2）
    static final String M_CODE = "IT-C10-M-001";
    static final String WAREHOUSE_ID = "2";     // seed WH-MAIN
    static final String UOM_ID = "1";
    static final BigDecimal UNIT_COST = new BigDecimal("5");
    static final BigDecimal USED_QTY = new BigDecimal("2");
    static final BigDecimal POSTING_AMOUNT = new BigDecimal("10"); // 2 × 5

    @RegisterExtension
    static C07FrozenClockExtension frozenClock = new C07FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testMntRequestSparePartClosedLoop() {
        // ---------- 1. 自包含维护请求 save（OPEN，引用 seed 设备 EQ-2026-001） ----------
        ApiResponse<?> reqSave = rpcMutation("ErpMntRequest__save", request("1_request_save.json5", Map.class));
        output("1_request_save_response.json5", reqSave);
        assertEquals(0, reqSave.getStatus(), "维护请求保存应成功");
        String reqId = idOf(reqSave);
        addVar("reqId", reqId);
        addVar("reqCode", REQ_CODE);

        // 非法迁移守卫：OPEN 直接 startRepair 应拒
        ApiResponse<?> illegal = rpcMutation("ErpMntRequest__startRepair",
                request("2_request_start_repair_illegal.json5", Map.class));
        output("2_request_start_repair_illegal_response.json5", illegal);
        assertEquals("erp.err.mnt.request-illegal-status-transition", illegal.getCode(),
                "OPEN 直接 startRepair 应拒（非法迁移守卫）");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_OPEN, reloadRequest(reqId).getStatus(),
                "守卫拒绝后请求保持 OPEN");

        // ---------- 2. accept（OPEN→ACCEPTED + 响应式生成 DRAFT Visit） ----------
        ApiResponse<?> accept = rpcMutation("ErpMntRequest__accept", request("3_request_accept.json5", Map.class));
        output("3_request_accept_response.json5", accept);
        assertEquals(0, accept.getStatus(), "受理应成功");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, reloadRequest(reqId).getStatus(),
                "accept 后 request ACCEPTED");

        // 层 1 锚点：响应式访问 DRAFT/RESPONSIVE + requestId 回填 + 设备关联
        ErpMntVisit visit = findVisitByCode("VST-REQ-" + reqId);
        assertNotNull(visit, "accept 应生成响应式维护访问");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_DRAFT, visit.getStatus(), "生成访问 DRAFT");
        assertEquals(ErpMntDaoConstants.VISIT_TYPE_RESPONSIVE, visit.getVisitType(), "访问类型 RESPONSIVE");
        assertEquals(reqId, visit.getRequestId(), "访问回填 requestId");
        assertEquals(EQUIPMENT_ID, visit.getEquipmentId(), "访问关联设备 EQ-2026-001");
        assertEquals(LocalDate.of(2026, 7, 17), visit.getVisitDate(), "访问日期 = 冻结参考日 2026-07-17");
        String visitId = visit.getId();
        addVar("visitId", visitId);

        // ---------- 3. startRepair → complete（六态状态机终态） ----------
        ApiResponse<?> startRepair = rpcMutation("ErpMntRequest__startRepair",
                request("4_request_start_repair.json5", Map.class));
        output("4_request_start_repair_response.json5", startRepair);
        assertEquals(0, startRepair.getStatus(), "开始维修应成功");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS, reloadRequest(reqId).getStatus(),
                "startRepair 后 IN_PROGRESS");

        ApiResponse<?> completeReq = rpcMutation("ErpMntRequest__complete",
                request("5_request_complete.json5", Map.class));
        output("5_request_complete_response.json5", completeReq);
        assertEquals(0, completeReq.getStatus(), "请求完成应成功");
        ErpMntRequest doneReq = reloadRequest(reqId);
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_COMPLETED, doneReq.getStatus(),
                "complete 后六态状态机终态 COMPLETED");
        assertNotNull(doneReq.getCompletedAt(), "完成时间已记录");

        // ---------- 4. 访问 schedule → start → complete（设备状态联动） ----------
        ApiResponse<?> schedule = rpcMutation("ErpMntVisit__schedule", request("6_visit_schedule.json5", Map.class));
        output("6_visit_schedule_response.json5", schedule);
        assertEquals(0, schedule.getStatus(), "访问排程应成功");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_SCHEDULED, reloadVisit(visitId).getStatus(),
                "schedule 后 SCHEDULED");

        ApiResponse<?> startVisit = rpcMutation("ErpMntVisit__start", request("7_visit_start.json5", Map.class));
        output("7_visit_start_response.json5", startVisit);
        assertEquals(0, startVisit.getStatus(), "访问开工应成功");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, reloadVisit(visitId).getStatus(),
                "start 后 IN_PROGRESS");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE, equipmentStatus(),
                "访问执行中设备置 UNDER_MAINTENANCE");

        ApiResponse<?> completeVisit = rpcMutation("ErpMntVisit__complete", request("8_visit_complete.json5", Map.class));
        output("8_visit_complete_response.json5", completeVisit);
        assertEquals(0, completeVisit.getStatus(), "访问完成应成功");
        ErpMntVisit doneVisit = reloadVisit(visitId);
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, doneVisit.getStatus(), "访问终态 COMPLETED");
        assertNotNull(doneVisit.getEndTime(), "访问结束时间已记录");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipmentStatus(), "访问完成恢复设备 RUNNING");

        // ---------- 5. 自包含备件物料 M + 入库（10 @ 5）→ 备件消耗单 + 行 → confirm（过账门控开启） ----------
        ApiResponse<?> matSave = rpcMutation("ErpMdMaterial__save", request("9_material_save.json5", Map.class));
        output("9_material_save_response.json5", matSave);
        assertEquals(0, matSave.getStatus(), "备件物料保存应成功");
        addVar("mId", idOf(matSave));

        ApiResponse<?> incomingMove = rpcMutation("ErpInvStockMove__generateMove",
                request("10_m_incoming_move.json5", Map.class));
        output("10_m_incoming_move_response.json5", incomingMove);
        assertEquals(0, incomingMove.getStatus(), "备件物料入库移动应成功");

        ApiResponse<?> usageSave = rpcMutation("ErpMntSparePartUsage__save", request("11_usage_save.json5", Map.class));
        output("11_usage_save_response.json5", usageSave);
        assertEquals(0, usageSave.getStatus(), "备件消耗单保存应成功");
        String usageId = idOf(usageSave);
        addVar("usageId", usageId);

        ApiResponse<?> usageLine = rpcMutation("ErpMntSparePartUsageLine__save",
                request("12_usage_line_save.json5", Map.class));
        output("12_usage_line_save_response.json5", usageLine);
        assertEquals(0, usageLine.getStatus(), "备件消耗行保存应成功");

        ApiResponse<?> confirm = rpcMutation("ErpMntSparePartUsage__confirm",
                request("13_usage_confirm.json5", Map.class));
        output("13_usage_confirm_response.json5", confirm);
        assertEquals(0, confirm.getStatus(), "备件消耗确认应成功");

        // 层 1 锚点：posted=true + OUTGOING 出库移动 + 库存扣减 10→8
        ErpMntSparePartUsage usage = daoProvider.daoFor(ErpMntSparePartUsage.class).getEntityById(usageId);
        assertEquals(Boolean.TRUE, usage.getPosted(), "备件消耗确认后 posted=true");
        ErpInvStockMove move = findStockMove(ErpMntConstants.RELATED_BILL_TYPE_MNT_SPARE_PART, SPU_CODE);
        assertNotNull(move, "备件消耗应生成出库移动");
        assertEquals("OUTGOING", move.getMoveType(), "备件消耗移动 MOVE_TYPE=OUTGOING");
        ErpInvStockBalance balance = stockBalance(idOf(matSave), WAREHOUSE_ID);
        assertNotNull(balance, "备件物料库存余额应存在");
        assertEquals(0, new BigDecimal("8").compareTo(balance.getTotalQuantity()),
                "备件库存扣减：入库 10 − 消耗 2 = 8");

        // 层 1 锚点：MAINTENANCE_ISSUE 凭证借贷平衡（Dr 6602 维修费用 / Cr 1403 存货 = 17）
        ErpFinVoucherBillR link = findMntVoucherLink(SPU_CODE + "-MI");
        assertNotNull(link, "备件消耗应生成 MAINTENANCE_ISSUE 凭证回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "MAINTENANCE_ISSUE 凭证应落库");
        assertEquals(0, POSTING_AMOUNT.compareTo(voucher.getTotalDebit()), "MAINTENANCE_ISSUE 借方合计=10");
        assertEquals(0, POSTING_AMOUNT.compareTo(voucher.getTotalCredit()), "MAINTENANCE_ISSUE 贷方合计=10");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "MAINTENANCE_ISSUE 凭证已过账");
        List<ErpFinVoucherLine> lines = findVoucherLines(voucher.getId());
        ErpFinVoucherLine expenseDebit = lines.stream()
                .filter(l -> "6602".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine invCredit = lines.stream()
                .filter(l -> "1403".equals(l.getSubjectCode())).findFirst().orElse(null);
        assertNotNull(expenseDebit, "MAINTENANCE_ISSUE 凭证应含 6602 维修费用行");
        assertNotNull(invCredit, "MAINTENANCE_ISSUE 凭证应含 1403 存货行");
        assertEquals("DEBIT", expenseDebit.getDcDirection(), "维修费用借方方向");
        assertEquals(0, POSTING_AMOUNT.compareTo(expenseDebit.getDebitAmount()), "维修费用借方=10");
        assertEquals("CREDIT", invCredit.getDcDirection(), "存货贷方方向");
        assertEquals(0, POSTING_AMOUNT.compareTo(invCredit.getCreditAmount()), "存货贷方=10");
        assertEquals(2, lines.size(), "MAINTENANCE_ISSUE 凭证 2 行");

        // ---------- 6. ErpAstAsset__get（设备关联资产卡片 AST-2026-002 核对） ----------
        ApiResponse<?> assetGet = executeRpc(GraphQLOperationType.query, "ErpAstAsset__get",
                request("12_asset_get.json5", Map.class));
        output("12_asset_get_response.json5", assetGet);
        assertEquals(0, assetGet.getStatus(), "资产卡片查询应成功");

        // 层 1 锚点：设备 EQ-2026-001.assetId=2 → AST-2026-002 卡片（code/name/status）
        Map<?, ?> assetData = (Map<?, ?>) assetGet.getData();
        assertEquals("AST-2026-002", String.valueOf(assetData.get("code")), "设备关联资产 code=AST-2026-002");
        assertEquals("演示用数控机床", String.valueOf(assetData.get("name")), "设备关联资产名称核对");
        assertEquals("IN_SERVICE", String.valueOf(assetData.get("status")), "设备关联资产状态 IN_SERVICE");
    }

    // ---------- helpers ----------

    private ErpMntRequest reloadRequest(String requestId) {
        return daoProvider.daoFor(ErpMntRequest.class).getEntityById(requestId);
    }

    private ErpMntVisit reloadVisit(String visitId) {
        return daoProvider.daoFor(ErpMntVisit.class).getEntityById(visitId);
    }

    private String equipmentStatus() {
        return daoProvider.daoFor(ErpMntEquipment.class).getEntityById(EQUIPMENT_ID).getStatus();
    }

    private ErpMntVisit findVisitByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMntVisit> list = daoProvider.daoFor(ErpMntVisit.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockBalance stockBalance(String materialId, String warehouseId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        List<ErpInvStockBalance> list = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinVoucherBillR findMntVoucherLink(String billHeadCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billHeadCode));
        List<ErpFinVoucherBillR> list = daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }
}