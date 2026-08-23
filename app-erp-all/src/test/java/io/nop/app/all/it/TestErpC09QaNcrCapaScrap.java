package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B5 C09：质检门控与 NCR/CAPA/SCRAP 闭环（按 {@code docs/design/integration-testing.md §6 C09} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / WH-MAIN 仓库 2 / CNY 币种 1 / 2026-07 OPEN 期间 +
 * 科目 1401/6711 + 账套 1）：自包含产成品 P（MOVING_AVERAGE——SCRAP 过账经余额 avgCost 取价，
 * FIFO 余额 avgCost 恒 null 致凭证金额 0，实施期发现）+ 自包含 BOM（{@code inspectionRequired=true}，
 * 无子件行——齐套校验空需求即 STOCK_RESERVED）→ 自包含 P 入库（10 @ 5，报废过账成本基数）→ 自包含工单
 * （productId=P, bomId）save → submitForApproval → approve（DIRECT）→ checkAvailability → start（IN_PROCESS）
 * → {@code reportCompletion}(1) 被完工质检门控阻断（{@code erp-mfg.inspection-gate-enabled=true} +
 * BOM inspectionRequired → {@code erp.err.mfg.work-order.inspection-required}，WO 保持 IN_PROCESS 零完工）
 * → {@code close} 放行（CLOSED 结案，证明门控点 = reportCompletion 而非 close）→ 自包含质检单
 * （PENDING，relatedBillType=ERP_MFG_WORK_ORDER 回链工单）→ {@code recordResult} 实测 5 < 规格下限 10 →
 * REJECTED（自动生成 NCR，lotQuantity=2 驱动 quantity）+ {@code isInspectionCleared}=false →
 * NCR submitReview（OPEN→IN_REVIEW）→ resolve 门控双侧：无 CAPA 缺 noCapaReason 拒
 * {@code erp.err.qa.ncr.resolve-no-capa}；CAPA PENDING/COMPLETED 未验证拒
 * {@code erp.err.qa.ncr.resolve-capa-not-completed}；CAPA 三步 startAction/completeAction/verifyAction 后
 * resolve → RESOLVED → 置 dispositionType=SCRAP → {@code postNcr}（NCR_SCRAP 凭证 Dr 6711/Cr 1401 =
 * 2×5=10，posted=true）→ notify 域可达断言路径（自包含模板 + {@code ErpSysNotification__notify} →
 * markRead → countUnread 归零）。
 *
 * <p>Phase 1 Decision（三裁决，落盘设计文档 §6 C09 勘误）：
 * <ol>
 *   <li><b>完工门控阻断点</b>：设计文档 §6 C09 步骤 1 原述「{@code ErpMfgWorkOrder__close} 被拒」为漂移——
 *       实仓唯一抛点 = {@code ErpMfgWorkOrderReportCompletionProcessor}（reportCompletion，completedQty
 *       达计划量且 {@code isInspectionGated} 时抛 {@code erp.err.mfg.work-order.inspection-required}）；
 *       close 为 STOPPED/IN_PROCESS→CLOSED 结案动作（B4 C07 同型裁决）。用例按实仓落地：reportCompletion
 *       阻断 + close 放行双面证明。</li>
 *   <li><b>NCR 通知路径</b>：实仓 QA 域主代码无 NCR 状态变更 → {@code ErpSysNotification} 直连/订阅机制
 *       （grep 实证），设计文档 §6 C09 步骤 4「NCR 状态变更触发 ErpSysNotification 生成（markRead 断言）」
 *       为未实现动作面 → 勘误登记 + 改以 notify 域既有机制作可达断言路径：自包含通知模板 +
 *       {@code ErpSysNotification__notify}（NCR 语境 eventType）→ 站内通知落库 → markRead →
 *       countUnread 归零（不强行实现不存在的自动触发链路）。</li>
 *   <li><b>SCRAP 库存扣减</b>：设计文档 §6 C09 层 1「库存扣减数量正确」不成立——实仓
 *       {@code NcrPostingDispatcher} 明确不扣物理库存（报废存货出库仅经凭证贷方 1401 表达，物理余额扣减
 *       属 inventory 域 successor 避免与 SALES_OUTPUT 双计）→ 断言替换为凭证借贷平衡 + 金额 = quantity ×
 *       avgCost + 凭证行科目方向。</li>
 * </ol>
 *
 * <p>三层验证：层 1 = JUnit 关键断言（错误码三处 + 状态机 + 凭证借贷平衡 + 通知已读归零）；层 2 = 每步
 * response 快照；层 3 = output/tables 变更行。RECORDING→CHECKING 往返已按 M0.2 试点范式完成（录制后切回
 * 默认 CHECKING 复跑全绿）。冻结时钟 2026-07-17（复用 C07 扩展）保证自动生成日期列确定性。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-mfg.inspection-gate-enabled", value = "true")
