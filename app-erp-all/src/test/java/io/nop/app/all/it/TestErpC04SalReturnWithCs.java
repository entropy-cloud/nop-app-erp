package io.nop.app.all.it;

import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConstants;
import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.context.ContextProvider;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.notIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B2 C04：销售退货与客服联动（按 {@code docs/design/integration-testing.md §6 C04} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / CUST-001 客户 1 / MAT-001 物料 1 / WH-MAIN 仓库 1 /
 * CNY 币种 1 / 2026-07 OPEN 期间 / CS 工单类型 TT-COMPLAINT(1) / 库存余额 80@120）：CS 客服工单
 * （{@code ErpCsTicket__save} 客户投诉，自包含）→ 六态状态机推进（assign→start→resolve→close，以当前实现
 * 为准：无 respond 动作，先例 {@code TestErpCsTicketSlaCsat}）→ 自包含已过账销售链（Order → Delivery posted
 * [SALES_OUTPUT 凭证 + 库存出库] → Invoice posted [AR_INVOICE 凭证 + OPEN AR 项 113]）→ 退货单
 * save → submit → approve（DIRECT 审批轴：反向 INCOMING 入库移动 + SALES_RETURN 红字凭证
 * [借 1401 存货 / 贷 6401 成本，TOTAL_COST=4×120=480 current 成本口径——退货按当前 avgCost 入库，
 * 除尽无快照精度漂移] + DIRECTION_RECEIVABLE 负 openAmount 辅助账 credit memo [−45.2 含税口径，对齐
 * {@code TestErpSalReturnRefundEndToEnd} RETURN_WITH_TAX 先例]）→ 退款核销/open items 反查
 * （{@code IErpFinArApItemBiz.findOpenItemsByPartner}）。
 *
 * <p>前置勘误（Phase 3 登记设计文档 §6 C04）：「[seed] 既有已过账销售链（erp_sal_invoice posted + AR 项
 * OPEN）」为 stale 表述——实仓 AR 项全 SETTLED、OPEN 仅 HR 行，用例以自包含建数实现（对齐 §3.4 风险②
 * 「自包含建数」纪律）。
 *
 * <p>三层验证：层 1 = JUnit 关键断言；层 2 = 每步 response 快照；层 3 = output/tables 变更行。
 * RECORDING→CHECKING 往返已按 M0.2 试点范式完成（录制后切回默认 CHECKING 复跑全绿）。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC04SalReturnWithCs extends ErpIntegrationTestCase {

    static final BigDecimal INVOICE_TOTAL_WITH_TAX = new BigDecimal("113"); // 10 × 10 + 13%
    static final BigDecimal RETURN_WITH_TAX = new BigDecimal("45.2");       // 4 × 10 + 13%（credit memo 含税口径）
    // 退货成本（config erp-sal.return-cost-method=current：按当前库存 avgCost 120 入库——4 × 120 = 480；
    // current 策略下 8880/74=120.000000 除尽，快照 AVG_COST 无精度漂移；original 策略（行价 10）会使
    // 8440/74 除不尽触发 in-memory(6 位)↔DB(4 位) 数值不等，故本用例显式走 current 口径）
    static final BigDecimal RETURN_COST = new BigDecimal("480");
    static final BigDecimal REMAIN_BALANCE = new BigDecimal("67.8");        // 113 − 45.2

    static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IErpFinArApItemBiz arApItemBiz;
    @Inject
    IDaoProvider daoProvider;

    // 对齐 B1 C02 先例：@BeforeEach SYS caller（审计创建人为框架默认用户，approver=0 满足 SoD 分离）
    @BeforeEach
    public void setUpWfUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    @Test
    public void testSalReturnWithCs() {
        // ---------- 1. CS 工单（客户投诉）save → 六态状态机推进（assign → start → resolve → close） ----------
        // config off 隔离自动分派维度（无同码 cs/crm 团队 seed 时无匹配池留 NEW；手动分派驱动状态机，
        // 对齐 TestErpCsTicketCreateEnrichment.testAutoAssignConfigOffSkipsAssignOnly 范式）
        String ticketId;
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_AUTO_ASSIGN_ON_CREATE, "false");
        try {
            ApiResponse<?> ticketSave = rpcMutation("ErpCsTicket__save", request("1_ticket_save.json5", Map.class));
            output("1_ticket_save_response.json5", ticketSave);
            assertEquals(0, ticketSave.getStatus(), "工单保存应成功");
            ticketId = idOf(ticketSave);
            addVar("ticketId", ticketId);
            assertEquals(ErpCsConstants.TICKET_STATUS_NEW, reload(ErpCsTicket.class, ticketId).getStatus(),
                    "工单初始状态 NEW（config off 不自动分派）");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_AUTO_ASSIGN_ON_CREATE, "true");
        }

        ApiResponse<?> assign = rpcMutation("ErpCsTicket__assign", request("2_ticket_assign.json5", Map.class));
        output("2_ticket_assign_response.json5", assign);
        assertEquals(0, assign.getStatus(), "工单分派应成功");
        assertEquals(ErpCsConstants.TICKET_STATUS_ASSIGNED, reload(ErpCsTicket.class, ticketId).getStatus(),
                "工单状态 ASSIGNED");

        ApiResponse<?> start = rpcMutation("ErpCsTicket__start", request("3_ticket_start.json5", Map.class));
        output("3_ticket_start_response.json5", start);
        assertEquals(0, start.getStatus(), "工单开始处理应成功");
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reload(ErpCsTicket.class, ticketId).getStatus(),
                "工单状态 IN_PROGRESS");

        ApiResponse<?> resolve = rpcMutation("ErpCsTicket__resolve", request("4_ticket_resolve.json5", Map.class));
        output("4_ticket_resolve_response.json5", resolve);
        assertEquals(0, resolve.getStatus(), "工单解决应成功");
        assertEquals(ErpCsConstants.TICKET_STATUS_RESOLVED, reload(ErpCsTicket.class, ticketId).getStatus(),
                "工单状态 RESOLVED");

        ApiResponse<?> close = rpcMutation("ErpCsTicket__close", request("5_ticket_close.json5", Map.class));
        output("5_ticket_close_response.json5", close);
        assertEquals(0, close.getStatus(), "工单关闭应成功");
        ErpCsTicket closedTicket = reload(ErpCsTicket.class, ticketId);
        assertEquals(ErpCsConstants.TICKET_STATUS_CLOSED, closedTicket.getStatus(), "工单终态 CLOSED");

        // ---------- 2. 自包含已过账销售链：Order → Delivery（SALES_OUTPUT + 库存出库）→ Invoice（AR_INVOICE + OPEN AR 项） ----------
        ApiResponse<?> orderSave = rpcMutation("ErpSalOrder__save", request("6_order_save.json5", Map.class));
        output("6_order_save_response.json5", orderSave);
        assertEquals(0, orderSave.getStatus(), "销售订单保存应成功");
        String orderId = idOf(orderSave);
        addVar("orderId", orderId);

        ApiResponse<?> orderLine = rpcMutation("ErpSalOrderLine__save", request("7_order_line_save.json5", Map.class));
        output("7_order_line_save_response.json5", orderLine);
        assertEquals(0, orderLine.getStatus(), "订单行保存应成功");
        addVar("orderLineId", idOf(orderLine));

        ApiResponse<?> orderSubmit = rpcMutation("ErpSalOrder__submitForApproval", request("8_order_submit.json5", Map.class));
        output("8_order_submit_response.json5", orderSubmit);
        assertEquals(0, orderSubmit.getStatus(), "销售订单提交审批应成功");
        ApiResponse<?> orderApprove = rpcMutation("ErpSalOrder__approve", request("9_order_approve.json5", Map.class));
        output("9_order_approve_response.json5", orderApprove);
        assertEquals(0, orderApprove.getStatus(), "销售订单审批应成功");

        ApiResponse<?> deliverySave = rpcMutation("ErpSalDelivery__save", request("10_delivery_save.json5", Map.class));
        output("10_delivery_save_response.json5", deliverySave);
        assertEquals(0, deliverySave.getStatus(), "出库单保存应成功");
        String deliveryId = idOf(deliverySave);
        String deliveryCode = String.valueOf(((Map<?, ?>) deliverySave.getData()).get("code"));
        addVar("deliveryId", deliveryId);
        addVar("deliveryCode", deliveryCode);

        ApiResponse<?> deliveryLine = rpcMutation("ErpSalDeliveryLine__save", request("11_delivery_line_save.json5", Map.class));
        output("11_delivery_line_save_response.json5", deliveryLine);
        assertEquals(0, deliveryLine.getStatus(), "出库行保存应成功");
        addVar("deliveryLineId", idOf(deliveryLine));

        ApiResponse<?> deliverySubmit = rpcMutation("ErpSalDelivery__submitForApproval", request("12_delivery_submit.json5", Map.class));
        output("12_delivery_submit_response.json5", deliverySubmit);
        assertEquals(0, deliverySubmit.getStatus(), "出库单提交审批应成功");
        ApiResponse<?> deliveryApprove = rpcMutation("ErpSalDelivery__approve", request("13_delivery_approve.json5", Map.class));
        output("13_delivery_approve_response.json5", deliveryApprove);
        assertEquals(0, deliveryApprove.getStatus(), "出库单审批应成功");
        ErpSalDelivery approvedDelivery = reload(ErpSalDelivery.class, deliveryId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedDelivery.getApproveStatus(), "出库单 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedDelivery.getPosted(), "出库审核后 posted=true（SALES_OUTPUT 暂估应收前置）");

        ApiResponse<?> invoiceSave = rpcMutation("ErpSalInvoice__save", request("14_invoice_save.json5", Map.class));
        output("14_invoice_save_response.json5", invoiceSave);
        assertEquals(0, invoiceSave.getStatus(), "销售发票保存应成功");
        String invoiceId = idOf(invoiceSave);
        String invoiceCode = String.valueOf(((Map<?, ?>) invoiceSave.getData()).get("code"));
        addVar("invoiceId", invoiceId);
        addVar("invoiceCode", invoiceCode);

        ApiResponse<?> invoiceLine = rpcMutation("ErpSalInvoiceLine__save", request("15_invoice_line_save.json5", Map.class));
        output("15_invoice_line_save_response.json5", invoiceLine);
        assertEquals(0, invoiceLine.getStatus(), "发票行保存应成功");

        ApiResponse<?> invoiceSubmit = rpcMutation("ErpSalInvoice__submitForApproval", request("16_invoice_submit.json5", Map.class));
        output("16_invoice_submit_response.json5", invoiceSubmit);
        assertEquals(0, invoiceSubmit.getStatus(), "发票提交审批应成功");
        ApiResponse<?> invoiceApprove = rpcMutation("ErpSalInvoice__approve", request("17_invoice_approve.json5", Map.class));
        output("17_invoice_approve_response.json5", invoiceApprove);
        assertEquals(0, invoiceApprove.getStatus(), "发票审批应成功");
        ErpSalInvoice approvedInvoice = reload(ErpSalInvoice.class, invoiceId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedInvoice.getApproveStatus(), "发票 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedInvoice.getPosted(), "发票审核后 posted=true (AR_INVOICE)");

        ErpFinArApItem invoiceItem = findApItem(ErpFinConstants.SOURCE_BILL_AR_INVOICE, invoiceCode);
        assertNotNull(invoiceItem, "已过账销售链应生成 OPEN AR 项");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, invoiceItem.getStatus(), "AR 项 OPEN");
        assertEquals(0, INVOICE_TOTAL_WITH_TAX.compareTo(invoiceItem.getOpenAmountFunctional()), "AR 项 openAmount=113");

        // ---------- 3. 退货单 save → submit → approve（反向入库 + SALES_RETURN 红字凭证 + 负 AR） ----------
        // 退货成本策略 = current（当前库存 avgCost 120 入库）：与库存 ledger 同源（ReturnCostStrategyResolver），
        // 且 AVG_COST 除尽无快照精度漂移（见常量注释）；original 默认（行价 10）会使余额 AVG_COST 除不尽。
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_RETURN_COST_METHOD, ErpSalConstants.RETURN_COST_METHOD_CURRENT);
        try {
            ApiResponse<?> returnSave = rpcMutation("ErpSalReturn__save", request("18_return_save.json5", Map.class));
            output("18_return_save_response.json5", returnSave);
            assertEquals(0, returnSave.getStatus(), "退货单保存应成功");
            String returnId = idOf(returnSave);
            String returnCode = String.valueOf(((Map<?, ?>) returnSave.getData()).get("code"));
            addVar("returnId", returnId);
            addVar("returnCode", returnCode);

            ApiResponse<?> returnLine = rpcMutation("ErpSalReturnLine__save", request("19_return_line_save.json5", Map.class));
            output("19_return_line_save_response.json5", returnLine);
            assertEquals(0, returnLine.getStatus(), "退货行保存应成功");

            ApiResponse<?> returnSubmit = rpcMutation("ErpSalReturn__submitForApproval", request("20_return_submit.json5", Map.class));
            output("20_return_submit_response.json5", returnSubmit);
            assertEquals(0, returnSubmit.getStatus(), "退货单提交审批应成功");
            ApiResponse<?> returnApprove = rpcMutation("ErpSalReturn__approve", request("21_return_approve.json5", Map.class));
            output("21_return_approve_response.json5", returnApprove);
            assertEquals(0, returnApprove.getStatus(), "退货单审批应成功");

            // 层 1 锚点：Return approveStatus=APPROVED + posted=true
            ErpSalReturn approvedReturn = reload(ErpSalReturn.class, returnId);
            assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedReturn.getApproveStatus(), "退货单 approveStatus=APPROVED");
            assertEquals(Boolean.TRUE, approvedReturn.getPosted(), "退货审核后 posted=true (SALES_RETURN)");

            // 层 1 锚点：反向入库移动存在（relatedBill 反查，INCOMING）
            ErpInvStockMove reverseMove = findStockMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_RETURN, returnCode);
            assertNotNull(reverseMove, "退货审批应触发反向入库移动");
            assertEquals(ErpSalConstants.MOVE_TYPE_INCOMING, reverseMove.getMoveType(), "反向入库 MOVE_TYPE=INCOMING");
            assertEquals("DONE", reverseMove.getDocStatus(), "反向入库 docStatus=DONE");

            // 层 1 锚点：SALES_RETURN 红字凭证借贷平衡（借 1401 存货 / 贷 6401 成本 = 反向 SALES_OUTPUT）
            ErpFinVoucherBillR rtLink = findBillLink(returnCode);
            ErpFinVoucher returnVoucher = requireVoucherBalanced(rtLink, RETURN_COST, "SALES_RETURN");
            assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, returnVoucher.getDocStatus(), "红字凭证已过账");
            List<ErpFinVoucherLine> rtLines = findVoucherLines(returnVoucher.getId());
            assertEquals(2, rtLines.size(), "SALES_RETURN 凭证 2 行");
            ErpFinVoucherLine rtInvLine = rtLines.stream()
                    .filter(l -> "1401".equals(l.getSubjectCode())).findFirst().orElse(null);
            ErpFinVoucherLine rtCogsLine = rtLines.stream()
                    .filter(l -> "6401".equals(l.getSubjectCode())).findFirst().orElse(null);
            assertNotNull(rtInvLine, "红字凭证应含 1401 存货行");
            assertNotNull(rtCogsLine, "红字凭证应含 6401 成本行");
            assertEquals("DEBIT", rtInvLine.getDcDirection(), "红字方向：1401 借方（反向 SALES_OUTPUT 贷方）");
            assertEquals(0, RETURN_COST.compareTo(rtInvLine.getDebitAmount()), "红字 1401 借方=480");
            assertEquals("CREDIT", rtCogsLine.getDcDirection(), "红字方向：6401 贷方");
            assertEquals(0, RETURN_COST.compareTo(rtCogsLine.getCreditAmount()), "红字 6401 贷方=480");

            // 层 1 锚点：AR 项负向登记（credit memo，openAmount = −totalAmountWithTax 含税口径）
            ErpFinArApItem returnItem = findApItem(ErpFinConstants.SOURCE_BILL_SAL_RETURN, returnCode);
            assertNotNull(returnItem, "退货过账应生成负 AR 辅助账");
            assertEquals(ErpFinConstants.DIRECTION_RECEIVABLE, returnItem.getDirection(), "方向=应收");
            assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, returnItem.getStatus(), "退货辅助账 OPEN");
            assertEquals(0, RETURN_WITH_TAX.negate().compareTo(returnItem.getOpenAmountFunctional()),
                    "openAmount = −45.2（credit memo，含税口径）");

            // 层 1 锚点：余额回减断言（sumOpen 口径，客户 1：113 − 45.2 = 67.8）
            assertEquals(0, REMAIN_BALANCE.compareTo(arReceivableOpen("1")),
                    "应收余额回减 = 67.8（113 − 45.2）");

            // 层 1 锚点：退款核销/open items 反查（findOpenItemsByPartner：负项在位 + 总额回减）
            List<ErpFinArApItem> openItems = arApItemBiz.findOpenItemsByPartner("1",
                    ErpFinConstants.DIRECTION_RECEIVABLE, CTX);
            boolean hasReturnItem = openItems.stream().anyMatch(it -> ErpFinConstants.SOURCE_BILL_SAL_RETURN
                    .equals(it.getSourceBillType()) && returnCode.equals(it.getSourceBillCode()));
            assertTrue(hasReturnItem, "open items 反查应含退货负项");
            BigDecimal openSum = openItems.stream().map(it -> nz(it.getOpenAmountFunctional()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, REMAIN_BALANCE.compareTo(openSum), "open items 合计 = 67.8");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpSalConstants.CONFIG_RETURN_COST_METHOD, ErpSalConstants.RETURN_COST_METHOD_ORIGINAL);
        }
    }

    // ---------- helpers ----------

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    /**
     * AR 总额（客户维度）：RECEIVABLE 方向 openAmountFunctional 合计，排除 SETTLED/CANCELLED
     * （对齐 TestErpSalReturnRefundEndToEnd 的 sumOpen 口径，即 PartnerBalanceUpdater.sumOpen）。
     */
    private BigDecimal arReceivableOpen(String partnerId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", partnerId));
        q.addFilter(eq("direction", ErpFinConstants.DIRECTION_RECEIVABLE));
        q.addFilter(notIn("status", List.of(ErpFinConstants.AR_AP_STATUS_SETTLED,
                ErpFinConstants.AR_AP_STATUS_CANCELLED)));
        List<ErpFinArApItem> items = daoProvider.daoFor(ErpFinArApItem.class).findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : items) {
            if (it.getOpenAmountFunctional() != null) {
                sum = sum.add(it.getOpenAmountFunctional());
            }
        }
        return sum;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}