package io.nop.app.all.it;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.notIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * B1 C01：P2P 采购到付款黄金路径（按 {@code docs/design/integration-testing.md §6 C01} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / SUP-001 供应商 3 / MAT-001 物料 1 / WH-RAW 仓库 2 /
 * CNY 币种 1 / 2026-07 OPEN 期间）：PO（2 行物料）→ submit/approve → Receive → submit/approve
 * （入库移动 DONE + PURCHASE_INPUT 暂估凭证）→ Invoice（2 行）→ submit/approve（AP_INVOICE 凭证 +
 * AP 辅助账 OPEN）→ Payment save → submit/approve（PAYMENT 凭证 + 付款辅助账，xwf 轴经
 * {@code setUserId("0")} 后端驱动——本类 {@link #setUpWfUser()} 即 Phase 1 先导复核载体）→
 * {@code ErpPurPayment__settle} 独立核销（发票 paidStatus=PAID / 付款 writtenOffStatus=PAID）→
 * 财务正式核销单 {@link ErpFinReconciliation} 核销（AP 辅助账 openAmount→0 + SETTLED）。
 *
 * <p>GL 应付余额联动断言（Phase 1 Decision 裁决口径）：数据源 = 应付账款科目（2202）VoucherLine
 * 聚合（对齐 {@code ProfitLossClosingService} VoucherLine 权威口径，{@code gl_balance} 表为 seed
 * 静态不作断言源）；正式应付口径 = businessType ∈ {AP_INVOICE, PAYMENT} 的 credit−debit 净额
 * （PURCHASE_INPUT 暂估不产生 ar_ap_item，不计入 AP 联动）。与 AP 总额（供应商 3 PAYABLE 方向
 * openAmount 合计，排除 SETTLED/CANCELLED）在发票审核后（79.1=79.1）与核销完成后（0=0）两处一致。
 *
 * <p>三层验证：层 1 = JUnit 关键断言；层 2 = 每步 response 快照；层 3 = output/tables 变更行。
 * RECORDING→CHECKING 往返已按 M0.2 试点范式完成（录制后切回默认 CHECKING 复跑全绿）。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC01P2pGoldenPath extends ErpIntegrationTestCase {

    static final BigDecimal LINE1_TOTAL = new BigDecimal("50");   // 10 × 5
    static final BigDecimal LINE2_TOTAL = new BigDecimal("20");   // 4 × 5
    static final BigDecimal TOTAL = new BigDecimal("70");         // 不含税
    static final BigDecimal TAX = new BigDecimal("9.1");          // 13%
    static final BigDecimal TOTAL_WITH_TAX = new BigDecimal("79.1");

    static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IErpFinReconciliationBiz reconciliationBiz;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    io.nop.dao.api.IDaoProvider daoProvider;

    // Phase 1 Proof：xwf setUserId("0") × 机制 (c) 共存复核（app-erp-all 装配）——Payment
    // save → submitForApproval → approve 全链可达的前提（wf 步骤参与者 user:$0 = SYS(id=0)，
    // 对齐 TestErpPurPaymentWorkflowApproval / TestErpPurReturnRefundEndToEnd 先例路径）。
    @BeforeEach
    public void setUpWfUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    @Test
    public void testP2pGoldenPath() {
        // ---------- 1. 采购订单（自包含 2 行物料）→ submit → approve ----------
        ApiResponse<?> poSave = rpcMutation("ErpPurOrder__save", request("1_order_save.json5", Map.class));
        output("1_order_save_response.json5", poSave);
        assertEquals(0, poSave.getStatus(), "PO 保存应成功");
        String poId = idOf(poSave);
        addVar("poId", poId);

        ApiResponse<?> poLine1 = rpcMutation("ErpPurOrderLine__save", request("2_order_line1_save.json5", Map.class));
        output("2_order_line1_save_response.json5", poLine1);
        assertEquals(0, poLine1.getStatus(), "PO 行1保存应成功");
        addVar("poLineId1", idOf(poLine1));

        ApiResponse<?> poLine2 = rpcMutation("ErpPurOrderLine__save", request("3_order_line2_save.json5", Map.class));
        output("3_order_line2_save_response.json5", poLine2);
        assertEquals(0, poLine2.getStatus(), "PO 行2保存应成功");
        addVar("poLineId2", idOf(poLine2));

        ApiResponse<?> poSubmit = rpcMutation("ErpPurOrder__submitForApproval", request("4_order_submit.json5", Map.class));
        output("4_order_submit_response.json5", poSubmit);
        assertEquals(0, poSubmit.getStatus(), "PO 提交审批应成功");

        ApiResponse<?> poApprove = rpcMutation("ErpPurOrder__approve", request("5_order_approve.json5", Map.class));
        output("5_order_approve_response.json5", poApprove);
        assertEquals(0, poApprove.getStatus(), "PO 审批应成功");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED,
                reload(ErpPurOrder.class, poId).getApproveStatus(), "PO approveStatus=APPROVED");

        // ---------- 2. 收货单（2 行，引用 PO 行）→ submit → approve（入库移动 + 暂估凭证） ----------
        ApiResponse<?> rcvSave = rpcMutation("ErpPurReceive__save", request("6_receive_save.json5", Map.class));
        output("6_receive_save_response.json5", rcvSave);
        assertEquals(0, rcvSave.getStatus(), "收货单保存应成功");
        String receiveId = idOf(rcvSave);
        String receiveCode = String.valueOf(((Map<?, ?>) rcvSave.getData()).get("code"));
        addVar("receiveId", receiveId);
        addVar("receiveCode", receiveCode);

        ApiResponse<?> rcvLine1 = rpcMutation("ErpPurReceiveLine__save", request("7_receive_line1_save.json5", Map.class));
        output("7_receive_line1_save_response.json5", rcvLine1);
        assertEquals(0, rcvLine1.getStatus(), "收货行1保存应成功");
        addVar("receiveLineId1", idOf(rcvLine1));

        ApiResponse<?> rcvLine2 = rpcMutation("ErpPurReceiveLine__save", request("8_receive_line2_save.json5", Map.class));
        output("8_receive_line2_save_response.json5", rcvLine2);
        assertEquals(0, rcvLine2.getStatus(), "收货行2保存应成功");
        addVar("receiveLineId2", idOf(rcvLine2));

        ApiResponse<?> rcvSubmit = rpcMutation("ErpPurReceive__submitForApproval", request("9_receive_submit.json5", Map.class));
        output("9_receive_submit_response.json5", rcvSubmit);
        assertEquals(0, rcvSubmit.getStatus(), "收货单提交审批应成功");

        ApiResponse<?> rcvApprove = rpcMutation("ErpPurReceive__approve", request("10_receive_approve.json5", Map.class));
        output("10_receive_approve_response.json5", rcvApprove);
        assertEquals(0, rcvApprove.getStatus(), "收货单审批应成功");
        ErpPurReceive approvedReceive = reload(ErpPurReceive.class, receiveId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedReceive.getApproveStatus(), "收货单 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedReceive.getPosted(), "收货审核后 posted=true");
        ErpInvStockMove move = findStockMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receiveCode);
        assertNotNull(move, "收货审批应触发入库移动");
        assertEquals("DONE", move.getDocStatus(), "入库移动 docStatus=DONE");
        assertEquals(Boolean.TRUE, move.getPosted(), "入库移动 posted=true");

        // ---------- 3. 发票（2 行，引用收货行）→ submit → approve（AP_INVOICE 凭证 + AP 辅助账 OPEN） ----------
        ApiResponse<?> invSave = rpcMutation("ErpPurInvoice__save", request("11_invoice_save.json5", Map.class));
        output("11_invoice_save_response.json5", invSave);
        assertEquals(0, invSave.getStatus(), "发票保存应成功");
        String invoiceId = idOf(invSave);
        String invoiceCode = String.valueOf(((Map<?, ?>) invSave.getData()).get("code"));
        addVar("invoiceId", invoiceId);
        addVar("invoiceCode", invoiceCode);

        ApiResponse<?> invLine1 = rpcMutation("ErpPurInvoiceLine__save", request("12_invoice_line1_save.json5", Map.class));
        output("12_invoice_line1_save_response.json5", invLine1);
        assertEquals(0, invLine1.getStatus(), "发票行1保存应成功");
        ApiResponse<?> invLine2 = rpcMutation("ErpPurInvoiceLine__save", request("13_invoice_line2_save.json5", Map.class));
        output("13_invoice_line2_save_response.json5", invLine2);
        assertEquals(0, invLine2.getStatus(), "发票行2保存应成功");

        ApiResponse<?> invSubmit = rpcMutation("ErpPurInvoice__submitForApproval", request("14_invoice_submit.json5", Map.class));
        output("14_invoice_submit_response.json5", invSubmit);
        assertEquals(0, invSubmit.getStatus(), "发票提交审批应成功");

        ApiResponse<?> invApprove = rpcMutation("ErpPurInvoice__approve", request("15_invoice_approve.json5", Map.class));
        output("15_invoice_approve_response.json5", invApprove);
        assertEquals(0, invApprove.getStatus(), "发票审批应成功");
        ErpPurInvoice approvedInvoice = reload(ErpPurInvoice.class, invoiceId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedInvoice.getApproveStatus(), "发票 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedInvoice.getPosted(), "发票审核后 posted=true (AP_INVOICE)");

        // 层 1 锚点：三张凭证借贷平衡（暂估 70 / AP_INVOICE 79.1）
        // 暂估凭证的业财回链 billCode = 库存移动单 code（InvPostingDispatcher 口径），非收货单 code。
        ErpFinVoucherBillR inputLink = findBillLink(move.getCode());
        ErpFinVoucher inputVoucher = requireVoucherBalanced(inputLink, TOTAL, "PURCHASE_INPUT");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, inputVoucher.getDocStatus(), "暂估凭证已过账");
        ErpFinVoucherBillR apLink = findBillLink(invoiceCode);
        ErpFinVoucher apVoucher = requireVoucherBalanced(apLink, TOTAL_WITH_TAX, "AP_INVOICE");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, apVoucher.getDocStatus(), "AP_INVOICE 凭证已过账");

        // 层 1 锚点：AP 辅助账 OPEN + openAmount=79.1
        ErpFinArApItem apItem = findApItem(ErpFinConstants.SOURCE_BILL_AP_INVOICE, invoiceCode);
        assertNotNull(apItem, "AP_INVOICE 过账应生成应付辅助账");
        assertEquals(ErpFinConstants.DIRECTION_PAYABLE, apItem.getDirection(), "方向=应付");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, apItem.getStatus(), "初始 OPEN");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(apItem.getOpenAmountFunctional()), "AP 辅助账 openAmount=79.1");

        // 层 1 锚点（GL 口径裁决落地）：发票审核后 GL 应付余额（2202 VoucherLine 聚合）= AP 总额（供应商 3）= 79.1
        assertEquals(0, TOTAL_WITH_TAX.compareTo(glPayableBalance()), "GL 应付余额=79.1");
        assertEquals(0, glPayableBalance().compareTo(apPayableOpen("3")), "GL 应付余额与 AP 总额一致");

        // ---------- 4. 付款单（xwf 轴 setUserId("0") 后端驱动）→ submit → approve（PAYMENT 凭证 + 付款辅助账） ----------
        ApiResponse<?> paySave = rpcMutation("ErpPurPayment__save", request("16_payment_save.json5", Map.class));
        output("16_payment_save_response.json5", paySave);
        assertEquals(0, paySave.getStatus(), "付款单保存应成功");
        String paymentId = idOf(paySave);
        String paymentCode = String.valueOf(((Map<?, ?>) paySave.getData()).get("code"));
        addVar("paymentId", paymentId);
        addVar("paymentCode", paymentCode);

        ApiResponse<?> paySubmit = rpcMutation("ErpPurPayment__submitForApproval", request("17_payment_submit.json5", Map.class));
        output("17_payment_submit_response.json5", paySubmit);
        assertEquals(0, paySubmit.getStatus(), "付款提交审批应成功（SYS caller 匹配 wf 步骤参与者 user:$0）");

        ApiResponse<?> payApprove = rpcMutation("ErpPurPayment__approve", request("18_payment_approve.json5", Map.class));
        output("18_payment_approve_response.json5", payApprove);
        assertEquals(0, payApprove.getStatus(), "付款审批应成功");
        ErpPurPayment approvedPayment = reload(ErpPurPayment.class, paymentId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedPayment.getApproveStatus(),
                "xwf 复核判据：Payment approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedPayment.getPosted(), "付款审核后 posted=true (PAYMENT)");

        ErpFinVoucherBillR payLink = findBillLink(paymentCode);
        ErpFinVoucher payVoucher = requireVoucherBalanced(payLink, TOTAL_WITH_TAX, "PAYMENT");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, payVoucher.getDocStatus(), "PAYMENT 凭证已过账");
        assertEquals(0, BigDecimal.ZERO.compareTo(glPayableBalance()),
                "付款过账后 GL 应付余额归零（借 2202 / 贷 1002）");

        ErpFinArApItem payItem = findApItem(ErpFinConstants.SOURCE_BILL_PAYMENT, paymentCode);
        assertNotNull(payItem, "PAYMENT 过账应生成付款辅助账");
        assertEquals(ErpFinConstants.DIRECTION_PAYABLE, payItem.getDirection(), "方向=应付");

        // ---------- 5. 独立核销动作 ErpPurPayment__settle（域级：发票 paidStatus / 付款 writtenOffStatus） ----------
        ApiResponse<?> settle = rpcMutation("ErpPurPayment__settle", request("19_settle.json5", Map.class));
        output("19_settle_response.json5", settle);
        assertEquals(0, settle.getStatus(), "付款核销应成功");
        ErpPurInvoice settledInvoice = reload(ErpPurInvoice.class, invoiceId);
        ErpPurPayment settledPayment = reload(ErpPurPayment.class, paymentId);
        assertEquals(0, TOTAL_WITH_TAX.compareTo(settledInvoice.getPaidAmount()), "发票已付=79.1");
        assertEquals(ErpPurConstants.PAID_STATUS_PAID, settledInvoice.getPaidStatus(), "发票 paidStatus=PAID");
        assertEquals(ErpPurConstants.PAID_STATUS_PAID, settledPayment.getWrittenOffStatus(), "付款 writtenOffStatus=PAID");

        // ---------- 6. 财务正式核销单（ErpFinReconciliation create + post）：AP 辅助账 openAmount→0 + SETTLED ----------
        ErpFinReconciliation recon = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_PAYABLE, "3", LocalDate.of(2026, 7, 15),
                Collections.singletonList(reconLine(payItem.getId(), apItem.getId(), "79.1")), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(recon.getId(), CTX));
        ErpFinReconciliation postedRecon = reload(ErpFinReconciliation.class, recon.getId());
        Map<String, Object> reconState = new LinkedHashMap<>();
        reconState.put("id", postedRecon.getId());
        reconState.put("docStatus", postedRecon.getDocStatus());
        reconState.put("totalAmountFunctional", postedRecon.getTotalAmountFunctional());
        output("20_reconciliation_post_response.json5", reconState);
        assertEquals(ErpFinConstants.RECON_STATUS_POSTED, postedRecon.getDocStatus(), "核销单 POSTED");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(postedRecon.getTotalAmountFunctional()), "核销单总额=79.1");

        // 层 1 锚点：核销后 AP 辅助账 openAmount=0（发票项 + 付款项）+ SETTLED
        ErpFinArApItem settledApItem = reload(ErpFinArApItem.class, apItem.getId());
        ErpFinArApItem settledPayItem = reload(ErpFinArApItem.class, payItem.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(settledApItem.getOpenAmountFunctional()), "核销后发票辅助账 openAmount=0");
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, settledApItem.getStatus(), "发票辅助账 SETTLED");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(settledApItem.getSettledAmountFunctional()), "发票辅助账 settledAmount=79.1");
        assertEquals(0, BigDecimal.ZERO.compareTo(settledPayItem.getOpenAmountFunctional()), "核销后付款辅助账 openAmount=0");
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, settledPayItem.getStatus(), "付款辅助账 SETTLED");

        // 层 1 锚点（GL 口径裁决落地）：核销后 AP 总额归零 = GL 应付余额归零（0=0）
        assertEquals(0, BigDecimal.ZERO.compareTo(apPayableOpen("3")), "核销后 AP 总额=0");
        assertEquals(0, BigDecimal.ZERO.compareTo(glPayableBalance()), "核销后 GL 应付余额=0");
        assertEquals(0, glPayableBalance().compareTo(apPayableOpen("3")), "GL 应付余额与 AP 总额一致");
    }

    // ---------- helpers ----------

    private ReconciliationLineInput reconLine(String paymentItemId, String invoiceItemId, String amount) {
        BigDecimal amt = new BigDecimal(amount);
        ReconciliationLineInput in = new ReconciliationLineInput();
        in.setPaymentItemId(paymentItemId);
        in.setInvoiceItemId(invoiceItemId);
        in.setSettledAmountSource(amt);
        in.setSettledAmountFunctional(amt);
        return in;
    }

    /**
     * GL 应付余额（Phase 1 Decision 裁决口径）：应付账款科目（2202）VoucherLine 聚合，正式应付口径
     * （businessType ∈ {AP_INVOICE, PAYMENT} 的 credit−debit 净额）。数据源对齐 ProfitLossClosingService
     * 的 VoucherLine 权威口径（gl_balance 表为 seed 静态 5 行，不由过账引擎维护，不作断言源）。
     * PURCHASE_INPUT 暂估（同科目 2202）不产生 ar_ap_item，不计入 AP 联动口径。
     */
    private BigDecimal glPayableBalance() {
        BigDecimal net = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : daoAll(ErpFinVoucherLine.class)) {
            if (!"2202".equals(l.getSubjectCode())) {
                continue;
            }
            String bt = l.getBusinessType();
            if (!ErpFinConstants.SOURCE_BILL_AP_INVOICE.equals(bt)
                    && !ErpFinConstants.SOURCE_BILL_PAYMENT.equals(bt)) {
                continue;
            }
            net = net.add(nz(l.getCreditAmount())).subtract(nz(l.getDebitAmount()));
        }
        return net;
    }

    /**
     * AP 总额（供应商维度）：PAYABLE 方向 openAmountFunctional 合计，排除 SETTLED/CANCELLED
     * （对齐 TestErpPurReturnRefundEndToEnd 的 sumOpen 口径，即 PartnerBalanceUpdater.sumOpen）。
     */
    private BigDecimal apPayableOpen(String partnerId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", partnerId));
        q.addFilter(eq("direction", ErpFinConstants.DIRECTION_PAYABLE));
        q.addFilter(notIn("status", List.of(ErpFinConstants.AR_AP_STATUS_SETTLED,
                ErpFinConstants.AR_AP_STATUS_CANCELLED)));
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : daoAll(ErpFinArApItem.class)) {
            if (it.getOpenAmountFunctional() == null) {
                continue;
            }
            boolean matched = partnerId.equals(it.getPartnerId())
                    && ErpFinConstants.DIRECTION_PAYABLE.equals(it.getDirection())
                    && !ErpFinConstants.AR_AP_STATUS_SETTLED.equals(it.getStatus())
                    && !ErpFinConstants.AR_AP_STATUS_CANCELLED.equals(it.getStatus());
            if (matched) {
                sum = sum.add(it.getOpenAmountFunctional());
            }
        }
        return sum;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private <T extends io.nop.orm.IOrmEntity> java.util.List<T> daoAll(Class<T> clazz) {
        return daoProvider.daoFor(clazz).findAllByQuery(new QueryBean());
    }
}