@NopTestProperty(name = "erp-qua.ncr-default-acct-schema", value = "1")
public class TestErpC09QaNcrCapaScrap extends ErpIntegrationTestCase {

    static final String P_CODE = "IT-C09-P-001";
    static final String BOM_CODE = "IT-C09-BOM-001";
    static final String WO_CODE = "IT-C09-WO-001";
    static final String INS_CODE = "IT-C09-INS-001";
    static final String NOTIFY_USER = "it-c09-user";
    static final Long VERIFICATION_PERSON = 7501L;
    static final BigDecimal SCRAP_AMOUNT = new BigDecimal("10"); // 2 × avgCost 5

    @RegisterExtension
    static C07FrozenClockExtension frozenClock = new C07FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testQaNcrCapaScrapClosedLoop() {
        // ---------- 1. 自包含产成品 P + BOM（inspectionRequired=true，无子件行） ----------
        ApiResponse<?> matPSave = rpcMutation("ErpMdMaterial__save", request("1_material_p_save.json5", Map.class));
        output("1_material_p_save_response.json5", matPSave);
        assertEquals(0, matPSave.getStatus(), "产成品物料保存应成功");
        addVar("pId", idOf(matPSave));

        ApiResponse<?> bomSave = rpcMutation("ErpMfgBom__save", request("2_bom_save.json5", Map.class));
        output("2_bom_save_response.json5", bomSave);
        assertEquals(0, bomSave.getStatus(), "BOM 保存应成功");
        addVar("bomId", idOf(bomSave));

        // 自包含 P 入库（10 @ 5）——NCR SCRAP 过账的成本基数（avgCost=5）
        ApiResponse<?> incomingMove = rpcMutation("ErpInvStockMove__generateMove", request("3_p_incoming_move.json5", Map.class));
        output("3_p_incoming_move_response.json5", incomingMove);
        assertEquals(0, incomingMove.getStatus(), "P 入库移动应成功");

        // ---------- 2. 自包含工单 save → submitForApproval → approve → checkAvailability → start ----------
        ApiResponse<?> woSave = rpcMutation("ErpMfgWorkOrder__save", request("4_wo_save.json5", Map.class));
        output("4_wo_save_response.json5", woSave);
        assertEquals(0, woSave.getStatus(), "工单保存应成功");
        String woId = idOf(woSave);
        addVar("woId", woId);
        addVar("woCode", WO_CODE);

        ApiResponse<?> woSubmit = rpcMutation("ErpMfgWorkOrder__submitForApproval", request("5_wo_submit.json5", Map.class));
        output("5_wo_submit_response.json5", woSubmit);
        assertEquals(0, woSubmit.getStatus(), "工单提交审批应成功");

        ApiResponse<?> woApprove = rpcMutation("ErpMfgWorkOrder__approve", request("6_wo_approve.json5", Map.class));
        output("6_wo_approve_response.json5", woApprove);
        assertEquals(0, woApprove.getStatus(), "工单审批应成功");

        ApiResponse<?> woCheck = rpcMutation("ErpMfgWorkOrder__checkAvailability", request("7_wo_check_availability.json5", Map.class));
        output("7_wo_check_availability_response.json5", woCheck);
        assertEquals(0, woCheck.getStatus(), "齐套校验应成功");
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED,
                reload(ErpMfgWorkOrder.class, woId).getDocStatus(), "齐套校验后 docStatus=STOCK_RESERVED");

