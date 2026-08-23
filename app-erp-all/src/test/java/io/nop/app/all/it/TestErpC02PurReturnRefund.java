package io.nop.app.all.it;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B1 C02：采购退货与退款闭环（按 {@code docs/design/integration-testing.md §6 C02} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / SUP-001 供应商 3 / MAT-001 物料 1 / WH-RAW 仓库 2 /
 * CNY 币种 1 / 2026-07 OPEN 期间）：自包含已过账采购链（PO → Receive → Invoice posted，得 OPEN AP 项
 * +56.5）；合同数据来源按 Phase 2 Decision 落地 = 自包含 {@code ErpCtContract__save}（无 erp_ct_contract
 * seed，实仓核验 94 CSV 无 contract 文件）→ {@code ErpCtContract__get} 前置核对退货条款；退货单
 * save → submit → approve（DIRECT 审批轴：反向 OUTGOING 出库移动 + PURCHASE_RETURN 红字凭证 +
 * DIRECTION_PAYABLE 负 openAmount 辅助账 credit memo）；退款核销/open items 反查
 * （{@code IErpFinArApItemBiz.findOpenItemsByPartner}，对齐 {@code TestErpPurReturnRefundEndToEnd}
 * 红字/负项断言范式）。
 *
 * <p>三层验证：层 1 = JUnit 关键断言；层 2 = 每步 response 快照；层 3 = output/tables 变更行。
 * RECORDING→CHECKING 往返已按 M0.2 试点范式完成（录制后切回默认 CHECKING 复跑全绿）。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC02PurReturnRefund extends ErpIntegrationTestCase {

    static final BigDecimal INVOICE_TOTAL = new BigDecimal("56.5"); // 含税（10 × 5 + 13% 税）
    static final BigDecimal RETURN_AMOUNT = new BigDecimal("20");   // 4 × 5
    static final String RETURN_CLAUSE = "退货条款：收货后 30 天内可申请退货，退货金额按原单价冲减";

    static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IErpFinArApItemBiz arApItemBiz;
    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testPurReturnRefund() {
        // ---------- 0. 合同数据（Phase 2 Decision = 自包含 ErpCtContract__save）→ get 前置核对退货条款 ----------
        ApiResponse<?> ctSave = rpcMutation("ErpCtContract__save", request("1_contract_save.json5", Map.class));
        output("1_contract_save_response.json5", ctSave);
        assertEquals(0, ctSave.getStatus(), "合同保存应成功");
        String contractId = idOf(ctSave);
        addVar("contractId", contractId);

        ApiResponse<?> ctGet = executeRpc(GraphQLOperationType.query, "ErpCtContract__get",
                request("2_contract_get.json5", Map.class));
        output("2_contract_get_response.json5", ctGet);
        assertEquals(0, ctGet.getStatus(), "合同读取应成功");
        String description = String.valueOf(((Map<?, ?>) ctGet.getData()).get("description"));
        assertTrue(description != null && description.contains("退货条款"),
                "合同应含退货条款（前置核对）：" + description);

        // ---------- 1. 自包含已过账采购链：PO → Receive（入库 + 暂估）→ Invoice（AP_INVOICE + AP 项 OPEN） ----------
        ApiResponse<?> poSave = rpcMutation("ErpPurOrder__save", request("3_order_save.json5", Map.class));
        output("3_order_save_response.json5", poSave);
        assertEquals(0, poSave.getStatus(), "PO 保存应成功");
        String poId = idOf(poSave);
        addVar("poId", poId);

        ApiResponse<?> poLine = rpcMutation("ErpPurOrderLine__save", request("4_order_line_save.json5", Map.class));
        output("4_order_line_save_response.json5", poLine);
        assertEquals(0, poLine.getStatus(), "PO 行保存应成功");
        addVar("poLineId", idOf(poLine));

        ApiResponse<?> poSubmit = rpcMutation("ErpPurOrder__submitForApproval", request("5_order_submit.json5", Map.class));
        output("5_order_submit_response.json5", poSubmit);
        assertEquals(0, poSubmit.getStatus(), "PO 提交审批应成功");
        ApiResponse<?> poApprove = rpcMutation("ErpPurOrder__approve", request("6_order_approve.json5", Map.class));
        output("6_order_approve_response.json5", poApprove);
        assertEquals(0, poApprove.getStatus(), "PO 审批应成功");

        ApiResponse<?> rcvSave = rpcMutation("ErpPurReceive__save", request("7_receive_save.json5", Map.class));
        output("7_receive_save_response.json5", rcvSave);
        assertEquals(0, rcvSave.getStatus(), "收货单保存应成功");
        String receiveId = idOf(rcvSave);
        String receiveCode = String.valueOf(((Map<?, ?>) rcvSave.getData()).get("code"));
        addVar("receiveId", receiveId);
        addVar("receiveCode", receiveCode);

        ApiResponse<?> rcvLine = rpcMutation("ErpPurReceiveLine__save", request("8_receive_line_save.json5", Map.class));
        output("8_receive_line_save_response.json5", rcvLine);
        assertEquals(0, rcvLine.getStatus(), "收货行保存应成功");
        addVar("receiveLineId", idOf(rcvLine));

        ApiResponse<?> rcvSubmit = rpcMutation("ErpPurReceive__submitForApproval", request("9_receive_submit.json5", Map.class));
        output("9_receive_submit_response.json5", rcvSubmit);
        assertEquals(0, rcvSubmit.getStatus(), "收货单提交审批应成功");
        ApiResponse<?> rcvApprove = rpcMutation("ErpPurReceive__approve", request("10_receive_approve.json5", Map.class));
        output("10_receive_approve_response.json5", rcvApprove);
        assertEquals(0, rcvApprove.getStatus(), "收货单审批应成功");
        ErpPurReceive approvedReceive = reload(ErpPurReceive.class, receiveId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedReceive.getApproveStatus(), "收货单 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedReceive.getPosted(), "收货审核后 posted=true");

        ApiResponse<?> invSave = rpcMutation("ErpPurInvoice__save", request("11_invoice_save.json5", Map.class));
        output("11_invoice_save_response.json5", invSave);
        assertEquals(0, invSave.getStatus(), "发票保存应成功");
        String invoiceId = idOf(invSave);
        String invoiceCode = String.valueOf(((Map<?, ?>) invSave.getData()).get("code"));
        addVar("invoiceId", invoiceId);
        addVar("invoiceCode", invoiceCode);

        ApiResponse<?> invLine = rpcMutation("ErpPurInvoiceLine__save", request("12_invoice_line_save.json5", Map.class));
        output("12_invoice_line_save_response.json5", invLine);
        assertEquals(0, invLine.getStatus(), "发票行保存应成功");

        ApiResponse<?> invSubmit = rpcMutation("ErpPurInvoice__submitForApproval", request("13_invoice_submit.json5", Map.class));
        output("13_invoice_submit_response.json5", invSubmit);
        assertEquals(0, invSubmit.getStatus(), "发票提交审批应成功");
        ApiResponse<?> invApprove = rpcMutation("ErpPurInvoice__approve", request("14_invoice_approve.json5", Map.class));
        output("14_invoice_approve_response.json5", invApprove);
        assertEquals(0, invApprove.getStatus(), "发票审批应成功");
        ErpPurInvoice approvedInvoice = reload(ErpPurInvoice.class, invoiceId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedInvoice.getApproveStatus(), "发票 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedInvoice.getPosted(), "发票审核后 posted=true (AP_INVOICE)");

        ErpFinVoucherBillR apLink = findBillLink(invoiceCode);
        requireVoucherBalanced(apLink, INVOICE_TOTAL, "AP_INVOICE");
        ErpFinArApItem apItem = findApItem(ErpFinConstants.SOURCE_BILL_AP_INVOICE, invoiceCode);
        assertNotNull(apItem, "已过账采购链应生成 OPEN AP 项");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, apItem.getStatus(), "AP 项 OPEN");
        assertEquals(0, INVOICE_TOTAL.compareTo(apItem.getOpenAmountFunctional()), "AP 项 openAmount=56.5");

        // ---------- 2. 退货单 save → submit → approve（反向出库 + PURCHASE_RETURN 红字凭证 + 负 AP） ----------
        ApiResponse<?> rtSave = rpcMutation("ErpPurReturn__save", request("15_return_save.json5", Map.class));
        output("15_return_save_response.json5", rtSave);
        assertEquals(0, rtSave.getStatus(), "退货单保存应成功");
        String returnId = idOf(rtSave);
        String returnCode = String.valueOf(((Map<?, ?>) rtSave.getData()).get("code"));
        addVar("returnId", returnId);
        addVar("returnCode", returnCode);

        ApiResponse<?> rtLine = rpcMutation("ErpPurReturnLine__save", request("16_return_line_save.json5", Map.class));
        output("16_return_line_save_response.json5", rtLine);
        assertEquals(0, rtLine.getStatus(), "退货行保存应成功");

        ApiResponse<?> rtSubmit = rpcMutation("ErpPurReturn__submitForApproval", request("17_return_submit.json5", Map.class));
        output("17_return_submit_response.json5", rtSubmit);
        assertEquals(0, rtSubmit.getStatus(), "退货单提交审批应成功");
        ApiResponse<?> rtApprove = rpcMutation("ErpPurReturn__approve", request("18_return_approve.json5", Map.class));
        output("18_return_approve_response.json5", rtApprove);
        assertEquals(0, rtApprove.getStatus(), "退货单审批应成功");

        // 层 1 锚点：Return approveStatus=APPROVED + posted=true
        ErpPurReturn approvedReturn = reload(ErpPurReturn.class, returnId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedReturn.getApproveStatus(), "退货单 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedReturn.getPosted(), "退货审核后 posted=true (PURCHASE_RETURN)");

        // 层 1 锚点：反向出库移动存在（relatedBill 反查，OUTGOING；posted 由退货红字凭证承载，
        // 移动单本身不置 posted——以当前实现为准，对齐设计文档 §6 C02「反向出库移动存在」断言）
        ErpInvStockMove reverseMove = findStockMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RETURN, returnCode);
        assertNotNull(reverseMove, "退货审批应触发反向出库移动");
        assertEquals(ErpPurConstants.MOVE_TYPE_OUTGOING, reverseMove.getMoveType(), "反向出库 MOVE_TYPE=OUTGOING");
        assertEquals("DONE", reverseMove.getDocStatus(), "反向出库 docStatus=DONE");

        // 层 1 锚点：PURCHASE_RETURN 红字凭证借贷平衡（借 2202 暂估应付 / 贷 1401 存货 = 反向 PURCHASE_INPUT）
        ErpFinVoucherBillR rtLink = findBillLink(returnCode);
        ErpFinVoucher returnVoucher = requireVoucherBalanced(rtLink, RETURN_AMOUNT, "PURCHASE_RETURN");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, returnVoucher.getDocStatus(), "红字凭证已过账");
        List<ErpFinVoucherLine> rtLines = findVoucherLines(returnVoucher.getId());
        assertEquals(2, rtLines.size(), "PURCHASE_RETURN 凭证 2 行");
        ErpFinVoucherLine rtApLine = rtLines.stream()
                .filter(l -> "2202".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine rtInvLine = rtLines.stream()
                .filter(l -> "1401".equals(l.getSubjectCode())).findFirst().orElse(null);
        assertNotNull(rtApLine, "红字凭证应含 2202 暂估应付行");
        assertNotNull(rtInvLine, "红字凭证应含 1401 存货行");
        assertEquals("DEBIT", rtApLine.getDcDirection(), "红字方向：2202 借方（反向 PURCHASE_INPUT 贷方）");
        assertEquals(0, RETURN_AMOUNT.compareTo(rtApLine.getDebitAmount()), "红字 2202 借方=20");
        assertEquals("CREDIT", rtInvLine.getDcDirection(), "红字方向：1401 贷方");
        assertEquals(0, RETURN_AMOUNT.compareTo(rtInvLine.getCreditAmount()), "红字 1401 贷方=20");

        // 层 1 锚点：AP 项负向登记（credit memo，openAmount = −total）
        ErpFinArApItem returnItem = findApItem(ErpFinConstants.SOURCE_BILL_PUR_RETURN, returnCode);
        assertNotNull(returnItem, "退货过账应生成负 AP 辅助账");
        assertEquals(ErpFinConstants.DIRECTION_PAYABLE, returnItem.getDirection(), "方向=应付");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, returnItem.getStatus(), "退货辅助账 OPEN");
        assertEquals(0, RETURN_AMOUNT.negate().compareTo(returnItem.getOpenAmountFunctional()),
                "openAmount = −20（credit memo）");

        // 层 1 锚点：余额回减断言（sumOpen 口径，供应商 3：56.5 − 20 = 36.5）
        assertEquals(0, new BigDecimal("36.5").compareTo(apPayableOpen("3")),
                "应付余额回减 = 36.5（56.5 − 20）");

        // 层 1 锚点：退款核销/open items 反查（findOpenItemsByPartner：负项在位 + 总额回减）
        List<ErpFinArApItem> openItems = arApItemBiz.findOpenItemsByPartner("3",
                ErpFinConstants.DIRECTION_PAYABLE, CTX);
        boolean hasReturnItem = openItems.stream().anyMatch(it -> ErpFinConstants.SOURCE_BILL_PUR_RETURN
                .equals(it.getSourceBillType()) && returnCode.equals(it.getSourceBillCode()));
        assertTrue(hasReturnItem, "open items 反查应含退货负项");
        BigDecimal openSum = openItems.stream().map(it -> nz(it.getOpenAmountFunctional()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("36.5").compareTo(openSum), "open items 合计 = 36.5");
    }

    // ---------- helpers ----------

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    /**
     * 供应商应付（sumOpen 口径，对齐 TestErpPurReturnRefundEndToEnd / PartnerBalanceUpdater.sumOpen）：
     * PAYABLE 方向 openAmountFunctional 合计，排除 SETTLED/CANCELLED。
     */
    private BigDecimal apPayableOpen(String partnerId) {
        List<ErpFinArApItem> items = daoProvider.daoFor(ErpFinArApItem.class).findAllByQuery(new QueryBean());
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem it : items) {
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
}