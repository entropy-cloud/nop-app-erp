package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * M0.2 试点用例：P2P 简化链（按 M0.1 设计文档 C01 规格裁剪）。
 *
 * <p>采购到付款核心闭环的前三段：PO 审批 → 收货审批（触发入库移动 + 暂估凭证）→ 发票审批
 * （AP_INVOICE 凭证 + AP 辅助账）。全部自包含数据经 GraphQL 动作创建（引用部署 seed：
 * 组织 2 / SUP-001 供应商 3 / MAT-001 物料 1 / WH-RAW 仓库 2 / CNY 币种 1 / 2026-07 OPEN 期间）。
 *
 * <p>三层验证模型：层 1 = JUnit 关键断言（审批状态翻转 / posted / 凭证借贷平衡 / AP 辅助账 openAmount）；
 * 层 2 = 每步 response 快照；层 3 = output/tables 变更行快照（自动录制）。
 *
 * <p>RECORDING→CHECKING 往返：本类经 RECORDING 完成首次录制后已切回默认 CHECKING 复跑校验全绿
 * （M0.2 机制裁决待证风险 ①/②/③/④/⑤ 的实证载体，逐项结论见设计文档 §3.4）。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpP2pPilot extends ErpIntegrationTestCase {

    @Test
    public void testP2pChain() {
        // 1. 采购订单保存（自包含，引用 seed 供应商/物料/仓库）
        ApiResponse<?> poSave = rpcMutation("ErpPurOrder__save", request("1_order_save.json5", Map.class));
        output("1_order_save_response.json5", poSave);
        assertEquals(0, poSave.getStatus(), "PO 保存应成功");
        String poId = idOf(poSave);
        addVar("poId", poId);

        // 2. 采购订单行保存（10 × 5 = 50）
        ApiResponse<?> poLineSave = rpcMutation("ErpPurOrderLine__save",
                request("2_order_line_save.json5", Map.class));
        output("2_order_line_save_response.json5", poLineSave);
        assertEquals(0, poLineSave.getStatus(), "PO 行保存应成功");
        String poLineId = idOf(poLineSave);
        addVar("poLineId", poLineId);

        // 3. PO 提交审批
        ApiResponse<?> poSubmit = submitForApproval("ErpPurOrder", poId);
        output("3_order_submit_response.json5", poSubmit);
        assertEquals(0, poSubmit.getStatus(), "PO 提交审批应成功");

        // 4. PO 审批（DIRECT 轴）→ APPROVED
        ApiResponse<?> poApprove = approve("ErpPurOrder", poId);
        output("4_order_approve_response.json5", poApprove);
        assertEquals(0, poApprove.getStatus(), "PO 审批应成功");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED,
                reload(ErpPurOrder.class, poId).getApproveStatus(), "PO approveStatus=APPROVED");

        // 5. 收货单保存（引用 PO）
        ApiResponse<?> rcvSave = rpcMutation("ErpPurReceive__save",
                request("5_receive_save.json5", Map.class));
        output("5_receive_save_response.json5", rcvSave);
        assertEquals(0, rcvSave.getStatus(), "收货单保存应成功");
        String receiveId = idOf(rcvSave);
        String receiveCode = String.valueOf(((Map<?, ?>) rcvSave.getData()).get("code"));
        addVar("receiveId", receiveId);
        addVar("receiveCode", receiveCode);

        // 6. 收货行保存（引用 PO 行）
        ApiResponse<?> rcvLineSave = rpcMutation("ErpPurReceiveLine__save",
                request("6_receive_line_save.json5", Map.class));
        output("6_receive_line_save_response.json5", rcvLineSave);
        assertEquals(0, rcvLineSave.getStatus(), "收货行保存应成功");
        String receiveLineId = idOf(rcvLineSave);
        addVar("receiveLineId", receiveLineId);

        // 7. 收货单提交审批
        ApiResponse<?> rcvSubmit = submitForApproval("ErpPurReceive", receiveId);
        output("7_receive_submit_response.json5", rcvSubmit);
        assertEquals(0, rcvSubmit.getStatus(), "收货单提交审批应成功");

        // 8. 收货单审批（DIRECT 轴）→ APPROVED + 触发入库移动 DONE + 暂估凭证
        ApiResponse<?> rcvApprove = approve("ErpPurReceive", receiveId);
        output("8_receive_approve_response.json5", rcvApprove);
        assertEquals(0, rcvApprove.getStatus(), "收货单审批应成功");
        ErpPurReceive approvedReceive = reload(ErpPurReceive.class, receiveId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedReceive.getApproveStatus(),
                "收货单 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedReceive.getPosted(), "收货审核后 posted=true");
        ErpInvStockMove move = findStockMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receiveCode);
        assertNotNull(move, "收货审批应触发入库移动");
        assertEquals("DONE", move.getDocStatus(), "入库移动 docStatus=DONE");
        assertEquals(Boolean.TRUE, move.getPosted(), "入库移动 posted=true");

        // 9. 采购发票保存（引用 PO/收货）
        ApiResponse<?> invSave = rpcMutation("ErpPurInvoice__save",
                request("9_invoice_save.json5", Map.class));
        output("9_invoice_save_response.json5", invSave);
        assertEquals(0, invSave.getStatus(), "发票保存应成功");
        String invoiceId = idOf(invSave);
        String invoiceCode = String.valueOf(((Map<?, ?>) invSave.getData()).get("code"));
        addVar("invoiceId", invoiceId);
        addVar("invoiceCode", invoiceCode);

        // 10. 发票行保存（引用收货行，taxRate=13）
        ApiResponse<?> invLineSave = rpcMutation("ErpPurInvoiceLine__save",
                request("10_invoice_line_save.json5", Map.class));
        output("10_invoice_line_save_response.json5", invLineSave);
        assertEquals(0, invLineSave.getStatus(), "发票行保存应成功");

        // 11. 发票提交审批
        ApiResponse<?> invSubmit = submitForApproval("ErpPurInvoice", invoiceId);
        output("11_invoice_submit_response.json5", invSubmit);
        assertEquals(0, invSubmit.getStatus(), "发票提交审批应成功");

        // 12. 发票审批（DIRECT 轴）→ APPROVED + AP_INVOICE 凭证 + AP 辅助账
        ApiResponse<?> invApprove = approve("ErpPurInvoice", invoiceId);
        output("12_invoice_approve_response.json5", invApprove);
        assertEquals(0, invApprove.getStatus(), "发票审批应成功");
        ErpPurInvoice approvedInvoice = reload(ErpPurInvoice.class, invoiceId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedInvoice.getApproveStatus(),
                "发票 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedInvoice.getPosted(), "发票审核后 posted=true (AP_INVOICE)");

        // 层 1 锚点：AP_INVOICE 凭证借贷平衡（Dr 50 费用 + Dr 6.5 进项税 / Cr 56.5 应付）
        ErpFinVoucherBillR link = findBillLink(invoiceCode);
        ErpFinVoucher voucher = requireVoucherBalanced(link, new BigDecimal("56.5"), "AP_INVOICE");

        // 层 1 锚点：AP 辅助账 OPEN + openAmount=56.5
        ErpFinArApItem apItem = findApItem(ErpFinConstants.SOURCE_BILL_AP_INVOICE, invoiceCode);
        assertNotNull(apItem, "AP_INVOICE 过账应生成应付辅助账");
        assertEquals(ErpFinConstants.DIRECTION_PAYABLE, apItem.getDirection(), "方向=应付");
        assertEquals(ErpFinConstants.AR_AP_STATUS_OPEN, apItem.getStatus(), "初始 OPEN");
        assertEquals(0, new BigDecimal("56.5").compareTo(apItem.getOpenAmountFunctional()),
                "AP 辅助账 openAmount=56.5");
    }
}