        ApiResponse<?> woStart = rpcMutation("ErpMfgWorkOrder__start", request("8_wo_start.json5", Map.class));
        output("8_wo_start_response.json5", woStart);
        assertEquals(0, woStart.getStatus(), "开工应成功");
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS,
                reload(ErpMfgWorkOrder.class, woId).getDocStatus(), "开工后 docStatus=IN_PROCESS");

        // ---------- 3. 完工门控负路径：reportCompletion 达计划量被质检门控阻断 ----------
        ApiResponse<?> blocked = rpcMutation("ErpMfgWorkOrder__reportCompletion",
                request("9_report_completion.json5", Map.class));
        output("9_report_completion_response.json5", blocked);
        assertEquals("erp.err.mfg.work-order.inspection-required", blocked.getCode(),
                "BOM inspectionRequired + 门控开启 → reportCompletion 阻断 ERR_INSPECTION_REQUIRED");

        // 层 1 锚点：WO 保持 IN_PROCESS 零完工、无完工入库移动
        ErpMfgWorkOrder gatedWo = reload(ErpMfgWorkOrder.class, woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, gatedWo.getDocStatus(),
                "门控阻断后工单保持 IN_PROCESS");
        assertEquals(0, gatedWo.getCompletedQuantity().compareTo(BigDecimal.ZERO), "门控阻断后完工数量=0");
        assertNull(findStockMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, WO_CODE),
                "门控阻断后无完工入库移动");

        // 门控点裁决证据：close（结案动作）放行 —— 门控仅挂在 reportCompletion
        ApiResponse<?> woClose = rpcMutation("ErpMfgWorkOrder__close", request("10_wo_close.json5", Map.class));
        output("10_wo_close_response.json5", woClose);
        assertEquals(0, woClose.getStatus(), "close 结案应成功（非门控点）");
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_CLOSED, reload(ErpMfgWorkOrder.class, woId).getDocStatus(),
                "close 后 docStatus=CLOSED");

        // ---------- 4. 自包含质检单（PENDING）→ recordResult REJECTED（自动生成 NCR） ----------
        ApiResponse<?> insSave = rpcMutation("ErpQaInspection__save", request("11_inspection_save.json5", Map.class));
        output("11_inspection_save_response.json5", insSave);
        assertEquals(0, insSave.getStatus(), "质检单保存应成功");
        String insId = idOf(insSave);
        addVar("insId", insId);
        addVar("insCode", INS_CODE);

        ApiResponse<?> insLine = rpcMutation("ErpQaInspectionLine__save", request("12_inspection_line_save.json5", Map.class));
        output("12_inspection_line_save_response.json5", insLine);
        assertEquals(0, insLine.getStatus(), "质检行保存应成功");

        ApiResponse<?> recordResult = rpcMutation("ErpQaInspection__recordResult", request("13_record_result.json5", Map.class));
        output("13_record_result_response.json5", recordResult);
        assertEquals(0, recordResult.getStatus(), "质检结果录入应成功");
        assertEquals(ErpQaConstants.INSPECTION_RESULT_REJECTED,
                reload(ErpQaInspection.class, insId).getResult(), "实测 5 < 规格下限 10 → REJECTED");

        // REJECTED 自动生成 NCR（sourceType=INSPECTION / sourceCode=质检单号 / quantity=lotQuantity=2）
        ErpQaNonConformance autoNcr = findNcrBySourceCode(INS_CODE);
        assertNotNull(autoNcr, "REJECTED 应自动生成 NCR");
        assertEquals(ErpQaConstants.NCR_STATUS_OPEN, autoNcr.getStatus(), "自动 NCR 状态 OPEN");
        assertEquals(ErpQaConstants.NCR_SOURCE_TYPE_INSPECTION, autoNcr.getSourceType(), "NCR 来源 INSPECTION");
        assertEquals(0, new BigDecimal("2").compareTo(autoNcr.getQuantity()), "NCR 数量 = lotQuantity 2");
        String ncrId = autoNcr.getId();
        addVar("ncrId", ncrId);

        // 层 1 锚点：REJECTED 检验未清 → isInspectionCleared=false
        ApiResponse<?> cleared = executeRpc(GraphQLOperationType.query, "ErpQaInspection__isInspectionCleared",
                request("14_is_inspection_cleared.json5", Map.class));
        output("14_is_inspection_cleared_response.json5", cleared);
        assertEquals(0, cleared.getStatus(), "isInspectionCleared 查询应成功");
        assertFalse(Boolean.TRUE.equals(cleared.getData()), "REJECTED 检验未清 → isInspectionCleared=false");

        // ---------- 5. NCR submitReview → resolve 门控双侧 → CAPA 三步闭包 → RESOLVED ----------
        ApiResponse<?> submitReview = rpcMutation("ErpQaNonConformance__submitReview",
                request("15_ncr_submit_review.json5", Map.class));
        output("15_ncr_submit_review_response.json5", submitReview);
        assertEquals(0, submitReview.getStatus(), "NCR 提交评审应成功");
        assertEquals(ErpQaConstants.NCR_STATUS_IN_REVIEW, reloadNcr(ncrId).getStatus(), "submitReview 后 IN_REVIEW");

        // 无 CAPA + 缺 noCapaReason → ERR_NCR_RESOLVE_NO_CAPA
        ApiResponse<?> noCapa = rpcMutation("ErpQaNonConformance__resolve", request("16_ncr_resolve_no_capa.json5", Map.class));
        output("16_ncr_resolve_no_capa_response.json5", noCapa);
        assertEquals("erp.err.qa.ncr.resolve-no-capa", noCapa.getCode(), "无 CAPA 缺 noCapaReason 拒");

        // CAPA save（PENDING）→ resolve 仍拒（ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED）
        ApiResponse<?> actionSave = rpcMutation("ErpQaAction__save", request("17_action_save.json5", Map.class));
        output("17_action_save_response.json5", actionSave);
        assertEquals(0, actionSave.getStatus(), "CAPA 措施保存应成功");
        String actionId = idOf(actionSave);
        addVar("actionId", actionId);

        ApiResponse<?> capaBlocked = rpcMutation("ErpQaNonConformance__resolve",
                request("18_ncr_resolve_capa_pending.json5", Map.class));
        output("18_ncr_resolve_capa_pending_response.json5", capaBlocked);
        assertEquals("erp.err.qa.ncr.resolve-capa-not-completed", capaBlocked.getCode(),
                "CAPA PENDING 未完成 → resolve 拒");

        // CAPA 三步：startAction → completeAction → verifyAction
        ApiResponse<?> startAction = rpcMutation("ErpQaAction__startAction", request("19_action_start.json5", Map.class));
        output("19_action_start_response.json5", startAction);
        assertEquals(0, startAction.getStatus(), "CAPA 启动应成功");
        assertEquals(ErpQaConstants.ACTION_STATUS_IN_PROGRESS, reloadAction(actionId).getStatus(),
                "startAction 后 IN_PROGRESS");

        ApiResponse<?> completeAction = rpcMutation("ErpQaAction__completeAction", request("20_action_complete.json5", Map.class));
        output("20_action_complete_response.json5", completeAction);
        assertEquals(0, completeAction.getStatus(), "CAPA 完成应成功");
        assertEquals(ErpQaConstants.ACTION_STATUS_COMPLETED, reloadAction(actionId).getStatus(),
                "completeAction 后 COMPLETED");

        // 完成但缺效果验证 → resolve 仍拒
        ApiResponse<?> unverified = rpcMutation("ErpQaNonConformance__resolve",
                request("21_ncr_resolve_unverified.json5", Map.class));
        output("21_ncr_resolve_unverified_response.json5", unverified);
        assertEquals("erp.err.qa.ncr.resolve-capa-not-completed", unverified.getCode(),
                "CAPA COMPLETED 缺验证 → resolve 拒");

        ApiResponse<?> verifyAction = rpcMutation("ErpQaAction__verifyAction", request("22_action_verify.json5", Map.class));
        output("22_action_verify_response.json5", verifyAction);
        assertEquals(0, verifyAction.getStatus(), "CAPA 效果验证应成功");

        ApiResponse<?> resolve = rpcMutation("ErpQaNonConformance__resolve", request("23_ncr_resolve.json5", Map.class));
        output("23_ncr_resolve_response.json5", resolve);
        assertEquals(0, resolve.getStatus(), "CAPA 全闭 + 验证后 resolve 应成功");
        assertEquals(ErpQaConstants.NCR_STATUS_RESOLVED, reloadNcr(ncrId).getStatus(), "resolve 后 RESOLVED");
        assertNotNull(reloadNcr(ncrId).getResolvedAt(), "resolve 记录解决时间");

        // ---------- 6. 置 SCRAP 处置 → postNcr（NCR_SCRAP 凭证） ----------
        ormTemplate.runInSession(() -> {
            ErpQaNonConformance ncr = daoProvider.daoFor(ErpQaNonConformance.class).getEntityById(ncrId);
            ncr.setDispositionType(ErpQaConstants.DISPOSITION_TYPE_SCRAP);
            daoProvider.daoFor(ErpQaNonConformance.class).updateEntity(ncr);
        });

        ApiResponse<?> postNcr = rpcMutation("ErpQaNonConformance__postNcr", request("24_ncr_post.json5", Map.class));
        output("24_ncr_post_response.json5", postNcr);
        assertEquals(0, postNcr.getStatus(), "SCRAP 处置过账应成功");

        // 层 1 锚点：posted=true + NCR_SCRAP 凭证借贷平衡（Dr 6711 营业外支出 / Cr 1401 存货 = 2×5=10）
        ErpQaNonConformance postedNcr = reloadNcr(ncrId);
        assertEquals(Boolean.TRUE, postedNcr.getPosted(), "postNcr 后 posted=true");
        assertNotNull(postedNcr.getPostedAt(), "过账时间已记录");
        ErpFinVoucherBillR link = findBillLink(postedNcr.getCode());
        ErpFinVoucher voucher = requireVoucherBalanced(link, SCRAP_AMOUNT, "NCR_SCRAP");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "NCR_SCRAP 凭证已过账");
        List<ErpFinVoucherLine> lines = findVoucherLines(voucher.getId());
        ErpFinVoucherLine lossDebit = lines.stream()
                .filter(l -> "6711".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine invCredit = lines.stream()
                .filter(l -> "1401".equals(l.getSubjectCode())).findFirst().orElse(null);
        assertNotNull(lossDebit, "NCR_SCRAP 凭证应含 6711 营业外支出行");
        assertNotNull(invCredit, "NCR_SCRAP 凭证应含 1401 存货行");
        assertEquals("DEBIT", lossDebit.getDcDirection(), "报废损失借方方向");
        assertEquals(0, SCRAP_AMOUNT.compareTo(lossDebit.getDebitAmount()), "报废损失借方=10");
        assertEquals("CREDIT", invCredit.getDcDirection(), "存货贷方方向");
        assertEquals(0, SCRAP_AMOUNT.compareTo(invCredit.getCreditAmount()), "存货贷方=10");
        assertEquals(2, lines.size(), "报废凭证 2 行");

        // ---------- 7. notify 域可达断言路径（设计文档 §6 C09 步骤 4 勘误替代） ----------
        seedNotificationTemplate();
        ApiResponse<?> notify = rpcMutation("ErpSysNotification__notify", request("25_notify.json5", Map.class));
        output("25_notify_response.json5", notify);
        assertEquals(0, notify.getStatus(), "通知派发应成功");

        // 层 1 锚点：通知落库（接收人 + 正文含 NCR 码）→ markRead → countUnread 归零
        List<ErpSysNotification> sent = notificationsOf(NOTIFY_USER);
        assertEquals(1, sent.size(), "应派发 1 条 NCR 通知");
        ErpSysNotification notification = sent.get(0);
        assertTrue(notification.getBody().contains("NCR-IT-C09-INS-001"), "通知正文含 NCR 码: " + notification.getBody());
        addVar("notifId", notification.getId());

        ApiResponse<?> markRead = rpcMutation("ErpSysNotification__markRead", request("26_notify_mark_read.json5", Map.class));
        output("26_notify_mark_read_response.json5", markRead);
        assertEquals(0, markRead.getStatus(), "markRead 应成功");

        ApiResponse<?> unread = executeRpc(GraphQLOperationType.query, "ErpSysNotification__countUnread",
                request("27_notify_count_unread.json5", Map.class));
        output("27_notify_count_unread_response.json5", unread);
        assertEquals(0, unread.getStatus(), "countUnread 查询应成功");
        assertEquals(0, ((Number) unread.getData()).intValue(), "markRead 后 countUnread 归零");
    }

    // ---------- helpers ----------

    private ErpQaNonConformance reloadNcr(String ncrId) {
        return daoProvider.daoFor(ErpQaNonConformance.class).getEntityById(ncrId);
    }

    private ErpQaAction reloadAction(String actionId) {
        return daoProvider.daoFor(ErpQaAction.class).getEntityById(actionId);
    }

    private ErpQaNonConformance findNcrBySourceCode(String sourceCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceCode", sourceCode));
        q.setLimit(1);
        List<ErpQaNonConformance> list = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private List<ErpSysNotification> notificationsOf(String userId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addOrderField("createTime", true);
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private void seedNotificationTemplate() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", "7109");
            t.setNotificationType("qa.ncr.resolved");
            t.setName("TPL-qa.ncr.resolved");
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("NCR处理通知: ${ncrCode}");
            t.setBodyTpl("NCR ${ncrCode} 已完成 CAPA 闭包并过账，请知悉");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig("{\"userIds\":[\"" + NOTIFY_USER + "\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            dao.saveEntity(t);
        });
    }
}