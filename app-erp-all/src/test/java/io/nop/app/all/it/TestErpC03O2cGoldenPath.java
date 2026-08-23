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
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
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
 * B2 C03：O2C 销售到收款黄金路径（B2B EDI 订单发起，按 {@code docs/design/integration-testing.md §6 C03} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / CUST-001 客户 1 / MAT-001 物料 1 / WH-MAIN 仓库 1 /
 * CNY 币种 1 / 2026-07 OPEN 期间 / 库存余额 80@120（MOVING_AVERAGE 成本层））：B2B EDI 入站报文接收
 * （{@code ErpB2bEdiDoc__createInbound}，SALES_ORDER 关联，先例 {@code TestErpB2bEdiEnvelope}）→ CRM 机会
 * 自包含建数（{@code ErpCrmLead__save} leadType=OPPORTUNITY——以当前实现为准：无独立 ErpCrmOpportunity
 * 实体，商机为 CRM lead 类型，设计文档动作名按此落地）→ 转化来源核对 → 销售订单（10 × 100 = 1000/130/1130）
 * → submit/approve（DIRECT）→ 出库单 save/行 → submit/approve（SALES_OUTPUT 凭证 + 库存出库 + COGS=1200）→
 * 发票 save/行 → submit/approve（AR_INVOICE 凭证 1131/6001/2221 + AR 辅助账 OPEN 1130）→ 收款单 save →
 * submit/approve（RECEIPT 凭证 1002/1131，**xwf 轴 setUserId("0") 后端驱动——本类 {@link #setUpWfUser()}
 * 即 Phase 1 Proof 复核载体，对齐 B1 Payment 复核结论**）→ {@code ErpSalReceipt__settle} 独立核销
 * （发票 receivedStatus=RECEIVED）→ 财务正式核销单 {@link ErpFinReconciliation}（AR 辅助账 openAmount→0
 * + SETTLED——对齐 B1 实施期发现：ar_ap_item 归零由财务核销单驱动，域级 settle 仅回写域状态）。
 *
 * <p>GL 应收余额断言（对齐 B1 Phase 1 Decision 裁决口径）：数据源 = 应收账款科目（1131）VoucherLine
 * 聚合（businessType ∈ {AR_INVOICE, RECEIPT} 的 debit−credit 净额；seed 应收凭证行科目为 1122，不混入
 * 1131 聚合），与 AR 总额（客户 1 RECEIVABLE 方向 openAmount 合计，排除 SETTLED/CANCELLED）在发票审核后
 * （1130=1130）与核销完成后（0=0）两处一致。
 *
 * <p>三层验证：层 1 = JUnit 关键断言；层 2 = 每步 response 快照；层 3 = output/tables 变更行。
 * RECORDING→CHECKING 往返已按 M0.2 试点范式完成（录制后切回默认 CHECKING 复跑全绿）。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC03O2cGoldenPath extends ErpIntegrationTestCase {

    static final BigDecimal TOTAL = new BigDecimal("1000");     // 不含税
    static final BigDecimal TAX = new BigDecimal("130");        // 13%
    static final BigDecimal TOTAL_WITH_TAX = new BigDecimal("1130");
    static final BigDecimal COGS = new BigDecimal("1200");      // 10 × 120（seed 成本层 MOVING_AVERAGE 80@120）
    static final String ORDER_CODE = "IT-C03-SO-001";

    static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IErpFinReconciliationBiz reconciliationBiz;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IDaoProvider daoProvider;

    // Phase 1 Proof：Receipt xwf setUserId("0") × 机制 (c) 共存复核（app-erp-all 装配）——B1 已裁决
    // Payment 同机制成立（TestErpPurPaymentWorkflowApproval / TestErpPurReturnRefundEndToEnd 先例），
    // 本类以 Receipt save → submitForApproval → approve 全链实测（wf 步骤参与者 user:$0 = SYS(id=0)）。
    @BeforeEach
    public void setUpWfUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    @Test
    public void testO2cGoldenPath() {
        // ---------- 1. B2B EDI 入站报文接收（自包含，SALES_ORDER 关联——无 erp_b2b seed，实仓核验） ----------
        ApiResponse<?> ediResp = rpcMutation("ErpB2bEdiDoc__createInbound", request("1_edi_inbound.json5", Map.class));
        output("1_edi_inbound_response.json5", ediResp);
        assertEquals(0, ediResp.getStatus(), "EDI 入站接收应成功");
        String ediDocId = idOf(ediResp);
        String ediCode = String.valueOf(((Map<?, ?>) ediResp.getData()).get("code"));
        addVar("ediDocId", ediDocId);
        addVar("ediCode", ediCode);
        assertEquals("RECEIVED", String.valueOf(((Map<?, ?>) ediResp.getData()).get("state")),
                "EDI 信封状态=RECEIVED（入站终态前状态）");

        // ---------- 2. CRM 机会自包含建数（以当前实现为准：商机 = ErpCrmLead leadType=OPPORTUNITY） ----------
        ApiResponse<?> oppSave = rpcMutation("ErpCrmLead__save", request("2_crm_opportunity_save.json5", Map.class));
        output("2_crm_opportunity_save_response.json5", oppSave);
        assertEquals(0, oppSave.getStatus(), "CRM 机会保存应成功");
        String oppId = idOf(oppSave);
        addVar("oppId", oppId);

        ApiResponse<?> oppGet = executeRpc(GraphQLOperationType.query, "ErpCrmLead__get",
                request("3_crm_opportunity_get.json5", Map.class));
        output("3_crm_opportunity_get_response.json5", oppGet);
        assertEquals(0, oppGet.getStatus(), "CRM 机会读取应成功");
        assertEquals("OPPORTUNITY", String.valueOf(((Map<?, ?>) oppGet.getData()).get("leadType")),
                "商机 leadType=OPPORTUNITY（转化来源核对）");
        assertEquals("1", String.valueOf(((Map<?, ?>) oppGet.getData()).get("partnerId")), "商机 partnerId=客户 1");

        // ---------- 3. 销售订单（自包含 1 行，EDI 订单转化）→ submit → approve ----------
        ApiResponse<?> orderSave = rpcMutation("ErpSalOrder__save", request("4_order_save.json5", Map.class));
        output("4_order_save_response.json5", orderSave);
        assertEquals(0, orderSave.getStatus(), "销售订单保存应成功");
        String orderId = idOf(orderSave);
        addVar("orderId", orderId);

        ApiResponse<?> orderLine = rpcMutation("ErpSalOrderLine__save", request("5_order_line_save.json5", Map.class));
        output("5_order_line_save_response.json5", orderLine);
        assertEquals(0, orderLine.getStatus(), "订单行保存应成功");
        addVar("orderLineId", idOf(orderLine));

        ApiResponse<?> orderSubmit = rpcMutation("ErpSalOrder__submitForApproval", request("6_order_submit.json5", Map.class));
        output("6_order_submit_response.json5", orderSubmit);
        assertEquals(0, orderSubmit.getStatus(), "销售订单提交审批应成功");

        ApiResponse<?> orderApprove = rpcMutation("ErpSalOrder__approve", request("7_order_approve.json5", Map.class));
        output("7_order_approve_response.json5", orderApprove);
        assertEquals(0, orderApprove.getStatus(), "销售订单审批应成功");
        ErpSalOrder approvedOrder = reload(ErpSalOrder.class, orderId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedOrder.getApproveStatus(),
                "订单 approveStatus=APPROVED");

        // ---------- 4. 出库单 save/行 → submit → approve（库存出库 + SALES_OUTPUT 凭证 + COGS） ----------
        ApiResponse<?> deliverySave = rpcMutation("ErpSalDelivery__save", request("8_delivery_save.json5", Map.class));
        output("8_delivery_save_response.json5", deliverySave);
        assertEquals(0, deliverySave.getStatus(), "出库单保存应成功");
        String deliveryId = idOf(deliverySave);
        String deliveryCode = String.valueOf(((Map<?, ?>) deliverySave.getData()).get("code"));
        addVar("deliveryId", deliveryId);
        addVar("deliveryCode", deliveryCode);

        ApiResponse<?> deliveryLine = rpcMutation("ErpSalDeliveryLine__save", request("9_delivery_line_save.json5", Map.class));
        output("9_delivery_line_save_response.json5", deliveryLine);
        assertEquals(0, deliveryLine.getStatus(), "出库行保存应成功");
        addVar("deliveryLineId", idOf(deliveryLine));

        ApiResponse<?> deliverySubmit = rpcMutation("ErpSalDelivery__submitForApproval", request("10_delivery_submit.json5", Map.class));
        output("10_delivery_submit_response.json5", deliverySubmit);
        assertEquals(0, deliverySubmit.getStatus(), "出库单提交审批应成功");

        ApiResponse<?> deliveryApprove = rpcMutation("ErpSalDelivery__approve", request("11_delivery_approve.json5", Map.class));
        output("11_delivery_approve_response.json5", deliveryApprove);
        assertEquals(0, deliveryApprove.getStatus(), "出库单审批应成功");
        ErpSalDelivery approvedDelivery = reload(ErpSalDelivery.class, deliveryId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedDelivery.getApproveStatus(), "出库单 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedDelivery.getPosted(), "出库审核后 posted=true");
        assertEquals(ErpSalConstants.DELIVERY_STATUS_DELIVERED, reload(ErpSalOrder.class, orderId).getDeliveryStatus(),
                "订单发货状态回写 DELIVERED");

        // 层 1 锚点：库存出库移动 + SALES_OUTPUT 凭证（COGS = 出库成本 1200，MOVING_AVERAGE 口径）
        ErpInvStockMove outMove = findStockMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, deliveryCode);
        assertNotNull(outMove, "出库审批应触发库存出库移动");
        assertEquals(ErpSalConstants.MOVE_TYPE_OUTGOING, outMove.getMoveType(), "出库移动 MOVE_TYPE=OUTGOING");
        assertEquals("DONE", outMove.getDocStatus(), "出库移动 docStatus=DONE");
        ErpFinVoucherBillR cogsLink = findBillLink(outMove.getCode());
        ErpFinVoucher cogsVoucher = requireVoucherBalanced(cogsLink, COGS, "SALES_OUTPUT");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, cogsVoucher.getDocStatus(), "SALES_OUTPUT 凭证已过账");
        List<ErpFinVoucherLine> cogsLines = findVoucherLines(cogsVoucher.getId());
        ErpFinVoucherLine cogsDebit = cogsLines.stream()
                .filter(l -> "6401".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine cogsCredit = cogsLines.stream()
                .filter(l -> "1401".equals(l.getSubjectCode())).findFirst().orElse(null);
        assertNotNull(cogsDebit, "COGS 凭证应含 6401 主营业务成本行");
        assertNotNull(cogsCredit, "COGS 凭证应含 1401 库存商品行");
        assertEquals("DEBIT", cogsDebit.getDcDirection(), "COGS 借方方向");
        assertEquals(0, COGS.compareTo(cogsDebit.getDebitAmount()), "COGS 借方=1200");
        assertEquals("CREDIT", cogsCredit.getDcDirection(), "存货贷方方向");
        assertEquals(0, COGS.compareTo(cogsCredit.getCreditAmount()), "存货贷方=1200");

        // ---------- 5. 发票 save/行 → submit → approve（AR_INVOICE 凭证 + AR 辅助账 OPEN） ----------
        ApiResponse<?> invoiceSave = rpcMutation("ErpSalInvoice__save", request("12_invoice_save.json5", Map.class));
        output("12_invoice_save_response.json5", invoiceSave);
        assertEquals(0, invoiceSave.getStatus(), "销售发票保存应成功");
        String invoiceId = idOf(invoiceSave);
        String invoiceCode = String.valueOf(((Map<?, ?>) invoiceSave.getData()).get("code"));
        addVar("invoiceId", invoiceId);
        addVar("invoiceCode", invoiceCode);

        ApiResponse<?> invoiceLine = rpcMutation("ErpSalInvoiceLine__save", request("13_invoice_line_save.json5", Map.class));
        output("13_invoice_line_save_response.json5", invoiceLine);
        assertEquals(0, invoiceLine.getStatus(), "发票行保存应成功");

        ApiResponse<?> invoiceSubmit = rpcMutation("ErpSalInvoice__submitForApproval", request("14_invoice_submit.json5", Map.class));
        output("14_invoice_submit_response.json5", invoiceSubmit);
        assertEquals(0, invoiceSubmit.getStatus(), "发票提交审批应成功");

        ApiResponse<?> invoiceApprove = rpcMutation("ErpSalInvoice__approve", request("15_invoice_approve.json5", Map.class));
        output("15_invoice_approve_response.json5", invoiceApprove);
        assertEquals(0, invoiceApprove.getStatus(), "发票审批应成功");
        ErpSalInvoice approvedInvoice = reload(ErpSalInvoice.class, invoiceId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedInvoice.getApproveStatus(), "发票 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedInvoice.getPosted(), "发票审核后 posted=true (AR_INVOICE)");

        ErpFinVoucherBillR arLink = findBillLink(invoiceCode);
        ErpFinVoucher arVoucher = requireVoucherBalanced(arLink, TOTAL_WITH_TAX, "AR_INVOICE");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, arVoucher.getDocStatus(), "AR_INVOICE 凭证已过账");
        List<ErpFinVoucherLine> arLines = findVoucherLines(arVoucher.getId());
        assertEquals(3, arLines.size(), "AR_INVOICE 凭证 3 行");
        ErpFinVoucherLine arDebit = arLines.stream()
                .filter(l -> "1131".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine revenueLine = arLines.stream()
                .filter(l -> "6001".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine vatLine = arLines.stream()
                .filter(l -> "2221".equals(l.getSubjectCode())).findFirst().orElse(null);
        assertNotNull(arDebit, "AR_INVOICE 凭证应含 1131 应收账款行");
        assertNotNull(revenueLine, "AR_INVOICE 凭证应含 6001 收入行");
        assertNotNull(vatLine, "AR_INVOICE 凭证应含 2221 销项税行");
        assertEquals("DEBIT", arDebit.getDcDirection(), "应收借方方向");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(arDebit.getDebitAmount()), "应收借方=1130");
        assertEquals("CREDIT", revenueLine.getDcDirection(), "收入贷方方向");
        assertEquals(0, TOTAL.compareTo(revenueLine.getCreditAmount()), "收入贷方=1000");
        assertEquals("CREDIT", vatLine.getDcDirection(), "销项税贷方方向");
        assertEquals(0, TAX.compareTo(vatLine.getCreditAmount()), "销项税贷方=130");

        ErpFinArApItem invoiceItem = findApItem(ErpFinConstants.SOURCE_BILL_AR_INVOICE, invoiceCode);
        assertNotNull(invoiceItem, "AR_INVOICE 过账应生成应收辅助账");
        assertEquals(ErpFinConstants.DIRECTION_RECEIVABLE, invoiceItem.getDirection(), "方向=应收");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, invoiceItem.getStatus(), "初始 OPEN");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(invoiceItem.getOpenAmountFunctional()), "发票辅助账 openAmount=1130");

        // 层 1 锚点（GL 口径裁决落地）：发票审核后 GL 应收余额（1131 VoucherLine 聚合）= AR 总额（客户 1）= 1130
        assertEquals(0, TOTAL_WITH_TAX.compareTo(glReceivableBalance()), "GL 应收余额=1130");
        assertEquals(0, glReceivableBalance().compareTo(arReceivableOpen("1")), "GL 应收余额与 AR 总额一致");

        // ---------- 6. 收款单（xwf 轴 setUserId("0") 后端驱动）→ submit → approve（RECEIPT 凭证 + 收款辅助账） ----------
        ApiResponse<?> receiptSave = rpcMutation("ErpSalReceipt__save", request("16_receipt_save.json5", Map.class));
        output("16_receipt_save_response.json5", receiptSave);
        assertEquals(0, receiptSave.getStatus(), "收款单保存应成功");
        String receiptId = idOf(receiptSave);
        String receiptCode = String.valueOf(((Map<?, ?>) receiptSave.getData()).get("code"));
        addVar("receiptId", receiptId);
        addVar("receiptCode", receiptCode);

        ApiResponse<?> receiptSubmit = rpcMutation("ErpSalReceipt__submitForApproval", request("17_receipt_submit.json5", Map.class));
        output("17_receipt_submit_response.json5", receiptSubmit);
        assertEquals(0, receiptSubmit.getStatus(), "收款提交审批应成功（SYS caller 匹配 wf 步骤参与者 user:$0）");

        ApiResponse<?> receiptApprove = rpcMutation("ErpSalReceipt__approve", request("18_receipt_approve.json5", Map.class));
        output("18_receipt_approve_response.json5", receiptApprove);
        assertEquals(0, receiptApprove.getStatus(), "收款审批应成功");
        ErpSalReceipt approvedReceipt = reload(ErpSalReceipt.class, receiptId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedReceipt.getApproveStatus(),
                "xwf 复核判据：Receipt approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedReceipt.getPosted(), "收款审核后 posted=true (RECEIPT)");

        ErpFinVoucherBillR receiptLink = findBillLink(receiptCode);
        ErpFinVoucher receiptVoucher = requireVoucherBalanced(receiptLink, TOTAL_WITH_TAX, "RECEIPT");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, receiptVoucher.getDocStatus(), "RECEIPT 凭证已过账");
        List<ErpFinVoucherLine> receiptLines = findVoucherLines(receiptVoucher.getId());
        assertEquals(2, receiptLines.size(), "RECEIPT 凭证 2 行");
        ErpFinVoucherLine bankLine = receiptLines.stream()
                .filter(l -> "1002".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine arCredit = receiptLines.stream()
                .filter(l -> "1131".equals(l.getSubjectCode())).findFirst().orElse(null);
        assertNotNull(bankLine, "RECEIPT 凭证应含 1002 银行存款行");
        assertNotNull(arCredit, "RECEIPT 凭证应含 1131 应收账款行");
        assertEquals("DEBIT", bankLine.getDcDirection(), "银行存款借方方向");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(bankLine.getDebitAmount()), "银行存款借方=1130");
        assertEquals("CREDIT", arCredit.getDcDirection(), "应收贷方方向");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(arCredit.getCreditAmount()), "应收贷方=1130");

        ErpFinArApItem receiptItem = findApItem(ErpFinConstants.SOURCE_BILL_RECEIPT, receiptCode);
        assertNotNull(receiptItem, "RECEIPT 过账应生成收款辅助账");
        assertEquals(ErpFinConstants.DIRECTION_RECEIVABLE, receiptItem.getDirection(), "方向=应收");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(receiptItem.getOpenAmountFunctional()), "收款辅助账 openAmount=1130");

        // ---------- 7. 独立核销动作 ErpSalReceipt__settle（域级：发票 receivedStatus / 收款 writtenOffStatus） ----------
        ApiResponse<?> settle = rpcMutation("ErpSalReceipt__settle", request("19_receipt_settle.json5", Map.class));
        output("19_receipt_settle_response.json5", settle);
        assertEquals(0, settle.getStatus(), "收款核销应成功");
        ErpSalInvoice settledInvoice = reload(ErpSalInvoice.class, invoiceId);
        ErpSalReceipt settledReceipt = reload(ErpSalReceipt.class, receiptId);
        assertEquals(0, TOTAL_WITH_TAX.compareTo(settledInvoice.getReceivedAmount()), "发票已收=1130");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_RECEIVED, settledInvoice.getReceivedStatus(), "发票 receivedStatus=RECEIVED");
        assertEquals(ErpSalConstants.RECEIVED_STATUS_RECEIVED, settledReceipt.getWrittenOffStatus(), "收款 writtenOffStatus=RECEIVED");

        // ---------- 8. 财务正式核销单（ErpFinReconciliation create + post）：AR 辅助账 openAmount→0 + SETTLED ----------
        ErpFinReconciliation recon = ormTemplate.runInSession(session -> reconciliationBiz.create(
                ErpFinConstants.DIRECTION_RECEIVABLE, "1", LocalDate.of(2026, 7, 18),
                Collections.singletonList(reconLine(receiptItem.getId(), invoiceItem.getId(), "1130")), CTX));
        ormTemplate.runInSession(() -> reconciliationBiz.post(recon.getId(), CTX));
        ErpFinReconciliation postedRecon = reload(ErpFinReconciliation.class, recon.getId());
        Map<String, Object> reconState = new LinkedHashMap<>();
        reconState.put("id", postedRecon.getId());
        reconState.put("docStatus", postedRecon.getDocStatus());
        reconState.put("totalAmountFunctional", postedRecon.getTotalAmountFunctional());
        output("20_reconciliation_post_response.json5", reconState);
        assertEquals(ErpFinConstants.RECON_STATUS_POSTED, postedRecon.getDocStatus(), "核销单 POSTED");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(postedRecon.getTotalAmountFunctional()), "核销单总额=1130");

        // 层 1 锚点：核销后 AR 辅助账 openAmount=0（发票项 + 收款项）+ SETTLED
        ErpFinArApItem settledInvoiceItem = reload(ErpFinArApItem.class, invoiceItem.getId());
        ErpFinArApItem settledReceiptItem = reload(ErpFinArApItem.class, receiptItem.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(settledInvoiceItem.getOpenAmountFunctional()), "核销后发票辅助账 openAmount=0");
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, settledInvoiceItem.getStatus(), "发票辅助账 SETTLED");
        assertEquals(0, TOTAL_WITH_TAX.compareTo(settledInvoiceItem.getSettledAmountFunctional()), "发票辅助账 settledAmount=1130");
        assertEquals(0, BigDecimal.ZERO.compareTo(settledReceiptItem.getOpenAmountFunctional()), "核销后收款辅助账 openAmount=0");
        assertEquals(ErpFinConstants.AR_AP_STATUS_SETTLED, settledReceiptItem.getStatus(), "收款辅助账 SETTLED");

        // 层 1 锚点（GL 口径裁决落地）：核销后 AR 总额归零 = GL 应收余额归零（0=0）
        assertEquals(0, BigDecimal.ZERO.compareTo(arReceivableOpen("1")), "核销后 AR 总额=0");
        assertEquals(0, BigDecimal.ZERO.compareTo(glReceivableBalance()), "核销后 GL 应收余额=0");
        assertEquals(0, glReceivableBalance().compareTo(arReceivableOpen("1")), "GL 应收余额与 AR 总额一致");
    }

    // ---------- helpers ----------

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

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
     * GL 应收余额（对齐 B1 Phase 1 Decision 裁决口径）：应收账款科目（1131）VoucherLine 聚合，
     * businessType ∈ {AR_INVOICE, RECEIPT} 的 debit−credit 净额。seed 应收凭证行科目为 1122，
     * 不混入 1131 聚合；gl_balance 表为 seed 静态不作断言源。
     */
    private BigDecimal glReceivableBalance() {
        BigDecimal net = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : daoAll(ErpFinVoucherLine.class)) {
            if (!"1131".equals(l.getSubjectCode())) {
                continue;
            }
            String bt = l.getBusinessType();
            if (!ErpFinConstants.SOURCE_BILL_AR_INVOICE.equals(bt)
                    && !ErpFinConstants.SOURCE_BILL_RECEIPT.equals(bt)) {
                continue;
            }
            net = net.add(nz(l.getDebitAmount())).subtract(nz(l.getCreditAmount()));
        }
        return net;
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
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : daoAll(ErpFinArApItem.class)) {
            if (it.getOpenAmountFunctional() == null) {
                continue;
            }
            boolean matched = partnerId.equals(it.getPartnerId())
                    && ErpFinConstants.DIRECTION_RECEIVABLE.equals(it.getDirection())
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

    private <T extends io.nop.orm.IOrmEntity> List<T> daoAll(Class<T> clazz) {
        return daoProvider.daoFor(clazz).findAllByQuery(new QueryBean());
    }
}