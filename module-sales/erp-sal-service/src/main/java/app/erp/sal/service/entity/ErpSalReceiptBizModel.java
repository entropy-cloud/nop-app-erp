
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalReceiptBiz;
import app.erp.md.biz.SettlementAllocation;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.service.processor.ErpSalReceiptProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 收款单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalReceiptProcessor#onSubmit}/{@link ErpSalReceiptProcessor#onApproved}/{@link ErpSalReceiptProcessor#onReverseApproved}。
 */
@BizModel("ErpSalReceipt")
public class ErpSalReceiptBizModel extends CrudBizModel<ErpSalReceipt> implements IErpSalReceiptBiz {

    @Inject
    ErpSalReceiptProcessor receiptProcessor;

    public ErpSalReceiptBizModel() {
        setEntityName(ErpSalReceipt.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalReceipt cancel(@Name("receiptId") Long receiptId, IServiceContext context) {
        return receiptProcessor.cancel(String.valueOf(receiptId), context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt settle(@Name("receiptId") Long receiptId,
                                @Name("allocations") List<SettlementAllocation> allocations,
                                IServiceContext context) {
        return receiptProcessor.settle(String.valueOf(receiptId), allocations, context);
    }

    @Override
    @BizMutation
    public ErpSalReceipt reverseSettlement(@Name("receiptId") Long receiptId,
                                           @Name("invoiceId") Long invoiceId,
                                           IServiceContext context) {
        return receiptProcessor.reverseSettlement(String.valueOf(receiptId), invoiceId, context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------
    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

    @BizLoader(forType = ErpSalReceipt.class)
    public List<String> customerName(@ContextSource List<ErpSalReceipt> receipts) {
        orm().batchLoadProps(receipts, Collections.singleton("customer"));
        List<String> result = new ArrayList<>(receipts.size());
        for (ErpSalReceipt receipt : receipts) {
            result.add(receipt.getCustomer() != null ? receipt.getCustomer().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalReceipt.class)
    public List<String> currencyName(@ContextSource List<ErpSalReceipt> receipts) {
        orm().batchLoadProps(receipts, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(receipts.size());
        for (ErpSalReceipt receipt : receipts) {
            result.add(receipt.getCurrency() != null ? receipt.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalReceipt.class)
    public List<String> orgName(@ContextSource List<ErpSalReceipt> receipts) {
        orm().batchLoadProps(receipts, Collections.singleton("org"));
        List<String> result = new ArrayList<>(receipts.size());
        for (ErpSalReceipt receipt : receipts) {
            result.add(receipt.getOrg() != null ? receipt.getOrg().getName() : null);
        }
        return result;
    }
}
