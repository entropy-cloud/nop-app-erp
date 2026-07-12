
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurPaymentBiz;
import app.erp.md.biz.SettlementAllocation;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.service.processor.ErpPurPaymentProcessor;
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
 * 付款单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由 xbiz 一行委托注入 Processor；非审批动作（cancel/settle/reverseSettlement）
 * 在本类完成 Long→String 转换后委托 Processor。
 */
@BizModel("ErpPurPayment")
public class ErpPurPaymentBizModel extends CrudBizModel<ErpPurPayment> implements IErpPurPaymentBiz {

    @Inject
    ErpPurPaymentProcessor paymentProcessor;

    public ErpPurPaymentBizModel() {
        setEntityName(ErpPurPayment.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurPayment cancel(@Name("paymentId") Long paymentId, IServiceContext context) {
        return paymentProcessor.cancel(String.valueOf(paymentId), context);
    }

    @Override
    @BizMutation
    public ErpPurPayment settle(@Name("paymentId") Long paymentId,
                                @Name("allocations") List<SettlementAllocation> allocations,
                                IServiceContext context) {
        return paymentProcessor.settle(String.valueOf(paymentId), allocations, context);
    }

    @Override
    @BizMutation
    public ErpPurPayment reverseSettlement(@Name("paymentId") Long paymentId,
                                           @Name("invoiceId") Long invoiceId,
                                           IServiceContext context) {
        return paymentProcessor.reverseSettlement(String.valueOf(paymentId), invoiceId, context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------
    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

    @BizLoader(forType = ErpPurPayment.class)
    public List<String> supplierName(@ContextSource List<ErpPurPayment> payments) {
        orm().batchLoadProps(payments, Collections.singleton("supplier"));
        List<String> result = new ArrayList<>(payments.size());
        for (ErpPurPayment payment : payments) {
            result.add(payment.getSupplier() != null ? payment.getSupplier().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurPayment.class)
    public List<String> currencyName(@ContextSource List<ErpPurPayment> payments) {
        orm().batchLoadProps(payments, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(payments.size());
        for (ErpPurPayment payment : payments) {
            result.add(payment.getCurrency() != null ? payment.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurPayment.class)
    public List<String> orgName(@ContextSource List<ErpPurPayment> payments) {
        orm().batchLoadProps(payments, Collections.singleton("org"));
        List<String> result = new ArrayList<>(payments.size());
        for (ErpPurPayment payment : payments) {
            result.add(payment.getOrg() != null ? payment.getOrg().getName() : null);
        }
        return result;
    }
}
