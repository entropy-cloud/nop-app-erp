package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalInvoiceLineBiz;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.dao.entity.ErpSalReceiptLine;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.gt;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 销售退货退款编排器（{@code returns.md §退款}）。退货审核通过后，按客户应收收款状态路由：
 *
 * <ul>
 *   <li><b>未收款退货</b>：SALES_RETURN 过账生成负 AR 辅助账（credit memo）即回减 {@code receivableBalance}，
 *       本类无需额外动作。</li>
 *   <li><b>已收款退货</b>：客户原发票已被收款核销时，对每条已核销 (receipt, invoice) 生成反向收款核销行
 *       （负金额，复用 {@link ReceiptSettler#reverseSettlement}），回写 {@code ErpSalInvoice.receivedStatus}/
 *       {@code receivedAmount} 与 {@code ErpSalReceipt.writtenOffStatus}，使应收/退款闭环一致。</li>
 * </ul>
 *
 * <p>退款**方式路由**（原路退回/其他账户/预收款抵扣/现金）属 {@code treasury.md} 资金面，为本计划 Non-Goal。
 *
 * <p>本类为非 BizModel 服务（intra-module 聚合写），对齐 {@code PaymentSettler}/{@code ReceiptSettler} 用
 * {@code daoFor} 处理同模块实体的模式；不跨 REQUIRES_NEW 边界（域内核销回写为纯域内操作）。
 */
public class ReturnRefundOrchestrator {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpSalInvoiceLineBiz invoiceLineBiz;

    @Inject
    ReceiptSettler receiptSettler;

    /**
     * 退货审核后调用。对客户已收款核销的发票，生成反向核销行恢复发票/收款余额与状态。无已核销记录时空操作。
     *
     * <p>P1-CK-sal-001 修复：反转核销**限定退货关联发票**（经退货行 deliveryLineId → 发票行链路，
     * 对齐 {@code ErpSalReturnProcessor.validateInvoiceNotSettled} 既有链路解析）——修复前按客户
     * 全量反转（无关联发票的未开票退货也会反转客户名下其他订单发票的核销）；无关联发票 → 跳过
     * （与 owner doc returns.md RC-R1.19「无关联发票跳过」一致）。
     */
    public void orchestrateRefund(ErpSalReturn returnOrder) {
        if (returnOrder.getCustomerId() == null) {
            return;
        }
        Set<String> relatedInvoiceIds = relatedInvoiceIds(returnOrder);
        if (relatedInvoiceIds.isEmpty()) {
            return;
        }
        List<ErpSalInvoice> receivedInvoices = findReceivedInvoicesOfCustomer(returnOrder.getCustomerId(),
                relatedInvoiceIds);
        for (ErpSalInvoice invoice : receivedInvoices) {
            reverseSettlementsForInvoice(invoice);
        }
    }

    /** 退货关联发票集合：退货行 deliveryLineId → ErpSalInvoiceLine.deliveryLineId → invoiceId。 */
    private Set<String> relatedInvoiceIds(ErpSalReturn returnOrder) {
        Set<String> deliveryLineIds = new HashSet<>();
        for (ErpSalReturnLine line : returnOrder.getLines()) {
            if (line.getDeliveryLineId() != null) {
                deliveryLineIds.add(line.getDeliveryLineId());
            }
        }
        if (deliveryLineIds.isEmpty()) {
            return Collections.emptySet();
        }
        // 经 I*Biz（对齐跨实体访问纪律，不新增 daoFor 站点）
        QueryBean q = new QueryBean();
        q.addFilter(in("deliveryLineId", deliveryLineIds));
        Set<String> invoiceIds = new HashSet<>();
        // plan 2026-08-31-1426-2 修正：I*Biz 化时 context 误传 null——管道内 getEvalScope() NPE；
        // 显式构造 ServiceContextImpl（无请求上下文的内部编排场景）。
        for (ErpSalInvoiceLine il : invoiceLineBiz.findList(q, null, new io.nop.core.context.ServiceContextImpl())) {
            if (il.getInvoiceId() != null) {
                invoiceIds.add(il.getInvoiceId());
            }
        }
        return invoiceIds;
    }

    /**
     * 退货红冲（反审核/作废）时调用。已退款（已生成反向核销行）的发票恢复其原核销。
     * MVP 实现：反向核销行已使发票 receivedAmount 自然回退，红冲时仅取消退货自身的负 AR 辅助账（由 finance
     * cancelOnReverse 完成）；如需恢复原收款核销可在此扩展。当前为空操作（保留闭环对称占位）。
     */
    public void restoreRefund(ErpSalReturn returnOrder) {
        // MVP：退款红冲经红字凭证 cancelOnReverse 取消负 AR 辅助账即可；原收款核销的反向冲销恢复
        // 属退款方式路由（treasury 面）Non-Goal，触发条件满足时再扩展。
    }

    private List<ErpSalInvoice> findReceivedInvoicesOfCustomer(String customerId, Set<String> invoiceIds) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("customerId", customerId),
                eq("approveStatus", ErpSalConstants.APPROVE_STATUS_APPROVED),
                gt("receivedAmount", BigDecimal.ZERO),
                in("id", invoiceIds)));
        return dao.findAllByQuery(q);
    }

    private void reverseSettlementsForInvoice(ErpSalInvoice invoice) {
        IEntityDao<ErpSalReceiptLine> lineDao = daoProvider.daoFor(ErpSalReceiptLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("invoiceId", invoice.getId()), gt("amount", BigDecimal.ZERO)));
        List<ErpSalReceiptLine> positiveLines = lineDao.findAllByQuery(q);
        if (positiveLines.isEmpty()) {
            return;
        }
        Set<String> touchedReceipts = new HashSet<>();
        for (ErpSalReceiptLine line : positiveLines) {
            if (touchedReceipts.contains(line.getReceiptId())) {
                continue;
            }
            touchedReceipts.add(line.getReceiptId());
            ErpSalReceipt receipt = line.getReceipt();
            if (receipt == null) {
                continue;
            }
            receiptSettler.reverseSettlement(receipt, invoice.getId());
        }
    }
}
