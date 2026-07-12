
package app.erp.sal.service.entity;

import app.erp.sal.biz.IErpSalQuotationBiz;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.service.processor.ErpSalQuotationProcessor;
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
 * 销售报价单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由平台 {@code approval-support.xbiz} 标准 source 提供，业务联动经 xbiz
 * {@code <source x:override="replace">} 注入 {@link ErpSalQuotationProcessor#onSubmit}/{@link ErpSalQuotationProcessor#onApproved}。
 */
@BizModel("ErpSalQuotation")
public class ErpSalQuotationBizModel extends CrudBizModel<ErpSalQuotation> implements IErpSalQuotationBiz {

    @Inject
    ErpSalQuotationProcessor quotationProcessor;

    public ErpSalQuotationBizModel() {
        setEntityName(ErpSalQuotation.class.getName());
    }

    @Override
    @BizMutation
    public ErpSalQuotation cancel(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.cancel(String.valueOf(quotationId), context);
    }

    @Override
    @BizMutation
    public ErpSalQuotation confirmCustomerAccepted(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.confirmCustomerAccepted(String.valueOf(quotationId), context);
    }

    @Override
    @BizMutation
    public ErpSalOrder convertToOrder(@Name("quotationId") Long quotationId, IServiceContext context) {
        return quotationProcessor.convertToOrder(String.valueOf(quotationId), context);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name 字段 + BizLoader 批量加载防 N+1）----------
    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

    @BizLoader(forType = ErpSalQuotation.class)
    public List<String> customerName(@ContextSource List<ErpSalQuotation> quotations) {
        orm().batchLoadProps(quotations, Collections.singleton("customer"));
        List<String> result = new ArrayList<>(quotations.size());
        for (ErpSalQuotation quotation : quotations) {
            result.add(quotation.getCustomer() != null ? quotation.getCustomer().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalQuotation.class)
    public List<String> currencyName(@ContextSource List<ErpSalQuotation> quotations) {
        orm().batchLoadProps(quotations, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(quotations.size());
        for (ErpSalQuotation quotation : quotations) {
            result.add(quotation.getCurrency() != null ? quotation.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalQuotation.class)
    public List<String> orgName(@ContextSource List<ErpSalQuotation> quotations) {
        orm().batchLoadProps(quotations, Collections.singleton("org"));
        List<String> result = new ArrayList<>(quotations.size());
        for (ErpSalQuotation quotation : quotations) {
            result.add(quotation.getOrg() != null ? quotation.getOrg().getName() : null);
        }
        return result;
    }
}
